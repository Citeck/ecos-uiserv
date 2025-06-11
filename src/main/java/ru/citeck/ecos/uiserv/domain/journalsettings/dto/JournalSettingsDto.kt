package ru.citeck.ecos.uiserv.domain.journalsettings.dto

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.serialization.annotation.IncludeNonDefault
import java.util.*

@IncludeNonDefault
@JsonDeserialize(builder = JournalSettingsDto.Builder::class)
open class JournalSettingsDto(
    val id: String,
    val name: MLText,
    val authorities: List<String>,
    val journalId: String,
    val settings: ObjectData,
    val creator: String,
    val workspaces: List<String>
) {
    constructor(other: JournalSettingsDto) : this(
        other.id,
        other.name,
        other.authorities,
        other.journalId,
        other.settings,
        other.creator,
        other.workspaces
    )

    constructor() : this(
        "",
        MLText.EMPTY,
        Collections.emptyList(),
        "",
        ObjectData.create(),
        "",
        emptyList()
    )

    companion object {

        val EMPTY = create().build()

        @JvmStatic
        fun create(): Builder {
            return Builder()
        }
    }

    fun getAuthority(): String {
        return authorities.firstOrNull() ?: ""
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
        if (authorities != other.authorities) return false
        if (journalId != other.journalId) return false
        if (settings != other.settings) return false
        if (creator != other.creator) return false
        if (workspaces != other.workspaces) return false

        return true
    }

    @Override
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + authorities.hashCode()
        result = 31 * result + journalId.hashCode()
        result = 31 * result + settings.hashCode()
        result = 31 * result + creator.hashCode()
        result = 31 * result + workspaces.hashCode()
        return result
    }

    @JsonPOJOBuilder
    open class Builder() {

        var id: String = ""
        var name: MLText = MLText.EMPTY
        var authorities: List<String> = Collections.emptyList()
        var journalId: String = ""
        var settings: ObjectData = ObjectData.create()
        var creator: String = ""
        var workspaces: List<String> = listOf("default")

        constructor(base: JournalSettingsDto) : this() {
            id = base.id
            name = base.name
            authorities = base.authorities
            journalId = base.journalId
            settings = base.settings.deepCopy()
            creator = base.creator
            workspaces = base.workspaces
        }

        fun withId(id: String): Builder {
            this.id = id
            return this
        }

        fun withName(name: MLText?): Builder {
            this.name = name ?: MLText.EMPTY
            return this
        }

        fun getAuthority(): String {
            return authorities.firstOrNull() ?: ""
        }

        fun withAuthority(authority: String?): Builder {
            return if (authority.isNullOrBlank()) {
                withAuthorities(null)
            } else {
                withAuthorities(listOf(authority))
            }
        }

        fun withAuthorities(authorities: List<String>?): Builder {
            this.authorities = authorities ?: emptyList()
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

        fun withWorkspaces(workspaces: List<String>?): Builder {
            this.workspaces = workspaces?.filter { it.isNotBlank() } ?: listOf("default")
            return this
        }

        fun build(): JournalSettingsDto {
            return JournalSettingsDto(
                id,
                name,
                authorities,
                journalId,
                settings,
                creator,
                workspaces
            )
        }
    }
}
