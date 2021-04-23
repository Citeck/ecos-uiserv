package ru.citeck.ecos.uiserv.domain.evaluator.repo

import java.util.*
import javax.persistence.*

@Entity
@Table(name = "evaluators")
class EvaluatorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "evaluators_seq_gen")
    @SequenceGenerator(name = "evaluators_seq_gen")
    val id: Long? = null

    @Column(name = "evaluator_id")
    var evaluatorId: String? = null

    @Column(name = "type")
    var type: String? = null

    @Lob
    @Column(name = "config_json")
    var configJson: String? = null

    @Column(name = "inverse")
    var inverse = false

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as EvaluatorEntity
        return id == that.id
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }
}
