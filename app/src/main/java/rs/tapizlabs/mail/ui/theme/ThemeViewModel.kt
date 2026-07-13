package rs.tapizlabs.mail.ui.theme

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

/** Root-level holder for the persisted theme preference — same [PrefsStore]-backed
 * pattern as `LanguageViewModel`, so [rs.tapizlabs.mail.ui.settings.SettingsScreen]'s
 * theme choice actually reaches `MailTheme` in `MainActivity` instead of living only in
 * `SettingsViewModel`'s in-memory state. */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val prefsStore: PrefsStore,
) : ViewModel() {

    val themePref: StateFlow<ThemePref> = prefsStore.themePref.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = runBlocking { prefsStore.themeBlocking() },
    )

    val skinPref: StateFlow<MailSkin> = prefsStore.skinPref.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = runBlocking { prefsStore.skinBlocking() },
    )

    fun setTheme(pref: ThemePref) {
        viewModelScope.launch { prefsStore.setThemePref(pref) }
    }

    fun setSkin(skin: MailSkin) {
        viewModelScope.launch { prefsStore.setSkinPref(skin) }
    }
}
