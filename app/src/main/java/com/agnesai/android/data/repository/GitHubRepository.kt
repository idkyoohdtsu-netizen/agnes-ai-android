package com.agnesai.android.data.repository

import com.agnesai.android.data.model.FileItem
import com.agnesai.android.data.model.GitHubConfig

/**
 * Repository interface for GitHub operations (used as memory/file storage).
 * Implementation will be provided by Agent 2.
 */
interface GitHubRepository {
    /**
     * List files in a directory in the configured GitHub repo.
     * @param config GitHub configuration
     * @param path Directory path within the repo
     * @return Result with list of FileItems
     */
    suspend fun listFiles(
        config: GitHubConfig,
        path: String = ""
    ): Result<List<FileItem>>

    /**
     * Download a file from GitHub.
     * @param config GitHub configuration
     * @param filePath Path to the file within the repo
     * @return Result with file content as ByteArray
     */
    suspend fun downloadFile(
        config: GitHubConfig,
        filePath: String
    ): Result<ByteArray>

    /**
     * Upload a file to GitHub repo.
     * @param config GitHub configuration
     * @param filePath Target path within the repo
     * @param content File content as ByteArray
     * @param commitMessage Git commit message
     * @return Result with the created FileItem
     */
    suspend fun uploadFile(
        config: GitHubConfig,
        filePath: String,
        content: ByteArray,
        commitMessage: String = "Upload via Agnes AI"
    ): Result<FileItem>

    /**
     * Delete a file from the GitHub repo.
     * @param config GitHub configuration
     * @param filePath Path to file within the repo
     * @param sha Current SHA of the file (required by GitHub API)
     * @return Result<Unit>
     */
    suspend fun deleteFile(
        config: GitHubConfig,
        filePath: String,
        sha: String
    ): Result<Unit>

    /**
     * Save conversation memory to GitHub.
     * @param config GitHub configuration
     * @param conversationId Unique identifier for the conversation
     * @param messages Serialized conversation history
     * @return Result<Unit>
     */
    suspend fun saveMemory(
        config: GitHubConfig,
        conversationId: String,
        messages: String
    ): Result<Unit>

    /**
     * Load conversation memory from GitHub.
     * @param config GitHub configuration
     * @param conversationId Unique identifier for the conversation
     * @return Result with serialized conversation history
     */
    suspend fun loadMemory(
        config: GitHubConfig,
        conversationId: String
    ): Result<String>
}
