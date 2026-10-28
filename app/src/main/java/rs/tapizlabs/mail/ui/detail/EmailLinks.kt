package rs.tapizlabs.mail.ui.detail

import java.net.URI
import java.util.Locale

internal data class EmailLink(val start: Int, val end: Int, val target: String)

/** Shared allowlist for detected text URLs and untrusted HTML hrefs. */
internal object EmailLinks {
    private val candidates = Regex(
        """(?i)(?<![\w@])(?:https?://|www\.)[^\s<>"']+|(?:mailto:)?[A-Z0-9._%+\-]+@[A-Z0-9.\-]+\.[A-Z]{2,}""",
        RegexOption.IGNORE_CASE,
    )

    fun normalize(raw: String): String? {
        val value = raw.trim()
        if (value.isEmpty() || value.any { it.isISOControl() || it in '\u202A'..'\u202E' || it in '\u2066'..'\u2069' }) return null
        val target = when {
            value.startsWith("www.", ignoreCase = true) -> "https://$value"
            value.startsWith("//") -> "https:$value"
            else -> value
        }
        val uri = runCatching { URI(target) }.getOrNull() ?: return null
        return when (uri.scheme?.lowercase(Locale.ROOT)) {
            "http", "https" -> if (!uri.host.isNullOrBlank() && uri.rawUserInfo == null) uri.toASCIIString() else null
            "mailto" -> if (uri.rawSchemeSpecificPart.substringBefore('?').contains('@')) uri.toASCIIString() else null
            else -> null
        }
    }

    fun find(text: String): List<EmailLink> = candidates.findAll(text).mapNotNull { match ->
        var value = match.value.trimEnd('.', ',', ';', ':', '!', '?')
        // Keep balanced parentheses in paths, but exclude surrounding prose punctuation.
        for ((open, close) in listOf('(' to ')', '[' to ']', '{' to '}')) {
            while (value.endsWith(close) && value.count { it == close } > value.count { it == open }) {
                value = value.dropLast(1)
            }
        }
        val raw = if (!value.contains(":") && !value.startsWith("www.", true) && value.contains('@')) "mailto:$value" else value
        normalize(raw)?.let { EmailLink(match.range.first, match.range.first + value.length, it) }
    }.toList()
}
