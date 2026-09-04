package com.dualdex.cheats

import android.content.Context
import android.util.Log
import com.dualdex.emulator.LibretroHost
import org.json.JSONArray
import org.json.JSONObject

class CheatManager(context: Context) {
    private val prefs = context.getSharedPreferences("dualdex_cheats", Context.MODE_PRIVATE)

    fun getCheats(gameKey: String): List<CheatItem> {
        val safeKey = cleanGameKey(gameKey)
        val jsonStr = prefs.getString("cheats_$safeKey", null)
        if (jsonStr == null) {
            // Seed with useful presets for this game
            val defaultPresets = getPresetsForGame(gameKey)
            if (defaultPresets.isNotEmpty()) {
                saveCheats(gameKey, defaultPresets)
                return defaultPresets
            }
            return emptyList()
        }
        return try {
            val list = ArrayList<CheatItem>()
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CheatItem(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        code = obj.optString("code"),
                        enabled = obj.optBoolean("enabled", false),
                        isPreset = obj.optBoolean("isPreset", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e("DualDex_CheatManager", "Error parsing cheats for $safeKey", e)
            emptyList()
        }
    }

    fun saveCheats(gameKey: String, cheats: List<CheatItem>) {
        val safeKey = cleanGameKey(gameKey)
        try {
            val arr = JSONArray()
            for (c in cheats) {
                val obj = JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("code", c.code)
                    put("enabled", c.enabled)
                    put("isPreset", c.isPreset)
                }
                arr.put(obj)
            }
            prefs.edit().putString("cheats_$safeKey", arr.toString()).apply()
        } catch (e: Exception) {
            Log.e("DualDex_CheatManager", "Error saving cheats for $safeKey", e)
        }
    }

    fun addCheat(gameKey: String, cheat: CheatItem) {
        val current = getCheats(gameKey).toMutableList()
        current.add(cheat)
        saveCheats(gameKey, current)
        applyCheats(gameKey)
    }

    fun updateCheat(gameKey: String, updated: CheatItem) {
        val current = getCheats(gameKey).toMutableList()
        val idx = current.indexOfFirst { it.id == updated.id }
        if (idx != -1) {
            current[idx] = updated
            saveCheats(gameKey, current)
            applyCheats(gameKey)
        }
    }

    fun deleteCheat(gameKey: String, cheatId: String) {
        val current = getCheats(gameKey).filter { it.id != cheatId }
        saveCheats(gameKey, current)
        applyCheats(gameKey)
    }

    fun toggleCheat(gameKey: String, cheatId: String, enabled: Boolean) {
        val current = getCheats(gameKey).toMutableList()
        val idx = current.indexOfFirst { it.id == cheatId }
        if (idx != -1) {
            current[idx] = current[idx].copy(enabled = enabled)
            saveCheats(gameKey, current)
            applyCheats(gameKey)
        }
    }

    fun resetToDefaultPresets(gameKey: String): List<CheatItem> {
        val presets = getPresetsForGame(gameKey)
        saveCheats(gameKey, presets)
        applyCheats(gameKey)
        return presets
    }

    fun applyCheats(gameKey: String) {
        try {
            LibretroHost.nativeCheatReset()
            val cheats = getCheats(gameKey)
            var activeIdx = 0
            for (c in cheats) {
                if (!c.enabled) continue
                val cleanLines = c.code.lines()
                    .map { it.trim() }
                    .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("//") }
                if (cleanLines.isNotEmpty()) {
                    val codePayload = cleanLines.joinToString("\n")
                    Log.i("DualDex_CheatManager", "Applying cheat #${activeIdx}: '${c.name}' (${cleanLines.size} lines)")
                    LibretroHost.nativeCheatSet(activeIdx, true, codePayload)
                    activeIdx++
                }
            }
            Log.i("DualDex_CheatManager", "Total active cheats applied: $activeIdx")
        } catch (e: Throwable) {
            Log.e("DualDex_CheatManager", "Error applying cheats: ${e.message}", e)
        }
    }

    private fun cleanGameKey(key: String): String {
        return key.trim().lowercase().replace("[^a-z0-9_]+".toRegex(), "_")
    }

    fun getPresetsForGame(gameKey: String): List<CheatItem> {
        val lower = gameKey.lowercase()
        val isEmeraldOrHnS = lower.contains("heart") || lower.contains("soul") || lower.contains("emer")
        val isFireRed = lower.contains("fire") || lower.contains("leaf")

        val presets = mutableListOf<CheatItem>()

        if (isEmeraldOrHnS) {
            // Heart and Soul 2.0 / Emerald Action Replay & CodeBreaker codes
            presets.add(
                CheatItem(
                    name = "Heart & Soul: Master Code (Must Enable for AR)",
                    code = "D8BAE4D9 4864DCE5\nB3C94DA9 C04D368C",
                    enabled = false,
                    isPreset = true
                )
            )
            presets.add(
                CheatItem(
                    name = "Max Money (CodeBreaker)",
                    code = "82003884 0F42\n82003886 003F",
                    enabled = false,
                    isPreset = true
                )
            )
            presets.add(
                CheatItem(
                    name = "Rare Candies in PC Item Storage (CodeBreaker)",
                    code = "82003884 002C\n820257C4 002C",
                    enabled = false,
                    isPreset = true
                )
            )
            presets.add(
                CheatItem(
                    name = "Master Balls in PC Item Storage (CodeBreaker)",
                    code = "82003884 0001\n820257C4 0001",
                    enabled = false,
                    isPreset = true
                )
            )
            presets.add(
                CheatItem(
                    name = "100% Catch Rate (Action Replay)",
                    code = "87ACF046 F75DF7BD",
                    enabled = false,
                    isPreset = true
                )
            )
            presets.add(
                CheatItem(
                    name = "Walk Through Walls (Ghost Mode)",
                    code = "7881A409 E2026E0C\n8E883EFF 92E9660D",
                    enabled = false,
                    isPreset = true
                )
            )
            presets.add(
                CheatItem(
                    name = "Unlimited PP for All Moves (CodeBreaker)",
                    code = "42024AA4 FFFF\n00000002 0002",
                    enabled = false,
                    isPreset = true
                )
            )
        } else if (isFireRed) {
            presets.add(
                CheatItem(
                    name = "FireRed: Master Code (Must Enable for AR)",
                    code = "000014D1 000A\n1003DAE6 0007",
                    enabled = false,
                    isPreset = true
                )
            )
            presets.add(
                CheatItem(
                    name = "Max Money (CodeBreaker)",
                    code = "82003884 0F42\n82003886 003F",
                    enabled = false,
                    isPreset = true
                )
            )
            presets.add(
                CheatItem(
                    name = "Rare Candies in PC Item Storage",
                    code = "82025840 0044",
                    enabled = false,
                    isPreset = true
                )
            )
            presets.add(
                CheatItem(
                    name = "Master Balls in PC Item Storage",
                    code = "82025840 0001",
                    enabled = false,
                    isPreset = true
                )
            )
            presets.add(
                CheatItem(
                    name = "Walk Through Walls (Ghost Mode)",
                    code = "509197D3 542975F4\n78DA625D 6FA79E13",
                    enabled = false,
                    isPreset = true
                )
            )
        } else {
            presets.add(
                CheatItem(
                    name = "Example Action Replay Code",
                    code = "XXXXXXXX XXXXXXXX",
                    enabled = false,
                    isPreset = true
                )
            )
        }
        return presets
    }
}
