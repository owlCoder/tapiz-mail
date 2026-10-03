package rs.tapizlabs.mail.ui.components

import android.text.format.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Locale-aware date/time labels for message lists and the detail header, formatted against
 * the in-app language ([rs.tapizlabs.mail.ui.i18n.LocalAppLanguage]) rather than the device
 * locale. Formatters are cached per locale — list rows ask for one on every (re)composition,
 * and building a [DateTimeFormatter] from a pattern each time is measurable while scrolling.
 */
internal object MailDateFormat {

    private val shortDateFormatters = ConcurrentHashMap<Locale, DateTimeFormatter>()
    private val timeFormatters = ConcurrentHashMap<Locale, DateTimeFormatter>()
    private val fullFormatters = ConcurrentHashMap<Locale, DateTimeFormatter>()

    /** Day + abbreviated month in the locale's own order ("3. okt" / "Oct 3" / "3 oct"). */
    private fun shortDate(locale: Locale): DateTimeFormatter = shortDateFormatters.getOrPut(locale) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMd"), locale)
    }

    private fun time(locale: Locale): DateTimeFormatter = timeFormatters.getOrPut(locale) {
        DateTimeFormatter.ofPattern("HH:mm", locale)
    }

    private fun full(locale: Locale): DateTimeFormatter = fullFormatters.getOrPut(locale) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale)
    }

    fun localDate(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()

    fun shortDate(date: LocalDate, locale: Locale): String = date.format(shortDate(locale))

    /** Row timestamp: time of day for today's mail, short date otherwise. */
    fun rowTimestamp(epochMillis: Long, locale: Locale): String {
        val then = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
        return if (then.toLocalDate() == LocalDate.now()) {
            then.format(time(locale))
        } else {
            then.format(shortDate(locale))
        }
    }

    /** Full date + time for the message detail header. */
    fun fullTimestamp(epochMillis: Long, locale: Locale): String =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(full(locale))
}
