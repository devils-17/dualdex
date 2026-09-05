package com.dualdex.companion.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.dualdex.companion.CompanionViewModel
import com.dualdex.pokemon.MapNodeType
import com.dualdex.pokemon.PlayerLocation
import com.dualdex.pokemon.RegionId
import com.dualdex.pokemon.RegionMapDatabase
import com.dualdex.pokemon.RegionMapSection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MapScreenView(
    context: Context,
    private val viewModel: CompanionViewModel
) : FrameLayout(context) {

    private val density = context.resources.displayMetrics.density
    private fun dp(v: Int): Int = (v * density).toInt()

    private val locationTitleView: TextView
    private val locationSubtitleView: TextView
    private val envBadgeView: TextView

    private val regionMapView: RegionMapView = RegionMapView(context).apply {
        onSectionSelected = { section ->
            displaySectionDetails(section)
        }
    }

    // Detail card views
    private val detailNameView: TextView
    private val detailTypeBadgeView: TextView
    private val detailDescView: TextView
    private val gymInfoView: TextView
    private val poiContainer: LinearLayout
    private val connectionsView: TextView
    private val expandToggleView: TextView
    private val expandableContent: LinearLayout
    private var isSheetExpanded: Boolean = false

    private val regionButtons = mutableMapOf<RegionId, Button>()
    private var observerJob: Job? = null

    init {
        setBackgroundColor(0xFF0F172A.toInt()) // Dark slate navy

        // 1. Full-bleed interactive map canvas (fills entire container)
        regionMapView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(regionMapView)

        // 2. Top Floating Control Pill (compact translucent card over map)
        val topPill = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(0xEE162032.toInt())
                setStroke(dp(1), 0x99334155.toInt())
            }
        }
        val topLp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP
            setMargins(dp(8), dp(8), dp(8), 0)
        }
        addView(topPill, topLp)

        // Top Row: Location Title, Environment Badge, Quick Action Buttons
        val topRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        locationTitleView = TextView(context).apply {
            text = "🗺️ Loading Town Map..."
            setTextColor(0xFFF8FAFC.toInt())
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f)
        }
        topRow.addView(locationTitleView)

        envBadgeView = TextView(context).apply {
            text = "🌲 Overworld"
            setTextColor(0xFF38BDF8.toInt())
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(6), dp(3), dp(6), dp(3))
            background = GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setColor(0xFF0F172A.toInt())
            }
        }
        val envLp = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            setMargins(dp(6), 0, dp(6), 0)
        }
        topRow.addView(envBadgeView, envLp)

        val centerBtn = Button(context).apply {
            text = "🎯"
            textSize = 12f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(0xFF2563EB.toInt())
            }
            setPadding(dp(8), dp(2), dp(8), dp(2))
            setOnClickListener { regionMapView.centerOnPlayer() }
        }
        val centerLp = LinearLayout.LayoutParams(dp(36), dp(30)).apply {
            setMargins(0, 0, dp(4), 0)
        }
        topRow.addView(centerBtn, centerLp)

        val resetZoomBtn = Button(context).apply {
            text = "🔍"
            textSize = 12f
            setTextColor(0xFFCBD5E1.toInt())
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(0xFF334155.toInt())
            }
            setPadding(dp(8), dp(2), dp(8), dp(2))
            setOnClickListener { regionMapView.resetZoom() }
        }
        val resetLp = LinearLayout.LayoutParams(dp(36), dp(30))
        topRow.addView(resetZoomBtn, resetLp)

        topPill.addView(topRow)

        // Sub Row: Coordinates / Map Subtitle & Region Selector Buttons
        val subRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, 0)
        }

        locationSubtitleView = TextView(context).apply {
            text = "Reading live EWRAM player position..."
            setTextColor(0xFF94A3B8.toInt())
            textSize = 10.5f
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f)
        }
        subRow.addView(locationSubtitleView)

        val johtoBtn = createRegionTabButton("Johto", RegionId.JOHTO)
        val kantoBtn = createRegionTabButton("Kanto", RegionId.KANTO)
        val hoennBtn = createRegionTabButton("Hoenn", RegionId.HOENN)
        subRow.addView(johtoBtn)
        subRow.addView(kantoBtn)
        subRow.addView(hoennBtn)

        topPill.addView(subRow)

        // 3. Bottom Floating Collapsible Detail Sheet
        val bottomCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(0xF21E293B.toInt())
                setStroke(dp(1), 0x99334155.toInt())
            }
        }
        val bottomLp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM
            setMargins(dp(8), 0, dp(8), dp(8))
        }
        addView(bottomCard, bottomLp)

        // Always-visible Header Row (tap to expand/collapse)
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            setPadding(dp(4), dp(2), dp(4), dp(2))
            setOnClickListener {
                toggleSheetExpansion()
            }
        }

        detailNameView = TextView(context).apply {
            text = "New Bark Town"
            setTextColor(0xFFF8FAFC.toInt())
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f)
        }
        headerRow.addView(detailNameView)

        detailTypeBadgeView = TextView(context).apply {
            text = "Town"
            setTextColor(0xFF10B981.toInt())
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(6), dp(2), dp(6), dp(2))
            background = GradientDrawable().apply {
                cornerRadius = dp(4).toFloat()
                setColor(0xFF0F172A.toInt())
            }
        }
        val badgeLp = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 0, dp(10), 0)
        }
        headerRow.addView(detailTypeBadgeView, badgeLp)

        expandToggleView = TextView(context).apply {
            text = "▲ Details"
            setTextColor(0xFF38BDF8.toInt())
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(6), dp(2), dp(6), dp(2))
        }
        headerRow.addView(expandToggleView)
        bottomCard.addView(headerRow)

        // Expandable Content Body (collapsed by default to save 90% of screen height)
        expandableContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dp(4), dp(6), dp(4), dp(2))
        }

        detailDescView = TextView(context).apply {
            text = "The Town Where the Winds of a New Beginning Blow."
            setTextColor(0xFFCBD5E1.toInt())
            textSize = 11.5f
            setPadding(0, 0, 0, dp(4))
        }
        expandableContent.addView(detailDescView)

        gymInfoView = TextView(context).apply {
            visibility = View.GONE
            setTextColor(0xFFFBBF24.toInt())
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(3))
        }
        expandableContent.addView(gymInfoView)

        poiContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(3))
        }
        expandableContent.addView(poiContainer)

        connectionsView = TextView(context).apply {
            setTextColor(0xFF94A3B8.toInt())
            textSize = 10.5f
        }
        expandableContent.addView(connectionsView)

        bottomCard.addView(expandableContent)

        // Initial default display
        displaySectionDetails(RegionMapDatabase.JOHTO_DEFAULT)
    }

    private fun toggleSheetExpansion() {
        isSheetExpanded = !isSheetExpanded
        expandableContent.visibility = if (isSheetExpanded) View.VISIBLE else View.GONE
        expandToggleView.text = if (isSheetExpanded) "▼ Hide" else "▲ Details"
    }

    private fun createRegionTabButton(title: String, region: RegionId): Button {
        val btn = Button(context).apply {
            text = title
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (region == RegionId.JOHTO) Color.WHITE else 0xFF94A3B8.toInt())
            background = GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setColor(if (region == RegionId.JOHTO) 0xFF2563EB.toInt() else 0xFF1E293B.toInt())
            }
            setPadding(dp(6), dp(1), dp(6), dp(1))
            val lp = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, dp(26)).apply {
                setMargins(dp(3), 0, 0, 0)
            }
            layoutParams = lp
            setOnClickListener {
                regionMapView.currentRegion = region
                updateRegionTabStyles(region)
                val firstSec = RegionMapDatabase.getSections(region).firstOrNull()
                if (firstSec != null) {
                    displaySectionDetails(firstSec)
                }
            }
        }
        regionButtons[region] = btn
        return btn
    }

    private fun updateRegionTabStyles(activeRegion: RegionId) {
        regionButtons.forEach { (reg, btn) ->
            val isAct = (reg == activeRegion)
            btn.setTextColor(if (isAct) Color.WHITE else 0xFF94A3B8.toInt())
            btn.background = GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setColor(if (isAct) 0xFF2563EB.toInt() else 0xFF1E293B.toInt())
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startObserving()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        observerJob?.cancel()
        observerJob = null
    }

    private fun startObserving() {
        observerJob?.cancel()
        observerJob = CoroutineScope(Dispatchers.Main).launch {
            launch {
                viewModel.playerLocation.collectLatest { loc ->
                    updatePlayerLocation(loc)
                }
            }
            launch {
                viewModel.resolvedLocation.collectLatest { sec ->
                    // Auto select the player's current location on first load
                    if (regionMapView.selectedSection == null) {
                        displaySectionDetails(sec)
                        regionMapView.selectedSection = sec
                    }
                }
            }
        }
    }

    fun refreshUI() {
        val prof = viewModel.activeProfile.value
        val region = when (prof.gameId) {
            1 -> if (prof.id == "heart_and_soul" || prof.name.contains("Heart", ignoreCase = true)) RegionId.JOHTO else RegionId.HOENN
            2 -> RegionId.KANTO
            else -> RegionId.JOHTO
        }
        regionMapView.currentRegion = region
        updateRegionTabStyles(region)
        updatePlayerLocation(viewModel.playerLocation.value)
    }

    private fun updatePlayerLocation(loc: PlayerLocation?) {
        regionMapView.playerLocation = loc

        if (loc == null || !loc.isValid) {
            locationTitleView.text = "📍 Waiting for player..."
            locationSubtitleView.text = "In-game location will appear once the ROM loads"
            envBadgeView.text = "❓ Unknown"
            return
        }

        val isHns = (regionMapView.currentRegion == RegionId.JOHTO)
        val sec = RegionMapDatabase.resolveLocation(if (isHns) 1 else 2, isHns, loc)

        locationTitleView.text = "📍 ${sec.name}"
        locationSubtitleView.text = "${sec.region.displayName} • Map Tile (${loc.localX}, ${loc.localY}) • SB1 Group ${loc.mapGroup} Num ${loc.mapNum}"

        if (loc.isIndoors) {
            envBadgeView.text = "🏡 Indoors"
            envBadgeView.setTextColor(0xFFFBBF24.toInt()) // Amber
        } else if (sec.nodeType == MapNodeType.DUNGEON) {
            envBadgeView.text = "⛰️ Cave / Dungeon"
            envBadgeView.setTextColor(0xFFA78BFA.toInt()) // Purple
        } else {
            envBadgeView.text = "🌲 Overworld"
            envBadgeView.setTextColor(0xFF38BDF8.toInt()) // Cyan
        }
    }

    private fun displaySectionDetails(sec: RegionMapSection) {
        detailNameView.text = sec.name
        detailTypeBadgeView.text = sec.nodeType.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }

        val badgeColor = when (sec.nodeType) {
            MapNodeType.CITY -> 0xFF3B82F6.toInt()
            MapNodeType.TOWN -> 0xFF10B981.toInt()
            MapNodeType.DUNGEON -> 0xFF8B5CF6.toInt()
            MapNodeType.FACILITY -> 0xFFF59E0B.toInt()
            else -> 0xFF94A3B8.toInt()
        }
        detailTypeBadgeView.setTextColor(badgeColor)

        detailDescView.text = if (sec.description.isNotEmpty()) sec.description else "A serene location in the ${sec.region.displayName} region."

        if (sec.gymLeader != null) {
            gymInfoView.visibility = View.VISIBLE
            gymInfoView.text = "🏆 Gym: ${sec.gymLeader} • ${sec.badge ?: "Badge"}"
        } else {
            gymInfoView.visibility = View.GONE
        }

        poiContainer.removeAllViews()
        if (sec.landmarks.isNotEmpty()) {
            sec.landmarks.take(3).forEach { poi ->
                val poiChip = TextView(context).apply {
                    text = "• $poi"
                    setTextColor(0xFFCBD5E1.toInt())
                    textSize = 10.5f
                    setPadding(0, 0, dp(10), 0)
                }
                poiContainer.addView(poiChip)
            }
        }

        if (sec.connections.isNotEmpty()) {
            connectionsView.visibility = View.VISIBLE
            connectionsView.text = "🔗 Connected to: ${sec.connections.joinToString(", ")}"
        } else {
            connectionsView.visibility = View.GONE
        }
    }
}
