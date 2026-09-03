package com.dualdex.settings

import android.content.Context
import android.content.SharedPreferences
import com.dualdex.emulator.ShaderFilter

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("dualdex_settings", Context.MODE_PRIVATE)

    var shaderFilter: ShaderFilter
        get() {
            val name = prefs.getString(KEY_SHADER_FILTER, ShaderFilter.NEAREST.name)
            return try {
                ShaderFilter.valueOf(name ?: ShaderFilter.NEAREST.name)
            } catch (e: Exception) {
                ShaderFilter.NEAREST
            }
        }
        set(value) {
            prefs.edit().putString(KEY_SHADER_FILTER, value.name).apply()
        }

    var fastForwardMultiplier: Int
        get() = prefs.getInt(KEY_FAST_FORWARD, 2)
        set(value) {
            prefs.edit().putInt(KEY_FAST_FORWARD, value).apply()
        }

    var isAudioEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUDIO_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_AUDIO_ENABLED, value).apply()
        }

    var geminiApiKey: String?
        get() = prefs.getString(KEY_GEMINI_API_KEY, null)
        set(value) {
            prefs.edit().putString(KEY_GEMINI_API_KEY, value).apply()
        }

    companion object {
        private const val KEY_SHADER_FILTER = "key_shader_filter"
        private const val KEY_FAST_FORWARD = "key_fast_forward"
        private const val KEY_AUDIO_ENABLED = "key_audio_enabled"
        private const val KEY_GEMINI_API_KEY = "key_gemini_api_key"
    }
}
