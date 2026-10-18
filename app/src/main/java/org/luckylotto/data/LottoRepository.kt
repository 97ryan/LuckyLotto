// SPDX-License-Identifier: GPL-3.0-or-later
package org.luckylotto.data

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONObject
import org.luckylotto.domain.*
import java.net.HttpURLConnection
import java.net.URI
import java.time.Instant

class LottoRepository(context: Context) {
    private val prefs = context.getSharedPreferences("luckylotto", Context.MODE_PRIVATE)
    private val cache = java.io.File(context.filesDir, "olg-cache.json")

    fun readSettings(): PlanSettings = PlanSettings(
        budgetCents = prefs.getInt("budget", 2000).coerceIn(0, 50_000),
        targetCents = prefs.getLong("target", 10_000).coerceIn(1, 10_000_000_000L),
        allowEncore = prefs.getBoolean("encore", true),
        includeShared = prefs.getBoolean("shared", false),
        enabledGames = prefs.getStringSet("games", null)?.toSet()
            ?: Catalog.games.filter { it.supported }.map { it.id }.toSet(),
    )
    fun saveSettings(s: PlanSettings) { prefs.edit().putInt("budget", s.budgetCents).putLong("target", s.targetCents)
        .putBoolean("encore", s.allowEncore).putBoolean("shared", s.includeShared).putStringSet("games", s.enabledGames).apply() }
    fun theme() = prefs.getString("theme", "system") ?: "system"
    fun setTheme(value: String) { prefs.edit().putString("theme", value).apply() }
    fun dynamicColor() = prefs.getBoolean("dynamic", true)
    fun setDynamicColor(value: Boolean) { prefs.edit().putBoolean("dynamic", value).apply() }

    fun cached(): FeedState = try {
        if (!cache.exists()) FeedState() else decode(cache.readText())
    } catch (_: Exception) { FeedState() }

    private fun decode(text: String): FeedState {
        val root = JSONObject(text)
        val products = root.getJSONObject("products")
        val failed = root.optJSONObject("errors") ?: JSONObject()
        return FeedState(
            draws = products.keys().asSequence().flatMap { id -> OlgFeed.parse(id, products.getJSONArray(id).toString()).asSequence() }.toList(),
            fetchedAt = Instant.parse(root.getString("fetchedAt")),
            lastAttempt = Instant.parse(root.getString("fetchedAt")),
            errors = failed.keys().asSequence().associateWith { failed.getString(it) },
        )
    }

    suspend fun refresh(): FeedState = withContext(Dispatchers.IO) {
        val gate = Semaphore(4)
        val results = coroutineScope {
            OlgFeed.products.map { id -> async {
                gate.withPermit { id to runCatching {
                    val body = get(OlgFeed.url(id))
                    OlgFeed.parse(id, body)
                    body
                } }
            } }.awaitAll()
        }
        val now = Instant.now()
        val products = JSONObject()
        val errors = mutableMapOf<String, String>()
        results.forEach { (id, result) -> result.fold(
            onSuccess = { products.put(id, org.json.JSONArray(it)) },
            onFailure = { errors[id] = it.message ?: "Feed unavailable" }
        ) }
        if (products.length() == 0) return@withContext cached().copy(lastAttempt = now, errors = errors)
        val root = JSONObject().put("fetchedAt", now.toString()).put("products", products).put("errors", JSONObject(errors))
        val state = decode(root.toString())
        runCatching {
            val temporary = java.io.File(cache.parentFile, "olg-cache.tmp")
            temporary.writeText(root.toString())
            check(temporary.renameTo(cache))
        }
        state
    }

    companion object {
        fun get(url: String): String {
            val uri = URI(url)
            require(uri.scheme == "https" && uri.host in setOf("www.olg.ca", "gateway.www.olg.ca"))
            val connection = uri.toURL().openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 8_000; connection.readTimeout = 10_000
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("User-Agent", "LuckyLotto/0.1.0 (FOSS Android lottery planner)")
                val status = connection.responseCode
                if (status != 200) throw IllegalStateException("OLG returned HTTP $status")
                return connection.inputStream.use { stream ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = stream.read(buffer)
                        if (count < 0) break
                        require(output.size() + count <= 3_000_000) { "Response too large" }
                        output.write(buffer, 0, count)
                    }
                    output.toString(Charsets.UTF_8.name())
                }
            } finally { connection.disconnect() }
        }
    }
}
