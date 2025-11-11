package ru.citeck.ecos.uiserv.domain.theme.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.mutate.ValueMutateDao
import ru.citeck.ecos.uiserv.app.common.perms.UiServSystemArtifactPerms
import ru.citeck.ecos.uiserv.domain.theme.service.ThemeService
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class ApplyThemeRecordsDao(
    private val themeService: ThemeService,
    private val perms: UiServSystemArtifactPerms
) : AbstractRecordsDao(), ValueMutateDao<ApplyThemeRecordsDao.ApplyThemeDto> {

    companion object {
        const val ID = "apply-theme"
        private const val ACTIVE_THEME_CONFIG_REF = "eapps/config@app/uiserv\$active-theme"
        private const val ATT_VALUE = "_value"
    }

    override fun getId(): String {
        return ID
    }

    override fun mutate(value: ApplyThemeDto): Any? {
        perms.checkWrite(EntityRef.create(AppName.UISERV, ThemeRecords.ID, value.themeId))

        if (themeService.activeTheme == value.themeId) {
            error("Theme ${value.themeId} already activated")
        }

        recordsService.mutate(
            ACTIVE_THEME_CONFIG_REF,
            mapOf(
                ATT_VALUE to value.themeId
            )
        )
        return ""
    }

    data class ApplyThemeDto(
        val themeId: String
    )
}
