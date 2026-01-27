package com.example.musicapp.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ListView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.musicapp.R
import com.example.musicapp.models.Playlist
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(this)
    }

    // Lista
    private lateinit var songListView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val songList: MutableList<String> = mutableListOf()
    private var selectedSongs = mutableListOf<String>()
    private var isSelectionMode = false
    private var currentSongIndex: Int = -1

    // Guziki
    private lateinit var playPauseButton: Button
    private lateinit var nextButton: Button
    private lateinit var prevButton: Button
    private lateinit var playModeButton: Button
    private lateinit var addPlaylistButton: Button
    private lateinit var addToButton: Button

    // Pasek i czas
    private lateinit var progressBar: SeekBar
    private lateinit var currentTimeText: TextView
    private lateinit var totalTimeText: TextView
    private var isTouchingSeekBar = false
    private var currentDurationMs: Int = 0

    private lateinit var spinner: Spinner
    private lateinit var spinnerAdapter: ArrayAdapter<String>

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.savePlaybackState()?.let { state ->
            outState.putInt("currentSongIndex", state.activeIndex)
            outState.putBoolean("isPlaying", state.isPlaying)
            outState.putInt("currentPosition", state.progressMs)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val currentSongIndex = savedInstanceState.getInt("currentSongIndex", -1)
        val isPlaying = savedInstanceState.getBoolean("isPlaying", false)
        val currentPosition = savedInstanceState.getInt("currentPosition", 0)

        if (currentSongIndex >= 0) {
            viewModel.restorePlaybackState(
                PlaybackSaveState(
                    activeIndex = currentSongIndex,
                    isPlaying = isPlaying,
                    progressMs = currentPosition
                )
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicjalizacja widoków
        songListView = findViewById(R.id.songListView)
        playPauseButton = findViewById(R.id.playPauseButton)
        nextButton = findViewById(R.id.nextButton)
        prevButton = findViewById(R.id.prevButton)
        playModeButton = findViewById(R.id.playModeButton)
        progressBar = findViewById(R.id.progressBar)
        currentTimeText = findViewById(R.id.currentTime)
        totalTimeText = findViewById(R.id.totalTime)
        addPlaylistButton = findViewById(R.id.addPlaylistButton)
        addToButton = findViewById(R.id.addToButton)
        spinner = findViewById(R.id.playlistSpinner)

        // Obsługa przycisków
        playPauseButton.setOnClickListener { viewModel.onPlayPause() }
        nextButton.setOnClickListener { viewModel.onNext() }
        prevButton.setOnClickListener { viewModel.onPrevious() }
        playModeButton.setOnClickListener { viewModel.onChangePlayMode() }
        addPlaylistButton.setOnClickListener { showNewPlaylistDialog() }

        progressBar.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isTouchingSeekBar = true
                    viewModel.onSeekStarted()
                    true
                }

                MotionEvent.ACTION_UP -> {
                    isTouchingSeekBar = false
                    viewModel.onSeekStopped()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (currentDurationMs > 0) {
                        val seekTime = (currentDurationMs * event.x / progressBar.width)
                            .toInt()
                            .coerceIn(0, currentDurationMs)
                        viewModel.onSeekChanged(seekTime)
                    }
                    true
                }

                else -> false
            }
        }

        addToButton.setOnClickListener {
            val playlists = viewModel.getAllPlaylists()
            val playlistNames = playlists.map { it.name }

            val dialog = AlertDialog.Builder(this)
                .setTitle("Wybierz playlistę")
                .setItems(playlistNames.toTypedArray()) { _, which ->
                    playlists.getOrNull(which)?.let { playlist ->
                        addSelectedSongsToPlaylist(playlist)
                    }
                }
                .create()

            dialog.show()
        }

        adapter = object : ArrayAdapter<String>(this, R.layout.list_item_song, R.id.songTitle, songList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: layoutInflater.inflate(R.layout.list_item_song, parent, false)

                val textView = view.findViewById<TextView>(R.id.songTitle)
                val checkBox = view.findViewById<CheckBox>(R.id.songCheckBox)

                textView.text = songList[position]

                // Czy element zaznaczony w trybie wyboru
                if (isSelectionMode) {
                    checkBox.setOnCheckedChangeListener(null)
                    checkBox.visibility = View.VISIBLE
                    checkBox.isChecked = selectedSongs.contains(songList[position])
                } else {
                    checkBox.visibility = View.GONE
                }

                // Pogrubienie tekstu odtwarzanej piosenki
                if (position == currentSongIndex) {
                    textView.setTypeface(null, Typeface.BOLD)
                } else {
                    textView.setTypeface(null, Typeface.NORMAL)
                }

                // Obsługa kliknięcia na checkbox
                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedSongs.add(songList[position])
                    } else {
                        selectedSongs.remove(songList[position])
                    }

                    if (selectedSongs.isEmpty()) {
                        isSelectionMode = false
                        updateListView()
                    }
                }

                return view
            }
        }
        songListView.adapter = adapter

        songListView.setOnItemClickListener { _, _, position, _ ->
            viewModel.onSongSelected(position)
        }

        songListView.setOnItemLongClickListener { _, _, position, _ ->
            isSelectionMode = true
            updateListView()
            selectedSongs.add(songList[position])
            true
        }

        spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf())
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedName = spinnerAdapter.getItem(position) ?: return
                viewModel.loadSongsForPlaylist(selectedName)
                selectedSongs.clear()
                isSelectionMode = false
                updateListView()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Sprawdzenie uprawnień
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                1
            )
        }

        observeState()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    songList.clear()
                    songList.addAll(state.songList)
                    adapter.notifyDataSetChanged()

                    currentSongIndex = state.activeIndex
                    currentDurationMs = state.durationMs

                    val playModeText = when (state.playMode) {
                        com.example.musicapp.domain.player.PlayMode.STOP -> "Stop"
                        com.example.musicapp.domain.player.PlayMode.NEXT -> "Next"
                        com.example.musicapp.domain.player.PlayMode.SHUFFLE -> "Shuffle"
                    }
                    playModeButton.text = "Mode: $playModeText"

                    playPauseButton.text = if (state.isPlaying) "Pause" else "Play"

                    if (!isTouchingSeekBar && state.durationMs > 0) {
                        progressBar.progress = (state.progressMs * 100) / state.durationMs
                        currentTimeText.text = formatTime(state.progressMs)
                    }
                    totalTimeText.text = formatTime(state.durationMs)

                    findViewById<TextView>(R.id.songNameText).text = state.activeTrack ?: ""

                    updatePlaylistSpinner(state.playlistNames)

                    state.errorMessage?.let { message ->
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    private fun updatePlaylistSpinner(playlistNames: List<String>) {
        spinnerAdapter.clear()
        spinnerAdapter.addAll(playlistNames)
        spinnerAdapter.notifyDataSetChanged()
    }

    private fun showNewPlaylistDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Nowa Playlista")

        val dialogView = layoutInflater.inflate(R.layout.dialog_new_playlist, null)
        dialogBuilder.setView(dialogView)

        val input = dialogView.findViewById<EditText>(R.id.playlistNameInput)
        val songListView = dialogView.findViewById<ListView>(R.id.songListView)

        val allSongs = viewModel.getAllSongs()
        val songAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, allSongs)
        songListView.adapter = songAdapter
        songListView.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        dialogBuilder.setPositiveButton("Zatwierdź") { _, _ ->
            val playlistName = input.text.toString().trim()
            val selectedSongs = mutableListOf<String>()
            for (i in 0 until songListView.count) {
                if (songListView.isItemChecked(i)) {
                    selectedSongs.add(allSongs[i])
                }
            }

            val playlist = viewModel.createPlaylist(playlistName, selectedSongs)
            playlist?.let { created ->
                val index = viewModel.uiState.value.playlistNames.indexOf(created.name)
                if (index != -1) spinner.setSelection(index)
                Toast.makeText(this, "Playlista \"${created.name}\" została dodana!", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBuilder.setNegativeButton("Anuluj") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = dialogBuilder.create()
        dialog.show()
    }

    private fun updateListView() {
        if (isSelectionMode) {
            addToButton.visibility = View.VISIBLE
            songListView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        } else {
            addToButton.visibility = View.GONE
            songListView.choiceMode = ListView.CHOICE_MODE_NONE
        }
    }

    private fun addSelectedSongsToPlaylist(playlist: Playlist) {
        viewModel.addSelectedSongsToPlaylist(playlist, selectedSongs)

        selectedSongs.clear()
        isSelectionMode = false
        updateListView()
    }

    private fun formatTime(milliseconds: Int): String {
        val minutes = (milliseconds / 1000) / 60
        val seconds = Math.round((milliseconds.toFloat() / 1000) % 60)
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onBackPressed() {
        if (isSelectionMode) {
            isSelectionMode = false
            selectedSongs.clear()
            updateListView()
        } else {
            super.onBackPressed()
        }
    }
}
