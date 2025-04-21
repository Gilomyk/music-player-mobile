package com.example.musicapp.models

data class Playlist(
    val name: String,
    val songs: MutableList<String>
) {
    fun addSong(song: String) {
        if (!songs.contains(song)) {
            songs.add(song)
        }
    }
}