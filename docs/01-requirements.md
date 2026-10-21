# 01 – Requirements

**Project:** Crypto Dashboard (Android, native Kotlin) – USTH Mobile Application Development <br>
**Version:** 1.0 | **Team:** 6 | **Duration:** ~10 working days <br>
**Scope of this file:** *what* the app must do and how we verify it. What each screen contains and how it behaves is in `02-ui-spec.md`. *How* it is built is in `03-architecture.md`, API facts are in `04-api-contract.md`, interfaces in `05-interfaces.md`, tasks in `06-task-plan.md`, tests in `07-test-plan.md`.

Priority legend: **M** = Must (demo cannot fail without it), **S** = Should (do after all M are merged), **C** = Could (only if all Must and Should items are done).

---

## 1. Purpose

### 1.1 Objectives
| ID | Objective |
|---|---|
| O1 | Let a beginner browse the top 100 cryptocurrencies and understand basic market information (price, change, market cap) at a glance. |
| O2 | Let a user inspect one coin: price history over several timeframes and key statistics. |
| O3 | Let a user keep a personal list of favorite coins that survives app restarts. |
| O4 | Stay usable when data is unavailable (no network, rate limit, missing fields) and work in USD and VND. |
| O5 | Let a beginner understand every market term and number the app shows (rank, market cap, 24h change, trend line, volume, supply, all-time high and low) without leaving the app. |

### 1.2 Target users
Beginners who want to explore the crypto market. The app is for **monitoring and learning only**. It is not a trading platform.

A beginner here is someone who has heard of cryptocurrencies but does not know market terms such as market cap, 24h change, trading volume, supply or all-time high. The app explains each term where it appears, in one short sentence, in the UI language (FR-INFO-03, FR-CD-10). It never says what to buy or sell (FR-INFO-02).

### 1.3 Demo success criteria
The scripted demo runs end to end on an emulator with a recent stable Android image (API 36), and the app is also checked on an API 24 emulator (NFR-09): Market List → tap ⓘ and read how to read the list → search/sort → open Detail → tap ⓘ next to a statistic and read its explanation → switch timeframes and touch the chart → add favorite → Watchlist → restart app, favorite still there → switch to VND → turn off the network and pull to refresh: the data already on screen stays visible with its last-updated time and a clear error message → relaunch without network: error screen with Retry.

---

## 2. Scope

### 2.1 In scope
Market List, Coin Detail, Watchlist, USD/VND currency selection, loading/empty/error states, data attribution, disclaimer.

### 2.2 Out of scope
iOS and other platforms, offline use and any persistent storage of market data (only favorite coin IDs and the currency choice are stored), accounts/login, cloud sync, portfolio, trading, price alerts, push notifications, tablet layouts, landscape orientation, search beyond the loaded top 100, in-app language switch, a custom backend, several or named watchlists (the app has exactly one watchlist), a horizontal ticker strip of coins, sharing or copying a link to a coin.

### 2.3 Constraints
- Native Android, Kotlin with XML Views (Activities/Fragments, RecyclerView, Material components; no Jetpack Compose), Android Studio. minSdk 24 (Android 7.0). Portrait orientation only.
- Market data from the CoinGecko API, free Demo plan, under the CoinGecko API Terms of Service (version of 5 Sept 2025). The app needs an internet connection.
- The app does not store CoinGecko market data on the device (ToS clause 6: storing Data is not encouraged, and copying is not allowed unless the Terms permit it; exact text in 2.5).
- The attribution "Powered by CoinGecko" is mandatory (ToS clause 4.3). No public statements about CoinGecko beyond a factual description (ToS clause 4.1.7.6).
- One shared team API key (see 2.5). The app is not published and the APK is not shared outside the team.
- No network or heavy work on the main thread; no View access from worker threads.
- Permission: `INTERNET` only (plus `ACCESS_NETWORK_STATE` if needed for connectivity detection).
- API keys are never committed to the repository.
- Delivery: ~10 days, 6 members.

### 2.4 Assumptions checked in the API spikes (details in `04-api-contract.md`)
These were assumptions before the spikes S1 (API capture) and S2 (network test). The Status column shows what the spikes found. If a result differs from the assumption, the affected requirement is updated before `05` is frozen.

| # | Assumption | Affects | Status |
|---|---|---|---|
| A1 | `days=365` (1Y) works on Demo; Demo history is limited to the past 365 days (documented). | FR-CD-03 | Confirmed (S1): `days=365` returns 366 points about 1 day apart. |
| A2 | `days=1` returns about 5-minute points, enough for 1H (about 12 points). | FR-CD-03 | Confirmed (S1): 289 points about 5 minutes apart; 13 points in the last 60 minutes. |
| A3 | `vs_currency=vnd` works on the markets, detail and chart endpoints. | FR-CUR | Confirmed (S1). |
| A4 | Which fields can be null (`max_supply`, `ath_date`, description, `sparkline_in_7d`, `price_change_percentage_24h`, …). | FR-ML-09, FR-CD-06 | Partly confirmed (S1): `max_supply` is null for 46 of 100 coins in the USD list and for ethereum and solana in detail; `roi` is null for 91 of 100; the other fields checked were not null in the sample. The list is in `04`; the edge-case files cover the rest. |
| A5 | HTTPS to the API and the image host works on an API 24 emulator and on the demo emulator. | All | Confirmed with a fix (S2): on API 24 it first failed (the device does not trust the certificate chain). It works with a network security configuration (`03` §8.1); both hosts returned 200. Not yet tried on the API 36 demo emulator. |
| A6 | Image requests (coin icons) do not count against the API call quota. | FR-ML-01 | Partly confirmed (S2): no credits were charged for icon requests, but every measured request was a cache hit, so "icons are free" cannot be told apart from "cache hits are free". |

### 2.5 Confirmed facts and decisions
| Item | Value |
|---|---|
| Rate limit | Demo account: **100 calls/minute** (account dashboard, 13 Oct 2026). Monthly allowance: 10,000 credits. |
| API key (D-KEY) | One shared team key, held by the team lead, used for integration and demo builds. Daily development uses fake data (see `fake-data.md`). The key stays in `local.properties` and is never committed. |
| Attribution | "Powered by CoinGecko", legible font, size ≥ 10, linking to https://www.coingecko.com (ToS clause 4.3). |
| Data storage | No market data is stored on the device. |
| Caching and copying (ToS) | Clause 6.1: CoinGecko does not encourage caching or storage of Data; if Data is cached it must be refreshed at least every 24 hours and protected with strong encryption. Clause 6.2: Data may not be copied or reproduced "except as expressly permitted" by the Terms. Clause 6 has no exception for development or testing; clause 3.1 licenses use of the API to develop, test and support applications. Clause 2.6 defines Data as content made available by CoinGecko (coin information, market data, statistics); it does not name images or logos. |
| Terms | API Terms of Service, version of 5 Sept 2025, read on 13 Oct 2026; clauses 2.6 and 6 re-read on 19 Oct 2026. |
| Demo environment | An emulator with a recent stable image (API 36); compatibility checked on an API 24 emulator (decided 19 Oct 2026). |
| HTTPS on Android 7.0 | Needs a network security configuration that trusts ISRG Root X1 and X2 for `coingecko.com` (S2); see `03` §8.1. |

---

## 3. User stories

| ID | Story | Requirements |
|---|---|---|
| US-1 | As a beginner, I want to see the top coins with price and 24h change so I know what the market is doing. | FR-ML-01, FR-ML-02 |
| US-2 | As a user, I want to search and sort the list so I can find a coin quickly. | FR-ML-03…05 |
| US-3 | As a user, I want to open a coin and see its price chart for different periods. | FR-CD-02…05 |
| US-4 | As a beginner, I want to see market cap, volume, ATH/ATL and supply plus a description to understand the coin. | FR-CD-06, FR-CD-07 |
| US-5 | As a user, I want to mark favorite coins and see them together, even after restarting the app. | FR-WL, FR-ML-08, FR-CD-08 |
| US-6 | As a Vietnamese user, I want prices in VND. | FR-CUR |
| US-7 | As a user on a bad connection, I want to keep seeing what I already loaded and understand what went wrong. | FR-DATA, FR-ERR |
| US-8 | As a beginner, I want a short explanation of the terms and numbers I see, so I can understand them without looking them up elsewhere. | FR-INFO-03, FR-CD-10 |

---

## 4. Functional requirements

### 4.1 Navigation (FR-NAV)
| ID | Requirement | Pri | Acceptance criteria |
|---|---|---|---|
| FR-NAV-01 | Main screen with two tabs: Market and Watchlist | M | App opens on Market. The tabs are switched with a two-segment toggle (Market and Watchlist) in the top bar; there is no bottom navigation. Switching tabs keeps each tab's state (search text, sort, scroll position). |
| FR-NAV-02 | Open Coin Detail | M | Tapping a row on Market or Watchlist opens Detail for that coin. |
| FR-NAV-03 | Back from Detail | M | Back returns to the previous tab with search text, sort and scroll position unchanged. Detail shows the same currency as the list it came from. If the currency was changed on Detail (FR-CUR-05), the list reloads in the new currency on return: search text and sort are kept, the scroll position is not guaranteed. |

### 4.2 Market List (FR-ML)
| ID | Requirement | Pri | Acceptance criteria |
|---|---|---|---|
| FR-ML-01 | Show top 100 coins | M | "Top 100" = the first 100 coins returned by the data provider ordered by market cap rank at fetch time (may include stablecoins). Each row: rank, icon, name, symbol, price, 24h % change, market cap, sparkline. Rank is the provider's rank and does not change when the user sorts. If fewer than 100 coins are returned, show what was returned without error. |
| FR-ML-02 | 7-day sparkline per row | M | Line chart without axes. Green if last value > first value, red if lower, neutral if equal or fewer than 2 points. A 24h % that is not zero is always shown with a sign next to it (a value that rounds to `0.00%` has no sign, §5.2), so colour is never the only indicator. |
| FR-ML-03 | Search | M | Filters the loaded list by name or symbol, case-insensitive, "contains" match, leading/trailing spaces ignored, applied after 300 ms of no typing. No match → empty state "No coins found". Clearing the text restores the full list. |
| FR-ML-04 | Sort | M | By price, 24h % change, or market cap. Default: market cap descending. Tapping the active criterion toggles ascending/descending; tapping another criterion starts descending. Active criterion and direction are visible. Ties are ordered by rank ascending. |
| FR-ML-05 | Search and sort compose | M | Sorting applies only to the filtered result; changing the search text keeps the current sort. |
| FR-ML-06 | Pull-to-refresh | M | Shows a refresh indicator. If the last successful fetch was less than 60 s ago, no network call is made and the indicator stops. Otherwise it fetches. Search text, sort and scroll position are preserved. On failure the existing rows stay visible and an error message is shown (FR-ERR). |
| FR-ML-07 | Open detail | M | See FR-NAV-02. |
| FR-ML-08 | Favorite star on each Market row | S | Toggles the coin in the Watchlist. State matches Detail and Watchlist immediately. If this requirement is not built, Market rows have no star; the star on Watchlist rows stays (FR-WL-01). |
| FR-ML-09 | Missing data in rows | M | Any missing field (price, %, market cap, rank, sparkline) is shown as "—" (sparkline: empty placeholder). When sorting, rows with a missing value in the sort field go last in both directions. The app never shows "null", "NaN" or a crash. |
| FR-ML-10 | Auto-refresh while visible | C | Only while the Market tab is visible; interval ≥ 5 minutes; stops when the app is in the background. |

### 4.3 Coin Detail (FR-CD)
| ID | Requirement | Pri | Acceptance criteria |
|---|---|---|---|
| FR-CD-01 | Header | M | Name, symbol, icon, current price, 24h % change (signed). |
| FR-CD-02 | Price chart | M | Line chart of price for the selected timeframe, in the selected currency. |
| FR-CD-03 | Timeframes | M | 1H, 24H, 7D, 1M, 1Y. Default 24H. Switching shows a loading state in the chart area only; the rest of the screen stays usable. If the user switches quickly, only the response for the currently selected timeframe is displayed. **1H** = points whose timestamp is within 60 minutes before the latest point of the 1-day dataset (resolution is limited by the provider, see A2). Fewer than 2 points → chart empty state "Not enough data". Data already loaded for a timeframe is reused for the freshness periods in FR-DATA-01. |
| FR-CD-04 | Touch interaction | M | Touching/dragging on the chart shows a marker at the nearest data point with its price (same format as the header) and its time. Lifting the finger hides it. The marker stays inside the chart bounds. Time format: `HH:mm` for 1H/24H, `dd/MM HH:mm` for 7D, `dd/MM/yyyy` for 1M/1Y, device time zone. |
| FR-CD-05 | Chart colour | S | Green if the last price of the period ≥ the first, red otherwise. |
| FR-CD-06 | Statistics | M | Market cap, 24h trading volume, ATH, ATL and circulating supply are always listed; a missing value shows "—". Total supply and max supply are listed only when the provider returns a value (a missing max supply can mean "no limit", so "—" would mislead). A real value of 0 is shown as 0. |
| FR-CD-07 | Description | M | Plain text (markup removed, links not clickable). Collapsed to about 4 lines with "Read more" / "Show less". No description → "No description available." Description language is English regardless of the app language. |
| FR-CD-08 | Favorite toggle | M | Adds/removes the coin; state matches the Market and Watchlist screens immediately. |
| FR-CD-09 | ATH/ATL date and % from ATH | C | Show the date and the distance from the all-time high. |
| FR-CD-10 | Short explanation of stats | S | Each statistic of FR-CD-06 (market cap, 24h trading volume, circulating supply, total supply, max supply, all-time high, all-time low) has an ⓘ icon after its label. Tapping the label area (touch target ≥ 48 dp) opens a dialog with a one-sentence, plain-language explanation of that statistic; "Got it" closes it. The text comes from string resources, English and Vietnamese (FR-UI-02). The explanation never says that a value is good or bad. |
| FR-CD-11 | Area fill under the line | S | The area between the line and the bottom of the chart is filled with the chart colour at low opacity. Dropped if the chart library cannot do it (decided in S3). |
| FR-CD-12 | Reference line | C | A dashed horizontal line at the first price of the selected period, so the user sees whether the price is above or below where the period started. |
| FR-CD-13 | Axis labels | S | Price labels (3 to 4 values) along the right edge, formatted like the header price; time labels along the bottom, formatted per FR-CD-04. |

### 4.4 Watchlist (FR-WL)
| ID | Requirement | Pri | Acceptance criteria |
|---|---|---|---|
| FR-WL-01 | Add/remove favorites | M | From Detail (M) and from the Market row star (S, FR-ML-08). Every Watchlist row has a filled star; tapping it removes the coin and the row disappears. State matches Detail and Market immediately. |
| FR-WL-02 | Persistence | M | Favorites survive closing the app and process kill. |
| FR-WL-03 | Watchlist screen | M | Shows favorites in the order they were added (oldest first), with the same row content as Market (price, 24h %, sparkline, market cap). Tap opens Detail. |
| FR-WL-04 | Data by coin | M | Data is fetched for the favorite coins themselves, not taken from the Market List, so a coin that left the top 100 is still shown. |
| FR-WL-05 | Empty state | M | No favorites → friendly message and a button that goes to the Market tab. |
| FR-WL-06 | Swipe to remove with undo | C | Swipe removes the row and shows an Undo action. |
| FR-WL-07 | Search/sort in Watchlist | C | Reuses Market List behaviour. |
| FR-WL-08 | Limit | M | Maximum 50 favorites. Adding the 51st shows "Watchlist is full (50)". |
| FR-WL-09 | Coin without data | M | If the provider returns no data for a favorite coin, its row shows the coin ID/name with "Data unavailable" and can still be removed. Other rows are not affected. |
| FR-WL-10 | Pull-to-refresh | M | Same rule as FR-ML-06 (60 s minimum). |

### 4.5 Currency (FR-CUR)
| ID | Requirement | Pri | Acceptance criteria |
|---|---|---|---|
| FR-CUR-01 | Supported currencies | M | USD and VND. Default on first launch: USD. |
| FR-CUR-02 | Selection | M | A selector at the right end of the top bar, on both the Market and the Watchlist tab (and on Detail, FR-CUR-05). The choice is saved and restored after restart. |
| FR-CUR-03 | Consistency | M | After a change, every price, market cap, volume, ATH, ATL and chart value on Market, Watchlist and Detail uses the new currency. Values come from the provider in that currency (no conversion inside the app). Percentages are unchanged. |
| FR-CUR-04 | Switching behaviour | M | Switching shows a loading state, then new data. Search text and sort are kept. Data of the other currency is never shown as the current one. If the request fails, FR-ERR-03 applies: the error state with Retry replaces the old data, and the new currency stays selected. |
| FR-CUR-05 | Selector on Detail | S | The top bar of Detail has the same selector as FR-CUR-02, showing the saved currency. Choosing the other currency saves it, as in FR-CUR-02, and Detail shows its loading state, then the price, statistics and chart in the new currency; the description and the favorite state do not change. Data of the other currency is never shown as the current one. If this requirement is not built, Detail has no selector and uses the saved currency. |

### 4.6 Data freshness (FR-DATA)
| ID | Requirement | Pri | Acceptance criteria |
|---|---|---|---|
| FR-DATA-01 | Minimum refresh intervals | M | The app does not call the network for the same data again within: market list and watchlist 60 s; chart 1H/24H 60 s; 7D 5 min; 1M/1Y 15 min; coin detail stats 60 s. A user action inside the interval (pull, reopening Detail) uses the data already held. |
| FR-DATA-02 | No stored market data | M | Market, chart and detail data are held in memory only while the app runs and are never written to device storage. Only favorite coin IDs and the currency choice are stored. |
| FR-DATA-03 | Last updated | M | Market and Watchlist show the time the data on screen was fetched (format in §5.2) at the right of the footer bar, next to a clock icon whose content description is "Last updated". The time stays visible when old data is kept after an error (FR-ERR-03). |
| FR-DATA-04 | Launch without network | M | No data and no network → an error state that fills the content area of the screen, with Retry (no empty list). |

### 4.7 States and errors (FR-ERR)
| ID | Requirement | Pri | Acceptance criteria |
|---|---|---|---|
| FR-ERR-01 | Four states | M | Every data screen (Market, Watchlist, Detail, chart area) implements loading, success, empty and error states. |
| FR-ERR-02 | Error categories | M | Each category has its own user-facing message (see table below) and a Retry action. |
| FR-ERR-03 | Keep old data | M | If a refresh of the same request fails (same currency, coin and timeframe: pull-to-refresh, or Retry while data is on screen), the data stays and the error is shown as a message (banner/Snackbar). A change of currency or timeframe is a new request: the data of the old request is removed when the change is made, and if the new request fails, the error state that fills the content area is shown with Retry, even though data was on screen before. The new currency stays selected. When there is no data, the error state that fills the content area is shown. |
| FR-ERR-04 | No retry storm | M | No automatic retry. After HTTP 429, no new network request for at least 60 s. The app never crashes on any category. |

| Category | Trigger | Message intent |
|---|---|---|
| No connection | No network, or the secure connection cannot be established | "No internet connection" |
| Timeout | No response within 15 s | "The request timed out" |
| Rate limited | HTTP 429 | "Too many requests, try again in a minute" |
| Server error | HTTP 5xx | "The data service has a problem, try later" |
| Request/key error | other HTTP 4xx (e.g. 401/403) | "Cannot load data (request rejected)" |
| Invalid data | response cannot be parsed | "Received unexpected data" |

Known limitation (decided 19 Oct 2026): the network library reports a failed secure (TLS) connection in the same way as a missing network, so in that rare case the message "No internet connection" can be misleading. Retry is offered. There is no separate category for it.

### 4.8 Information (FR-INFO)
| ID | Requirement | Pri | Acceptance criteria |
|---|---|---|---|
| FR-INFO-01 | Data attribution | M | Market, Watchlist and Detail show the text "Powered by CoinGecko" in a fixed footer bar at the bottom of the screen, in a legible font of size ≥ 10, visible without scrolling past data; tapping opens https://www.coingecko.com. Not hidden or obscured. Nothing in the app suggests CoinGecko endorses it. |
| FR-INFO-02 | Disclaimer | M | Detail shows "For information only. Not financial advice." as the last block of the screen, below the description. It is part of the Detail content: it is shown whenever Detail shows its content (also when an inline error is shown over data that is already on screen, FR-ERR-03), and it is not shown while Detail is in the full-screen loading or error state, where there is no data to explain (`02` SCR-04). |
| FR-INFO-03 | How to read the list | S | The main top bar, shared by the Market and the Watchlist tab, has an ⓘ icon at its left end (touch target ≥ 48 dp, content description "How to read this list"). Tapping it opens a dialog that explains, in one short sentence each: rank, market cap (MC), the 24h change label (sign and colour), the 7-day trend line (and that it can point a different way than the 24h change) and the star. The dialog is the same on both tabs; "Got it" closes it. The text comes from string resources, English and Vietnamese (FR-UI-02). The dialog needs no network and no data. |

### 4.9 Appearance and language
| ID | Requirement | Pri | Acceptance criteria |
|---|---|---|---|
| FR-UI-01 | Dark theme only | M | The app has a single dark theme regardless of the system setting; no light theme and no toggle. All screens, including the price chart and sparklines, meet NFR-08. |
| FR-UI-02 | UI language | S | English and Vietnamese UI strings; follows the system language, English for any other. No in-app switch. Number format does not change with language (see §5.2). |

---

## 5. Non-functional requirements

### 5.1 Table
| ID | Category | Requirement | Pri | Verification |
|---|---|---|---|---|
| NFR-01 | Performance | The first rows appear within 3 s on Wi-Fi after launch. | M | Manual stopwatch, 3 runs on the demo emulator (API 36) |
| NFR-02 | Performance | Fast fling over the 100-row list shows no visible stutter; janky frames reported by the profiler ≤ 10%. | S | Android Studio profiler / GPU rendering bars |
| NFR-03 | Threading | No network or disk work and no JSON parsing on the main thread; no View access from other threads. | M | StrictMode (debug build) shows no violations; code review |
| NFR-04 | Reliability | No crash on: no network, timeout, 429, malformed JSON, missing fields, empty list, coin not found. | M | Test plan cases for every FR-ERR category (fake responses) |
| NFR-05 | Maintainability | Layered structure; Activities/Fragments/adapters contain no networking or storage code; shared formatters used everywhere. | M | Code review checklist |
| NFR-06 | Testability | Business logic (filter, sort, mapping, formatting, 1H slicing, state holders) can run in JVM unit tests without an emulator. | M | `testDebugUnitTest` passes in CI |
| NFR-07 | Security | No API key in the repository; the build still succeeds without a local key (CI). | M | Repository check, CI green |
| NFR-08 | Accessibility | Touch targets ≥ 48×48 dp; text contrast ≥ 4.5:1; meaning not shown by colour only; icons have content descriptions. | S | Accessibility Scanner, manual check |
| NFR-09 | Compatibility | Runs on Android 7.0+ (API 24). Verified on an API 24 emulator and on the demo emulator image (API 36). | M | Demo run on both |
| NFR-10 | Build quality | Android lint reports no errors; CI runs lint and unit tests on every pull request. | M | CI |
| NFR-11 | Portrait | The app is locked to portrait. | M | Inspection |
| NFR-12 | Data source switch | The app runs on fake data from `assets/` or on the real API, chosen by a build setting. The default, including CI without a key, is fake data. At startup the app writes the active data source (FAKE or REAL) to the log; the UI shows no label. See `fake-data.md`. | M | Build and run with the setting on and off; the startup log line shows the source |
| NFR-13 | Screen sizes | Portrait layouts work on phones from 360 dp width upward (the team's choice), at the default system font size: no overlap, no main information (name, price, 24h change) cut off, nothing unreachable. Long names are shortened with an ellipsis; layouts use dp/sp and flexible widths. Rules in `03-architecture.md`, section 11. | S | Emulators: 360×640 dp and 411×891 dp |

### 5.2 Formatting rules (fixed locale, independent of device language)
Grouping separator `,` and decimal separator `.` for both currencies.

| Item | Rule | Examples |
|---|---|---|
| USD price | ≥ 1: 2 decimals; 0.01 to < 1: 4 decimals; < 0.01: 8 decimals | `$67,234.50`, `$0.2345`, `$0.00001234` |
| VND price | ≥ 1,000: 0 decimals; 1 to < 1,000: 2 decimals; < 1: 6 decimals; symbol `₫` after the number | `1,650,000 ₫`, `312.50 ₫` |
| Compact numbers (market cap, volume, supply) | 2 decimals. From 1,000 upward the number is divided and gets the suffix K, M, B or T; below 1,000 there is no suffix; a value of 0 shows `0`. The suffix is chosen after rounding, so 999,996 shows `1.00M`, not `1000.00K`. Market cap and volume carry the currency symbol as above; supply is a count of coins and has no currency symbol | `$1.23B`, `340.00M`, `32.50T ₫`, `$523.40`, `1.00M` |
| Percent | Signed, 2 decimals. Round first, then choose the sign: a value that rounds to zero is shown as `0.00%` with no sign (never `+0.00%` or `-0.00%`) | `+2.35%`, `-0.40%`, `0.00%` |
| Missing value | `—` | |
| Last-updated time | `HH:mm dd/MM` in device time zone | `14:32 13/10` |

Every rounding in this table is half up: a value exactly halfway rounds away from zero (0.005 becomes 0.01). The decimal rules are the team's proposal; they may be adjusted after the spike shows real price ranges, but must stay in this table.

---

## 6. Priority summary (10 days)

- **Must:** FR-NAV-01…03, FR-ML-01…07 and 09, FR-CD-01…04, 06…08, FR-WL-01…05, 08…10, FR-CUR-01…04, FR-DATA-01…04, FR-ERR-01…04, FR-INFO-01…02, FR-UI-01, NFR-01, 03…07, 09…12.
- **Should:** FR-ML-08, FR-CD-05, FR-CD-10, FR-CD-11, FR-CD-13, FR-CUR-05, FR-INFO-03, FR-UI-02, NFR-02, NFR-08, NFR-13.
- **Could:** FR-ML-10, FR-CD-09, FR-CD-12, FR-WL-06, FR-WL-07.

If time runs short, cut in this order: Could → FR-CUR-05 → FR-CD-11 → FR-CD-13 → FR-UI-02 → FR-CD-05 → FR-ML-08 → FR-INFO-03 → FR-CD-10. The two explanation items are cut last because the target users are beginners (§1.2).

---

## 7. Traceability

Every FR and NFR above gets at least one test case in `07-test-plan.md`, named by its ID. Every pull request description lists the requirement IDs it implements. Objectives map as: O1 → FR-ML; O2 → FR-CD; O3 → FR-WL; O4 → FR-CUR, FR-DATA, FR-ERR; O5 → FR-INFO-03, FR-CD-10.

---

## 8. Glossary
- **Sparkline** – small line chart without axes showing a trend.
- **ATH / ATL** – all-time high / all-time low price.
- **Market cap** – price × circulating supply.
- **Circulating supply** – coins currently available on the market.
- **Top 100** – see FR-ML-01.
