package ru.citeck.ecos.uiserv.domain.action.dao

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.uiserv.domain.action.repo.ActionEntity
import ru.citeck.ecos.uiserv.domain.action.repo.ActionRepository
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverter
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverterFactory

@Component
class ActionRepoDao(
    private val repo: ActionRepository,
    private val jpaSearchConverterFactory: JpaSearchConverterFactory
) : ActionDao {

    private lateinit var searchConv: JpaSearchConverter<ActionEntity>

    @PostConstruct
    fun init() {
        searchConv = jpaSearchConverterFactory.createConverter(ActionEntity::class.java).build()
    }

    override fun getActions(predicate: Predicate, max: Int, skip: Int, sort: List<SortBy>): List<ActionEntity> {
        return searchConv.findAll(repo, predicate, max, skip, sort)
    }

    override fun getCount(): Long {
        return repo.count()
    }

    override fun getCount(predicate: Predicate): Long {
        return searchConv.getCount(repo, predicate)
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
}
