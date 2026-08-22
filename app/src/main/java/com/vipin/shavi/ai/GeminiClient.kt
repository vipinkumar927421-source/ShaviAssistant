package com.vipin.shavi.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class GeminiResult {
    data class Success(val text: String) : GeminiResult()
    data class Error(val userMessage: String, val cause: Throwable? = null) : GeminiResult()
}

/**
 * Thin wrapper around the Gemini generateContent REST endpoint.
 * The API key is read fresh from SecureKeyStore for every call and
 * is never logged (not even on failure — errors are sanitized).
 */
class GeminiClient(
    private val apiKeyProvider: () -> String?
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val model = "gemini-3.6-flash"

    private val systemPrompt = """
        You are Shavi, a cute, soft-spoken, friendly and intelligent personal AI assistant
        with a playful, innocent young-girl personality. You understand Hindi, English and
        Hinglish, and you reply naturally in whichever of these the user used. Keep replies
        concise and conversational since they will be spoken aloud via text-to-speech.
    """.trimIndent()

    // Keeps the last few turns for short-term conversational context.
    private val history = mutableListOf<Pair<String, String>>() // (role, text)

    suspend fun send(userText: String): GeminiResult = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider()
        if (apiKey.isNullOrBlank()) {
            return@withContext GeminiResult.Error(
                "Gemini API key nahi mila. Please settings me apni key add karein."
            )
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val contents = JSONArray()
            history.takeLast(10).forEach { (role, text) ->
                contents.put(JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().put(JSONObject().put("text", text)))
                })
            }
            contents.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", userText)))
            })

            val body = JSONObject().apply {
                put("system_instruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
                })
                put("contents", contents)
            }

            val request = Request.Builder()
                .url(url)
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()

                when (response.code) {
                    200 -> {
                        val text = parseText(bodyStr)
                        if (text != null) {
                            history.add("user" to userText)
                            history.add("model" to text)
                            GeminiResult.Success(text)
                        } else {
                            GeminiResult.Error("Shavi ko response samajh nahi aaya. Phir se try karein.")
                        }
                    }
                    401, 403 -> GeminiResult.Error("API key invalid ya expired lag rahi hai. Settings me check karein.")
                    429 -> GeminiResult.Error("Rate limit exceeded. Thodi der baad try karein.")
                    in 500..599 -> GeminiResult.Error("Gemini AI service abhi temporarily unavailable hai. Please thodi der baad try karein.")
                    else -> GeminiResult.Error("Kuch गड़बड़ hui (code ${response.code}). Please dobara try karein.")
                }
            }
        } catch (e: IOException) {
            GeminiResult.Error("Network se connect nahi ho pa raha. Internet check karein.", e)
        } catch (e: Exception) {
            GeminiResult.Error("Ek unexpected error aayi. Please phir se try karein.", e)
        }
    }

    private fun parseText(rawJson: String): String? = try {
        val json = JSONObject(rawJson)
        val candidates = json.optJSONArray("candidates")
        candidates?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text")
    } catch (e: Exception) {
        null
    }

    fun resetConversation() = history.clear()
}
