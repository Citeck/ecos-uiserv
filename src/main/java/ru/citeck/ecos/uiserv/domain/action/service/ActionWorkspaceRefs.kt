package ru.citeck.ecos.uiserv.domain.action.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.ValuePredicate
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDto
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.function.Function

/**
 * Reference keys have the same meaning at every config depth, including custom actions/payloads.
 * Only the owning workspace is made portable on export; explicit foreign references stay intact.
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

    @JvmStatic
    fun prepareForExport(action: ActionDto, workspaceService: WorkspaceService): ActionDto {
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
    fun bindToWorkspace(action: ActionDto, workspace: String, workspaceService: WorkspaceService): ActionDto {
        return rewrite(action) { ref ->
            ref.withLocalId(workspaceService.replaceCurrentWsPlaceholderToWsPrefix(ref.getLocalId(), workspace))
        }
    }

    private fun rewrite(action: ActionDto, transform: Function<EntityRef, EntityRef>): ActionDto {
        val result = ActionDto(action)
        val confirm = result.confirm
        if (confirm != null && EntityRef.isNotEmpty(confirm.formRef) && isStatic(confirm.formRef.toString())) {
            result.confirm = confirm.copy().withFormRef(transform.apply(confirm.formRef)).build()
        }
        result.predicate = rewritePredicate(action.predicate, transform)
        result.evaluator = Json.mapper.copy(action.evaluator)
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
                        val predicate = Json.mapper.convert(value, Predicate::class.java)
                        mappedValue = Json.mapper.toJson(rewritePredicate(predicate, transform))
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

    private fun rewritePredicate(predicate: Predicate?, transform: Function<EntityRef, EntityRef>): Predicate? {
        val result = predicate?.copy<Predicate>()
        // Visit values on the copy, retaining the original structure, including empty groups.
        PredicateUtils.mapValuePredicates(
            result,
            { pred ->
                if (pred.getAttribute() in setOf("_type", "_type?id", "_type?localId") &&
                    pred.getType() in setOf(ValuePredicate.Type.EQ, ValuePredicate.Type.IN, ValuePredicate.Type.CONTAINS)
                ) {
                    val value = rewriteNode(pred.getValue().copy().asJson(), transform, true, "emodel/type")
                    pred.setValue(value)
                }
                pred
            },
            onlyAnd = false,
            optimize = false,
            filterEmptyComposite = false
        )
        return result
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
