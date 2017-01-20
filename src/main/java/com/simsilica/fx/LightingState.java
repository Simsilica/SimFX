/*
 * $Id: LightingState.java 161 2014-06-28 16:14:28Z pspeed42 $
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

package com.simsilica.fx;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import com.simsilica.lemur.core.VersionedHolder;
import com.simsilica.lemur.core.VersionedReference;
import com.simsilica.lemur.event.BaseAppState;

import java.io.IOException;

/**
 * @author Paul Speed
 */
public class LightingState extends BaseAppState implements Savable, Cloneable, JmeCloneable {

    public static final ColorRGBA DEFAULT_DIFFUSE = ColorRGBA.White.mult(2);
    public static final ColorRGBA DEFAULT_AMBIENT = new ColorRGBA(0.25f, 0.25f, 0.25f, 1);

    public static final Vector3f NEGATE_Y = Vector3f.UNIT_Y.negate();

    /**
     * The light direction reference.
     */
    protected VersionedHolder<Vector3f> lightDir;

    /**
     * The sunGeometry light.
     */
    protected DirectionalLight sunLight;

    /**
     * The ambient light.
     */
    protected AmbientLight ambientLight;

    /**
     * The node to attach lights.
     */
    protected Node rootNode;

    protected Quaternion temp1;
    protected Quaternion temp2;

    /**
     * The time of day.
     */
    private float timeOfDay;

    /**
     * The inclination.
     */
    private float inclination;

    /**
     * The orientation.
     */
    private float orientation;

    public LightingState() {
        this(FastMath.atan2(1, 0.3f) / FastMath.PI);
    }

    public LightingState(final float time) {
        this.lightDir = new VersionedHolder<>(new Vector3f());
        this.temp1 = new Quaternion();
        this.temp2 = new Quaternion();
        this.timeOfDay = time;
        this.inclination = FastMath.HALF_PI - FastMath.atan2(1, 0.4f);
        this.orientation = 0; //FastMath.HALF_PI;
        this.sunLight = new DirectionalLight();
        this.sunLight.setColor(DEFAULT_DIFFUSE);
        this.sunLight.setDirection(lightDir.getObject());
        this.ambientLight = new AmbientLight();
        this.ambientLight.setColor(DEFAULT_AMBIENT);
        resetLightDir(); // just in case it didn't change but we still need to calculate it
    }

    /**
     * @return the sunGeometry light.
     */
    public DirectionalLight getSunLight() {
        return sunLight;
    }

    /**
     * @return the ambient light.
     */
    public AmbientLight getAmbientLight() {
        return ambientLight;
    }

    /**
     * @return the light direction reference.
     */
    public VersionedReference<Vector3f> getLightDirRef() {
        return lightDir.createReference();
    }

    public void setSunColor(final ColorRGBA color) {
        this.sunLight.setColor(color);
    }

    public ColorRGBA getSunColor() {
        return sunLight.getColor();
    }

    /**
     * @param ambientColor the ambient color.
     */
    public void setAmbientColor(final ColorRGBA ambientColor) {
        this.ambientLight.setColor(ambientColor);
    }

    /**
     * @return the ambient color.
     */
    public ColorRGBA getAmbientColor() {
        return ambientLight.getColor();
    }

    /**
     * @param time the time of day.
     */
    public void setTimeOfDay(final float time) {
        if (timeOfDay == time) return;
        timeOfDay = time;
        resetLightDir();
    }

    /**
     * @return the time of day.
     */
    public float getTimeOfDay() {
        return timeOfDay;
    }

    /**
     * @param orientation the orientation.
     */
    public void setOrientation(final float orientation) {
        if (this.orientation == orientation) return;
        this.orientation = orientation;
        resetLightDir();
    }

    /**
     * @return the orientation.
     */
    public float getOrientation() {
        return orientation;
    }

    /**
     * Update a light direction.
     */
    protected void resetLightDir() {

        float angle = timeOfDay * FastMath.PI;

        temp1.fromAngles(0, 0, (angle - FastMath.HALF_PI));
        temp2.fromAngles(inclination, orientation, 0);
        temp2.multLocal(temp1).mult(NEGATE_Y, lightDir.getObject());

        lightDir.setObject(lightDir.getObject());

        if (sunLight != null) {
            sunLight.setDirection(lightDir.getObject());
        }
    }

    @Override
    protected void initialize(final Application app) {

        if (rootNode == null) {
            rootNode = ((SimpleApplication) app).getRootNode();
        }

        resetLightDir();
    }

    @Override
    protected void cleanup(Application app) {
        rootNode = null;
    }

    /**
     * @param rootNode the root node.
     */
    public void setRootNode(final Node rootNode) {
        this.rootNode = rootNode;
    }

    /**
     * @return the root node.
     */
    public Node getRootNode() {
        return rootNode;
    }

    @Override
    protected void enable() {
        if (rootNode == null) return;
        rootNode.addLight(sunLight);
        rootNode.addLight(ambientLight);
    }

    @Override
    protected void disable() {
        if (rootNode == null) return;
        rootNode.removeLight(sunLight);
        rootNode.removeLight(ambientLight);
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
        lightDir = new VersionedHolder<>(new Vector3f());
        temp1 = new Quaternion();
        temp2 = new Quaternion();
        rootNode = cloner.clone(rootNode);
        ambientLight = cloner.clone(ambientLight);
        sunLight = cloner.clone(sunLight);
        resetLightDir();
    }

    @Override
    public void write(final JmeExporter exporter) throws IOException {
        final OutputCapsule capsule = exporter.getCapsule(this);
        capsule.write(isEnabled(), "enabled", false);
        capsule.write(getAmbientColor(), "ambientLight", ColorRGBA.White);
        capsule.write(getSunColor(), "sunLight", ColorRGBA.White);
        capsule.write(getTimeOfDay(), "timeOfDay", FastMath.atan2(1, 0.3f) / FastMath.PI);
        capsule.write(getOrientation(), "orientation", 0);
    }

    @Override
    public void read(final JmeImporter importer) throws IOException {
        final InputCapsule capsule = importer.getCapsule(this);
        setEnabled(capsule.readBoolean("enabled", false));
        setAmbientColor((ColorRGBA) capsule.readSavable("ambientLight", ColorRGBA.White));
        setSunColor((ColorRGBA) capsule.readSavable("sunLight", ColorRGBA.White));
        setTimeOfDay(capsule.readFloat("timeOfDay", FastMath.atan2(1, 0.3f) / FastMath.PI));
        setOrientation(capsule.readFloat("orientation", 0F));
    }
}

