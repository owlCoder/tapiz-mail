package rs.tapizlabs.mail.mail

import java.util.Properties
import rs.tapizlabs.mail.data.local.entity.AccountEntity
import rs.tapizlabs.mail.data.local.entity.ConnectionSecurity

/**
 * Builds [javax.mail.Session] instances for a given [AccountEntity], one for IMAP
 * (read/IDLE) and one for SMTP (send). Kept as a stateless object: sessions are cheap to
 * build and callers should not cache a shared mutable [Properties] instance across accounts.
 */
object MailSession {

    /** Connection/read timeouts short enough that a stalled UNS-style server fails fast
     * instead of tying up a thread (and, for the foreground IDLE service, blocking its
     * lifecycle scope) for minutes. */
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 20_000

    fun imapSession(account: AccountEntity): javax.mail.Session {
        val props = Properties()
        val protocol = if (account.imapSecurity == ConnectionSecurity.SSL_TLS) "imaps" else "imap"

        props["mail.store.protocol"] = protocol
        props["mail.$protocol.host"] = account.imapHost
        props["mail.$protocol.port"] = account.imapPort.toString()
        props["mail.$protocol.connectiontimeout"] = CONNECT_TIMEOUT_MS.toString()
        props["mail.$protocol.timeout"] = READ_TIMEOUT_MS.toString()
        props["mail.$protocol.writetimeout"] = READ_TIMEOUT_MS.toString()

        when (account.imapSecurity) {
            ConnectionSecurity.SSL_TLS -> {
                props["mail.$protocol.ssl.enable"] = "true"
            }
            ConnectionSecurity.STARTTLS -> {
                props["mail.$protocol.starttls.enable"] = "true"
                props["mail.$protocol.starttls.required"] = "true"
            }
            ConnectionSecurity.NONE -> {
                // Plaintext — only expected for local/dev servers; never used for the
                // three supported real providers, but the account model allows it.
            }
        }

        // Fetch without flipping \Seen so background sync doesn't mark mail read before
        // the user has actually opened it in the UI.
        props["mail.imap.peek"] = "true"
        props["mail.imaps.peek"] = "true"

        // Belt-and-braces: some Store implementations look at both the protocol-specific
        // key and the generic "mail.imap.*" one regardless of imaps/imap selection.
        props["mail.imap.connectiontimeout"] = CONNECT_TIMEOUT_MS.toString()
        props["mail.imap.timeout"] = READ_TIMEOUT_MS.toString()

        return javax.mail.Session.getInstance(props)
    }

    fun smtpSession(account: AccountEntity): javax.mail.Session {
        val props = Properties()

        props["mail.smtp.host"] = account.smtpHost
        props["mail.smtp.port"] = account.smtpPort.toString()
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.connectiontimeout"] = CONNECT_TIMEOUT_MS.toString()
        props["mail.smtp.timeout"] = READ_TIMEOUT_MS.toString()
        props["mail.smtp.writetimeout"] = READ_TIMEOUT_MS.toString()

        when (account.smtpSecurity) {
            ConnectionSecurity.SSL_TLS -> {
                props["mail.smtp.ssl.enable"] = "true"
            }
            ConnectionSecurity.STARTTLS -> {
                props["mail.smtp.starttls.enable"] = "true"
                props["mail.smtp.starttls.required"] = "true"
            }
            ConnectionSecurity.NONE -> {
                // Plaintext SMTP — dev/local only.
            }
        }

        return javax.mail.Session.getInstance(props)
    }
}
