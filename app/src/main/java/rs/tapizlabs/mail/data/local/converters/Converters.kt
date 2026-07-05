package rs.tapizlabs.mail.data.local.converters

import androidx.room.TypeConverter
import rs.tapizlabs.mail.data.local.entity.ConnectionSecurity
import rs.tapizlabs.mail.data.local.entity.FolderType
import rs.tapizlabs.mail.data.local.entity.RuleMatchField
import rs.tapizlabs.mail.data.local.entity.RuleMatchType
import rs.tapizlabs.mail.data.local.entity.SwipeAction

/** Room [TypeConverter]s for the enums used across the local persistence entities. */
class Converters {

    @TypeConverter
    fun fromConnectionSecurity(value: ConnectionSecurity): String = value.name

    @TypeConverter
    fun toConnectionSecurity(value: String): ConnectionSecurity = ConnectionSecurity.valueOf(value)

    @TypeConverter
    fun fromFolderType(value: FolderType): String = value.name

    @TypeConverter
    fun toFolderType(value: String): FolderType = FolderType.valueOf(value)

    @TypeConverter
    fun fromRuleMatchField(value: RuleMatchField): String = value.name

    @TypeConverter
    fun toRuleMatchField(value: String): RuleMatchField = RuleMatchField.valueOf(value)

    @TypeConverter
    fun fromRuleMatchType(value: RuleMatchType): String = value.name

    @TypeConverter
    fun toRuleMatchType(value: String): RuleMatchType = RuleMatchType.valueOf(value)

    @TypeConverter
    fun fromSwipeAction(value: SwipeAction): String = value.name

    @TypeConverter
    fun toSwipeAction(value: String): SwipeAction = SwipeAction.valueOf(value)
}
