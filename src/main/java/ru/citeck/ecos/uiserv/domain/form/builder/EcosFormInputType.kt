package ru.citeck.ecos.uiserv.domain.form.builder

import ru.citeck.ecos.model.lib.attributes.dto.AttributeType

enum class EcosFormInputType(val typeId: String) {

    TEXT_FIELD("textfield"),
    ML_TEXT("mlText"),
    TEXT_AREA("textarea"),
    ML_TEXT_AREA("mlTextarea"),
    NUMBER("number"),
    CHECKBOX("checkbox"),
    JOURNAL("selectJournal"),
    DATETIME("datetime"),
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
                else -> return TEXT_FIELD
            }
        }
    }
}
