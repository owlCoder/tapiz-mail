package rs.tapizlabs.mail.ui.i18n

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import rs.tapizlabs.mail.core.local.PrefsStore
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val prefsStore: PrefsStore,
) : ViewModel() {

    val language: StateFlow<AppLanguage> = prefsStore.languagePref.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = runBlocking { prefsStore.languageBlocking() },
    )

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { prefsStore.setLanguagePref(language) }
    }
}
