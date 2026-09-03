package com.dualdex.emulator

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.KeyEvent
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class EmulatorSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs), GLSurfaceView.Renderer {

    private val inputManager = InputManager()
    private val pixelBuffer: ByteBuffer = ByteBuffer.allocateDirect(512 * 512 * 4).order(ByteOrder.nativeOrder())
    private val frameMetadata = IntArray(4) // width, height, pitch, pixelFormat
    private var textureId: Int = 0
    private var isTextureAllocated = false
    private var lastAllocatedWidth = 0
    private var lastAllocatedHeight = 0

    @Volatile
    private var speedMultiplier: Int = 1

    @Volatile
    private var currentFilter: ShaderFilter = ShaderFilter.NEAREST
    private var activeProgram: Int = 0

    // Shader programs map
    private val programMap = mutableMapOf<ShaderFilter, Int>()

    private val vertexShaderCode = """
        attribute vec4 aPosition;
        attribute vec2 aTexCoord;
        varying vec2 vTexCoord;
        void main() {
            gl_Position = aPosition;
            vTexCoord = aTexCoord;
        }
    """.trimIndent()

    private val nearestFragmentShader = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;
        void main() {
            gl_FragColor = texture2D(uTexture, vTexCoord);
        }
    """.trimIndent()

    private val sharpBilinearFragmentShader = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;
        uniform vec2 uTextureSize;
        void main() {
            vec2 texel = vTexCoord * uTextureSize;
            vec2 texelFloor = floor(texel);
            vec2 texelFract = fract(texel);
            // Sharp clamping factor
            vec2 sharpFract = clamp((texelFract - 0.5) * 4.0 + 0.5, 0.0, 1.0);
            vec2 coord = (texelFloor + sharpFract) / uTextureSize;
            gl_FragColor = texture2D(uTexture, coord);
        }
    """.trimIndent()

    private val lcdGridFragmentShader = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;
        uniform vec2 uTextureSize;
        void main() {
            vec4 color = texture2D(uTexture, vTexCoord);
            vec2 grid = fract(vTexCoord * uTextureSize);
            float border = 1.0;
            if (grid.x < 0.08 || grid.x > 0.92 || grid.y < 0.08 || grid.y > 0.92) {
                border = 0.82;
            }
            gl_FragColor = vec4(color.rgb * border, color.a);
        }
    """.trimIndent()

    private val crtScanlineFragmentShader = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;
        uniform vec2 uTextureSize;
        void main() {
            vec4 color = texture2D(uTexture, vTexCoord);
            float scanline = sin(vTexCoord.y * 3.14159265 * 320.0);
            scanline = 0.88 + 0.12 * scanline * scanline;
            gl_FragColor = vec4(color.rgb * scanline, color.a);
        }
    """.trimIndent()

    private val vertexBuffer: FloatBuffer
    private val texCoordBuffer: FloatBuffer

    init {
        setEGLContextClientVersion(2)
        setRenderer(this)
        renderMode = RENDERMODE_CONTINUOUSLY
        isFocusable = true
        isFocusableInTouchMode = true

        // 3:2 aspect ratio scaling for AYN Thor 16:9 top display (1920x1080)
        val aspectScaleX = 0.84375f
        val quadVertices = floatArrayOf(
            -aspectScaleX, -1.0f,
             aspectScaleX, -1.0f,
            -aspectScaleX,  1.0f,
             aspectScaleX,  1.0f
        )
        vertexBuffer = ByteBuffer.allocateDirect(quadVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(quadVertices)
                position(0)
            }

        val texCoords = floatArrayOf(
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f
        )
        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(texCoords)
                position(0)
            }
    }

    fun setShaderFilter(filter: ShaderFilter) {
        currentFilter = filter
    }

    fun setSpeedMultiplier(multiplier: Int) {
        speedMultiplier = when {
            multiplier in 1..8 -> multiplier
            multiplier > 8 -> 8
            else -> 1
        }
    }

    fun getSpeedMultiplier(): Int = speedMultiplier

    fun toggleFastForward() {
        speedMultiplier = if (speedMultiplier == 1) 2 else if (speedMultiplier == 2) 4 else 1
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // Compile all filter shader programs
        programMap[ShaderFilter.NEAREST] = createProgram(vertexShaderCode, nearestFragmentShader)
        programMap[ShaderFilter.SHARP_BILINEAR] = createProgram(vertexShaderCode, sharpBilinearFragmentShader)
        programMap[ShaderFilter.LCD_GRID] = createProgram(vertexShaderCode, lcdGridFragmentShader)
        programMap[ShaderFilter.CRT_SCANLINE] = createProgram(vertexShaderCode, crtScanlineFragmentShader)

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        isTextureAllocated = false
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // Fast-Forward emulation steps
        val steps = speedMultiplier
        for (i in 0 until steps) {
            LibretroHost.nativeStepFrame()
        }

        // Fetch latest frame pixels
        pixelBuffer.position(0)
        val hasFrame = LibretroHost.nativeGetVideoFrame(pixelBuffer, frameMetadata)

        if (hasFrame && frameMetadata[0] > 0 && frameMetadata[1] > 0) {
            val width = frameMetadata[0]
            val height = frameMetadata[1]
            val pixelFormat = frameMetadata[3] // 2 = RGB565, 1 = XRGB8888

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            pixelBuffer.position(0)

            val glFormat = if (pixelFormat == 2) GLES20.GL_RGB else GLES20.GL_RGBA
            val glType = if (pixelFormat == 2) GLES20.GL_UNSIGNED_SHORT_5_6_5 else GLES20.GL_UNSIGNED_BYTE

            if (!isTextureAllocated || lastAllocatedWidth != width || lastAllocatedHeight != height) {
                // Initial texture allocation
                GLES20.glTexImage2D(
                    GLES20.GL_TEXTURE_2D, 0, glFormat,
                    width, height, 0,
                    glFormat, glType, pixelBuffer
                )
                isTextureAllocated = true
                lastAllocatedWidth = width
                lastAllocatedHeight = height
            } else {
                // High-performance zero-reallocation subimage update
                GLES20.glTexSubImage2D(
                    GLES20.GL_TEXTURE_2D, 0, 0, 0,
                    width, height, glFormat, glType, pixelBuffer
                )
            }
        }

        // Render with active shader filter
        activeProgram = programMap[currentFilter] ?: programMap[ShaderFilter.NEAREST]!!
        GLES20.glUseProgram(activeProgram)

        val posHandle = GLES20.glGetAttribLocation(activeProgram, "aPosition")
        val texCoordHandle = GLES20.glGetAttribLocation(activeProgram, "aTexCoord")
        val texHandle = GLES20.glGetUniformLocation(activeProgram, "uTexture")
        val sizeHandle = GLES20.glGetUniformLocation(activeProgram, "uTextureSize")

        if (sizeHandle != -1 && frameMetadata[0] > 0 && frameMetadata[1] > 0) {
            GLES20.glUniform2f(sizeHandle, frameMetadata[0].toFloat(), frameMetadata[1].toFloat())
        }

        GLES20.glEnableVertexAttribArray(posHandle)
        GLES20.glVertexAttribPointer(posHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(texHandle, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(posHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (inputManager.onKeyDown(keyCode)) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (inputManager.onKeyUp(keyCode)) return true
        return super.onKeyUp(keyCode, event)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }

    private fun createProgram(vertexCode: String, fragmentCode: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentCode)

        return GLES20.glCreateProgram().also { program ->
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)
        }
    }
}
