package rs.tapizlabs.mail.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import rs.tapizlabs.mail.ui.account.AddAccountScreen
import rs.tapizlabs.mail.ui.account.ChooseProviderScreen
import rs.tapizlabs.mail.ui.compose.ComposeScreen
import rs.tapizlabs.mail.ui.detail.MailDetailScreen
import rs.tapizlabs.mail.ui.i18n.CurrentStrings
import rs.tapizlabs.mail.ui.i18n.LanguageViewModel
import rs.tapizlabs.mail.ui.i18n.LocalAppLanguage
import rs.tapizlabs.mail.ui.i18n.LocalStrings
import rs.tapizlabs.mail.ui.i18n.stringsFor
import rs.tapizlabs.mail.ui.inbox.InboxScreen
import rs.tapizlabs.mail.ui.onboarding.LanguagePickerScreen
import rs.tapizlabs.mail.ui.onboarding.NotificationPermissionScreen
import rs.tapizlabs.mail.ui.onboarding.OnboardingScreen
import rs.tapizlabs.mail.ui.settings.AboutScreen
import rs.tapizlabs.mail.ui.settings.AppearanceSettingsScreen
import rs.tapizlabs.mail.ui.settings.MailSettingsScreen
import rs.tapizlabs.mail.ui.settings.NotificationsSettingsScreen
import rs.tapizlabs.mail.ui.settings.PrivacyScreen
import rs.tapizlabs.mail.ui.settings.SettingsScreen
import rs.tapizlabs.mail.ui.theme.AppColors

/**
 * Top-level NavHost, called with no args from `MainActivity` inside `MailTheme { }`.
 *
 * No bottom nav bar — design_handoff_tapiz_mail_android/design-reference.html shows a
 * full-bleed Inbox with no tab bar at all. Compose/Drafts/Settings are reached via icon
 * buttons on the Inbox top bar and are plain push destinations (with a back arrow), not
 * tabs. Search is the one exception — it's a local full-screen overlay inside
 * [rs.tapizlabs.mail.ui.inbox.InboxScreen] (Gmail-style), not a NavHost route at all.
 *
 * Start destination depends on whether any account is configured yet ([RootViewModel]):
 * no accounts -> Language picker -> Onboarding -> Add-Account (first-run); otherwise Inbox,
 * with Drafts/Compose/Settings/MailDetail/Add-Account-edit reached via push-navigation.
 *
 * @param pendingMessageId set by `MainActivity` when the app was launched/resumed from a
 * "new mail" notification tap ([rs.tapizlabs.mail.sync.NewMailNotifier]) — navigates straight
 * to that message's detail screen once Inbox is up, instead of requiring the user to find it
 * themselves.
 */
@Composable
fun RootNavigation(
    pendingMessageId: String? = null,
    onRequestNotificationPermission: () -> Unit = {},
    rootViewModel: RootViewModel = hiltViewModel(),
    languageViewModel: LanguageViewModel = hiltViewModel(),
) {
    val startState by rootViewModel.startState.collectAsStateWithLifecycle()
    val language by languageViewModel.language.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    // Keep the non-composable snapshot (ViewModel/repository messages) in sync.
    CurrentStrings.value = stringsFor(language)

    // No loading screen here — resolving whether an account exists is a local Room read
    // that finishes before the native splash even hands off, so a brand loading state
    // would never actually get seen; it only added a pointless composition step.
    if (startState == RootStartState.Loading) return

    val startDestination = if (startState == RootStartState.NoAccounts) Routes.LANGUAGE_PICKER else Routes.INBOX

    LaunchedEffect(startState, pendingMessageId) {
        if (startState == RootStartState.HasAccounts && pendingMessageId != null) {
            navController.navigate(Routes.mailDetail(pendingMessageId))
        }
    }

    CompositionLocalProvider(LocalStrings provides stringsFor(language), LocalAppLanguage provides language) {
    // Solid theme-colored backdrop behind the whole NavHost — without this, the brief gap
    // between two screens sliding past each other during a push/pop transition (each screen
    // is its own Scaffold with its own containerColor, but nothing paints the space *around*
    // them while they're both mid-animation) falls through to the Android Window's own
    // background, which defaults to white and reads as a flash in dark mode.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.canvasTop),
    ) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        // Signature app-wide transition recipe (matches InboxScreen's category-chip
        // AnimatedContent and MailSheet's overlay family): 220ms FastOutSlowInEasing in,
        // 140ms LinearOutSlowInEasing out. Push slides the new screen in from the right
        // while the old one recedes to the left; pop reverses the direction.
        enterTransition = {
            fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { it / 4 }
        },
        exitTransition = {
            fadeOut(tween(140, easing = LinearOutSlowInEasing)) +
                slideOutHorizontally(tween(140, easing = LinearOutSlowInEasing)) { -it / 4 }
        },
        popEnterTransition = {
            fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { -it / 4 }
        },
        popExitTransition = {
            fadeOut(tween(140, easing = LinearOutSlowInEasing)) +
                slideOutHorizontally(tween(140, easing = LinearOutSlowInEasing)) { it / 4 }
        },
    ) {
        composable(Routes.LANGUAGE_PICKER) {
            LanguagePickerScreen(
                selected = language,
                onLanguageChosen = { chosen ->
                    languageViewModel.setLanguage(chosen)
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.LANGUAGE_PICKER) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.ONBOARDING) {
            OnboardingScreen(onGetStarted = { navController.navigate(Routes.addAccount(firstRun = true)) })
        }

        composable(
            route = Routes.ADD_ACCOUNT,
            arguments = listOf(navArgument("firstRun") { type = NavType.BoolType; defaultValue = false }),
        ) { backStackEntry ->
            val firstRun = backStackEntry.arguments?.getBoolean("firstRun") ?: false
            ChooseProviderScreen(
                onBack = { navController.popBackStack() },
                onProviderChosen = { provider ->
                    navController.navigate(Routes.addAccountDetails(provider.name, firstRun = firstRun))
                },
            )
        }

        composable(
            route = Routes.ADD_ACCOUNT_DETAILS,
            arguments = listOf(
                navArgument("provider") { type = NavType.StringType },
                navArgument("firstRun") { type = NavType.BoolType; defaultValue = false },
            ),
        ) { backStackEntry ->
            val firstRun = backStackEntry.arguments?.getBoolean("firstRun") ?: false
            AddAccountScreen(
                onBack = { navController.popBackStack() },
                onSaved = {
                    if (firstRun) {
                        // First-run save: continue to the notification-permission step,
                        // clearing onboarding/chooser/details from the back stack so back
                        // doesn't return to them.
                        navController.navigate(Routes.NOTIFICATION_PERMISSION) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        // Adding an extra account later (from Settings/Inbox) — just return.
                        navController.popBackStack()
                    }
                },
            )
        }

        composable(Routes.NOTIFICATION_PERMISSION) {
            // Reuses SettingsViewModel purely for its setNotificationsEnabled setter — this
            // onboarding step is the in-app counterpart of the system POST_NOTIFICATIONS
            // dialog, so Allow/Skip here also drives the app-level notifications-enabled
            // preference (Settings > Notifications), not just the OS permission prompt.
            val settingsViewModel: rs.tapizlabs.mail.ui.settings.SettingsViewModel = hiltViewModel()
            NotificationPermissionScreen(
                onAllow = {
                    settingsViewModel.setNotificationsEnabled(true)
                    onRequestNotificationPermission()
                    navController.navigate(Routes.INBOX) { popUpTo(0) { inclusive = true } }
                },
                onSkip = {
                    settingsViewModel.setNotificationsEnabled(false)
                    navController.navigate(Routes.INBOX) { popUpTo(0) { inclusive = true } }
                },
            )
        }

        composable(
            route = Routes.EDIT_ACCOUNT,
            arguments = listOf(navArgument("accountId") { type = NavType.StringType }),
        ) {
            AddAccountScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        composable(Routes.INBOX) {
            InboxScreen(
                onOpenMessage = { messageId -> navController.navigate(Routes.mailDetail(messageId)) },
                onOpenDraft = { messageId -> navController.navigate(Routes.compose(mode = "draft", messageId = messageId)) },
                onAddAccount = { navController.navigate(Routes.addAccount()) },
                onCompose = { navController.navigate(Routes.compose(mode = "new")) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(
            route = Routes.COMPOSE,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType; defaultValue = "new" },
                navArgument("messageId") { type = NavType.StringType; nullable = true },
            ),
        ) {
            ComposeScreen(
                onSent = { navController.popBackStack(Routes.INBOX, inclusive = false) },
                onBack = { navController.popBackStack(Routes.INBOX, inclusive = false) },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onAddAccount = { navController.navigate(Routes.addAccount()) },
                onEditAccount = { accountId -> navController.navigate(Routes.editAccount(accountId)) },
                onOpenMailSettings = { navController.navigate(Routes.SETTINGS_MAIL) },
                onOpenNotificationsSettings = { navController.navigate(Routes.SETTINGS_NOTIFICATIONS) },
                onOpenAppearanceSettings = { navController.navigate(Routes.SETTINGS_APPEARANCE) },
                onOpenAbout = { navController.navigate(Routes.SETTINGS_ABOUT) },
                onOpenPrivacy = { navController.navigate(Routes.SETTINGS_PRIVACY) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS_MAIL) {
            MailSettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS_NOTIFICATIONS) {
            NotificationsSettingsScreen(
                onBack = { navController.popBackStack() },
                onRequestSystemPermission = onRequestNotificationPermission,
            )
        }

        composable(Routes.SETTINGS_APPEARANCE) {
            AppearanceSettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS_ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS_PRIVACY) {
            PrivacyScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.MAIL_DETAIL,
            arguments = listOf(navArgument("messageId") { type = NavType.StringType }),
        ) {
            MailDetailScreen(
                onReply = { messageId -> navController.navigate(Routes.compose(mode = "reply", messageId = messageId)) },
                onForward = { messageId -> navController.navigate(Routes.compose(mode = "forward", messageId = messageId)) },
                onBack = { navController.popBackStack() },
            )
        }
    }
    }
    }
}
