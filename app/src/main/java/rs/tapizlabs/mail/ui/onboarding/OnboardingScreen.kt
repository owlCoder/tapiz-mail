package rs.tapizlabs.mail.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import rs.tapizlabs.mail.ui.components.MailPrimaryButton
import rs.tapizlabs.mail.ui.i18n.LocalStrings
import rs.tapizlabs.mail.ui.theme.AppColors

/**
 * First-run "Get Started" screen — shown only when no account is configured yet
 * (see `RootNavigation`'s account-count check). Adapted from the reference dark
 * onboarding mock into Tapiz brand tokens rather than an arbitrary dark palette.
 */
@Composable
fun OnboardingScreen(onGetStarted: () -> Unit) {
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
                .padding(horizontal = 28.dp)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.MarkEmailRead,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(44.dp),
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = strings.onboardingHeadline,
                style = MaterialTheme.typography.displaySmall.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                ),
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = strings.onboardingSubtext,
                style = MaterialTheme.typography.bodyLarge.copy(color = colors.textSecondary),
            )

            Spacer(Modifier.height(40.dp))

            MailPrimaryButton(
                text = strings.onboardingGetStarted,
                onClick = onGetStarted,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
