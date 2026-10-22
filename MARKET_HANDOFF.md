# Market List — An Khánh

Work on `feature/market-list`. Requirements: FR-ML-01–09, FR-CUR-04 and FR-ERR-03.
The shared MainActivity, data layer and documentation are not edited.

## Run locally

Build the debug variant, then launch **Market Preview**, or run:

```
adb shell am start -n vn.edu.usth.cryptodashboard/.features.marketlist.preview.MarketPreviewActivity
```

The preview is a TEMPORARY, debug-only host approved for local testing. It reads
the team's existing fixtures on a worker thread, has a 60-second refresh interval,
and uses placeholder icons. It never calls the real API.

To check a scenario, force-stop the app and launch it with an extra:

```
adb shell am force-stop vn.edu.usth.cryptodashboard
adb shell am start -n vn.edu.usth.cryptodashboard/.features.marketlist.preview.MarketPreviewActivity --es fake_scenario missing_fields
```

Supported: normal, missing_fields, short_list, empty, invalid_json, no_connection,
timeout, rate_limited, server_error, rejected and fail_then_ok.

The temporary currency selector saves USD/VND for previewing. It is not a replacement
for Hải's C7. FavoriteButton and SharedPrefsFavoriteStore are the existing C3 classes.
The Detail screen currently only displays the coin ID because Kiên has not merged
the full screen yet.

## Files and flow

- MarketListFragment observes MarketViewModel and renders StateView, RecyclerView
  and AttributionView. Search is debounced by 300 ms.
- MarketViewModel keeps query/sort, calls MarketRepository, and rejects stale
  callbacks after a currency change. Failed refreshes keep data; a failed request
  for another currency does not.
- CoinFilterSorter is plain Kotlin. Null sort values always go last; ties use rank.
- CoinListAdapter and item_market_coin.xml are C4 for both Market and Watchlist.
  Use submitCoins for Market, or submitList with CoinRow(id, null, currency) for an
  unavailable Watchlist coin. FavoriteButton observes FavoriteStore.favoriteIds;
  the Watchlist owner observes the same LiveData to remove rows after a toggle.
- SparklineView draws seven-day prices on Canvas, independently of the 24h change.
- All Market text is in strings_market.xml in values/ and values-vi/.
- app/build.gradle.kts adds only the approved SwipeRefreshLayout 1.2.0 dependency.

## Integration with the team

CryptoApp, the real Market repository, CurrencySettings and CoinImageLoader are
still absent from main. MarketDependencies/MarketFeatureHost are explicitly
temporary bridges. Replace the bridge with the properties exposed by CryptoApp
when they land; pass its MarketRepository to marketViewModelFactory, observe C7
currency changes, and pass its CoinImageLoader to the adapter. Delete the debug
preview and its repository after the shared fake repository is ready.

Ask Hải to host MarketListFragment in the Market tab using show/hide. Do not create
a second production top bar. Bảo Linh can reuse CoinListAdapter without networking
or storage logic. Have Hải review the Vietnamese strings and the compact row layout.

## Verification

Run `gradlew.bat testDebugUnitTest lintDebug assembleDebug`.
Check 360×640 dp and 411×891 dp, both languages, long names, tiny USD prices,
large VND prices, empty data and missing fields. Real API integration remains a
team integration task and needs the shared repository and key; never put a key in
source or chat.


## Verified on 23 October 2026

- Build: assembleDebug and assembleDebugAndroidTest succeeded.
- JVM: 39 tests passed, including 19 Market tests.
- Android lint: 0 errors, 42 warnings. SDK/plugin compatibility and existing
  shared-resource warnings remain; no checks were suppressed.
- API 36: all four UI tests passed at 360×640 and 411×891 dp, in English and
  Vietnamese. The final normal flow was rerun after the last startup-focus fix.
- API 24: all four UI tests passed, including search, sort, refresh, favorites,
  currency, Detail navigation, unavailable rows and fake error scenarios.
  This legacy emulator reports 320×640 dp. Its screenshot API returns null, so
  the test captures the activity decor view and records that fallback in Logcat.
- Required-size evidence and full instrumentation logs are in the sibling
  ../verification directory. The initial API 24 screenshot failure is retained
  there alongside the passing final run.
- Documentation, shared code, MainActivity and the main manifest are unchanged.
  The only existing tracked file edited is the approved SwipeRefreshLayout
  dependency in app/build.gradle.kts. No commit or push has been made.

