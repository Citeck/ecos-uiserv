package ru.citeck.ecos.uiserv.domain.journal.dto

import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import ru.citeck.ecos.commons.json.serialization.annotation.IncludeNonDefault

@IncludeNonDefault
@JsonDeserialize(builder = ColumnSearchConfig.Builder::class)
data class ColumnSearchConfig(
    val delimiters: List<String> = emptyList(),
    val searchByText: SearchByText
) {

    companion object {

        val EMPTY = create {}

        fun create(): Builder {
            return Builder()
        }

        fun create(builder: Builder.() -> Unit): ColumnSearchConfig {
            val builderObj = Builder()
            builder.invoke(builderObj)
            return builderObj.build()
        }
    }

    fun withDelimiters(delimiters: List<String>): ColumnSearchConfig {
        return copy().withDelimiters(delimiters).build()
    }

    fun copy(): Builder {
        return Builder(this)
    }

    fun copy(builder: Builder.() -> Unit): ColumnSearchConfig {
        val builderObj = Builder(this)
        builder.invoke(builderObj)
        return builderObj.build()
    }

    class Builder() {

        var delimiters: List<String> = emptyList()
        var searchByText: SearchByText = SearchByText.EMPTY

        constructor(base: ColumnSearchConfig) : this() {
            withDelimiters(base.delimiters)
            searchByText = base.searchByText
        }

        fun withDelimiters(delimiters: List<String>?): Builder {
            this.delimiters = delimiters?.filter { it.isNotEmpty() } ?: emptyList()
            return this
        }

        fun withSearchByText(searchByText: SearchByText?): Builder {
            this.searchByText = searchByText ?: SearchByText.EMPTY
            return this
        }

        fun build(): ColumnSearchConfig {
            return ColumnSearchConfig(delimiters, searchByText)
        }
    }
}
