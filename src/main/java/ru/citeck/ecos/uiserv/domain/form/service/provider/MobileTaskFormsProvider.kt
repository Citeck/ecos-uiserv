package ru.citeck.ecos.uiserv.domain.form.service.provider

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.data.entity.EntityWithMeta
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.uiserv.domain.form.builder.EcosFormBuilder
import ru.citeck.ecos.uiserv.domain.form.builder.EcosFormBuilderFactory
import ru.citeck.ecos.uiserv.domain.form.builder.EcosFormInputType
import ru.citeck.ecos.uiserv.domain.form.builder.EcosFormWidth
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

        val formBuilder = formBuilderFactory.createBuilder()
        buildDocumentFields(formBuilder, ecosFormService.getFormById(taskAtts.documentFormId).orElse(null))
        buildTaskOutcomeFields(formBuilder, taskAtts.possibleOutcomes)

        formBuilder.withWidth(EcosFormWidth.SMALL)

        return EntityWithMeta(formBuilder.build())
    }

    private fun buildTaskOutcomeFields(builder: EcosFormBuilder, outcomes: List<OutcomeAtts>) {
        outcomes.forEach {
            builder.addButton()
                .setKey("outcome_" + it.id)
                .setName(it.name)
                .build()
        }
    }

    private fun buildDocumentFields(builder: EcosFormBuilder, documentForm: EcosFormDef?) {

        if (documentForm == null) {
            builder.addInput(AttributeType.TEXT, ObjectData.create()).setName(
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
                    .setData(component)
                    .setKey("_ECM_" + component["key"].asText())
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
