package com.studypulse.pro.core

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator

object Alarm {
    private var mp: MediaPlayer? = null
    private var vib: Vibrator? = null
    private val h = Handler(Looper.getMainLooper())

    fun start(c: Context, seconds: Int = 25) {
        stop()
        val ctx = c.applicationContext
        runCatching {
            val am = ctx.getSystemService(AudioManager::class.java)
            am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
        }
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
                )
                setDataSource(ctx, uri)
                isLooping = true
                prepare()
                start()
            }
        }
        runCatching {
            vib = ctx.getSystemService(Vibrator::class.java).also {
                it.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 400), 0))
            }
        }
        h.postDelayed({ stop() }, seconds * 1000L)
    }

    fun buzz(c: Context) {
        runCatching {
            c.getSystemService(Vibrator::class.java)
                .vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    fun stop() {
        h.removeCallbacksAndMessages(null)
        mp?.let { runCatching { it.stop() }; runCatching { it.release() } }
        mp = null
        runCatching { vib?.cancel() }
        vib = null
    }
}
