package com.dualdex.emulator

import android.view.KeyEvent

class InputManager {

    companion object {
        const val BTN_B: Int      = 1 shl 0
        const val BTN_Y: Int      = 1 shl 1
        const val BTN_SELECT: Int = 1 shl 2
        const val BTN_START: Int  = 1 shl 3
        const val BTN_UP: Int     = 1 shl 4
        const val BTN_DOWN: Int   = 1 shl 5
        const val BTN_LEFT: Int   = 1 shl 6
        const val BTN_RIGHT: Int  = 1 shl 7
        const val BTN_A: Int      = 1 shl 8
        const val BTN_X: Int      = 1 shl 9
        const val BTN_L: Int      = 1 shl 10
        const val BTN_R: Int      = 1 shl 11
        const val BTN_L2: Int     = 1 shl 12
        const val BTN_R2: Int     = 1 shl 13
    }

    private var currentMask: Int = 0

    fun onKeyDown(keyCode: Int): Boolean {
        val mask = mapKeyCodeToMask(keyCode)
        if (mask != 0) {
            currentMask = currentMask or mask
            LibretroHost.nativeSetInputButtons(currentMask)
            return true
        }
        return false
    }

    fun onKeyUp(keyCode: Int): Boolean {
        val mask = mapKeyCodeToMask(keyCode)
        if (mask != 0) {
            currentMask = currentMask and mask.inv()
            LibretroHost.nativeSetInputButtons(currentMask)
            return true
        }
        return false
    }

    private fun mapKeyCodeToMask(keyCode: Int): Int {
        return when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_DPAD_CENTER -> BTN_A
            KeyEvent.KEYCODE_BUTTON_B -> BTN_B
            KeyEvent.KEYCODE_BUTTON_X -> BTN_X
            KeyEvent.KEYCODE_BUTTON_Y -> BTN_Y
            KeyEvent.KEYCODE_BUTTON_L1 -> BTN_L
            KeyEvent.KEYCODE_BUTTON_R1 -> BTN_R
            KeyEvent.KEYCODE_BUTTON_L2 -> BTN_L2
            KeyEvent.KEYCODE_BUTTON_R2 -> BTN_R2
            KeyEvent.KEYCODE_BUTTON_START -> BTN_START
            KeyEvent.KEYCODE_BUTTON_SELECT -> BTN_SELECT
            KeyEvent.KEYCODE_DPAD_UP -> BTN_UP
            KeyEvent.KEYCODE_DPAD_DOWN -> BTN_DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> BTN_LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> BTN_RIGHT
            else -> 0
        }
    }
}
