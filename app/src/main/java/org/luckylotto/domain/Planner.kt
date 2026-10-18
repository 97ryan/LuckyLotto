// SPDX-License-Identifier: GPL-3.0-or-later
package org.luckylotto.domain

import java.time.*
import java.time.temporal.TemporalAdjusters
import kotlin.math.*

object Planner {
    fun weekStart(now: Instant, next: Boolean = false): LocalDate = now.atZone(Ontario).toLocalDate()
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(if (next) 1 else 0)

    fun occurrences(game: Game, start: LocalDate, now: Instant): List<Instant> = (0L..6L).flatMap { offset ->
        val date = start.plusDays(offset)
        if (date.dayOfWeek !in game.days) emptyList() else game.times.map { date.atTime(it).atZone(Ontario).toInstant() }
    }.filter { it > now }

    fun options(settings: PlanSettings, feed: FeedState, now: Instant, games: List<Game> = Catalog.games): List<TicketOption> {
        val start = weekStart(now, settings.nextWeek)
        return games.filter { it.supported && it.id in settings.enabledGames }.flatMap { game ->
            occurrences(game, start, now).flatMap { at ->
                // Jackpots apply only to the precise draw, never to a later hypothetical rollover.
                val draw = if (feed.fresh(now)) feed.draws.find { it.gameId == game.id && it.closesAt == at } else null
                val base = Catalog.componentsFor(game, draw).let { c -> union(c.map { it.chance(settings.targetCents, settings.includeShared) }) }
                val extra = if (settings.allowEncore && game.encore) 10 else 0
                (0..extra).map { en ->
                    val p = -expm1(ln1p(-base) + en * ln1p(-Encore.chance(settings.targetCents)))
                    TicketOption(game, at, en, p)
                }
            }
        }.filter { it.probability > 0 && it.cost * 100 <= settings.budgetCents }
    }

    /** Unbounded knapsack on additive negative log failure, exact for this independent-play model. */
    fun optimize(settings: PlanSettings, feed: FeedState, now: Instant, games: List<Game> = Catalog.games): WeeklyPlan {
        require(settings.budgetCents in 0..50_000)
        require(settings.targetCents in 1..10_000_000_000L)
        val all = options(settings, feed, now, games)
        // Equal-value draws are interchangeable; spreading happens after optimization.
        val candidates = all.distinctBy { Triple(it.key, it.cost, it.score) }
        val budget = settings.budgetCents / 100
        val score = DoubleArray(budget + 1) { Double.NEGATIVE_INFINITY }.also { it[0] = 0.0 }
        val previous = IntArray(budget + 1) { -1 }
        for (money in 1..budget) {
            candidates.forEachIndexed { i, item ->
                if (item.cost <= money && score[money - item.cost].isFinite()) {
                    val proposed = score[money - item.cost] + item.score
                    if (proposed > score[money] + 1e-16) { score[money] = proposed; previous[money] = i }
                }
            }
        }
        var used = 0
        for (i in 1..budget) if (score[i] > score[used] + 1e-16) used = i
        val spent = used
        val selected = mutableListOf<TicketOption>()
        val load = mutableMapOf<Instant, Int>()
        while (used > 0 && previous[used] >= 0) {
            val template = candidates[previous[used]]
            val item = all.filter { it.key == template.key && abs(it.score - template.score) < 1e-16 }
                .minWith(compareBy<TicketOption> { load[it.at] ?: 0 }.thenBy { it.at })
            selected += item
            load[item.at] = (load[item.at] ?: 0) + item.cost
            used -= item.cost
        }
        val lines = selected.groupingBy { it }.eachCount().map { PlanLine(it.key, it.value) }.sortedBy { it.ticket.at }
        val comparisons = all.groupBy { it.game.id }.map { (_, items) -> items.maxBy { it.score / it.cost } }
            .sortedByDescending { it.score / it.cost }
        return WeeklyPlan(weekStart(now, settings.nextWeek), lines, spent * 100, -expm1(-score[spent]), comparisons, settings)
    }
}
