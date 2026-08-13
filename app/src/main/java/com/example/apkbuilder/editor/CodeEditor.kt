package com.example.apkbuilder.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CodeEditor(
    fileName: String,
    initialCode: String,
    onSave: (String) -> Unit
) {
    var code by remember(fileName, initialCode) {
        mutableStateOf(initialCode)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(fileName, style = MaterialTheme.typography.titleMedium)
            Button({ onSave(code) }) { Text("Save") }
        }

        HorizontalDivider()

        OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            modifier = Modifier.fillMaxSize().padding(8.dp)
        )
    }
}
