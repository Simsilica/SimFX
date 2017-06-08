/*
 * $Id$
 * 
 * Copyright (c) 2016, Simsilica, LLC
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

package com.simsilica.fx.shadow;

import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.Filter;
import com.jme3.renderer.Camera;
import com.jme3.renderer.Camera.FrustumIntersect;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.GeometryComparator;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.shader.VarType;
import com.jme3.shadow.ShadowUtil;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;


/**
 * A shadow filter that simulates shadows using a simple box-shaped shadow volume that projects a shadow below the
 * object some distance.
 *
 * @author Paul Speed
 */
public class DropShadowFilter extends Filter implements JmeCloneable, Cloneable {

    private static final int VERTS_PER_SHADOW = 8; // one per box corner
    private static final int TRIS_PER_SHADOW = 12; // two per face
    private static final int INDEXES_PER_SHADOW = TRIS_PER_SHADOW * 3;

    private static final Vector3f[] BASE_CORNERS = {
            new Vector3f(-1, -1, 1),  // 0
            new Vector3f(1, -1, 1),  // 1
            new Vector3f(1, -1, -1),  // 2
            new Vector3f(-1, -1, -1),  // 3
            new Vector3f(-1, 1, 1),  // 4
            new Vector3f(1, 1, 1),  // 5
            new Vector3f(1, 1, -1),  // 6
            new Vector3f(-1, 1, -1)   // 7
    };

    private static final short[] BASE_INDEXES = {
            // top
            4, 5, 6, 4, 6, 7,
            // bottom
            3, 2, 1, 3, 1, 0,
            // +z
            0, 1, 5, 0, 5, 4,
            // -z
            2, 3, 7, 2, 7, 6,
            // -x
            3, 0, 4, 3, 4, 7,
            // +x
            1, 2, 6, 1, 6, 5
    };

    /**
     * The shadow color.
     */
    private ColorRGBA shadowColor;

    /**
     * The shadow geometry.
     */
    private Geometry shadowGeom;

    /**
     * The shadow material.
     */
    private Material shadowMaterial;

    /**
     * The shadow mesh.
     */
    private Mesh mesh;

    /**
     * The max shadows.
     */
    private int maxShadows;

    private VertexBuffer vbPos;
    private VertexBuffer vbNormal;
    private VertexBuffer vbTexCoord;
    private VertexBuffer vbTexCoord2;
    private VertexBuffer vbIndex;

    private GeometryList casters;

    /**
     * Creates a drop shadow filter that will show shadows, up to the 500 nearest shadow-casting objects.
     */
    public DropShadowFilter() {
        this(500);
    }

    /**
     * Creates a drop shadow filter that will show shadows, up to the specified number of nearest shadow-casting
     * objects.
     */
    public DropShadowFilter(int maxShadows) {
        this.shadowColor = new ColorRGBA(0, 0, 0, 0.75f);
        this.maxShadows = maxShadows;
    }

    /**
     * Get a shadow color.
     *
     * @return the shadow color.
     */
    public ColorRGBA getShadowColor() {
        return shadowColor;
    }

    /**
     * Set a shadow color.
     *
     * @param color the new shadow color.
     */
    public void setShadowColor(final ColorRGBA color) {
        shadowColor.set(color);

        if (shadowMaterial != null) {
            shadowMaterial.setParam("ShadowColor", VarType.Vector4, shadowColor);
        }
    }

    /**
     * Set a shadow intensity.
     *
     * @param shadowIntensity the shadow intensity.
     */
    public void setShadowIntensity(final float shadowIntensity) {
        shadowColor.a = shadowIntensity;

        if (shadowMaterial != null) {
            shadowMaterial.setParam("ShadowColor", VarType.Vector4, shadowColor);
        }
    }

    /**
     * Set a shadow intensity.
     *
     * @return the shadow intensity.
     */
    public float getShadowIntensity() {
        return shadowColor.a;
    }

    /**
     * Get a msx shadows count.
     *
     * @return the max shadows count.
     */
    public int getMaxShadows() {
        return maxShadows;
    }

    /**
     * Set a max shadows count.
     *
     * @param maxShadows the max shadows count.
     */
    public void setMaxShadows(final int maxShadows) {
        this.maxShadows = maxShadows;
        if (shadowMaterial != null) {
            initGeometry();
        }
    }

    @Override
    protected boolean isRequiresDepthTexture() {
        return true;
    }

    @Override
    protected void initFilter(AssetManager assets, RenderManager rm, ViewPort vp, int w, int h) {

        // Cheating... side effect of being lazy and using a filter
        // without actually needing to filter anything.
        material = new Material(assets, "MatDefs/shadow/Null.j3md");

        shadowMaterial = new Material(assets, "MatDefs/shadow/Shadows.j3md");
        shadowMaterial.setColor("ShadowColor", new ColorRGBA(0, 0, 0, 0.75f));
        shadowMaterial.getAdditionalRenderState().setDepthWrite(false);
        shadowMaterial.getAdditionalRenderState().setDepthTest(false);
        shadowMaterial.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);

        initGeometry();

        // Set our custom comparator for shadow casters
        casters = new GeometryList(new CasterComparator());
    }

    private void initGeometry() {

        mesh = new Mesh();
        // Setup the mesh for the max shadows size
        mesh.setBuffer(Type.Position, 3, BufferUtils.createVector3Buffer(maxShadows * VERTS_PER_SHADOW));
        mesh.setBuffer(Type.Normal, 3, BufferUtils.createVector3Buffer(maxShadows * VERTS_PER_SHADOW));
        mesh.setBuffer(Type.TexCoord, 3, BufferUtils.createVector3Buffer(maxShadows * VERTS_PER_SHADOW));
        mesh.setBuffer(Type.TexCoord2, 3, BufferUtils.createVector3Buffer(maxShadows * VERTS_PER_SHADOW));
        mesh.setBuffer(Type.Index, 3, BufferUtils.createShortBuffer(maxShadows * INDEXES_PER_SHADOW));

        vbPos = mesh.getBuffer(Type.Position);
        vbNormal = mesh.getBuffer(Type.Normal);
        vbTexCoord = mesh.getBuffer(Type.TexCoord);
        vbTexCoord2 = mesh.getBuffer(Type.TexCoord2);
        vbIndex = mesh.getBuffer(Type.Index);

        shadowGeom = new Geometry("shadowVolumes", mesh);
        shadowGeom.setMaterial(shadowMaterial);
        shadowGeom.setLocalTranslation(0, 100, 0);
        shadowGeom.updateLogicalState(0.1f);
        shadowGeom.updateGeometricState();
    }

    @Override
    protected void cleanUpFilter(final Renderer renderer) {
        super.cleanUpFilter(renderer);

        material = null;
        shadowMaterial = null;
        mesh = null;
        vbPos = null;
        vbNormal = null;
        vbTexCoord = null;
        vbTexCoord2 = null;
        vbIndex = null;
        shadowGeom = null;
    }

    @Override
    protected Material getMaterial() {
        return material;
    }

    @Override
    protected void postFrame(final RenderManager renderManager, final ViewPort viewPort,
                             final FrameBuffer prevFilterBuffer, final FrameBuffer sceneBuffer) {

        final RenderQueue renderQueue = viewPort.getQueue();

        for (final Spatial scene : viewPort.getScenes()) {
            ShadowUtil.getGeometriesInCamFrustum(scene, viewPort.getCamera(), ShadowMode.Cast, casters);
        }

        if (casters.size() == 0) {
            return;
        }

        final Camera cam = viewPort.getCamera();
        final BoundingSphere cullCheck = new BoundingSphere();
        final Vector3f pos = new Vector3f();

        final FrameBuffer.RenderBuffer colorBuffer = prevFilterBuffer.getColorBuffer();
        final FrameBuffer.RenderBuffer depthBuffer = prevFilterBuffer.getDepthBuffer();

        final Texture frameTex = colorBuffer.getTexture();
        final Texture depthTex = depthBuffer.getTexture();

        shadowMaterial.setTexture("FrameTexture", frameTex);

        if (frameTex != null && frameTex.getImage().getMultiSamples() > 1) {
            shadowMaterial.setInt("NumSamples", frameTex.getImage().getMultiSamples());
        } else {
            shadowMaterial.clearParam("NumSamples");
        }

        shadowMaterial.setTexture("DepthTexture", depthTex);

        if (depthTex != null && depthTex.getImage().getMultiSamples() > 1) {
            shadowMaterial.setInt("NumSamplesDepth", depthTex.getImage().getMultiSamples());
        } else {
            shadowMaterial.clearParam("NumSamplesDepth");
        }

        int size = casters.size();

        if (size > maxShadows) {
            // Give the shadows their best chance by sorting them.
            casters.setCamera(cam);
            casters.sort();
        }

        final FloatBuffer bPos = (FloatBuffer) vbPos.getData().rewind();
        final FloatBuffer bNormal = (FloatBuffer) vbNormal.getData().rewind();
        final FloatBuffer bTexCoord = (FloatBuffer) vbTexCoord.getData().rewind();
        final FloatBuffer bTexCoord2 = (FloatBuffer) vbTexCoord2.getData().rewind();
        final ShortBuffer bIndex = (ShortBuffer) vbIndex.getData().rewind();

        final Matrix4f viewMatrix = cam.getViewMatrix();
        final Matrix4f worldMatrix = new Matrix4f();
        final Matrix4f worldViewMatrix = new Matrix4f();

        float[] angles = new float[3];

        Vector3f vTemp = new Vector3f();
        Vector3f vert = new Vector3f();
        Vector3f viewDir = new Vector3f();
        Vector3f boxScale = new Vector3f();

        int rendered = 0;

        for (int i = 0; i < size; i++) {

            final Geometry geometry = casters.get(i);

            // Use the geometry bounds.  We assumg it is still y-up
            // and merely rotated.  It's a decent enough approximiation
            // in many cases and will produce better shadows for oblong
            // objects than a simple round radius would.
            final BoundingBox bounds = (BoundingBox) geometry.getModelBound();

            final float scale = geometry.getWorldScale().x;
            final float xEx = bounds.getXExtent() * scale;
            final float yEx = bounds.getYExtent() * scale;
            final float zEx = bounds.getZExtent() * scale;
            final float volumeHeight = Math.max(yEx, Math.min(xEx, zEx));

            float xOffset = bounds.getCenter().x * scale;
            float yOffset = bounds.getCenter().y * scale;
            float zOffset = bounds.getCenter().z * scale;

            yOffset -= yEx;
            yOffset -= volumeHeight * 0.5f;
            yOffset += 0.01f;

            pos.set(geometry.getWorldTranslation());
            pos.addLocal(xOffset, yOffset, zOffset);

            // A conservative approximation that works because our shadow volume
            // is really just a round blob
            final float radius = Math.max(xEx, Math.max(yEx, zEx));
            cullCheck.setCenter(pos);
            cullCheck.setRadius(radius);

            int save = cam.getPlaneState();
            cam.setPlaneState(0);
            FrustumIntersect intersect = cam.contains(cullCheck);
            cam.setPlaneState(save);

            if (intersect == FrustumIntersect.Outside) {
                continue;
            }

            boxScale.set(0.5f / xEx, 0.5f / volumeHeight, 0.5f / zEx);

            Quaternion quat = geometry.getWorldRotation();
            angles = quat.toAngles(angles);

            Quaternion rotation = new Quaternion().fromAngles(0, angles[1], 0);
            Quaternion invRotation = rotation.inverse();
            worldMatrix.setTranslation(pos);
            worldMatrix.setRotationQuaternion(rotation);

            worldViewMatrix.set(viewMatrix);
            worldViewMatrix.multLocal(worldMatrix);

            // Setup the vertexes for each corner
            for (int j = 0; j < VERTS_PER_SHADOW; j++) {
                vTemp.set(BASE_CORNERS[j].x * xEx,
                        BASE_CORNERS[j].y * volumeHeight,
                        BASE_CORNERS[j].z * zEx);

                // Get the transformed coordinate in world space
                vert = worldMatrix.mult(vTemp, vert);
                bPos.put(vert.x).put(vert.y).put(vert.z);

                // Now calculate the view direction
                vert = vert.subtractLocal(cam.getLocation());
                vert.normalizeLocal();
                viewDir = invRotation.mult(vert, viewDir);
                bNormal.put(viewDir.x).put(viewDir.y).put(viewDir.z);

                // Model space is easy to calculate
                bTexCoord.put(BASE_CORNERS[j].x * xEx + xEx);
                bTexCoord.put(BASE_CORNERS[j].y * volumeHeight + volumeHeight);
                bTexCoord.put(BASE_CORNERS[j].z * zEx + zEx);

                // And so is the scale... since it's always the same
                bTexCoord2.put(boxScale.x).put(boxScale.y).put(boxScale.z);
            }

            // Fill in the index buffer
            for (int j = 0; j < INDEXES_PER_SHADOW; j++) {
                bIndex.put((short) (BASE_INDEXES[j] + rendered * VERTS_PER_SHADOW));
            }

            rendered++;
            if (rendered >= maxShadows) {
                break;
            }
        }

        if (rendered > 0) {
            // Need to zero out the left-overs
            for (int i = rendered; i < maxShadows; i++) {
                for (int j = 0; j < INDEXES_PER_SHADOW; j++) {
                    bIndex.put((short) 0);
                }
            }

            // Update the buffers
            bPos.rewind();
            bNormal.rewind();
            bTexCoord.rewind();
            bTexCoord2.rewind();
            bIndex.rewind();

            vbPos.updateData(bPos);
            vbNormal.updateData(bNormal);
            vbTexCoord.updateData(bTexCoord);
            vbTexCoord2.updateData(bTexCoord2);
            vbIndex.updateData(bIndex);

            shadowGeom.updateGeometricState();
            renderManager.renderGeometry(shadowGeom);
        }

        casters.clear();
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
        shadowColor = cloner.clone(shadowColor);
    }

    @Override
    public void write(final JmeExporter exporter) throws IOException {
        super.write(exporter);
        final OutputCapsule capsule = exporter.getCapsule(this);
        capsule.write(shadowColor, "shadowColor", null);
        capsule.write(maxShadows, "maxShadows", 1);
    }

    @Override
    public void read(final JmeImporter importer) throws IOException {
        super.read(importer);
        final InputCapsule capsule = importer.getCapsule(this);
        shadowColor = (ColorRGBA) capsule.readSavable("shadowColor", null);
        maxShadows = capsule.readInt("maxShadows", 1);
    }

    private class CasterComparator implements GeometryComparator {

        private Camera cam;
        private final Vector3f tempVec = new Vector3f();
        private final Vector3f tempVec2 = new Vector3f();

        public void setCamera(Camera cam) {
            this.cam = cam;
        }

        public float distanceToCam(Geometry spat) {
            if (spat == null) {
                return Float.NEGATIVE_INFINITY;
            }

            if (spat.queueDistance != Float.NEGATIVE_INFINITY) {
                return spat.queueDistance;
            }

            Vector3f camPosition = cam.getLocation();
            Vector3f viewVector = cam.getDirection(tempVec2);
            Vector3f spatPosition;

            if (spat.getWorldBound() != null) {
                spatPosition = spat.getWorldBound().getCenter();
            } else {
                spatPosition = spat.getWorldTranslation();
            }

            spatPosition.subtract(camPosition, tempVec);
            spat.queueDistance = tempVec.dot(viewVector);

            return spat.queueDistance;
        }

        public int compare(Geometry o1, Geometry o2) {
            // Front to back sort
            float d1 = distanceToCam(o1);
            float d2 = distanceToCam(o2);

            if (d1 == d2) {
                return 0;
            } else if (d1 < d2) {
                return -1;
            } else {
                return 1;
            }
        }
    }
}
