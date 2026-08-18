package com.example.apkbuilder

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.apkbuilder.build.BuildManager
import com.example.apkbuilder.build.LocalBuildEngine
import com.example.apkbuilder.build.BuildPreviewActivity
import com.example.apkbuilder.editor.CodeEditor
import com.example.apkbuilder.editor.ProjectExplorer
import com.example.apkbuilder.editor.ProjectFile
import com.example.apkbuilder.editor.ProjectManager
import java.io.File

class IDEActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val projectPath = intent.getStringExtra("project")

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
                EditorScreen(File(projectPath))
            }
        }
    }

    @Composable
    private fun EditorScreen(project: File) {

        val projectManager = remember {
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

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = {

                        try {

                            startActivity(
                                android.content.Intent(
                                    this@IDEActivity,
                                    BuildPreviewActivity::class.java
                                ).apply {
                                    putExtra("project", project.absolutePath)
                                }
                            )
                            val engine =
                                this@IDEActivity,
                                result.message,
                                Toast.LENGTH_LONG
                            ).show()

                        } catch (e: Exception) {

                            status =
                                e.message
                                    ?: "Build failed"

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

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxSize()
            ) {

                Box(
                    modifier = Modifier.width(240.dp)
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
                                        e.message ?: "unknown error"
                                    }"
                            }
                        }
                    )
                }

                HorizontalDivider()

                Box(
                    modifier = Modifier.weight(1f)
                ) {

                    val file = selectedFile

                    if (file != null) {

                        CodeEditor(
                            fileName = file.file.name,
                            initialCode = code,
                            onSave = { newCode ->

                                try {

                                    file.file.writeText(
                                        newCode
                                    )

                                    code = newCode

                                    status =
                                        "Saved ${file.relativePath}"

                                    files =
                                        projectManager.listFiles(
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
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp)
                        ) {

                            Text(
                                text = "Select a file"
                            )

                            Text(
                                text =
                                    "Architecture: ${
                                        BuildManager.architecture()
                                    }",
                                modifier = Modifier.padding(
                                    top = 12.dp
                                )
                            )

                            Text(
                                text =
                                    "Build mode: ${
                                        BuildManager.preferredMode()
                                    }",
                                modifier = Modifier.padding(
                                    top = 4.dp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
