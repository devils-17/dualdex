package com.dualdex.pokemon

object PokemonBridge {
    init {
        System.loadLibrary("dualdex_native")
    }

    /**
     * Detect game from a 16-character ROM header title.
     */
    external fun detectGame(romTitle: String): Int

    /**
     * Parse a single raw 100-byte Pokemon memory block into ParsedPokemon.
     */
    external fun parsePokemon(rawBytes: ByteArray, isPartyMon: Boolean): ParsedPokemon?

    /**
     * Read the entire player party (up to 6 Pokemon) from a 256KB EWRAM buffer.
     */
    external fun readPlayerParty(ewramBytes: ByteArray, gameId: Int): Array<ParsedPokemon>

    /**
     * Read the active enemy/opponent party from EWRAM during battle.
     */
    external fun readEnemyParty(ewramBytes: ByteArray, gameId: Int): Array<ParsedPokemon>
}
