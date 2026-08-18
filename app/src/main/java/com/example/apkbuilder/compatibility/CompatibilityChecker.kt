package com.example.apkbuilder.compatibility

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import java.io.File

enum class CheckState {
    PASS,
    WARNING,
    FAIL
}

data class CompatibilityItem(
    val name: String,
    val value: String,
    val state: CheckState,
    val details: String = ""
)

data class CompatibilityReport(
    val items: List<CompatibilityItem>,
    val localBuildReady: Boolean,
    val toolchainSizeBytes: Long
)

object CompatibilityChecker {

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"

        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)

        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)

        val gb = mb / 1024.0
        return "%.2f GB".format(gb)
    }

    private fun directorySize(file: File): Long {
        if (!file.exists()) return 0L

        if (file.isFile) {
            return file.length()
        }

        return file.listFiles()
            ?.sumOf { directorySize(it) }
            ?: 0L
    }

    fun check(context: Context): CompatibilityReport {

        val items = mutableListOf<CompatibilityItem>()

        // Android version
        val sdk = Build.VERSION.SDK_INT
        val androidVersion = Build.VERSION.RELEASE ?: "Unknown"

        items += CompatibilityItem(
            name = "Android",
            value = "Android $androidVersion (API $sdk)",
            state = if (sdk >= 23) CheckState.PASS else CheckState.FAIL,
            details = if (sdk >= 23)
                "Android version meets APKBuilder minimum requirement."
            else
                "Android 6.0/API 23 or newer is required."
        )

        // CPU ABI
        val supportedAbis = Build.SUPPORTED_ABIS.toList()
        val supported64 = Build.SUPPORTED_64_BIT_ABIS.toList()

        val primaryAbi =
            supportedAbis.firstOrNull() ?: "unknown"

        val is64Bit =
            supported64.isNotEmpty()

        val hasArm64 =
            supported64.contains("arm64-v8a")

        items += CompatibilityItem(
            name = "CPU architecture",
            value = primaryAbi,
            state = if (is64Bit) CheckState.PASS else CheckState.FAIL,
            details = if (is64Bit)
                "64-bit CPU detected."
            else
                "A 64-bit CPU is required for local compilation."
        )

        items += CompatibilityItem(
            name = "64-bit ABI",
            value = if (supported64.isEmpty())
                "None"
            else
                supported64.joinToString(", "),
            state = if (is64Bit) CheckState.PASS else CheckState.FAIL,
            details = if (hasArm64)
                "ARM64 (arm64-v8a) is supported."
            else if (is64Bit)
                "A 64-bit ABI is available, but ARM64 was not detected."
            else
                "No 64-bit ABI detected."
        )

        // RAM
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        val memoryInfo =
            ActivityManager.MemoryInfo()

        activityManager.getMemoryInfo(memoryInfo)

        val totalRam =
            memoryInfo.totalMem

        val totalRamText =
            formatBytes(totalRam)

        val ramState =
            when {
                totalRam >= 3L * 1024 * 1024 * 1024 -> CheckState.PASS
                totalRam >= 2L * 1024 * 1024 * 1024 -> CheckState.WARNING
                else -> CheckState.FAIL
            }

        items += CompatibilityItem(
            name = "RAM",
            value = totalRamText,
            state = ramState,
            details = when (ramState) {
                CheckState.PASS ->
                    "Good for APKBuilder. Local builds may still be memory intensive."
                CheckState.WARNING ->
                    "Builds may work but memory pressure is possible."
                CheckState.FAIL ->
                    "RAM is low for local Android compilation."
            }
        )

        // Internal storage
        val dataDir =
            context.filesDir

        val stat =
            StatFs(dataDir.absolutePath)

        val freeStorage =
            stat.availableBytes

        val freeStorageText =
            formatBytes(freeStorage)

        val storageState =
            when {
                freeStorage >= 5L * 1024 * 1024 * 1024 ->
                    CheckState.PASS
                freeStorage >= 2L * 1024 * 1024 * 1024 ->
                    CheckState.WARNING
                else ->
                    CheckState.FAIL
            }

        items += CompatibilityItem(
            name = "Free internal storage",
            value = freeStorageText,
            state = storageState,
            details = when (storageState) {
                CheckState.PASS ->
                    "Enough free storage for a toolchain installation."
                CheckState.WARNING ->
                    "Toolchain installation may require freeing storage."
                CheckState.FAIL ->
                    "Not enough free storage for a complete local toolchain."
            }
        )

        // Toolchain
        val toolchainRoot =
            File(context.filesDir, "toolchain")

        val sdkDir =
            File(toolchainRoot, "sdk")

        val jdkDir =
            File(toolchainRoot, "jdk")

        val aapt2 =
            File(toolchainRoot, "aapt2")

        val d8 =
            File(toolchainRoot, "d8")

        val zipalign =
            File(toolchainRoot, "zipalign")

        val apksigner =
            File(toolchainRoot, "apksigner")

        val sdkInstalled =
            sdkDir.isDirectory

        val jdkInstalled =
            jdkDir.isDirectory

        val aapt2Ready =
            aapt2.isFile && aapt2.canExecute()

        val d8Ready =
            d8.isFile && d8.canExecute()

        val zipalignReady =
            zipalign.isFile && zipalign.canExecute()

        val apksignerReady =
            apksigner.isFile && apksigner.canExecute()

        items += CompatibilityItem(
            name = "Android SDK",
            value = if (sdkInstalled) "Installed" else "Not installed",
            state = if (sdkInstalled) CheckState.PASS else CheckState.WARNING,
            details = sdkDir.absolutePath
        )

        items += CompatibilityItem(
            name = "JDK",
            value = if (jdkInstalled) "Installed" else "Not installed",
            state = if (jdkInstalled) CheckState.PASS else CheckState.WARNING,
            details = jdkDir.absolutePath
        )

        items += CompatibilityItem(
            name = "AAPT2",
            value = if (aapt2Ready) "Ready" else "Not installed",
            state = if (aapt2Ready) CheckState.PASS else CheckState.WARNING,
            details = aapt2.absolutePath
        )

        items += CompatibilityItem(
            name = "D8",
            value = if (d8Ready) "Ready" else "Not installed",
            state = if (d8Ready) CheckState.PASS else CheckState.WARNING,
            details = d8.absolutePath
        )

        items += CompatibilityItem(
            name = "Zipalign",
            value = if (zipalignReady) "Ready" else "Not installed",
            state = if (zipalignReady) CheckState.PASS else CheckState.WARNING,
            details = zipalign.absolutePath
        )

        items += CompatibilityItem(
            name = "Apksigner",
            value = if (apksignerReady) "Ready" else "Not installed",
            state = if (apksignerReady) CheckState.PASS else CheckState.WARNING,
            details = apksigner.absolutePath
        )

        val toolchainSize =
            directorySize(toolchainRoot)

        items += CompatibilityItem(
            name = "Installed toolchain size",
            value = formatBytes(toolchainSize),
            state = CheckState.PASS,
            details = if (toolchainSize == 0L)
                "No local compiler toolchain is installed."
            else
                "Size of APKBuilder local build toolchain."
        )

        val localBuildReady =
            is64Bit &&
            hasArm64 &&
            sdkInstalled &&
            jdkInstalled &&
            aapt2Ready &&
            d8Ready &&
            zipalignReady &&
            apksignerReady

        items += CompatibilityItem(
            name = "Local APK build",
            value = if (localBuildReady)
                "READY"
            else
                "TOOLCHAIN REQUIRED",
            state = if (localBuildReady)
                CheckState.PASS
            else if (is64Bit && hasArm64)
                CheckState.WARNING
            else
                CheckState.FAIL,
            details = if (localBuildReady)
                "Device and local build toolchain are ready."
            else if (is64Bit && hasArm64)
                "Device is compatible. Install the ARM64-compatible build toolchain."
            else
                "Device does not meet the architecture requirement."
        )

        return CompatibilityReport(
            items = items,
            localBuildReady = localBuildReady,
            toolchainSizeBytes = toolchainSize
        )
    }
}
