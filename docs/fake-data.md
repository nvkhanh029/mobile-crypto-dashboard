# Fake Data Guide

**Purpose:** let all six members build and test features without calling the real CoinGecko API, so we do not spend the 10,000-credit monthly allowance, and so tests are repeatable. <br>
**Related:** `01-requirements.md` (NFR-12, D-KEY), `04-api-contract.md` (the real responses these files come from), `05-interfaces.md` (the data-source interfaces). <br>
**Owner:** the team lead creates the files and the fake classes; feature owners may request extra scenarios.

---

## 1. Principle

The app has one data source interface per kind of data (market list, coins by ID, coin detail, chart). Each interface has two implementations. The repositories (in-memory data, refresh intervals, the pause after an HTTP 429) sit on top of the data sources and are the same classes in both modes (`03-architecture.md`, D19).

| Implementation | Where data comes from | When used |
|---|---|---|
| **Fake** | JSON files in `app/src/main/assets/fake/` (captured responses) and `app/src/main/assets/fake/edge/` (edge-case files) | Default. Daily development, unit tests, CI. |
| **Real** | CoinGecko API through the network layer | Integration testing and the demo. |

UI code and state holders never know which one is used, and they never see a data source at all: they call a repository. A build setting selects the data source.

The fake data sources read a file and turn it into models with the same mappers as the remote data sources (`03-architecture.md` §8.4, D23). So a file tests the real mapping code: `markets_invalid.json` ends as `AppError.InvalidData` because the mapper throws, and `detail_ethereum_edited.json` loses its markup because the mapper removes it, not because the fake data source does.

**Fake JSON must be real API responses**, captured once on day 1 during the API spike, so that the shape matches exactly. We do not write fake JSON by hand from imagination. (Exception: edge-case files in section 3, which are edited copies of real ones.)

---

## 2. Files to capture from the real API (day 1)

Capture once, with the shared team key. The exact requests and the capture dates are recorded in `04-api-contract.md`; the table below only says which kind of request each file comes from. Do not edit these files afterwards. Edited copies are listed separately in section 3 and live in the subfolder `edge/`, so the folder `fake/` itself contains captured files only.

| File | Real request it comes from | Used for |
|---|---|---|
| `markets_usd.json` | `/coins/markets`, `vs_currency=usd`, market-cap order, 100 per page, sparkline on | Market List (USD), search, sort, sparkline |
| `markets_vnd.json` | same, `vs_currency=vnd` | VND display and formatting |
| `markets_by_ids_usd.json` | same endpoint with `ids=` for 3 coins, **including at least one coin that is not in `markets_usd.json`** | Watchlist data by ID (FR-WL-04) |
| `markets_by_ids_vnd.json` | same, `vnd` | Watchlist in VND |
| `detail_<coin>.json` | `/coins/{id}` for 3 coins (see below). The request has no currency parameter: price fields inside the response are keyed by currency, so **one file serves both USD and VND** | Coin Detail header, statistics, description (USD and VND) |
| `chart_<coin>_1d_usd.json` | `/coins/{id}/market_chart`, 1 day | 24H chart and derived 1H |
| `chart_<coin>_7d_usd.json` | 7 days | 7D chart |
| `chart_<coin>_30d_usd.json` | 30 days | 1M chart |
| `chart_<coin>_365d_usd.json` | 365 days | 1Y chart |
| `chart_<coin>_1d_vnd.json` | 1 day, `vnd` | 24H and 1H chart in VND |
| `chart_<coin>_7d_vnd.json` | 7 days, `vnd` | 7D chart in VND |
| `chart_<coin>_30d_vnd.json` | 30 days, `vnd` | 1M chart in VND |
| `chart_<coin>_365d_vnd.json` | 365 days, `vnd` | 1Y chart in VND |

**Coins for detail files:** (1) `bitcoin`: a large coin with full data; (2) `ethereum`: the HTML case. Six real coins were checked on 18 Oct 2026 (bitcoin, ethereum, solana, tether, usd-coin, tron) and none has any markup in its description, so the HTML case uses the edited copy `detail_ethereum_edited.json` (section 3), and the real `detail_ethereum.json` is kept as well; (3) `solana`: a coin with a missing field in the real API (`max_supply` is `null`; it is `null` for ethereum too). The remaining missing-field cases use the edited copies in section 3.

**How the fake data source answers (decided 21 Oct 2026).** Only a few coins have files, but every screen must work on them, so the fake data source follows these rules. They exist in fake mode only; the real API is not affected.

| Request | The fake data source answers with |
|---|---|
| Market list | `markets_<cur>.json` for the requested currency |
| Coins by ID (Watchlist) | The coins of `markets_<cur>.json` plus, for coins outside the top 100, those of `markets_by_ids_<cur>.json` (`akedo`); only the requested ids are kept; the order is market-cap order, as in the real API (`04` §3): first the top-100 coins in file order, then the coins outside the top 100; an id that is in neither file is left out, without an error, as in the real API. So any watchlist of up to 50 coins of the top 100 works, and `akedo` works as a coin outside the top 100 |
| Coin detail | `detail_<coin>.json` if that file exists (`bitcoin`, `ethereum`, `solana`); for any other coin id, `detail_bitcoin.json`. The Detail screen then shows Bitcoin's data for another coin, which is accepted for development |
| Chart | The same chart file for any coin ID (only one coin's charts are captured), for the requested currency and timeframe |

**Time stamps:** captured files contain old timestamps. This is fine: the 1H rule is "within 60 minutes before the latest point in the data" (FR-CD-03), not relative to the phone clock. Do not shift timestamps.

---

## 3. Edge-case files (edited copies of the real samples)

All files of this section are in `app/src/main/assets/fake/edge/`, separate from the captured files in `fake/`. Each is copied from a real sample and then modified (`markets_empty.json` is simply `[]`). Use exactly the file names in the table below; this table is the only list of edited files, and nothing in it is a captured file. The suffix `_edited` is used only where a captured file with the same base name exists (`detail_ethereum.json` in `fake/` and `detail_ethereum_edited.json` in `edge/`).

Seven of the eight files are generated by `tools/make_edge_cases.py` (in the repository root, next to `app/`). The script only reads the captured files in `fake/` and only writes those seven names into `fake/edge/`, so running it again gives the same result. Run it from the repository root: `python tools/make_edge_cases.py`. `detail_ethereum_edited.json` is not produced by the script; it is a hand-edited copy of `detail_ethereum.json`.

| File | Based on | Change | Tests |
|---|---|---|---|
| `markets_missing_fields_usd.json` | `markets_usd.json` | A different coin has a different field set to `null`: coin #5 (`ripple`) `current_price`, coin #10 (`zcash`) `market_cap`, coin #15 (`whitebit`) `price_change_percentage_24h`, coin #20 (`stellar`) `market_cap_rank`, coin #25 (`dai`) `sparkline_in_7d` (the whole object) | FR-ML-09, sort with missing values |
| `markets_short_usd.json` | `markets_usd.json` | Keep only the first 3 coins | FR-ML-01 (fewer than 100) |
| `markets_empty.json` | none | `[]` | Empty state |
| `markets_invalid.json` | `markets_usd.json` | Cut the text at half of its length, in the middle of a coin (invalid JSON) | Error "invalid data" |
| `detail_ethereum_edited.json` | `detail_ethereum.json` | Insert a fake link into `description.en`: `<a href=\"https://ethereum.org/\">Ethereum</a>` (the link target is arbitrary) | FR-CD-07 (markup removed, plain text) |
| `detail_missing_fields.json` | `detail_bitcoin.json` | `max_supply` and `total_supply` are `null`; in `ath_date` and `atl_date` (objects keyed by currency) the `usd` and `vnd` values are `null` and the other keys are unchanged; `description.en` is an empty string | FR-CD-06, FR-CD-07 |
| `markets_by_ids_missing_usd.json` | `markets_by_ids_usd.json` | Remove the last requested coin (`akedo`) from the array | FR-WL-09 (coin without data); unit tests only, the scenario `missing_fields` does the same by code (section 5.2) |
| `chart_short_1d_usd.json` | `chart_bitcoin_1d_usd.json` | Keep only the last point of each list (`prices`, `market_caps`, `total_volumes`) | 1H "Not enough data" |

**Edge files and the currency (decided 21 Oct 2026).** The code that handles missing values, short lists, an empty list, invalid JSON or a chart with one point is the same for USD and VND, so testing it with the USD files is enough. Only two edge files carry prices and exist for USD only: `markets_missing_fields_usd.json` and `markets_short_usd.json`. While VND is selected, the scenarios `missing_fields` and `short_list` give the normal VND market list; USD values are never shown as VND (FR-CUR-04). Everything else works in both currencies: `markets_empty.json` and `markets_invalid.json` carry no prices, the detail files serve both currencies, the single point of `chart_short_1d_usd.json` is never shown as a price (the chart says "Not enough data"), and the Watchlist scenario removes a coin by code. A layout case that needs both a missing value and large VND numbers is not covered by a file; check such a case by hand if it matters.

---

## 4. Error scenarios (no file needed)

The fake data source produces these errors by code, in the same error type the real source produces (defined in `05-interfaces.md`):

`NO_CONNECTION`, `TIMEOUT`, `RATE_LIMITED` (429), `SERVER_ERROR` (5xx), `REJECTED` (other 4xx).

Each of these scenarios makes all four fake data sources (market list, coins by ID, coin detail, chart) answer with the matching error.

The errors go through the repository like real ones. In particular, after `RATE_LIMITED` the repository makes no new call to the data source for 60 s (FR-ERR-04), so a Retry inside that time shows the same message again.

---

## 5. How to use it

### 5.1 Selecting the mode
Two entries in the developer's own `local.properties` (not committed):

```
USE_FAKE_DATA=true            # true (default) or false
FAKE_SCENARIO=normal          # see list below
FAKE_DELAY_MS=800             # simulated response time, to see loading states
CG_API_KEY=                   # only needed when USE_FAKE_DATA=false
```

If `local.properties` or a value is missing (as on CI), the defaults are `USE_FAKE_DATA=true`, `FAKE_SCENARIO=normal`, `FAKE_DELAY_MS=800`. The build must not fail without a key.

The value of `FAKE_SCENARIO` is the name of the scenario in lower case (`rate_limited` is the scenario `RATE_LIMITED` of section 4). A value that matches no scenario stops the app at startup with the message `Unknown FAKE_SCENARIO: …`; it never falls back to `normal`.

### 5.2 Scenarios
Captured files are read from `fake/`, edge-case files from `fake/edge/`. The answers of the fake data source for every request type are in section 2; a scenario changes only what is listed here.

| `FAKE_SCENARIO` | Behaviour |
|---|---|
| `normal` | Real-sample files for the requested currency |
| `missing_fields` | Market list: `markets_missing_fields_usd.json` (USD only, see section 3). Detail: `detail_missing_fields.json` for every coin. Coins by ID: the last requested id is left out of the answer (a coin without data, FR-WL-09) |
| `short_list` | Market list: `markets_short_usd.json` (USD only, see section 3) |
| `empty` | `markets_empty.json` for the market list |
| `invalid_json` | `markets_invalid.json` for the market list |
| `html_description` | Detail: `detail_ethereum_edited.json` for every coin (FR-CD-07) |
| `short_chart` | Chart: `chart_short_1d_usd.json` for every timeframe (1H and all others show "Not enough data") |
| `no_connection`, `timeout`, `rate_limited`, `server_error`, `rejected` | The matching error (section 4) |
| `fail_then_ok` | The first request fails with `server_error`, the next succeeds (tests Retry and "keep old data"). Each fake data source counts its own first request (market list, coins by ID, coin detail and chart separately), so every screen can be tried on its own |

### 5.3 Unit tests
Unit tests build the fake data source directly with a scenario, for example `MarketListFakeDataSource(reader, Scenario.EMPTY, 0, executor, mainThread)` (the constructor is in `05-interfaces.md`, section 7), without reading `local.properties`. In a test the `reader` reads `src/main/assets` from the disk and `mainThread` runs the action at once, so a test needs no Android and no waiting. A repository test passes that fake data source and a fake clock to the repository. JVM tests read the same JSON files as the app, so there is **one copy** of each file. The Gradle test task runs in the project directory (`app/`) by default (Gradle documentation, read 22 Oct 2026), so no change to the build file is expected; whether the Android Gradle Plugin keeps this is checked by the first test. The team lead adds the `org.json` library for tests, because plain JVM tests do not contain a working `JSONObject`. (Setup details are to be confirmed when the project is created.)

Watchlist storage has its own in-memory fake for unit tests; no JSON is involved.

### 5.4 Real mode
1. Set `USE_FAKE_DATA=false` and `CG_API_KEY=<team key>` in your `local.properties`.
2. Do not leave auto-refresh loops running. The app already refuses to call the same data within the intervals of FR-DATA-01.
3. Before the demo, build with `USE_FAKE_DATA=false` and check that the startup log line says `Data source: REAL` and that the prices differ from the sample files (for example Bitcoin is not at the price in `markets_usd.json`).

### 5.5 Data source log
At startup the app writes one log line, `Data source: FAKE` or `Data source: REAL` (NFR-12). The UI shows no label, so this line is how to check which mode a build is in. In Logcat, filter on `Data source`.

---

## 6. Rules

- Fake files are **frozen** after the spike. If the real API turns out to differ from a fake file, fix the fake file first (re-capture), then the code, and tell the team.
- Never edit a real-sample file by hand. The folder `fake/` holds captured files only; every edited copy goes into `fake/edge/` and is added to the table in section 3.
- Fake files contain only public market data. They contain no API key.
- The captured files are snapshots of CoinGecko API responses, kept in the repository as sample data for development and tests only. The repository is public. The CoinGecko API Terms (clause 6) do not expressly permit storing or copying Data and have no exception for development or testing; the team accepts this risk for a coursework project (decided 19 Oct 2026). The app does not store market data on user devices (see `01-requirements.md`).
- Icon URLs inside the files are left as captured. Icon loading works the same in both modes and does not use the fake source (whether icon requests count toward the API quota is assumption A6 in `01`).
