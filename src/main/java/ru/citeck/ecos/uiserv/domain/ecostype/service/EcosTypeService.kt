package ru.citeck.ecos.uiserv.domain.ecostype.service

import org.springframework.stereotype.Service
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.type.dto.CreateVariantDef
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.uiserv.domain.board.service.BoardService
import ru.citeck.ecos.uiserv.domain.ecostype.config.EcosTypesComponent
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService
import ru.citeck.ecos.uiserv.domain.journal.service.JournalService
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry

@Service
class EcosTypeService(
    private val typesComponent: EcosTypesComponent,
    private val formService: EcosFormService,
    private val boardService: BoardService,
    private val journalService: JournalService,
    private val ecosTypesRegistry: EcosTypesRegistry
) {

    fun getTypeRefByForm(formRef: RecordRef?): RecordRef {
        if (formRef == null || RecordRef.isEmpty(formRef)) {
            return RecordRef.EMPTY
        }
        val typeRefForForm = formService.getFormById(formRef.getLocalId())
            .map { it.typeRef }
            .orElse(RecordRef.EMPTY)
        if (EntityRef.isNotEmpty(typeRefForForm)) {
            return typeRefForForm
        }
        val ref = RecordRef.create("uiserv", "form", formRef.id)
        return RecordRef.valueOf(typesComponent.getTypeRefByForm(ref))
    }

    fun getTypeRefByBoard(boardId: String?): RecordRef {
        if (boardId.isNullOrBlank()) {
            return RecordRef.EMPTY
        }
        val boardData = boardService.getBoardById(boardId)
        if (boardData != null && EntityRef.isNotEmpty(boardData.boardDef.typeRef)) {
            return boardData.boardDef.typeRef
        }
        val ref = RecordRef.create("uiserv", "board", boardId)
        return RecordRef.valueOf(typesComponent.getTypeRefByBoard(ref))
    }

    fun getTypeRefByJournal(journalRef: RecordRef?): RecordRef {
        if (journalRef == null || RecordRef.isEmpty(journalRef)) {
            return RecordRef.EMPTY
        }
        val journal = journalService.getJournalById(journalRef.getLocalId())
        if (journal != null && EntityRef.isNotEmpty(journal.journalDef.typeRef)) {
            return journal.journalDef.typeRef
        }
        val ref = RecordRef.create("uiserv", "journal", journalRef.id)
        return RecordRef.valueOf(typesComponent.getTypeRefByJournal(ref))
    }

    fun getJournalRefByTypeRef(typeRef: RecordRef): RecordRef {

        if (EntityRef.isEmpty(typeRef)) {
            return RecordRef.EMPTY
        }

        var journalRef = typesComponent.getJournalRefByType(typeRef)
        if (EntityRef.isNotEmpty(journalRef)) {
            return RecordRef.valueOf(journalRef)
        }
        val parents = ecosTypesRegistry.getParents(typeRef)
        for (parentRef in parents) {
            if (EntityRef.isNotEmpty(parentRef)) {
                journalRef = typesComponent.getJournalRefByType(parentRef)
                if (EntityRef.isNotEmpty(journalRef)) {
                    return RecordRef.valueOf(journalRef)
                }
            }
        }

        return RecordRef.EMPTY
    }

    fun getTypeInfo(typeRef: RecordRef?): TypeDef? {
        if (typeRef == null) {
            return null
        }
        val typeInfo = typesComponent.getTypeInfo(typeRef) ?: return null

        val copy = typeInfo.copy()
        copy.withCreateVariants(filterCreateVariants(copy.createVariants))
        return copy.build()
    }

    private fun filterCreateVariants(variants: List<CreateVariantDef>?): List<CreateVariantDef>? {
        variants ?: return null
        val currentAuthorities = AuthContext.getCurrentUserWithAuthorities().toHashSet()
        return variants.filter {
            it.allowedFor.isEmpty() || it.allowedFor.any { auth -> currentAuthorities.contains(auth) }
        }
    }
}
