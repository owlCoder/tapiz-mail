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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import rs.tapizlabs.mail.R
import rs.tapizlabs.mail.ui.components.MailPrimaryButton
import rs.tapizlabs.mail.ui.components.ProviderBadge
import rs.tapizlabs.mail.ui.components.ProviderBrandColors
import rs.tapizlabs.mail.ui.i18n.LocalStrings
import rs.tapizlabs.mail.ui.theme.AppColors

/**
 * First-run "Add account" screen — matches
 * design_handoff_tapiz_mail_android/design-reference.html's "Add account" screen 1:1:
 * a full-bleed colored hero panel with the app mark, headline/subtext, two
 * OAuth-style provider rows (Gmail/Outlook), a manual-IMAP form block, and a bottom
 * "Connect account" CTA pinned to the screen bottom (not the scroll content) —
 * all on one screen, not split across steps.
 *
 * The Gmail/Outlook rows here are visual-only shortcuts into the same manual flow
 * (this app has no OAuth backend — everything is direct IMAP/SMTP per CLAUDE.md),
 * so tapping either one pre-fills the known host/port and continues to
 * [rs.tapizlabs.mail.ui.account.AddAccountScreen] exactly like the existing
 * Choose-Provider step does.
 */
@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    onGmail: () -> Unit = onGetStarted,
    onOutlook: () -> Unit = onGetStarted,
) {
    val colors = AppColors
    val strings = LocalStrings.current
    // Reference: light mode hero = primary indigo panel; dark mode hero = the
    // dedicated deep-violet `card` token (oklch(28% 0.05 265)), not the plain dark
    // canvas. Hero text is white in both modes.
    val heroBackground = if (colors.isDark) colors.card else colors.primary
    // Reference CTA button: dark mode = primary-bg/onPrimary-text (a light-indigo
    // button that pops against the deep-violet hero); light mode = white-bg/
    // primary-text (a white button against the indigo hero). Not the same
    // inversion in both modes, so pass explicit colors rather than one flag.
    val ctaContainer = if (colors.isDark) colors.primary else Color.White
    val ctaContent = if (colors.isDark) colors.onPrimary else colors.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(heroBackground),
    ) {
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
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.primaryBright),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.splash_logo),
                        contentDescription = null,
                        tint = if (colors.isDark) colors.onPrimary else Color.White,
                        modifier = Modifier.size(52.dp),
                    )
                }

                Spacer(Modifier.height(22.dp))

                Text(
                    text = strings.onboardingHeadline,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    ),
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = strings.onboardingSubtext,
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.8f)),
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
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(18.dp))

                ManualFormPreview(imapHostLabel = strings.onboardingImapHost, usernameLabel = strings.onboardingUsername)
            }

            MailPrimaryButton(
                text = strings.onboardingGetStarted,
                onClick = onGetStarted,
                containerColorOverride = ctaContainer,
                contentColorOverride = ctaContent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp, top = 12.dp),
            )
        }
    }
}

/** OAuth-style row (reference: 22x22 provider-brand icon + label, translucent
 * bordered pill on the colored hero panel). Not a real OAuth flow — this app has
 * no backend, so this shortcuts into the manual IMAP/SMTP form pre-filled with the
 * provider's known host/port. */
@Composable
private fun OAuthProviderRow(label: String, letter: String, badgeColor: Color, onClick: () -> Unit) {
    val colors = AppColors
    val bg = if (colors.isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.12f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProviderBadge(letter = letter, color = badgeColor, size = 30.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.White,
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
    val bg = if (colors.isDark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.08f)
    val border = if (colors.isDark) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.18f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = imapHostLabel,
            style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.6f)),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "mail.university.edu",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        )
        HorizontalDivider(color = border)
        Spacer(Modifier.height(10.dp))
        Text(
            text = usernameLabel,
            style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.6f)),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "arlene@school.edu",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        )
        HorizontalDivider(color = border)
    }
}
