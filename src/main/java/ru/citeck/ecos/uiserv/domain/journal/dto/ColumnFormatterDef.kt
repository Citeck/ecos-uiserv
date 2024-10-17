package ru.citeck.ecos.uiserv.domain.journal.dto

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.serialization.annotation.IncludeNonDefault

@IncludeNonDefault
@JsonDeserialize(builder = ColumnFormatterDef.Builder::class)
data class ColumnFormatterDef(
    val type: String,
    val config: ObjectData
) {
    companion object {
        @JvmField
        val EMPTY = ColumnFormatterDef("", ObjectData.create())

        fun create(): Builder {
            return Builder()
        }
    }

    class Builder() {

        var type: String = ""
        var config: ObjectData = ObjectData.create()

        constructor(base: ColumnFormatterDef) : this() {
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

        fun build(): ColumnFormatterDef {
            return ColumnFormatterDef(type, config)
        }
    }
}
