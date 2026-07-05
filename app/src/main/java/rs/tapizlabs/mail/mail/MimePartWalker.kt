package rs.tapizlabs.mail.mail

import javax.mail.Multipart
import javax.mail.Part
import javax.mail.internet.MimePart
import javax.mail.internet.MimeUtility

/**
 * Recursive MIME body-part traversal shared by [ImapClient]'s message parsing (text +
 * attachment metadata) and attachment-download-by-index lookup. Kept separate from
 * [ImapClient] so the protocol/connection class stays focused on IMAP commands rather
 * than MIME tree structure.
 */
internal object MimePartWalker {

    /** Extracts the first plain-text part, first HTML part, and attachment metadata
     * (no bytes) found while walking [part]'s MIME tree — recurses into nested
     * multiparts (e.g. multipart/mixed wrapping multipart/alternative), which is common
     * for mail with both a text body and files. */
    fun extractBody(part: Part): Triple<String?, String?, List<ParsedAttachment>> {
        var plain: String? = null
        var html: String? = null
        val attachments = mutableListOf<ParsedAttachment>()
        walk(part, attachments, intArrayOf(0)) { text, isHtml ->
            if (isHtml) html = html ?: text else plain = plain ?: text
        }
        return Triple(plain, html, attachments)
    }

    /** Finds the attachment [Part] at [targetIndex] (matching the `partIndex` assigned
     * during [extractBody]'s traversal) so a single attachment can be downloaded without
     * re-parsing the whole message body. */
    fun findAttachmentPart(part: Part, targetIndex: Int): Part? =
        findIndexed(part, targetIndex, intArrayOf(0))

    private fun walk(
        part: Part,
        attachments: MutableList<ParsedAttachment>,
        partIndexCounter: IntArray,
        onText: (String, Boolean) -> Unit,
    ) {
        when {
            part.isMimeType("text/plain") && disposition(part) != Part.ATTACHMENT ->
                runCatching { onText(part.content as String, false) }
            part.isMimeType("text/html") && disposition(part) != Part.ATTACHMENT ->
                runCatching { onText(part.content as String, true) }
            part.isMimeType("multipart/*") -> {
                val mp = part.content as Multipart
                for (i in 0 until mp.count) {
                    walk(mp.getBodyPart(i), attachments, partIndexCounter, onText)
                }
            }
            isAttachment(part) -> attachments.add(toParsedAttachment(part, partIndexCounter[0]++))
        }
    }

    private fun findIndexed(part: Part, targetIndex: Int, counter: IntArray): Part? {
        if (part.isMimeType("multipart/*")) {
            val mp = part.content as Multipart
            for (i in 0 until mp.count) {
                findIndexed(mp.getBodyPart(i), targetIndex, counter)?.let { return it }
            }
            return null
        }
        if (isAttachment(part) && counter[0]++ == targetIndex) return part
        return null
    }

    private fun toParsedAttachment(part: Part, index: Int) = ParsedAttachment(
        partIndex = index,
        fileName = decodeFileName(part.fileName) ?: "attachment",
        mimeType = part.contentType?.substringBefore(';')?.trim() ?: "application/octet-stream",
        sizeBytes = part.size.toLong().coerceAtLeast(0),
        contentId = (part as? MimePart)?.getHeader("Content-ID")?.firstOrNull(),
    )

    private fun disposition(part: Part): String? = runCatching { part.disposition }.getOrNull()

    private fun isAttachment(part: Part): Boolean {
        val disp = disposition(part)
        return disp == Part.ATTACHMENT || disp == Part.INLINE || part.fileName != null
    }

    private fun decodeFileName(raw: String?): String? =
        raw?.let { runCatching { MimeUtility.decodeText(it) }.getOrDefault(it) }
}
