package rs.tapizlabs.mail.ui.navigation

/** Route string constants + arg-substitution helpers for [RootNavigation]'s `NavHost`. */
object Routes {
    /** First-run start destination — language picker precedes [ONBOARDING] so a new user
     * chooses their language before reading any UI copy (standard first-run locale pattern). */
    const val LANGUAGE_PICKER = "language_picker"
    const val ONBOARDING = "onboarding"
    const val ADD_ACCOUNT = "add_account"
    /** Provider chosen on [ADD_ACCOUNT] lands here — a separate page rather than the same
     * screen growing a form beneath the chooser, so picking a provider reads as a clear
     * step forward instead of the page just getting longer. */
    const val ADD_ACCOUNT_DETAILS = "add_account/details/{provider}"
    const val EDIT_ACCOUNT = "add_account/edit/{accountId}"
    const val INBOX = "inbox"
    const val SEARCH = "search"
    const val COMPOSE = "compose?mode={mode}&messageId={messageId}"
    const val SETTINGS = "settings"
    const val MAIL_DETAIL = "mail/{messageId}"

    fun addAccountDetails(provider: String) = "add_account/details/$provider"
    fun editAccount(accountId: String) = "add_account/edit/$accountId"
    fun mailDetail(messageId: String) = "mail/$messageId"
    fun compose(mode: String, messageId: String? = null) = "compose?mode=$mode&messageId=${messageId ?: ""}"
}

/** The 4 bottom-nav destinations, per the design guideline (bottom nav only for 3+ genuinely
 * distinct top-level destinations that need to be one tap away).
 *
 * [route] is the concrete, navigable destination for tapping the tab (Compose's tab tap always
 * means "new message", so it resolves the `mode=new` literal rather than the `{mode}`/
 * `{messageId}` placeholder pattern used when *registering* the route in the NavHost — see
 * `Routes.COMPOSE` for that pattern).
 */
enum class BottomNavTab(val route: String) {
    INBOX(Routes.INBOX),
    SEARCH(Routes.SEARCH),
    COMPOSE(Routes.compose(mode = "new")),
    SETTINGS(Routes.SETTINGS),
}
