package ru.citeck.ecos.uiserv.domain.journal.dto

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.serialization.annotation.IncludeNonDefault

@IncludeNonDefault
@JsonDeserialize(builder = ColumnEditorDef.Builder::class)
data class ColumnEditorDef(
    val type: String,
    val config: ObjectData
) {
    companion object {
        @JvmField
        val EMPTY = ColumnEditorDef("", ObjectData.create())
    }

    class Builder() {

        var type: String = ""
        var config: ObjectData = ObjectData.create()

        constructor(base: ColumnEditorDef) : this() {
            this.type = base.type
            this.config = ObjectData.deepCopyOrNew(base.config)
        }

        fun withType(type: String?): Builder {
            this.type = type ?: ""
            return this
        }

        fun withConfig(config: ObjectData?): Builder {
            this.config = config ?: ObjectData.create()
            return this
        }

        fun build(): ColumnEditorDef {
            return ColumnEditorDef(type, config)
        }
    }
}
