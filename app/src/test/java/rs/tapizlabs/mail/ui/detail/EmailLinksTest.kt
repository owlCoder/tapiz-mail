package rs.tapizlabs.mail.ui.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmailLinksTest {
    @Test fun detectsUrlsAndEmailWithOriginalOffsets() {
        val text = "Pogledaj https://example.com/a?x=1&y=2, www.tapiz.rs. Piši help@example.com."
        val links = EmailLinks.find(text)
        assertEquals(listOf("https://example.com/a?x=1&y=2", "https://www.tapiz.rs", "mailto:help@example.com"), links.map { it.target })
        assertEquals(listOf("https://example.com/a?x=1&y=2", "www.tapiz.rs", "help@example.com"), links.map { text.substring(it.start, it.end) })
    }

    @Test fun keepsBalancedParenthesesAndDropsProsePunctuation() {
        val text = "(https://example.com/wiki/Test_(example))."
        val link = EmailLinks.find(text).single()
        assertEquals("https://example.com/wiki/Test_(example)", link.target)
        assertEquals(link.target, text.substring(link.start, link.end))
    }

    @Test fun preservesQueryFragmentAndMailtoParameters() {
        assertEquals("https://example.com/path?a=1&b=two#section", EmailLinks.normalize("https://example.com/path?a=1&b=two#section"))
        assertEquals("mailto:user@example.com?subject=Hello%20there", EmailLinks.normalize("mailto:user@example.com?subject=Hello%20there"))
    }

    @Test fun normalizesWwwAndProtocolRelativeHtmlLinks() {
        assertEquals("https://www.example.com", EmailLinks.normalize(" www.example.com "))
        assertEquals("https://example.com/path", EmailLinks.normalize("//example.com/path"))
        assertEquals("HTTPS://example.com", EmailLinks.normalize("HTTPS://example.com"))
    }

    @Test fun rejectsUnsupportedSchemesAndAmbiguousDestinations() {
        for (value in listOf("javascript:alert(1)", "intent://example.com", "file:///sdcard/file", "content://mail/1", "data:text/html,test", "/relative", "#section", "https://", "https://trusted.example@evil.example", "https://example.com/a\nb", "https://example.com/\u202Eevil", "mailto:")) {
            assertNull(value, EmailLinks.normalize(value))
        }
    }

    @Test fun keepsRepeatedAndQuotedLinksSeparate() {
        val text = "> https://example.com\n> https://example.com"
        val links = EmailLinks.find(text)
        assertEquals(2, links.size)
        assertEquals(2, links[0].start)
        assertEquals(text.lastIndexOf("https://"), links[1].start)
        assertEquals(emptyList<EmailLink>(), EmailLinks.find("Običan tekst bez linkova."))
    }
}
