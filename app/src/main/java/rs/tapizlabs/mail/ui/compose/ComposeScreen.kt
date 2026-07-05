package rs.tapizlabs.mail.ui.compose

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import rs.tapizlabs.mail.ui.components.MailGhostButton
import rs.tapizlabs.mail.ui.components.MailPrimaryButton
import rs.tapizlabs.mail.ui.components.MailSheet
import rs.tapizlabs.mail.ui.i18n.LocalStrings
import rs.tapizlabs.mail.ui.i18n.Strings
import rs.tapizlabs.mail.ui.theme.AppColors

/**
 * Compose screen — matches design_handoff_tapiz_mail_android/design-reference.html's
 * "Compose" screen: X (close) left, "New message" centered title, filled rounded-square
 * send button right, divider below; From/To/Subject flat rows (each with its own bottom
 * divider, no card background); free-text body; bottom attach/camera/image toolbar with a
 * divider above. Handles New/Reply/Forward, driven by nav args read inside [ComposeViewModel]
 * (`mode` + `messageId` via `SavedStateHandle`).
 *
 * @param onSent invoked once the message finishes sending successfully (navigate back).
 * @param onBack invoked on the back/close action without sending.
 */
@Composable
fun ComposeScreen(
    onSent: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ComposeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = AppColors
    val strings = LocalStrings.current
    val context = LocalContext.current
    var showExitSheet by rememberSaveable { mutableStateOf(false) }

    if (uiState.sent) {
        onSent()
        return
    }

    val requestExit = {
        if (uiState.hasContent) {
            showExitSheet = true
        } else {
            viewModel.saveDraftAndExit(onDone = onBack)
        }
    }

    BackHandler(onBack = requestExit)

    val attachmentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        val picked = uris.map { uri ->
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            ComposeAttachmentUi(
                uri = uri.toString(),
                displayName = queryDisplayName(context, uri) ?: uri.lastPathSegment ?: "attachment",
            )
        }
        viewModel.addAttachments(picked)
    }

    Scaffold(
        modifier = modifier.statusBarsPadding(),
        containerColor = colors.canvasTop,
        topBar = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                ) {
                    IconButton(
                        onClick = requestExit,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = colors.textMuted,
                        )
                    }
                    Text(
                        text = strings.composeNewMessage,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 44.dp),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(36.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (uiState.isSending || uiState.to.isBlank()) colors.primary.copy(alpha = 0.4f) else colors.primary)
                            .clickable(enabled = !uiState.isSending && uiState.to.isNotBlank(), onClick = viewModel::send),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (uiState.isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = colors.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Send,
                                contentDescription = "Send",
                                tint = colors.onPrimary,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
                HorizontalDivider(color = colors.stroke)
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = colors.stroke)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    IconButton(onClick = { attachmentPicker.launch(arrayOf("*/*")) }) {
                        Icon(
                            imageVector = Icons.Outlined.AttachFile,
                            contentDescription = "Add attachment",
                            tint = colors.textMuted,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                    IconButton(onClick = { attachmentPicker.launch(arrayOf("image/*")) }) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoCamera,
                            contentDescription = "Add photo",
                            tint = colors.textMuted,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                    IconButton(onClick = { attachmentPicker.launch(arrayOf("image/*")) }) {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = "Add image",
                            tint = colors.textMuted,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                LabeledField(
                    label = strings.composeFrom,
                    value = uiState.fromEmail,
                    onValueChange = {},
                    enabled = false,
                )

                RecipientFields(
                    to = uiState.to,
                    cc = uiState.cc,
                    bcc = uiState.bcc,
                    ccBccExpanded = uiState.ccBccExpanded,
                    onToChange = viewModel::updateTo,
                    onCcChange = viewModel::updateCc,
                    onBccChange = viewModel::updateBcc,
                    onToggleCcBcc = viewModel::toggleCcBcc,
                    strings = strings,
                )

                LabeledField(
                    label = strings.composeSubject,
                    value = uiState.subject,
                    onValueChange = viewModel::updateSubject,
                    showDivider = false,
                )
            }

            Spacer(Modifier.height(16.dp))

            if (uiState.attachments.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    uiState.attachments.forEach { attachment ->
                        AttachmentChip(
                            name = attachment.displayName,
                            onRemove = { viewModel.removeAttachment(attachment.uri) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp)) {
                if (uiState.body.isEmpty()) {
                    Text(
                        text = strings.composeBodyPlaceholder,
                        style = MaterialTheme.typography.bodyMedium.copy(color = colors.textMuted),
                    )
                }
                BasicTextField(
                    value = uiState.body,
                    onValueChange = viewModel::updateBody,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary, lineHeight = 22.sp),
                    cursorBrush = SolidColor(colors.primary),
                )
            }

            if (uiState.sendError != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = uiState.sendError.orEmpty(),
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.coral),
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    ComposeExitSheet(
        visible = showExitSheet,
        onDismiss = { showExitSheet = false },
        onSaveDraft = {
            showExitSheet = false
            viewModel.saveDraftAndExit(onDone = onBack)
        },
        onDiscard = {
            showExitSheet = false
            viewModel.discardAndExit(onDone = onBack)
        },
        strings = strings,
    )
}

/** Shown when closing Compose with unsaved content — offers Save-as-draft (continue later,
 * see [rs.tapizlabs.mail.data.repository.MailRepository.saveDraft]), Discard (deletes any
 * backing draft row), or Cancel (stay on Compose). Not in design-reference.html — that
 * mockup has no unsaved-changes flow — kept out of the main X/header layout so the reference
 * chrome stays exact, surfaced only as this sheet when there's actually something to lose. */
@Composable
private fun ComposeExitSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSaveDraft: () -> Unit,
    onDiscard: () -> Unit,
    strings: Strings,
) {
    val colors = AppColors

    MailSheet(visible = visible, onDismiss = onDismiss) {
        Text(
            text = strings.composeDiscardTitle,
            style = MaterialTheme.typography.titleMedium.copy(color = colors.textPrimary, fontWeight = FontWeight.Bold),
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = strings.composeDiscardMessage,
            style = MaterialTheme.typography.bodyMedium.copy(color = colors.textMuted),
        )

        Spacer(Modifier.height(20.dp))

        MailPrimaryButton(
            text = strings.composeSaveDraft,
            onClick = onSaveDraft,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(10.dp))

        MailGhostButton(
            text = strings.composeDiscard,
            onClick = onDiscard,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = strings.composeCancel,
            style = MaterialTheme.typography.bodyMedium.copy(color = colors.textMuted, fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onDismiss)
                .padding(vertical = 12.dp),
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun RecipientFields(
    to: String,
    cc: String,
    bcc: String,
    ccBccExpanded: Boolean,
    onToChange: (String) -> Unit,
    onCcChange: (String) -> Unit,
    onBccChange: (String) -> Unit,
    onToggleCcBcc: () -> Unit,
    strings: Strings,
) {
    val colors = AppColors

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f)) {
            LabeledField(
                label = strings.composeTo,
                value = to,
                onValueChange = onToChange,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                ),
            )
        }
        IconButton(
            onClick = onToggleCcBcc,
            modifier = Modifier
                .padding(end = 4.dp)
                .size(28.dp),
        ) {
            Icon(
                imageVector = if (ccBccExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (ccBccExpanded) "Hide Cc/Bcc" else "Show Cc/Bcc",
                tint = colors.textMuted,
            )
        }
    }

    if (ccBccExpanded) {
        LabeledField(label = strings.composeCc, value = cc, onValueChange = onCcChange)
        LabeledField(label = strings.composeBcc, value = bcc, onValueChange = onBccChange)
    }
}

/** Label-left / value-right row with a bottom divider, matching the reference's
 * From/To/Subject block — a bare [BasicTextField] rather than an outlined field, since
 * this bordered-block context needs a shared bottom rule, not a per-field outline. */
@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    showDivider: Boolean = true,
    enabled: Boolean = true,
) {
    val colors = AppColors
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(color = colors.textMuted),
            )
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                singleLine = true,
                keyboardOptions = keyboardOptions,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                ),
                cursorBrush = SolidColor(colors.primary),
            )
        }
        if (showDivider) {
            HorizontalDivider(color = colors.stroke)
        }
    }
}

@Composable
private fun AttachmentChip(name: String, onRemove: () -> Unit) {
    val colors = AppColors
    val shape = RoundedCornerShape(999.dp)

    Row(
        modifier = Modifier
            .clip(shape)
            .background(colors.cardSubtle)
            .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.InsertDriveFile,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium.copy(color = colors.textPrimary),
            maxLines = 1,
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Remove attachment",
                tint = colors.textMuted,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

private fun queryDisplayName(context: android.content.Context, uri: android.net.Uri): String? {
    val projection = arrayOf(android.provider.OpenableColumns.DISPLAY_NAME)
    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index >= 0) return cursor.getString(index)
        }
    }
    return null
}
