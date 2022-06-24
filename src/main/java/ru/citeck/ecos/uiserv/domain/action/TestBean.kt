package ru.citeck.ecos.uiserv.domain.action

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.config.lib.consumer.bean.EcosConfig

@Component
class TestBean {

    companion object {
        val log = KotlinLogging.logger { }
    }

    var oldValue: Boolean = false

    @EcosConfig("test-config-type")
    fun changeCOnfig(newValue: Boolean) {
        log.error { "CHANGE TEST VALUE: " + newValue }
    }
}
