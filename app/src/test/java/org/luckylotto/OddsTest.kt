// SPDX-License-Identifier: GPL-3.0-or-later
package org.luckylotto

import org.junit.Assert.*
import org.junit.Test
import org.luckylotto.domain.*

class OddsTest {
    @Test fun `encore reproduces overall odds and top prize`() {
        assertEquals(1 / 9.17, Encore.chance(1), 0.0001)
        assertEquals(1e-7, Encore.chance(100_000_000), 1e-14)
        assertEquals(0.0, Encore.chance(100_000_001), 0.0)
        assertEquals(1e-6, Encore.chance(10_000_000), 1e-14)
    }
    @Test fun `encore includes simultaneous prefix and suffix awards once`() {
        val tiers = Encore.component.tiers.associate { it.dollars to it.probability }
        assertEquals(9e-7, tiers[502L]!!, 1e-14)
        assertEquals(9e-7, tiers[55L]!!, 1e-14)
        assertEquals(81e-7, tiers[52L]!!, 1e-14)
        assertEquals(0.0, Encore.chance(100_000_001), 0.0)
    }
    @Test fun `hypergeometric probabilities partition outcomes`() {
        for (spots in 2..10) assertEquals(1.0, (0..spots).sumOf { match(70,20,spots,it) }, 1e-12)
        assertEquals(13_983_816.0,choose(49,6),0.0)
        assertEquals(33_446_140.0,choose(52,7)/4,0.0)
        assertEquals(0.0,choose(5,6),0.0)
    }
    @Test fun `keno uses 70 numbers and wager scales payouts not probability`() {
        val one = Catalog.games.first { it.id == "DKENO_4_1" }
        val ten = Catalog.games.first { it.id == "DKENO_4_10" }
        assertEquals(1 / 189.23,one.chance(10_000,false),0.00001)
        assertEquals(one.chance(10_000,false),ten.chance(100_000,false),1e-14)
        assertEquals(0.0,one.chance(10_001,false),0.0)
    }
    @Test fun `fixed prizes do not include shared jackpots or free plays`() {
        val ont = Catalog.games.first { it.id == "ONT49" }
        assertEquals(0.0,ont.chance(5_000_000,false),0.0)
        assertTrue(ont.chance(5_000_000,true) > 0)
        assertTrue(ont.chance(1,false) < 1/6.6)
    }
    @Test fun `649 unknown pools are never assigned an invented amount`() {
        val game = Catalog.games.first { it.id == "649" }
        assertEquals(0.0, game.chance(100_000,false),0.0)
        assertEquals(1/choose(49,6),game.chance(100_000,true),1e-14)
    }
    @Test fun `every component has a valid mutually exclusive probability mass`() {
        for (game in Catalog.games) for (component in game.components) {
            assertTrue("${game.id}: ${component.name}",component.tiers.sumOf { it.probability } <= 1.000001)
            assertTrue(component.tiers.all { it.probability.isFinite() })
        }
    }
    @Test fun `target is a single prize rather than combined small prizes`() {
        val game = Game("S","Small","Small",1,source="",components=listOf(
            PrizeComponent("a",listOf(Prize("small",60,1.0))),PrizeComponent("b",listOf(Prize("small",60,1.0)))))
        assertEquals(0.0,game.chance(10_000,false),0.0)
    }
    @Test fun `decimal money parsing rejects invalid formats and overflow`() {
        assertEquals(2001L,parseCents("20.01"))
        assertEquals(0L,parseCents("0"))
        listOf("-1","NaN","Infinity","1.001","1e4","1,00","999999999999999999999999999999").forEach { assertNull(parseCents(it)) }
    }
    @Test fun `published pick two price is two dollars`() {
        val game = Catalog.games.first { it.id == "PICK2" }
        assertEquals(2,game.price)
        assertEquals(0.10,game.chance(200,false),1e-14)
    }
    @Test fun `megadice multiplicities match exhaustive seven-die outcomes`() {
        val counts=mutableMapOf<Long,Int>()
        repeat(279936) { value ->
            var code=value
            val faces=IntArray(6)
            repeat(7) { faces[code%6]++;code/=6 }
            val sorted=faces.sortedDescending()
            val prize=when {
                faces[0]==7 -> 7500L
                sorted[0]==7 -> 1500L
                sorted[0]==6 -> 100L
                sorted[0]==4 && sorted[1]==3 -> 25L
                sorted[0]==5 -> 10L
                sorted[0]==3 && sorted[1]==3 -> 5L
                sorted.take(3)==listOf(3,2,2) -> 4L
                faces.all { it>=1 } -> 3L
                sorted[0]==4 -> 2L
                else -> 0L
            }
            counts[prize]=(counts[prize]?:0)+1
        }
        val instant=Catalog.games.first { it.id=="MDICE" }.components.first()
        for(tier in instant.tiers) assertEquals(counts[tier.dollars]!!/279936.0,tier.probability,1e-14)
        assertEquals(4.3,1/instant.chance(1,false),0.05) // official odds rounded to one decimal
    }

}
