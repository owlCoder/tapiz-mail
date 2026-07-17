package rs.tapizlabs.mail.ui.detail

import android.content.Intent
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MarkEmailUnread
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import rs.tapizlabs.mail.ui.components.BackArrowButton
import rs.tapizlabs.mail.ui.i18n.LocalStrings
import rs.tapizlabs.mail.ui.i18n.Strings
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
    val strings = LocalStrings.current

    Scaffold(
        modifier = modifier,
        containerColor = colors.canvasTop,
        bottomBar = {
            if (!uiState.notFound) {
                DetailBottomBar(
                    onReply = { onReply(uiState.messageId) },
                    onForward = { onForward(uiState.messageId) },
                    strings = strings,
                )
            }
        },
    ) { padding ->
        if (uiState.notFound) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                DetailTopBar(onBack = onBack)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = strings.detailMessageNotFound,
                        style = MaterialTheme.typography.bodyMedium.copy(color = colors.textMuted),
                    )
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            DetailActionBar(
                onBack = onBack,
                onDelete = { viewModel.delete(onDeleted = onBack) },
                onMarkUnread = { viewModel.markUnread(); onBack() },
            )

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                MessageHeader(
                    fromName = uiState.fromName,
                    fromAddress = uiState.fromAddress,
                    toAddresses = uiState.toAddresses,
                    isStarred = uiState.isStarred,
                    onToggleStar = { viewModel.toggleStar(uiState.isStarred) },
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = uiState.subject.ifBlank { strings.detailNoSubject },
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                    ),
                )

                Spacer(Modifier.height(16.dp))

                MessageBody(bodyHtml = uiState.bodyHtml, bodyPlain = uiState.bodyPlain)

                if (uiState.attachments.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = strings.detailAttachmentsCount(uiState.attachments.size),
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    Spacer(Modifier.height(8.dp))
                    uiState.attachments.forEach { attachment ->
                        AttachmentRow(
                            attachment = attachment,
                            onDownload = { onReady -> viewModel.downloadAttachment(attachment.id, onReady) },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackArrowButton(onBack)
    }
}

/** Back on the left, message actions (delete/mark-unread) end-aligned on the right — all in
 * one row, not stacked (a stacked back-then-actions layout was tried and explicitly rejected
 * in favor of this single-row arrangement). */
@Composable
private fun DetailActionBar(onBack: () -> Unit, onDelete: () -> Unit, onMarkUnread: () -> Unit) {
    val colors = AppColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Back",
                tint = colors.textPrimary,
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Delete",
                tint = colors.textMuted,
            )
        }
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onMarkUnread, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Outlined.MarkEmailUnread,
                contentDescription = "Mark as unread",
                tint = colors.textMuted,
            )
        }
    }
}

@Composable
private fun DetailBottomBar(onReply: () -> Unit, onForward: () -> Unit, strings: Strings) {
    val colors = AppColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.canvasTop)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PastelActionButton(
            text = strings.detailReply,
            icon = Icons.AutoMirrored.Outlined.Send,
            onClick = onReply,
            modifier = Modifier.weight(1f),
        )
        PastelActionButton(
            text = strings.detailForward,
            icon = Icons.AutoMirrored.Outlined.Forward,
            onClick = onForward,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Reference: Reply/Forward are both the same pastel-bg pill (cardSubtle/accentSoft),
 * not a primary+ghost pair — 50/50 split with a small corner-arrow icon each. */
@Composable
private fun PastelActionButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppColors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cardSubtle)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(7.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(color = colors.textPrimary),
        )
    }
}

@Composable
private fun MessageHeader(
    fromName: String,
    fromAddress: String,
    toAddresses: List<String>,
    isStarred: Boolean,
    onToggleStar: () -> Unit,
) {
    val colors = AppColors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
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
                    fontWeight = FontWeight.Bold,
                ),
            )
            if (toAddresses.isNotEmpty()) {
                Text(
                    text = "to ${toAddresses.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textMuted),
                )
            }
        }
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
private fun MessageBody(bodyHtml: String?, bodyPlain: String) {
    val colors = AppColors
    if (bodyHtml != null) {
        val backgroundArgb = colors.canvasTop.toArgb()
        val textArgb = colors.textPrimary.toArgb()
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                WebView(context).apply {
                    // Untrusted remote email HTML: JS stays off, no file/content access.
                    settings.javaScriptEnabled = false
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    setBackgroundColor(backgroundArgb)
                }
            },
            update = { webView ->
                webView.setBackgroundColor(backgroundArgb)
                webView.loadDataWithBaseURL(null, wrapEmailHtml(bodyHtml, backgroundArgb, textArgb), "text/html", "UTF-8", null)
            },
        )
    } else {
        PlainTextBody(bodyPlain)
    }
}

/** Forces remote email HTML to respect the app's current theme instead of showing through
 * with its own (usually white) background — email HTML almost never ships a dark-mode
 * variant. `!important` on `html`/`body` covers the common case where the message doesn't
 * set an inline background directly on `<body>`; deeply-nested elements with their own
 * explicit `background:white` divs are a known remaining limitation (no JS means no DOM
 * rewriting is possible here). */
private fun wrapEmailHtml(bodyHtml: String, backgroundArgb: Int, textArgb: Int): String {
    val backgroundHex = String.format("#%06X", 0xFFFFFF and backgroundArgb)
    val textHex = String.format("#%06X", 0xFFFFFF and textArgb)
    return """
        <html>
        <head>
        <meta name="color-scheme" content="light dark">
        <style>
            html, body {
                background: $backgroundHex !important;
                color: $textHex !important;
            }
        </style>
        </head>
        <body>$bodyHtml</body>
        </html>
    """.trimIndent()
}

/** Renders plain-text bodies with `>`-prefixed reply/forward quotes visually set apart
 * (muted color, smaller type, left rule) — [bodyPlain] otherwise reads as one undifferentiated
 * wall of text once a reply chain has a few levels of quoting. Groups consecutive quoted
 * lines into a single block rather than drawing a rule per line. */
@Composable
private fun PlainTextBody(bodyPlain: String) {
    val colors = AppColors
    val lines = bodyPlain.lines()
    var index = 0
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        while (index < lines.size) {
            val line = lines[index]
            if (line.trimStart().startsWith(">")) {
                val quoteLines = mutableListOf<String>()
                while (index < lines.size && lines[index].trimStart().startsWith(">")) {
                    quoteLines.add(lines[index].trimStart().removePrefix(">").trimStart())
                    index++
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .padding(vertical = 4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(2.dp)
                            .background(colors.stroke),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = quoteLines.joinToString("\n"),
                        style = MaterialTheme.typography.bodySmall.copy(color = colors.textMuted),
                    )
                }
            } else {
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
                )
                index++
            }
        }
    }
}

@Composable
private fun AttachmentRow(attachment: AttachmentUi, onDownload: (onReady: (uri: String) -> Unit) -> Unit) {
    val colors = AppColors
    val context = LocalContext.current
    val shape = RoundedCornerShape(12.dp)

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(attachment.mimeType.ifBlank { "*/*" }),
    ) { destinationUri ->
        val localUri = attachment.localUri ?: return@rememberLauncherForActivityResult
        if (destinationUri != null) {
            context.contentResolver.openInputStream(android.net.Uri.parse(localUri))?.use { input ->
                context.contentResolver.openOutputStream(destinationUri)?.use { out -> input.copyTo(out) }
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
        fun openAttachment(localUri: String) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(android.net.Uri.parse(localUri), attachment.mimeType.ifBlank { "*/*" })
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        }

        if (attachment.isDownloading) {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                rs.tapizlabs.mail.ui.components.MailPulseSpinner(size = 22.dp, showIcon = false)
            }
        } else {
            IconButton(
                onClick = {
                    // Not yet cached locally — fetch it first (see MailDetailViewModel.
                    // downloadAttachment), then immediately follow through with the open the
                    // user actually tapped, instead of requiring a second tap once it lands.
                    attachment.localUri?.let(::openAttachment) ?: onDownload(::openAttachment)
                },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = "Open",
                    tint = colors.textMuted,
                )
            }
            IconButton(
                onClick = {
                    attachment.localUri?.let { saveLauncher.launch(attachment.fileName) }
                        ?: onDownload { saveLauncher.launch(attachment.fileName) }
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = "Save to device",
                    tint = colors.textMuted,
                )
            }
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024 -> "%.0f KB".format(bytes / 1_024.0)
    else -> "$bytes B"
}
