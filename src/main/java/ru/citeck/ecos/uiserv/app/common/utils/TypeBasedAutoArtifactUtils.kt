package ru.citeck.ecos.uiserv.app.common.utils

import com.hazelcast.replicatedmap.ReplicatedMap
import ru.citeck.ecos.commons.data.entity.EntityWithMeta
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef

object TypeBasedAutoArtifactUtils {

    fun <T : Any> processTypeChanged(
        registry: ReplicatedMap<String, EntityWithMeta<T>>,
        before: EntityWithMeta<TypeDef>?,
        after: EntityWithMeta<TypeDef>?,
        autoArtifactPrefix: String,
        getArtifactRef: (TypeDef) -> EntityRef,
        createRegistryValue: (EntityWithMeta<TypeDef>) -> EntityWithMeta<T>?
    ) {
        val idBefore = before?.entity?.let { getArtifactRef(it) }?.getLocalId() ?: ""
        val idAfter = after?.entity?.let { getArtifactRef(it) }?.getLocalId() ?: ""
        if (idBefore != idAfter && idBefore.startsWith(autoArtifactPrefix)) {
            registry.remove(idBefore)
        }
        if (!idAfter.startsWith(autoArtifactPrefix)) {
            return
        }
        val newValue = after?.let { createRegistryValue(it) }
        if (registry[idAfter] != newValue) {
            if (newValue != null) {
                registry[idAfter] = newValue
            } else {
                registry.remove(idAfter)
            }
        }
    }
}
