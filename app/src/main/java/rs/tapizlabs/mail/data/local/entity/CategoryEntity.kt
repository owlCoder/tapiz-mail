package rs.tapizlabs.mail.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A user- or system-defined label used by the rule-based categorization engine.
 *
 * [accountId] is nullable: null means the category applies across all accounts.
 * [colorIndex] indexes into the theme's `categoryTints` list rather than storing a hardcoded
 * color, so categories stay theme-consistent (light/dark) without duplicating color values.
 */
@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("accountId")],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val accountId: String?,
    val name: String,
    val colorIndex: Int,
    val isSystemDefault: Boolean,
)
