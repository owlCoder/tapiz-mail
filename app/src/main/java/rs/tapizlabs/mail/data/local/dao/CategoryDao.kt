package rs.tapizlabs.mail.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import rs.tapizlabs.mail.data.local.entity.CategoryEntity

@Dao
interface CategoryDao {

    /** Categories visible for an account: global (accountId IS NULL) + account-specific ones. */
    @Query("SELECT * FROM categories WHERE accountId IS NULL OR accountId = :accountId ORDER BY name ASC")
    fun getCategoriesForAccount(accountId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategoryOnce(categoryId: String): CategoryEntity?

    @Upsert
    suspend fun upsert(category: CategoryEntity)

    @Upsert
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteById(categoryId: String)
}
