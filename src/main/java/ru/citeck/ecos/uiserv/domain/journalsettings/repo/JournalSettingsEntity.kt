package ru.citeck.ecos.uiserv.domain.journalsettings.repo

import ru.citeck.ecos.uiserv.app.common.repo.AbstractAuditingEntity
import javax.persistence.*

@Entity
@Table(name = "journal_settings")
class JournalSettingsEntity : AbstractAuditingEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "journal_settings_seq_gen")
    @SequenceGenerator(name = "journal_settings_seq_gen")
    val id: Long? = null

    @Column(unique = true)
    lateinit var extId: String

    var name: String? = null
    var journalId: String? = null
    var authority: String? = null
    var settings: String? = null
}
