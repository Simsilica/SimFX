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
import com.simsilica.fx.LightingState;
import com.simsilica.fx.geom.TruncatedDome;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.core.VersionedReference;
import com.simsilica.lemur.event.BaseAppState;

/**
 * @author Paul Speed
 */
public class SkyState extends BaseAppState {

    public static final int EARTH_RADIUS = 6378100;

    public static final ColorRGBA SUN_COLOR = new ColorRGBA(1, 1, 0.9f, 1);
    public static final ColorRGBA FLAT_COLOR = new ColorRGBA(0.5f, 0.5f, 1f, 1);
    public static final ColorRGBA GROUND_COLOR = new ColorRGBA(0.25f, 0.25f, 0.3f, 1);

    protected AtmosphericParameters atmosphericParms;
    protected VersionedReference<Vector3f> lightDir;

    protected Vector3f temp1;

    protected Node rootNode;

    protected Geometry sky;
    protected Geometry sun;
    protected Geometry groundDisc;

    protected ColorRGBA lightingColor;

    protected Material flatMaterial;
    protected Material atmosphericMaterial;
    protected Material groundMaterial;
    protected Material sunMaterial;

    protected float domeInnerRadius;
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

        this.sun = new Geometry("Sun", sunSphere);
        this.sky = new Geometry("Sky", skyDome);
        this.sky.setModelBound(new BoundingSphere(Float.POSITIVE_INFINITY, Vector3f.ZERO));
        this.sky.setMaterial(flatMaterial);
        this.sky.setQueueBucket(Bucket.Sky);
        this.sky.setCullHint(CullHint.Never);
        this.groundDisc = new Geometry("ground", ground);
        this.groundDisc.rotate(FastMath.PI, 0, 0);
        this.groundDisc.setQueueBucket(Bucket.Sky);
        this.groundDisc.setCullHint(CullHint.Never);
    }

    public AtmosphericParameters getAtmosphericParameters() {
        return atmosphericParms;
    }

    public void setSkyParent(final Node node) {
        this.rootNode = node;
    }

    public Node getSkyParent() {
        return rootNode;
    }

    public void setFlatShaded(final boolean flatShaded) {
        if (isFlatShaded() == flatShaded) return;
        this.flatShaded = flatShaded;
        resetMaterials();
    }

    public boolean isFlatShaded() {
        return flatShaded;
    }

    public void setShowGroundDisc(final boolean showGround) {
        if (isShowGroundDisc() == showGround) return;
        this.showGround = showGround;
        resetGround();
    }

    public boolean isShowGroundDisc() {
        return showGround;
    }

    public ColorRGBA getGroundColor() {
        if (groundMaterial == null) return null;
        final MatParam matParam = groundMaterial.getParam("Color");
        return (ColorRGBA) matParam.getValue();
    }

    public void setGroundColor(final ColorRGBA groundColor) {
        if (groundMaterial == null) return;
        groundMaterial.setParam("Color", VarType.Vector4, groundColor);
    }

    public ColorRGBA getSunColor() {
        if (sunMaterial == null) return null;
        final MatParam matParam = sunMaterial.getParam("Color");
        return (ColorRGBA) matParam.getValue();
    }

    public void setSunColor(final ColorRGBA sunColor) {
        if (sunMaterial == null) return;
        sunMaterial.setParam("Color", VarType.Vector4, sunColor);
    }

    public ColorRGBA getFlatColor() {
        if (flatMaterial == null) return null;
        final MatParam matParam = flatMaterial.getParam("Color");
        return (ColorRGBA) matParam.getValue();
    }

    public void setFlatColor(final ColorRGBA flatColor) {
        if (flatMaterial == null) return;
        flatMaterial.setParam("Color", VarType.Vector4, flatColor);
    }

    protected void resetMaterials() {
        if (isFlatShaded()) {
            sky.setMaterial(flatMaterial);
            sun.setCullHint(CullHint.Inherit);
            groundMaterial.setBoolean("UseScattering", false);
        } else {
            sky.setMaterial(atmosphericMaterial);
            sun.setCullHint(CullHint.Never);
            groundMaterial.setBoolean("UseScattering", true);
        }
    }

    protected void resetGround() {
        if (groundDisc == null) return;
        if (!isEnabled()) return;
        if (isShowGroundDisc()) {
            rootNode.attachChild(groundDisc);
        } else {
            groundDisc.removeFromParent();
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
            sun.setMaterial(sunMaterial);
        }

        if (flatMaterial == null) {
            flatMaterial = guiGlobals.createMaterial(FLAT_COLOR.clone(), false).getMaterial();
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
            groundDisc.setMaterial(groundMaterial);
            atmosphericParms.applyGroundParameters(groundMaterial, true);
        }

        sun.move(lightDirection.mult(-900, temp1));

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
            sun.setLocalTranslation(direction.mult(-900, temp1));
            atmosphericParms.setLightDirection(direction);
            atmosphericParms.calculateGroundColor(ColorRGBA.White, Vector3f.UNIT_X, 1f, 0, lightingColor);
        }

        if (isFlatShaded()) {
            sky.setLocalTranslation(getApplication().getCamera().getLocation());
        }
    }

    @Override
    protected void enable() {
        if (rootNode == null) return;
        rootNode.attachChild(sky);
        if (isShowGroundDisc()) {
            rootNode.attachChild(groundDisc);
        }
    }

    @Override
    protected void disable() {
        if (rootNode == null) return;
        sky.removeFromParent();
        groundDisc.removeFromParent();
    }
}
