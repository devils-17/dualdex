package com.dualdex.companion.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import com.dualdex.cheats.CheatItem
import com.dualdex.cheats.CheatManager
import com.dualdex.companion.CompanionViewModel

class CheatsScreenView(
    context: Context,
    private val viewModel: CompanionViewModel
) : LinearLayout(context) {

    private val cheatManager = CheatManager(context)
    private val cheatsListContainer: LinearLayout
    private val activeRomLabel: TextView

    private val density = context.resources.displayMetrics.density
    private fun dp(v: Int): Int = (v * density).toInt()

    init {
        orientation = VERTICAL
        setBackgroundColor(0xFF121216.toInt())
        setPadding(dp(16), dp(16), dp(16), dp(20))

        val scroll = ScrollView(context).apply {
            isVerticalScrollBarEnabled = true
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        val mainContent = LinearLayout(context).apply {
            orientation = VERTICAL
        }
        scroll.addView(mainContent)
        addView(scroll)

        // Header Card
        val headerCard = createCardLayout().apply {
            val titleRow = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val titleView = TextView(context).apply {
                text = "⚡ Action Replay & Cheats"
                setTextColor(Color.WHITE)
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
            }
            titleRow.addView(titleView)
            addView(titleRow)

            activeRomLabel = TextView(context).apply {
                val gameTitle = getActiveGameTitle()
                text = "Game: $gameTitle"
                setTextColor(0xFF4A9EFF.toInt())
                textSize = 13.5f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, dp(4), 0, dp(8))
            }
            addView(activeRomLabel)

            val descView = TextView(context).apply {
                text = "DualDex supports Action Replay v3, GameShark, and CodeBreaker codes. " +
                       "Codes are injected directly into the mGBA core in real-time."
                setTextColor(0xFFAAAAAA.toInt())
                textSize = 12f
                setPadding(0, 0, 0, dp(12))
            }
            addView(descView)

            // Button Row: Add Custom Cheat, Load Presets, Disable All
            val btnRow = LinearLayout(context).apply {
                orientation = HORIZONTAL
            }

            val addBtn = Button(context).apply {
                text = "➕ Add Cheat"
                setTextColor(Color.WHITE)
                textSize = 11.5f
                typeface = Typeface.DEFAULT_BOLD
                background = GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setColor(0xFF2E5B88.toInt())
                }
                setPadding(dp(12), dp(8), dp(12), dp(8))
                setOnClickListener { showAddCheatDialog() }
            }
            val lpAdd = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f).apply {
                setMargins(0, 0, dp(6), 0)
            }
            btnRow.addView(addBtn, lpAdd)

            val presetBtn = Button(context).apply {
                text = "⚡ Load Presets"
                setTextColor(Color.WHITE)
                textSize = 11.5f
                typeface = Typeface.DEFAULT_BOLD
                background = GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setColor(0xFF2E6B4A.toInt())
                }
                setPadding(dp(12), dp(8), dp(12), dp(8))
                setOnClickListener {
                    val gameTitle = getActiveGameTitle()
                    cheatManager.resetToDefaultPresets(gameTitle)
                    refreshUI()
                    Toast.makeText(context, "Loaded presets for $gameTitle", Toast.LENGTH_SHORT).show()
                }
            }
            val lpPreset = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f).apply {
                setMargins(0, 0, dp(6), 0)
            }
            btnRow.addView(presetBtn, lpPreset)

            val disableAllBtn = Button(context).apply {
                text = "🚫 Disable All"
                setTextColor(Color.WHITE)
                textSize = 11.5f
                typeface = Typeface.DEFAULT_BOLD
                background = GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setColor(0xFF383844.toInt())
                }
                setPadding(dp(12), dp(8), dp(12), dp(8))
                setOnClickListener {
                    val gameTitle = getActiveGameTitle()
                    val cheats = cheatManager.getCheats(gameTitle)
                    val updated = cheats.map { it.copy(enabled = false) }
                    cheatManager.saveCheats(gameTitle, updated)
                    cheatManager.applyCheats(gameTitle)
                    refreshUI()
                    Toast.makeText(context, "All cheats disabled", Toast.LENGTH_SHORT).show()
                }
            }
            val lpDisable = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f)
            btnRow.addView(disableAllBtn, lpDisable)

            addView(btnRow)
        }
        mainContent.addView(headerCard)

        // Cheats List Container
        cheatsListContainer = LinearLayout(context).apply {
            orientation = VERTICAL
        }
        mainContent.addView(cheatsListContainer)

        refreshUI()
    }

    fun refreshUI() {
        val gameTitle = getActiveGameTitle()
        activeRomLabel.text = "Game: $gameTitle"
        cheatsListContainer.removeAllViews()

        val cheats = cheatManager.getCheats(gameTitle)
        if (cheats.isEmpty()) {
            val emptyCard = createCardLayout().apply {
                val emptyText = TextView(context).apply {
                    text = "No cheats configured for $gameTitle.\n\nTap '⚡ Load Presets' to get standard codes for this game, or tap '➕ Add Cheat' to paste Action Replay codes."
                    setTextColor(0xFFAAAAAA.toInt())
                    textSize = 14f
                    gravity = Gravity.CENTER
                    setPadding(dp(16), dp(24), dp(16), dp(24))
                }
                addView(emptyText)
            }
            cheatsListContainer.addView(emptyCard)
            return
        }

        cheats.forEach { cheat ->
            val cheatCard = createCardLayout().apply {
                // Top Row: Title + [PRESET] + Toggle Button
                val titleRow = LinearLayout(context).apply {
                    orientation = HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                val titleView = TextView(context).apply {
                    text = cheat.name
                    setTextColor(if (cheat.enabled) 0xFF4A9EFF.toInt() else Color.WHITE)
                    textSize = 14.5f
                    typeface = Typeface.DEFAULT_BOLD
                }
                val lpTitle = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f)
                titleRow.addView(titleView, lpTitle)

                if (cheat.isPreset) {
                    val presetBadge = TextView(context).apply {
                        text = "PRESET"
                        textSize = 9.5f
                        setTextColor(0xFF88CC88.toInt())
                        typeface = Typeface.DEFAULT_BOLD
                        setPadding(dp(6), dp(2), dp(6), dp(2))
                        background = GradientDrawable().apply {
                            cornerRadius = dp(6).toFloat()
                            setColor(0xFF1E2B20.toInt())
                        }
                    }
                    val lpBadge = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, dp(8), 0)
                    }
                    titleRow.addView(presetBadge, lpBadge)
                }

                val toggleBtn = Button(context).apply {
                    text = if (cheat.enabled) "ACTIVE" else "OFF"
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.WHITE)
                    background = GradientDrawable().apply {
                        cornerRadius = dp(8).toFloat()
                        setColor(if (cheat.enabled) 0xFF2E6B4A.toInt() else 0xFF2C2C36.toInt())
                    }
                    setPadding(dp(10), dp(4), dp(10), dp(4))
                    setOnClickListener {
                        val newState = !cheat.enabled
                        cheatManager.toggleCheat(gameTitle, cheat.id, newState)
                        refreshUI()
                        Toast.makeText(context, "${cheat.name}: ${if (newState) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
                    }
                }
                val lpToggle = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                titleRow.addView(toggleBtn, lpToggle)
                addView(titleRow)

                // Code snippet view (monospaced)
                val codeView = TextView(context).apply {
                    text = cheat.code
                    setTextColor(0xFFB0B0C0.toInt())
                    textSize = 12f
                    typeface = Typeface.MONOSPACE
                    setPadding(dp(10), dp(8), dp(10), dp(8))
                    background = GradientDrawable().apply {
                        cornerRadius = dp(8).toFloat()
                        setColor(0xFF14141A.toInt())
                    }
                }
                val lpCode = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, dp(8), 0, dp(8))
                }
                addView(codeView, lpCode)

                // Action Row (Delete button)
                val actionRow = LinearLayout(context).apply {
                    orientation = HORIZONTAL
                    gravity = Gravity.END
                }
                val deleteBtn = TextView(context).apply {
                    text = "🗑️ Delete"
                    textSize = 11.5f
                    setTextColor(0xFFFF6B6B.toInt())
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                    setOnClickListener {
                        cheatManager.deleteCheat(gameTitle, cheat.id)
                        refreshUI()
                    }
                }
                actionRow.addView(deleteBtn)
                addView(actionRow)
            }
            cheatsListContainer.addView(cheatCard)
        }
    }

    private fun showAddCheatDialog() {
        val gameTitle = getActiveGameTitle()
        val builder = AlertDialog.Builder(context)
        builder.setTitle("➕ Add Cheat Code")

        val dialogContent = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dp(20), dp(10), dp(20), dp(10))
        }

        val nameLabel = TextView(context).apply {
            text = "Cheat Name / Description:"
            setTextColor(0xFFCCCCCC.toInt())
            textSize = 13f
        }
        dialogContent.addView(nameLabel)

        val nameInput = EditText(context).apply {
            hint = "e.g. 999 Rare Candies, Max Cash"
            textSize = 14f
            setTextColor(Color.WHITE)
            setHintTextColor(0xFF777777.toInt())
        }
        dialogContent.addView(nameInput)

        val codeLabel = TextView(context).apply {
            text = "\nCheat Code (Action Replay / GameShark / CodeBreaker):"
            setTextColor(0xFFCCCCCC.toInt())
            textSize = 13f
        }
        dialogContent.addView(codeLabel)

        val codeInput = EditText(context).apply {
            hint = "XXXXXXXX XXXXXXXX\nYYYYYYYY YYYYYYYY"
            textSize = 13f
            typeface = Typeface.MONOSPACE
            minLines = 4
            setTextColor(Color.WHITE)
            setHintTextColor(0xFF777777.toInt())
        }
        dialogContent.addView(codeInput)

        val helperText = TextView(context).apply {
            text = "\nEnter standard Action Replay v3 (16 hex chars per line) or CodeBreaker (8+4 hex chars). Multiple lines are supported."
            setTextColor(0xFF888888.toInt())
            textSize = 11f
        }
        dialogContent.addView(helperText)

        builder.setView(dialogContent)

        builder.setPositiveButton("Add & Enable") { _, _ ->
            val name = nameInput.text.toString().trim()
            val code = codeInput.text.toString().trim()
            if (code.isNotBlank()) {
                val cheat = CheatItem(
                    name = if (name.isNotBlank()) name else "Custom Cheat",
                    code = code,
                    enabled = true,
                    isPreset = false
                )
                cheatManager.addCheat(gameTitle, cheat)
                refreshUI()
                Toast.makeText(context, "Added cheat: ${cheat.name}", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun getActiveGameTitle(): String {
        val title = viewModel.activeRomTitle.value
        if (title.isNotBlank()) return title
        val prof = viewModel.activeProfile.value
        if (prof.name.isNotBlank()) return prof.name
        return "Pokemon Heart & Soul"
    }

    private fun createCardLayout(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(0xFF1E1E26.toInt())
            }
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(14))
            }
        }
    }
}
