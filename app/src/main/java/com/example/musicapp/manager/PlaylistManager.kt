package com.example.musicapp.manager

import android.content.Context
import org.json.JSONObject
import java.io.File
import com.example.musicapp.models.Playlist

class PlaylistManager (private val context: Context){

    private val playlistFile = "playlists.json"

    fun savePlaylist(playlist: Playlist) {
        val playlists = loadPlaylists().toMutableMap()
        playlists[playlist.name] = playlist.songs
        val jsonObject = JSONObject((playlists as Map<*, *>?)!!)
        writeToFile(jsonObject.toString())
    }

    fun loadPlaylists(): Map<String, List<String>> {
        val playlists = mutableMapOf<String, List<String>>()
        val file = File(context.filesDir, playlistFile)
        if (!file.exists()) {
            file.writeText("{}")
            return playlists
        }
        val jsonString = file.readText()
        val jsonObject = JSONObject(jsonString)
        jsonObject.keys().forEach { key ->
            val songs = jsonObject.getJSONArray(key)
            val songList = (0 until songs.length()).map { songs.getString(it) }
            playlists[key] = songList
        }
        return playlists
    }

    fun writeToFile(data: String) {
        val file = File(context.filesDir, playlistFile)
        file.writeText(data)
    }

    fun getAllPlaylists(): List<Playlist> {
        return loadPlaylists().map { (name, songs) ->
            Playlist(name, songs.toMutableList())
        }
    }

    fun getPlaylist(name: String): Playlist? {
        return getAllPlaylists().find { it.name == name }
    }


    fun getSongsInPlaylist(playlistName: String): List<String> {
        return loadPlaylists()[playlistName] ?: emptyList()
    }
}