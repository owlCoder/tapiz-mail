package rs.tapizlabs.mail.ui.compose

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.tapizlabs.mail.data.local.entity.MessageEntity
import rs.tapizlabs.mail.data.repository.MailRepository
import rs.tapizlabs.mail.data.repository.MailSyncGateway
import rs.tapizlabs.mail.data.repository.OutgoingAttachmentRef
import rs.tapizlabs.mail.mail.MailError
import rs.tapizlabs.mail.ui.i18n.CurrentStrings
import rs.tapizlabs.mail.ui.model.AccountSummaryUi
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
    /** Every configured account, for the From picker — a new message used to be sendable
     * only from whichever account happened to be first. */
    val accounts: List<AccountSummaryUi> = emptyList(),
    val isReply: Boolean = false,
    val isForward: Boolean = false,
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
            val accounts = repository.observeAccounts().first()
            val summaries = accounts.map { AccountSummaryUi(it.id, it.displayName, it.emailAddress) }
            if (sourceMessageId == null) {
                val defaultAccount = accounts.firstOrNull()
                _uiState.update {
                    it.copy(
                        accounts = summaries,
                        accountId = defaultAccount?.id,
                        fromEmail = defaultAccount?.emailAddress.orEmpty(),
                    )
                }
                return@launch
            }

            val source = repository.getMessageOnce(sourceMessageId)
            if (source == null) {
                _uiState.update { it.copy(accounts = summaries) }
                return@launch
            }
            val account = accounts.find { it.id == source.accountId }
            val strings = CurrentStrings.value
            _uiState.update { state ->
                val base = state.copy(
                    accounts = summaries,
                    accountId = source.accountId,
                    fromEmail = account?.emailAddress.orEmpty(),
                )
                when (mode) {
                    is ComposeMode.Reply -> base.copy(
                        isReply = true,
                        to = source.fromAddress,
                        subject = prefixSubject("Re:", source.subject),
                        body = quoteBody(strings.composeOriginalMessage(source.senderLabel()), source.bodyPlain),
                    )
                    is ComposeMode.Forward -> base.copy(
                        isForward = true,
                        subject = prefixSubject("Fwd:", source.subject),
                        body = quoteBody(strings.composeOriginalMessage(source.senderLabel()), source.bodyPlain),
                    )
                    is ComposeMode.EditDraft -> base.copy(
                        draftId = source.id,
                        to = source.toAddresses,
                        subject = source.subject,
                        body = source.bodyPlain,
                    )
                    ComposeMode.New -> base
                }
            }
        }
    }

    /** From-account switch — only offered for new mail/drafts; a reply or forward stays tied
     * to the account the original message belongs to. */
    fun selectAccount(accountId: String) = update { state ->
        val account = state.accounts.find { it.id == accountId } ?: return@update state
        state.copy(accountId = account.id, fromEmail = account.emailAddress)
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

        val strings = CurrentStrings.value
        val to = splitAddresses(state.to)
        val cc = splitAddresses(state.cc)
        val bcc = splitAddresses(state.bcc)
        // Caught here, before any network I/O, so a typo gets a precise message instead of
        // whatever the SMTP server makes of it.
        val invalid = (to + cc + bcc).firstOrNull { !EMAIL_PATTERN.matches(it) }
        if (invalid != null) {
            update { it.copy(sendError = strings.composeInvalidRecipient(invalid)) }
            return
        }
        if (to.isEmpty()) return

        viewModelScope.launch {
            update { it.copy(isSending = true, sendError = null) }
            val result = syncGateway.sendMessage(
                accountId = accountId,
                to = to,
                cc = cc,
                bcc = bcc,
                subject = state.subject,
                bodyPlain = state.body,
                attachments = state.attachments.map { OutgoingAttachmentRef(uri = it.uri, displayName = it.displayName) },
                inReplyToMessageId = (mode as? ComposeMode.Reply)?.messageId,
            )
            result.fold(
                onSuccess = {
                    // The Sent-folder append and the follow-up sync are the gateway's job
                    // (on the application scope) — this screen is about to close.
                    state.draftId?.let { repository.discardDraft(it) }
                    update { it.copy(isSending = false, sent = true) }
                },
                onFailure = { error ->
                    val message = when (error) {
                        is MailError.InvalidAddress -> strings.composeInvalidRecipient(error.address)
                        is MailError.AuthenticationFailed -> error.message ?: strings.composeSendFailed
                        else -> strings.composeSendFailed
                    }
                    update { it.copy(isSending = false, sendError = message) }
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
        _uiState.update(transform)
    }
}

/** Deliberately loose — just "something@something.tld" — the server is the real validator;
 * this only catches obvious slips (missing @, stray spaces) before a send is attempted. */
private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

private fun MessageEntity.senderLabel(): String = fromName.ifBlank { fromAddress }

private fun prefixSubject(prefix: String, subject: String): String =
    if (subject.startsWith(prefix, ignoreCase = true)) subject else "$prefix $subject"

/** Quotes the original with `> ` prefixes under a one-line attribution — the form the
 * detail screen's plain-text renderer (and every other mail client) displays as a quote. */
private fun quoteBody(attribution: String, bodyPlain: String): String =
    "\n\n$attribution\n" + bodyPlain.trimEnd().lines().joinToString("\n") { "> $it" }

private fun splitAddresses(raw: String): List<String> =
    raw.split(",", ";").map { it.trim() }.filter { it.isNotEmpty() }
