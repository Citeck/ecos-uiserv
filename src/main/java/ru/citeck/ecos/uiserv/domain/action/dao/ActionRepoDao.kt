package ru.citeck.ecos.uiserv.domain.action.dao

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Component
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.uiserv.domain.action.repo.ActionEntity
import ru.citeck.ecos.uiserv.domain.action.repo.ActionRepository
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Root

@Component
class ActionRepoDao(
    private val repo: ActionRepository
) : ActionDao {

    override fun getActions(max: Int, skip: Int, predicate: Predicate, sort: SortBy): List<ActionEntity> {
        val page = PageRequest.of(skip / max, max, Sort.by(Sort.Direction.DESC, "id"))
        val spec = toSpec(predicate)
        return if (spec != null) {
            repo.findAll(spec, page).toList()
        } else {
            repo.findAll(page).toList()
        }
    }

    override fun getCount(): Long {
        return repo.count()
    }

    override fun getCount(predicate: Predicate): Long {
        val spec = toSpec(predicate)
        return spec?.let { repo.count(it) } ?: repo.count()
    }

    override fun getAction(id: String): ActionEntity? {
        return repo.findByExtId(id)
    }

    override fun save(entity: ActionEntity): ActionEntity {
        return repo.save(entity)
    }

    override fun delete(action: ActionEntity) {
        repo.delete(action)
    }

    private fun toSpec(predicate: Predicate): Specification<ActionEntity>? {

        if (predicate is VoidPredicate) {
            return null
        }

        val predicateDto = PredicateUtils.convertToDto(predicate, PredicateDto::class.java)
        var spec: Specification<ActionEntity>? = null

        val moduleId = predicateDto.moduleId
        if (!moduleId.isNullOrBlank()) {
            spec = Specification { root: Root<ActionEntity>,
                query: CriteriaQuery<*>?,
                builder: CriteriaBuilder ->

                builder.like(builder.lower(root.get("extId")), "%" + moduleId.toLowerCase() + "%")
            }
        }

        val name = predicateDto.name
        if (!name.isNullOrBlank()) {

            val nameSpec = Specification { root: Root<ActionEntity>,
                query: CriteriaQuery<*>?,
                builder: CriteriaBuilder ->

                builder.like(builder.lower(root.get("name")), "%" + name.toLowerCase() + "%")
            }
            spec = spec?.or(nameSpec) ?: nameSpec
        }

        return spec
    }

    class PredicateDto {
        var moduleId: String? = null
        var name: String? = null
    }
}
