package com.example.apkbuilder.github

import android.os.Bundle
import java.io.File
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GitHubRepositoryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                RepositoryScreen()
            }
        }
    }

    @Composable
    private fun RepositoryScreen() {

        val client =
            remember {
                GitHubClient(this@GitHubRepositoryActivity)
            }

        val scope = rememberCoroutineScope()

        var repositories by remember {
            mutableStateOf<List<GitHubRepository>>(emptyList())
        }

        var selected by remember {
            mutableStateOf<GitHubRepository?>(null)
        }

        var message by remember {
            mutableStateOf("Loading repositories...")
        }

        var loading by remember {
            mutableStateOf(false)
        }

        fun loadRepositories() {
            loading = true
            message = "Loading repositories..."

            scope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        client.repositories.repositories()
                    }

                result
                    .onSuccess {
                        repositories = it
                        message =
                            if (it.isEmpty())
                                "No repositories found."
                            else
                                "${it.size} repositories found."
                    }
                    .onFailure {
                        message =
                            it.message
                                ?: "Could not load repositories."
                    }

                loading = false
            }
        }

        LaunchedEffect(Unit) {
            loadRepositories()
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
        ) {

            Text(
                text = "GitHub Build",
                style =
                    MaterialTheme
                        .typography
                        .headlineSmall
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(message)

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                OutlinedButton(
                    enabled = !loading,
                    onClick = {
                        loadRepositories()
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text("Refresh")
                }

                OutlinedButton(
                    onClick = {
                        finish()
                    },
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text("Back")
                }
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {

                items(repositories) { repository ->

                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(14.dp)
                        ) {

                            Text(
                                text = repository.fullName,
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
                                text =
                                    "Branch: ${repository.defaultBranch}"
                            )

                            Text(
                                text =
                                    if (repository.private)
                                        "Private repository"
                                    else
                                        "Public repository"
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )

                            Button(
                                onClick = {
                                    selected = repository
                                },
                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (selected == repository)
                                        "Selected"
                                    else
                                        "Select"
                                )
                            }
                        }
                    }
                }
            }

            selected?.let { repository ->

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Button(
                    enabled = !loading,
                    onClick = {

                        loading = true
                        message =
                            "Starting GitHub Actions build..."

                        scope.launch {

                            val result =
                                withContext(Dispatchers.IO) {

                                    val beforeRuns =
                                        client.repositories
                                            .workflowRuns(
                                                owner = repository.owner,
                                                repository = repository.name
                                            )
                                            .getOrThrow()

                                    val previousIds =
                                        beforeRuns
                                            .map { it.id }
                                            .toSet()

                                    client.repositories
                                        .triggerWorkflow(
                                            owner = repository.owner,
                                            repository = repository.name,
                                            workflowFile = "build.yml",
                                            branch = repository.defaultBranch
                                        )
                                        .getOrThrow()

                                    var newRun: GitHubWorkflowRun? = null

                                    repeat(30) {

                                        Thread.sleep(5000)

                                        val runs =
                                            client.repositories
                                                .workflowRuns(
                                                    owner = repository.owner,
                                                    repository = repository.name
                                                )
                                                .getOrThrow()

                                        newRun =
                                            runs.firstOrNull {
                                                it.id !in previousIds &&
                                                    it.branch ==
                                                    repository.defaultBranch
                                            }

                                        if (newRun != null) {
                                            return@repeat
                                        }
                                    }

                                    val run =
                                        newRun
                                            ?: error(
                                                "GitHub Actions run was not detected."
                                            )

                                    withContext(Dispatchers.Main) {
                                        message =
                                            "Build detected. Waiting for completion..."
                                    }

                                    var completedRun = run

                                    repeat(60) {

                                        Thread.sleep(5000)

                                        val runs =
                                            client.repositories
                                                .workflowRuns(
                                                    owner = repository.owner,
                                                    repository = repository.name
                                                )
                                                .getOrThrow()

                                        completedRun =
                                            runs.firstOrNull {
                                                it.id == run.id
                                            } ?: completedRun

                                        if (
                                            completedRun.status ==
                                                "completed"
                                        ) {
                                            return@repeat
                                        }

                                        withContext(Dispatchers.Main) {
                                            message =
                                                "GitHub build running..."
                                        }
                                    }

                                    if (
                                        completedRun.status !=
                                            "completed"
                                    ) {
                                        error(
                                            "GitHub Actions build timed out."
                                        )
                                    }

                                    if (
                                        completedRun.conclusion !=
                                            "success"
                                    ) {
                                        error(
                                            "GitHub Actions build failed: " +
                                                "${completedRun.conclusion ?: "unknown"}"
                                        )
                                    }

                                    withContext(Dispatchers.Main) {
                                        message =
                                            "Build successful. Finding APK artifact..."
                                    }

                                    val artifacts =
                                        client.repositories
                                            .artifacts(
                                                owner = repository.owner,
                                                repository = repository.name,
                                                runId = completedRun.id
                                            )
                                            .getOrThrow()

                                    val artifact =
                                        artifacts.firstOrNull {
                                            !it.expired &&
                                                it.name ==
                                                "APKBuilder-v2-debug"
                                        }
                                        ?: error(
                                            "APK artifact was not found."
                                        )

                                    val downloader =
                                        GitHubArtifactDownloader(
                                            context =
                                                this@GitHubRepositoryActivity,
                                            api =
                                                client.api
                                        )

                                    val downloaded =
                                        downloader
                                            .downloadApk(
                                                owner =
                                                    repository.owner,
                                                repository =
                                                    repository.name,
                                                artifactId =
                                                    artifact.id
                                            )
                                            .getOrThrow()

                                    val downloadsDirectory =
                                        File(
                                            filesDir,
                                            "downloads"
                                        )

                                    downloadsDirectory.mkdirs()

                                    val destination =
                                        File(
                                            downloadsDirectory,
                                            downloaded.apk.name
                                        )

                                    downloaded.apk.copyTo(
                                        destination,
                                        overwrite = true
                                    )

                                    destination
                                }

                            result
                                .onSuccess { apk ->

                                    message =
                                        "Build completed and APK downloaded."

                                    Toast.makeText(
                                        this@GitHubRepositoryActivity,
                                        "APK saved: ${apk.absolutePath}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                .onFailure { error ->

                                    message =
                                        error.message
                                            ?: "GitHub build failed."
                                }

                            loading = false
                        }
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (loading)
                            "BUILDING..."
                        else
                            "BUILD & DOWNLOAD APK"
                    )
                }
            }

        }
    }
}
