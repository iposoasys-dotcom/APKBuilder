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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
            mutableStateOf(
                CompatibilityChecker.check(this@CompatibilityActivity)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "Device Compatibility",
                    style = MaterialTheme.typography.headlineSmall
                )

                TextButton(
                    onClick = {
                        report =
                            CompatibilityChecker.check(
                                this@CompatibilityActivity
                            )
                    }
                ) {
                    Text("Refresh")
                }
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            val headline =
                if (report.localBuildReady) {
                    "🟢 Local build ready"
                } else {
                    "🟡 Device compatible — toolchain required"
                }

            Text(
                text = headline,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            HorizontalDivider()

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {

                items(report.items) { item ->
                    CompatibilityCard(item)
                }

                item {
                    Spacer(
                        modifier = Modifier.height(12.dp)
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
                        Text("Run Compatibility Test Again")
                    }

                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun CompatibilityCard(
        item: CompatibilityItem
    ) {

        val icon =
            when (item.state) {
                CheckState.PASS -> "✓"
                CheckState.WARNING -> "!"
                CheckState.FAIL -> "✕"
            }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {

            Column(
                modifier = Modifier.padding(14.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Text(
                        text = "$icon  ${item.name}",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = item.value,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (item.details.isNotBlank()) {

                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )

                    Text(
                        text = item.details,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
