package com.example.musicapp.ui.main

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.musicapp.data.player.AudioPlayerImpl
import com.example.musicapp.data.playlist.PlaylistRepositoryImpl
import com.example.musicapp.domain.player.PlaybackController
import com.example.musicapp.manager.PlaylistManager
import java.io.File

class MainViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            val playlistManager = PlaylistManager(context)
            val playlistRepository = PlaylistRepositoryImpl(playlistManager)
            val audioPlayer = AudioPlayerImpl()
            val musicFolder = File(Environment.getExternalStorageDirectory(), "Moja muzyka")
            val controller = PlaybackController(audioPlayer, musicFolder)
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(controller, playlistRepository, musicFolder) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
