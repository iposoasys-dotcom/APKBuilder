package com.example.apkbuilder.github

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.File

class GitHubApi(
    private val tokenProvider: () -> String?
) {

    private val client = OkHttpClient()

    private fun request(
        url: String
    ): Request {
        val builder =
            Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")

        tokenProvider()
            ?.takeIf { it.isNotBlank() }
            ?.let {
                builder.header(
                    "Authorization",
                    "Bearer $it"
                )
            }

        return builder.build()
    }

    fun post(
        url: String,
        body: RequestBody? = null
    ): Result<String> =
        runCatching {
            val request =
                request(url)
                    .newBuilder()
                    .post(body ?: RequestBody.create(null, ByteArray(0)))
                    .build()

            client
                .newCall(request)
                .execute()
                .use { response ->
                    val responseBody =
                        response.body?.string()
                            ?: ""

                    if (!response.isSuccessful) {
                        error(
                            "GitHub API ${response.code}: $responseBody"
                        )
                    }

                    responseBody
                }
        }

    fun download(
        url: String,
        destination: File
    ): Result<File> =
        runCatching {
            destination.parentFile?.mkdirs()

            val request =
                request(url)

            client
                .newCall(request)
                .execute()
                .use { response ->

                    if (!response.isSuccessful) {
                        val body =
                            response.body?.string() ?: ""

                        error(
                            "GitHub download ${response.code}: $body"
                        )
                    }

                    val body =
                        response.body
                            ?: error("GitHub returned an empty download.")

                    body.byteStream().use { input ->
                        destination.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    destination
                }
        }

    fun get(
        url: String
    ): Result<String> =
        runCatching {
            client
                .newCall(request(url))
                .execute()
                .use { response ->
                    val body =
                        response.body?.string()
                            ?: ""

                    if (!response.isSuccessful) {
                        error(
                            "GitHub API ${response.code}: $body"
                        )
                    }

                    body
                }
        }
}
