package ru.citeck.ecos.uiserv.domain.journalsettings.dto

import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.serialization.annotation.IncludeNonDefault

@IncludeNonDefault
@JsonDeserialize(builder = JournalSettingsDto.Builder::class)
open class JournalSettingsDto(
    val id: String,
    val name: MLText,
    val authority: String,
    val journalId: String,
    val settings: ObjectData,
    val creator: String
) {
    constructor(other: JournalSettingsDto) : this(
        other.id,
        other.name,
        other.authority,
        other.journalId,
        other.settings,
        other.creator
    )

    constructor() : this(
        "",
        MLText.EMPTY,
        "",
        "",
        ObjectData.create(),
        ""
    )

    companion object {

        val EMPTY = create {}

        @JvmStatic
        fun create(): Builder {
            return Builder()
        }

        @JvmStatic
        fun create(builder: Builder.() -> Unit): JournalSettingsDto {
            val builderObj = Builder()
            builder.invoke(builderObj)
            return builderObj.build()
        }
    }

    fun copy(): Builder {
        return Builder(this)
    }

    fun copy(builder: Builder.() -> Unit): JournalSettingsDto {
        val builderObj = Builder(this)
        builder.invoke(builderObj)
        return builderObj.build()
    }

    @Override
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JournalSettingsDto) return false

        if (id != other.id) return false
        if (name != other.name) return false
        if (authority != other.authority) return false
        if (journalId != other.journalId) return false
        if (settings != other.settings) return false
        if (creator != other.creator) return false

        return true
    }

    @Override
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + authority.hashCode()
        result = 31 * result + journalId.hashCode()
        result = 31 * result + settings.hashCode()
        result = 31 * result + creator.hashCode()
        return result
    }

    open class Builder() {

        var id: String = ""
        var name: MLText = MLText.EMPTY
        var authority: String = ""
        var journalId: String = ""
        var settings: ObjectData = ObjectData.create()
        var creator: String = ""

        constructor(base: JournalSettingsDto) : this() {
            id = base.id
            name = base.name
            authority = base.authority
            journalId = base.journalId
            settings = base.settings.deepCopy()
            creator = base.creator
        }

        fun withId(id: String): Builder {
            this.id = id
            return this
        }

        fun withName(name: MLText?): Builder {
            this.name = name ?: MLText.EMPTY
            return this
        }

        fun withAuthority(authority: String?): Builder {
            this.authority = authority ?: ""
            return this
        }

        fun withJournalId(journalId: String?): Builder {
            this.journalId = journalId ?: ""
            return this
        }

        fun withSettings(settings: ObjectData?): Builder {
            this.settings = settings ?: ObjectData.create()
            return this
        }

        fun withCreator(creator: String?): Builder {
            this.creator = creator ?: ""
            return this
        }

        fun build(): JournalSettingsDto {
            return JournalSettingsDto(
                id,
                name,
                authority,
                journalId,
                settings,
                creator
            )
        }
    }
}
