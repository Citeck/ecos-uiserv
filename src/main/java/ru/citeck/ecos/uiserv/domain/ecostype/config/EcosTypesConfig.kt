package ru.citeck.ecos.uiserv.domain.ecostype.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.model.lib.type.dto.TypeDef
import ru.citeck.ecos.model.lib.type.dto.TypeModelDef
import ru.citeck.ecos.model.lib.type.repo.TypesRepo
import ru.citeck.ecos.model.lib.type.service.utils.TypeUtils
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.source.dao.local.RemoteSyncRecordsDao
import ru.citeck.ecos.uiserv.domain.ecostype.dto.EcosTypeInfo
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Configuration
class EcosTypesConfig {

    private val parentByType = ConcurrentHashMap<RecordRef, RecordRef>()
    private val childrenByType = ConcurrentHashMap<RecordRef, MutableList<RecordRef>>()
    private val typeInfoByTypeRef = ConcurrentHashMap<RecordRef, EcosTypeInfo>()

    private val typeByJournal = ConcurrentHashMap<RecordRef, RecordRef>()
    private val journalByType = ConcurrentHashMap<RecordRef, RecordRef>()

    @Bean(name = ["remoteTypesSyncRecordsDao"])
    fun createRemoteTypesSyncRecordsDao(): RemoteSyncRecordsDao<EcosTypeInfo> {
        val syncDao = RemoteSyncRecordsDao("emodel/type", EcosTypeInfo::class.java)
        syncDao.addOnChangeListener { onTypeChanged(it) }
        return syncDao
    }

    private fun onTypeChanged(type: EcosTypeInfo) {

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

        val prevJournal = journalByType[typeRef] ?: RecordRef.EMPTY
        val newJournal = type.journalRef ?: RecordRef.EMPTY

        if (newJournal != prevJournal) {
            if (RecordRef.isNotEmpty(prevJournal)) {
                typeByJournal.remove(prevJournal)
            }
            if (RecordRef.isNotEmpty(newJournal)) {
                typeByJournal[newJournal] = typeRef
            }
            journalByType[typeRef] = newJournal
        }
    }

    fun addOnTypeChangedListener(listener: (EcosTypeInfo) -> Unit) {
        createRemoteTypesSyncRecordsDao().addOnChangeListener { listener.invoke(it) }
    }

    fun getTypeRefByJournal(journalRef: RecordRef): RecordRef {
        return typeByJournal[journalRef] ?: RecordRef.EMPTY
    }

    fun getTypeInfo(typeRef: RecordRef): EcosTypeInfo? {
        return typeInfoByTypeRef[typeRef]
    }

    @Bean
    fun ecosTypesRepo(): TypesRepo {

        val typesDao = createRemoteTypesSyncRecordsDao()

        return object : TypesRepo {

            override fun getChildren(typeRef: RecordRef): List<RecordRef> {
                return childrenByType[typeRef] ?: emptyList()
            }

            override fun getTypeDef(typeRef: RecordRef): TypeDef? {

                val typeInfo = typesDao.getRecord(typeRef.id).orElse(null) ?: return null

                return TypeDef.create()
                    .withId(typeInfo.id)
                    .withName(typeInfo.name ?: MLText())
                    .withParentRef(typeInfo.parentRef)
                    .withModel(typeInfo.model ?: TypeModelDef.EMPTY)
                    .build()
            }
        }
    }
}
