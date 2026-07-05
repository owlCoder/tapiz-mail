package rs.tapizlabs.mail.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import rs.tapizlabs.mail.data.repository.AccountRepository

sealed interface RootStartState {
    data object Loading : RootStartState
    data object NoAccounts : RootStartState
    data object HasAccounts : RootStartState
}

/** Decides whether [RootNavigation] starts at Onboarding or the main tabbed scaffold —
 * the only "session" concept in a backend-less, on-device mail app is "is at least one
 * account configured yet." */
@HiltViewModel
class RootViewModel @Inject constructor(
    accountRepository: AccountRepository,
) : ViewModel() {

    val startState: StateFlow<RootStartState> = accountRepository.observeAccounts()
        .map { accounts -> if (accounts.isEmpty()) RootStartState.NoAccounts else RootStartState.HasAccounts }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RootStartState.Loading)
}
