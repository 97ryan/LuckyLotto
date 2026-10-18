// SPDX-License-Identifier: GPL-3.0-or-later
package org.luckylotto

import org.junit.Assert.*
import org.junit.Test
import org.luckylotto.domain.*
import java.time.*
import kotlin.math.*

class PlannerTest {
    private val now = Instant.parse("2026-10-19T12:00:00Z")
    private fun game(id: String,cost: Int,p: Double,encore: Boolean=false) = Game(id,id,id,cost,encore=encore,source="",components=listOf(PrizeComponent("draw",listOf(Prize("cash",100,p)))))
    @Test fun `optimizer matches exhaustive integer allocations including mixed plans`() {
        val games = listOf(game("a",3,.20),game("b",2,.13))
        for (budget in 0..15) {
            val settings = PlanSettings(budget*100,10_000,enabledGames=setOf("a","b"))
            val result = Planner.optimize(settings,FeedState(),now,games)
            val reference = (0..budget/3).flatMap { a -> (0..budget/2).filter { a*3+it*2<=budget }.map { b -> 1-.8.pow(a)*.87.pow(b) } }.max()
            assertEquals(reference,result.probability,1e-12)
            assertTrue(result.spentCents<=budget*100)
        }
    }
    @Test fun `encore requires a host and charges all selected plays`() {
        val host = game("host",1,0.0,true)
        val settings = PlanSettings(1200,100,enabledGames=setOf("host"))
        val result = Planner.optimize(settings,FeedState(),now,listOf(host))
        assertTrue(result.lines.isNotEmpty())
        assertTrue(result.lines.all { it.ticket.encoreCount in 0..10 && it.cost==it.count*(1+it.ticket.encoreCount) })
        assertEquals(1100,result.spentCents)
        assertEquals(1-(1-Encore.chance(100)).pow(10),result.probability,1e-12)
    }
    @Test fun `turning encore off removes encore candidates`() {
        val g=game("host",1,0.0,true)
        val result=Planner.optimize(PlanSettings(2000,100,allowEncore=false,enabledGames=setOf("host")),FeedState(),now,listOf(g))
        assertTrue(result.lines.isEmpty())
    }
    @Test fun `empty games zero budget and impossible target leave money unspent`() {
        val settings=PlanSettings(2000,10_000,enabledGames=emptySet())
        val a=Planner.optimize(settings,FeedState(),now)
        assertEquals(0,a.spentCents);assertEquals(0.0,a.probability,0.0)
        val b=Planner.optimize(settings.copy(budgetCents=0,enabledGames=Catalog.games.map { it.id }.toSet()),FeedState(),now)
        assertTrue(b.lines.isEmpty())
        val c=Planner.optimize(settings.copy(targetCents=10_000_000_000,enabledGames=Catalog.games.map { it.id }.toSet()),FeedState(),now)
        assertTrue(c.lines.isEmpty())
    }
    @Test fun `cents are never rounded up into extra tickets`() {
        val result=Planner.optimize(PlanSettings(199,10_000,enabledGames=setOf("a")),FeedState(),now,listOf(game("a",1,.01)))
        assertEquals(100,result.spentCents)
    }
    @Test fun `elapsed and exact closing times are excluded in Toronto`() {
        val g=Catalog.games.first { it.id=="649" }
        val instant=Instant.parse("2026-10-22T02:30:00Z")
        val times=Planner.occurrences(g,LocalDate.parse("2026-10-19"),instant)
        assertEquals(1,times.size)
        assertEquals(Instant.parse("2026-10-25T02:30:00Z"),times.single())
    }
    @Test fun `Toronto week handles DST and UTC midnight correctly`() {
        val instant=Instant.parse("2026-11-02T01:00:00Z") // Sunday 8pm ET
        assertEquals(LocalDate.parse("2026-10-26"),Planner.weekStart(instant))
        assertEquals(LocalDate.parse("2026-11-02"),Planner.weekStart(instant,true))
        val g=game("daily",1,.01)
        assertEquals(Instant.parse("2026-11-02T03:30:00Z"),Planner.occurrences(g,LocalDate.parse("2026-10-26"),instant).single())
    }
    @Test fun `jackpot counts apply only to a fresh matching draw`() {
        val at=Instant.parse("2026-10-21T02:30:00Z")
        val draw=Draw("LMAX","1",at,90_000_000,maxMillions=20,maxPlus=90)
        val settings=PlanSettings(600,10_000_000,allowEncore=false,includeShared=true,enabledGames=setOf("LMAX"))
        val fresh=Planner.options(settings,FeedState(listOf(draw),now),now)
        assertTrue(fresh.first { it.at==at }.probability>0)
        assertFalse(fresh.any { it.at!=at })
        val stale=Planner.options(settings,FeedState(listOf(draw),now.minusSeconds(21601)),now)
        assertTrue(stale.isEmpty())
        assertTrue(Planner.options(settings,FeedState(listOf(draw),now,bundled=true),now).isEmpty())
    }
    @Test fun `draw allocations account for all spending and reported probability`() {
        val settings=PlanSettings(4999,10000,enabledGames=Catalog.games.filter { it.supported }.map { it.id }.toSet())
        val result=Planner.optimize(settings,FeedState(),now)
        assertEquals(result.spentCents,result.lines.sumOf { it.cost }*100)
        assertEquals(result.probability,union(result.lines.map { it.probability }),1e-12)
        assertTrue(result.lines.all { it.ticket.at > now })
    }
    @Test(expected=IllegalArgumentException::class) fun `oversize budget is rejected`() { Planner.optimize(PlanSettings(50001),FeedState(),now) }
}
