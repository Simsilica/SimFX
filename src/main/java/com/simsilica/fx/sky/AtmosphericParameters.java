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
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


/**
 * @author Paul Speed
 */
public class AtmosphericParameters implements Cloneable, JmeCloneable, Savable {

    public static final Material[] EMPTY_MATERIALS = new Material[0];

    /**
     * The objects to help with calculatings.
     */
    private Vector3f[] temp1;
    private Vector3f temp2;
    private Vector3f temp3;
    private Vector3f temp4;
    private Vector3f temp5;
    private Vector3f temp6;
    private Vector3f temp7;
    private Vector3f temp8;
    private Vector3f temp9;

    // This one will be common and global so we might as
    // well keep an instance around.
    private Material skyMaterial;

    // For auto-updating if the caller opts for it
    private Set<Material> groundMaterials;

    /**
     * The 'position' of the light in the skyGeometry, ie: -direction.
     */
    private Vector3f sunPosition;

    private Vector3f waveLengths;
    private Vector3f waveLengthsPow4;
    private Vector3f invPow4WaveLengths;
    private Vector3f invPow4WavelengthsKrESun;
    private Vector4f scatteringConstants;
    private Vector3f kWavelengths4PI;

    private int nSamples;

    private float lightIntensity;
    private float skyExposure;
    private float groundExposure;
    private float skyGamma;
    private float groundGamma;
    private float mpaFactor;
    private float innerRadius;
    private float outerRadius;
    private float averageDensityScale;
    private float kFlatteningSky;
    private float skyDomeRadius;
    private float planetRadius; // used for ground scale
    private float skyFlattening;
    private float fSamples;

    public AtmosphericParameters() {
        this.temp1 = new Vector3f[]{new Vector3f(), new Vector3f(), new Vector3f()};
        this.temp2 = new Vector3f();
        this.temp3 = new Vector3f();
        this.temp4 = new Vector3f();
        this.temp5 = new Vector3f();
        this.temp6 = new Vector3f();
        this.temp7 = new Vector3f();
        this.temp8 = new Vector3f();
        this.temp9 = new Vector3f();
        this.groundMaterials = new HashSet<>();
        this.sunPosition = new Vector3f();
        this.waveLengths = new Vector3f();
        this.waveLengthsPow4 = new Vector3f();
        this.invPow4WaveLengths = new Vector3f();
        this.invPow4WavelengthsKrESun = new Vector3f();
        this.scatteringConstants = new Vector4f();
        this.kWavelengths4PI = new Vector3f();

        setWaveLengths(0.650f, 0.570f, 0.475f);
        setRayleighConstant(0.0025f);
        setMieConstant(0.001f);

        this.nSamples = 2;
        this.fSamples = 2.0f;
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
        this.skyFlattening = 0;
    }

    public Material getSkyMaterial(final AssetManager assets) {
        if (skyMaterial != null) return skyMaterial;

        skyMaterial = new Material(assets, "MatDefs/SkyAtmospherics.j3md");
        skyMaterial.setVector3("SunPosition", sunPosition);
        skyMaterial.setVector3("InvWavelengthsKrESun", invPow4WavelengthsKrESun);
        skyMaterial.setVector3("KWavelengths4PI", kWavelengths4PI);

        updateSkyMaterial(skyMaterial);

        return skyMaterial;
    }

    /**
     * Update all materials.
     */
    protected void updateMaterials() {

        // Right now just the one potential
        if (skyMaterial != null) {
            updateSkyMaterial(skyMaterial);
        }

        for (final Material material : groundMaterials) {
            applyGroundParameters(material);
        }
    }

    protected void updatePackedStructures() {

        //vec3 attenuation = exp(-scatter * (m_InvWavelengths * r4PI + m4PI));
        // K(waveLengths) * 4 * PI = m_InvWavelengths * r4PI + m4PI

        float r4PI = scatteringConstants.y;
        float m4PI = scatteringConstants.w;

        kWavelengths4PI.x = invPow4WaveLengths.x * r4PI + m4PI;
        kWavelengths4PI.y = invPow4WaveLengths.y * r4PI + m4PI;
        kWavelengths4PI.z = invPow4WaveLengths.z * r4PI + m4PI;

        float rESun = scatteringConstants.x * lightIntensity;

        invPow4WavelengthsKrESun.x = invPow4WaveLengths.x * rESun;
        invPow4WavelengthsKrESun.y = invPow4WaveLengths.y * rESun;
        invPow4WavelengthsKrESun.z = invPow4WaveLengths.z * rESun;
    }

    /**
     * Update a skyGeometry material.
     *
     * @param material the skyGeometry material.
     */
    protected void updateSkyMaterial(final Material material) {
        updatePackedStructures();

        material.setFloat("KmESun", scatteringConstants.z * lightIntensity);
        material.setFloat("Exposure", skyExposure);
        material.setFloat("InnerRadius", innerRadius);
        material.setFloat("RadiusScale", 1 / (outerRadius - innerRadius));
        material.setFloat("Flattening", kFlatteningSky);
        material.setFloat("PlanetScale", outerRadius / skyDomeRadius);
        material.setFloat("AverageDensityScale", averageDensityScale);
        material.setFloat("InvAverageDensityHeight", 1 / ((outerRadius - innerRadius) * averageDensityScale));

        float g = mpaFactor;
        float g2 = g * g;
        float phasePrefix1 = 1.5f * ((1.0f - g2) / (2.0f + g2));
        float phasePrefix2 = 1.0f + g2;
        float phasePrefix3 = 2.0f * g;

        material.setFloat("PhasePrefix1", phasePrefix1);
        material.setFloat("PhasePrefix2", phasePrefix2);
        material.setFloat("PhasePrefix3", phasePrefix3);
        material.setFloat("Flattening", skyFlattening);
    }

    /**
     * Apply ground parameters to a material.
     *
     * @param material   the ground material.
     * @param autoUpdate true if need auto update.
     */
    public void applyGroundParameters(final Material material, boolean autoUpdate) {
        applyGroundParameters(material);
        if (autoUpdate) {
            groundMaterials.add(material);
        }
    }

    /**
     * Apply ground parameters to a material.
     *
     * @param material the ground material.
     */
    public void applyGroundParameters(final Material material) {
        updatePackedStructures();

        // We may have never set them before
        material.setFloat("KmESun", scatteringConstants.z * lightIntensity);
        material.setVector3("SunPosition", sunPosition);
        material.setVector3("InvWavelengthsKrESun", invPow4WavelengthsKrESun);
        material.setVector3("KWavelengths4PI", kWavelengths4PI);

        material.setFloat("Exposure", groundExposure);
        material.setFloat("InnerRadius", innerRadius);
        material.setFloat("RadiusScale", 1 / (outerRadius - innerRadius));
        material.setFloat("PlanetScale", innerRadius / planetRadius);
        material.setFloat("AverageDensityScale", averageDensityScale);
        material.setFloat("InvAverageDensityHeight", 1 / ((outerRadius - innerRadius) * averageDensityScale));
    }

    /**
     * Sets the percentage elevation of the average atmospheric density.  For example, 0.25 is 25% of the distance
     * between sea level and the outer atmosphere.  This controls the density curve of the atmosphere.
     */
    public void setAverageDensityScale(final float averageDensityScale) {
        if (getAverageDensityScale() == averageDensityScale) return;
        this.averageDensityScale = averageDensityScale;
        updateMaterials();
    }

    /**
     * @return the average density scale.
     */
    public float getAverageDensityScale() {
        return averageDensityScale;
    }

    /**
     * @param skyFlattening the skyGeometry flattening.
     */
    public void setSkyFlattening(final float skyFlattening) {
        if (getSkyFlattening() == skyFlattening) return;
        this.skyFlattening = skyFlattening;
        updateMaterials();
    }

    /**
     * @return the skyGeometry flattening.
     */
    public float getSkyFlattening() {
        return skyFlattening;
    }

    /**
     * Sets the radius of the skyGeometry dome in geometry units. This is not based on the real world and is only based
     * on the actual radius of the skyGeometry dome geometry and allows the shaders to properly scale points into the
     * internal dimensions used by the shaders.
     */
    public void setSkyDomeRadius(final float skyDomeRadius) {
        if (getSkyDomeRadius() == skyDomeRadius) return;
        this.skyDomeRadius = skyDomeRadius;
        updateMaterials();
    }

    /**
     * @return the skyGeometry dome radius.
     */
    public float getSkyDomeRadius() {
        return skyDomeRadius;
    }

    /**
     * Controls the scale of ground-based scattering.  Set this to the real planet radius in geometry units. For
     * example, if 1 unit = 1 meter then for earth the radius would be: 6378100 Changing this value will change how fast
     * ground points attenuate over distance.
     */
    public void setPlanetRadius(final float planetRadius) {
        if (getPlanetRadius() == planetRadius) return;
        this.planetRadius = planetRadius;
        updateMaterials();
    }

    /**
     * @return the planet radius.
     */
    public float getPlanetRadius() {
        return planetRadius;
    }

    /**
     * @param rayleighConstant the rayleigh constant.
     */
    public final void setRayleighConstant(final float rayleighConstant) {
        if (getRayleighConstant() == rayleighConstant) return;
        this.scatteringConstants.x = rayleighConstant;
        this.scatteringConstants.y = rayleighConstant * 4 * FastMath.PI;
        updateMaterials();
    }

    /**
     * @return the rayleigh constant.
     */
    public float getRayleighConstant() {
        return scatteringConstants.x;
    }

    /**
     * @param mieConstant the mie constant.
     */
    public final void setMieConstant(final float mieConstant) {
        if (getMieConstant() == mieConstant) return;
        this.scatteringConstants.z = mieConstant;
        this.scatteringConstants.w = mieConstant * 4 * FastMath.PI;
        updateMaterials();
    }

    /**
     * @return the mie constant.
     */
    public float getMieConstant() {
        return scatteringConstants.z;
    }

    /**
     * @param miePhaseAsymmetryFactor the mie phase asymmetry factor.
     */
    public final void setMiePhaseAsymmetryFactor(float miePhaseAsymmetryFactor) {
        if (getMiePhaseAsymmetryFactor() == miePhaseAsymmetryFactor) return;
        this.mpaFactor = miePhaseAsymmetryFactor;
        updateMaterials();
    }

    /**
     * @return the mie phase asymmetry factor.
     */
    public float getMiePhaseAsymmetryFactor() {
        return mpaFactor;
    }

    /**
     * @param lightDirection the light direction.
     */
    public void setLightDirection(final Vector3f lightDirection) {
        sunPosition.set(-lightDirection.x, -lightDirection.y, -lightDirection.z);
    }

    /**
     * @return the light direction.
     */
    public Vector3f getLightDirection() {
        return sunPosition.negate();
    }

    /**
     * @param lightIntensity the light intensity.
     */
    public void setLightIntensity(final float lightIntensity) {
        if (getLightIntensity() == lightIntensity) return;
        this.lightIntensity = lightIntensity;
        updateMaterials();
    }

    /**
     * @return the light intensity.
     */
    public float getLightIntensity() {
        return lightIntensity;
    }

    /**
     * @param skyExposure the skyGeometry exposure.
     */
    public void setSkyExposure(final float skyExposure) {
        if (getSkyExposure() == skyExposure) return;
        this.skyExposure = skyExposure;
        updateMaterials();
    }

    /**
     * @return the skyGeometry exposure.
     */
    public float getSkyExposure() {
        return skyExposure;
    }

    /**
     * @param groundExposure the ground exposure.
     */
    public void setGroundExposure(float groundExposure) {
        if (getGroundExposure() == groundExposure) return;
        this.groundExposure = groundExposure;
        updateMaterials();
    }

    /**
     * @return the ground exposure.
     */
    public float getGroundExposure() {
        return groundExposure;
    }

    /**
     * @param redWaveLength the red wave length.
     */
    public void setRedWaveLength(final float redWaveLength) {
        if (getRedWaveLength() == redWaveLength) return;
        setWaveLengths(redWaveLength, waveLengths.y, waveLengths.z);
    }

    /**
     * @param red   the red wave length.
     * @param green the green wave length.
     * @param blue  the blue wave length.
     */
    public final void setWaveLengths(final float red, final float green, final float blue) {
        waveLengths.set(red, green, blue);
        waveLengthsPow4.x = FastMath.pow(waveLengths.x, 4);
        waveLengthsPow4.y = FastMath.pow(waveLengths.y, 4);
        waveLengthsPow4.z = FastMath.pow(waveLengths.z, 4);
        invPow4WaveLengths.x = 1 / waveLengthsPow4.x;
        invPow4WaveLengths.y = 1 / waveLengthsPow4.y;
        invPow4WaveLengths.z = 1 / waveLengthsPow4.z;
        updateMaterials();
    }

    /**
     * @return the red wave length.
     */
    public float getRedWaveLength() {
        return waveLengths.x;
    }

    /**
     * @param greenWaveLength the green wave length.
     */
    public void setGreenWaveLength(final float greenWaveLength) {
        if (getGreenWaveLength() == greenWaveLength) return;
        setWaveLengths(waveLengths.x, greenWaveLength, waveLengths.z);
    }

    /**
     * @return the green wave length.
     */
    public float getGreenWaveLength() {
        return waveLengths.y;
    }

    /**
     * @param blueWaveLength the blue wave length.
     */
    public void setBlueWaveLength(final float blueWaveLength) {
        if (getBlueWaveLength() == blueWaveLength) return;
        setWaveLengths(waveLengths.x, waveLengths.y, blueWaveLength);
    }

    /**
     * @return the blue wave length.
     */
    public float getBlueWaveLength() {
        return waveLengths.z;
    }

    public ColorRGBA calculateGroundColor(final ColorRGBA color, final Vector3f direction, final float distance,
                                          final float elevation, ColorRGBA target) {

        if (target == null) {
            target = new ColorRGBA(0, 0, 0, 1);
        }

        final float planetScale = innerRadius / planetRadius;

        final Vector3f[] vector3fs = calculateGroundInAtmosphere(direction, distance * planetScale,
                elevation * planetScale, temp1);

        // return (vColor + color * vColor2) * m_Exposure;
        target.r = (vector3fs[0].x + color.r * vector3fs[1].x) * groundExposure;
        target.g = (vector3fs[0].y + color.g * vector3fs[1].y) * groundExposure;
        target.b = (vector3fs[0].z + color.b * vector3fs[1].z) * groundExposure;
        target.a = color.a;

        return target;
    }

    private float scale(final float fCos) {
        float x = 1.0f - fCos;
        return averageDensityScale * FastMath.exp(-0.00287f + x * (0.459f + x * (3.83f + x * (-6.80f + x * 5.25f))));
    }

    public Vector3f[] calculateGroundInAtmosphere(final Vector3f direction, final float distance, final float elevation,
                                                  Vector3f[] target) {

        if (target == null) {
            target = new Vector3f[]{new Vector3f(), new Vector3f()};
        }

        final Vector3f invWavelengthsKrESun = invPow4WavelengthsKrESun;

        //float scaleDepth = averageDensityScale;
        final float scaleOverScaleDepth = 1 / ((outerRadius - innerRadius) * averageDensityScale);
        //float innerRadius = innerRadius;
        final float radiusScale = 1 / (outerRadius - innerRadius);
        final float mESun = scatteringConstants.z * lightIntensity;
        final float rayLength = distance;

        final Vector3f camPos = temp4.set(0, innerRadius + elevation, 0);

        // Setup to cast the ray sections for sample accumulation
        //Vector3f start = camPos;

        // Trying something... going to try doing the ray backwards
        final Vector3f start = camPos.add(direction.mult(distance, temp2), temp3);
        final Vector3f negateDirection = direction.mult(-1, temp2);

        float height = start.y;  // camera is always centered so y is good enough for elevation.
        float offset = innerRadius - height;
        float depth = FastMath.exp(scaleOverScaleDepth * offset);
        float startAngle = negateDirection.dot(start) / height;
        float startOffset = depth * scale(startAngle);

        // Setup the loop stepping
        float sampleLength = rayLength / fSamples;
        float scaledLength = sampleLength * radiusScale;  // samppleLength * (1 / (outer - inner))

        final Vector3f sampleStep = negateDirection.mult(sampleLength, temp5);
        final Vector3f samplePoint = start.add(sampleStep.mult(0.5f, temp6), temp7);

        float scatter = 0.0f;

        final Vector3f accumulator = temp6.set(0.0f, 0.0f, 0.0f);
        final Vector3f attenuation = temp8.set(0.0f, 0.0f, 0.0f);

        for (int i = 0; i < nSamples; i++) {

            // Ground points are generally always close enough that we pretend
            // the world is flat.   
            height = samplePoint.y;
            offset = innerRadius - height;
            depth = FastMath.exp(scaleOverScaleDepth * offset);

            float lightAngle = sunPosition.dot(samplePoint) / height;
            float cameraAngle = negateDirection.dot(samplePoint) / height;

            scatter = startOffset + depth * (scale(lightAngle) - scale(cameraAngle));

            // m_InvWaveLength = 1 / (waveLength ^ 4)
            // m_KWavelengths4PI = K(wavelength) * 4 * PI
            //  = (m_InvWavelengths * r4PI + m4PI) 
            //attenuation = exp(-scatter * (m_InvWavelengths * r4PI + m4PI));
            attenuation.x = FastMath.exp(-scatter * kWavelengths4PI.x);
            attenuation.y = FastMath.exp(-scatter * kWavelengths4PI.y);
            attenuation.z = FastMath.exp(-scatter * kWavelengths4PI.z);

            accumulator.addLocal(attenuation.mult(depth * scaledLength, temp9));

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
        temp1 = new Vector3f[]{new Vector3f(), new Vector3f(), new Vector3f()};
        temp2 = new Vector3f();
        temp3 = new Vector3f();
        temp4 = new Vector3f();
        temp5 = new Vector3f();
        temp6 = new Vector3f();
        temp7 = new Vector3f();
        temp8 = new Vector3f();
        temp9 = new Vector3f();
        skyMaterial = cloner.clone(skyMaterial);
        groundMaterials = cloner.clone(groundMaterials);
        sunPosition = cloner.clone(sunPosition);
        waveLengths = cloner.clone(waveLengths);
        waveLengthsPow4 = cloner.clone(waveLengthsPow4);
        invPow4WaveLengths = cloner.clone(invPow4WaveLengths);
        invPow4WavelengthsKrESun = cloner.clone(invPow4WavelengthsKrESun);
        scatteringConstants = cloner.clone(scatteringConstants);
        kWavelengths4PI = cloner.clone(kWavelengths4PI);

        skyMaterial.setVector3("SunPosition", sunPosition);
        skyMaterial.setVector3("InvWavelengthsKrESun", invPow4WavelengthsKrESun);
        skyMaterial.setVector3("KWavelengths4PI", kWavelengths4PI);
    }

    @Override
    public void write(final JmeExporter exporter) throws IOException {
        final OutputCapsule capsule = exporter.getCapsule(this);
        capsule.write(skyMaterial, "skyMaterial", null);
        capsule.write(groundMaterials.toArray(new Material[groundMaterials.size()]), "groundMaterials", EMPTY_MATERIALS);
        capsule.write(sunPosition, "sunPosition", null);
        capsule.write(waveLengths, "waveLengths", null);
        capsule.write(waveLengthsPow4, "waveLengthsPow4", null);
        capsule.write(invPow4WaveLengths, "invPow4WaveLengths", null);
        capsule.write(invPow4WavelengthsKrESun, "invPow4WavelengthsKrESun", null);
        capsule.write(scatteringConstants, "scatteringConstants", null);
        capsule.write(kWavelengths4PI, "kWavelengths4PI", null);
        capsule.write(lightIntensity, "lightIntensity", 0);
        capsule.write(skyExposure, "skyExposure", 0);
        capsule.write(groundExposure, "groundExposure", 0);
        capsule.write(skyGamma, "skyGamma", 0);
        capsule.write(groundGamma, "groundGamma", 0);
        capsule.write(mpaFactor, "mpaFactor", 0);
        capsule.write(innerRadius, "innerRadius", 0);
        capsule.write(outerRadius, "outerRadius", 0);
        capsule.write(averageDensityScale, "averageDensityScale", 0);
        capsule.write(kFlatteningSky, "kFlatteningSky", 0);
        capsule.write(skyDomeRadius, "skyDomeRadius", 0);
        capsule.write(planetRadius, "planetRadius", 0);
        capsule.write(skyFlattening, "skyFlattening", 0);
    }

    @Override
    public void read(final JmeImporter importer) throws IOException {

        final InputCapsule capsule = importer.getCapsule(this);
        final Savable[] materials = capsule.readSavableArray("groundMaterials", EMPTY_MATERIALS);

        for (final Savable material : materials) {
            groundMaterials.add((Material) material);
        }

        skyMaterial = (Material) capsule.readSavable("skyMaterial", null);
        sunPosition = (Vector3f) capsule.readSavable("sunPosition", null);
        waveLengths = (Vector3f) capsule.readSavable("waveLengths", null);
        waveLengthsPow4 = (Vector3f) capsule.readSavable("waveLengthsPow4", null);
        invPow4WaveLengths = (Vector3f) capsule.readSavable("invPow4WaveLengths", null);
        invPow4WavelengthsKrESun = (Vector3f) capsule.readSavable("invPow4WavelengthsKrESun", null);
        scatteringConstants = (Vector4f) capsule.readSavable("scatteringConstants", null);
        kWavelengths4PI = (Vector3f) capsule.readSavable("kWavelengths4PI", null);

        lightIntensity = capsule.readFloat("lightIntensity", 0F);
        skyExposure = capsule.readFloat("skyExposure", 0F);
        groundExposure = capsule.readFloat("groundExposure", 0F);
        skyGamma = capsule.readFloat("skyGamma", 0F);
        groundGamma = capsule.readFloat("groundGamma", 0F);
        mpaFactor = capsule.readFloat("mpaFactor", 0F);
        innerRadius = capsule.readFloat("innerRadius", 0F);
        outerRadius = capsule.readFloat("outerRadius", 0F);
        averageDensityScale = capsule.readFloat("averageDensityScale", 0F);
        kFlatteningSky = capsule.readFloat("kFlatteningSky", 0F);
        skyDomeRadius = capsule.readFloat("skyDomeRadius", 0F);
        planetRadius = capsule.readFloat("planetRadius", 0F);
        skyFlattening = capsule.readFloat("skyFlattening", 0F);

        skyMaterial.setVector3("SunPosition", sunPosition);
        skyMaterial.setVector3("InvWavelengthsKrESun", invPow4WavelengthsKrESun);
        skyMaterial.setVector3("KWavelengths4PI", kWavelengths4PI);

        for (final Material material : groundMaterials) {
            material.setVector3("SunPosition", sunPosition);
            material.setVector3("InvWavelengthsKrESun", invPow4WavelengthsKrESun);
            material.setVector3("KWavelengths4PI", kWavelengths4PI);
        }
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
