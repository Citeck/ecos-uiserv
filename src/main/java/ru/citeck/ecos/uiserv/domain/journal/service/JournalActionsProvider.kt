package ru.citeck.ecos.uiserv.domain.journal.service

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDto
import ru.citeck.ecos.uiserv.domain.action.service.ActionsProvider
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalActionDef

@Component
class JournalActionsProvider(
    private val recordsService: RecordsService
) : ActionsProvider {

    override fun getAction(actionId: String): ActionDto? {

        val journalIdDelim = actionId.indexOf('$')
        if (journalIdDelim < 1 || journalIdDelim >= actionId.length - 1) {
            return null
        }

        val journalId = actionId.substring(0, journalIdDelim)

        val ctxCache: MutableMap<String, JournalActionAtts> = RequestContext.getCurrent()
            ?.getMap(JournalActionsProvider::class.java.name + ".jrl-act-def") ?: HashMap()

        val actionAtts = ctxCache.computeIfAbsent(journalId) {
            recordsService.getAtts(RecordRef.create("rjournal", it), JournalActionAtts::class.java)
        }

        val requiredActionId = "journal$$actionId"
        return actionAtts.actionsDef.firstOrNull { it.id == requiredActionId }?.let {
            Json.mapper.convert(it, ActionDto::class.java)
        }
    }

    override fun getType() = "journal"

    class JournalActionAtts(
        val actionsDef: List<JournalActionDef>
    )
}

