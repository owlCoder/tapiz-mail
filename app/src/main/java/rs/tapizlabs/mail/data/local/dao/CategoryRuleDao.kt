package rs.tapizlabs.mail.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import rs.tapizlabs.mail.data.local.entity.CategoryRuleEntity

@Dao
interface CategoryRuleDao {

    @Query("SELECT * FROM category_rules WHERE categoryId = :categoryId")
    fun getRulesForCategory(categoryId: String): Flow<List<CategoryRuleEntity>>

    /** All rules, e.g. for the sync layer to run [rs.tapizlabs.mail.data.local.entity.CategoryMatcher] once per account. */
    @Query("SELECT * FROM category_rules")
    suspend fun getAllRulesOnce(): List<CategoryRuleEntity>

    /** Rules that apply to [accountId]'s mail: those of global categories plus those of the
     * account's own categories — never another account's. */
    @Query(
        """
        SELECT category_rules.* FROM category_rules
        INNER JOIN categories ON category_rules.categoryId = categories.id
        WHERE categories.accountId IS NULL OR categories.accountId = :accountId
        """
    )
    suspend fun getRulesForAccountOnce(accountId: String): List<CategoryRuleEntity>

    @Upsert
    suspend fun upsert(rule: CategoryRuleEntity)

    @Upsert
    suspend fun upsertAll(rules: List<CategoryRuleEntity>)

    @Delete
    suspend fun delete(rule: CategoryRuleEntity)

    @Query("DELETE FROM category_rules WHERE categoryId = :categoryId")
    suspend fun deleteAllForCategory(categoryId: String)
}
