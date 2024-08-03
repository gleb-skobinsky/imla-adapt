/*
 * Copyright 2024, Serhii Yaremych
 * SPDX-License-Identifier: MIT
 */

package dev.serhiiyaremych.imla.uirenderer.postprocessing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.toSize
import androidx.tracing.trace
import dev.serhiiyaremych.imla.renderer.Shader
import dev.serhiiyaremych.imla.renderer.SimpleRenderer
import dev.serhiiyaremych.imla.renderer.SubTexture2D
import dev.serhiiyaremych.imla.renderer.Texture
import dev.serhiiyaremych.imla.renderer.Texture2D
import dev.serhiiyaremych.imla.renderer.VertexBuffer
import dev.serhiiyaremych.imla.renderer.toFloatBuffer
import dev.serhiiyaremych.imla.uirenderer.shaderSources.SIMPLE_QUAD_FRAG
import dev.serhiiyaremych.imla.uirenderer.shaderSources.SIMPLE_QUAD_VERT
import java.nio.FloatBuffer

internal class SimpleQuadRenderer(
    val renderer: SimpleRenderer
) {
    private var vbo: VertexBuffer? = null
    private val simpleQuadShader by lazy(LazyThreadSafetyMode.NONE) {
        Shader.create(
            name = "simple_quad",
            vertexSrc = SIMPLE_QUAD_VERT,
            fragmentSrc = SIMPLE_QUAD_FRAG
        ).apply {
            bindUniformBlock(
                SimpleRenderer.TEXTURE_DATA_UBO_BLOCK,
                SimpleRenderer.TEXTURE_DATA_UBO_BINDING_POINT
            )
        }
    }
    private val simpleDataCache: FloatBuffer by lazy(LazyThreadSafetyMode.NONE) {
        FloatArray(renderer.data.textureDataUBO.elements).toFloatBuffer()
    }

    private var texCoord: Array<Offset> = defaultTextureCoords
    private var texSize: Size = Size.Unspecified
    private var flipY: Boolean = false
    private var alpha: Float = 1.0f

    fun draw(shader: Shader = simpleQuadShader, texture: Texture? = null, alpha: Float = 1.0f) =
        trace("SimpleQuadRenderer#draw") {
            renderer.data.vao.bind()
            vbo?.bind()
            shader.bind()
            if (texture != null) {
                texture.bind()
                uploadTextureDataIfNeed(alpha, texture)
            }
            renderer.flush()
        }

    private fun uploadTextureDataIfNeed(alpha: Float, texture: Texture) {
        val texCoord: Array<Offset> = getTextureCoordinates(texture)
        val flipY: Boolean = texture.flipTexture
        val texCoordinatesChanged = isTexCoordinatesChanged(texCoord)
        val flipChanged = isFlipChanged(flipY)
        val alphaChanged = isAlphaChanged(alpha)
        val isDataChanged =
            texCoordinatesChanged || flipChanged || alphaChanged

        if (isDataChanged) {
            trace("uploadDataIfNeed") {
                simpleDataCache.rewind()

                // Bottom Left
                simpleDataCache.put(texCoord[0].x)
                simpleDataCache.put(texCoord[0].y)
                simpleDataCache.put(0.0f)       // padding
                simpleDataCache.put(0.0f)       // padding
                // Bottom Right
                simpleDataCache.put(texCoord[1].x)
                simpleDataCache.put(texCoord[1].y)
                simpleDataCache.put(0.0f)       // padding
                simpleDataCache.put(0.0f)       // padding
                // Top Right
                simpleDataCache.put(texCoord[2].x)
                simpleDataCache.put(texCoord[2].y)
                simpleDataCache.put(0.0f)       // padding
                simpleDataCache.put(0.0f)       // padding
                // Top Left
                simpleDataCache.put(texCoord[3].x)
                simpleDataCache.put(texCoord[3].y)
                simpleDataCache.put(0.0f)       // padding
                simpleDataCache.put(0.0f)       // padding
                // Flip Y & Alpha
                simpleDataCache.put(if (flipY) 1.0f else 0.0f)
                simpleDataCache.put(alpha)
                simpleDataCache.put(0.0f)       // padding
                simpleDataCache.put(0.0f)       // padding

                renderer.data.textureDataUBO.setData(simpleDataCache.position(0))

                this.texCoord = texCoord
                this.flipY = flipY
                this.alpha = alpha
            }
        }
    }

    private fun isAlphaChanged(alpha: Float): Boolean {
        return this.alpha != alpha
    }

    private fun isFlipChanged(flipY: Boolean): Boolean {
        return this.flipY != flipY
    }

    private fun isTexCoordinatesChanged(texCoord: Array<Offset>): Boolean {
        return this.texCoord[0] != texCoord[0] || this.texCoord[1] != texCoord[1] || this.texCoord[2] != texCoord[2] || this.texCoord[3] != texCoord[3]
    }

    private fun getTextureCoordinates(texture: Texture): Array<Offset> {
        return when (texture) {
            is Texture2D -> defaultTextureCoords
            is SubTexture2D -> texture.texCoords
            else -> defaultTextureCoords
        }
    }

    private fun getTextureSize(texture: Texture): Size {
        return when (texture) {
            is Texture2D -> Size(texture.width.toFloat(), texture.height.toFloat())
            is SubTexture2D -> texture.subTextureSize.toSize()
            else -> Size.Unspecified
        }
    }

    companion object {
        private val bottomLeft = Offset(0.0f, 0.0f)
        private val bottomRight = Offset(1.0f, 0.0f)
        private val topRight = Offset(1.0f, 1.0f)
        private val topLeft = Offset(0.0f, 1.0f)
        val defaultTextureCoords = arrayOf(bottomLeft, bottomRight, topRight, topLeft) // CCW
    }
}