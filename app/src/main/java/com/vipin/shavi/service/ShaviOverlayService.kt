package com.vipin.shavi.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.vipin.shavi.ai.GeminiClient
import com.vipin.shavi.ai.GeminiResult
import com.vipin.shavi.security.SecureKeyStore
import com.vipin.shavi.voice.TextToSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShaviOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private lateinit var statusText: TextView
    private lateinit var inputField: EditText
    private lateinit var keyStore: SecureKeyStore
    private lateinit var geminiClient: GeminiClient
    private lateinit var ttsManager: TextToSpeechManager
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        keyStore = SecureKeyStore(this)
        geminiClient = GeminiClient { keyStore.getApiKey() }
        ttsManager = TextToSpeechManager(
            context = this,
            onSpeakingStarted = {},
            onSpeakingFinished = {}
        )
        showOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val initialText = intent?.getStringExtra(EXTRA_HEARD_TEXT)
        if (!initialText.isNullOrBlank() && ::statusText.isInitialized) {
            statusText.text = initialText
        }
        return START_NOT_STICKY
    }

    private fun showOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 32)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1C1C1E"))
                cornerRadius = 40f
            }
        }

        statusText = TextView(this).apply {
            text = "Sun rahi hoon..."
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(0, 0, 0, 24)
        }

        inputField = EditText(this).apply {
            hint = "Ya yahan type karo..."
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
        }

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 24, 0, 0)
        }

        val sendButton = Button(this).apply {
            text = "Bhejo"
            setOnClickListener {
                val text = inputField.text.toString()
                if (text.isNotBlank()) {
                    handleTypedCommand(text)
                    inputField.setText("")
                }
            }
        }

        val closeButton = Button(this).apply {
            text = "Band karo"
            setOnClickListener { stopSelf() }
        }

        buttonRow.addView(sendButton)
        buttonRow.addView(closeButton)

        container.addView(statusText)
        container.addView(inputField)
        container.addView(buttonRow)

        val overlayType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
            y = 120
        }

        overlayView = container
        windowManager?.addView(overlayView, params)
    }

    private fun handleTypedCommand(command: String) {
        statusText.text = command

        scope.launch {
            statusText.text = "Soch rahi hoon..."
            val result = withContext(Dispatchers.IO) { geminiClient.send(command) }
            when (result) {
                is GeminiResult.Success -> {
                    statusText.text = result.text
                    ttsManager.speak(result.text, isHindi = true)
                }
                is GeminiResult.Error -> {
                    statusText.text = result.userMessage
                    ttsManager.speak(result.userMessage, isHindi = true)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let {
            try { windowManager?.removeView(it) } catch (e: Exception) {}
        }
        ttsManager.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_HEARD_TEXT = "heard_text"
    }
}
