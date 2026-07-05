# Tapiz Mail — Play Store Listing Copy

Draft copy for the Google Play Console store listing. Character counts noted against
each Play Store limit; trim/adjust at submission time if Play Console's live counter
differs slightly from this count.

---

## 1. App title

**Tapiz Mail**

(10 characters — well under the 30-character limit.)

Considered "Tapiz Mail — Email Client" (26 chars, fits) but rejected it: the base name
is short, memorable, and unambiguous on its own once the icon + short description carry
context. A qualifier adds no discoverability the short description doesn't already cover,
and search/ASO value comes from the short description and keywords, not stuffing the title.

---

## 2. Short description

**One inbox for every account. Private, on-device, no backend — ever.**

(72 characters — under the 80-character limit.)

---

## 3. Full description

(Plain text, unicode bullets — no markdown. 4000-character limit; draft below is ~1900
characters, leaving headroom for localization or future edits.)

```
Stuck with an ugly, disorganized webmail? Tapiz Mail connects to any email account —
Gmail, Outlook, or your university's plain IMAP inbox — and gives you one clean, fast,
organized inbox instead.

No cloud account. No sign-up. No middleman server. Just your mail, on your phone,
under your control.

WHY TAPIZ MAIL

• One inbox, every account — add Gmail, Outlook, and custom IMAP/SMTP accounts side by
side and switch between them instantly.

• Actually organized — build your own rules to sort mail into categories and folders
automatically, by sender, subject, or content. No more scrolling through a flat pile of
unread mail.

• Instant notifications, battery-friendly — get pushed new mail the moment it arrives
while you're using the app, with a smart background sync that respects your battery the
rest of the time.

• Swipe like you mean it — Gmail-style swipe gestures to archive, delete, or mark
messages read, and you choose what each direction does.

• Full attachment support — send and receive files, preview them inline, download and
save what you need.

• Search that doesn't wait on a server — full-text search across all your synced mail,
instantly, even offline.

• Works with any provider — if it speaks IMAP and SMTP, Tapiz Mail can connect to it.
Gmail, Outlook, your school or university webmail, or any private mail server. Just
enter the host, port, and your login — no complicated setup wizard.

YOUR EMAIL NEVER LEAVES YOUR PHONE

Tapiz Mail has no backend server. There's no Tapiz cloud sitting between you and your
mailbox — the app talks directly to your provider's own IMAP/SMTP servers, the same
ones your webmail already uses. Your credentials are encrypted on-device with Android's
Keystore and never transmitted anywhere except your mail provider. Nobody — not even
us — can see your email, your password, or your messages.

If you've ever been handed a bare-bones university or workplace webmail and wished it
worked like a real email app, this is that app.

Connect your first account and get one clean inbox in under a minute.
```

---

## 4. Category suggestion

**Communication**

Reasoning: Tapiz Mail's core function is sending/receiving personal correspondence via
standard mail protocols — the canonical use case for the Communication category, where
Gmail, Outlook, K-9 Mail, and FairEmail are all listed. "Productivity" is a reasonable
runner-up (organization/categorization angle), but email clients specifically are
categorized under Communication by convention on Play Store, and users searching for
"email app" browse that category first.

---

## 5. Content rating & Data Safety considerations

Notes for the developer going into Play Console's Content Rating questionnaire and
Data Safety form — flagging what's accurate for this app, not filling the forms out.

**Content rating questionnaire:**
- No user-generated content shared publicly, no social/chat features between users of
  the app itself, no ads, no in-app purchases, no gambling, no violence/mature content.
  This should rate at the lowest tier (e.g. "Everyone") on all major rating systems.

**Data Safety section — this is the important one to get precisely right:**
- **Data collected:** email address, email content/attachments, and mail server
  credentials (host/username/password) are the sensitive categories at play.
- **Critical distinction for the form:** this data is *stored* on-device (Room database
  for mail metadata/content cache, Android Keystore-backed encrypted storage for
  credentials) but is **never transmitted to or processed by any Tapiz server** — there
  is no backend at all. All network traffic goes directly from the device to the
  user's own mail provider (Gmail/Outlook/university IMAP server), which the user
  already trusts by using that provider.
- Because of this, when the Data Safety form asks "Is data collected by your app shared
  with third parties?" / "Is data transmitted off-device to you (the developer)?" —
  the honest answer is **no data is transmitted to or shared with Tapiz** — only to the
  mail provider the user configured, which is not "sharing with a third party" in the
  Play Console sense so much as "the app doing its job as an email client."
- No analytics SDK, no crash reporting sent off-device (verify against current app
  code before submitting — if any telemetry library is added later, this section must
  be revisited), no ads SDK, no in-app purchases.
- Encryption in transit: depends on the account's configured connection security
  (TLS/STARTTLS/plain) — worth noting in the Data Safety form's "data encrypted in
  transit" question that this is user/provider-dependent, not always guaranteed, since
  Tapiz Mail supports connecting to legacy/plain servers if a provider requires it.
- Account deletion: since there's no backend, "deleting" is just removing the account
  from the app (local data wipe) — Data Safety form may ask about a data deletion
  request mechanism; local uninstall/remove-account satisfies this since nothing exists
  server-side to delete.

---

## 6. Suggested keywords / ASO notes

**Target search terms:**
- email client
- IMAP email app
- Gmail Outlook client
- university email app
- secure email android
- multiple email accounts
- offline email search
- private email app
- no ads email app
- custom IMAP SMTP

**Positioning against incumbents:**

Against Gmail/Outlook's own apps: Tapiz Mail's edge is multi-provider — one inbox for
accounts those apps handle awkwardly or not at all (arbitrary university/custom IMAP
servers), plus true on-device privacy since there's no vendor backend indexing your
mail. Against K-9 Mail/FairEmail (both already privacy-respecting, IMAP-first, open
tools): the honest differentiator is simplicity and design — Tapiz Mail aims for a
clean, modern, opinionated inbox experience (rule-based categorization, Gmail-style
swipes) rather than the power-user configurability those apps are known for. Don't
claim to out-feature FairEmail on raw options, and don't claim AI/smart categorization
— it's user-defined rules, which is an honest, understandable feature, not a buzzword.
The realistic pitch: "the clean multi-account inbox for people whose provider gave them
an ugly one, with a real privacy story."
