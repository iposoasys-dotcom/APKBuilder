package com.example.apkbuilder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.apkbuilder.editor.*
import java.io.File

class IDEActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = intent.getStringExtra("project")
        if (path == null) {
            finish()
            return
        }
        setContent { EditorScreen(File(path)) }
    }

    @Composable
    private fun EditorScreen(project: File) {
        val manager = ProjectManager(this)
        val files = remember(project) { manager.listFiles(project) }
        var selected by remember { mutableStateOf<ProjectFile?>(null) }
        var code by remember { mutableStateOf("") }
        var status by remember { mutableStateOf("Ready") }

        MaterialTheme {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(project.name, style = MaterialTheme.typography.titleLarge)
                    Text(status)
                }

                Row(Modifier.weight(1f)) {
                    Box(Modifier.width(240.dp)) {
                        ProjectExplorer(files) {
                            selected = it
                            code = it.file.readText()
                            status = "Opened ${it.relativePath}"
                        }
                    }

                    VerticalDivider()

                    Box(Modifier.weight(1f)) {
                        val file = selected
                        if (file != null) {
                            CodeEditor(file.file.name, code) {
                                file.file.writeText(it)
                                code = it
                                status = "Saved ${file.relativePath}"
                            }
                        } else {
                            Text("Select a file", Modifier.padding(20.dp))
                        }
                    }
                }
            }
        }
    }
}
