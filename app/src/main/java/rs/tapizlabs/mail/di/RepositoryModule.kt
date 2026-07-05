package rs.tapizlabs.mail.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import rs.tapizlabs.mail.data.repository.AccountRepository
import rs.tapizlabs.mail.data.repository.DefaultAccountRepository
import rs.tapizlabs.mail.data.repository.DefaultMailSyncGateway
import rs.tapizlabs.mail.data.repository.MailRepository
import rs.tapizlabs.mail.data.repository.MailSyncGateway
import rs.tapizlabs.mail.data.repository.RoomMailRepository
import javax.inject.Singleton

/**
 * Binds [MailRepository] to its Room-backed implementation for injection into the four UI
 * ViewModels (Inbox/Detail/Compose/Search), and [MailSyncGateway] to the real IMAP/SMTP-backed
 * [DefaultMailSyncGateway] (replaces the earlier `NoOpMailSyncGateway` placeholder now that the
 * protocol/sync layer exists).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMailRepository(impl: RoomMailRepository): MailRepository

    @Binds
    @Singleton
    abstract fun bindMailSyncGateway(impl: DefaultMailSyncGateway): MailSyncGateway

    /** Account CRUD + connection-test facade for Add-Account/Settings — see
     * `data/repository/AccountRepository.kt` for why this is separate from [MailRepository]. */
    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: DefaultAccountRepository): AccountRepository
}
