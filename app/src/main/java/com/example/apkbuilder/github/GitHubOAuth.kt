package com.example.apkbuilder.github

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

data class GitHubOAuthRequest(
    val state: String,
    val codeVerifier: String,
    val authorizationUrl: String
)

object GitHubOAuth {

    /*
     * Replace this with the Client ID from your GitHub OAuth App.
     *
     * Never put a GitHub client secret in the Android application.
     */
    const val CLIENT_ID = "Ov23liNrFIanHNFkQfuN"

    const val REDIRECT_URI =
        "apkbuilder://github/oauth"

    fun createRequest(): GitHubOAuthRequest {

        val verifier =
            randomUrlSafe(64)

        val state =
            randomUrlSafe(32)

        val challenge =
            Base64.encodeToString(
                MessageDigest
                    .getInstance("SHA-256")
                    .digest(verifier.toByteArray()),
                Base64.URL_SAFE or
                    Base64.NO_PADDING or
                    Base64.NO_WRAP
            )

        val url =
            "https://github.com/login/oauth/authorize" +
                "?client_id=$CLIENT_ID" +
                "&redirect_uri=$REDIRECT_URI" +
                "&state=$state" +
                "&code_challenge=$challenge" +
                "&code_challenge_method=S256" +
                "&allow_signup=true"

        return GitHubOAuthRequest(
            state = state,
            codeVerifier = verifier,
            authorizationUrl = url
        )
    }

    private fun randomUrlSafe(
        bytes: Int
    ): String {

        val data =
            ByteArray(bytes)

        SecureRandom().nextBytes(data)

        return Base64.encodeToString(
            data,
            Base64.URL_SAFE or
                Base64.NO_PADDING or
                Base64.NO_WRAP
        )
    }
}
