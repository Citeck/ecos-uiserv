package ru.citeck.ecos.uiserv.domain.ecostype.service

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.model.lib.attributes.dto.AttributeDef
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import java.util.*

object EcosTypeAttsUtils {

    val STD_ATTS = listOf(
        AttributeDef.create()
            .withId("_disp")
            .withName(
                MLText(
                    Locale("ru") to "Имя",
                    Locale.ENGLISH to "Name"
                )
            )
            .withType(AttributeType.TEXT)
            .build(),
        AttributeDef.create()
            .withId("_status")
            .withName(
                MLText(
                    Locale("ru") to "Статус",
                    Locale.ENGLISH to "Status"
                )
            )
            .withType(AttributeType.TEXT)
            .build(),
        AttributeDef.create()
            .withId("_docNum")
            .withName(
                MLText(
                    Locale("ru") to "Номер документа",
                    Locale.ENGLISH to "Document number"
                )
            )
            .withType(AttributeType.TEXT)
            .build()
    ).associateBy { it.id }
}
