package rs.tapizlabs.mail

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import rs.tapizlabs.mail.data.local.dao.AccountDao
import rs.tapizlabs.mail.sync.IdleSyncService
import rs.tapizlabs.mail.sync.NewMailNotifier
import rs.tapizlabs.mail.sync.SyncScheduler
import rs.tapizlabs.mail.ui.navigation.RootNavigation
import rs.tapizlabs.mail.ui.theme.MailTheme
import rs.tapizlabs.mail.ui.theme.ThemeViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var accountDao: AccountDao
    @Inject lateinit var syncScheduler: SyncScheduler

    private var pendingMessageId by mutableStateOf<String?>(null)

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way — the
            "new mail" push is a courtesy, not something we gate other functionality behind */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Kick off the IDLE foreground service on app launch — it self-filters to only
        // accounts with supportsIdle = true and self-stops a few minutes after the app
        // backgrounds (see IdleSyncService), so this is safe to call unconditionally here
        // rather than threading an account-aware check through the UI layer.
        ContextCompat.startForegroundService(this, Intent(this, IdleSyncService::class.java))
        // Defensively re-arm every account's periodic WorkManager sync on every app launch.
        // scheduleFor() is otherwise only called from Add-Account/Settings, so if the OS ever
        // drops the enqueued periodic work (Force Stop, battery-optimization "clear background
        // data", app data cleared and restored, WorkManager DB loss) nothing else brings it
        // back — the user is stuck with only the foreground IDLE service, which itself stops
        // ~3 minutes after backgrounding, i.e. exactly "sync only happens when I open the app".
        // enqueueUniquePeriodicWork's UPDATE policy makes this a safe no-op when work is already
        // correctly scheduled.
        lifecycleScope.launch {
            val accounts = accountDao.getActiveAccounts().first()
            syncScheduler.rescheduleAll(accounts)
        }
        pendingMessageId = intent?.getStringExtra(NewMailNotifier.EXTRA_MESSAGE_ID)
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val themePref by themeViewModel.themePref.collectAsStateWithLifecycle()
            val skinPref by themeViewModel.skinPref.collectAsStateWithLifecycle()
            MailTheme(themePref = themePref, skin = skinPref) {
                RootNavigation(
                    pendingMessageId = pendingMessageId,
                    onRequestNotificationPermission = ::requestNotificationPermissionIfNeeded,
                )
            }
        }
    }

    /** Android 13+ requires runtime consent for POST_NOTIFICATIONS — without this, the
     * manifest permission alone silently no-ops [NewMailNotifier]'s notify() calls. Called
     * from the dedicated onboarding "Notifications" step (not fired blindly from onCreate)
     * so a returning user with accounts already set up never sees the system prompt jump
     * in front of whatever screen they landed on. */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val alreadyGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!alreadyGranted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // App already running, user tapped another "new mail" notification — re-set so
        // RootNavigation's LaunchedEffect(pendingMessageId) fires again for the new id.
        setIntent(intent)
        pendingMessageId = intent.getStringExtra(NewMailNotifier.EXTRA_MESSAGE_ID)
    }
}
