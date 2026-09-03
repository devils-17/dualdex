package com.dualdex.companion.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*
import com.dualdex.companion.CompanionViewModel

class AssistantPlaceholderView(
    context: Context,
    private val viewModel: CompanionViewModel
) : LinearLayout(context) {

    private val responseView: TextView
    private val queryInput: EditText

    init {
        orientation = VERTICAL
        setBackgroundColor(0xFF121216.toInt())
        setPadding(24, 24, 24, 24)

        val scroll = ScrollView(context).apply { isVerticalScrollBarEnabled = true }
        val content = LinearLayout(context).apply { orientation = VERTICAL }
        scroll.addView(content)
        addView(scroll)

        // Header
        val titleView = TextView(context).apply {
            text = "🤖 DualDex ROM Hack Assistant"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 8)
        }
        val subTitle = TextView(context).apply {
            text = "Powered by Google Gemini 2.5 Flash + Google Search Grounding"
            setTextColor(0xFF4A9EFF.toInt())
            textSize = 13f
            setPadding(0, 0, 0, 16)
        }
        content.addView(titleView)
        content.addView(subTitle)

        // Input Card
        val inputCard = createCardLayout().apply {
            val label = TextView(context).apply {
                text = "Ask anything about your current ROM hack:"
                setTextColor(Color.WHITE)
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, 8)
            }
            addView(label)

            queryInput = EditText(context).apply {
                hint = "e.g. Where do I get HM02 Fly in Ghost Grey?"
                setHintTextColor(0xFF777788.toInt())
                setTextColor(Color.WHITE)
                textSize = 14f
                background = GradientDrawable().apply {
                    cornerRadius = 14f
                    setColor(0xFF16161E.toInt())
                    setStroke(2, 0xFF333345.toInt())
                }
                setPadding(18, 14, 18, 14)
            }
            addView(queryInput)

            val askBtn = Button(context).apply {
                text = "Ask Assistant 🌐"
                setTextColor(Color.WHITE)
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                background = GradientDrawable().apply {
                    cornerRadius = 16f
                    setColor(0xFF4A9EFF.toInt())
                }
                setPadding(24, 10, 24, 10)
                setOnClickListener {
                    val q = queryInput.text.toString().trim()
                    if (q.isNotEmpty()) {
                        handleSampleQuery(q)
                    }
                }
            }
            val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 12, 0, 0)
            }
            addView(askBtn, lp)
        }
        content.addView(inputCard)

        // Quick query chips
        val quickLabel = TextView(context).apply {
            text = "Suggested Questions:"
            setTextColor(0xFFAAAAAA.toInt())
            textSize = 12f
            setPadding(4, 8, 4, 8)
        }
        content.addView(quickLabel)

        val chipsRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(0, 0, 0, 16)
        }
        listOf("Where is Fly?", "Ghost Grey Evolutions", "Steel Type Changes").forEach { chipText ->
            val chip = Button(context).apply {
                text = chipText
                textSize = 11f
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    cornerRadius = 14f
                    setColor(0xFF282834.toInt())
                }
                setPadding(16, 6, 16, 6)
                setOnClickListener {
                    queryInput.setText(chipText)
                    handleSampleQuery(chipText)
                }
            }
            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 8, 0)
            }
            chipsRow.addView(chip, lp)
        }
        content.addView(chipsRow)

        // Response Card
        val resCard = createCardLayout().apply {
            val resTitle = TextView(context).apply {
                text = "Assistant Response"
                setTextColor(0xFFFFD700.toInt())
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, 8)
            }
            addView(resTitle)

            responseView = TextView(context).apply {
                text = "DualDex Assistant connects directly to Google Gemini with live web grounding to answer questions about item locations, gym leader strategies, and custom ROM hack mechanics.\n\nType a question above or tap one of the suggested prompts!"
                setTextColor(0xFFDDDDDD.toInt())
                textSize = 14f
                setLineSpacing(6f, 1f)
            }
            addView(responseView)
        }
        content.addView(resCard)
    }

    private fun handleSampleQuery(query: String) {
        val gameTitle = viewModel.activeRomTitle.value.ifEmpty { "Pokemon Ghost Grey" }
        responseView.text = "🔍 Searching Google web results for '$query' in $gameTitle...\n\n" +
                "[Phase 7 Preview]\n" +
                "Firebase AI Logic integration with live web grounding will query live PokeCommunity and Reddit discussions and display clickable source citations here."
    }

    private fun createCardLayout(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(20, 18, 20, 18)
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
