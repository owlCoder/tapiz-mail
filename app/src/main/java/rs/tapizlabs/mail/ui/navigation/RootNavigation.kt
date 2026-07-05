package rs.tapizlabs.mail.ui.navigation

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
import rs.tapizlabs.mail.ui.components.MailLoadingScreen
import rs.tapizlabs.mail.ui.compose.ComposeScreen
import rs.tapizlabs.mail.ui.detail.MailDetailScreen
import rs.tapizlabs.mail.ui.i18n.CurrentStrings
import rs.tapizlabs.mail.ui.i18n.LanguageViewModel
import rs.tapizlabs.mail.ui.i18n.LocalStrings
import rs.tapizlabs.mail.ui.i18n.stringsFor
import rs.tapizlabs.mail.ui.inbox.InboxScreen
import rs.tapizlabs.mail.ui.onboarding.LanguagePickerScreen
import rs.tapizlabs.mail.ui.onboarding.NotificationPermissionScreen
import rs.tapizlabs.mail.ui.onboarding.OnboardingScreen
import rs.tapizlabs.mail.ui.search.SearchScreen
import rs.tapizlabs.mail.ui.settings.SettingsScreen

/**
 * Top-level NavHost, called with no args from `MainActivity` inside `MailTheme { }`.
 *
 * No bottom nav bar — design_handoff_tapiz_mail_android/design-reference.html shows a
 * full-bleed Inbox with no tab bar at all. Compose is reached only via the FAB; Search
 * and Settings are reached via icon buttons on the Inbox top bar and are plain push
 * destinations (with a back arrow), not tabs.
 *
 * Start destination depends on whether any account is configured yet ([RootViewModel]):
 * no accounts -> Language picker -> Onboarding -> Add-Account (first-run); otherwise Inbox,
 * with Search/Compose/Settings/MailDetail/Add-Account-edit reached via push-navigation.
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

    when (startState) {
        RootStartState.Loading -> {
            MailLoadingScreen(modifier = Modifier.fillMaxSize())
            return
        }
        else -> Unit
    }

    val startDestination = if (startState == RootStartState.NoAccounts) Routes.LANGUAGE_PICKER else Routes.INBOX

    LaunchedEffect(startState, pendingMessageId) {
        if (startState == RootStartState.HasAccounts && pendingMessageId != null) {
            navController.navigate(Routes.mailDetail(pendingMessageId))
        }
    }

    CompositionLocalProvider(LocalStrings provides stringsFor(language)) {
    NavHost(navController = navController, startDestination = startDestination) {
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
            NotificationPermissionScreen(
                onAllow = {
                    onRequestNotificationPermission()
                    navController.navigate(Routes.INBOX) { popUpTo(0) { inclusive = true } }
                },
                onSkip = {
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
                onAddAccount = { navController.navigate(Routes.addAccount()) },
                onCompose = { navController.navigate(Routes.compose(mode = "new")) },
                onSearch = { navController.navigate(Routes.SEARCH) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                onOpenMessage = { messageId -> navController.navigate(Routes.mailDetail(messageId)) },
                onBack = { navController.popBackStack() },
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
                onBack = { navController.popBackStack() },
            )
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
