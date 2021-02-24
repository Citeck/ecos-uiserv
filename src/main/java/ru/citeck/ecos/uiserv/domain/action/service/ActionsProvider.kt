package ru.citeck.ecos.uiserv.domain.action.service

import ru.citeck.ecos.uiserv.domain.action.dto.ActionDto

interface ActionsProvider {

    fun getAction(actionId: String): ActionDto?

    fun getType(): String
}
