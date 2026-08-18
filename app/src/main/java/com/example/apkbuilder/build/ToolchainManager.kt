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
    val tools: List<ToolStatus>
) {
    val readyForLocalBuild: Boolean
        get() = supportedArchitecture && tools.all { it.available }
}

class ToolchainManager(private val context: Context) {

    private val root =
        File(context.filesDir, "toolchain").apply { mkdirs() }

    private fun tool(name: String): File =
        File(root, name)

    fun inspect(): ToolchainReport {

        val architecture =
            Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"

        val is64Bit =
            Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()

        val tools = listOf(
            ToolStatus(
                name = "Android SDK",
                available = File(root, "sdk").isDirectory,
                path = File(root, "sdk").absolutePath
            ),
            ToolStatus(
                name = "AAPT2",
                available = tool("aapt2").canExecute(),
                path = tool("aapt2").absolutePath
            ),
            ToolStatus(
                name = "D8",
                available = tool("d8").canExecute(),
                path = tool("d8").absolutePath
            ),
            ToolStatus(
                name = "Zipalign",
                available = tool("zipalign").canExecute(),
                path = tool("zipalign").absolutePath
            ),
            ToolStatus(
                name = "Apksigner",
                available = tool("apksigner").canExecute(),
                path = tool("apksigner").absolutePath
            ),
            ToolStatus(
                name = "JDK",
                available = File(root, "jdk").isDirectory,
                path = File(root, "jdk").absolutePath
            )
        )

        return ToolchainReport(
            supportedArchitecture = is64Bit,
            architecture = architecture,
            tools = tools
        )
    }

    fun toolchainDirectory(): File = root

    fun localBuildAvailable(): Boolean =
        inspect().readyForLocalBuild
}
