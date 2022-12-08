package ru.citeck.ecos.uiserv.app.application.config

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.config.lib.dto.ConfigValueType
import ru.citeck.ecos.config.lib.service.EcosConfigService
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.lib.patch.annotaion.EcosPatch

@Component
@EcosPatch("configs-migration", "2022-06-24T00:00:00Z")
class ConfigsMigration(
    private val records: RecordsService,
    private val configService: EcosConfigService
) : Function0<Any?> {

    companion object {
        private val log = KotlinLogging.logger {}

        private val UISERV_CONFIGS = listOf(
            "active-theme",
            "create-menu-type",
            "custom-feedback-url",
            "custom-report-issue-url",
            "footer-content",
            "home-link-url",
            "login-page-redirect-url",
            "main-menu-type",
            "menu-group-priority",
            "orgstruct-allUsers-group-shortName",
            "orgstruct-search-user-extra-fields",
            "orgstruct-search-user-middle-name",
            "restrict-access-to-edit-dashboard",
            "separate-action-list-for-query",
            "site-dashboard-enable",
            "tabs-enabled",
        )
    }

    override fun invoke(): Any? {
        log.info { "Configs migrations started. UISERV config keys: $UISERV_CONFIGS" }
        val resultMessages = mutableListOf<String>()
        val msg: (String) -> Unit = {
            log.info { it }
            resultMessages.add(it)
        }
        for (conf in UISERV_CONFIGS) {
            var attToLoad = "value"
            val valueDef = configService.getConfig(conf).valueDef
            if (valueDef.multiple) {
                attToLoad += "[]"
            }
            attToLoad += if (valueDef.type == ConfigValueType.JSON) {
                "?json"
            } else {
                "?str"
            }
            val value = records.getAtt("config@$conf", attToLoad)
            if (value.isTextual()) {
                if (value.asText() != "null") {
                    records.mutateAtt("cfg@$conf", "value", value)
                    msg("'$conf' changed to '$value'")
                } else {
                    msg("config is empty: $conf")
                }
            } else if (value.isArray()) {
                records.mutateAtt("cfg@$conf", "value", value)
                msg("'$conf' changed to '$value'")
            } else if (value.isNull()) {
                msg("config is empty: $conf")
            } else {
                msg("Invalid config value. Key: $conf Value: $value")
            }
        }
        return resultMessages
    }
}
