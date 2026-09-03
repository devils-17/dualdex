package com.dualdex.emulator

import com.dualdex.pokemon.ParsedPokemon
import java.nio.ByteBuffer

object LibretroHost {
    init {
        System.loadLibrary("dualdex_native")
    }

    external fun nativeLoadCore(coreLibPath: String): Boolean
    external fun nativeLoadRom(romPath: String): Boolean
    external fun nativeStepFrame()
    external fun nativeSetInputButtons(buttonMask: Int)
    external fun nativeGetVideoFrame(directBuffer: ByteBuffer, outMetadata: IntArray): Boolean
    external fun nativeGetAudioSamples(outBuffer: ShortArray): Int
    external fun nativeReadPartyFromCore(gameId: Int): Array<ParsedPokemon>?
    external fun nativeSaveState(statePath: String): Boolean
    external fun nativeLoadState(statePath: String): Boolean
    external fun nativeCleanup()
}
