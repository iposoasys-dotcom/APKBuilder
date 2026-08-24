package com.example.apkbuilder.github

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GitHubOAuthCallbackActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent?.data

        val code =
            uri?.getQueryParameter("code")

        val state =
            uri?.getQueryParameter("state")

        val error =
            uri?.getQueryParameter("error")

        if (!error.isNullOrBlank()) {
            finishWithMessage(
                "GitHub authorization failed: $error"
            )
            return
        }

        if (code.isNullOrBlank() || state.isNullOrBlank()) {
            finishWithMessage(
                "Invalid GitHub OAuth callback."
            )
            return
        }

        val preferences =
            getSharedPreferences(
                "github_oauth",
                MODE_PRIVATE
            )

        val savedState =
            preferences.getString(
                "state",
                null
            )

        val codeVerifier =
            preferences.getString(
                "code_verifier",
                null
            )

        if (
            savedState.isNullOrBlank() ||
            savedState != state
        ) {
            finishWithMessage(
                "GitHub OAuth state verification failed."
            )
            return
        }

        if (codeVerifier.isNullOrBlank()) {
            finishWithMessage(
                "GitHub OAuth verifier is missing."
            )
            return
        }

        preferences.edit()
            .clear()
            .apply()

        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {

            val exchange =
                GitHubOAuthExchange()

            val tokenResult =
                exchange.exchange(
                    code = code,
                    codeVerifier = codeVerifier
                )

            val token =
                tokenResult.getOrElse {
                    finishWithMessage(
                        it.message
                            ?: "Could not obtain GitHub access token."
                    )
                    return@launch
                }

            val usernameResult =
                exchange.username(
                    token.accessToken
                )

            val username =
                usernameResult.getOrElse {
                    finishWithMessage(
                        it.message
                            ?: "Could not read GitHub username."
                    )
                    return@launch
                }

            GitHubSessionStore(
                this@GitHubOAuthCallbackActivity
            ).save(
                accessToken = token.accessToken,
                username = username
            )

            Toast.makeText(
                this@GitHubOAuthCallbackActivity,
                "GitHub connected as $username",
                Toast.LENGTH_LONG
            ).show()

            finish()
        }
    }

    private fun finishWithMessage(
        message: String
    ) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()

        finish()
    }
}
