package com.dualdex.companion

import android.net.Uri
import com.dualdex.emulator.LibretroHost
import com.dualdex.pokemon.ParsedPokemon
import com.dualdex.romhack.RomHackProfile
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RomItem(
    val title: String,
    val fileName: String,
    val uri: Uri,
    val sizeFormatted: String
)

enum class CompanionTab(val title: String, val iconEmoji: String) {
    HOME("Home", "🏠"),
    PARTY("Party", "👥"),
    CALC("Calc", "⚔️"),
    TYPES("Types", "🛡️"),
    DOCS("Docs", "📖"),
    SAVES("Saves", "💾"),
    ASSISTANT("Assistant", "🤖"),
    SETTINGS("Settings", "⚙️")
}

class CompanionViewModel(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val _selectedTab = MutableStateFlow(CompanionTab.HOME)
    val selectedTab: StateFlow<CompanionTab> = _selectedTab.asStateFlow()

    private val _scannedRoms = MutableStateFlow<List<RomItem>>(emptyList())
    val scannedRoms: StateFlow<List<RomItem>> = _scannedRoms.asStateFlow()

    private val _playerParty = MutableStateFlow<List<ParsedPokemon>>(emptyList())
    val playerParty: StateFlow<List<ParsedPokemon>> = _playerParty.asStateFlow()

    private val _enemyParty = MutableStateFlow<List<ParsedPokemon>>(emptyList())
    val enemyParty: StateFlow<List<ParsedPokemon>> = _enemyParty.asStateFlow()

    private val _selectedMemberIndex = MutableStateFlow(0)
    val selectedMemberIndex: StateFlow<Int> = _selectedMemberIndex.asStateFlow()

    private val _isInBattle = MutableStateFlow(false)
    val isInBattle: StateFlow<Boolean> = _isInBattle.asStateFlow()

    private val _activeGameId = MutableStateFlow(0)
    val activeGameId: StateFlow<Int> = _activeGameId.asStateFlow()

    private val _activeRomTitle = MutableStateFlow("")
    val activeRomTitle: StateFlow<String> = _activeRomTitle.asStateFlow()

    private val _activeProfile = MutableStateFlow(RomHackProfile.DEFAULT_FIRERED)
    val activeProfile: StateFlow<RomHackProfile> = _activeProfile.asStateFlow()

    private var pollingJob: Job? = null

    fun selectTab(tab: CompanionTab) {
        _selectedTab.value = tab
    }

    fun setScannedRoms(roms: List<RomItem>) {
        _scannedRoms.value = roms
    }

    fun selectMember(index: Int) {
        if (index in 0..5) {
            _selectedMemberIndex.value = index
        }
    }

    fun setRomInfo(gameId: Int, romTitle: String, profile: RomHackProfile? = null) {
        _activeGameId.value = gameId
        _activeRomTitle.value = romTitle
        if (profile != null) {
            _activeProfile.value = profile
        }
    }

    fun setProfile(profile: RomHackProfile) {
        _activeProfile.value = profile
        _activeGameId.value = profile.gameId
        _activeRomTitle.value = profile.name
    }

    fun startPolling(intervalMs: Long = 100L) {
        if (pollingJob?.isActive == true) return

        pollingJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val gameId = _activeGameId.value
                    val playerPartyRaw = LibretroHost.nativeReadPartyFromCore(gameId)
                    if (playerPartyRaw != null) {
                        val playerList = playerPartyRaw.filterNotNull().filter { !it.isEmpty && it.isValid }
                        if (playerList.isNotEmpty()) {
                            _playerParty.value = playerList
                        }
                    }

                    val enemyPartyRaw = LibretroHost.nativeReadEnemyPartyFromCore(gameId)
                    if (enemyPartyRaw != null) {
                        val enemyList = enemyPartyRaw.filterNotNull().filter { !it.isEmpty && it.isValid }
                        _enemyParty.value = enemyList
                        _isInBattle.value = enemyList.isNotEmpty()
                    }
                } catch (e: Throwable) {
                    android.util.Log.e("DualDex_Companion", "Error in memory poller: ${e.message}", e)
                }
                delay(intervalMs)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun updateManualParty(party: List<ParsedPokemon>) {
        _playerParty.value = party
    }
}
