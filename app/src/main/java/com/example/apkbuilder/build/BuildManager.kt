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

    fun preferredMode(): BuildMode =
        if (is64BitDevice()) BuildMode.ON_DEVICE
        else BuildMode.GITHUB_ACTIONS

    fun capability(): BuildCapability =
        if (is64BitDevice()) {
            BuildCapability(
                BuildMode.ON_DEVICE,
                true,
                architecture(),
                "64-bit Android detected. On-device build mode selected."
            )
        } else {
            BuildCapability(
                BuildMode.GITHUB_ACTIONS,
                true,
                architecture(),
                "32-bit Android detected. GitHub Actions build mode selected."
            )
        }
}
