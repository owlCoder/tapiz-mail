package rs.tapizlabs.mail.core.local

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import rs.tapizlabs.mail.ui.i18n.AppLanguage
import javax.inject.Inject
import javax.inject.Singleton

private val Context.prefsDataStore by preferencesDataStore("mail_prefs")

/** App-wide DataStore-backed preferences (currently just the language pref; the natural
 * home for future simple prefs like sync-interval/theme, rather than a second DataStore
 * file — see the sibling apps' `core/local/PrefsStore.kt` for the same convention). */
@Singleton
class PrefsStore @Inject constructor(@ApplicationContext private val ctx: Context) {

    private val KEY_LANGUAGE = stringPreferencesKey("app_language")

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
}
