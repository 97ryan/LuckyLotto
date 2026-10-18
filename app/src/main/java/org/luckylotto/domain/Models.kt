// SPDX-License-Identifier: GPL-3.0-or-later
package org.luckylotto.domain

import java.time.*
import kotlin.math.*

val Ontario: ZoneId = ZoneId.of("America/Toronto")

enum class PrizeKind { CASH, SHARED, VARIABLE, FREE_PLAY }

data class Prize(val name: String, val dollars: Long?, val probability: Double, val kind: PrizeKind = PrizeKind.CASH) {
    init { require(probability in 0.0..1.0); require(dollars == null || dollars >= 0) }
}

/** Tiers within a component are mutually exclusive. Components are independent in the model. */
data class PrizeComponent(val name: String, val tiers: List<Prize>) {
    fun chance(targetCents: Long, includeShared: Boolean): Double = tiers.sumOf {
        if (it.dollars != null && it.dollars * 100 >= targetCents &&
            (it.kind == PrizeKind.CASH || (includeShared && it.kind == PrizeKind.SHARED))) it.probability else 0.0
    }.coerceIn(0.0, 1.0)
}

data class Game(
    val id: String, val name: String, val shortName: String, val price: Int,
    val days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val times: List<LocalTime> = listOf(LocalTime.of(22, 30)),
    val encore: Boolean = true, val source: String,
    val note: String = "", val supported: Boolean = true,
    val components: List<PrizeComponent> = emptyList(),
) {
    fun chance(targetCents: Long, shared: Boolean): Double = union(components.map { it.chance(targetCents, shared) })
}

data class Draw(
    val gameId: String, val number: String, val closesAt: Instant,
    val jackpot: Long? = null, val maxMillions: Int = 0, val maxPlus: Int = 0,
    val goldBall: Long? = null,
)

data class FeedState(
    val draws: List<Draw> = emptyList(), val fetchedAt: Instant? = null,
    val lastAttempt: Instant? = null, val errors: Map<String, String> = emptyMap(),
    val bundled: Boolean = false,
) {
    fun fresh(now: Instant): Boolean = !bundled && fetchedAt != null &&
        Duration.between(fetchedAt, now).let { !it.isNegative && it <= Duration.ofHours(6) }
}

data class PlanSettings(
    val budgetCents: Int = 2_000, val targetCents: Long = 10_000,
    val allowEncore: Boolean = true, val includeShared: Boolean = false,
    val enabledGames: Set<String> = emptySet(), val nextWeek: Boolean = false,
)

data class TicketOption(val game: Game, val at: Instant, val encoreCount: Int, val probability: Double) {
    val cost: Int get() = game.price + encoreCount
    val score: Double get() = -ln1p(-probability.coerceAtMost(1.0 - 1e-15))
    val key: String get() = "${game.id}:$encoreCount"
}
data class PlanLine(val ticket: TicketOption, val count: Int) {
    val cost: Int get() = ticket.cost * count
    val probability: Double get() = -expm1(-ticket.score * count)
}
data class WeeklyPlan(
    val starts: LocalDate, val lines: List<PlanLine>, val spentCents: Int,
    val probability: Double, val comparisons: List<TicketOption>, val settings: PlanSettings,
)

fun union(probabilities: List<Double>): Double = -expm1(probabilities.sumOf { ln1p(-it.coerceIn(0.0, 1.0)) })
fun choose(n: Int, k: Int): Double {
    if (n < 0 || k < 0 || k > n) return 0.0
    var result = 1.0
    for (i in 1..min(k, n - k)) result = result * (n - i + 1) / i
    return result
}
fun match(n: Int, drawn: Int, picked: Int, hits: Int): Double =
    choose(drawn, hits) * choose(n - drawn, picked - hits) / choose(n, picked)
fun bonusMatch(n: Int, drawn: Int, hits: Int, bonus: Boolean): Double =
    choose(drawn, hits) * choose(n - drawn - 1, drawn - hits - if (bonus) 1 else 0) / choose(n, drawn)

fun parseCents(value: String): Long? = try {
    val clean = value.trim()
    if (!Regex("[0-9]+(?:\\.[0-9]{1,2})?").matches(clean)) null
    else clean.toBigDecimal().movePointRight(2).longValueExact()
} catch (_: ArithmeticException) { null } catch (_: NumberFormatException) { null }
