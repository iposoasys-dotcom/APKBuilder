package com.example.apkbuilder.github

import android.content.Context

class GitHubClient(
    context: Context
) {

    private val sessionStore =
        GitHubSessionStore(context)

    val api =
        GitHubApi {
            sessionStore.token()
        }

    val repositories =
        GitHubRepositoryApi(api)

    fun isSignedIn(): Boolean =
        sessionStore.isSignedIn()

    fun username(): String? =
        sessionStore.session()?.username

    fun signOut() {
        sessionStore.clear()
    }
}
