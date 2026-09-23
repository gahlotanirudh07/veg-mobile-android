package com.freshveg.app.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.freshveg.app.core.ui.theme.*
import com.freshveg.app.core.update.UpdateDownloadState
import com.freshveg.app.core.update.UpdateInfo

@Composable
fun UpdatePromptDialog(
    updateInfo: UpdateInfo,
    downloadState: UpdateDownloadState,
    onStartDownload: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDismissable = !updateInfo.isForceUpdate &&
        downloadState !is UpdateDownloadState.Downloading &&
        downloadState !is UpdateDownloadState.Installing

    AlertDialog(
        onDismissRequest = {
            if (isDismissable) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = isDismissable,
            dismissOnClickOutside = isDismissable
        ),
        shape = RoundedCornerShape(20.dp),
        containerColor = NeutralSurface,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Icon Badge
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            when (downloadState) {
                                is UpdateDownloadState.Installing -> ActionGreen.copy(alpha = 0.15f)
                                is UpdateDownloadState.ReadyToInstall -> ActionGreen.copy(alpha = 0.15f)
                                is UpdateDownloadState.Error -> RedError.copy(alpha = 0.15f)
                                else -> ActionGreen.copy(alpha = 0.12f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when (downloadState) {
                        is UpdateDownloadState.Installing -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = ActionGreen,
                                strokeWidth = 3.dp
                            )
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = ActionGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        is UpdateDownloadState.Downloading -> {
                            if (downloadState.progressPercent in 0..100) {
                                CircularProgressIndicator(
                                    progress = { downloadState.progressPercent / 100f },
                                    modifier = Modifier.size(36.dp),
                                    color = ActionGreen,
                                    strokeWidth = 3.dp,
                                    trackColor = ActionGreen.copy(alpha = 0.2f)
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    color = ActionGreen,
                                    strokeWidth = 3.dp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = ActionGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        is UpdateDownloadState.ReadyToInstall -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = ActionGreen,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        is UpdateDownloadState.Error -> {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = RedError,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = ActionGreen,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = when (downloadState) {
                        is UpdateDownloadState.Installing -> "Launching Installer..."
                        is UpdateDownloadState.ReadyToInstall -> "Update Ready to Install"
                        is UpdateDownloadState.Downloading -> {
                            if (downloadState.progressPercent >= 100) "Verifying Update..."
                            else if (downloadState.progressPercent > 0) "Downloading Update (${downloadState.progressPercent}%)"
                            else "Connecting to Server..."
                        }
                        is UpdateDownloadState.Error -> "Download Interrupted"
                        else -> "App Update Available"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MainInk
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Version Badge Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SecondarySurface
                    ) {
                        Text(
                            text = "Current: v${updateInfo.currentVersionName}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = InkSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Text("?", fontSize = 12.sp, color = InkTertiary)

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ActionGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "New: v${updateInfo.latestVersionName}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ActionGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (downloadState) {
                    is UpdateDownloadState.Downloading -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SecondarySurface,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = ActionGreen,
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = downloadState.stage,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MainInk
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (downloadState.progressPercent in 0..100) {
                                    LinearProgressIndicator(
                                        progress = { downloadState.progressPercent / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = ActionGreen,
                                        trackColor = Color(0xFFE2E8F0)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${downloadState.progressPercent}% Downloaded",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = ActionGreen
                                        )
                                        Text(
                                            text = formatBytesProgress(
                                                downloadState.bytesDownloaded,
                                                downloadState.totalBytes
                                            ),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = InkSecondary
                                        )
                                    }
                                } else {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = ActionGreen,
                                        trackColor = Color(0xFFE2E8F0)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Downloading...",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = ActionGreen
                                        )
                                        Text(
                                            text = formatBytesProgress(
                                                downloadState.bytesDownloaded,
                                                downloadState.totalBytes
                                            ),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = InkSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    is UpdateDownloadState.Installing -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ActionGreen.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, ActionGreen.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(26.dp),
                                    color = ActionGreen,
                                    strokeWidth = 2.5.dp
                                )
                                Column {
                                    Text(
                                        text = "Launching Package Installer...",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MainInk
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Waiting for installation prompt. Tap 'Update' or 'Install' on your screen to complete.",
                                        fontSize = 11.5.sp,
                                        color = InkSecondary,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    is UpdateDownloadState.ReadyToInstall -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SecondarySurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "MandiExpress APK is downloaded. Tap below to launch the package installer.",
                                fontSize = 13.sp,
                                color = InkSecondary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    is UpdateDownloadState.Error -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = RedError.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = downloadState.message,
                                fontSize = 12.sp,
                                color = RedError,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    else -> {
                        // Release notes
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SecondarySurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    "What's New:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MainInk
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = updateInfo.releaseNotes,
                                    fontSize = 12.sp,
                                    color = InkSecondary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (downloadState) {
                is UpdateDownloadState.Downloading -> {
                    Button(
                        onClick = {},
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = ActionGreen.copy(alpha = 0.75f),
                            disabledContentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val btnLabel = if (downloadState.progressPercent in 0..99) {
                            "Downloading... ${downloadState.progressPercent}%"
                        } else if (downloadState.progressPercent >= 100) {
                            "Verifying Package..."
                        } else {
                            "Connecting to Server..."
                        }
                        Text(btnLabel, fontWeight = FontWeight.Bold)
                    }
                }
                is UpdateDownloadState.Installing -> {
                    Button(
                        onClick = onInstall,
                        colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Waiting for Prompt (Tap to Reopen)", fontWeight = FontWeight.Bold)
                    }
                }
                is UpdateDownloadState.ReadyToInstall -> {
                    Button(
                        onClick = onInstall,
                        colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Install Update Now", fontWeight = FontWeight.Bold)
                    }
                }
                is UpdateDownloadState.Error -> {
                    Button(
                        onClick = onStartDownload,
                        colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Retry Download", fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    Button(
                        onClick = onStartDownload,
                        colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Update Now (1-Tap)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            if (isDismissable && downloadState !is UpdateDownloadState.ReadyToInstall && downloadState !is UpdateDownloadState.Installing && downloadState !is UpdateDownloadState.Downloading) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Later", color = InkTertiary)
                }
            }
        }
    )
}

private fun formatBytesProgress(downloaded: Long, total: Long): String {
    val dMb = downloaded / (1024 * 1024.0)
    if (total <= 0) return "${dMb.format(1)} MB downloaded"
    val tMb = total / (1024 * 1024.0)
    return "${dMb.format(1)} MB / ${tMb.format(1)} MB"
}

private fun Double.format(digits: Int): String {
    return String.format(java.util.Locale.US, "%.${digits}f", this)
}
