/*
 * Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.core.examples.kotlin.common.rendering

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import com.google.ar.core.PointCloud
import java.io.IOException

/** Renders a point cloud.  */
class PointCloudRenderer {
    private var vbo = 0
    private var vboSize = 0
    private var programName = 0
    private var positionAttribute = 0
    private var modelViewProjectionUniform = 0
    private var colorUniform = 0
    private var pointSizeUniform = 0
    private var numPoints = 0

    // Keep track of the last point cloud rendered to avoid updating the VBO if point cloud
    // was not changed.  Do this using the timestamp since we can't compare PointCloud objects.
    private var lastTimestamp: Long = 0

    /**
     * Allocates and initializes OpenGL resources needed by the plane renderer. Must be called on the
     * OpenGL thread, typically in [GLSurfaceView.Renderer.onSurfaceCreated].
     *
     * @param context Needed to access shader source.
     */
    @Throws(IOException::class)
    fun createOnGlThread(context: Context?) {
        ShaderUtil.checkGLError(TAG, "before create")
        val buffers = IntArray(1)
        GLES20.glGenBuffers(1, buffers, 0)
        vbo = buffers[0]
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
        vboSize = INITIAL_BUFFER_POINTS * BYTES_PER_POINT
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vboSize, null, GLES20.GL_DYNAMIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        ShaderUtil.checkGLError(TAG, "buffer alloc")
        val vertexShader = ShaderUtil.loadGLShader(TAG, context!!, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_NAME)
        val passthroughShader = ShaderUtil.loadGLShader(TAG, context, GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_NAME)
        programName = GLES20.glCreateProgram()
        GLES20.glAttachShader(programName, vertexShader)
        GLES20.glAttachShader(programName, passthroughShader)
        GLES20.glLinkProgram(programName)
        GLES20.glUseProgram(programName)
        ShaderUtil.checkGLError(TAG, "program")
        positionAttribute = GLES20.glGetAttribLocation(programName, "a_Position")
        colorUniform = GLES20.glGetUniformLocation(programName, "u_Color")
        modelViewProjectionUniform = GLES20.glGetUniformLocation(programName, "u_ModelViewProjection")
        pointSizeUniform = GLES20.glGetUniformLocation(programName, "u_PointSize")
        ShaderUtil.checkGLError(TAG, "program  params")
    }

    /**
     * Updates the OpenGL buffer contents to the provided point. Repeated calls with the same point
     * cloud will be ignored.
     */
    fun update(cloud: PointCloud) {
        if (cloud.timestamp == lastTimestamp) {
            // Redundant call.
            return
        }
        ShaderUtil.checkGLError(TAG, "before update")
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
        lastTimestamp = cloud.timestamp

        // If the VBO is not large enough to fit the new point cloud, resize it.
        numPoints = cloud.points.remaining() / FLOATS_PER_POINT
        if (numPoints * BYTES_PER_POINT > vboSize) {
            while (numPoints * BYTES_PER_POINT > vboSize) {
                vboSize *= 2
            }
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vboSize, null, GLES20.GL_DYNAMIC_DRAW)
        }
        GLES20.glBufferSubData(
                GLES20.GL_ARRAY_BUFFER, 0, numPoints * BYTES_PER_POINT, cloud.points)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        ShaderUtil.checkGLError(TAG, "after update")
    }

    /**
     * Renders the point cloud. ARCore point cloud is given in world space.
     *
     * @param cameraView the camera view matrix for this frame, typically from [     ][com.google.ar.core.Camera.getViewMatrix].
     * @param cameraPerspective the camera projection matrix for this frame, typically from [     ][com.google.ar.core.Camera.getProjectionMatrix].
     */
    fun draw(cameraView: FloatArray?, cameraPerspective: FloatArray?) {
        val modelViewProjection = FloatArray(16)
        Matrix.multiplyMM(modelViewProjection, 0, cameraPerspective, 0, cameraView, 0)
        ShaderUtil.checkGLError(TAG, "Before draw")
        GLES20.glUseProgram(programName)
        GLES20.glEnableVertexAttribArray(positionAttribute)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
        GLES20.glVertexAttribPointer(positionAttribute, 4, GLES20.GL_FLOAT, false, BYTES_PER_POINT, 0)
        GLES20.glUniform4f(colorUniform, 31.0f / 255.0f, 188.0f / 255.0f, 210.0f / 255.0f, 1.0f)
        GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, modelViewProjection, 0)
        GLES20.glUniform1f(pointSizeUniform, 5.0f)
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, numPoints)
        GLES20.glDisableVertexAttribArray(positionAttribute)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        ShaderUtil.checkGLError(TAG, "Draw")
    }

    companion object {
        private val TAG = PointCloud::class.java.simpleName

        // Shader names.
        private const val VERTEX_SHADER_NAME = "shaders/point_cloud.vert"
        private const val FRAGMENT_SHADER_NAME = "shaders/point_cloud.frag"
        private const val BYTES_PER_FLOAT = java.lang.Float.SIZE / 8
        private const val FLOATS_PER_POINT = 4 // X,Y,Z,confidence.
        private const val BYTES_PER_POINT = BYTES_PER_FLOAT * FLOATS_PER_POINT
        private const val INITIAL_BUFFER_POINTS = 1000
    }
}