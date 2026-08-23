package com.example.apkbuilder

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.apkbuilder.editor.ProjectManager
import com.example.apkbuilder.github.GitHubAccountActivity
import com.example.apkbuilder.github.GitHubRepositoryActivity
import java.io.File

class ProjectLibraryActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                LibraryScreen()
            }
        }
    }

    @Composable
    private fun LibraryScreen() {

        val manager =
            remember {
                ProjectManager(this@ProjectLibraryActivity)
            }

        var projects by remember {
            mutableStateOf(
                manager.projects()
            )
        }

        var renameProject by remember {
            mutableStateOf<File?>(null)
        }

        var renameText by remember {
            mutableStateOf("")
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
        ) {

            Text(
                text = "Project Library",
                style =
                    MaterialTheme
                        .typography
                        .headlineSmall
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                Button(
                    onClick = {
                        startActivity(
                            Intent(
                                this@ProjectLibraryActivity,
                                MainActivity::class.java
                            ).apply {
                                putExtra("create_project", true)
                            }
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("New Project")
                }

                OutlinedButton(
                    onClick = {
                        projects =
                            manager.projects()
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text("Refresh")
                }

                OutlinedButton(
                    onClick = {
                        startActivity(
                            Intent(
                                this@ProjectLibraryActivity,
                                GitHubRepositoryActivity::class.java
                            )
                        )
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text("GitHub")
                }
            }

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            if (projects.isEmpty()) {

                Text(
                    text =
                        "No projects yet."
                )

            } else {

                LazyColumn(
                    modifier =
                        Modifier.fillMaxSize()
                ) {

                    items(
                        projects,
                        key = {
                            it.absolutePath
                        }
                    ) { project ->

                        Card(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        vertical = 4.dp
                                    )
                        ) {

                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {

                                            startActivity(
                                                Intent(
                                                    this@ProjectLibraryActivity,
                                                    IDEActivity::class.java
                                                ).apply {
                                                    putExtra(
                                                        "project",
                                                        project.absolutePath
                                                    )
                                                }
                                            )
                                        }
                                        .padding(14.dp)
                            ) {

                                Text(
                                    text =
                                        project.name,
                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleMedium
                                )

                                Text(
                                    text =
                                        project.absolutePath,
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodySmall
                                )

                                Row(
                                    modifier =
                                        Modifier.fillMaxWidth(),
                                    horizontalArrangement =
                                        Arrangement.spacedBy(8.dp)
                                ) {

                                    TextButton(
                                        onClick = {
                                            renameProject =
                                                project
                                            renameText =
                                                project.name
                                        }
                                    ) {
                                        Text("Rename")
                                    }

                                    TextButton(
                                        onClick = {

                                            try {

                                                manager
                                                    .deleteProject(
                                                        project
                                                    )

                                                projects =
                                                    manager.projects()

                                            } catch (e: Exception) {

                                                Toast.makeText(
                                                    this@ProjectLibraryActivity,
                                                    e.message
                                                        ?: "Delete failed",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    ) {
                                        Text("Delete")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (renameProject != null) {

            AlertDialog(
                onDismissRequest = {
                    renameProject = null
                },
                title = {
                    Text("Rename project")
                },
                text = {

                    OutlinedTextField(
                        value = renameText,
                        onValueChange = {
                            renameText = it
                        },
                        singleLine = true,
                        label = {
                            Text("Project name")
                        }
                    )
                },
                confirmButton = {

                    TextButton(
                        onClick = {

                            try {

                                manager.renameProject(
                                    renameProject!!,
                                    renameText.trim()
                                )

                                projects =
                                    manager.projects()

                                renameProject = null

                            } catch (e: Exception) {

                                Toast.makeText(
                                    this@ProjectLibraryActivity,
                                    e.message
                                        ?: "Rename failed",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    ) {
                        Text("Rename")
                    }
                },
                dismissButton = {

                    TextButton(
                        onClick = {
                            renameProject = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
