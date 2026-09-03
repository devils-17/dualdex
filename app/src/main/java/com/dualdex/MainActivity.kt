package com.dualdex

import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var companionPresentation: Presentation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Top screen view (Emulator display)
        val topView = TextView(this).apply {
            text = "DualDex - AYN Thor Top Screen (Emulator Host)"
            textSize = 20f
            setPadding(40, 40, 40, 40)
        }
        setContentView(topView)

        // Initialize secondary display for AYN Thor bottom screen
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
                val bottomView = TextView(context).apply {
                    text = "DualDex Companion - AYN Thor Bottom Screen\n(Damage Calc & Pokemon Stats)"
                    textSize = 18f
                    setPadding(30, 30, 30, 30)
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
    }
}
