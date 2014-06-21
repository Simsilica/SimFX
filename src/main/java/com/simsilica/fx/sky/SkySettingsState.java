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
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.simsilica.fx.AtmosphericParameters;
import com.simsilica.fx.LightingState;
import com.simsilica.lemur.core.VersionedReference;
import com.simsilica.lemur.event.BaseAppState;
import com.simsilica.lemur.props.PropertyPanel;


/**
 *
 *
 *  @author    Paul Speed
 */
public class SkySettingsState extends BaseAppState {

    private AtmosphericParameters atmosphericParms;

    private float mieConstant;
    private float rayleighConstant;

    private VersionedReference<Vector3f> lightDir;
    
    private PropertyPanel settings;

    public SkySettingsState() {
    }

    public PropertyPanel getSettings() {
        return settings;
    }

    public void setMieConstant( float f ) {
        atmosphericParms.setMieConstant(f / 10);
        this.mieConstant = f / 10;
    }
    
    public float getMieConstant() {
        return this.mieConstant * 10;
    }
    
    public void setRayleighConstant( float f ) {
        atmosphericParms.setRayleighConstant(f / 10);
        this.rayleighConstant = f / 10;
    }
    
    public float getRayleighConstant() {
        return this.rayleighConstant * 10;
    }
    
    @Override
    protected void initialize( Application app ) {
    
        lightDir = getState(LightingState.class).getLightDirRef();

        atmosphericParms = getState(SkyState.class).getAtmosphericParameters();
        
        mieConstant = atmosphericParms.getMieConstant();
        rayleighConstant = atmosphericParms.getRayleighConstant(); 
        
        settings = new PropertyPanel("glass");
        settings.addFloatProperty("Intensity", atmosphericParms, "lightIntensity", 0, 100, 1);
        settings.addFloatProperty("Sky Exposure", atmosphericParms, "skyExposure", 0, 10, 0.1f);
        settings.addFloatProperty("Rayleigh Constant(x10)", this, "rayleighConstant", 0, 1, 0.001f);
        settings.addFloatProperty("Scale Depth", atmosphericParms, "averageDensityScale", 0, 1, 0.001f);
        settings.addFloatProperty("Mie Constant(x10)", this, "mieConstant", 0, 1, 0.001f);
        settings.addFloatProperty("MPA Factor", atmosphericParms, "miePhaseAsymmetryFactor", -1.5f, 0, 0.001f);
        settings.addFloatProperty("Flattening", atmosphericParms, "skyFlattening", 0, 1, 0.01f);
        settings.addFloatProperty("Red Wavelength (nm)", atmosphericParms, "redWavelength", 0, 1, 0.001f);
        settings.addFloatProperty("Green Wavelength (nm)", atmosphericParms, "greenWavelength", 0, 1, 0.001f);
        settings.addFloatProperty("Blue Wavelength (nm)", atmosphericParms, "blueWavelength", 0, 1, 0.001f);

        settings.addFloatProperty("Time", getState(LightingState.class), "timeOfDay", -0.1f, 1.1f, 0.01f);
        settings.addFloatProperty("Orientation", getState(LightingState.class), "orientation", 0f, FastMath.TWO_PI, 0.01f);
        
        settings.addBooleanProperty("Flat Shaded", getState(SkyState.class), "flatShaded");
    }

    @Override
    protected void cleanup( Application app ) {
    }

    @Override
    protected void enable() {
    }

    @Override
    protected void disable() {
    }
}
