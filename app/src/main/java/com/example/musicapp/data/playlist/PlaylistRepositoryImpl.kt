package com.example.musicapp.data.playlist

import com.example.musicapp.manager.PlaylistManager
import com.example.musicapp.models.Playlist

class PlaylistRepositoryImpl(
    private val playlistManager: PlaylistManager
) : PlaylistRepository {
    override fun getAllPlaylists(): List<Playlist> = playlistManager.getAllPlaylists()

    override fun getPlaylist(name: String): Playlist? = playlistManager.getPlaylist(name)

    override fun getSongsInPlaylist(name: String): List<String> =
        playlistManager.getSongsInPlaylist(name)

    override fun savePlaylist(playlist: Playlist) {
        playlistManager.savePlaylist(playlist)
    }
}
