# Reviewed primary sources

Reviewed October 18, 2026. Monetary values are CAD. Game facts are encoded independently; no OLG branding or website assets are bundled.

- [OLG current game catalogue](https://www.olg.ca/en/lottery/top-games.html)
- [LOTTO MAX current rules and $6 / four-line format](https://www.olg.ca/en/lottery/play-lotto-max-encore/how-to-play.html)
- [LOTTO MAX odds](https://www.olg.ca/en/lottery/play-lotto-max-encore/odds-and-payouts.html)
- [LOTTO 6/49 current Classic / Gold Ball payouts](https://www.olg.ca/en/lottery/play-lotto-649-encore/past-results.html) — the older `/odds-and-payouts.html` contains obsolete pool proportions; current Classic has a fixed $5m shared jackpot.
- [ONTARIO 49 payouts](https://www.olg.ca/en/lottery/play-ontario-49-encore/past-results.html)
- [LOTTARIO payouts and Early Bird](https://www.olg.ca/en/lottery/play-lottario-encore/odds-and-payouts.html)
- [DAILY GRAND payouts](https://www.olg.ca/en/lottery/play-daily-grand-encore/past-results.html)
- [ENCORE 22 prize categories](https://www.olg.ca/en/lottery/play-encore/odds-and-payouts.html)
- [ENCORE 1–10 add-ons and draw matching](https://www.olg.ca/en/lottery/play-encore/about.html)
- [PICK-2 cost and schedule](https://www.olg.ca/en/lottery/play-pick-2/about.html), [payouts](https://www.olg.ca/en/lottery/play-pick-2/odds-and-payouts.html)
- [PICK-3 variable pool and methods](https://www.olg.ca/en/lottery/play-pick-3/odds-and-payouts.html)
- [PICK-4 boxes and liability limits](https://www.olg.ca/en/lottery/play-pick-4/odds-and-payouts.html)
- [DAILY KENO 70-number field, wagers and times](https://www.olg.ca/en/lottery/play-daily-keno-encore/about.html), [payouts](https://www.olg.ca/en/lottery/play-daily-keno-encore/odds-and-payouts.html)
- [CASH POP distributions for each wager](https://www.olg.ca/en/lottery/play-cash-pop/past-results.html#cash-pop-odds), [draw times](https://www.olg.ca/en/lottery/play-cash-pop/about.html)
- [POKER LOTTO](https://www.olg.ca/en/lottery/play-poker-lotto/about.html), [hand and nightly payouts](https://www.olg.ca/en/lottery/play-poker-lotto/odds-and-payouts.html)
- [MEGADICE LOTTO payouts](https://www.olg.ca/en/lottery/play-megadice-lotto/odds-and-payouts.html)
- [LIGHTNING LOTTO odds](https://www.olg.ca/en/lottery/play-lightning-lotto/odds-and-payouts.html), [daily sales hours](https://www.olg.ca/en/lottery/play-lightning-lotto/about.html)
- [POWERBUCKS official information](https://www.olg.ca/en/lottery/play-powerbucks-watch-n-win/about.html)
- [Official Material 3 release notes](https://developer.android.com/jetpack/androidx/releases/compose-material3) — stable 1.4.0 verified; 1.5.0 was beta.

Public feed request format, observed from OLG.ca's public web client:

`https://gateway.www.olg.ca/bede-middleware/lottery/drawinformation?productId=LMAX&startingDrawNumber=0&numberOfDraws=2`

No credentials are sent. Treat schema changes, HTTP errors and missing jackpots as unavailable data. The included 649 / LMAX JSON files are recorded parser fixtures and must not be shipped as fresh results.
