package com.example.apkbuilder.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProjectExplorer(
    files: List<ProjectFile>,
    onFileClick: (ProjectFile) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Text("Project", Modifier.padding(12.dp))
        files.forEach { item ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !item.isDirectory) {
                        onFileClick(item)
                    }
                    .padding(10.dp)
            ) {
                Text(
                    if (item.isDirectory)
                        "📁 ${item.relativePath}"
                    else
                        "📄 ${item.relativePath}"
                )
            }
        }
    }
}
