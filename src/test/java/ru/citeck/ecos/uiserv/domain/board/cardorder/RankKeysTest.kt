package ru.citeck.ecos.uiserv.domain.board.cardorder

import org.junit.jupiter.api.Test
import ru.citeck.ecos.uiserv.domain.board.cardorder.service.RankKeys
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RankKeysTest {

    @Test
    fun `between null and null is a mid key`() {
        val k = RankKeys.between(null, null)
        assertTrue(k.isNotEmpty())
        assertTrue("" < k)
    }

    @Test
    fun `between produces strictly increasing keys`() {
        val a = RankKeys.between(null, null) // some mid key
        val before = RankKeys.between(null, a)
        val after = RankKeys.between(a, null)
        val mid = RankKeys.between(before, a)
        assertTrue(before < a, "$before < $a")
        assertTrue(a < after, "$a < $after")
        assertTrue(before < mid && mid < a, "$before < $mid < $a")
    }

    @Test
    fun `between adjacent keys grows length`() {
        // "i0" and "j0": no char strictly between 'i' and 'j' at pos 0 -> must extend
        val k = RankKeys.between("i0", "j0")
        assertTrue("i0" < k && k < "j0", "i0 < $k < j0")
        assertTrue(k.length >= 2)
    }

    @Test
    fun `seedSpread is strictly increasing and bounded length`() {
        val keys = RankKeys.seedSpread(5)
        assertEquals(5, keys.size)
        for (i in 1 until keys.size) {
            assertTrue(keys[i - 1] < keys[i], "${keys[i - 1]} < ${keys[i]}")
        }
        assertTrue(keys.all { it.length <= 4 })
    }

    @Test
    fun `random insert sequence keeps lexicographic order matching insertion intent`() {
        val rnd = Random(42)
        val list = ArrayDeque<Pair<String, Int>>() // rankKey to logical value
        repeat(300) { value ->
            val pos = if (list.isEmpty()) 0 else rnd.nextInt(list.size + 1)
            val prev = if (pos == 0) null else list[pos - 1].first
            val next = if (pos == list.size) null else list[pos].first
            val key = RankKeys.between(prev, next)
            list.add(pos, key to value)
        }
        // keys, read left to right, must be strictly increasing
        for (i in 1 until list.size) {
            assertTrue(list[i - 1].first < list[i].first, "broken at $i: ${list[i - 1]} vs ${list[i]}")
        }
    }

    @Test
    fun `MAX_RANK_LEN is exposed and reasonable`() {
        assertTrue(RankKeys.MAX_RANK_LEN in 16..64)
    }

    @Test
    fun `between never returns a key outside the open interval`() {
        listOf("i0" to "j0", null to "1", "z" to null, "a" to "b", "9" to "a").forEach { (lo, hi) ->
            val k = RankKeys.between(lo, hi)
            if (lo != null) assertTrue(lo < k, "expected '$lo' < '$k'")
            if (hi != null) assertTrue(k < hi, "expected '$k' < '$hi'")
        }
    }

    @Test
    fun `seedSpread keys never end in the minimum digit`() {
        for (n in 1..40) {
            RankKeys.seedSpread(n).forEach { k ->
                assertTrue(k.isNotEmpty() && k.last() != '0', "seedSpread($n) produced '$k' ending in '0'")
            }
        }
    }
}
