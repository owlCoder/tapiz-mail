package rs.tapizlabs.mail.ui.detail

import android.content.Intent
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File
import java.text.DateFormat
import java.util.Date
import rs.tapizlabs.mail.ui.components.MailGhostButton
import rs.tapizlabs.mail.ui.components.MailPrimaryButton
import rs.tapizlabs.mail.ui.theme.AppColors

/**
 * Full message view. Renders [MailDetailUiState.bodyHtml] via a JS-disabled [WebView] when
 * present (untrusted remote HTML — never execute scripts), falling back to plain text.
 *
 * @param onReply / [onForward] navigate to Compose pre-filled — actual nav route wiring is the
 * nav-graph agent's job, this screen only needs the message id back out.
 * @param onBack pops back to Inbox/Search.
 */
@Composable
fun MailDetailScreen(
    onReply: (messageId: String) -> Unit,
    onForward: (messageId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MailDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = AppColors

    Scaffold(
        modifier = modifier,
        containerColor = colors.canvasTop,
        topBar = {
            DetailTopBar(
                isStarred = uiState.isStarred,
                onBack = onBack,
                onToggleStar = { viewModel.toggleStar(uiState.isStarred) },
            )
        },
        bottomBar = {
            if (!uiState.notFound) {
                DetailBottomBar(
                    onReply = { onReply(uiState.messageId) },
                    onForward = { onForward(uiState.messageId) },
                )
            }
        },
    ) { padding ->
        if (uiState.notFound) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Message not found",
                    style = MaterialTheme.typography.bodyMedium.copy(color = colors.textMuted),
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                text = uiState.subject.ifBlank { "(no subject)" },
                style = MaterialTheme.typography.titleLarge.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                ),
            )

            Spacer(Modifier.height(12.dp))

            MessageHeader(
                fromName = uiState.fromName,
                fromAddress = uiState.fromAddress,
                toAddresses = uiState.toAddresses,
                sentAt = uiState.sentAt,
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = colors.stroke.copy(alpha = 0.5f))
            Spacer(Modifier.height(16.dp))

            MessageBody(bodyHtml = uiState.bodyHtml, bodyPlain = uiState.bodyPlain)

            if (uiState.attachments.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Attachments (${uiState.attachments.size})",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Spacer(Modifier.height(8.dp))
                uiState.attachments.forEach { attachment ->
                    AttachmentRow(attachment = attachment)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailTopBar(isStarred: Boolean, onBack: () -> Unit, onToggleStar: () -> Unit) {
    val colors = AppColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = colors.textPrimary,
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onToggleStar) {
            Icon(
                imageVector = if (isStarred) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                contentDescription = if (isStarred) "Unstar" else "Star",
                tint = if (isStarred) colors.amber else colors.textMuted,
            )
        }
    }
}

@Composable
private fun DetailBottomBar(onReply: () -> Unit, onForward: () -> Unit) {
    val colors = AppColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.card)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MailGhostButton(
            text = "Forward",
            icon = Icons.AutoMirrored.Outlined.Forward,
            onClick = onForward,
            modifier = Modifier.weight(1f),
        )
        MailPrimaryButton(
            text = "Reply",
            onClick = onReply,
            icon = Icons.AutoMirrored.Outlined.Send,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MessageHeader(
    fromName: String,
    fromAddress: String,
    toAddresses: List<String>,
    sentAt: Long,
) {
    val colors = AppColors
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = (fromName.trim().firstOrNull() ?: fromAddress.firstOrNull() ?: '?')
                    .uppercaseChar().toString(),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fromName.ifBlank { fromAddress },
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Text(
                text = fromAddress,
                style = MaterialTheme.typography.bodySmall.copy(color = colors.textMuted),
            )
            if (toAddresses.isNotEmpty()) {
                Text(
                    text = "to ${toAddresses.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textMuted),
                )
            }
        }
        Text(
            text = formatSentAt(sentAt),
            style = MaterialTheme.typography.labelSmall.copy(color = colors.textMuted),
        )
    }
}

@Composable
private fun MessageBody(bodyHtml: String?, bodyPlain: String) {
    val colors = AppColors
    if (bodyHtml != null) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                WebView(context).apply {
                    // Untrusted remote email HTML: JS stays off, no file/content access.
                    settings.javaScriptEnabled = false
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL(null, bodyHtml, "text/html", "UTF-8", null)
            },
        )
    } else {
        Text(
            text = bodyPlain,
            style = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
        )
    }
}

@Composable
private fun AttachmentRow(attachment: AttachmentUi) {
    val colors = AppColors
    val context = LocalContext.current
    val shape = RoundedCornerShape(12.dp)

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(attachment.mimeType.ifBlank { "*/*" }),
    ) { destinationUri ->
        val localUri = attachment.localUri ?: return@rememberLauncherForActivityResult
        if (destinationUri != null) {
            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                File(localUri).inputStream().use { input -> input.copyTo(out) }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.cardSubtle)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.AttachFile,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = attachment.fileName,
                style = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
                maxLines = 1,
            )
            Text(
                text = formatSize(attachment.sizeBytes),
                style = MaterialTheme.typography.labelSmall.copy(color = colors.textMuted),
            )
        }
        IconButton(
            onClick = {
                val localUri = attachment.localUri ?: return@IconButton
                val file = File(localUri)
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(contentUri, attachment.mimeType.ifBlank { "*/*" })
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
            },
            enabled = attachment.localUri != null,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = "Open",
                tint = colors.textMuted,
            )
        }
        IconButton(
            onClick = { saveLauncher.launch(attachment.fileName) },
            enabled = attachment.localUri != null,
        ) {
            Icon(
                imageVector = Icons.Outlined.Download,
                contentDescription = "Save to device",
                tint = colors.textMuted,
            )
        }
    }
}

private fun formatSentAt(epochMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMillis))

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024 -> "%.0f KB".format(bytes / 1_024.0)
    else -> "$bytes B"
}
