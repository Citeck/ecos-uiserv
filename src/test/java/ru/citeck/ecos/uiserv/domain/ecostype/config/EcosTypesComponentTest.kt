package ru.citeck.ecos.uiserv.domain.ecostype.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.citeck.ecos.model.lib.workspace.IdInWs
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry

class EcosTypesComponentTest {

    private val wsA = "ws-a"
    private val wsAPrefix = "ws-a-sys-id:"
    private val wsB = "ws-b"
    private val wsBPrefix = "ws-b-sys-id:"

    private lateinit var component: EcosTypesComponent
    private lateinit var listener: (String, TypeDef?, TypeDef?) -> Unit

    @BeforeEach
    fun setup() {
        val typesRegistry = mock<EcosTypesRegistry>()
        component = EcosTypesComponent(typesRegistry, fakeWorkspaceService())
        component.init()

        val captor = argumentCaptor<(String, TypeDef?, TypeDef?) -> Unit>()
        verify(typesRegistry).listenEvents(captor.capture())
        listener = captor.firstValue
    }

    @Test
    fun `same-workspace ref is indexed`() {
        fireType(
            id = "${wsAPrefix}my-type",
            workspace = wsA,
            formRef = EntityRef.valueOf("uiserv/form@${wsAPrefix}my-form"),
            journalRef = EntityRef.valueOf("uiserv/journal@${wsAPrefix}my-journal"),
            boardRef = EntityRef.valueOf("uiserv/board@${wsAPrefix}my-board"),
        )

        assertThat(component.getTypeRefByForm(EntityRef.valueOf("uiserv/form@${wsAPrefix}my-form")).getLocalId())
            .isEqualTo("${wsAPrefix}my-type")
        assertThat(component.getTypeRefByJournal(EntityRef.valueOf("uiserv/journal@${wsAPrefix}my-journal")).getLocalId())
            .isEqualTo("${wsAPrefix}my-type")
        assertThat(component.getTypeRefByBoard(EntityRef.valueOf("uiserv/board@${wsAPrefix}my-board")).getLocalId())
            .isEqualTo("${wsAPrefix}my-type")
    }

    @Test
    fun `cross-workspace ref WS to default is not indexed`() {
        // type in ws-A pointing to a global form/journal/board via bare ref —
        // legitimate platform-wise but must not be findable as the type-of-F
        fireType(
            id = "${wsAPrefix}my-type",
            workspace = wsA,
            formRef = EntityRef.valueOf("uiserv/form@global-form"),
            journalRef = EntityRef.valueOf("uiserv/journal@global-journal"),
            boardRef = EntityRef.valueOf("uiserv/board@global-board"),
        )

        assertThat(component.getTypeRefByForm(EntityRef.valueOf("uiserv/form@global-form")))
            .isEqualTo(EntityRef.EMPTY)
        assertThat(component.getTypeRefByJournal(EntityRef.valueOf("uiserv/journal@global-journal")))
            .isEqualTo(EntityRef.EMPTY)
        assertThat(component.getTypeRefByBoard(EntityRef.valueOf("uiserv/board@global-board")))
            .isEqualTo(EntityRef.EMPTY)
    }

    @Test
    fun `cross-workspace ref default to WS is not indexed`() {
        // global type pointing to a WS-scoped form — forbidden by platform rules
        fireType(
            id = "global-type",
            workspace = "",
            formRef = EntityRef.valueOf("uiserv/form@${wsAPrefix}my-form"),
            journalRef = EntityRef.EMPTY,
            boardRef = EntityRef.EMPTY,
        )

        assertThat(component.getTypeRefByForm(EntityRef.valueOf("uiserv/form@${wsAPrefix}my-form")))
            .isEqualTo(EntityRef.EMPTY)
    }

    @Test
    fun `global type with global ref is indexed`() {
        fireType(
            id = "global-type",
            workspace = "",
            formRef = EntityRef.valueOf("uiserv/form@global-form"),
            journalRef = EntityRef.EMPTY,
            boardRef = EntityRef.EMPTY,
        )

        assertThat(component.getTypeRefByForm(EntityRef.valueOf("uiserv/form@global-form")).getLocalId())
            .isEqualTo("global-type")
    }

    @Test
    fun `types in different workspaces sharing a bare ref do not collide`() {
        fireType(
            id = "${wsAPrefix}my-type",
            workspace = wsA,
            formRef = EntityRef.valueOf("uiserv/form@global-form"),
            journalRef = EntityRef.EMPTY,
            boardRef = EntityRef.EMPTY,
        )
        fireType(
            id = "${wsBPrefix}my-type",
            workspace = wsB,
            formRef = EntityRef.valueOf("uiserv/form@global-form"),
            journalRef = EntityRef.EMPTY,
            boardRef = EntityRef.EMPTY,
        )

        // Neither cross-workspace association is indexed, so the lookup must not return either type
        assertThat(component.getTypeRefByForm(EntityRef.valueOf("uiserv/form@global-form")))
            .isEqualTo(EntityRef.EMPTY)
    }

    @Test
    fun `reverse lookup type-to-ref tracks the type's ref regardless of workspace alignment`() {
        // WS→default bare ref: NOT indexed in typeByRef, but reverse direction must still record it
        fireType(
            id = "${wsAPrefix}my-type",
            workspace = wsA,
            formRef = EntityRef.EMPTY,
            journalRef = EntityRef.valueOf("uiserv/journal@global-journal"),
            boardRef = EntityRef.EMPTY,
        )
        val typeRef = EntityRef.valueOf("emodel/type@${wsAPrefix}my-type")

        assertThat(component.getJournalRefByType(typeRef).toString())
            .isEqualTo("uiserv/journal@global-journal")
    }

    @Test
    fun `reverse lookup is cleared when type ref is cleared`() {
        val typeId = "${wsAPrefix}my-type"
        val typeRef = EntityRef.valueOf("emodel/type@$typeId")
        fireType(
            id = typeId,
            workspace = wsA,
            formRef = EntityRef.EMPTY,
            journalRef = EntityRef.valueOf("uiserv/journal@${wsAPrefix}j1"),
            boardRef = EntityRef.EMPTY,
        )
        assertThat(component.getJournalRefByType(typeRef).getLocalId()).isEqualTo("${wsAPrefix}j1")

        fireType(
            id = typeId,
            workspace = wsA,
            formRef = EntityRef.EMPTY,
            journalRef = EntityRef.EMPTY,
            boardRef = EntityRef.EMPTY,
        )
        assertThat(component.getJournalRefByType(typeRef)).isEqualTo(EntityRef.EMPTY)
    }

    @Test
    fun `ref change re-indexes under the new key and drops the old one`() {
        val typeId = "${wsAPrefix}my-type"
        fireType(
            id = typeId,
            workspace = wsA,
            formRef = EntityRef.valueOf("uiserv/form@${wsAPrefix}form-1"),
            journalRef = EntityRef.EMPTY,
            boardRef = EntityRef.EMPTY,
        )
        fireType(
            id = typeId,
            workspace = wsA,
            formRef = EntityRef.valueOf("uiserv/form@${wsAPrefix}form-2"),
            journalRef = EntityRef.EMPTY,
            boardRef = EntityRef.EMPTY,
        )

        assertThat(component.getTypeRefByForm(EntityRef.valueOf("uiserv/form@${wsAPrefix}form-1")))
            .isEqualTo(EntityRef.EMPTY)
        assertThat(component.getTypeRefByForm(EntityRef.valueOf("uiserv/form@${wsAPrefix}form-2")).getLocalId())
            .isEqualTo(typeId)
    }

    private fun fireType(
        id: String,
        workspace: String,
        formRef: EntityRef,
        journalRef: EntityRef,
        boardRef: EntityRef,
    ) {
        val typeDef = TypeDef.create()
            .withId(id)
            .withWorkspace(workspace)
            .withFormRef(formRef)
            .withJournalRef(journalRef)
            .withBoardRef(boardRef)
            .build()
        listener.invoke(typeDef.id, null, typeDef)
    }

    private fun fakeWorkspaceService(): WorkspaceService {
        val service = mock<WorkspaceService>()
        whenever(service.convertToIdInWs(any())).doAnswer { inv ->
            val strId = inv.getArgument<String>(0)
            when {
                strId.startsWith(wsAPrefix) -> IdInWs.create(wsA, strId.substring(wsAPrefix.length))
                strId.startsWith(wsBPrefix) -> IdInWs.create(wsB, strId.substring(wsBPrefix.length))
                else -> IdInWs.create("", strId)
            }
        }
        return service
    }
}
