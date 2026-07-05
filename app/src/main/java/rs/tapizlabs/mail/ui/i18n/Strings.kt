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
    /** `%d` placeholder for the account count — use [inboxAccountsSynced]. */
    lateinit var inboxAccountsSyncedTemplate: String
    fun inboxAccountsSynced(count: Int): String = inboxAccountsSyncedTemplate.format(count)
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

    // Compose
    lateinit var composeNewMessage: String
    lateinit var composeTo: String
    lateinit var composeCc: String
    lateinit var composeBcc: String
    lateinit var composeSubject: String
    lateinit var composeBodyPlaceholder: String

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
    lateinit var settingsSyncIntervalLabel: String
    lateinit var settingsSyncIntervalMinutesTemplate: String
    fun settingsSyncIntervalMinutes(minutes: Int): String = settingsSyncIntervalMinutesTemplate.format(minutes)
    lateinit var settingsSwipeActionsSection: String
    lateinit var settingsSwipeLeft: String
    lateinit var settingsSwipeRight: String
    lateinit var settingsCategoriesSection: String
    lateinit var settingsNoCategories: String
    lateinit var settingsAppearanceSection: String
    lateinit var settingsTheme: String
    lateinit var settingsThemeSystem: String
    lateinit var settingsThemeLight: String
    lateinit var settingsThemeDark: String
}

val SrStrings = Strings().apply {
    onboardingHeadline = "Sva tvoja pošta,\njedno prijemno sanduče."
    onboardingSubtext = "Poveži Gmail, Outlook ili bilo koji IMAP nalog i dobij jedno " +
        "uredno, kategorisano prijemno sanduče — sinhronizovano u pozadini, po tvojim uslovima."
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
    providerCustomDescription = "Bilo koji drugi provajder — uključujući univerzitetsku/školsku poštu"

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

    inboxAccountsSyncedTemplate = "%d naloga sinhronizovano"
    inboxUnreadCountTemplate = "%d nepročitano"
    inboxUnreadSubtext = "Automatski sortirano po tvojim pravilima"
    inboxNoUnread = "Nema nepročitanih"
    inboxNoMessages = "Nema poruka"
    inboxNoMessagesSubtext = "Nova pošta će se pojaviti ovde nakon sinhronizacije naloga."
    inboxAllAccounts = "Svi nalozi"
    inboxAddAccount = "Dodaj nalog"
    inboxToday = "Danas"
    inboxYesterday = "Juče"

    composeNewMessage = "Nova poruka"
    composeTo = "Prima"
    composeCc = "Cc"
    composeBcc = "Bcc"
    composeSubject = "Naslov"
    composeBodyPlaceholder = "Napiši poruku…"

    detailReply = "Odgovori"
    detailForward = "Prosledi"
    detailMessageNotFound = "Poruka nije pronađena"
    detailNoSubject = "(bez naslova)"
    detailAttachmentsTemplate = "Prilozi (%d)"

    settingsTitle = "Podešavanja"
    settingsAccountsSection = "Nalozi"
    settingsNoAccounts = "Još nema naloga — dodaj jedan da počneš sinhronizaciju pošte."
    settingsAddAccount = "Dodaj nalog"
    settingsEditAccount = "Izmeni"
    settingsRemoveAccount = "Ukloni"
    settingsRemoveAccountTitle = "Ukloni nalog?"
    settingsRemoveAccountMessageTemplate = "Ovo briše %s i zaustavlja sinhronizaciju. Lokalno keširana pošta se takođe uklanja."
    settingsRemove = "Ukloni"
    settingsCancel = "Otkaži"
    settingsSyncSection = "Sinhronizacija"
    settingsSyncIntervalLabel = "Proveri novu poštu svakih"
    settingsSyncIntervalMinutesTemplate = "%d min"
    settingsSwipeActionsSection = "Swipe akcije"
    settingsSwipeLeft = "Swipe levo"
    settingsSwipeRight = "Swipe desno"
    settingsCategoriesSection = "Kategorije i pravila"
    settingsNoCategories = "Još nema kategorija — dodaj jednu da automatski sortiraš poštu po pošiljaocu, naslovu ili sadržaju."
    settingsAppearanceSection = "Izgled"
    settingsTheme = "Tema"
    settingsThemeSystem = "Sistemska"
    settingsThemeLight = "Svetla"
    settingsThemeDark = "Tamna"
}

val EnStrings = Strings().apply {
    onboardingHeadline = "All your mail,\none inbox."
    onboardingSubtext = "Connect Gmail, Outlook, or any IMAP account and get one clean, " +
        "categorized inbox — synced in the background, on your terms."
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
    providerCustomDescription = "Any other provider — including university/school mail"

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

    inboxAccountsSyncedTemplate = "%d accounts synced"
    inboxUnreadCountTemplate = "%d unread"
    inboxUnreadSubtext = "Sorted automatically by your rules"
    inboxNoUnread = "No unread"
    inboxNoMessages = "No messages"
    inboxNoMessagesSubtext = "New mail will show up here once an account is synced."
    inboxAllAccounts = "All accounts"
    inboxAddAccount = "Add account"
    inboxToday = "Today"
    inboxYesterday = "Yesterday"

    composeNewMessage = "New message"
    composeTo = "To"
    composeCc = "Cc"
    composeBcc = "Bcc"
    composeSubject = "Subject"
    composeBodyPlaceholder = "Write your message…"

    detailReply = "Reply"
    detailForward = "Forward"
    detailMessageNotFound = "Message not found"
    detailNoSubject = "(no subject)"
    detailAttachmentsTemplate = "Attachments (%d)"

    settingsTitle = "Settings"
    settingsAccountsSection = "Accounts"
    settingsNoAccounts = "No accounts yet — add one to start syncing mail."
    settingsAddAccount = "Add account"
    settingsEditAccount = "Edit"
    settingsRemoveAccount = "Remove"
    settingsRemoveAccountTitle = "Remove account?"
    settingsRemoveAccountMessageTemplate = "This deletes %s and stops syncing it. Locally cached mail is removed too."
    settingsRemove = "Remove"
    settingsCancel = "Cancel"
    settingsSyncSection = "Sync"
    settingsSyncIntervalLabel = "Check for new mail every"
    settingsSyncIntervalMinutesTemplate = "%d min"
    settingsSwipeActionsSection = "Swipe actions"
    settingsSwipeLeft = "Swipe left"
    settingsSwipeRight = "Swipe right"
    settingsCategoriesSection = "Categories & rules"
    settingsNoCategories = "No categories yet — add one to auto-sort mail by sender, subject, or body."
    settingsAppearanceSection = "Appearance"
    settingsTheme = "Theme"
    settingsThemeSystem = "System"
    settingsThemeLight = "Light"
    settingsThemeDark = "Dark"
}

val DeStrings = Strings().apply {
    onboardingHeadline = "Deine gesamte Post,\nein Posteingang."
    onboardingSubtext = "Verbinde Gmail, Outlook oder ein beliebiges IMAP-Konto und erhalte " +
        "einen übersichtlichen, kategorisierten Posteingang — im Hintergrund synchronisiert, ganz nach deinen Wünschen."
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
    providerCustomDescription = "Jeder andere Anbieter — einschließlich Universitäts-/Schul-E-Mail"

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

    inboxAccountsSyncedTemplate = "%d Konten synchronisiert"
    inboxUnreadCountTemplate = "%d ungelesen"
    inboxUnreadSubtext = "Automatisch nach deinen Regeln sortiert"
    inboxNoUnread = "Keine ungelesenen"
    inboxNoMessages = "Keine Nachrichten"
    inboxNoMessagesSubtext = "Neue Post erscheint hier, sobald ein Konto synchronisiert wurde."
    inboxAllAccounts = "Alle Konten"
    inboxAddAccount = "Konto hinzufügen"
    inboxToday = "Heute"
    inboxYesterday = "Gestern"

    composeNewMessage = "Neue Nachricht"
    composeTo = "An"
    composeCc = "Cc"
    composeBcc = "Bcc"
    composeSubject = "Betreff"
    composeBodyPlaceholder = "Schreibe deine Nachricht…"

    detailReply = "Antworten"
    detailForward = "Weiterleiten"
    detailMessageNotFound = "Nachricht nicht gefunden"
    detailNoSubject = "(kein Betreff)"
    detailAttachmentsTemplate = "Anhänge (%d)"

    settingsTitle = "Einstellungen"
    settingsAccountsSection = "Konten"
    settingsNoAccounts = "Noch keine Konten — füge eines hinzu, um die Synchronisierung zu starten."
    settingsAddAccount = "Konto hinzufügen"
    settingsEditAccount = "Bearbeiten"
    settingsRemoveAccount = "Entfernen"
    settingsRemoveAccountTitle = "Konto entfernen?"
    settingsRemoveAccountMessageTemplate = "Dies löscht %s und beendet die Synchronisierung. Lokal zwischengespeicherte Post wird ebenfalls entfernt."
    settingsRemove = "Entfernen"
    settingsCancel = "Abbrechen"
    settingsSyncSection = "Synchronisierung"
    settingsSyncIntervalLabel = "Auf neue Post prüfen alle"
    settingsSyncIntervalMinutesTemplate = "%d Min."
    settingsSwipeActionsSection = "Wischaktionen"
    settingsSwipeLeft = "Nach links wischen"
    settingsSwipeRight = "Nach rechts wischen"
    settingsCategoriesSection = "Kategorien & Regeln"
    settingsNoCategories = "Noch keine Kategorien — füge eine hinzu, um Post automatisch nach Absender, Betreff oder Inhalt zu sortieren."
    settingsAppearanceSection = "Erscheinungsbild"
    settingsTheme = "Design"
    settingsThemeSystem = "System"
    settingsThemeLight = "Hell"
    settingsThemeDark = "Dunkel"
}

val EsStrings = Strings().apply {
    onboardingHeadline = "Todo tu correo,\nuna sola bandeja."
    onboardingSubtext = "Conecta Gmail, Outlook o cualquier cuenta IMAP y obtén una bandeja " +
        "de entrada limpia y organizada — sincronizada en segundo plano, a tu manera."
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
    providerCustomDescription = "Cualquier otro proveedor — incluyendo correo universitario/escolar"

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

    inboxAccountsSyncedTemplate = "%d cuentas sincronizadas"
    inboxUnreadCountTemplate = "%d sin leer"
    inboxUnreadSubtext = "Ordenado automáticamente según tus reglas"
    inboxNoUnread = "Sin no leídos"
    inboxNoMessages = "Sin mensajes"
    inboxNoMessagesSubtext = "El correo nuevo aparecerá aquí una vez que se sincronice una cuenta."
    inboxAllAccounts = "Todas las cuentas"
    inboxAddAccount = "Agregar cuenta"
    inboxToday = "Hoy"
    inboxYesterday = "Ayer"

    composeNewMessage = "Nuevo mensaje"
    composeTo = "Para"
    composeCc = "Cc"
    composeBcc = "Cco"
    composeSubject = "Asunto"
    composeBodyPlaceholder = "Escribe tu mensaje…"

    detailReply = "Responder"
    detailForward = "Reenviar"
    detailMessageNotFound = "Mensaje no encontrado"
    detailNoSubject = "(sin asunto)"
    detailAttachmentsTemplate = "Adjuntos (%d)"

    settingsTitle = "Ajustes"
    settingsAccountsSection = "Cuentas"
    settingsNoAccounts = "Aún no hay cuentas — agrega una para empezar a sincronizar correo."
    settingsAddAccount = "Agregar cuenta"
    settingsEditAccount = "Editar"
    settingsRemoveAccount = "Eliminar"
    settingsRemoveAccountTitle = "¿Eliminar cuenta?"
    settingsRemoveAccountMessageTemplate = "Esto elimina %s y detiene su sincronización. El correo almacenado localmente también se elimina."
    settingsRemove = "Eliminar"
    settingsCancel = "Cancelar"
    settingsSyncSection = "Sincronización"
    settingsSyncIntervalLabel = "Buscar correo nuevo cada"
    settingsSyncIntervalMinutesTemplate = "%d min"
    settingsSwipeActionsSection = "Acciones de deslizamiento"
    settingsSwipeLeft = "Deslizar a la izquierda"
    settingsSwipeRight = "Deslizar a la derecha"
    settingsCategoriesSection = "Categorías y reglas"
    settingsNoCategories = "Aún no hay categorías — agrega una para ordenar automáticamente el correo por remitente, asunto o contenido."
    settingsAppearanceSection = "Apariencia"
    settingsTheme = "Tema"
    settingsThemeSystem = "Sistema"
    settingsThemeLight = "Claro"
    settingsThemeDark = "Oscuro"
}

val FrStrings = Strings().apply {
    onboardingHeadline = "Tout ton courrier,\nune seule boîte de réception."
    onboardingSubtext = "Connecte Gmail, Outlook ou n'importe quel compte IMAP et profite d'une " +
        "boîte de réception claire et organisée — synchronisée en arrière-plan, à ta façon."
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
    providerCustomDescription = "Tout autre fournisseur — y compris le courrier universitaire/scolaire"

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

    inboxAccountsSyncedTemplate = "%d comptes synchronisés"
    inboxUnreadCountTemplate = "%d non lus"
    inboxUnreadSubtext = "Triés automatiquement selon tes règles"
    inboxNoUnread = "Aucun non lu"
    inboxNoMessages = "Aucun message"
    inboxNoMessagesSubtext = "Le nouveau courrier apparaîtra ici une fois un compte synchronisé."
    inboxAllAccounts = "Tous les comptes"
    inboxAddAccount = "Ajouter un compte"
    inboxToday = "Aujourd'hui"
    inboxYesterday = "Hier"

    composeNewMessage = "Nouveau message"
    composeTo = "À"
    composeCc = "Cc"
    composeBcc = "Cci"
    composeSubject = "Objet"
    composeBodyPlaceholder = "Écris ton message…"

    detailReply = "Répondre"
    detailForward = "Transférer"
    detailMessageNotFound = "Message introuvable"
    detailNoSubject = "(sans objet)"
    detailAttachmentsTemplate = "Pièces jointes (%d)"

    settingsTitle = "Paramètres"
    settingsAccountsSection = "Comptes"
    settingsNoAccounts = "Aucun compte pour l'instant — ajoutes-en un pour commencer à synchroniser le courrier."
    settingsAddAccount = "Ajouter un compte"
    settingsEditAccount = "Modifier"
    settingsRemoveAccount = "Supprimer"
    settingsRemoveAccountTitle = "Supprimer le compte ?"
    settingsRemoveAccountMessageTemplate = "Cela supprime %s et arrête sa synchronisation. Le courrier mis en cache localement est également supprimé."
    settingsRemove = "Supprimer"
    settingsCancel = "Annuler"
    settingsSyncSection = "Synchronisation"
    settingsSyncIntervalLabel = "Vérifier le nouveau courrier toutes les"
    settingsSyncIntervalMinutesTemplate = "%d min"
    settingsSwipeActionsSection = "Actions de balayage"
    settingsSwipeLeft = "Balayer à gauche"
    settingsSwipeRight = "Balayer à droite"
    settingsCategoriesSection = "Catégories et règles"
    settingsNoCategories = "Aucune catégorie pour l'instant — ajoutes-en une pour trier automatiquement le courrier par expéditeur, objet ou contenu."
    settingsAppearanceSection = "Apparence"
    settingsTheme = "Thème"
    settingsThemeSystem = "Système"
    settingsThemeLight = "Clair"
    settingsThemeDark = "Sombre"
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

/**
 * Snapshot of the active dictionary for non-composable layers (ViewModel /
 * repository validation messages). Kept in sync at the app root (RootNavigation)
 * whenever the language changes.
 */
object CurrentStrings {
    @Volatile
    var value: Strings = SrStrings
}
