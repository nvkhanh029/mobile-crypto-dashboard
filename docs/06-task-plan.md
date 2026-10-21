# 06 – Task Plan

**Project:** Crypto Dashboard (Android, native Kotlin, XML Views) | **Team:** 6 | **Duration:** ~10 working days (day-based plan; no fixed calendar dates because members' schedules vary) <br>
**Related:** `01-requirements.md` (requirement IDs), `02-ui-spec.md` (what each screen contains), `fake-data.md` (development without the real API), `05-interfaces.md` (contracts).

Principles:
1. **Each member owns one feature** and is responsible for understanding and finishing its whole flow: UI, state, data, tests.
2. **The team agrees on architecture and interfaces first.** Features talk to each other only through the contracts in section 3.
3. **Shared parts have one clear owner**, and everyone else must know how to use them.
4. **At integration, each member must explain their feature**: how it works, where its data comes from, and why it is designed that way (see section 6).

---

## 1. Ownership

| # | Owner | Area | Requirements (IDs in `01`) |
|---|---|---|---|
| 1 | **Vân Khánh** (team lead) | **Data layer (shared)**, project setup, API spike | FR-DATA-01…04, FR-ERR-01…04 (error mapping), FR-WL-04 (data by ID), NFR-03, NFR-06, NFR-07, NFR-10, NFR-12, manual checklist and regression run |
| 2 | **Hải** | **UI foundation (shared)** | FR-NAV-01 (the toggle and the shared main top bar), FR-CUR-01…04, FR-INFO-01, (S) FR-INFO-03 (the ⓘ icon and the dialog), FR-UI-01, FR-UI-02, formatting rules §5.2, NFR-08, NFR-13 |
| 3 | **An Khánh** | **Market List** | FR-ML-01…07, FR-ML-09, (S) FR-ML-08 (star on Market rows), (C) FR-ML-10, NFR-01, NFR-02 |
| 4 | **Kiên** | **Coin Detail** | FR-CD-01, 06, 07, 08, (S) 10, (C) 09, (S) FR-CUR-05 (selector and reload on Detail), FR-INFO-02, FR-NAV-02/03 (detail side) |
| 5 | **Duy** | **Price chart** | FR-CD-02…05, FR-CD-11…13 (S/C), (S) chart reload on currency change (FR-CUR-05) |
| 6 | **Bảo Linh** | **Watchlist** | FR-WL-01…03, 05, 08…10, (C) 06, 07, the favorite button (C3, also used by FR-ML-08) |

### What each owner delivers
- **Vân Khánh:** project skeleton and repo, `Application` class, `ViewModelFactory`, the Volley dependency and the `INTERNET` permission, the Network Security Configuration (`res/xml/network_security_config.xml` and the two certificate files in `res/raw`, see `03-architecture.md`, section 8.1), the shared `RequestQueue(NoCache(), BasicNetwork(HurlStack()))`, JSON mappers, the repositories (one set of classes used with both the remote and the fake data sources, `03` D19), the remote and the fake data sources, error mapping (`ErrorMapper` with its unit test, including the HTTP 429 and 5xx cases that cannot be produced on the real API), in-memory data with refresh intervals, `CoinImageLoader` in `core/image` for the coin icons (C8, `03` D20), build settings and key handling, the startup log line `Data source: FAKE|REAL`, CI. Also keeps the manual test checklist (about 20–30 lines, `07-test-plan.md`), runs it on days 7 and 9 and keeps the bug list. Also the day-1 tasks S1 (API capture, done), S2 (network smoke test, done) and S4 (project setup, done). **S2** covered four things: (1) HTTPS to the API host and the image host works on an API 24 emulator (A5); (2) which Volley exception or status appears for each failure; (3) on which thread the Volley callbacks run; (4) whether icon requests count against the credits (A6). Results are in `03-architecture.md` and `04-api-contract.md`.
- **Hải:** main screen with two tabs and the shared main top bar (the pill toggle that switches the tabs, CMP-13; the ⓘ icon; the currency selector at the right end; there is no bottom navigation, no tab row and no swiping, see `02`, SCR-01), dark theme (colors, text styles, spacing), the layout rules for all screens (`03-architecture.md`, section 11, NFR-13), English and Vietnamese strings (including the wording of the explanation dialog in `02`, CMP-12; the Vietnamese wording is supplied by the team lead), formatters (price, compact number, percent, date), `UiState` and `StateView` (loading/empty/error), the explanation dialog and the ⓘ icon that opens it (CMP-12, FR-INFO-03), the footer bar ("Powered by CoinGecko" on the left, last-updated time on the right, FR-INFO-01, FR-DATA-03 display), currency settings and the selector view (also used by Kiên on Detail), the placeholder and error drawables for coin icons (C8). Reviews other members' PRs for correct use of styles. **First task, day 1:** push (merged to `main`) the files of style names (colours, text styles, spacing, corner sizes) with the role names listed in `02`, §2.1 and rough placeholder values; the other members use these names from the start. Hải then replaces the values with the real design, which the team lead approves. Layouts and code do not change when the values change.
- **An Khánh:** Market List screen, coin row layout and adapter (the coin icon is a `NetworkImageView`, C8), sparkline view, search, sort, pull-to-refresh, states. The coin row includes the star: Bảo Linh's Watchlist needs it to remove a coin (FR-WL-01), so it is always in the row layout; on Market rows it is shown only if FR-ML-08 is built. Reuses Hải's components and Bảo Linh's favorite button.
- **Kiên:** Coin Detail screen (header with the coin icon as a `NetworkImageView` (C8), statistics with the ⓘ explanations of FR-CD-10, description, favorite, disclaimer), hosting Duy's chart. As a Should (FR-CUR-05): the currency selector in the Detail top bar and the reload of the Detail data when the currency changes.
- **Duy:** price chart: the `PriceChart` interface over MPAndroidChart (`03` D9), `PriceChartFragment`, five timeframes, 1H rule, touch marker, area fill, reference line and axis labels (S/C, dropped if the library cannot do them), loading/empty state of the chart area, chart colours, reload of the chart when the currency changes (Should, FR-CUR-05), and the debug-only chart sandbox screen (see `03-architecture.md`, section 13) used to test the chart alone.
- **Bảo Linh:** favorite storage and listener, favorite button, Watchlist screen (data by ID, empty state, limit, coin without data).

**Tests:** every member writes the unit tests for their own area. Vân Khánh keeps the manual checklist and runs it at integration; nobody writes another person's unit tests.

---

## 2. Rules for everyone

- Develop on **fake data** by default (`fake-data.md`). Use the real API only when integrating.
- No hard-coded colours, sizes or strings in layouts or Kotlin code: use the styles and resources from Hải. Follow the layout rules (`03-architecture.md`, section 11) and run the two screen-size checks before every PR.
- No networking or storage code in Activities, Fragments or adapters (NFR-05).
- Every pull request lists the requirement IDs it implements.
- Branches: one per area (`feature/market-list`, …), merged into `main` through a pull request with at least one reviewer who is not the author. Merge at least every 2 days. `main` always builds and runs. `main` is protected on GitHub.
- Commit message style: `feat(market-list): add sort toggle (FR-ML-04)`.
- CI runs lint and unit tests on every pull request (NFR-10).
- Do not commit the API key. Do not share the APK outside the team (see `01`, section 2.3). The repository is public, so nothing secret goes into it: no API key, no `local.properties`, no personal data.

### Review pairs (reviewer is the person who touches your component)
| Author | Reviewer | Why |
|---|---|---|
| An Khánh | Bảo Linh | Watchlist reuses the coin row and adapter |
| Bảo Linh | An Khánh | Market List uses the favorite button and listener |
| Kiên | Duy | Detail hosts the chart |
| Duy | Kiên | Same contract, other side |
| Hải | Vân Khánh | Foundation and data layer are used by everyone |
| Vân Khánh | Hải | Same |

---

## 3. Contracts (agree before coding; details go to `05-interfaces.md`)

| # | Contract | Owner | Used by | Depends on | Agree by |
|---|---|---|---|---|---|
| C1 | Domain models and repository interfaces, including the error type and the rule "callbacks run on the main thread" | Vân Khánh | Everyone | `04-api-contract.md`, then `05-interfaces.md` | End of day 2 |
| C2 | Formatters, `UiState`, `StateView`, explanation dialog (CMP-12), style guide (one sample row) | Hải | Everyone | The style-names files (Hải, day 1) | First version end of day 2; no later than day 3 |
| C3 | Favorite store, listener and favorite button | Bảo Linh | An Khánh, Kiên | – | Day 2 (interface and working version) |
| C4 | Coin row layout and adapter: takes coin data and favorite state, reports row tap and favorite toggle; the star is part of the row | An Khánh | Bảo Linh | C1, C2, C3 | Day 3 |
| C5 | `PriceChartFragment.newInstance(coinId)`: Detail only needs the coin ID; the chart brings its own timeframe selector and states | Duy | Kiên | C1, C2 (C7 only for the reload on a currency change) | Day 3 |
| C6 | Opening Detail: extra key `coin_id` | Kiên | An Khánh, Bảo Linh | – | Day 1 |
| C7 | Currency settings and listener: current currency and change notification, and the selector view | Hải | All state holders, Kiên | – | Day 3 |
| C8 | Coin icons: `CoinImageLoader` from `CryptoApp`, used with Volley's `NetworkImageView`; placeholder and error drawables | Vân Khánh (loader), Hải (drawables) | An Khánh, Kiên | Minimal `CryptoApp` (C1) | Signature in `05`, drawables on day 3; working loader on day 4 |

A minimal `CryptoApp` and `ViewModelFactory` come with C1 (end of day 2), so that every owner can run a screen on fake data. Until C8 exists, rows and the Detail header show the placeholder image. The data source interfaces (`03`, section 3) are internal to the data layer and are not a contract for the other members.

---

## 4. Day plan (rough; adjust if members are unavailable)

| Day | Goal | Main work |
|---|---|---|
| 1 | Start and unblock | **Vân Khánh:** minimal project skeleton pushed first (S4), then API capture (S1) and the network smoke test (S2); `04-api-contract.md`. **Hải:** first push the style-names files with placeholder values (`02`, §2.1), then collect reference screenshots and draft the real theme. **Kiên:** C6 (open Detail with `coin_id`, first version pushed early). **Duy:** the `PriceChart` interface and the debug chart sandbox on temporary data (the library choice, S3, is done). **Others:** read `01`, set up Android Studio, build layouts against temporary data and the style names from Hải's files. |
| 2 | Freeze contracts | Vân Khánh: `05-interfaces.md`, then models, repository interfaces, data source interfaces and fake data sources (C1) from the S1 and S2 results, minimal `CryptoApp` and `ViewModelFactory`; CI. Hải: theme and formatters v1 (C2). Bảo Linh: C3 (favorite store and favorite button, working). |
| 3 | Shared parts ready | Hải: currency settings and selector (C7), shared main top bar, coin icon placeholder drawables (C8). An Khánh: coin row and adapter (C4). Duy: `PriceChartFragment` (C5). Vân Khánh: the repositories (market list, coins by ID, detail, chart: in-memory data, refresh intervals, pause after a 429) on top of the fake data sources, so the screens can run on them; `CryptoApp` chooses fake or real, key handling, startup log line. |
| 4 | Screens on fakes | Market, Detail and Watchlist screens. Vân Khánh: remote data source for Market (the real network path) with the `ErrorMapper` class; `CoinImageLoader` (C8). |
| 5 | Real data begins | **Market List runs on real data by the end of day 5.** Remote data sources for Detail, chart and data by ID. Kiên attaches the chart to Detail. Owners write their tests and edge cases. |
| 6–7 | Integration | Real chart data, favorites synchronised across screens, VND, error and rate-limit cases, full flow works end to end. Vân Khánh: the `ErrorMapper` unit test (including 429 and 5xx), the manual checklist written (day 6), run at the end of day 7. |
| 8 | Quality | Hải: UI polish pass. Vietnamese strings. Error and edge cases. Could items only if every Must is done and merged. |
| 9 | QA | Vân Khánh runs the full checklist; fix bugs; CI green; README. |
| 10 | Presentation | Demo script, rehearsal, recorded demo as backup, tag the release on `main`. |

Integration rule: Should and Could items that are not integrated by the end of day 7 are cut, following the cut order in `01` section 6 (Could, then the currency selector on Detail, then area fill and axis labels, Vietnamese UI, chart colour, favorite star on Market rows, the explanations).

Work that nobody waits for and that is not in a milestone must be done and merged by the end of day 5.

Communication: a short message in the group chat each day (what I finished, what blocks me). Because schedules differ, nobody waits for a meeting to ask for help.

---

## 5. Definition of Done (every task)

1. Meets the acceptance criteria of its requirement IDs.
2. Loading, empty and error states are implemented where the screen has data (FR-ERR-01).
3. Unit tests for non-UI logic, written by the owner.
4. Works on fake data **and** on real data; checked on the emulator.
5. No hard-coded colours, sizes or strings; no key in code; Android lint has no errors; the layout rules and the two screen-size checks of `03-architecture.md`, section 11 are done.
6. Reviewed and merged through a pull request.
7. The owner can explain the feature (section 6).

---

## 6. Explain-your-feature checklist (for integration and the presentation)

Each owner can answer, in their own words:
1. What does the user see and do in this feature? Which requirement IDs?
2. Where does the data come from, and through which layers does it travel to the screen?
3. Which contracts does the feature use or provide, and with whom?
4. What happens when something goes wrong (no network, 429, missing field)?
5. Why was it designed this way, and what was the alternative?
6. How is it tested?

Everyone must also know how to use the shared parts: repository interfaces (C1), formatters, StateView and explanation dialog (C2), favorite store (C3), coin icons (C8).

---

## 7. Risks

| Risk | Likelihood | Impact | Mitigation | Owner |
|---|---|---|---|---|
| Vân Khánh is the critical path on days 2–3 (`05`, models, interfaces, fake data sources, minimal `CryptoApp`, then the repositories) | High | High | Fixed order (`05` first, then C1 with the models and interfaces, then the repositories); others work on layouts with temporary data and stubs meanwhile | Vân Khánh |
| Chart takes longer than planned | Medium | High | The library is chosen (S3 passed); the chart sandbox lets Duy work alone; fall back to a basic line chart without touch if the library later does not fit | Duy |
| Inconsistent UI between screens | Medium | Medium | Hải's style names on day 1 (placeholder values), then the style guide and sample row by day 2–3; review pairs check style use; polish pass on day 8 | Hải |
| A member is slow or unavailable | Medium | Medium | Watchlist is the lightest area; check progress on day 3; reassign Could items first | Vân Khánh |
| API quota or rate limit during development | Medium | Medium | Fake data by default; one shared key; real mode only when integrating | Vân Khánh |
| Late integration | High | High | Merge every 2 days; Market List on real data by day 5; end-to-end flow by day 7 | Everyone |
| The main top bar (pill toggle, ⓘ, currency selector) does not fit at 360 dp, or the pill look is hard to get | Medium | Low | Hải builds it early and checks it at 360×640 dp with English and Vietnamese labels; fallback: plain toggle buttons or shorter labels | Hải |
| Presentation unprepared | Low | High | Section 6 checklist; demo script and backup recording on day 10 | Vân Khánh |
