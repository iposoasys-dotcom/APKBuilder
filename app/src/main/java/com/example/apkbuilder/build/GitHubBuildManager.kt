package com.example.apkbuilder.build

import android.content.Context
import java.io.File

data class GitHubBuildResult(
    val success: Boolean,
    val message: String,
    val apk: File? = null
)

class GitHubBuildManager(
    private val context: Context
) {

    fun isAvailable(): Boolean = true

    fun prepareProject(project: File): File {
        require(project.exists() && project.isDirectory) {
            "Project directory does not exist."
        }

        val exportDir = File(
            context.cacheDir,
            "github-build-${System.currentTimeMillis()}"
        )

        exportDir.deleteRecursively()
        exportDir.mkdirs()

        project.copyRecursively(
            target = exportDir,
            overwrite = true
        )

        return exportDir
    }

    fun build(project: File): GitHubBuildResult {
        return try {
            val exported = prepareProject(project)

            GitHubBuildResult(
                success = false,
                message = "Project prepared for GitHub Actions: ${exported.absolutePath}"
            )
        } catch (e: Exception) {
            GitHubBuildResult(
                success = false,
                message = e.message ?: "Could not prepare project."
            )
        }
    }
}
