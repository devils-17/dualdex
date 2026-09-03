package com.dualdex.companion.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.dualdex.companion.CompanionTab
import com.dualdex.companion.CompanionViewModel

class CompanionScreenView(
    context: Context,
    private val viewModel: CompanionViewModel
) : LinearLayout(context) {

    private val contentContainer: FrameLayout
    private val tabButtons = mutableMapOf<CompanionTab, TextView>()

    private val partyView: PartyScreenView by lazy { PartyScreenView(context, viewModel) }
    private val calcView: CalcTabScreenView by lazy { CalcTabScreenView(context, viewModel) }
    private val typesView: TypeChartScreenView by lazy { TypeChartScreenView(context) }
    private val assistantView: AssistantPlaceholderView by lazy { AssistantPlaceholderView(context, viewModel) }

    init {
        orientation = VERTICAL
        setBackgroundColor(0xFF101014.toInt())

        // Top Status Header (ROM Name, DualDex Brand, Battle Status)
        val headerBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(24, 16, 24, 16)
            setBackgroundColor(0xFF16161E.toInt())
        }

        val brandLabel = TextView(context).apply {
            text = "⚡ DualDex Companion"
            setTextColor(0xFF4A9EFF.toInt())
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
        }
        headerBar.addView(brandLabel)

        val spacer = View(context).apply {
            layoutParams = LayoutParams(0, 1, 1.0f)
        }
        headerBar.addView(spacer)

        val battleBadge = TextView(context).apply {
            text = "Ready"
            setTextColor(0xFF50C878.toInt())
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(14, 4, 14, 4)
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
            setPadding(8, 10, 8, 12)
            setBackgroundColor(0xFF16161E.toInt())
        }

        CompanionTab.values().forEach { tab ->
            val tabBtn = TextView(context).apply {
                text = "${tab.iconEmoji}\n${tab.title}"
                gravity = Gravity.CENTER
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(12, 6, 12, 6)
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

    fun notifyPartyUpdated() {
        post {
            if (viewModel.selectedTab.value == CompanionTab.PARTY) {
                partyView.refreshUI()
            } else if (viewModel.selectedTab.value == CompanionTab.CALC) {
                calcView.refreshUI()
            }
        }
    }
}
