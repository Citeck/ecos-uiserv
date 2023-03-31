package ru.citeck.ecos.uiserv.app.application.eapps

import org.springframework.stereotype.Component
import ru.citeck.ecos.apps.app.domain.ecostype.service.ModelTypeArtifactResolver
import ru.citeck.ecos.uiserv.domain.dashdoard.service.DashboardService
import ru.citeck.ecos.uiserv.domain.form.api.records.EcosFormRecordsDao
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class UiservModelTypeArtifactResolver(
    val dashboardService: DashboardService,
    val formsService: EcosFormService
) : ModelTypeArtifactResolver {

    override fun getTypeArtifacts(typeRef: EntityRef): List<EntityRef> {

        val result = mutableListOf<EntityRef>()
        val forms = formsService.getFormsForExactType(typeRef)
        result.addAll(
            forms.map {
                EntityRef.valueOf("uiserv/" + EcosFormRecordsDao.ID + "@" + it.id)
            }
        )

        return result
    }
}
