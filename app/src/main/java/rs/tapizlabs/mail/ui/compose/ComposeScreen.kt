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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rs.tapizlabs.mail.ui.components.MailGhostButton
import rs.tapizlabs.mail.ui.components.MailPickerSheet
import rs.tapizlabs.mail.ui.components.MailPrimaryButton
import rs.tapizlabs.mail.ui.components.MailSheet
import rs.tapizlabs.mail.ui.components.PickerSheetOption
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = AppColors
    val strings = LocalStrings.current
    val context = LocalContext.current
    var showExitSheet by rememberSaveable { mutableStateOf(false) }
    var showAccountPicker by rememberSaveable { mutableStateOf(false) }

    // As an effect, exactly once — calling onSent() straight from composition re-fired it on
    // every recomposition during the exit transition, popping the back stack more than once.
    val currentOnSent by rememberUpdatedState(onSent)
    LaunchedEffect(uiState.sent) {
        if (uiState.sent) currentOnSent()
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
            // Not every provider offers a persistable grant (it throws SecurityException
            // then); the temporary grant that came with the result is enough to send.
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            ComposeAttachmentUi(
                uri = uri.toString(),
                displayName = queryDisplayName(context, uri) ?: uri.lastPathSegment ?: "attachment",
            )
        }
        viewModel.addAttachments(picked)
    }

    // Camera capture straight into an attachment: the photo is written to a file in the
    // app's own cache (exposed through the existing FileProvider "attachments" path), so no
    // storage or camera permission is involved.
    var pendingPhotoUri by rememberSaveable { mutableStateOf<String?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingPhotoUri
        if (saved && uri != null) {
            viewModel.addAttachments(
                listOf(ComposeAttachmentUi(uri = uri, displayName = android.net.Uri.parse(uri).lastPathSegment ?: "photo.jpg")),
            )
        }
        pendingPhotoUri = null
    }
    val takePhoto = {
        runCatching {
            val dir = java.io.File(context.cacheDir, "attachments/camera").apply { mkdirs() }
            val file = java.io.File(dir, "IMG_${System.currentTimeMillis()}.jpg")
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            pendingPhotoUri = uri.toString()
            cameraLauncher.launch(uri)
        }
    }

    Scaffold(
        // imePadding keeps the attachment toolbar docked on top of the keyboard instead of
        // hidden behind it (edge-to-edge windows aren't resized by adjustResize).
        // Background first, so the status-bar strip is the same flat canvas as the rest of
        // the screen instead of the root gradient showing through above the header.
        modifier = modifier.background(colors.canvasTop).statusBarsPadding().imePadding(),
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
                            .size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = strings.composeCancel,
                            tint = colors.textMuted,
                        )
                    }
                    Text(
                        text = when {
                            uiState.isReply -> strings.composeReplyTitle
                            uiState.isForward -> strings.composeForwardTitle
                            else -> strings.composeNewMessage
                        },
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
                                imageVector = Icons.AutoMirrored.Outlined.Send,
                                contentDescription = strings.composeSend,
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
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    IconButton(onClick = { attachmentPicker.launch(arrayOf("*/*")) }) {
                        Icon(
                            imageVector = Icons.Outlined.AttachFile,
                            contentDescription = strings.composeAddAttachment,
                            tint = colors.textMuted,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                    IconButton(onClick = { takePhoto() }) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoCamera,
                            contentDescription = strings.composeTakePhoto,
                            tint = colors.textMuted,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                    IconButton(onClick = { attachmentPicker.launch(arrayOf("image/*")) }) {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = strings.composeAddImage,
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
                // Tappable only where switching makes sense: more than one account, and not
                // a reply/forward (those stay with the original message's account).
                val canSwitchAccount = uiState.accounts.size > 1 && !uiState.isReply && !uiState.isForward
                LabeledField(
                    label = strings.composeFrom,
                    value = uiState.fromEmail,
                    onValueChange = {},
                    enabled = false,
                    onClick = if (canSwitchAccount) ({ showAccountPicker = true }) else null,
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
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    showDivider = false,
                )
            }

            // Directly under the fields it refers to — below the body it was off-screen
            // behind the keyboard at exactly the moment it mattered.
            if (uiState.sendError != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = uiState.sendError.orEmpty(),
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.coral),
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
                            removeLabel = strings.composeRemoveAttachment,
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
                    // Same min height as the box, so tapping anywhere in the empty body
                    // area focuses it — not just its single first line.
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary, lineHeight = 22.sp),
                    cursorBrush = SolidColor(colors.primary),
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    MailPickerSheet(
        visible = showAccountPicker,
        title = strings.composeFrom,
        options = uiState.accounts.map { PickerSheetOption(it.id, it.emailAddress) },
        selected = uiState.accountId.orEmpty(),
        onSelect = viewModel::selectAccount,
        onDismiss = { showAccountPicker = false },
    )

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
            icon = Icons.Outlined.Save,
            onClick = onSaveDraft,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(10.dp))

        MailGhostButton(
            text = strings.composeDiscard,
            icon = Icons.Outlined.DeleteOutline,
            onClick = onDiscard,
            danger = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(10.dp))

        MailGhostButton(
            text = strings.composeCancel,
            icon = Icons.Outlined.Close,
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
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

    LabeledField(
        label = strings.composeTo,
        value = to,
        onValueChange = onToChange,
        keyboardOptions = EMAIL_KEYBOARD,
        trailing = {
            IconButton(onClick = onToggleCcBcc, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = if (ccBccExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = "${strings.composeCc}/${strings.composeBcc}",
                    tint = colors.textMuted,
                )
            }
        },
    )

    if (ccBccExpanded) {
        LabeledField(label = strings.composeCc, value = cc, onValueChange = onCcChange, keyboardOptions = EMAIL_KEYBOARD)
        LabeledField(label = strings.composeBcc, value = bcc, onValueChange = onBccChange, keyboardOptions = EMAIL_KEYBOARD)
    }
}

private val EMAIL_KEYBOARD = KeyboardOptions(
    capitalization = KeyboardCapitalization.None,
    autoCorrectEnabled = false,
    keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
)

/** Label + value row with a full-width bottom divider, matching the reference's
 * From/To/Subject block — a bare [BasicTextField] rather than an outlined field, since this
 * bordered-block context needs a shared bottom rule, not a per-field outline. Values are
 * start-aligned in a common column: with end-aligned text, tapping the (empty) left part of
 * a field drops the cursor at the START of what's already typed, so the next keystrokes land
 * in front of it. [trailing] sits inside the row so the divider still spans the full width. */
@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    showDivider: Boolean = true,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = AppColors
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(color = colors.textMuted),
                modifier = Modifier.widthIn(min = 64.dp),
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
                ),
                cursorBrush = SolidColor(colors.primary),
            )
            if (onClick != null) {
                Icon(
                    imageVector = Icons.Outlined.UnfoldMore,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.padding(start = 4.dp).size(18.dp),
                )
            }
            trailing?.invoke()
        }
        if (showDivider) {
            HorizontalDivider(color = colors.stroke)
        }
    }
}

@Composable
private fun AttachmentChip(name: String, removeLabel: String, onRemove: () -> Unit) {
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
                contentDescription = removeLabel,
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
