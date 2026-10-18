# Probability model

## Objective

A success is one individual cash award whose gross value is at least the selected threshold. Smaller cash awards are not accumulated. A free ticket has cash value zero in this objective; it is not automatically replayed, converted to cash or reinvested. The budget is an upper bound, not a required spend. Fractional dollars are retained as unspent funds because modeled plays have whole-dollar prices.

## Prize components

Within one component, prize tiers are mutually exclusive. Qualifying tier probabilities are summed. Independent components (for example four random LOTTO MAX lines) combine as `p = 1 − product(1 − p_component)`.

Standard draw probabilities use combinations/hypergeometric distributions wherever the complete rules are available. Multi-line products include each line at its actual standard ticket price. Published approximate per-play distributions are used for LIGHTNING LOTTO and CASH POP; the last digits are not treated as new precision.

POKER LOTTO instant hand counts use the 2,598,960 possible five-card hands; nightly matches use the exact hypergeometric distribution. Its nightly match probability is constant conditional on the dealt hand, permitting an independent-component model for the base play. MEGADICE instant awards use exact multiplicity counts among 6^7 dice rolls. Its nightly draw matches a random independent draw of six numbers from 39. Conditional on any printed selection, nightly match odds are the same, so this outcome is independent of the instant roll even if ticket fields were generated together.

## ENCORE

Each seven-digit winning string has 10,000,000 equally likely outcomes. For each of the 128 equality masks, a matching position has one outcome and a nonmatching position has nine. The mask has probability `9^(7 − matched_positions) / 10^7`.

The longest matching prefix and suffix determine the cash amount. Valid prefix and suffix awards are added for OLG's combination-win categories; a complete seven-digit match is one $1m prize. This prevents double-counting overlapping awards. Tests reproduce the overall approximately 1-in-9.17 odds, the 1-in-10m jackpot and combination categories such as $502, $55 and $52.

A bundle has one standard host play and `k` ENCORE plays, `0 ≤ k ≤ 10`. Cost is `host_cost + k`. Failure probability is `(1 − p_host) × (1 − p_encore)^k`. ENCORE is never offered as a standalone ticket. For daily host games it shares the corresponding midday/evening slot; for weekly host games only the evening slot is used.

## Budget optimization

Independent bundle failures multiply. Their negative logarithms add: `value = −log(1 − p)`. An unbounded knapsack dynamic program finds the largest total value for any integer spend up to the weekly budget (maximum $500). It considers every enabled modeled variant and legal ENCORE count. Exact-score ties prefer leaving money unspent. Equal-value draws are spread by existing spend, then date; there is no invented benefit to playing a particular day.

This is exact for the **specified independent random-play model**, not for arbitrary correlated number selections. Deliberately distinct number coverage, multiple CASH POP POPs, boxed number coverage across tickets, selected-number wheels, finite ENCORE number assignment and the effect of ticket duplication require different joint probability models. Multiple randomly selected tickets can duplicate numbers. The app does not select numbers.

## Shared and unknown prizes

The default counts fixed cash tiers only. An optional setting includes a shared jackpot at its advertised total (or a published minimum / cash option). This explicitly assumes the prize is not split. The resulting threshold estimate may overstate the chance of taking home X. Fixed advertised payouts can also be reduced by liability limits on certain games.

Variable pool payouts cannot be mapped to a cash threshold before sales and winning-ticket counts are known; those tiers are excluded. Gold Ball depends on selections issued, so it is excluded. LOTTARIO Early Bird is a shared $50k pool with unknown per-winner cash, so it is excluded. Promotions, POWERBUCKS instant distributions and POKER LOTTO ALL IN are not ranked. These exclusions can change which game would be best in reality; no global all-game optimality is claimed.

## Calendar and freshness

Weeks are Monday–Sunday in America/Toronto. The current-week plan includes only purchase deadlines strictly after the current instant. No money is retrospectively assigned to elapsed draws. The clock is rechecked on resume and every 30 seconds; this is a planner, not a last-second purchasing clock.

Published schedules generate the week's candidate slots. Feeds confirm the next two draw records and provide available jackpots. Only the exact corresponding purchase deadline can use a fresh live jackpot; a later draw gets no guessed rollover or MAXPLUS/MAXMILLIONS count. Live values expire six hours after a successful fetch. Partial fetches replace the live set with successful products only; failed products remain explicitly unavailable. A completely failed refresh preserves the prior successful cache and original timestamp until it expires.

Retail-only LIGHTNING LOTTO resolves at purchase; its displayed time is the daily sales deadline, not a draw time. It uses the published $125k minimum, not a fetched progressive jackpot.

## Practical interpretation

The result is a comparison of modeled chances of a qualifying prize. It is neither an expected-value calculation nor a prediction of net financial gain. Lottery draws do not become easier because of past winning numbers. Keeping unspent money has no chance of a lottery prize and avoids spending it; the planner does not optimize broader financial utility.
