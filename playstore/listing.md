# Tapiz Mail — Play Store listing

## Osnovni podaci

| Polje | Vrednost |
|---|---|
| Naziv aplikacije (app name) | Tapiz Mail |
| Package name (applicationId) | `rs.tapizlabs.mail` |
| Kategorija | Communication (Komunikacija) |
| Sadrži oglase | Ne |
| In-app kupovine | Ne |
| Kontakt email (developer) | *(popuni — email koji želiš da bude javno vidljiv na listing-u)* |
| Web sajt | *(opciono — app trenutno nema web prisustvo)* |
| Privacy Policy URL | **Obavezno pre objave** — Play Console zahteva javno dostupan URL. Tekst je već u app-u (`ui/settings/PrivacyScreen.kt` — "Privatnost" ekran), treba ga postaviti i na javnu stranicu (npr. prosta stranica na `tapiz.site` ili GitHub Pages) i uneti link u Console. |

---

## Naslov (Title) — max 30 karaktera

```
Tapiz Mail
```
(10 karaktera — ima prostora za dodatak ako poželiš, npr. "Tapiz Mail — Email" bi stalo, ali čist naziv je čitljiviji.)

---

## Kratak opis (Short description) — max 80 karaktera

```
IMAP/SMTP email klijent za sve tvoje naloge — brz, privatan, bez reklama.
```
(74 karaktera)

Alternativa (fokus na fakultet/webmail use-case):
```
Email klijent za Gmail, Outlook i školski webmail — jedan inbox, bez reklama.
```
(78 karaktera)

---

## Pun opis (Full description) — max 4000 karaktera

```
Tapiz Mail je brz i privatan email klijent za Android koji povezuje sve tvoje
naloge — Gmail, Outlook, školski/fakultetski webmail ili bilo koji drugi
IMAP/SMTP server — na jednom mestu, bez reklama i bez praćenja.

ZAŠTO TAPIZ MAIL

• Bez reklama, bez prodaje podataka. Tapiz Mail se ne oslanja na tvoje
  podatke da bi zarađivao — nema analitiku treće strane, nema reklamnih
  mreža.
• Tvoje lozinke ostaju na tvom telefonu. Konekcija ide direktno sa uređaja
  na tvoj mail server (IMAP/SMTP) — nema Tapiz servera između, nema
  posrednika koji čita tvoju poštu.
• Radi sa bilo kojim nalogom. Gmail i Outlook imaju gotova podešavanja za
  jedan klik; svaki drugi IMAP/SMTP nalog (školski webmail, poslovni mail,
  privatni domen) dodaješ ručno uz test konekcije pre čuvanja.

GLAVNE FUNKCIJE

• Više naloga u jednom inbox-u — prebacuj se između naloga jednim tapom.
• Automatsko sortiranje pošte po pravilima koje sam praviš (pošiljalac,
  naslov ili sadržaj) — bez ML-a i bez čudnih algoritama koji odlučuju
  umesto tebe.
• Brzi swipe-akcije prilagođene tvom stilu — biraš šta swipe ulevo i
  udesno rade (arhiviraj, obriši, označi pročitano, zvezdica...).
• Trenutna obaveštenja o novoj pošti (IMAP IDLE push, ne agresivan
  polling koji troši bateriju), sa mogućnošću potpunog isključivanja u
  Podešavanjima ako ih ne želiš.
• Pretraga kroz sve poruke, prilagodljiv interval sinhronizacije,
  podrška za prilagane fajlove (pregled, preuzimanje, čuvanje).
• Svetla i tamna tema, uz automatsko praćenje sistemske teme.
• Pet jezika interfejsa: srpski (latinica), engleski, nemački, španski i
  francuski.

ZA KOGA JE NAMENJEN

Tapiz Mail je nastao da reši konkretan problem: fakultetski/univerzitetski
webmail nalozi (npr. UNS i slični) često nemaju kategorije, pravila ni
moderan interfejs — samo osnovni inbox. Tapiz Mail dodaje sve to preko
običnog IMAP/SMTP pristupa, bez ikakve posebne integracije — pa radi
podjednako dobro i za lični Gmail nalog i za školski mail.

PRIVATNOST PRE SVEGA

Tapiz Mail nema svoj backend server. Sve što app radi — sinhronizacija,
slanje, pretraga — ide direktno sa tvog telefona ka mail serveru koji si
sam podesio. Lozinke se čuvaju isključivo lokalno, u Android Keystore-u
(EncryptedSharedPreferences), nikad u čistom tekstu i nikad na spoljnom
serveru. Kompletnu privatnost politiku možeš pročitati u samoj aplikaciji
(Podešavanja → Privatnost).

Tapiz Mail se aktivno razvija — javi nam grešku ili predlog kroz kontakt
u samoj aplikaciji.
```

(~1850 karaktera — ima dosta rezerve do limita od 4000 ako želiš da dodaš još sekcija, npr. changelog istaknutih funkcija ili FAQ.)

---

## Screenshots (pripremljeni u `playstore/screenshots/`)

Snimljeno na telefon-rezoluciji (1280×2856, 9:20ish — Play Store prihvata sve u opsegu
16:9 do 9:21), sa mock/test sadržajem (nije stvarna prepiska korisnika). Minimum za Play
Store je 2 screenshot-a, preporuka je 4–8:

| Fajl | Opis |
|---|---|
| `01_inbox.png` / `01_inbox_light.png` | Inbox — nalog, kategorije (Prijemno/Poslato/Nedovršeno/Otpad), lista poruka grupisana po danu |
| `02_detail.png` / `02_detail_light.png` | Mail Detail — otvorena poruka, zvezdica, Odgovori/Prosledi |
| `03_compose.png` / `03_compose_light.png` | Compose — nova poruka sa popunjenim poljima |
| `04_settings.png` / `04_settings_light.png` | Podešavanja — nalozi, Pošta, **Obaveštenja** (nova grupa), Izgled i jezik, Privatnost, O aplikaciji |

Redosled upload-a na Play Console: preporučeno je naizmenično dark/light ili grupisano
(sve dark pa sve light) — bilo koji redosled je validan, ali prva 1–2 slike su najbitnije
(vide se u search rezultatima), pa stavi `01_inbox` (dark ili light, koji god više
predstavlja app default — trenutni default je dark) na prvo mesto.

Nije snimljen video promo (opciono polje na Play Console) — može se dodati kasnije.

---

## Grafika (pripremljena u `playstore/assets/`)

| Fajl | Dimenzije | Namena |
|---|---|---|
| `icon_512.png` | 512×512 | App icon za Play Store listing (Google sam zaobljuje uglove — upload pun kvadrat, ne pred-zaobljen) |
| `feature_graphic_1024x500.png` | 1024×500 | Feature graphic (banner na vrhu listing stranice) |

Oba generisana iz `icon.html` / `feature_graphic.html` (isti izvor) preko headless Chrome
render-a — koriste identičan brand gradijent (`#1EB5D4` → `#0E7088`) i envelope glyph kao
`ic_launcher_foreground.xml`/`ic_launcher_background.xml` u samoj app-u, pa je vizuelni
identitet konzistentan sa stvarnom app ikonicom na telefonu.

Ako poželiš izmenu teksta/boje na feature graphic-u, izmeni `feature_graphic.html` i
ponovo renderuj:
```bash
"/c/Program Files/Google/Chrome/Application/chrome.exe" --headless --disable-gpu \
  --screenshot="D:\data\tapiz\tapiz-mail\playstore\assets\feature_graphic_1024x500.png" \
  --window-size=1024,500 \
  "file:///D:/data/tapiz/tapiz-mail/playstore/assets/feature_graphic.html"
```

---

## Šta još treba pre objave (van onoga što sam pripremio)

1. **Privacy Policy URL** — Play Console to zahteva kao javni link, ne kao tekst unutar
   app-a. Najbrže rešenje: prekopiraj tekst iz `PrivacyScreen.kt` na prostu statičku
   stranicu (GitHub Pages, ili poddomen na `tapiz.site`) i unesi taj URL u Console.
2. **Content rating questionnaire** — Play Console upitnik (nasilje/sadržaj za odrasle
   itd.) — za email klijent bez generisanog sadržaja odgovori su svi "Ne", treba samo
   popuniti.
3. **Data safety section** — Play Console traži eksplicitnu deklaraciju: app prikuplja
   email adresu i lozinku (šalje ih samo ka IMAP/SMTP serveru koji korisnik unese, ne ka
   Tapiz serverima), ne deli podatke sa trećim stranama. Treba popuniti formu u skladu sa
   `PrivacyScreen.kt` tekstom.
4. **Release build** — potreban potpisan AAB (`./gradlew :app:bundleRelease`); keystore
   (`tapiz-mail` alias) je već kreiran u `android-release-keys/tapiz-mail-release.jks`
   (videti `tapiz-mail/CLAUDE.md`).
5. **Testers/Internal testing track** — preporuka je prvo Internal testing pa Closed/Open
   pre punog Production rollout-a, standardna Play Console praksa.
