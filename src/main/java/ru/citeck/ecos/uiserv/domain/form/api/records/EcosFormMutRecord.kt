package ru.citeck.ecos.uiserv.domain.form.api.records

import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef
import ru.citeck.ecos.webapp.api.entity.EntityRef

class EcosFormMutRecord(other: EcosFormDef) : EcosFormDef.Builder(other) {

    constructor() : this(EcosFormDef.EMPTY)

    fun withLocalIdInWorkspace(localId: String) {

        //withId(WsScopedArtifactUtils.addWsPrefixToId(localId, ))
    }

    fun withWorkspaceRef(workspaceRef: EntityRef) {
        withWorkspace(workspaceRef.getLocalId())
    }

    fun withModuleId(value: String?) {
        withId(value)
    }
}
