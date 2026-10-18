// SPDX-License-Identifier: GPL-3.0-or-later
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package org.luckylotto.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.luckylotto.domain.*
import org.luckylotto.data.OlgFeed
import java.text.NumberFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.*

fun money(cents: Long): String = NumberFormat.getCurrencyInstance(Locale.CANADA).format(cents / 100.0)
fun dollars(value: Int): String = money(value * 100L).removeSuffix(".00")
fun percent(p: Double): String = when { p <= 0 -> "0%"; p < 0.000001 -> "%.7f%%".format(Locale.CANADA, p * 100); p < 0.001 -> "%.4f%%".format(Locale.CANADA,p * 100); else -> "%.2f%%".format(Locale.CANADA,p * 100) }
fun odds(p: Double): String = if (p <= 0) "No qualifying cash prize modeled" else "1 in ${NumberFormat.getNumberInstance(Locale.CANADA).apply { maximumFractionDigits = 1 }.format(1 / p)}"
private val dateFormat = DateTimeFormatter.ofPattern("MMM d", Locale.CANADA)
private val timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.CANADA)
private fun family(g: Game) = g.id.substringBefore('_')
private fun open(context: android.content.Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

@Composable
fun LottoApp(model: LottoViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var detail by remember { mutableStateOf<Game?>(null) }
    LuckyTheme(state.theme, state.dynamicColor) {
        Scaffold(
            topBar = {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 22.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Forest, shape = RoundedCornerShape(15.dp), modifier = Modifier.size(42.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Spa, null, tint = Lime, modifier = Modifier.size(25.dp)) }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("LuckyLotto", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = model::refresh, enabled = !state.refreshing) {
                        if (state.refreshing) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Outlined.Refresh, "Refresh OLG draw data")
                    }
                }
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    listOf(Triple("Plan",Icons.Outlined.CalendarMonth,0), Triple("Games",Icons.Outlined.GridView,1), Triple("Settings",Icons.Outlined.Tune,2)).forEach { (label, icon, index) ->
                        NavigationBarItem(selected = tab == index, onClick = { tab = index }, icon = { Icon(icon, label) }, label = { Text(label) })
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when(tab) {
                    0 -> PlanScreen(state, model, onGame = { detail = it }, onCompare = { tab = 1 })
                    1 -> GamesScreen(state, model, onGame = { detail = it })
                    else -> SettingsScreen(state, model)
                }
            }
        }
        detail?.let { GameSheet(it, state, onDismiss = { detail = null }) }
    }
}

@Composable
private fun PlanScreen(state: LottoState, model: LottoViewModel, onGame: (Game) -> Unit, onCompare: () -> Unit) {
    var budget by rememberSaveable { mutableStateOf((state.settings.budgetCents / 100.0).toBigDecimal().stripTrailingZeros().toPlainString()) }
    var target by rememberSaveable { mutableStateOf((state.settings.targetCents / 100.0).toBigDecimal().stripTrailingZeros().toPlainString()) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    val budgetCents = parseCents(budget)
    val targetCents = parseCents(target)
    val validBudget = budgetCents != null && budgetCents in 0..50_000
    val validTarget = targetCents != null && targetCents in 1..10_000_000_000L
    val dirty = budgetCents != state.settings.budgetCents.toLong() || targetCents != state.settings.targetCents
    val plan = state.plan
    var selectedDay by rememberSaveable(state.settings.nextWeek) { mutableIntStateOf(if (state.settings.nextWeek) 0 else state.now.atZone(Ontario).dayOfWeek.value - 1) }
    LazyColumn(contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Eyebrow("A LITTLE LUCK. A CLEARER PLAN.")
            Spacer(Modifier.height(8.dp))
            Text("Your week,\nwell planned.", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!state.settings.nextWeek, onClick = { model.settings { it.copy(nextWeek = false) } }, label = { Text("This week") })
                FilterChip(state.settings.nextWeek, onClick = { model.settings { it.copy(nextWeek = true) } }, label = { Text("Next week") })
            }
        }
        item {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Set your limits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = budget, onValueChange = { budget = it }, modifier = Modifier.weight(1f), label = { Text("Weekly budget") }, prefix = { Text("$") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), isError = attempted && !validBudget, shape = RoundedCornerShape(16.dp))
                        OutlinedTextField(value = target, onValueChange = { target = it }, modifier = Modifier.weight(1f), label = { Text("Prize target") }, prefix = { Text("$") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), isError = attempted && !validTarget, shape = RoundedCornerShape(16.dp))
                    }
                    Text("One cash prize of at least your target. All amounts in CAD.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (attempted && (!validBudget || !validTarget)) Text("Enter a budget from $0 to $500 and a target from $0.01 to $100 million (up to 2 decimals).", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Button(onClick = {
                        attempted = true
                        if (validBudget && validTarget) {
                            model.settings { it.copy(budgetCents = budgetCents!!.toInt(), targetCents = targetCents!!) }
                        }
                    }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = !state.calculating) {
                        Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                        Text(if (dirty) "Update my plan" else "Build my plan")
                    }
                    if (dirty && plan != null) Text("Update to apply these amounts. The plan below uses ${money(state.settings.budgetCents.toLong())} / ${money(state.settings.targetCents)}.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { FeedBanner(state) }
        if (state.calculating && plan == null) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        plan?.let { weekly ->
            item { ResultCard(weekly) }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Your daily plan", style = MaterialTheme.typography.titleLarge)
                        Text("${weekly.starts.format(dateFormat)} – ${weekly.starts.plusDays(6).format(dateFormat)} • Eastern Time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onCompare) { Text("Compare") }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    (0..6).forEach { i ->
                        val date = weekly.starts.plusDays(i.toLong())
                        val has = weekly.lines.any { it.ticket.at.atZone(Ontario).toLocalDate() == date }
                        val active = i == selectedDay
                        Surface(onClick = { selectedDay = i }, modifier = Modifier.weight(1f).semantics { contentDescription = "${date.dayOfWeek} ${date.dayOfMonth}, ${if (has) "purchases planned" else "no purchases"}" },
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(date.dayOfWeek.name.take(1), style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(5.dp)); Text("${date.dayOfMonth}", fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(7.dp)); Box(Modifier.size(4.dp).clip(CircleShape).background(if (has) (if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary) else Color.Transparent))
                            }
                        }
                    }
                }
            }
            val date = weekly.starts.plusDays(selectedDay.toLong())
            val dayLines = weekly.lines.filter { it.ticket.at.atZone(Ontario).toLocalDate() == date }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(date.format(DateTimeFormatter.ofPattern("EEEE, MMM d",Locale.CANADA)), modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text(dollars(dayLines.sumOf { it.cost }), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
            if (dayLines.isEmpty()) item {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.EventAvailable, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(12.dp)); Text(if (date < state.now.atZone(Ontario).toLocalDate()) "This day has passed. Only future draws are planned." else "No purchase planned. Your budget is allocated to other draws or left unspent.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            items(dayLines) { line -> TicketCard(line, onClick = { onGame(line.ticket.game) }) }
            item {
                Text("Estimates use random, independent plays. A larger jackpot does not change the chance of matching numbers. Buying every day is not required.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ResultCard(plan: WeeklyPlan) {
    Surface(color = Forest, contentColor = Color.White, shape = RoundedCornerShape(30.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Insights, null, tint = Lime, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                Text("BEST MODELED PLAN", style = MaterialTheme.typography.labelMedium, letterSpacing = 1.sp, color = Lime)
            }
            Text(percent(plan.probability), fontSize = 50.sp, lineHeight = 56.sp, fontWeight = FontWeight.Medium, letterSpacing = (-2).sp)
            Text("chance of at least one ${money(plan.settings.targetCents)}+ cash prize", style = MaterialTheme.typography.titleMedium)
            Text("${odds(plan.probability)} • ${percent(1 - plan.probability)} chance of missing the target", style = MaterialTheme.typography.bodySmall, color = Color(0xFFCCE3D5))
            HorizontalDivider(color = Color.White.copy(alpha = .18f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat("PLANNED", money(plan.spentCents.toLong()))
                Stat("LEFT OVER", money((plan.settings.budgetCents - plan.spentCents).toLong()))
                Stat("PLAYS", "${plan.lines.sumOf { it.count }}")
            }
            Text(if (plan.settings.includeShared) "Shared jackpot amounts assume no split. Unknown pool tiers remain excluded." else "Fixed cash tiers only. Shared jackpots, unknown pool tiers and free plays are excluded.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFCCE3D5))
            if (plan.lines.isEmpty()) Text("No eligible modeled plays meet this target and budget. Try another target, week, or game selection.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
@Composable private fun Stat(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(label, fontSize = 10.sp, letterSpacing = 1.sp, color = Lime); Text(value, fontWeight = FontWeight.SemiBold, fontSize = 20.sp) }
}
@Composable private fun Eyebrow(text: String) { Text(text, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.4.sp, color = MaterialTheme.colorScheme.primary) }

@Composable
private fun TicketCard(line: PlanLine, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLowest, border = BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GameMark(line.ticket.game.shortName)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(line.ticket.game.name, fontWeight = FontWeight.SemiBold)
                    val time = line.ticket.at.atZone(Ontario).format(timeFormat)
                    Text(if (line.ticket.game.id == "LTNING") "Buy in store before $time ET" else "Buy before $time ET", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(dollars(line.cost), fontWeight = FontWeight.SemiBold)
            }
            Text("${line.count} × ${dollars(line.ticket.game.price)} play" + if (line.ticket.encoreCount > 0) " + ${line.ticket.encoreCount} ENCORE per play" else " • no ENCORE", style = MaterialTheme.typography.bodyMedium)
            Text("${percent(line.probability)} modeled chance for these plays", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
@Composable private fun GameMark(name: String) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(14.dp), modifier = Modifier.size(44.dp)) {
        Box(contentAlignment = Alignment.Center) { Text(when(name) { "LOTTO MAX" -> "MAX"; "LOTTO 6/49" -> "6/49"; "ONTARIO 49" -> "49"; "DAILY KENO" -> "K"; "DAILY GRAND" -> "DG"; else -> name.take(2) }, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
    }
}
@Composable
private fun FeedBanner(state: LottoState) {
    val live = state.feed.fresh(state.now)
    val count = if (live) state.feed.draws.map { it.gameId }.distinct().size else 0
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (live) Icons.Outlined.CloudDone else Icons.Outlined.CloudOff, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(if (state.refreshing) "Checking OLG draw data…" else if (live) "$count of ${OlgFeed.products.size} draw feeds refreshed" else "Using published schedules & saved rules", style = MaterialTheme.typography.labelLarge)
                Text(if (live) "Other draws use published schedules. Unavailable jackpots are not treated as live." else "Live jackpots unavailable or older than 6 hours. Refresh to check OLG.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun GamesScreen(state: LottoState, model: LottoViewModel, onGame: (Game) -> Unit) {
    val families = Catalog.games.groupBy { family(it) }
    val ranked = state.plan?.comparisons ?: emptyList()
    val order = ranked.map { family(it.game) }.distinct()
    val sorted = families.entries.sortedBy { order.indexOf(it.key).let { i -> if (i < 0) Int.MAX_VALUE else i } }
    LazyColumn(contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Eyebrow("MAKE AN INFORMED PICK")
            Spacer(Modifier.height(8.dp)); Text("The games,\nby the numbers.", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(10.dp)); Text("Compared for a ${money(state.settings.targetCents)}+ cash prize, by modeled chance per dollar. Tap a game for rules and prize tiers.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { FeedBanner(state) }
        items(sorted, key = { it.key }) { (id, variants) ->
            val best = ranked.firstOrNull { family(it.game) == id }
            val game = best?.game ?: variants.first()
            val enabled = variants.any { it.id in state.settings.enabledGames }
            val nextDraw = if (state.feed.fresh(state.now)) state.feed.draws.filter { it.gameId == id && it.closesAt > state.now }.minByOrNull { it.closesAt } else null
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLowest, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GameMark(game.shortName); Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f).clickable { onGame(game) }) {
                            Text(game.shortName, fontWeight = FontWeight.SemiBold)
                            Text(if (variants.size > 1) "${variants.size} play options" else "${dollars(game.price)} per play", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(enabled, onCheckedChange = { checked -> model.settings { s -> s.copy(enabledGames = if (checked) s.enabledGames + variants.filter { it.supported }.map { it.id }.toSet() else s.enabledGames - variants.map { it.id }.toSet()) } }, enabled = game.supported, modifier = Modifier.semantics { contentDescription = "Include ${game.shortName} in plan" })
                    }
                    if (nextDraw?.jackpot != null) Text("Next advertised jackpot ${money(nextDraw.jackpot * 100)}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    if (nextDraw?.goldBall != null) Text("Gold Ball: ${money(nextDraw.goldBall * 100)} • sales-dependent odds excluded", style = MaterialTheme.typography.bodySmall)
                    if (nextDraw != null && nextDraw.maxPlus > 0) Text("${nextDraw.maxPlus} MAXPLUS • ${nextDraw.maxMillions} MAXMILLIONS", style = MaterialTheme.typography.bodySmall)
                    if (best != null) {
                        Text(best.game.name, style = MaterialTheme.typography.bodySmall)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${percent(best.probability)} per ${dollars(best.cost)} bundle", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                            Text(if (best.encoreCount > 0) "+ ${best.encoreCount} ENCORE" else "No ENCORE", style = MaterialTheme.typography.labelMedium)
                        }
                    } else Text(if (!game.supported) "Not ranked • model unavailable" else if (!enabled) "Excluded from your plan" else "No qualifying modeled prize or draw within this budget/week", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { onGame(game) }, contentPadding = PaddingValues(0.dp)) { Text("Prizes & source"); Spacer(Modifier.width(6.dp)); Icon(Icons.AutoMirrored.Outlined.OpenInNew, null,Modifier.size(14.dp)) }
                }
            }
        }
        item { Text("Coverage includes the current OLG draw-game catalogue. POWERBUCKS, promotions and unmodeled prize tiers are disclosed instead of guessed. Scratch tickets and casino games are outside the weekly draw planner.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun SettingsScreen(state: LottoState, model: LottoViewModel) {
    val context = LocalContext.current
    LazyColumn(contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Eyebrow("YOUR PLAN. YOUR PREFERENCES."); Spacer(Modifier.height(8.dp)); Text("Keep it simple.", style = MaterialTheme.typography.displaySmall) }
        item {
            SettingsCard("Planning") {
                Toggle("Consider ENCORE", "Compare 0–10 ENCORE plays per eligible host ticket. Each costs $1.",state.settings.allowEncore) { checked -> model.settings { it.copy(allowEncore = checked) } }
                HorizontalDivider()
                Toggle("Include shared jackpots", "Use advertised jackpot / cash-option amounts before splits. Unknown pool payouts and Gold Ball remain excluded.",state.settings.includeShared) { checked -> model.settings { it.copy(includeShared = checked) } }
            }
        }
        item {
            SettingsCard("Appearance") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system" to "Auto", "light" to "Light", "dark" to "Dark").forEach { (value,label) -> FilterChip(state.theme == value, onClick = { model.theme(value) }, label = { Text(label) }) }
                }
                Toggle("Wallpaper colours", "Use Android’s Material You palette on Android 12 and later.",state.dynamicColor, model::dynamicColor)
            }
        }
        item {
            SettingsCard("How the math works") {
                Text("The goal is at least one individual cash prize at or above your target. Smaller prizes are not added together. Winnings are gross prizes, before subtracting ticket costs.")
                Text("For each legal bundle, the model combines host-game and ENCORE failure probabilities. A budget optimizer chooses the combination with the lowest modeled chance of missing your target.")
                Text("Random independent plays are assumed. Multi-line games include every standard line. ENCORE is calculated from all 128 digit-match patterns. Published rounded odds are used where exact rules are unavailable.")
                Text("Free plays, variable pool tiers, Gold Ball, Early Bird and unsupported promotions are excluded. CASH POP models one random POP per play; choosing distinct POPs on a ticket needs a separate coverage model.")
                Text("These are estimates across modeled options, not exact odds across every OLG game or a guarantee of profit. Liability limits and shared prizes may reduce payouts. Your budget is a ceiling; unspent money stays unspent.")
            }
        }
        item {
            SettingsCard("Data & privacy") {
                Text("OLG draw data refreshes when the app opens or when you tap Refresh. After 6 hours, live jackpots stop being used. Other dates follow published weekly schedules; future rollovers are never guessed.")
                Text("Rules reviewed October 18, 2026. The rules catalogue ships with the app and needs an app update when OLG changes a game. No account, analytics, ads, purchases or location permission. Preferences and cached public draw data stay on your device.")
                state.feed.fetchedAt?.let { Text("Last successful refresh: ${it.atZone(Ontario).format(DateTimeFormatter.ofPattern("MMM d, h:mm a 'ET'",Locale.CANADA))}", style = MaterialTheme.typography.bodySmall) }
                if (state.feed.errors.isNotEmpty()) Text("Unavailable feeds: " + state.feed.errors.entries.joinToString { "${it.key} (${it.value})" }, style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            SettingsCard("Free, open & independent") {
                Text("LuckyLotto 0.1.0 • GPL-3.0-or-later\nIndependent project, not affiliated with OLG. Lottery is for adults 18+ in Ontario.")
                OutlinedButton(onClick = { open(context,"https://www.olg.ca/en/lottery/top-games.html") }) { Text("Official OLG game information"); Spacer(Modifier.width(8.dp)); Icon(Icons.AutoMirrored.Outlined.OpenInNew,null,Modifier.size(16.dp)) }
                TextButton(onClick = { open(context,"https://www.olg.ca/en/finding-help.html") }) { Text("PlaySmart & finding help") }
            }
        }
    }
}
@Composable private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); content() }
    }
}
@Composable private fun Toggle(title: String, description: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Medium); Spacer(Modifier.height(4.dp)); Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Switch(checked, onCheckedChange = change, modifier = Modifier.semantics { contentDescription = title })
    }
}

@Composable
private fun GameSheet(game: Game, state: LottoState, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val variants = Catalog.games.filter { family(it) == family(game) }
    var selected by remember(game.id) { mutableStateOf(game) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 36.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text(selected.name, style = MaterialTheme.typography.headlineSmall); Text("${dollars(selected.price)} per standard play • CAD", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (variants.size > 1) item {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }) { Text("Choose play option"); Icon(Icons.Outlined.ExpandMore,null) }
                    DropdownMenu(expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 360.dp)) {
                        variants.forEach { v -> DropdownMenuItem(text = { Text(v.name) }, onClick = { selected = v; expanded = false }) }
                    }
                }
            }
            item { if (selected.note.isNotBlank()) Text(selected.note); Text("Published rules reviewed Oct 18, 2026. Prize odds below are per component; free plays and unknown cash amounts do not qualify.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            selected.components.distinctBy { it.tiers }.forEach { component ->
                item { Text(component.name, fontWeight = FontWeight.SemiBold) }
                items(component.tiers) { tier ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(tier.name, style = MaterialTheme.typography.bodyMedium)
                            Text(when(tier.kind) { PrizeKind.FREE_PLAY -> "Free play • excluded"; PrizeKind.VARIABLE -> "Unknown cash payout • excluded"; PrizeKind.SHARED -> "Shared • ${if (state.settings.includeShared) "counted before splits" else "excluded"}"; else -> tier.dollars?.let { money(it * 100) } ?: "Unknown" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(odds(tier.probability), style = MaterialTheme.typography.labelMedium)
                    }
                    HorizontalDivider(Modifier.padding(top = 8.dp))
                }
            }
            if (selected.encore) item {
                Text("ENCORE • $1 each", fontWeight = FontWeight.SemiBold)
                Text("${percent(Encore.chance(state.settings.targetCents))} chance per ENCORE of a ${money(state.settings.targetCents)}+ prize. Up to 10 per host ticket; the planner charges the host game too.")
            }
            item { Button(onClick = { open(context,selected.source) }, modifier = Modifier.fillMaxWidth()) { Text("View official OLG prizes & rules"); Spacer(Modifier.width(8.dp)); Icon(Icons.AutoMirrored.Outlined.OpenInNew,null,Modifier.size(16.dp)) } }
        }
    }
}
