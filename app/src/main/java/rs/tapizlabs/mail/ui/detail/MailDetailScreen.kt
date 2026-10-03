package rs.tapizlabs.mail.ui.detail

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.webkit.WebView
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Reply
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rs.tapizlabs.mail.ui.components.BackArrowButton
import rs.tapizlabs.mail.ui.components.MailDateFormat
import rs.tapizlabs.mail.ui.i18n.LocalAppLanguage
import rs.tapizlabs.mail.ui.i18n.LocalStrings
import rs.tapizlabs.mail.ui.i18n.Strings
import rs.tapizlabs.mail.ui.i18n.toLocale
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = AppColors
    val strings = LocalStrings.current
    val context = LocalContext.current

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
                DetailActionBar(onBack = onBack, strings = strings)
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
                strings = strings,
            )

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                MessageHeader(
                    fromName = uiState.fromName,
                    fromAddress = uiState.fromAddress,
                    toAddresses = uiState.toAddresses,
                    sentAt = uiState.sentAt,
                    isStarred = uiState.isStarred,
                    onToggleStar = { viewModel.toggleStar(uiState.isStarred) },
                    strings = strings,
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
                            onDownload = { onReady ->
                                viewModel.downloadAttachment(
                                    attachmentId = attachment.id,
                                    onReady = onReady,
                                    onFailed = {
                                        Toast.makeText(context, strings.detailAttachmentDownloadFailed, Toast.LENGTH_SHORT).show()
                                    },
                                )
                            },
                            strings = strings,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

/** Back chip on the left (same [BackArrowButton] as every other pushed screen), message
 * actions (delete/mark-unread) end-aligned on the right — all in one row, not stacked (a
 * stacked back-then-actions layout was tried and explicitly rejected in favor of this
 * single-row arrangement). The actions are omitted when there's no message to act on. */
@Composable
private fun DetailActionBar(
    onBack: () -> Unit,
    strings: Strings,
    onDelete: (() -> Unit)? = null,
    onMarkUnread: (() -> Unit)? = null,
) {
    val colors = AppColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackArrowButton(onBack)
        Spacer(Modifier.weight(1f))
        if (onDelete != null) {
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = strings.swipeActionDelete,
                    tint = colors.coral,
                )
            }
        }
        if (onMarkUnread != null) {
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onMarkUnread, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Outlined.MarkEmailUnread,
                    contentDescription = strings.swipeActionMarkUnread,
                    tint = colors.textMuted,
                )
            }
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
            icon = Icons.AutoMirrored.Outlined.Reply,
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
    sentAt: Long,
    isStarred: Boolean,
    onToggleStar: () -> Unit,
    strings: Strings,
) {
    val colors = AppColors
    val locale = LocalAppLanguage.current.toLocale()
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // The address itself, when the row above shows a display name — a name alone
            // doesn't tell you who actually sent it.
            if (fromName.isNotBlank() && fromAddress.isNotBlank()) {
                Text(
                    text = fromAddress,
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textMuted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (toAddresses.isNotEmpty()) {
                Text(
                    text = "${strings.messageToPrefix}: ${toAddresses.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textMuted),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (sentAt > 0L) {
                Text(
                    text = remember(sentAt, locale) { MailDateFormat.fullTimestamp(sentAt, locale) },
                    style = MaterialTheme.typography.labelSmall.copy(color = colors.textMuted),
                )
            }
        }
        IconButton(onClick = onToggleStar) {
            Icon(
                imageVector = if (isStarred) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                contentDescription = if (isStarred) strings.actionUnstar else strings.actionStar,
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
        val linkArgb = colors.primary.toArgb()
        val isDark = colors.isDark
        val document = remember(bodyHtml, backgroundArgb, textArgb, linkArgb) {
            wrapEmailHtml(bodyHtml, backgroundArgb, textArgb, linkArgb)
        }
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                WebView(context).apply {
                    // Untrusted remote email HTML: JS stays off, no file/content access.
                    settings.javaScriptEnabled = false
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    // The page scrolls with the surrounding Column; the WebView itself only
                    // ever needs to pan sideways for a layout that can't be narrowed.
                    isVerticalScrollBarEnabled = false
                    setBackgroundColor(backgroundArgb)
                }
            },
            update = { webView ->
                webView.setBackgroundColor(backgroundArgb)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Lets WebView darken light-only email markup (white table cells etc.)
                    // when the app is in its dark theme — CSS alone can't reach those.
                    webView.settings.isAlgorithmicDarkeningAllowed = isDark
                }
                // `update` runs on every recomposition (star toggle, attachment progress…);
                // reloading the document each time made the body flash and reset scroll.
                if (webView.tag != document) {
                    webView.tag = document
                    webView.loadDataWithBaseURL(null, document, "text/html", "UTF-8", null)
                }
            },
        )
    } else {
        PlainTextBody(bodyPlain)
    }
}

/** Wraps remote email HTML so it fits a phone screen and follows the app's theme: a real
 * viewport (without one the page is laid out ~980px wide and clipped), fixed-width
 * tables/images capped to the screen, and the app's own background/text/link colors instead
 * of the mail's implicit white page. `!important` on `html`/`body` covers the common case
 * where the message doesn't set an inline background directly on `<body>`; elements with
 * their own explicit backgrounds are handled by WebView's algorithmic darkening where
 * available (see [MessageBody]) — which is also why this must NOT declare
 * `color-scheme: light dark`: that tells WebView the page handles dark mode itself and
 * switches the darkening off. */
private fun wrapEmailHtml(bodyHtml: String, backgroundArgb: Int, textArgb: Int, linkArgb: Int): String {
    val backgroundHex = String.format("#%06X", 0xFFFFFF and backgroundArgb)
    val textHex = String.format("#%06X", 0xFFFFFF and textArgb)
    val linkHex = String.format("#%06X", 0xFFFFFF and linkArgb)
    return """
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
            html, body {
                background: $backgroundHex !important;
                color: $textHex !important;
                margin: 0;
                padding: 0;
                overflow-wrap: anywhere;
            }
            body { font-size: 15px; line-height: 1.5; }
            a { color: $linkHex; }
            img, video { max-width: 100% !important; height: auto !important; }
            table { max-width: 100% !important; }
            pre { white-space: pre-wrap; }
            blockquote { margin: 8px 0 8px 12px; padding-left: 10px; border-left: 2px solid #88888866; }
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
private fun AttachmentRow(
    attachment: AttachmentUi,
    onDownload: (onReady: (uri: String) -> Unit) -> Unit,
    strings: Strings,
) {
    val colors = AppColors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(12.dp)

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(attachment.mimeType.ifBlank { "*/*" }),
    ) { destinationUri ->
        val localUri = attachment.localUri ?: return@rememberLauncherForActivityResult
        if (destinationUri != null) {
            // Off the main thread — attachments run to tens of MB.
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(android.net.Uri.parse(localUri))?.use { input ->
                            context.contentResolver.openOutputStream(destinationUri)?.use { out -> input.copyTo(out) }
                        } != null
                    }.getOrDefault(false)
                }
                val message = if (saved) strings.detailAttachmentSaved else strings.detailAttachmentDownloadFailed
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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
                overflow = TextOverflow.Ellipsis,
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
            // No installed app for this file type is an ordinary situation, not a crash.
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, strings.detailNoAppForAttachment, Toast.LENGTH_SHORT).show()
            }
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
                    contentDescription = strings.detailOpenAttachment,
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
                    contentDescription = strings.detailSaveAttachment,
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
