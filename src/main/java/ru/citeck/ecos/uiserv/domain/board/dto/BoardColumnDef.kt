package ru.citeck.ecos.uiserv.domain.board.dto

import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.serialization.annotation.IncludeNonDefault
import java.time.Instant

@IncludeNonDefault
@JsonDeserialize(builder = BoardColumnDef.Builder::class)
data class BoardColumnDef(
    /**
     * Internal column name corresponds to a *board type* status ID.
     *
     * Mandatory
     */
    val id: String,
    /**
     * Name to display in column header.
     */
    val name: MLText,
    val hideOldItems: Boolean,
    val hideItemsOlderThan: Instant?
) {
    companion object {

        val EMPTY = create {}

        @JvmStatic
        fun create(): Builder {
            return Builder()
        }

        @JvmStatic
        fun create(builder: Builder.() -> Unit): BoardColumnDef {
            val builderObj = Builder()
            builder.invoke(builderObj)
            return builderObj.build()
        }
    }

    fun copy(): Builder {
        return Builder(this)
    }

    open class Builder() {

        var id: String = ""
        var name: MLText = MLText.EMPTY
        var hideOldItems: Boolean = false
        var hideItemsOlderThan: Instant? = null

        constructor(base: BoardColumnDef) : this() {
            this.id = base.id
            this.name = base.name
            this.hideOldItems = base.hideOldItems
            this.hideItemsOlderThan = base.hideItemsOlderThan
        }

        fun withId(id: String): Builder {
            this.id = id
            return this
        }

        fun withName(name: MLText?): Builder {
            this.name = name ?: MLText.EMPTY
            return this
        }

        fun withHideOldItems(hideOldItems: Boolean?): Builder {
            this.hideOldItems = hideOldItems ?: false
            return this
        }

        fun withHideItemsOlderThan(hideItemsOlderThan: Instant?): Builder {
            this.hideItemsOlderThan = hideItemsOlderThan
            return this
        }

        fun build(): BoardColumnDef {
            return BoardColumnDef(
                id,
                name,
                hideOldItems,
                hideItemsOlderThan
            )
        }
    }
}
