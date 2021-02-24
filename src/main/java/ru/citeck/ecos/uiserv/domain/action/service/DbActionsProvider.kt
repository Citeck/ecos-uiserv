package ru.citeck.ecos.uiserv.domain.action.service

import org.springframework.stereotype.Component
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDto
import ru.citeck.ecos.uiserv.domain.action.repo.ActionRepository

@Component
class DbActionsProvider(
    private val actionRepository: ActionRepository,
    private val actionEntityMapper: ActionEntityMapper
) : ActionsProvider {

    override fun getAction(actionId: String): ActionDto? {
        return actionEntityMapper.toDto(actionRepository.findByExtId(actionId))
    }

    override fun getType(): String {
        return ""
    }
}
