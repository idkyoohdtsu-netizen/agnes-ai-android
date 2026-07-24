package com.agnesai.android.data.repository.impl

import android.util.Base64
import com.agnesai.android.data.model.FileItem
import com.agnesai.android.data.model.GitHubConfig
import com.agnesai.android.data.repository.GitHubRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GitHubRepositoryImpl : GitHubRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun buildRequest(config: GitHubConfig, path: String): Request.Builder {
        val url = "${config.apiBaseUrl}/$path".replace("//", "/")
            .replace("https:/", "https://")
        return Request.Builder()
            .url(url)
            .addHeader("Authorization", "token ${config.token}")
            .addHeader("Accept", "application/vnd.github.v3+json")
            .addHeader("User-Agent", "Agnes-AI-Android")
    }

    override suspend fun listFiles(
        config: GitHubConfig,
        path: String
    ): Result<List<FileItem>> {
        return try {
            val encodedPath = if (path.isBlank()) "contents" else "contents/$path"
            val request = buildRequest(config, encodedPath)
                .addHeader("ref", config.branch)
                .url("${config.apiBaseUrl}/$encodedPath?ref=${config.branch}")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "[]"

            if (!response.isSuccessful) {
                return Result.failure(Exception("GitHub API error ${response.code}: $body"))
            }

            val array = JSONArray(body)
            val items = mutableListOf<FileItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                items.add(
                    FileItem(
                        name = obj.optString("name"),
                        path = obj.optString("path"),
                        size = obj.optLong("size", 0L),
                        downloadUrl = obj.optString("download_url", ""),
                        sha = obj.optString("sha", ""),
                        type = obj.optString("type", "file"),
                        htmlUrl = obj.optString("html_url", "")
                    )
                )
            }
            Result.success(items.sortedWith(compareBy({ it.type != "dir" }, { it.name })))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadFile(
        config: GitHubConfig,
        filePath: String
    ): Result<ByteArray> {
        return try {
            // First get the file metadata to get download_url
            val metaRequest = Request.Builder()
                .url("${config.apiBaseUrl}/contents/$filePath?ref=${config.branch}")
                .addHeader("Authorization", "token ${config.token}")
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("User-Agent", "Agnes-AI-Android")
                .get()
                .build()

            val metaResponse = client.newCall(metaRequest).execute()
            val metaBody = metaResponse.body?.string() ?: ""
            if (!metaResponse.isSuccessful) {
                return Result.failure(Exception("Failed to get file metadata: ${metaResponse.code}"))
            }

            val metaJson = JSONObject(metaBody)
            // For small files, content is embedded as base64
            val encodedContent = metaJson.optString("content", "")
            if (encodedContent.isNotBlank()) {
                val cleaned = encodedContent.replace("\n", "").replace("\r", "")
                val bytes = Base64.decode(cleaned, Base64.DEFAULT)
                return Result.success(bytes)
            }

            // For large files, use download_url
            val downloadUrl = metaJson.optString("download_url", "")
            if (downloadUrl.isBlank()) {
                return Result.failure(Exception("No download URL available"))
            }

            val downloadRequest = Request.Builder()
                .url(downloadUrl)
                .addHeader("Authorization", "token ${config.token}")
                .addHeader("User-Agent", "Agnes-AI-Android")
                .get()
                .build()

            val downloadResponse = client.newCall(downloadRequest).execute()
            if (!downloadResponse.isSuccessful) {
                return Result.failure(Exception("Download failed: ${downloadResponse.code}"))
            }

            Result.success(downloadResponse.body!!.bytes())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadFile(
        config: GitHubConfig,
        filePath: String,
        content: ByteArray,
        commitMessage: String
    ): Result<FileItem> {
        return try {
            // Check if file exists to get its SHA (needed for updates)
            val existingSha = getFileSha(config, filePath)

            val b64Content = Base64.encodeToString(content, Base64.NO_WRAP)
            val bodyObj = JSONObject().apply {
                put("message", commitMessage)
                put("content", b64Content)
                put("branch", config.branch)
                if (existingSha != null) put("sha", existingSha)
            }

            val request = Request.Builder()
                .url("${config.apiBaseUrl}/contents/$filePath")
                .addHeader("Authorization", "token ${config.token}")
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("User-Agent", "Agnes-AI-Android")
                .put(bodyObj.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return Result.failure(Exception("Upload failed ${response.code}: $responseBody"))
            }

            val json = JSONObject(responseBody)
            val fileObj = json.optJSONObject("content") ?: JSONObject()
            Result.success(
                FileItem(
                    name = fileObj.optString("name", filePath.substringAfterLast('/')),
                    path = fileObj.optString("path", filePath),
                    size = content.size.toLong(),
                    sha = fileObj.optString("sha", ""),
                    downloadUrl = fileObj.optString("download_url", ""),
                    htmlUrl = fileObj.optString("html_url", "")
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteFile(
        config: GitHubConfig,
        filePath: String,
        sha: String
    ): Result<Unit> {
        return try {
            val bodyObj = JSONObject().apply {
                put("message", "Delete $filePath via Agnes AI")
                put("sha", sha)
                put("branch", config.branch)
            }

            val request = Request.Builder()
                .url("${config.apiBaseUrl}/contents/$filePath")
                .addHeader("Authorization", "token ${config.token}")
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("User-Agent", "Agnes-AI-Android")
                .delete(bodyObj.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                return Result.failure(Exception("Delete failed ${response.code}: $errorBody"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveMemory(
        config: GitHubConfig,
        conversationId: String,
        messages: String
    ): Result<Unit> {
        return try {
            val path = "${config.memoryPath}/$conversationId.json"
            val content = messages.toByteArray(Charsets.UTF_8)
            uploadFile(config, path, content, "Save memory: $conversationId").map { Unit }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loadMemory(
        config: GitHubConfig,
        conversationId: String
    ): Result<String> {
        return try {
            val path = "${config.memoryPath}/$conversationId.json"
            downloadFile(config, path).map { bytes ->
                bytes.toString(Charsets.UTF_8)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getFileSha(config: GitHubConfig, filePath: String): String? {
        return try {
            val request = Request.Builder()
                .url("${config.apiBaseUrl}/contents/$filePath?ref=${config.branch}")
                .addHeader("Authorization", "token ${config.token}")
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("User-Agent", "Agnes-AI-Android")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            JSONObject(body).optString("sha").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
