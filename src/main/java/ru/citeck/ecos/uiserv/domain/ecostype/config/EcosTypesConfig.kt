package ru.citeck.ecos.uiserv.domain.ecostype.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.core.task.AsyncTaskExecutor
import ru.citeck.ecos.model.lib.type.dto.TypeModelDef
import ru.citeck.ecos.model.lib.type.repo.TypesRepo
import ru.citeck.ecos.model.lib.type.service.utils.TypeUtils
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.source.dao.local.InMemRecordsDao
import ru.citeck.ecos.records2.source.dao.local.RemoteSyncRecordsDao
import ru.citeck.ecos.uiserv.domain.ecostype.dto.EcosTypeInfo
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Configuration
class EcosTypesConfig(
    @Qualifier("taskExecutor")
    private val taskExecutor: AsyncTaskExecutor
) : ApplicationListener<ContextRefreshedEvent> {

    private val parentByType = ConcurrentHashMap<RecordRef, RecordRef>()
    private val childrenByType = ConcurrentHashMap<RecordRef, MutableList<RecordRef>>()
    private val typeInfoByTypeRef = ConcurrentHashMap<RecordRef, EcosTypeInfo>()

    private val typeByJournal = ConcurrentHashMap<RecordRef, RecordRef>()
    private val journalByType = ConcurrentHashMap<RecordRef, RecordRef>()

    private var typesSyncStarted = false

    @Value("\${uiserv.ecos-types-sync.active}")
    private var typesSyncEnabled: Boolean = false

    @Bean(name = ["typesSyncRecordsDao"])
    fun createRemoteTypesSyncRecordsDao(): InMemRecordsDao<EcosTypeInfo> {
        val syncDao: InMemRecordsDao<EcosTypeInfo> = if (typesSyncEnabled) {
            RemoteSyncRecordsDao("emodel/type", EcosTypeInfo::class.java)
        } else {
            InMemRecordsDao("emodel/type")
        }
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

    fun getJournalRefByType(journalRef: RecordRef): RecordRef {
        return journalByType[journalRef] ?: RecordRef.EMPTY
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

            override fun getModel(typeRef: RecordRef): TypeModelDef {
                return typesDao.getRecord(typeRef.id).orElse(null)?.model ?: return TypeModelDef.EMPTY
            }

            override fun getParent(typeRef: RecordRef): RecordRef {
                return typesDao.getRecord(typeRef.id).orElse(null)?.parentRef ?: RecordRef.EMPTY
            }

            override fun getChildren(typeRef: RecordRef): List<RecordRef> {
                return childrenByType[typeRef] ?: emptyList()
            }
        }
    }

    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        if (!typesSyncStarted && typesSyncEnabled) {
            taskExecutor.execute {
                Thread.sleep(5_000)
                createRemoteTypesSyncRecordsDao().getRecord("base")
            }
            typesSyncStarted = true
        }
    }
}
