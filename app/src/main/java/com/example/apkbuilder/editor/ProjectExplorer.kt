package com.example.apkbuilder.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProjectExplorer(
    files: List<ProjectFile>,
    onFileClick: (ProjectFile) -> Unit
) {
    val expandedFolders = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Project",
            modifier = Modifier.padding(12.dp)
        )

        files.forEach { item ->

            val path = item.relativePath
            val parentPaths = path.split("/").dropLast(1)

            val visible = parentPaths.all {
                expandedFolders.contains(it)
            }

            if (!visible) return@forEach

            val depth = path.count { it == '/' }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (item.isDirectory) {
                            if (expandedFolders.contains(path)) {
                                expandedFolders.remove(path)
                            } else {
                                expandedFolders.add(path)
                            }
                        } else {
                            onFileClick(item)
                        }
                    }
                    .padding(
                        start = (10 + depth * 18).dp,
                        top = 10.dp,
                        bottom = 10.dp,
                        end = 10.dp
                    )
            ) {
                val icon = if (item.isDirectory) {
                    if (expandedFolders.contains(path)) "📂" else "📁"
                } else {
                    "📄"
                }

                Text("$icon ${item.file.name}")
            }
        }
    }
}
