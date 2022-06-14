package ru.citeck.ecos.uiserv.domain.journal.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.uiserv.domain.action.dto.ExecForQueryConfig
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalActionDef
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalDef

@SpringBootTest
@ExtendWith(SpringExtension::class)
class JournalActionsTest {

    @Autowired
    lateinit var journalsService: JournalService
    @Autowired
    lateinit var recordsService: RecordsService

    @Test
    fun test() {

        journalsService.save(JournalDef.create {
            withId("some-journal")
            withActionsDef(listOf(
                JournalActionDef.create()
                .withId("some-action")
                .withType("some-type")
                .withExecForQueryConfig(ExecForQueryConfig(true))
                .build())
            )
        })

        val actionRefs = recordsService.getAtt(
            RecordRef.create("rjournal", "some-journal"),
            "actionsDef[].id"
        ).asStrList()

        assertThat(actionRefs).containsExactly("journal\$some-journal\$some-action")
        val actionRef = RecordRef.create("action", actionRefs[0])
        val execForQueryConfig = recordsService.getAtt(actionRef, "execForQueryConfig?json")

        assertTrue(execForQueryConfig.get("execAsForRecords").asBoolean())
    }

}
