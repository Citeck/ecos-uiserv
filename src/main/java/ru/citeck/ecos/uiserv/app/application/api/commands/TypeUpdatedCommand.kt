package ru.citeck.ecos.uiserv.app.application.api.commands

import org.springframework.stereotype.Component
import ru.citeck.ecos.commands.CommandExecutor
import ru.citeck.ecos.commands.annotation.CommandType
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService
import ru.citeck.ecos.uiserv.domain.journal.service.JournalService

@Component
class TypeUpdatedCommandExecutor(
    val formService: EcosFormService,
    val journalService: JournalService
) : CommandExecutor<TypeUpdatedCommand> {

    override fun execute(command: TypeUpdatedCommand): TypeUpdatedCommandResponse {

        val typeRef = command.typeRef ?: return TypeUpdatedCommandResponse(true)
        val formRef = command.formRef
        if (formRef != null && RecordRef.isNotEmpty(formRef)) {
            formService.updateFormType(formRef.id, typeRef)
        }
        val journalRef = command.journalRef
        if (journalRef != null && RecordRef.isNotEmpty(journalRef)) {
            journalService.updateJournalType(journalRef.id, typeRef)
        }
        return TypeUpdatedCommandResponse(true)
    }
}

@CommandType("uiserv.type-updated")
class TypeUpdatedCommand(
    val typeRef: RecordRef?,
    val formRef: RecordRef?,
    val journalRef: RecordRef?
)

class TypeUpdatedCommandResponse(
    val result: Boolean
)
