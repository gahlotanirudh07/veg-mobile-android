package com.freshveg.app.core.ui.components

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
                    if (downloadState is UpdateDownloadState.Installing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = ActionGreen,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = when (downloadState) {
                                is UpdateDownloadState.ReadyToInstall -> Icons.Default.CheckCircle
                                is UpdateDownloadState.Error -> Icons.Default.ErrorOutline
                                is UpdateDownloadState.Downloading -> Icons.Default.Download
                                else -> Icons.Default.SystemUpdate
                            },
                            contentDescription = null,
                            tint = when (downloadState) {
                                is UpdateDownloadState.ReadyToInstall -> ActionGreen
                                is UpdateDownloadState.Error -> RedError
                                else -> ActionGreen
                            },
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = when (downloadState) {
                        is UpdateDownloadState.Installing -> "Installing Update..."
                        is UpdateDownloadState.ReadyToInstall -> "Update Ready to Install"
                        is UpdateDownloadState.Downloading -> "Downloading Update..."
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
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (downloadState.progressPercent >= 0) {
                                LinearProgressIndicator(
                                    progress = { downloadState.progressPercent / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = ActionGreen,
                                    trackColor = SecondarySurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "${downloadState.progressPercent}%",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MainInk
                                    )
                                    Text(
                                        formatBytesProgress(
                                            downloadState.bytesDownloaded,
                                            downloadState.totalBytes
                                        ),
                                        fontSize = 11.sp,
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
                                    trackColor = SecondarySurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Downloading MandiExpress update...",
                                    fontSize = 12.sp,
                                    color = InkSecondary
                                )
                            }
                        }
                    }

                    is UpdateDownloadState.Installing -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ActionGreen.copy(alpha = 0.08f),
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
                                    modifier = Modifier.size(22.dp),
                                    color = ActionGreen,
                                    strokeWidth = 2.dp
                                )
                                Column {
                                    Text(
                                        text = "Launching Installer...",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MainInk
                                    )
                                    Text(
                                        text = "Please tap 'Update' on the system prompt to finish installation.",
                                        fontSize = 11.5.sp,
                                        color = InkSecondary
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
                    // Downloading in progress
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
                        Text("Reopen Installer", fontWeight = FontWeight.Bold)
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
            if (isDismissable && downloadState !is UpdateDownloadState.ReadyToInstall && downloadState !is UpdateDownloadState.Installing) {
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
    if (total <= 0) return "${(downloaded / (1024 * 1024.0)).format(1)} MB"
    val dMb = downloaded / (1024 * 1024.0)
    val tMb = total / (1024 * 1024.0)
    return "${dMb.format(1)} MB / ${tMb.format(1)} MB"
}

private fun Double.format(digits: Int): String {
    return String.format(java.util.Locale.US, "%.${digits}f", this)
}
