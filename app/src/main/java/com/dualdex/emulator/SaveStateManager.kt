package com.dualdex.emulator

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SaveSlotInfo(
    val slotIndex: Int,
    val exists: Boolean,
    val timestampMs: Long,
    val formattedDate: String,
    val sizeBytes: Long
)

class SaveStateManager(private val context: Context) {

    private val savesDir: File
        get() = File(context.filesDir, "saves").apply {
            if (!exists()) mkdirs()
        }

    fun getSaveFilePath(gameKey: String, slotIndex: Int): File {
        val cleanKey = gameKey.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(savesDir, "${cleanKey}_slot_${slotIndex}.state")
    }

    fun getQuickSaveFilePath(gameKey: String): File {
        val cleanKey = gameKey.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(savesDir, "${cleanKey}_quicksave.state")
    }

    fun saveSlot(gameKey: String, slotIndex: Int): Boolean {
        val file = getSaveFilePath(gameKey, slotIndex)
        return LibretroHost.nativeSaveState(file.absolutePath)
    }

    fun loadSlot(gameKey: String, slotIndex: Int): Boolean {
        val file = getSaveFilePath(gameKey, slotIndex)
        if (!file.exists()) return false
        return LibretroHost.nativeLoadState(file.absolutePath)
    }

    fun quickSave(gameKey: String): Boolean {
        val file = getQuickSaveFilePath(gameKey)
        return LibretroHost.nativeSaveState(file.absolutePath)
    }

    fun quickLoad(gameKey: String): Boolean {
        val file = getQuickSaveFilePath(gameKey)
        if (!file.exists()) return false
        return LibretroHost.nativeLoadState(file.absolutePath)
    }

    fun getSlotInfo(gameKey: String, slotIndex: Int): SaveSlotInfo {
        val file = getSaveFilePath(gameKey, slotIndex)
        return if (file.exists()) {
            val ts = file.lastModified()
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(ts))
            SaveSlotInfo(
                slotIndex = slotIndex,
                exists = true,
                timestampMs = ts,
                formattedDate = dateStr,
                sizeBytes = file.length()
            )
        } else {
            SaveSlotInfo(
                slotIndex = slotIndex,
                exists = false,
                timestampMs = 0L,
                formattedDate = "Empty",
                sizeBytes = 0L
            )
        }
    }

    fun getAllSlotsInfo(gameKey: String, maxSlots: Int = 5): List<SaveSlotInfo> {
        return (1..maxSlots).map { getSlotInfo(gameKey, it) }
    }
}
