package rs.tapizlabs.mail.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import rs.tapizlabs.mail.data.local.dao.AccountDao
import rs.tapizlabs.mail.data.local.dao.CategoryDao
import rs.tapizlabs.mail.data.local.dao.CategoryRuleDao
import rs.tapizlabs.mail.data.local.dao.MessageDao
import rs.tapizlabs.mail.data.local.dao.SwipeActionConfigDao
import rs.tapizlabs.mail.data.local.entity.AccountEntity
import rs.tapizlabs.mail.data.local.entity.CategoryEntity
import rs.tapizlabs.mail.data.local.entity.CategoryMatcher
import rs.tapizlabs.mail.data.local.entity.CategoryRuleEntity
import rs.tapizlabs.mail.data.local.entity.SwipeActionConfigEntity
import rs.tapizlabs.mail.di.ApplicationScope
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

    /** Creates a category together with its initial rules in one step — so a new category
     * can be given rules in the same sheet it's created in, instead of save-then-reopen. */
    suspend fun saveCategoryWithRules(category: CategoryEntity, rules: List<CategoryRuleEntity>)

    fun observeSwipeConfig(accountId: String): Flow<SwipeActionConfigEntity?>
    suspend fun saveSwipeConfig(config: SwipeActionConfigEntity)
}

/** How much of each body BODY-rules are evaluated against when re-categorizing — matches
 * are overwhelmingly near the top, and this keeps the walk from reading whole bodies. */
private const val RULE_BODY_CHARS = 20_000
private const val RECATEGORIZE_PAGE_SIZE = 200

@Singleton
class DefaultAccountRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val categoryRuleDao: CategoryRuleDao,
    private val swipeActionConfigDao: SwipeActionConfigDao,
    private val credentialStore: CredentialStore,
    private val imapClient: ImapClient,
    private val messageDao: MessageDao,
    @ApplicationScope private val appScope: CoroutineScope,
) : AccountRepository {

    /** Serializes [recategorizeCachedMail] runs — rapid rule edits would otherwise walk and
     * rewrite the same rows concurrently, with the older rule set possibly finishing last. */
    private val recategorizeMutex = Mutex()

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
        recategorizeCachedMail()
    }

    override suspend fun saveRule(rule: CategoryRuleEntity) {
        categoryRuleDao.upsert(rule)
        recategorizeCachedMail()
    }

    override suspend fun deleteRule(rule: CategoryRuleEntity) {
        categoryRuleDao.delete(rule)
        recategorizeCachedMail()
    }

    override suspend fun saveCategoryWithRules(category: CategoryEntity, rules: List<CategoryRuleEntity>) {
        categoryDao.upsert(category)
        if (rules.isNotEmpty()) {
            categoryRuleDao.upsertAll(rules)
            recategorizeCachedMail()
        }
    }

    /** Re-runs the rules over mail that's already cached. Sync only categorizes messages as
     * they arrive, so without this a new or changed rule did nothing visible until the next
     * new mail happened to match it — and a deleted rule's messages stayed categorized. On
     * the application scope: it should finish even if the Settings screen is left. */
    private fun recategorizeCachedMail() {
        appScope.launch {
            recategorizeMutex.withLock {
                for (account in accountDao.getAllAccounts().first()) {
                    val rules = categoryRuleDao.getRulesForAccountOnce(account.id)
                    var offset = 0
                    while (true) {
                        val rows = messageDao.getCategorizableRows(account.id, RULE_BODY_CHARS, RECATEGORIZE_PAGE_SIZE, offset)
                        if (rows.isEmpty()) break
                        for (row in rows) {
                            val categoryId = CategoryMatcher.categorize(row.fromName, row.fromAddress, row.subject, row.bodyHead, rules)
                            if (categoryId != row.categoryId) messageDao.setCategory(row.id, categoryId)
                        }
                        offset += rows.size
                    }
                }
            }
        }
    }

    override fun observeSwipeConfig(accountId: String): Flow<SwipeActionConfigEntity?> =
        swipeActionConfigDao.getConfigForAccount(accountId)

    override suspend fun saveSwipeConfig(config: SwipeActionConfigEntity) =
        swipeActionConfigDao.upsert(config)
}
