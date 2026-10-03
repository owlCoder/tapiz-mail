package rs.tapizlabs.mail.ui.account

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.tapizlabs.mail.data.local.entity.AccountEntity
import rs.tapizlabs.mail.data.local.entity.ConnectionSecurity
import rs.tapizlabs.mail.data.repository.AccountRepository
import rs.tapizlabs.mail.data.repository.MailSyncGateway
import rs.tapizlabs.mail.security.CredentialStore
import rs.tapizlabs.mail.sync.SyncScheduler

/** Known-provider templates the chooser step prefills — anything else (including the
 * user's UNS university mail) falls through to Custom with blank host/port fields. */
enum class MailProvider { GMAIL, OUTLOOK, CUSTOM }

private data class ProviderTemplate(
    val imapHost: String,
    val imapPort: Int,
    val imapSecurity: ConnectionSecurity,
    val smtpHost: String,
    val smtpPort: Int,
    val smtpSecurity: ConnectionSecurity,
)

private val providerTemplates = mapOf(
    MailProvider.GMAIL to ProviderTemplate(
        imapHost = "imap.gmail.com", imapPort = 993, imapSecurity = ConnectionSecurity.SSL_TLS,
        smtpHost = "smtp.gmail.com", smtpPort = 587, smtpSecurity = ConnectionSecurity.STARTTLS,
    ),
    MailProvider.OUTLOOK to ProviderTemplate(
        imapHost = "outlook.office365.com", imapPort = 993, imapSecurity = ConnectionSecurity.SSL_TLS,
        smtpHost = "smtp.office365.com", smtpPort = 587, smtpSecurity = ConnectionSecurity.STARTTLS,
    ),
)

enum class ConnectionTestState { IDLE, TESTING, SUCCESS, FAILED }

data class AddAccountUiState(
    val isEditMode: Boolean = false,
    val provider: MailProvider? = null,
    val displayName: String = "",
    val emailAddress: String = "",
    val username: String = "",
    val password: String = "",
    val imapHost: String = "",
    val imapPort: String = "993",
    val imapSecurity: ConnectionSecurity = ConnectionSecurity.SSL_TLS,
    val smtpHost: String = "",
    val smtpPort: String = "587",
    val smtpSecurity: ConnectionSecurity = ConnectionSecurity.STARTTLS,
    val syncIntervalMinutes: Int = 15,
    val connectionTestState: ConnectionTestState = ConnectionTestState.IDLE,
    val connectionError: String? = null,
    val probedSupportsIdle: Boolean = false,
    val saving: Boolean = false,
    val saveError: String? = null,
) {
    /** Save is gated on a successful test in this session — mirrors the guideline that the
     * server (here: the IMAP handshake) remains the source of truth, not a client-side guess. */
    val canSave: Boolean
        get() = displayName.isNotBlank() && emailAddress.isNotBlank() && username.isNotBlank() &&
            password.isNotBlank() && imapHost.isNotBlank() && smtpHost.isNotBlank() &&
            connectionTestState == ConnectionTestState.SUCCESS && !saving
}

@HiltViewModel
class AddAccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val credentialStore: CredentialStore,
    private val syncScheduler: SyncScheduler,
    private val syncGateway: MailSyncGateway,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val editingAccountId: String? = savedStateHandle.get<String>("accountId")

    /** Preserves the original account's `createdAt`/`sortOrder`/`supportsIdle`/`isActive`
     * across an edit-mode save rather than resetting them — populated once the existing
     * account loads in [init]. */
    private var editingAccountSnapshot: AccountEntity? = null

    private val _state = MutableStateFlow(AddAccountUiState(isEditMode = editingAccountId != null))
    val state: StateFlow<AddAccountUiState> = _state.asStateFlow()

    init {
        if (editingAccountId != null) {
            viewModelScope.launch {
                val account = accountRepository.getAccountOnce(editingAccountId) ?: return@launch
                editingAccountSnapshot = account
                val imapPassword = credentialStore.getImapPassword(editingAccountId).orEmpty()
                _state.update {
                    it.copy(
                        provider = MailProvider.CUSTOM,
                        displayName = account.displayName,
                        emailAddress = account.emailAddress,
                        username = account.username,
                        password = imapPassword,
                        imapHost = account.imapHost,
                        imapPort = account.imapPort.toString(),
                        imapSecurity = account.imapSecurity,
                        smtpHost = account.smtpHost,
                        smtpPort = account.smtpPort.toString(),
                        smtpSecurity = account.smtpSecurity,
                        syncIntervalMinutes = account.syncIntervalMinutes,
                        // Editing an existing, presumably-working account doesn't require
                        // re-testing before Save is allowed.
                        connectionTestState = ConnectionTestState.SUCCESS,
                        probedSupportsIdle = account.supportsIdle,
                    )
                }
            }
        } else {
            // First-run add flow: the provider was already chosen on ChooseProviderScreen
            // and travels here as a nav arg (see Routes.ADD_ACCOUNT_DETAILS) rather than
            // being picked on this screen, so apply its template immediately.
            savedStateHandle.get<String>("provider")
                ?.let { runCatching { MailProvider.valueOf(it) }.getOrNull() }
                ?.let(::applyProviderTemplate)
        }
    }

    private fun applyProviderTemplate(provider: MailProvider) {
        val template = providerTemplates[provider]
        _state.update {
            if (template != null) {
                it.copy(
                    provider = provider,
                    imapHost = template.imapHost,
                    imapPort = template.imapPort.toString(),
                    imapSecurity = template.imapSecurity,
                    smtpHost = template.smtpHost,
                    smtpPort = template.smtpPort.toString(),
                    smtpSecurity = template.smtpSecurity,
                    connectionTestState = ConnectionTestState.IDLE,
                )
            } else {
                it.copy(provider = provider, connectionTestState = ConnectionTestState.IDLE)
            }
        }
    }

    fun updateDisplayName(value: String) = invalidateTest { it.copy(displayName = value) }
    /** Keeps the username in step with the email while the two still match — it's the same
     * value for nearly every provider, so the user shouldn't have to type it twice. */
    fun updateEmailAddress(value: String) = invalidateTest {
        val usernameFollows = it.username.isEmpty() || it.username == it.emailAddress
        it.copy(emailAddress = value, username = if (usernameFollows) value else it.username)
    }
    fun updateUsername(value: String) = invalidateTest { it.copy(username = value) }
    fun updatePassword(value: String) = invalidateTest { it.copy(password = value) }
    fun updateImapHost(value: String) = invalidateTest { it.copy(imapHost = value) }
    fun updateImapPort(value: String) = invalidateTest { it.copy(imapPort = value) }
    fun updateImapSecurity(value: ConnectionSecurity) = invalidateTest { it.copy(imapSecurity = value) }
    fun updateSmtpHost(value: String) = invalidateTest { it.copy(smtpHost = value) }
    fun updateSmtpPort(value: String) = invalidateTest { it.copy(smtpPort = value) }
    fun updateSmtpSecurity(value: ConnectionSecurity) = invalidateTest { it.copy(smtpSecurity = value) }
    fun updateSyncInterval(value: Int) = _state.update { it.copy(syncIntervalMinutes = value) }

    private inline fun invalidateTest(crossinline update: (AddAccountUiState) -> AddAccountUiState) {
        _state.update { update(it).copy(connectionTestState = ConnectionTestState.IDLE, connectionError = null) }
    }

    fun testConnection() {
        val current = _state.value
        if (current.connectionTestState == ConnectionTestState.TESTING) return
        val port = current.imapPort.toIntOrNull()?.takeIf { it in 1..65535 }
        val smtpPort = current.smtpPort.toIntOrNull()?.takeIf { it in 1..65535 }
        if (port == null || smtpPort == null) {
            val which = if (port == null) "IMAP" else "SMTP"
            _state.update { it.copy(connectionTestState = ConnectionTestState.FAILED, connectionError = "$which port?") }
            return
        }
        val probeAccount = AccountEntity(
            id = editingAccountId ?: "probe",
            displayName = current.displayName,
            emailAddress = current.emailAddress.trim(),
            imapHost = current.imapHost.trim(),
            imapPort = port,
            imapSecurity = current.imapSecurity,
            smtpHost = current.smtpHost.trim(),
            smtpPort = smtpPort,
            smtpSecurity = current.smtpSecurity,
            username = current.username.trim(),
            syncIntervalMinutes = current.syncIntervalMinutes,
            supportsIdle = false,
            isActive = true,
            sortOrder = 0,
            createdAt = System.currentTimeMillis(),
        )

        viewModelScope.launch {
            _state.update { it.copy(connectionTestState = ConnectionTestState.TESTING, connectionError = null) }
            // Both directions: incoming (IMAP, also probes IDLE) and outgoing (SMTP). Testing
            // only IMAP let an account with wrong SMTP settings be saved as "verified" and
            // fail on the first message the user tried to send.
            val imapResult = accountRepository.testConnectionWithIdleProbe(probeAccount, current.password)
            val failure = imapResult.exceptionOrNull()?.let { "IMAP: ${it.message}" }
                ?: syncGateway.testSmtpConnection(probeAccount, current.password)
                    .exceptionOrNull()?.let { "SMTP: ${it.message}" }
            _state.update {
                // Ignore the outcome if the form was edited while the test was running —
                // those results describe settings that are no longer on screen.
                if (it.connectionTestState != ConnectionTestState.TESTING) return@update it
                if (failure == null) {
                    it.copy(
                        connectionTestState = ConnectionTestState.SUCCESS,
                        probedSupportsIdle = imapResult.getOrDefault(false),
                    )
                } else {
                    it.copy(connectionTestState = ConnectionTestState.FAILED, connectionError = failure)
                }
            }
        }
    }

    /** Removes the account being edited (its cached mail goes with it via foreign keys) and
     * stops its background sync. Only meaningful in edit mode. */
    fun removeAccount(onRemoved: () -> Unit) {
        val account = editingAccountSnapshot ?: return
        viewModelScope.launch {
            accountRepository.deleteAccount(account)
            syncScheduler.cancelFor(account.id)
            onRemoved()
        }
    }

    fun save(onSaved: (AccountEntity) -> Unit) {
        val current = _state.value
        if (!current.canSave) return

        viewModelScope.launch {
            _state.update { it.copy(saving = true, saveError = null) }
            val existing = editingAccountSnapshot
            val account = AccountEntity(
                id = editingAccountId ?: UUID.randomUUID().toString(),
                displayName = current.displayName.trim(),
                emailAddress = current.emailAddress.trim(),
                imapHost = current.imapHost.trim(),
                imapPort = current.imapPort.toIntOrNull() ?: 993,
                imapSecurity = current.imapSecurity,
                smtpHost = current.smtpHost.trim(),
                smtpPort = current.smtpPort.toIntOrNull() ?: 587,
                smtpSecurity = current.smtpSecurity,
                username = current.username.trim(),
                syncIntervalMinutes = current.syncIntervalMinutes,
                // Re-probed on every successful test (including edit-mode re-saves without a
                // fresh test, where probedSupportsIdle was seeded from the existing account in
                // init) rather than trusting a stale existing?.supportsIdle across host/port
                // changes.
                supportsIdle = current.probedSupportsIdle,
                isActive = existing?.isActive ?: true,
                sortOrder = existing?.sortOrder ?: 0,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            )
            accountRepository.saveAccount(account, current.password, current.password)
            syncScheduler.scheduleFor(account)
            _state.update { it.copy(saving = false) }
            // Kick off an immediate fetch instead of waiting for the first periodic
            // WorkManager run — a newly added account should show mail right away, not
            // whenever its sync interval next fires. On the application scope: onSaved
            // navigates away, which cancels this ViewModel's scope mid-sync otherwise.
            syncGateway.refreshInBackground(account.id)
            onSaved(account)
        }
    }
}
