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
import dagger.hilt.android.AndroidEntryPoint
import rs.tapizlabs.mail.sync.IdleSyncService
import rs.tapizlabs.mail.sync.NewMailNotifier
import rs.tapizlabs.mail.ui.navigation.RootNavigation
import rs.tapizlabs.mail.ui.theme.MailTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

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
        requestNotificationPermissionIfNeeded()
        pendingMessageId = intent?.getStringExtra(NewMailNotifier.EXTRA_MESSAGE_ID)
        setContent {
            MailTheme {
                RootNavigation(pendingMessageId = pendingMessageId)
            }
        }
    }

    /** Android 13+ requires runtime consent for POST_NOTIFICATIONS — without this, the
     * manifest permission alone silently no-ops [NewMailNotifier]'s notify() calls. */
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
