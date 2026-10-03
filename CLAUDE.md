# Tapiz Mail — Claude Code Rules

Native Android email klijent, potpuno samostalan (bez ikakvog Tapiz backend servisa) — direktna IMAP/SMTP konekcija sa telefona. Kotlin + Jetpack Compose (Material3), Hilt, Room, WorkManager, JavaMail (`com.sun.mail:android-mail`, `javax.mail.*` namespace — **ne** `jakarta.mail`, iako je 1.6.6 GAV `com.sun.mail`/artifact ime "android-mail" zbunjujuće blizu jakarta paketa). Package `rs.tapizlabs.mail`, minSdk 26 / targetSdk 36.

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
ui/components/       MailSheet (deljeni bottom-sheet overlay), MailButtons (ravna dugmad, bez
                     press efekata), MessageListItem, SwipeableMessageRow, CategoryChipsRow,
                     MailTextField/MailDropdown/MailCard/MailConfirmDialog/MailSectionHeader,
                     SettingsNavRow/SettingsValueRow
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
                     Compose (FAB) i Settings (ikona na Inbox top baru) su push destinacije
                     sa back arrow-om, ne tabovi; Sent/Drafts/Trash su čipovi u Inbox-u
```

## Pravila

- Route → ViewModel → Repository → Room DAO / ImapClient / SmtpClient. Ekrani ne zovu DAO ili mail/ klase direktno.
- Nema bottom nav bar-a — Inbox je full-bleed start destination bez tab bara; Compose (FAB) i Settings (ikona na Inbox top baru) su push-navigacija sa back arrow-om, Sent/Drafts/Trash su pseudo-čipovi u istom redu sa kategorijama (jedini ulaz, bez posebnih ruta), Search je in-screen overlay unutar Inbox-a (ne NavHost ruta). Ne dodavati novi bottom tab bar bez jake IA opravdanosti (videti `_local/reference/standards/design-guidelines.md` u Tapiz workspace-u, `~/Desktop/tapiz/`).
- `AppColors.*` (iz `ui/theme/TapizColors.kt`) za sve boje — bez hardkodovanih hex vrednosti u ekranima. `categoryTints` je cycled-index paleta za kategorije, ne semantička kao Boards-ov priority coloring.
- Dugmad: ravna, bez senke i bez press-edge/lift efekata (`MailButtons.kt`; signal-edge press je bio potpis starog brenda i namerno je izbačen u "Signal" rebrendu). Svako dugme ima ikonu. Jedno filled (`MailPrimaryButton`) po grupi, ostala su `MailGhostButton`; destruktivna sporedna akcija je `MailGhostButton(danger = true)` (coral), nikad primarna boja.
- Sheets/dialozi idu kroz `MailSheet`/`MailConfirmDialog`/`MailPickerSheet` (shared overlay primitivi), ne ad-hoc `ModalBottomSheet`/`AlertDialog`/`DropdownMenu` pozive (izuzetak: `MailDropdownField`, koji živi i unutar sheet-ova). `MailConfirmDialog` je isključivo za destruktivne potvrde (coral confirm dugme); svako brisanje koje se ne može poništiti ide kroz njega.
- Tekst vidljiv korisniku, uključujući `contentDescription` na ikonama, ide kroz `Strings` (`ui/i18n/Strings.kt`, svih pet jezika), bez hardkodovanih literala u ekranima.
- **JavaMail import namespace**: `com.sun.mail:android-mail:1.6.6` koristi `javax.mail.*`/`javax.mail.internet.*`, NE `jakarta.mail.*` — lako se pomeša jer je artifact ID i deo Maven koordinata "jakarta.mail" u pom metadata-i te biblioteke. `IMAPFolder.FetchProfileItem` ima samo HEADERS/SIZE/MESSAGE/INTERNALDATE — UID konstanta je na `javax.mail.UIDFolder.FetchProfileItem.UID`.
- `WorkRequest.MIN_BACKOFF_MILLIS` (ne `WorkManager.MIN_BACKOFF_MILLIS`) za backoff kriterijume.
- `PullToRefreshBox`/`rememberPullToRefreshState` žive u `androidx.compose.material3.pulltorefresh` (odvojen subpackage od ostatka Material3), zahtevaju `@OptIn(ExperimentalMaterial3Api::class)`.
- `MailApp` implementira `Configuration.Provider` (HiltWorkerFactory) — manifest mora imati `androidx.startup.InitializationProvider` sa `tools:node="remove"` na WorkManager initializer meta-data, inače dvostruka inicijalizacija ruši app na startu.
- `IdleSyncService` se (re)startuje iz `MainActivity` dok je aktivnost u `STARTED` stanju i postoji bar jedan aktivan nalog sa `supportsIdle=true` (`repeatOnLifecycle` + `ContextCompat.startForegroundService`) — ne jednom iz `onCreate`, jer se servis sam gasi ~3min posle app-background i povratak u već živu aktivnost ne poziva `onCreate`. Servis je `START_NOT_STICKY` (sticky restart u pozadini na Android 12+ baca `ForegroundServiceStartNotAllowedException`), prati listu naloga uživo i implementira `onTimeout` (Android 15 limit za `dataSync`).
- IMAP IDLE: `IMAPFolder.idle(true)` (vraća se posle JEDNE notifikacije), ne `idle()` bez argumenta (taj se sam nikad ne vraća za novu poštu). IDLE konekcija koristi posebnu sesiju sa dugim read timeout-om (`MailSession.imapSession(forIdle = true)`) — običan 20s timeout ubija IDLE; sync posle push-a ide preko zasebne kratke konekcije (`SyncRepository.syncInbox`).
- Liste poruka (Inbox/Sent/Drafts/Trash/Search) čitaju `MessageListRow` projekciju (bez `bodyPlain`/`bodyHtml`), nikad `SELECT *` — body kolone idu samo kroz `getMessage`/`getMessageOnce` za jedan red. Brojači za čipove su SQL upiti (`observeMailboxCounts`, `getCategoryUnreadCounts`).
- Lokalni Trash/Drafts pseudo-folderi se uvek razrešavaju po id-ju (`local-trash-<accountId>`/`local-drafts-<accountId>`), nikad po `FolderType` — nalog može imati i pravi serverski Trash/Drafts folder istog tipa. Serverski Trash/Drafts/Junk folderi se ne sinhronizuju; virtuelni Gmail folderi (`\All`/`\Flagged`/`\Important`) se ni ne provizionišu.
- Promena read/star flaga ide isključivo kroz `MailSyncGateway.setRead`/`setStarred` (lokalni upis + best-effort IMAP + oznaka "pending" da sync reconcile ne vrati staro stanje) — ne zvati `messageDao.setRead` direktno iz ViewModela.
- Posao koji mora da preživi zatvaranje ekrana (sync posle slanja, prvi sync novog naloga, re-kategorizacija) ide na `@ApplicationScope` `CoroutineScope` (`di/SyncModule.kt`), ne na `viewModelScope`.

## Build

Gradle 8.13 ne radi sa JDK 25 (pada sa besmislenom porukom `What went wrong: 25.0.1`) — koristiti JDK 17/21, npr. `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` na macOS-u.

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

**Potpisivanje i verzija:** release build čita `keystore.properties` iz root-a repoa (gitignored; `storeFile`/`storePassword`/`keyAlias=tapiz-mail`/`keyPassword`). `storeFile` je apsolutna putanja do `tapiz-mail-release.jks` u `~/Desktop/tapiz/_local/secrets/android-release-keys/`, pa se ključ ne kopira u repo; bez tog fajla release build izlazi nepotpisan. `versionName` prati `M.DDMMYY[.N]-a` (datum izdanja, `.N` samo za drugo izdanje istog dana), `versionCode` je +1 po izdanju (videti `_local/reference/standards/versioning-scheme.md`). AAB izlazi u `app/build/outputs/bundle/release/app-release.aab`.

Ako neka nova biblioteka koristi reflection/plugin-style lookup, odmah joj dodati `-keep`/`-dontwarn` pravila u `proguard-rules.pro` — ne čekati da release build first-hand otkrije problem.

## Nedovršeno / sledeći koraci

- Nema `LogoMark variant="mail"` u `@tapizlabs/ui` još — envelope glif postoji samo kao Android drawable (`ic_launcher_foreground.xml`/`splash_logo.xml`), dodati u design system kad/ako Tapiz Mail dobije web prisustvo ili kad se `@tapizlabs/ui` ažurira za sve 7 proizvoda.
- Automatska kategorizacija (`CategoryMatcher`) je pravilo-bazirana (sender/subject/body contains/equals/starts-with), evaluira se u `SyncRepository` tokom sync-a — nema learning/ML komponentu, po dizajnu.
