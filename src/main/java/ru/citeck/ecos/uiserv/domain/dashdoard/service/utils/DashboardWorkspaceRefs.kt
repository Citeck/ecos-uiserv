package ru.citeck.ecos.uiserv.domain.dashdoard.service.utils

import ru.citeck.ecos.uiserv.domain.dashdoard.dto.DashboardDto
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.function.Function

/**
 * Single source of truth for the workspace-scoped refs of a [DashboardDto] (`typeRef`, `appliedToRef`).
 *
 * [rewrite] applies [transform] to every non-empty one of them and returns a copy. Every site that
 * converts dashboard refs to/from the `CURRENT_WS:` placeholder routes through here:
 *  - [ru.citeck.ecos.uiserv.domain.dashdoard.eapps.DashboardArtifactHandler] — deploy + listen/export;
 *  - `DashboardRecords` — `?data` export (download) and records-mutate import.
 */
object DashboardWorkspaceRefs {

    @JvmStatic
    fun rewrite(dashboard: DashboardDto, transform: Function<EntityRef, EntityRef>): DashboardDto {
        val builder = dashboard.copy()
        if (EntityRef.isNotEmpty(dashboard.typeRef)) {
            builder.withTypeRef(transform.apply(dashboard.typeRef))
        }
        if (EntityRef.isNotEmpty(dashboard.appliedToRef)) {
            builder.withAppliedToRef(transform.apply(dashboard.appliedToRef))
        }
        return builder.build()
    }
}
