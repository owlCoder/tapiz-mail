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
import rs.tapizlabs.mail.data.local.entity.MessageEntity
import rs.tapizlabs.mail.data.repository.AccountRepository
import rs.tapizlabs.mail.data.repository.MailRepository
import rs.tapizlabs.mail.data.repository.MailSyncGateway
import rs.tapizlabs.mail.data.local.entity.SwipeAction
import rs.tapizlabs.mail.ui.model.AccountSummaryUi
import rs.tapizlabs.mail.ui.model.CategoryChipUi
import rs.tapizlabs.mail.ui.model.MessageListItemUi
import javax.inject.Inject
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
    /** Counts for the fixed Inbox/Drafts/Trash chips, shown as small badges — computed
     * independently of [messages] so they stay correct regardless of which pseudo-category
     * (or user category) is currently selected. */
    val inboxCount: Int = 0,
    val draftsCount: Int = 0,
    val trashCount: Int = 0,
) {
    val isTrashSelected: Boolean get() = selectedCategoryId == PSEUDO_CATEGORY_TRASH
}

/** Sentinel ids for the fixed Inbox/Drafts/Trash chips prepended to the user's own category
 * chips — never collide with real [CategoryEntity] ids, which are UUIDs. Selecting one of
 * these re-routes [InboxViewModel]'s messages flow to [MailRepository.observeDrafts]/
 * [MailRepository.observeTrash] instead of the normal account/category query. */
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
    private val error = MutableStateFlow<String?>(null)

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
        val countsFlow = combine(inboxCountFlow, draftsCountFlow, trashCountFlow, ::Triple)

        combine(messagesFlow, categoriesFlow, swipeConfigFlow, countsFlow) { allMessages, categories, swipeConfig, counts ->
            // Local-only drafts (see MailRepository.saveDraft) live in the same `messages`
            // table as synced mail — exclude them here so they don't show up mixed into the
            // normal Inbox/category list; the Drafts pseudo-chip queries them separately above.
            val isPseudoSelection = categoryId == PSEUDO_CATEGORY_DRAFTS || categoryId == PSEUDO_CATEGORY_TRASH
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
                inboxCount = counts.first,
                draftsCount = counts.second,
                trashCount = counts.third,
                swipeLeftAction = swipeConfig?.swipeLeftAction ?: SwipeAction.DELETE,
                swipeRightAction = swipeConfig?.swipeRightAction ?: SwipeAction.MARK_READ,
            )
        }
    }.distinctUntilChanged().stateIn(
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
        }
    }

    fun markRead(messageId: String, isRead: Boolean) {
        viewModelScope.launch {
            repository.setRead(messageId, isRead)
        }
    }

    /** Permanently removes a message — only meant to be called from within the Trash
     * pseudo-category view; everywhere else, "delete" means [moveToTrash]. */
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
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

    /** Applies the account's configured swipe action; caller (screen) determines direction ->
     * this just executes whichever [SwipeAction] Settings has configured for that direction.
     * Delete moves the message into the local Trash rather than an immediate permanent
     * delete, so a swipe is always reversible (see [rs.tapizlabs.mail.ui.inbox.PSEUDO_CATEGORY_TRASH]). */
    fun applySwipeAction(messageId: String, action: SwipeAction) {
        viewModelScope.launch {
            when (action) {
                SwipeAction.DELETE -> repository.moveToTrash(messageId)
                SwipeAction.MARK_READ -> repository.setRead(messageId, true)
                SwipeAction.MARK_UNREAD -> repository.setRead(messageId, false)
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

private data class Quintuple<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

private operator fun <A, B, C, D, E> Quintuple<A, B, C, D, E>.component1() = a
private operator fun <A, B, C, D, E> Quintuple<A, B, C, D, E>.component2() = b
private operator fun <A, B, C, D, E> Quintuple<A, B, C, D, E>.component3() = c
private operator fun <A, B, C, D, E> Quintuple<A, B, C, D, E>.component4() = d
private operator fun <A, B, C, D, E> Quintuple<A, B, C, D, E>.component5() = e
