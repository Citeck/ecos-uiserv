package ru.citeck.ecos.uiserv.domain.form.builder

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText

interface EcosFormInputBuilder {

    fun setData(data: DataValue): EcosFormInputBuilder

    fun setKey(key: String): EcosFormInputBuilder

    fun setName(name: MLText): EcosFormInputBuilder

    fun setMultiple(multiple: Boolean): EcosFormInputBuilder

    fun setMandatory(mandatory: Boolean): EcosFormInputBuilder

    fun build(): EcosFormBuilder
}
