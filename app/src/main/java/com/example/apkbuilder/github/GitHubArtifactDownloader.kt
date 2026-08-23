package com.example.apkbuilder.github

import android.content.Context
import java.io.File
import java.util.zip.ZipFile

data class GitHubApkDownloadResult(
    val apk: File,
    val artifactZip: File,
    val extractedDirectory: File
)

class GitHubArtifactDownloader(
    private val context: Context,
    private val api: GitHubApi
) {

    fun downloadApk(
        owner: String,
        repository: String,
        artifactId: Long
    ): Result<GitHubApkDownloadResult> =
        runCatching {

            val root =
                File(
                    context.cacheDir,
                    "github-artifacts/$artifactId"
                )

            root.deleteRecursively()
            root.mkdirs()

            val zipFile =
                File(
                    root,
                    "artifact-$artifactId.zip"
                )

            val extractedDirectory =
                File(
                    root,
                    "extracted"
                )

            extractedDirectory.mkdirs()

            val url =
                "https://api.github.com/repos/" +
                    "$owner/$repository/" +
                    "actions/artifacts/$artifactId/zip"

            api.download(
                url = url,
                destination = zipFile
            ).getOrThrow()

            extractSafely(
                zipFile = zipFile,
                destination = extractedDirectory
            )

            val apk =
                findApk(extractedDirectory)
                    ?: error(
                        "Artifact downloaded, but no APK was found."
                    )

            GitHubApkDownloadResult(
                apk = apk,
                artifactZip = zipFile,
                extractedDirectory = extractedDirectory
            )
        }

    private fun extractSafely(
        zipFile: File,
        destination: File
    ) {

        val destinationCanonical =
            destination.canonicalFile

        ZipFile(zipFile).use { zip ->

            val entries =
                zip.entries()

            while (entries.hasMoreElements()) {

                val entry =
                    entries.nextElement()

                val output =
                    File(
                        destination,
                        entry.name
                    )

                val outputCanonical =
                    output.canonicalFile

                if (
                    outputCanonical != destinationCanonical &&
                    !outputCanonical.path.startsWith(
                        destinationCanonical.path +
                            File.separator
                    )
                ) {
                    error(
                        "Unsafe ZIP entry: ${entry.name}"
                    )
                }

                if (entry.isDirectory) {
                    outputCanonical.mkdirs()
                    continue
                }

                outputCanonical.parentFile?.mkdirs()

                zip.getInputStream(entry).use { input ->
                    outputCanonical.outputStream().use { out ->
                        input.copyTo(out)
                    }
                }
            }
        }
    }

    private fun findApk(
        directory: File
    ): File? =
        directory
            .walkTopDown()
            .firstOrNull {
                it.isFile &&
                    it.extension.equals(
                        "apk",
                        ignoreCase = true
                    )
            }
}
