package com.dualdex.emulator

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log

class AudioDriver(private var inSampleRate: Int = 32768) {

    private var audioTrack: AudioTrack? = null
    @Volatile private var isRunning = false
    private var audioThread: Thread? = null

    fun start() {
        if (isRunning) return

        try {
            val coreRate = LibretroHost.nativeGetAudioSampleRate().toInt().takeIf { it in 8000..96000 } ?: inSampleRate
            inSampleRate = coreRate

            val canUseCoreRate = AudioTrack.getMinBufferSize(coreRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT) > 0

            val effectiveRate = if (canUseCoreRate) {
                coreRate
            } else if (AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT) > 0) {
                44100
            } else {
                48000
            }

            val minBufferSize = AudioTrack.getMinBufferSize(
                effectiveRate,
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
                        .setSampleRate(effectiveRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            isRunning = true

            val needsResample = (effectiveRate != coreRate)
            val resampleRatio = if (needsResample) coreRate.toDouble() / effectiveRate.toDouble() else 1.0

            audioThread = Thread({
                val rawBuf = ShortArray(2048)
                val resampleBuf = if (needsResample) {
                    val maxCapacity = (2048 * (effectiveRate.toDouble() / coreRate.toDouble()) + 128).toInt()
                    ShortArray(maxCapacity)
                } else rawBuf

                while (isRunning) {
                    val count = LibretroHost.nativeGetAudioSamples(rawBuf)
                    if (count > 0) {
                        try {
                            if (!needsResample) {
                                audioTrack?.write(rawBuf, 0, count)
                            } else {
                                val framesIn = count / 2
                                val framesOut = (framesIn / resampleRatio).toInt()
                                var outIdx = 0
                                for (f in 0 until framesOut) {
                                    val currentSrcFrame = f * resampleRatio
                                    val idx0 = currentSrcFrame.toInt()
                                    val frac = (currentSrcFrame - idx0).toFloat()
                                    val idx1 = minOf(idx0 + 1, framesIn - 1)

                                    val left0 = rawBuf[idx0 * 2].toFloat()
                                    val right0 = rawBuf[idx0 * 2 + 1].toFloat()
                                    val left1 = rawBuf[idx1 * 2].toFloat()
                                    val right1 = rawBuf[idx1 * 2 + 1].toFloat()

                                    val outL = (left0 + (left1 - left0) * frac).toInt().coerceIn(-32768, 32767).toShort()
                                    val outR = (right0 + (right1 - right0) * frac).toInt().coerceIn(-32768, 32767).toShort()

                                    if (outIdx + 1 < resampleBuf.size) {
                                        resampleBuf[outIdx++] = outL
                                        resampleBuf[outIdx++] = outR
                                    }
                                }
                                audioTrack?.write(resampleBuf, 0, outIdx)
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
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

            Log.i("DualDexAudio", "AudioDriver started (core=${coreRate}Hz, track=${effectiveRate}Hz, resample=$needsResample)")
        } catch (e: Exception) {
            Log.e("DualDexAudio", "Failed to start AudioDriver: ${e.message}")
        }
    }

    fun stop() {
        isRunning = false
        audioThread?.interrupt()
        audioThread?.join(500)
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
