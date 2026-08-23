package com.example.apkbuilder.build

import android.os.Build

enum class BuildMode {
    ON_DEVICE,
    GITHUB_ACTIONS
}

data class BuildCapability(
    val mode: BuildMode,
    val available: Boolean,
    val architecture: String,
    val message: String
)

object BuildManager {

    fun architecture(): String =
        Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"

    fun is64BitDevice(): Boolean =
        Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()

    /**
     * GitHub Actions is the default builder for all Android devices.
     *
     * This allows both 32-bit and 64-bit phones to build APKs without
     * depending on the native Android CPU architecture.
     */
    fun preferredMode(): BuildMode =
        BuildMode.GITHUB_ACTIONS

    fun capability(): BuildCapability =
        BuildCapability(
            mode = BuildMode.GITHUB_ACTIONS,
            available = true,
            architecture = architecture(),
            message = if (is64BitDevice()) {
                "64-bit Android detected. GitHub Actions build mode selected."
            } else {
                "32-bit Android detected. GitHub Actions build mode selected."
            }
        )
}
