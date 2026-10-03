package rs.tapizlabs.mail.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rs.tapizlabs.mail.data.local.entity.AccountEntity
import rs.tapizlabs.mail.data.repository.MailRepository
import rs.tapizlabs.mail.data.repository.MailSyncGateway
import rs.tapizlabs.mail.ui.model.AccountSummaryUi
import rs.tapizlabs.mail.ui.model.MessageListItemUi
import rs.tapizlabs.mail.ui.model.toListItemUi
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

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MailRepository,
    private val syncGateway: MailSyncGateway,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filters = MutableStateFlow(SearchFiltersUi())

    /** Filtering (account, has-attachment) and the result cap happen in SQL, against the
     * body-less list projection — the old path loaded every matching row *with* its full
     * bodies and filtered in memory on each keystroke. */
    private val results = combine(
        query.debounce(SEARCH_DEBOUNCE_MS).map { it.trim() }.distinctUntilChanged(),
        filters,
        ::Pair,
    ).flatMapLatest { (q, currentFilters) ->
        if (q.isBlank()) {
            flowOf(emptyList())
        } else {
            repository.searchMessages(q, currentFilters.accountId, currentFilters.hasAttachmentOnly)
        }
    }

    val uiState: StateFlow<SearchUiState> = combine(
        query,
        filters,
        repository.observeAccounts(),
        results,
    ) { currentQuery, currentFilters, accounts, results ->
        SearchUiState(
            query = currentQuery,
            accounts = accounts.map { it.toSummaryUi() },
            filters = currentFilters,
            results = results.map { it.toListItemUi() },
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
        viewModelScope.launch { syncGateway.setStarred(messageId, !currentlyStarred) }
    }
}

private fun AccountEntity.toSummaryUi() = AccountSummaryUi(
    id = id,
    displayName = displayName,
    emailAddress = emailAddress,
)
