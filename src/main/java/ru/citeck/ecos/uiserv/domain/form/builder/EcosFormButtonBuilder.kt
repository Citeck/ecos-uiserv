package ru.citeck.ecos.uiserv.domain.form.builder

import ru.citeck.ecos.commons.data.MLText

interface EcosFormButtonBuilder<T> {

    fun withKey(key: String): EcosFormButtonBuilder<T>

    fun withName(name: MLText): EcosFormButtonBuilder<T>

    fun withProperty(key: String, value: String): EcosFormButtonBuilder<T>

    fun build(): T
}
