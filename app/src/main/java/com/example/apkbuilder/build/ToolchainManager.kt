package com.example.apkbuilder.build

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.io.File
import java.util.concurrent.TimeUnit

data class ToolStatus(
    val name: String,
    val available: Boolean,
    val path: String? = null,
    val details: String = ""
)

data class CompatibilityCheck(
    val name: String,
    val passed: Boolean,
    val details: String
)

data class ToolchainReport(
    val supportedArchitecture: Boolean,
    val architecture: String,
    val is64BitAndroid: Boolean,
    val androidVersion: Int,
    val androidRelease: String,
    val deviceModel: String,
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val freeStorageBytes: Long,
    val tools: List<ToolStatus>,
    val checks: List<CompatibilityCheck>,
    val installed: Boolean,
    val toolchainSizeBytes: Long,
    val executionTestsPassed: Boolean
) {

    val readyForLocalBuild: Boolean
        get() =
            supportedArchitecture &&
            is64BitAndroid &&
            installed &&
            tools.all { it.available } &&
            checks.filter { it.name.startsWith("EXECUTION:") }
                .all { it.passed }

    val passedChecks: Int
        get() = checks.count { it.passed }

    val failedChecks: Int
        get() = checks.count { !it.passed }
}

class ToolchainManager(
    private val context: Context
) {

    private val root =
        File(
            context.filesDir,
            "toolchain"
        ).apply {
            mkdirs()
        }

    private val installer =
        ToolchainInstaller(context)

    private fun tool(name: String): File =
        File(root, name)

    private fun executable(name: String): Boolean {
        val file = tool(name)

        return file.isFile &&
            file.canExecute()
    }

    /**
     * Basic device architecture check.
     */
    fun isArm64Device(): Boolean {
        return Build.SUPPORTED_64_BIT_ABIS
            .contains("arm64-v8a")
    }

    /**
     * True when the Android runtime itself is 64-bit.
     *
     * On Android 11+ this uses Process.is64Bit().
     * On older Android versions the presence of a 64-bit ABI
     * is used as the compatibility signal.
     */
    private fun is64BitAndroid(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.os.Process.is64Bit()
        } else {
            Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
        }
    }

    /**
     * Returns a human-readable ABI description.
     */
    fun architecture(): String {
        val supported =
            Build.SUPPORTED_ABIS.joinToString(", ")

        val supported64 =
            Build.SUPPORTED_64_BIT_ABIS
                .joinToString(", ")

        return if (supported.isBlank()) {
            "unknown"
        } else {
            "Primary: ${Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"}; " +
                "64-bit: ${supported64.ifBlank { "none" }}"
        }
    }

    /**
     * Returns the device's total RAM.
     */
    private fun totalRamBytes(): Long {
        val manager =
            context.getSystemService(
                Context.ACTIVITY_SERVICE
            ) as ActivityManager

        val info =
            ActivityManager.MemoryInfo()

        manager.getMemoryInfo(info)

        return info.totalMem
    }

    /**
     * Returns currently available RAM.
     */
    private fun availableRamBytes(): Long {
        val manager =
            context.getSystemService(
                Context.ACTIVITY_SERVICE
            ) as ActivityManager

        val info =
            ActivityManager.MemoryInfo()

        manager.getMemoryInfo(info)

        return info.availMem
    }

    /**
     * Returns available internal storage.
     */
    private fun freeStorageBytes(): Long {
        return try {
            val stats =
                StatFs(
                    context.filesDir.absolutePath
                )

            stats.availableBlocksLong *
                stats.blockSizeLong

        } catch (_: Exception) {
            0L
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "unknown"

        val mb =
            bytes / (1024L * 1024L)

        if (mb < 1024L) {
            return "${mb} MB"
        }

        val gb =
            mb / 1024L

        return "${gb} GB"
    }

    /**
     * Checks that a tool exists and has executable permission.
     *
     * This is deliberately separate from the real execution test.
     */
    private fun fileCheck(
        name: String,
        path: File
    ): ToolStatus {

        if (!path.exists()) {
            return ToolStatus(
                name = name,
                available = false,
                path = path.absolutePath,
                details = "File does not exist."
            )
        }

        if (!path.isFile) {
            return ToolStatus(
                name = name,
                available = false,
                path = path.absolutePath,
                details = "Path exists but is not a file."
            )
        }

        if (!path.canExecute()) {
            return ToolStatus(
                name = name,
                available = false,
                path = path.absolutePath,
                details = "File is not executable."
            )
        }

        return ToolStatus(
            name = name,
            available = true,
            path = path.absolutePath,
            details = "Executable file found."
        )
    }

    /**
     * Executes an executable and captures its output.
     *
     * This is the important difference from the old implementation:
     * a file being present does NOT mean Android can execute it.
     */
    private fun execute(
        executable: File,
        vararg arguments: String,
        timeoutSeconds: Long = 15L
    ): ExecutionResult {

        if (!executable.isFile) {
            return ExecutionResult(
                false,
                "Executable not found."
            )
        }

        if (!executable.canExecute()) {
            return ExecutionResult(
                false,
                "Executable permission is unavailable."
            )
        }

        return try {

            val command =
                ArrayList<String>().apply {
                    add(executable.absolutePath)
                    addAll(arguments)
                }

            val process =
                ProcessBuilder(command)
                    .directory(executable.parentFile)
                    .redirectErrorStream(true)
                    .start()

            val output =
                process.inputStream
                    .bufferedReader()
                    .use { reader ->
                        reader.readText()
                    }

            val finished =
                process.waitFor(
                    timeoutSeconds,
                    TimeUnit.SECONDS
                )

            if (!finished) {
                process.destroyForcibly()

                return ExecutionResult(
                    false,
                    "Process timed out after ${timeoutSeconds}s."
                )
            }

            val exitCode =
                process.exitValue()

            val cleanOutput =
                output
                    .trim()
                    .take(500)

            if (exitCode == 0) {
                ExecutionResult(
                    true,
                    if (cleanOutput.isBlank()) {
                        "Executed successfully."
                    } else {
                        cleanOutput
                    }
                )
            } else {
                ExecutionResult(
                    false,
                    "Exit code $exitCode" +
                        if (cleanOutput.isBlank()) {
                            ""
                        } else {
                            ": $cleanOutput"
                        }
                )
            }

        } catch (e: Exception) {

            ExecutionResult(
                false,
                "${e.javaClass.simpleName}: " +
                    (e.message ?: "execution failed")
            )
        }
    }

    /**
     * Runs the real toolchain compatibility tests.
     *
     * IMPORTANT:
     * This method can start external processes and should ideally
     * be called from a background thread/coroutine.
     */
    fun runCompatibilityCheck(): ToolchainReport {

        val checks =
            mutableListOf<CompatibilityCheck>()

        val arm64 =
            isArm64Device()

        checks += CompatibilityCheck(
            name = "DEVICE: ARM64 ABI",
            passed = arm64,
            details =
                if (arm64) {
                    "arm64-v8a is supported."
                } else {
                    "arm64-v8a is not supported."
                }
        )

        val bit64 =
            is64BitAndroid()

        checks += CompatibilityCheck(
            name = "DEVICE: 64-bit Android",
            passed = bit64,
            details =
                if (bit64) {
                    "The Android runtime is 64-bit."
                } else {
                    "The Android runtime is 32-bit."
                }
        )

        val ram =
            totalRamBytes()

        val ramPass =
            ram >= 2L * 1024L * 1024L * 1024L

        checks += CompatibilityCheck(
            name = "DEVICE: RAM",
            passed = ramPass,
            details =
                "${formatBytes(ram)} total RAM " +
                    "(2 GB minimum compatibility threshold)."
        )

        val storage =
            freeStorageBytes()

        val storagePass =
            storage >= 3L * 1024L * 1024L * 1024L

        checks += CompatibilityCheck(
            name = "DEVICE: Free storage",
            passed = storagePass,
            details =
                "${formatBytes(storage)} available " +
                    "(3 GB minimum compatibility threshold)."
        )

        val sdk =
            tool("sdk")

        val jdk =
            tool("jdk")

        val aapt2 =
            tool("aapt2")

        val d8 =
            tool("d8")

        val zipalign =
            tool("zipalign")

        val apksigner =
            tool("apksigner")

        val tools =
            listOf(
                fileCheck(
                    "Android SDK",
                    sdk
                ),

                ToolStatus(
                    name = "JDK",
                    available =
                        jdk.isDirectory &&
                            File(
                                jdk,
                                "bin/java"
                            ).isFile &&
                            File(
                                jdk,
                                "bin/java"
                            ).canExecute(),
                    path = jdk.absolutePath,
                    details =
                        if (
                            jdk.isDirectory &&
                            File(
                                jdk,
                                "bin/java"
                            ).canExecute()
                        ) {
                            "JDK directory and Java executable found."
                        } else {
                            "JDK/Java executable missing."
                        }
                ),

                fileCheck(
                    "AAPT2",
                    aapt2
                ),

                fileCheck(
                    "D8",
                    d8
                ),

                fileCheck(
                    "Zipalign",
                    zipalign
                ),

                fileCheck(
                    "Apksigner",
                    apksigner
                )
            )

        val installed =
            installer.isInstalled()

        checks += CompatibilityCheck(
            name = "TOOLCHAIN: Installed",
            passed = installed,
            details =
                if (installed) {
                    "Toolchain installation marker and required files exist."
                } else {
                    "Complete toolchain is not installed."
                }
        )

        /*
         * Real execution tests.
         */

        val java =
            File(
                jdk,
                "bin/java"
            )

        val javaResult =
            execute(
                java,
                "-version"
            )

        checks += CompatibilityCheck(
            name = "EXECUTION: Java",
            passed = javaResult.success,
            details = javaResult.message
        )

        val aaptResult =
            execute(
                aapt2,
                "version"
            )

        checks += CompatibilityCheck(
            name = "EXECUTION: AAPT2",
            passed = aaptResult.success,
            details = aaptResult.message
        )

        /*
         * D8 normally accepts --version.
         */
        val d8Result =
            execute(
                d8,
                "--version"
            )

        checks += CompatibilityCheck(
            name = "EXECUTION: D8",
            passed = d8Result.success,
            details = d8Result.message
        )

        val zipalignResult =
            execute(
                zipalign,
                "-h"
            )

        checks += CompatibilityCheck(
            name = "EXECUTION: Zipalign",
            passed = zipalignResult.success,
            details = zipalignResult.message
        )

        /*
         * Apksigner normally supports --version.
         */
        val apksignerResult =
            execute(
                apksigner,
                "--version"
            )

        checks += CompatibilityCheck(
            name = "EXECUTION: Apksigner",
            passed = apksignerResult.success,
            details = apksignerResult.message
        )

        val executionChecks =
            checks.filter {
                it.name.startsWith(
                    "EXECUTION:"
                )
            }

        val executionPassed =
            executionChecks.isNotEmpty() &&
                executionChecks.all {
                    it.passed
                }

        checks += CompatibilityCheck(
            name = "LOCAL BUILD: Complete toolchain execution",
            passed =
                arm64 &&
                    bit64 &&
                    installed &&
                    executionPassed,
            details =
                if (
                    arm64 &&
                    bit64 &&
                    installed &&
                    executionPassed
                ) {
                    "The installed ARM64 toolchain successfully executed."
                } else {
                    "One or more required device/toolchain tests failed."
                }
        )

        return ToolchainReport(
            supportedArchitecture = arm64,
            architecture = architecture(),
            is64BitAndroid = bit64,
            androidVersion =
                Build.VERSION.SDK_INT,
            androidRelease =
                Build.VERSION.RELEASE ?: "unknown",
            deviceModel =
                "${Build.MANUFACTURER} ${Build.MODEL}",
            totalRamBytes = ram,
            availableRamBytes =
                availableRamBytes(),
            freeStorageBytes = storage,
            tools = tools,
            checks = checks,
            installed = installed,
            toolchainSizeBytes =
                directorySize(root),
            executionTestsPassed =
                executionPassed
        )
    }

    /**
     * Compatibility alias for existing code.
     *
     * Existing BuildPreviewActivity currently calls:
     *
     *     engine.validate(project)
     *
     * so this manager does not interfere with LocalBuildEngine.
     */
    fun inspect(): ToolchainReport {
        return runCompatibilityCheck()
    }

    fun toolchainDirectory(): File =
        root

    fun installer(): ToolchainInstaller =
        installer

    fun localBuildAvailable(): Boolean =
        runCompatibilityCheck()
            .readyForLocalBuild

    fun missingTools(): List<ToolStatus> =
        inspect()
            .tools
            .filterNot {
                it.available
            }

    fun resetToolchain(): Boolean {
        return try {
            root.deleteRecursively()
            root.mkdirs()
            true
        } catch (_: Exception) {
            false
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

    private data class ExecutionResult(
        val success: Boolean,
        val message: String
    )
}
