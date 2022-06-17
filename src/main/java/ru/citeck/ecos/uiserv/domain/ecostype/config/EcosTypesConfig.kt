package ru.citeck.ecos.uiserv.domain.ecostype.config

import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.model.lib.type.service.utils.TypeUtils
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import javax.annotation.PostConstruct

@Configuration
class EcosTypesConfig(
    private val typesRegistry: EcosTypesRegistry
) {

    private val parentByType = ConcurrentHashMap<RecordRef, RecordRef>()
    private val childrenByType = ConcurrentHashMap<RecordRef, MutableList<RecordRef>>()
    private val typeInfoByTypeRef = ConcurrentHashMap<RecordRef, TypeDef>()

    private val typeByJournal = ConcurrentHashMap<RecordRef, RecordRef>()
    private val journalByType = ConcurrentHashMap<RecordRef, RecordRef>()

    private val typeByForm = ConcurrentHashMap<RecordRef, RecordRef>()
    private val formByType = ConcurrentHashMap<RecordRef, RecordRef>()

    private val typeByBoard = ConcurrentHashMap<RecordRef, RecordRef>()
    private val boardByType = ConcurrentHashMap<RecordRef, RecordRef>()

    @PostConstruct
    fun init() {
        typesRegistry.listenEvents { _, _, after ->
            if (after != null) {
                onTypeChanged(after)
            }
        }
    }

    private fun onTypeChanged(type: TypeDef) {

        val typeRef = TypeUtils.getTypeRef(type.id)
        typeInfoByTypeRef[typeRef] = type

        val prevParentRef = parentByType[typeRef] ?: RecordRef.EMPTY
        val newParentRef = type.parentRef ?: RecordRef.EMPTY

        if (prevParentRef != type.parentRef) {
            if (RecordRef.isNotEmpty(prevParentRef)) {
                childrenByType[prevParentRef]?.remove(typeRef)
            }
            if (RecordRef.isNotEmpty(newParentRef)) {
                childrenByType.computeIfAbsent(newParentRef) { CopyOnWriteArrayList() }.add(typeRef)
            }
            parentByType[typeRef] = newParentRef
        }

        updateRefs(journalByType, typeByJournal, typeRef, type.journalRef)
        updateRefs(formByType, typeByForm, typeRef, type.formRef)
        updateRefs(boardByType, typeByBoard, typeRef, type.boardRef)
    }

    private fun updateRefs(
        refByTypeMap: MutableMap<RecordRef, RecordRef>,
        typeByRefMap: MutableMap<RecordRef, RecordRef>,
        typeRef: RecordRef,
        newRef: RecordRef?
    ) {
        val newRefNotNull = newRef ?: RecordRef.EMPTY
        val prevRef = refByTypeMap[typeRef] ?: RecordRef.EMPTY

        if (newRef != prevRef) {
            if (RecordRef.isNotEmpty(prevRef)) {
                typeByRefMap.remove(prevRef)
            }
            if (RecordRef.isNotEmpty(newRef)) {
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

    fun getJournalRefByType(journalRef: RecordRef): RecordRef {
        return journalByType[journalRef] ?: RecordRef.EMPTY
    }

    fun getTypeRefByJournal(journalRef: RecordRef): RecordRef {
        return typeByJournal[journalRef] ?: RecordRef.EMPTY
    }

    fun getTypeRefByForm(formRef: RecordRef): RecordRef {
        return typeByForm[formRef] ?: RecordRef.EMPTY
    }

    fun getTypeRefByBoard(boardRef: RecordRef): RecordRef {
        return typeByBoard[boardRef] ?: RecordRef.EMPTY
    }

    fun getTypeInfo(typeRef: RecordRef): TypeDef? {
        return typeInfoByTypeRef[typeRef]
    }
}
