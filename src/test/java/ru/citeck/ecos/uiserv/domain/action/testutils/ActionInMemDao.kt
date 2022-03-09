package ru.citeck.ecos.uiserv.domain.action.testutils

import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.ValuePredicate
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.uiserv.domain.action.dao.ActionDao
import ru.citeck.ecos.uiserv.domain.action.repo.ActionEntity

class ActionInMemDao(private val predicateService: PredicateService) : ActionDao {

    private val data: MutableMap<String, ActionEntity> = mutableMapOf()

    override fun getActions(max: Int, skip: Int, predicate: Predicate, sort: SortBy): List<ActionEntity> {

        val mappedFilter = PredicateUtils.mapValuePredicates(predicate) {
            if (it.getAttribute() == "moduleId") {
                ValuePredicate("extId", it.getType(), it.getValue())
            } else {
                it
            }
        } ?: VoidPredicate.INSTANCE

        val actions = predicateService.filter(data.values, mappedFilter)
        if (skip >= actions.size || max == 0) {
            return emptyList()
        }
        val result = mutableListOf<ActionEntity>()
        for (i in skip until actions.size) {
            result.add(actions[i])
            if (result.size >= max) {
                break
            }
        }
        return result
    }

    override fun getCount(): Long {
        return data.size.toLong()
    }

    override fun getCount(predicate: Predicate): Long {
        return predicateService.filter(data.values, predicate).size.toLong()
    }

    override fun getAction(id: String): ActionEntity? {
        return data[id]
    }

    override fun save(entity: ActionEntity): ActionEntity {
        data[entity.extId] = entity
        return entity
    }

    override fun delete(action: ActionEntity) {
        data.remove(action.extId)
    }
}
