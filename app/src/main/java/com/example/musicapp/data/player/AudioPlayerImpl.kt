package com.example.musicapp.data.player

import android.media.MediaPlayer
import com.example.musicapp.domain.player.IAudioPlayer
import java.io.File

class AudioPlayerImpl : IAudioPlayer {
    private val mediaPlayer: MediaPlayer = MediaPlayer()
    private var completionListener: (() -> Unit)? = null

    override val isPlaying: Boolean
        get() = mediaPlayer.isPlaying

    override val duration: Int
        get() = runCatching { mediaPlayer.duration }.getOrDefault(0)

    override val currentPosition: Int
        get() = runCatching { mediaPlayer.currentPosition }.getOrDefault(0)

    override fun setSource(file: File) {
        mediaPlayer.reset()
        mediaPlayer.setDataSource(file.absolutePath)
        mediaPlayer.prepare()
        mediaPlayer.setOnCompletionListener {
            completionListener?.invoke()
        }
    }

    override fun play() {
        mediaPlayer.start()
    }

    override fun pause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }

    override fun stop() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
    }

    override fun seekTo(positionMs: Int) {
        mediaPlayer.seekTo(positionMs)
    }

    override fun release() {
        mediaPlayer.release()
    }

    override fun setOnCompletionListener(listener: (() -> Unit)?) {
        completionListener = listener
        mediaPlayer.setOnCompletionListener {
            completionListener?.invoke()
        }
    }
}
