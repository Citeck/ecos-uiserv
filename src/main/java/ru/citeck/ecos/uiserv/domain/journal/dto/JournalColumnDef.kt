package ru.citeck.ecos.uiserv.domain.journal.dto

import ecos.com.fasterxml.jackson210.annotation.JsonSetter
import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.serialization.annotation.IncludeNonDefault
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import java.util.*

@IncludeNonDefault
@JsonDeserialize(builder = JournalColumnDef.Builder::class)
class JournalColumnDef(

    /**
     * Internal column name. Used in data query when attribute is not set.
     * Allowed characters: ^[a-zA-Z][$\da-zA-Z:_-]*$
     *
     * Mandatory
     */
    val id: String,

    /**
     * Label to display in column header.
     */
    val name: MLText,

    /**
     * Data type.
     */
    val type: AttributeType?,

    /**
     * Attribute to load data.
     * Can be nested, e.g. ecos:counterparty.ecos:inn
     * Allowed characters: ^[a-zA-Z][$.\da-zA-Z:_-]*$
     *
     * If not specified field 'name' will be used to load data
     */
    val attribute: String,

    val attSchema: String,

    val editor: ColumnEditorDef,

    val formatter: ColumnFormatterDef,

    // features

    /**
     * Is filtering allowed for this column?
     */
    val searchable: Boolean?,

    val searchableByText: Boolean?,

    /**
     * Is sorting allowed for this column?
     */
    val sortable: Boolean?,

    /**
     * Can data be grouped by this column?
     */
    val groupable: Boolean?,

    /**
     * Is column editable?
     */
    val editable: Boolean?,

    /**
     * Is column visible or not.
     * This parameter can be changed by user in UI
     */
    val visible: Boolean?,

    /**
     * Can this column be shown in a table.
     * Useful with searchable = true, but without ability to add column in table
     */
    val hidden: Boolean?,

    /**
     * Is this attribute contains multiple values
     */
    val multiple: Boolean?,

    // /features

    val computed: List<JournalComputedDef>,

    /**
     * Custom properties for temporal or very specific
     * parameters which can't be added as field for this DTO
     */
    val properties: ObjectData = ObjectData.create()
) {
    companion object {

        fun create(): Builder {
            return Builder()
        }

        fun create(builder: Builder.() -> Unit): JournalColumnDef {
            val builderObj = Builder()
            builder.invoke(builderObj)
            return builderObj.build()
        }
    }

    fun copy(): Builder {
        return Builder(this)
    }

    fun copy(builder: Builder.() -> Unit): JournalColumnDef {
        val builderObj = Builder(this)
        builder.invoke(builderObj)
        return builderObj.build()
    }

    class Builder() {

        var id: String = ""
        var name: MLText = MLText.EMPTY
        var type: AttributeType? = null
        var attribute: String = ""
        var attSchema: String = ""
        var editor: ColumnEditorDef = ColumnEditorDef.EMPTY
        var formatter: ColumnFormatterDef = ColumnFormatterDef.EMPTY
        var searchable: Boolean? = null
        var searchableByText: Boolean? = null
        var sortable: Boolean? = null
        var groupable: Boolean? = null
        var editable: Boolean? = null
        var visible: Boolean? = null
        var hidden: Boolean? = null
        var multiple: Boolean? = null
        var computed: List<JournalComputedDef> = emptyList()
        var properties: ObjectData = ObjectData.create()

        constructor(base: JournalColumnDef) : this() {
            id = base.id
            name = base.name
            type = base.type
            attribute = base.attribute
            attSchema = base.attSchema
            editor = base.editor
            formatter = base.formatter
            searchable = base.searchable
            searchableByText = base.searchableByText
            sortable = base.sortable
            groupable = base.groupable
            editable = base.editable
            visible = base.visible
            hidden = base.hidden
            multiple = base.multiple
            computed = base.computed.toList()
            properties = ObjectData.deepCopyOrNew(base.properties)
        }

        fun withId(id: String?): Builder {
            this.id = id ?: ""
            return this
        }

        fun withName(name: MLText?): Builder {
            this.name = name ?: MLText.EMPTY
            if (this.id.isEmpty()) {
                // legacy config support
                this.id = this.name.get(Locale.ENGLISH)
            }
            return this
        }

        @Deprecated(message = "deprecated field", replaceWith = ReplaceWith("withName"))
        fun withLabel(label: MLText?): Builder {
            this.name = label ?: MLText.EMPTY
            return this
        }

        @JsonSetter
        fun withType(type: String?): Builder {
            this.type = JournalColumnUtils.getAttType(type)
            return this
        }

        fun withType(type: AttributeType?): Builder {
            this.type = type ?: AttributeType.TEXT
            return this
        }

        fun withAttribute(attribute: String?): Builder {
            this.attribute = attribute ?: ""
            return this
        }

        fun withAttSchema(attSchema: String?): Builder {
            this.attSchema = attSchema
            return this
        }

        fun withEditor(editor: ColumnEditorDef?): Builder {
            this.editor = editor ?: ColumnEditorDef.EMPTY
            return this
        }

        fun withFormatter(formatter: ColumnFormatterDef?): Builder {
            this.formatter = formatter ?: ColumnFormatterDef.EMPTY
            return this
        }

        fun withSearchable(searchable: Boolean?): Builder {
            this.searchable = searchable
            return this
        }

        fun withSearchableByText(searchableByText: Boolean?): Builder {
            this.searchableByText = searchableByText
            return this
        }

        fun withSortable(sortable: Boolean?): Builder {
            this.sortable = sortable
            return this
        }

        fun withGroupable(groupable: Boolean?): Builder {
            this.groupable = groupable
            return this
        }

        fun withEditable(editable: Boolean?): Builder {
            this.editable = editable
            return this
        }

        fun withVisible(visible: Boolean?): Builder {
            this.visible = visible
            return this
        }

        fun withHidden(hidden: Boolean?): Builder {
            this.hidden = hidden
            return this
        }

        fun withMultiple(multiple: Boolean?): Builder {
            this.multiple = multiple
            return this
        }

        fun withComputed(computed: List<JournalComputedDef>?): Builder {
            this.computed = computed?.filter { it.id.isNotBlank() } ?: emptyList()
            return this
        }

        fun withProperties(properties: ObjectData?): Builder {
            this.properties = properties ?: ObjectData.create()
            return this
        }

        fun build(): JournalColumnDef {

            return JournalColumnDef(
                id,
                name,
                type,
                attribute,
                attSchema,
                editor,
                formatter,
                searchable,
                searchableByText,
                sortable,
                groupable,
                editable,
                visible,
                hidden,
                multiple,
                computed,
                properties
            )
        }
    }
}
