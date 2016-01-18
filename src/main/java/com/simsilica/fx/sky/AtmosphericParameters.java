/*
 * $Id: AtmosphericParameters.java 149 2014-06-22 12:02:17Z pspeed42 $
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

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import java.util.HashSet;
import java.util.Set;




/**
 *
 *
 *  @author    Paul Speed
 */
public class AtmosphericParameters {

    // This one will be common and global so we might as
    // well keep an instance around.
    private Material skyMaterial;
 
    // For auto-updating if the caller opts for it
    private Set<Material> groundMaterials = new HashSet<Material>();
       
    /**
     *  The 'position' of the light in the sky, ie:
     *  -direction.
     */
    private Vector3f sunPosition = new Vector3f();
 
    private float lightIntensity;   
    private float skyExposure;
    private float groundExposure;
    private float skyGamma;
    private float groundGamma;
    
    private Vector3f wavelengths = new Vector3f();
    private Vector3f wavelengthsPow4 = new Vector3f();
    private Vector3f invPow4Wavelengths = new Vector3f();
    private Vector3f invPow4WavelengthsKrESun = new Vector3f();
    private Vector4f scatteringConstants = new Vector4f();
    private Vector3f kWavelengths4PI = new Vector3f();
    private float mpaFactor;
        
    private float innerRadius;
    private float outerRadius;
    private float averageDensityScale;
    private float kFlatteningSky;
    private float skyDomeRadius;
    private float planetRadius; // used for ground scale 
 
    private float skyFlattening = 0;
    
    public AtmosphericParameters() {
        setWavelengths(0.650f, 0.570f, 0.475f);
        setRayleighConstant(0.0025f);
        setMieConstant(0.001f);
        this.mpaFactor = -0.990f;
 
        this.sunPosition.set(0, 1, 0);       
        this.lightIntensity = 20;
        this.skyExposure = 1;
        this.groundExposure = 1;
        this.skyGamma = 2.0f;
        this.groundGamma = 0;

        this.innerRadius = 10;
        this.outerRadius = 10.25f;
        this.averageDensityScale = 0.25f;
        this.skyDomeRadius = 10;
        this.planetRadius = 10;  
        this.kFlatteningSky = 0.0f;
    }
    
    public Material getSkyMaterial( AssetManager assets ) {
 
        if( skyMaterial != null ) {
            return skyMaterial;
        }
       
        skyMaterial = new Material(assets, "MatDefs/SkyAtmospherics.j3md");
        skyMaterial.setVector3("SunPosition", sunPosition);
        skyMaterial.setVector3("InvWavelengthsKrESun", invPow4WavelengthsKrESun);        
        skyMaterial.setVector3("KWavelengths4PI", kWavelengths4PI);        

        updateSkyMaterial(skyMaterial);        

        return skyMaterial; 
    }

    protected void updateMaterials() {
        // Right now just the one potential
        if( skyMaterial != null ) {
            updateSkyMaterial(skyMaterial);
        }
        for( Material m : groundMaterials ) {
            applyGroundParameters(m);
        }
    }

    protected void updatePackedStructures() {
        //vec3 attenuation = exp(-scatter * (m_InvWavelengths * r4PI + m4PI));
        // K(wavelengths) * 4 * PI = m_InvWavelengths * r4PI + m4PI        
        float r4PI = scatteringConstants.y; 
        float m4PI = scatteringConstants.w; 
        kWavelengths4PI.x = invPow4Wavelengths.x * r4PI + m4PI;            
        kWavelengths4PI.y = invPow4Wavelengths.y * r4PI + m4PI;            
        kWavelengths4PI.z = invPow4Wavelengths.z * r4PI + m4PI;
                    
        float rESun = scatteringConstants.x * lightIntensity;
        invPow4WavelengthsKrESun.x = invPow4Wavelengths.x * rESun;
        invPow4WavelengthsKrESun.y = invPow4Wavelengths.y * rESun;
        invPow4WavelengthsKrESun.z = invPow4Wavelengths.z * rESun;
    }

    protected void updateSkyMaterial( Material m ) {    
        updatePackedStructures();
        
        m.setFloat("KmESun", scatteringConstants.z * lightIntensity); 
        m.setFloat("Exposure", skyExposure);
        m.setFloat("InnerRadius", innerRadius);
        m.setFloat("RadiusScale", 1 / (outerRadius - innerRadius));
        m.setFloat("Flattening", kFlatteningSky);
        m.setFloat("PlanetScale", outerRadius / skyDomeRadius); 
        m.setFloat("AverageDensityScale", averageDensityScale);
        m.setFloat("InvAverageDensityHeight", 1 / ((outerRadius - innerRadius) * averageDensityScale));
 
        float g = mpaFactor;
        float g2 = g * g;
        float phasePrefix1 = 1.5f * ((1.0f - g2) / (2.0f + g2));
        float phasePrefix2 = 1.0f + g2;
        float phasePrefix3 = 2.0f * g;  
        m.setFloat("PhasePrefix1", phasePrefix1);                
        m.setFloat("PhasePrefix2", phasePrefix2);                
        m.setFloat("PhasePrefix3", phasePrefix3);

        m.setFloat("Flattening", skyFlattening);         
    }

    public void applyGroundParameters( Material m, boolean autoUpdate ) {
        applyGroundParameters(m);
        if( autoUpdate ) {
            groundMaterials.add(m);
        }
    }
    
    public void applyGroundParameters( Material m ) {
        updatePackedStructures();
        
        // We may have never set them before
        m.setFloat("KmESun", scatteringConstants.z * lightIntensity); 
        m.setVector3("SunPosition", sunPosition);
        m.setVector3("InvWavelengthsKrESun", invPow4WavelengthsKrESun);        
        m.setVector3("KWavelengths4PI", kWavelengths4PI);
                
        m.setFloat("Exposure", groundExposure);
        m.setFloat("InnerRadius", innerRadius);
        m.setFloat("RadiusScale", 1 / (outerRadius - innerRadius));
        m.setFloat("PlanetScale", innerRadius / planetRadius); 
        m.setFloat("AverageDensityScale", averageDensityScale);
        m.setFloat("InvAverageDensityHeight", 1 / ((outerRadius - innerRadius) * averageDensityScale));
    }

    /**
     *  Sets the percentage elevation of the average atmospheric 
     *  density.  For example, 0.25 is 25% of the distance between
     *  sea level and the outer atmosphere.  This controls the
     *  density curve of the atmosphere.
     */
    public void setAverageDensityScale( float f ) {
        if( this.averageDensityScale == f ) {
            return;
        }
        this.averageDensityScale = f;
        updateMaterials();
    }
    
    public float getAverageDensityScale() {
        return averageDensityScale;
    } 

    public void setSkyFlattening( float f ) {
        this.skyFlattening = f;
        updateMaterials();
    }
    
    public float getSkyFlattening() {
        return skyFlattening;
    }

    /**
     *  Sets the radius of the sky dome in geometry units.
     *  This is not based on the real world and is only based
     *  on the actual radius of the sky dome geometry and
     *  allows the shaders to properly scale points into
     *  the internal dimensions used by the shaders.
     */
    public void setSkyDomeRadius( float f ) {
        if( this.skyDomeRadius == f ) {
            return;
        }
        this.skyDomeRadius = f;
        updateMaterials();
    }
    
    public float getSkyDomeRadius() {
        return skyDomeRadius;
    }
 
    /**
     *  Controls the scale of ground-based scattering.  Set
     *  this to the real planet radius in geometry units.
     *  For example, if 1 unit = 1 meter then for earth the
     *  radius would be: 6378100
     *  Changing this value will change how fast ground
     *  points attenuate over distance. 
     */   
    public void setPlanetRadius( float f ) {
        if( this.planetRadius == f ) {
            return;
        }
        this.planetRadius = f;
        updateMaterials();
    }
    
    public float getPlanetRadius() {
        return planetRadius;
    }

    public final void setRayleighConstant( float f ) {
        if( this.scatteringConstants.x == f ) {
            return;
        }
        this.scatteringConstants.x = f;
        this.scatteringConstants.y = f * 4 * FastMath.PI;        
        updateMaterials();
    }
        
    public float getRayleighConstant() {
        return scatteringConstants.x;
    }
    
    public final void setMieConstant( float f ) {
        if( this.scatteringConstants.z == f ) {
            return;
        }
        this.scatteringConstants.z = f;
        this.scatteringConstants.w = f * 4 * FastMath.PI;        
        updateMaterials();
    }
        
    public float getMieConstant() {
        return scatteringConstants.z;
    }
    
    public final void setMiePhaseAsymmetryFactor( float f ) {
        if( this.mpaFactor == f ) {
            return;
        }
        this.mpaFactor = f;
        updateMaterials();
    }
    
    public float getMiePhaseAsymmetryFactor() {
        return mpaFactor;
    }
 
    public void setLightDirection( Vector3f dir ) {
        sunPosition.set(-dir.x, -dir.y, -dir.z);
    }
    
    public Vector3f getLightDirection() {
        return sunPosition.negate();
    }
    
    public void setLightIntensity( float f ) {
        if( this.lightIntensity == f ) {
            return;
        }
        this.lightIntensity = f;
        updateMaterials();
    }
    
    public float getLightIntensity() {
        return lightIntensity;
    }
    
    public void setSkyExposure( float f ) {
        if( this.skyExposure == f ) {
            return;
        }
        this.skyExposure = f;
        updateMaterials();        
    }
    
    public float getSkyExposure() {
        return skyExposure;
    }

    public void setGroundExposure( float f ) {
        if( this.groundExposure == f ) {
            return;
        }
        this.groundExposure = f;
        updateMaterials();        
    }
    
    public float getGroundExposure() {
        return groundExposure;
    }
    
    public final void setWavelengths( float r, float g, float b ) {
        wavelengths.set(r, g, b);
        wavelengthsPow4.x = FastMath.pow(wavelengths.x, 4);
        wavelengthsPow4.y = FastMath.pow(wavelengths.y, 4);
        wavelengthsPow4.z = FastMath.pow(wavelengths.z, 4);
        invPow4Wavelengths.x = 1 / wavelengthsPow4.x;
        invPow4Wavelengths.y = 1 / wavelengthsPow4.y;
        invPow4Wavelengths.z = 1 / wavelengthsPow4.z;
        updateMaterials();               
    }
    
    public void setRedWavelength( float f ) {
        if( this.wavelengths.x == f ) {
            return;
        }
        setWavelengths(f, wavelengths.y, wavelengths.z); 
    }
    
    public float getRedWavelength() {
        return wavelengths.x;
    }
    
    public void setGreenWavelength( float f ) {
        if( this.wavelengths.y == f ) {
            return;
        }
        setWavelengths(wavelengths.x, f, wavelengths.z); 
    }
    
    public float getGreenWavelength() {
        return wavelengths.y;
    }
    
    public void setBlueWavelength( float f ) {
        if( this.wavelengths.z == f ) {
            return;
        }
        setWavelengths(wavelengths.x, wavelengths.y, f); 
    }
    
    public float getBlueWavelength() {
        return wavelengths.z;
    }
 
    public ColorRGBA calculateGroundColor( ColorRGBA color, Vector3f direction, float distance, float elevation, ColorRGBA target ) {
        if( target == null ) {
            target = new ColorRGBA(0, 0, 0, 1);
        }
        
        float planetScale = innerRadius / planetRadius;        
        
        Vector3f[] parms = calculateGroundInAtmosphere(direction, distance * planetScale, elevation * planetScale, null);
        
        // return (vColor + color * vColor2) * m_Exposure;
        target.r = (parms[0].x + color.r * parms[1].x) * groundExposure;
        target.g = (parms[0].y + color.g * parms[1].y) * groundExposure;
        target.b = (parms[0].z + color.b * parms[1].z) * groundExposure;
        target.a = color.a;       
        
        return target;
    }        
 
    private int nSamples = 2;
    private float fSamples = 2.0f;

    private float scale( float fCos ) {
	    float x = 1.0f - fCos;
	    return averageDensityScale * FastMath.exp(-0.00287f + x*(0.459f + x*(3.83f + x*(-6.80f + x*5.25f))));
    }
  
    public Vector3f[] calculateGroundInAtmosphere( Vector3f direction, float distance, float elevation, Vector3f[] target ) {
        if( target == null ) {
            target = new Vector3f[] { new Vector3f(), new Vector3f() };
        }
        
        float scaleDepth = averageDensityScale;
        float scaleOverScaleDepth = 1 / ((outerRadius - innerRadius) * averageDensityScale);
        
        //float innerRadius = innerRadius;
        float radiusScale = 1 / (outerRadius - innerRadius);
        Vector3f invWavelengthsKrESun = invPow4WavelengthsKrESun;
        float mESun = scatteringConstants.z * lightIntensity;
        
        Vector3f camPos = new Vector3f(0, innerRadius + elevation, 0);
        
        float rayLength = distance;
        
        // Setup to cast the ray sections for sample accumulation
        //Vector3f start = camPos;
    
        // Trying something... going to try doing the ray backwards
        Vector3f start = camPos.add(direction.mult(distance));
        direction = direction.mult(-1);
    
        float height = start.y;  // camera is always centered so y is good enough for elevation.
        float offset = innerRadius - height;
        float depth = FastMath.exp(scaleOverScaleDepth * offset);
        float startAngle = direction.dot(start) / height;
        float startOffset = depth * scale(startAngle);
    
        // Setup the loop stepping
        float sampleLength = rayLength / fSamples;
        float scaledLength = sampleLength * radiusScale;  // samppleLength * (1 / (outer - inner))
        Vector3f sampleStep = direction.mult(sampleLength);
        Vector3f samplePoint = start.add(sampleStep.mult(0.5f));  
        float scatter = 0.0f;
 
        Vector3f accumulator = new Vector3f(0.0f, 0.0f, 0.0f);
        Vector3f attenuation = new Vector3f(0.0f, 0.0f, 0.0f);
        
        for( int i = 0; i < nSamples; i++ ) {
 
            // Ground points are generally always close enough that we pretend
            // the world is flat.   
            height = samplePoint.y;                                 
            offset = innerRadius - height;
            depth = FastMath.exp(scaleOverScaleDepth * offset);
  
            float lightAngle = sunPosition.dot(samplePoint) / height;
            float cameraAngle = direction.dot(samplePoint) / height;
 
            scatter = startOffset + depth * (scale(lightAngle) - scale(cameraAngle));

            // m_InvWaveLength = 1 / (waveLength ^ 4)
            // m_KWavelengths4PI = K(wavelength) * 4 * PI
            //  = (m_InvWavelengths * r4PI + m4PI) 
            //attenuation = exp(-scatter * (m_InvWavelengths * r4PI + m4PI));
            attenuation.x = FastMath.exp(-scatter * kWavelengths4PI.x);
            attenuation.y = FastMath.exp(-scatter * kWavelengths4PI.y);
            attenuation.z = FastMath.exp(-scatter * kWavelengths4PI.z);
 
            accumulator.addLocal(attenuation.mult(depth * scaledLength));
        
            // Step the sample point to the next value
            samplePoint.addLocal(sampleStep);
        }

        // Now set the out parameters
    
        // General attenuation... we stick it in the Mie color because I'm lazy
        target[1].set(attenuation); 
    
        // Rayleigh color
        // rColor = accumulator * (invWavelengthsKrESun + mESun);
        target[0].x = accumulator.x * (invWavelengthsKrESun.x + mESun);               
        target[0].y = accumulator.y * (invWavelengthsKrESun.y + mESun);               
        target[0].z = accumulator.z * (invWavelengthsKrESun.z + mESun);               
        
        return target;
    }       
}


/*

From the shader

void calculateGroundInAtmosphere( in vec3 direction, in float distance, in float elevation, in vec3 lightPos, out vec3 rColor, out vec3 mColor ) {

    float scaleDepth = m_AverageDensityScale;  
    float scaleOverScaleDepth = m_InvAverageDensityHeight;
     
    float innerRadius = m_InnerRadius;
    float radiusScale = m_RadiusScale;
    vec3 invWavelengthsKrESun = m_InvWavelengthsKrESun;
    float mESun = m_KmESun; 
 
    // Create a camera position relative to sea level
    // From here on, positions will be relative to sea level so that
    // they properly track the curve of the planet   
    vec3 camPos = vec3(0.0, innerRadius + elevation, 0.0);  
    //vec3 lightPos = m_LightPosition;
    
    float rayLength = distance;
    
    // Setup to cast the ray sections for sample accumulation
    vec3 start = camPos;
    
    // Trying something... going to try doing the ray backwards
    start = camPos + direction * distance;
    direction *= -1.0;
    
    float height = start.y;  // camera is always centered so y is good enough for elevation.
    float offset = innerRadius - height;
    float depth = exp(scaleOverScaleDepth * offset);
    float startAngle = dot(direction, start) / height;
    float startOffset = depth * scale(startAngle);
    
    // Setup the loop stepping
    float sampleLength = rayLength / fSamples;
    float scaledLength = sampleLength * radiusScale;  // samppleLength * (1 / (outer - inner))  
    vec3 sampleStep = direction * sampleLength;
    vec3 samplePoint = start + sampleStep * 0.5; // samples are in the middle of the sample ray
    float scatter = 0.0;
 
    vec3 accumulator = vec3(0.0, 0.0, 0.0);
    vec3 attenuation = vec3(0.0, 0.0, 0.0);
    for( int i = 0; i < nSamples; i++ ) {
 
        // Ground points are generally always close enough that we pretend
        // the world is flat.   
        height = samplePoint.y;                                 
        offset = innerRadius - height;
        depth = exp(scaleOverScaleDepth * offset);
  
        float lightAngle = dot(lightPos, samplePoint) / height;
        float cameraAngle = dot(direction, samplePoint) / height;
 
        scatter = startOffset + depth * (scale(lightAngle) - scale(cameraAngle));

        // m_InvWaveLength = 1 / (waveLength ^ 4)
        // m_KWavelengths4PI = K(wavelength) * 4 * PI
        //  = (m_InvWavelengths * r4PI + m4PI) 
        //attenuation = exp(-scatter * (m_InvWavelengths * r4PI + m4PI));
        attenuation = exp(-scatter * m_KWavelengths4PI);
        
        accumulator += attenuation * (depth * scaledLength);
        
        // Step the sample point to the next value
        samplePoint += sampleStep;
    }

    // Now set the out parameters
    
    // General attenuation... we stick it in the Mie color because I'm lazy
    mColor = attenuation; 
    
    // Rayleigh color
    rColor = accumulator * (invWavelengthsKrESun + mESun);
} 

void calculateVertexGroundScattering( in vec3 worldPos, in vec3 cameraPos ) {

    vec3 direction = worldPos - cameraPos;
    direction *= m_PlanetScale;
    float distance = length(direction);

    vec3 rColor = vec3(0.0, 0.0, 0.0);
    vec3 mColor = vec3(0.0, 0.0, 0.0);
 
    calculateGroundInAtmosphere( direction, distance,  
                                 cameraPos.y * m_PlanetScale,
                                 m_SunPosition, 
                                 rColor, mColor );
    
    vColor.rgb = rColor;
    vColor.a = 1.0;
    vColor2.rgb = mColor;
    vColor2.a = 1.0;     
} 


*/
