package com.vipin.shavi.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
    private var conversationActive = false

    private enum class Mode { WAKE_WORD, COMMAND }

    private val wakePhrases = listOf("shavi", "savi", "shabi", "sabi", "शावी", "सावी", "शबी")
    private val exitPhrases = listOf("bye", "bas", "band karo", "chalo bye", "rehne do", "theek hai bye", "बाय", "बस करो", "बंद करो", "रहने दो")

    fun start() {
        if (running) return
        running = true
        mode = Mode.WAKE_WORD
        conversationActive = false
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

    fun listenForCommandNow() {
        mode = Mode.COMMAND
        conversationActive = true
        onStateChanged(ShaviListenState.LISTENING_FOR_COMMAND)
        listenOnce()
    }

    fun resumeAfterSpeaking() {
        if (!running) return
        if (conversationActive) {
            mode = Mode.COMMAND
            onStateChanged(ShaviListenState.LISTENING_FOR_COMMAND)
        } else {
            mode = Mode.WAKE_WORD
            onStateChanged(ShaviListenState.LISTENING_FOR_WAKE_WORD)
        }
        listenOnce()
    }

    private fun listenOnce() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
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
                    conversationActive = true
                    mode = Mode.COMMAND
                    onStateChanged(ShaviListenState.LISTENING_FOR_COMMAND)
                    listenOnce()
                } else if (running) {
                    listenOnce()
                }
            }
            Mode.COMMAND -> {
                if (text.isNotBlank()) {
                    if (exitPhrases.any { text.contains(it) }) {
                        conversationActive = false
                        onCommandRecognized("__EXIT__")
                    } else {
                        onStateChanged(ShaviListenState.THINKING)
                        onCommandRecognized(text)
                    }
                } else if (running) {
                    listenOnce()
                }
            }
        }
    }

    override fun onError(error: Int) {
        val recoverable = error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
        if (!recoverable) {
            onError("Voice recognition error code $error")
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
