package rs.tapizlabs.mail.ui.compose

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import rs.tapizlabs.mail.data.local.entity.MessageEntity
import rs.tapizlabs.mail.data.repository.MailRepository
import rs.tapizlabs.mail.data.repository.MailSyncGateway
import java.util.UUID
import javax.inject.Inject

/** Mirrors the nav-arg convention read from [SavedStateHandle]: `mode` is one of
 * "new" / "reply" / "forward" / "draft" (string, nav-graph-friendly), `messageId` is
 * required for all but "new". Kept as a sealed type internally once resolved from those
 * raw args. */
sealed class ComposeMode {
    data object New : ComposeMode()
    data class Reply(val messageId: String) : ComposeMode()
    data class Forward(val messageId: String) : ComposeMode()
    /** Re-opens a previously saved local draft (see [rs.tapizlabs.mail.data.repository.MailRepository.saveDraft]) — edits update the same row instead of inserting a new one. */
    data class EditDraft(val messageId: String) : ComposeMode()
}

data class ComposeUiState(
    val accountId: String? = null,
    val fromEmail: String = "",
    /** Set once this compose session has a backing draft row (freshly saved or re-opened
     * via [ComposeMode.EditDraft]) — subsequent saves update this row instead of inserting. */
    val draftId: String? = null,
    val to: String = "",
    val cc: String = "",
    val bcc: String = "",
    val ccBccExpanded: Boolean = false,
    val subject: String = "",
    val body: String = "",
    val attachments: List<ComposeAttachmentUi> = emptyList(),
    val isSending: Boolean = false,
    val sendError: String? = null,
    val sent: Boolean = false,
) {
    /** Whether there's anything worth keeping as a draft — an empty New compose closed via
     * back/X shouldn't leave a blank row behind. */
    val hasContent: Boolean
        get() = to.isNotBlank() || cc.isNotBlank() || bcc.isNotBlank() ||
            subject.isNotBlank() || body.isNotBlank() || attachments.isNotEmpty()
}

data class ComposeAttachmentUi(val uri: String, val displayName: String)

@HiltViewModel
class ComposeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MailRepository,
    private val syncGateway: MailSyncGateway,
) : ViewModel() {

    private val mode: ComposeMode = when (savedStateHandle.get<String>("mode")) {
        "reply" -> ComposeMode.Reply(checkNotNull(savedStateHandle["messageId"]))
        "forward" -> ComposeMode.Forward(checkNotNull(savedStateHandle["messageId"]))
        "draft" -> ComposeMode.EditDraft(checkNotNull(savedStateHandle["messageId"]))
        else -> ComposeMode.New
    }

    private val _uiState = MutableStateFlow(ComposeUiState())
    val uiState: StateFlow<ComposeUiState> = _uiState.asStateFlow()

    init {
        prefillFromMode()
    }

    private fun prefillFromMode() {
        val sourceMessageId = when (val m = mode) {
            is ComposeMode.Reply -> m.messageId
            is ComposeMode.Forward -> m.messageId
            is ComposeMode.EditDraft -> m.messageId
            ComposeMode.New -> null
        }

        viewModelScope.launch {
            if (sourceMessageId == null) {
                val defaultAccount = repository.observeAccounts().first().firstOrNull()
                if (defaultAccount != null) {
                    _uiState.value = _uiState.value.copy(
                        accountId = defaultAccount.id,
                        fromEmail = defaultAccount.emailAddress,
                    )
                }
                return@launch
            }

            val source = repository.getMessageOnce(sourceMessageId) ?: return@launch
            val account = repository.observeAccounts().first().find { it.id == source.accountId }
            _uiState.value = when (mode) {
                is ComposeMode.Reply -> _uiState.value.copy(
                    accountId = source.accountId,
                    fromEmail = account?.emailAddress.orEmpty(),
                    to = source.fromAddress,
                    subject = prefixSubject("Re:", source.subject),
                    body = quoteBody(source.fromName, source.bodyPlain),
                )
                is ComposeMode.Forward -> _uiState.value.copy(
                    accountId = source.accountId,
                    fromEmail = account?.emailAddress.orEmpty(),
                    subject = prefixSubject("Fwd:", source.subject),
                    body = quoteBody(source.fromName, source.bodyPlain),
                )
                is ComposeMode.EditDraft -> _uiState.value.copy(
                    accountId = source.accountId,
                    fromEmail = account?.emailAddress.orEmpty(),
                    draftId = source.id,
                    to = source.toAddresses,
                    subject = source.subject,
                    body = source.bodyPlain,
                )
                ComposeMode.New -> _uiState.value
            }
        }
    }

    fun updateTo(value: String) = update { it.copy(to = value) }
    fun updateCc(value: String) = update { it.copy(cc = value) }
    fun updateBcc(value: String) = update { it.copy(bcc = value) }
    fun toggleCcBcc() = update { it.copy(ccBccExpanded = !it.ccBccExpanded) }
    fun updateSubject(value: String) = update { it.copy(subject = value) }
    fun updateBody(value: String) = update { it.copy(body = value) }

    fun addAttachments(uris: List<ComposeAttachmentUi>) = update {
        it.copy(attachments = it.attachments + uris)
    }

    fun removeAttachment(uri: String) = update {
        it.copy(attachments = it.attachments.filterNot { a -> a.uri == uri })
    }

    fun send() {
        val state = _uiState.value
        val accountId = state.accountId ?: return
        if (state.isSending) return

        viewModelScope.launch {
            _uiState.value = state.copy(isSending = true, sendError = null)
            val result = syncGateway.sendMessage(
                accountId = accountId,
                to = splitAddresses(state.to),
                cc = splitAddresses(state.cc),
                bcc = splitAddresses(state.bcc),
                subject = state.subject,
                bodyPlain = state.body,
                attachmentUris = state.attachments.map { it.uri },
                inReplyToMessageId = (mode as? ComposeMode.Reply)?.messageId,
            )
            result.fold(
                onSuccess = {
                    state.draftId?.let { repository.discardDraft(it) }
                    _uiState.value = _uiState.value.copy(isSending = false, sent = true)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        sendError = error.message ?: "Failed to send",
                    )
                },
            )
        }
    }

    /** Called when the user backs/closes out of Compose without sending — persists a
     * non-empty draft (updating the same row if one already exists for this session) so it
     * shows up in Drafts, or does nothing for an untouched New compose. [onDone] runs after
     * the save completes so the caller can navigate back only once it's safe to. */
    fun saveDraftAndExit(onDone: () -> Unit) {
        val state = _uiState.value
        val accountId = state.accountId
        if (!state.hasContent || accountId == null) {
            onDone()
            return
        }

        viewModelScope.launch {
            val draft = MessageEntity(
                id = state.draftId ?: UUID.randomUUID().toString(),
                accountId = accountId,
                folderId = "",
                uid = 0L,
                messageIdHeader = "",
                subject = state.subject,
                fromAddress = state.fromEmail,
                fromName = state.fromEmail,
                toAddresses = state.to,
                sentAt = System.currentTimeMillis(),
                snippet = state.body.take(140),
                bodyPlain = state.body,
                bodyHtml = "",
                isRead = true,
                isStarred = false,
                hasAttachments = state.attachments.isNotEmpty(),
                categoryId = null,
                isSynced = false,
            )
            repository.saveDraft(draft)
            onDone()
        }
    }

    /** Explicit "discard" — deletes the backing draft row (if any) instead of saving it,
     * then exits. For a draft that was never saved (a fresh New compose), this is a no-op
     * beyond calling [onDone]. */
    fun discardAndExit(onDone: () -> Unit) {
        val draftId = _uiState.value.draftId
        if (draftId == null) {
            onDone()
            return
        }
        viewModelScope.launch {
            repository.discardDraft(draftId)
            onDone()
        }
    }

    private fun update(transform: (ComposeUiState) -> ComposeUiState) {
        _uiState.value = transform(_uiState.value)
    }
}

private fun prefixSubject(prefix: String, subject: String): String =
    if (subject.startsWith(prefix, ignoreCase = true)) subject else "$prefix $subject"

private fun quoteBody(fromName: String, bodyPlain: String): String =
    "\n\n---- Original message from $fromName ----\n$bodyPlain"

private fun splitAddresses(raw: String): List<String> =
    raw.split(",", ";").map { it.trim() }.filter { it.isNotEmpty() }
