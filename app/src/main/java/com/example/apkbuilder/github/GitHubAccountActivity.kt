package com.example.apkbuilder.github

import android.os.Bundle
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class GitHubAccountActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                AccountScreen()
            }
        }
    }

    @Composable
    private fun AccountScreen() {

        val client =
            remember {
                GitHubClient(this@GitHubAccountActivity)
            }

        var signedIn by remember {
            mutableStateOf(
                client.isSignedIn()
            )
        }

        var username by remember {
            mutableStateOf(
                client.username()
            )
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(20.dp),
            verticalArrangement =
                Arrangement.Center
        ) {

            Text(
                text = "GitHub Account",
                style =
                    MaterialTheme
                        .typography
                        .headlineSmall
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            if (signedIn) {

                Text(
                    text =
                        "Signed in as ${username ?: "GitHub user"}"
                )

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                OutlinedButton(
                    onClick = {
                        client.signOut()
                        signedIn = false
                        username = null
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text("Sign out")
                }

            } else {

                Text(
                    text =
                        "Connect APKBuilder to your GitHub account to manage repositories and GitHub Actions builds."
                )

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                Button(
                    onClick = {
                        val request =
                            GitHubOAuth.createRequest()

                        getSharedPreferences(
                            "github_oauth",
                            MODE_PRIVATE
                        ).edit()
                            .putString(
                                "state",
                                request.state
                            )
                            .putString(
                                "code_verifier",
                                request.codeVerifier
                            )
                            .apply()

                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(
                                    request.authorizationUrl
                                )
                            )
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text("Sign in with GitHub")
                }
            }

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            OutlinedButton(
                onClick = {
                    finish()
                },
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }
        }
    }
}
