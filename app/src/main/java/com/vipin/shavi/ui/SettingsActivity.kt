package com.vipin.shavi.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vipin.shavi.R
import com.vipin.shavi.databinding.ActivitySettingsBinding
import com.vipin.shavi.security.SecureKeyStore

/**
 * The API key field NEVER pre-fills with the saved key. Once saved, the UI
 * only ever shows a masked confirmation state — the raw key is not
 * re-displayed, logged, or included in any screenshot-visible text.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var keyStore: SecureKeyStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        keyStore = SecureKeyStore(this)
        refreshKeyStatus()

        binding.saveKeyButton.setOnClickListener {
            val entered = binding.apiKeyInput.text?.toString().orEmpty()
            if (entered.isBlank()) {
                binding.keyStatusText.text = getString(R.string.key_cannot_be_empty)
                return@setOnClickListener
            }
            keyStore.saveApiKey(entered)
            binding.apiKeyInput.text?.clear()
            refreshKeyStatus()
        }

        binding.clearKeyButton.setOnClickListener {
            keyStore.clearApiKey()
            refreshKeyStatus()
        }

        binding.pitchSeekBar.max = 100
        binding.speedSeekBar.max = 100
        binding.volumeSeekBar.max = 100
    }

    private fun refreshKeyStatus() {
        binding.keyStatusText.text = if (keyStore.hasApiKey()) {
            getString(R.string.key_saved)
        } else {
            getString(R.string.no_key_saved)
        }
    }
}
