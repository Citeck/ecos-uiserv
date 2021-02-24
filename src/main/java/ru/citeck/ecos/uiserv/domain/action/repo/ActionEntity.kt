package ru.citeck.ecos.uiserv.domain.action.repo

import lombok.Data
import ru.citeck.ecos.uiserv.app.common.repo.AbstractAuditingEntity
import ru.citeck.ecos.uiserv.domain.evaluator.repo.EvaluatorEntity
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "actions")
class ActionEntity : AbstractAuditingEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "actions_seq_gen")
    @SequenceGenerator(name = "actions_seq_gen")
    val id: Long? = null

    @Column(unique = true)
    lateinit var extId: String

    var name: String? = null
    var type: String? = null
    var icon: String? = null

    @Lob
    @Column(name = "config_json")
    var configJson: String? = null

    @OneToOne(cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "evaluator")
    var evaluator: EvaluatorEntity? = null
    var confirm: String? = null
    var result: String? = null
    var features: String? = null
    var pluralName: String? = null
    var predicate: String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as ActionEntity
        return id == that.id
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }
}
