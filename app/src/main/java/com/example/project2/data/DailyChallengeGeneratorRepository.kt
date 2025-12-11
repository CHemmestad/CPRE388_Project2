package com.example.project2.data

import android.util.Log
import com.example.project2.BuildConfig
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class DailyChallengeGenerationResult(
    val documentId: String,
    val rawJson: String
)

/**
 * Generates daily challenge JSON via Gemini and persists the result to Firestore.
 */
class DailyChallengeGeneratorRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(90, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {

    /**
     * Generate a challenge from Gemini and save the payload to Firestore.
     *
     * @param prompt structured prompt text for Gemini
     * @return id of the stored document and the raw JSON returned
     */
    suspend fun generateAndStore(prompt: String): DailyChallengeGenerationResult {
        val trimmedPrompt = prompt.trim()
        require(trimmedPrompt.isNotBlank()) { "Prompt must not be blank" }

        val geminiResponse = requestGemini(trimmedPrompt)
        val jsonObject = extractJsonPayload(geminiResponse)
        val documentId = saveToFirestore(trimmedPrompt, jsonObject)

        return DailyChallengeGenerationResult(
            documentId = documentId,
            rawJson = jsonObject.toString()
        )
    }

    /**
     * Call Gemini generateContent with the provided prompt text.
     */
    private suspend fun requestGemini(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        require(apiKey.isNotBlank()) { "Missing GEMINI_API_KEY in local.properties." }

        // Combine system instructions and user prompt into a single text block for Gemini.
        val payload = JSONObject().apply {
            put(
                "contents",
                JSONArray().apply {
                    put(
                        JSONObject().apply {
                            put(
                                "parts",
                                JSONArray().apply {
                                    put(
                                        JSONObject().apply {
                                            put("text", "$SYSTEM_INSTRUCTIONS\n\nUser prompt:\n$prompt")
                                        }
                                    )
                                }
                            )
                        }
                    )
                }
            )
            put(
                "generationConfig",
                JSONObject().apply {
                    put("temperature", 0.8)
                }
            )
        }

        val requestBody = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url("$GEMINI_URL?key=$apiKey")
            .post(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            val bodyString = response.body?.string()
            if (!response.isSuccessful) {
                Log.e("Gemini", "Gemini error ${response.code}: ${bodyString ?: "no body"}")
                throw IOException("Gemini error ${response.code}: ${bodyString ?: "no body"}")
            }
            bodyString ?: throw IOException("Empty Gemini response body")
        }
    }

    /**
     * Parse Gemini response JSON and extract the generated text payload.
     */
    private fun extractJsonPayload(rawResponse: String): JSONObject {
        val root = JSONObject(rawResponse)
        val content = root.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text")
            ?.trim()

        if (content.isNullOrBlank()) {
            throw IllegalStateException("Gemini response did not contain content")
        }

        val cleaned = removeCodeFences(content)
        return JSONObject(cleaned)
    }

    /**
     * Persist the generated JSON to Firestore, overwriting previous daily challenges.
     */
    private suspend fun saveToFirestore(prompt: String, jsonObject: JSONObject): String {
        val data = hashMapOf<String, Any?>(
            "prompt" to prompt,
            "rawJson" to jsonObject.toString(),
            "parsedContent" to jsonObject.toMap(),
            "model" to GEMINI_MODEL,
            "createdAt" to Timestamp.now(),
            "createdBy" to auth.currentUser?.uid
        )

        val collection = firestore.collection(DAILY_CHALLENGES_COLLECTION)
        val existing = collection.get().await()
        firestore.runBatch { batch ->
            existing.documents.forEach { batch.delete(it.reference) }
        }.await()

        collection.document("current").set(data).await()
        return "current"
    }

    /** Convert a JSONObject to a Kotlin Map recursively. */
    private fun JSONObject.toMap(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        val keysIterator = keys()
        while (keysIterator.hasNext()) {
            val key = keysIterator.next()
            val value = opt(key)
            result[key] = when (value) {
                is JSONObject -> value.toMap()
                is JSONArray -> value.toList()
                JSONObject.NULL -> null
                else -> value
            }
        }
        return result
    }

    /** Convert a JSONArray to a Kotlin List recursively. */
    private fun JSONArray.toList(): List<Any?> {
        val list = mutableListOf<Any?>()
        for (index in 0 until length()) {
            val value = opt(index)
            list += when (value) {
                is JSONObject -> value.toMap()
                is JSONArray -> value.toList()
                JSONObject.NULL -> null
                else -> value
            }
        }
        return list
    }

    companion object {
        // Align with working chatbot example (v1beta gemini-2.5-flash)
        private const val GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
        private const val GEMINI_MODEL = "gemini-2.5-flash"
        private const val DAILY_CHALLENGES_COLLECTION = "dailyChallenges"
        private const val SYSTEM_INSTRUCTIONS =
            "You are an elite MindMatch puzzle designer. Act like a user filling out the Mastermind builder. Return only the JSON object with keys: title, description, mastermindConfig { colors, slots, guesses, levels, code }. Allowed colors (use exactly these words): Red, Blue, Green, Yellow, Orange, Purple, Pink, White. Choose 1-8 colors. Slots: 1-8 and must be >= number of chosen colors. Guesses: 1-20. Levels: at least 1. Code length equals slots and uses only the chosen colors (duplicates allowed). Keep description under 50 words. No commentary."
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}

/**
 * Strip Markdown code fences from the model output.
 *
 * @param rawContent Gemini text response
 * @return cleaned JSON string without fences
 */
private fun removeCodeFences(rawContent: String): String {
    var result = rawContent.trim()
    if (result.startsWith("```")) {
        result = result.removePrefix("```json").removePrefix("```JSON").removePrefix("```").trimStart()
    }
    if (result.endsWith("```")) {
        result = result.removeSuffix("```").trimEnd()
    }
    return result.trim()
}
