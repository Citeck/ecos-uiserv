package ru.citeck.ecos.uiserv.domain.form.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.model.lib.attributes.dto.AttributeDef
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.uiserv.domain.ecostype.service.EcosTypeAttsUtils
import ru.citeck.ecos.uiserv.domain.ecostype.service.EcosTypeService
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService
import ru.citeck.ecos.uiserv.domain.form.service.FormDefUtils
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.entity.ifEmpty
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef

@Component
class EcosResolvedFormRecordsDao(
    private val ecosFormRecordsDao: EcosFormRecordsDao,
    private val ecosTypeService: EcosTypeService,
    private val formService: EcosFormService,
    private val workspaceService: WorkspaceService
) : AbstractRecordsDao(), RecordAttsDao, RecordsQueryDao {

    companion object {
        const val ID = "rform"
    }

    override fun queryRecords(recsQuery: RecordsQuery): Any? {
        val formsResult = recordsService.query(
            recsQuery.copy()
                .withSourceId(EcosFormRecordsDao.ID)
                .build()
        )
        val queryResult = RecsQueryRes<EntityRef>()
        queryResult.setHasMore(formsResult.getHasMore())
        queryResult.setTotalCount(formsResult.getTotalCount())
        queryResult.setRecords(
            formsResult.getRecords().map {
                EntityRef.create(AppName.UISERV, ID, it.getLocalId())
            }
        )
        return queryResult
    }

    override fun getRecordAtts(recordId: String): Any? {
        val form = ecosFormRecordsDao.getRecordAtts(recordId) ?: return null
        return mapToResolvedRecord(form)
    }

    private fun mapToResolvedRecord(form: EcosFormRecord): ResolvedFormRecord {
        var typeRef = form.def.typeRef
        if (EntityRef.isEmpty(typeRef)) {
            typeRef = ecosTypeService.getTypeRefByForm(EntityRef.create("uiserv", "form", form.def.id))
        }
        val typeInfo = ecosTypeService.getTypeInfo(typeRef)
        return ResolvedFormRecord(form, typeInfo, formService, workspaceService)
    }

    override fun getId(): String {
        return ID
    }

    class ResolvedFormRecord(
        @AttName("...") val form: EcosFormRecord,
        val typeInfo: TypeDef?,
        val formService: EcosFormService,
        val workspaceService: WorkspaceService
    ) {

        fun getTypeRef(): EntityRef {
            return form.def.typeRef.ifEmpty { ModelUtils.getTypeRef(typeInfo?.id ?: "base") }
        }

        fun getDefinition(): ObjectData {
            val definition: ObjectData = form.def.definition
            val attributes = typeInfo?.model?.getAllAttributes()?.associateBy { it.id } ?: emptyMap()
            val mappedDef = FormDefUtils.mapComponents(definition.getData().copy(), { true }) {
                mapComponent(it, attributes)
            }
            return ObjectData.create(mappedDef)
        }

        private fun mapComponent(component: DataValue, attributes: Map<String, AttributeDef>): DataValue? {

            if (component["type"].asText() == "includeForm") {
                val formRef = EntityRef.valueOf(component["formRef"].asText())
                if (formRef.getLocalId().isBlank()) {
                    return null
                }
                if (component["conditionalForm"].asBoolean(false)) {
                    return component
                }
                val idInWs = workspaceService.convertToIdInWs(formRef.getLocalId())
                val formDef = formService.getFormById(idInWs).orElse(null)
                if (formDef?.definition != null) {
                    val components = formDef.definition["components"]
                    if (components.isArray()) {
                        return components
                    }
                }
                return null
            }

            if (!component["input"].asBoolean(false) || attributes.isEmpty()) {
                return component
            }

            val componentAtt: String = FormDefUtils.getComponentAtt(component)
            val attDef: AttributeDef? = attributes[componentAtt] ?: EcosTypeAttsUtils.STD_ATTS[componentAtt]

            return if (attDef != null && !MLText.isEmpty(attDef.name) && isLabelEmpty(component, componentAtt)) {
                component.set("label", attDef.name)
            } else {
                component
            }
        }

        private fun isLabelEmpty(component: DataValue, attribute: String): Boolean {
            val label = component["label"]
            if (label.isNull()) {
                return true
            }
            if (label.isTextual()) {
                return isStrLabelEmpty(label.asText(), attribute)
            } else if (label.isObject()) {
                var notEmptyAttsCount = 0
                label.forEach { _, v ->
                    if (v.isTextual() && !isStrLabelEmpty(v.asText(), attribute)) {
                        notEmptyAttsCount++
                    }
                }
                return notEmptyAttsCount == 0
            }
            return true
        }

        private fun isStrLabelEmpty(label: String, attribute: String): Boolean {
            return label.isBlank() || label == attribute
        }
    }
}
