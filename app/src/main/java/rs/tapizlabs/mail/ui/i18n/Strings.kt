package rs.tapizlabs.mail.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Supported UI languages. Serbian (Latin) is the shipped default; the other four
 * are fully translated for the strings populated so far (onboarding + language
 * picker — see [Strings]).
 */
enum class AppLanguage(val code: String) {
    SR("sr"),
    EN("en"),
    DE("de"),
    ES("es"),
    FR("fr"),
}

/** [java.util.Locale] for this language — used by `java.time` formatters (e.g. relative
 * message timestamps) so date/month names follow the in-app language selection rather than
 * the device's system locale. Serbian is pinned to the Latin script (`sr-Latn`) explicitly —
 * plain `Locale.forLanguageTag("sr")` resolves to Cyrillic month names by default (ICU's
 * default script for "sr"), which would silently mismatch this app's Latin-script Serbian
 * strings (see [SrStrings]) with Cyrillic-lettered dates. */
fun AppLanguage.toLocale(): java.util.Locale = when (this) {
    AppLanguage.SR -> java.util.Locale.forLanguageTag("sr-Latn")
    else -> java.util.Locale.forLanguageTag(code)
}

/**
 * i18n dictionary for the app. Strings are resolved from the active [AppLanguage]
 * through [LocalStrings]. Add a `lateinit var` property here, then a value in
 * every one of [SrStrings]/[EnStrings]/[DeStrings]/[EsStrings]/[FrStrings], and
 * read it via `LocalStrings.current` in the composable.
 *
 * Deliberately NOT a data class with a huge primary constructor: a >255-register
 * constructor invocation compiles but crashes at runtime with a VerifyError (ART
 * DEX limit). `lateinit var` + `apply { }` blocks avoid that ceiling.
 *
 * NOTE: this is the first i18n pass for Tapiz Mail — only the onboarding screen
 * and the language-picker screen have been migrated so far. The rest of the app
 * (Inbox, Compose, Settings, account setup, etc.) still uses hardcoded English
 * literals and should migrate to this dictionary incrementally.
 */
class Strings internal constructor() {
    // Onboarding
    lateinit var onboardingHeadline: String
    lateinit var onboardingSubtext: String
    lateinit var onboardingGetStarted: String
    lateinit var onboardingContinueWithGmail: String
    lateinit var onboardingContinueWithOutlook: String
    lateinit var onboardingOrConnectManually: String
    lateinit var onboardingImapHost: String
    lateinit var onboardingUsername: String

    // Language picker
    lateinit var languagePickerTitle: String
    lateinit var languagePickerSubtitle: String
    lateinit var languageNameSerbian: String
    lateinit var languageNameEnglish: String
    lateinit var languageNameGerman: String
    lateinit var languageNameSpanish: String
    lateinit var languageNameFrench: String

    // Notification permission (onboarding step)
    lateinit var notifPermTitle: String
    lateinit var notifPermSubtext: String
    lateinit var notifPermAllow: String
    lateinit var notifPermSkip: String

    // Choose provider
    lateinit var chooseProviderTitle: String
    lateinit var chooseProviderSectionHeader: String
    lateinit var providerGmailDescription: String
    lateinit var providerOutlookDescription: String
    lateinit var providerCustomTitle: String
    lateinit var providerCustomDescription: String

    // Add account details form
    lateinit var addAccountTitleNew: String
    lateinit var addAccountTitleEdit: String
    lateinit var accountDetailsSectionHeader: String
    lateinit var fieldDisplayName: String
    lateinit var fieldEmailAddress: String
    lateinit var fieldUsername: String
    lateinit var fieldUsernameHint: String
    lateinit var fieldPassword: String
    lateinit var incomingMailSectionHeader: String
    lateinit var outgoingMailSectionHeader: String
    lateinit var fieldImapHost: String
    lateinit var fieldSmtpHost: String
    lateinit var fieldPort: String
    lateinit var fieldSecurity: String
    lateinit var testConnection: String
    lateinit var testingConnection: String
    lateinit var connectionVerified: String
    lateinit var connectionFailed: String
    lateinit var verifyingSettings: String
    lateinit var savingAccount: String
    lateinit var saveAccount: String

    // Inbox
    /** Per-language pluralization for the account-count line — a single `"%d naloga"`
     * template can't cover Serbian's three-way count agreement (1 nalog / 2-4 naloga /
     * 5+ naloga), so each language sets its own count -> String rule here instead of
     * formatting one fixed template. */
    lateinit var inboxAccountsSynced: (Int) -> String
    /** `%d` placeholder for the unread count — use [inboxUnreadCount]. */
    lateinit var inboxUnreadCountTemplate: String
    fun inboxUnreadCount(count: Int): String = inboxUnreadCountTemplate.format(count)
    lateinit var inboxUnreadSubtext: String
    lateinit var inboxNoUnread: String
    lateinit var inboxNoMessages: String
    lateinit var inboxNoMessagesSubtext: String
    lateinit var inboxAllAccounts: String
    lateinit var inboxAddAccount: String
    lateinit var inboxToday: String
    lateinit var inboxYesterday: String
    /** Fixed pseudo-category chip labels, prepended to the user's own category chips —
     * the only entry points into the Inbox/Drafts/Trash views (no separate top-bar icons). */
    lateinit var inboxChipInbox: String
    lateinit var inboxChipSent: String
    lateinit var inboxChipDrafts: String
    lateinit var inboxChipTrash: String
    /** "Empty trash" action shown atop the message list only while the Trash pseudo-category
     * is selected and non-empty — permanent delete, gated by [MailConfirmDialog]. */
    lateinit var trashEmptyAllLabel: String
    lateinit var trashEmptyAllConfirmTitle: String
    lateinit var trashEmptyAllConfirmMessage: String
    lateinit var trashEmptyAllConfirmButton: String

    // Compose
    lateinit var composeNewMessage: String
    lateinit var composeFrom: String
    lateinit var composeTo: String
    lateinit var composeCc: String
    lateinit var composeBcc: String
    lateinit var composeSubject: String
    lateinit var composeBodyPlaceholder: String
    lateinit var composeDiscardTitle: String
    lateinit var composeDiscardMessage: String
    lateinit var composeSaveDraft: String
    lateinit var composeDiscard: String
    lateinit var composeCancel: String

    // Drafts
    lateinit var draftsTitle: String
    lateinit var draftsEmpty: String
    lateinit var draftsEmptySubtext: String
    lateinit var inboxDrafts: String

    // Search
    lateinit var searchPlaceholder: String
    lateinit var searchAllAccounts: String
    lateinit var searchHasAttachment: String
    lateinit var searchHint: String
    lateinit var searchNoResults: String
    lateinit var searchResultsCountTemplate: String
    fun searchResultsCount(count: Int): String = searchResultsCountTemplate.format(count)

    // Mail detail
    lateinit var detailReply: String
    lateinit var detailForward: String
    lateinit var detailMessageNotFound: String
    lateinit var detailNoSubject: String
    /** `%d` placeholder for the attachment count — use [detailAttachmentsCount]. */
    lateinit var detailAttachmentsTemplate: String
    fun detailAttachmentsCount(count: Int): String = detailAttachmentsTemplate.format(count)

    // Settings
    lateinit var settingsTitle: String
    lateinit var settingsAccountsSection: String
    lateinit var settingsNoAccounts: String
    lateinit var settingsAddAccount: String
    lateinit var settingsEditAccount: String
    lateinit var settingsRemoveAccount: String
    lateinit var settingsRemoveAccountTitle: String
    lateinit var settingsRemoveAccountMessageTemplate: String
    fun settingsRemoveAccountMessage(email: String): String = settingsRemoveAccountMessageTemplate.format(email)
    lateinit var settingsRemove: String
    lateinit var settingsCancel: String
    lateinit var settingsSyncSection: String
    lateinit var settingsSyncSectionSubtitle: String
    lateinit var settingsSyncIntervalLabel: String
    lateinit var settingsSyncIntervalMinutesTemplate: String
    fun settingsSyncIntervalMinutes(minutes: Int): String = settingsSyncIntervalMinutesTemplate.format(minutes)
    lateinit var settingsSwipeActionsSection: String
    lateinit var settingsSwipeActionsSectionSubtitle: String
    lateinit var settingsSwipeLeft: String
    lateinit var settingsSwipeRight: String
    lateinit var swipeActionDelete: String
    lateinit var swipeActionMarkRead: String
    lateinit var swipeActionMarkUnread: String
    lateinit var swipeActionNone: String
    lateinit var categoryEditorNewTitle: String
    lateinit var categoryEditorEditTitle: String
    lateinit var categoryEditorNameLabel: String
    lateinit var categoryEditorMatchRulesLabel: String
    lateinit var categoryEditorFieldLabel: String
    lateinit var categoryEditorMatchLabel: String
    lateinit var categoryEditorValueLabel: String
    lateinit var categoryEditorAddRule: String
    lateinit var categoryEditorSave: String
    lateinit var ruleFieldSender: String
    lateinit var ruleFieldSubject: String
    lateinit var ruleFieldBody: String
    lateinit var ruleTypeContains: String
    lateinit var ruleTypeEquals: String
    lateinit var ruleTypeStartsWith: String
    lateinit var settingsCategoriesSection: String
    lateinit var settingsCategoriesSectionSubtitle: String
    lateinit var settingsNoCategories: String
    lateinit var settingsAppearanceSection: String
    lateinit var settingsAppearanceSectionSubtitle: String
    lateinit var settingsTheme: String
    lateinit var settingsThemeSystem: String
    lateinit var settingsThemeLight: String
    lateinit var settingsThemeDark: String
    lateinit var settingsSkinSection: String
    lateinit var settingsSkinSectionSubtitle: String
    lateinit var skinDefault: String
    lateinit var skinOcean: String
    lateinit var skinForest: String
    lateinit var skinRose: String
    lateinit var skinGraphite: String
    lateinit var skinSand: String
    lateinit var skinCrimson: String
    lateinit var skinAurora: String
    lateinit var settingsLanguageSection: String
    lateinit var settingsLanguageSectionSubtitle: String
    lateinit var settingsLanguage: String
    lateinit var settingsMailSection: String
    lateinit var settingsMailSectionSubtitle: String
    lateinit var settingsNotificationsSection: String
    lateinit var settingsNotificationsSectionSubtitle: String
    lateinit var settingsNotificationsToggleLabel: String
    lateinit var settingsNotificationsToggleSubtext: String
    lateinit var settingsNotificationsSoundToggleLabel: String
    lateinit var settingsNotificationsSoundToggleSubtext: String
    lateinit var settingsBatteryLabel: String
    lateinit var settingsBatterySubtext: String
    lateinit var settingsAppearanceLanguageSection: String
    lateinit var settingsAppearanceLanguageSectionSubtitle: String
    lateinit var settingsPrivacySection: String
    lateinit var settingsPrivacySectionSubtitle: String
    lateinit var settingsPrivacyParagraph1: String
    lateinit var settingsPrivacyParagraph2: String
    lateinit var settingsPrivacyParagraph3: String
    lateinit var settingsPrivacyParagraph4: String
    lateinit var settingsAboutSection: String
    lateinit var settingsAppName: String
    lateinit var settingsAboutTagline: String
    lateinit var settingsAboutVersion: String
    lateinit var settingsAboutPlatform: String
    lateinit var settingsAboutAuthor: String
    /** `%s` placeholder for the copyright year — use [settingsCopyright]. */
    lateinit var settingsCopyrightTemplate: String
    fun settingsCopyright(year: String): String = settingsCopyrightTemplate.format(year)
    /** `%s` placeholder for the version name — use [settingsAppVersion]. */
    lateinit var settingsAppVersionTemplate: String
    fun settingsAppVersion(versionName: String): String = settingsAppVersionTemplate.format(versionName)
}

val SrStrings = Strings().apply {
    onboardingHeadline = "Sva tvoja pošta,\njedno prijemno sanduče."
    onboardingSubtext = "Poveži Gmail, Outlook ili bilo koji IMAP nalog i dobij jedno " +
        "uredno, kategorisano prijemno sanduče, sinhronizovano u pozadini, po tvojim uslovima."
    onboardingGetStarted = "Započni"
    onboardingContinueWithGmail = "Nastavi sa Gmail-om"
    onboardingContinueWithOutlook = "Nastavi sa Outlook-om"
    onboardingOrConnectManually = "ili poveži ručno"
    onboardingImapHost = "IMAP HOST"
    onboardingUsername = "KORISNIČKO IME"

    languagePickerTitle = "Izaberite jezik"
    languagePickerSubtitle = "Možete ga promeniti kasnije u podešavanjima."
    languageNameSerbian = "Srpski (latinica)"
    languageNameEnglish = "English"
    languageNameGerman = "Deutsch"
    languageNameSpanish = "Español"
    languageNameFrench = "Français"

    notifPermTitle = "Budi u toku sa svojim sandučetom"
    notifPermSubtext = "Dobijaj obaveštenja kada stigne nova pošta. Ovo možeš promeniti bilo kada u podešavanjima obaveštenja na telefonu."
    notifPermAllow = "Dozvoli obaveštenja"
    notifPermSkip = "Ne sada"

    chooseProviderTitle = "Dodaj nalog"
    chooseProviderSectionHeader = "Izaberi provajdera"
    providerGmailDescription = "imap.gmail.com · smtp.gmail.com"
    providerOutlookDescription = "outlook.office365.com · smtp.office365.com"
    providerCustomTitle = "Prilagođeno (IMAP/SMTP)"
    providerCustomDescription = "Bilo koji drugi provajder, uključujući univerzitetsku/školsku poštu"

    addAccountTitleNew = "Detalji naloga"
    addAccountTitleEdit = "Izmeni nalog"
    accountDetailsSectionHeader = "Detalji naloga"
    fieldDisplayName = "Prikazano ime"
    fieldEmailAddress = "Email adresa"
    fieldUsername = "Korisničko ime"
    fieldUsernameHint = "Obično isto kao email adresa"
    fieldPassword = "Lozinka"
    incomingMailSectionHeader = "Dolazna pošta (IMAP)"
    outgoingMailSectionHeader = "Odlazna pošta (SMTP)"
    fieldImapHost = "IMAP host"
    fieldSmtpHost = "SMTP host"
    fieldPort = "Port"
    fieldSecurity = "Bezbednost"
    testConnection = "Testiraj konekciju"
    testingConnection = "Testiranje…"
    connectionVerified = "Konekcija potvrđena"
    connectionFailed = "Konekcija neuspešna"
    verifyingSettings = "Proveravanje podešavanja…"
    savingAccount = "Čuvanje…"
    saveAccount = "Sačuvaj nalog"

    // Serbian count agreement: 1 -> "nalog" (singular), 2-4 -> "naloga" (paucal), 0/5+/
    // 11-14 -> "naloga" (genitive plural) — same surface word for paucal and genitive
    // plural here, but the singular form genuinely differs ("nalog" vs "naloga").
    inboxAccountsSynced = { count ->
        val mod10 = count % 10
        val mod100 = count % 100
        val noun = if (mod10 == 1 && mod100 != 11) "nalog" else "naloga"
        "$count $noun"
    }
    inboxUnreadCountTemplate = "%d nepročitano"
    inboxUnreadSubtext = "Pošta stiže i sinhronizuje se u pozadini"
    inboxNoUnread = "Nema nepročitanih"
    inboxNoMessages = "Nema poruka"
    inboxNoMessagesSubtext = "Nova pošta će se pojaviti ovde nakon sinhronizacije naloga."
    inboxAllAccounts = "Svi nalozi"
    inboxAddAccount = "Dodaj nalog"
    inboxToday = "Danas"
    inboxYesterday = "Juče"
    inboxChipInbox = "Prijemno"
    inboxChipSent = "Poslato"
    inboxChipDrafts = "Nedovršeno"
    inboxChipTrash = "Otpad"
    trashEmptyAllLabel = "Isprazni otpad"
    trashEmptyAllConfirmTitle = "Isprazniti otpad?"
    trashEmptyAllConfirmMessage = "Ovo trajno briše sve poruke u otpadu. Ova radnja se ne može poništiti."
    trashEmptyAllConfirmButton = "Isprazni"

    composeNewMessage = "Nova poruka"
    composeFrom = "Od"
    composeTo = "Prima"
    composeCc = "Cc"
    composeBcc = "Bcc"
    composeSubject = "Naslov"
    composeBodyPlaceholder = "Napiši poruku…"
    composeDiscardTitle = "Odbaciti poruku?"
    composeDiscardMessage = "Možeš sačuvati poruku kao draft i nastaviti kasnije, ili je trajno odbaciti."
    composeSaveDraft = "Sačuvaj draft"
    composeDiscard = "Odbaci"
    composeCancel = "Otkaži"

    draftsTitle = "Draftovi"
    draftsEmpty = "Nema draftova"
    draftsEmptySubtext = "Nedovršene poruke se čuvaju ovde."
    inboxDrafts = "Draftovi"

    searchPlaceholder = "Pretraži poštu"
    searchAllAccounts = "Svi nalozi"
    searchHasAttachment = "Sa prilogom"
    searchHint = "Pretraži svu sinhronizovanu poštu"
    searchNoResults = "Nema rezultata"
    searchResultsCountTemplate = "%d rezultata"

    detailReply = "Odgovori"
    detailForward = "Prosledi"
    detailMessageNotFound = "Poruka nije pronađena"
    detailNoSubject = "(bez naslova)"
    detailAttachmentsTemplate = "Prilozi (%d)"

    settingsTitle = "Podešavanja"
    settingsAccountsSection = "Nalozi"
    settingsNoAccounts = "Još nema naloga. Dodaj jedan da počneš sinhronizaciju pošte."
    settingsAddAccount = "Dodaj nalog"
    settingsEditAccount = "Izmeni"
    settingsRemoveAccount = "Ukloni"
    settingsRemoveAccountTitle = "Ukloni nalog?"
    settingsRemoveAccountMessageTemplate = "Ovo briše %s i zaustavlja sinhronizaciju. Lokalno keširana pošta se takođe uklanja."
    settingsRemove = "Ukloni"
    settingsCancel = "Otkaži"
    settingsSyncSection = "Sinhronizacija"
    settingsSyncSectionSubtitle = "Koliko često app proverava novu poštu"
    settingsSyncIntervalLabel = "Proveri novu poštu svakih"
    settingsSyncIntervalMinutesTemplate = "%d min"
    settingsSwipeActionsSection = "Swipe akcije"
    settingsSwipeActionsSectionSubtitle = "Šta se dešava kad prevučeš poruku levo ili desno"
    settingsSwipeLeft = "Swipe levo"
    settingsSwipeRight = "Swipe desno"
    swipeActionDelete = "Obriši"
    swipeActionMarkRead = "Označi kao pročitano"
    swipeActionMarkUnread = "Označi kao nepročitano"
    swipeActionNone = "Ništa"
    categoryEditorNewTitle = "Nova kategorija"
    categoryEditorEditTitle = "Izmeni kategoriju"
    categoryEditorNameLabel = "Naziv kategorije"
    categoryEditorMatchRulesLabel = "Pravila za uparivanje"
    categoryEditorFieldLabel = "Polje"
    categoryEditorMatchLabel = "Uslov"
    categoryEditorValueLabel = "Vrednost"
    categoryEditorAddRule = "Dodaj pravilo"
    categoryEditorSave = "Sačuvaj kategoriju"
    ruleFieldSender = "Pošiljalac"
    ruleFieldSubject = "Naslov"
    ruleFieldBody = "Sadržaj"
    ruleTypeContains = "Sadrži"
    ruleTypeEquals = "Jednako"
    ruleTypeStartsWith = "Počinje sa"
    settingsCategoriesSection = "Kategorije i pravila"
    settingsCategoriesSectionSubtitle = "Automatsko sortiranje pošte po pravilima"
    settingsNoCategories = "Još nema kategorija. Dodaj jednu da automatski sortiraš poštu po pošiljaocu, naslovu ili sadržaju."
    settingsAppearanceSection = "Izgled"
    settingsAppearanceSectionSubtitle = "Svetla, tamna ili automatska tema"
    settingsTheme = "Tema"
    settingsThemeSystem = "Auto"
    settingsThemeLight = "Svetla"
    settingsThemeDark = "Tamna"
    settingsSkinSection = "Boja teme"
    settingsSkinSectionSubtitle = "Izaberi paletu boja aplikacije"
    skinDefault = "Podrazumevana"
    skinOcean = "Okean"
    skinForest = "Šuma"
    skinRose = "Roze"
    skinGraphite = "Grafit"
    skinSand = "Pesak"
    skinCrimson = "Rubin"
    skinAurora = "Aurora"
    settingsLanguageSection = "Jezik"
    settingsLanguageSectionSubtitle = "Jezik korisničkog interfejsa aplikacije"
    settingsLanguage = "Jezik aplikacije"
    settingsMailSection = "Pošta"
    settingsMailSectionSubtitle = "Sinhronizacija, swipe akcije, kategorije i pravila"
    settingsNotificationsSection = "Obaveštenja"
    settingsNotificationsSectionSubtitle = "Uključi ili isključi obaveštenja o novoj pošti"
    settingsNotificationsToggleLabel = "Obaveštenja o novoj pošti"
    settingsNotificationsToggleSubtext = "Prikaži obaveštenje kada stigne nova poruka u Inbox"
    settingsNotificationsSoundToggleLabel = "Zvuk obaveštenja"
    settingsNotificationsSoundToggleSubtext = "Pusti zvuk uz obaveštenje o novoj pošti"
    settingsBatteryLabel = "Pošta ne stiže u pozadini?"
    settingsBatterySubtext = "Dozvoli aplikaciji rad u pozadini (isključi optimizaciju baterije)"
    settingsAppearanceLanguageSection = "Izgled i jezik"
    settingsAppearanceLanguageSectionSubtitle = "Tema i jezik aplikacije"
    settingsPrivacySection = "Privatnost"
    settingsPrivacySectionSubtitle = "Kako Tapiz Mail čuva tvoje podatke"
    settingsPrivacyParagraph1 = "Tapiz Mail nema svoj backend server. Aplikacija se povezuje direktno sa IMAP/SMTP serverom tvog naloga (Gmail, Outlook, univerzitetski mejl ili bilo koji drugi). Pošta nikad ne prolazi kroz servere Tapiz Labs-a niti bilo koje treće strane."
    settingsPrivacyParagraph2 = "Kredencijali naloga (lozinke) čuvaju se isključivo lokalno na uređaju, u sistemskom Android Keystore-u preko šifrovanog skladišta. Nikad u običnom tekstu, nikad u bazi podataka aplikacije, i nikad se ne šalju nikuda osim direktno tvom mejl provajderu radi autentifikacije."
    settingsPrivacyParagraph3 = "Poruke, prilozi i podešavanja (kategorije, pravila, swipe akcije) čuvaju se lokalno u bazi podataka na uređaju radi brzog pristupa van mreže. Ovi podaci se ne sinhronizuju ni na jedan Tapiz server niti bilo koju cloud uslugu. Ostaju na tvom telefonu."
    settingsPrivacyParagraph4 = "Aplikacija ne prikuplja analitiku niti telemetriju o tvom korišćenju i ne deli podatke sa oglašivačima ili trećim stranama. Jedina mrežna komunikacija je direktna IMAP/SMTP konekcija ka nalogu koji si sam podesio."
    settingsAboutSection = "O aplikaciji"
    settingsAppName = "Tapiz Mail"
    settingsAboutTagline = "Nezavisan mejl klijent, direktno sa telefona na tvoj IMAP/SMTP nalog, bez posrednika."
    settingsAboutVersion = "Verzija"
    settingsAboutPlatform = "Platforma"
    settingsAboutAuthor = "Autor"
    settingsCopyrightTemplate = "© %s Tapiz Labs. Sva prava zadržana."
    settingsAppVersionTemplate = "Verzija %s"
}

val EnStrings = Strings().apply {
    onboardingHeadline = "All your mail,\none inbox."
    onboardingSubtext = "Connect Gmail, Outlook, or any IMAP account and get one clean, " +
        "categorized inbox, synced in the background, on your terms."
    onboardingGetStarted = "Get Started"
    onboardingContinueWithGmail = "Continue with Gmail"
    onboardingContinueWithOutlook = "Continue with Outlook"
    onboardingOrConnectManually = "or connect manually"
    onboardingImapHost = "IMAP HOST"
    onboardingUsername = "USERNAME"

    languagePickerTitle = "Choose your language"
    languagePickerSubtitle = "You can change this later in settings."
    languageNameSerbian = "Srpski (latinica)"
    languageNameEnglish = "English"
    languageNameGerman = "Deutsch"
    languageNameSpanish = "Español"
    languageNameFrench = "Français"

    notifPermTitle = "Stay on top of your inbox"
    notifPermSubtext = "Get notified when new mail arrives. You can change this anytime in your phone's notification settings."
    notifPermAllow = "Allow notifications"
    notifPermSkip = "Not now"

    chooseProviderTitle = "Add account"
    chooseProviderSectionHeader = "Choose a provider"
    providerGmailDescription = "imap.gmail.com · smtp.gmail.com"
    providerOutlookDescription = "outlook.office365.com · smtp.office365.com"
    providerCustomTitle = "Custom (IMAP/SMTP)"
    providerCustomDescription = "Any other provider, including university/school mail"

    addAccountTitleNew = "Account details"
    addAccountTitleEdit = "Edit account"
    accountDetailsSectionHeader = "Account details"
    fieldDisplayName = "Display name"
    fieldEmailAddress = "Email address"
    fieldUsername = "Username"
    fieldUsernameHint = "Usually the same as your email address"
    fieldPassword = "Password"
    incomingMailSectionHeader = "Incoming mail (IMAP)"
    outgoingMailSectionHeader = "Outgoing mail (SMTP)"
    fieldImapHost = "IMAP host"
    fieldSmtpHost = "SMTP host"
    fieldPort = "Port"
    fieldSecurity = "Security"
    testConnection = "Test connection"
    testingConnection = "Testing…"
    connectionVerified = "Connection verified"
    connectionFailed = "Connection failed"
    verifyingSettings = "Verifying settings…"
    savingAccount = "Saving…"
    saveAccount = "Save account"

    inboxAccountsSynced = { count -> if (count == 1) "1 account" else "$count accounts" }
    inboxUnreadCountTemplate = "%d unread"
    inboxUnreadSubtext = "New mail syncs automatically in the background"
    inboxNoUnread = "No unread"
    inboxNoMessages = "No messages"
    inboxNoMessagesSubtext = "New mail will show up here once an account is synced."
    inboxAllAccounts = "All accounts"
    inboxAddAccount = "Add account"
    inboxToday = "Today"
    inboxYesterday = "Yesterday"
    inboxChipInbox = "Inbox"
    inboxChipSent = "Sent"
    inboxChipDrafts = "Drafts"
    inboxChipTrash = "Trash"
    trashEmptyAllLabel = "Empty trash"
    trashEmptyAllConfirmTitle = "Empty trash?"
    trashEmptyAllConfirmMessage = "This permanently deletes every message in the trash. This action cannot be undone."
    trashEmptyAllConfirmButton = "Empty"

    composeNewMessage = "New message"
    composeFrom = "From"
    composeTo = "To"
    composeCc = "Cc"
    composeBcc = "Bcc"
    composeSubject = "Subject"
    composeBodyPlaceholder = "Write your message…"
    composeDiscardTitle = "Discard message?"
    composeDiscardMessage = "You can save this as a draft and continue later, or discard it for good."
    composeSaveDraft = "Save draft"
    composeDiscard = "Discard"
    composeCancel = "Cancel"

    draftsTitle = "Drafts"
    draftsEmpty = "No drafts"
    draftsEmptySubtext = "Unfinished messages are kept here."
    inboxDrafts = "Drafts"

    searchPlaceholder = "Search mail"
    searchAllAccounts = "All accounts"
    searchHasAttachment = "Has attachment"
    searchHint = "Search across all your synced mail"
    searchNoResults = "No results"
    searchResultsCountTemplate = "%d results"

    detailReply = "Reply"
    detailForward = "Forward"
    detailMessageNotFound = "Message not found"
    detailNoSubject = "(no subject)"
    detailAttachmentsTemplate = "Attachments (%d)"

    settingsTitle = "Settings"
    settingsAccountsSection = "Accounts"
    settingsNoAccounts = "No accounts yet. Add one to start syncing mail."
    settingsAddAccount = "Add account"
    settingsEditAccount = "Edit"
    settingsRemoveAccount = "Remove"
    settingsRemoveAccountTitle = "Remove account?"
    settingsRemoveAccountMessageTemplate = "This deletes %s and stops syncing it. Locally cached mail is removed too."
    settingsRemove = "Remove"
    settingsCancel = "Cancel"
    settingsSyncSection = "Sync"
    settingsSyncSectionSubtitle = "How often the app checks for new mail"
    settingsSyncIntervalLabel = "Check for new mail every"
    settingsSyncIntervalMinutesTemplate = "%d min"
    settingsSwipeActionsSection = "Swipe actions"
    settingsSwipeActionsSectionSubtitle = "What happens when you swipe a message left or right"
    settingsSwipeLeft = "Swipe left"
    settingsSwipeRight = "Swipe right"
    swipeActionDelete = "Delete"
    swipeActionMarkRead = "Mark as read"
    swipeActionMarkUnread = "Mark as unread"
    swipeActionNone = "None"
    categoryEditorNewTitle = "New category"
    categoryEditorEditTitle = "Edit category"
    categoryEditorNameLabel = "Category name"
    categoryEditorMatchRulesLabel = "Match rules"
    categoryEditorFieldLabel = "Field"
    categoryEditorMatchLabel = "Match"
    categoryEditorValueLabel = "Value"
    categoryEditorAddRule = "Add rule"
    categoryEditorSave = "Save category"
    ruleFieldSender = "Sender"
    ruleFieldSubject = "Subject"
    ruleFieldBody = "Body"
    ruleTypeContains = "Contains"
    ruleTypeEquals = "Equals"
    ruleTypeStartsWith = "Starts with"
    settingsCategoriesSection = "Categories & rules"
    settingsCategoriesSectionSubtitle = "Auto-sort mail by your own rules"
    settingsNoCategories = "No categories yet. Add one to auto-sort mail by sender, subject, or body."
    settingsAppearanceSection = "Appearance"
    settingsAppearanceSectionSubtitle = "Light, dark, or automatic theme"
    settingsTheme = "Theme"
    settingsThemeSystem = "Auto"
    settingsThemeLight = "Light"
    settingsThemeDark = "Dark"
    settingsSkinSection = "Color skin"
    settingsSkinSectionSubtitle = "Choose the app's color palette"
    skinDefault = "Default"
    skinOcean = "Ocean"
    skinForest = "Forest"
    skinRose = "Rose"
    skinGraphite = "Graphite"
    skinSand = "Sand"
    skinCrimson = "Crimson"
    skinAurora = "Aurora"
    settingsLanguageSection = "Language"
    settingsLanguageSectionSubtitle = "The app's interface language"
    settingsLanguage = "App language"
    settingsMailSection = "Mail"
    settingsMailSectionSubtitle = "Sync, swipe actions, categories & rules"
    settingsNotificationsSection = "Notifications"
    settingsNotificationsSectionSubtitle = "Turn new-mail notifications on or off"
    settingsNotificationsToggleLabel = "New mail notifications"
    settingsNotificationsToggleSubtext = "Show a notification when new mail arrives in Inbox"
    settingsNotificationsSoundToggleLabel = "Notification sound"
    settingsNotificationsSoundToggleSubtext = "Play a sound with new-mail notifications"
    settingsBatteryLabel = "Mail not arriving in the background?"
    settingsBatterySubtext = "Allow the app to run in the background (disable battery optimization)"
    settingsAppearanceLanguageSection = "Appearance & language"
    settingsAppearanceLanguageSectionSubtitle = "Theme and app language"
    settingsPrivacySection = "Privacy"
    settingsPrivacySectionSubtitle = "How Tapiz Mail handles your data"
    settingsPrivacyParagraph1 = "Tapiz Mail has no backend server of its own. The app connects directly to your account's IMAP/SMTP server (Gmail, Outlook, your university mail, or any other). Your mail never passes through Tapiz Labs' servers or any third party."
    settingsPrivacyParagraph2 = "Account credentials (passwords) are stored only locally on your device, in the system Android Keystore via encrypted storage. Never in plain text, never in the app's database, and never sent anywhere except directly to your mail provider for authentication."
    settingsPrivacyParagraph3 = "Messages, attachments, and settings (categories, rules, swipe actions) are stored locally in an on-device database for fast offline access. This data is never synced to any Tapiz server or cloud service, it stays on your phone."
    settingsPrivacyParagraph4 = "The app collects no analytics or telemetry about your usage and shares no data with advertisers or third parties. The only network traffic is the direct IMAP/SMTP connection to the account you set up yourself."
    settingsAboutSection = "About"
    settingsAppName = "Tapiz Mail"
    settingsAboutTagline = "An independent mail client, straight from your phone to your IMAP/SMTP account, no middleman."
    settingsAboutVersion = "Version"
    settingsAboutPlatform = "Platform"
    settingsAboutAuthor = "Author"
    settingsCopyrightTemplate = "© %s Tapiz Labs. All rights reserved."
    settingsAppVersionTemplate = "Version %s"
}

val DeStrings = Strings().apply {
    onboardingHeadline = "Deine gesamte Post,\nein Posteingang."
    onboardingSubtext = "Verbinde Gmail, Outlook oder ein beliebiges IMAP-Konto und erhalte " +
        "einen übersichtlichen, kategorisierten Posteingang, im Hintergrund synchronisiert, ganz nach deinen Wünschen."
    onboardingGetStarted = "Loslegen"
    onboardingContinueWithGmail = "Mit Gmail fortfahren"
    onboardingContinueWithOutlook = "Mit Outlook fortfahren"
    onboardingOrConnectManually = "oder manuell verbinden"
    onboardingImapHost = "IMAP-HOST"
    onboardingUsername = "BENUTZERNAME"

    languagePickerTitle = "Wähle deine Sprache"
    languagePickerSubtitle = "Du kannst dies später in den Einstellungen ändern."
    languageNameSerbian = "Srpski (latinica)"
    languageNameEnglish = "English"
    languageNameGerman = "Deutsch"
    languageNameSpanish = "Español"
    languageNameFrench = "Français"

    notifPermTitle = "Bleib über deinen Posteingang informiert"
    notifPermSubtext = "Erhalte Benachrichtigungen, wenn neue Post eintrifft. Du kannst dies jederzeit in den Benachrichtigungseinstellungen deines Telefons ändern."
    notifPermAllow = "Benachrichtigungen erlauben"
    notifPermSkip = "Nicht jetzt"

    chooseProviderTitle = "Konto hinzufügen"
    chooseProviderSectionHeader = "Anbieter wählen"
    providerGmailDescription = "imap.gmail.com · smtp.gmail.com"
    providerOutlookDescription = "outlook.office365.com · smtp.office365.com"
    providerCustomTitle = "Benutzerdefiniert (IMAP/SMTP)"
    providerCustomDescription = "Jeder andere Anbieter, einschließlich Universitäts-/Schul-E-Mail"

    addAccountTitleNew = "Kontodetails"
    addAccountTitleEdit = "Konto bearbeiten"
    accountDetailsSectionHeader = "Kontodetails"
    fieldDisplayName = "Anzeigename"
    fieldEmailAddress = "E-Mail-Adresse"
    fieldUsername = "Benutzername"
    fieldUsernameHint = "Normalerweise identisch mit deiner E-Mail-Adresse"
    fieldPassword = "Passwort"
    incomingMailSectionHeader = "Eingehende Post (IMAP)"
    outgoingMailSectionHeader = "Ausgehende Post (SMTP)"
    fieldImapHost = "IMAP-Host"
    fieldSmtpHost = "SMTP-Host"
    fieldPort = "Port"
    fieldSecurity = "Sicherheit"
    testConnection = "Verbindung testen"
    testingConnection = "Wird getestet…"
    connectionVerified = "Verbindung bestätigt"
    connectionFailed = "Verbindung fehlgeschlagen"
    verifyingSettings = "Einstellungen werden geprüft…"
    savingAccount = "Wird gespeichert…"
    saveAccount = "Konto speichern"

    inboxAccountsSynced = { count -> if (count == 1) "1 Konto" else "$count Konten" }
    inboxUnreadCountTemplate = "%d ungelesen"
    inboxUnreadSubtext = "Neue E-Mails werden automatisch im Hintergrund synchronisiert"
    inboxNoUnread = "Keine ungelesenen"
    inboxNoMessages = "Keine Nachrichten"
    inboxNoMessagesSubtext = "Neue Post erscheint hier, sobald ein Konto synchronisiert wurde."
    inboxAllAccounts = "Alle Konten"
    inboxAddAccount = "Konto hinzufügen"
    inboxToday = "Heute"
    inboxYesterday = "Gestern"
    inboxChipInbox = "Posteingang"
    inboxChipSent = "Gesendet"
    inboxChipDrafts = "Entwürfe"
    inboxChipTrash = "Papierkorb"
    trashEmptyAllLabel = "Papierkorb leeren"
    trashEmptyAllConfirmTitle = "Papierkorb leeren?"
    trashEmptyAllConfirmMessage = "Dies löscht dauerhaft alle Nachrichten im Papierkorb. Diese Aktion kann nicht rückgängig gemacht werden."
    trashEmptyAllConfirmButton = "Leeren"

    composeNewMessage = "Neue Nachricht"
    composeFrom = "Von"
    composeTo = "An"
    composeCc = "Cc"
    composeBcc = "Bcc"
    composeSubject = "Betreff"
    composeBodyPlaceholder = "Schreibe deine Nachricht…"
    composeDiscardTitle = "Nachricht verwerfen?"
    composeDiscardMessage = "Du kannst sie als Entwurf speichern und später fortsetzen, oder endgültig verwerfen."
    composeSaveDraft = "Entwurf speichern"
    composeDiscard = "Verwerfen"
    composeCancel = "Abbrechen"

    draftsTitle = "Entwürfe"
    draftsEmpty = "Keine Entwürfe"
    draftsEmptySubtext = "Unfertige Nachrichten werden hier aufbewahrt."
    inboxDrafts = "Entwürfe"

    searchPlaceholder = "Mail durchsuchen"
    searchAllAccounts = "Alle Konten"
    searchHasAttachment = "Mit Anhang"
    searchHint = "Durchsuche deine gesamte synchronisierte Mail"
    searchNoResults = "Keine Ergebnisse"
    searchResultsCountTemplate = "%d Ergebnisse"

    detailReply = "Antworten"
    detailForward = "Weiterleiten"
    detailMessageNotFound = "Nachricht nicht gefunden"
    detailNoSubject = "(kein Betreff)"
    detailAttachmentsTemplate = "Anhänge (%d)"

    settingsTitle = "Einstellungen"
    settingsAccountsSection = "Konten"
    settingsNoAccounts = "Noch keine Konten, füge eines hinzu, um die Synchronisierung zu starten."
    settingsAddAccount = "Konto hinzufügen"
    settingsEditAccount = "Bearbeiten"
    settingsRemoveAccount = "Entfernen"
    settingsRemoveAccountTitle = "Konto entfernen?"
    settingsRemoveAccountMessageTemplate = "Dies löscht %s und beendet die Synchronisierung. Lokal zwischengespeicherte Post wird ebenfalls entfernt."
    settingsRemove = "Entfernen"
    settingsCancel = "Abbrechen"
    settingsSyncSection = "Synchronisierung"
    settingsSyncSectionSubtitle = "Wie oft die App nach neuer Post sucht"
    settingsSyncIntervalLabel = "Auf neue Post prüfen alle"
    settingsSyncIntervalMinutesTemplate = "%d Min."
    settingsSwipeActionsSection = "Wischaktionen"
    settingsSwipeActionsSectionSubtitle = "Was beim Wischen einer Nachricht nach links oder rechts passiert"
    settingsSwipeLeft = "Nach links wischen"
    settingsSwipeRight = "Nach rechts wischen"
    swipeActionDelete = "Löschen"
    swipeActionMarkRead = "Als gelesen markieren"
    swipeActionMarkUnread = "Als ungelesen markieren"
    swipeActionNone = "Keine"
    categoryEditorNewTitle = "Neue Kategorie"
    categoryEditorEditTitle = "Kategorie bearbeiten"
    categoryEditorNameLabel = "Kategoriename"
    categoryEditorMatchRulesLabel = "Zuordnungsregeln"
    categoryEditorFieldLabel = "Feld"
    categoryEditorMatchLabel = "Bedingung"
    categoryEditorValueLabel = "Wert"
    categoryEditorAddRule = "Regel hinzufügen"
    categoryEditorSave = "Kategorie speichern"
    ruleFieldSender = "Absender"
    ruleFieldSubject = "Betreff"
    ruleFieldBody = "Inhalt"
    ruleTypeContains = "Enthält"
    ruleTypeEquals = "Ist gleich"
    ruleTypeStartsWith = "Beginnt mit"
    settingsCategoriesSection = "Kategorien & Regeln"
    settingsCategoriesSectionSubtitle = "Post automatisch nach deinen eigenen Regeln sortieren"
    settingsNoCategories = "Noch keine Kategorien, füge eine hinzu, um Post automatisch nach Absender, Betreff oder Inhalt zu sortieren."
    settingsAppearanceSection = "Erscheinungsbild"
    settingsAppearanceSectionSubtitle = "Helles, dunkles oder automatisches Design"
    settingsTheme = "Design"
    settingsThemeSystem = "Auto"
    settingsThemeLight = "Hell"
    settingsThemeDark = "Dunkel"
    settingsSkinSection = "Farbschema"
    settingsSkinSectionSubtitle = "Farbpalette der App wählen"
    skinDefault = "Standard"
    skinOcean = "Ozean"
    skinForest = "Wald"
    skinRose = "Rosé"
    skinGraphite = "Graphit"
    skinSand = "Sand"
    skinCrimson = "Karmesin"
    skinAurora = "Aurora"
    settingsLanguageSection = "Sprache"
    settingsLanguageSectionSubtitle = "Die Sprache der App-Oberfläche"
    settingsLanguage = "App-Sprache"
    settingsMailSection = "Mail"
    settingsMailSectionSubtitle = "Synchronisierung, Swipe-Aktionen, Kategorien & Regeln"
    settingsNotificationsSection = "Benachrichtigungen"
    settingsNotificationsSectionSubtitle = "Benachrichtigungen für neue Post ein- oder ausschalten"
    settingsNotificationsToggleLabel = "Benachrichtigungen für neue Post"
    settingsNotificationsToggleSubtext = "Benachrichtigung anzeigen, wenn neue Post im Posteingang eintrifft"
    settingsNotificationsSoundToggleLabel = "Benachrichtigungston"
    settingsNotificationsSoundToggleSubtext = "Bei neuen E-Mail-Benachrichtigungen einen Ton abspielen"
    settingsBatteryLabel = "Kommen E-Mails im Hintergrund nicht an?"
    settingsBatterySubtext = "Erlaube der App, im Hintergrund zu laufen (Akku-Optimierung deaktivieren)"
    settingsAppearanceLanguageSection = "Erscheinungsbild & Sprache"
    settingsAppearanceLanguageSectionSubtitle = "Design und Sprache der App"
    settingsPrivacySection = "Datenschutz"
    settingsPrivacySectionSubtitle = "Wie Tapiz Mail mit deinen Daten umgeht"
    settingsPrivacyParagraph1 = "Tapiz Mail hat keinen eigenen Backend-Server. Die App verbindet sich direkt mit dem IMAP/SMTP-Server deines Kontos (Gmail, Outlook, deine Universitäts-Mail oder jedes andere), deine Post läuft nie über Server von Tapiz Labs oder Dritten."
    settingsPrivacyParagraph2 = "Kontodaten (Passwörter) werden ausschließlich lokal auf deinem Gerät gespeichert, im System-Android-Keystore über verschlüsselten Speicher, nie im Klartext, nie in der Datenbank der App, und nie irgendwohin gesendet außer direkt an deinen Mail-Anbieter zur Authentifizierung."
    settingsPrivacyParagraph3 = "Nachrichten, Anhänge und Einstellungen (Kategorien, Regeln, Swipe-Aktionen) werden lokal in einer Datenbank auf dem Gerät gespeichert, für schnellen Offline-Zugriff. Diese Daten werden nie mit einem Tapiz-Server oder Cloud-Dienst synchronisiert, sie bleiben auf deinem Telefon."
    settingsPrivacyParagraph4 = "Die App sammelt keine Analyse- oder Telemetriedaten über deine Nutzung und teilt keine Daten mit Werbetreibenden oder Dritten. Der einzige Netzwerkverkehr ist die direkte IMAP/SMTP-Verbindung zu dem Konto, das du selbst eingerichtet hast."
    settingsAboutSection = "Über die App"
    settingsAppName = "Tapiz Mail"
    settingsAboutTagline = "Ein unabhängiger Mail-Client, direkt von deinem Telefon zu deinem IMAP/SMTP-Konto, ohne Mittelsmann."
    settingsAboutVersion = "Version"
    settingsAboutPlatform = "Plattform"
    settingsAboutAuthor = "Autor"
    settingsCopyrightTemplate = "© %s Tapiz Labs. Alle Rechte vorbehalten."
    settingsAppVersionTemplate = "Version %s"
}

val EsStrings = Strings().apply {
    onboardingHeadline = "Todo tu correo,\nuna sola bandeja."
    onboardingSubtext = "Conecta Gmail, Outlook o cualquier cuenta IMAP y obtén una bandeja " +
        "de entrada limpia y organizada, sincronizada en segundo plano, a tu manera."
    onboardingGetStarted = "Comenzar"
    onboardingContinueWithGmail = "Continuar con Gmail"
    onboardingContinueWithOutlook = "Continuar con Outlook"
    onboardingOrConnectManually = "o conectar manualmente"
    onboardingImapHost = "HOST IMAP"
    onboardingUsername = "NOMBRE DE USUARIO"

    languagePickerTitle = "Elige tu idioma"
    languagePickerSubtitle = "Puedes cambiarlo más tarde en los ajustes."
    languageNameSerbian = "Srpski (latinica)"
    languageNameEnglish = "English"
    languageNameGerman = "Deutsch"
    languageNameSpanish = "Español"
    languageNameFrench = "Français"

    notifPermTitle = "Mantente al tanto de tu bandeja de entrada"
    notifPermSubtext = "Recibe notificaciones cuando llegue correo nuevo. Puedes cambiar esto en cualquier momento en la configuración de notificaciones de tu teléfono."
    notifPermAllow = "Permitir notificaciones"
    notifPermSkip = "Ahora no"

    chooseProviderTitle = "Agregar cuenta"
    chooseProviderSectionHeader = "Elige un proveedor"
    providerGmailDescription = "imap.gmail.com · smtp.gmail.com"
    providerOutlookDescription = "outlook.office365.com · smtp.office365.com"
    providerCustomTitle = "Personalizado (IMAP/SMTP)"
    providerCustomDescription = "Cualquier otro proveedor, incluyendo correo universitario/escolar"

    addAccountTitleNew = "Detalles de la cuenta"
    addAccountTitleEdit = "Editar cuenta"
    accountDetailsSectionHeader = "Detalles de la cuenta"
    fieldDisplayName = "Nombre para mostrar"
    fieldEmailAddress = "Dirección de correo"
    fieldUsername = "Nombre de usuario"
    fieldUsernameHint = "Generalmente igual a tu dirección de correo"
    fieldPassword = "Contraseña"
    incomingMailSectionHeader = "Correo entrante (IMAP)"
    outgoingMailSectionHeader = "Correo saliente (SMTP)"
    fieldImapHost = "Host IMAP"
    fieldSmtpHost = "Host SMTP"
    fieldPort = "Puerto"
    fieldSecurity = "Seguridad"
    testConnection = "Probar conexión"
    testingConnection = "Probando…"
    connectionVerified = "Conexión verificada"
    connectionFailed = "Conexión fallida"
    verifyingSettings = "Verificando configuración…"
    savingAccount = "Guardando…"
    saveAccount = "Guardar cuenta"

    inboxAccountsSynced = { count -> if (count == 1) "1 cuenta" else "$count cuentas" }
    inboxUnreadCountTemplate = "%d sin leer"
    inboxUnreadSubtext = "El correo nuevo se sincroniza automáticamente en segundo plano"
    inboxNoUnread = "Sin no leídos"
    inboxNoMessages = "Sin mensajes"
    inboxNoMessagesSubtext = "El correo nuevo aparecerá aquí una vez que se sincronice una cuenta."
    inboxAllAccounts = "Todas las cuentas"
    inboxAddAccount = "Agregar cuenta"
    inboxToday = "Hoy"
    inboxYesterday = "Ayer"
    inboxChipInbox = "Bandeja"
    inboxChipSent = "Enviados"
    inboxChipDrafts = "Borradores"
    inboxChipTrash = "Papelera"
    trashEmptyAllLabel = "Vaciar papelera"
    trashEmptyAllConfirmTitle = "¿Vaciar la papelera?"
    trashEmptyAllConfirmMessage = "Esto elimina permanentemente todos los mensajes de la papelera. Esta acción no se puede deshacer."
    trashEmptyAllConfirmButton = "Vaciar"

    composeNewMessage = "Nuevo mensaje"
    composeFrom = "De"
    composeTo = "Para"
    composeCc = "Cc"
    composeBcc = "Cco"
    composeSubject = "Asunto"
    composeBodyPlaceholder = "Escribe tu mensaje…"
    composeDiscardTitle = "¿Descartar mensaje?"
    composeDiscardMessage = "Puedes guardarlo como borrador y continuar más tarde, o descartarlo definitivamente."
    composeSaveDraft = "Guardar borrador"
    composeDiscard = "Descartar"
    composeCancel = "Cancelar"

    draftsTitle = "Borradores"
    draftsEmpty = "Sin borradores"
    draftsEmptySubtext = "Los mensajes sin terminar se guardan aquí."
    inboxDrafts = "Borradores"

    searchPlaceholder = "Buscar correo"
    searchAllAccounts = "Todas las cuentas"
    searchHasAttachment = "Con adjunto"
    searchHint = "Busca en todo tu correo sincronizado"
    searchNoResults = "Sin resultados"
    searchResultsCountTemplate = "%d resultados"

    detailReply = "Responder"
    detailForward = "Reenviar"
    detailMessageNotFound = "Mensaje no encontrado"
    detailNoSubject = "(sin asunto)"
    detailAttachmentsTemplate = "Adjuntos (%d)"

    settingsTitle = "Ajustes"
    settingsAccountsSection = "Cuentas"
    settingsNoAccounts = "Aún no hay cuentas, agrega una para empezar a sincronizar correo."
    settingsAddAccount = "Agregar cuenta"
    settingsEditAccount = "Editar"
    settingsRemoveAccount = "Eliminar"
    settingsRemoveAccountTitle = "¿Eliminar cuenta?"
    settingsRemoveAccountMessageTemplate = "Esto elimina %s y detiene su sincronización. El correo almacenado localmente también se elimina."
    settingsRemove = "Eliminar"
    settingsCancel = "Cancelar"
    settingsSyncSection = "Sincronización"
    settingsSyncSectionSubtitle = "Con qué frecuencia la app busca correo nuevo"
    settingsSyncIntervalLabel = "Buscar correo nuevo cada"
    settingsSyncIntervalMinutesTemplate = "%d min"
    settingsSwipeActionsSection = "Acciones de deslizamiento"
    settingsSwipeActionsSectionSubtitle = "Qué ocurre al deslizar un mensaje a la izquierda o derecha"
    settingsSwipeLeft = "Deslizar a la izquierda"
    settingsSwipeRight = "Deslizar a la derecha"
    swipeActionDelete = "Eliminar"
    swipeActionMarkRead = "Marcar como leído"
    swipeActionMarkUnread = "Marcar como no leído"
    swipeActionNone = "Ninguna"
    categoryEditorNewTitle = "Nueva categoría"
    categoryEditorEditTitle = "Editar categoría"
    categoryEditorNameLabel = "Nombre de la categoría"
    categoryEditorMatchRulesLabel = "Reglas de coincidencia"
    categoryEditorFieldLabel = "Campo"
    categoryEditorMatchLabel = "Condición"
    categoryEditorValueLabel = "Valor"
    categoryEditorAddRule = "Agregar regla"
    categoryEditorSave = "Guardar categoría"
    ruleFieldSender = "Remitente"
    ruleFieldSubject = "Asunto"
    ruleFieldBody = "Contenido"
    ruleTypeContains = "Contiene"
    ruleTypeEquals = "Es igual a"
    ruleTypeStartsWith = "Comienza con"
    settingsCategoriesSection = "Categorías y reglas"
    settingsCategoriesSectionSubtitle = "Ordena el correo automáticamente según tus propias reglas"
    settingsNoCategories = "Aún no hay categorías, agrega una para ordenar automáticamente el correo por remitente, asunto o contenido."
    settingsAppearanceSection = "Apariencia"
    settingsAppearanceSectionSubtitle = "Tema claro, oscuro o automático"
    settingsTheme = "Tema"
    settingsThemeSystem = "Auto"
    settingsThemeLight = "Claro"
    settingsThemeDark = "Oscuro"
    settingsSkinSection = "Combinación de colores"
    settingsSkinSectionSubtitle = "Elige la paleta de colores de la app"
    skinDefault = "Predeterminado"
    skinOcean = "Océano"
    skinForest = "Bosque"
    skinRose = "Rosa"
    skinGraphite = "Grafito"
    skinSand = "Arena"
    skinCrimson = "Carmesí"
    skinAurora = "Aurora"
    settingsLanguageSection = "Idioma"
    settingsLanguageSectionSubtitle = "El idioma de la interfaz de la app"
    settingsLanguage = "Idioma de la app"
    settingsMailSection = "Correo"
    settingsMailSectionSubtitle = "Sincronización, acciones de deslizamiento, categorías y reglas"
    settingsNotificationsSection = "Notificaciones"
    settingsNotificationsSectionSubtitle = "Activa o desactiva las notificaciones de correo nuevo"
    settingsNotificationsToggleLabel = "Notificaciones de correo nuevo"
    settingsNotificationsToggleSubtext = "Mostrar una notificación cuando llegue correo nuevo a la bandeja de entrada"
    settingsNotificationsSoundToggleLabel = "Sonido de notificación"
    settingsNotificationsSoundToggleSubtext = "Reproducir un sonido con las notificaciones de correo nuevo"
    settingsBatteryLabel = "¿No llegan los correos en segundo plano?"
    settingsBatterySubtext = "Permite que la app se ejecute en segundo plano (desactiva la optimización de batería)"
    settingsAppearanceLanguageSection = "Apariencia e idioma"
    settingsAppearanceLanguageSectionSubtitle = "Tema e idioma de la app"
    settingsPrivacySection = "Privacidad"
    settingsPrivacySectionSubtitle = "Cómo Tapiz Mail maneja tus datos"
    settingsPrivacyParagraph1 = "Tapiz Mail no tiene servidor backend propio. La app se conecta directamente al servidor IMAP/SMTP de tu cuenta (Gmail, Outlook, tu correo universitario o cualquier otro), tu correo nunca pasa por servidores de Tapiz Labs ni de terceros."
    settingsPrivacyParagraph2 = "Las credenciales de la cuenta (contraseñas) se guardan solo localmente en tu dispositivo, en el Keystore de Android mediante almacenamiento cifrado, nunca en texto plano, nunca en la base de datos de la app, y nunca se envían a ningún sitio salvo directamente a tu proveedor de correo para autenticación."
    settingsPrivacyParagraph3 = "Los mensajes, adjuntos y ajustes (categorías, reglas, acciones de deslizamiento) se guardan localmente en una base de datos del dispositivo para acceso rápido sin conexión. Estos datos nunca se sincronizan con ningún servidor de Tapiz ni servicio en la nube, permanecen en tu teléfono."
    settingsPrivacyParagraph4 = "La app no recopila análisis ni telemetría sobre tu uso y no comparte datos con anunciantes ni terceros. El único tráfico de red es la conexión IMAP/SMTP directa a la cuenta que configuraste tú mismo."
    settingsAboutSection = "Acerca de"
    settingsAppName = "Tapiz Mail"
    settingsAboutTagline = "Un cliente de correo independiente, directo desde tu teléfono a tu cuenta IMAP/SMTP, sin intermediarios."
    settingsAboutVersion = "Versión"
    settingsAboutPlatform = "Plataforma"
    settingsAboutAuthor = "Autor"
    settingsCopyrightTemplate = "© %s Tapiz Labs. Todos los derechos reservados."
    settingsAppVersionTemplate = "Versión %s"
}

val FrStrings = Strings().apply {
    onboardingHeadline = "Tout ton courrier,\nune seule boîte de réception."
    onboardingSubtext = "Connecte Gmail, Outlook ou n'importe quel compte IMAP et profite d'une " +
        "boîte de réception claire et organisée, synchronisée en arrière-plan, à ta façon."
    onboardingGetStarted = "Commencer"
    onboardingContinueWithGmail = "Continuer avec Gmail"
    onboardingContinueWithOutlook = "Continuer avec Outlook"
    onboardingOrConnectManually = "ou se connecter manuellement"
    onboardingImapHost = "HÔTE IMAP"
    onboardingUsername = "NOM D'UTILISATEUR"

    languagePickerTitle = "Choisissez votre langue"
    languagePickerSubtitle = "Vous pourrez la changer plus tard dans les paramètres."
    languageNameSerbian = "Srpski (latinica)"
    languageNameEnglish = "English"
    languageNameGerman = "Deutsch"
    languageNameSpanish = "Español"
    languageNameFrench = "Français"

    notifPermTitle = "Reste informé de ta boîte de réception"
    notifPermSubtext = "Reçois une notification à l'arrivée d'un nouveau courrier. Tu peux modifier ceci à tout moment dans les paramètres de notification de ton téléphone."
    notifPermAllow = "Autoriser les notifications"
    notifPermSkip = "Pas maintenant"

    chooseProviderTitle = "Ajouter un compte"
    chooseProviderSectionHeader = "Choisir un fournisseur"
    providerGmailDescription = "imap.gmail.com · smtp.gmail.com"
    providerOutlookDescription = "outlook.office365.com · smtp.office365.com"
    providerCustomTitle = "Personnalisé (IMAP/SMTP)"
    providerCustomDescription = "Tout autre fournisseur, y compris le courrier universitaire/scolaire"

    addAccountTitleNew = "Détails du compte"
    addAccountTitleEdit = "Modifier le compte"
    accountDetailsSectionHeader = "Détails du compte"
    fieldDisplayName = "Nom d'affichage"
    fieldEmailAddress = "Adresse e-mail"
    fieldUsername = "Nom d'utilisateur"
    fieldUsernameHint = "Généralement identique à ton adresse e-mail"
    fieldPassword = "Mot de passe"
    incomingMailSectionHeader = "Courrier entrant (IMAP)"
    outgoingMailSectionHeader = "Courrier sortant (SMTP)"
    fieldImapHost = "Hôte IMAP"
    fieldSmtpHost = "Hôte SMTP"
    fieldPort = "Port"
    fieldSecurity = "Sécurité"
    testConnection = "Tester la connexion"
    testingConnection = "Test en cours…"
    connectionVerified = "Connexion vérifiée"
    connectionFailed = "Échec de la connexion"
    verifyingSettings = "Vérification des paramètres…"
    savingAccount = "Enregistrement…"
    saveAccount = "Enregistrer le compte"

    inboxAccountsSynced = { count -> if (count == 1) "1 compte" else "$count comptes" }
    inboxUnreadCountTemplate = "%d non lus"
    inboxUnreadSubtext = "Les nouveaux messages se synchronisent automatiquement en arrière-plan"
    inboxNoUnread = "Aucun non lu"
    inboxNoMessages = "Aucun message"
    inboxNoMessagesSubtext = "Le nouveau courrier apparaîtra ici une fois un compte synchronisé."
    inboxAllAccounts = "Tous les comptes"
    inboxAddAccount = "Ajouter un compte"
    inboxToday = "Aujourd'hui"
    inboxYesterday = "Hier"
    inboxChipInbox = "Boîte de réception"
    inboxChipSent = "Envoyés"
    inboxChipDrafts = "Brouillons"
    inboxChipTrash = "Corbeille"
    trashEmptyAllLabel = "Vider la corbeille"
    trashEmptyAllConfirmTitle = "Vider la corbeille ?"
    trashEmptyAllConfirmMessage = "Cela supprime définitivement tous les messages de la corbeille. Cette action est irréversible."
    trashEmptyAllConfirmButton = "Vider"

    composeNewMessage = "Nouveau message"
    composeFrom = "De"
    composeTo = "À"
    composeCc = "Cc"
    composeBcc = "Cci"
    composeSubject = "Objet"
    composeBodyPlaceholder = "Écris ton message…"
    composeDiscardTitle = "Abandonner le message ?"
    composeDiscardMessage = "Tu peux l'enregistrer comme brouillon et continuer plus tard, ou l'abandonner définitivement."
    composeSaveDraft = "Enregistrer le brouillon"
    composeDiscard = "Abandonner"
    composeCancel = "Annuler"

    draftsTitle = "Brouillons"
    draftsEmpty = "Aucun brouillon"
    draftsEmptySubtext = "Les messages inachevés sont conservés ici."
    inboxDrafts = "Brouillons"

    searchPlaceholder = "Rechercher dans les mails"
    searchAllAccounts = "Tous les comptes"
    searchHasAttachment = "Avec pièce jointe"
    searchHint = "Recherche dans tous tes mails synchronisés"
    searchNoResults = "Aucun résultat"
    searchResultsCountTemplate = "%d résultats"

    detailReply = "Répondre"
    detailForward = "Transférer"
    detailMessageNotFound = "Message introuvable"
    detailNoSubject = "(sans objet)"
    detailAttachmentsTemplate = "Pièces jointes (%d)"

    settingsTitle = "Paramètres"
    settingsAccountsSection = "Comptes"
    settingsNoAccounts = "Aucun compte pour l'instant, ajoutes-en un pour commencer à synchroniser le courrier."
    settingsAddAccount = "Ajouter un compte"
    settingsEditAccount = "Modifier"
    settingsRemoveAccount = "Supprimer"
    settingsRemoveAccountTitle = "Supprimer le compte ?"
    settingsRemoveAccountMessageTemplate = "Cela supprime %s et arrête sa synchronisation. Le courrier mis en cache localement est également supprimé."
    settingsRemove = "Supprimer"
    settingsCancel = "Annuler"
    settingsSyncSection = "Synchronisation"
    settingsSyncSectionSubtitle = "Fréquence de vérification du nouveau courrier"
    settingsSyncIntervalLabel = "Vérifier le nouveau courrier toutes les"
    settingsSyncIntervalMinutesTemplate = "%d min"
    settingsSwipeActionsSection = "Actions de balayage"
    settingsSwipeActionsSectionSubtitle = "Ce qui se passe en balayant un message à gauche ou à droite"
    settingsSwipeLeft = "Balayer à gauche"
    settingsSwipeRight = "Balayer à droite"
    swipeActionDelete = "Supprimer"
    swipeActionMarkRead = "Marquer comme lu"
    swipeActionMarkUnread = "Marquer comme non lu"
    swipeActionNone = "Aucune"
    categoryEditorNewTitle = "Nouvelle catégorie"
    categoryEditorEditTitle = "Modifier la catégorie"
    categoryEditorNameLabel = "Nom de la catégorie"
    categoryEditorMatchRulesLabel = "Règles de correspondance"
    categoryEditorFieldLabel = "Champ"
    categoryEditorMatchLabel = "Condition"
    categoryEditorValueLabel = "Valeur"
    categoryEditorAddRule = "Ajouter une règle"
    categoryEditorSave = "Enregistrer la catégorie"
    ruleFieldSender = "Expéditeur"
    ruleFieldSubject = "Objet"
    ruleFieldBody = "Contenu"
    ruleTypeContains = "Contient"
    ruleTypeEquals = "Est égal à"
    ruleTypeStartsWith = "Commence par"
    settingsCategoriesSection = "Catégories et règles"
    settingsCategoriesSectionSubtitle = "Trie automatiquement le courrier selon tes propres règles"
    settingsNoCategories = "Aucune catégorie pour l'instant, ajoutes-en une pour trier automatiquement le courrier par expéditeur, objet ou contenu."
    settingsAppearanceSection = "Apparence"
    settingsAppearanceSectionSubtitle = "Thème clair, sombre ou automatique"
    settingsTheme = "Thème"
    settingsThemeSystem = "Auto"
    settingsThemeLight = "Clair"
    settingsThemeDark = "Sombre"
    settingsSkinSection = "Palette de couleurs"
    settingsSkinSectionSubtitle = "Choisir la palette de couleurs de l'application"
    skinDefault = "Par défaut"
    skinOcean = "Océan"
    skinForest = "Forêt"
    skinRose = "Rose"
    skinGraphite = "Graphite"
    skinSand = "Sable"
    skinCrimson = "Cramoisi"
    skinAurora = "Aurora"
    settingsLanguageSection = "Langue"
    settingsLanguageSectionSubtitle = "La langue de l'interface de l'application"
    settingsLanguage = "Langue de l'application"
    settingsMailSection = "Mail"
    settingsMailSectionSubtitle = "Synchronisation, actions de balayage, catégories et règles"
    settingsNotificationsSection = "Notifications"
    settingsNotificationsSectionSubtitle = "Activer ou désactiver les notifications de nouveaux messages"
    settingsNotificationsToggleLabel = "Notifications de nouveaux messages"
    settingsNotificationsToggleSubtext = "Afficher une notification à l'arrivée d'un nouveau message dans la boîte de réception"
    settingsNotificationsSoundToggleLabel = "Son de notification"
    settingsNotificationsSoundToggleSubtext = "Jouer un son avec les notifications de nouveaux messages"
    settingsBatteryLabel = "Les e-mails n'arrivent pas en arrière-plan ?"
    settingsBatterySubtext = "Autoriser l'app à s'exécuter en arrière-plan (désactiver l'optimisation de la batterie)"
    settingsAppearanceLanguageSection = "Apparence et langue"
    settingsAppearanceLanguageSectionSubtitle = "Thème et langue de l'application"
    settingsPrivacySection = "Confidentialité"
    settingsPrivacySectionSubtitle = "Comment Tapiz Mail traite tes données"
    settingsPrivacyParagraph1 = "Tapiz Mail n'a pas de serveur backend propre. L'application se connecte directement au serveur IMAP/SMTP de ton compte (Gmail, Outlook, ta messagerie universitaire ou toute autre), ton courrier ne passe jamais par les serveurs de Tapiz Labs ni par un tiers."
    settingsPrivacyParagraph2 = "Les identifiants du compte (mots de passe) sont stockés uniquement en local sur ton appareil, dans le Keystore Android via un stockage chiffré, jamais en texte clair, jamais dans la base de données de l'application, et jamais envoyés ailleurs que directement à ton fournisseur de messagerie pour l'authentification."
    settingsPrivacyParagraph3 = "Les messages, pièces jointes et réglages (catégories, règles, actions de balayage) sont stockés localement dans une base de données sur l'appareil pour un accès rapide hors ligne. Ces données ne sont jamais synchronisées avec un serveur Tapiz ou un service cloud, elles restent sur ton téléphone."
    settingsPrivacyParagraph4 = "L'application ne collecte aucune donnée d'analyse ni de télémétrie sur ton utilisation et ne partage aucune donnée avec des annonceurs ou des tiers. Le seul trafic réseau est la connexion IMAP/SMTP directe vers le compte que tu as toi-même configuré."
    settingsAboutSection = "À propos"
    settingsAppName = "Tapiz Mail"
    settingsAboutTagline = "Un client mail indépendant, directement de ton téléphone vers ton compte IMAP/SMTP, sans intermédiaire."
    settingsAboutVersion = "Version"
    settingsAboutPlatform = "Plateforme"
    settingsAboutAuthor = "Auteur"
    settingsCopyrightTemplate = "© %s Tapiz Labs. Tous droits réservés."
    settingsAppVersionTemplate = "Version %s"
}

fun stringsFor(language: AppLanguage): Strings = when (language) {
    AppLanguage.SR -> SrStrings
    AppLanguage.EN -> EnStrings
    AppLanguage.DE -> DeStrings
    AppLanguage.ES -> EsStrings
    AppLanguage.FR -> FrStrings
}

/** The active dictionary. Defaults to Serbian; overridden at the app root. */
val LocalStrings = staticCompositionLocalOf { SrStrings }

/** The active UI language itself (not just its [Strings] dictionary) — needed wherever a
 * `java.time`/`java.util.Locale`-aware formatter (relative timestamps, date labels) must
 * follow the in-app language selection instead of the device's system locale, which may
 * differ from what the user picked in this app's own language picker. Provided alongside
 * [LocalStrings] at the app root (RootNavigation). */
val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.SR }

/**
 * Snapshot of the active dictionary for non-composable layers (ViewModel /
 * repository validation messages). Kept in sync at the app root (RootNavigation)
 * whenever the language changes.
 */
object CurrentStrings {
    @Volatile
    var value: Strings = SrStrings
}
