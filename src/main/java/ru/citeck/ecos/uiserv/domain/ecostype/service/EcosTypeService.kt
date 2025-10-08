package ru.citeck.ecos.uiserv.domain.ecostype.service

import org.springframework.stereotype.Service
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.type.dto.CreateVariantDef
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.uiserv.domain.board.service.BoardService
import ru.citeck.ecos.uiserv.domain.ecostype.config.EcosTypesComponent
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService
import ru.citeck.ecos.uiserv.domain.journal.service.JournalService
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry

@Service
class EcosTypeService(
    private val typesComponent: EcosTypesComponent,
    private val formService: EcosFormService,
    private val boardService: BoardService,
    private val journalService: JournalService,
    private val ecosTypesRegistry: EcosTypesRegistry,
    private val workspaceService: WorkspaceService
) {

    fun getTypeRefByForm(formRef: EntityRef?): EntityRef {
        if (formRef == null || EntityRef.isEmpty(formRef)) {
            return EntityRef.EMPTY
        }
        val idInWs = workspaceService.convertToIdInWs(formRef.getLocalId())
        val typeRefForForm = formService.getFormById(idInWs)
            .map { it.typeRef }
            .orElse(EntityRef.EMPTY)
        if (EntityRef.isNotEmpty(typeRefForForm)) {
            return typeRefForForm
        }
        val ref = EntityRef.create(AppName.UISERV, "form", formRef.getLocalId())
        return EntityRef.valueOf(typesComponent.getTypeRefByForm(ref))
    }

    fun getTypeRefByBoard(boardId: String?): EntityRef {
        if (boardId.isNullOrBlank()) {
            return EntityRef.EMPTY
        }
        val boardData = boardService.getBoardById(boardId)
        if (boardData != null && EntityRef.isNotEmpty(boardData.boardDef.typeRef)) {
            return boardData.boardDef.typeRef
        }
        val ref = EntityRef.create(AppName.UISERV, "board", boardId)
        return EntityRef.valueOf(typesComponent.getTypeRefByBoard(ref))
    }

    fun getTypeRefByJournal(journalRef: EntityRef?): EntityRef {
        if (journalRef == null || EntityRef.isEmpty(journalRef)) {
            return EntityRef.EMPTY
        }
        val idInWs = workspaceService.convertToIdInWs(journalRef.getLocalId())
        val journal = journalService.getJournalById(idInWs)
        if (journal != null && EntityRef.isNotEmpty(journal.journalDef.typeRef)) {
            return journal.journalDef.typeRef
        }
        val ref = EntityRef.create(AppName.UISERV, "journal", journalRef.getLocalId())
        return EntityRef.valueOf(typesComponent.getTypeRefByJournal(ref))
    }

    fun getJournalRefByTypeRef(typeRef: EntityRef): EntityRef {

        if (EntityRef.isEmpty(typeRef)) {
            return EntityRef.EMPTY
        }

        var journalRef = typesComponent.getJournalRefByType(typeRef)
        if (EntityRef.isNotEmpty(journalRef)) {
            return EntityRef.valueOf(journalRef)
        }
        val parents = ecosTypesRegistry.getParents(typeRef)
        for (parentRef in parents) {
            if (EntityRef.isNotEmpty(parentRef)) {
                journalRef = typesComponent.getJournalRefByType(parentRef)
                if (EntityRef.isNotEmpty(journalRef)) {
                    return EntityRef.valueOf(journalRef)
                }
            }
        }

        return EntityRef.EMPTY
    }

    fun getTypeInfo(typeRef: EntityRef?): TypeDef? {
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
