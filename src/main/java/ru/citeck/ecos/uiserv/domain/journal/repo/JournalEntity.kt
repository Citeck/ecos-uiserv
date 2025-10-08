package ru.citeck.ecos.uiserv.domain.journal.repo

import jakarta.persistence.*
import ru.citeck.ecos.uiserv.app.common.repo.AbstractAuditingEntity

@Entity
@Table(name = "journal")
class JournalEntity : AbstractAuditingEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence")
    val id: Long? = null

    @Column(unique = true)
    lateinit var extId: String
    lateinit var columns: String

    var sourceId: String? = null
    var metaRecord: String? = null

    @Column(name = "label")
    var name: String? = null
    var groupBy: String? = null
    var sortBy: String? = null
    var typeRef: String? = null
    var workspace: String = ""
    var predicate: String? = null
    var defaultFilters: String? = null
    var queryData: String? = null
    var searchConfig: String? = null
    var editable: Boolean? = null
    var attributes: String? = null
    var hideImportDataActions: Boolean? = null
    var actionsFromType: Boolean? = null
    var actions: String? = null
    var actionsDef: String? = null
    var computed: String? = null

    var system: Boolean? = null
}
