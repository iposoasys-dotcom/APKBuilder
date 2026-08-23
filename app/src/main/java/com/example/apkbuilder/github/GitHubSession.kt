package com.example.apkbuilder.github

import android.content.Context

data class GitHubSession(
    val accessToken: String,
    val username: String
)

class GitHubSessionStore(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            "github_session",
            Context.MODE_PRIVATE
        )

    fun save(
        accessToken: String,
        username: String
    ) {
        preferences.edit()
            .putString("access_token", accessToken)
            .putString("username", username)
            .apply()
    }

    fun session(): GitHubSession? {
        val token =
            preferences.getString(
                "access_token",
                null
            )

        val username =
            preferences.getString(
                "username",
                null
            )

        if (
            token.isNullOrBlank() ||
            username.isNullOrBlank()
        ) {
            return null
        }

        return GitHubSession(
            accessToken = token,
            username = username
        )
    }

    fun token(): String? =
        session()?.accessToken

    fun clear() {
        preferences.edit()
            .clear()
            .apply()
    }

    fun isSignedIn(): Boolean =
        session() != null
}
