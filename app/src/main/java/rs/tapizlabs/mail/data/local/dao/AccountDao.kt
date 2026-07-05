package rs.tapizlabs.mail.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import rs.tapizlabs.mail.data.local.entity.AccountEntity

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts ORDER BY sortOrder ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :accountId")
    fun getAccount(accountId: String): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE id = :accountId")
    suspend fun getAccountOnce(accountId: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY sortOrder ASC")
    fun getActiveAccounts(): Flow<List<AccountEntity>>

    @Upsert
    suspend fun upsert(account: AccountEntity)

    @Upsert
    suspend fun upsertAll(accounts: List<AccountEntity>)

    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :accountId")
    suspend fun deleteById(accountId: String)
}
