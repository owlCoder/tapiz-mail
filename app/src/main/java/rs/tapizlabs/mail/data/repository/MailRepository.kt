package rs.tapizlabs.mail.data.repository

import kotlinx.coroutines.flow.Flow
import rs.tapizlabs.mail.data.local.dao.AccountDao
import rs.tapizlabs.mail.data.local.dao.AttachmentDao
import rs.tapizlabs.mail.data.local.dao.CategoryDao
import rs.tapizlabs.mail.data.local.dao.MessageDao
import rs.tapizlabs.mail.data.local.entity.AccountEntity
import rs.tapizlabs.mail.data.local.entity.AttachmentEntity
import rs.tapizlabs.mail.data.local.entity.CategoryEntity
import rs.tapizlabs.mail.data.local.entity.MessageEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read/local-mutation facade over the Room DAOs for the four UI screens (Inbox, Detail,
 * Compose, Search) — screens/ViewModels never call DAOs directly per project convention.
 *
 * This does NOT cover network I/O (IMAP fetch, SMTP send): those are exposed separately via
 * [MailSyncGateway], implemented by the sync-layer agent's work, so this repository stays a
 * thin, easily-testable Room wrapper.
 */
interface MailRepository {
    fun observeAccounts(): Flow<List<AccountEntity>>
    fun observeActiveAccounts(): Flow<List<AccountEntity>>
    suspend fun getAccountOnce(accountId: String): AccountEntity?

    fun observeMessagesForAccount(accountId: String): Flow<List<MessageEntity>>
    fun observeMessagesForFolder(folderId: String): Flow<List<MessageEntity>>
    fun observeMessagesForCategory(categoryId: String): Flow<List<MessageEntity>>
    fun observeMessage(messageId: String): Flow<MessageEntity?>
    suspend fun getMessageOnce(messageId: String): MessageEntity?
    fun searchMessages(query: String): Flow<List<MessageEntity>>

    fun observeCategoriesForAccount(accountId: String): Flow<List<CategoryEntity>>
    fun observeAllCategories(): Flow<List<CategoryEntity>>

    fun observeAttachmentsForMessage(messageId: String): Flow<List<AttachmentEntity>>

    suspend fun setRead(messageId: String, isRead: Boolean)
    suspend fun setStarred(messageId: String, isStarred: Boolean)
    suspend fun setCategory(messageId: String, categoryId: String?)
    suspend fun deleteMessage(messageId: String)
}

@Singleton
class RoomMailRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val messageDao: MessageDao,
    private val categoryDao: CategoryDao,
    private val attachmentDao: AttachmentDao,
) : MailRepository {

    override fun observeAccounts(): Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    override fun observeActiveAccounts(): Flow<List<AccountEntity>> = accountDao.getActiveAccounts()

    override suspend fun getAccountOnce(accountId: String): AccountEntity? =
        accountDao.getAccountOnce(accountId)

    override fun observeMessagesForAccount(accountId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForAccount(accountId)

    override fun observeMessagesForFolder(folderId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForFolder(folderId)

    override fun observeMessagesForCategory(categoryId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForCategory(categoryId)

    override fun observeMessage(messageId: String): Flow<MessageEntity?> =
        messageDao.getMessage(messageId)

    override suspend fun getMessageOnce(messageId: String): MessageEntity? =
        messageDao.getMessageOnce(messageId)

    override fun searchMessages(query: String): Flow<List<MessageEntity>> =
        messageDao.searchMessages(query)

    override fun observeCategoriesForAccount(accountId: String): Flow<List<CategoryEntity>> =
        categoryDao.getCategoriesForAccount(accountId)

    override fun observeAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    override fun observeAttachmentsForMessage(messageId: String): Flow<List<AttachmentEntity>> =
        attachmentDao.getAttachmentsForMessage(messageId)

    override suspend fun setRead(messageId: String, isRead: Boolean) =
        messageDao.setRead(messageId, isRead)

    override suspend fun setStarred(messageId: String, isStarred: Boolean) =
        messageDao.setStarred(messageId, isStarred)

    override suspend fun setCategory(messageId: String, categoryId: String?) =
        messageDao.setCategory(messageId, categoryId)

    override suspend fun deleteMessage(messageId: String) = messageDao.deleteById(messageId)
}
