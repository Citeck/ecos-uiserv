package ru.citeck.ecos.uiserv.domain.action.api.records

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.uiserv.domain.action.testutils.ActionsTestBase
import ru.citeck.ecos.webapp.api.entity.EntityRef

class RecordsDaoTest : ActionsTestBase() {

    @Test
    fun test() {

        val actionAtts = ObjectData.create(
            """
            {
                "moduleId": "test-action",
                "name": {
                    "en": "En",
                    "ru": "Ru"
                },
                "predicate": {
                    "t": "eq",
                    "att": "testAtt",
                    "val": "testVal"
                }
            }
            """.trimIndent()
        )

        assertThat(actionDao.getCount()).isEqualTo(0)

        val newActionRef = records.mutate(
            EntityRef.create("uiserv", "action", ""),
            actionAtts
        )

        assertThat(EntityRef.isNotEmpty(newActionRef)).isTrue()
        assertThat(actionDao.getCount()).isEqualTo(1)

        assertThat(records.getAtt(newActionRef, "name.ru").asText()).isEqualTo("Ru")
        assertThat(records.getAtt(newActionRef, "name.en").asText()).isEqualTo("En")

        val query = RecordsQuery.create()
            .withSourceId(ActionRecords.ID)
            .withQuery(Predicates.eq("moduleId", "test-action"))
            .withLanguage(PredicateService.LANGUAGE_PREDICATE)
            .build()

        val queryRes = records.query(query)
        assertThat(queryRes.getRecords()).hasSize(1)
        assertThat(queryRes.getRecords()[0].getLocalId()).isEqualTo("test-action")

        val queryWithEmptyRes = query.copy()
            .withQuery(Predicates.eq("moduleId", "unknown"))
            .build()
        val queryEmptyRes = records.query(queryWithEmptyRes)

        assertThat(queryEmptyRes.getRecords()).isEmpty()
    }
}
