package ru.citeck.ecos.uiserv.domain.form.builder

import ru.citeck.ecos.model.lib.attributes.dto.AttributeType

enum class EcosFormInputType(val typeId: String) {

    TEXT_FIELD("textfield"),
    ML_TEXT("mlText"),
    NUMBER("number"),
    CHECKBOX("checkbox"),
    JOURNAL("selectJournal"),
    ORGSTRUCT_AUTHORITY("selectOrgstruct"),
    ORGSTRUCT_PERSON("selectOrgstruct"),
    ORGSTRUCT_GROUP("selectOrgstruct"),
    DATETIME("datetime"),
    JSON("textarea"),
    FILE("file"),
    DATE("datetime");

    companion object {

        fun getFromAttributeType(type: AttributeType): EcosFormInputType {
            return when (type) {
                AttributeType.TEXT -> TEXT_FIELD
                AttributeType.MLTEXT -> ML_TEXT
                AttributeType.NUMBER -> NUMBER
                AttributeType.BOOLEAN -> CHECKBOX
                AttributeType.DATE -> DATE
                AttributeType.DATETIME -> DATETIME
                AttributeType.ASSOC -> JOURNAL
                AttributeType.AUTHORITY -> ORGSTRUCT_AUTHORITY
                AttributeType.AUTHORITY_GROUP -> ORGSTRUCT_GROUP
                AttributeType.PERSON -> ORGSTRUCT_PERSON
                AttributeType.JSON -> JSON
                AttributeType.CONTENT -> FILE
                else -> return TEXT_FIELD
            }
        }
    }
}
