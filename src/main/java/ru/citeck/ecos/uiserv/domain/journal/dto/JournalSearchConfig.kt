package ru.citeck.ecos.uiserv.domain.journal.dto

import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import ru.citeck.ecos.commons.json.serialization.annotation.IncludeNonDefault

@IncludeNonDefault
@JsonDeserialize(builder = JournalSearchConfig.Builder::class)
data class JournalSearchConfig(
    val delimiters: List<String> = emptyList(),
    val headerSearchEnabled: Boolean = true
) {

    companion object {

        val EMPTY = create {}

        fun create(): Builder {
            return Builder()
        }

        fun create(builder: Builder.() -> Unit): JournalSearchConfig {
            val builderObj = Builder()
            builder.invoke(builderObj)
            return builderObj.build()
        }
    }

    fun copy(): Builder {
        return Builder(this)
    }

    fun copy(builder: Builder.() -> Unit): JournalSearchConfig {
        val builderObj = Builder(this)
        builder.invoke(builderObj)
        return builderObj.build()
    }

    class Builder() {

        var delimiters: List<String> = emptyList()
        var headerSearchEnabled: Boolean = true

        constructor(base: JournalSearchConfig) : this() {
            withDelimiters(base.delimiters)
            headerSearchEnabled = base.headerSearchEnabled
        }

        fun withDelimiters(delimiters: List<String>?): Builder {
            this.delimiters = delimiters?.filter { it.isNotEmpty() } ?: emptyList()
            return this
        }

        fun withHeaderSearchEnabled(headerSearchEnabled: Boolean?): Builder {
            this.headerSearchEnabled = headerSearchEnabled ?: true
            return this
        }

        fun build(): JournalSearchConfig {
            return JournalSearchConfig(delimiters, headerSearchEnabled)
        }
    }
}
