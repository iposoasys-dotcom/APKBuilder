package com.example.apkbuilder.compatibility

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class CompatibilityActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                CompatibilityScreen()
            }
        }
    }

    @Composable
    private fun CompatibilityScreen() {

        var report by remember {
            mutableStateOf<CompatibilityReport?>(null)
        }

        LaunchedEffect(Unit) {
            report =
                CompatibilityChecker.check(this@CompatibilityActivity)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Top
        ) {

            Text(
                text = "Device Compatibility",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            val r = report

            if (r == null) {

                Text("Testing device...")

            } else {

                Text(
                    text = r.deviceName,
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                HorizontalDivider()

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                StatusRow(
                    "64-bit Android",
                    r.is64Bit
                )

                StatusRow(
                    "ARM64 (arm64-v8a)",
                    r.arm64
                )

                StatusRow(
                    "Architecture",
                    r.architectureOk
                )

                StatusRow(
                    "RAM: ${r.ramMb} MB",
                    r.ramOk
                )

                StatusRow(
                    "Free storage: ${r.freeStorageMb} MB",
                    r.storageOk
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    "Android ${r.androidVersion} (SDK ${r.sdk})"
                )

                Text(
                    "Primary ABI: ${r.primaryAbi}"
                )

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                if (r.compatible) {

                    Text(
                        text = "DEVICE HARDWARE: COMPATIBLE",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        "The phone meets APKBuilder's minimum hardware requirements."
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        "Local compiler status: NOT INSTALLED"
                    )

                } else {

                    Text(
                        text = "DEVICE: NOT READY",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        "The required hardware/storage conditions are not currently satisfied."
                    )
                }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                Button(
                    onClick = {
                        report =
                            CompatibilityChecker.check(
                                this@CompatibilityActivity
                            )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Run Test Again")
                }

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Button(
                    onClick = {
                        finish()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back")
                }
            }
        }
    }

    @Composable
    private fun StatusRow(
        label: String,
        passed: Boolean
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
        ) {

            Text(
                text = if (passed) "✓ " else "✗ ",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = label
            )
        }
    }
}
