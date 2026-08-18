package com.example.apkbuilder.build

import android.content.Context
import android.os.Build
import java.io.File

data class ToolStatus(
    val name: String,
    val available: Boolean,
    val path: String? = null
)

data class ToolchainReport(
    val supportedArchitecture: Boolean,
    val architecture: String,
    val tools: List<ToolStatus>,
    val installed: Boolean,
    val toolchainSizeBytes: Long
) {
    val readyForLocalBuild: Boolean
        get() =
            supportedArchitecture &&
            installed &&
            tools.all { it.available }
}

class ToolchainManager(
    private val context: Context
) {

    private val root =
        File(context.filesDir, "toolchain").apply {
            mkdirs()
        }

    private val installer =
        ToolchainInstaller(context)

    private fun tool(name: String): File =
        File(root, name)

    private fun executable(name: String): Boolean {
        val file = tool(name)
        return file.isFile && file.canExecute()
    }

    fun inspect(): ToolchainReport {

        val architecture =
            Build.SUPPORTED_ABIS.firstOrNull()
                ?: "unknown"

        val isArm64 =
            Build.SUPPORTED_64_BIT_ABIS
                .contains("arm64-v8a")

        val sdk =
            File(root, "sdk")

        val jdk =
            File(root, "jdk")

        val tools =
            listOf(
                ToolStatus(
                    name = "Android SDK",
                    available = sdk.isDirectory,
                    path = sdk.absolutePath
                ),

                ToolStatus(
                    name = "JDK",
                    available = jdk.isDirectory &&
                        File(jdk, "bin/java").canExecute(),
                    path = jdk.absolutePath
                ),

                ToolStatus(
                    name = "AAPT2",
                    available = executable("aapt2"),
                    path = tool("aapt2").absolutePath
                ),

                ToolStatus(
                    name = "D8",
                    available = executable("d8"),
                    path = tool("d8").absolutePath
                ),

                ToolStatus(
                    name = "Zipalign",
                    available = executable("zipalign"),
                    path = tool("zipalign").absolutePath
                ),

                ToolStatus(
                    name = "Apksigner",
                    available = executable("apksigner"),
                    path = tool("apksigner").absolutePath
                )
            )

        return ToolchainReport(
            supportedArchitecture = isArm64,
            architecture = architecture,
            tools = tools,
            installed = installer.isInstalled(),
            toolchainSizeBytes = directorySize(root)
        )
    }

    fun toolchainDirectory(): File =
        root

    fun installer(): ToolchainInstaller =
        installer

    fun isArm64Device(): Boolean =
        Build.SUPPORTED_64_BIT_ABIS
            .contains("arm64-v8a")

    fun localBuildAvailable(): Boolean =
        inspect().readyForLocalBuild

    fun missingTools(): List<ToolStatus> =
        inspect()
            .tools
            .filterNot { it.available }

    fun resetToolchain(): Boolean {
        return try {
            root.deleteRecursively()
            root.mkdirs()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun directorySize(file: File): Long {

        if (!file.exists()) {
            return 0L
        }

        if (file.isFile) {
            return file.length()
        }

        return file.listFiles()
            ?.sumOf { directorySize(it) }
            ?: 0L
    }
}
