package rs.tapizlabs.mail.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rs.tapizlabs.mail.data.local.entity.ConnectionSecurity
import rs.tapizlabs.mail.ui.components.MailDropdownField
import rs.tapizlabs.mail.ui.components.MailGhostButton
import rs.tapizlabs.mail.ui.components.MailPasswordField
import rs.tapizlabs.mail.ui.components.MailPrimaryButton
import rs.tapizlabs.mail.ui.components.MailPulseSpinner
import rs.tapizlabs.mail.ui.components.MailSectionHeader
import rs.tapizlabs.mail.ui.components.MailTextField
import rs.tapizlabs.mail.ui.i18n.LocalStrings
import rs.tapizlabs.mail.ui.i18n.Strings
import rs.tapizlabs.mail.ui.theme.AppColors

/**
 * Second step of the add-account flow (and the whole edit-account flow): the account
 * details form, prefilled from the provider template chosen on [ChooseProviderScreen]
 * (passed in as a nav arg, read by [AddAccountViewModel] via `SavedStateHandle`) or from
 * the existing account's stored values in edit mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddAccountViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = AppColors
    val strings = LocalStrings.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.isEditMode) strings.addAccountTitleEdit else strings.addAccountTitleNew,
                        color = colors.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            AccountDetailsForm(state = state, viewModel = viewModel, strings = strings)

            Spacer(Modifier.height(4.dp))

            ConnectionSection(state = state, onTest = viewModel::testConnection, strings = strings)

            Spacer(Modifier.height(4.dp))

            if (state.saveError != null) {
                Text(text = state.saveError.orEmpty(), color = colors.coral, style = MaterialTheme.typography.bodySmall)
            }

            MailPrimaryButton(
                text = if (state.saving) strings.savingAccount else strings.saveAccount,
                icon = Icons.Outlined.Save,
                onClick = { viewModel.save { onSaved() } },
                enabled = state.canSave,
                loading = state.saving,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AccountDetailsForm(state: AddAccountUiState, viewModel: AddAccountViewModel, strings: Strings) {
    val isCustom = state.provider == MailProvider.CUSTOM
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MailSectionHeader(title = strings.accountDetailsSectionHeader, icon = Icons.Outlined.AlternateEmail)

        MailTextField(
            value = state.displayName,
            onValueChange = viewModel::updateDisplayName,
            label = strings.fieldDisplayName,
            leadingIcon = Icons.Outlined.Badge,
        )
        MailTextField(
            value = state.emailAddress,
            onValueChange = viewModel::updateEmailAddress,
            label = strings.fieldEmailAddress,
            leadingIcon = Icons.Outlined.AlternateEmail,
            keyboardType = KeyboardType.Email,
        )
        MailTextField(
            value = state.username,
            onValueChange = viewModel::updateUsername,
            label = strings.fieldUsername,
            leadingIcon = Icons.Outlined.PersonOutline,
            supportingText = strings.fieldUsernameHint,
        )
        MailPasswordField(
            value = state.password,
            onValueChange = viewModel::updatePassword,
            label = strings.fieldPassword,
            imeAction = ImeAction.Next,
        )

        Spacer(Modifier.height(8.dp))
        MailSectionHeader(title = strings.incomingMailSectionHeader, icon = Icons.Outlined.Dns)
        MailTextField(
            value = state.imapHost,
            onValueChange = viewModel::updateImapHost,
            label = strings.fieldImapHost,
            leadingIcon = Icons.Outlined.Dns,
            enabled = isCustom || state.isEditMode,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MailTextField(
                value = state.imapPort,
                onValueChange = viewModel::updateImapPort,
                label = strings.fieldPort,
                leadingIcon = Icons.Outlined.Numbers,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
                enabled = isCustom || state.isEditMode,
            )
            MailDropdownField(
                label = strings.fieldSecurity,
                options = ConnectionSecurity.entries,
                selected = state.imapSecurity,
                optionLabel = { it.name },
                onSelect = viewModel::updateImapSecurity,
                modifier = Modifier.weight(1.4f),
            )
        }

        Spacer(Modifier.height(8.dp))
        MailSectionHeader(title = strings.outgoingMailSectionHeader, icon = Icons.AutoMirrored.Outlined.Send)
        MailTextField(
            value = state.smtpHost,
            onValueChange = viewModel::updateSmtpHost,
            label = strings.fieldSmtpHost,
            leadingIcon = Icons.Outlined.Dns,
            enabled = isCustom || state.isEditMode,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MailTextField(
                value = state.smtpPort,
                onValueChange = viewModel::updateSmtpPort,
                label = strings.fieldPort,
                leadingIcon = Icons.Outlined.Numbers,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
                enabled = isCustom || state.isEditMode,
            )
            MailDropdownField(
                label = strings.fieldSecurity,
                options = ConnectionSecurity.entries,
                selected = state.smtpSecurity,
                optionLabel = { it.name },
                onSelect = viewModel::updateSmtpSecurity,
                modifier = Modifier.weight(1.4f),
            )
        }
    }
}

@Composable
private fun ConnectionSection(state: AddAccountUiState, onTest: () -> Unit, strings: Strings) {
    val colors = AppColors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MailGhostButton(
            text = if (state.connectionTestState == ConnectionTestState.TESTING) strings.testingConnection else strings.testConnection,
            icon = Icons.Outlined.NetworkCheck,
            onClick = onTest,
            enabled = state.connectionTestState != ConnectionTestState.TESTING,
            modifier = Modifier.fillMaxWidth(),
        )

        when (state.connectionTestState) {
            ConnectionTestState.TESTING -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MailPulseSpinner(size = 22.dp, showIcon = false)
                Text(text = strings.verifyingSettings, color = colors.textMuted, style = MaterialTheme.typography.bodySmall)
            }
            ConnectionTestState.SUCCESS -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = colors.mint, modifier = Modifier.size(16.dp))
                Text(text = strings.connectionVerified, color = colors.mint, style = MaterialTheme.typography.bodySmall)
            }
            ConnectionTestState.FAILED -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Error, contentDescription = null, tint = colors.coral, modifier = Modifier.size(16.dp))
                Text(
                    text = state.connectionError ?: strings.connectionFailed,
                    color = colors.coral,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            ConnectionTestState.IDLE -> Unit
        }
    }
}
