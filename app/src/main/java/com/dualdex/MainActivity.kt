package com.dualdex

import android.content.Context
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.dualdex.calculator.DamageCalculator
import com.dualdex.companion.CompanionPresentation
import com.dualdex.companion.CompanionViewModel
import com.dualdex.companion.ui.CompanionScreenView
import com.dualdex.emulator.AudioDriver
import com.dualdex.emulator.EmulatorSurfaceView
import com.dualdex.emulator.LibretroHost
import com.dualdex.emulator.SaveStateManager
import com.dualdex.romhack.ProfileLoader
import com.dualdex.romhack.RomHackDetector
import com.dualdex.romhack.RomHackProfile
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity(), DisplayManager.DisplayListener {

    private val viewModel = CompanionViewModel()
    private var emulatorView: EmulatorSurfaceView? = null
    private var companionPresentation: CompanionPresentation? = null
    private var displayManager: DisplayManager? = null
    private var loadedProfiles: List<RomHackProfile> = emptyList()
    private val audioDriver = AudioDriver(32768)

    private val openRomLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            handleSelectedRom(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize QuickJS damage calculator engine
        DamageCalculator.initialize(this)

        // 2. Load ROM Hack JSON profiles from assets
        loadedProfiles = ProfileLoader.loadProfilesFromAssets(this)
        Log.i("DualDex", "Loaded ${loadedProfiles.size} ROM hack profiles.")

        // 3. Initialize mGBA Libretro core
        val corePath = "${applicationInfo.nativeLibraryDir}/mgba_libretro.so"
        if (File(corePath).exists()) {
            val loaded = LibretroHost.nativeLoadCore(corePath)
            Log.i("DualDex", "Loaded mGBA Libretro core: $loaded (path=$corePath)")
            if (loaded) {
                audioDriver.start()
            }
        } else {
            Log.w("DualDex", "Core file not found at: $corePath")
        }

        // 4. Register DisplayManager listener for AYN Thor secondary display
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        displayManager?.registerDisplayListener(this, null)

        // 5. Setup display UI
        setupDisplays()

        // 6. Start background memory poller (10Hz)
        viewModel.startPolling(100L)
    }

    private fun handleSelectedRom(uri: Uri) {
        try {
            // Copy URI stream to a local cache file for Libretro dlopen/fopen access
            val romsDir = File(filesDir, "roms").apply { if (!exists()) mkdirs() }
            val localRomFile = File(romsDir, "current_game.gba")

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(localRomFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (!localRomFile.exists() || localRomFile.length() == 0L) {
                Toast.makeText(this, "Failed to read ROM file", Toast.LENGTH_SHORT).show()
                return
            }

            // Detect ROM Hack Profile via SHA-256 and header title
            val profile = RomHackDetector.detectProfile(localRomFile, loadedProfiles)
            viewModel.setProfile(profile)
            Log.i("DualDex", "Detected ROM Hack Profile: ${profile.name} (Engine: ${profile.engine}, GameId: ${profile.gameId})")

            // Load ROM into mGBA core
            val ok = LibretroHost.nativeLoadRom(localRomFile.absolutePath)
            if (ok) {
                Toast.makeText(this, "Loaded: ${profile.name} (${profile.engine})", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Failed to load ROM in mGBA core", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error opening ROM: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDisplays() {
        val dm = displayManager ?: return
        val presentationDisplays = dm.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)

        if (presentationDisplays.isNotEmpty()) {
            // Dual-screen mode (AYN Thor detected)
            Log.i("DualDex", "AYN Thor dual-screen detected. Attaching CompanionPresentation to Display 1.")

            emulatorView = EmulatorSurfaceView(this).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            setContentView(emulatorView)

            showPresentation(presentationDisplays[0])
        } else {
            // Single-screen fallback mode (vertical split)
            Log.i("DualDex", "Single display detected. Running in split-screen fallback mode.")

            val splitLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            emulatorView = EmulatorSurfaceView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1.0f
                )
            }
            splitLayout.addView(emulatorView)

            val companionView = CompanionScreenView(this, viewModel) {
                openRomLauncher.launch(arrayOf("*/*"))
            }.apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1.0f
                )
            }
            splitLayout.addView(companionView)

            setContentView(splitLayout)
        }
    }

    private fun showPresentation(display: Display) {
        companionPresentation?.dismiss()
        companionPresentation = CompanionPresentation(this, display, viewModel) {
            openRomLauncher.launch(arrayOf("*/*"))
        }.apply {
            show()
        }
    }

    override fun onDisplayAdded(displayId: Int) {
        setupDisplays()
    }

    override fun onDisplayRemoved(displayId: Int) {
        if (companionPresentation?.display?.displayId == displayId) {
            companionPresentation?.dismiss()
            companionPresentation = null
            setupDisplays()
        }
    }

    override fun onDisplayChanged(displayId: Int) {}

    override fun onPause() {
        super.onPause()
        audioDriver.stop()
        // Auto-save quick state on pause
        val gameKey = viewModel.activeRomTitle.value
        if (gameKey.isNotBlank()) {
            SaveStateManager(this).quickSave(gameKey)
        }
    }

    override fun onResume() {
        super.onResume()
        audioDriver.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioDriver.stop()
        viewModel.stopPolling()
        displayManager?.unregisterDisplayListener(this)
        companionPresentation?.dismiss()
        companionPresentation = null
        LibretroHost.nativeCleanup()
    }
}
