package com.dualdex.companion.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*
import com.dualdex.companion.CompanionViewModel
import com.dualdex.pokemon.PokemonType
import com.dualdex.pokemon.TypeChart

class TypeChartScreenView(
    context: Context,
    private val viewModel: CompanionViewModel? = null
) : LinearLayout(context) {

    private var selectedType1: PokemonType = PokemonType.FIRE
    private var selectedType2: PokemonType? = PokemonType.FLYING
    private var steelResistsGhostDark = false

    private val resultContainer: LinearLayout

    init {
        orientation = VERTICAL
        setBackgroundColor(0xFF121216.toInt())
        setPadding(24, 24, 24, 24)

        val scroll = ScrollView(context).apply { isVerticalScrollBarEnabled = true }
        val content = LinearLayout(context).apply { orientation = VERTICAL }
        scroll.addView(content)
        addView(scroll)

        // Title
        val titleView = TextView(context).apply {
            text = "🛡️ Type Matchup & Weakness Calculator"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 16)
        }
        content.addView(titleView)

        // Primary Type Selector Card
        val type1Card = createCardLayout().apply {
            val label = TextView(context).apply {
                text = "Primary Defending Type"
                setTextColor(0xFF4A9EFF.toInt())
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, 8)
            }
            addView(label)

            val grid = createTypePickerGrid { chosen ->
                selectedType1 = chosen
                updateMatchupDisplay()
            }
            addView(grid)
        }
        content.addView(type1Card)

        // Secondary Type Selector Card
        val type2Card = createCardLayout().apply {
            val label = TextView(context).apply {
                text = "Secondary Type (Optional)"
                setTextColor(0xFF50C878.toInt())
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, 8)
            }
            addView(label)

            val noneBtn = Button(context).apply {
                text = "None (Mono Type)"
                textSize = 12f
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    cornerRadius = 14f
                    setColor(0xFF333340.toInt())
                }
                setPadding(16, 6, 16, 6)
                setOnClickListener {
                    selectedType2 = null
                    updateMatchupDisplay()
                }
            }
            addView(noneBtn)

            val grid = createTypePickerGrid { chosen ->
                selectedType2 = chosen
                updateMatchupDisplay()
            }
            addView(grid)
        }
        content.addView(type2Card)

        // Results Card
        val resCard = createCardLayout().apply {
            val label = TextView(context).apply {
                text = "Defensive Matchup Breakdown"
                setTextColor(0xFFFFD700.toInt())
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, 12)
            }
            addView(label)

            resultContainer = LinearLayout(context).apply {
                orientation = VERTICAL
            }
            addView(resultContainer)
        }
        content.addView(resCard)

        updateMatchupDisplay()
    }

    fun updateMatchupDisplay() {
        resultContainer.removeAllViews()

        val steelResists = viewModel?.activeProfile?.value?.steelResistsGhostDark ?: steelResistsGhostDark
        val profile = TypeChart.getDefenseProfile(selectedType1, selectedType2, steelResists)

        val headerText = TextView(context).apply {
            val t2Str = selectedType2?.let { " / ${it.displayName}" } ?: ""
            text = "Target: ${selectedType1.displayName}$t2Str\n"
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        }
        resultContainer.addView(headerText)

        // 4x Weaknesses
        if (profile.weaknesses4x.isNotEmpty()) {
            addMatchupRow("🚨 4x Double Weak:", profile.weaknesses4x, 0xFFFF3333.toInt())
        }

        // 2x Weaknesses
        if (profile.weaknesses2x.isNotEmpty()) {
            addMatchupRow("⚠️ 2x Weak:", profile.weaknesses2x, 0xFFFF7744.toInt())
        }

        // 0.5x Resistances
        if (profile.resistancesHalf.isNotEmpty()) {
            addMatchupRow("🛡️ 0.5x Resist:", profile.resistancesHalf, 0xFF44CC66.toInt())
        }

        // 0.25x Resistances
        if (profile.resistancesQuarter.isNotEmpty()) {
            addMatchupRow("✨ 0.25x Double Resist:", profile.resistancesQuarter, 0xFF22AA88.toInt())
        }

        // Immunities
        if (profile.immunities.isNotEmpty()) {
            addMatchupRow("🚫 0x Immune:", profile.immunities, 0xFFAAAAAA.toInt())
        }
    }

    private fun addMatchupRow(title: String, types: List<PokemonType>, titleColor: Int) {
        val row = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(0, 8, 0, 8)
        }
        val label = TextView(context).apply {
            text = title
            setTextColor(titleColor)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 4)
        }
        row.addView(label)

        val badgesRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
        }
        types.forEach { type ->
            val badge = TextView(context).apply {
                text = type.displayName
                setTextColor(Color.WHITE)
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(14, 4, 14, 4)
                background = GradientDrawable().apply {
                    cornerRadius = 12f
                    setColor(type.colorHex.toInt())
                }
            }
            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 8, 0)
            }
            badgesRow.addView(badge, lp)
        }
        row.addView(badgesRow)
        resultContainer.addView(row)
    }

    private fun createTypePickerGrid(onSelected: (PokemonType) -> Unit): LinearLayout {
        val container = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(0, 8, 0, 8)
        }

        val allTypes = PokemonType.values().toList()
        // 3 rows of 6 types
        for (row in 0 until 3) {
            val rowLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                setPadding(0, 4, 0, 4)
            }
            for (col in 0 until 6) {
                val idx = row * 6 + col
                if (idx < allTypes.size) {
                    val t = allTypes[idx]
                    val btn = Button(context).apply {
                        text = t.displayName
                        textSize = 10f
                        setTextColor(Color.WHITE)
                        background = GradientDrawable().apply {
                            cornerRadius = 12f
                            setColor(t.colorHex.toInt())
                        }
                        setPadding(8, 2, 8, 2)
                        setOnClickListener { onSelected(t) }
                    }
                    val lp = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f).apply {
                        setMargins(2, 0, 2, 0)
                    }
                    rowLayout.addView(btn, lp)
                }
            }
            container.addView(rowLayout)
        }

        return container
    }

    private fun createCardLayout(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(20, 16, 20, 16)
            background = GradientDrawable().apply {
                cornerRadius = 20f
                setColor(0xFF1E1E26.toInt())
            }
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, 16)
            }
        }
    }
}
