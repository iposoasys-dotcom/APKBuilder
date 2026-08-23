package com.example.apkbuilder.github

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class GitHubRepository(
    val fullName: String,
    val name: String,
    val owner: String,
    val defaultBranch: String,
    val private: Boolean
)

data class GitHubFile(
    val name: String,
    val path: String,
    val type: String,
    val sha: String?
)

data class GitHubWorkflowRun(
    val id: Long,
    val status: String?,
    val conclusion: String?,
    val branch: String?,
    val workflowName: String?
)

data class GitHubArtifact(
    val id: Long,
    val name: String,
    val size: Long,
    val expired: Boolean
)

class GitHubRepositoryApi(
    private val api: GitHubApi
) {

    companion object {
        private const val BASE =
            "https://api.github.com"
    }

    fun repositories(): Result<List<GitHubRepository>> =
        api.get("$BASE/user/repos?per_page=100")
            .map { body ->
                val array = JSONArray(body)

                buildList {
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        val owner =
                            item
                                .getJSONObject("owner")
                                .getString("login")

                        add(
                            GitHubRepository(
                                fullName =
                                    item.getString("full_name"),
                                name =
                                    item.getString("name"),
                                owner =
                                    owner,
                                defaultBranch =
                                    item.optString(
                                        "default_branch",
                                        "main"
                                    ),
                                private =
                                    item.optBoolean(
                                        "private",
                                        false
                                    )
                            )
                        )
                    }
                }
            }

    fun files(
        owner: String,
        repository: String,
        path: String = ""
    ): Result<List<GitHubFile>> =
        api.get(
            "$BASE/repos/$owner/$repository/contents/$path"
        ).map { body ->
            val array = JSONArray(body)

            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)

                    add(
                        GitHubFile(
                            name =
                                item.getString("name"),
                            path =
                                item.getString("path"),
                            type =
                                item.getString("type"),
                            sha =
                                item.optString(
                                    "sha",
                                    null
                                )
                        )
                    )
                }
            }
        }

    fun readFile(
        owner: String,
        repository: String,
        path: String
    ): Result<String> =
        api.get(
            "$BASE/repos/$owner/$repository/contents/$path"
        ).map { body ->
            JSONObject(body).getString("content")
        }

    fun triggerWorkflow(
        owner: String,
        repository: String,
        workflowFile: String = "build.yml",
        branch: String = "main"
    ): Result<Unit> =
        api.post(
            "$BASE/repos/$owner/$repository/actions/workflows/$workflowFile/dispatches",
            org.json.JSONObject()
                .put("ref", branch)
                .toString()
                .toRequestBody(
                    "application/json; charset=utf-8".toMediaType()
                )
                .also { }
        ).map { }

    fun artifacts(
        owner: String,
        repository: String,
        runId: Long
    ): Result<List<GitHubArtifact>> =
        api.get(
            "$BASE/repos/$owner/$repository/actions/runs/$runId/artifacts"
        ).map { body ->
            val artifacts =
                JSONObject(body)
                    .getJSONArray("artifacts")

            buildList {
                for (i in 0 until artifacts.length()) {
                    val item =
                        artifacts.getJSONObject(i)

                    add(
                        GitHubArtifact(
                            id =
                                item.getLong("id"),
                            name =
                                item.getString("name"),
                            size =
                                item.optLong(
                                    "size_in_bytes",
                                    0L
                                ),
                            expired =
                                item.optBoolean(
                                    "expired",
                                    false
                                )
                        )
                    )
                }
            }
        }

    fun workflowRuns(
        owner: String,
        repository: String
    ): Result<List<GitHubWorkflowRun>> =
        api.get(
            "$BASE/repos/$owner/$repository/actions/runs?per_page=20"
        ).map { body ->
            val runs =
                JSONObject(body)
                    .getJSONArray("workflow_runs")

            buildList {
                for (i in 0 until runs.length()) {
                    val item =
                        runs.getJSONObject(i)

                    add(
                        GitHubWorkflowRun(
                            id =
                                item.getLong("id"),
                            status =
                                item.optString(
                                    "status",
                                    null
                                ),
                            conclusion =
                                item.optString(
                                    "conclusion",
                                    null
                                ),
                            branch =
                                item.optString(
                                    "head_branch",
                                    null
                                ),
                            workflowName =
                                item.optString(
                                    "name",
                                    null
                                )
                        )
                    )
                }
            }
        }
}
