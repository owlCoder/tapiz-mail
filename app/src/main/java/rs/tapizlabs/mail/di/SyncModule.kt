package rs.tapizlabs.mail.di

import android.content.ContentResolver
import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides [WorkManager] for [rs.tapizlabs.mail.sync.SyncScheduler]. `MailSyncWorker`
 * itself doesn't need a binding here — `hilt-work`'s `@HiltWorker`/`@AssistedInject` wires
 * it automatically through the [HiltWorkerFactory][androidx.hilt.work.HiltWorkerFactory]
 * that `MailApp` supplies via `Configuration.Provider`. */
@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)

    /** [SmtpClient] streams outgoing attachments from content `Uri`s (SAF picker results)
     * via this resolver rather than reading them into memory up front. */
    @Provides
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver
}
