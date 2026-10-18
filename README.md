# LuckyLotto 🍀

A free, open-source, native Android weekly planner for Ontario lottery games. Set a weekly budget and a cash-prize target; compare standard plays and legal ENCORE bundles; inspect the resulting day-by-day plan and the assumptions behind it.

**Status: working 0.1.0 beta.** This is an auditable model, not an exact all-game winning-probability service. Some OLG feeds require access that is not available to an anonymous client, and several prize amounts depend on future sales or other winners. The app shows that uncertainty and excludes unquantifiable tiers. It does not claim to predict winning numbers.

## Android app

- Kotlin + Jetpack Compose; **Material 3 1.4.0**, the latest stable Material 3 release verified on October 18, 2026.
- Material You wallpaper colours on Android 12+, light/dark/system themes, edge-to-edge layout.
- Android 8.0+ (API 26); targets API 36. No Play Services required.
- Weekly CAD budget ($0–$500), prize target ($0.01–$100 million), game filters and optional ENCORE.
- This week / next week, remaining draws only, Toronto time and daylight-saving rules.
- Day selector with ticket counts, purchase cutoffs, ENCORE counts, daily costs and modeled chance.
- Game comparison, prize tiers, source links, data timestamps and feed failures.
- Local preferences and public-data cache; no login, ads, analytics, paid services or purchases.

## What “win at least X” means

At least **one individual cash prize** at or above X. Smaller prizes across tickets or the week are not added together. Amounts are gross cash awards, not net profit. Free plays do not qualify.

The optimizer maximizes `1 − product(1 − p)` for random independent standard plays, with an integer-budget dynamic program. ENCORE bundles include the host-game price and 0–10 $1 add-ons per ticket. Read [the methodology](docs/METHODOLOGY.md) before treating a result as a spending recommendation.

## Game coverage

| Family | What is modeled | Important limits |
| --- | --- | --- |
| LOTTO MAX | Current $6 / four 7-of-52 lines; fixed $20 tiers; live jackpot, MAXPLUS and MAXMILLIONS with shared-prize option | Variable pool tiers excluded; announced amounts apply only to that draw |
| LOTTO 6/49 | Classic fixed cash tiers; $5m shared Classic jackpot optionally | Gold Ball and variable Classic pools excluded |
| ONTARIO 49 | All standard cash tiers; shared top tiers optionally | Split prizes can fall below target |
| LOTTARIO | Two standard lines and fixed cash tiers; minimum/live shared main jackpot optionally | Early Bird excluded because per-winner share is unknown |
| DAILY GRAND | Standard cash tiers; shared cash options optionally | No valuation of a lifetime annuity |
| PICK-2 | $2 play, $99 / $2 awards | Published rules |
| PICK-3 | $1 straight-play host for ENCORE | Its sales-dependent cash prizes excluded |
| PICK-4 | Straight and 4/6/12/24-way boxes | Liability caps can reduce awards |
| DAILY KENO | All 2–10 pick variants and $1/$2/$5/$10 wagers; exact 20-of-70 match probabilities | Liability caps can reduce awards |
| CASH POP | All $2/$5/$10/$20 single-POP wagers and published prize distributions | Random single-POP plays; distinct multi-POP coverage not optimized |
| POKER LOTTO | Base $2 instant hand plus nightly draw | ALL IN and free plays excluded |
| MEGADICE LOTTO | Exact seven-die instant cash tiers and nightly draw | Liability caps may reduce nightly awards |
| LIGHTNING LOTTO | Published per-play approximate odds, $125k minimum jackpot | Instant retail game, modeled at its daily purchase deadline; live progressive amount unavailable |
| POWERBUCKS / promotions | Catalogue entries and official links | Excluded until distributions and dated rules are verified |

Scratch tickets, casino games, sports betting, combination-number tickets and quick-pick promotional packs are outside this first version. The optimizer is optimal **within its disclosed modeled candidates**, not across all possible lottery strategies.

## Live data

The app makes anonymous HTTPS requests directly to the public OLG draw endpoint used by OLG.ca. No embedded website API keys, user credentials or private interfaces.

In this development environment the LOTTO MAX and 6/49 requests succeeded; eight other requested product feeds returned HTTP 401. Each refresh preserves that distinction. Published recurring schedules and the shipped rules still support planning; unavailable jackpot amounts are not invented. All jackpots age out of calculations after six hours. Future rollovers are not copied from the current jackpot. Test fixtures are recorded examples, never presented as live data.

Rule tables are **reviewed and shipped with the app**, not dynamically scraped into trusted calculations. A rule change needs review, tests and an app update. [Sources and review date](docs/SOURCES.md).

## Build and test

Install JDK 21, Android SDK Platform 36 and the Android build tools; open this directory in Android Studio, or run:

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

The Gradle wrapper downloads Gradle 9.3.1. Set `ANDROID_HOME` or your untracked `local.properties` to your Android SDK location. First build needs access to Google Maven and Maven Central.

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`. It is signed with a development key and is suitable for sideload testing, not a production release. The GitHub Actions workflow builds and uploads a debug APK on pushes, pull requests and manual runs.

Release preparation:

```sh
./gradlew assembleRelease
```

A production release requires an owner-controlled signing key. Do not commit signing keys or passwords. [Release checklist](docs/RELEASING.md).

See [verification results and remaining limitations](docs/VERIFICATION.md).

## Repository layout

```text
app/src/main/java/org/luckylotto/
  domain/   game rules, exact ENCORE enumeration, odds, budget optimizer
  data/     anonymous OLG adapter, local settings/cache
  ui/       Material 3 screens, theme and application state
app/src/test/  numeric, optimizer, time-zone and recorded-feed tests
docs/         methodology, sources, release notes and verification
```

## Contributing and license

Licensed **GPL-3.0-or-later**. Copyright © 2026 LuckyLotto contributors. The source and modification rights stay available to users of distributed builds. See [LICENSE](LICENSE), [CONTRIBUTING](CONTRIBUTING.md) and [NOTICE](NOTICE).

Independent project; not affiliated with or endorsed by OLG. OLG and lottery-game names are their owners’ marks. Lottery participation in Ontario is for adults 18+.
