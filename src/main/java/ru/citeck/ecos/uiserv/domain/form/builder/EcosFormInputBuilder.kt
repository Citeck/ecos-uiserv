package ru.citeck.ecos.uiserv.domain.form.builder

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText

interface EcosFormInputBuilder<T> {

    fun withData(data: DataValue): EcosFormInputBuilder<T>

    fun withKey(key: String): EcosFormInputBuilder<T>

    fun withName(name: MLText): EcosFormInputBuilder<T>

    fun withMultiple(multiple: Boolean): EcosFormInputBuilder<T>

    fun withMandatory(mandatory: Boolean): EcosFormInputBuilder<T>

    fun build(): T
}
