package ru.citeck.ecos.uiserv.domain.journal.dto

import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.serialization.annotation.IncludeNonDefault
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.VoidPredicate

@IncludeNonDefault
@JsonDeserialize(builder = JournalDef.Builder::class)
data class JournalDef(

    /**
     * Journal identifier.
     */
    val id: String,

    /**
     * Journal label. Field for information.
     */
    val name: MLText,

    val sourceId: String,

    val metaRecord: RecordRef,

    /**
     * Predicate for base entities filtering in a table.
     * This predicate can't be changed by user
     * and always should be joined by other filter predicates by AND
     */
    val predicate: Predicate,

    val defaultFilters: List<Predicate>,

    val queryData: ObjectData,

    val searchConfig: JournalSearchConfig,

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
    val defaultSortBy: List<JournalSortByDef>,

    /**
     * Include actions from typeDef
     */
    val actionsFromType: Boolean?,

    /**
     * Actions for every entity in a table.
     * Can be filtered for specific entities by evaluator.
     */
    val actions: List<RecordRef>,

    /**
     * Action definitions for every entity in a table.
     * Can be filtered for specific entities by evaluator.
     */
    val actionsDef: List<JournalActionDef>,

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
        var name: MLText = MLText.EMPTY
        var sourceId: String = ""
        var metaRecord: RecordRef = RecordRef.EMPTY
        var predicate: Predicate = VoidPredicate.INSTANCE
        var defaultFilters: List<Predicate> = emptyList()
        var queryData: ObjectData = ObjectData.create()
        var searchConfig: JournalSearchConfig = JournalSearchConfig.EMPTY
        var typeRef: RecordRef = RecordRef.EMPTY
        var groupBy: List<String> = emptyList()
        var defaultSortBy: List<JournalSortByDef> = emptyList()
        var actionsFromType: Boolean? = null
        var actions: List<RecordRef> = emptyList()
        var actionsDef: List<JournalActionDef> = emptyList()
        var editable: Boolean = true
        var columns: List<JournalColumnDef> = emptyList()
        var computed: List<JournalComputedDef> = emptyList()
        var system: Boolean = false
        var properties: ObjectData = ObjectData.create()

        constructor(base: JournalDef) : this() {
            id = base.id
            name = base.name
            sourceId = base.sourceId
            metaRecord = base.metaRecord
            predicate = base.predicate.copy()
            withDefaultFilters(base.defaultFilters)
            queryData = ObjectData.deepCopyOrNew(base.queryData)
            searchConfig = base.searchConfig
            typeRef = base.typeRef
            groupBy = base.groupBy.let { DataValue.create(it).asStrList() }
            withDefaultSortBy(base.defaultSortBy)
            actionsFromType = base.actionsFromType
            actions = base.actions.let { DataValue.create(it).asList(RecordRef::class.java) }
            actionsDef = base.actionsDef.let { DataValue.create(it).asList(JournalActionDef::class.java) }
            editable = base.editable
            withColumns(base.columns)
            computed = base.computed.let { DataValue.create(it).asList(JournalComputedDef::class.java) }
            system = base.system
            properties = ObjectData.deepCopyOrNew(base.properties)
        }

        fun withId(id: String): Builder {
            this.id = id
            return this
        }

        fun withName(name: MLText?): Builder {
            this.name = name ?: MLText.EMPTY
            return this
        }

        fun withSourceId(sourceId: String?): Builder {
            this.sourceId = sourceId ?: ""
            return this
        }

        fun withMetaRecord(metaRecord: RecordRef?): Builder {
            this.metaRecord = RecordRef.valueOf(metaRecord)
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

        fun withSearchConfig(searchConfig: JournalSearchConfig?): Builder {
            this.searchConfig = searchConfig ?: JournalSearchConfig.EMPTY
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

        @Deprecated("withDefaultSortBy", ReplaceWith("withDefaultSortBy(sortBy)"))
        fun setSortBy(sortBy: List<JournalSortByDef>?): Builder {
            return withDefaultSortBy(sortBy)
        }

        @Deprecated("withDefaultSortBy", ReplaceWith("withDefaultSortBy(sortBy)"))
        fun withSortBy(sortBy: List<JournalSortByDef>?): Builder {
            return withDefaultSortBy(sortBy)
        }

        fun withDefaultSortBy(defaultSortBy: List<JournalSortByDef>?): Builder {
            this.defaultSortBy = defaultSortBy?.filter { it.attribute.isNotBlank() } ?: emptyList()
            return this
        }

        fun withDefaultFilters(defaultFilters: List<Predicate>?): Builder {
            this.defaultFilters = defaultFilters?.filter { it !is VoidPredicate } ?: emptyList()
            return this
        }

        fun withActionsFromType(actionsFromType: Boolean?): Builder {
            this.actionsFromType = actionsFromType
            return this
        }

        fun withActions(actions: List<RecordRef>?): Builder {
            this.actions = actions?.filter { RecordRef.isNotEmpty(it) } ?: emptyList()
            return this
        }

        fun withActionsDef(actionsDef: List<JournalActionDef>?): Builder {
            this.actionsDef = actionsDef?.filter { it.type.isNotBlank() } ?: emptyList()
            return this
        }

        fun withEditable(editable: Boolean?): Builder {
            this.editable = editable ?: true
            return this
        }

        fun withColumns(columns: List<JournalColumnDef>): Builder {
            this.columns = columns.filter { it.id.isNotBlank() }
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
                id = id,
                name = name,
                sourceId = sourceId,
                metaRecord = metaRecord,
                predicate = predicate,
                defaultFilters = defaultFilters,
                queryData = queryData,
                searchConfig = searchConfig,
                typeRef = typeRef,
                groupBy = groupBy,
                defaultSortBy = defaultSortBy,
                actionsFromType = actionsFromType,
                actions = actions,
                actionsDef = actionsDef,
                editable = editable,
                columns = columns,
                computed = computed,
                system = system,
                properties = properties
            )
        }
    }
}
