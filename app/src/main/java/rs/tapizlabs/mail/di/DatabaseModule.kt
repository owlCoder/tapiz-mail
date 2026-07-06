package rs.tapizlabs.mail.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import rs.tapizlabs.mail.data.local.MailDatabase
import rs.tapizlabs.mail.data.local.dao.AccountDao
import rs.tapizlabs.mail.data.local.dao.AttachmentDao
import rs.tapizlabs.mail.data.local.dao.CategoryDao
import rs.tapizlabs.mail.data.local.dao.CategoryRuleDao
import rs.tapizlabs.mail.data.local.dao.FolderDao
import rs.tapizlabs.mail.data.local.dao.MessageDao
import rs.tapizlabs.mail.data.local.dao.SwipeActionConfigDao
import javax.inject.Singleton

/** Provides the singleton [MailDatabase] and its DAOs for injection. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMailDatabase(@ApplicationContext context: Context): MailDatabase =
        Room.databaseBuilder(context, MailDatabase::class.java, MailDatabase.DATABASE_NAME)
            .addMigrations(MailDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideAccountDao(database: MailDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideFolderDao(database: MailDatabase): FolderDao = database.folderDao()

    @Provides
    fun provideMessageDao(database: MailDatabase): MessageDao = database.messageDao()

    @Provides
    fun provideAttachmentDao(database: MailDatabase): AttachmentDao = database.attachmentDao()

    @Provides
    fun provideCategoryDao(database: MailDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideCategoryRuleDao(database: MailDatabase): CategoryRuleDao = database.categoryRuleDao()

    @Provides
    fun provideSwipeActionConfigDao(database: MailDatabase): SwipeActionConfigDao =
        database.swipeActionConfigDao()
}
