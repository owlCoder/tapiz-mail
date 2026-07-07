package rs.tapizlabs.mail.core.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import rs.tapizlabs.mail.ui.i18n.AppLanguage
import rs.tapizlabs.mail.ui.theme.ThemePref
import javax.inject.Inject
import javax.inject.Singleton

private val Context.prefsDataStore by preferencesDataStore("mail_prefs")

/** App-wide DataStore-backed preferences (language + theme; the natural home for future
 * simple prefs like sync-interval, rather than a second DataStore file — see the sibling
 * apps' `core/local/PrefsStore.kt` for the same convention). */
@Singleton
class PrefsStore @Inject constructor(@ApplicationContext private val ctx: Context) {

    private val KEY_LANGUAGE = stringPreferencesKey("app_language")
    private val KEY_THEME = stringPreferencesKey("app_theme")
    private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    private val KEY_NOTIFICATION_SOUND_ENABLED = booleanPreferencesKey("notification_sound_enabled")

    val languagePref: Flow<AppLanguage> = ctx.prefsDataStore.data.map { prefs ->
        when (prefs[KEY_LANGUAGE]) {
            "en" -> AppLanguage.EN
            "de" -> AppLanguage.DE
            "es" -> AppLanguage.ES
            "fr" -> AppLanguage.FR
            "sr" -> AppLanguage.SR
            else -> AppLanguage.SR
        }
    }

    suspend fun setLanguagePref(language: AppLanguage) {
        ctx.prefsDataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = language.code
        }
    }

    suspend fun languageBlocking(): AppLanguage = languagePref.first()

    val themePref: Flow<ThemePref> = ctx.prefsDataStore.data.map { prefs ->
        when (prefs[KEY_THEME]) {
            "light" -> ThemePref.Light
            "dark" -> ThemePref.Dark
            else -> ThemePref.System
        }
    }

    suspend fun setThemePref(pref: ThemePref) {
        ctx.prefsDataStore.edit { prefs ->
            prefs[KEY_THEME] = when (pref) {
                ThemePref.System -> "system"
                ThemePref.Light -> "light"
                ThemePref.Dark -> "dark"
            }
        }
    }

    suspend fun themeBlocking(): ThemePref = themePref.first()

    /** Defaults to `true` (opted in) — matches the existing behavior before this preference
     * existed, so upgrading users keep getting notified without an extra step. */
    val notificationsEnabledPref: Flow<Boolean> = ctx.prefsDataStore.data.map { prefs ->
        prefs[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    suspend fun setNotificationsEnabledPref(enabled: Boolean) {
        ctx.prefsDataStore.edit { prefs ->
            prefs[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun notificationsEnabledBlocking(): Boolean = notificationsEnabledPref.first()

    /** Defaults to `false` (silent) — the user asked for new-mail notifications to be
     * non-intrusive by default; sound is an explicit opt-in from Settings, not a surprise. */
    val notificationSoundEnabledPref: Flow<Boolean> = ctx.prefsDataStore.data.map { prefs ->
        prefs[KEY_NOTIFICATION_SOUND_ENABLED] ?: false
    }

    suspend fun setNotificationSoundEnabledPref(enabled: Boolean) {
        ctx.prefsDataStore.edit { prefs ->
            prefs[KEY_NOTIFICATION_SOUND_ENABLED] = enabled
        }
    }
}
