package com.example.musicapp.ui.main

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.data.playlist.PlaylistRepository
import com.example.musicapp.domain.player.PlayMode
import com.example.musicapp.domain.player.PlaybackController
import com.example.musicapp.domain.player.PlaybackUpdate
import com.example.musicapp.domain.player.ProgressInfo
import com.example.musicapp.models.Playlist
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val playbackController: PlaybackController,
    private val playlistRepository: PlaylistRepository,
    private val musicFolder: File = File(Environment.getExternalStorageDirectory(), "Moja muzyka"),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    private var progressJob: Job? = null
    private var wasPlayingBeforeSeek = false

    init {
        playbackController.setPlaybackListener(::handlePlaybackUpdate)
        refreshPlaylists()
        loadSongsForPlaylist(MainUiState.ALL_SONGS_LABEL)
    }

    fun refreshPlaylists() {
        val playlistNames = mutableListOf(MainUiState.ALL_SONGS_LABEL)
        playlistNames.addAll(playlistRepository.getAllPlaylists().map { it.name })
        _uiState.update { it.copy(playlistNames = playlistNames) }
    }

    fun loadSongsForPlaylist(playlistName: String) {
        val songs = if (playlistName == MainUiState.ALL_SONGS_LABEL) {
            getAllSongsFromFolder()
        } else {
            playlistRepository.getSongsInPlaylist(playlistName)
        }

        val currentIndex = _uiState.value.activeIndex
        val newIndex = if (songs.isEmpty()) {
            -1
        } else if (currentIndex in songs.indices) {
            currentIndex
        } else {
            0
        }

        playbackController.setPlaylist(songs, newIndex.coerceAtLeast(0))
        _uiState.update {
            it.copy(
                songList = songs,
                activeIndex = newIndex,
                activeTrack = songs.getOrNull(newIndex)
            )
        }
    }

    fun onSongSelected(index: Int) {
        playbackController.playAt(index)
    }

    fun onPlayPause() {
        playbackController.togglePlayPause()
    }

    fun onNext() {
        playbackController.playNext()
    }

    fun onPrevious() {
        playbackController.playPrevious()
    }

    fun onChangePlayMode() {
        playbackController.changePlayMode()
    }

    fun onSeekStarted() {
        val isPlaying = _uiState.value.isPlaying
        if (isPlaying) {
            wasPlayingBeforeSeek = true
            playbackController.togglePlayPause()
        } else {
            wasPlayingBeforeSeek = false
        }
    }

    fun onSeekChanged(positionMs: Int) {
        playbackController.seekTo(positionMs)
    }

    fun onSeekStopped() {
        if (wasPlayingBeforeSeek) {
            playbackController.togglePlayPause()
        }
        wasPlayingBeforeSeek = false
    }

    fun addSelectedSongsToPlaylist(playlist: Playlist, selectedSongs: List<String>) {
        selectedSongs.forEach { song ->
            playlist.addSong(song)
        }
        playlistRepository.savePlaylist(playlist)
    }

    fun getAllPlaylists(): List<Playlist> = playlistRepository.getAllPlaylists()

    fun getAllSongs(): List<String> = getAllSongsFromFolder()

    fun createPlaylist(name: String, songs: List<String>): Playlist? {
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Nazwa playlisty nie może być pusta!") }
            return null
        }
        if (playlistRepository.getPlaylist(name) != null) {
            _uiState.update { it.copy(errorMessage = "Playlista \"$name\" już istnieje!") }
            return null
        }
        val playlist = Playlist(name, songs.toMutableList())
        playlistRepository.savePlaylist(playlist)
        refreshPlaylists()
        return playlist
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun savePlaybackState(): PlaybackSaveState? {
        val state = _uiState.value
        return if (state.activeIndex >= 0) {
            PlaybackSaveState(
                activeIndex = state.activeIndex,
                isPlaying = state.isPlaying,
                progressMs = state.progressMs
            )
        } else {
            null
        }
    }

    fun restorePlaybackState(savedState: PlaybackSaveState) {
        if (savedState.activeIndex >= 0) {
            playbackController.playAt(
                savedState.activeIndex,
                autoStart = savedState.isPlaying,
                seekToMs = savedState.progressMs
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        playbackController.release()
    }

    private fun handlePlaybackUpdate(update: PlaybackUpdate) {
        val snapshot = update.snapshot
        _uiState.update {
            it.copy(
                activeTrack = snapshot.track,
                activeIndex = snapshot.index,
                isPlaying = snapshot.isPlaying,
                durationMs = snapshot.durationMs,
                progressMs = snapshot.positionMs,
                playMode = snapshot.playMode,
                errorMessage = update.errorMessage
            )
        }
        if (snapshot.isPlaying) {
            startProgressUpdates()
        } else {
            progressJob?.cancel()
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch(dispatcher) {
            while (_uiState.value.isPlaying) {
                val progress = playbackController.getProgress()
                updateProgress(progress)
                delay(PROGRESS_UPDATE_MS)
            }
        }
    }

    private fun updateProgress(progress: ProgressInfo) {
        _uiState.update {
            it.copy(
                progressMs = progress.positionMs,
                durationMs = progress.durationMs
            )
        }
    }

    private fun getAllSongsFromFolder(): List<String> {
        val songs = mutableListOf<String>()
        if (musicFolder.exists() && musicFolder.isDirectory) {
            val files = musicFolder.listFiles { _, name ->
                name.endsWith(".mp3") || name.endsWith(".wav")
            }
            files?.forEach { file ->
                songs.add(file.name)
            }
        }
        return songs
    }

    companion object {
        private const val PROGRESS_UPDATE_MS = 1000L
    }
}

data class MainUiState(
    val songList: List<String> = emptyList(),
    val playlistNames: List<String> = listOf(ALL_SONGS_LABEL),
    val activeTrack: String? = null,
    val activeIndex: Int = -1,
    val isPlaying: Boolean = false,
    val progressMs: Int = 0,
    val durationMs: Int = 0,
    val playMode: PlayMode = PlayMode.STOP,
    val errorMessage: String? = null
) {
    companion object {
        const val ALL_SONGS_LABEL = "All Songs"
    }
}

data class PlaybackSaveState(
    val activeIndex: Int,
    val isPlaying: Boolean,
    val progressMs: Int
)
