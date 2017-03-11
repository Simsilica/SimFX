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
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
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
 
    private Geometry groundDisc;
    private boolean showGround = true;
 
    private boolean flat = false;
    
    // Colors used for flat shading   
    private ColorRGBA skyColor;
    private ColorRGBA sunColor;
 
    // Color calculated from current atmospheric parms
    private ColorRGBA lightingColor = new ColorRGBA(1, 1, 1, 1);
 
    private Material flatMaterial;
    private Material atmosphericMaterial;
    
    private ColorRGBA groundColor = new ColorRGBA(0.25f, 0.25f, 0.3f, 1); 
    private Material groundMaterial;
    
    private AtmosphericParameters atmosphericParms;
    
    private VersionedReference<Vector3f> lightDir;

    public SkyState() {
        this(null, false);
    }
    
    public SkyState( ColorRGBA groundColor ) {
        this(groundColor, groundColor != null);   
    }
    
    public SkyState( ColorRGBA groundColor, boolean showGroundDisc ) {
        
        this.showGround = showGroundDisc;
        if( groundColor != null ) {
            this.groundColor.set(groundColor);
        } 
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
    
    public void setShowGroundDisc( boolean b ) {
        if( showGround == b ) {
            return;
        }
        this.showGround = b;
        resetGround();
    }
    
    public boolean getShowGroundDisc() {
        return showGround;
    }
    
    public Material getGroundDiscMaterial() {
        return groundMaterial;
    }

    public ColorRGBA getGroundColor() {
        return groundColor;
    }

    public ColorRGBA getLightingColor() {
        return lightingColor;
    }
    
    protected void resetMaterials() {
        if( flat ) {
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
        if( groundDisc == null ) {
            return;
        }
        if( isEnabled() ) {
            if( showGround ) {
                rootNode.attachChild(groundDisc);
            } else {
                groundDisc.removeFromParent();
            }
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
        
        AssetManager assets = app.getAssetManager();
        
        TruncatedDome ground = new TruncatedDome(domeInnerRadius, domeOuterRadius, 100, 50, true);
        groundDisc = new Geometry("ground", ground);
        groundDisc.rotate(FastMath.PI, 0, 0);
        groundDisc.setQueueBucket(Bucket.Sky);
        groundDisc.setCullHint(CullHint.Never);
        groundMaterial = mat = new Material(assets, "MatDefs/GroundAtmospherics.j3md");
        mat.setColor("GroundColor", groundColor);
        mat.setBoolean("FollowCamera", true);
        mat.setBoolean("UseScattering", true);
        mat.setFloat("GroundScale", 10);
        //mat.getAdditionalRenderState().setWireframe(true);
        groundDisc.setMaterial(mat);
        atmosphericParms.applyGroundParameters(mat, true);
           
        atmosphericParms.calculateGroundColor(ColorRGBA.White, Vector3f.UNIT_X, 1f, 0, lightingColor);
    }

    @Override
    protected void cleanup( Application app ) {
    }

    @Override
    public void update( float tpf ) {
        if( lightDir.update() ) {
            sun.setLocalTranslation(lightDir.get().mult(-900));
            atmosphericParms.setLightDirection(lightDir.get());
            atmosphericParms.calculateGroundColor(ColorRGBA.White, Vector3f.UNIT_X, 1f, 0, lightingColor);
        }
        
        if( flat ) {
            sky.setLocalTranslation(getApplication().getCamera().getLocation());
        }
    }

    @Override
    protected void enable() {
        rootNode.attachChild(sky);
        
        resetMaterials();
        resetGround();
    }

    @Override
    protected void disable() {
        sky.removeFromParent();
        groundDisc.removeFromParent();
    }    
}
