/*
 * Copyright 2024, Serhii Yaremych
 * SPDX-License-Identifier: MIT
 */

package dev.serhiiyaremych.imla.uirenderer.postprocessing.noise

import dev.serhiiyaremych.imla.renderer.BufferLayout
import dev.serhiiyaremych.imla.renderer.Shader
import dev.serhiiyaremych.imla.renderer.ShaderProgram
import dev.serhiiyaremych.imla.renderer.objects.defaultQuadBufferLayout
import dev.serhiiyaremych.imla.renderer.objects.defaultQuadVertexMapper
import dev.serhiiyaremych.imla.renderer.primitive.QuadVertex
import dev.serhiiyaremych.imla.uirenderer.shaderSources.DEFAULT_QUAD_VERT
import dev.serhiiyaremych.imla.uirenderer.shaderSources.NOISE_FRAG

internal class NoiseShaderProgram : ShaderProgram {
    override val shader: Shader = Shader.create(
        name = "default_quad",
        vertexSrc = DEFAULT_QUAD_VERT,
        fragmentSrc = NOISE_FRAG
    )
    override val vertexBufferLayout: BufferLayout = defaultQuadBufferLayout
    override val componentsCount: Int = vertexBufferLayout.elements.sumOf { it.type.components }

    override fun mapVertexData(quadVertexBufferBase: List<QuadVertex>) =
        defaultQuadVertexMapper(quadVertexBufferBase)

    override fun destroy() {
        shader.destroy()
    }
}