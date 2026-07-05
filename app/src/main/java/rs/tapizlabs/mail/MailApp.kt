package rs.tapizlabs.mail

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * [Configuration.Provider] is required so WorkManager builds [MailSyncWorker] through Hilt
 * (via [HiltWorkerFactory]) instead of its default no-arg reflection — without this,
 * `MailSyncWorker`'s constructor-injected `SyncRepository` would have no way to be supplied
 * when WorkManager instantiates it on its own.
 */
@HiltAndroidApp
class MailApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
