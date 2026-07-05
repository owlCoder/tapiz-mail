package rs.tapizlabs.mail.ui.compose

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import rs.tapizlabs.mail.data.repository.MailRepository
import rs.tapizlabs.mail.data.repository.MailSyncGateway
import javax.inject.Inject

/** Mirrors the nav-arg convention read from [SavedStateHandle]: `mode` is one of
 * "new" / "reply" / "forward" (string, nav-graph-friendly), `messageId` is required for the
 * latter two. Kept as a sealed type internally once resolved from those raw args. */
sealed class ComposeMode {
    data object New : ComposeMode()
    data class Reply(val messageId: String) : ComposeMode()
    data class Forward(val messageId: String) : ComposeMode()
}

data class ComposeUiState(
    val accountId: String? = null,
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
)

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
            ComposeMode.New -> null
        } ?: return

        viewModelScope.launch {
            val source = repository.getMessageOnce(sourceMessageId) ?: return@launch
            _uiState.value = when (mode) {
                is ComposeMode.Reply -> _uiState.value.copy(
                    accountId = source.accountId,
                    to = source.fromAddress,
                    subject = prefixSubject("Re:", source.subject),
                    body = quoteBody(source.fromName, source.bodyPlain),
                )
                is ComposeMode.Forward -> _uiState.value.copy(
                    accountId = source.accountId,
                    subject = prefixSubject("Fwd:", source.subject),
                    body = quoteBody(source.fromName, source.bodyPlain),
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
                onSuccess = { _uiState.value = _uiState.value.copy(isSending = false, sent = true) },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        sendError = error.message ?: "Failed to send",
                    )
                },
            )
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
