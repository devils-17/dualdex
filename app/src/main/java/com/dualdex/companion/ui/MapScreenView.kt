package com.dualdex.companion.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
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
) : LinearLayout(context) {

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

    private var observerJob: Job? = null

    init {
        orientation = VERTICAL
        setBackgroundColor(0xFF0F172A.toInt()) // Dark slate navy

        // 1. Top Header: Live Player Location Bar
        val headerBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setBackgroundColor(0xFF1E293B.toInt())
        }

        val locationTextCol = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f)
        }

        locationTitleView = TextView(context).apply {
            text = "🗺️ Loading Town Map..."
            setTextColor(0xFFF8FAFC.toInt())
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
        }
        locationTextCol.addView(locationTitleView)

        locationSubtitleView = TextView(context).apply {
            text = "Reading live EWRAM player position..."
            setTextColor(0xFF94A3B8.toInt())
            textSize = 11.5f
        }
        locationTextCol.addView(locationSubtitleView)

        headerBar.addView(locationTextCol)

        envBadgeView = TextView(context).apply {
            text = "🌲 Overworld"
            setTextColor(0xFF38BDF8.toInt())
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(8), dp(4), dp(8), dp(4))
            background = GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setColor(0xFF0F172A.toInt())
            }
        }
        headerBar.addView(envBadgeView)

        addView(headerBar)

        // 2. Action Controls Bar (Center on Player, Reset Zoom, Region Switchers)
        val controlsBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setBackgroundColor(0xFF161E2E.toInt())
        }

        val centerBtn = Button(context).apply {
            text = "🎯 Center on Player"
            textSize = 11f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(0xFF2563EB.toInt())
            }
            setPadding(dp(10), dp(4), dp(10), dp(4))
            setOnClickListener {
                regionMapView.centerOnPlayer()
            }
        }
        val centerLp = LayoutParams(LayoutParams.WRAP_CONTENT, dp(34)).apply {
            setMargins(0, 0, dp(8), 0)
        }
        controlsBar.addView(centerBtn, centerLp)

        val resetZoomBtn = Button(context).apply {
            text = "🔍 Reset Zoom"
            textSize = 11f
            setTextColor(0xFFCBD5E1.toInt())
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(0xFF334155.toInt())
            }
            setPadding(dp(10), dp(4), dp(10), dp(4))
            setOnClickListener {
                regionMapView.resetZoom()
            }
        }
        val resetLp = LayoutParams(LayoutParams.WRAP_CONTENT, dp(34)).apply {
            setMargins(0, 0, dp(8), 0)
        }
        controlsBar.addView(resetZoomBtn, resetLp)

        val spacer = View(context).apply {
            layoutParams = LayoutParams(0, 1, 1.0f)
        }
        controlsBar.addView(spacer)

        // Region Switcher buttons
        val johtoBtn = createRegionTabButton("Johto", RegionId.JOHTO)
        val kantoBtn = createRegionTabButton("Kanto", RegionId.KANTO)
        val hoennBtn = createRegionTabButton("Hoenn", RegionId.HOENN)

        controlsBar.addView(johtoBtn)
        controlsBar.addView(kantoBtn)
        controlsBar.addView(hoennBtn)

        addView(controlsBar)

        // 3. Interactive Map Canvas (Middle, weighted 1.0f)
        regionMapView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.0f)
        addView(regionMapView)

        // 4. Selected Location Detail Card (Bottom ScrollView)
        val detailScroll = ScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(135))
            setBackgroundColor(0xFF1E293B.toInt())
            setPadding(dp(14), dp(8), dp(14), dp(8))
        }

        val detailLayout = LinearLayout(context).apply {
            orientation = VERTICAL
        }

        val nameRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        detailNameView = TextView(context).apply {
            text = "New Bark Town"
            setTextColor(0xFFF8FAFC.toInt())
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f)
        }
        nameRow.addView(detailNameView)

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
        nameRow.addView(detailTypeBadgeView)
        detailLayout.addView(nameRow)

        detailDescView = TextView(context).apply {
            text = "The Town Where the Winds of a New Beginning Blow."
            setTextColor(0xFF94A3B8.toInt())
            textSize = 11.5f
            setPadding(0, dp(2), 0, dp(4))
        }
        detailLayout.addView(detailDescView)

        gymInfoView = TextView(context).apply {
            visibility = View.GONE
            setTextColor(0xFFFBBF24.toInt())
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(3))
        }
        detailLayout.addView(gymInfoView)

        poiContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(0, dp(2), 0, dp(4))
        }
        detailLayout.addView(poiContainer)

        connectionsView = TextView(context).apply {
            setTextColor(0xFF64748B.toInt())
            textSize = 10.5f
        }
        detailLayout.addView(connectionsView)

        detailScroll.addView(detailLayout)
        addView(detailScroll)

        // Initial default display
        displaySectionDetails(RegionMapDatabase.JOHTO_DEFAULT)
    }

    private fun createRegionTabButton(title: String, region: RegionId): Button {
        return Button(context).apply {
            text = title
            textSize = 10.5f
            setTextColor(0xFFCBD5E1.toInt())
            background = GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setColor(0xFF1E293B.toInt())
            }
            setPadding(dp(8), dp(2), dp(8), dp(2))
            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, dp(30)).apply {
                setMargins(dp(3), 0, 0, 0)
            }
            layoutParams = lp
            setOnClickListener {
                regionMapView.currentRegion = region
                val firstSec = RegionMapDatabase.getSections(region).firstOrNull()
                if (firstSec != null) {
                    displaySectionDetails(firstSec)
                }
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
