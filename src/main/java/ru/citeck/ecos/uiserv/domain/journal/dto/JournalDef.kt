package ru.citeck.ecos.uiserv.domain.journal.dto

import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import ecos.com.fasterxml.jackson210.annotation.JsonInclude
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.VoidPredicate

@JsonDeserialize(builder = JournalDef.Builder::class)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class JournalDef(

    /**
     * Journal identifier.
     */
    val id: String,

    /**
     * Journal label. Field for information.
     */
    val label: MLText,

    /**
     * Predicate for base entities filtering in a table.
     * This predicate can't be changed by user
     * and always should be joined by other filter predicates by AND
     */
    val predicate: Predicate,

    val queryData: ObjectData,

    /**
     * Record to load metadata from edge.
     *
     * @see ru.citeck.ecos.records3.record.op.atts.service.value.AttValue.getEdge
     * @see ru.citeck.ecos.records3.record.op.atts.service.value.AttEdge
     */
    val metaRecord: RecordRef,

    /**
     * ECOS type.
     */
    val typeRef: RecordRef,

    /**
     * Group records by specified attributes.
     */
    val groupBy: List<String>,

    /**
     * Default sorting.
     */
    val sortBy: List<JournalSortByDef>,

    /**
     * Actions for every entity in a table.
     * Can be filtered for specific entities by evaluator.
     */
    val actions: List<RecordRef>,

    /**
     * Can attributes of entities in a table be edited or not.
     * Global config for all columns.
     * If manual setup for columns is required see JournalColumnDto::editable.
     */
    val editable: Boolean,

    /**
     * Journal columns to display in table.
     */
    val columns: List<JournalColumnDef>,

    val computed: List<JournalComputedDef>,

    val system: Boolean,

    /**
     * Custom properties for temporal or very specific
     * parameters which can't be added as field for this DTO
     */
    val properties: ObjectData
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
        var label: MLText = MLText.EMPTY
        var predicate: Predicate = VoidPredicate.INSTANCE
        var queryData: ObjectData = ObjectData.create()
        var metaRecord: RecordRef = RecordRef.EMPTY
        var typeRef: RecordRef = RecordRef.EMPTY
        var groupBy: List<String> = emptyList()
        var sortBy: List<JournalSortByDef> = emptyList()
        var actions: List<RecordRef> = emptyList()
        var editable: Boolean = true
        var columns: List<JournalColumnDef> = emptyList()
        var computed: List<JournalComputedDef> = emptyList()
        var system: Boolean = false
        var properties: ObjectData = ObjectData.create()

        constructor(base: JournalDef) : this() {
            id = base.id
            label = base.label
            predicate = base.predicate.copy()
            queryData = ObjectData.deepCopyOrNew(base.queryData)
            metaRecord = base.metaRecord
            typeRef = base.typeRef
            groupBy = base.groupBy.let { DataValue.create(it).asStrList() }
            sortBy = base.sortBy.let { DataValue.create(it).asList(JournalSortByDef::class.java) }
            actions = base.actions.let { DataValue.create(it).asList(RecordRef::class.java) }
            editable = base.editable
            columns = base.columns.toList()
            computed = base.computed.let { DataValue.create(it).asList(JournalComputedDef::class.java) }
            system = base.system
            properties = ObjectData.deepCopyOrNew(base.properties)
        }

        fun withId(id: String): Builder {
            this.id = id
            return this
        }

        fun withLabel(label: MLText?): Builder {
            this.label = label ?: MLText.EMPTY
            return this
        }

        fun withPredicate(predicate: Predicate?): Builder {
            this.predicate = predicate ?: VoidPredicate.INSTANCE
            return this
        }

        fun withQueryData(queryData: ObjectData?): Builder {
            this.queryData = queryData ?: ObjectData.create()
            return this
        }

        fun withMetaRecord(metaRecord: RecordRef?): Builder {
            this.metaRecord = metaRecord ?: RecordRef.EMPTY
            return this
        }

        fun withTypeRef(typeRef: RecordRef?): Builder {
            this.typeRef = typeRef ?: RecordRef.EMPTY
            return this
        }

        fun withGroupBy(groupBy: List<String>?): Builder {
            this.groupBy = groupBy?.filter { it.isNotBlank() } ?: emptyList()
            return this
        }

        fun withSortBy(sortBy: List<JournalSortByDef>?): Builder {
            this.sortBy = sortBy?.filter { it.attribute.isNotBlank() } ?: emptyList()
            return this
        }

        fun withActions(actions: List<RecordRef>?): Builder {
            this.actions = actions?.filter { RecordRef.isNotEmpty(it) } ?: emptyList()
            return this
        }

        fun withEditable(editable: Boolean?): Builder {
            this.editable = editable ?: true
            return this
        }

        fun withColumns(columns: List<JournalColumnDef>): Builder {
            this.columns = columns.filter { it.name.isNotBlank() }
            return this
        }

        fun withComputed(computed: List<JournalComputedDef>?): Builder {
            this.computed = computed?.filter { it.id.isNotBlank() } ?: emptyList()
            return this
        }

        fun withSystem(system: Boolean?): Builder {
            this.system = system ?: false
            return this
        }

        fun withProperties(properties: ObjectData?): Builder {
            this.properties = properties ?: ObjectData.create()
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
