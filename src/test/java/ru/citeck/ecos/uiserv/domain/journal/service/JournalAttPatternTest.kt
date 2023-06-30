package ru.citeck.ecos.uiserv.domain.journal.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JournalAttPatternTest {

    @Test
    fun test() {

        fun assert(str: String, matchExpected: Boolean) {
            val matcher = JournalServiceImpl.VALID_COLUMN_ATT_PATTERN.matcher(str)
            if (matchExpected) {
                assertThat(matcher.matches()).describedAs(str).isTrue
            } else {
                assertThat(matcher.matches()).describedAs(str).isFalse
            }
        }
        assert("", false)
        assert("name", true)
        assert("name*", false)
        assert("name?str", false)
        assert("name{", false)
        assert("name()", false)
        assert("name(abc)", true)
        assert("()", false)
        assert("(abc)", true)
    }
}
