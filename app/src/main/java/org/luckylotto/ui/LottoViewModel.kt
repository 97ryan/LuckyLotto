// SPDX-License-Identifier: GPL-3.0-or-later
package org.luckylotto.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.luckylotto.data.LottoRepository
import org.luckylotto.domain.*
import java.time.Instant

data class LottoState(
    val settings: PlanSettings, val feed: FeedState = FeedState(), val plan: WeeklyPlan? = null,
    val refreshing: Boolean = false, val calculating: Boolean = false,
    val now: Instant = Instant.now(), val theme: String = "system", val dynamicColor: Boolean = true,
)

class LottoViewModel(app: Application): AndroidViewModel(app) {
    private val repository = LottoRepository(app)
    private val mutable = MutableStateFlow(LottoState(repository.readSettings(), repository.cached(),
        theme = repository.theme(), dynamicColor = repository.dynamicColor()))
    val state: StateFlow<LottoState> = mutable.asStateFlow()
    private var computeJob: Job? = null

    init {
        calculate()
        refresh()
        viewModelScope.launch { while (true) { delay(30_000); tick() } }
    }
    fun tick() { mutable.update { it.copy(now = Instant.now()) }; calculate() }
    fun settings(transform: (PlanSettings) -> PlanSettings) {
        val next = transform(mutable.value.settings)
        require(next.budgetCents in 0..50_000 && next.targetCents in 1..10_000_000_000L)
        repository.saveSettings(next)
        mutable.update { it.copy(settings = next) }; calculate()
    }
    fun theme(value: String) { repository.setTheme(value); mutable.update { it.copy(theme = value) } }
    fun dynamicColor(value: Boolean) { repository.setDynamicColor(value); mutable.update { it.copy(dynamicColor = value) } }
    fun refresh() {
        if (mutable.value.refreshing) return
        mutable.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            val result = runCatching { repository.refresh() }
            mutable.update { it.copy(feed = result.getOrElse { _ -> it.feed }, refreshing = false, now = Instant.now()) }
            calculate()
        }
    }
    private fun calculate() {
        computeJob?.cancel()
        computeJob = viewModelScope.launch {
            mutable.update { it.copy(calculating = true) }
            val snapshot = mutable.value
            val plan = withContext(Dispatchers.Default) { Planner.optimize(snapshot.settings, snapshot.feed, snapshot.now) }
            ensureActive()
            mutable.update { it.copy(plan = plan, calculating = false) }
        }
    }
}
