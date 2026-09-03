package com.dualdex

import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.dualdex.calculator.CalcMoveInput
import com.dualdex.calculator.CalcPokemonInput
import com.dualdex.calculator.DamageCalculationRequest
import com.dualdex.calculator.DamageCalculator
import com.dualdex.calculator.StatBlock
import com.dualdex.emulator.EmulatorSurfaceView
import com.dualdex.emulator.LibretroHost
import java.io.File

class MainActivity : AppCompatActivity() {

    private var emulatorView: EmulatorSurfaceView? = null
    private var companionPresentation: Presentation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Damage Calculator QuickJS engine
        DamageCalculator.initialize(this)

        // Initialize Libretro core path
        val corePath = "${applicationInfo.nativeLibraryDir}/mgba_libretro.so"
        if (File(corePath).exists()) {
            val loaded = LibretroHost.nativeLoadCore(corePath)
            Log.i("DualDex", "Loaded mGBA Libretro core: $loaded (path=$corePath)")
        } else {
            Log.w("DualDex", "Core file not found at: $corePath")
        }

        // Top Screen: Emulator Surface View
        val rootLayout = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        emulatorView = EmulatorSurfaceView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        rootLayout.addView(emulatorView)
        setContentView(rootLayout)

        // Secondary Screen (Bottom Display on AYN Thor)
        setupSecondaryDisplay()
    }

    private fun setupSecondaryDisplay() {
        val dm = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager ?: return
        val displays = dm.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)

        if (displays.isNotEmpty()) {
            val secondaryDisplay = displays[0]
            Log.i("DualDex", "Found secondary display: ${secondaryDisplay.name} (id=${secondaryDisplay.displayId})")
            showCompanionPresentation(secondaryDisplay)
        } else {
            Log.w("DualDex", "No secondary display found. Running in single-screen fallback mode.")
        }
    }

    private fun showCompanionPresentation(display: Display) {
        val presentation = object : Presentation(this, display) {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)

                // Run a sample damage calculation to verify live calculator on companion screen
                val sampleCalc = DamageCalculator.calculate(
                    DamageCalculationRequest(
                        gen = 3,
                        attacker = CalcPokemonInput(
                            species = "Salamence",
                            level = 50,
                            nature = "Adamant",
                            item = "Choice Band",
                            evs = StatBlock(atk = 252, spe = 252)
                        ),
                        defender = CalcPokemonInput(
                            species = "Skarmory",
                            level = 50,
                            nature = "Impish",
                            evs = StatBlock(hp = 252, def = 252)
                        ),
                        move = CalcMoveInput(name = "Rock Slide")
                    )
                )

                val companionText = buildString {
                    append("=== DualDex AYN Thor Companion ===\n\n")
                    append("--- Active Damage Calculator Preview ---\n")
                    if (sampleCalc.success) {
                        append("${sampleCalc.desc}\n\n")
                        append("Damage Range: ${sampleCalc.minDamage} - ${sampleCalc.maxDamage} HP\n")
                        append("Move: ${sampleCalc.moveName} (${sampleCalc.moveType}, Power: ${sampleCalc.movePower})\n")
                    } else {
                        append("Calculator loading: ${sampleCalc.error}\n")
                    }
                    append("\n--- Pokemon Party Live Stats ---\n")
                    append("Waiting for ROM to load...\n")
                }

                val bottomView = TextView(context).apply {
                    text = companionText
                    textSize = 16f
                    setPadding(32, 32, 32, 32)
                }
                setContentView(bottomView)
            }
        }
        presentation.show()
        companionPresentation = presentation
    }

    override fun onDestroy() {
        super.onDestroy()
        companionPresentation?.dismiss()
        companionPresentation = null
        LibretroHost.nativeCleanup()
    }
}
