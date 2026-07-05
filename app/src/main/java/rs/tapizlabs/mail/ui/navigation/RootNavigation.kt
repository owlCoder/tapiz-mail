package rs.tapizlabs.mail.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
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
import rs.tapizlabs.mail.ui.onboarding.OnboardingScreen
import rs.tapizlabs.mail.ui.search.SearchScreen
import rs.tapizlabs.mail.ui.settings.SettingsScreen
import rs.tapizlabs.mail.ui.theme.AppColors

/**
 * Top-level NavHost, called with no args from `MainActivity` inside `MailTheme { }`.
 *
 * Start destination depends on whether any account is configured yet ([RootViewModel]):
 * no accounts -> Onboarding -> Add-Account (first-run); otherwise the tabbed Inbox/Search/
 * Compose/Settings scaffold, with MailDetail and the Add-Account edit-flow reached via
 * push-navigation (not bottom-tab) from within that scaffold.
 *
 * TODO: verify InboxScreen/MailDetailScreen/ComposeScreen/SearchScreen call sites against
 * their actual composable signatures once/if they change — as of this writing the four
 * screens already exist under ui/inbox, ui/detail, ui/compose, ui/search and were read
 * directly (not guessed): `InboxScreen(onOpenMessage, onAddAccount)`,
 * `MailDetailScreen(onReply, onForward, onBack)` (messageId comes from the nav-arg
 * SavedStateHandle, not a direct param), `ComposeScreen(onSent, onBack)` (mode/messageId
 * likewise via SavedStateHandle), `SearchScreen(onOpenMessage)`.
 *
 * @param pendingMessageId set by `MainActivity` when the app was launched/resumed from a
 * "new mail" notification tap ([rs.tapizlabs.mail.sync.NewMailNotifier]) — navigates straight
 * to that message's detail screen once the tabbed scaffold is up, instead of requiring the
 * user to find it in the Inbox themselves.
 */
@Composable
fun RootNavigation(
    pendingMessageId: String? = null,
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
            OnboardingScreen(onGetStarted = { navController.navigate(Routes.ADD_ACCOUNT) })
        }

        composable(Routes.ADD_ACCOUNT) {
            ChooseProviderScreen(
                onBack = { navController.popBackStack() },
                onProviderChosen = { provider ->
                    navController.navigate(Routes.addAccountDetails(provider.name))
                },
            )
        }

        composable(
            route = Routes.ADD_ACCOUNT_DETAILS,
            arguments = listOf(navArgument("provider") { type = NavType.StringType }),
        ) {
            AddAccountScreen(
                onBack = { navController.popBackStack() },
                onSaved = {
                    // First-run save: land on the main tabbed scaffold, clearing onboarding
                    // and the chooser/details steps from the back stack so back doesn't
                    // return to them.
                    navController.navigate(Routes.INBOX) {
                        popUpTo(0) { inclusive = true }
                    }
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
            MailScaffold(navController = navController, selectedTab = BottomNavTab.INBOX) {
                InboxScreen(
                    onOpenMessage = { messageId -> navController.navigate(Routes.mailDetail(messageId)) },
                    onAddAccount = { navController.navigate(Routes.ADD_ACCOUNT) },
                )
            }
        }

        composable(Routes.SEARCH) {
            MailScaffold(navController = navController, selectedTab = BottomNavTab.SEARCH) {
                SearchScreen(
                    onOpenMessage = { messageId -> navController.navigate(Routes.mailDetail(messageId)) },
                )
            }
        }

        composable(
            route = Routes.COMPOSE,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType; defaultValue = "new" },
                navArgument("messageId") { type = NavType.StringType; nullable = true },
            ),
        ) {
            MailScaffold(navController = navController, selectedTab = BottomNavTab.COMPOSE) {
                ComposeScreen(
                    onSent = { navController.popBackStack(Routes.INBOX, inclusive = false) },
                    onBack = { navController.popBackStack(Routes.INBOX, inclusive = false) },
                )
            }
        }

        composable(Routes.SETTINGS) {
            MailScaffold(navController = navController, selectedTab = BottomNavTab.SETTINGS) {
                SettingsScreen(
                    onAddAccount = { navController.navigate(Routes.ADD_ACCOUNT) },
                    onEditAccount = { accountId -> navController.navigate(Routes.editAccount(accountId)) },
                )
            }
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

/** Wraps the 4 bottom-tab destinations in a shared [Scaffold] + [MailBottomBar] so each tab
 * composable stays a plain content composable rather than re-declaring the bar 4 times. */
@Composable
private fun MailScaffold(
    navController: androidx.navigation.NavController,
    selectedTab: BottomNavTab,
    content: @Composable () -> Unit,
) {
    Scaffold(
        containerColor = AppColors.canvasTop,
        bottomBar = {
            MailBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    if (tab != selectedTab) {
                        navController.navigate(tab.route) {
                            // Standard single-top bottom-nav behavior: one instance of each
                            // tab on the back stack, restore state when re-selecting a tab.
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            content()
        }
    }
}
