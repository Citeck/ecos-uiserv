package ru.citeck.ecos.uiserv.domain.journal.dto

import ecos.com.fasterxml.jackson210.annotation.JsonInclude
import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData

@JsonDeserialize(builder = JournalColumnDef.Builder::class)
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
class JournalColumnDef(

    /**
     * Internal column name. Used in data query when attribute is not set.
     * Allowed characters: [a-zA-Z\-_:]+.
     *
     * Mandatory
     */
    val name: String,

    /**
     * Label to display in column header.
     */
    val label: MLText?,

    /**
     * Data type.
     * ["text", "int", "long", "float", "options", "assoc", etc. ]
     */
    val type: String?,

    /**
     * Attribute to load data.
     * Can be nested, e.g. ecos:counterparty.ecos:inn
     *
     * If not specified field 'name' will be used to load data
     */
    val attribute: String?,

    val editor: ColumnEditorDef?,

    val formatter: ColumnFormatterDef?,

    // features

    /**
     * Is filtering allowed for this column?
     */
    val searchable: Boolean?,

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

    val options: List<ColumnOptionDef>? = null,

    val computed: List<JournalComputedDef>? = null,

    /**
     * Custom properties for temporal or very specific
     * parameters which can't be added as field for this DTO
     */
    val properties: ObjectData? = null
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

        var name: String = ""
        var label: MLText? = null
        var type: String? = null
        var attribute: String? = null
        var editor: ColumnEditorDef? = null
        var formatter: ColumnFormatterDef? = null
        var searchable: Boolean? = null
        var sortable: Boolean? = null
        var groupable: Boolean? = null
        var editable: Boolean? = null
        var visible: Boolean? = null
        var hidden: Boolean? = null
        var multiple: Boolean? = null
        var options: List<ColumnOptionDef>? = null
        var computed: List<JournalComputedDef>? = null
        var properties: ObjectData? = null

        constructor(base: JournalColumnDef) : this() {
            name = base.name
            label = base.label
            type = base.type
            attribute = base.attribute
            editor = base.editor
            formatter = base.formatter
            searchable = base.searchable
            sortable = base.sortable
            groupable = base.groupable
            editable = base.editable
            visible = base.visible
            hidden = base.hidden
            multiple = base.multiple
            options = base.options?.toList()
            computed = base.computed?.toList()
            properties = ObjectData.deepCopy(base.properties)
        }

        fun withName(name: String?): Builder {
            this.name = name ?: ""
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

        fun withType(type: String?): Builder {
            this.type = if (type.isNullOrBlank()) {
                null
            } else {
                type
            }
            return this
        }

        fun withAttribute(attribute: String?): Builder {
            this.attribute = if (attribute.isNullOrBlank()) {
                null
            } else {
                attribute
            }
            return this
        }

        fun withEditor(editor: ColumnEditorDef?): Builder {
            this.editor = if (editor?.type.isNullOrBlank()) {
                null
            } else {
                editor
            }
            return this
        }

        fun withFormatter(formatter: ColumnFormatterDef?): Builder {
            this.formatter = if (formatter?.type.isNullOrBlank()) {
                null
            } else {
                formatter
            }
            return this
        }

        fun withSearchable(searchable: Boolean?): Builder {
            this.searchable = searchable
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

        fun withOptions(options: List<ColumnOptionDef>?): Builder {
            this.options = if (options?.filter { !MLText.isEmpty(it.label) }.isNullOrEmpty()) {
                null
            } else {
                options
            }
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

        fun withProperties(properties: ObjectData?): Builder {
            this.properties = if ((properties?.size() ?: 0) == 0) {
                null
            } else {
                properties
            }
            return this
        }

        fun build(): JournalColumnDef {

            return JournalColumnDef(
                name,
                label,
                type,
                attribute,
                editor,
                formatter,
                searchable,
                sortable,
                groupable,
                editable,
                visible,
                hidden,
                multiple,
                options,
                computed,
                properties
            )
        }
    }
}
