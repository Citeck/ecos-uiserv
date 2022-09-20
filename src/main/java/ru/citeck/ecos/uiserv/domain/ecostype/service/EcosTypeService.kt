package ru.citeck.ecos.uiserv.domain.ecostype.service

import org.springframework.stereotype.Service
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.type.dto.CreateVariantDef
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.uiserv.domain.ecostype.config.EcosTypesConfig
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry

@Service
class EcosTypeService(
    private val typesConfig: EcosTypesConfig,
    private val ecsosTypesRegistry: EcosTypesRegistry
) {

    fun getTypeRefByForm(formRef: RecordRef?): RecordRef {
        if (formRef == null || RecordRef.isEmpty(formRef)) {
            return RecordRef.EMPTY
        }
        val ref = RecordRef.create("uiserv", "form", formRef.id)
        return typesConfig.getTypeRefByForm(ref)
    }

    fun getTypeRefByBoard(boardId: String?): RecordRef {
        if (boardId.isNullOrBlank()) {
            return RecordRef.EMPTY
        }
        val ref = RecordRef.create("uiserv", "board", boardId)
        return typesConfig.getTypeRefByBoard(ref)
    }

    fun getTypeRefByJournal(journalRef: RecordRef?): RecordRef {
        if (journalRef == null || RecordRef.isEmpty(journalRef)) {
            return RecordRef.EMPTY
        }
        val ref = RecordRef.create("uiserv", "journal", journalRef.id)
        return typesConfig.getTypeRefByJournal(ref)
    }

    fun getJournalRefByTypeRef(typeRef: RecordRef): RecordRef {

        if (EntityRef.isEmpty(typeRef)) {
            return RecordRef.EMPTY
        }

        var journalRef = typesConfig.getJournalRefByType(typeRef)
        if (EntityRef.isNotEmpty(journalRef)) {
            return journalRef
        }
        val parents = ecsosTypesRegistry.getParents(typeRef)
        for (parentRef in parents) {
            if (EntityRef.isNotEmpty(parentRef)) {
                journalRef = typesConfig.getJournalRefByType(parentRef)
                if (EntityRef.isNotEmpty(journalRef)) {
                    return journalRef
                }
            }
        }

        return RecordRef.EMPTY
    }

    fun getTypeInfo(typeRef: RecordRef?): TypeDef? {
        if (typeRef == null) {
            return null
        }
        val typeInfo = typesConfig.getTypeInfo(typeRef) ?: return null

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
