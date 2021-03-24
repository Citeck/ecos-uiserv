package ru.citeck.ecos.uiserv.domain.action.service

import org.springframework.stereotype.Component
import ru.citeck.ecos.uiserv.domain.action.dao.ActionDao
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDto

@Component
class DaoActionsProvider(
    private val actionDao: ActionDao,
    private val actionEntityMapper: ActionEntityMapper
) : ActionsProvider {

    override fun getAction(actionId: String): ActionDto? {
        return actionEntityMapper.toDto(actionDao.getAction(actionId))
    }

    override fun getType(): String {
        return ""
    }
}
