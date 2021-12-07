package ru.citeck.ecos.uiserv.domain.ecostype.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.core.task.AsyncTaskExecutor
import ru.citeck.ecos.model.lib.type.dto.TypeInfo
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

    private val typeByForm = ConcurrentHashMap<RecordRef, RecordRef>()
    private val formByType = ConcurrentHashMap<RecordRef, RecordRef>()

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

        updateRefs(journalByType, typeByJournal, typeRef, type.journalRef)
        updateRefs(formByType, typeByForm, typeRef, type.formRef)
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

    fun addOnTypeChangedListener(listener: (EcosTypeInfo) -> Unit) {
        createRemoteTypesSyncRecordsDao().addOnChangeListener { listener.invoke(it) }
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

            override fun getTypeInfo(typeRef: RecordRef): TypeInfo? {
                return TypeInfo.create {
                    withModel(typesDao.getRecord(typeRef.id).orElse(null)?.model)
                    withParentRef(typesDao.getRecord(typeRef.id).orElse(null)?.parentRef)
                }
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
