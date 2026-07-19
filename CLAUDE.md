# Tapiz Mail — Claude Code Rules

Native Android email klijent, potpuno samostalan (bez ikakvog Tapiz backend servisa) — direktna IMAP/SMTP konekcija sa telefona. Kotlin + Jetpack Compose (Material3), Hilt, Room, WorkManager, JavaMail (`com.sun.mail:android-mail`, `javax.mail.*` namespace — **ne** `jakarta.mail`, iako je 1.6.6 GAV `com.sun.mail`/artifact ime "android-mail" zbunjujuće blizu jakarta paketa). Package `rs.tapizlabs.mail`, minSdk 26 / targetSdk 35.

Rešava konkretan problem: korisnikov UNS (univerzitetski) webmail nema kategorije/organizaciju, samo osnovni inbox/sent/trash. Aplikacija radi sa bilo kojim IMAP/SMTP nalogom (Gmail, Outlook, UNS, custom) preko ručno unetih host/port/username/password — nema OAuth2, nema backend, kredencijali nikad ne napuštaju uređaj.

## Arhitektura

```
data/local/         Room: entity/ (Account, Folder, Message, Attachment, Category, CategoryRule,
                     SwipeActionConfig) + dao/ + MailDatabase + converters/ (enum TypeConverters)
data/local/entity/CategoryMatcher.kt   čist pravilo-bazirani (ne ML) kategorizacioni engine —
                     evaluira CategoryRuleEntity liste protiv poruke, pozvan iz SyncRepository
data/repository/    MailRepository (read-facade za UI), AccountRepository (account CRUD +
                     test-connection + swipe/category config), SyncRepository (deljeni
                     fetch→parse→categorize→upsert put koji koriste i MailSyncWorker i
                     IdleSyncService), MailSyncGateway/DefaultMailSyncGateway (refresh/send
                     facade za ViewModele)
security/           CredentialStore — EncryptedSharedPreferences (Keystore-backed), per-account
                     IMAP/SMTP lozinke, nikad u Room-u niti u plaintext-u
mail/                MailSession (javax.mail Session builder po ConnectionSecurity),
                     ImapClient (connect/testConnection/listFolders/fetchNewMessages/idle/
                     downloadAttachment), SmtpClient (MIME multipart send sa attachmentima),
                     MimePartWalker (MIME-tree ekstrakcija teksta/attachmenata)
sync/                SyncScheduler (WorkManager periodic po nalogu, Doze-aware),
                     MailSyncWorker (CoroutineWorker fallback sync), IdleSyncService
                     (foreground servis, drži IMAP IDLE otvoren samo za supportsIdle=true
                     naloge dok je app foreground/kratko background — self-stopping)
di/                  Hilt moduli: DatabaseModule, RepositoryModule, SyncModule
ui/theme/            TapizColors/MailTheme/ThemePref — isti recept kao ostali Tapiz Android appovi
ui/components/       MailSheet (deljeni bottom-sheet overlay), MailButtons (flat + signal-edge
                     press stil), MessageListItem, SwipeableMessageRow, CategoryChipsRow,
                     MailTextField/MailDropdown/MailCard/MailConfirmDialog/MailSectionHeader
ui/onboarding/       Prvi-run "Get Started" ekran (dark, brand tokeni)
ui/account/          AddAccountScreen — provider chooser (Gmail/Outlook prepopulate/Custom) +
                     manual IMAP/SMTP forma + test-connection pre save-a
ui/inbox/            Inbox tab — account switcher, kategorija chips, swipe akcije, pull-to-refresh
ui/detail/           Mail Detail — WebView (JS disabled) za HTML body, attachment download/open/save
ui/compose/          Compose — to/cc/bcc, attachment picker (SAF), reply/forward pre-fill
ui/search/           Search — debounced lokalna Room pretraga + filteri, prikazan kao full-screen
                     overlay unutar InboxScreen-a (Gmail-style), nije NavHost ruta
ui/settings/         Accounts/sync-interval/swipe-mapping/categories-rules/theme
ui/navigation/       Routes, RootNavigation (NavHost), RootViewModel (no-accounts→Onboarding
                     routing). Nema bottom nav bar-a — full-bleed Inbox je start destination;
                     Compose/Drafts/Settings su push destinacije (icon dugmad na Inbox top baru,
                     back arrow), ne tabovi
```

## Pravila

- Route → ViewModel → Repository → Room DAO / ImapClient / SmtpClient. Ekrani ne zovu DAO ili mail/ klase direktno.
- Nema bottom nav bar-a — Inbox je full-bleed start destination bez tab bara; Compose/Drafts/Settings su push-navigacija (icon dugmad na Inbox top baru + back arrow), Search je in-screen overlay unutar Inbox-a (ne NavHost ruta). Ne dodavati novi bottom tab bar bez jake IA opravdanosti (videti `_local/reference/design-guidelines.md` u root-u workspace-a).
- `AppColors.*` (iz `ui/theme/TapizColors.kt`) za sve boje — bez hardkodovanih hex vrednosti u ekranima. `categoryTints` je cycled-index paleta za kategorije, ne semantička kao Boards-ov priority coloring.
- Dugmad: flat-at-rest, signal-colored bottom+right edge na press (`MailButtons.kt`) — ne all-around shadow, ne translateY lift.
- Sheets/dialozi idu kroz `MailSheet`/`MailConfirmDialog` (shared overlay primitivi), ne ad-hoc `ModalBottomSheet`/`AlertDialog` pozive.
- **JavaMail import namespace**: `com.sun.mail:android-mail:1.6.6` koristi `javax.mail.*`/`javax.mail.internet.*`, NE `jakarta.mail.*` — lako se pomeša jer je artifact ID i deo Maven koordinata "jakarta.mail" u pom metadata-i te biblioteke. `IMAPFolder.FetchProfileItem` ima samo HEADERS/SIZE/MESSAGE/INTERNALDATE — UID konstanta je na `javax.mail.UIDFolder.FetchProfileItem.UID`.
- `WorkRequest.MIN_BACKOFF_MILLIS` (ne `WorkManager.MIN_BACKOFF_MILLIS`) za backoff kriterijume.
- `PullToRefreshBox`/`rememberPullToRefreshState` žive u `androidx.compose.material3.pulltorefresh` (odvojen subpackage od ostatka Material3), zahtevaju `@OptIn(ExperimentalMaterial3Api::class)`.
- `MailApp` implementira `Configuration.Provider` (HiltWorkerFactory) — manifest mora imati `androidx.startup.InitializationProvider` sa `tools:node="remove"` na WorkManager initializer meta-data, inače dvostruka inicijalizacija ruši app na startu.
- `IdleSyncService` se pokreće iz `MainActivity.onCreate` (`ContextCompat.startForegroundService`) bezuslovno — servis sam filtrira naloge sa `supportsIdle=true` i sam se gasi ~3min posle app-background (videti komentare u `IdleSyncService.kt`).

## Build

```bash
cd tapiz-mail
unset ANDROID_HOME && ./gradlew :app:assembleDebug --console=plain -q
unset ANDROID_HOME && ./gradlew :app:installDebug
```

`local.properties` mora imati forward slashes i tačnu aktivnu `sdk.dir` liniju za trenutnog OS korisnika (Danijel vs owl/Claude Code) — pogrešan korisnik daje `AccessDeniedException` na SDK jar fajlovima. Ako se SDK path promeni dok je Gradle daemon živ, uraditi `./gradlew --stop` pre ponovnog build-a (stari path ostaje keširan u daemonu).

**Pre svakog `bundleRelease` koji ide na Play Store, obavezno testirati sam release build, ne samo debug:**

```bash
unset ANDROID_HOME && ./gradlew :app:assembleRelease --console=plain -q
unset ANDROID_HOME && ./gradlew :app:installRelease
```

Razlog: release build prolazi kroz R8 (`isMinifyEnabled = true`), debug ne prolazi — R8 briše/preimenuje sve što ne vidi kao direktno pozvano iz koda (reflection, `Class.forName`, `ServiceLoader`, provider registri poput JavaMaila), pa nešto može raditi savršeno u debug-u i pucati samo u release-u (npr. bag iz 2026-07-07: `com.sun.mail`/`javax.mail` registruje IMAP/SMTP provider klase isključivo preko reflection-a; bez `-keep` pravila u `proguard-rules.pro` R8 ih je obrisao/obfuskovao i svaki custom-nalog connect je pucao sa `NoSuchProviderException` čije je obfuskovano ime klase u poruci izgledalo kao besmisleno jedno slovo — trag da je R8 umešan). Instaliraj `installRelease` na uređaj/emulator i ručno prođi kroz add-account flow (bar jedan IMAP + jedan SMTP test-connection) pre nego što se AAB uploaduje.

Ako neka nova biblioteka koristi reflection/plugin-style lookup, odmah joj dodati `-keep`/`-dontwarn` pravila u `proguard-rules.pro` — ne čekati da release build first-hand otkrije problem.

## Nedovršeno / sledeći koraci

- Nema `LogoMark variant="mail"` u `@tapizlabs/ui` još — envelope glif postoji samo kao Android drawable (`ic_launcher_foreground.xml`/`splash_logo.xml`), dodati u design system kad/ako Tapiz Mail dobije web prisustvo ili kad se `@tapizlabs/ui` ažurira za sve 7 proizvoda.
- Automatska kategorizacija (`CategoryMatcher`) je pravilo-bazirana (sender/subject/body contains/equals/starts-with), evaluira se u `SyncRepository` tokom sync-a — nema learning/ML komponentu, po dizajnu.
