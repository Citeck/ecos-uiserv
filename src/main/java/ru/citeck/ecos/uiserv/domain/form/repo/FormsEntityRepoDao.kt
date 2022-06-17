package ru.citeck.ecos.uiserv.domain.form.repo

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Component
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.uiserv.domain.form.service.FormsEntityDao
import java.util.stream.Collectors
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Root

@Component
class FormsEntityRepoDao(
    private val repo: EcosFormsRepository
) : FormsEntityDao {

    override fun count(): Long {
        return repo.count()
    }

    override fun findByExtId(formId: String): EcosFormEntity? {
        return repo.findByExtId(formId)
    }

    override fun save(entity: EcosFormEntity): EcosFormEntity {
        return repo.save(entity)
    }

    override fun findAll(predicate: Predicate, max: Int, skip: Int): List<EcosFormEntity> {

        val page = PageRequest.of(skip / max, max, Sort.by(Sort.Direction.DESC, "id"))

        if (predicate is VoidPredicate) {
            return repo.findAll(page)
                .stream()
                .collect(Collectors.toList())
        }

        val dto = PredicateUtils.convertToDto(predicate, FilterPredicate::class.java)
            ?: return repo.findAll(page)
                .stream()
                .collect(Collectors.toList())

        var spec: Specification<EcosFormEntity>? = null

        spec = orContains(spec, "extId", dto.moduleId)
        spec = orContains(spec, "title", dto.title)
        spec = orContains(spec, "formKey", dto.formKey)

        return if (spec != null) {
            repo.findAll(spec, page)
                .stream()
                .collect(Collectors.toList())
        } else {
            repo.findAll(page)
                .stream()
                .collect(Collectors.toList())
        }
    }

    private fun orContains(
        spec0: Specification<EcosFormEntity>?,
        field: String,
        value: String?
    ): Specification<EcosFormEntity>? {

        if (value == null || value.isBlank()) {
            return spec0
        }
        return orSpec(
            spec0,
            Specification { root: Root<EcosFormEntity>,
                _: CriteriaQuery<*>,
                builder: CriteriaBuilder ->

                builder.like(
                    builder.lower(root.get(field)),
                    "%" + value.toLowerCase() + "%"
                )
            }
        )
    }

    private fun orSpec(
        spec0: Specification<EcosFormEntity>?,
        spec1: Specification<EcosFormEntity>?
    ): Specification<EcosFormEntity>? {
        if (spec0 == null) {
            return spec1
        } else if (spec1 == null) {
            return spec0
        }
        return spec0.or(spec1)
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

    data class FilterPredicate(
        var moduleId: String? = null,
        var formKey: String? = null,
        var title: String? = null
    )
}
