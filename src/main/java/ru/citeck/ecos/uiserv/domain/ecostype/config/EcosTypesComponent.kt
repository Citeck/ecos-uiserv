package ru.citeck.ecos.uiserv.domain.ecostype.config

import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Configuration
class EcosTypesComponent(
    private val typesRegistry: EcosTypesRegistry,
    private val workspaceService: WorkspaceService
) {

    private val parentByType = ConcurrentHashMap<EntityRef, EntityRef>()
    private val childrenByType = ConcurrentHashMap<EntityRef, MutableList<EntityRef>>()
    private val typeInfoByTypeRef = ConcurrentHashMap<EntityRef, TypeDef>()

    private val typeByJournal = ConcurrentHashMap<EntityRef, EntityRef>()
    private val journalByType = ConcurrentHashMap<EntityRef, EntityRef>()

    private val typeByForm = ConcurrentHashMap<EntityRef, EntityRef>()
    private val formByType = ConcurrentHashMap<EntityRef, EntityRef>()

    private val typeByBoard = ConcurrentHashMap<EntityRef, EntityRef>()
    private val boardByType = ConcurrentHashMap<EntityRef, EntityRef>()

    @PostConstruct
    fun init() {
        typesRegistry.listenEvents { _, _, after ->
            if (after != null) {
                onTypeChanged(after)
            }
        }
    }

    private fun onTypeChanged(type: TypeDef) {

        val typeRef = ModelUtils.getTypeRef(type.id)
        typeInfoByTypeRef[typeRef] = type

        val prevParentRef = parentByType[typeRef] ?: EntityRef.EMPTY
        val newParentRef = type.parentRef

        if (prevParentRef != type.parentRef) {
            if (EntityRef.isNotEmpty(prevParentRef)) {
                childrenByType[prevParentRef]?.remove(typeRef)
            }
            if (EntityRef.isNotEmpty(newParentRef)) {
                childrenByType.computeIfAbsent(newParentRef) { CopyOnWriteArrayList() }.add(typeRef)
            }
            parentByType[typeRef] = newParentRef
        }

        val typeWs = type.workspace
        updateRefs(journalByType, typeByJournal, typeRef, typeWs, type.journalRef)
        updateRefs(formByType, typeByForm, typeRef, typeWs, type.formRef)
        updateRefs(boardByType, typeByBoard, typeRef, typeWs, type.boardRef)
    }

    private fun updateRefs(
        refByTypeMap: MutableMap<EntityRef, EntityRef>,
        typeByRefMap: MutableMap<EntityRef, EntityRef>,
        typeRef: EntityRef,
        typeWorkspace: String,
        newRef: EntityRef?
    ) {
        val newRefNotNull = newRef ?: EntityRef.EMPTY
        val prevRef = refByTypeMap[typeRef] ?: EntityRef.EMPTY

        if (newRefNotNull == prevRef) {
            return
        }

        // Forward direction (refByType) records what a type points at, always.
        if (EntityRef.isNotEmpty(newRefNotNull)) {
            refByTypeMap[typeRef] = newRefNotNull
        } else {
            refByTypeMap.remove(typeRef)
        }

        // Reverse direction (typeByRef) is gated: we only index "type-of-X"
        // associations where the ref's encoded workspace matches the type's
        // workspace. Cross-workspace refs (WS→default bare refs, etc.) stay
        // out of the reverse index — see review-notes / task COREDEV-111.
        if (EntityRef.isNotEmpty(prevRef)) {
            typeByRefMap.remove(prevRef, typeRef)
        }
        if (EntityRef.isNotEmpty(newRefNotNull) && refMatchesWorkspace(newRefNotNull, typeWorkspace)) {
            typeByRefMap[newRefNotNull] = typeRef
        }
    }

    private fun refMatchesWorkspace(ref: EntityRef, typeWorkspace: String): Boolean {
        val refWs = workspaceService.convertToIdInWs(ref.getLocalId()).workspace
        return refWs == typeWorkspace
    }

    fun addOnTypeChangedListener(listener: (TypeDef) -> Unit) {
        typesRegistry.listenEvents { _, _, after ->
            if (after != null) {
                listener.invoke(after)
            }
        }
    }

    fun getJournalRefByType(journalRef: EntityRef): EntityRef {
        return journalByType[journalRef] ?: EntityRef.EMPTY
    }

    fun getTypeRefByJournal(journalRef: EntityRef): EntityRef {
        return typeByJournal[journalRef] ?: EntityRef.EMPTY
    }

    fun getTypeRefByForm(formRef: EntityRef): EntityRef {
        return typeByForm[formRef] ?: EntityRef.EMPTY
    }

    fun getTypeRefByBoard(boardRef: EntityRef): EntityRef {
        return typeByBoard[boardRef] ?: EntityRef.EMPTY
    }

    fun getTypeInfo(typeRef: EntityRef): TypeDef? {
        return typeInfoByTypeRef[typeRef]
    }
}
