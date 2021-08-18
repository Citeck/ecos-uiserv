package ru.citeck.ecos.uiserv.domain.form

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.model.lib.attributes.dto.AttributeDef
import ru.citeck.ecos.model.lib.type.dto.TypeModelDef
import ru.citeck.ecos.model.lib.type.service.utils.TypeUtils
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.uiserv.domain.ecostype.dto.EcosTypeInfo
import ru.citeck.ecos.uiserv.domain.form.api.records.EcosFormRecordsDao
import java.util.*

class FormResolvedRecordsTest : FormsTestBase() {

    @Test
    fun testLabelFromType() {

        val titleForSomeKey0 = MLText(
            Locale("ru") to "RU title someKey0",
            Locale.ENGLISH to "En title someKey0"
        );
        val titleForSomeKey2 = MLText(
            Locale("ru") to "RU title someKey2",
            Locale.ENGLISH to "En title someKey2"
        );

        types[TypeUtils.getTypeRef("test-type")] = EcosTypeInfo(
            "test-type",
            MLText.EMPTY,
            RecordRef.EMPTY,
            RecordRef.EMPTY,
            RecordRef.EMPTY,
            RecordRef.EMPTY,
            "",
            emptyList(),
            "",
            emptyList(),
            TypeModelDef.create {
                withAttributes(listOf(
                    AttributeDef.create {
                        withId("someKey0")
                        withName(titleForSomeKey0)
                    },
                    AttributeDef.create {
                        withId("someKey2")
                        withName(titleForSomeKey2)
                    }
                ))
            },
            emptyList()
        )

        val formRef = RecordRef.create("uiserv", "form", "test-form")
        typeByForm[formRef] = TypeUtils.getTypeRef("test-type")

        recordsService.create(EcosFormRecordsDao.ID, mapOf(
            "moduleId" to formRef.id,
            "definition" to mapOf(
                "components" to listOf(
                    mapOf(
                        "id" to "id0",
                        "type" to "text",
                        "key" to "someKey0",
                        "input" to true
                    ),
                    mapOf(
                        "id" to "id1",
                        "key" to "someKey1",
                        "type" to "number",
                        "input" to true,
                        "properties" to mapOf(
                            "attribute" to "someKey2"
                        )
                    )
                )
            )
        ))

        val components = recordsService.getAtt("rform@${formRef.id}", "definition.components[]?json")

        val id0Comp = components.first { it.get("id").asText() == "id0" }
        assertThat(id0Comp.get("label").getAs(MLText::class.java)).isEqualTo(titleForSomeKey0)

        val id1Comp = components.first { it.get("id").asText() == "id1" }
        assertThat(id1Comp.get("label").getAs(MLText::class.java)).isEqualTo(titleForSomeKey2)
    }

    @Test
    fun testIncludeForm() {

        val componentsToImport = listOf(
            mapOf(
                "id" to "id0",
                "type" to "text",
                "input" to true
            ),
            mapOf(
                "id" to "id1",
                "type" to "number",
                "input" to true
            )
        )

        recordsService.create(EcosFormRecordsDao.ID, mapOf(
            "moduleId" to "test-form-to-import",
            "definition" to mapOf(
                "components" to componentsToImport
            )
        ))

        val componentsOfBaseForm = listOf(
            mapOf(
                "id" to "id111",
                "type" to "text",
                "input" to false
            )
        )

        recordsService.create(EcosFormRecordsDao.ID, mapOf(
            "moduleId" to "test-form",
            "definition" to mapOf(
                "components" to listOf(
                    *componentsOfBaseForm.toTypedArray(),
                    mapOf(
                        "id" to "id222",
                        "type" to "includeForm",
                        "formRef" to "uiserv/form@test-form-to-import"
                    )
                )
            )
        ))

        val actual = recordsService.getAtt("rform@test-form", "definition.components[]?json")

        assertThat(actual).isEqualTo(DataValue.create(listOf(
            *componentsOfBaseForm.toTypedArray(),
            *componentsToImport.toTypedArray()
        )))
    }
}
