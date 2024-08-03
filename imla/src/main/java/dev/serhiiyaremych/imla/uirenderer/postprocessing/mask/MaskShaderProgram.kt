/*
 * Copyright 2024, Serhii Yaremych
 * SPDX-License-Identifier: MIT
 */

package dev.serhiiyaremych.imla.uirenderer.postprocessing.mask

import dev.serhiiyaremych.imla.renderer.Shader
import dev.serhiiyaremych.imla.renderer.SimpleRenderer
import dev.serhiiyaremych.imla.renderer.Texture
import dev.serhiiyaremych.imla.renderer.Texture2D
import dev.serhiiyaremych.imla.uirenderer.shaderSources.SIMPLE_MASK_FRAG
import dev.serhiiyaremych.imla.uirenderer.shaderSources.SIMPLE_QUAD_VERT

internal class MaskShaderProgram {
    val shader: Shader = Shader.create(
        name = "simple_quad",
        vertexSrc = SIMPLE_QUAD_VERT,
        fragmentSrc = SIMPLE_MASK_FRAG
    ).apply {
        bindUniformBlock(
            SimpleRenderer.TEXTURE_DATA_UBO_BLOCK,
            SimpleRenderer.TEXTURE_DATA_UBO_BINDING_POINT
        )
    }

    fun setMask(mask: Texture2D) {
        shader.bind()
        mask.bind(2)
        shader.setInt("u_Mask", 2)
    }

    fun setBackground(background: Texture) {
        shader.bind()
        background.bind(3)
        shader.setInt("u_Background", 3)
    }

    fun destroy() {
        shader.destroy()
    }
}