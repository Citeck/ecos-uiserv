package ru.citeck.ecos.uiserv.domain.form.builder

import ru.citeck.ecos.commons.data.MLText

interface EcosFormPanelBuilder<T> {

    fun withKey(key: String): EcosFormPanelBuilder<T>

    fun withName(name: MLText): EcosFormPanelBuilder<T>

    fun withComponents(action: (EcosFormComponentsBuilder) -> Unit): EcosFormPanelBuilder<T>

    fun build(): T
}
