package ru.citeck.ecos.uiserv.domain.form.registry

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.core.ReplicatedMap
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.commons.data.entity.EntityWithMeta
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.dao.impl.ext.ExtStorageRecordsDao
import ru.citeck.ecos.records3.record.dao.impl.ext.ExtStorageRecordsDaoConfig
import ru.citeck.ecos.records3.record.dao.impl.ext.impl.ReadOnlyMapExtStorage
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService
import ru.citeck.ecos.uiserv.domain.form.service.provider.TypeFormsProvider
import ru.citeck.ecos.webapp.api.lock.EcosLockService
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import java.time.Duration

@Configuration
class FormsRegistryConfiguration(
    private val hazelcast: HazelcastInstance,
    private val formsService: EcosFormService,
    private val appLockService: EcosLockService,
    private val typeFormsProvider: TypeFormsProvider,
    val typesRegistry: EcosTypesRegistry
) {

    companion object {

        const val FORMS_REGISTRY_SOURCE_ID = "forms-registry"

        private const val TYPE_AUTO_FORM_PREFIX = "type$"
    }

    @Bean
    fun formsRegistryRecordsDao(): RecordsDao {

        val replicatedMap =
            hazelcast.getReplicatedMap<String, EntityWithMeta<FormRegistryValue>>(FORMS_REGISTRY_SOURCE_ID)

        appLockService.doInSync("forms-registry-initializer", Duration.ofMinutes(10)) {
            initDataFromDb(replicatedMap)
            formsService.addChangeWithMetaListener { before, after ->
                var id = after?.entity?.id
                if (id.isNullOrBlank()) {
                    id = before?.entity?.id
                }
                if (!id.isNullOrBlank()) {
                    replicatedMap[id] = createFormModelRegistryValue(after)
                }
            }
            formsService.addDeleteListener {
                replicatedMap.remove(it.entity.id)
            }
            typesRegistry.initializationPromise().get()
            typesRegistry.listenEventsWithMeta { _, before, after ->
                val idBefore = before?.entity?.formRef?.id ?: ""
                val idAfter = after?.entity?.formRef?.id ?: ""
                if (idBefore != idAfter) {
                    if (after != null && idAfter.startsWith(TYPE_AUTO_FORM_PREFIX)) {
                        replicatedMap[idAfter] = createRegistryValue(after)
                    } else if (idBefore.isNotBlank()) {
                        replicatedMap.remove(idBefore)
                    }
                }
            }
            typesRegistry.getAllValues().values.filter {
                it.entity.formRef.id.startsWith(TYPE_AUTO_FORM_PREFIX)
            }.forEach { value ->
                replicatedMap[value.entity.formRef.id] = createRegistryValue(value)
            }
        }

        val config = ExtStorageRecordsDaoConfig.create(ReadOnlyMapExtStorage(replicatedMap))
            .withSourceId(FORMS_REGISTRY_SOURCE_ID)
            .withEcosType("form")
            .build()

        return ExtStorageRecordsDao(config)
    }

    private fun initDataFromDb(registry: ReplicatedMap<String, EntityWithMeta<FormRegistryValue>>) {
        var skipCount = 0
        var forms = formsService.getAllFormsWithMeta(100, 0)
        while (forms.isNotEmpty()) {
            forms.forEach {
                registry[it.entity.id] = createFormModelRegistryValue(it)
            }
            skipCount += forms.size
            forms = formsService.getAllFormsWithMeta(100, skipCount)
        }
    }

    private fun createRegistryValue(typeDef: EntityWithMeta<TypeDef>): EntityWithMeta<FormRegistryValue> {
        val formDef = typeFormsProvider.createFormDef(typeDef.entity, false)
        return EntityWithMeta(
            FormRegistryValue(
                formDef.id,
                formDef.title,
                formDef.description,
                formDef.formKey,
                formDef.typeRef,
                formDef.system
            ),
            typeDef.meta
        )
    }

    private fun createFormModelRegistryValue(form: EntityWithMeta<EcosFormDef>?): EntityWithMeta<FormRegistryValue>? {
        form ?: return null
        return EntityWithMeta(
            FormRegistryValue(
                form.entity.id,
                form.entity.title,
                form.entity.description,
                form.entity.formKey,
                form.entity.typeRef,
                form.entity.system
            ),
            form.meta
        )
    }
}
