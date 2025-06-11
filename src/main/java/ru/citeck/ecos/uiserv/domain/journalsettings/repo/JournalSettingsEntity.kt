package ru.citeck.ecos.uiserv.domain.journalsettings.repo

import jakarta.persistence.*
import ru.citeck.ecos.uiserv.app.common.repo.AbstractAuditingEntity
import kotlin.math.min

@Entity
@Table(name = "journal_settings")
class JournalSettingsEntity : AbstractAuditingEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence")
    val id: Long? = null

    @Column(unique = true)
    lateinit var extId: String

    var name: String? = null
    var journalId: String? = null

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "journal_settings_id")
    @Column(name = "authority")
    @CollectionTable(name = "journal_settings_authority", joinColumns = [JoinColumn(name = "journal_settings_id")])
    var authorities: MutableList<String>? = null

    @Column(name = "workspace")
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "journal_settings_workspace", joinColumns = [JoinColumn(name = "journal_settings_id")])
    var workspaces: MutableList<String> = mutableListOf()

    var settings: String? = null

    fun setAuthoritiesForEntity(authorities: List<String>) {
        this.authorities = setListValues(this.authorities, authorities)
    }

    fun setWorkspacesForEntity(workspaces: List<String>) {
        this.workspaces = setListValues(this.workspaces, workspaces)
    }

    // this method required because hibernate doesn't allow to directly override list in @CollectionTable field
    private fun setListValues(currentList: MutableList<String>?, newList: List<String>): MutableList<String> {
        if (currentList == null) {
            return newList.toMutableList()
        }
        val notEmptyValues = newList.filter { it.isNotBlank() }
        if (currentList.isEmpty()) {
            currentList.addAll(notEmptyValues)
            return currentList
        }
        var firstNotMatchIdx = 0
        while (firstNotMatchIdx < min(notEmptyValues.size, currentList.size) &&
            notEmptyValues[firstNotMatchIdx] == currentList[firstNotMatchIdx]
        ) {
            firstNotMatchIdx++
        }
        while (currentList.size > firstNotMatchIdx) {
            currentList.removeLast()
        }
        for (i in firstNotMatchIdx until notEmptyValues.size) {
            currentList.add(notEmptyValues[i])
        }
        return currentList
    }
}
