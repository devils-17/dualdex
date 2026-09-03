package com.dualdex.companion.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*
import com.dualdex.calculator.*
import com.dualdex.companion.CompanionViewModel
import com.dualdex.pokemon.MoveDatabase
import com.dualdex.pokemon.SpeciesDatabase

class CalcTabScreenView(
    context: Context,
    private val viewModel: CompanionViewModel
) : LinearLayout(context) {

    private val resultTextView: TextView
    private val movesContainer: LinearLayout
    private val attackerHeaderView: TextView
    private val defenderHeaderView: TextView

    private var selectedDefenderSpecies = "Skarmory"

    init {
        orientation = VERTICAL
        setBackgroundColor(0xFF121216.toInt())
        setPadding(24, 24, 24, 24)

        val scroll = ScrollView(context).apply {
            isVerticalScrollBarEnabled = true
        }
        val content = LinearLayout(context).apply {
            orientation = VERTICAL
        }
        scroll.addView(content)
        addView(scroll)

        // Title
        val titleView = TextView(context).apply {
            text = "⚔️ DualDex Live Damage Calculator"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 16)
        }
        content.addView(titleView)

        // Matchup Card (Attacker vs Defender)
        val matchupCard = createCardLayout().apply {
            attackerHeaderView = TextView(context).apply {
                setTextColor(0xFF4A9EFF.toInt())
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
            }
            defenderHeaderView = TextView(context).apply {
                setTextColor(0xFFFF6B6B.toInt())
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 6, 0, 0)
            }
            addView(attackerHeaderView)
            addView(defenderHeaderView)

            // Quick defender switch buttons
            val defPickerRow = LinearLayout(context).apply {
                orientation = HORIZONTAL
                setPadding(0, 12, 0, 0)
            }
            val defLabel = TextView(context).apply {
                text = "Target: "
                setTextColor(0xFFAAAAAA.toInt())
                textSize = 13f
                gravity = Gravity.CENTER_VERTICAL
            }
            defPickerRow.addView(defLabel)

            listOf("Skarmory", "Blaziken", "Swampert", "Gengar", "Dragonite").forEach { targetName ->
                val btn = Button(context).apply {
                    text = targetName
                    textSize = 11f
                    setTextColor(Color.WHITE)
                    background = GradientDrawable().apply {
                        cornerRadius = 14f
                        setColor(0xFF2E2E38.toInt())
                    }
                    setPadding(16, 4, 16, 4)
                    setOnClickListener {
                        selectedDefenderSpecies = targetName
                        recalculateDefault()
                    }
                }
                val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 10, 0)
                }
                defPickerRow.addView(btn, lp)
            }
            addView(defPickerRow)
        }
        content.addView(matchupCard)

        // Moves Selection Card
        val movesCard = createCardLayout().apply {
            val movesTitle = TextView(context).apply {
                text = "Select Move to Calculate"
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, 10)
            }
            addView(movesTitle)

            movesContainer = LinearLayout(context).apply {
                orientation = VERTICAL
            }
            addView(movesContainer)
        }
        content.addView(movesCard)

        // Calculation Result Card
        val resultCard = createCardLayout().apply {
            val resTitle = TextView(context).apply {
                text = "Damage Calculation Result"
                setTextColor(0xFFFFD700.toInt())
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, 10)
            }
            addView(resTitle)

            resultTextView = TextView(context).apply {
                text = "Select a move above to run calculation."
                setTextColor(0xFFE0E0E0.toInt())
                textSize = 14f
                setLineSpacing(6f, 1f)
            }
            addView(resultTextView)
        }
        content.addView(resultCard)

        refreshUI()
    }

    fun refreshUI() {
        val party = viewModel.playerParty.value
        val selectedIdx = viewModel.selectedMemberIndex.value
        val attacker = if (party.isNotEmpty() && selectedIdx in party.indices) party[selectedIdx] else null

        val enemyParty = viewModel.enemyParty.value
        val activeEnemy = enemyParty.firstOrNull { !it.isEmpty && it.isValid }
        val defSpecies = activeEnemy?.nickname?.ifEmpty { null } ?: selectedDefenderSpecies

        val atkName = attacker?.nickname?.ifEmpty { null }
            ?: attacker?.let { SpeciesDatabase.get(it.species).name }
            ?: "Salamence"
        val atkLevel = attacker?.level ?: 50

        attackerHeaderView.text = "🔵 Attacker: $atkName (Lv. $atkLevel)"
        defenderHeaderView.text = "🔴 Defender: $defSpecies (Lv. 50)"

        movesContainer.removeAllViews()

        val availableMoves = mutableListOf<String>()
        if (attacker != null) {
            attacker.moves.forEach { mId ->
                if (mId > 0) {
                    val info = MoveDatabase.get(mId)
                    availableMoves.add(info.name)
                }
            }
        }
        if (availableMoves.isEmpty()) {
            availableMoves.addAll(listOf("Rock Slide", "Earthquake", "Flamethrower", "Hydro Pump"))
        }

        val btnRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
        }
        availableMoves.forEach { moveName ->
            val moveBtn = Button(context).apply {
                text = moveName
                textSize = 13f
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    cornerRadius = 16f
                    setColor(0xFF2B3A55.toInt())
                }
                setPadding(20, 10, 20, 10)
                setOnClickListener {
                    runCalculation(atkName, atkLevel, defSpecies, moveName)
                }
            }
            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 12, 8)
            }
            btnRow.addView(moveBtn, lp)
        }
        movesContainer.addView(btnRow)

        // Run default calc
        if (availableMoves.isNotEmpty()) {
            runCalculation(atkName, atkLevel, defSpecies, availableMoves[0])
        }
    }

    private fun recalculateDefault() {
        refreshUI()
    }

    private fun runCalculation(atkSpecies: String, atkLevel: Int, defSpecies: String, moveName: String) {
        val req = DamageCalculationRequest(
            gen = 3,
            attacker = CalcPokemonInput(
                species = atkSpecies,
                level = atkLevel,
                evs = StatBlock(atk = 252, spa = 252, spe = 252)
            ),
            defender = CalcPokemonInput(
                species = defSpecies,
                level = 50,
                evs = StatBlock(hp = 252, def = 252, spd = 252)
            ),
            move = CalcMoveInput(name = moveName)
        )

        val res = DamageCalculator.calculate(req)
        if (res.success) {
            val rangeStr = if (res.range.isNotEmpty()) "${res.minDamage} - ${res.maxDamage} HP" else "N/A"
            val rollsStr = res.range.joinToString(", ")
            val koText = if (res.koChanceText.isNotBlank()) "\nKO Chance: ${res.koChanceText}" else ""

            resultTextView.text = "${res.desc}\n\n" +
                    "Damage Range: $rangeStr\n" +
                    "Move: ${res.moveName} (${res.moveType} ${res.moveCategory}, ${res.movePower} Power)\n" +
                    "Defender Max HP: ${res.defenderMaxHP} HP$koText\n\n" +
                    "All Rolls (16 values): [$rollsStr]"
        } else {
            resultTextView.text = "Calculation failed: ${res.error ?: "Unknown error"}"
        }
    }

    private fun createCardLayout(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(24, 20, 24, 20)
            background = GradientDrawable().apply {
                cornerRadius = 24f
                setColor(0xFF1E1E26.toInt())
            }
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, 20)
            }
        }
    }
}
