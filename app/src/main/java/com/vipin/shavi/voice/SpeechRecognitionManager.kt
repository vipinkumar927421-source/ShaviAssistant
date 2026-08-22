package com.vipin.shavi.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

enum class ShaviListenState { IDLE, LISTENING_FOR_WAKE_WORD, LISTENING_FOR_COMMAND, THINKING, SPEAKING }

class SpeechRecognitionManager(
    private val context: Context,
    private val onStateChanged: (ShaviListenState) -> Unit,
    private val onCommandRecognized: (String) -> Unit,
    private val onError: (String) -> Unit
) : RecognitionListener {

    private var recognizer: SpeechRecognizer? = null
    private var mode = Mode.WAKE_WORD
    private var running = false
    private val handler = Handler(Looper.getMainLooper())

    private enum class Mode { WAKE_WORD, COMMAND }

    private val wakePhrases = listOf("shavi", "savi", "shabi", "sabi", "शावी", "सावी", "शबी")

    fun start() {
        if (running) return
        running = true
        mode = Mode.WAKE_WORD
        onStateChanged(ShaviListenState.LISTENING_FOR_WAKE_WORD)
        restartListening(delayMs = 0)
    }

    fun stop() {
        running = false
        handler.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        recognizer = null
        onStateChanged(ShaviListenState.IDLE)
    }

    fun listenForCommandNow() {
        mode = Mode.COMMAND
        onStateChanged(ShaviListenState.LISTENING_FOR_COMMAND)
        restartListening(delayMs = 0)
    }

    private fun restartListening(delayMs: Long = 300) {
        if (!running) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Is device par speech recognition available nahi hai. Google app installed/updated hai ya nahi check karein.")
            return
        }

        recognizer?.destroy()
        recognizer = null

        handler.postDelayed({
            if (!running) return@postDelayed
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@SpeechRecognitionManager)
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }
            recognizer?.startListening(intent)
        }, delayMs)
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
                } else {
                    onStateChanged(ShaviListenState.LISTENING_FOR_WAKE_WORD)
                }
                restartListening()
            }
            Mode.COMMAND -> {
                if (text.isNotBlank()) {
                    onStateChanged(ShaviListenState.THINKING)
                    onCommandRecognized(text)
                } else {
                    onError("Maine kuch suna nahi. Phir se \"Hey Shavi\" bol kar try karein.")
                }
                mode = Mode.WAKE_WORD
                onStateChanged(ShaviListenState.LISTENING_FOR_WAKE_WORD)
                restartListening()
            }
        }
    }

    override fun onError(error: Int) {
        val noSpeechHeard = error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT

        if (mode == Mode.COMMAND) {
            val message = if (noSpeechHeard) {
                "Maine kuch suna nahi. Phir se \"Hey Shavi\" bol kar try karein."
            } else {
                "Sorry, main samajh nahi payi (code $error). Phir se try karein."
            }
            onError(message)
        }

        mode = Mode.WAKE_WORD
        onStateChanged(ShaviListenState.LISTENING_FOR_WAKE_WORD)
        restartListening()
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
}
