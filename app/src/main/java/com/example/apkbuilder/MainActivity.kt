package com.example.apkbuilder

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.apkbuilder.compatibility.CompatibilityActivity
import com.example.apkbuilder.ProjectLibraryActivity
import com.example.apkbuilder.editor.ProjectManager

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.getBooleanExtra("create_project", false)) {
            setContent {
                MaterialTheme {
                    NewProjectScreen()
                }
            }
        } else {
            startActivity(
                Intent(this, ProjectLibraryActivity::class.java)
            )
            finish()
        }
    }

    @Composable
    private fun NewProjectScreen() {

        var name by remember {
            mutableStateOf("My App")
        }

        var packageName by remember {
            mutableStateOf("com.example.myapp")
        }

        var language by remember {
            mutableStateOf("kotlin")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Top
        ) {

            Text(
                text = "APK Builder 2.0",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Create Android apps directly on your device",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                },
                label = {
                    Text("App name")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            OutlinedTextField(
                value = packageName,
                onValueChange = {
                    packageName = it
                },
                label = {
                    Text("Package name")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Text(
                text = "Programming language",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                RadioButton(
                    selected = language == "kotlin",
                    onClick = {
                        language = "kotlin"
                    }
                )

                Text("Kotlin")

                Spacer(
                    modifier = Modifier.width(20.dp)
                )

                RadioButton(
                    selected = language == "java",
                    onClick = {
                        language = "java"
                    }
                )

                Text("Java")
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Button(
                onClick = {

                    try {

                        val project =
                            ProjectManager(this@MainActivity)
                                .createProject(
                                    name = name.trim(),
                                    packageName = packageName.trim(),
                                    language = language
                                )

                        startActivity(
                            Intent(
                                this@MainActivity,
                                IDEActivity::class.java
                            ).apply {
                                putExtra(
                                    "project",
                                    project.absolutePath
                                )
                            }
                        )

                    } catch (e: Exception) {

                        Toast.makeText(
                            this@MainActivity,
                            e.message ?: "Could not create project",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Project")
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            OutlinedButton(
                onClick = {

                    startActivity(
                        Intent(
                            this@MainActivity,
                            CompatibilityActivity::class.java
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = {
                        startActivity(
                            Intent(
                                this@MainActivity,
                                ProjectLibraryActivity::class.java
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Project Library")
                }

                Spacer(
                    modifier = Modifier.height(10.dp)
                )
                Text("Device Compatibility Test")
            }
        }
    }
}
