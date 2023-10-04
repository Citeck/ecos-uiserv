package ru.citeck.ecos.uiserv.domain.form.service.provider

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.data.entity.EntityWithMeta
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.uiserv.domain.form.builder.*
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService
import ru.citeck.ecos.uiserv.domain.form.service.FormDefUtils
import ru.citeck.ecos.webapp.api.entity.EntityRef
import javax.annotation.PostConstruct

@Component
class MobileTaskFormsProvider(
    val recordsService: RecordsService,
    val ecosFormService: EcosFormService,
    val formBuilderFactory: EcosFormBuilderFactory
) : EcosFormsProvider {

    @PostConstruct
    fun init() {
        ecosFormService.register(this)
    }

    override fun getFormById(id: String): EntityWithMeta<EcosFormDef>? {

        val taskRef = EntityRef.valueOf(id)
        val taskAtts = recordsService.getAtts(taskRef, TaskAtts::class.java)

        val form = formBuilderFactory.createBuilder()
            .withComponents { formComponents ->
                formComponents.addPanel()
                    .withKey("body_panel")
                    .withComponents { bodyComponents ->
                        buildDocumentFields(
                            bodyComponents,
                            ecosFormService.getFormById(taskAtts.documentFormId).orElse(null)
                        )
                    }.build()
                formComponents.addPanel()
                    .withKey("footer_panel")
                    .withComponents { footerComponents ->
                        buildTaskOutcomeFields(footerComponents, taskAtts.possibleOutcomes)
                    }.build()
        }.withWidth(EcosFormWidth.SMALL).build()

        return EntityWithMeta(form)
    }

    private fun buildTaskOutcomeFields(builder: EcosFormComponentsBuilder, outcomes: List<OutcomeAtts>) {
        outcomes.forEach {
            builder.addButton()
                .withKey("outcome_" + it.id)
                .withName(it.name)
                .build()
        }
    }

    private fun buildDocumentFields(builder: EcosFormComponentsBuilder, documentForm: EcosFormDef?) {

        if (documentForm == null) {
            builder.addInput(AttributeType.TEXT, ObjectData.create()).withName(
                MLText(
                    I18nContext.RUSSIAN to "Имя документа",
                    I18nContext.ENGLISH to "Document name"
                )
            ).build()
            return
        }

        FormDefUtils.mapInputComponents(documentForm.definition.getData()) { component ->
            val type = EcosFormInputType.getByTypeIdOrNull(component["type"].asText())
            if (type != null) {
                builder.addInput(type, ObjectData.create())
                    .withData(component)
                    .withKey("_ECM_" + component["key"].asText())
                    .build()
            }
            component
        }
    }

    override fun getType(): String {
        return "mobile-task"
    }

    private class TaskAtts(
        @AttName("documentRef._type.formRef?localId!")
        val documentFormId: String,
        val possibleOutcomes: List<OutcomeAtts>
    )

    private class OutcomeAtts(
        val config: ObjectData,
        val id: String,
        val name: MLText
    )
}
