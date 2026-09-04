package com.dualdex.companion.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import com.dualdex.companion.CompanionViewModel
import com.dualdex.pokemon.*

class PartyScreenView(
    context: Context,
    private val viewModel: CompanionViewModel
) : LinearLayout(context) {

    private val memberSelectorLayout: LinearLayout
    private val detailContainer: LinearLayout

    private val density = context.resources.displayMetrics.density
    private fun dp(v: Int): Int = (v * density).toInt()

    init {
        orientation = VERTICAL
        setBackgroundColor(0xFF121216.toInt()) // Deep modern dark theme

        // Top horizontal party member selector
        val horizontalScroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            clipToPadding = false
            clipChildren = false
            setPadding(dp(12), dp(10), dp(12), dp(8))
        }
        memberSelectorLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            clipChildren = false
        }
        horizontalScroll.addView(memberSelectorLayout)
        addView(horizontalScroll)

        // Main detail card container inside vertical scroll
        val verticalScroll = ScrollView(context).apply {
            isVerticalScrollBarEnabled = true
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        detailContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dp(16), dp(6), dp(16), dp(24))
        }
        verticalScroll.addView(detailContainer)
        addView(verticalScroll)

        refreshUI()
    }

    fun refreshUI() {
        val party = viewModel.playerParty.value
        val selectedIdx = viewModel.selectedMemberIndex.value
        val gameId = viewModel.activeGameId.value

        // 1. Build member selector chips
        memberSelectorLayout.removeAllViews()
        if (party.isEmpty()) {
            val emptyChip = TextView(context).apply {
                text = "No party data (Waiting for ROM)"
                setTextColor(0xFF888888.toInt())
                textSize = 14f
                setPadding(dp(20), dp(12), dp(20), dp(12))
            }
            memberSelectorLayout.addView(emptyChip)
        } else {
            party.forEachIndexed { index, mon ->
                val isSelected = (index == selectedIdx)
                val chip = LinearLayout(context).apply {
                    orientation = VERTICAL
                    isClickable = true
                    isFocusable = true
                    minimumWidth = dp(96)
                    minimumHeight = dp(56)
                    setPadding(dp(16), dp(10), dp(16), dp(10))
                    gravity = Gravity.CENTER
                    background = GradientDrawable().apply {
                        cornerRadius = dp(14).toFloat()
                        setColor(if (isSelected) 0xFF2E3A59.toInt() else 0xFF1E1E24.toInt())
                        if (isSelected) {
                            setStroke(dp(3), 0xFF4A9EFF.toInt())
                        } else {
                            setStroke(dp(1), 0xFF363644.toInt())
                        }
                    }
                    val nameView = TextView(context).apply {
                        isClickable = false
                        isFocusable = false
                        val displayName = if (mon.nickname.isNotBlank()) mon.nickname else "Mon #${mon.species}"
                        text = "$displayName (Lv.${mon.level})"
                        setTextColor(if (isSelected) 0xFFFFFFFF.toInt() else 0xFFAAAAAA.toInt())
                        textSize = 13.5f
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    val hpView = TextView(context).apply {
                        isClickable = false
                        isFocusable = false
                        text = "${mon.currentHp}/${mon.maxHp} HP"
                        setTextColor(getHpColor(mon.currentHp, mon.maxHp))
                        textSize = 12f
                    }
                    addView(nameView)
                    addView(hpView)

                    setOnClickListener {
                        viewModel.selectMember(index)
                        refreshUI()
                    }
                }
                val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, dp(10), 0)
                }
                memberSelectorLayout.addView(chip, lp)
            }
        }

        // 2. Build detail card
        detailContainer.removeAllViews()
        if (party.isEmpty()) {
            val emptyMsg = TextView(context).apply {
                text = "=== DualDex Party Monitor ===\n\nWaiting for game to load.\nOnce loaded, live Pokémon stats, IVs, EVs, and matchups will appear here in real-time."
                setTextColor(0xFFAAAAAA.toInt())
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(32, 64, 32, 32)
            }
            detailContainer.addView(emptyMsg)
            return
        }

        val safeIdx = if (selectedIdx in party.indices) selectedIdx else 0
        val mon = party[safeIdx]
        val speciesInfo = SpeciesDatabase.get(mon.species)
        val natureInfo = NatureTable.get(mon.nature)
        val isGhostGrey = (gameId == 6) // GAME_GHOST_GREY

        // Header Card (Nickname, Species, Level, Types)
        val headerCard = createCardLayout().apply {
            val titleRow = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val titleText = TextView(context).apply {
                val shinyBadge = if (mon.isShiny) " ⭐" else ""
                val displayName = if (mon.nickname.isNotBlank()) mon.nickname else speciesInfo.name
                text = "$displayName$shinyBadge"
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 24f
                typeface = Typeface.DEFAULT_BOLD
            }
            val levelText = TextView(context).apply {
                text = "  Lv. ${mon.level}"
                setTextColor(0xFF4A9EFF.toInt())
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
            }
            titleRow.addView(titleText)
            titleRow.addView(levelText)
            addView(titleRow)

            // Types badge row
            val typeRow = LinearLayout(context).apply {
                orientation = HORIZONTAL
                setPadding(0, 12, 0, 12)
            }
            typeRow.addView(createTypeBadge(speciesInfo.type1))
            speciesInfo.type2?.let { typeRow.addView(createTypeBadge(it)) }
            addView(typeRow)

            // HP Bar
            val hpRow = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 8, 0, 8)
            }
            val hpLabel = TextView(context).apply {
                text = "HP: ${mon.currentHp} / ${mon.maxHp}"
                setTextColor(getHpColor(mon.currentHp, mon.maxHp))
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
            }
            hpRow.addView(hpLabel)
            addView(hpRow)

            // Nature & Ability
            val metaText = TextView(context).apply {
                text = "Nature: ${natureInfo.formattedDescription}\nItem: ${if (mon.heldItem > 0) "Item #${mon.heldItem}" else "None"}"
                setTextColor(0xFFCCCCCC.toInt())
                textSize = 14f
                setPadding(0, 4, 0, 4)
            }
            addView(metaText)
        }
        detailContainer.addView(headerCard)

        // Stat Card (Adaptive: Base stats for Ghost Grey vs IVs/EVs for Radical Red/Vanilla)
        val statCard = createCardLayout().apply {
            val statTitle = TextView(context).apply {
                text = if (isGhostGrey) "Base Stats (Ghost Grey: No EVs/IVs)" else "Stats & Effort Values (EVs / IVs)"
                setTextColor(0xFF4A9EFF.toInt())
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, 12)
            }
            addView(statTitle)

            if (isGhostGrey) {
                val baseText = TextView(context).apply {
                    text = "HP: ${speciesInfo.baseHP}   |   Atk: ${speciesInfo.baseAtk}   |   Def: ${speciesInfo.baseDef}\n" +
                           "SpA: ${speciesInfo.baseSpA}   |   SpD: ${speciesInfo.baseSpD}   |   Spe: ${speciesInfo.baseSpe}\n\n" +
                           "Actual Stats: ${mon.maxHp} HP / ${mon.attack} Atk / ${mon.defense} Def / ${mon.spAttack} SpA / ${mon.spDefense} SpD / ${mon.speed} Spe"
                    setTextColor(0xFFE0E0E0.toInt())
                    textSize = 14f
                    setLineSpacing(6f, 1f)
                }
                addView(baseText)
            } else {
                val totalEv = mon.hpEv + mon.attackEv + mon.defenseEv + mon.spAttackEv + mon.spDefenseEv + mon.speedEv
                val evIvText = TextView(context).apply {
                    text = "IVs:  HP ${mon.hpIv}  |  Atk ${mon.attackIv}  |  Def ${mon.defenseIv}  |  SpA ${mon.spAttackIv}  |  SpD ${mon.spDefenseIv}  |  Spe ${mon.speedIv}\n" +
                           "EVs:  HP ${mon.hpEv}  |  Atk ${mon.attackEv}  |  Def ${mon.defenseEv}  |  SpA ${mon.spAttackEv}  |  SpD ${mon.spDefenseEv}  |  Spe ${mon.speedEv} (Total: $totalEv/510)\n" +
                           "Stats: ${mon.maxHp} HP / ${mon.attack} Atk / ${mon.defense} Def / ${mon.spAttack} SpA / ${mon.spDefense} SpD / ${mon.speed} Spe"
                    setTextColor(0xFFE0E0E0.toInt())
                    textSize = 13f
                    setLineSpacing(6f, 1f)
                }
                addView(evIvText)
            }
        }
        detailContainer.addView(statCard)

        // Moves Grid Card (4 Moves)
        val moveCard = createCardLayout().apply {
            val moveTitle = TextView(context).apply {
                text = "Known Moves"
                setTextColor(0xFF4A9EFF.toInt())
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, 12)
            }
            addView(moveTitle)

            for (i in 0 until 4) {
                val moveId = mon.moves.getOrNull(i) ?: 0
                val movePP = mon.pp.getOrNull(i) ?: 0
                val moveInfo = MoveDatabase.get(moveId)

                if (moveId > 0) {
                    val moveRow = LinearLayout(context).apply {
                        orientation = HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, 6, 0, 6)
                    }
                    val moveBadge = createTypeBadge(moveInfo.type)
                    val moveDetails = TextView(context).apply {
                        val catLabel = when (moveInfo.category) {
                            MoveCategory.PHYSICAL -> "⚔️ Phys"
                            MoveCategory.SPECIAL -> "✨ Spec"
                            MoveCategory.STATUS -> "🛡️ Status"
                        }
                        val powStr = if (moveInfo.power > 0) "${moveInfo.power} Pwr" else "--"
                        text = "  ${moveInfo.name}  ($catLabel, $powStr)  -  $movePP PP"
                        setTextColor(0xFFFFFFFF.toInt())
                        textSize = 14f
                    }
                    moveRow.addView(moveBadge)
                    moveRow.addView(moveDetails)
                    addView(moveRow)
                }
            }
        }
        detailContainer.addView(moveCard)

        // Type Defense Summary Card
        val defenseProfile = TypeChart.getDefenseProfile(
            speciesInfo.type1,
            speciesInfo.type2,
            steelResistsGhostDark = isGhostGrey
        )

        val defenseCard = createCardLayout().apply {
            val defTitle = TextView(context).apply {
                text = "Type Defenses & Weaknesses"
                setTextColor(0xFF4A9EFF.toInt())
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, 8)
            }
            addView(defTitle)

            // Weaknesses
            val weakTypes = defenseProfile.weaknesses4x + defenseProfile.weaknesses2x
            if (weakTypes.isNotEmpty()) {
                val weakRow = LinearLayout(context).apply {
                    orientation = HORIZONTAL
                    setPadding(0, 6, 0, 6)
                }
                val weakLabel = TextView(context).apply {
                    text = "⚠️ Weak (2x-4x): "
                    setTextColor(0xFFFF6B6B.toInt())
                    textSize = 13f
                    typeface = Typeface.DEFAULT_BOLD
                }
                weakRow.addView(weakLabel)
                weakTypes.forEach { weakRow.addView(createTypeBadge(it, 10f)) }
                addView(weakRow)
            }

            // Resistances
            val resTypes = defenseProfile.resistancesHalf + defenseProfile.resistancesQuarter
            if (resTypes.isNotEmpty()) {
                val resRow = LinearLayout(context).apply {
                    orientation = HORIZONTAL
                    setPadding(0, 6, 0, 6)
                }
                val resLabel = TextView(context).apply {
                    text = "🛡️ Resist (0.5x): "
                    setTextColor(0xFF50C878.toInt())
                    textSize = 13f
                    typeface = Typeface.DEFAULT_BOLD
                }
                resRow.addView(resLabel)
                resTypes.forEach { resRow.addView(createTypeBadge(it, 10f)) }
                addView(resRow)
            }

            // Immunities
            if (defenseProfile.immunities.isNotEmpty()) {
                val immRow = LinearLayout(context).apply {
                    orientation = HORIZONTAL
                    setPadding(0, 6, 0, 6)
                }
                val immLabel = TextView(context).apply {
                    text = "🚫 Immune (0x): "
                    setTextColor(0xFFAAAAAA.toInt())
                    textSize = 13f
                    typeface = Typeface.DEFAULT_BOLD
                }
                immRow.addView(immLabel)
                defenseProfile.immunities.forEach { immRow.addView(createTypeBadge(it, 10f)) }
                addView(immRow)
            }
        }
        detailContainer.addView(defenseCard)
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

    private fun createTypeBadge(type: PokemonType, textSizeSp: Float = 12f): TextView {
        return TextView(context).apply {
            text = type.displayName
            setTextColor(Color.WHITE)
            textSize = textSizeSp
            typeface = Typeface.DEFAULT_BOLD
            setPadding(18, 6, 18, 6)
            background = GradientDrawable().apply {
                cornerRadius = 16f
                setColor(type.colorHex.toInt())
            }
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 10, 0)
            }
        }
    }

    private fun getHpColor(cur: Int, max: Int): Int {
        if (max <= 0) return 0xFF50C878.toInt()
        val pct = cur.toFloat() / max.toFloat()
        return when {
            pct > 0.5f -> 0xFF50C878.toInt() // Green
            pct > 0.2f -> 0xFFFFD700.toInt() // Yellow
            else -> 0xFFFF4A4A.toInt()       // Red
        }
    }
}
