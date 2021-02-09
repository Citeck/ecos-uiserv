package ru.citeck.ecos.uiserv.domain.journal.dto

import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import ecos.com.fasterxml.jackson210.annotation.JsonInclude
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.utils.MandatoryParam
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.VoidPredicate

@JsonDeserialize(builder = JournalDef.Builder::class)
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
data class JournalDef(

    /**
     * Journal identifier.
     */
    val id: String,

    /**
     * Journal label. Field for information.
     */
    val label: MLText?,

    /**
     * Predicate for base entities filtering in a table.
     * This predicate can't be changed by user
     * and always should be joined by other filter predicates by AND
     */
    val predicate: Predicate?,

    val queryData: ObjectData?,

    /**
     * Record to load metadata from edge.
     *
     * @see ru.citeck.ecos.records3.record.op.atts.service.value.AttValue.getEdge
     * @see ru.citeck.ecos.records3.record.op.atts.service.value.AttEdge
     */
    val metaRecord: RecordRef?,

    /**
     * ECOS type.
     */
    val typeRef: RecordRef?,

    /**
     * Group records by specified attributes.
     */
    val groupBy: List<String>?,

    /**
     * Default sorting.
     */
    val sortBy: List<JournalSortByDef>?,

    /**
     * Actions for every entity in a table.
     * Can be filtered for specific entities by evaluator.
     */
    val actions: List<RecordRef>?,

    /**
     * Can attributes of entities in a table be edited or not.
     * Global config for all columns.
     * If manual setup for columns is required see JournalColumnDto::editable.
     */
    val editable: Boolean?,

    /**
     * Journal columns to display in table.
     */
    val columns: List<JournalColumnDef>,

    val computed: List<JournalComputedDef>?,

    val system: Boolean,

    /**
     * Custom properties for temporal or very specific
     * parameters which can't be added as field for this DTO
     */
    val properties: ObjectData?
) {
    companion object {

        val EMPTY = create {}

        @JvmStatic
        fun create(): Builder {
            return Builder()
        }

        @JvmStatic
        fun create(builder: Builder.() -> Unit): JournalDef {
            val builderObj = Builder()
            builder.invoke(builderObj)
            return builderObj.build()
        }
    }

    fun copy(): Builder {
        return Builder(this)
    }

    fun copy(builder: Builder.() -> Unit): JournalDef {
        val builderObj = Builder(this)
        builder.invoke(builderObj)
        return builderObj.build()
    }

    open class Builder() {

        var id: String = ""
        var label: MLText? = null
        var predicate: Predicate? = null
        var queryData: ObjectData? = null
        var metaRecord: RecordRef? = null
        var typeRef: RecordRef? = null
        var groupBy: List<String>? = null
        var sortBy: List<JournalSortByDef>? = null
        var actions: List<RecordRef>? = null
        var editable: Boolean? = null
        var columns: List<JournalColumnDef> = emptyList()
        var computed: List<JournalComputedDef>? = null
        var system: Boolean = false
        var properties: ObjectData? = null

        constructor(base: JournalDef) : this() {
            id = base.id
            label = base.label
            predicate = base.predicate?.copy()
            queryData = ObjectData.deepCopy(base.queryData)
            metaRecord = base.metaRecord
            typeRef = base.typeRef
            groupBy = base.groupBy?.let { DataValue.create(it).asStrList() }
            sortBy = base.sortBy?.let { DataValue.create(it).asList(JournalSortByDef::class.java) }
            actions = base.actions?.let { DataValue.create(it).asList(RecordRef::class.java) }
            editable = base.editable
            columns = base.columns.toList()
            computed = base.computed?.let { DataValue.create(it).asList(JournalComputedDef::class.java) }
            system = base.system
            properties = ObjectData.deepCopy(base.properties)
        }

        fun withId(id: String): Builder {
            this.id = id
            return this
        }

        fun withLabel(label: MLText?): Builder {
            this.label = if (MLText.isEmpty(label)) {
                null
            } else {
                label
            }
            return this
        }

        fun withPredicate(predicate: Predicate?): Builder {
            this.predicate = if (predicate == null || predicate == VoidPredicate.INSTANCE) {
                null
            } else {
                predicate
            }
            return this
        }

        fun withQueryData(queryData: ObjectData?): Builder {
            this.queryData = if (queryData?.size() ?: 0 == 0) {
                null
            } else {
                queryData
            }
            return this
        }

        fun withMetaRecord(metaRecord: RecordRef?): Builder {
            this.metaRecord = if (RecordRef.isEmpty(metaRecord)) {
                null
            } else {
                metaRecord
            }
            return this
        }

        fun withTypeRef(typeRef: RecordRef?): Builder {
            this.typeRef = if (RecordRef.isEmpty(typeRef)) {
                null
            } else {
                typeRef
            }
            return this
        }

        fun withGroupBy(groupBy: List<String>?): Builder {
            this.groupBy = if (groupBy?.filter { it.isNotBlank() }.isNullOrEmpty()) {
                null
            } else {
                groupBy
            }
            return this
        }

        fun withSortBy(sortBy: List<JournalSortByDef>?): Builder {
            this.sortBy = if (sortBy?.filter { it.attribute.isNotBlank() }.isNullOrEmpty()) {
                null
            } else {
                sortBy
            }
            return this
        }

        fun withActions(actions: List<RecordRef>?): Builder {
            this.actions = if (actions?.filter { RecordRef.isNotEmpty(it) }.isNullOrEmpty()) {
                null
            } else {
                actions
            }
            return this
        }

        fun withEditable(editable: Boolean?): Builder {
            this.editable = editable
            return this
        }

        fun withColumns(columns: List<JournalColumnDef>): Builder {
            this.columns = columns.filter { it.name.isNotBlank() }
            return this
        }

        fun withComputed(computed: List<JournalComputedDef>?): Builder {
            this.computed = if (computed?.filter { it.id.isNotBlank() }.isNullOrEmpty()) {
                null
            } else {
                computed
            }
            return this
        }

        fun withSystem(system: Boolean): Builder {
            this.system = system
            return this
        }

        fun withProperties(properties: ObjectData?): Builder {
            this.properties = if (properties?.size() ?: 0 == 0) {
                null
            } else {
                properties
            }
            return this
        }

        fun build(): JournalDef {

            return JournalDef(
                id,
                label,
                predicate,
                queryData,
                metaRecord,
                typeRef,
                groupBy,
                sortBy,
                actions,
                editable,
                columns,
                computed,
                system,
                properties
            )
        }
    }
}
