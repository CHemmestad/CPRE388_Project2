package com.example.project2.data

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

class DailyChallengeGeneratorRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(90, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {

    suspend fun generateAndStore(prompt: String): DailyChallengeGenerationResult {
        val trimmedPrompt = prompt.trim()
        require(trimmedPrompt.isNotBlank()) { "Prompt must not be blank" }

        val openAiResponse = requestOpenAi(trimmedPrompt)
        val jsonObject = extractJsonPayload(openAiResponse)
        val documentId = saveToFirestore(trimmedPrompt, jsonObject)

        return DailyChallengeGenerationResult(
            documentId = documentId,
            rawJson = jsonObject.toString()
        )
    }

    private suspend fun requestOpenAi(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.OPENAI_API_KEY
        require(apiKey.isNotBlank()) { "Missing OpenAI API key. Add OPENAI_API_KEY to local.properties." }

        val payload = JSONObject().apply {
            put("model", OPENAI_MODEL)
            put("temperature", 0.8)
            put("messages", JSONArray().apply {
                put(
                    JSONObject().apply {
                        put("role", "system")
                        put("content", SYSTEM_INSTRUCTIONS)
                    }
                )
                put(
                    JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    }
                )
            })
        }

        val requestBody = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(OPENAI_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                throw IOException("OpenAI error ${response.code}: ${errorBody ?: "no body"}")
            }
            response.body?.string() ?: throw IOException("Empty OpenAI response body")
        }
    }

    private fun extractJsonPayload(rawResponse: String): JSONObject {
        val root = JSONObject(rawResponse)
        val content = root.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
            ?.trim()

        if (content.isNullOrBlank()) {
            throw IllegalStateException("OpenAI response did not contain content")
        }

        val cleaned = removeCodeFences(content)
        return JSONObject(cleaned)
    }

    private suspend fun saveToFirestore(prompt: String, jsonObject: JSONObject): String =
        suspendCancellableCoroutine { continuation ->
            val data = hashMapOf<String, Any?>(
                "prompt" to prompt,
                "rawJson" to jsonObject.toString(),
                "parsedContent" to jsonObject.toMap(),
                "model" to OPENAI_MODEL,
                "createdAt" to Timestamp.now(),
                "createdBy" to auth.currentUser?.uid
            )

            firestore.collection(DAILY_CHALLENGES_COLLECTION)
                .add(data)
                .addOnSuccessListener { document ->
                    if (continuation.isActive) {
                        continuation.resume(document.id)
                    }
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(error)
                    }
                }
        }

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
        private const val OPENAI_URL = "https://api.openai.com/v1/chat/completions"
        private const val OPENAI_MODEL = "gpt-4o-mini"
        private const val DAILY_CHALLENGES_COLLECTION = "dailyChallenges"
        private const val SYSTEM_INSTRUCTIONS = "You are an elite MindMatch puzzle designer. Return only the JSON object for DailyPuzzleContent (instructions, grid, controls, stats)."
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}

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
