package com.dualdex.companion

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import android.view.ViewGroup
import com.dualdex.companion.ui.CompanionScreenView
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class CompanionPresentation(
    context: Context,
    display: Display,
    private val viewModel: CompanionViewModel,
    private val onOpenRomRequested: (() -> Unit)? = null
) : Presentation(context, display) {

    private var companionScreenView: CompanionScreenView? = null
    private val presentationScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val screen = CompanionScreenView(context, viewModel, onOpenRomRequested).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        companionScreenView = screen
        setContentView(screen)

        // Observe player party updates and notify bottom screen UI
        presentationScope.launch {
            viewModel.playerParty.collectLatest {
                companionScreenView?.notifyPartyUpdated()
            }
        }

        // Observe profile changes
        presentationScope.launch {
            viewModel.activeProfile.collectLatest {
                companionScreenView?.notifyProfileChanged()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        presentationScope.cancel()
        companionScreenView = null
    }
}
