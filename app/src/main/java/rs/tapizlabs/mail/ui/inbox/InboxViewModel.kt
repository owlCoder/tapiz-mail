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
import rs.tapizlabs.mail.data.repository.MailRepository
import rs.tapizlabs.mail.data.repository.MailSyncGateway
import rs.tapizlabs.mail.data.local.entity.SwipeAction
import rs.tapizlabs.mail.ui.model.AccountSummaryUi
import rs.tapizlabs.mail.ui.model.CategoryChipUi
import rs.tapizlabs.mail.ui.model.MessageListItemUi
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
    val error: String? = null,
)

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val repository: MailRepository,
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
            categoryId != null -> repository.observeMessagesForCategory(categoryId)
            effectiveAccountId != null -> repository.observeMessagesForAccount(effectiveAccountId)
            else -> kotlinx.coroutines.flow.flowOf(emptyList())
        }
        val categoriesFlow = if (effectiveAccountId != null) {
            repository.observeCategoriesForAccount(effectiveAccountId)
        } else {
            repository.observeAllCategories()
        }

        combine(messagesFlow, categoriesFlow) { messages, categories ->
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

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }

    /** Applies the account's configured swipe action; caller (screen) determines direction ->
     * this just executes whichever [SwipeAction] Settings has configured for that direction. */
    fun applySwipeAction(messageId: String, action: SwipeAction) {
        viewModelScope.launch {
            when (action) {
                SwipeAction.ARCHIVE, SwipeAction.DELETE -> repository.deleteMessage(messageId)
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
