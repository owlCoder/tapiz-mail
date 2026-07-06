package rs.tapizlabs.mail.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rs.tapizlabs.mail.data.local.entity.AccountEntity
import rs.tapizlabs.mail.data.local.entity.FolderType
import rs.tapizlabs.mail.data.local.entity.MessageEntity
import rs.tapizlabs.mail.data.repository.AccountRepository
import rs.tapizlabs.mail.data.repository.MailRepository
import rs.tapizlabs.mail.data.repository.MailSyncGateway
import rs.tapizlabs.mail.data.local.entity.SwipeAction
import rs.tapizlabs.mail.ui.model.AccountSummaryUi
import rs.tapizlabs.mail.ui.model.CategoryChipUi
import rs.tapizlabs.mail.ui.model.MessageListItemUi
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** "All accounts" is represented as a null selected account id throughout this ViewModel. */
data class InboxUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val accounts: List<AccountSummaryUi> = emptyList(),
    val selectedAccountId: String? = null,
    val categories: List<CategoryChipUi> = emptyList(),
    val selectedCategoryId: String? = null,
    val messages: List<MessageListItemUi> = emptyList(),
    val error: String? = null,
    /** Falls back to Delete-left/Mark-read-right when the account has no configured row yet
     * — matches [rs.tapizlabs.mail.ui.settings.MailSettingsScreen]'s own defaults so the
     * swipe behavior and the Settings display never silently disagree. */
    val swipeLeftAction: SwipeAction = SwipeAction.DELETE,
    val swipeRightAction: SwipeAction = SwipeAction.MARK_READ,
    /** Counts for the fixed Inbox/Sent/Drafts/Trash chips, shown as small badges — computed
     * independently of [messages] so they stay correct regardless of which pseudo-category
     * (or user category) is currently selected. */
    val inboxCount: Int = 0,
    val sentCount: Int = 0,
    val draftsCount: Int = 0,
    val trashCount: Int = 0,
    /** True while an older-messages page is being fetched (see [InboxViewModel.loadMore]) —
     * drives a small loading row at the bottom of the list, distinct from [isRefreshing]
     * (pull-to-refresh fetches newer mail, this fetches older). */
    val isLoadingMore: Boolean = false,
) {
    val isTrashSelected: Boolean get() = selectedCategoryId == PSEUDO_CATEGORY_TRASH
}

/** Sentinel ids for the fixed Inbox/Sent/Drafts/Trash chips prepended to the user's own
 * category chips — never collide with real [CategoryEntity] ids, which are UUIDs. Selecting
 * one of these re-routes [InboxViewModel]'s messages flow to [MailRepository.observeSent]/
 * [MailRepository.observeDrafts]/[MailRepository.observeTrash] instead of the normal
 * account/category query. */
const val PSEUDO_CATEGORY_SENT = "__sent__"
const val PSEUDO_CATEGORY_DRAFTS = "__drafts__"
const val PSEUDO_CATEGORY_TRASH = "__trash__"

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val repository: MailRepository,
    private val accountRepository: AccountRepository,
    private val syncGateway: MailSyncGateway,
) : ViewModel() {

    private val selectedAccountId = MutableStateFlow<String?>(null)
    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val isRefreshing = MutableStateFlow(false)
    private val isLoadingMore = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    /** Folder ids that have already returned an empty page from [loadMore] — the server has
     * nothing older left for them, so further scroll-triggered calls are skipped instead of
     * making a pointless IMAP round-trip every time the user reaches the bottom again. */
    private val exhaustedFolderIds = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<InboxUiState> = combine(
        repository.observeAccounts(),
        selectedAccountId,
        selectedCategoryId,
        isRefreshing,
        error,
    ) { accounts, accountId, categoryId, refreshing, err ->
        Quintuple(accounts, accountId, categoryId, refreshing, err)
    }.flatMapLatest { (accounts, accountId, categoryId, refreshing, err) ->
        val effectiveAccountId = accountId ?: accounts.firstOrNull()?.id
        val messagesFlow = when {
            categoryId == PSEUDO_CATEGORY_SENT ->
                effectiveAccountId?.let { repository.observeSent(it) } ?: flowOf(emptyList())
            categoryId == PSEUDO_CATEGORY_DRAFTS ->
                effectiveAccountId?.let { repository.observeDrafts(it) } ?: flowOf(emptyList())
            categoryId == PSEUDO_CATEGORY_TRASH ->
                effectiveAccountId?.let { repository.observeTrash(it) } ?: flowOf(emptyList())
            categoryId != null -> repository.observeMessagesForCategory(categoryId)
            effectiveAccountId != null -> repository.observeMessagesForAccount(effectiveAccountId)
            else -> flowOf(emptyList())
        }
        val categoriesFlow = if (effectiveAccountId != null) {
            repository.observeCategoriesForAccount(effectiveAccountId)
        } else {
            repository.observeAllCategories()
        }
        val swipeConfigFlow = if (effectiveAccountId != null) {
            accountRepository.observeSwipeConfig(effectiveAccountId)
        } else {
            flowOf(null)
        }
        // Always-on counts for the fixed chips' badges — independent of whichever
        // messagesFlow branch is currently selected above.
        val inboxCountFlow = if (effectiveAccountId != null) {
            repository.observeMessagesForAccount(effectiveAccountId).map { it.count { m -> m.isSynced } }
        } else {
            flowOf(0)
        }
        val sentCountFlow = if (effectiveAccountId != null) {
            repository.observeSent(effectiveAccountId).map { it.size }
        } else {
            flowOf(0)
        }
        val draftsCountFlow = if (effectiveAccountId != null) {
            repository.observeDrafts(effectiveAccountId).map { it.size }
        } else {
            flowOf(0)
        }
        val trashCountFlow = if (effectiveAccountId != null) {
            repository.observeTrash(effectiveAccountId).map { it.size }
        } else {
            flowOf(0)
        }
        val countsFlow = combine(inboxCountFlow, sentCountFlow, draftsCountFlow, trashCountFlow, ::Quadruple)

        combine(messagesFlow, categoriesFlow, swipeConfigFlow, countsFlow) { allMessages, categories, swipeConfig, counts ->
            // Local-only drafts (see MailRepository.saveDraft) live in the same `messages`
            // table as synced mail — exclude them here so they don't show up mixed into the
            // normal Inbox/category list; the Sent/Drafts/Trash pseudo-chips query them
            // separately above.
            val isPseudoSelection = categoryId == PSEUDO_CATEGORY_SENT ||
                categoryId == PSEUDO_CATEGORY_DRAFTS ||
                categoryId == PSEUDO_CATEGORY_TRASH
            val messages = if (isPseudoSelection) allMessages else allMessages.filter { it.isSynced }
            InboxUiState(
                isLoading = false,
                isRefreshing = refreshing,
                accounts = accounts.map { it.toSummaryUi() },
                selectedAccountId = effectiveAccountId,
                categories = categories.map { category ->
                    CategoryChipUi(
                        id = category.id,
                        name = category.name,
                        count = messages.count { it.categoryId == category.id },
                        colorIndex = category.colorIndex,
                    )
                },
                selectedCategoryId = categoryId,
                messages = messages.map { it.toListItemUi() },
                error = err,
                inboxCount = counts.a,
                sentCount = counts.b,
                draftsCount = counts.c,
                trashCount = counts.d,
                swipeLeftAction = swipeConfig?.swipeLeftAction ?: SwipeAction.DELETE,
                swipeRightAction = swipeConfig?.swipeRightAction ?: SwipeAction.MARK_READ,
            )
        }
    }.combine(isLoadingMore) { state, loadingMore -> state.copy(isLoadingMore = loadingMore) }
        .distinctUntilChanged().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = InboxUiState(),
        )

    fun selectAccount(accountId: String?) {
        selectedAccountId.value = accountId
        selectedCategoryId.value = null
    }

    fun selectCategory(categoryId: String?) {
        selectedCategoryId.value = categoryId
    }

    fun toggleStar(messageId: String, currentlyStarred: Boolean) {
        viewModelScope.launch {
            repository.setStarred(messageId, !currentlyStarred)
            // Best-effort IMAP `\Flagged` mirror — same fire-and-forget contract as markRead.
            syncGateway.setMessageStarredRemote(messageId, !currentlyStarred)
        }
    }

    fun markRead(messageId: String, isRead: Boolean) {
        viewModelScope.launch {
            repository.setRead(messageId, isRead)
            // Best-effort IMAP `\Seen` mirror so other clients/webmail agree with this app —
            // local Room state above is already the source of truth for the UI, so a failure
            // here (offline, server down) is intentionally swallowed rather than surfaced.
            syncGateway.setMessageSeenRemote(messageId, isRead)
        }
    }

    /** Permanently removes a message — only meant to be called from within the Trash
     * pseudo-category view; everywhere else, "delete" means [moveToTrash]. */
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            // Must run before the local Room delete below — it needs the still-existing row
            // (accountId/originFolderId/uid) to resolve the message back to its real IMAP
            // mailbox. Best-effort: a failure here must not block the local delete.
            syncGateway.deleteMessageRemote(messageId)
            repository.permanentlyDeleteMessage(messageId)
        }
    }

    /** Moves a message back to the account's Inbox — only meant to be called from within the
     * Trash pseudo-category view. */
    fun restoreMessage(messageId: String) {
        viewModelScope.launch {
            repository.restoreFromTrash(messageId)
        }
    }

    /** Permanently empties the effective account's Trash — only meant to be called from within
     * the Trash pseudo-category view, after the caller has already confirmed via
     * [rs.tapizlabs.mail.ui.components.MailConfirmDialog] since this is unrecoverable. Deletes
     * one message at a time through [deleteMessage] (remote IMAP delete + local Room delete)
     * rather than a bulk local-only delete, so "Empty trash" expunges every message
     * server-side too instead of silently leaving them on the IMAP server. */
    fun emptyTrash() {
        val accountId = uiState.value.selectedAccountId ?: return
        viewModelScope.launch {
            repository.observeTrash(accountId).first().forEach { message ->
                syncGateway.deleteMessageRemote(message.id)
                repository.permanentlyDeleteMessage(message.id)
            }
        }
    }

    /** Applies the account's configured swipe action; caller (screen) determines direction ->
     * this just executes whichever [SwipeAction] Settings has configured for that direction.
     * Delete moves the message into the local Trash rather than an immediate permanent
     * delete, so a swipe is always reversible (see [rs.tapizlabs.mail.ui.inbox.PSEUDO_CATEGORY_TRASH]). */
    fun applySwipeAction(messageId: String, action: SwipeAction) {
        viewModelScope.launch {
            when (action) {
                // Swipe-to-delete only moves the message into local Trash (still reversible),
                // so there's no IMAP-side mutation here — that only happens on the explicit
                // permanent delete in deleteMessage().
                SwipeAction.DELETE -> repository.moveToTrash(messageId)
                SwipeAction.MARK_READ -> {
                    repository.setRead(messageId, true)
                    syncGateway.setMessageSeenRemote(messageId, true)
                }
                SwipeAction.MARK_UNREAD -> {
                    repository.setRead(messageId, false)
                    syncGateway.setMessageSeenRemote(messageId, false)
                }
                SwipeAction.NONE -> Unit
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            error.value = null
            syncGateway.refresh(selectedAccountId.value)
                .onFailure { error.value = it.message ?: "Sync failed" }
            isRefreshing.value = false
        }
    }

    /** "Load more" older mail — called when the Inbox list scrolls near its bottom. Only
     * applies to the main Inbox view and the Sent pseudo-category (both real, paginable IMAP
     * folders); Drafts/Trash are local-only pseudo-folders with nothing further to page in
     * from the server, and user categories don't map to a single IMAP folder, so those are
     * silently no-ops here rather than special-cased by the caller. */
    fun loadMore() {
        val state = uiState.value
        val accountId = state.selectedAccountId ?: return
        if (isLoadingMore.value) return

        val folderType = when (state.selectedCategoryId) {
            null -> FolderType.INBOX
            PSEUDO_CATEGORY_SENT -> FolderType.SENT
            else -> return
        }

        viewModelScope.launch {
            val folderId = repository.getFolderIdByType(accountId, folderType) ?: return@launch
            if (folderId in exhaustedFolderIds.value) return@launch

            isLoadingMore.value = true
            val addedCount = syncGateway.loadOlderMessages(accountId, folderId)
            if (addedCount == 0) {
                exhaustedFolderIds.value = exhaustedFolderIds.value + folderId
            }
            isLoadingMore.value = false
        }
    }
}

private fun AccountEntity.toSummaryUi() = AccountSummaryUi(
    id = id,
    displayName = displayName,
    emailAddress = emailAddress,
)

private fun MessageEntity.toListItemUi() = MessageListItemUi(
    id = id,
    fromName = fromName,
    fromAddress = fromAddress,
    subject = subject,
    snippet = snippet,
    sentAt = sentAt,
    isRead = isRead,
    isStarred = isStarred,
    hasAttachments = hasAttachments,
    categoryColorIndex = null,
)

private data class Quadruple<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

private data class Quintuple<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

private operator fun <A, B, C, D, E> Quintuple<A, B, C, D, E>.component1() = a
private operator fun <A, B, C, D, E> Quintuple<A, B, C, D, E>.component2() = b
private operator fun <A, B, C, D, E> Quintuple<A, B, C, D, E>.component3() = c
private operator fun <A, B, C, D, E> Quintuple<A, B, C, D, E>.component4() = d
private operator fun <A, B, C, D, E> Quintuple<A, B, C, D, E>.component5() = e
