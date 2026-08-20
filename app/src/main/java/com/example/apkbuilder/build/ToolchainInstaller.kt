package com.example.apkbuilder.build

import android.content.Context
import android.os.Build
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

enum class InstallState {
    SUCCESS,
    ALREADY_INSTALLED,
    UNSUPPORTED_DEVICE,
    DOWNLOAD_FAILED,
    CHECKSUM_FAILED,
    EXTRACTION_FAILED,
    VERIFICATION_FAILED,
    CANCELLED,
    FAILED
}

data class InstallResult(
    val state: InstallState,
    val message: String,
    val installedBytes: Long = 0L
)

data class ToolchainPackage(
    val name: String,
    val url: String,
    val sha256: String,
    val required: Boolean = true
)

class ToolchainInstaller(
    private val context: Context
) {

    private val root =
        File(context.filesDir, "toolchain").apply {
            mkdirs()
        }

    private val downloadDir =
        File(root, ".downloads").apply {
            mkdirs()
        }

    private val marker =
        File(root, ".installed")

    private val requiredFiles =
        listOf(
            "jdk",
            "sdk",
            "aapt2",
            "d8",
            "zipalign",
            "apksigner"
        )

    private val executableNames =
        listOf(
            "aapt2",
            "d8",
            "zipalign",
            "apksigner"
        )

    private fun isArm64(): Boolean =
        Build.SUPPORTED_64_BIT_ABIS.contains("arm64-v8a")

    private fun requireArm64() {
        check(isArm64()) {
            "This local toolchain requires an ARM64 Android device."
        }
    }

    fun toolchainDirectory(): File =
        root

    fun isInstalled(): Boolean {
        if (!marker.isFile) {
            return false
        }

        if (!verifyLayout(root)) {
            return false
        }

        return try {
            verifyExecutables(root)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun installFromZip(
        zipFile: File,
        expectedSha256: String? = null,
        onProgress: (Int) -> Unit = {}
    ): InstallResult {

        return try {
            requireArm64()

            if (!zipFile.isFile) {
                return InstallResult(
                    InstallState.DOWNLOAD_FAILED,
                    "Toolchain ZIP does not exist: ${zipFile.absolutePath}"
                )
            }

            onProgress(2)

            if (expectedSha256 != null) {
                val expected =
                    expectedSha256.trim().lowercase()

                if (!expected.matches(Regex("[0-9a-f]{64}"))) {
                    return InstallResult(
                        InstallState.CHECKSUM_FAILED,
                        "Invalid SHA-256 checksum."
                    )
                }

                val actual =
                    sha256(zipFile)

                if (!actual.equals(expected, ignoreCase = true)) {
                    return InstallResult(
                        InstallState.CHECKSUM_FAILED,
                        "Toolchain checksum mismatch."
                    )
                }
            }

            onProgress(10)

            val staging =
                File(
                    root,
                    ".staging-${System.currentTimeMillis()}"
                )

            if (staging.exists()) {
                staging.deleteRecursively()
            }

            staging.mkdirs()

            try {
                extractZipSafely(
                    zipFile,
                    staging,
                    onProgress = { progress ->
                        onProgress(
                            10 + progress.coerceIn(0, 100) * 50 / 100
                        )
                    }
                )

                onProgress(65)

                val normalized =
                    normalizeToolchainRoot(staging)

                if (!verifyLayout(normalized)) {
                    return InstallResult(
                        InstallState.VERIFICATION_FAILED,
                        "Toolchain ZIP does not contain the required layout."
                    )
                }

                onProgress(70)

                val candidate =
                    File(
                        root,
                        ".candidate-${System.currentTimeMillis()}"
                    )

                if (candidate.exists()) {
                    candidate.deleteRecursively()
                }

                normalized.copyRecursively(
                    candidate,
                    overwrite = true
                )

                verifyExecutables(candidate)

                onProgress(82)

                val runtime =
                    verifyRuntime(candidate)

                if (!runtime.success) {
                    candidate.deleteRecursively()

                    return InstallResult(
                        InstallState.VERIFICATION_FAILED,
                        runtime.message
                    )
                }

                onProgress(90)

                requiredFiles.forEach { name ->
                    val source =
                        File(candidate, name)

                    val destination =
                        File(root, name)

                    if (destination.exists()) {
                        destination.deleteRecursively()
                    }

                    if (source.isDirectory) {
                        source.copyRecursively(
                            destination,
                            overwrite = true
                        )
                    } else {
                        source.copyTo(
                            destination,
                            overwrite = true
                        )
                    }
                }

                candidate.deleteRecursively()

                if (!verifyLayout(root)) {
                    return InstallResult(
                        InstallState.VERIFICATION_FAILED,
                        "Installed toolchain layout verification failed."
                    )
                }

                verifyExecutables(root)

                val finalRuntime =
                    verifyRuntime(root)

                if (!finalRuntime.success) {
                    marker.delete()

                    return InstallResult(
                        InstallState.VERIFICATION_FAILED,
                        finalRuntime.message
                    )
                }

                marker.writeText(
                    "APKBuilderV2 toolchain installed and runtime verified\n" +
                    "ABI=arm64-v8a\n" +
                    "timestamp=${System.currentTimeMillis()}\n"
                )

                onProgress(100)

                InstallResult(
                    InstallState.SUCCESS,
                    "ARM64 toolchain installed and executable runtime verified.",
                    directorySize(root)
                )

            } catch (e: Exception) {
                InstallResult(
                    InstallState.EXTRACTION_FAILED,
                    e.message
                        ?: "Toolchain installation failed."
                )
            } finally {
                staging.deleteRecursively()
            }

        } catch (e: IllegalStateException) {
            InstallResult(
                InstallState.UNSUPPORTED_DEVICE,
                e.message ?: "Unsupported device."
            )
        } catch (e: Exception) {
            InstallResult(
                InstallState.FAILED,
                e.message ?: "Toolchain installation failed."
            )
        }
    }

    fun downloadAndInstall(
        url: String,
        sha256: String,
        onProgress: (Int) -> Unit = {}
    ): InstallResult {

        return try {
            requireArm64()

            require(url.startsWith("https://")) {
                "Only HTTPS toolchain downloads are allowed."
            }

            if (!sha256.trim().matches(Regex("[0-9a-fA-F]{64}"))) {
                return InstallResult(
                    InstallState.CHECKSUM_FAILED,
                    "A valid SHA-256 checksum is required."
                )
            }

            val destination =
                File(
                    downloadDir,
                    "toolchain-${System.currentTimeMillis()}.zip"
                )

            try {
                download(
                    url,
                    destination,
                    onProgress
                )

                installFromZip(
                    destination,
                    sha256,
                    onProgress
                )
            } finally {
                destination.delete()
            }

        } catch (e: IllegalStateException) {
            InstallResult(
                InstallState.UNSUPPORTED_DEVICE,
                e.message ?: "Unsupported device."
            )
        } catch (e: Exception) {
            InstallResult(
                InstallState.DOWNLOAD_FAILED,
                e.message ?: "Toolchain download failed."
            )
        }
    }

    private fun download(
        urlString: String,
        destination: File,
        onProgress: (Int) -> Unit
    ) {
        val connection =
            URL(urlString)
                .openConnection() as HttpURLConnection

        connection.connectTimeout = 30_000
        connection.readTimeout = 60_000
        connection.requestMethod = "GET"
        connection.instanceFollowRedirects = true

        try {
            val code =
                connection.responseCode

            check(code in 200..299) {
                "Download failed with HTTP $code"
            }

            val total =
                connection.contentLengthLong

            var downloaded = 0L

            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->

                    val buffer =
                        ByteArray(64 * 1024)

                    while (true) {
                        val count =
                            input.read(buffer)

                        if (count <= 0) {
                            break
                        }

                        output.write(
                            buffer,
                            0,
                            count
                        )

                        downloaded += count

                        if (total > 0L) {
                            val percent =
                                ((downloaded * 100L) / total)
                                    .toInt()
                                    .coerceIn(0, 100)

                            onProgress(percent)
                        }
                    }

                    output.flush()
                }
            }

        } finally {
            connection.disconnect()
        }
    }

    private fun sha256(file: File): String {
        val digest =
            MessageDigest.getInstance("SHA-256")

        FileInputStream(file).use { input ->

            val buffer =
                ByteArray(64 * 1024)

            while (true) {
                val count =
                    input.read(buffer)

                if (count <= 0) {
                    break
                }

                digest.update(
                    buffer,
                    0,
                    count
                )
            }
        }

        return digest
            .digest()
            .joinToString("") {
                "%02x".format(it)
            }
    }

    private fun extractZipSafely(
        zipFile: File,
        destination: File,
        onProgress: (Int) -> Unit
    ) {
        ZipInputStream(
            FileInputStream(zipFile)
        ).use { zip ->

            var entryCount = 0

            while (true) {
                val entry =
                    zip.nextEntry
                        ?: break

                val base =
                    destination.canonicalFile

                val target =
                    File(
                        destination,
                        entry.name
                    ).canonicalFile

                require(
                    target.path == base.path ||
                        target.path.startsWith(
                            base.path + File.separator
                        )
                ) {
                    "Unsafe ZIP entry: ${entry.name}"
                }

                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()

                    FileOutputStream(target).use { output ->

                        val buffer =
                            ByteArray(64 * 1024)

                        while (true) {
                            val count =
                                zip.read(buffer)

                            if (count <= 0) {
                                break
                            }

                            output.write(
                                buffer,
                                0,
                                count
                            )
                        }
                    }

                    makeExecutableIfNeeded(target)
                }

                zip.closeEntry()

                entryCount++

                onProgress(
                    (entryCount % 100)
                )
            }
        }
    }

    private fun normalizeToolchainRoot(
        staging: File
    ): File {

        if (verifyLayout(staging)) {
            return staging
        }

        val children =
            staging.listFiles()
                ?: return staging

        if (
            children.size == 1 &&
            children[0].isDirectory
        ) {
            val nested =
                children[0]

            if (verifyLayout(nested)) {
                return nested
            }
        }

        return staging
    }

    private fun verifyLayout(
        directory: File
    ): Boolean {

        return requiredFiles.all { name ->
            File(
                directory,
                name
            ).exists()
        }
    }

    private fun verifyExecutables(
        directory: File
    ) {

        executableNames.forEach { name ->

            val file =
                File(
                    directory,
                    name
                )

            require(file.isFile) {
                "Missing executable: ${file.absolutePath}"
            }

            makeExecutableIfNeeded(file)

            require(file.canExecute()) {
                "Executable permission unavailable: ${file.absolutePath}"
            }
        }

        val java =
            File(
                directory,
                "jdk/bin/java"
            )

        require(java.isFile) {
            "Missing JDK java executable."
        }

        makeExecutableIfNeeded(java)

        require(java.canExecute()) {
            "JDK java executable cannot execute."
        }
    }

    private data class RuntimeCheck(
        val success: Boolean,
        val message: String
    )

    private fun verifyRuntime(
        directory: File
    ): RuntimeCheck {

        return try {

            val java =
                File(
                    directory,
                    "jdk/bin/java"
                )

            val aapt2 =
                File(
                    directory,
                    "aapt2"
                )

            val d8 =
                File(
                    directory,
                    "d8"
                )

            val zipalign =
                File(
                    directory,
                    "zipalign"
                )

            val apksigner =
                File(
                    directory,
                    "apksigner"
                )

            val javaResult =
                execute(
                    java,
                    listOf("-version"),
                    directory,
                    15
                )

            if (!javaResult.success) {
                return RuntimeCheck(
                    false,
                    "JDK runtime test failed: ${javaResult.output}"
                )
            }

            val aaptResult =
                execute(
                    aapt2,
                    listOf("version"),
                    directory,
                    15
                )

            if (!aaptResult.success) {
                return RuntimeCheck(
                    false,
                    "AAPT2 runtime test failed: ${aaptResult.output}"
                )
            }

            val d8Result =
                execute(
                    d8,
                    listOf("--help"),
                    directory,
                    20
                )

            if (!d8Result.success) {
                return RuntimeCheck(
                    false,
                    "D8 runtime test failed: ${d8Result.output}"
                )
            }

            val zipalignResult =
                execute(
                    zipalign,
                    listOf("-h"),
                    directory,
                    15
                )

            if (!zipalignResult.success) {
                return RuntimeCheck(
                    false,
                    "Zipalign runtime test failed: ${zipalignResult.output}"
                )
            }

            val apksignerResult =
                execute(
                    apksigner,
                    listOf("--help"),
                    directory,
                    15
                )

            if (!apksignerResult.success) {
                return RuntimeCheck(
                    false,
                    "Apksigner runtime test failed: ${apksignerResult.output}"
                )
            }

            RuntimeCheck(
                true,
                "All required ARM64 toolchain executables ran successfully."
            )

        } catch (e: Exception) {
            RuntimeCheck(
                false,
                "Toolchain runtime verification failed: ${
                    e.message ?: "unknown error"
                }"
            )
        }
    }

    private data class ProcessResult(
        val success: Boolean,
        val output: String
    )

    private fun execute(
        executable: File,
        arguments: List<String>,
        workingDirectory: File,
        timeoutSeconds: Long
    ): ProcessResult {

        val command =
            mutableListOf(
                executable.absolutePath
            ).apply {
                addAll(arguments)
            }

        val process =
            ProcessBuilder(command)
                .directory(workingDirectory)
                .redirectErrorStream(true)
                .start()

        val output =
            process.inputStream
                .bufferedReader()
                .use { reader ->
                    reader.readText()
                }
                .takeLast(4000)

        val finished =
            process.waitFor(
                timeoutSeconds,
                TimeUnit.SECONDS
            )

        if (!finished) {
            process.destroyForcibly()

            return ProcessResult(
                false,
                "Process timed out after ${timeoutSeconds}s."
            )
        }

        return ProcessResult(
            process.exitValue() == 0,
            output.ifBlank {
                "Process exited with code ${process.exitValue()}."
            }
        )
    }

    private fun makeExecutableIfNeeded(
        file: File
    ) {
        if (!file.isFile) {
            return
        }

        val name =
            file.name.lowercase()

        val executable =
            name in setOf(
                "aapt2",
                "d8",
                "zipalign",
                "apksigner",
                "java",
                "javac",
                "kotlinc"
            )

        if (executable) {
            file.setExecutable(
                true,
                false
            )
        }
    }

    private fun directorySize(
        file: File
    ): Long {

        if (!file.exists()) {
            return 0L
        }

        if (file.isFile) {
            return file.length()
        }

        return file.listFiles()
            ?.sumOf {
                directorySize(it)
            }
            ?: 0L
    }
}
