package ru.citeck.ecos.uiserv.domain.workspace

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.function.Function

/**
 * Rewrites reference fields at any nesting depth without changing scripts, labels or other text.
 */
object ComponentWorkspaceRefs {

    @JvmStatic
    fun rewrite(config: ObjectData, transform: Function<EntityRef, EntityRef>): ObjectData {
        val result = config.deepCopy()
        rewriteNode(result.getData().asJson(), transform)
        return result
    }

    private fun rewriteNode(node: JsonNode, transform: Function<EntityRef, EntityRef>) {
        if (node.isArray) {
            node.forEach { rewriteNode(it, transform) }
        } else if (node is ObjectNode) {
            node.properties().forEach { (key, value) ->
                if (key == "journalId" && value.isTextual && value.asText().isNotBlank()) {
                    val text = value.asText()
                    val fullRef = text.contains('@')
                    val ref = if (fullRef) EntityRef.valueOf(text) else EntityRef.create("uiserv", "journal", text)
                    val updated = transform.apply(ref)
                    node.put(key, if (fullRef) updated.toString() else updated.getLocalId())
                } else {
                    rewriteNode(value, transform)
                }
            }
        }
    }
}
