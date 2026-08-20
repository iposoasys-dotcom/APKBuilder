package com.example.apkbuilder

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.apkbuilder.build.BuildManager
import com.example.apkbuilder.build.BuildPreviewActivity
import com.example.apkbuilder.build.CompatibilityCheck
import com.example.apkbuilder.build.ToolStatus
import com.example.apkbuilder.build.ToolchainManager
import com.example.apkbuilder.build.ToolchainReport
import com.example.apkbuilder.editor.CodeEditor
import com.example.apkbuilder.editor.ProjectExplorer
import com.example.apkbuilder.editor.ProjectFile
import com.example.apkbuilder.editor.ProjectManager
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IDEActivity : ComponentActivity() {

    private val toolchainZipPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@registerForActivityResult

        lifecycleScope.launch {
            try {
                Toast.makeText(
                    this@IDEActivity,
                    "Installing toolchain...",
                    Toast.LENGTH_LONG
                ).show()

                val result = withContext(Dispatchers.IO) {
                    val zipFile = File(
                        cacheDir,
                        "selected-toolchain.zip"
                    )

                    if (zipFile.exists()) {
                        zipFile.delete()
                    }

                    contentResolver.openInputStream(uri).use { input ->
                        requireNotNull(input) {
                            "Unable to open selected ZIP file."
                        }

                        zipFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    ToolchainManager(this@IDEActivity)
                        .installer()
                        .installFromZip(zipFile)
                }

                  if (result.state == com.example.apkbuilder.build.InstallState.SUCCESS || result.state == com.example.apkbuilder.build.InstallState.ALREADY_INSTALLED) {
                      Toast.makeText(
                          this@IDEActivity,
                          "Toolchain installed. Rechecking compatibility...",
                          Toast.LENGTH_LONG
                      ).show()
                  }
                Toast.makeText(
                    this@IDEActivity,
                    result.message,
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    this@IDEActivity,
                    "Toolchain installation failed: ${e.message ?: "Unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val projectPath =
            intent.getStringExtra("project")

        if (projectPath.isNullOrBlank()) {
            Toast.makeText(
                this,
                "Project path missing",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        setContent {
            MaterialTheme {
                EditorScreen(
                    File(projectPath)
                )
            }
        }
    }

    @Composable
    private fun EditorScreen(
        project: File
    ) {

        val projectManager =
            remember {
                ProjectManager(this)
            }

        var files by remember(project.absolutePath) {
            mutableStateOf(
                projectManager.listFiles(project)
            )
        }

        var selectedFile by remember {
            mutableStateOf<ProjectFile?>(null)
        }

        var code by remember {
            mutableStateOf("")
        }

        var status by remember {
            mutableStateOf("Ready")
        }

        var showCompatibility by remember {
            mutableStateOf(false)
        }

        if (showCompatibility) {

            CompatibilityScreen(
                onBack = {
                    showCompatibility = false
                }
            )

            return
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text = project.name,
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge
                    )

                    Text(
                        text = status,
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {

                    OutlinedButton(
                        onClick = {
                            showCompatibility = true
                        }
                    ) {
                        Text("Compatibility")
                    }

                    Button(
                        onClick = {

                            try {

                                val intent =
                                    Intent(
                                        this@IDEActivity,
                                        BuildPreviewActivity::class.java
                                    ).apply {
                                        putExtra(
                                            "project",
                                            project.absolutePath
                                        )
                                    }

                                startActivity(intent)

                                status =
                                    "Opening build preview..."

                            } catch (e: Exception) {

                                status =
                                    e.message
                                        ?: "Unable to open build preview"
                }
                                Toast.makeText(
                                    this@IDEActivity,
                                    status,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    ) {
                        Text("Build")
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxSize()
            ) {

                Box(
                    modifier =
                        Modifier.width(240.dp)
                ) {

                    ProjectExplorer(
                        files = files,
                        onFileClick = { projectFile ->

                            try {

                                if (!projectFile.isDirectory) {

                                    code =
                                        projectFile.file.readText()

                                    selectedFile =
                                        projectFile

                                    status =
                                        "Opened ${projectFile.relativePath}"
                                }

                            } catch (e: Exception) {

                                status =
                                    "Open failed: ${
                                        e.message
                                            ?: "unknown error"
                                    }"
                            }
                        }
                    )
                }

                HorizontalDivider()

                Box(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    val file =
                        selectedFile

                    if (file != null) {

                        CodeEditor(
                            fileName =
                                file.file.name,
                            initialCode =
                                code,
                            onSave = { newCode ->

                                try {

                                    file.file.writeText(
                                        newCode
                                    )

                                    code =
                                        newCode

                                    status =
                                        "Saved ${file.relativePath}"

                                    files =
                                        projectManager
                                            .listFiles(
                                                project
                                            )

                                } catch (e: Exception) {

                                    status =
                                        "Save failed: ${
                                            e.message
                                                ?: "unknown error"
                                        }"
                                }
                            }
                        )

                    } else {

                        Column(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(20.dp)
                        ) {

                            Text(
                                text =
                                    "Select a file",
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium
                            )

                            Text(
                                text =
                                    "Architecture: ${
                                        BuildManager
                                            .architecture()
                                    }",
                                modifier =
                                    Modifier.padding(
                                        top = 12.dp
                                    )
                            )

                            Text(
                                text =
                                    "Build mode: ${
                                        BuildManager
                                            .preferredMode()
                                    }",
                                modifier =
                                    Modifier.padding(
                                        top = 4.dp
                                    )
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        16.dp
                                    )
                            )

                            Text(
                                text =
                                    "Use Compatibility to test whether this Android device can perform the local APK build."
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun CompatibilityScreen(
        onBack: () -> Unit
    ) {

        val manager =
            remember {
                ToolchainManager(this)
            }

        var report by remember {
            mutableStateOf<ToolchainReport?>(null)
        }

        var checking by remember {
            mutableStateOf(true)
        }

        suspend fun checkDevice() {

            checking = true

            val result =
                withContext(Dispatchers.IO) {
                    manager.runCompatibilityCheck()
                }

            report = result
            checking = false
        }

        LaunchedEffect(Unit) {
            checkDevice()
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(12.dp)
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            "Device Compatibility",
                        style =
                            MaterialTheme
                                .typography
                                .headlineSmall
                    )

                    Text(
                        text =
                            "Real local-build compatibility test",
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }

                OutlinedButton(
                    onClick = onBack
                ) {
                    Text("Back")
                }
            }

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            if (checking) {

                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier =
                            Modifier.padding(16.dp)
                    ) {

                        Text(
                            text =
                                "CHECKING DEVICE..."
                        )

                        Spacer(
                            modifier =
                                Modifier.height(6.dp)
                        )

                        Text(
                            text =
                                "Testing architecture, Android runtime, storage, RAM and installed toolchain."
                        )
                    }
                }

            } else {

                val current =
                    report

                if (current != null) {

                    DeviceSummary(current)

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )

                    FinalBuildResult(current)

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )

                    LazyColumn(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        item {

                            Text(
                                text =
                                    "Compatibility Tests",
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleLarge,
                                modifier =
                                    Modifier.padding(
                                        vertical = 6.dp
                                    )
                            )
                        }

                        items(
                            current.checks
                        ) { check ->

                            CompatibilityCheckCard(
                                check
                            )
                        }

                        item {

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        8.dp
                                    )
                            )

                            Text(
                                text =
                                    "Required Tools",
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleLarge,
                                modifier =
                                    Modifier.padding(
                                        vertical = 6.dp
                                    )
                            )
                        }

                        items(
                            current.tools
                        ) { tool ->

                            ToolStatusCard(tool)
                        }

                        item {

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        12.dp
                                    )
                            )

                            Text(
                                text =
                                    "Passed: ${current.passedChecks}    Failed: ${current.failedChecks}"
                            )

                            Text(
                                text =
                                    "Toolchain installed: ${
                                        if (current.installed)
                                            "YES"
                                        else
                                            "NO"
                                    }"
                            )

                            Text(
                                text =
                                    "Toolchain size: ${
                                        formatBytes(
                                            current.toolchainSizeBytes
                                        )
                                    }"
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        16.dp
                                    )
                            )
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )


                      Button(
                          onClick = {
                              toolchainZipPicker.launch(
                                  arrayOf("application/zip", "application/octet-stream")
                              )
                          },
                          modifier =
                              Modifier.fillMaxWidth()
                      ) {
                          Text("Install Toolchain from ZIP")
                      }

                      Spacer(
                          modifier =
                              Modifier.height(8.dp)
                      )

                    Button(
                        onClick = {
                            report = null

                            checking = true
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text("Recheck Device")
                    }

                    LaunchedEffect(checking) {

                        if (checking) {
                            checkDevice()
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun DeviceSummary(
        report: ToolchainReport
    ) {

        Card(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column(
                modifier =
                    Modifier.padding(14.dp)
            ) {

                Text(
                    text =
                        report.deviceModel,
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(
                    text =
                        "Android ${report.androidRelease} " +
                            "(API ${report.androidVersion})"
                )

                Text(
                    text =
                        "Architecture: ${report.architecture}"
                )

                Text(
                    text =
                        "Total RAM: ${
                            formatBytes(
                                report.totalRamBytes
                            )
                        }"
                )

                Text(
                    text =
                        "Available RAM: ${
                            formatBytes(
                                report.availableRamBytes
                            )
                        }"
                )

                Text(
                    text =
                        "Free storage: ${
                            formatBytes(
                                report.freeStorageBytes
                            )
                        }"
                )
            }
        }
    }

    @Composable
    private fun FinalBuildResult(
        report: ToolchainReport
    ) {

        val ready =
            report.readyForLocalBuild

        Card(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column(
                modifier =
                    Modifier.padding(16.dp)
            ) {

                Text(
                    text =
                        if (ready)
                            "✅ READY TO BUILD ON THIS DEVICE"
                        else
                            "❌ NOT READY TO BUILD ON THIS DEVICE",
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )

                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )

                Text(
                    text =
                        if (ready) {
                            "All required device and tool execution tests passed."
                        } else {
                            "One or more required compatibility or execution tests failed."
                        }
                )
            }
        }
    }

    @Composable
    private fun CompatibilityCheckCard(
        check: CompatibilityCheck
    ) {

        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 3.dp
                    )
        ) {

            Column(
                modifier =
                    Modifier.padding(12.dp)
            ) {

                Text(
                    text =
                        if (check.passed)
                            "✅ PASS — ${check.name}"
                        else
                            "❌ FAIL — ${check.name}",
                    style =
                        MaterialTheme
                            .typography
                            .titleSmall
                )

                Spacer(
                    modifier =
                        Modifier.height(3.dp)
                )

                Text(
                    text =
                        check.details,
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }
        }
    }

    @Composable
    private fun ToolStatusCard(
        tool: ToolStatus
    ) {

        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 3.dp
                    )
        ) {

            Column(
                modifier =
                    Modifier.padding(12.dp)
            ) {

                Text(
                    text =
                        if (tool.available)
                            "✅ PASS — ${tool.name}"
                        else
                            "❌ FAIL — ${tool.name}",
                    style =
                        MaterialTheme
                            .typography
                            .titleSmall
                )

                if (tool.path != null) {

                    Text(
                        text =
                            tool.path,
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }

                if (tool.details.isNotBlank()) {

                    Text(
                        text =
                            tool.details,
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }
            }
        }
    }

    private fun formatBytes(
        bytes: Long
    ): String {

        if (bytes <= 0L) {
            return "Unknown"
        }

        val mb =
            bytes / (1024L * 1024L)

        if (mb < 1024L) {
            return "$mb MB"
        }

        val gb =
            mb / 1024L

        return "$gb GB"
    }
}
