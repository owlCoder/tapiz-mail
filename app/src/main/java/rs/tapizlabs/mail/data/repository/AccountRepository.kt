package rs.tapizlabs.mail.data.repository

import kotlinx.coroutines.flow.Flow
import rs.tapizlabs.mail.data.local.dao.AccountDao
import rs.tapizlabs.mail.data.local.dao.CategoryDao
import rs.tapizlabs.mail.data.local.dao.CategoryRuleDao
import rs.tapizlabs.mail.data.local.dao.SwipeActionConfigDao
import rs.tapizlabs.mail.data.local.entity.AccountEntity
import rs.tapizlabs.mail.data.local.entity.CategoryEntity
import rs.tapizlabs.mail.data.local.entity.CategoryRuleEntity
import rs.tapizlabs.mail.data.local.entity.SwipeActionConfigEntity
import rs.tapizlabs.mail.mail.ImapClient
import rs.tapizlabs.mail.security.CredentialStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Account CRUD + connection-verification facade for the Add-Account and Settings screens.
 * Separate from [MailRepository] (which is scoped to Inbox/Detail/Compose/Search's read
 * needs) since this owns account/category/rule/swipe-config *writes*, credential storage, and
 * IMAP connection testing — none of which the read-only message repository needs.
 *
 * NOTE: `SyncScheduler` (scheduleFor/cancelFor/rescheduleAll) is owned by the sync-layer agent
 * and hadn't landed in `sync/` as of this writing. Callers (view models) invoke it directly
 * where available; this repository does not depend on it to avoid a compile-time dependency on
 * a package that may not exist yet.
 */
interface AccountRepository {
    fun observeAccounts(): Flow<List<AccountEntity>>
    suspend fun getAccountOnce(accountId: String): AccountEntity?
    suspend fun testConnection(account: AccountEntity, imapPassword: String): Result<Unit>
    suspend fun testConnectionWithIdleProbe(account: AccountEntity, imapPassword: String): Result<Boolean>
    suspend fun saveAccount(account: AccountEntity, imapPassword: String, smtpPassword: String)
    suspend fun deleteAccount(account: AccountEntity)

    fun observeAllCategories(): Flow<List<CategoryEntity>>
    fun observeRulesForCategory(categoryId: String): Flow<List<CategoryRuleEntity>>
    suspend fun saveCategory(category: CategoryEntity)
    suspend fun deleteCategory(category: CategoryEntity)
    suspend fun saveRule(rule: CategoryRuleEntity)
    suspend fun deleteRule(rule: CategoryRuleEntity)

    fun observeSwipeConfig(accountId: String): Flow<SwipeActionConfigEntity?>
    suspend fun saveSwipeConfig(config: SwipeActionConfigEntity)
}

@Singleton
class DefaultAccountRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val categoryRuleDao: CategoryRuleDao,
    private val swipeActionConfigDao: SwipeActionConfigDao,
    private val credentialStore: CredentialStore,
    private val imapClient: ImapClient,
) : AccountRepository {

    override fun observeAccounts(): Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    override suspend fun getAccountOnce(accountId: String): AccountEntity? =
        accountDao.getAccountOnce(accountId)

    override suspend fun testConnection(account: AccountEntity, imapPassword: String): Result<Unit> =
        imapClient.testConnection(account, imapPassword)

    override suspend fun testConnectionWithIdleProbe(account: AccountEntity, imapPassword: String): Result<Boolean> =
        imapClient.testConnectionWithIdleProbe(account, imapPassword)

    override suspend fun saveAccount(account: AccountEntity, imapPassword: String, smtpPassword: String) {
        accountDao.upsert(account)
        credentialStore.saveImapPassword(account.id, imapPassword)
        credentialStore.saveSmtpPassword(account.id, smtpPassword)
    }

    override suspend fun deleteAccount(account: AccountEntity) {
        credentialStore.deleteCredentials(account.id)
        accountDao.delete(account)
    }

    override fun observeAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    override fun observeRulesForCategory(categoryId: String): Flow<List<CategoryRuleEntity>> =
        categoryRuleDao.getRulesForCategory(categoryId)

    override suspend fun saveCategory(category: CategoryEntity) = categoryDao.upsert(category)

    override suspend fun deleteCategory(category: CategoryEntity) {
        categoryRuleDao.deleteAllForCategory(category.id)
        categoryDao.delete(category)
    }

    override suspend fun saveRule(rule: CategoryRuleEntity) = categoryRuleDao.upsert(rule)

    override suspend fun deleteRule(rule: CategoryRuleEntity) = categoryRuleDao.delete(rule)

    override fun observeSwipeConfig(accountId: String): Flow<SwipeActionConfigEntity?> =
        swipeActionConfigDao.getConfigForAccount(accountId)

    override suspend fun saveSwipeConfig(config: SwipeActionConfigEntity) =
        swipeActionConfigDao.upsert(config)
}
