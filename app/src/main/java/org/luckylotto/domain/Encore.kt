// SPDX-License-Identifier: GPL-3.0-or-later
package org.luckylotto.domain

import kotlin.math.pow

/** Enumerate 128 equality masks, weighted by the 9 possible mismatches at each position. */
object Encore {
    val component: PrizeComponent by lazy {
        val outcomes = mutableMapOf<Long, Double>()
        for (mask in 0 until 128) {
            var prefix = 0
            while (prefix < 7 && mask and (1 shl prefix) != 0) prefix++
            var suffix = 0
            while (suffix < 7 && mask and (1 shl (6 - suffix)) != 0) suffix++
            val left = longArrayOf(0, 0, 5, 10, 50, 500, 10_000)
            val right = longArrayOf(0, 2, 5, 10, 100, 1_000, 100_000)
            val cash = if (prefix == 7) 1_000_000L else left[prefix] + right[suffix]
            val probability = 9.0.pow(7 - Integer.bitCount(mask)) / 10_000_000.0
            outcomes[cash] = (outcomes[cash] ?: 0.0) + probability
        }
        PrizeComponent("ENCORE", outcomes.filterKeys { it > 0 }.map { (cash, p) -> Prize("ENCORE $$cash", cash, p) })
    }
    fun chance(targetCents: Long): Double = component.chance(targetCents, false)
}
