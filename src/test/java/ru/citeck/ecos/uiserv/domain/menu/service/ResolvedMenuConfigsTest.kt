package ru.citeck.ecos.uiserv.domain.menu.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.io.file.EcosFile
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDto
import ru.citeck.ecos.uiserv.domain.menu.dto.SubMenuDef
import ru.citeck.ecos.uiserv.domain.menu.service.testutils.MenuTestBase
import ru.citeck.ecos.webapp.api.entity.EntityRef

class ResolvedMenuConfigsTest : MenuTestBase() {

    companion object {
        private val MENU_RESOLVED_REF = EntityRef.valueOf("uiserv/rmenu@test-menu")

        private val log = KotlinLogging.logger {}
    }

    private lateinit var menuDto: MenuDto

    private fun init() {
        loadAndRegisterAllTypes("types")
        menuDto = MenuDto.create().withId(MENU_RESOLVED_REF.getLocalId()).build()
    }

    @Test
    fun test() {

        init()

        // for debug purposes
        // testImpl(findFiles("**/target-section-in-inner/input.json")[0].getParent()!!)

        findFiles("**/input.json").forEach {
            testImpl(it.getParent()!!)
        }
    }

    private fun testImpl(testRoot: EcosFile) {

        var testName = testRoot.getName()
        var parent = testRoot.getParent()
        while (parent != null && parent.getName() != "tests") {
            testName = parent.getName() + "/" + testName
            parent = parent.getParent()
        }

        log.info { "===== Test: '$testName' =====" }

        val inSubMenu = Json.mapper.read(testRoot.getFile("input.json")!!, DataValue::class.java)!!
        val outSubMenu = Json.mapper.read(
            testRoot.getFile("output.json") ?: error("Output file doesn't found"),
            DataValue::class.java
        ) ?: error("Output file reading failed")

        menuDto.copy().withSubMenu(inSubMenu.asMap(String::class.java, SubMenuDef::class.java)).build()
        menuService.save(menuDto)

        val resolvedMenu = records.getAtt(MENU_RESOLVED_REF, "subMenu?json")

        try {
            compare("", outSubMenu, resolvedMenu)
        } catch (e: Throwable) {
            log.info { "Resolved menu: \n${Json.mapper.toPrettyString(resolvedMenu)}" }
            throw e
        }
    }

    private fun compare(path: String, expected: DataValue, actual: DataValue) {
        when {
            expected.isArray() -> {
                assertThat(expected.size())
                    .describedAs("Array size doesn't match. Path: '$path'")
                    .isEqualTo(actual.size())

                for (item in expected.withIndex()) {
                    compare("$path[${item.index}]", item.value, actual.get(item.index))
                }
            }
            expected.isObject() -> {
                expected.forEach { k, v ->
                    compare("$path/$k", v, actual[k])
                }
            }
            else -> {
                assertThat(actual).describedAs("Path: '$path'").isEqualTo(expected)
            }
        }
    }
}
