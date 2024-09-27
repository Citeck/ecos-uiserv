package ru.citeck.ecos.uiserv.domain.journal.api.records

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.uiserv.domain.journalsettings.repo.JournalSettingsEntity

class JournalSettingsEntityTest {

    @Test
    fun test() {

        val entity = JournalSettingsEntity()
        entity.authorities = mutableListOf("aa", "bb")

        val initialList = entity.authorities

        listOf(
            listOf("aa", "bb", "cc"),
            listOf("aa", "dd", "bb"),
            listOf(),
            listOf("qq", "ee"),
            listOf(),
            listOf("rr", "tt"),
            listOf("rr", "tt", "aaa"),
            listOf("12", "34", "56", "78"),
            listOf("rr"),
        ).forEach {
            entity.setAuthoritiesForEntity(it)
            if (it.isEmpty()) {
                assertThat(entity.authorities).describedAs(it.toString()).isEmpty()
            } else {
                assertThat(entity.authorities).describedAs(it.toString()).containsExactlyElementsOf(it)
            }
        }

        assertThat(entity.authorities).isSameAs(initialList)
    }
}
