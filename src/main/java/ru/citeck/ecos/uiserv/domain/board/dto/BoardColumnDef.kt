package ru.citeck.ecos.uiserv.domain.board.dto

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.serialization.annotation.IncludeNonDefault
import java.time.Duration
import java.time.format.DateTimeParseException

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
    val hideItemsOlderThan: String? = null,

    val hasSum: Boolean,
    val sumAtt: String
) {
    companion object {

        val EMPTY = create().build()

        @JvmStatic
        fun create(): Builder {
            return Builder()
        }
    }

    fun copy(): Builder {
        return Builder(this)
    }

    open class Builder() {

        var id: String = ""
        var name: MLText = MLText.EMPTY
        var hideOldItems: Boolean = false
        var hideItemsOlderThan: String? = null

        var hasSum: Boolean = false
        var sumAtt: String = ""

        constructor(base: BoardColumnDef) : this() {
            this.id = base.id
            this.name = base.name
            this.hideOldItems = base.hideOldItems
            this.hideItemsOlderThan = base.hideItemsOlderThan
            this.hasSum = base.hasSum
            this.sumAtt = base.sumAtt
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

        fun withHideItemsOlderThan(hideItemsOlderThan: String?): Builder {
            hideItemsOlderThan?.let {
                try {
                    Duration.parse(hideItemsOlderThan)
                } catch (e: DateTimeParseException) {
                    throw IllegalArgumentException("Invalid Duration format: $hideItemsOlderThan", e)
                }
            }
            this.hideItemsOlderThan = hideItemsOlderThan
            return this
        }

        fun withHasSum(hasSum: Boolean?): Builder {
            this.hasSum = hasSum ?: false
            return this
        }

        fun withSumAtt(sumAtt: String?): Builder {
            this.sumAtt = sumAtt ?: ""
            return this
        }

        fun build(): BoardColumnDef {
            return BoardColumnDef(
                id,
                name,
                hideOldItems,
                hideItemsOlderThan,
                hasSum,
                sumAtt
            )
        }
    }
}
