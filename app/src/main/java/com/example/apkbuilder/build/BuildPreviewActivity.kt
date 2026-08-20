package com.example.apkbuilder.build

import android.content.Intent
import android.os.Bundle
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

class BuildPreviewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val projectPath =
            intent.getStringExtra("project")

        if (projectPath.isNullOrBlank()) {
            finish()
            return
        }

        setContent {
            MaterialTheme {
                BuildPreviewScreen(
                    File(projectPath)
                )
            }
        }
    }

    @Composable
    private fun BuildPreviewScreen(
        project: File
    ) {

        val engine =
            remember {
                LocalBuildEngine(this@BuildPreviewActivity)
            }

        var validation by remember {
            mutableStateOf(
                engine.validate(project)
            )
        }

        val ready =
            validation.status ==
                LocalBuildStatus.READY

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Text(
                text = "Build Preview",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = project.name,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text =
                    if (ready)
                        "Everything is ready to build."
                    else
                        validation.message,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            HorizontalDivider()

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {

                items(validation.steps) { step ->

                    PreviewStepCard(step)
                }

                item {

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {

                            Text(
                                text = "Build target",
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
                                    "Debug APK"
                            )

                            Text(
                                text =
                                    "Architecture: ARM64"
                            )

                            Text(
                                text =
                                    "Output: app/build/outputs/apk/"
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                OutlinedButton(
                    onClick = {
                        finish()
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                OutlinedButton(
                    onClick = {
                        validation =
                            engine.validate(project)
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text("Recheck")
                }

                Button(
                    enabled = ready,
                    onClick = {

                        val intent =
                            Intent(
                                this@BuildPreviewActivity,
                                BuildActivity::class.java
                            ).apply {
                                putExtra(
                                    "project",
                                    project.absolutePath
                                )
                            }

                        startActivity(intent)
                        finish()
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text("BUILD APK")
                }
            }
        }
    }

    @Composable
    private fun PreviewStepCard(
        step: BuildStep
    ) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {

            Column(
                modifier = Modifier.padding(14.dp)
            ) {

                Text(
                    text =
                        if (step.success)
                            "✓ ${step.name}"
                        else
                            "✕ ${step.name}",
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
                    text = step.message,
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium
                )
            }
        }
    }
}
