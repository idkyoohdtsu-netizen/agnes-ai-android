package com.agnesai.android.data.model

data class FileItem(
    val name: String,
    val path: String,
    val size: Long = 0L,
    val downloadUrl: String = "",
    val sha: String = "",
    val type: String = "file",
    val htmlUrl: String = ""
) {
    val displaySize: String
        get() = when {
            size < 1024 -> "${size}B"
            size < 1024 * 1024 -> "${size / 1024}KB"
            else -> String.format("%.1fMB", size / (1024.0 * 1024.0))
        }

    val extension: String
        get() = name.substringAfterLast('.', "").lowercase()

    val isImage: Boolean
        get() = extension in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg")

    val isDocument: Boolean
        get() = extension in listOf("pdf", "doc", "docx", "txt", "md", "csv", "xlsx")

    val isCode: Boolean
        get() = extension in listOf("kt", "java", "py", "js", "ts", "html", "css", "json", "xml", "gradle")
}
