package com.example.apkbuilder.build

import android.content.Context
import android.os.Build
import java.io.File

enum class LocalBuildStatus {
    READY,
    UNSUPPORTED_32_BIT,
    TOOLCHAIN_REQUIRED,
    INVALID_PROJECT,
    BUILD_FAILED,
    SUCCESS
}

data class BuildStep(
    val name: String,
    val success: Boolean,
    val message: String
)

data class BuildValidation(
    val status: LocalBuildStatus,
    val message: String,
    val steps: List<BuildStep>
)

data class LocalBuildResult(
    val status: LocalBuildStatus,
    val message: String,
    val apk: File? = null
)

class LocalBuildEngine(
    private val context: Context
) {

    private val toolchain =
        ToolchainManager(context)

    fun is64BitDevice(): Boolean =
        Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()

    fun validate(project: File): BuildValidation {

        val steps = mutableListOf<BuildStep>()

        if (!is64BitDevice()) {
            steps += BuildStep(
                "64-bit Android",
                false,
                "This Android environment does not expose a 64-bit ABI."
            )

            return BuildValidation(
                LocalBuildStatus.UNSUPPORTED_32_BIT,
                "A 64-bit Android environment is required for local compilation.",
                steps
            )
        }

        steps += BuildStep(
            "64-bit Android",
            true,
            Build.SUPPORTED_64_BIT_ABIS.joinToString(", ")
        )

        if (!project.exists() || !project.isDirectory) {
            steps += BuildStep(
                "Project directory",
                false,
                "Project directory does not exist."
            )

            return BuildValidation(
                LocalBuildStatus.INVALID_PROJECT,
                "Invalid project directory.",
                steps
            )
        }

        steps += BuildStep(
            "Project directory",
            true,
            project.absolutePath
        )

        val settings =
            File(project, "settings.gradle")

        val settingsKts =
            File(project, "settings.gradle.kts")

        val appGradle =
            File(project, "app/build.gradle")

        val appGradleKts =
            File(project, "app/build.gradle.kts")

        val manifest =
            File(project, "app/src/main/AndroidManifest.xml")

        val hasSettings =
            settings.isFile || settingsKts.isFile

        val hasAppGradle =
            appGradle.isFile || appGradleKts.isFile

        steps += BuildStep(
            "Gradle settings",
            hasSettings,
            if (hasSettings)
                "Gradle settings file found."
            else
                "settings.gradle or settings.gradle.kts is missing."
        )

        steps += BuildStep(
            "App Gradle file",
            hasAppGradle,
            if (hasAppGradle)
                "app Gradle configuration found."
            else
                "app/build.gradle or app/build.gradle.kts is missing."
        )

        steps += BuildStep(
            "AndroidManifest.xml",
            manifest.isFile,
            if (manifest.isFile)
                "Android manifest found."
            else
                "AndroidManifest.xml is missing."
        )

        if (!hasSettings || !hasAppGradle || !manifest.isFile) {
            return BuildValidation(
                LocalBuildStatus.INVALID_PROJECT,
                "Project structure is incomplete.",
                steps
            )
        }

        val report =
            toolchain.inspect()

        report.tools.forEach { tool ->

            steps += BuildStep(
                tool.name,
                tool.available,
                tool.path ?: "Unavailable"
            )
        }

        if (!report.readyForLocalBuild) {

            return BuildValidation(
                LocalBuildStatus.TOOLCHAIN_REQUIRED,
                "64-bit device detected, but the verified local Android toolchain is not ready.",
                steps
            )
        }

        steps += BuildStep(
            "Local build toolchain",
            true,
            "SDK, JDK and required build tools are available."
        )

        return BuildValidation(
            LocalBuildStatus.READY,
            "Everything is ready for a local APK build.",
            steps
        )
    }

    fun status(): LocalBuildStatus =
        if (!is64BitDevice()) {
            LocalBuildStatus.UNSUPPORTED_32_BIT
        } else if (toolchain.localBuildAvailable()) {
            LocalBuildStatus.READY
        } else {
            LocalBuildStatus.TOOLCHAIN_REQUIRED
        }

    fun build(project: File): LocalBuildResult {

        val validation =
            validate(project)

        if (validation.status != LocalBuildStatus.READY) {
            return LocalBuildResult(
                validation.status,
                validation.message
            )
        }

        return try {

            val result =
                runGradleBuild(project)

            if (!result.first) {
                LocalBuildResult(
                    LocalBuildStatus.BUILD_FAILED,
                    result.second
                )
            } else {

                val apk =
                    findGeneratedApk(project)

                if (apk == null) {
                    LocalBuildResult(
                        LocalBuildStatus.BUILD_FAILED,
                        "Build completed but no APK was found."
                    )
                } else {
                    LocalBuildResult(
                        LocalBuildStatus.SUCCESS,
                        "APK built successfully: ${apk.absolutePath}",
                        apk
                    )
                }
            }

        } catch (e: Exception) {

            LocalBuildResult(
                LocalBuildStatus.BUILD_FAILED,
                e.message ?: "Local build failed."
            )
        }
    }

    private fun runGradleBuild(
        project: File
    ): Pair<Boolean, String> {

        val gradlew =
            File(project, "gradlew")

        if (!gradlew.isFile) {
            return false to
                "Project does not contain Gradle Wrapper (gradlew)."
        }

        gradlew.setExecutable(true, false)

        val process =
            ProcessBuilder(
                gradlew.absolutePath,
                ":app:assembleDebug",
                "--no-daemon",
                "--stacktrace"
            )
                .directory(project)
                .redirectErrorStream(true)
                .start()

        val output =
            process.inputStream
                .bufferedReader()
                .use { it.readText() }

        val exitCode =
            process.waitFor()

        return if (exitCode == 0) {
            true to output
        } else {
            false to output.takeLast(12000)
        }
    }

    private fun findGeneratedApk(
        project: File
    ): File? {

        val output =
            File(
                project,
                "app/build/outputs/apk"
            )

        if (!output.isDirectory) {
            return null
        }

        return output
            .walkTopDown()
            .filter {
                it.isFile &&
                    it.extension.equals(
                        "apk",
                        ignoreCase = true
                    )
            }
            .maxByOrNull {
                it.lastModified()
            }
    }
}
