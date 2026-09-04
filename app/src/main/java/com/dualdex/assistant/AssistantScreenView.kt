package com.dualdex.assistant

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.*
import com.dualdex.companion.CompanionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AssistantScreenView(
    context: Context,
    private val viewModel: CompanionViewModel
) : LinearLayout(context) {

    private val scope = CoroutineScope(Dispatchers.Main)
    private val messagesContainer = LinearLayout(context).apply {
        orientation = VERTICAL
        setPadding(0, 4, 0, 16)
    }
    private val scroll = ScrollView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.0f)
        isVerticalScrollBarEnabled = true
    }
    private val queryInput = EditText(context).apply {
        hint = "Ask item, evolution, or gym question..."
        setHintTextColor(0xFF777788.toInt())
        setTextColor(Color.WHITE)
        textSize = 13.5f
        background = GradientDrawable().apply {
            cornerRadius = 14f
            setColor(0xFF1A1A24.toInt())
            setStroke(2, 0xFF333348.toInt())
        }
        setPadding(16, 10, 16, 10)
        layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f)
    }
    private val askButton = Button(context).apply {
        text = "Ask 🌐"
        setTextColor(Color.WHITE)
        textSize = 13f
        typeface = Typeface.DEFAULT_BOLD
        background = GradientDrawable().apply {
            cornerRadius = 14f
            setColor(0xFF4A9EFF.toInt())
        }
        setPadding(18, 10, 18, 10)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            setMargins(10, 0, 0, 0)
        }
    }

    init {
        orientation = VERTICAL
        setBackgroundColor(0xFF121216.toInt())
        setPadding(20, 20, 20, 20)

        // Header
        val headerBar = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(0, 0, 0, 12)
        }

        val titleView = TextView(context).apply {
            text = "🤖 DualDex ROM Hack Assistant"
            setTextColor(Color.WHITE)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
        }
        headerBar.addView(titleView)

        val subTitle = TextView(context).apply {
            text = "Powered by Google Gemini Flash 3.8 + Google Search Grounding"
            setTextColor(0xFF4A9EFF.toInt())
            textSize = 12.5f
            setPadding(0, 2, 0, 0)
        }
        headerBar.addView(subTitle)
        addView(headerBar)

        // Quick Suggestion Chips
        val chipsScroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(0, 0, 0, 12)
        }
        val chipsRow = LinearLayout(context).apply { orientation = HORIZONTAL }

        listOf(
            "Where do I get Fly?",
            "Ghost Grey Evolutions",
            "Steel Type Changes",
            "Exp Share Location",
            "Lichtoise Stats"
        ).forEach { chipText ->
            val chip = Button(context).apply {
                text = chipText
                textSize = 11f
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    cornerRadius = 14f
                    setColor(0xFF262634.toInt())
                }
                setPadding(16, 6, 16, 6)
                setOnClickListener {
                    queryInput.setText(chipText)
                    submitQuery(chipText)
                }
            }
            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 8, 0)
            }
            chipsRow.addView(chip, lp)
        }
        chipsScroll.addView(chipsRow)
        addView(chipsScroll)

        // Messages Scroll Area
        scroll.addView(messagesContainer)
        addView(scroll)

        // Bottom Input Area
        val inputRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 8, 0, 4)
        }
        inputRow.addView(queryInput)

        askButton.setOnClickListener {
            val q = queryInput.text.toString().trim()
            if (q.isNotEmpty()) {
                submitQuery(q)
            }
        }
        inputRow.addView(askButton)
        addView(inputRow)

        // Initial welcome message
        addAssistantMessage(
            text = "Welcome to DualDex Assistant!\n\nI can answer questions about item locations, gym leader movesets, evolution changes, and custom mechanics for ${viewModel.activeProfile.value.name}.\n\nTry asking a question above!",
            citations = emptyList(),
            queries = emptyList()
        )
    }

    private fun submitQuery(question: String) {
        addUserMessage(question)
        queryInput.setText("")
        askButton.isEnabled = false
        askButton.text = "Thinking..."

        val thinkingCard = addAssistantMessage("Searching game documentation and web grounding...", emptyList(), emptyList())

        scope.launch {
            try {
                val res = RomHackAssistant.askQuestion(context, question, viewModel)
                messagesContainer.removeView(thinkingCard)
                addAssistantMessage(res.text, res.citations, res.searchQueries, res.isOfflineFallback)
            } catch (e: Exception) {
                messagesContainer.removeView(thinkingCard)
                addAssistantMessage("Error generating answer: ${e.message}", emptyList(), emptyList())
            } finally {
                askButton.isEnabled = true
                askButton.text = "Ask 🌐"
                scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
            }
        }
    }

    private fun addUserMessage(text: String) {
        val userCard = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(18, 12, 18, 12)
            gravity = Gravity.END
            background = GradientDrawable().apply {
                cornerRadius = 18f
                setColor(0xFF2B4A77.toInt())
            }
            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                this.gravity = Gravity.END
                setMargins(48, 8, 0, 8)
            }
            layoutParams = lp
        }

        val msgView = TextView(context).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 14f
        }
        userCard.addView(msgView)
        messagesContainer.addView(userCard)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun addAssistantMessage(
        text: String,
        citations: List<WebCitation>,
        queries: List<String>,
        isOffline: Boolean = false
    ): View {
        val card = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(20, 16, 20, 16)
            background = GradientDrawable().apply {
                cornerRadius = 18f
                setColor(0xFF1E1E26.toInt())
            }
            val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 8, 32, 8)
            }
            layoutParams = lp
        }

        // Queries executed banner
        if (queries.isNotEmpty()) {
            val qText = TextView(context).apply {
                this.text = "🔍 Google Search: " + queries.joinToString(", ")
                setTextColor(0xFF4A9EFF.toInt())
                textSize = 11.5f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, 8)
            }
            card.addView(qText)
        }

        // Body
        val msgView = TextView(context).apply {
            this.text = text
            setTextColor(0xFFE8E8E8.toInt())
            textSize = 13.5f
            setLineSpacing(5f, 1f)
        }
        card.addView(msgView)

        // Web Grounding Citations (Google API TOS Compliance)
        if (citations.isNotEmpty()) {
            val citeHeader = TextView(context).apply {
                this.text = "\nWeb Sources & Citations:"
                setTextColor(0xFFFFD700.toInt())
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
            }
            card.addView(citeHeader)

            citations.forEach { citation ->
                val citeBtn = TextView(context).apply {
                    this.text = "🔗 ${citation.title}"
                    setTextColor(0xFF50C878.toInt())
                    textSize = 12f
                    setPadding(0, 4, 0, 4)
                    setOnClickListener {
                        try {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(citation.url))
                            context.startActivity(browserIntent)
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                }
                card.addView(citeBtn)
            }
        }

        if (isOffline) {
            val badge = TextView(context).apply {
                this.text = "ℹ️ Offline Knowledge Base"
                setTextColor(0xFF888899.toInt())
                textSize = 10.5f
                setPadding(0, 6, 0, 0)
            }
            card.addView(badge)
        }

        messagesContainer.addView(card)
        return card
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
    }
}
