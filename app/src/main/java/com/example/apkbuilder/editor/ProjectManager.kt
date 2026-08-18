package com.example.apkbuilder.editor

import android.content.Context
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ProjectManager(private val context: Context) {

    private val root =
        File(context.filesDir, "projects").apply {
            mkdirs()
        }

    fun projects(): List<File> =
        root.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()

    fun createProject(
        name: String,
        packageName: String,
        language: String
    ): File {

        require(name.matches(Regex("[A-Za-z][A-Za-z0-9 _-]*"))) {
            "Invalid project name."
        }

        require(
            packageName.matches(
                Regex("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+")
            )
        ) {
            "Invalid package name."
        }

        require(language == "java" || language == "kotlin") {
            "Language must be Java or Kotlin."
        }

        val project =
            File(root, name)

        require(!project.exists()) {
            "Project already exists."
        }

        val packagePath =
            packageName.replace('.', '/')

        val src =
            File(
                project,
                "app/src/main/java/$packagePath"
            )

        src.mkdirs()

        File(
            project,
            "app/src/main/res/values"
        ).mkdirs()

        File(
            project,
            "app/src/main/res/drawable"
        ).mkdirs()

        val safeName =
            name
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")

        File(
            project,
            "settings.gradle"
        ).writeText(
            """
            pluginManagement {
                repositories {
                    google()
                    mavenCentral()
                    gradlePluginPortal()
                }
            }

            dependencyResolutionManagement {
                repositoriesMode.set(
                    RepositoriesMode.FAIL_ON_PROJECT_REPOS
                )
                repositories {
                    google()
                    mavenCentral()
                }
            }

            rootProject.name = "GeneratedApp"
            include(":app")
            """.trimIndent()
        )

        File(
            project,
            "build.gradle"
        ).writeText(
            """
            plugins {
                id 'com.android.application' version '8.5.2' apply false
            }
            """.trimIndent()
        )

        File(
            project,
            "app/build.gradle"
        ).apply {
            parentFile!!.mkdirs()

            writeText(
                """
                plugins {
                    id 'com.android.application'
                }

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

        File(
            project,
            "app/src/main/res/values/styles.xml"
        ).writeText(
            """
            <resources>
                <style
                    name="AppTheme"
                    parent="android:style/Theme.Material.Light.NoActionBar"/>
            </resources>
            """.trimIndent()
        )

        File(
            project,
            "app/src/main/AndroidManifest.xml"
        ).apply {
            parentFile!!.mkdirs()

            writeText(
                """
                <manifest
                    xmlns:android="http://schemas.android.com/apk/res/android">

                    <application
                        android:theme="@style/AppTheme"
                        android:label="$safeName">

                        <activity
                            android:name=".MainActivity"
                            android:exported="true">

                            <intent-filter>
                                <action
                                    android:name="android.intent.action.MAIN"/>

                                <category
                                    android:name="android.intent.category.LAUNCHER"/>
                            </intent-filter>

                        </activity>

                    </application>

                </manifest>
                """.trimIndent()
            )
        }

        if (language == "kotlin") {

            File(
                src,
                "MainActivity.kt"
            ).writeText(
                """
                package $packageName

                import android.app.Activity
                import android.os.Bundle
                import android.widget.TextView

                class MainActivity : Activity() {

                    override fun onCreate(
                        savedInstanceState: Bundle?
                    ) {
                        super.onCreate(savedInstanceState)

                        val text =
                            TextView(this)

                        text.text =
                            "$safeName"

                        text.textSize =
                            28f

                        setContentView(text)
                    }
                }
                """.trimIndent()
            )

        } else {

            File(
                src,
                "MainActivity.java"
            ).writeText(
                """
                package $packageName;

                import android.app.Activity;
                import android.os.Bundle;
                import android.widget.TextView;

                public class MainActivity extends Activity {

                    @Override
                    protected void onCreate(
                        Bundle savedInstanceState
                    ) {
                        super.onCreate(savedInstanceState);

                        TextView text =
                            new TextView(this);

                        text.setText("$safeName");
                        text.setTextSize(28);

                        setContentView(text);
                    }
                }
                """.trimIndent()
            )
        }

        return project
    }

    fun renameProject(
        project: File,
        newName: String
    ): File {

        require(project.exists() && project.isDirectory) {
            "Project does not exist."
        }

        require(
            newName.matches(
                Regex("[A-Za-z][A-Za-z0-9 _-]*")
            )
        ) {
            "Invalid project name."
        }

        val destination =
            File(root, newName)

        require(!destination.exists()) {
            "A project with that name already exists."
        }

        require(project.renameTo(destination)) {
            "Could not rename project."
        }

        return destination
    }

    fun deleteProject(
        project: File
    ) {
        require(
            project.canonicalFile
                .parentFile
                ?.canonicalFile == root.canonicalFile
        ) {
            "Invalid project location."
        }

        require(project.exists()) {
            "Project does not exist."
        }

        require(project.deleteRecursively()) {
            "Could not delete project."
        }
    }

    fun duplicateProject(
        project: File,
        newName: String
    ): File {

        require(project.exists() && project.isDirectory) {
            "Project does not exist."
        }

        val destination =
            File(root, newName)

        require(!destination.exists()) {
            "Destination project already exists."
        }

        project.copyRecursively(
            destination,
            overwrite = false
        )

        return destination
    }

    fun exportProject(
        project: File,
        destination: File
    ) {

        require(project.exists() && project.isDirectory) {
            "Project does not exist."
        }

        destination.parentFile?.mkdirs()

        ZipOutputStream(
            destination.outputStream()
        ).use { zip ->

            project.walkTopDown()
                .filter { it.isFile }
                .forEach { file ->

                    val relative =
                        file.relativeTo(project)
                            .path
                            .replace(
                                File.separatorChar,
                                '/'
                            )

                    zip.putNextEntry(
                        ZipEntry(relative)
                    )

                    file.inputStream().use {
                        it.copyTo(zip)
                    }

                    zip.closeEntry()
                }
        }
    }

    fun importProject(
        zipFile: File,
        projectName: String
    ): File {

        require(zipFile.isFile) {
            "ZIP file does not exist."
        }

        val destination =
            File(root, projectName)

        require(!destination.exists()) {
            "Project already exists."
        }

        destination.mkdirs()

        try {

            ZipInputStream(
                zipFile.inputStream()
            ).use { zip ->

                while (true) {

                    val entry =
                        zip.nextEntry
                            ?: break

                    val target =
                        File(
                            destination,
                            entry.name
                        ).canonicalFile

                    val base =
                        destination.canonicalFile

                    require(
                        target.path == base.path ||
                            target.path.startsWith(
                                base.path +
                                    File.separator
                            )
                    ) {
                        "Unsafe ZIP entry."
                    }

                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()

                        target.outputStream().use {
                            zip.copyTo(it)
                        }
                    }

                    zip.closeEntry()
                }
            }

        } catch (e: Exception) {

            destination.deleteRecursively()
            throw e
        }

        return destination
    }

    fun listFiles(
        project: File
    ): List<ProjectFile> =
        project.walkTopDown()
            .filter {
                it != project
            }
            .sortedBy {
                it.relativeTo(project).path
            }
            .map {
                ProjectFile(
                    file = it,
                    relativePath =
                        it.relativeTo(project).path,
                    isDirectory =
                        it.isDirectory
                )
            }
            .toList()
}
