package com.example.apkbuilder.build

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

class BuildActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val projectPath = intent.getStringExtra("project")

        if (projectPath.isNullOrBlank()) {
            Toast.makeText(
                this,
                "Project path missing.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        setContent {
            MaterialTheme {
                BuildScreen(File(projectPath))
            }
        }
    }

    @Composable
    private fun BuildScreen(project: File) {

        var building by remember { mutableStateOf(false) }
        var message by remember { mutableStateOf("Ready to build.") }
        var success by remember { mutableStateOf(false) }

        val engine = remember {
            LocalBuildEngine(this@BuildActivity)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Top
        ) {

            Text(
                text = "Build APK",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = project.name,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Button(
                enabled = !building,
                modifier = Modifier.fillMaxWidth(),
                onClick = {

                    building = true
                    success = false
                    message = "Starting local build..."

                    try {

                        val validation =
                            engine.validate(project)

                        if (validation.status != LocalBuildStatus.READY) {
                            message = validation.message
                            building = false
                            return@Button
                        }

                        val result =
                            engine.build(project)

                        message = result.message
                        success =
                            result.status ==
                                LocalBuildStatus.SUCCESS

                    } catch (e: Exception) {

                        message =
                            e.message
                                ?: "Build failed."

                    } finally {

                        building = false
                    }
                }
            ) {
                Text(
                    if (building)
                        "BUILDING..."
                    else
                        "BUILD APK"
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            if (success) {

                Text(
                    text = "✓ APK build completed successfully.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    finish()
                }
            ) {
                Text("Close")
            }
        }
    }
}
