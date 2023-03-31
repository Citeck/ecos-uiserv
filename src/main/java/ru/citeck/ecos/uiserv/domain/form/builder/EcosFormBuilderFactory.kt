package ru.citeck.ecos.uiserv.domain.form.builder

import org.springframework.stereotype.Component
import ru.citeck.ecos.uiserv.domain.ecostype.config.EcosTypesComponent

@Component
class EcosFormBuilderFactory(private val ecosTypesComponent: EcosTypesComponent) {

    fun createBuilder(): EcosFormBuilder {
        return EcosFormBuilderImpl(FormBuilderContextImpl(ecosTypesComponent))
    }
}
