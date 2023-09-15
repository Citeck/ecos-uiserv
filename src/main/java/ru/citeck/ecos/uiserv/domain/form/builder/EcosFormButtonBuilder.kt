package ru.citeck.ecos.uiserv.domain.form.builder

import ru.citeck.ecos.commons.data.MLText

interface EcosFormButtonBuilder {

    fun setKey(key: String): EcosFormButtonBuilder

    fun setName(name: MLText): EcosFormButtonBuilder

    fun setProperty(key: String, value: String): EcosFormButtonBuilder

    fun build(): EcosFormBuilder
}
