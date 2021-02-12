package ru.citeck.ecos.uiserv.domain.journal.dto

import ecos.com.fasterxml.jackson210.annotation.JsonInclude
import ecos.com.fasterxml.jackson210.annotation.JsonValue
import ecos.com.fasterxml.jackson210.databind.JsonNode
import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import ecos.com.fasterxml.jackson210.databind.node.TextNode
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json

@JsonDeserialize(builder = ColumnFormatterDef.Builder::class)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class ColumnFormatterDef(
    val type: String,
    val config: ObjectData
) {
    companion object {
        @JvmField
        val EMPTY = ColumnFormatterDef("", ObjectData.create())
    }

    @JsonValue
    fun toJson(): JsonNode {
        val data = Json.mapper.newObjectNode()
        if (type.isBlank()) {
            return data
        }
        data.set<JsonNode>("type", TextNode.valueOf(type))
        data.set<JsonNode>("config", config.getData().asJson())
        return data
    }

    class Builder() {

        var type: String = ""
        var config: ObjectData = ObjectData.create()

        constructor(base: ColumnFormatterDef) : this() {
            this.type = base.type
            this.config = ObjectData.deepCopyOrNew(base.config);
        }

        fun withType(type: String?): Builder {
            this.type = type ?: ""
            return this;
        }

        fun withConfig(config: ObjectData?): Builder {
            this.config = config ?: ObjectData.create()
            return this
        }

        fun build(): ColumnFormatterDef {
            return ColumnFormatterDef(type, config)
        }
    }
}
