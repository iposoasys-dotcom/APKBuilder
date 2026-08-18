package com.example.apkbuilder.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProjectExplorer(
    files: List<ProjectFile>,
    onFileClick: (ProjectFile) -> Unit
) {
    val expanded = remember {
        mutableStateMapOf<String, Boolean>()
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Project",
            modifier = Modifier.padding(12.dp)
        )

        files.forEach { item ->

            val path = item.relativePath
            val parts = path.split("/")

            var visible = true

            if (parts.size > 1) {
                for (i in 0 until parts.size - 1) {
                    val parent = parts
                        .take(i + 1)
                        .joinToString("/")

                    if (expanded[parent] != true) {
                        visible = false
                        break
                    }
                }
            }

            if (!visible) {
                return@forEach
            }

            val depth = parts.size - 1

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {

                        if (item.isDirectory) {
                            expanded[path] =
                                expanded[path] != true
                        } else {
                            onFileClick(item)
                        }
                    }
                    .padding(
                        start = (10 + depth * 20).dp,
                        top = 10.dp,
                        bottom = 10.dp,
                        end = 10.dp
                    )
            ) {

                val icon =
                    if (item.isDirectory) {
                        if (expanded[path] == true) {
                            "📂"
                        } else {
                            "📁"
                        }
                    } else {
                        "📄"
                    }

                Text(
                    text = "$icon ${item.file.name}"
                )
            }
        }
    }
}
