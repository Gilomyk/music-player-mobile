package com.example.musicapp.domain.player

import java.io.File
import kotlin.random.Random

class PlaybackController(
    private val audioPlayer: IAudioPlayer,
    private val musicFolder: File,
    private val random: Random = Random.Default
) {
    private var playlist: List<String> = emptyList()
    private var currentIndex: Int = 0
    private var playMode: PlayMode = PlayMode.STOP
    private var playbackListener: ((PlaybackUpdate) -> Unit)? = null

    init {
        audioPlayer.setOnCompletionListener {
            handleCompletion()
        }
    }

    fun setPlaybackListener(listener: (PlaybackUpdate) -> Unit) {
        playbackListener = listener
    }

    fun setPlaylist(songs: List<String>, startIndex: Int = 0) {
        playlist = songs
        currentIndex = if (songs.isEmpty()) {
            -1
        } else {
            startIndex.coerceIn(0, songs.lastIndex)
        }
        emitState()
    }

    fun playAt(index: Int, autoStart: Boolean = true, seekToMs: Int? = null) {
        if (playlist.isEmpty()) {
            emitError("Playlist is empty")
            return
        }

        currentIndex = index.coerceIn(0, playlist.lastIndex)
        val song = playlist[currentIndex]
        val songFile = File(musicFolder, song)
        if (!songFile.exists()) {
            emitError("File not found: $song")
            return
        }

        audioPlayer.stop()
        audioPlayer.setSource(songFile)
        seekToMs?.let { audioPlayer.seekTo(it.coerceAtLeast(0)) }

        if (autoStart) {
            audioPlayer.play()
        } else {
            audioPlayer.pause()
        }

        emitState()
    }

    fun togglePlayPause() {
        if (audioPlayer.isPlaying) {
            audioPlayer.pause()
        } else {
            audioPlayer.play()
        }
        emitState()
    }

    fun playNext() {
        if (playlist.isEmpty()) {
            emitError("Playlist is empty")
            return
        }

        currentIndex = if (currentIndex < playlist.size - 1) {
            currentIndex + 1
        } else {
            0
        }
        playAt(currentIndex)
    }

    fun playPrevious() {
        if (playlist.isEmpty()) {
            emitError("Playlist is empty")
            return
        }

        currentIndex = if (currentIndex > 0) {
            currentIndex - 1
        } else {
            playlist.lastIndex
        }
        playAt(currentIndex)
    }

    fun changePlayMode(): PlayMode {
        playMode = playMode.next()
        emitState()
        return playMode
    }

    fun seekTo(positionMs: Int) {
        audioPlayer.seekTo(positionMs.coerceAtLeast(0))
        emitState()
    }

    fun getProgress(): ProgressInfo {
        return ProgressInfo(audioPlayer.currentPosition, audioPlayer.duration)
    }

    fun release() {
        audioPlayer.release()
    }

    private fun handleCompletion() {
        when (playMode) {
            PlayMode.STOP -> {
                playAt(currentIndex, autoStart = false, seekToMs = 0)
            }
            PlayMode.NEXT -> {
                playNext()
            }
            PlayMode.SHUFFLE -> {
                if (playlist.isEmpty()) {
                    emitError("Playlist is empty")
                    return
                }
                val availableIndices = playlist.indices.filter { it != currentIndex }
                currentIndex = if (availableIndices.isEmpty()) {
                    currentIndex
                } else {
                    availableIndices.random(random)
                }
                playAt(currentIndex)
            }
        }
    }

    private fun emitState() {
        playbackListener?.invoke(
            PlaybackUpdate(
                snapshot = PlaybackSnapshot(
                    track = playlist.getOrNull(currentIndex),
                    index = currentIndex,
                    isPlaying = audioPlayer.isPlaying,
                    durationMs = audioPlayer.duration,
                    positionMs = audioPlayer.currentPosition,
                    playMode = playMode
                ),
                errorMessage = null
            )
        )
    }

    private fun emitError(message: String) {
        playbackListener?.invoke(
            PlaybackUpdate(
                snapshot = PlaybackSnapshot(
                    track = playlist.getOrNull(currentIndex),
                    index = currentIndex,
                    isPlaying = audioPlayer.isPlaying,
                    durationMs = audioPlayer.duration,
                    positionMs = audioPlayer.currentPosition,
                    playMode = playMode
                ),
                errorMessage = message
            )
        )
    }
}

data class PlaybackSnapshot(
    val track: String?,
    val index: Int,
    val isPlaying: Boolean,
    val durationMs: Int,
    val positionMs: Int,
    val playMode: PlayMode
)

data class PlaybackUpdate(
    val snapshot: PlaybackSnapshot,
    val errorMessage: String?
)

data class ProgressInfo(
    val positionMs: Int,
    val durationMs: Int
)
