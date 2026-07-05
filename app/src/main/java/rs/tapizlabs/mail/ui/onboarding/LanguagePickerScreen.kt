package rs.tapizlabs.mail.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import rs.tapizlabs.mail.ui.components.MailCard
import rs.tapizlabs.mail.ui.i18n.AppLanguage
import rs.tapizlabs.mail.ui.i18n.LocalStrings
import rs.tapizlabs.mail.ui.i18n.Strings
import rs.tapizlabs.mail.ui.theme.AppColors

/**
 * First screen of first-run — lets a new user pick their language before seeing any other
 * UI copy (standard first-run locale pattern), then advances to [OnboardingScreen]. Each
 * language is shown in its own script/name (e.g. "Srpski (latinica)", "Deutsch") so users
 * can find their language even if they can't read the current UI language.
 *
 * No per-language icons — a single shared [Icons.Outlined.Language] icon-chip is reused
 * for every row instead of hunting for 5 distinct icons, per the design guideline against
 * duplicating icons on one screen.
 */
@Composable
fun LanguagePickerScreen(
    selected: AppLanguage,
    onLanguageChosen: (AppLanguage) -> Unit,
) {
    val colors = AppColors
    val strings = LocalStrings.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvasTop),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 72.dp, bottom = 32.dp),
        ) {
            Text(
                text = strings.languagePickerTitle,
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                ),
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = strings.languagePickerSubtitle,
                style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AppLanguage.entries.forEach { language ->
                    LanguageOptionRow(
                        name = displayName(language, strings),
                        isSelected = language == selected,
                        onClick = { onLanguageChosen(language) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageOptionRow(name: String, isSelected: Boolean, onClick: () -> Unit) {
    val colors = AppColors
    MailCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(colors.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp),
                )
            }

            Text(
                text = name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                ),
            )

            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = if (isSelected) colors.primary else colors.textMuted.copy(alpha = 0.3f),
            )
        }
    }
}

private fun displayName(language: AppLanguage, strings: Strings): String = when (language) {
    AppLanguage.SR -> strings.languageNameSerbian
    AppLanguage.EN -> strings.languageNameEnglish
    AppLanguage.DE -> strings.languageNameGerman
    AppLanguage.ES -> strings.languageNameSpanish
    AppLanguage.FR -> strings.languageNameFrench
}
