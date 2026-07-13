package rs.tapizlabs.mail.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import rs.tapizlabs.mail.ui.components.MailPrimaryButton
import rs.tapizlabs.mail.ui.components.ProviderBadge
import rs.tapizlabs.mail.ui.components.ProviderBrandColors
import rs.tapizlabs.mail.ui.components.TapizMailLogo
import rs.tapizlabs.mail.ui.i18n.LocalStrings
import rs.tapizlabs.mail.ui.theme.AppColors

/**
 * First-run "Add account" screen — now shares the ecosystem's [GradientBackground] canvas
 * (ported from tapiz-lms/apps/android) instead of a Mail-only full-bleed colored hero panel,
 * so Tapiz Mail's onboarding reads as one more Tapiz app rather than a standalone product: an
 * 84dp icon chip on `accentSoft` (Ink & Ember's `StepIconChip` pattern), headline/subtext in
 * `textPrimary`/`textSecondary`, two provider shortcut rows, a manual-IMAP preview block, and a
 * bottom "Connect account" CTA pinned to the screen bottom (not the scroll content).
 *
 * The Gmail/Outlook rows here are visual-only shortcuts into the same manual flow (this app has
 * no OAuth backend — everything is direct IMAP/SMTP per CLAUDE.md), so tapping either one
 * pre-fills the known host/port and continues to [rs.tapizlabs.mail.ui.account.AddAccountScreen]
 * exactly like the existing Choose-Provider step does.
 */
@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    onGmail: () -> Unit = onGetStarted,
    onOutlook: () -> Unit = onGetStarted,
) {
    val colors = AppColors
    val strings = LocalStrings.current

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(top = 24.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                TapizMailLogo(size = 44.dp, tile = false, mono = true, glyphColor = colors.primary)
            }

            Spacer(Modifier.height(22.dp))

            Text(
                text = strings.onboardingHeadline,
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                ),
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = strings.onboardingSubtext,
                style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary),
            )

            Spacer(Modifier.height(18.dp))

            Column {
                OAuthProviderRow(
                    label = strings.onboardingContinueWithGmail,
                    letter = "G",
                    badgeColor = ProviderBrandColors.Gmail,
                    onClick = onGmail,
                )
                Spacer(Modifier.height(10.dp))
                OAuthProviderRow(
                    label = strings.onboardingContinueWithOutlook,
                    letter = "O",
                    badgeColor = ProviderBrandColors.Outlook,
                    onClick = onOutlook,
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = strings.onboardingOrConnectManually,
                style = MaterialTheme.typography.bodySmall.copy(color = colors.textMuted),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(18.dp))

            ManualFormPreview(imapHostLabel = strings.onboardingImapHost, usernameLabel = strings.onboardingUsername)
        }

        MailPrimaryButton(
            text = strings.onboardingGetStarted,
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp, top = 12.dp),
        )
    }
}

/** OAuth-style row (provider-brand badge + label on a card surface). Not a real OAuth flow —
 * this app has no backend, so this shortcuts into the manual IMAP/SMTP form pre-filled with
 * the provider's known host/port. */
@Composable
private fun OAuthProviderRow(label: String, letter: String, badgeColor: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    val colors = AppColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cardSubtle)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProviderBadge(letter = letter, color = badgeColor, size = 30.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

/** Bordered IMAP HOST / USERNAME preview block (reference: static display, not an
 * editable form on this screen — actual editing happens on [rs.tapizlabs.mail.ui.account.AddAccountScreen]
 * after tapping through). */
@Composable
private fun ManualFormPreview(imapHostLabel: String, usernameLabel: String) {
    val colors = AppColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cardSubtle)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = imapHostLabel,
            style = MaterialTheme.typography.labelSmall.copy(color = colors.textMuted),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "webmail.uns.ac.rs",
            style = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        )
        HorizontalDivider(color = colors.stroke)
        Spacer(Modifier.height(10.dp))
        Text(
            text = usernameLabel,
            style = MaterialTheme.typography.labelSmall.copy(color = colors.textMuted),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "student@uns.ac.rs",
            style = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        )
        HorizontalDivider(color = colors.stroke)
    }
}
