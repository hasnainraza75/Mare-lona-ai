package com.example.data.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class AiAssistantRepository {

    private val defaultModelName = "gemini-3.8-flash"

    // Cache initialized models lazily
    private val modelsMap = mutableMapOf<String, GenerativeModel>()

    private fun getModel(modelName: String): GenerativeModel {
        val resolvedName = if (modelName.isBlank()) defaultModelName else modelName
        return modelsMap.getOrPut(resolvedName) {
            Firebase.ai.generativeModel(
                modelName = resolvedName,
                systemInstruction = content {
                    text(
                        "You are LONA AI, a high-intelligence cybernetic AI assistant and coding copilot. " +
                        "You provide helpful, concise, accurate, and futuristic responses. " +
                        "When outputting code, provide syntactically clean, robust code with clear structure. " +
                        "Support markdown formatting, headings, and code blocks."
                    )
                }
            )
        }
    }

    /**
     * Streams AI response chunk by chunk for ultra-fast TTFT.
     */
    fun streamResponse(prompt: String, modelName: String): Flow<String> = flow {
        val model = getModel(modelName)
        try {
            withTimeout(60_000L) {
                model.generateContentStream(prompt).collect { chunk ->
                    val text = chunk.text.orEmpty()
                    if (text.isNotEmpty()) {
                        emit(text)
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e("AiRepository", "Streaming timed out", e)
            emit("\n[LONA AI Warning: Response timed out after 60 seconds.]")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("AiRepository", "Streaming generation failed", e)
            val errorMsg = e.localizedMessage ?: "Unknown AI generation error."
            emit("\n[LONA AI System Error: $errorMsg]")
        }
    }

    /**
     * Unary response generation with 60s timeout.
     */
    suspend fun generateResponse(prompt: String, modelName: String): Result<String> {
        return try {
            val model = getModel(modelName)
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val text = response.text
            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(Exception("Model returned an empty response."))
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("Generation timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Direct Gemini API test check if user configures a custom API Key on the API setup screen.
     * Checks Google AI Studio API directly via HTTPS test ping.
     */
    suspend fun testCustomApiKey(apiKey: String): Result<Pair<Boolean, String>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("API key cannot be empty."))
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            val code = response.code
            val body = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                Result.success(Pair(true, "API connection verified! Gemini models list returned successfully (HTTP 200)."))
            } else {
                Result.failure(Exception("HTTP $code: Invalid API Key or access denied. Error: ${body.take(150)}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error connecting to Gemini API: ${e.localizedMessage}"))
        }
    }
}
