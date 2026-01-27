package com.example.musicapp.ui.main

import com.example.musicapp.data.playlist.PlaylistRepository
import com.example.musicapp.domain.player.IAudioPlayer
import com.example.musicapp.domain.player.PlaybackController
import com.example.musicapp.models.Playlist
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainViewModelTest {

    @Test
    fun `changing active track updates state`() {
        val testFolder = createTempDir()
        val first = createSongFile(testFolder, "first.mp3")
        val second = createSongFile(testFolder, "second.mp3")
        val viewModel = createViewModel(testFolder)

        viewModel.loadSongsForPlaylist(MainUiState.ALL_SONGS_LABEL)
        viewModel.onSongSelected(1)

        val state = viewModel.uiState.value
        assertEquals(listOf(first.name, second.name), state.songList)
        assertEquals("second.mp3", state.activeTrack)
        assertEquals(1, state.activeIndex)
    }

    @Test
    fun `play pause toggles isPlaying`() {
        val testFolder = createTempDir()
        createSongFile(testFolder, "track.mp3")
        val viewModel = createViewModel(testFolder)

        viewModel.loadSongsForPlaylist(MainUiState.ALL_SONGS_LABEL)
        viewModel.onSongSelected(0)
        assertTrue(viewModel.uiState.value.isPlaying)

        viewModel.onPlayPause()
        assertFalse(viewModel.uiState.value.isPlaying)
    }

    @Test
    fun `next and previous update active track`() {
        val testFolder = createTempDir()
        createSongFile(testFolder, "one.mp3")
        createSongFile(testFolder, "two.mp3")
        val viewModel = createViewModel(testFolder)

        viewModel.loadSongsForPlaylist(MainUiState.ALL_SONGS_LABEL)
        viewModel.onSongSelected(0)
        viewModel.onNext()

        assertEquals("two.mp3", viewModel.uiState.value.activeTrack)

        viewModel.onPrevious()
        assertEquals("one.mp3", viewModel.uiState.value.activeTrack)
    }

    private fun createViewModel(folder: File): MainViewModel {
        val audioPlayer = FakeAudioPlayer()
        val controller = PlaybackController(audioPlayer, folder)
        return MainViewModel(controller, FakePlaylistRepository(), folder)
    }

    private fun createSongFile(folder: File, name: String): File {
        val file = File(folder, name)
        file.writeText("test")
        return file
    }
}

private class FakeAudioPlayer : IAudioPlayer {
    private var currentFile: File? = null
    private var completionListener: (() -> Unit)? = null
    private var playing: Boolean = false
    private var position: Int = 0

    override val isPlaying: Boolean
        get() = playing

    override val duration: Int
        get() = if (currentFile != null) 180000 else 0

    override val currentPosition: Int
        get() = position

    override fun setSource(file: File) {
        currentFile = file
        position = 0
    }

    override fun play() {
        playing = true
    }

    override fun pause() {
        playing = false
    }

    override fun stop() {
        playing = false
        position = 0
    }

    override fun seekTo(positionMs: Int) {
        position = positionMs
    }

    override fun release() {
        playing = false
    }

    override fun setOnCompletionListener(listener: (() -> Unit)?) {
        completionListener = listener
    }

    fun triggerCompletion() {
        completionListener?.invoke()
    }
}

private class FakePlaylistRepository : PlaylistRepository {
    override fun getAllPlaylists(): List<Playlist> = emptyList()

    override fun getPlaylist(name: String): Playlist? = null

    override fun getSongsInPlaylist(name: String): List<String> = emptyList()

    override fun savePlaylist(playlist: Playlist) = Unit
}
