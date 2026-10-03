package rs.tapizlabs.mail.mail

import android.text.Html

/** Plain-text rendition of an HTML mail body, for snippets/search/quoting — not for display
 * (the detail screen renders the HTML itself). */
internal object HtmlText {

    /** Input cap — `Html.fromHtml` is linear but slow on multi-hundred-KB markup, and only
     * the leading text matters for the derived uses. */
    private const val MAX_INPUT_CHARS = 150_000

    /** `Html.fromHtml` keeps the text content of tags it doesn't know, so CSS/JS/head
     * boilerplate has to go first or it ends up as the "text" of the message. */
    private val NON_CONTENT_BLOCKS = Regex(
        "<(style|script|head|title)\\b[^>]*>.*?</\\1\\s*>|<!--.*?-->",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    /** U+FFFC is what `Html.fromHtml` leaves in place of each `<img>`. */
    private val OBJECT_PLACEHOLDERS = Regex("[\\uFFFC\\u00A0]")
    private val BLANK_LINE_RUN = Regex("\\n{3,}")

    fun toPlainText(html: String): String {
        val content = html.take(MAX_INPUT_CHARS).replace(NON_CONTENT_BLOCKS, " ")
        return Html.fromHtml(content, Html.FROM_HTML_MODE_COMPACT).toString()
            .replace(OBJECT_PLACEHOLDERS, " ")
            .replace(BLANK_LINE_RUN, "\n\n")
            .trim()
    }
}
