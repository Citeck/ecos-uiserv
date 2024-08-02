package ru.citeck.ecos.uiserv.domain.admin.repo

import jakarta.persistence.*
import lombok.Data
import ru.citeck.ecos.uiserv.app.common.repo.AbstractAuditingEntity
import java.io.Serializable

@Data
@Entity
@Table(name = "admin_sections_group")
class AdminSectionsGroupEntity : AbstractAuditingEntity(), Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence")
    val id: Long? = null

    lateinit var extId: String
    lateinit var sections: String

    var name: String? = null
    var groupOrder = 0f
}
