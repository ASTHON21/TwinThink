package com.example.data.audio

import android.media.MediaPlayer
import android.util.Log
import java.io.File

class LocalAudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var currentPath: String? = null

    fun playOrToggle(
        filePath: String,
        onCompletion: () -> Unit,
        onError: () -> Unit
    ): Boolean {
        val existing = mediaPlayer
        if (existing != null && currentPath == filePath) {
            return if (existing.isPlaying) {
                existing.pause()
                false
            } else {
                existing.start()
                true
            }
        }
        stop()
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                onError()
                return false
            }
            val mp = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    onCompletion()
                }
                start()
            }
            mediaPlayer = mp
            currentPath = filePath
            true
        } catch (e: Exception) {
            Log.w("LocalAudioPlayer", "Playback failed: ${e.message}")
            stop()
            onError()
            false
        }
    }

    fun currentProgress(): Float {
        val mp = mediaPlayer ?: return 0f
        return try {
            val dur = mp.duration
            if (dur <= 0) 0f else (mp.currentPosition.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
        } catch (_: Exception) {
            0f
        }
    }

    fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying == true
        } catch (_: Exception) {
            false
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
        currentPath = null
    }
}
