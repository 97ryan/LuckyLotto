// SPDX-License-Identifier: GPL-3.0-or-later
package org.luckylotto.data

import org.json.JSONArray
import org.json.JSONObject
import org.luckylotto.domain.Draw
import java.time.Instant

/** Anonymous public endpoint used by OLG.ca. No account, API key, cookies or private endpoints. */
object OlgFeed {
    const val base = "https://gateway.www.olg.ca/bede-middleware/lottery/drawinformation"
    val products = listOf("649", "LMAX", "LOTT", "DLYGND", "ONT49", "DKENO", "PICK2", "PICK3", "PICK4", "CPOP")
    fun url(product: String): String {
        require(product in products)
        return "$base?productId=$product&startingDrawNumber=0&numberOfDraws=2"
    }
    fun parse(product: String, body: String): List<Draw> {
        val array = JSONArray(body)
        require(array.length() in 1..100) { "Unexpected draw response" }
        return (0 until array.length()).map { i ->
            val row = array.getJSONObject(i)
            val closes = Instant.parse(row.getString("salesEndDate"))
            val jackpotData = row.optJSONObject("jackpots") ?: JSONObject()
            val jackpots = jackpotData.optJSONArray("drawJackpots") ?: JSONArray()
            fun amount(set: String): Long? = (0 until jackpots.length()).asSequence()
                .map { jackpots.getJSONObject(it) }.find { it.optString("drawSetName") == set }
                ?.optString("jackpotAmount")?.toBigDecimalOrNull()?.takeIf { it.signum() >= 0 && it <= 1_000_000_000.toBigDecimal() }?.toLong()
            Draw(product, row.getString("drawNumber"), closes, amount("REG"),
                jackpotData.optString("maxMillionsCount", "0").toIntOrNull()?.coerceIn(0, 1000) ?: 0,
                jackpotData.optString("maxplusCount", "0").toIntOrNull()?.coerceIn(0, 1000) ?: 0,
                amount("GBDW"))
        }
    }
}
