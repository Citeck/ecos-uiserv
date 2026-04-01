package ru.citeck.ecos.uiserv.domain.journal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.model.lib.workspace.IdInWs
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalDef
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalRepository
import ru.citeck.ecos.uiserv.domain.journal.service.JournalService
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [Application::class])
class JournalGlobalWorkspaceTest {

    @Autowired
    lateinit var journalService: JournalService

    @Autowired
    lateinit var journalRepository: JournalRepository

    @AfterEach
    fun cleanup() {
        journalRepository.deleteAll()
    }

    @Test
    fun `journal saved with admin workspace should be found with empty workspace`() {
        val journal = JournalDef.create()
            .withId("admin-ws-journal")
            .withSourceId("test-source")
            .withWorkspace("admin\$workspace")
            .withTypeRef(EntityRef.valueOf("emodel/type@test-type"))
            .build()

        journalService.save(journal)

        val found = journalService.getJournalById(IdInWs.create("admin-ws-journal"))
        assertThat(found).isNotNull
        assertThat(found!!.journalDef.id).isEqualTo("admin-ws-journal")
    }

    @Test
    fun `journal saved with default workspace should be found with empty workspace`() {
        val journal = JournalDef.create()
            .withId("default-ws-journal")
            .withSourceId("test-source")
            .withWorkspace("default")
            .withTypeRef(EntityRef.valueOf("emodel/type@test-type"))
            .build()

        journalService.save(journal)

        val found = journalService.getJournalById(IdInWs.create("default-ws-journal"))
        assertThat(found).isNotNull
        assertThat(found!!.journalDef.id).isEqualTo("default-ws-journal")
    }

    @Test
    fun `journal saved with regular workspace should NOT be found with empty workspace`() {
        val journal = JournalDef.create()
            .withId("regular-ws-journal")
            .withSourceId("test-source")
            .withWorkspace("corpport-workspace")
            .withTypeRef(EntityRef.valueOf("emodel/type@test-type"))
            .build()

        journalService.save(journal)

        val foundEmpty = journalService.getJournalById(IdInWs.create("regular-ws-journal"))
        assertThat(foundEmpty).isNull()

        val foundWs = journalService.getJournalById(IdInWs.create("corpport-workspace", "regular-ws-journal"))
        assertThat(foundWs).isNotNull
        assertThat(foundWs!!.journalDef.id).isEqualTo("regular-ws-journal")
    }
}
