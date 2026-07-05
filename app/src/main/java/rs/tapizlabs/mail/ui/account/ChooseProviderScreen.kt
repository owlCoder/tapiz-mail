package rs.tapizlabs.mail.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import rs.tapizlabs.mail.ui.components.MailCard
import rs.tapizlabs.mail.ui.components.MailIconChip
import rs.tapizlabs.mail.ui.components.MailSectionHeader
import rs.tapizlabs.mail.ui.theme.AppColors
import androidx.compose.material.icons.filled.CheckCircle

/**
 * First step of the add-account flow: pick a provider (Gmail/Outlook prefill known
 * host/port/security, Custom leaves them blank for the user's own IMAP/SMTP server —
 * including a university/school mailbox). Picking one navigates forward to
 * [AddAccountScreen] (the account-details form) as its own page, rather than expanding
 * this screen in place with a form appended below the chooser.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseProviderScreen(onBack: () -> Unit, onProviderChosen: (MailProvider) -> Unit) {
    val colors = AppColors

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Add account", color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = colors.canvasTop,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MailSectionHeader(title = "Choose a provider", icon = Icons.Outlined.Mail)
            ProviderOptionRow(
                title = "Gmail",
                description = "imap.gmail.com · smtp.gmail.com",
                onClick = { onProviderChosen(MailProvider.GMAIL) },
            )
            ProviderOptionRow(
                title = "Outlook",
                description = "outlook.office365.com · smtp.office365.com",
                onClick = { onProviderChosen(MailProvider.OUTLOOK) },
            )
            ProviderOptionRow(
                title = "Custom (IMAP/SMTP)",
                description = "Any other provider — including university/school mail",
                onClick = { onProviderChosen(MailProvider.CUSTOM) },
            )
        }
    }
}

@Composable
private fun ProviderOptionRow(title: String, description: String, onClick: () -> Unit) {
    val colors = AppColors
    MailCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MailIconChip(icon = Icons.Outlined.Mail)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = title, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                Text(text = description, color = colors.textMuted, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = colors.textMuted.copy(alpha = 0.3f))
        }
    }
}
