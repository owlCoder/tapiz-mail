package rs.tapizlabs.mail.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rs.tapizlabs.mail.data.local.entity.AccountEntity
import rs.tapizlabs.mail.data.local.entity.MessageEntity
import rs.tapizlabs.mail.data.repository.MailRepository
import rs.tapizlabs.mail.ui.model.AccountSummaryUi
import rs.tapizlabs.mail.ui.model.MessageListItemUi
import javax.inject.Inject

data class SearchFiltersUi(
    val accountId: String? = null,
    val hasAttachmentOnly: Boolean = false,
)

data class SearchUiState(
    val query: String = "",
    val accounts: List<AccountSummaryUi> = emptyList(),
    val filters: SearchFiltersUi = SearchFiltersUi(),
    val results: List<MessageListItemUi> = emptyList(),
    val isSearching: Boolean = false,
)

private const val SEARCH_DEBOUNCE_MS = 300L

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MailRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filters = MutableStateFlow(SearchFiltersUi())

    val uiState: StateFlow<SearchUiState> = combine(
        query,
        filters,
        repository.observeAccounts(),
        query.debounce(SEARCH_DEBOUNCE_MS).distinctUntilChanged()
            .flatMapLatest { q ->
                if (q.isBlank()) flowOf(emptyList()) else repository.searchMessages(q)
            },
    ) { currentQuery, currentFilters, accounts, results ->
        SearchUiState(
            query = currentQuery,
            accounts = accounts.map { it.toSummaryUi() },
            filters = currentFilters,
            results = results
                .filter { message ->
                    (currentFilters.accountId == null || message.accountId == currentFilters.accountId) &&
                        (!currentFilters.hasAttachmentOnly || message.hasAttachments)
                }
                .map { it.toListItemUi() },
            isSearching = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchUiState(),
    )

    fun updateQuery(value: String) {
        query.value = value
    }

    fun updateAccountFilter(accountId: String?) {
        filters.value = filters.value.copy(accountId = accountId)
    }

    fun toggleHasAttachmentFilter() {
        filters.value = filters.value.copy(hasAttachmentOnly = !filters.value.hasAttachmentOnly)
    }

    fun toggleStar(messageId: String, currentlyStarred: Boolean) {
        viewModelScope.launch {
            repository.setStarred(messageId, !currentlyStarred)
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
