package com.example.apkbuilder.editor
import java.io.File

data class ProjectFile(
    val file: File,
    val relativePath: String,
    val isDirectory: Boolean
)
