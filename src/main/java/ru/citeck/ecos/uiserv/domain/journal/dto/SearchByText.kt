package ru.citeck.ecos.uiserv.domain.journal.dto

import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.serialization.annotation.IncludeNonDefault

@IncludeNonDefault
@JsonDeserialize(builder = SearchByText.Builder::class)
data class SearchByText(
    /**
     * Query for preprocessing by column on the client. Analogue of ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery structure.
     * Default: {"sourceId":"","query":"","page":{"maxItems":20}}
     * Need to use RecordsQuery type, when ecos-records:3.51.1 will be used.
     * In ecos-records:3.51.1 RecordsQuery include annotation @IncludeNonDefault, for compact serialization to json.
     */
    val innerQuery: ObjectData,
    val innerQueryAttribute: String
) {
    companion object {
        val EMPTY = create {}

        @JvmStatic
        fun create(): Builder {
            return Builder()
        }

        @JvmStatic
        fun create(builder: Builder.() -> Unit): SearchByText {
            val builderObj = Builder()
            builder.invoke(builderObj)
            return builderObj.build()
        }
    }

    fun copy(): Builder {
        return Builder(this)
    }

    fun copy(builder: Builder.() -> Unit): SearchByText {
        val builderObj = Builder(this)
        builder.invoke(builderObj)
        return builderObj.build()
    }

    class Builder() {

        var innerQuery: ObjectData = ObjectData.create()
        var innerQueryAttribute: String = ""

        constructor(base: SearchByText) : this() {
            this.innerQuery = base.innerQuery.deepCopy()
            this.innerQueryAttribute = base.innerQueryAttribute
        }

        fun withInnerQuery(innerQuery: ObjectData?): Builder {
            if (innerQuery != null && innerQuery["sourceId"].isEmpty()) {
                this.innerQuery = ObjectData.create()
            } else {
                this.innerQuery = innerQuery ?: ObjectData.create()
            }
            return this
        }

        fun withInnerQueryAttribute(innerQueryAttribute: String?): Builder {
            this.innerQueryAttribute = innerQueryAttribute ?: ""
            return this
        }

        fun build(): SearchByText {
            return SearchByText(innerQuery, innerQueryAttribute)
        }
    }
}
