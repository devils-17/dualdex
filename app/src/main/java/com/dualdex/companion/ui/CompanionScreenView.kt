package com.dualdex.companion.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.dualdex.companion.CompanionTab
import com.dualdex.companion.CompanionViewModel

class CompanionScreenView(
    context: Context,
    private val viewModel: CompanionViewModel,
    private val onOpenRomRequested: (() -> Unit)? = null
) : LinearLayout(context) {

    private val contentContainer: FrameLayout
    private val tabButtons = mutableMapOf<CompanionTab, TextView>()
    private val profileLabel: TextView
    private val battleBadge: TextView

    private val partyView: PartyScreenView by lazy { PartyScreenView(context, viewModel) }
    private val calcView: CalcTabScreenView by lazy { CalcTabScreenView(context, viewModel) }
    private val typesView: TypeChartScreenView by lazy { TypeChartScreenView(context) }
    private val docsView: DocsScreenView by lazy { DocsScreenView(context, viewModel) }
    private val savesView: SaveStateScreenView by lazy { SaveStateScreenView(context, viewModel) }
    private val assistantView: com.dualdex.assistant.AssistantScreenView by lazy { com.dualdex.assistant.AssistantScreenView(context, viewModel) }

    init {
        orientation = VERTICAL
        setBackgroundColor(0xFF101014.toInt())

        // Top Status Header (ROM Name, Open ROM button, Battle Status)
        val headerBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(20, 14, 20, 14)
            setBackgroundColor(0xFF16161E.toInt())
        }

        profileLabel = TextView(context).apply {
            val prof = viewModel.activeProfile.value
            text = "⚡ ${prof.name}"
            setTextColor(0xFF4A9EFF.toInt())
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
        }
        headerBar.addView(profileLabel)

        val spacer = View(context).apply {
            layoutParams = LayoutParams(0, 1, 1.0f)
        }
        headerBar.addView(spacer)

        if (onOpenRomRequested != null) {
            val openRomBtn = Button(context).apply {
                text = "📁 Open ROM"
                textSize = 11f
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    cornerRadius = 10f
                    setColor(0xFF2B3A55.toInt())
                }
                setPadding(12, 4, 12, 4)
                setOnClickListener { onOpenRomRequested.invoke() }
            }
            val lpRom = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 10, 0)
            }
            headerBar.addView(openRomBtn, lpRom)
        }

        battleBadge = TextView(context).apply {
            text = "Ready"
            setTextColor(0xFF50C878.toInt())
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(12, 4, 12, 4)
            background = GradientDrawable().apply {
                cornerRadius = 10f
                setColor(0xFF1F2B24.toInt())
                setStroke(1, 0xFF50C878.toInt())
            }
        }
        headerBar.addView(battleBadge)
        addView(headerBar)

        // Middle Content Container
        contentContainer = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.0f)
        }
        addView(contentContainer)

        // Bottom Tab Navigation Bar
        val navBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(6, 8, 6, 10)
            setBackgroundColor(0xFF16161E.toInt())
        }

        CompanionTab.values().forEach { tab ->
            val tabBtn = TextView(context).apply {
                text = "${tab.iconEmoji}\n${tab.title}"
                gravity = Gravity.CENTER
                textSize = 10.5f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(8, 6, 8, 6)
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f)
                setOnClickListener {
                    viewModel.selectTab(tab)
                    switchTab(tab)
                }
            }
            tabButtons[tab] = tabBtn
            navBar.addView(tabBtn)
        }
        addView(navBar)

        // Initial tab
        switchTab(viewModel.selectedTab.value)
    }

    fun switchTab(tab: CompanionTab) {
        contentContainer.removeAllViews()

        val activeView: View = when (tab) {
            CompanionTab.PARTY -> {
                partyView.refreshUI()
                partyView
            }
            CompanionTab.CALC -> {
                calcView.refreshUI()
                calcView
            }
            CompanionTab.TYPES -> typesView
            CompanionTab.DOCS -> {
                docsView.refreshUI()
                docsView
            }
            CompanionTab.SAVES -> {
                savesView.refreshUI()
                savesView
            }
            CompanionTab.ASSISTANT -> assistantView
        }
        contentContainer.addView(activeView)

        tabButtons.forEach { (t, btn) ->
            val isSelected = (t == tab)
            btn.setTextColor(if (isSelected) 0xFF4A9EFF.toInt() else 0xFF888899.toInt())
            btn.background = if (isSelected) {
                GradientDrawable().apply {
                    cornerRadius = 12f
                    setColor(0xFF222B3D.toInt())
                }
            } else null
        }
    }

    fun notifyProfileChanged() {
        post {
            val prof = viewModel.activeProfile.value
            profileLabel.text = "⚡ ${prof.name}"
            if (viewModel.selectedTab.value == CompanionTab.PARTY) partyView.refreshUI()
            if (viewModel.selectedTab.value == CompanionTab.CALC) calcView.refreshUI()
            if (viewModel.selectedTab.value == CompanionTab.SAVES) savesView.refreshUI()
        }
    }

    fun notifyPartyUpdated() {
        post {
            val inBattle = viewModel.isInBattle.value
            battleBadge.text = if (inBattle) "⚔️ Battle" else "Ready"
            battleBadge.setTextColor(if (inBattle) 0xFFFF6B6B.toInt() else 0xFF50C878.toInt())

            if (viewModel.selectedTab.value == CompanionTab.PARTY) {
                partyView.refreshUI()
            } else if (viewModel.selectedTab.value == CompanionTab.CALC) {
                calcView.refreshUI()
            }
        }
    }
}
