package com.dualdex.emulator

enum class ShaderFilter(val displayName: String, val description: String) {
    NEAREST("Pixel Perfect (Nearest)", "Raw 1:1 crisp GBA pixels with zero smoothing"),
    SHARP_BILINEAR("Sharp Bilinear", "Smooth pixel boundaries avoiding uneven scaling artifacts"),
    LCD_GRID("GBA LCD Grid", "Authentic Game Boy Advance TFT LCD subpixel matrix simulation"),
    CRT_SCANLINE("CRT Scanlines", "Classic retro monitor scanlines with subtle phosphors")
}
