package com.dualdex

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.dualdex.calculator.DamageCalculator
import com.dualdex.companion.CompanionPresentation
import com.dualdex.companion.CompanionViewModel
import com.dualdex.companion.ui.CompanionScreenView
import com.dualdex.emulator.EmulatorSurfaceView
import com.dualdex.emulator.LibretroHost
import java.io.File

class MainActivity : AppCompatActivity(), DisplayManager.DisplayListener {

    private val viewModel = CompanionViewModel()
    private var emulatorView: EmulatorSurfaceView? = null
    private var companionPresentation: CompanionPresentation? = null
    private var displayManager: DisplayManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize QuickJS damage calculator engine
        DamageCalculator.initialize(this)

        // 2. Initialize mGBA Libretro core
        val corePath = "${applicationInfo.nativeLibraryDir}/mgba_libretro.so"
        if (File(corePath).exists()) {
            val loaded = LibretroHost.nativeLoadCore(corePath)
            Log.i("DualDex", "Loaded mGBA Libretro core: $loaded (path=$corePath)")
        } else {
            Log.w("DualDex", "Core file not found at: $corePath")
        }

        // 3. Register DisplayManager listener for AYN Thor secondary display
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        displayManager?.registerDisplayListener(this, null)

        // 4. Setup display UI
        setupDisplays()

        // 5. Start background memory poller (10Hz)
        viewModel.startPolling(100L)
    }

    private fun setupDisplays() {
        val dm = displayManager ?: return
        val presentationDisplays = dm.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)

        if (presentationDisplays.isNotEmpty()) {
            // Dual-screen mode (AYN Thor detected)
            Log.i("DualDex", "AYN Thor dual-screen detected. Attaching CompanionPresentation to Display 1.")

            // Top screen: full-screen Emulator
            emulatorView = EmulatorSurfaceView(this).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            setContentView(emulatorView)

            // Bottom screen: Presentation
            showPresentation(presentationDisplays[0])
        } else {
            // Single-screen fallback mode (split view top emulator / bottom companion)
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

            val companionView = CompanionScreenView(this, viewModel).apply {
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
        companionPresentation = CompanionPresentation(this, display, viewModel).apply {
            show()
        }
    }

    override fun onDisplayAdded(displayId: Int) {
        Log.i("DualDex", "Display added: $displayId")
        setupDisplays()
    }

    override fun onDisplayRemoved(displayId: Int) {
        Log.i("DualDex", "Display removed: $displayId")
        if (companionPresentation?.display?.displayId == displayId) {
            companionPresentation?.dismiss()
            companionPresentation = null
            setupDisplays()
        }
    }

    override fun onDisplayChanged(displayId: Int) {
        // Handle rotation or resolution change if needed
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopPolling()
        displayManager?.unregisterDisplayListener(this)
        companionPresentation?.dismiss()
        companionPresentation = null
        LibretroHost.nativeCleanup()
    }
}
