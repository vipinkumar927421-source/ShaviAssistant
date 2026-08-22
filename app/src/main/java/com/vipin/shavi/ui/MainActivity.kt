package com.vipin.shavi.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.vipin.shavi.R
import com.vipin.shavi.ai.GeminiClient
import com.vipin.shavi.ai.GeminiResult
import com.vipin.shavi.control.PhoneControlManager
import com.vipin.shavi.databinding.ActivityMainBinding
import com.vipin.shavi.security.SecureKeyStore
import com.vipin.shavi.service.ShaviForegroundService
import com.vipin.shavi.voice.ShaviListenState
import com.vipin.shavi.voice.SpeechRecognitionManager
import com.vipin.shavi.voice.TextToSpeechManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var keyStore: SecureKeyStore
    private lateinit var geminiClient: GeminiClient
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var speechManager: SpeechRecognitionManager
    private lateinit var phoneControl: PhoneControlManager

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results[Manifest.permission.RECORD_AUDIO] == true) {
            startListening()
        } else {
            binding.statusText.text = getString(R.string.mic_permission_required)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        keyStore = SecureKeyStore(this)
        geminiClient = GeminiClient { keyStore.getApiKey() }
        phoneControl = PhoneControlManager(this)

        ttsManager = TextToSpeechManager(
            context = this,
            onSpeakingStarted = { setState(ShaviListenState.SPEAKING) },
            onSpeakingFinished = { speechManager.resumeAfterSpeaking() }
        )

        speechManager = SpeechRecognitionManager(
            context = this,
            onStateChanged = { state -> runOnUiThread { setState(state) } },
            onCommandRecognized = { command -> handleCommand(command) },
            onError = { message ->
                runOnUiThread {
                    binding.responseText.text = message
                    ttsManager.speak(message, isHindi = true)
                }
            }
        )

        binding.micButton.setOnClickListener {
            speechManager.listenForCommandNow()
        }

        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        if (!keyStore.hasApiKey()) {
            binding.responseText.text = getString(R.string.please_add_api_key)
        }

        ensurePermissionsAndStart()
    }

    private fun ensurePermissionsAndStart() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) needed += Manifest.permission.RECORD_AUDIO

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) needed += Manifest.permission.POST_NOTIFICATIONS

        if (needed.isNotEmpty()) {
            requestPermissions.launch(needed.toTypedArray())
        } else {
            startListening()
        }
    }

    private fun startListening() {
        ContextCompat.startForegroundService(this, Intent(this, ShaviForegroundService::class.java))
        speechManager.start()
    }

    private fun setState(state: ShaviListenState) {
        binding.statusText.text = when (state) {
            ShaviListenState.IDLE -> getString(R.string.status_idle)
            ShaviListenState.LISTENING_FOR_WAKE_WORD -> getString(R.string.status_waiting_wake_word)
            ShaviListenState.LISTENING_FOR_COMMAND -> getString(R.string.status_listening)
            ShaviListenState.THINKING -> getString(R.string.status_thinking)
            ShaviListenState.SPEAKING -> getString(R.string.status_speaking)
        }
    }

    private fun handleCommand(command: String) {
        if (command == "__EXIT__") {
            val bye = "Theek hai, bye!"
            binding.responseText.text = bye
            ttsManager.speak(bye, isHindi = true)
            return
        }
        binding.conversationText.text = command

        // Phone-control intents are matched locally first (fast + reliable);
        // anything else is routed to Gemini for open-ended understanding.
        val handledLocally = tryHandlePhoneControl(command)
        if (handledLocally != null) {
            binding.responseText.text = handledLocally.message
            ttsManager.speak(handledLocally.message, isHindi = containsHindi(command))
            return
        }

        lifecycleScope.launch {
            when (val result = geminiClient.send(command)) {
                is GeminiResult.Success -> {
                    binding.responseText.text = result.text
                    ttsManager.speak(result.text, isHindi = containsHindi(command))
                }
                is GeminiResult.Error -> {
                    binding.responseText.text = result.userMessage
                    ttsManager.speak(result.userMessage, isHindi = true)
                }
            }
        }
    }

    private fun tryHandlePhoneControl(command: String): PhoneControlManager.ActionResult? {
        val lower = command.lowercase()
        return when {
            "open whatsapp" in lower -> phoneControl.openApp("whatsapp")
            "open instagram" in lower -> phoneControl.openApp("instagram")
            "open gmail" in lower -> phoneControl.openApp("gmail")
            "open youtube" in lower -> phoneControl.openApp("youtube")
            "wifi" in lower && ("on" in lower || "off" in lower || "toggle" in lower) -> phoneControl.toggleWifi()
            "bluetooth" in lower -> phoneControl.toggleBluetooth()
            "flashlight" in lower || "torch" in lower -> phoneControl.toggleFlashlight("on" in lower)
            "volume up" in lower || "increase the volume" in lower -> phoneControl.adjustVolume(raise = true)
            "volume down" in lower || "decrease the volume" in lower -> phoneControl.adjustVolume(raise = false)
            else -> null
        }
    }

    private fun containsHindi(text: String): Boolean =
        text.any { it.code in 0x0900..0x097F }

    override fun onDestroy() {
        super.onDestroy()
        speechManager.stop()
        ttsManager.shutdown()
    }
}
