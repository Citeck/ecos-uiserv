package ru.citeck.ecos.uiserv.domain.ecostype.config

import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Configuration
class EcosTypesComponent(
    private val typesRegistry: EcosTypesRegistry
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

        updateRefs(journalByType, typeByJournal, typeRef, type.journalRef)
        updateRefs(formByType, typeByForm, typeRef, type.formRef)
        updateRefs(boardByType, typeByBoard, typeRef, type.boardRef)
    }

    private fun updateRefs(
        refByTypeMap: MutableMap<EntityRef, EntityRef>,
        typeByRefMap: MutableMap<EntityRef, EntityRef>,
        typeRef: EntityRef,
        newRef: EntityRef?
    ) {
        val newRefNotNull = newRef ?: EntityRef.EMPTY
        val prevRef = refByTypeMap[typeRef] ?: EntityRef.EMPTY

        if (newRef != prevRef) {
            if (EntityRef.isNotEmpty(prevRef)) {
                typeByRefMap.remove(prevRef)
            }
            if (EntityRef.isNotEmpty(newRef)) {
                typeByRefMap[newRefNotNull] = typeRef
            }
            refByTypeMap[typeRef] = newRefNotNull
        }
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
