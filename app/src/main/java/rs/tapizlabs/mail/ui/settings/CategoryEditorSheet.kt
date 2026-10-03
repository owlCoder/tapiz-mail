package rs.tapizlabs.mail.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.emptyFlow
import rs.tapizlabs.mail.data.local.entity.CategoryEntity
import rs.tapizlabs.mail.data.local.entity.RuleMatchField
import rs.tapizlabs.mail.data.local.entity.RuleMatchType
import rs.tapizlabs.mail.ui.components.MailDropdownField
import rs.tapizlabs.mail.ui.components.MailGhostButton
import rs.tapizlabs.mail.ui.components.MailPrimaryButton
import rs.tapizlabs.mail.ui.components.MailSheet
import rs.tapizlabs.mail.ui.components.MailTextField
import rs.tapizlabs.mail.ui.i18n.Strings
import rs.tapizlabs.mail.ui.theme.AppColors

/**
 * Category create/edit sheet on the shared [MailSheet] primitive: name + a simple rule list
 * (sender/subject/body contains/equals/starts-with) — the manual half of the categorization
 * system (the other half, [rs.tapizlabs.mail.data.local.entity.CategoryMatcher], runs the rules
 * on-device during sync).
 */
@Composable
fun CategoryEditorSheet(
    visible: Boolean,
    category: CategoryEntity?,
    accountId: String?,
    viewModel: SettingsViewModel,
    strings: Strings,
    onDismiss: () -> Unit,
) {
    val colors = AppColors
    var name by remember(category, visible) { mutableStateOf(category?.name.orEmpty()) }
    var ruleField by remember(visible) { mutableStateOf(RuleMatchField.SENDER) }
    var ruleType by remember(visible) { mutableStateOf(RuleMatchType.CONTAINS) }
    var ruleValue by remember(visible) { mutableStateOf("") }
    // Rules added while creating a category — it has no id to attach them to until Save.
    var pendingRules by remember(visible) { mutableStateOf(emptyList<PendingRule>()) }

    val rulesFlow = remember(category?.id) {
        category?.id?.let { viewModel.observeRulesForCategory(it) } ?: emptyFlow()
    }
    val rules by rulesFlow.collectAsState(initial = emptyList())

    MailSheet(visible = visible, onDismiss = onDismiss) {
        Text(
            text = if (category == null) strings.categoryEditorNewTitle else strings.categoryEditorEditTitle,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(16.dp))

        MailTextField(value = name, onValueChange = { name = it }, label = strings.categoryEditorNameLabel)

        Spacer(Modifier.height(16.dp))
        Text(text = strings.categoryEditorMatchRulesLabel, color = colors.textMuted, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))

        rules.forEach { rule ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${ruleFieldLabel(rule.matchField, strings)} ${ruleTypeLabel(rule.matchType, strings).lowercase()} \"${rule.matchValue}\"",
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { viewModel.deleteRule(rule) }) {
                    Icon(Icons.Filled.Delete, contentDescription = strings.categoryEditorDeleteRule, tint = colors.coral)
                }
            }
        }

        pendingRules.forEach { rule ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${ruleFieldLabel(rule.field, strings)} ${ruleTypeLabel(rule.type, strings).lowercase()} \"${rule.value}\"",
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { pendingRules = pendingRules - rule }) {
                    Icon(Icons.Filled.Delete, contentDescription = strings.categoryEditorDeleteRule, tint = colors.coral)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MailDropdownField(
                label = strings.categoryEditorFieldLabel,
                options = RuleMatchField.entries,
                selected = ruleField,
                optionLabel = { ruleFieldLabel(it, strings) },
                onSelect = { ruleField = it },
                modifier = Modifier.weight(1f),
            )
            MailDropdownField(
                label = strings.categoryEditorMatchLabel,
                options = RuleMatchType.entries,
                selected = ruleType,
                optionLabel = { ruleTypeLabel(it, strings) },
                onSelect = { ruleType = it },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MailTextField(
                value = ruleValue,
                onValueChange = { ruleValue = it },
                label = strings.categoryEditorValueLabel,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        MailGhostButton(
            text = strings.categoryEditorAddRule,
            icon = Icons.Outlined.Add,
            onClick = {
                val categoryId = category?.id
                val value = ruleValue.trim()
                if (value.isNotEmpty()) {
                    if (categoryId != null) {
                        viewModel.saveRule(categoryId, ruleField, ruleType, value, existingId = null)
                    } else {
                        pendingRules = pendingRules + PendingRule(ruleField, ruleType, value)
                    }
                    ruleValue = ""
                }
            },
            enabled = ruleValue.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))

        MailPrimaryButton(
            text = strings.categoryEditorSave,
            icon = Icons.Outlined.Save,
            onClick = {
                // A rule still sitting in the value field counts — typing one and going
                // straight to Save shouldn't silently drop it.
                val typedRule = ruleValue.trim().takeIf { it.isNotEmpty() }?.let { PendingRule(ruleField, ruleType, it) }
                val existingId = category?.id
                if (existingId != null && typedRule != null) {
                    viewModel.saveRule(existingId, typedRule.field, typedRule.type, typedRule.value, existingId = null)
                }
                viewModel.saveCategory(
                    name = name,
                    existingId = existingId,
                    // An existing category keeps its own scope (including "all accounts");
                    // only a new one is created under the currently selected account.
                    accountId = if (category != null) category.accountId else accountId,
                    newRules = if (existingId == null) pendingRules + listOfNotNull(typedRule) else emptyList(),
                )
                onDismiss()
            },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun ruleFieldLabel(field: RuleMatchField, strings: Strings): String = when (field) {
    RuleMatchField.SENDER -> strings.ruleFieldSender
    RuleMatchField.SUBJECT -> strings.ruleFieldSubject
    RuleMatchField.BODY -> strings.ruleFieldBody
}

private fun ruleTypeLabel(type: RuleMatchType, strings: Strings): String = when (type) {
    RuleMatchType.CONTAINS -> strings.ruleTypeContains
    RuleMatchType.EQUALS -> strings.ruleTypeEquals
    RuleMatchType.STARTS_WITH -> strings.ruleTypeStartsWith
}
