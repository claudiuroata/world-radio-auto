package com.worldradio.auto.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldradio.auto.ui.theme.DarkCardBorder
import com.worldradio.auto.ui.theme.DarkCardElevated
import com.worldradio.auto.ui.theme.DarkCardSurface
import com.worldradio.auto.ui.theme.DeepObsidian
import com.worldradio.auto.ui.theme.ElectricEmerald
import com.worldradio.auto.ui.theme.TextHighEmphasis
import com.worldradio.auto.ui.theme.TextLowEmphasis
import com.worldradio.auto.ui.theme.TextMediumEmphasis
import com.worldradio.auto.util.DiagnosticLogger

@Composable
fun DiagnosticsDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val installer = remember { DiagnosticLogger.getInstallerPackage(context) }
    val isBatteryUnrestricted = remember { DiagnosticLogger.isIgnoringBatteryOptimizations(context) }
    var logsText by remember { mutableStateOf(DiagnosticLogger.getAllLogsText()) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = DarkCardSurface,
        title = {
            Text(
                text = "Android Auto Diagnostics",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextHighEmphasis
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Installer Source Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCardElevated),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkCardBorder))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isPlayStore = installer == "com.android.vending"
                            Icon(
                                imageVector = if (isPlayStore) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isPlayStore) ElectricEmerald else Color(0xFFFFB74D),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Column {
                                Text(
                                    text = "Installer Source",
                                    fontSize = 12.sp,
                                    color = TextLowEmphasis
                                )
                                Text(
                                    text = if (installer == "com.android.vending") "Google Play Store (Verified)" else installer,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextHighEmphasis
                                )
                            }
                        }
                        if (installer != "com.android.vending") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Notice: Because the app is sideloaded, Android Auto requires 'Unknown sources' enabled in AA Developer Settings.",
                                fontSize = 11.sp,
                                color = TextLowEmphasis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Battery Optimization Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCardElevated),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkCardBorder))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isBatteryUnrestricted) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isBatteryUnrestricted) ElectricEmerald else Color(0xFFFF5252),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Column {
                                Text(
                                    text = "Battery Optimization",
                                    fontSize = 12.sp,
                                    color = TextLowEmphasis
                                )
                                Text(
                                    text = if (isBatteryUnrestricted) "Unrestricted (Allowed in background)" else "Optimized (Warning: May be frozen)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextHighEmphasis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isBatteryUnrestricted) {
                                "Unrestricted is active. Samsung One UI automatically excludes Unrestricted apps from the 'Never auto sleeping' list."
                            } else {
                                "Tip: Set App Info > Battery > Unrestricted so Samsung never puts the app to sleep."
                            },
                            fontSize = 11.sp,
                            color = TextLowEmphasis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Live Connection & Lifecycle Logs:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextHighEmphasis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Scrollable Monospace Logs Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 220.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DeepObsidian)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = if (logsText.isNotBlank()) logsText else "No logs recorded yet.",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ElectricEmerald,
                        lineHeight = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action buttons: Copy & Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("WorldRadioAuto Diagnostics", logsText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Diagnostic logs copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextHighEmphasis)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Copy Logs", fontSize = 12.sp)
                    }

                    TextButton(
                        onClick = {
                            DiagnosticLogger.clearLogs()
                            logsText = DiagnosticLogger.getAllLogsText()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5252))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Clear", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismissRequest,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricEmerald)
            ) {
                Text("Close", color = DeepObsidian, fontWeight = FontWeight.Bold)
            }
        }
    )
}
