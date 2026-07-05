package rs.tapizlabs.mail.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import rs.tapizlabs.mail.data.local.entity.SwipeActionConfigEntity

@Dao
interface SwipeActionConfigDao {

    @Query("SELECT * FROM swipe_action_configs WHERE accountId = :accountId")
    fun getConfigForAccount(accountId: String): Flow<SwipeActionConfigEntity?>

    @Query("SELECT * FROM swipe_action_configs WHERE accountId = :accountId")
    suspend fun getConfigForAccountOnce(accountId: String): SwipeActionConfigEntity?

    @Upsert
    suspend fun upsert(config: SwipeActionConfigEntity)

    @Query("DELETE FROM swipe_action_configs WHERE accountId = :accountId")
    suspend fun deleteForAccount(accountId: String)
}
