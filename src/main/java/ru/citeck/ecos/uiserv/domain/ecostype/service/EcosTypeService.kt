package ru.citeck.ecos.uiserv.domain.ecostype.service

import org.springframework.stereotype.Service
import ru.citeck.ecos.model.lib.type.dto.CreateVariantDef
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.uiserv.app.common.service.AuthoritiesSupport
import ru.citeck.ecos.uiserv.domain.ecostype.config.EcosTypesConfig
import ru.citeck.ecos.uiserv.domain.ecostype.dto.EcosTypeInfo

@Service
class EcosTypeService(
    private val typesConfig: EcosTypesConfig,
    private val authoritiesSupport: AuthoritiesSupport
) {

    fun getTypeRefByJournal(journalRef: RecordRef?): RecordRef {
        if (journalRef == null || RecordRef.isEmpty(journalRef)) {
            return RecordRef.EMPTY
        }
        val ref = RecordRef.create("uiserv", "journal", journalRef.id);
        return typesConfig.getTypeRefByJournal(ref)
    }

    fun getJournalRefByTypeRef(typeRef: RecordRef): RecordRef {

        if (RecordRef.isEmpty(typeRef)) {
            return RecordRef.EMPTY
        }

        var journalRef = typesConfig.getJournalRefByType(typeRef)
        if (RecordRef.isNotEmpty(journalRef)) {
            return journalRef
        }
        val parents = typesConfig.getTypeInfo(typeRef)?.parents ?: emptyList()
        for (parentRef in parents) {
            if (RecordRef.isNotEmpty(parentRef)) {
                journalRef = typesConfig.getJournalRefByType(parentRef)
                if (RecordRef.isNotEmpty(journalRef)) {
                    return journalRef
                }
            }
        }

        return RecordRef.EMPTY
    }

    fun getTypeInfo(typeRef: RecordRef?): EcosTypeInfo? {
        if (typeRef == null) {
            return null
        }
        val typeInfo = typesConfig.getTypeInfo(typeRef) ?: return null

        val copy = EcosTypeInfo(typeInfo)
        copy.inhCreateVariants = filterCreateVariants(copy.inhCreateVariants)
        return copy
    }

    private fun filterCreateVariants(variants: List<CreateVariantDef>?): List<CreateVariantDef>? {
        variants ?: return null
        val currentAuthorities = authoritiesSupport.currentUserAuthorities.toHashSet()
        return variants.filter {
            it.allowedFor.isEmpty() || it.allowedFor.any { auth -> currentAuthorities.contains(auth) }
        }
    }
}
