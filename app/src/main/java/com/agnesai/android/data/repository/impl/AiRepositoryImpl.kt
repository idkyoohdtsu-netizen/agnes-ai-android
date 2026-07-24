package com.agnesai.android.data.repository.impl

import com.agnesai.android.data.model.Message
import com.agnesai.android.data.model.MessageRole
import com.agnesai.android.data.model.TokenConfig
import com.agnesai.android.data.repository.AiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.util.concurrent.TimeUnit
import android.util.Base64

class AiRepositoryImpl : AiRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun sendMessage(
        messages: List<Message>,
        config: TokenConfig
    ): Flow<String> = flow {
        val messagesArray = JSONArray()
        messages.forEach { msg ->
            val msgObj = JSONObject().apply {
                put("role", if (msg.role == MessageRole.USER) "user" else "assistant")
                put("content", msg.content)
            }
            messagesArray.put(msgObj)
        }

        val body = JSONObject().apply {
            put("model", config.model)
            put("messages", messagesArray)
            put("max_tokens", config.maxTokens)
            put("temperature", config.temperature.toDouble())
            put("stream", true)
        }

        val request = Request.Builder()
            .url("${config.baseUrl.trimEnd('/')}/chat/completions")
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            throw Exception("API error ${response.code}: $errorBody")
        }

        val reader: BufferedReader = response.body!!.source().inputStream().bufferedReader()
        reader.use { br ->
            br.forEachLine { line ->
                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ").trim()
                    if (data == "[DONE]") return@forEachLine
                    try {
                        val json = JSONObject(data)
                        val choices = json.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val delta = choices.getJSONObject(0).optJSONObject("delta")
                            val content = delta?.optString("content", "") ?: ""
                            if (content.isNotEmpty()) {
                                kotlinx.coroutines.runBlocking { emit(content) }
                            }
                        }
                    } catch (_: Exception) { /* skip malformed chunks */ }
                }
            }
        }
    }

    override suspend fun sendMessageWithAttachments(
        messages: List<Message>,
        config: TokenConfig,
        attachmentPaths: List<String>
    ): Flow<String> = flow {
        val messagesArray = JSONArray()

        // Add previous messages (non-last)
        messages.dropLast(1).forEach { msg ->
            val msgObj = JSONObject().apply {
                put("role", if (msg.role == MessageRole.USER) "user" else "assistant")
                put("content", msg.content)
            }
            messagesArray.put(msgObj)
        }

        // Last user message with attachments
        val lastMsg = messages.last()
        val contentArray = JSONArray()

        // Text part
        if (lastMsg.content.isNotBlank()) {
            contentArray.put(JSONObject().apply {
                put("type", "text")
                put("text", lastMsg.content)
            })
        }

        // File attachments - add as text summaries or base64 images
        attachmentPaths.forEach { path ->
            val file = File(path)
            if (file.exists()) {
                val ext = file.extension.lowercase()
                if (ext in listOf("jpg", "jpeg", "png", "gif", "webp")) {
                    val bytes = file.readBytes()
                    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val mime = when (ext) {
                        "jpg", "jpeg" -> "image/jpeg"
                        "png" -> "image/png"
                        "gif" -> "image/gif"
                        "webp" -> "image/webp"
                        else -> "image/jpeg"
                    }
                    contentArray.put(JSONObject().apply {
                        put("type", "image_url")
                        put("image_url", JSONObject().apply {
                            put("url", "data:$mime;base64,$b64")
                        })
                    })
                } else {
                    // Text/code files - include as text
                    try {
                        val text = file.readText()
                        contentArray.put(JSONObject().apply {
                            put("type", "text")
                            put("text", "[File: ${file.name}]\n```\n${text.take(8000)}\n```")
                        })
                    } catch (_: Exception) {
                        contentArray.put(JSONObject().apply {
                            put("type", "text")
                            put("text", "[Binary file: ${file.name}, size: ${file.length()} bytes]")
                        })
                    }
                }
            }
        }

        messagesArray.put(JSONObject().apply {
            put("role", "user")
            put("content", contentArray)
        })

        val body = JSONObject().apply {
            put("model", config.model)
            put("messages", messagesArray)
            put("max_tokens", config.maxTokens)
            put("temperature", config.temperature.toDouble())
            put("stream", true)
        }

        val request = Request.Builder()
            .url("${config.baseUrl.trimEnd('/')}/chat/completions")
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            throw Exception("API error ${response.code}: $errorBody")
        }

        val reader: BufferedReader = response.body!!.source().inputStream().bufferedReader()
        reader.use { br ->
            br.forEachLine { line ->
                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ").trim()
                    if (data == "[DONE]") return@forEachLine
                    try {
                        val json = JSONObject(data)
                        val choices = json.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val delta = choices.getJSONObject(0).optJSONObject("delta")
                            val content = delta?.optString("content", "") ?: ""
                            if (content.isNotEmpty()) {
                                kotlinx.coroutines.runBlocking { emit(content) }
                            }
                        }
                    } catch (_: Exception) { }
                }
            }
        }
    }

    override suspend fun getAvailableModels(config: TokenConfig): Result<List<String>> {
        return try {
            val request = Request.Builder()
                .url("${config.baseUrl.trimEnd('/')}/models")
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return Result.failure(Exception("Failed to fetch models: ${response.code}"))
            }

            val json = JSONObject(response.body!!.string())
            val dataArray = json.optJSONArray("data")
            val models = mutableListOf<String>()
            if (dataArray != null) {
                for (i in 0 until dataArray.length()) {
                    val id = dataArray.getJSONObject(i).optString("id")
                    if (id.isNotBlank()) models.add(id)
                }
            }
            Result.success(models.sorted())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
