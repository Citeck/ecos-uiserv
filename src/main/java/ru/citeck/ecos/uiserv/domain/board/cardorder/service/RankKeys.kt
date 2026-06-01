package ru.citeck.ecos.uiserv.domain.board.cardorder.service

/**
 * Fractional ("LexoRank-style") ranking over base-36 strings ('0'..'9','a'..'z').
 * Lexicographic string order == intended order, because in ASCII '0'<'9'<'a'<'z'.
 */
object RankKeys {

    private const val DIGITS = "0123456789abcdefghijklmnopqrstuvwxyz"
    private const val BASE = 36

    /**
     * Advisory cap on rank-key length. Not enforced here — callers should detect keys longer than
     * this and rebalance the affected column via [seedSpread].
     */
    const val MAX_RANK_LEN = 32

    private fun valueOf(c: Char): Int {
        val v = DIGITS.indexOf(c)
        require(v >= 0) { "Illegal rank char: '$c'" }
        return v
    }

    /**
     * Returns a key strictly between [prev] and [next] (lexicographically).
     * [prev] == null means "before everything", [next] == null means "after everything".
     * Requires prev < next when both non-null.
     */
    fun between(prev: String?, next: String?): String {
        val p = prev ?: ""
        val n = next
        require(n == null || p < n) { "Invalid order: prev='$prev' next='$next'" }

        val sb = StringBuilder()
        var i = 0
        var diverged = false // true once the result is already strictly less than `next`
        while (true) {
            val pc = if (i < p.length) valueOf(p[i]) else 0 // missing prev digit -> 0
            val nc = if (!diverged && n != null && i < n.length) valueOf(n[i]) else BASE // missing/irrelevant next digit -> open
            if (pc == nc) {
                sb.append(DIGITS[pc])
                i++
                continue
            }
            // here pc < nc always (guaranteed by require + the equal-digit branch above)
            val mid = (pc + nc) / 2
            if (mid > pc) {
                sb.append(DIGITS[mid]) // mid > pc >= 0  =>  mid >= 1  =>  result never ends in '0'
                return sb.toString()
            }
            // adjacent (mid == pc): take prev's digit, descend; the result is now strictly below `next`
            sb.append(DIGITS[pc])
            i++
            diverged = true
        }
    }

    /**
     * Returns [count] strictly-increasing seed keys, none ending in the minimum digit '0',
     * spread to leave head/tail room. Used to (re)materialize a column's order.
     */
    fun seedSpread(count: Int): List<String> {
        require(count >= 0)
        if (count == 0) return emptyList()
        val slots = BASE - 1 // usable digits 1..35
        if (count <= slots) {
            val step = slots.toDouble() / (count + 1)
            val raw = (1..count).map { idx -> "${DIGITS[(step * idx).toInt().coerceIn(1, BASE - 1)]}" }
            // fix any collisions caused by rounding
            val out = ArrayList<String>(count)
            var last: String? = null
            for (k in raw) {
                val fixed = if (last != null && k <= last) between(last, null) else k
                out.add(fixed)
                last = fixed
            }
            return out
        }
        val result = ArrayList<String>(count)
        var prev: String? = null
        repeat(count) {
            val k = between(prev, null)
            result.add(k)
            prev = k
        }
        return result
    }
}
