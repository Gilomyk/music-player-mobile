package com.example.musicapp.domain.player

import java.io.File

interface IAudioPlayer {
    val isPlaying: Boolean
    val duration: Int
    val currentPosition: Int

    fun setSource(file: File)
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Int)
    fun release()
    fun setOnCompletionListener(listener: (() -> Unit)?)
}
