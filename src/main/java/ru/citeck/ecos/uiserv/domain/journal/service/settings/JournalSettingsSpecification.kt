package ru.citeck.ecos.uiserv.domain.journal.service.settings

import org.springframework.data.jpa.domain.Specification
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalSettingsEntity

class JournalSettingsSpecification {

    companion object {
        @JvmStatic
        fun journalEquals(journalId: String): Specification<JournalSettingsEntity> {
            return Specification { root, query, builder ->
                val journalIdAtt = root.get<Any>("journalId")
                builder.equal(journalIdAtt, journalId)
            }
        }

        @JvmStatic
        fun authorityIn(authorities: List<String>): Specification<JournalSettingsEntity> {
            return Specification { root, query, builder ->
                val authorityAtt = root.get<Any>("authority")
                builder.and(authorityAtt.`in`(authorities))
            }
        }

        @JvmStatic
        fun authorityNotEqualToCreator(): Specification<JournalSettingsEntity> {
            return Specification { root, query, builder ->
                val authorityAtt = root.get<Any>("authority")
                val createdByAtt = root.get<Any>("createdBy")
                builder.notEqual(authorityAtt, createdByAtt)
            }
        }
    }

}
