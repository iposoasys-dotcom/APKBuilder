package com.example.apkbuilder

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.apkbuilder.editor.ProjectManager

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                NewProjectScreen()
            }
        }
    }

    @Composable
    private fun NewProjectScreen() {
        var name by remember { mutableStateOf("My App") }
        var pkg by remember { mutableStateOf("com.example.myapp") }
        var language by remember { mutableStateOf("kotlin") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text(
                text = "APK Builder 2.0",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("App name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = pkg,
                onValueChange = { pkg = it },
                label = { Text("Package name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Language")

            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = language == "kotlin",
                    onClick = { language = "kotlin" }
                )

                Text("Kotlin")

                Spacer(modifier = Modifier.width(16.dp))

                RadioButton(
                    selected = language == "java",
                    onClick = { language = "java" }
                )

                Text("Java")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    try {
                        val project = ProjectManager(this@MainActivity)
                            .createProject(name, pkg, language)

                        startActivity(
                            Intent(
                                this@MainActivity,
                                IDEActivity::class.java
                            ).putExtra(
                                "project",
                                project.absolutePath
                            )
                        )
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@MainActivity,
                            e.message ?: "Error",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Project")
            }
        }
    }
}
