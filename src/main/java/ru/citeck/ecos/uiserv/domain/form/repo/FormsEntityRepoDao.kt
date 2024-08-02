package ru.citeck.ecos.uiserv.domain.form.repo

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.uiserv.domain.form.service.FormsEntityDao
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaEntityFieldType
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverter
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverterFactory

@Component
class FormsEntityRepoDao(
    private val repo: EcosFormsRepository,
    private val jpaSearchConverterFactory: JpaSearchConverterFactory
) : FormsEntityDao {

    private lateinit var searchConv: JpaSearchConverter<EcosFormEntity>

    @PostConstruct
    fun init() {
        searchConv = jpaSearchConverterFactory.createConverter(EcosFormEntity::class.java)
            .withFieldType("title", JpaEntityFieldType.MLTEXT)
            .build()
    }

    override fun count(predicate: Predicate): Long {
        return searchConv.getCount(repo, predicate)
    }

    override fun count(): Long {
        return repo.count()
    }

    override fun findByExtId(formId: String): EcosFormEntity? {
        return repo.findByExtId(formId)
    }

    override fun findAllByExtIdIn(ids: Set<String>): Set<EcosFormEntity> {
        return repo.findAllByExtIdIn(ids)
    }

    override fun save(entity: EcosFormEntity): EcosFormEntity {
        return repo.save(entity)
    }

    override fun findAll(predicate: Predicate, max: Int, skip: Int, sort: List<SortBy>): List<EcosFormEntity> {
        return searchConv.findAll(repo, predicate, max, skip, sort)
    }

    override fun findFirstByFormKey(formKey: String): EcosFormEntity? {
        return repo.findFirstByFormKey(formKey)
    }

    override fun delete(entity: EcosFormEntity) {
        repo.delete(entity)
    }

    override fun findAllByTypeRef(typeRef: String): List<EcosFormEntity> {
        return repo.findAllByTypeRef(typeRef)
    }

    override fun findAllByTypeRefIn(typeRefs: List<String>): List<EcosFormEntity> {
        return repo.findAllByTypeRefIn(typeRefs)
    }
}
