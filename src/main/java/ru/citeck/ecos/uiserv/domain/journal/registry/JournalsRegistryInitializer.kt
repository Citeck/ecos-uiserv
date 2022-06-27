package ru.citeck.ecos.uiserv.domain.journal.registry

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.entity.EntityMeta
import ru.citeck.ecos.commons.data.entity.EntityWithMeta
import ru.citeck.ecos.commons.promise.Promises
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.model.lib.type.service.utils.TypeUtils
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalWithMeta
import ru.citeck.ecos.uiserv.domain.journal.service.JournalService
import ru.citeck.ecos.webapp.api.lock.EcosLockService
import ru.citeck.ecos.webapp.api.promise.Promise
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import ru.citeck.ecos.webapp.lib.registry.MutableEcosRegistry
import ru.citeck.ecos.webapp.lib.registry.init.EcosRegistryInitializer
import java.time.Duration

@Component
class JournalsRegistryInitializer(
    val appLockService: EcosLockService,
    val journalsService: JournalService,
    val typesRegistry: EcosTypesRegistry
) : EcosRegistryInitializer<JournalRegistryValue> {

    companion object {
        private val NAME_PREFIXES = mapOf(
            I18nContext.RUSSIAN to "Журнал для ",
            I18nContext.ENGLISH to "Journal for ",
        )
        private const val TYPE_AUTO_JOURNAL_PREFIX = "type$"
        private val log = KotlinLogging.logger {}
    }

    override fun init(
        registry: MutableEcosRegistry<JournalRegistryValue>,
        values: Map<String, EntityWithMeta<JournalRegistryValue>>
    ): Promise<*> {
        appLockService.doInSync("journals-registry-initializer", Duration.ofMinutes(10)) {
            val journals = journalsService.getAll(10000000, 0)
            journals.forEach {
                registry.setValue(it.journalDef.id, createRegistryValue(it))
            }
            journalsService.onJournalWithMetaChanged { before, after ->
                val id = after.journalDef.id.ifBlank { before.journalDef.id }
                if (id.isNotBlank()) {
                    registry.setValue(id, createRegistryValue(after))
                }
            }
            typesRegistry.initializationPromise().get()
            typesRegistry.listenEventsWithMeta { _, before, after ->
                if (before != null && after == null && before.entity.journalRef.id.startsWith(TYPE_AUTO_JOURNAL_PREFIX)) {
                    registry.setValue(before.entity.journalRef.id, null)
                } else if (after != null && after.entity.journalRef.id.startsWith(TYPE_AUTO_JOURNAL_PREFIX)) {
                    registry.setValue(after.entity.journalRef.id, createRegistryValue(after))
                }
            }
            typesRegistry.getAllValues().values.filter {
                it.entity.journalRef.id.startsWith(TYPE_AUTO_JOURNAL_PREFIX)
            }.forEach { value ->
                registry.setValue(value.entity.journalRef.id, createRegistryValue(value))
            }
        }
        return Promises.resolve(true)
    }

    private fun createRegistryValue(typeDef: EntityWithMeta<TypeDef>): EntityWithMeta<JournalRegistryValue> {
        val name = MLText(
            typeDef.entity.name.getValues().entries.associate {
                it.key to ((NAME_PREFIXES[it.key] ?: "") + it.value)
            }
        )
        return EntityWithMeta(
            JournalRegistryValue(
                typeDef.entity.journalRef.id,
                name,
                typeDef.entity.sourceId,
                TypeUtils.getTypeRef(typeDef.entity.id),
                typeDef.entity.system
            ),
            typeDef.meta
        )
    }

    private fun createRegistryValue(journal: JournalWithMeta?): EntityWithMeta<JournalRegistryValue>? {
        journal ?: return null
        return EntityWithMeta(
            JournalRegistryValue(
                journal.journalDef.id,
                journal.journalDef.name,
                journal.journalDef.sourceId,
                journal.journalDef.typeRef,
                journal.journalDef.system
            ),
            EntityMeta(
                journal.created,
                journal.creator,
                journal.modified,
                journal.modifier
            )
        )
    }

    override fun getKey(): String {
        return "journals-service"
    }
}
