// SPDX-License-Identifier: GPL-3.0-or-later
package org.luckylotto.domain

import java.time.DayOfWeek.*
import java.time.LocalTime

/** Published game rules reviewed 2026-10-18. All monetary amounts are CAD. */
object Catalog {
    private const val root = "https://www.olg.ca/en/lottery/play-"
    private val twice = listOf(LocalTime.of(14, 0), LocalTime.of(22, 30))
    private fun source(slug: String) = "$root$slug/past-results.html"
    private fun p(name: String, amount: Long, probability: Double, kind: PrizeKind = PrizeKind.CASH) = Prize(name, amount, probability, kind)
    private fun pool(name: String, probability: Double) = Prize(name, null, probability, PrizeKind.VARIABLE)
    private fun component(name: String, vararg tiers: Prize) = PrizeComponent(name, tiers.toList())
    private fun lotto49(ontario: Boolean): List<PrizeComponent> {
        val n = 49
        return listOf(component("Main draw • one line",
            p("6/6 • shared jackpot", if (ontario) 2_000_000 else 5_000_000, bonusMatch(n, 6, 6, false), PrizeKind.SHARED),
            if (ontario) p("5/6 + bonus • shared", 50_000, bonusMatch(n, 6, 5, true), PrizeKind.SHARED) else pool("5/6 + bonus • variable pool", bonusMatch(n, 6, 5, true)),
            if (ontario) p("5/6", 500, bonusMatch(n, 6, 5, false)) else pool("5/6 • variable pool", bonusMatch(n, 6, 5, false)),
            if (ontario) p("4/6", 50, match(n, 6, 6, 4)) else pool("4/6 • variable pool", match(n, 6, 6, 4)),
            p("3/6", 10, match(n, 6, 6, 3)),
            p("2/6 + bonus", if (ontario) 3 else 5, bonusMatch(n, 6, 2, true)),
            Prize("2/6 • free play", 0, bonusMatch(n, 6, 2, false), PrizeKind.FREE_PLAY)
        ))
    }
    private fun maxLines(jackpot: Long? = null) = List(4) { line -> component("Main draw • line ${line + 1} of 4",
        if (jackpot != null) p("7/7 • advertised shared jackpot", jackpot, 1 / choose(52, 7), PrizeKind.SHARED) else pool("7/7 • jackpot unavailable", 1 / choose(52, 7)),
        pool("6/7 + bonus • variable pool", bonusMatch(52, 7, 6, true)),
        pool("6/7 • variable pool", bonusMatch(52, 7, 6, false)),
        pool("5/7 + bonus • variable pool", bonusMatch(52, 7, 5, true)),
        pool("5/7 • variable pool", bonusMatch(52, 7, 5, false)),
        pool("4/7 + bonus • variable pool", bonusMatch(52, 7, 4, true)),
        p("4/7", 20, bonusMatch(52, 7, 4, false)),
        p("3/7 + bonus", 20, bonusMatch(52, 7, 3, true)),
        Prize("3/7 • free play", 0, bonusMatch(52, 7, 3, false), PrizeKind.FREE_PLAY)
    ) }
    private val lottario = List(2) { i -> component("Main draw • line ${i + 1} of 2",
        p("6/6 • minimum shared jackpot", 250_000, 1 / choose(45, 6), PrizeKind.SHARED),
        p("5/6 + bonus", 10_000, bonusMatch(45, 6, 5, true)),
        p("5/6", 500, bonusMatch(45, 6, 5, false)),
        p("4/6 + bonus", 30, bonusMatch(45, 6, 4, true)),
        p("4/6", 10, bonusMatch(45, 6, 4, false)),
        p("3/6 + bonus", 5, bonusMatch(45, 6, 3, true)),
        p("3/6", 4, bonusMatch(45, 6, 3, false)),
        Prize("0/6 + bonus • free play", 0, bonusMatch(45, 6, 0, true), PrizeKind.FREE_PLAY)
    ) }
    private val dailyGrand = listOf(component("Main draw",
        p("5 + Grand Number • shared cash option", 7_000_000, match(49, 5, 5, 5) / 7, PrizeKind.SHARED),
        p("5 • shared cash option", 500_000, match(49, 5, 5, 5) * 6 / 7, PrizeKind.SHARED),
        p("4 + Grand Number", 1_000, match(49, 5, 5, 4) / 7),
        p("4", 500, match(49, 5, 5, 4) * 6 / 7),
        p("3 + Grand Number", 100, match(49, 5, 5, 3) / 7),
        p("3", 20, match(49, 5, 5, 3) * 6 / 7),
        p("2 + Grand Number", 10, match(49, 5, 5, 2) / 7),
        p("1 + Grand Number", 4, match(49, 5, 5, 1) / 7),
        Prize("Grand Number only • free play", 0, match(49, 5, 5, 0) / 7, PrizeKind.FREE_PLAY)
    ))
    private val kenoPayouts = mapOf(
        2 to mapOf(2 to 7L), 3 to mapOf(3 to 25L), 4 to mapOf(4 to 100L),
        5 to mapOf(5 to 250L, 4 to 5L), 6 to mapOf(6 to 1_000L, 5 to 25L),
        7 to mapOf(7 to 5_000L, 6 to 50L, 5 to 5L), 8 to mapOf(8 to 25_000L, 7 to 200L, 6 to 10L),
        9 to mapOf(9 to 50_000L, 8 to 1_000L, 7 to 100L, 6 to 5L),
        10 to mapOf(10 to 250_000L, 9 to 5_000L, 8 to 200L, 7 to 25L, 0 to 2L)
    )
    private val kenoGames = kenoPayouts.flatMap { (spots, prizes) -> listOf(1, 2, 5, 10).map { wager ->
        Game("DKENO_${spots}_$wager", "DAILY KENO • $spots pick • $$wager", "DAILY KENO", wager,
            times = twice, source = source("daily-keno-encore"),
            note = "Pick $spots of 70 numbers. Prize liability limits can reduce advertised payouts.",
            components = listOf(PrizeComponent("$spots pick • $$wager wager", prizes.map { (hits, dollars) -> p("$hits of $spots matches", dollars * wager, match(70, 20, spots, hits)) })))
    } }
    private val pick4 = listOf(1 to 5_000L, 4 to 1_250L, 6 to 800L, 12 to 400L, 24 to 200L).map { (perms, dollars) ->
        val category = if (perms == 1) "Straight" else "$perms-way box"
        Game("PICK4_$perms", "PICK-4 • $category", "PICK-4", 1, times = twice,
            source = source("pick-4"), note = "Choose digits matching the $category pattern. Prize liability limits can reduce payouts.",
            components = listOf(component(category, p(category, dollars, perms / 10_000.0))))
    }
    // CASH POP distributions differ by wager. These are OLG's published unconditional odds per POP.
    private val popGames = listOf(
        Triple(2, listOf(10L,14,20,30,40,50,100,200,500), listOf(30.39,71.43,107.14,187.50,294.12,937.50,2624.99,5000.0,20000.0)),
        Triple(5, listOf(25L,35,50,75,100,125,250,500,1250), listOf(30.96,75.0,107.14,172.41,254.24,769.23,2500.0,4687.50,20000.0)),
        Triple(10, listOf(50L,70,100,150,200,250,500,1000,2500), listOf(31.88,75.0,107.14,162.16,238.10,697.67,1875.0,3947.37,20000.0)),
        Triple(20, listOf(100L,140,200,300,400,500,1000,2000,5000), listOf(32.0,83.33,114.50,147.06,204.08,500.0,1500.0,3750.0,20000.0))
    ).map { (wager, prizes, odds) ->
        Game("CPOP_$wager", "CASH POP • $$wager per POP", "CASH POP", wager,
            times = listOf(LocalTime.of(9,30), LocalTime.of(13,30), LocalTime.of(16,0), LocalTime.of(19,30), LocalTime.of(23,30)),
            encore = false, source = source("cash-pop") + "#cash-pop-odds",
            note = "One randomly selected POP per modeled play; published odds are rounded. Multi-POP ticket coverage is not modeled.",
            components = listOf(PrizeComponent("One POP • $$wager wager", prizes.zip(odds).map { (cash, denominator) -> p("$$cash", cash, 1 / denominator) })))
    }
    private val poker = listOf(
        component("Instant hand", p("Royal flush",5_000,4 / choose(52,5)), p("Straight flush",500,36 / choose(52,5)),
            p("Four of a kind",175,624 / choose(52,5)), p("Full house",75,3744 / choose(52,5)),
            p("Flush",50,5108 / choose(52,5)), p("Straight",20,10200 / choose(52,5)),
            p("Three of a kind",5,54912 / choose(52,5)), p("Two pair",2,123552 / choose(52,5))),
        component("Nightly draw", p("5/5",100_000,match(52,5,5,5)), p("4/5",500,match(52,5,5,4)),
            p("3/5",25,match(52,5,5,3)), p("2/5",5,match(52,5,5,2)))
    )
    // Exact seven-die multiplicities; each count is out of 6^7 equally likely rolls.
    private val mega = listOf(component("Instant dice roll",
        p("Seven ones",7_500,1/279936.0), p("Seven of a kind (2–6)",1_500,5/279936.0),
        p("Six of a kind",100,210/279936.0), p("Four + three of a kind",25,1050/279936.0),
        p("Five of a kind",10,3150/279936.0), p("Three + three of a kind",5,8400/279936.0),
        p("Three of a kind + two pairs",4,12600/279936.0), p("Straight (all six faces)",3,15120/279936.0),
        p("Four of a kind",2,25200/279936.0)),
        component("Nightly draw", p("6/6",100_000,1 / choose(39,6)),
        p("5/6 + bonus",5_000,bonusMatch(39,6,5,true)), p("5/6",250,bonusMatch(39,6,5,false)),
        p("4/6",15,match(39,6,6,4)), p("3/6",3,match(39,6,6,3))))

    val games: List<Game> = listOf(
        Game("LMAX", "LOTTO MAX", "LOTTO MAX", 6, setOf(TUESDAY, FRIDAY), source = source("lotto-max-encore"),
            note = "Four 7-of-52 lines per $6 play. Variable pools excluded; live MAXPLUS/MAXMILLIONS counted only with shared prizes enabled.", components = maxLines()),
        Game("649", "LOTTO 6/49", "LOTTO 6/49", 3, setOf(WEDNESDAY, SATURDAY), source = source("lotto-649-encore"),
            note = "Gold Ball odds depend on selections sold and are excluded. Variable Classic pool tiers are excluded.", components = lotto49(false)),
        Game("ONT49", "ONTARIO 49", "ONTARIO 49", 1, setOf(WEDNESDAY, SATURDAY), source = source("ontario-49-encore"), components = lotto49(true)),
        Game("LOTT", "LOTTARIO", "LOTTARIO", 1, setOf(SATURDAY), source = source("lottario-encore"),
            note = "Two 6-of-45 lines. Early Bird is excluded because the per-winner share is unknown. $250,000 minimum jackpot pool unless a live amount is available.", components = lottario),
        Game("DLYGND", "DAILY GRAND", "DAILY GRAND", 3, setOf(MONDAY, THURSDAY), source = source("daily-grand-encore"),
            note = "Life prizes use their cash options when shared prizes are enabled; multiple winners share the cash pool.", components = dailyGrand),
        Game("PICK2", "PICK-2", "PICK-2", 2, times = twice, source = source("pick-2"),
            components = listOf(component("Two digits",p("Both digits in order",99,0.01),p("First digit only",2,0.09)))),
        Game("PICK3", "PICK-3 • ENCORE host", "PICK-3", 1, times = twice, source = source("pick-3"),
            note = "All PICK-3 cash prizes are sales-dependent and excluded. The $1 straight play may still be a legal host for ENCORE.",
            components = listOf(component("Straight play", pool("3 digits • variable pool",0.001)))),
        Game("POKLOT", "POKER LOTTO", "POKER LOTTO", 2, encore = false, source = source("poker-lotto"),
            note = "Base $2 hand: instant cash plus nightly draw. ALL IN and free plays excluded.", components = poker),
        Game("MDICE", "MEGADICE LOTTO", "MEGADICE", 2, encore = false, source = source("megadice-lotto"),
            note = "Includes the instant seven-die roll and nightly 6-of-39 draw. Liability limits may reduce nightly prizes.", components = mega),
        Game("LTNING", "LIGHTNING LOTTO", "LIGHTNING", 2, times = listOf(LocalTime.of(23,58)), encore = false,
            source = source("lightning-lotto"), note = "Instant retail result, not an evening draw. Three lines per play. Jackpot uses the $125,000 starting minimum, not a live progressive amount.",
            components = listOf(component("Instant result • per $2 play (published approximate odds)",
                p("5/5 • minimum jackpot",125_000,1/635628.0),p("4/5 + bonus",5_000,1/127125.6),p("4/5",250,1/2956.4),p("3/5",7,1/67.19),p("2/5",2,1/4.8)))),
        Game("PBUCKS", "POWERBUCKS WATCH ‘N WIN", "POWERBUCKS", 3, encore = false, source = "$root" + "powerbucks-watch-n-win/about.html",
            note = "Not ranked: theme-specific instant prize distributions and progressive data need a verified model.", supported = false),
        Game("DREAM", "DREAM DRAWS & promotions", "PROMOTIONS", 0, encore = false, source = "$root" + "lotto-max-encore/dream-draws.html",
            note = "Not ranked: limited-time promotions need their own dated rules and sales-dependent odds.", supported = false)
    ) + pick4 + kenoGames + popGames

    fun componentsFor(game: Game, draw: Draw?): List<PrizeComponent> {
        if (game.id == "LMAX") {
            val result = maxLines(draw?.jackpot).toMutableList()
            if (draw != null) {
                repeat(draw.maxMillions) { i -> result += component("MAXMILLIONS ${i + 1}", p("Shared $1 million",1_000_000,1 / choose(52,7),PrizeKind.SHARED))
                    .let { c -> c.copy(tiers = c.tiers.map { it.copy(probability = 1 - Math.pow(1 - it.probability,4.0)) }) } }
                repeat(draw.maxPlus) { i -> result += component("MAXPLUS ${i + 1}", p("Shared $100,000",100_000,1 - Math.pow(1 - 1 / choose(52,7),4.0),PrizeKind.SHARED)) }
            }
            return result
        }
        if (game.id == "LOTT" && draw?.jackpot != null) return game.components.map { c -> c.copy(tiers = c.tiers.map { if (it.kind == PrizeKind.SHARED) it.copy(dollars = draw.jackpot, name = "6/6 • advertised shared jackpot") else it }) }
        return game.components
    }
}
