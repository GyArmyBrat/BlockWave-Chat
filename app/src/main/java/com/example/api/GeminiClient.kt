package com.example.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Base function preserved for backward compatibility
    suspend fun generateResponse(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured or is the default placeholder.")
            return@withContext "Error: Gemini API key is missing. Please add it via the Secrets panel in AI Studio."
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()

        val systemInstructionJson = if (systemInstruction != null) {
            """, "systemInstruction": { "parts": [ { "text": "${escapeJson(systemInstruction)}" } ] }"""
        } else {
            ""
        }

        val jsonRequest = """
            {
              "contents": [
                {
                  "parts": [
                    {
                      "text": "${escapeJson(prompt)}"
                    }
                  ]
                }
              ]$systemInstructionJson
            }
        """.trimIndent()

        val requestBody = jsonRequest.toRequestBody(mediaType)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "API call failed with code: ${response.code}, body: $errBody")
                    return@withContext "Error response from AI network: Code ${response.code}."
                }

                val bodyString = response.body?.string() ?: return@withContext "Error: Received empty response from Gemini API."
                return@withContext parseResponse(bodyString)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network failure calling Gemini", e)
            return@withContext "Network error: Unable to connect to Gemini API."
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error calling Gemini", e)
            return@withContext "Error: ${e.localizedMessage}"
        }
    }

    // Dynamic Multi-turn Chat Response with Role and Specific Model
    suspend fun generateChatResponse(
        prompt: String,
        model: String,
        systemInstruction: String?,
        history: List<Pair<String, Boolean>> // Text to isUser (true = user, false = model)
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: Gemini API key is missing. Configure it in AI Studio Secrets."
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()

        // Map chat history to standard contents array
        val contentsJson = history.joinToString(separator = ",") { (text, isUser) ->
            val role = if (isUser) "user" else "model"
            """
            {
              "role": "$role",
              "parts": [
                {
                  "text": "${escapeJson(text)}"
                }
              ]
            }
            """.trimIndent()
        }

        val appendNewPrompt = if (history.isNotEmpty()) "," else ""
        val fullContentsJson = """
            [
              $contentsJson
              $appendNewPrompt
              {
                "role": "user",
                "parts": [
                  {
                    "text": "${escapeJson(prompt)}"
                  }
                ]
              }
            ]
        """.trimIndent()

        val systemInstructionJson = if (systemInstruction != null) {
            """, "systemInstruction": { "parts": [ { "text": "${escapeJson(systemInstruction)}" } ] }"""
        } else {
            ""
        }

        val jsonRequest = """
            {
              "contents": $fullContentsJson
              $systemInstructionJson
            }
        """.trimIndent()

        val requestBody = jsonRequest.toRequestBody(mediaType)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Chat API failed: code=${response.code}, body=$errBody")
                    return@withContext "Error ${response.code}: $errBody"
                }

                val bodyString = response.body?.string() ?: return@withContext "Empty response."
                return@withContext parseResponse(bodyString)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in generateChatResponse", e)
            return@withContext "Error: ${e.localizedMessage}"
        }
    }

    // Image Creation & Editing via gemini-3.1-flash-image-preview
    suspend fun generateImage(prompt: String, aspectRatio: String): String? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") return@withContext null

        val mediaType = "application/json; charset=utf-8".toMediaType()

        val jsonRequest = """
            {
              "contents": [
                {
                  "parts": [
                    {
                      "text": "${escapeJson(prompt)}"
                    }
                  ]
                }
              ],
              "generationConfig": {
                "imageConfig": {
                  "aspectRatio": "$aspectRatio",
                  "imageSize": "1K"
                },
                "responseModalities": ["TEXT", "IMAGE"]
              }
            }
        """.trimIndent()

        val requestBody = jsonRequest.toRequestBody(mediaType)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-image-preview:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyString = response.body?.string() ?: return@withContext null
                return@withContext extractBase64InlineData(bodyString)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception generating image", e)
            return@withContext null
        }
    }

    // Music Generation via lyria-3-clip-preview / lyria-3-pro-preview
    suspend fun generateMusic(prompt: String, isShortClip: Boolean): String? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") return@withContext null

        val modelName = if (isShortClip) "lyria-3-clip-preview" else "lyria-3-pro-preview"
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val jsonRequest = """
            {
              "contents": [
                {
                  "parts": [
                    {
                      "text": "${escapeJson(prompt)}"
                    }
                  ]
                }
              ],
              "generationConfig": {
                "responseModalities": ["AUDIO"]
              }
            }
        """.trimIndent()

        val requestBody = jsonRequest.toRequestBody(mediaType)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyString = response.body?.string() ?: return@withContext null
                return@withContext extractBase64InlineData(bodyString)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception generating music", e)
            return@withContext null
        }
    }

    private fun extractBase64InlineData(jsonResponse: String): String? {
        return try {
            val inlineDataIndex = jsonResponse.indexOf("\"inlineData\"")
            if (inlineDataIndex == -1) return null
            val dataIndex = jsonResponse.indexOf("\"data\"", inlineDataIndex)
            if (dataIndex == -1) return null
            val firstQuote = jsonResponse.indexOf("\"", dataIndex + 6)
            if (firstQuote == -1) return null
            val secondQuote = jsonResponse.indexOf("\"", firstQuote + 1)
            if (secondQuote == -1) return null
            jsonResponse.substring(firstQuote + 1, secondQuote)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseResponse(jsonResponse: String): String {
        return try {
            val candidatesKeyword = "\"candidates\""
            val textKeyword = "\"text\""
            
            if (!jsonResponse.contains(candidatesKeyword)) {
                return "No candidate response."
            }

            val textStartIndex = jsonResponse.indexOf(textKeyword)
            if (textStartIndex == -1) return "No response text found."

            val firstQuoteAfterText = jsonResponse.indexOf("\"", textStartIndex + textKeyword.length + 1)
            if (firstQuoteAfterText == -1) return "No response text found."

            val secondQuoteAfterText = jsonResponse.indexOf("\"", firstQuoteAfterText + 1)
            if (secondQuoteAfterText == -1) return "No response text found."

            val rawText = jsonResponse.substring(firstQuoteAfterText + 1, secondQuoteAfterText)
            unescapeJson(rawText)
        } catch (e: Exception) {
            "Error parsing AI response."
        }
    }

    private fun escapeJson(input: String): String {
        return input.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun unescapeJson(input: String): String {
        return input.replace("\\\\", "\\")
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }
}
