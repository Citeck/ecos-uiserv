package ru.citeck.ecos.uiserv.domain.form.builder

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.webapp.api.entity.EntityRef

open class EcosFormInputBuilderImpl<T>(
    type: EcosFormInputType,
    config: ObjectData,
    context: FormBuilderContext,
    private val buildImpl: (DataValue) -> T
) : EcosFormInputBuilder<T> {

    companion object {

        private const val TYPE = "type"
        private const val INPUT = "input"
        private const val MULTIPLE = "multiple"
        private const val REQUIRED = "$.validate.required"

        private const val ENABLE_TIME = "enableTime"

        private const val JOURNAL_ID = "journalId"
        private const val TYPE_REF = "typeRef"
        private const val ALLOWED_AUTHORITY_TYPE = "allowedAuthorityType"
    }

    private var data = DataValue.createObj()

    init {
        data[TYPE] = type.typeId
        data[INPUT] = true
        data[MULTIPLE] = false

        when (type) {
            EcosFormInputType.DATE -> data[ENABLE_TIME] = false
            EcosFormInputType.DATETIME -> data[ENABLE_TIME] = true
            EcosFormInputType.JOURNAL -> {
                if (config.has(TYPE_REF)) {
                    val journalRef = context.getJournalRefByType(
                        config[TYPE_REF].getAs(EntityRef::class.java) ?: EntityRef.EMPTY
                    )
                    if (EntityRef.isNotEmpty(journalRef)) {
                        data[JOURNAL_ID] = journalRef.getLocalId()
                    }
                }
                if (!data.has(JOURNAL_ID)) {
                    data[JOURNAL_ID] = "search"
                }
            }
            EcosFormInputType.ORGSTRUCT_AUTHORITY -> data[ALLOWED_AUTHORITY_TYPE] = "USER, GROUP"
            EcosFormInputType.ORGSTRUCT_PERSON -> data[ALLOWED_AUTHORITY_TYPE] = "USER"
            EcosFormInputType.ORGSTRUCT_GROUP -> data[ALLOWED_AUTHORITY_TYPE] = "GROUP"
            EcosFormInputType.JSON -> data["editor"] = "ace"
            else -> {}
        }
    }

    override fun withData(data: DataValue): EcosFormInputBuilder<T> {
        this.data = data.copy()
        return this
    }

    override fun withKey(key: String): EcosFormInputBuilder<T> {
        data[EcosFormComponentProps.KEY] = key
        return this
    }

    override fun withName(name: MLText): EcosFormInputBuilder<T> {
        data[EcosFormComponentProps.NAME] = name
        return this
    }

    override fun withMultiple(multiple: Boolean): EcosFormInputBuilder<T> {
        data[MULTIPLE] = multiple
        return this
    }

    override fun withMandatory(mandatory: Boolean): EcosFormInputBuilder<T> {
        if (mandatory) {
            data[REQUIRED] = true
        } else {
            data.remove(REQUIRED)
        }
        return this
    }

    override fun build(): T {
        return buildImpl(data)
    }
}
