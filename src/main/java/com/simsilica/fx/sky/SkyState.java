/*
 * $Id: SkyState.java 174 2014-07-01 08:22:41Z pspeed42 $
 * 
 * Copyright (c) 2014, Simsilica, LLC
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its 
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED 
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.simsilica.fx.sky;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingSphere;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Sphere;
import com.jme3.shader.VarType;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import com.simsilica.fx.LightingState;
import com.simsilica.fx.geom.TruncatedDome;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.core.VersionedReference;
import com.simsilica.lemur.event.BaseAppState;

import java.io.IOException;

/**
 * @author Paul Speed
 */
public class SkyState extends BaseAppState implements Savable, Cloneable, JmeCloneable {

    public static final int EARTH_RADIUS = 6378100;

    public static final ColorRGBA SUN_COLOR = new ColorRGBA(1, 1, 0.9f, 1);
    public static final ColorRGBA FLAT_COLOR = new ColorRGBA(0.5f, 0.5f, 1f, 1);
    public static final ColorRGBA GROUND_COLOR = new ColorRGBA(0.25f, 0.25f, 0.3f, 1);

    /**
     * The atmospheric parameters.
     */
    protected AtmosphericParameters atmosphericParms;

    /**
     * The reference to light direction.
     */
    protected VersionedReference<Vector3f> lightDir;

    protected Vector3f temp1;

    /**
     * The root node.
     */
    protected Node rootNode;

    /**
     * The sky geometry.
     */
    protected Geometry skyGeometry;

    /**
     * The sun geometry.
     */
    protected Geometry sunGeometry;

    /**
     * The ground geometry.
     */
    protected Geometry groundGeometry;

    /**
     * The lighting color.
     */
    protected ColorRGBA lightingColor;

    /**
     * The flattening material.
     */
    protected Material flatteningMaterial;

    /**
     * The atmospheric material.
     */
    protected Material atmosphericMaterial;

    /**
     * The ground material.
     */
    protected Material groundMaterial;

    /**
     * The sun material.
     */
    protected Material sunMaterial;

    /**
     * The dome inner radius.
     */
    protected float domeInnerRadius;

    /**
     * The dome outer radius.
     */
    protected float domeOuterRadius;

    protected boolean showGround;
    protected boolean flatShaded;

    public SkyState() {
        this.temp1 = new Vector3f();
        this.lightingColor = new ColorRGBA(1, 1, 1, 1);
        this.domeInnerRadius = 2000;
        this.domeOuterRadius = 2000 * 1.025f;
        this.atmosphericParms = new AtmosphericParameters();
        this.atmosphericParms.setSkyDomeRadius(domeOuterRadius);
        this.atmosphericParms.setPlanetRadius(SkyState.EARTH_RADIUS * 0.01f);
        this.showGround = true;
        this.flatShaded = true;

        final Sphere sunSphere = new Sphere(6, 12, 50);
        final TruncatedDome skyDome = new TruncatedDome(domeInnerRadius, domeOuterRadius, 100, 50, true);
        final TruncatedDome ground = new TruncatedDome(domeInnerRadius, domeOuterRadius, 100, 50, true);

        this.sunGeometry = new Geometry("Sun", sunSphere);
        this.skyGeometry = new Geometry("Sky", skyDome);
        this.skyGeometry.setModelBound(new BoundingSphere(Float.POSITIVE_INFINITY, Vector3f.ZERO));
        this.skyGeometry.setMaterial(flatteningMaterial);
        this.skyGeometry.setQueueBucket(Bucket.Sky);
        this.skyGeometry.setCullHint(CullHint.Never);
        this.groundGeometry = new Geometry("ground", ground);
        this.groundGeometry.rotate(FastMath.PI, 0, 0);
        this.groundGeometry.setQueueBucket(Bucket.Sky);
        this.groundGeometry.setCullHint(CullHint.Never);
    }

    /**
     * @return the atmospheric parameters.
     */
    public AtmosphericParameters getAtmosphericParameters() {
        return atmosphericParms;
    }

    /**
     * @param node the parent node.
     */
    public void setSkyParent(final Node node) {
        this.rootNode = node;
    }

    /**
     * @return the parent node.
     */
    public Node getSkyParent() {
        return rootNode;
    }

    /**
     * @param flatShaded true if need to use flat shader.
     */
    public void setFlatShaded(final boolean flatShaded) {
        if (isFlatShaded() == flatShaded) return;
        this.flatShaded = flatShaded;
        resetMaterials();
    }

    /**
     * @return true if need to use flat shader.
     */
    public boolean isFlatShaded() {
        return flatShaded;
    }

    /**
     * @param showGround true if need to show ground geometry.
     */
    public void setShowGroundGeometry(final boolean showGround) {
        if (isShowGroundGeometry() == showGround) return;
        this.showGround = showGround;
        resetGround();
    }

    /**
     * @return true if need to show ground geometry.
     */
    public boolean isShowGroundGeometry() {
        return showGround;
    }

    /**
     * @return the ground color.
     */
    public ColorRGBA getGroundColor() {
        if (groundMaterial == null) return null;
        final MatParam matParam = groundMaterial.getParam("Color");
        return (ColorRGBA) matParam.getValue();
    }

    /**
     * @param groundColor the ground color.
     */
    public void setGroundColor(final ColorRGBA groundColor) {
        if (groundMaterial == null) return;
        groundMaterial.setParam("Color", VarType.Vector4, groundColor);
    }

    /**
     * @return the sun color.
     */
    public ColorRGBA getSunColor() {
        if (sunMaterial == null) return null;
        final MatParam matParam = sunMaterial.getParam("Color");
        return (ColorRGBA) matParam.getValue();
    }

    /**
     * @param sunColor the sun color.
     */
    public void setSunColor(final ColorRGBA sunColor) {
        if (sunMaterial == null) return;
        sunMaterial.setParam("Color", VarType.Vector4, sunColor);
    }

    /**
     * @return the flattening color.
     */
    public ColorRGBA getFlatteningColor() {
        if (flatteningMaterial == null) return null;
        final MatParam matParam = flatteningMaterial.getParam("Color");
        return (ColorRGBA) matParam.getValue();
    }

    /**
     * @param flatColor the flattening color.
     */
    public void setFlatteningColor(final ColorRGBA flatColor) {
        if (flatteningMaterial == null) return;
        flatteningMaterial.setParam("Color", VarType.Vector4, flatColor);
    }

    protected void resetMaterials() {
        if (isFlatShaded()) {
            skyGeometry.setMaterial(flatteningMaterial);
            sunGeometry.setCullHint(CullHint.Inherit);
            groundMaterial.setBoolean("UseScattering", false);
        } else {
            skyGeometry.setMaterial(atmosphericMaterial);
            sunGeometry.setCullHint(CullHint.Never);
            groundMaterial.setBoolean("UseScattering", true);
        }
    }

    protected void resetGround() {
        if (groundGeometry == null) return;
        if (!isEnabled()) return;
        if (isShowGroundGeometry()) {
            rootNode.attachChild(groundGeometry);
        } else {
            groundGeometry.removeFromParent();
        }
    }

    @Override
    protected void initialize(final Application app) {

        final AssetManager assetManager = app.getAssetManager();

        if (GuiGlobals.getInstance() == null) {
            GuiGlobals.initialize(app);
        }

        final GuiGlobals guiGlobals = GuiGlobals.getInstance();

        if (rootNode == null) {
            rootNode = ((SimpleApplication) app).getRootNode();
        }

        final LightingState state = getState(LightingState.class);
        lightDir = state.getLightDirRef();

        final Vector3f lightDirection = lightDir.get();

        if (sunMaterial == null) {
            sunMaterial = guiGlobals.createMaterial(SUN_COLOR.clone(), false).getMaterial();
            sunGeometry.setMaterial(sunMaterial);
        }

        if (flatteningMaterial == null) {
            flatteningMaterial = guiGlobals.createMaterial(FLAT_COLOR.clone(), false).getMaterial();
        }

        if (atmosphericMaterial == null) {
            atmosphericMaterial = atmosphericParms.getSkyMaterial(assetManager);
        }

        atmosphericParms.setLightDirection(lightDirection);

        if (groundMaterial == null) {
            groundMaterial = new Material(assetManager, "MatDefs/GroundAtmospherics.j3md");
            groundMaterial.setColor("GroundColor", GROUND_COLOR.clone());
            groundMaterial.setBoolean("FollowCamera", true);
            groundMaterial.setBoolean("UseScattering", true);
            groundMaterial.setFloat("GroundScale", 10);
            groundGeometry.setMaterial(groundMaterial);
            atmosphericParms.applyGroundParameters(groundMaterial, true);
        }

        sunGeometry.move(lightDirection.mult(-900, temp1));

        atmosphericParms.calculateGroundColor(ColorRGBA.White, Vector3f.UNIT_X, 1f, 0, lightingColor);

        resetMaterials();
        resetGround();
    }

    @Override
    protected void cleanup(final Application app) {
        lightDir = null;
    }

    @Override
    public void update(float tpf) {

        if (lightDir.update()) {
            final Vector3f direction = lightDir.get();
            sunGeometry.setLocalTranslation(direction.mult(-900, temp1));
            atmosphericParms.setLightDirection(direction);
            atmosphericParms.calculateGroundColor(ColorRGBA.White, Vector3f.UNIT_X, 1f, 0, lightingColor);
        }

        if (isFlatShaded()) {
            skyGeometry.setLocalTranslation(getApplication().getCamera().getLocation());
        }
    }

    @Override
    protected void enable() {
        if (rootNode == null) return;
        rootNode.attachChild(skyGeometry);
        if (isShowGroundGeometry()) {
            rootNode.attachChild(groundGeometry);
        }
    }

    @Override
    protected void disable() {
        if (rootNode == null) return;
        skyGeometry.removeFromParent();
        groundGeometry.removeFromParent();
    }

    @Override
    public Object jmeClone() {
        try {
            return super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void cloneFields(final Cloner cloner, final Object original) {
        temp1 = cloner.clone(temp1);
        skyGeometry = cloner.clone(skyGeometry);
        sunGeometry = cloner.clone(sunGeometry);
        groundGeometry = cloner.clone(groundGeometry);
        lightingColor = cloner.clone(lightingColor);
        flatteningMaterial = cloner.clone(flatteningMaterial);
        atmosphericMaterial = cloner.clone(atmosphericMaterial);
        groundMaterial = cloner.clone(groundMaterial);
        sunMaterial = cloner.clone(sunMaterial);
        atmosphericParms = cloner.clone(atmosphericParms);
    }

    @Override
    public void write(final JmeExporter exporter) throws IOException {
        final OutputCapsule capsule = exporter.getCapsule(this);
        capsule.write(skyGeometry, "skyGeometry", null);
        capsule.write(sunGeometry, "sunGeometry", null);
        capsule.write(groundGeometry, "groundGeometry", null);
        capsule.write(lightingColor, "lightingColor", null);
        capsule.write(flatteningMaterial, "flatteningMaterial", null);
        capsule.write(atmosphericMaterial, "atmosphericMaterial", null);
        capsule.write(groundMaterial, "groundMaterial", null);
        capsule.write(sunMaterial, "sunMaterial", null);
        capsule.write(atmosphericParms, "atmosphericParms", null);
        capsule.write(flatShaded, "flatShaded", false);
        capsule.write(showGround, "showGround", false);
        capsule.write(domeInnerRadius, "domeInnerRadius", 0);
        capsule.write(domeOuterRadius, "domeOuterRadius", 0);
    }

    @Override
    public void read(final JmeImporter importer) throws IOException {
        final InputCapsule capsule = importer.getCapsule(this);
        skyGeometry = (Geometry) capsule.readSavable("skyGeometry", null);
        sunGeometry = (Geometry) capsule.readSavable("sunGeometry", null);
        groundGeometry = (Geometry) capsule.readSavable("groundGeometry", null);
        lightingColor = (ColorRGBA) capsule.readSavable("lightingColor", null);
        flatteningMaterial = (Material) capsule.readSavable("flatteningMaterial", null);
        atmosphericMaterial = (Material) capsule.readSavable("atmosphericMaterial", null);
        groundMaterial = (Material) capsule.readSavable("groundMaterial", null);
        sunMaterial = (Material) capsule.readSavable("sunMaterial", null);
        atmosphericParms = (AtmosphericParameters) capsule.readSavable("atmosphericParms", null);
        flatShaded = capsule.readBoolean("flatShaded", false);
        showGround = capsule.readBoolean("showGround", false);
        domeInnerRadius = capsule.readFloat("domeInnerRadius", 0);
        domeOuterRadius = capsule.readFloat("domeOuterRadius", 0);
    }
}
