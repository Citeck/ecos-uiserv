package ru.citeck.ecos.uiserv.app.application.eapps

import org.springframework.stereotype.Component
import ru.citeck.ecos.apps.app.domain.ecostype.service.ModelTypeArtifactResolver
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.uiserv.domain.dashdoard.service.DashboardService
import ru.citeck.ecos.uiserv.domain.form.api.records.EcosFormRecordsDao
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService

@Component
class UiservModelTypeArtifactResolver(
    val dashboardService: DashboardService,
    val formsService: EcosFormService
) : ModelTypeArtifactResolver {

    override fun getTypeArtifacts(typeRef: RecordRef): List<RecordRef> {

        val result = mutableListOf<RecordRef>()
        val forms = formsService.getFormsForExactType(typeRef)
        result.addAll(
            forms.map {
                RecordRef.valueOf("uiserv/" + EcosFormRecordsDao.ID + "@" + it.id)
            }
        )

        return result
    }
}
