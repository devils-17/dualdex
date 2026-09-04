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
    private val defenderAutoInput: AutoCompleteTextView
    private val weatherSpinner: Spinner
    private val critCheckBox: CheckBox
    private val screensCheckBox: CheckBox

    private var selectedDefenderSpecies = "Skarmory"
    private var selectedMoveName = "Rock Slide"
    private var currentWeather: String? = null
    private var isCrit: Boolean = false
    private var hasScreens: Boolean = false

    init {
        orientation = VERTICAL
        setBackgroundColor(0xFF121216.toInt())
        setPadding(20, 20, 20, 20)

        val scroll = ScrollView(context).apply { isVerticalScrollBarEnabled = true }
        val content = LinearLayout(context).apply { orientation = VERTICAL }
        scroll.addView(content)
        addView(scroll)

        // Title
        val titleView = TextView(context).apply {
            text = "⚔️ DualDex Damage Calculator"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 14)
        }
        content.addView(titleView)

        // Attacker & Defender Header Card
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
                setPadding(0, 4, 0, 8)
            }
            addView(attackerHeaderView)
            addView(defenderHeaderView)

            // Defender Autocomplete Search Input
            val searchLabel = TextView(context).apply {
                text = "Target Pokemon Search / Autocomplete:"
                setTextColor(0xFFAAAAAA.toInt())
                textSize = 12f
                setPadding(0, 4, 0, 4)
            }
            addView(searchLabel)

            val speciesSuggestions = listOf(
                "Bulbasaur", "Ivysaur", "Venusaur", "Charmander", "Charmeleon", "Charizard",
                "Squirtle", "Wartortle", "Blastoise", "Pikachu", "Raichu", "Gengar", "Gyarados",
                "Lapras", "Snorlax", "Dragonite", "Mewtwo", "Treecko", "Grovyle", "Sceptile",
                "Torchic", "Combusken", "Blaziken", "Mudkip", "Marshtomp", "Swampert",
                "Gardevoir", "Sableye", "Aggron", "Flygon", "Salamence", "Metagross", "Rayquaza",
                "Skarmory", "Lichtoise", "Spectrasaur", "Phantomander"
            )

            val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, speciesSuggestions)
            defenderAutoInput = AutoCompleteTextView(context).apply {
                hint = "Type defender species (e.g. Blaziken)"
                setHintTextColor(0xFF777788.toInt())
                setTextColor(Color.WHITE)
                textSize = 14f
                setAdapter(adapter)
                threshold = 1
                background = GradientDrawable().apply {
                    cornerRadius = 12f
                    setColor(0xFF16161E.toInt())
                    setStroke(2, 0xFF333348.toInt())
                }
                setPadding(16, 10, 16, 10)
                setOnItemClickListener { _, _, position, _ ->
                    val chosen = adapter.getItem(position) ?: return@setOnItemClickListener
                    selectedDefenderSpecies = chosen
                    recalculate()
                }
            }
            addView(defenderAutoInput)
        }
        content.addView(matchupCard)

        // Field Conditions Card (Weather, Screens, Crits)
        val fieldCard = createCardLayout().apply {
            val fieldLabel = TextView(context).apply {
                text = "Field Conditions & Modifiers"
                setTextColor(0xFF50C878.toInt())
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, 8)
            }
            addView(fieldLabel)

            val row1 = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val weatherLabel = TextView(context).apply {
                text = "Weather: "
                setTextColor(Color.WHITE)
                textSize = 13f
            }
            row1.addView(weatherLabel)

            val weatherOptions = listOf("None", "Sun", "Rain", "Sand", "Hail")
            val weatherAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, weatherOptions)
            weatherSpinner = Spinner(context).apply {
                this.adapter = weatherAdapter
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p0: AdapterView<*>?, p1: android.view.View?, pos: Int, p3: Long) {
                        currentWeather = if (pos == 0) null else weatherOptions[pos]
                        recalculate()
                    }
                    override fun onNothingSelected(p0: AdapterView<*>?) {}
                }
            }
            row1.addView(weatherSpinner)
            addView(row1)

            val row2 = LinearLayout(context).apply {
                orientation = HORIZONTAL
                setPadding(0, 6, 0, 0)
            }

            critCheckBox = CheckBox(context).apply {
                text = "Critical Hit"
                setTextColor(Color.WHITE)
                textSize = 13f
                setOnCheckedChangeListener { _, checked ->
                    isCrit = checked
                    recalculate()
                }
            }
            row2.addView(critCheckBox)

            screensCheckBox = CheckBox(context).apply {
                text = "Reflect / Light Screen"
                setTextColor(Color.WHITE)
                textSize = 13f
                setOnCheckedChangeListener { _, checked ->
                    hasScreens = checked
                    recalculate()
                }
            }
            val lpScreen = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(20, 0, 0, 0)
            }
            row2.addView(screensCheckBox, lpScreen)
            addView(row2)
        }
        content.addView(fieldCard)

        // Moves Selection Card
        val movesCard = createCardLayout().apply {
            val movesTitle = TextView(context).apply {
                text = "Attacker Moves"
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, 8)
            }
            addView(movesTitle)

            movesContainer = LinearLayout(context).apply {
                orientation = VERTICAL
            }
            addView(movesContainer)
        }
        content.addView(movesCard)

        // Result Card
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

        // Auto-populate defender from opponent memory read if in battle!
        val enemyParty = viewModel.enemyParty.value
        val inBattle = viewModel.isInBattle.value
        if (inBattle && enemyParty.isNotEmpty()) {
            val enemyMon = enemyParty[0]
            val speciesName = SpeciesDatabase.get(enemyMon.species).name
            selectedDefenderSpecies = speciesName
            defenderHeaderView.text = "🔴 Defender (Opponent In-Battle): $selectedDefenderSpecies (Lv. ${enemyMon.level})"
        } else {
            defenderHeaderView.text = "🔴 Defender: $selectedDefenderSpecies (Lv. 50)"
        }

        val atkName = attacker?.let {
            if (it.nickname.isNotBlank()) "${it.nickname} (${SpeciesDatabase.get(it.species).name})" else SpeciesDatabase.get(it.species).name
        } ?: "Salamence"
        val atkLevel = attacker?.level ?: 50

        attackerHeaderView.text = "🔵 Attacker: $atkName (Lv. $atkLevel)"

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

        val btnRow = LinearLayout(context).apply { orientation = HORIZONTAL }
        availableMoves.forEach { moveName ->
            val isSelected = (moveName == selectedMoveName)
            val moveBtn = Button(context).apply {
                text = moveName
                textSize = 12.5f
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    cornerRadius = 14f
                    setColor(if (isSelected) 0xFF4A9EFF.toInt() else 0xFF2B3A55.toInt())
                }
                setPadding(18, 8, 18, 8)
                setOnClickListener {
                    selectedMoveName = moveName
                    refreshUI()
                }
            }
            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 10, 6)
            }
            btnRow.addView(moveBtn, lp)
        }
        movesContainer.addView(btnRow)

        if (selectedMoveName.isEmpty() && availableMoves.isNotEmpty()) {
            selectedMoveName = availableMoves[0]
        }
        recalculate()
    }

    private fun recalculate() {
        val party = viewModel.playerParty.value
        val selectedIdx = viewModel.selectedMemberIndex.value
        val attacker = if (party.isNotEmpty() && selectedIdx in party.indices) party[selectedIdx] else null

        val atkSpecies = attacker?.let { SpeciesDatabase.get(it.species).name }
            ?: "Salamence"
        val atkLevel = attacker?.level ?: 50

        val req = DamageCalculationRequest(
            gen = 3,
            attacker = CalcPokemonInput(
                species = atkSpecies,
                level = atkLevel,
                evs = StatBlock(atk = 252, spa = 252, spe = 252)
            ),
            defender = CalcPokemonInput(
                species = selectedDefenderSpecies,
                level = 50,
                evs = StatBlock(hp = 252, def = 252, spd = 252)
            ),
            move = CalcMoveInput(name = selectedMoveName, isCrit = isCrit),
            field = CalcFieldInput(
                weather = currentWeather,
                gameType = "singles",
                defenderSide = if (hasScreens) SideConditions(isReflect = true, isLightScreen = true) else null
            )
        )

        val res = DamageCalculator.calculate(req)
        if (res.success) {
            val rangeStr = if (res.range.isNotEmpty()) "${res.minDamage} - ${res.maxDamage} HP" else "N/A"
            val rollsStr = res.range.joinToString(", ")
            val koText = if (res.koChanceText.isNotBlank()) "\nKO Chance: ${res.koChanceText}" else ""
            val critText = if (isCrit) " [Critical Hit!]" else ""
            val weatherText = if (currentWeather != null) " [Weather: $currentWeather]" else ""

            resultTextView.text = "${res.desc}$critText$weatherText\n\n" +
                    "Damage Range: $rangeStr\n" +
                    "Move: ${res.moveName} (${res.moveType} ${res.moveCategory}, ${res.movePower} Power)\n" +
                    "Defender Max HP: ${res.defenderMaxHP} HP$koText\n\n" +
                    "Damage Rolls (16): [$rollsStr]"
        } else {
            resultTextView.text = "Calculation: ${res.error ?: "Select move to calculate"}"
        }
    }

    private fun createCardLayout(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(18, 16, 18, 16)
            background = GradientDrawable().apply {
                cornerRadius = 18f
                setColor(0xFF1E1E26.toInt())
            }
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, 14)
            }
        }
    }
}
