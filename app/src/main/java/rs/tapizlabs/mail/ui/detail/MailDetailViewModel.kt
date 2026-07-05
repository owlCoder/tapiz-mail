package rs.tapizlabs.mail.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rs.tapizlabs.mail.data.local.entity.AttachmentEntity
import rs.tapizlabs.mail.data.local.entity.MessageEntity
import rs.tapizlabs.mail.data.repository.MailRepository
import javax.inject.Inject

data class MailDetailUiState(
    val isLoading: Boolean = true,
    val messageId: String = "",
    val subject: String = "",
    val fromName: String = "",
    val fromAddress: String = "",
    val toAddresses: List<String> = emptyList(),
    val sentAt: Long = 0L,
    val bodyPlain: String = "",
    val bodyHtml: String? = null,
    val isStarred: Boolean = false,
    val attachments: List<AttachmentUi> = emptyList(),
    val notFound: Boolean = false,
)

data class AttachmentUi(
    val id: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val localUri: String?,
)

/** Nav-arg driven per Hilt+Nav convention used in `tapiz-boards`: the nav-graph agent passes
 * `messageId` as a route argument, read here via [SavedStateHandle]. */
@HiltViewModel
class MailDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MailRepository,
) : ViewModel() {

    private val messageId: String = checkNotNull(savedStateHandle["messageId"]) {
        "MailDetailViewModel requires a `messageId` nav argument"
    }

    val uiState: StateFlow<MailDetailUiState> = combine(
        repository.observeMessage(messageId),
        repository.observeAttachmentsForMessage(messageId),
    ) { message, attachments ->
        if (message == null) {
            MailDetailUiState(isLoading = false, messageId = messageId, notFound = true)
        } else {
            message.toUiState(attachments)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MailDetailUiState(messageId = messageId),
    )

    init {
        // Opening the detail screen marks the message read — mirrors standard mail-client
        // behavior; the sync layer's IMAP fetch already uses `mail.imap.peek` so this local
        // flip is the only place `\Seen` gets set from the read path.
        viewModelScope.launch {
            repository.setRead(messageId, true)
        }
    }

    fun toggleStar(currentlyStarred: Boolean) {
        viewModelScope.launch {
            repository.setStarred(messageId, !currentlyStarred)
        }
    }

    fun markUnread() {
        viewModelScope.launch {
            repository.setRead(messageId, false)
        }
    }

    /** [onDeleted] fires once the row is gone so the screen can navigate back — without this,
     * [uiState] would just flip to `notFound = true` post-delete (observeMessage emitting
     * null) and strand the user on a "message not found" screen instead of returning them
     * to the inbox. */
    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.moveToTrash(messageId)
            onDeleted()
        }
    }
}

private fun MessageEntity.toUiState(attachments: List<AttachmentEntity>) = MailDetailUiState(
    isLoading = false,
    messageId = id,
    subject = subject,
    fromName = fromName,
    fromAddress = fromAddress,
    toAddresses = toAddresses.split(",").map { it.trim() }.filter { it.isNotEmpty() },
    sentAt = sentAt,
    bodyPlain = bodyPlain,
    bodyHtml = bodyHtml.ifBlank { null },
    isStarred = isStarred,
    attachments = attachments.map {
        AttachmentUi(
            id = it.id,
            fileName = it.fileName,
            mimeType = it.mimeType,
            sizeBytes = it.sizeBytes,
            localUri = it.localUri,
        )
    },
)
