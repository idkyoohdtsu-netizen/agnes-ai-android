package com.agnesai.android.data.repository.impl

import android.util.Base64
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
import java.io.File
import java.util.concurrent.TimeUnit

class AiRepositoryImpl : AiRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun buildMessagesArray(messages: List<Message>): JSONArray {
        val array = JSONArray()
        messages.forEach { msg ->
            array.put(JSONObject().apply {
                put("role", if (msg.role == MessageRole.USER) "user" else "assistant")
                put("content", msg.content)
            })
        }
        return array
    }

    private fun buildRequestBody(messagesArray: JSONArray, config: TokenConfig): String {
        return JSONObject().apply {
            put("model", config.model)
            put("messages", messagesArray)
            put("max_tokens", config.maxTokens)
            put("temperature", config.temperature.toDouble())
            put("stream", true)
        }.toString()
    }

    private fun parseStreamChunk(line: String): String {
        if (!line.startsWith("data: ")) return ""
        val data = line.removePrefix("data: ").trim()
        if (data == "[DONE]") return ""
        return try {
            val json = JSONObject(data)
            val choices = json.optJSONArray("choices") ?: return ""
            if (choices.length() == 0) return ""
            val delta = choices.getJSONObject(0).optJSONObject("delta") ?: return ""
            delta.optString("content", "")
        } catch (_: Exception) {
            ""
        }
    }

    override suspend fun sendMessage(
        messages: List<Message>,
        config: TokenConfig
    ): Flow<String> = flow {
        val requestBody = buildRequestBody(buildMessagesArray(messages), config)

        val request = Request.Builder()
            .url("${config.baseUrl.trimEnd('/')}/chat/completions")
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            throw Exception("API error ${response.code}: $errorBody")
        }

        val reader = response.body!!.source().inputStream().bufferedReader()
        try {
            var line = reader.readLine()
            while (line != null) {
                val chunk = parseStreamChunk(line)
                if (chunk.isNotEmpty()) emit(chunk)
                line = reader.readLine()
            }
        } finally {
            reader.close()
        }
    }

    override suspend fun sendMessageWithAttachments(
        messages: List<Message>,
        config: TokenConfig,
        attachmentPaths: List<String>
    ): Flow<String> = flow {
        val messagesArray = JSONArray()

        // Add all messages except the last user message
        messages.dropLast(1).forEach { msg ->
            messagesArray.put(JSONObject().apply {
                put("role", if (msg.role == MessageRole.USER) "user" else "assistant")
                put("content", msg.content)
            })
        }

        // Build last user message with file attachments as multipart content
        val lastMsg = messages.last()
        val contentArray = JSONArray()

        if (lastMsg.content.isNotBlank()) {
            contentArray.put(JSONObject().apply {
                put("type", "text")
                put("text", lastMsg.content)
            })
        }

        attachmentPaths.forEach { path ->
            val file = File(path)
            if (!file.exists()) return@forEach
            val ext = file.extension.lowercase()
            if (ext in listOf("jpg", "jpeg", "png", "gif", "webp")) {
                val bytes = file.readBytes()
                val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                val mime = when (ext) {
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    "gif" -> "image/gif"
                    else -> "image/webp"
                }
                contentArray.put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().apply {
                        put("url", "data:$mime;base64,$b64")
                    })
                })
            } else {
                try {
                    val text = file.readText()
                    contentArray.put(JSONObject().apply {
                        put("type", "text")
                        put("text", "[File: ${file.name}]\n```\n${text.take(8000)}\n```")
                    })
                } catch (_: Exception) {
                    contentArray.put(JSONObject().apply {
                        put("type", "text")
                        put("text", "[Binary file: ${file.name}, ${file.length()} bytes]")
                    })
                }
            }
        }

        messagesArray.put(JSONObject().apply {
            put("role", "user")
            put("content", contentArray)
        })

        val requestBody = buildRequestBody(messagesArray, config)

        val request = Request.Builder()
            .url("${config.baseUrl.trimEnd('/')}/chat/completions")
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            throw Exception("API error ${response.code}: $errorBody")
        }

        val reader = response.body!!.source().inputStream().bufferedReader()
        try {
            var line = reader.readLine()
            while (line != null) {
                val chunk = parseStreamChunk(line)
                if (chunk.isNotEmpty()) emit(chunk)
                line = reader.readLine()
            }
        } finally {
            reader.close()
        }
    }

    override suspend fun getAvailableModels(config: TokenConfig): Result<List<String>> {
        return try {
            val request = Request.Builder()
                .url("${config.baseUrl.trimEnd('/')}/models")
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("User-Agent", "Agnes-AI-Android")
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
