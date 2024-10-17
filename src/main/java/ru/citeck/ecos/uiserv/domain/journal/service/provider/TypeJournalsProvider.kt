package ru.citeck.ecos.uiserv.domain.journal.service.provider

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.entity.EntityWithMeta
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalColumnDef
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalDef
import ru.citeck.ecos.uiserv.domain.journal.service.JournalService
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry

@Component
class TypeJournalsProvider(
    val typesRegistry: EcosTypesRegistry,
    val journalService: JournalService
) : JournalsProvider {

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

        val columns = mutableListOf<JournalColumnDef>()

        val attributes = typeDef.model.attributes
        if (attributes.none { it.id == "name" }) {
            columns.add(
                JournalColumnDef.create()
                    .withId(ScalarType.DISP.mirrorAtt)
                    .withName(
                        MLText(
                            I18nContext.RUSSIAN to "Имя",
                            I18nContext.ENGLISH to "Name"
                        )
                    )
                    .build()
            )
        }
        columns.addAll(
            attributes.map {
                JournalColumnDef.create()
                    .withId(it.id)
                    .withType(it.type)
                    .withName(it.name)
                    .build()
            }
        )

        return JournalDef.create()
            .withName(typeDef.name)
            .withTypeRef(ModelUtils.getTypeRef(typeDef.id))
            .withColumns(columns)
            .build()
    }

    override fun getType(): String {
        return "type"
    }
}
