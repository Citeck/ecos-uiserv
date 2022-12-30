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
import ru.citeck.ecos.webapp.api.lock.EcosLockApi
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import java.time.Duration

@Configuration
class FormsRegistryConfiguration(
    private val hazelcast: HazelcastInstance,
    private val formsService: EcosFormService,
    private val appLockService: EcosLockApi,
    private val typeFormsProvider: TypeFormsProvider,
    val typesRegistry: EcosTypesRegistry
) {

    companion object {

        const val FORMS_REGISTRY_SOURCE_ID = "forms-registry"

        private const val TYPE_AUTO_FORM_PREFIX = "type$"
    }

    @Bean
    fun formsRegistryRecordsDao(): RecordsDao {

        val registry =
            hazelcast.getReplicatedMap<String, EntityWithMeta<FormRegistryValue>>(FORMS_REGISTRY_SOURCE_ID)

        appLockService.doInSync("forms-registry-initializer", Duration.ofMinutes(10)) {
            initDataFromDb(registry)
            formsService.addChangeWithMetaListener { before, after ->
                var id = after?.entity?.id
                if (id.isNullOrBlank()) {
                    id = before?.entity?.id
                }
                if (!id.isNullOrBlank()) {
                    setRegistryValue(registry, id, createFormModelRegistryValue(after))
                }
            }
            formsService.addDeleteListener {
                registry.remove(it.entity.id)
            }
            typesRegistry.initializationPromise().get()
            typesRegistry.listenEventsWithMeta { _, before, after ->
                val idBefore = before?.entity?.formRef?.id ?: ""
                val idAfter = after?.entity?.formRef?.id ?: ""
                if (idBefore != idAfter || before?.entity?.name != after?.entity?.name) {
                    if (after != null && idAfter.startsWith(TYPE_AUTO_FORM_PREFIX)) {
                        setRegistryValue(registry, idAfter, createRegistryValue(after))
                    } else if (idBefore.isNotBlank()) {
                        registry.remove(idBefore)
                    }
                }
            }
            typesRegistry.getAllValues().values.filter {
                it.entity.formRef.id.startsWith(TYPE_AUTO_FORM_PREFIX)
            }.forEach { value ->
                setRegistryValue(registry, value.entity.formRef.id, createRegistryValue(value))
            }
        }

        val config = ExtStorageRecordsDaoConfig.create(ReadOnlyMapExtStorage(registry))
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
                setRegistryValue(registry, it.entity.id, createFormModelRegistryValue(it))
            }
            skipCount += forms.size
            forms = formsService.getAllFormsWithMeta(100, skipCount)
        }
    }

    private fun setRegistryValue(
        registry: ReplicatedMap<String, EntityWithMeta<FormRegistryValue>>,
        key: String,
        value: EntityWithMeta<FormRegistryValue>?
    ) {
        if (value != null) {
            registry[key] = value
        } else {
            registry.remove(key)
        }
    }

    private fun createRegistryValue(typeDef: EntityWithMeta<TypeDef>): EntityWithMeta<FormRegistryValue>? {
        val formId = typeDef.entity.formRef.getLocalId().substringAfter('$')
        val formDef = typeFormsProvider.getFormById(formId, false) ?: return null
        return EntityWithMeta(
            FormRegistryValue(
                formDef.entity.id,
                formDef.entity.title,
                formDef.entity.description,
                formDef.entity.formKey,
                formDef.entity.typeRef,
                formDef.entity.system
            ),
            formDef.meta
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
