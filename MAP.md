# tapiz-mail — Map

> Native Android email klijent, potpuno samostalan (bez Tapiz backend servisa) — direktna
> IMAP/SMTP konekcija sa telefona. Čitaj `CLAUDE.md` pre rada. Kotlin + Jetpack Compose
> (Material3), Hilt, Room, WorkManager, JavaMail (`com.sun.mail:android-mail`, `javax.mail.*`
> namespace). Package `rs.tapizlabs.mail`.

## Struktura

```
app/src/main/java/rs/tapizlabs/mail/
├── MailApp.kt              — @HiltAndroidApp + Configuration.Provider (HiltWorkerFactory)
├── MainActivity.kt         — entry point; startuje IdleSyncService (foreground)
├── core/local/
│   └── PrefsStore.kt       — DataStore (theme/language/onboarding prefs)
├── security/
│   └── CredentialStore.kt  — EncryptedSharedPreferences (Keystore) per-account IMAP/SMTP lozinke
├── data/local/
│   ├── MailDatabase.kt     — Room DB
│   ├── entity/             — Account, Folder, Message, Attachment, Category, CategoryRule,
│   │                         SwipeActionConfig + CategoryMatcher.kt (pravilo-bazirani engine)
│   ├── dao/                — Account/Attachment/Category/CategoryRule/Folder/Message/SwipeActionConfig DAO
│   └── converters/         — enum TypeConverters
├── data/repository/
│   ├── MailRepository.kt       — read-facade za Inbox/Detail/Compose/Search (poruke)
│   ├── AccountRepository.kt     — account CRUD + testConnection + swipe/category config (writes)
│   ├── SyncRepository.kt        — deljeni fetch→parse→categorize→upsert put (Worker + IdleService)
│   └── MailSyncGateway.kt       — refresh/send facade za ViewModele
├── mail/
│   ├── MailSession.kt      — javax.mail.Session builder po ConnectionSecurity (SSL_TLS/STARTTLS/NONE)
│   ├── ImapClient.kt       — connect/testConnection/listFolders/fetchNewMessages/idle/downloadAttachment
│   ├── SmtpClient.kt       — MIME multipart send sa attachmentima
│   ├── MimePartWalker.kt   — MIME-tree ekstrakcija teksta/attachmenata
│   └── MailDtos.kt         — MailError (sealed), ParsedMessage/FolderInfo/ParsedAttachment
├── sync/
│   ├── SyncScheduler.kt    — WorkManager periodic po nalogu (Doze-aware)
│   ├── MailSyncWorker.kt   — CoroutineWorker fallback sync
│   ├── IdleSyncService.kt  — foreground servis, drži IMAP IDLE (supportsIdle=true), self-stopping
│   └── NewMailNotifier.kt  — notifikacije za novu poštu
├── di/                     — Hilt: DatabaseModule, RepositoryModule, SyncModule
└── ui/
    ├── theme/              — TapizColors/AppColors, MailTheme, ThemePref, ThemeViewModel, Type
    ├── i18n/               — Strings (sr/en/de/es/fr), LocalStrings, LanguageViewModel
    ├── navigation/         — Routes, RootNavigation (NavHost), RootViewModel (no-accounts→Onboarding), MailBottomBar (4 taba)
    ├── onboarding/         — OnboardingScreen, LanguagePickerScreen, NotificationPermissionScreen
    ├── account/            — ChooseProviderScreen (Gmail/Outlook/Custom), AddAccountScreen + AddAccountViewModel (manual IMAP/SMTP + test-connection)
    ├── inbox/              — InboxScreen + InboxViewModel (account switcher, kategorija chips, swipe, pull-to-refresh; hostuje Search overlay)
    ├── detail/             — MailDetailScreen + MailDetailViewModel (WebView JS-disabled za HTML body, attachment download)
    ├── compose/            — ComposeScreen + ComposeViewModel (to/cc/bcc, SAF attachment picker, reply/forward)
    ├── search/             — SearchScreen (floating overlay) + SearchViewModel (debounced Room pretraga + filteri)
    ├── settings/           — SettingsScreen, MailSettingsScreen, AppearanceSettingsScreen, AboutScreen, PrivacyScreen, CategoryEditorSheet, SettingsViewModel
    └── components/         — MailSheet (bottom-sheet overlay), MailButtons (flat+signal-edge), MailTextField/MailDropdown/MailCard/MailConfirmDialog,
                              MailSectionHeader, MessageListItem, SwipeableMessageRow, CategoryChipsRow, MailPickerSheet, SegmentedPickerCard,
                              ProviderIcons, Skeleton, MailLoadingSpinner, SettingsNavRow
```

## Gde da počneš

| Task | Počni ovde |
|---|---|
| Dodaj/izmeni ekran | `ui/<feature>/<Feature>ViewModel.kt` + `<Feature>Screen.kt` |
| Promeni add-account / test-connection flow | `ui/account/AddAccountViewModel.kt` → `AccountRepository.testConnection` → `mail/ImapClient.kt` |
| Popravi IMAP/SMTP konekciju (TLS, portovi, timeouts) | `mail/MailSession.kt` (Session props) + `mail/ImapClient.kt`/`mail/SmtpClient.kt` |
| Greške konekcije / mapiranje | `mail/MailDtos.kt` (`MailError` sealed) — `ConnectionFailed` = svaka ne-auth `MessagingException` |
| Sync (background/IDLE) | `sync/SyncRepository.kt` (deljeni put) + `MailSyncWorker.kt` / `IdleSyncService.kt` |
| Kategorije (auto-tagging) | `data/local/entity/CategoryMatcher.kt` (pravilo-bazirano, eval u `SyncRepository`) |
| Promeni boje/temu | `ui/theme/TapizColors.kt` (`categoryTints` = cycled-index paleta) |
| Promeni navigaciju | `ui/navigation/RootNavigation.kt` + `Routes.kt` (bottom nav = tačno 4 taba) |
| Sheet/overlay | `ui/components/MailSheet.kt` (bottom) / `ui/search/SearchScreen.kt` (top floating) — ne ad-hoc `ModalBottomSheet` |
| Kredencijali (lozinke) | `security/CredentialStore.kt` (EncryptedSharedPreferences, nikad Room/plaintext) |
| Lokalizacija (5 jezika) | `ui/i18n/Strings.kt` — popuni svih 5 (sr/en/de/es/fr) pri dodavanju polja |

## Build

```bash
cd tapiz-mail
unset ANDROID_HOME && ./gradlew :app:assembleDebug --console=plain -q
unset ANDROID_HOME && ./gradlew :app:installDebug
```

`local.properties` drži dve `sdk.dir` linije (Danijel / owl) — aktivna mora biti za trenutnog
OS korisnika, forward slashes; pogrešan korisnik daje `AccessDeniedException` na SDK jar.
Ako se SDK path promeni dok Gradle daemon živi: `./gradlew --stop` pre ponovnog build-a.
