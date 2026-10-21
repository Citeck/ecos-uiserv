package ru.citeck.ecos.uiserv.domain.action.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.model.lib.workspace.bindRefToWorkspace
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.ComposedPredicate
import ru.citeck.ecos.records2.predicate.model.NotPredicate
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.ValuePredicate
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDto
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.IdentityHashMap
import java.util.function.Function

/**
 * Reference keys have the same meaning at every config depth, including custom actions/payloads.
 * Global actions bypass export rewriting. Only the owning workspace is made portable otherwise; foreign refs stay intact.
 * The action DTO's unused top-level typeRef is outside the config traversal.
 */
object ActionWorkspaceRefs {

    // Null source: only full references are accepted; never infer a source for a record target.
    private val referenceSources = mapOf(
        "typeRef" to "emodel/type",
        "_type" to "emodel/type",
        "formId" to "uiserv/form",
        "formRef" to "uiserv/form",
        "editorFormId" to "uiserv/form",
        "journalId" to "uiserv/journal",
        "journalRef" to "uiserv/journal",
        "templateRef" to "transformations/template",
        "postActionRef" to "uiserv/action",
        "actionRef" to "uiserv/action",
        "recordRef" to null,
        "recordId" to null,
        "entityRef" to null,
        "taskRef" to null,
        "record" to null,
        "destination" to null,
        "id" to null,
        "recordsToReset" to null
    ).let { fields ->
        fields + fields.filterKeys { it.endsWith("Ref") || it.endsWith("Id") }.mapKeys { it.key + "s" }
    }

    private val typeAttributes = setOf("_type", "_type?id", "_type?localId")
    private val typeValuePredicates = setOf(ValuePredicate.Type.EQ, ValuePredicate.Type.IN, ValuePredicate.Type.CONTAINS)

    @JvmStatic
    fun prepareForExport(action: ActionDto, workspaceService: WorkspaceService): ActionDto {
        if (workspaceService.isWorkspaceWithGlobalEntities(action.workspace)) {
            return copyAction(action)
        }
        return rewrite(action) { ref ->
            val id = ref.getLocalId()
            val portableId = workspaceService.replaceWsPrefixToCurrentWsPlaceholder(id)
            // Rebinding must round-trip to the same id: this also handles generated type$ forms.
            val ownId = workspaceService.replaceCurrentWsPlaceholderToWsPrefix(portableId, action.workspace)
            if (ownId == id) {
                ref.withLocalId(portableId)
            } else {
                ref
            }
        }
    }

    @JvmStatic
    @JvmOverloads
    fun bindToWorkspace(
        action: ActionDto,
        workspace: String,
        workspaceService: WorkspaceService,
        coDeployedRefs: Set<EntityRef> = emptySet()
    ): ActionDto {
        return rewrite(action) { ref ->
            workspaceService.bindRefToWorkspace(ref, workspace, coDeployedRefs)
        }
    }

    private fun copyAction(action: ActionDto): ActionDto {
        val result = ActionDto(action)
        // The domain copy preserves groups; ActionDto's JSON copy can optimize their structure.
        result.predicate = action.predicate?.copy<Predicate>()
        result.evaluator = Json.mapper.copy(action.evaluator)
        return result
    }

    private fun rewrite(action: ActionDto, transform: Function<EntityRef, EntityRef>): ActionDto {
        val result = copyAction(action)
        val confirm = result.confirm
        if (confirm != null && EntityRef.isNotEmpty(confirm.formRef) && isStatic(confirm.formRef.toString())) {
            result.confirm = confirm.copy().withFormRef(transform.apply(confirm.formRef)).build()
        }
        rewritePredicate(result.predicate, transform)
        result.evaluator?.config?.let { rewriteNode(it.getData().asJson(), transform) }
        rewriteNode(result.config.getData().asJson(), transform)
        return result
    }

    private fun rewriteNode(
        node: JsonNode,
        transform: Function<EntityRef, EntityRef>,
        referenceValue: Boolean = false,
        source: String? = null
    ): JsonNode {
        when {
            node.isTextual && referenceValue -> return TextNode(rewriteText(node.asText(), source, transform))
            node is ArrayNode -> for (index in 0 until node.size()) {
                node.set(index, rewriteNode(node[index], transform, referenceValue, source))
            }
            node is ObjectNode -> {
                node.properties().forEach { (key, value) ->
                    var mappedValue = value
                    if (key == "predicate" && value.isObject) {
                        mappedValue = rewriteNestedPredicate(value, transform)
                    }
                    node.set<JsonNode>(
                        key,
                        rewriteNode(
                            mappedValue,
                            transform,
                            referenceSources.containsKey(key),
                            referenceSources[key]
                        )
                    )
                }
            }
        }
        return node
    }

    private fun rewriteNestedPredicate(node: JsonNode, transform: Function<EntityRef, EntityRef>): JsonNode {
        val predicate = Json.mapper.convert(node, Predicate::class.java)
        val values = IdentityHashMap<ValuePredicate, JsonNode>()
        rewritePredicate(predicate, transform, values)
        // Project changed values only: serializing the model would normalize aliases and discard custom data.
        applyPredicateValues(node, predicate, values)
        return node
    }

    // Visits values in place (callers pass a copy), retaining the original structure, including empty groups.
    private fun rewritePredicate(
        predicate: Predicate?,
        transform: Function<EntityRef, EntityRef>,
        values: MutableMap<ValuePredicate, JsonNode>? = null
    ) {
        PredicateUtils.mapValuePredicates(
            predicate,
            { pred ->
                if (pred.getAttribute() in typeAttributes && pred.getType() in typeValuePredicates) {
                    val original = pred.getValue().asJson()
                    val value = rewriteNode(original.deepCopy<JsonNode>(), transform, true, "emodel/type")
                    if (value != original) {
                        pred.setValue(value)
                        values?.put(pred, value)
                    }
                }
                pred
            },
            onlyAnd = false,
            optimize = false,
            filterEmptyComposite = false
        )
    }

    private fun applyPredicateValues(
        node: JsonNode,
        predicate: Predicate?,
        values: Map<ValuePredicate, JsonNode>
    ) {
        if (node !is ObjectNode || (node.has("val") && node.has("v"))) {
            // Two value fields are ambiguous; preserve their original data.
            return
        }
        val valueName: String
        if (node.has("val")) {
            valueName = "val"
        } else {
            valueName = "v"
        }
        when (predicate) {
            is ValuePredicate -> {
                val value = values[predicate]
                if (value != null && node.has(valueName)) {
                    node.set<JsonNode>(valueName, value)
                }
            }
            is ComposedPredicate -> {
                val children = node.get(valueName)
                val predicates = predicate.getPredicates()
                if (children is ArrayNode && children.size() == predicates.size) {
                    for (index in predicates.indices) {
                        applyPredicateValues(children[index], predicates[index], values)
                    }
                }
            }
            is NotPredicate -> {
                var child = node.get(valueName)
                if (node.path("t").asText().startsWith("not-")) {
                    child = node
                }
                if (child != null) {
                    applyPredicateValues(child, predicate.getPredicate(), values)
                }
            }
        }
    }

    private fun rewriteText(text: String, source: String?, transform: Function<EntityRef, EntityRef>): String {
        if (text.isBlank() || !isStatic(text)) {
            return text
        }
        val fullRef = text.contains('@')
        val ref: EntityRef
        if (fullRef) {
            ref = EntityRef.valueOf(text)
        } else {
            if (source == null) {
                return text
            }
            ref = EntityRef.create(source, text)
        }
        val updated = transform.apply(ref)
        if (fullRef) {
            return updated.toString()
        }
        return updated.getLocalId()
    }

    private fun isStatic(text: String): Boolean {
        return !text.contains("\${") && !text.contains("{{")
    }
}
