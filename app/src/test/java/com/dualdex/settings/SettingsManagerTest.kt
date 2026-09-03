package com.dualdex.settings

import com.dualdex.emulator.ShaderFilter
import org.junit.Assert.*
import org.junit.Test

class SettingsManagerTest {

    @Test
    fun testShaderFilterEnumValues() {
        val filters = ShaderFilter.values()
        assertEquals(4, filters.size)

        val nearest = ShaderFilter.valueOf("NEAREST")
        assertEquals("Pixel Perfect (Nearest)", nearest.displayName)
        assertTrue(nearest.description.contains("1:1"))

        val sharp = ShaderFilter.valueOf("SHARP_BILINEAR")
        assertEquals("Sharp Bilinear", sharp.displayName)

        val lcd = ShaderFilter.valueOf("LCD_GRID")
        assertEquals("GBA LCD Grid", lcd.displayName)
        assertTrue(lcd.description.contains("LCD"))

        val crt = ShaderFilter.valueOf("CRT_SCANLINE")
        assertEquals("CRT Scanlines", crt.displayName)
        assertTrue(crt.description.contains("scanlines"))
    }
}
