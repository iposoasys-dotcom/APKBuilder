package com.example.apkbuilder.editor

import android.content.Context
import java.io.File

class ProjectManager(private val context: Context) {
    private val root = File(context.filesDir, "projects")

    init { root.mkdirs() }

    fun createProject(
        name: String,
        packageName: String,
        language: String
    ): File {
        require(name.matches(Regex("[A-Za-z][A-Za-z0-9 _-]*")))
        require(packageName.matches(
            Regex("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+")
        ))
        require(language == "java" || language == "kotlin")

        val project = File(root, name)
        require(!project.exists()) { "Project already exists" }

        val packagePath = packageName.replace('.', '/')
        val src = File(project, "app/src/main/java/$packagePath")
        src.mkdirs()
        File(project, "app/src/main/res/values").mkdirs()
        File(project, "app/src/main/res/layout").mkdirs()

        File(project, "app/src/main/AndroidManifest.xml").writeText(
            """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <application
                    android:theme="@style/AppTheme"
                    android:label="$name">
                    <activity
                        android:name=".MainActivity"
                        android:exported="true">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN"/>
                            <category android:name="android.intent.category.LAUNCHER"/>
                        </intent-filter>
                    </activity>
                </application>
            </manifest>
            """.trimIndent()
        )

        File(project, "app/src/main/res/values/styles.xml").writeText(
            """
            <resources>
                <style name="AppTheme"
                    parent="android:style/Theme.Material.Light.NoActionBar"/>
            </resources>
            """.trimIndent()
        )

        val safeName = name.replace("\\", "\\\\").replace("\"", "\\\"")

        if (language == "kotlin") {
            File(src, "MainActivity.kt").writeText(
                """
                package $packageName

                import android.app.Activity
                import android.os.Bundle
                import android.widget.TextView

                class MainActivity : Activity() {
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        val text = TextView(this)
                        text.text = "$safeName"
                        text.textSize = 28f
                        setContentView(text)
                    }
                }
                """.trimIndent()
            )
        } else {
            File(src, "MainActivity.java").writeText(
                """
                package $packageName;

                import android.app.Activity;
                import android.os.Bundle;
                import android.widget.TextView;

                public class MainActivity extends Activity {
                    @Override
                    protected void onCreate(Bundle savedInstanceState) {
                        super.onCreate(savedInstanceState);
                        TextView text = new TextView(this);
                        text.setText("$safeName");
                        text.setTextSize(28);
                        setContentView(text);
                    }
                }
                """.trimIndent()
            )
        }

        File(project, "settings.gradle").writeText(
            """
            pluginManagement {
                repositories {
                    google()
                    mavenCentral()
                    gradlePluginPortal()
                }
            }
            dependencyResolutionManagement {
                repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                repositories {
                    google()
                    mavenCentral()
                }
            }
            rootProject.name = "GeneratedApp"
            include(":app")
            """.trimIndent()
        )

        File(project, "build.gradle").writeText(
            """
            plugins {
                id 'com.android.application' version '8.5.2' apply false
            }
            """.trimIndent()
        )

        File(project, "app/build.gradle").apply {
            parentFile!!.mkdirs()
            writeText(
                """
                plugins { id 'com.android.application' }

                android {
                    namespace '$packageName'
                    compileSdk 35
                    defaultConfig {
                        applicationId '$packageName'
                        minSdk 23
                        targetSdk 35
                        versionCode 1
                        versionName "1.0"
                    }
                }
                """.trimIndent()
            )
        }

        return project
    }

    fun listFiles(project: File): List<ProjectFile> =
        project.walkTopDown()
            .filter { it != project }
            .sortedBy { it.relativeTo(project).path }
            .map {
                ProjectFile(
                    it,
                    it.relativeTo(project).path,
                    it.isDirectory
                )
            }
            .toList()
}
