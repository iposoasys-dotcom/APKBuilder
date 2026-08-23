package com.example.apkbuilder.github

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class GitHubOAuthToken(
    val accessToken: String
)

class GitHubOAuthExchange {

    private val client =
        OkHttpClient()

    suspend fun exchange(
        code: String,
        codeVerifier: String
    ): Result<GitHubOAuthToken> =
        withContext(Dispatchers.IO) {

            runCatching {

                val body =
                    FormBody.Builder()
                        .add(
                            "client_id",
                            GitHubOAuth.CLIENT_ID
                        )
                        .add(
                            "code",
                            code
                        )
                        .add(
                            "redirect_uri",
                            GitHubOAuth.REDIRECT_URI
                        )
                        .add(
                            "code_verifier",
                            codeVerifier
                        )
                        .build()

                val request =
                    Request.Builder()
                        .url(
                            "https://github.com/login/oauth/access_token"
                        )
                        .post(body)
                        .header(
                            "Accept",
                            "application/json"
                        )
                        .build()

                client
                    .newCall(request)
                    .execute()
                    .use { response ->

                        val responseBody =
                            response.body
                                ?.string()
                                ?: ""

                        if (!response.isSuccessful) {
                            error(
                                "GitHub OAuth ${response.code}: $responseBody"
                            )
                        }

                        val json =
                            JSONObject(responseBody)

                        val token =
                            json.optString(
                                "access_token"
                            )

                        if (token.isBlank()) {
                            error(
                                json.optString(
                                    "error_description",
                                    "GitHub did not return an access token."
                                )
                            )
                        }

                        GitHubOAuthToken(
                            accessToken = token
                        )
                    }
            }
        }

    suspend fun username(
        accessToken: String
    ): Result<String> =
        withContext(Dispatchers.IO) {

            runCatching {

                val request =
                    Request.Builder()
                        .url(
                            "https://api.github.com/user"
                        )
                        .header(
                            "Accept",
                            "application/vnd.github+json"
                        )
                        .header(
                            "Authorization",
                            "Bearer $accessToken"
                        )
                        .header(
                            "X-GitHub-Api-Version",
                            "2022-11-28"
                        )
                        .build()

                client
                    .newCall(request)
                    .execute()
                    .use { response ->

                        val responseBody =
                            response.body
                                ?.string()
                                ?: ""

                        if (!response.isSuccessful) {
                            error(
                                "GitHub user API ${response.code}: $responseBody"
                            )
                        }

                        val username =
                            JSONObject(responseBody)
                                .optString("login")

                        if (username.isBlank()) {
                            error(
                                "GitHub username was not returned."
                            )
                        }

                        username
                    }
            }
        }
}
