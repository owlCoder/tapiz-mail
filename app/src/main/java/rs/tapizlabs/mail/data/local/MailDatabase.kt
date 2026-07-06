package rs.tapizlabs.mail.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import rs.tapizlabs.mail.data.local.converters.Converters
import rs.tapizlabs.mail.data.local.dao.AccountDao
import rs.tapizlabs.mail.data.local.dao.AttachmentDao
import rs.tapizlabs.mail.data.local.dao.CategoryDao
import rs.tapizlabs.mail.data.local.dao.CategoryRuleDao
import rs.tapizlabs.mail.data.local.dao.FolderDao
import rs.tapizlabs.mail.data.local.dao.MessageDao
import rs.tapizlabs.mail.data.local.dao.SwipeActionConfigDao
import rs.tapizlabs.mail.data.local.entity.AccountEntity
import rs.tapizlabs.mail.data.local.entity.AttachmentEntity
import rs.tapizlabs.mail.data.local.entity.CategoryEntity
import rs.tapizlabs.mail.data.local.entity.CategoryRuleEntity
import rs.tapizlabs.mail.data.local.entity.FolderEntity
import rs.tapizlabs.mail.data.local.entity.MessageEntity
import rs.tapizlabs.mail.data.local.entity.SwipeActionConfigEntity

/**
 * The single on-device Room database for Tapiz Mail. Fully local — there is no backend, so this
 * is the source of truth for accounts, folders, messages, attachments and categorization rules.
 *
 * Credentials are NOT stored here; see [rs.tapizlabs.mail.security.CredentialStore].
 */
@Database(
    entities = [
        AccountEntity::class,
        FolderEntity::class,
        MessageEntity::class,
        AttachmentEntity::class,
        CategoryEntity::class,
        CategoryRuleEntity::class,
        SwipeActionConfigEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class MailDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun folderDao(): FolderDao
    abstract fun messageDao(): MessageDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun categoryDao(): CategoryDao
    abstract fun categoryRuleDao(): CategoryRuleDao
    abstract fun swipeActionConfigDao(): SwipeActionConfigDao

    companion object {
        const val DATABASE_NAME = "tapiz_mail.db"

        /** Adds [rs.tapizlabs.mail.data.local.entity.MessageEntity.originFolderId], used to
         * remember a message's real IMAP folder after it's moved into the local-only Trash
         * pseudo-folder, so a later "delete forever"/read-flag sync can still find it on the
         * server. Nullable with no default needed at the SQL level beyond `NULL`, since
         * existing rows (never moved to local Trash under the old schema) correctly have no
         * origin folder to record. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN originFolderId TEXT DEFAULT NULL")
            }
        }
    }
}
