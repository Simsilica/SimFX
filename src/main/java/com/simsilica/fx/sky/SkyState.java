/*
 * $Id$
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
import com.jme3.bounding.BoundingSphere;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Sphere;
import com.simsilica.fx.LightingState;
import com.simsilica.fx.geom.TruncatedDome;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.core.VersionedReference;
import com.simsilica.lemur.event.BaseAppState;


/**
 *
 *
 *  @author    Paul Speed
 */
public class SkyState extends BaseAppState {

    private Node rootNode;

    private Geometry sky;
    private Geometry sun;
    private float domeInnerRadius = 2000;
    private float domeOuterRadius = 2000 * 1.025f;
 
    private boolean flat = false;
    
    // Colors used for flat shading   
    private ColorRGBA skyColor;
    private ColorRGBA sunColor;
 
    private Material flatMaterial;
    private Material atmosphericMaterial;
    
    private AtmosphericParameters atmosphericParms;
    
    private VersionedReference<Vector3f> lightDir;

    public SkyState() {
        this.sunColor = new ColorRGBA(1, 1, 0.9f, 1);
        this.skyColor = new ColorRGBA(0.5f, 0.5f, 1f, 1);
        
        float earthRadius = 6378100;
 
        this.domeInnerRadius = 2000;
        this.domeOuterRadius = 2000 * 1.025f;
 
        atmosphericParms = new AtmosphericParameters();
        atmosphericParms.setSkyDomeRadius(domeOuterRadius);
        atmosphericParms.setPlanetRadius(earthRadius * 0.01f);         
    }

    public AtmosphericParameters getAtmosphericParameters() {
        return atmosphericParms;
    }

    public void setSkyParent( Node node ) {
        this.rootNode = node;
    }

    public Node getSkyParent() {
        return rootNode;
    }

    public void setFlatShaded( boolean b ) {
        if( flat == b ) {
            return;
        }
        this.flat = b;
        resetMaterials();
    }
    
    public boolean isFlatShaded() {
        return flat;
    }
    
    protected void resetMaterials() {
        if( flat ) {
            sky.setMaterial(flatMaterial);
            sun.setCullHint(CullHint.Inherit);
        } else {
            sky.setMaterial(atmosphericMaterial);
            sun.setCullHint(CullHint.Never);
        }
    }
    
    @Override
    protected void initialize( Application app ) {
        if( rootNode == null ) {
            rootNode = ((SimpleApplication)app).getRootNode();
        }
        
        lightDir = getState(LightingState.class).getLightDirRef();
        atmosphericParms.setLightDirection(lightDir.get());
        
 
        // Add a sun sphere
        Sphere orb = new Sphere(6, 12, 50);
        sun = new Geometry("Sun", orb);
        Material mat = GuiGlobals.getInstance().createMaterial(sunColor, false).getMaterial(); 
        sun.setMaterial(mat);
        sun.move(lightDir.get().mult(-900));

        TruncatedDome skyDome = new TruncatedDome(domeInnerRadius, domeOuterRadius, 100, 50, true);
        sky = new Geometry("Sky", skyDome); 
        sky.setModelBound(new BoundingSphere(Float.POSITIVE_INFINITY, Vector3f.ZERO));        
        flatMaterial = GuiGlobals.getInstance().createMaterial(skyColor, false).getMaterial();
        //flatMaterial.getAdditionalRenderState().setWireframe(true);
        sky.setMaterial(flatMaterial);
        atmosphericMaterial = atmosphericParms.getSkyMaterial(app.getAssetManager());
        
        sky.setMaterial(atmosphericMaterial); 
        sky.setQueueBucket(Bucket.Sky);
        sky.setCullHint(CullHint.Never);
    }

    @Override
    protected void cleanup( Application app ) {
    }

    @Override
    public void update( float tpf ) {
        if( lightDir.update() ) {
            sun.setLocalTranslation(lightDir.get().mult(-900));
            atmosphericParms.setLightDirection(lightDir.get());
        }
        
        if( flat ) {
            sky.setLocalTranslation(getApplication().getCamera().getLocation());
        }
    }

    @Override
    protected void enable() {
        rootNode.attachChild(sky);
    }

    @Override
    protected void disable() {
        sky.removeFromParent();
    }
}
