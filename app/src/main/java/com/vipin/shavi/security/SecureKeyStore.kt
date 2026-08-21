package com.vipin.shavi.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores the user's Gemini API key using AES-256 encrypted SharedPreferences,
 * backed by the Android Keystore. The key never touches plain-text disk storage,
 * is never logged, and is never rendered back into the UI once saved
 * (the settings screen only shows a masked placeholder such as "Key saved ✓").
 */
class SecureKeyStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "shavi_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveApiKey(key: String) {
        prefs.edit().putString(KEY_GEMINI_API, key.trim()).apply()
    }

    fun getApiKey(): String? = prefs.getString(KEY_GEMINI_API, null)

    fun hasApiKey(): Boolean = !getApiKey().isNullOrBlank()

    fun clearApiKey() {
        prefs.edit().remove(KEY_GEMINI_API).apply()
    }

    companion object {
        private const val KEY_GEMINI_API = "gemini_api_key"
    }
}
