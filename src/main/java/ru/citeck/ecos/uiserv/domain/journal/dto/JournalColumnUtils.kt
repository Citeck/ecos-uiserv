package ru.citeck.ecos.uiserv.domain.journal.dto

import ru.citeck.ecos.model.lib.attributes.dto.AttributeType

object JournalColumnUtils {

    private val TYPES_MAPPING = mapOf(
        "text" to AttributeType.TEXT,
        "mltext" to AttributeType.MLTEXT,
        "date" to AttributeType.DATE,
        "datetime" to AttributeType.DATETIME,
        "assoc" to AttributeType.ASSOC,
        "category" to AttributeType.ASSOC,
        "content" to AttributeType.CONTENT,
        "byte" to AttributeType.NUMBER,
        "int" to AttributeType.NUMBER,
        "long" to AttributeType.NUMBER,
        "float" to AttributeType.NUMBER,
        "double" to AttributeType.NUMBER,
        "boolean" to AttributeType.BOOLEAN,
        "qname" to AttributeType.TEXT,
        "noderef" to AttributeType.ASSOC,
        "options" to AttributeType.TEXT,
        "person" to AttributeType.PERSON,
        "authorityGroup" to AttributeType.AUTHORITY_GROUP,
        "authority" to AttributeType.AUTHORITY
    )

    fun getAttType(type: String?): AttributeType? {

        if (type.isNullOrBlank()) {
            return null
        }

        return TYPES_MAPPING[type] ?: AttributeType.valueOf(type)
    }
}
