package ru.citeck.ecos.uiserv.domain.form.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.model.lib.attributes.dto.AttributeDef
import ru.citeck.ecos.model.lib.type.service.utils.TypeUtils
import ru.citeck.ecos.records2.RecordRef
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
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.entity.ifEmpty
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef

@Component
class EcosResolvedFormRecordsDao(
    private val ecosFormRecordsDao: EcosFormRecordsDao,
    private val ecosTypeService: EcosTypeService,
    private val formService: EcosFormService
) : AbstractRecordsDao(), RecordAttsDao, RecordsQueryDao {

    companion object {
        const val ID = "rform"
    }

    override fun queryRecords(recsQuery: RecordsQuery): Any? {
        val formsResult = ecosFormRecordsDao.queryRecords(recsQuery) ?: return null
        val queryResult = RecsQueryRes<ResolvedFormRecord>()
        queryResult.setHasMore(formsResult.getHasMore())
        queryResult.setTotalCount(formsResult.getTotalCount())
        queryResult.setRecords(formsResult.getRecords().map { mapToResolvedRecord(it) })
        return queryResult
    }

    override fun getRecordAtts(recordId: String): Any? {
        val form = ecosFormRecordsDao.getRecordAtts(recordId) ?: return null
        return mapToResolvedRecord(form)
    }

    private fun mapToResolvedRecord(form: EcosFormRecord): ResolvedFormRecord {
        var typeRef = form.typeRef
        if (RecordRef.isEmpty(typeRef)) {
            typeRef = ecosTypeService.getTypeRefByForm(RecordRef.create("uiserv", "form", form.id))
        }
        val typeInfo = ecosTypeService.getTypeInfo(typeRef)
        return ResolvedFormRecord(form, typeInfo, formService)
    }

    override fun getId(): String {
        return ID
    }

    class ResolvedFormRecord(
        @AttName("...") val form: EcosFormRecord,
        val typeInfo: TypeDef?,
        val formService: EcosFormService
    ) {

        fun getTypeRef(): EntityRef {
            return form.typeRef.ifEmpty { TypeUtils.getTypeRef(typeInfo?.id ?: "base") }
        }

        fun getDefinition(): ObjectData {
            val definition: ObjectData = form.definition
            val attributes = typeInfo?.model?.getAllAttributes()?.associateBy { it.id } ?: emptyMap()
            val mappedDef = FormDefUtils.mapComponents(definition.getData().copy(), { true }) {
                mapComponent(it, attributes)
            }
            return ObjectData.create(mappedDef)
        }

        private fun mapComponent(component: DataValue, attributes: Map<String, AttributeDef>): DataValue? {

            if (component["type"].asText() == "includeForm") {
                val formRef = RecordRef.valueOf(component["formRef"].asText())
                if (formRef.id.isBlank()) {
                    return null
                }
                if (component["conditionalForm"].asBoolean(false)) {
                    return component
                }
                val formDef = formService.getFormById(formRef.id).orElse(null)
                if (formDef != null && formDef.definition != null) {
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
