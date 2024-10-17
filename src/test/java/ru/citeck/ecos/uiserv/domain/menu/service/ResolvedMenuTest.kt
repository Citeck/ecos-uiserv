package ru.citeck.ecos.uiserv.domain.menu.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.model.lib.type.dto.CreateVariantDef
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDto
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuItemDef
import ru.citeck.ecos.uiserv.domain.menu.dto.SubMenuDef
import ru.citeck.ecos.uiserv.domain.menu.service.testutils.MenuTestBase
import ru.citeck.ecos.webapp.api.entity.EntityRef

class ResolvedMenuTest : MenuTestBase() {

    @Test
    fun test() {

        val testType = TypeDto(
            "test-type",
            listOf(
                CreateVariantDef.create()
                    .withId("DEFAULT")
                    .withFormRef(EntityRef.valueOf("test-form"))
                    .withName(MLText("test-name"))
                    .withTypeRef(EntityRef.valueOf("emodel/type@test-type"))
                    .build(),
                CreateVariantDef.create()
                    .withId("DEFAULT")
                    .withFormRef(EntityRef.valueOf("custom-test-form"))
                    .withName(MLText("custom-test-name"))
                    .withTypeRef(EntityRef.valueOf("emodel/type@custom-test-type"))
                    .build()
            )
        )
        registerType(testType)

        val testType2 = TypeDto(
            "custom-test-type",
            listOf(testType.createVariants[1])
        )
        registerType(testType2)

        val menuToInclude = createMenuToInclude("include-menu")
        menuService.save(menuToInclude)

        val menu = MenuDto.create()
        menu.withId("test-menu")

        val leftMenu = SubMenuDef()
        leftMenu.items = listOf(
            MenuItemDef.create()
                .withId("1")
                .withType("LINK-CREATE-CASE")
                .withConfig(
                    ObjectData.create(
                        CreateCaseConfig(
                            typeRef = EntityRef.valueOf("test-type-ref"),
                            variantTypeRef = EntityRef.valueOf("emodel/type@test-type"),
                            variantId = "DEFAULT"
                        )
                    )
                )
                .build(),
            MenuItemDef.create()
                .withId("2")
                .withType("LINK-CREATE-CASE")
                .withConfig(
                    ObjectData.create(
                        CreateCaseConfig(
                            typeRef = EntityRef.valueOf("test-type-ref"),
                            variantTypeRef = EntityRef.valueOf("emodel/type@custom-test-type"),
                            variantId = "DEFAULT"
                        )
                    )
                )
                .build(),
            MenuItemDef.create()
                .withId("3")
                .withType("LINK-CREATE-CASE")
                .withConfig(
                    ObjectData.create(
                        CreateCaseConfig(
                            typeRef = EntityRef.valueOf("test-type-ref"),
                            variantTypeRef = EntityRef.valueOf("emodel/type@unknown-type"),
                            variantId = "DEFAULT"
                        )
                    )
                )
                .build(),
            MenuItemDef.create()
                .withId("4")
                .withType("LINK-CREATE-CASE")
                .withHidden(true)
                .build(),
            MenuItemDef.create()
                .withId("5")
                .withType("INCLUDE_MENU")
                .withConfig(ObjectData.create().set("menuRef", "uiserv/menu@${menuToInclude.id}"))
                .build()
        )

        val createMenu = SubMenuDef()
        createMenu.items = listOf(
            MenuItemDef.create()
                .withId("create-1")
                .withType("INCLUDE_MENU")
                .withConfig(ObjectData.create().set("menuRef", "uiserv/menu@${menuToInclude.id}"))
                .build()
        )

        menu.withSubMenu(
            mapOf(
                "left" to leftMenu,
                "create" to createMenu
            )
        )

        menuService.save(menu.build())

        val resolvedLeftSubMenu = records.getAtt(
            EntityRef.valueOf("uiserv/rmenu@test-menu"),
            "subMenu.left?json"
        ).getAs(SubMenuDef::class.java)!!

        val resolvedCreateSubMenu = records.getAtt(
            EntityRef.valueOf("uiserv/rmenu@test-menu"),
            "subMenu.create?json"
        ).getAs(SubMenuDef::class.java)!!

        assertThat(resolvedLeftSubMenu.items).hasSize(5)
        assertThat(resolvedLeftSubMenu.items[0].id).isEqualTo(leftMenu.items[0].id)
        assertThat(resolvedLeftSubMenu.items[0].type).isEqualTo(leftMenu.items[0].type)

        assertThat(
            resolvedLeftSubMenu.items[0].config["variant"].getAs(CreateVariantDef::class.java)
        ).isEqualTo(testType.createVariants[0])

        assertThat(
            resolvedLeftSubMenu.items[1].config["variant"].getAs(CreateVariantDef::class.java)
        ).isEqualTo(testType2.createVariants[0])

        fun assertItems(itemsToCheck: List<MenuItemDef>, expected: List<MenuItemDef>, unexpected: List<MenuItemDef>) {
            expected.forEach { expectedItem ->
                assertThat(itemsToCheck.find { it.id == expectedItem.id })
                    .describedAs("item-id: " + expectedItem.id)
                    .isEqualTo(expectedItem)
            }
            unexpected.forEach { unexpectedItem ->
                assertThat(itemsToCheck.find { it.id == unexpectedItem.id })
                    .describedAs("item-id: " + unexpectedItem.id)
                    .isNull()
            }
        }
        assertItems(
            itemsToCheck = resolvedLeftSubMenu.items,
            expected = menuToInclude.subMenu["left"]!!.items,
            unexpected = menuToInclude.subMenu["create"]!!.items
        )
        assertItems(
            itemsToCheck = resolvedCreateSubMenu.items,
            expected = menuToInclude.subMenu["create"]!!.items,
            unexpected = menuToInclude.subMenu["left"]!!.items
        )
    }

    private fun createMenuToInclude(id: String): MenuDto {

        val leftMenu = SubMenuDef()
        leftMenu.items = listOf(
            MenuItemDef.create()
                .withId("$id-left-1")
                .withType("LINK")
                .build(),
            MenuItemDef.create()
                .withId("$id-left-2")
                .withType("LINK")
                .build()
        )
        val createMenu = SubMenuDef()
        createMenu.items = listOf(
            MenuItemDef.create()
                .withId("$id-create-1")
                .withType("LINK")
                .build(),
            MenuItemDef.create()
                .withId("$id-create-2")
                .withType("LINK")
                .build()
        )

        return MenuDto.create()
            .withId(id)
            .withSubMenu(
                mapOf(
                    "left" to leftMenu,
                    "create" to createMenu
                )
            ).build()
    }

    class CreateCaseConfig(
        val typeRef: EntityRef,
        val variantTypeRef: EntityRef,
        val variantId: String
    )

    class TypeDto(
        val id: String,
        val createVariants: List<CreateVariantDef>
    )
}
