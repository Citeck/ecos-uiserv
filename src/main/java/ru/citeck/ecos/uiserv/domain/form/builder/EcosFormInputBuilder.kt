package ru.citeck.ecos.uiserv.domain.form.builder

import ru.citeck.ecos.commons.data.MLText

interface EcosFormInputBuilder {

    fun setKey(key: String): EcosFormInputBuilder

    fun setName(name: MLText): EcosFormInputBuilder

    fun setMultiple(multiple: Boolean): EcosFormInputBuilder

    fun build(): EcosFormBuilder
}
