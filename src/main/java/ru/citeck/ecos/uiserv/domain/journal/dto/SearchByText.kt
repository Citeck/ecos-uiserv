package ru.citeck.ecos.uiserv.domain.journal.dto

import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.serialization.annotation.IncludeNonDefault

@IncludeNonDefault
@JsonDeserialize(builder = SearchByText.Builder::class)
data class SearchByText(
    //todo use when RecordsQuery will include @IncludeNonDefault annotation (ecos-records:3.51.1)
//    val innerQuery: ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery,
    val innerQuery: ObjectData,
    val innerQueryAttribute: String
) {
    companion object {
        @JvmField
        val EMPTY = SearchByText(
            ObjectData.create("""{"sourceId":"","query":"","page":{"maxItems":20}}"""),
            ""
        )
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
            this.innerQuery = innerQuery ?: ObjectData.create()
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
