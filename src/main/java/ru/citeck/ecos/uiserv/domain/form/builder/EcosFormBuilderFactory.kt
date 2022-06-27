package ru.citeck.ecos.uiserv.domain.form.builder

import org.springframework.stereotype.Component
import ru.citeck.ecos.uiserv.domain.ecostype.config.EcosTypesConfig

@Component
class EcosFormBuilderFactory(private val ecosTypesConfig: EcosTypesConfig) {

    fun createBuilder(): EcosFormBuilder {
        return EcosFormBuilderImpl(FormBuilderContextImpl(ecosTypesConfig))
    }
}
