package com.example.apkbuilder.build

import android.os.Build
import java.io.File

enum class LocalBuildStatus {
    SUPPORTED,
    UNSUPPORTED_32_BIT,
    NOT_IMPLEMENTED
}

data class LocalBuildResult(
    val status: LocalBuildStatus,
    val message: String,
    val apk: File? = null
)

class LocalBuildEngine(
    private val filesDir: File
) {

    fun is64BitDevice(): Boolean =
        Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()

    fun status(): LocalBuildStatus =
        if (is64BitDevice()) {
            LocalBuildStatus.NOT_IMPLEMENTED
        } else {
            LocalBuildStatus.UNSUPPORTED_32_BIT
        }

    fun build(project: File): LocalBuildResult {

        if (!is64BitDevice()) {
            return LocalBuildResult(
                status = LocalBuildStatus.UNSUPPORTED_32_BIT,
                message = "32-bit device detected. Use GitHub Actions."
            )
        }

        return LocalBuildResult(
            status = LocalBuildStatus.NOT_IMPLEMENTED,
            message = "64-bit device detected. Embedded on-device build engine is not implemented yet."
        )
    }
}
