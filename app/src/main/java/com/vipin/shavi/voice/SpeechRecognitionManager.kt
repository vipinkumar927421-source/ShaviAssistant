package com.vipin.shavi.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

enum class ShaviListenState { IDLE, LISTENING_FOR_WAKE_WORD, LISTENING_FOR_COMMAND, THINKING, SPEAKING }

/**
 * Foreground continuous-listening manager. Runs a loop of short SpeechRecognizer
 * sessions, checking each transcript for the wake phrase "hey shavi" (English)
 * or "हे शावी" / "शावी" (Hindi/Hinglish spellings). This works while the app
 * (or its foreground service) is alive — it is NOT a true system-wide
 * always-on wake word like Google Assistant, since Android does not expose
 * that capability to third-party apps without a dedicated low-power DSP
 * wake-word SDK (e.g. Picovoice Porcupine). Swap this class out for a
 * Porcupine-based detector if always-on-in-background is required.
 */
class SpeechRecognitionManager(
    private val context: Context,
    private val onStateChanged: (ShaviListenState) -> Unit,
    private val onCommandRecognized: (String) -> Unit,
    private val onError: (String) -> Unit
) : RecognitionListener {

    private var recognizer: SpeechRecognizer? = null
    private var mode = Mode.WAKE_WORD
    private var running = false

    private enum class Mode { WAKE_WORD, COMMAND }

    private val wakePhrases = listOf("hey shavi", "hey shabi", "हे शावी", "शावी")

    fun start() {
        if (running) return
        running = true
        mode = Mode.WAKE_WORD
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(this@SpeechRecognitionManager)
        }
        onStateChanged(ShaviListenState.LISTENING_FOR_WAKE_WORD)
        listenOnce()
    }

    fun stop() {
        running = false
        recognizer?.destroy()
        recognizer = null
        onStateChanged(ShaviListenState.IDLE)
    }

    /** Called by the manual microphone button to skip straight to command mode. */
    fun listenForCommandNow() {
        mode = Mode.COMMAND
        onStateChanged(ShaviListenState.LISTENING_FOR_COMMAND)
        listenOnce()
    }

    private fun listenOnce() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            // Allow Hindi as a secondary preferred language for Hinglish support
            putExtra(RecognizerIntent.EXTRA_PREFERRED_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        recognizer?.startListening(intent)
    }

    override fun onResults(results: Bundle?) {
        val text = results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.lowercase(Locale.getDefault())
            .orEmpty()

        when (mode) {
            Mode.WAKE_WORD -> {
                if (wakePhrases.any { text.contains(it) }) {
                    mode = Mode.COMMAND
                    onStateChanged(ShaviListenState.LISTENING_FOR_COMMAND)
                    listenOnce()
                } else if (running) {
                    listenOnce() // keep listening for wake word
                }
            }
            Mode.COMMAND -> {
                if (text.isNotBlank()) {
                    onStateChanged(ShaviListenState.THINKING)
                    onCommandRecognized(text)
                }
                mode = Mode.WAKE_WORD
                if (running) {
                    onStateChanged(ShaviListenState.LISTENING_FOR_WAKE_WORD)
                    listenOnce()
                }
            }
        }
    }

    override fun onError(error: Int) {
        val noSpeechHeard = error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT

        if (mode == Mode.COMMAND) {
            // Whether nothing was heard or a real error occurred, never stay
            // silently stuck waiting — always acknowledge, then reset.
            val message = if (noSpeechHeard) {
                "Maine kuch suna nahi. Phir se \"Hey Shavi\" bol kar try karein."
            } else {
                "Sorry, main samajh nahi payi (code $error). Phir se try karein."
            }
            onError(message)
            mode = Mode.WAKE_WORD
            if (running) onStateChanged(ShaviListenState.LISTENING_FOR_WAKE_WORD)
        } else if (!noSpeechHeard) {
            // Non-recoverable error while idly listening for the wake word —
            // surface it, but no need to interrupt with speech since the
            // user never actively addressed Shavi.
            onError("Voice recognition me dikkat aayi (code $error).")
        }
        if (running) listenOnce()
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
}
