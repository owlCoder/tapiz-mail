package rs.tapizlabs.mail.data.local.entity

/**
 * Pure, on-device rule evaluation for the non-ML category/label system.
 *
 * This lives alongside the entities it evaluates because it is tightly coupled to their schema
 * (no I/O, no coroutine/DB access). It is intentionally NOT wired into any sync pipeline here —
 * the sync-layer agent is responsible for calling [categorize] once messages/rules are loaded.
 */
object CategoryMatcher {

    /**
     * Returns the id of the first [rules] entry whose category matches [message], or null if
     * no rule matches. Rules are evaluated in list order; the caller controls priority via order.
     */
    fun categorize(message: MessageEntity, rules: List<CategoryRuleEntity>): String? =
        rules.firstOrNull { rule -> matches(message, rule) }?.categoryId

    private fun matches(message: MessageEntity, rule: CategoryRuleEntity): Boolean {
        val fieldValue = when (rule.matchField) {
            RuleMatchField.SENDER -> "${message.fromName} ${message.fromAddress}"
            RuleMatchField.SUBJECT -> message.subject
            RuleMatchField.BODY -> message.bodyPlain
        }
        return matchesValue(fieldValue, rule.matchType, rule.matchValue)
    }

    private fun matchesValue(fieldValue: String, matchType: RuleMatchType, matchValue: String): Boolean {
        val haystack = fieldValue.lowercase()
        val needle = matchValue.lowercase()
        return when (matchType) {
            RuleMatchType.CONTAINS -> haystack.contains(needle)
            RuleMatchType.EQUALS -> haystack == needle
            RuleMatchType.STARTS_WITH -> haystack.startsWith(needle)
        }
    }
}
