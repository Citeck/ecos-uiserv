package ru.citeck.ecos.uiserv.domain.journal.service.provider

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.entity.EntityWithMeta
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.model.lib.type.service.utils.TypeUtils
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalColumnDef
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalDef
import ru.citeck.ecos.uiserv.domain.journal.service.JournalService
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import javax.annotation.PostConstruct

@Component
class TypeJournalsProvider(
    val typesRegistry: EcosTypesRegistry,
    val journalService: JournalService
) : JournalsProvider {

    companion object {
        private val NAME_PREFIXES = mapOf(
            I18nContext.RUSSIAN to "Журнал по умолчанию для ",
            I18nContext.ENGLISH to "Default journal for ",
        )
    }

    @PostConstruct
    fun init() {
        journalService.registerProvider(this)
    }

    override fun getJournalById(id: String): EntityWithMeta<JournalDef>? {
        val typeDef = typesRegistry.getValueWithMeta(id) ?: return null
        return EntityWithMeta(
            createJournalDef(typeDef.entity),
            typeDef.meta
        )
    }

    private fun createJournalDef(typeDef: TypeDef): JournalDef {

        val name = MLText(
            typeDef.name.getValues().entries.associate {
                it.key to ((NAME_PREFIXES[it.key] ?: "") + it.value)
            }
        )

        return JournalDef.create {
            withName(name)
            withTypeRef(TypeUtils.getTypeRef(typeDef.id))
            withColumns(
                typeDef.model.attributes.map {
                    JournalColumnDef.create()
                        .withId(it.id)
                        .withType(it.type)
                        .withName(name)
                        .build()
                }
            )
        }
    }

    override fun getType(): String {
        return "type"
    }
}
