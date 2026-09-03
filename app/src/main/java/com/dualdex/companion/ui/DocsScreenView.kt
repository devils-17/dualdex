package com.dualdex.companion.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.dualdex.companion.CompanionViewModel

@SuppressLint("SetJavaScriptEnabled")
class DocsScreenView(
    context: Context,
    private val viewModel: CompanionViewModel
) : LinearLayout(context) {

    private val webView: WebView
    private val offlineGuideContainer: ScrollView
    private val urlHeader: TextView

    init {
        orientation = VERTICAL
        setBackgroundColor(0xFF101014.toInt())

        // Top URL Bar
        val topBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(16, 10, 16, 10)
            setBackgroundColor(0xFF16161E.toInt())
        }

        urlHeader = TextView(context).apply {
            setTextColor(0xFF4A9EFF.toInt())
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f)
        }
        topBar.addView(urlHeader)

        val refreshBtn = Button(context).apply {
            text = "🔄 Reload"
            textSize = 11f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = 10f
                setColor(0xFF2E2E3C.toInt())
            }
            setPadding(12, 4, 12, 4)
            setOnClickListener { refreshUI() }
        }
        topBar.addView(refreshBtn)
        addView(topBar)

        // WebView
        webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        addView(webView)

        // Offline Guide Fallback (used for hacks like Ghost Grey with no web dex)
        offlineGuideContainer = ScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            setPadding(24, 24, 24, 24)
            visibility = View.GONE
        }
        addView(offlineGuideContainer)

        refreshUI()
    }

    fun refreshUI() {
        val profile = viewModel.activeProfile.value
        val docsUrl = profile.docsUrl

        if (!docsUrl.isNullOrBlank()) {
            urlHeader.text = "🌐 ${profile.name} Web Dex ($docsUrl)"
            webView.visibility = View.VISIBLE
            offlineGuideContainer.visibility = View.GONE
            webView.loadUrl(docsUrl)
        } else {
            urlHeader.text = "📖 ${profile.name} In-Game Documentation"
            webView.visibility = View.GONE
            offlineGuideContainer.visibility = View.VISIBLE
            buildOfflineGuide()
        }
    }

    private fun buildOfflineGuide() {
        val profile = viewModel.activeProfile.value
        offlineGuideContainer.removeAllViews()

        val content = LinearLayout(context).apply {
            orientation = VERTICAL
        }

        val card = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(24, 20, 24, 20)
            background = GradientDrawable().apply {
                cornerRadius = 20f
                setColor(0xFF1A1A24.toInt())
            }
        }

        val title = TextView(context).apply {
            text = "${profile.name} — ROM Hack Reference"
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 12)
        }
        card.addView(title)

        val body = TextView(context).apply {
            text = "• Base Game: ${profile.baseGame}\n" +
                   "• Engine: ${profile.engine}\n" +
                   "• Developer: ${profile.developer.ifEmpty { "Community" }}\n\n" +
                   "--- Key Mechanics ---\n" +
                   "• Effort Values (EVs): ${if (profile.hasEvs) "Enabled" else "REMOVED (Flat stat system)"}\n" +
                   "• Individual Values (IVs): ${if (profile.hasIvs) "Enabled" else "REMOVED (All Pokemon equal IVs)"}\n" +
                   "• Physical / Special Split: ${if (profile.hasPhysSpecSplit) "Enabled (Move-specific categories)" else "Type-based (Vanilla Gen 3)"}\n" +
                   "• Type Chart: ${if (profile.steelResistsGhostDark) "Pre-Gen 6 (Steel resists Ghost and Dark)" else "Modern Gen 6+"}\n\n" +
                   "--- Notable Regional Variants ---\n" +
                   "• Lichtoise (#500): Water / Ghost (Base: 79/63/100/85/105/78)\n" +
                   "• Spectrasaur (#501): Grass / Ghost (Base: 80/82/83/100/100/80)\n" +
                   "• Phantomander (#502): Fire / Ghost (Base: 78/84/78/109/85/100)\n\n" +
                   "Tip: Use the DualDex Assistant tab to ask specific item or encounter questions!"
            setTextColor(0xFFCCCCCC.toInt())
            textSize = 14f
            setLineSpacing(6f, 1f)
        }
        card.addView(body)
        content.addView(card)
        offlineGuideContainer.addView(content)
    }
}
