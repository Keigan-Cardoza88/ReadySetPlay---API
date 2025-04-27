package com.example.readysetplay_performancecompanion

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.style.UnderlineSpan
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.readysetplay_performancecompanion.model.Song
import com.example.readysetplay_performancecompanion.model.SongResponse
import com.example.readysetplay_performancecompanion.ui.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import android.widget.ScrollView
import android.os.Handler
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.Log
import android.graphics.Paint
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.readysetplay_performancecompanion.model.ChordDetector
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.io.BufferedReader
import java.io.InputStreamReader

private val OPEN_FILE_REQUEST_CODE = 1234

class MainActivity : AppCompatActivity() {

    private val STORAGE_PERMISSION_CODE = 1001
    private lateinit var searchInput: EditText
    private lateinit var searchButton: Button
    private lateinit var songDropdown: Spinner
    private lateinit var textEditor: EditText
    private lateinit var sendButton: Button
    private lateinit var undoButton: Button
    private lateinit var pageBreakButton: Button  // Button for page break
    private lateinit var newFileButton: Button  // Button for new file
    private lateinit var saveFileButton: Button  // Button for saving the file
    private lateinit var openFileButton: Button  // Button for opening a saved file
    private lateinit var autoScrollButton: Button  // Auto-scroll button
    private lateinit var increaseSpeedButton: Button  // Increase speed button
    private lateinit var decreaseSpeedButton: Button  // Decrease speed button
    private lateinit var textEditorScrollView: ScrollView

    private var selectedSong: Song? = null  // Store selected song
    private val undoStack = mutableListOf<String>()  // Stack to store previous states

    private var isAutoScrolling = false
    private var scrollSpeed = 10 // Default scroll speed (px)
    private val handler = Handler()
    private var scrollRunnable: Runnable? = null

    private lateinit var boldButton: Button
    private lateinit var italicButton: Button
    private lateinit var underlineButton: Button
    private var currentTranspose = 0
    private lateinit var chordDetector: ChordDetector
    private lateinit var aiAutoScrollButton: Button
    private var isAutoScrollActive = false
    private var isChordDetectionRunning = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Views
        searchInput = findViewById(R.id.searchInput)
        searchButton = findViewById(R.id.searchButton)
        undoButton = findViewById(R.id.undoButton)
        pageBreakButton = findViewById(R.id.pageBreakButton)  // Initialize Page Break Button
        newFileButton = findViewById(R.id.newFileButton)
        autoScrollButton = findViewById(R.id.autoScrollButton)
        increaseSpeedButton = findViewById(R.id.increaseSpeedButton)
        decreaseSpeedButton = findViewById(R.id.decreaseSpeedButton)
        sendButton = findViewById<Button>(R.id.sendButton)
        songDropdown = findViewById<Spinner>(R.id.songDropdown)
        textEditor = findViewById(R.id.textEditor)
        textEditorScrollView = findViewById(R.id.textEditorScrollView)
        boldButton = findViewById(R.id.boldButton)
        italicButton = findViewById(R.id.italicButton)
        underlineButton = findViewById(R.id.underlineButton)
        val textView = findViewById<TextView>(R.id.textView)
        val transposeUpButton: MaterialButton = findViewById(R.id.transposeUpButton)
        val transposeDownButton: MaterialButton = findViewById(R.id.transposeDownButton)
        val transposeLevelText: TextView = findViewById(R.id.transposeLevelText)
        val textEditor: TextInputEditText = findViewById(R.id.textEditor)
        val saveButton = findViewById<MaterialButton>(R.id.saveFileButton)
        val openButton = findViewById<MaterialButton>(R.id.openFileButton)
        val newFileButton = findViewById<MaterialButton>(R.id.newFileButton)
        aiAutoScrollButton = findViewById(R.id.aiAutoScrollButton)
        aiAutoScrollButton.text = "Start AI AutoScroll"

        aiAutoScrollButton.setOnClickListener {
            isAutoScrollActive = !isAutoScrollActive

            if (isAutoScrollActive) {
                aiAutoScrollButton.text = "Stop AI AutoScroll"
                aiAutoScrollButton.post {
                    startaiAutoScroll()
                }
            } else {
                aiAutoScrollButton.text = "Start AI AutoScroll"
                stopaiAutoScroll()
            }

        }

        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), 1)

        chordDetector = ChordDetector()

        chordDetector.startListening(this) { detectedChord ->
            runOnUiThread {
                Log.d("ChordDetected", "Detected chord: $detectedChord")
                // Show on UI or scroll
            }
        }

        // Search Button Click
        searchButton.setOnClickListener {
            val query = searchInput.text.toString().trim()
            if (query.isNotEmpty()) {
                searchSongs(query)
            } else {
                Toast.makeText(this, "Enter a song name", Toast.LENGTH_SHORT).show()
            }
        }

        autoScrollButton.setOnClickListener {
            if (isAutoScrolling) {
                stopAutoScroll()
            } else {
                startAutoScroll()
            }
        }

        increaseSpeedButton.setOnClickListener {
            increaseScrollSpeed()
        }

        // Decrease Speed Button Click
        decreaseSpeedButton.setOnClickListener {
            decreaseScrollSpeed()
        }

        // Send Button Click
        sendButton.setOnClickListener {
            insertLyricsAndChords()
        }

        // Undo Button Click
        undoButton.setOnClickListener {
            undoLastChange()
        }

        // Page Break Button Click
        pageBreakButton.setOnClickListener {
            insertPageBreak()
        }

        newFileButton.setOnClickListener {
            createNewFile()
        }

        saveButton.setOnClickListener {
            val editText = EditText(this)
            editText.hint = "Enter file name"

            AlertDialog.Builder(this)
                .setTitle("Save Setlist")
                .setView(editText)
                .setPositiveButton("Save") { _, _ ->
                    val fileName = editText.text.toString().trim()
                    if (fileName.isNotEmpty()) {
                        saveToPublicFolder(fileName)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        openButton.setOnClickListener {
            openTextFile()
        }

        // Bold Button
        boldButton.setOnClickListener {
            applyStyle(StyleSpan(Typeface.BOLD))
        }

        // Italic Button
        italicButton.setOnClickListener {
            applyStyle(StyleSpan(Typeface.ITALIC))
        }

        // Underline Button
        underlineButton.setOnClickListener {
            applyStyle(UnderlineSpan())
        }


        fun updateTransposeText() {
            transposeLevelText.text = when {
                currentTranspose > 0 -> "+${currentTranspose}"
                currentTranspose < 0 -> "${currentTranspose}"
                else -> "±0"
            }
        }

        transposeUpButton.setOnClickListener {
            val currentText = textEditor.text.toString()
            val cursorPosition = textEditor.selectionStart  // 🔹 Saves cursor position
            currentTranspose += 1
            val transposed = transposeChords(currentText, 1)
            textEditor.setText(transposed)
            updateTransposeText()
            // 🔹 Restore cursor if it's within bounds
            if (cursorPosition <= transposed.length) {
                textEditor.setSelection(cursorPosition)
            }
        }

        transposeDownButton.setOnClickListener {
            val currentText = textEditor.text.toString()
            val cursorPosition = textEditor.selectionStart  // 🔹 Save cursor position
            currentTranspose -= 1
            val transposed = transposeChords(currentText, -1)
            textEditor.setText(transposed)
            updateTransposeText()
            // 🔹 Restore cursor if it's within bounds
            if (cursorPosition <= transposed.length) {
                textEditor.setSelection(cursorPosition)
            }
        }


        // Dropdown Selection
        songDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val songs = songDropdown.tag as? List<Song>
                selectedSong = songs?.get(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedSong = null
            }
        }
    }

    private fun isValidChord(chord: String): Boolean {
        val validChords = listOf("C", "D", "E", "F", "G", "A", "B", "Cm", "Dm", "Em", "Fm", "Gm", "Am", "Bm")
        return validChords.contains(chord)
    }

    private fun startAutoScroll() {
        if (isAutoScrolling) {
            Log.d("Normal Autoscroll", "Normal auto-scroll is already active.")
            return
        }

        if (!::textEditorScrollView.isInitialized) {
            Log.e("AutoScroll", "textEditorScrollView is not initialized!")
            return
        }

        // If AI is on, turn it off
        if (isAutoScrollActive) {
            stopaiAutoScroll()
        }

        isAutoScrolling = true
        updateAutoScrollButtonText()

        scrollRunnable = object : Runnable {
            override fun run() {
                textEditorScrollView.scrollBy(0, scrollSpeed)
                if (isAutoScrolling) {
                    handler.postDelayed(this, 30)
                }
            }
        }

        handler.post(scrollRunnable!!)
    }

    private fun stopAutoScroll() {
        if (!isAutoScrolling) {
            Log.d("Normal Autoscroll", "Normal auto-scroll is not active.")
            return
        }

        isAutoScrolling = false
        updateAutoScrollButtonText()
        scrollRunnable?.let { handler.removeCallbacks(it) }
        Log.d("Normal Autoscroll", "Normal autoscroll stopped.")
    }

    private fun startaiAutoScroll() {
        if (isAutoScrolling) stopAutoScroll()

        if (!isChordDetectionRunning) {
            Log.d("AI Autoscroll", "AI Autoscroll started!")
            chordDetector.startListening(this) { detectedChord ->
                runOnUiThread {
                    Log.d("ChordDetected", "Detected chord: $detectedChord")
                    if (isValidChord(detectedChord)) scrollToChord(detectedChord)
                }
            }
            isChordDetectionRunning = true
        }
    }


    private fun stopaiAutoScroll() {
        if (isChordDetectionRunning) {
            chordDetector.stopListening()
            Log.d("AI Autoscroll", "AI Autoscroll stopped.")
            isChordDetectionRunning = false
        }
    }




    // Update button text with speed
    private fun updateAutoScrollButtonText() {
        if (isAutoScrolling) {
            autoScrollButton.text = "🚫 Auto Scroll ($scrollSpeed)"
        } else {
            autoScrollButton.text = "Auto-Scroll ($scrollSpeed)"
        }
    }

    // Increase scroll speed
    private fun increaseScrollSpeed() {
        scrollSpeed += 1
        updateAutoScrollButtonText()
    }

    // Decrease scroll speed
    private fun decreaseScrollSpeed() {
        if (scrollSpeed > 1) {
            scrollSpeed -= 1
            updateAutoScrollButtonText()
        }
    }


    private fun scrollToChord(chord: String) {
        val text = textEditor.text.toString()
        val cursorPosition = textEditor.selectionStart  // Get the current cursor position
        val searchStartIndex = cursorPosition + 1  // Start searching for the chord after the current cursor position

        // Match only chords using the same regex logic as transposeChords
        val chordRegex = Regex("""\b([A-G][#b]?)([^/\s]*)""")

        // Find the next valid chord occurrence after the cursor position
        val matchResults = chordRegex.findAll(text)

        var nextChordIndex = -1
        var searchPastCursor = false

        // Iterate through all the matches and find the first match after the cursor position
        for (match in matchResults) {
            if (match.range.first >= searchStartIndex) {  // Make sure we are past the cursor position
                val chordBase = match.groupValues[1]  // Extract the root of the chord (e.g., A, C#)
                val chordSuffix = match.groupValues[2]  // Extract any suffix (e.g., "m", "maj", etc.)

                // Check if the detected chord matches the target chord, ignoring the minor (m) suffix
                if (chordBase.equals(chord, ignoreCase = true) || (chordBase.equals(chord.dropLast(1), ignoreCase = true) && chordSuffix.equals("m", ignoreCase = true))) {
                    nextChordIndex = match.range.first  // Found the matching chord
                    break
                }
            }
        }

        if (nextChordIndex != -1) {
            textEditor.requestFocus()

            // Move cursor to the next chord position
            textEditor.setSelection(nextChordIndex)

            // Find line number
            val layout = textEditor.layout
            if (layout != null) {
                val line = layout.getLineForOffset(nextChordIndex)
                val y = layout.getLineTop(line)

                // Scroll the ScrollView to the line
                textEditorScrollView.post {
                    textEditorScrollView.smoothScrollTo(0, y)
                }
            }
        } else {
            Toast.makeText(this, "Chord \"$chord\" not found", Toast.LENGTH_SHORT).show()
        }
    }

    fun applyStyle(style: Any) {
        val start = textEditor.selectionStart
        val end = textEditor.selectionEnd

        if (start != end) {
            val spannable = SpannableStringBuilder(textEditor.text)
            spannable.setSpan(style, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            // Update text but preserve cursor position
            textEditor.text = spannable
            textEditor.setSelection(end)  // Set cursor back to where it was
        }
    }
    // Function to search for songs from the API
    private fun searchSongs(query: String) {
        RetrofitClient.api.searchSongs(query).enqueue(object : Callback<SongResponse> {
            override fun onResponse(call: Call<SongResponse>, response: Response<SongResponse>) {
                if (response.isSuccessful) {
                    val songs = response.body()?.songs ?: emptyList()
                    if (songs.isNotEmpty()) {
                        setupDropdown(songs)
                    } else {
                        Toast.makeText(applicationContext, "No songs found", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<SongResponse>, t: Throwable) {
                Toast.makeText(applicationContext, "Failed: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Setup the song dropdown list
    private fun setupDropdown(songs: List<Song>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, songs.map { it.song_name })
        songDropdown.adapter = adapter
        songDropdown.tag = songs

        songDropdown.visibility = View.VISIBLE
        sendButton.visibility = View.VISIBLE
        undoButton.visibility = View.VISIBLE
    }

    // Insert lyrics and chords at the cursor position
    private fun insertLyricsAndChords() {
        if (selectedSong == null) {
            Toast.makeText(this, "Please select a song first!", Toast.LENGTH_SHORT).show()
            return
        }

        val song = selectedSong!!
        val lyricsAndChords = "\n${song.chords_lyrics}\n"
        val cursorPosition = textEditor.selectionStart

        // Save previous text before modification (for multiple undos)
        undoStack.add(textEditor.text.toString())

        // Insert lyrics & chords at cursor position
        textEditor.text.insert(cursorPosition, lyricsAndChords)

        // Move cursor to end of inserted text
        textEditor.setSelection(cursorPosition + lyricsAndChords.length)
        // Hide the Send Lyrics and Chords button
        sendButton.visibility = View.GONE
        // Hide the Dropdown
        songDropdown.visibility = View.GONE

        Toast.makeText(this, "Lyrics & chords added", Toast.LENGTH_SHORT).show()
    }

    // Insert a visual page break (separator line) at the cursor
    private fun insertPageBreak() {
        val cursorPosition = textEditor.selectionStart
        val pageBreak = "\n-------------------------- PAGE BREAK --------------------------\n"

        // Save previous text before modification (for multiple undos)
        undoStack.add(textEditor.text.toString())

        // Insert page break at cursor position
        textEditor.text.insert(cursorPosition, pageBreak)

        // Move cursor to end of inserted page break
        textEditor.setSelection(cursorPosition + pageBreak.length)

        Toast.makeText(this, "Page Break added", Toast.LENGTH_SHORT).show()
    }

    // Undo the last change made to the text editor
    private fun undoLastChange() {
        if (undoStack.isNotEmpty()) {
            val lastText = undoStack.removeAt(undoStack.size - 1) // Get the last state
            textEditor.setText(lastText)
            textEditor.setSelection(lastText.length) // Move cursor to end
            Toast.makeText(this, "Undo successful", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Nothing to undo!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissionAndSave() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)

        } else {
            showSaveDialog()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showSaveDialog()
        } else {
            Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val write = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val read = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
            write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                101
            )
        }
    }

    private fun saveToPublicFolder(fileName: String) {
        if (!hasStoragePermission()) {
            requestStoragePermission()
            return
        }

        val content = findViewById<TextInputEditText>(R.id.textEditor).text.toString()
        val publicDir = File(Environment.getExternalStorageDirectory(), "Android/RSPSetlists")

        if (!publicDir.exists()) {
            publicDir.mkdirs()
        }

        val file = File(publicDir, "$fileName.txt")

        try {
            FileOutputStream(file).use { it.write(content.toByteArray()) }
            Toast.makeText(this, "Saved to: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save file", Toast.LENGTH_SHORT).show()
        }
    }


    private fun showSaveDialog() {
        val input = EditText(this)
        input.hint = "Enter file name"

        AlertDialog.Builder(this)
            .setTitle("Save As")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val fileName = input.text.toString()
                if (fileName.isNotEmpty()) {
                    saveToFile(fileName)
                } else {
                    Toast.makeText(this, "File name can't be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveToFile(fileName: String) {
        val content = findViewById<TextInputEditText>(R.id.textEditor).text.toString()

        // 🔥 Build custom path: Android/RSPSetlists/
        val rspFolder = File(getExternalFilesDir(null), "Android/RSPSetlists")

        if (!rspFolder.exists()) {
            rspFolder.mkdirs() // 📁 Create folder if it doesn't exist
        }

        val file = File(rspFolder, "$fileName.txt")

        try {
            FileOutputStream(file).use { it.write(content.toByteArray()) }
            Toast.makeText(this, "File saved to: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save file", Toast.LENGTH_SHORT).show()
        }
    }


    private fun openTextFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
        }
        startActivityForResult(intent, OPEN_FILE_REQUEST_CODE)
    }

    private fun createNewFile() {
        val content = textEditor.text.toString().trim()

        if (content.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Save Changes?")
                .setMessage("Do you want to save the current file before creating a new one?")
                .setPositiveButton("Yes") { _, _ ->
                    // Ask for file name and save
                    val input = EditText(this)
                    input.hint = "Enter file name"

                    AlertDialog.Builder(this)
                        .setTitle("Save Setlist")
                        .setView(input)
                        .setPositiveButton("Save") { _, _ ->
                            val fileName = input.text.toString().trim()
                            if (fileName.isNotEmpty()) {
                                saveToPublicFolder(fileName)
                                textEditor.text?.clear()
                                Toast.makeText(this, "New file created", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
                .setNegativeButton("No") { _, _ ->
                    // Just clear the editor
                    textEditor.text?.clear()
                    Toast.makeText(this, "New file created", Toast.LENGTH_SHORT).show()
                }
                .setNeutralButton("Cancel", null)
                .show()
        } else {
            // If editor is already empty, just clear and show toast
            textEditor.text?.clear()
            Toast.makeText(this, "New file created", Toast.LENGTH_SHORT).show()
        }
    }

    private fun transposeChords(text: String, steps: Int): String {
        val sharpNotes = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val flatNotes  = listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")

        // Match chords like A, Am, A#, A#m7, Bbmaj7, Fsus4, Gadd9, etc.
        val chordRegex = Regex("""\b([A-G][#b]?)([^/\s]*)""")

        // Transpose one root note
        fun transposeChord(root: String): String {
            val index = sharpNotes.indexOf(root).takeIf { it != -1 }
                ?: flatNotes.indexOf(root).takeIf { it != -1 }

            if (index != null) {
                val newIndex = (index + steps + 12) % 12
                return sharpNotes[newIndex]
            }
            return root
        }

        return text.lines().joinToString("\n") { line ->
            val words = line.trim().split(Regex("""\s+"""))
            val chordLikeCount = words.count { chordRegex.matches(it) }
            val isChordLine = words.isNotEmpty() && chordLikeCount.toDouble() / words.size > 0.6

            if (isChordLine) {
                chordRegex.replace(line) { match ->
                    val root = match.groupValues[1]
                    val suffix = match.groupValues[2]
                    transposeChord(root) + suffix
                }
            } else {
                line
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        chordDetector.stopListening()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == OPEN_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val contentResolver = applicationContext.contentResolver
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val text = reader.readText()
                    findViewById<TextInputEditText>(R.id.textEditor).setText(text)
                    Toast.makeText(this, "File loaded!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}
