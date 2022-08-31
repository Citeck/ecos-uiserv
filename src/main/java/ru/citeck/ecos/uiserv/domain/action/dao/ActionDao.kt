package ru.citeck.ecos.uiserv.domain.action.dao

import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.uiserv.domain.action.repo.ActionEntity

interface ActionDao {

    fun getActions(predicate: Predicate, max: Int, skip: Int, sort: List<SortBy>): List<ActionEntity>

    fun getCount(): Long

    fun getCount(predicate: Predicate): Long

    fun getAction(id: String): ActionEntity?

    fun save(entity: ActionEntity): ActionEntity

    fun delete(action: ActionEntity)
}
