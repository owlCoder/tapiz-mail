package rs.tapizlabs.mail.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keystore-backed encrypted storage for IMAP/SMTP account passwords.
 *
 * Tapiz Mail has no backend: this is the only place credentials are persisted, and they never
 * leave the device. Backed by Jetpack Security's [EncryptedSharedPreferences] — keys are
 * encrypted with AES256-SIV, values with AES256-GCM, and both are wrapped by a Keystore-resident
 * [MasterKey] (AES256-GCM key scheme), so the underlying prefs file on disk is never plaintext.
 *
 * One shared prefs file for all accounts, with per-account/per-protocol key namespacing
 * (`imap_pw_<accountId>` / `smtp_pw_<accountId>`) rather than one file per account, since Android
 * pays a fixed Keystore/Tink setup cost per EncryptedSharedPreferences instance.
 */
@Singleton
class CredentialStore @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun saveImapPassword(accountId: String, password: String) {
        prefs.edit().putString(imapKey(accountId), password).apply()
    }

    fun getImapPassword(accountId: String): String? = prefs.getString(imapKey(accountId), null)

    fun saveSmtpPassword(accountId: String, password: String) {
        prefs.edit().putString(smtpKey(accountId), password).apply()
    }

    fun getSmtpPassword(accountId: String): String? = prefs.getString(smtpKey(accountId), null)

    fun deleteCredentials(accountId: String) {
        prefs.edit()
            .remove(imapKey(accountId))
            .remove(smtpKey(accountId))
            .apply()
    }

    private fun imapKey(accountId: String) = "imap_pw_$accountId"

    private fun smtpKey(accountId: String) = "smtp_pw_$accountId"

    private companion object {
        const val PREFS_FILE_NAME = "tapiz_mail_credentials"
    }
}
