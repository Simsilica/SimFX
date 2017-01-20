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
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.simsilica.lemur.core.VersionedHolder;
import com.simsilica.lemur.core.VersionedReference;
import com.simsilica.lemur.event.BaseAppState;

/**
 * @author Paul Speed
 */
public class LightingState extends BaseAppState {

    public static final ColorRGBA DEFAULT_DIFFUSE = ColorRGBA.White.mult(2);
    public static final ColorRGBA DEFAULT_AMBIENT = new ColorRGBA(0.25f, 0.25f, 0.25f, 1);

    public static final Vector3f NEGATE_Y = Vector3f.UNIT_Y.negate();

    protected VersionedHolder<Vector3f> lightDir;

    protected DirectionalLight sun;
    protected AmbientLight ambient;
    protected Node rootNode;

    protected Quaternion temp1;
    protected Quaternion temp2;

    private float timeOfDay;
    private float inclination;
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
        this.sun = new DirectionalLight();
        this.sun.setColor(DEFAULT_DIFFUSE);
        this.sun.setDirection(lightDir.getObject());
        this.ambient = new AmbientLight();
        this.ambient.setColor(DEFAULT_AMBIENT);
        resetLightDir(); // just in case it didn't change but we still need to calculate it
        setEnabled(false);
    }

    public DirectionalLight getSun() {
        return sun;
    }

    public AmbientLight getAmbient() {
        return ambient;
    }

    public VersionedReference<Vector3f> getLightDirRef() {
        return lightDir.createReference();
    }

    public void setSunColor(ColorRGBA color) {
        this.sun.setColor(color);
    }

    public ColorRGBA getSunColor() {
        return sun.getColor();
    }

    public void setAmbientColor(ColorRGBA ambient) {
        this.ambient.setColor(ambient);
    }

    public ColorRGBA getAmbientColor() {
        return ambient.getColor();
    }

    public void setTimeOfDay(final float time) {
        if (timeOfDay == time) return;
        timeOfDay = time;
        resetLightDir();
    }

    public float getTimeOfDay() {
        return timeOfDay;
    }

    public void setOrientation(final float orientation) {
        if (this.orientation == orientation) return;
        this.orientation = orientation;
        resetLightDir();
    }

    public float getOrientation() {
        return orientation;
    }

    protected void resetLightDir() {

        float angle = timeOfDay * FastMath.PI;

        temp1.fromAngles(0, 0, (angle - FastMath.HALF_PI));
        temp2.fromAngles(inclination, orientation, 0);
        temp2.multLocal(temp1).mult(NEGATE_Y, lightDir.getObject());

        lightDir.setObject(lightDir.getObject());

        if (sun != null) {
            sun.setDirection(lightDir.getObject());
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
        rootNode.addLight(sun);
        rootNode.addLight(ambient);
    }

    @Override
    protected void disable() {
        if (rootNode == null) return;
        rootNode.removeLight(sun);
        rootNode.removeLight(ambient);
    }
}

