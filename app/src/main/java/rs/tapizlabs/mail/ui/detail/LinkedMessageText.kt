package rs.tapizlabs.mail.ui.detail

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import rs.tapizlabs.mail.ui.theme.AppColors

@Composable
internal fun LinkedMessageText(text: String, style: TextStyle, onLinkClick: (String) -> Unit) {
    val latestOnLinkClick = rememberUpdatedState(onLinkClick)
    val linkColor = AppColors.primary
    val annotated = remember(text, linkColor) {
        buildAnnotatedString {
            append(text)
            EmailLinks.find(text).forEach { link ->
                addLink(
                    LinkAnnotation.Clickable(
                        tag = link.target,
                        styles = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)),
                        linkInteractionListener = { latestOnLinkClick.value(link.target) },
                    ),
                    start = link.start,
                    end = link.end,
                )
            }
        }
    }
    Text(text = annotated, style = style)
}
