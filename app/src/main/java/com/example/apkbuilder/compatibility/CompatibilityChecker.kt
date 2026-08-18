package com.example.apkbuilder.compatibility

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs

data class CompatibilityReport(
    val deviceName: String,
    val androidVersion: String,
    val sdk: Int,
    val primaryAbi: String,
    val is64Bit: Boolean,
    val arm64: Boolean,
    val ramMb: Long,
    val freeStorageMb: Long,
    val storageOk: Boolean,
    val ramOk: Boolean,
    val architectureOk: Boolean,
    val localBuildPossible: Boolean
) {
    val compatible: Boolean
        get() = architectureOk && storageOk && ramOk
}

object CompatibilityChecker {

    fun check(context: Context): CompatibilityReport {

        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE)
                    as ActivityManager

        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val ramMb =
            memoryInfo.totalMem / (1024L * 1024L)

        val statFs =
            StatFs(Environment.getDataDirectory().path)

        val freeStorageMb =
            statFs.availableBytes / (1024L * 1024L)

        val abis =
            Build.SUPPORTED_ABIS.toList()

        val is64Bit =
            Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()

        val arm64 =
            abis.any {
                it.equals(
                    "arm64-v8a",
                    ignoreCase = true
                )
            }

        val architectureOk =
            is64Bit && arm64

        val ramOk =
            ramMb >= 3072

        val storageOk =
            freeStorageMb >= 1536

        return CompatibilityReport(
            deviceName =
                "${Build.MANUFACTURER} ${Build.MODEL}",

            androidVersion =
                Build.VERSION.RELEASE,

            sdk =
                Build.VERSION.SDK_INT,

            primaryAbi =
                abis.firstOrNull()
                    ?: "unknown",

            is64Bit =
                is64Bit,

            arm64 =
                arm64,

            ramMb =
                ramMb,

            freeStorageMb =
                freeStorageMb,

            storageOk =
                storageOk,

            ramOk =
                ramOk,

            architectureOk =
                architectureOk,

            localBuildPossible =
                false
        )
    }
}
