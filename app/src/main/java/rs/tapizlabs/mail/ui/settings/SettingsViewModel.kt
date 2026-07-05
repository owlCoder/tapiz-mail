package rs.tapizlabs.mail.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rs.tapizlabs.mail.data.local.entity.AccountEntity
import rs.tapizlabs.mail.data.local.entity.CategoryEntity
import rs.tapizlabs.mail.data.local.entity.CategoryRuleEntity
import rs.tapizlabs.mail.data.local.entity.RuleMatchField
import rs.tapizlabs.mail.data.local.entity.RuleMatchType
import rs.tapizlabs.mail.data.local.entity.SwipeAction
import rs.tapizlabs.mail.data.local.entity.SwipeActionConfigEntity
import rs.tapizlabs.mail.data.repository.AccountRepository
import rs.tapizlabs.mail.security.CredentialStore
import rs.tapizlabs.mail.sync.SyncScheduler
import rs.tapizlabs.mail.ui.theme.ThemePref

data class SettingsUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val selectedAccountId: String? = null,
    val swipeConfig: SwipeActionConfigEntity? = null,
    val themePref: ThemePref = ThemePref.System,
) {
    val selectedAccount: AccountEntity?
        get() = accounts.find { it.id == selectedAccountId } ?: accounts.firstOrNull()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val credentialStore: CredentialStore,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    private val _selectedAccountId = MutableStateFlow<String?>(null)
    private val _themePref = MutableStateFlow(ThemePref.System)

    val state: StateFlow<SettingsUiState> = combine(
        accountRepository.observeAccounts(),
        accountRepository.observeAllCategories(),
        _selectedAccountId,
        _themePref,
    ) { accounts, categories, selectedId, themePref ->
        val resolvedSelectedId = selectedId ?: accounts.firstOrNull()?.id
        Quadruple(accounts, categories, resolvedSelectedId, themePref)
    }.flatMapLatest { (accounts, categories, resolvedSelectedId, themePref) ->
        val swipeFlow = resolvedSelectedId?.let { accountRepository.observeSwipeConfig(it) } ?: flowOf(null)
        swipeFlow.map { swipeConfig ->
            SettingsUiState(
                accounts = accounts,
                categories = categories,
                selectedAccountId = resolvedSelectedId,
                swipeConfig = swipeConfig,
                themePref = themePref,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun selectAccount(accountId: String) {
        _selectedAccountId.value = accountId
    }

    fun setTheme(pref: ThemePref) {
        _themePref.value = pref
    }

    /** Applies a new sync interval to an account and reschedules its background sync. */
    fun updateSyncInterval(account: AccountEntity, minutes: Int) {
        viewModelScope.launch {
            val updated = account.copy(syncIntervalMinutes = minutes)
            accountRepository.saveAccount(
                updated,
                credentialStore.getImapPassword(account.id).orEmpty(),
                credentialStore.getSmtpPassword(account.id).orEmpty(),
            )
            syncScheduler.scheduleFor(updated)
        }
    }

    fun updateSwipeConfig(accountId: String, left: SwipeAction, right: SwipeAction) {
        viewModelScope.launch {
            accountRepository.saveSwipeConfig(
                SwipeActionConfigEntity(accountId = accountId, swipeLeftAction = left, swipeRightAction = right),
            )
        }
    }

    fun removeAccount(account: AccountEntity) {
        viewModelScope.launch {
            accountRepository.deleteAccount(account)
            syncScheduler.cancelFor(account.id)
            if (_selectedAccountId.value == account.id) _selectedAccountId.value = null
        }
    }

    fun observeRulesForCategory(categoryId: String) = accountRepository.observeRulesForCategory(categoryId)

    fun saveCategory(name: String, existingId: String?, accountId: String?) {
        viewModelScope.launch {
            accountRepository.saveCategory(
                CategoryEntity(
                    id = existingId ?: UUID.randomUUID().toString(),
                    accountId = accountId,
                    name = name,
                    colorIndex = (existingId?.hashCode() ?: name.hashCode()).let { kotlin.math.abs(it) % 6 },
                    isSystemDefault = false,
                ),
            )
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch { accountRepository.deleteCategory(category) }
    }

    fun saveRule(categoryId: String, field: RuleMatchField, type: RuleMatchType, value: String, existingId: String?) {
        viewModelScope.launch {
            accountRepository.saveRule(
                CategoryRuleEntity(
                    id = existingId ?: UUID.randomUUID().toString(),
                    categoryId = categoryId,
                    matchField = field,
                    matchType = type,
                    matchValue = value,
                ),
            )
        }
    }

    fun deleteRule(rule: CategoryRuleEntity) {
        viewModelScope.launch { accountRepository.deleteRule(rule) }
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
