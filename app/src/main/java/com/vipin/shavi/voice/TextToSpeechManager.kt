package com.vipin.shavi.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale

/**
 * Wraps Android's built-in TextToSpeech engine and biases voice selection
 * toward higher-pitched, female-sounding voices to approximate a cute,
 * young-girl persona (true bespoke child-like voices require a paid
 * expressive TTS provider — Android's system TTS only exposes what the
 * device's installed engine offers, e.g. Google Speech Services voices).
 */
class TextToSpeechManager(
    context: Context,
    private val onSpeakingStarted: () -> Unit,
    private val onSpeakingFinished: () -> Unit
) {
    private var tts: TextToSpeech? = null
    private var ready = false

    var pitch: Float = 1.3f   // higher pitch = younger sounding
    var speechRate: Float = 1.05f
    var volume: Float = 1.0f

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                configureVoice()
            }
        }
    }

    private fun configureVoice() {
        val engine = tts ?: return
        engine.language = Locale("hi", "IN") // will fall back gracefully per-utterance if needed
        engine.setPitch(pitch)
        engine.setSpeechRate(speechRate)

        val femaleVoice: Voice? = engine.voices?.firstOrNull { voice ->
            voice.name.contains("female", ignoreCase = true) ||
                voice.name.contains("f00", ignoreCase = true)
        }
        femaleVoice?.let { engine.voice = it }

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = onSpeakingStarted()
            override fun onDone(utteranceId: String?) = onSpeakingFinished()
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = onSpeakingFinished()
        })
    }

    fun speak(text: String, isHindi: Boolean = false) {
        if (!ready) return
        tts?.language = if (isHindi) Locale("hi", "IN") else Locale.US
        tts?.setPitch(pitch)
        tts?.setSpeechRate(speechRate)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "shavi_utterance")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }
}
