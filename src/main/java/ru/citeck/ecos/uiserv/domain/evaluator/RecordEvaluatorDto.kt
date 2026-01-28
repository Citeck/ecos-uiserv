package ru.citeck.ecos.uiserv.domain.evaluator

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json

@JsonDeserialize(using = RecordEvaluatorDtoDeserializer::class)
class RecordEvaluatorDto {
    var id: String? = null
    var type: String? = null
    var inverse = false
    var config: ObjectData? = null
}

class RecordEvaluatorDtoDeserializer : JsonDeserializer<RecordEvaluatorDto?>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): RecordEvaluatorDto? {
        val node: JsonNode = p.codec.readTree(p)

        if (node.isNull || (node.isTextual && node.asText().isNullOrBlank())) {
            return null
        }

        if (node.isObject) {
            val dto = RecordEvaluatorDto()
            dto.id = node.get("id")?.takeIf { !it.isNull }?.asText()
            dto.type = node.get("type")?.takeIf { !it.isNull }?.asText()
            dto.inverse = node.get("inverse")?.takeIf { !it.isNull }?.asBoolean() ?: false
            node.get("config")?.takeIf { !it.isNull }?.let { configNode ->
                dto.config = Json.mapper.convert(configNode, ObjectData::class.java)
            }
            return dto
        }

        return null
    }
}
