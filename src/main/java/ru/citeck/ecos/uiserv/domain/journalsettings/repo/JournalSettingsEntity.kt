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
    var authority: String? = null

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "journal_settings_id")
    @Column(name = "authority")
    @CollectionTable(name = "journal_settings_authority", joinColumns = [JoinColumn(name = "journal_settings_id")])
    var authorities: MutableList<String>? = null
    var settings: String? = null

    // this method required because hibernate doesn't allow to directly override list in @CollectionTable field
    fun setAuthoritiesForEntity(authorities: List<String>) {
        val currentAuthorities = this.authorities
        if (currentAuthorities == null) {
            this.authorities = authorities.toMutableList()
            return
        }
        val notEmptyValues = authorities.filter { it.isNotBlank() }
        if (currentAuthorities.isEmpty()) {
            currentAuthorities.addAll(notEmptyValues)
            return
        }
        var firstNotMatchIdx = 0
        while (firstNotMatchIdx < min(notEmptyValues.size, currentAuthorities.size) &&
            notEmptyValues[firstNotMatchIdx] == currentAuthorities[firstNotMatchIdx]
        ) {
            firstNotMatchIdx++
        }
        while (currentAuthorities.size > firstNotMatchIdx) {
            currentAuthorities.removeLast()
        }
        for (i in firstNotMatchIdx until notEmptyValues.size) {
            currentAuthorities.add(notEmptyValues[i])
        }
    }
}
