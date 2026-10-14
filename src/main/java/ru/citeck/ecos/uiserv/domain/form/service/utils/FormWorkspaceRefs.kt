package ru.citeck.ecos.uiserv.domain.form.service.utils

import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef
import ru.citeck.ecos.uiserv.domain.workspace.ComponentWorkspaceRefs
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.function.Function

object FormWorkspaceRefs {

    @JvmStatic
    fun rewrite(form: EcosFormDef, transform: Function<EntityRef, EntityRef>): EcosFormDef {
        val builder = form.copy().withDefinition(ComponentWorkspaceRefs.rewrite(form.definition, transform))
        if (EntityRef.isNotEmpty(form.typeRef)) {
            builder.withTypeRef(transform.apply(form.typeRef))
        }
        return builder.build()
    }
}
