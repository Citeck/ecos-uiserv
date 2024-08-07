package ru.citeck.ecos.uiserv.domain.evaluator.repo

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "evaluators")
class EvaluatorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence")
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
