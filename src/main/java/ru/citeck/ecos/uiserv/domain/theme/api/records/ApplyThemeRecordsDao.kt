package ru.citeck.ecos.uiserv.domain.theme.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.mutate.ValueMutateDao
import ru.citeck.ecos.uiserv.domain.theme.service.ThemeService

@Component
class ApplyThemeRecordsDao(
    private val themeService: ThemeService,
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
        if (AuthContext.isNotRunAsSystemOrAdmin()) {
            error("Permission denied")
        }

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
