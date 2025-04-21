package com.example.musicapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import android.media.MediaPlayer
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.example.musicapp.models.Playlist
import com.example.musicapp.manager.PlaylistManager


class MainActivity : AppCompatActivity() {

    // Lista
    private lateinit var songListView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    var selectedSongs = mutableListOf<String>()
    var isSelectionMode = false

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

    private var mediaPlayer: MediaPlayer? = null
    private var songList: MutableList<String> = mutableListOf()
    private var currentSongIndex: Int = 0
    private var playMode = 1 // 1 = Stop, 2 = Next, 3 = Shuffle

    private var updateProgressRunnable: Runnable? = null

    private lateinit var musicFolder: File
    private lateinit var playlistManager: PlaylistManager

    private lateinit var spinner: Spinner
    private lateinit var spinnerAdapter: ArrayAdapter<String>
    private val playlistNames = mutableListOf<String>()


    val handler = Handler(Looper.getMainLooper())

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
        spinner = findViewById<Spinner>(R.id.playlistSpinner)

        // Obsługa przycisków
        playPauseButton.setOnClickListener { togglePlayPause() }
        nextButton.setOnClickListener { playNextSong() }
        prevButton.setOnClickListener { playPreviousSong() }
        playModeButton.setOnClickListener { changePlayMode() }
        addPlaylistButton.setOnClickListener { showNewPlaylistDialog() }

        // menadżer playlist
        playlistManager = PlaylistManager(this)

        // pobranie i obsługa playlist
        playlistNames.add("All Songs") // + dodaj inne z loadPlaylists()
        playlistNames.addAll(playlistManager.getAllPlaylists().map { it.name })

        progressBar.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (mediaPlayer != null && mediaPlayer!!.isPlaying){
                        isTouchingSeekBar = true
                        mediaPlayer?.pause()
                        playPauseButton.text = "Play"
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                        isTouchingSeekBar = false
                        mediaPlayer?.start()
                        playPauseButton.text = "Pause"
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (isTouchingSeekBar) {
                        val duration = mediaPlayer?.duration ?: 0
                        var seekTime = (duration * event.x / progressBar.width).toInt()
                        seekTime = seekTime.coerceIn(0, duration)
                        mediaPlayer?.seekTo(seekTime)
                        progressBar.progress = (seekTime * 100) / duration
                        currentTimeText.text = formatTime(seekTime)
                    }
                    true
                }
                else -> false
            }
        }

        addToButton.setOnClickListener {
            // Wyświetlanie listy playlist w oknie dialogowym
            val playlists = playlistManager.getAllPlaylists()
            val playlistNames = playlists.map { it.name } // Zakładając, że Playlist ma nazwę

            val dialog = AlertDialog.Builder(this)
                .setTitle("Wybierz playlistę")
                .setItems(playlistNames.toTypedArray()) { _, which ->
                    val selectedPlaylist = playlists[which]
                    addSelectedSongsToPlaylist(selectedPlaylist)
                }
                .create()

            dialog.show()
        }
        musicFolder = File(Environment.getExternalStorageDirectory(), "Moja muzyka")

        // Sprawdzenie uprawnień
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                1
            )
        } else {
            loadSongs()
        }

        // Ustawienie listy piosenek
        adapter = object : ArrayAdapter<String>(this, R.layout.list_item_song, R.id.songTitle, songList) {

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: layoutInflater.inflate(R.layout.list_item_song, parent, false)

                val textView = view.findViewById<TextView>(R.id.songTitle)
                val checkBox = view.findViewById<CheckBox>(R.id.songCheckBox)

                textView.text = songList[position]

                // Czy element zaznaczony w trybie wyboru
                if (isSelectionMode) {
                    checkBox.setOnCheckedChangeListener(null) // Resetuj nasłuchiwanie, aby uniknąć bugów
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

                    // Jeśli wszystkie elementy są odznaczone, wyłącz tryb wyboru
                    if (selectedSongs.isEmpty()) {
                        isSelectionMode = false
                        updateListView() // Przejdź z powrotem do normalnego trybu
                    }
                }

                return view
            }
        }
        songListView.adapter = adapter

        songListView.setOnItemLongClickListener { _, _, position, _ ->
            isSelectionMode = true
            updateListView() // Zmiana wyglądu
            selectedSongs.add(songList[position])
            true
        }

        // Poczatkowe sortowanie
        sortSongs()

        // Test playlist
        //testPlaylists()

        // ustawienie listy piosenek dla playlisty
        spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, playlistNames)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinner.adapter = spinnerAdapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedName = playlistNames[position]

                songList.clear()
                if (selectedName == "All Songs") {
                    songList.addAll(getAllSongsFromFolder()) // <- To Twoja istniejąca metoda
                } else {
                    val songsFromPlaylist = playlistManager.getSongsInPlaylist(selectedName)
                    songList.addAll(songsFromPlaylist)
                }

                selectedSongs.clear()
                isSelectionMode = false
                updateListView()
                adapter.notifyDataSetChanged()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // załadowanie piosenek
    @SuppressLint("SdCardPath")
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

    private fun loadSongs() {
        songList.clear()
        songList.addAll(getAllSongsFromFolder())

        songListView.setOnItemClickListener { _, _, position, _ ->
            currentSongIndex = position
            playSong(songList[currentSongIndex])
            adapter.notifyDataSetChanged()
        }
    }


    // Posortowanie listy
    private fun sortSongs() {
        // Zapamiętanie aktualnej piosenki
        val currentSong = songList[currentSongIndex]

        // Sortowanie listy alfabetycznie
        songList.sort()

        // Znalezienie nowego indeksu dla aktualnie odtwarzanej piosenki
        currentSongIndex = songList.indexOf(currentSong)

        // Odświeżenie listy, aby pokazać posortowane piosenki
        (songListView.adapter as ArrayAdapter<String>).notifyDataSetChanged()
    }

    // Odtwarzanie piosenek
    private fun playSong(song: String) {
        mediaPlayer?.stop()
        mediaPlayer?.reset()

        // Ścieżka do pliku
        val songFile = File("${musicFolder.absolutePath}/${song}")

        if (songFile.exists()) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(songFile.absolutePath)
                    prepare()
                    start()

                    val songNameTextView = findViewById<TextView>(R.id.songNameText)
                    songNameTextView.text = song

                    totalTimeText.text = formatTime(duration)

                    // Start Timeru dla paska postępu
                    updateProgressRunnable = object : Runnable {
                        override fun run() {

                            if (mediaPlayer != null) {
                                if (isTouchingSeekBar) {
                                    handler.postDelayed(this, 1000) // Aktualizacja co 1 sekundę
                                } else {
                                val currentPosition = mediaPlayer!!.currentPosition
                                val progress = (currentPosition * 100) / mediaPlayer!!.duration
                                progressBar.progress = progress
                                currentTimeText.text = formatTime(currentPosition)
                                handler.postDelayed(this, 1000) // Aktualizacja co 1 sekundę
                                }
                            }
                        }
                    }
                    handler.post(updateProgressRunnable as Runnable)
                }

                //Toast.makeText(this, "Playing: $song", Toast.LENGTH_SHORT).show()
                playPauseButton.text = "Pause"
                adapter.notifyDataSetChanged()

                mediaPlayer?.setOnCompletionListener {
                    handler.removeCallbacks(updateProgressRunnable as Runnable)
                    updateProgressRunnable = null // Czyścimy referencję
                    songEnd(song)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                //Toast.makeText(this, "Error playing the song.", Toast.LENGTH_SHORT).show()
            }
        } else {
            //Toast.makeText(this, "Song not found.", Toast.LENGTH_SHORT).show()
        }
    }

    // Przycisk Play/Pause
    private fun togglePlayPause() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            playPauseButton.text = "Play"
        } else {
            mediaPlayer?.start()
            playPauseButton.text = "Pause"
        }
    }

    // Przycisk Next
    private fun playNextSong() {
        if (currentSongIndex < songList.size - 1) {
            currentSongIndex++
        } else {
            currentSongIndex = 0 // Można zrobić pętlę do początku
        }
        playSong(songList[currentSongIndex])
    }

    // Przycisk Prev
    private fun playPreviousSong() {
        if (currentSongIndex > 0) {
            currentSongIndex--
        } else {
            currentSongIndex = songList.size - 1 // Można przejść na koniec listy
        }
        playSong(songList[currentSongIndex])
    }

    // Koniec piosenki działanie
    private fun songEnd(currentSong: String) {
        when (playMode) {
            1 -> {
                // Tryb 1: Zatrzymaj
                playSong(currentSong)
                mediaPlayer?.pause()
                playPauseButton.text = "Play"

            }
            2 -> {
                // Tryb 2: Odtwórz następny utwór
                playNextSong()
            }
            3 -> {
                // Tryb 3: Odtwórz losowy utwór
                val randomIndex = songList.indices.filter { it != songList.indexOf(currentSong) }.random()
                currentSongIndex = randomIndex
                playSong(songList[randomIndex])
            }
        }
    }

    // Zmiana trybu zmian piosenki
    private fun changePlayMode() {
        playMode = (playMode % 3) + 1 // Przełączaj między 1, 2, 3
        val modeText = when (playMode) {
            1 -> "Stop"
            2 -> "Next"
            3 -> "Shuffle"
            else -> "Stop"
        }
        playModeButton.text = "Mode: $modeText"
    }

    private fun testPlaylists() {
        // Przykład tworzenia nowej playlisty
        val workoutPlaylist = Playlist("Workout", mutableListOf("Bułka.mp3", "Cel.mp3"))
        playlistManager.savePlaylist(workoutPlaylist)

        // Odczytanie i wyświetlenie wszystkich playlist
        val allPlaylists = playlistManager.getAllPlaylists()
        for (playlist in allPlaylists) {
            Toast.makeText(this, "Playlista: ${playlist.name}, Utwory: ${playlist.songs}", Toast.LENGTH_SHORT).show()
        }

        // Pobranie utworów z konkretnej playlisty
        val workoutSongs = playlistManager.getSongsInPlaylist("Workout")
        Toast.makeText(this, "Utwory w Workout: $workoutSongs", Toast.LENGTH_LONG).show()
    }

    private fun showNewPlaylistDialog() {
        // Tworzymy dialog
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Nowa Playlista")

        // Layout dla dialogu
        val dialogView = layoutInflater.inflate(R.layout.dialog_new_playlist, null)
        dialogBuilder.setView(dialogView)

        // Referencje do elementów w dialogu
        val input = dialogView.findViewById<EditText>(R.id.playlistNameInput)
        val songListView = dialogView.findViewById<ListView>(R.id.songListView)

        // Adapter dla listy utworów
        val allSongs = getAllSongsFromFolder()
        val songAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, allSongs)
        songListView.adapter = songAdapter
        songListView.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        // Przyciski dialogu
        dialogBuilder.setPositiveButton("Zatwierdź") { _, _ ->
            val playlistName = input.text.toString().trim()
            if (playlistName.isNotEmpty()) {
                if (playlistManager.getPlaylist(playlistName) != null) {
                    // Jeśli playlista o tej nazwie już istnieje, wyświetl ostrzeżenie
                    Toast.makeText(this, "Playlista \"$playlistName\" już istnieje!", Toast.LENGTH_SHORT).show()
                } else {
                    val selectedSongs = mutableListOf<String>()
                    for (i in 0 until songListView.count) {
                        if (songListView.isItemChecked(i)) {
                            selectedSongs.add(allSongs[i])
                        }
                    }
                    val newPlaylist = Playlist(playlistName, selectedSongs)
                    playlistManager.savePlaylist(newPlaylist)

                    // Aktualizacja spinnera
                    playlistNames.add(playlistName)
                    spinnerAdapter.notifyDataSetChanged()
                    val index = playlistNames.indexOf(playlistName)
                    if (index != -1) spinner.setSelection(index)

                    Toast.makeText(this, "Playlista \"$playlistName\" została dodana!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Nazwa playlisty nie może być pusta!", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBuilder.setNegativeButton("Anuluj") { dialog, _ ->
            dialog.dismiss()
        }

        // Wyświetlenie dialogu
        val dialog = dialogBuilder.create()
        dialog.show()
    }

    fun updateListView() {
        if (isSelectionMode) {
            addToButton.visibility = View.VISIBLE // Przyciski pokazane w trybie wyboru
            songListView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        } else {
            addToButton.visibility = View.GONE // Przyciski ukryte w normalnym trybie
            songListView.choiceMode = ListView.CHOICE_MODE_NONE
        }
    }

    fun addSelectedSongsToPlaylist(playlist: Playlist) {
        // Dodajemy zaznaczone piosenki do playlisty
        selectedSongs.forEach { song ->
            // Załóżmy, że Playlist ma metodę do dodawania piosenek
            playlist.addSong(song)
        }

        // zaktualizowanie playlisty
        playlistManager.savePlaylist(playlist)

        // Zresetowanie zaznaczeń i trybu wyboru
        selectedSongs.clear()
        isSelectionMode = false
        updateListView()  // Przywrócenie normalnego trybu
    }

    // Formatowanie czasu do MM:SS
    private fun formatTime(milliseconds: Int): String {
        val minutes = (milliseconds / 1000) / 60
        val seconds = Math.round((milliseconds.toFloat() / 1000) % 60)
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }

    override fun onBackPressed() {
        if (isSelectionMode) {
            isSelectionMode = false
            selectedSongs.clear()
            updateListView() // Odświeżenie listy bez checkboxów
        } else {
            super.onBackPressed() // Standardowe zachowanie (zamykanie aktywności)
        }
    }


}
