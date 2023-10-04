package ru.citeck.ecos.uiserv.domain.form.builder

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef

interface EcosFormBuilder {

    fun withId(id: String): EcosFormBuilder

    fun withWidth(width: EcosFormWidth): EcosFormBuilder

    fun withTitle(title: MLText): EcosFormBuilder

    fun withComponents(action: (EcosFormComponentsBuilder) -> Unit): EcosFormBuilder

    fun build(): EcosFormDef
}
