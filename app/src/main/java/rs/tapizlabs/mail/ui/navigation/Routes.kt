package rs.tapizlabs.mail.ui.navigation

/** Route string constants + arg-substitution helpers for [RootNavigation]'s `NavHost`. */
object Routes {
    /** First-run start destination — language picker precedes [ONBOARDING] so a new user
     * chooses their language before reading any UI copy (standard first-run locale pattern). */
    const val LANGUAGE_PICKER = "language_picker"
    const val ONBOARDING = "onboarding"
    /** Shown once, right after the first account is saved — see [rs.tapizlabs.mail.ui.onboarding.NotificationPermissionScreen]. */
    const val NOTIFICATION_PERMISSION = "notification_permission"
    const val ADD_ACCOUNT = "add_account?firstRun={firstRun}"
    /** Provider chosen on [ADD_ACCOUNT] lands here — a separate page rather than the same
     * screen growing a form beneath the chooser, so picking a provider reads as a clear
     * step forward instead of the page just getting longer. */
    const val ADD_ACCOUNT_DETAILS = "add_account/details/{provider}?firstRun={firstRun}"
    const val EDIT_ACCOUNT = "add_account/edit/{accountId}"
    const val INBOX = "inbox"
    const val SEARCH = "search"
    const val COMPOSE = "compose?mode={mode}&messageId={messageId}"
    const val SETTINGS = "settings"
    const val MAIL_DETAIL = "mail/{messageId}"

    /** [firstRun] distinguishes the onboarding save (which continues to the
     * notification-permission step) from adding an extra account later from Settings
     * (which just pops back) — both share this same details screen/route pattern. */
    fun addAccount(firstRun: Boolean = false) = "add_account?firstRun=$firstRun"
    fun addAccountDetails(provider: String, firstRun: Boolean = false) = "add_account/details/$provider?firstRun=$firstRun"
    fun editAccount(accountId: String) = "add_account/edit/$accountId"
    fun mailDetail(messageId: String) = "mail/$messageId"
    fun compose(mode: String, messageId: String? = null) = "compose?mode=$mode&messageId=${messageId ?: ""}"
}
