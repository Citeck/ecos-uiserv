package ru.citeck.ecos.uiserv.domain.journal.repo

import ru.citeck.ecos.uiserv.app.common.repo.AbstractAuditingEntity
import javax.persistence.*

@Entity
@Table(name = "journal")
class JournalEntity : AbstractAuditingEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "journal_seq_gen")
    @SequenceGenerator(name = "journal_seq_gen")
    val id: Long? = null

    @Column(unique = true)
    lateinit var extId: String
    lateinit var columns: String

    var label: String? = null
    var groupBy: String? = null
    var sortBy: String? = null
    var metaRecord: String? = null
    var typeRef: String? = null
    var predicate: String? = null
    var queryData: String? = null
    var editable: Boolean? = null
    var attributes: String? = null
    var actions: String? = null
    var computed: String? = null
}
