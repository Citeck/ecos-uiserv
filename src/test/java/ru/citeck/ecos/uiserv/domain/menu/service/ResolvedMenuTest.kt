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
                .build()
        )

        menu.withSubMenu(mapOf("left" to leftMenu))

        menuService.save(menu.build())

        val resolvedSubMenu = records.getAtt(
            EntityRef.valueOf("uiserv/rmenu@test-menu"),
            "subMenu.left?json"
        ).getAs(SubMenuDef::class.java)!!

        assertThat(resolvedSubMenu.items).hasSize(3)
        assertThat(resolvedSubMenu.items[0].id).isEqualTo(leftMenu.items[0].id)
        assertThat(resolvedSubMenu.items[0].type).isEqualTo(leftMenu.items[0].type)

        assertThat(
            resolvedSubMenu.items[0].config["variant"].getAs(CreateVariantDef::class.java)
        ).isEqualTo(testType.createVariants[0])

        assertThat(
            resolvedSubMenu.items[1].config["variant"].getAs(CreateVariantDef::class.java)
        ).isEqualTo(testType2.createVariants[0])
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
