package com.example.readysetplay_performancecompanion.model

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchProcessor
import kotlin.math.log2
import kotlin.math.pow

class ChordDetector {
    private var dispatcher: AudioDispatcher? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isListening = false

    private var noteBuffer = mutableListOf<String>()
    private val MAX_BUFFER = 6
    private var lastDetectedChord: String? = null
    private var stableCount = 0
    private val STABILITY_THRESHOLD = 2  // Threshold for stable chord detection

    private val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    private val sharpToFlat = mapOf(
        "C#" to "Db", "D#" to "Eb", "F#" to "Gb",
        "G#" to "Ab", "A#" to "Bb"
    )

    // Updated triads: Major and Minor chords with full names
    private val triadChords = linkedMapOf(
        setOf("C", "E", "G") to "C Major",
        setOf("C#", "F", "G#") to "C# Major",
        setOf("D", "F#", "A") to "D Major",
        setOf("D#", "G", "A#") to "D# Major",
        setOf("E", "G#", "B") to "E Major",
        setOf("F", "A", "C") to "F Major",
        setOf("F#", "A#", "C#") to "F# Major",
        setOf("G", "B", "D") to "G Major",
        setOf("G#", "C", "D#") to "G# Major",
        setOf("A", "C#", "E") to "A Major",
        setOf("A#", "D", "F") to "A# Major",
        setOf("B", "D#", "F#") to "B Major",
        // Minor triads
        setOf("C", "D#", "G") to "C Minor",
        setOf("C#", "E", "G#") to "C# Minor",
        setOf("D", "F", "A") to "D Minor",
        setOf("D#", "F#", "A#") to "D# Minor",
        setOf("E", "G", "B") to "E Minor",
        setOf("F", "G#", "C") to "F Minor",
        setOf("F#", "A", "C#") to "F# Minor",
        setOf("G", "A#", "D") to "G Minor",
        setOf("G#", "B", "D#") to "G# Minor",
        setOf("A", "C", "E") to "A Minor",
        setOf("A#", "C#", "F") to "A# Minor",
        setOf("B", "D", "F#") to "B Minor"
    )

    fun startListening(context: Context, onChordDetected: (String) -> Unit) {
        val sampleRate = 22050f
        val bufferSize = 2048
        val overlap = 256

        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(sampleRate.toInt(), bufferSize, overlap)

        val pitchProcessor = PitchProcessor(
            PitchProcessor.PitchEstimationAlgorithm.YIN,  // Use YIN for more accurate pitch detection
            sampleRate,
            bufferSize
        ) { result, _ ->
            val pitch = result.pitch
            if (pitch > 0) {
                val note = pitchToNote(pitch)
                if (note.isNotEmpty()) {
                    noteBuffer.add(note)
                    if (noteBuffer.size > MAX_BUFFER) noteBuffer.removeAt(0)

                    val chord = detectChordFromBuffer(noteBuffer)
                    if (chord != null) {
                        // Only proceed if the chord has changed
                        if (chord != lastDetectedChord) {
                            stableCount = 0 // Reset count if chord changed
                            lastDetectedChord = chord
                        }

                        stableCount++
                        if (stableCount == STABILITY_THRESHOLD) {  // Only detect after chord is stable
                            handler.postDelayed({
                                Log.d("ChordDetector", "Detected chord: $chord")  // Log the chord
                                onChordDetected(convertToFlatsIfNeeded(chord))  // Notify with chord
                            }, 2000)  // 3000ms = 3 seconds delay
                        }
                    }
                }
            }
        }

        dispatcher?.addAudioProcessor(pitchProcessor)
        Thread(dispatcher, "ChordDetector").start()
    }


    fun stopListening() {
        dispatcher?.stop()
    }

    private fun detectChordFromBuffer(buffer: List<String>): String? {
        val safeBuffer = buffer.toList() // Clone to avoid ConcurrentModificationException
        val noteCounts = safeBuffer.groupingBy { it }.eachCount()
        Log.d("ChordDetector", "Detected notes: $noteCounts")

        // If you want to pick the most frequent note:
        val mostFrequentNote = noteCounts.maxByOrNull { it.value }?.key

        // Or if you want the first detected note from the buffer (without considering frequency):
        val firstDetectedNote = safeBuffer.firstOrNull()

        // You can return either the most frequent note or the first detected note:
        return mostFrequentNote ?: firstDetectedNote
    }


    private fun pitchToNote(pitch: Float): String {
        // Apply a half-step shift (increase pitch by 1 semitone)
        val shiftedPitch = pitch * 2.0.pow(1.0 / 48.0)

        // Convert the shifted pitch to a MIDI note
        val midi = (12 * log2(shiftedPitch / 440.0) + 69).toInt()
        val correctedMidi = midi + 0.5 // Slight correction for pitch deviation

        // Map to note
        return noteNames[((correctedMidi % 12 + 12) % 12).toInt()]
    }

    private fun convertToFlatsIfNeeded(chord: String): String {
        // Replace root with flat equivalent if available (e.g. C# → Db)
        val root = if (chord.endsWith("m")) chord.dropLast(1) else chord
        val suffix = if (chord.endsWith("m")) "m" else ""

        return (sharpToFlat[root] ?: root) + suffix
    }

    fun getLastDetectedChord(): String? {
        return lastDetectedChord
    }
}
