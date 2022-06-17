package ru.citeck.ecos.uiserv.domain.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.config.lib.consumer.bean.EcosConfig
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [Application::class])
class EcosConfigTest {

    @EcosConfig("test-config")
    var testConfig: String? = null

    @Autowired
    lateinit var records: RecordsService

    @Test
    fun test() {
        val expectedValue = "test-config-value"

        assertThat(testConfig).isEqualTo(expectedValue)
        assertThat(records.getAtt("meta@", "\$cfg.test-config").asText()).isEqualTo(expectedValue)
        assertThat(records.getAtt("cfg@test-config", "value").asText()).isEqualTo(expectedValue)
    }
}
