package rs.tapizlabs.mail.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Which message field a [CategoryRuleEntity] is matched against. */
enum class RuleMatchField {
    SENDER,
    SUBJECT,
    BODY,
}

/** How [CategoryRuleEntity.matchValue] is compared against the target field. */
enum class RuleMatchType {
    CONTAINS,
    EQUALS,
    STARTS_WITH,
}

/**
 * A single heuristic rule feeding the on-device categorization engine ([CategoryMatcher]).
 * A category can have multiple rules; any single match assigns the category.
 */
@Entity(
    tableName = "category_rules",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("categoryId")],
)
data class CategoryRuleEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val matchField: RuleMatchField,
    val matchType: RuleMatchType,
    val matchValue: String,
)
