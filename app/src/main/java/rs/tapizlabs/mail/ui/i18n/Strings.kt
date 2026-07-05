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

    // Language picker
    lateinit var languagePickerTitle: String
    lateinit var languagePickerSubtitle: String
    lateinit var languageNameSerbian: String
    lateinit var languageNameEnglish: String
    lateinit var languageNameGerman: String
    lateinit var languageNameSpanish: String
    lateinit var languageNameFrench: String
}

val SrStrings = Strings().apply {
    onboardingHeadline = "Sva tvoja pošta,\njedno prijemno sanduče."
    onboardingSubtext = "Poveži Gmail, Outlook ili bilo koji IMAP nalog i dobij jedno " +
        "uredno, kategorisano prijemno sanduče — sinhronizovano u pozadini, po tvojim uslovima."
    onboardingGetStarted = "Započni"

    languagePickerTitle = "Izaberite jezik"
    languagePickerSubtitle = "Možete ga promeniti kasnije u podešavanjima."
    languageNameSerbian = "Srpski (latinica)"
    languageNameEnglish = "English"
    languageNameGerman = "Deutsch"
    languageNameSpanish = "Español"
    languageNameFrench = "Français"
}

val EnStrings = Strings().apply {
    onboardingHeadline = "All your mail,\none inbox."
    onboardingSubtext = "Connect Gmail, Outlook, or any IMAP account and get one clean, " +
        "categorized inbox — synced in the background, on your terms."
    onboardingGetStarted = "Get Started"

    languagePickerTitle = "Choose your language"
    languagePickerSubtitle = "You can change this later in settings."
    languageNameSerbian = "Srpski (latinica)"
    languageNameEnglish = "English"
    languageNameGerman = "Deutsch"
    languageNameSpanish = "Español"
    languageNameFrench = "Français"
}

val DeStrings = Strings().apply {
    onboardingHeadline = "Deine gesamte Post,\nein Posteingang."
    onboardingSubtext = "Verbinde Gmail, Outlook oder ein beliebiges IMAP-Konto und erhalte " +
        "einen übersichtlichen, kategorisierten Posteingang — im Hintergrund synchronisiert, ganz nach deinen Wünschen."
    onboardingGetStarted = "Loslegen"

    languagePickerTitle = "Wähle deine Sprache"
    languagePickerSubtitle = "Du kannst dies später in den Einstellungen ändern."
    languageNameSerbian = "Srpski (latinica)"
    languageNameEnglish = "English"
    languageNameGerman = "Deutsch"
    languageNameSpanish = "Español"
    languageNameFrench = "Français"
}

val EsStrings = Strings().apply {
    onboardingHeadline = "Todo tu correo,\nuna sola bandeja."
    onboardingSubtext = "Conecta Gmail, Outlook o cualquier cuenta IMAP y obtén una bandeja " +
        "de entrada limpia y organizada — sincronizada en segundo plano, a tu manera."
    onboardingGetStarted = "Comenzar"

    languagePickerTitle = "Elige tu idioma"
    languagePickerSubtitle = "Puedes cambiarlo más tarde en los ajustes."
    languageNameSerbian = "Srpski (latinica)"
    languageNameEnglish = "English"
    languageNameGerman = "Deutsch"
    languageNameSpanish = "Español"
    languageNameFrench = "Français"
}

val FrStrings = Strings().apply {
    onboardingHeadline = "Tout ton courrier,\nune seule boîte de réception."
    onboardingSubtext = "Connecte Gmail, Outlook ou n'importe quel compte IMAP et profite d'une " +
        "boîte de réception claire et organisée — synchronisée en arrière-plan, à ta façon."
    onboardingGetStarted = "Commencer"

    languagePickerTitle = "Choisissez votre langue"
    languagePickerSubtitle = "Vous pourrez la changer plus tard dans les paramètres."
    languageNameSerbian = "Srpski (latinica)"
    languageNameEnglish = "English"
    languageNameGerman = "Deutsch"
    languageNameSpanish = "Español"
    languageNameFrench = "Français"
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
