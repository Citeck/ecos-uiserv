package ru.citeck.ecos.uiserv.domain.form.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.model.lib.attributes.dto.AttributeDef
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.uiserv.domain.ecostype.dto.EcosTypeInfo
import ru.citeck.ecos.uiserv.domain.ecostype.service.EcosTypeService
import ru.citeck.ecos.uiserv.domain.form.service.FormDefUtils

@Component
class EcosResolvedFormRecordsDao(
    private val ecosFormRecordsDao: EcosFormRecordsDao,
    private val ecosTypeService: EcosTypeService
) : AbstractRecordsDao(), RecordAttsDao {

    companion object {
        const val ID = "rform"
    }

    override fun getRecordAtts(recordId: String): Any? {
        val form = ecosFormRecordsDao.getRecordAtts(recordId) ?: return null
        var typeRef = form.typeRef
        if (RecordRef.isEmpty(typeRef)) {
            typeRef = ecosTypeService.getTypeRefByForm(RecordRef.create("uiserv", "form", recordId))
        }
        val typeInfo = ecosTypeService.getTypeInfo(typeRef)
        return ResolvedFormRecord(form, typeInfo)
    }

    override fun getId(): String {
        return ID
    }

    class ResolvedFormRecord(
        @AttName("...") val form: EcosFormRecord,
        val typeInfo: EcosTypeInfo?
    ) {
        fun getDefinition(): ObjectData {
            val definition: ObjectData = form.definition
            val attributes = typeInfo?.model?.attributes?.associateBy { it.id } ?: emptyMap()
            if (attributes.isEmpty()) {
                return definition
            }
            val mappedDef = FormDefUtils.mapInputComponents(definition.getData().copy()) { component ->

                val componentAtt: String = FormDefUtils.getComponentAtt(component)
                val attDef: AttributeDef? = attributes[componentAtt]

                if (attDef != null && !MLText.isEmpty(attDef.name) && isLabelEmpty(component, componentAtt)) {
                    component.set("label", attDef.name)
                } else {
                    component
                }
            }
            return ObjectData.create(mappedDef)
        }

        private fun isLabelEmpty(component: DataValue, attribute: String): Boolean {
            val label = component.get("label")
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
