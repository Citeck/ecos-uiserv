package ru.citeck.ecos.uiserv.domain.form.builder

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef

open class EcosFormInputBuilderImpl(
    type: EcosFormInputType,
    config: ObjectData,
    context: FormBuilderContext,
    private val buildImpl: (DataValue) -> EcosFormBuilder
) : EcosFormInputBuilder {

    companion object {
        private const val KEY = "key"
        private const val TYPE = "type"
        private const val NAME = "label"
        private const val INPUT = "input"
        private const val MULTIPLE = "multiple"

        private const val ENABLE_TIME = "enableTime"

        private const val JOURNAL_ID = "journalId"
        private const val TYPE_REF = "typeRef"
    }

    private val data = DataValue.createObj()

    init {
        data[TYPE] = type.typeId
        data[INPUT] = true
        data[MULTIPLE] = false

        if (type == EcosFormInputType.DATE) {
            data[ENABLE_TIME] = false
        } else if (type == EcosFormInputType.DATETIME) {
            data[ENABLE_TIME] = true
        }

        if (type == EcosFormInputType.JOURNAL) {
            if (config.has(TYPE_REF)) {
                val journalRef = context.getJournalRefByType(
                    config[TYPE_REF].getAs(RecordRef::class.java) ?: RecordRef.EMPTY
                )
                if (RecordRef.isNotEmpty(journalRef)) {
                    data[JOURNAL_ID] = journalRef.id
                }
            }
            if (!data.has(JOURNAL_ID)) {
                data[JOURNAL_ID] = "search"
            }
        }
    }

    override fun setKey(key: String): EcosFormInputBuilder {
        data[KEY] = key
        return this
    }

    override fun setName(name: MLText): EcosFormInputBuilder {
        data[NAME] = name
        return this
    }

    override fun setMultiple(multiple: Boolean): EcosFormInputBuilder {
        data[MULTIPLE] = multiple
        return this
    }

    override fun build(): EcosFormBuilder {
        return buildImpl(data)
    }
}
