# 07 – Test Plan

**Project:** Crypto Dashboard (Android, native Kotlin, XML Views) | **Team:** 6
**Related:** `01-requirements.md` (requirement IDs and the verification column of §5.1), `fake-data.md` (scenarios and how to switch the data source), `06-task-plan.md` (who writes which test, Definition of Done), `03-architecture.md` §15 (testing approach).

This file answers one question: **how is each requirement checked?** Section 2 gives one row for every FR and NFR of `01` (traceability, `01` §7). Section 3 is the manual checklist that is run on the emulator before the demo.

---

## 1. Kinds of checks

| Code | Kind | Runs | By whom |
|---|---|---|---|
| **U** | JVM unit test (`app/src/test`) | `.\gradlew.bat :app:testDebugUnitTest`, and in CI on every pull request | Each owner, for their own area (`06` §1) |
| **I** | Instrumented test on the emulator (`app/src/androidTest`) | Android Studio green arrow, or `.\gradlew.bat :app:connectedDebugAndroidTest`; not in CI | The owner of the screen |
| **M** | Manual check, listed in §3 as M01, M02, … | On the emulator, before the demo | Vân Khánh runs the whole list; owners run the lines of their screen before a pull request |
| **R** | Code review | In every pull request (review pairs, `06` §2) | The reviewer |
| **CI** | The GitHub Actions run (`.github/workflows/ci.yml`): unit tests and Android lint | On every pull request and every push to `main` | Automatic |

A test that needs a fake scenario is skipped unless `FAKE_SCENARIO` in `local.properties` has the needed value (`assumeTrue`); a skipped test is normal, not a failure.

**Settings used by the manual checks** (keys of `local.properties`, `fake-data.md` §5.1): `USE_FAKE_DATA` (`true` default, `false` for the real API), `FAKE_SCENARIO` (names in `fake-data.md` §5.2), `FAKE_DELAY_MS`, `CG_API_KEY` (only for real data; never in a commit, a log or a chat). After changing a value, run the app again with `.\gradlew.bat :app:installDebug` or the Run button.

**Devices:** an emulator with a recent stable image (API 36, the demo device) and an API 24 emulator (`01` NFR-09). Screen sizes for layout checks: 360×640 dp and 411×891 dp (`01` NFR-13).

---

## 2. Traceability: requirement → check

### 2.1 Navigation, Market List

| ID | Pri | How it is checked |
|---|---|---|
| FR-NAV-01 | M | M01, M16 |
| FR-NAV-02 | M | M07; I `MarketScreenTest.test01NormalSearchSortFavoriteCurrencyAndDetail` |
| FR-NAV-03 | M | M16 |
| FR-ML-01 | M | U `CoinMapperTest`, `MarketListFakeDataSourceTest`, `MarketViewModelTest`; I `MarketScreenTest.test01…`; M02, M19 |
| FR-ML-02 | M | U `SparklineTrendTest`; M02 |
| FR-ML-03 | M | U `CoinFilterSorterTest` (trim, case, part of symbol, no match); I `MarketScreenTest.test01…`; M04 |
| FR-ML-04 | M | U `CoinFilterSorterTest` (all criteria, both directions, ties by rank, toggle rule); M05 |
| FR-ML-05 | M | U `CoinFilterSorterTest.searchAndSortComposeWithoutMutatingProviderListOrRanks`, `MarketViewModelTest.currencyChangeClearsOldRowsButRetainsSearchAndSort`; M05 |
| FR-ML-06 | M | U `MarketViewModelTest` (failed refresh keeps rows, no duplicate refresh), `MarketRepositoryImplTest.within60s_…`; M06 |
| FR-ML-07 | M | See FR-NAV-02 |
| FR-ML-08 | S | I `MarketScreenTest.test01…`; M10 |
| FR-ML-09 | M | U `CoinMapperTest` (missing and null fields), `CoinFilterSorterTest.missingValuesComeLastInBothDirectionsForEveryCriterion`; I `MarketScreenTest.test04ShortAndMissingData`; M14 |
| FR-ML-10 | C | M26 (only if the requirement is built) |

### 2.2 Coin Detail and chart

| ID | Pri | How it is checked |
|---|---|---|
| FR-CD-01 | M | U `CoinDetailViewModelTest`, `CoinDetailMapperTest`; M07 |
| FR-CD-02 | M | U `PriceChartViewModelTest`, `ChartMapperTest`; M08 |
| FR-CD-03 | M | U `TimeframeSlicerTest` (1H rule), `PriceChartViewModelTest` (quick switch, stale response, 1H with fewer than two points), `ChartRepositoryImplTest` (days per timeframe, reuse); M08, M19 |
| FR-CD-04 | M | U `ChartTimeLabelFormatterTest`, `DateFormatterTest.chartLabelsUseTheDocumentedPatterns`; M08 (marker, bounds, no page scroll on a horizontal drag) |
| FR-CD-05 | S | M08 |
| FR-CD-06 | M | U `CoinDetailStatsMapperTest` (order, total and max supply hidden when null), `CoinDetailMapperTest`, `CompactNumberFormatterTest`; M07, M14 |
| FR-CD-07 | M | U `CoinDetailMapperTest` (`description_…`, markup removed); M09 |
| FR-CD-08 | M | M10 |
| FR-CD-09 | C | M07 (only if the requirement is built) |
| FR-CD-10 | S | M07 |
| FR-CD-11 | S | M08 |
| FR-CD-12 | C | M08 (only if the requirement is built) |
| FR-CD-13 | S | M08 |

### 2.3 Watchlist, currency

| ID | Pri | How it is checked |
|---|---|---|
| FR-WL-01 | M | U `WatchlistViewModelTest.removingACoinAfterSuccessHasFewerRowsAndNoNewRequest`; M10, M12 |
| FR-WL-02 | M | M11 |
| FR-WL-03 | M | U `WatchlistRowBuilderTest.rows_follow_favorite_order_not_server_order`; M12 |
| FR-WL-04 | M | U `WatchlistViewModelTest`, `MarketRepositoryImplTest.coinsByIds_…`, `RemoteDataSourcesTest.coinsByIds_…`; M12 (real data) |
| FR-WL-05 | M | U `WatchlistViewModelTest.emptyFavoritesShowEmptyStateWithoutRequest`; M12 |
| FR-WL-06 | C | M12 (only if the requirement is built) |
| FR-WL-07 | C | M12 (only if the requirement is built) |
| FR-WL-08 | M | M13 |
| FR-WL-09 | M | U `WatchlistRowBuilderTest.missing_id_becomes_unavailable_row_in_its_place`; M14 |
| FR-WL-10 | M | U `MarketRepositoryImplTest` (held data within 60 s), `WatchlistViewModelTest.failedRefreshKeepsOldRowsAndStopsSpinner`; M06 |
| FR-CUR-01 | M | U `CurrencyParserTest`; M15 |
| FR-CUR-02 | M | M15 |
| FR-CUR-03 | M | U `PriceFormatterTest`, `CompactNumberFormatterTest`, `MarketRepositoryImplTest.otherCurrency_…`; M15 |
| FR-CUR-04 | M | U `MarketViewModelTest.currencyChangeClearsOldRowsButRetainsSearchAndSort`, `WatchlistViewModelTest.currencyChangeShowsLoadingAndFailureHasNoPreviousData`; M15 |
| FR-CUR-05 | S | U `CoinDetailViewModelTest` (currency change), `PriceChartViewModelTest` (currency change); M15 |

### 2.4 Data freshness, errors, information, appearance

| ID | Pri | How it is checked |
|---|---|---|
| FR-DATA-01 | M | U `RefreshPolicyTest`, `MarketRepositoryImplTest`, `CoinDetailRepositoryImplTest`, `ChartRepositoryImplTest` |
| FR-DATA-02 | M | R (`NoCache`, no file or database code in the data layer); M28 |
| FR-DATA-03 | M | U `DateFormatterTest.lastUpdatedUsesHoursMinutesAndDayMonth`, `MarketViewModelTest.failedRefreshKeepsFilteredRowsAndOriginalTimestamp`; M02, M20 |
| FR-DATA-04 | M | M17, M20 |
| FR-ERR-01 | M | U the four ViewModel tests (`MarketViewModelTest`, `WatchlistViewModelTest`, `CoinDetailViewModelTest`, `PriceChartViewModelTest`); M17 |
| FR-ERR-02 | M | U `ErrorMapperTest` (every category), `RemoteLoaderTest`, `RemoteDataSourcesTest.aFailedRequest_isAnAppErrorInEveryDataSource`; I `MarketScreenTest.test03ErrorScenariosUseSharedMessages`; M17, M18 |
| FR-ERR-03 | M | U `MarketViewModelTest`, `WatchlistViewModelTest`, `CoinDetailViewModelTest`, `PriceChartViewModelTest` (failed refresh keeps data, failed new request does not); M18, M20 |
| FR-ERR-04 | M | U `MarketRepositoryImplTest.rateLimited_blocksEveryRequestFor60s_thenLetsThemThrough`, `ChartRepositoryImplTest.rateLimited_blocksTheOtherTimeframes`, `CoinDetailRepositoryImplTest.aRateLimitSeenByAnotherRepository_alsoBlocksThisOne`; M17 |
| FR-INFO-01 | M | M02 |
| FR-INFO-02 | M | M07 |
| FR-INFO-03 | S | M03 |
| FR-UI-01 | M | M24 |
| FR-UI-02 | S | M21 |

### 2.5 Non-functional

| ID | Pri | How it is checked |
|---|---|---|
| NFR-01 | M | M01 (stopwatch, 3 runs, API 36 emulator, real data) |
| NFR-02 | S | M25 |
| NFR-03 | M | R; M27 |
| NFR-04 | M | U `ErrorMapperTest`, `RemoteLoaderTest`, the mapper tests (`CoinMapperTest`, `CoinDetailMapperTest`, `ChartMapperTest`: malformed JSON, missing fields, empty list); I `MarketScreenTest.test02…`, `test03…`; M17, M18, M14 |
| NFR-05 | M | R (checklist: no networking, JSON or storage code in Activities, Fragments or adapters; shared formatters used everywhere) |
| NFR-06 | M | CI (`testDebugUnitTest` passes) |
| NFR-07 | M | CI (the build succeeds without a local key); M30 |
| NFR-08 | S | M23 |
| NFR-09 | M | M29 |
| NFR-10 | M | CI (lint and unit tests) |
| NFR-11 | M | M24; R (`screenOrientation="portrait"` on every Activity in the manifest) |
| NFR-12 | M | U `ScenarioTest`; M28 |
| NFR-13 | S | M22 |

---

## 3. Manual checklist

Run each line with the setting in the second column (`normal` is the default scenario). Lines marked **real** are run again with `USE_FAKE_DATA=false` and the team key. A line passes only if the expected result holds on every device it is run on.

| # | Setting | What to do | Expected | IDs |
|---|---|---|---|---|
| M01 | fake `normal`, then **real** | Cold start the app, 3 times; measure the time until the first rows appear. | App opens on Market. First rows within 3 s (real data, API 36 emulator). | FR-NAV-01, NFR-01 |
| M02 | **real** | Read a few rows; tap "Powered by CoinGecko". | Up to 100 rows with icon, name, symbol, price, signed 24h %, market cap and sparkline. Sparkline green, red or neutral by its trend. The footer shows "Powered by CoinGecko" and the last-updated time; the tap opens coingecko.com. | FR-ML-01, FR-ML-02, FR-INFO-01, FR-DATA-03 |
| M03 | `normal` | On Market and on Watchlist tap the ⓘ icon in the top bar. | The same dialog on both tabs, one sentence each for market cap, the 24h label, the 7-day line and the star. "Got it" closes it. | FR-INFO-03 |
| M04 | `normal` | Type `bit`, then ` ETH `, then `zzz`, then clear the text. | Matching rows only, case and surrounding spaces ignored. `zzz` shows the "No coins found" state. Clearing restores the full list. | FR-ML-03 |
| M05 | `normal` | Sort by price, 24h change and market cap; tap the active one again; then search while a sort is active. | Default is market cap descending. Active criterion and direction are visible. The second tap reverses the direction. Search keeps the sort. | FR-ML-04, FR-ML-05 |
| M06 | **real** | Pull to refresh twice within 60 s; later pull again after 60 s. | Inside 60 s the indicator stops and the last-updated time does not change. After 60 s the data and the time are new. Search, sort and scroll position stay. The same on Watchlist. | FR-ML-06, FR-WL-10, FR-DATA-01 |
| M07 | **real** | Open a coin from the list. Tap the ⓘ of every statistic. Scroll to the end. | Header with name, symbol, icon, price and signed 24h %. Statistics list market cap, 24h volume, ATH, ATL and circulating supply; total and max supply only when they have a value. Each ⓘ opens a one-sentence dialog. "For information only. Not financial advice." is the last block. | FR-NAV-02, FR-CD-01, FR-CD-06, FR-CD-10, FR-INFO-02 |
| M08 | **real** | On Detail switch 1H, 24H, 7D, 1M, 1Y (quickly, several times); touch and drag on the chart; drag vertically. | 24H is the default. Only the chart area shows loading. The marker shows price and time with the format of the timeframe, stays inside the chart and disappears on lift. A horizontal drag does not scroll the page; a vertical drag does. 1H shows about 12 points. Colour is green when the last price is not below the first, red otherwise. | FR-CD-02 to FR-CD-05, FR-CD-11, FR-CD-13 |
| M09 | `html_description`; `missing_fields` | Open a coin; tap "Read more" and "Show less". Then open a coin with `missing_fields`. | Plain text, no markup, no clickable links; collapsed to about 4 lines. Without a description: "No description available." | FR-CD-07 |
| M10 | `normal` | Add a coin with the star on Detail; add another with the star on a Market row; open Watchlist. | Both coins are in Watchlist. The star state matches on Market, Detail and Watchlist at once. | FR-CD-08, FR-ML-08, FR-WL-01 |
| M11 | `normal` | With favorites set, close the app, swipe it away from recents and open it again. | The favorites are still there. | FR-WL-02 |
| M12 | **real** | Open Watchlist with 3 favorites; remove one with its star; remove all; tap the button of the empty state. | Rows in the order they were added (oldest first), same row content as Market. A removed row disappears. The empty state has a message and a button that switches to the Market tab. | FR-WL-01, FR-WL-03 to FR-WL-05 |
| M13 | `normal` | Add favorites until there are 50; try to add the 51st. | "Watchlist is full (50)"; the list stays at 50. Also check that Watchlist with 50 rows loads and scrolls. | FR-WL-08 |
| M14 | `missing_fields` | Look at Market, Detail and Watchlist (with the last favorite added). | A missing value shows "—", never "null" or "NaN". Rows with a missing sort value go last in both directions. The last favorite shows "Data unavailable" and can be removed; other rows are unaffected. | FR-ML-09, FR-CD-06, FR-WL-09, NFR-04 |
| M15 | **real** | Choose VND on Market. Open Watchlist and Detail (selector on Detail) and change the currency there. Restart the app. | Every price, market cap, volume, ATH, ATL and chart value uses the new currency; percentages do not change. Loading is shown, then new data; search and sort are kept. The choice survives a restart. | FR-CUR-01 to FR-CUR-05 |
| M16 | `normal` | Search and sort on Market, scroll, open Detail, go back; switch tabs and back. Change the currency on Detail and go back. | State of both tabs is kept after tab switches and after Back. After a currency change on Detail the list reloads in the new currency with search and sort kept. | FR-NAV-01, FR-NAV-03 |
| M17 | `no_connection`, `timeout`, `rate_limited`, `server_error`, `rejected`, `invalid_json` (one at a time) | Start the app and open Watchlist and Detail. | Each category shows its own message (`01` §4.7) in a state that fills the content area, with Retry; no empty list; no crash. After `rate_limited`, Retry within 60 s makes no request and shows the same message. | FR-ERR-01, FR-ERR-02, FR-ERR-04, FR-DATA-04, NFR-04 |
| M18 | `fail_then_ok` | On each of Market, Watchlist, Detail and the chart: fail, then Retry. | The first load fails with the error state; Retry succeeds. After a currency or timeframe change that fails, the old data is gone and the error state is shown. | FR-ERR-02, FR-ERR-03, NFR-04 |
| M19 | `short_list`, `empty`, `short_chart` | Start the app; open Detail. | Three rows without an error; the empty provider list shows the empty state; every timeframe of the chart shows "Not enough data". | FR-ML-01, FR-CD-03 |
| M20 | **real**, network off | With data on screen turn the network off and pull to refresh. Then close the app, keep the network off and start it. | Data stays on screen with its last-updated time and a clear error message. A launch without network shows the error state with Retry. | FR-DATA-03, FR-DATA-04, FR-ERR-03 |
| M21 | `normal` | Switch the device language to Vietnamese, open every screen, then to another language. | Vietnamese strings everywhere; no cut-off text (check the Detail disclaimer and the footer); English for any other language. | FR-UI-02 |
| M22 | `normal`, long names and large values | On 360×640 dp and 411×891 dp open Market, Watchlist and Detail; use VND and a coin with a 7-digit price. | No overlap; name, price and 24h change are never cut off; nothing is unreachable. | NFR-13 |
| M23 | `normal` | Run Accessibility Scanner on the three screens. | Touch targets at least 48×48 dp, contrast at least 4.5:1, icons have content descriptions, meaning is not shown by colour alone. | NFR-08 |
| M24 | `normal` | Set the system theme to light; rotate the device. | The app stays dark and in portrait. | FR-UI-01, NFR-11 |
| M25 | **real** | Fling the 100-row list fast, with the GPU profiler or "Profile GPU rendering". | No visible stutter; janky frames at most 10%. | NFR-02 |
| M26 | **real** | Leave the Market tab open for more than 5 minutes (only if FR-ML-10 is built). | The list refreshes by itself only while visible, at intervals of at least 5 minutes, and not in the background. | FR-ML-10 |
| M27 | debug build, **real** | Run the flow M02 to M12 with Logcat filtered by `StrictMode`. | No StrictMode violation is logged. | NFR-03 |
| M28 | debug build | Run `adb shell run-as vn.edu.usth.cryptodashboard ls -R .` after M02 to M12; read the start-up log line (filter `Data source`) in both modes. | The app's folder holds only the preference files of the favorites and the currency; no file with market data. The log line shows `Data source: FAKE` or `Data source: REAL` to match the setting. | FR-DATA-02, NFR-12 |
| M29 | both emulators, **real** | Run M01 to M08, M12, M15 and M20 on the API 24 and the API 36 emulator. | The same results on both. Coin icons load on both. | NFR-09 |
| M30 | repository | Search the repository for the key (`git grep` for the first characters of the key, without writing the key anywhere) and check that `local.properties` is ignored. | No match; `local.properties` is not tracked. | NFR-07 |

Cases that cannot be produced on the real API (HTTP 429 and 5xx) are covered by U `ErrorMapperTest`, `RemoteLoaderTest` and the repository tests, and on the screen by M17.
