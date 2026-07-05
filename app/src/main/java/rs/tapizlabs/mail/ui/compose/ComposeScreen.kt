package rs.tapizlabs.mail.ui.compose

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import rs.tapizlabs.mail.ui.components.MailPrimaryButton
import rs.tapizlabs.mail.ui.theme.AppColors

/**
 * Compose screen — handles New/Reply/Forward, driven by nav args read inside
 * [ComposeViewModel] (`mode` + `messageId` via `SavedStateHandle`, see that file). This
 * composable itself takes no mode params; the nav-graph agent passes them as route arguments.
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
    val context = LocalContext.current

    if (uiState.sent) {
        onSent()
        return
    }

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
        modifier = modifier,
        containerColor = colors.canvasTop,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Close",
                        tint = colors.textPrimary,
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { attachmentPicker.launch(arrayOf("*/*")) }) {
                    Icon(
                        imageVector = Icons.Outlined.AttachFile,
                        contentDescription = "Add attachment",
                        tint = colors.textMuted,
                    )
                }
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                MailPrimaryButton(
                    text = "Send",
                    icon = Icons.AutoMirrored.Outlined.Send,
                    onClick = viewModel::send,
                    enabled = !uiState.isSending && uiState.to.isNotBlank(),
                    loading = uiState.isSending,
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            RecipientFields(
                to = uiState.to,
                cc = uiState.cc,
                bcc = uiState.bcc,
                ccBccExpanded = uiState.ccBccExpanded,
                onToChange = viewModel::updateTo,
                onCcChange = viewModel::updateCc,
                onBccChange = viewModel::updateBcc,
                onToggleCcBcc = viewModel::toggleCcBcc,
            )

            HorizontalDivider(color = colors.stroke.copy(alpha = 0.5f))

            OutlinedTextField(
                value = uiState.subject,
                onValueChange = viewModel::updateSubject,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                placeholder = { Text("Subject") },
                singleLine = true,
                colors = plainFieldColors(),
            )

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

            OutlinedTextField(
                value = uiState.body,
                onValueChange = viewModel::updateBody,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp),
                placeholder = { Text("Write your message…") },
                colors = plainFieldColors(),
            )

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
) {
    val colors = AppColors

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = to,
            onValueChange = onToChange,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 52.dp),
            placeholder = { Text("To") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
            ),
            colors = plainFieldColors(),
        )
        IconButton(onClick = onToggleCcBcc) {
            Icon(
                imageVector = if (ccBccExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (ccBccExpanded) "Hide Cc/Bcc" else "Show Cc/Bcc",
                tint = colors.textMuted,
            )
        }
    }

    if (ccBccExpanded) {
        OutlinedTextField(
            value = cc,
            onValueChange = onCcChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            placeholder = { Text("Cc") },
            singleLine = true,
            colors = plainFieldColors(),
        )
        OutlinedTextField(
            value = bcc,
            onValueChange = onBccChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            placeholder = { Text("Bcc") },
            singleLine = true,
            colors = plainFieldColors(),
        )
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

@Composable
private fun plainFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AppColors.primary,
    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
    focusedContainerColor = AppColors.cardSubtle,
    unfocusedContainerColor = AppColors.cardSubtle,
    cursorColor = AppColors.primary,
)

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
