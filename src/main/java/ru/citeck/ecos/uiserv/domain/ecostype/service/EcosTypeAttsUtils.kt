package ru.citeck.ecos.uiserv.domain.ecostype.service

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.model.lib.attributes.dto.AttributeDef
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType

object EcosTypeAttsUtils {

    val STD_ATTS = listOf(
        AttributeDef.create()
            .withId("_disp")
            .withName(
                MLText(
                    I18nContext.RUSSIAN to "Имя",
                    I18nContext.ENGLISH to "Name"
                )
            )
            .withType(AttributeType.TEXT)
            .build(),
        AttributeDef.create()
            .withId("_name")
            .withName(
                MLText(
                    I18nContext.RUSSIAN to "Имя",
                    I18nContext.ENGLISH to "Name"
                )
            )
            .withType(AttributeType.MLTEXT)
            .build(),
        AttributeDef.create()
            .withId("_status")
            .withName(
                MLText(
                    I18nContext.RUSSIAN to "Статус",
                    I18nContext.ENGLISH to "Status"
                )
            )
            .withType(AttributeType.TEXT)
            .build(),
        AttributeDef.create()
            .withId("_stage")
            .withName(
                MLText(
                    I18nContext.RUSSIAN to "Этап",
                    I18nContext.ENGLISH to "Stage"
                )
            )
            .withType(AttributeType.TEXT)
            .build(),
        AttributeDef.create()
            .withId("_docNum")
            .withName(
                MLText(
                    I18nContext.RUSSIAN to "Номер документа",
                    I18nContext.ENGLISH to "Document number"
                )
            )
            .withType(AttributeType.TEXT)
            .build()
    ).associateBy { it.id }
}
