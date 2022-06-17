package ru.citeck.ecos.uiserv.domain.action.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDto
import ru.citeck.ecos.uiserv.domain.action.testutils.ActionsTestBase
import java.util.*

class ActionServiceTest : ActionsTestBase() {

    @Test
    fun test() {

        val action = ActionDto()
        action.predicate = Predicates.eq("testField", "testValue")

        action.name = MLText.EMPTY
            .withValue(Locale.ENGLISH, "En Name")
            .withValue(Locale.FRANCE, "Fr Value")

        action.id = "test-action"
        action.config = ObjectData.create("""{"aa":"bb"}""")
        action.icon = "icon-test"

        actionService.updateAction(action)

        val savedAction = actionService.getAction(action.id)

        assertEquals(action, savedAction)
    }

    @Test
    fun testCustomActionProvider() {

        val testProvActionDto = ActionDto()
        testProvActionDto.id = "test-prov-action"
        testProvActionDto.name = MLText(
            Locale.ENGLISH to "En",
            Locale.FRANCE to "Fr"
        )

        actionService.addActionProvider(object : ActionsProvider {
            override fun getAction(actionId: String): ActionDto? {
                if (actionId == testProvActionDto.id) {
                    return testProvActionDto
                }
                return null
            }
            override fun getType(): String = "testProv"
        })

        val action = actionService.getAction("testProv\$test-prov-action")!!
        assertThat(action).isEqualTo(testProvActionDto)

        val nullAction = actionService.getAction("unknown\$test-prov-action")
        assertThat(nullAction).isNull()

        val nullAction2 = actionService.getAction("test-prov-action")
        assertThat(nullAction2).isNull()

        val nullAction3 = actionService.getAction("testProv\$unknown")
        assertThat(nullAction3).isNull()
    }
}
