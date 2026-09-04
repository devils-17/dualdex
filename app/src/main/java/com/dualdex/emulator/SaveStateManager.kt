package com.dualdex.emulator

import android.content.Context
import android.net.Uri
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

    // ---------------------------------------------------------
    // Cartridge Battery Save (.sav) Management (My Boy! & GBA)
    // ---------------------------------------------------------

    fun getBatterySaveFilePath(gameKey: String): File {
        val cleanKey = gameKey.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(savesDir, "${cleanKey}.sav")
    }

    fun loadBatterySave(gameKey: String): Boolean {
        val file = getBatterySaveFilePath(gameKey)
        if (!file.exists() || file.length() == 0L) return false
        return LibretroHost.nativeLoadSaveRam(file.absolutePath)
    }

    fun flushBatterySave(gameKey: String): Boolean {
        val file = getBatterySaveFilePath(gameKey)
        return LibretroHost.nativeFlushSaveRam(file.absolutePath)
    }

    fun getBatterySaveInfo(gameKey: String): SaveSlotInfo {
        val file = getBatterySaveFilePath(gameKey)
        return if (file.exists() && file.length() > 0L) {
            val ts = file.lastModified()
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(ts))
            SaveSlotInfo(
                slotIndex = 0,
                exists = true,
                timestampMs = ts,
                formattedDate = dateStr,
                sizeBytes = file.length()
            )
        } else {
            SaveSlotInfo(
                slotIndex = 0,
                exists = false,
                timestampMs = 0L,
                formattedDate = "No .sav file",
                sizeBytes = 0L
            )
        }
    }

    fun importBatterySave(gameKey: String, inputStream: java.io.InputStream): Boolean {
        try {
            val rawBytes = inputStream.use { it.readBytes() }
            if (rawBytes.isEmpty()) return false

            // Standard GBA Flash 1M is 131,072 bytes (128 KB)
            // Standalone mGBA on PC adds a 16-byte RTC footer (131,088 bytes)
            val cleanBytes = if (rawBytes.size == 131088) {
                rawBytes.copyOfRange(0, 131072)
            } else {
                rawBytes
            }

            val file = getBatterySaveFilePath(gameKey)
            file.writeBytes(cleanBytes)

            // Inject into core memory and reset
            val loaded = LibretroHost.nativeLoadSaveRam(file.absolutePath)
            if (loaded) {
                LibretroHost.nativeResetCore()
            }
            return loaded
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun exportBatterySave(gameKey: String, outputStream: java.io.OutputStream): Boolean {
        try {
            // First flush any pending in-game saves from active core memory to disk
            flushBatterySave(gameKey)

            val file = getBatterySaveFilePath(gameKey)
            if (!file.exists() || file.length() == 0L) return false

            outputStream.use { out ->
                file.inputStream().use { input ->
                    input.copyTo(out)
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun importBatterySave(gameKey: String, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                importBatterySave(gameKey, stream)
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun exportBatterySave(gameKey: String, uri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                exportBatterySave(gameKey, stream)
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
