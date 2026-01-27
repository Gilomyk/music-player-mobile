package com.example.musicapp.data.playlist

import com.example.musicapp.models.Playlist

interface PlaylistRepository {
    fun getAllPlaylists(): List<Playlist>
    fun getPlaylist(name: String): Playlist?
    fun getSongsInPlaylist(name: String): List<String>
    fun savePlaylist(playlist: Playlist)
}
