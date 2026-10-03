package rs.tapizlabs.mail.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.tapizlabs.mail.data.local.entity.AccountEntity
import rs.tapizlabs.mail.data.local.entity.FolderType
import rs.tapizlabs.mail.data.local.entity.SwipeAction
import rs.tapizlabs.mail.data.repository.AccountRepository
import rs.tapizlabs.mail.data.repository.MailRepository
import rs.tapizlabs.mail.data.repository.MailSyncGateway
import rs.tapizlabs.mail.ui.model.AccountSummaryUi
import rs.tapizlabs.mail.ui.model.CategoryChipUi
import rs.tapizlabs.mail.ui.model.MessageListItemUi
import rs.tapizlabs.mail.ui.model.toListItemUi
import javax.inject.Inject

/** "All accounts" is represented as a null selected account id throughout this ViewModel. */
data class InboxUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val accounts: List<AccountSummaryUi> = emptyList(),
    val selectedAccountId: String? = null,
    val categories: List<CategoryChipUi> = emptyList(),
    val selectedCategoryId: String? = null,
    val messages: List<MessageListItemUi> = emptyList(),
    /** True when the last refresh failed (offline, server unreachable) — the screen shows a
     * dismissible banner with a retry action; the cached list stays usable underneath. */
    val syncFailed: Boolean = false,
    /** Falls back to Delete-left/Mark-read-right when the account has no configured row yet
     * — matches [rs.tapizlabs.mail.ui.settings.MailSettingsScreen]'s own defaults so the
     * swipe behavior and the Settings display never silently disagree. */
    val swipeLeftAction: SwipeAction = SwipeAction.DELETE,
    val swipeRightAction: SwipeAction = SwipeAction.MARK_READ,
    /** Badges for the fixed Inbox/Drafts/Trash chips — computed in SQL independently of
     * [messages] so they stay correct regardless of which chip is currently selected. */
    val inboxUnreadCount: Int = 0,
    val draftsCount: Int = 0,
    val trashCount: Int = 0,
    /** True while an older-messages page is being fetched (see [InboxViewModel.loadMore]) —
     * drives a small loading row at the bottom of the list, distinct from [isRefreshing]
     * (pull-to-refresh fetches newer mail, this fetches older). */
    val isLoadingMore: Boolean = false,
) {
    val isTrashSelected: Boolean get() = selectedCategoryId == PSEUDO_CATEGORY_TRASH
    val isDraftsSelected: Boolean get() = selectedCategoryId == PSEUDO_CATEGORY_DRAFTS
    val isSentSelected: Boolean get() = selectedCategoryId == PSEUDO_CATEGORY_SENT
}

/** Sentinel ids for the fixed Inbox/Sent/Drafts/Trash chips prepended to the user's own
 * category chips — never collide with real [rs.tapizlabs.mail.data.local.entity.CategoryEntity]
 * ids, which are UUIDs. Selecting one of these re-routes [InboxViewModel]'s messages flow to
 * [MailRepository.observeSent]/[MailRepository.observeDrafts]/[MailRepository.observeTrash]
 * instead of the normal account/category query. */
const val PSEUDO_CATEGORY_SENT = "__sent__"
const val PSEUDO_CATEGORY_DRAFTS = "__drafts__"
const val PSEUDO_CATEGORY_TRASH = "__trash__"

/** Refresh / load-more / error flags — everything in [InboxUiState] that isn't derived from
 * Room. Kept in one flow, combined in AFTER the Room queries, so flipping a flag (e.g. a
 * pull-to-refresh starting) never tears down and re-subscribes those queries. */
private data class TransientState(
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val syncFailed: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InboxViewModel @Inject constructor(
    private val repository: MailRepository,
    private val accountRepository: AccountRepository,
    private val syncGateway: MailSyncGateway,
) : ViewModel() {

    private val selectedAccountId = MutableStateFlow<String?>(null)
    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val transient = MutableStateFlow(TransientState())

    /** Folder ids that have already returned an empty page from [loadMore] — the server has
     * nothing older left for them, so further scroll-triggered calls are skipped instead of
     * making a pointless IMAP round-trip every time the user reaches the bottom again. */
    private val exhaustedFolderIds = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<InboxUiState> = combine(
        repository.observeAccounts(),
        selectedAccountId,
        selectedCategoryId,
        ::Triple,
    ).flatMapLatest { (accounts, requestedAccountId, categoryId) ->
        // A selected account that has since been removed falls back to "all accounts".
        val accountId = requestedAccountId?.takeIf { id -> accounts.any { it.id == id } }
        val isOutgoingView = categoryId == PSEUDO_CATEGORY_SENT || categoryId == PSEUDO_CATEGORY_DRAFTS
        val messagesFlow = when (categoryId) {
            PSEUDO_CATEGORY_SENT -> repository.observeSent(accountId)
            PSEUDO_CATEGORY_DRAFTS -> repository.observeDrafts(accountId)
            PSEUDO_CATEGORY_TRASH -> repository.observeTrash(accountId)
            else -> repository.observeInbox(accountId, categoryId)
        }
        val categoriesFlow = if (accountId != null) {
            repository.observeCategoriesForAccount(accountId)
        } else {
            repository.observeAllCategories()
        }
        // Swipe actions are configured per account; with exactly one account "all accounts"
        // means that account, otherwise the unified view uses the defaults.
        val swipeConfigFlow = (accountId ?: accounts.singleOrNull()?.id)
            ?.let { accountRepository.observeSwipeConfig(it) }
            ?: flowOf(null)

        combine(
            messagesFlow,
            categoriesFlow,
            swipeConfigFlow,
            repository.observeMailboxCounts(accountId),
            repository.observeCategoryUnreadCounts(accountId),
        ) { messages, categories, swipeConfig, counts, categoryUnread ->
            InboxUiState(
                isLoading = false,
                accounts = accounts.map { it.toSummaryUi() },
                selectedAccountId = accountId,
                categories = categories.map { category ->
                    CategoryChipUi(
                        id = category.id,
                        name = category.name,
                        count = categoryUnread[category.id] ?: 0,
                        colorIndex = category.colorIndex,
                    )
                },
                selectedCategoryId = categoryId,
                messages = messages.map { it.toListItemUi(isOutgoing = isOutgoingView) },
                inboxUnreadCount = counts.inboxUnread,
                draftsCount = counts.drafts,
                trashCount = counts.trash,
                swipeLeftAction = swipeConfig?.swipeLeftAction ?: SwipeAction.DELETE,
                swipeRightAction = swipeConfig?.swipeRightAction ?: SwipeAction.MARK_READ,
            )
        }
    }.combine(transient) { state, flags ->
        state.copy(
            isRefreshing = flags.isRefreshing,
            isLoadingMore = flags.isLoadingMore,
            syncFailed = flags.syncFailed,
        )
    }
        // Row mapping for a few hundred messages is cheap but not free — keep it off the
        // main thread, since it re-runs on every Room invalidation during a sync.
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = InboxUiState(),
        )

    init {
        // Opening the app should show current mail, not whatever the last background sync
        // left behind up to a full sync interval ago.
        refresh(showIndicator = false)
    }

    fun selectAccount(accountId: String?) {
        selectedAccountId.value = accountId
        selectedCategoryId.value = null
    }

    fun selectCategory(categoryId: String?) {
        selectedCategoryId.value = categoryId
    }

    fun toggleStar(messageId: String, currentlyStarred: Boolean) {
        viewModelScope.launch { syncGateway.setStarred(messageId, !currentlyStarred) }
    }

    /** Permanently removes a message — only meant to be called from within the Trash
     * pseudo-category view; everywhere else, "delete" means [MailRepository.moveToTrash]. */
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            // Must run before the local Room delete below — it needs the still-existing row
            // (accountId/originFolderId/uid) to resolve the message back to its real IMAP
            // mailbox. Best-effort: a failure here must not block the local delete.
            syncGateway.deleteMessageRemote(messageId)
            repository.permanentlyDeleteMessage(messageId)
        }
    }

    /** Moves a message back to the folder it was deleted from — only meant to be called from
     * within the Trash pseudo-category view. */
    fun restoreMessage(messageId: String) {
        viewModelScope.launch { repository.restoreFromTrash(messageId) }
    }

    /** Permanently empties the selected account's (or every account's) Trash — only meant to
     * be called from within the Trash pseudo-category view, after the caller has already
     * confirmed via [rs.tapizlabs.mail.ui.components.MailConfirmDialog] since this is
     * unrecoverable. Deletes the whole batch server-side through one grouped IMAP
     * connection-per-folder (see [MailSyncGateway.deleteMessagesRemote]) rather than one
     * connection per message, then removes the local Room rows. */
    fun emptyTrash() {
        val accountId = uiState.value.selectedAccountId
        viewModelScope.launch {
            val messageIds = repository.getTrashMessageIds(accountId)
            if (messageIds.isEmpty()) return@launch
            syncGateway.deleteMessagesRemote(messageIds)
            repository.permanentlyDeleteMessages(messageIds)
        }
    }

    /** Applies the account's configured swipe action; caller (screen) determines direction ->
     * this just executes whichever [SwipeAction] Settings has configured for that direction.
     * Delete moves the message into the local Trash rather than an immediate permanent
     * delete, so a swipe is always reversible (see [PSEUDO_CATEGORY_TRASH]). */
    fun applySwipeAction(messageId: String, action: SwipeAction) {
        viewModelScope.launch {
            when (action) {
                // Swipe-to-delete only moves the message into local Trash (still reversible),
                // so there's no IMAP-side mutation here — that only happens on the explicit
                // permanent delete in deleteMessage().
                SwipeAction.DELETE -> repository.moveToTrash(messageId)
                SwipeAction.MARK_READ -> syncGateway.setRead(messageId, true)
                SwipeAction.MARK_UNREAD -> syncGateway.setRead(messageId, false)
                SwipeAction.NONE -> Unit
            }
        }
    }

    /** Fetches new mail for the selected account (or all of them). [showIndicator] is false
     * for the automatic refresh on screen open, which shouldn't flash the pull-to-refresh
     * spinner over a list the user didn't ask to reload. */
    fun refresh(showIndicator: Boolean = true) {
        if (transient.value.isRefreshing) return
        viewModelScope.launch {
            transient.update { it.copy(isRefreshing = showIndicator, syncFailed = false) }
            val result = syncGateway.refresh(selectedAccountId.value)
            // A fresh sync may have brought in a new, older tail for a folder (or a first
            // sync its first messages) — let "load more" try those folders again.
            exhaustedFolderIds.value = emptySet()
            // The automatic refresh fails silently — an error banner the moment the app opens
            // offline would be noise; the user only gets one for a refresh they asked for.
            transient.update { it.copy(isRefreshing = false, syncFailed = result.isFailure && showIndicator) }
        }
    }

    fun dismissSyncError() {
        transient.update { it.copy(syncFailed = false) }
    }

    /** "Load more" older mail — called when the Inbox list scrolls near its bottom. Only
     * applies to the main Inbox view and the Sent pseudo-category (both real, paginable IMAP
     * folders); Drafts/Trash are local-only pseudo-folders with nothing further to page in
     * from the server, and user categories don't map to a single IMAP folder, so those are
     * silently no-ops here rather than special-cased by the caller. */
    fun loadMore() {
        val state = uiState.value
        if (transient.value.isLoadingMore) return

        val folderType = when (state.selectedCategoryId) {
            null -> FolderType.INBOX
            PSEUDO_CATEGORY_SENT -> FolderType.SENT
            else -> return
        }
        val accountIds = state.selectedAccountId?.let(::listOf) ?: state.accounts.map { it.id }
        if (accountIds.isEmpty()) return

        viewModelScope.launch {
            val targets = accountIds
                .mapNotNull { accountId -> repository.getFolderIdByType(accountId, folderType)?.let { accountId to it } }
                .filterNot { (_, folderId) -> folderId in exhaustedFolderIds.value }
            if (targets.isEmpty()) return@launch

            transient.update { it.copy(isLoadingMore = true) }
            for ((accountId, folderId) in targets) {
                // Only a successful empty page means "nothing older"; after a failed attempt
                // (offline) the folder stays eligible, but is parked until the next refresh
                // so reaching the bottom again doesn't retry in a tight loop.
                val added = syncGateway.loadOlderMessages(accountId, folderId).getOrDefault(0)
                if (added == 0) exhaustedFolderIds.update { it + folderId }
            }
            transient.update { it.copy(isLoadingMore = false) }
        }
    }
}

private fun AccountEntity.toSummaryUi() = AccountSummaryUi(
    id = id,
    displayName = displayName,
    emailAddress = emailAddress,
)
