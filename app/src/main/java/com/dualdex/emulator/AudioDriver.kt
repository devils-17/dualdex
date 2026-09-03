package com.dualdex.emulator

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log

class AudioDriver(private val sampleRate: Int = 32768) {

    private var audioTrack: AudioTrack? = null
    private var isRunning = false
    private var audioThread: Thread? = null

    fun start() {
        if (isRunning) return

        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBufferSize, 4096)

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            isRunning = true

            audioThread = Thread({
                val sampleBuf = ShortArray(2048)
                while (isRunning) {
                    val count = LibretroHost.nativeGetAudioSamples(sampleBuf)
                    if (count > 0) {
                        audioTrack?.write(sampleBuf, 0, count)
                    } else {
                        try {
                            Thread.sleep(4)
                        } catch (e: InterruptedException) {
                            break
                        }
                    }
                }
            }, "DualDexAudioThread").apply {
                priority = Thread.MAX_PRIORITY
                start()
            }

            Log.i("DualDexAudio", "AudioDriver started at ${sampleRate}Hz")
        } catch (e: Exception) {
            Log.e("DualDexAudio", "Failed to start AudioDriver: ${e.message}")
        }
    }

    fun stop() {
        isRunning = false
        audioThread?.interrupt()
        audioThread = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // ignore
        }
        audioTrack = null
        Log.i("DualDexAudio", "AudioDriver stopped")
    }
}
