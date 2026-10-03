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

    /** Read timeout for the long-lived IDLE connection only. The socket read timeout also
     * applies while blocked in IDLE, so the normal [READ_TIMEOUT_MS] would kill every IDLE
     * after 20s of server silence; this is long enough to actually wait for mail, yet still
     * bounded so a silently dropped connection (NAT timeout, network switch) is noticed and
     * re-established instead of idling forever on a dead socket. */
    private const val IDLE_READ_TIMEOUT_MS = 9 * 60_000

    /** Body parts are fetched in chunks of this size; the 16 KB default costs one IMAP
     * round-trip per 16 KB of message text. */
    private const val FETCH_SIZE_BYTES = 256 * 1024

    fun imapSession(account: AccountEntity, forIdle: Boolean = false): javax.mail.Session {
        val props = Properties()
        val protocol = if (account.imapSecurity == ConnectionSecurity.SSL_TLS) "imaps" else "imap"

        props["mail.store.protocol"] = protocol
        props["mail.$protocol.host"] = account.imapHost
        props["mail.$protocol.port"] = account.imapPort.toString()
        val readTimeoutMs = if (forIdle) IDLE_READ_TIMEOUT_MS else READ_TIMEOUT_MS
        props["mail.$protocol.connectiontimeout"] = CONNECT_TIMEOUT_MS.toString()
        props["mail.$protocol.timeout"] = readTimeoutMs.toString()
        props["mail.$protocol.writetimeout"] = READ_TIMEOUT_MS.toString()
        props["mail.$protocol.fetchsize"] = FETCH_SIZE_BYTES.toString()

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
        props["mail.imap.timeout"] = readTimeoutMs.toString()

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
