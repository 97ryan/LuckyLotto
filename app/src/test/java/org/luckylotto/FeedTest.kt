// SPDX-License-Identifier: GPL-3.0-or-later
package org.luckylotto

import org.junit.Assert.*
import org.junit.Test
import org.luckylotto.data.OlgFeed
import java.time.Instant

class FeedTest {
    private fun fixture(name: String)=javaClass.getResource("/$name")!!.readText()
    @Test fun `parse real OLG 649 feed and distinguish missing future jackpots`() {
        val draws=OlgFeed.parse("649",fixture("olg-649.json"))
        assertEquals(2,draws.size)
        assertEquals(Instant.parse("2026-10-22T02:30:00Z"),draws.first().closesAt)
        assertEquals(5_000_000L,draws.first().jackpot)
        assertEquals(26_000_000L,draws.first().goldBall)
        assertNull(draws.last().jackpot)
    }
    @Test fun `parse real new Lotto Max feed including MAXPLUS`() {
        val draws=OlgFeed.parse("LMAX",fixture("olg-lmax.json"))
        assertEquals(90_000_000L,draws.first().jackpot)
        assertEquals(90,draws.first().maxPlus)
        assertEquals(20,draws.first().maxMillions)
    }
    @Test fun `malformed changed and denied feeds do not produce fabricated data`() {
        listOf("[]","{}","{\"fault\":\"Unauthorized\"}","<html>access denied</html>","[{\"drawNumber\":1}]").forEach { body ->
            assertTrue(runCatching { OlgFeed.parse("649",body) }.isFailure)
        }
    }
    @Test(expected=IllegalArgumentException::class) fun `product IDs cannot inject another URL`() { OlgFeed.url("../../players") }
}
