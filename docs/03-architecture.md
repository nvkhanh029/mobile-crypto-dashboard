# 03 – Architecture

**Project:** Crypto Dashboard (Android, native Kotlin, XML Views) | **Version:** 1.0 <br>
**Related:** `01-requirements.md` (what), `04-api-contract.md` (API facts), `05-interfaces.md` (exact signatures), `06-task-plan.md` (owners), `fake-data.md` (development data). <br>
**Purpose of this file:** show how the app is structured, how data moves, and **why** each decision was made, so six people can build in parallel and each can explain the design.

---

## 1. Goals that shape the architecture

| Goal | What it means for the design |
|---|---|
| Six people work in parallel | One feature per owner; features talk only through small contracts (section 12) |
| Work does not wait for the network or the API quota | Every data source has a fake and a real implementation behind the same interface |
| Testable without an emulator | Logic lives in plain Kotlin classes that do not use Android types (NFR-06) |
| Never block the main thread | One threading rule for the whole app (section 5) |
| Survive things going wrong | One error type and one state model used by every screen (sections 6, 8) |

---

## 2. Layers

```
┌──────────────────────────────────────────────────────────────┐
│ UI layer      Activities, Fragments, XML layouts, adapters,  │
│               custom Views.  Shows state, sends user events. │
├──────────────────────────────────────────────────────────────┤
│ ViewModel     One per screen. Holds UI state (LiveData),     │
│               search/sort/timeframe, calls repositories.     │
├──────────────────────────────────────────────────────────────┤
│ Domain        Model classes (Coin, CoinDetail, ChartPoint…)  │
│               and repository INTERFACES. No Android code.    │
├──────────────────────────────────────────────────────────────┤
│ Data          Repository implementations (in-memory data,    │
│               refresh rules) on top of data sources (Volley  │
│               or fake), JSON mappers.                        │
└──────────────────────────────────────────────────────────────┘
Dependencies point down only: UI → ViewModel → Domain ← Data
```

| Layer | Responsibility | Must not |
|---|---|---|
| UI | Draw state, forward taps and text input | Call the network, parse JSON, hold business logic |
| ViewModel | Decide what the screen shows: filter, sort, loading/error state, current timeframe | Reference Views or a Context |
| Domain | Define what data looks like and what the app can ask for | Depend on Android, Volley or JSON |
| Data | Get data (through a remote or a fake data source), turn JSON into models, keep it in memory, map errors | Know about screens |

**Why layers:** each layer has one job, so a change in one place (for example switching the data source) does not touch the others. The repository **interface** in the domain layer is the key: UI developers code against it using fake data, while the real network code is written at the same time. This follows the layered structure recommended in the Android architecture guide (UI layer, data layer, repositories as the single place that exposes data; a domain layer for reusable logic is optional, and we keep only models and interfaces in it).

**Repositories and data sources inside the Data layer:** a repository decides *when* to get data (in-memory data, refresh intervals, the pause after a 429); a data source decides *where* the data comes from (the CoinGecko API through Volley, or the JSON files in `assets/fake`). Only repositories talk to data sources; ViewModels and everything above never do. The Android data layer guide describes the same split: the repository is the only entry point of the data layer, and it can keep an in-memory cache as its source of truth (section 18). The data source interfaces are internal to the Data layer, so they are not a contract between members (section 12).

**Coin icons are not market data:** they are pictures shown by the UI, so their loader is a shared UI helper in `core/image`, not a data source (D20).

---

## 3. Package structure and owners

Root package: `vn.edu.usth.cryptodashboard`

```
app/                 CryptoApp                                             Vân Khánh
                     MainActivity (two tabs, shared top bar)               Hải
core/
  ui/                StateView, PriceChangeLabel, AttributionView, UiState,
                     explanation dialog                                     Hải
  format/            price, compact number, percent, date formatters        Hải
  currency/          CurrencySettings, currency selector                    Hải
  favorite/          FavoriteStore, FavoriteButton                          Bảo Linh
  image/             CoinImageLoader (coin icons, C8)                       Vân Khánh
domain/
  model/             Coin, CoinDetail, ChartPoint, Timeframe, Currency,
                     AppError                                               Vân Khánh
  repository/        MarketRepository, CoinDetailRepository,
                     ChartRepository, RepositoryCallback                    Vân Khánh
data/
  source/            data source interfaces, one per kind of data
                     (market list, coins by ID, coin detail, chart)         Vân Khánh
  mapper/            JSON mappers (CoinMapper, CoinDetailMapper,
                     ChartMapper), used by remote and fake                  Vân Khánh
  remote/            remote data sources, CoinGeckoRequest                  Vân Khánh
                     ErrorMapper and its unit test                          Hải
  fake/              fake data sources (read JSON from assets/fake)         Vân Khánh
  repository/        repository implementations, RefreshPolicy              Vân Khánh
features/
  marketlist/        fragment, ViewModel and its factory, coin row +
                     adapter, sparkline, filter and sort logic              An Khánh
  detail/            CoinDetailActivity, ViewModel and its factory,
                     stats, description                                     Kiên
  chart/             PriceChartFragment, ViewModel and its factory,
                     chart view, timeframe slicing                          Duy
  watchlist/         WatchlistFragment, ViewModel and its factory           Bảo Linh
res/                 shared values: theme, colours, dimens, text styles,
                     shared drawables, common strings (EN and VI)           Hải
                     layouts, drawables and strings file of one screen      owner of that screen
res/xml/             network_security_config.xml (section 8.1)              Vân Khánh
res/raw/             the two certificate files used by that config          Vân Khánh
```

The names of the data source classes and interfaces follow the pattern *type of data + type of source + DataSource* (for example a market list from the remote API, or from the fake files); the exact names are in `05-interfaces.md`.

Rules:
- `features/*` depend on `domain` and `core`, never on `data`. They never import each other, except through the contracts in section 12 (Watchlist uses the coin row from `marketlist`; Detail hosts the chart fragment).
- `data` depends only on `domain`. Inside `data`, data sources are used only by the repository implementations. `remote` and `fake` both use `mapper` and never import each other (D23).
- Formatters, filter/sort, timeframe slicing and mappers are plain Kotlin (no Android types) so they run in JVM unit tests.
- Package by feature (not by type) so each owner works in their own folder and merge conflicts stay rare.
- **Resources follow the same rule.** The layouts and drawables of a screen belong to its owner (the shared coin row layout is An Khánh's, C4). The strings of a screen go in a strings file of the owner's own, in `values/` and in `values-vi/`; Android takes the file name as arbitrary and uses the `name` of each `<string>` (checked 21 Oct 2026, section 18), so several strings files work together. Hải owns the common `strings.xml` (words used by several screens, for example Retry, and the shared error messages) and reviews the Vietnamese text. A string name must be unique across all files (general Android knowledge, not checked in the docs), so each owner starts the names of their strings with the name of the feature.
- **`AndroidManifest.xml`:** each owner declares their own Activity; every other change (permissions, the network security configuration, the application element) goes through Vân Khánh.

---

## 4. Data flow

One request, from the user's action to the screen (Market List, real data):

```
Fragment (UI)
  │ user pulls to refresh
  ▼
MarketViewModel                         – main thread
  │ repository.getTopCoins(currency, callback)
  ▼
MarketRepositoryImpl                    – main thread
  │ data newer than the refresh interval? → return it, no data source call (FR-DATA-01)
  │ less than 60 s after a 429? → "rate limited" error, no data source call (FR-ERR-04)
  │ otherwise → ask the data source
  ▼
Market remote data source               – main thread
  │ CoinGeckoRequest into the shared Volley RequestQueue
  ▼
Volley network thread → HTTPS → CoinGecko
  ▼
Response listener                       – main thread (confirmed in S2: Volley delivers here)
  │ hand the raw JSON string to the executor
  ▼
Single background thread (ExecutorService)
  │ JSON → mapper → List<Coin>
  │ error? → ErrorMapper → AppError
  ▼
Handler(main looper).post(...)          – back to the main thread
  ▼
data source callback onSuccess / onError – main thread
  ▼
MarketRepositoryImpl                    – main thread
  │ stores the result in memory with its fetch time, then calls the caller's callback
  ▼
MarketViewModel                         – updates LiveData<UiState<…>>
  │ filter + sort applied here (search text and sort live in the ViewModel)
  ▼
Fragment observes the LiveData → adapter shows rows / StateView shows error
```

With fake data only the data source changes: the fake data source reads a JSON file from `assets/fake`, turns it into models with the same mapper as the remote data source, waits `FAKE_DELAY_MS`, and calls the same callback. The repository and everything above it are the same classes in both modes, so the refresh intervals and the pause after a 429 also work on fake data (D19).

**What S2 confirmed about threads (API 24 emulator, scratch project):** Volley's `parseNetworkResponse` of a `Request` runs on a Volley worker thread, and the response listener (and the error listener) is delivered on the main thread. Requests can finish in a different order from the one they were sent in. Parsing the top-100 response (about 395 KB) into a `JSONArray` took 187 to 332 ms on the emulator, so parsing on the main thread would drop frames (NFR-03). That is why the listener only hands the raw string to the executor.

**Why a callback that always returns on the main thread:** whoever calls a repository can touch LiveData and Views directly, with no thread handling. One rule in one place instead of six people each solving it. Data sources follow the same rule towards the repository.

---

## 5. Threading rules

1. **Network:** only through Volley's request queue (it runs requests off the main thread).
2. **JSON parsing and any disk work:** on the single background `ExecutorService` created once in `CryptoApp`.
3. **Results to the UI:** posted back with a `Handler` on the main looper. Repository callbacks (and data source callbacks) always arrive on the main thread.
4. **Views and LiveData `setValue`:** main thread only. Fragments and ViewModels never create threads.
5. Verified in debug builds with StrictMode (NFR-03).

**Why an executor and a Handler, not `AsyncTask`:** `AsyncTask` is deprecated. The Android threading guide describes this pattern: create the thread pool once (for example in the `Application` class), run work with `execute`, and deliver the result to the main thread through a `Handler` created with the main looper, using a callback interface. It uses only platform classes (`java.util.concurrent`, `android.os.Handler`) and works the same in Kotlin. Kotlin coroutines are an alternative; they are not used here because they add a library and a new concept for the team.

---

## 6. State and lifecycle: ViewModel + LiveData

Each screen has one `ViewModel` (an Android Jetpack class) that exposes `LiveData<UiState<T>>`.

`UiState<T>` has four cases that match FR-ERR-01: **Loading, Success (data + "updated at" time), Empty, Error (AppError, optionally with old data to keep showing, FR-ERR-03)**. Every screen reads the same shape, so `StateView` (Hải) works everywhere.

**Old data in `Error`:** `Error` carries old data only when a refresh of the same request failed (same currency, coin and timeframe: pull-to-refresh, or Retry with data on screen). After a change of currency or timeframe the ViewModel emits `Error` without old data, because the old data belongs to another request (FR-ERR-03, FR-CUR-04). The ViewModel decides this, not the screen.

**Why ViewModel + LiveData:**
- LiveData only notifies observers whose screen is visible, and removes them when the screen is destroyed, so a response that arrives after the user left the screen cannot crash the app. This is the most common bug for beginners.
- A ViewModel lives as long as its screen, so search text, sort and timeframe are kept without extra code (FR-NAV-01/03).
- The ViewModel never holds a View, so its logic can be unit tested.

**Boundaries:** LiveData is used in the UI layer only (ViewModels and what observes them). Repositories use plain callbacks, because the Android documentation advises against LiveData in the data layer.

**Creating ViewModels:** a ViewModel that receives repositories (or a `coin_id`) in its constructor cannot be created by the default factory, so each owner writes a small factory for their own ViewModel in their feature package, with `viewModelFactory { initializer { … } }` (an Android `ViewModelProvider.Factory`, section 18). The factory takes the repositories from `CryptoApp`, which only exposes them as properties (`05-interfaces.md`). There is no shared factory, so nobody edits a file of another member to register a ViewModel (D22).

**Stale responses:** each ViewModel keeps a request counter and ignores a response that is not from the latest request (FR-CD-03: quick timeframe switching; currency change while loading). This is needed in practice: S2 showed that responses can arrive in a different order from the requests.

---

## 7. Dependencies and shared objects

`CryptoApp` (the `Application` subclass) creates, once:
- the Volley `RequestQueue`,
- the background `ExecutorService` and the main-thread `Handler`,
- the data sources (remote or fake, chosen by the build setting, section 10) and, on top of them, the repositories (the same repository classes in both modes),
- the `CoinImageLoader` (on the same `RequestQueue`),
- `CurrencySettings` and `FavoriteStore`.

Other classes receive what they need from `CryptoApp` (manual dependency injection: no framework). The Android guide recommends dependency injection (and names Hilt); we do the same idea by hand because it is a few lines, has no annotation processing, and is easy to explain.

---

## 8. Data layer details

### 8.1 Networking
- A single `CoinGeckoRequest` (extends Volley's `StringRequest`) adds the `x-cg-demo-api-key` header and decodes the body as UTF-8. It is used by the remote data sources only.
- **Timeout and no retry:** the request uses `DefaultRetryPolicy(timeoutMs, 0, 1f)` with a 15-second timeout, 0 retries and backoff multiplier 1, so Volley never retries by itself (FR-ERR-04).
- **Nothing is cached by Volley:** the shared queue is created as `RequestQueue(NoCache(), BasicNetwork(HurlStack()))`. `NoCache` stores nothing, so no response of any request is written to the device's disk (FR-DATA-02). This is used for the whole queue, not per request with `setShouldCache(false)`, so no one can forget it on a new request. The app's own in-memory data and refresh intervals decide freshness (section 8.2). Used in the S2 scratch project for API requests; the combination with `ImageLoader` is covered in the next point.
- **HTTPS on Android 7.0 (API 24):** Android 7.0 does not trust the certificate root that the CoinGecko hosts need (ISRG Root X1 is trusted from Android 7.1.1, ISRG Root X2 from Android 14). In S2 the first request on the API 24 emulator failed for this reason. The fix is a Network Security Configuration (`res/xml/network_security_config.xml`, referenced from the manifest): a `domain-config` for `coingecko.com` (including subdomains, so both `api.coingecko.com` and `coin-images.coingecko.com` are covered) whose trust anchors are the two root certificates stored as PEM files in `res/raw`, plus the system certificates. Verified in S2: after this, both hosts returned 200 on API 24. The app never disables certificate checks and never trusts all certificates. The two PEM files are copied from the official Let's Encrypt page (section 18). The Android documentation says a config like this is the supported way to add trusted certificates on older versions.
- **Coin icons** (contract C8, D20): `CoinImageLoader` in `core/image` extends Volley's `ImageLoader` with an in-memory `LruCache` as its image cache, on the same `NoCache` queue, so icons are not written to disk either (`05-interfaces.md`, section 5). The layouts show an icon with Volley's `NetworkImageView` (`setImageUrl(url, loader)`, with a placeholder and an error image from the shared resources). Checked in the Volley sources (section 18): `NetworkImageView` and `ImageLoader` exist in Volley 1.2.1 (the 1.2.0 release notes add default and error images to `NetworkImageView`; 1.2.1 only separates Cronet); `ImageLoader` uses only the image cache it is given (a first-level in-memory cache, which must not block) and sends an `ImageRequest` to the queue, and it does not read or write the queue's disk cache, so `NoCache` should not matter. Not run yet on our queue; this is seen at once when `CoinImageLoader` is written. Two things to know: (1) `ImageLoader.get` throws if it is called off the main thread (binding rows in an adapter is on the main thread, so this is fine); (2) `ImageLoader` decodes images as `RGB_565`, which has no transparency, so icons with a transparent background may show a dark background: look at it once, and if it looks wrong, override `makeImageRequest` (a `protected` method of `ImageLoader`) in `CoinImageLoader` to decode with transparency. Whether the CoinGecko icons have a transparent background is not known.

### 8.2 Freshness (FR-DATA-01)
`RefreshPolicy` holds the intervals in one place: market list and watchlist 60 s, chart 1H/24H 60 s, 7D 5 min, 1M/1Y 15 min, coin detail 60 s. A repository keeps the last result in memory with its fetch time, keyed by request (endpoint, parameters, currency). If the stored result is younger than its interval, it is returned without calling the data source. Because 1H is cut from the 1-day dataset, 1H and 24H share the same stored response. The repository is the same with the remote and the fake data source, so this rule can be tried on fake data.

### 8.3 Errors (FR-ERR)
`ErrorMapper` turns every failure of the remote data source into one `AppError` with a category from `01`: no connection, timeout, rate limited, server error, request rejected, invalid data. This table is the single place where the mapping is defined; `05-interfaces.md` only refers to it. The fake data source creates the same `AppError` directly (it has no Volley error to map).

**Rule: status first, class second.** If the Volley error carries a network response (`error.networkResponse != null`), classify by its HTTP status code: 429 → rate limited, 5xx → server error, any other 4xx → request rejected. Only when there is no response, classify by the kind of failure. A failure that matches no row of the table is mapped to server error. `ErrorMapper` never reads the response body (CoinGecko error bodies come in two different shapes, `{"error":"coin not found"}` and `{"status":{"error_code":10002,...}}`, and the documentation gives no example of either) and never logs or shows the message of a `JSONException`, because that message can contain an excerpt of the data.

| Situation | What Volley gives | `AppError` category | Evidence |
|---|---|---|---|
| Airplane mode / no network | `NoConnectionError` (a subclass of `NetworkError`; any `NetworkError` without a response is treated the same) | No connection | Observed in S2 |
| TLS failure (certificate not trusted) | `NoConnectionError` | No connection (known limitation, D16) | Observed in S2 |
| No response within the timeout | `TimeoutError` | Timeout | Observed in S2 |
| HTTP 401 (missing or wrong key) | `AuthFailureError` with a network response | Request rejected | Observed in S2 |
| HTTP 404 | `ClientError` with a network response | Request rejected | Observed in S2 |
| HTTP 429 | `ClientError` (Volley treats 4xx other than 401/403 the same way) | Rate limited | **Prediction**, not observed; unit test with a fake error |
| HTTP 5xx | `ServerError` | Server error | **Prediction**, not observed; unit test with a fake error |
| Response is not valid JSON, or a mapper rejects a value (05 §4) | `JSONException` thrown by the mapper on the background thread (not a Volley error), or Volley's `ParseError` | Invalid data | Edge-case file `markets_invalid.json` |
| Any other failure (for example a 3xx status, or a `VolleyError` without a response and without a more specific class) | Other `VolleyError` | Server error | Team decision; no such case observed |

The 429 and 5xx rows are covered by a JVM unit test of `ErrorMapper` that builds Volley errors with a fake `NetworkResponse`; the real API is never pushed into those states on purpose.

After a 429, the repository refuses new calls to the data source for 60 s (FR-ERR-04). Because this rule is in the repository, it also applies to the fake `rate_limited` scenario.

### 8.4 JSON mapping
Mappers use `JSONObject`/`JSONArray` and return domain models. Fields that may be missing are nullable types (for example `Double?`), so "missing" (`null`) is different from 0 (FR-CD-06, FR-ML-09). An absent key and a JSON `null` are treated the same way (`04` §1.1). Which fields can be null is confirmed from real responses in S1 and recorded in `04-api-contract.md`.

The mappers are three plain Kotlin objects in `data/mapper`: `CoinMapper` (market list and coins by ID), `CoinDetailMapper` and `ChartMapper`. The remote and the fake data sources both call them, so a JSON text is read in the same way in both modes (D23; signatures in `05-interfaces.md`).

### 8.5 Fake data
Fake data sources implement the same data source interfaces as the remote ones, read the JSON files in `assets/fake`, parse them with the same mappers as the remote ones (section 8.4), and can produce the error scenarios. The repositories above them are identical in both modes (D19). Details in `fake-data.md`.

---

## 9. Navigation and screens

- `MainActivity` hosts two Fragments (`MarketListFragment`, `WatchlistFragment`) and owns the shared main top bar: the ⓘ icon (CMP-12), a two-segment pill toggle that switches the tabs (CMP-13, a Material `MaterialButtonToggleGroup`) and the currency selector (CMP-07). There is no bottom navigation and no swiping between tabs. **Both Fragments are added once and shown/hidden when the toggle changes**, so scroll position and ViewModel state are kept (FR-NAV-01). Replacing the Fragment on each tab switch would destroy that state, so it is not used. `MainActivity` does not draw the footer bar: each screen has its own (section 11).
- `CoinDetailActivity` is a separate Activity opened with an explicit `Intent` carrying the string extra `coin_id` (contract C6). Its top bar has the back arrow, the coin name, the currency selector (Should, FR-CUR-05) and the favorite star. Because `MainActivity` stays underneath, going back shows the previous tab as it was (FR-NAV-03); if the currency was changed on Detail, the list reloads in the new currency on return (search and sort kept, scroll position not guaranteed).
- The app is locked to portrait in the manifest (NFR-11), so rotation is not a case to handle.
- Setup note: check in the first project build that content does not draw under the status bar (the layout behaviour depends on the target SDK).

---

## 10. Build settings and keys

- The build reads `USE_FAKE_DATA`, `FAKE_SCENARIO`, `FAKE_DELAY_MS` and `CG_API_KEY` from the developer's `local.properties` (never committed) into `BuildConfig`. If the file or a value is missing, defaults apply (fake data, `normal`, 800 ms), so CI builds without a key (NFR-07, NFR-12). The Gradle `buildConfig` option (`buildFeatures { buildConfig = true }`) must be enabled for this. Confirmed in S2: with this option, reading `local.properties` in the Kotlin build script works with the Android Gradle Plugin version of the scratch project, and a missing key gives an empty string.
- `CryptoApp` picks the fake or the remote data sources once, at startup, and builds the repositories on top of them. No other class checks the flag.
- The key is sent in a header, not in the URL. It still ends up inside the APK, which is acceptable only because the app is not published (`01`, section 2.3).

---

## 11. Shared UI parts

- **Theme:** one dark theme; colors, text styles and spacing defined once in resources by Hải. Layouts and Kotlin code use these names only (no hard-coded colors, sizes, strings).
- **Strings:** English default in `values/`, Vietnamese in `values-vi/`.
- **Formatters:** plain Kotlin classes with a fixed locale (rules in `01`, §5.2). Dates use `SimpleDateFormat`/`Date`, which works on API 24 without extra setup. Always pass `Locale.US` to `String.format` (or the `format` extension): without a locale it uses the device's default locale, so a device whose language uses a comma as decimal separator would break the fixed format. `%f` rounds half up, which is the rule of `01` §5.2. `DecimalFormat` rounds half even by default, so it is not used for these formats unless its rounding mode is set to half up. Both statements come from the Java SE specification (section 18); the Android reference pages could not be read, so the behaviour on Android is not checked in the docs: the unit tests of the formatters (for example `0.005` → `0.01`, `999,996` → `1.00M`) are the check.
- **StateView:** shows loading, empty or error (with Retry) from a `UiState`.
- **Coin icons:** shown with Volley's `NetworkImageView` and the `CoinImageLoader` that `CryptoApp` provides (contract C8, section 8.1). The placeholder and the error image are drawables in the shared resources (Hải). A layout never loads an image in any other way.
- **Currency:** the `Currency` type itself (USD, VND) is a domain model in `domain/model`, because the repository interfaces take it (section 3, D18). `CurrencySettings` stores the choice in `SharedPreferences` and exposes it as LiveData. A screen's Fragment observes it and tells its ViewModel to reload when it changes (FR-CUR-04). The selector sits at the right end of the main top bar (both tabs) and, as a Should, in the Detail top bar (FR-CUR-05); both use the same saved value.
- **Favorites:** `FavoriteStore` saves the coin IDs as one JSON array string in `SharedPreferences`, because a `SharedPreferences` string *set* has no order and FR-WL-03 needs "oldest first". It enforces the limit of 50 and exposes the list as LiveData, so a star tapped on one screen updates the others (FR-CD-08).
- **Layout rules (NFR-13):** every layout and every PR follows these. Hải owns them and checks them in review.
  1. Sizes in `dp`, text in `sp`. Never `px`. Never set text size in Kotlin code.
  2. Take colors, text styles, spacing and corner sizes only from the theme resources (no literal values in layouts).
  3. Use `ConstraintLayout` (or `LinearLayout` with weights). Widths are `0dp` (match constraint) or `wrap_content`. No fixed width on a container, and no fixed height on any view that holds text (use `minHeight`).
  4. Every text view that can hold variable text sets `maxLines` and `ellipsize="end"`. In a coin row, the coin name shrinks first and the price is never cut. Number columns use `minWidth`, not a fixed width.
  5. Touch targets are at least 48×48 dp (NFR-08), even if the icon is smaller (use padding or `minWidth`/`minHeight`).
  6. A screen that can be taller than the display (Coin Detail) scrolls (`NestedScrollView` or `RecyclerView`). Lists use `RecyclerView`.
  7. Charts, sparklines and images take their size from the parent (constraints) or from `dimens`. Custom Views read their size in `onSizeChanged`/`onMeasure`, never from constants.
  8. Do not use `ScrollView` inside `ScrollView`, and do not design for only one device: run the two checks below before opening a PR.
  - **Two checks per screen:** emulator 360×640 dp and emulator 411×891 dp, both at the default font size. Also try the longest realistic content: a long coin name, a 7-digit price, and a VND market cap (`32.50T ₫`).
- **Footer bar (`AttributionView`):** the only bottom bar, fixed at the bottom of the screen on Market, Watchlist and Detail, with "Powered by CoinGecko" (link) on the left and, on Market and Watchlist, the last-updated time with a clock icon on the right (FR-INFO-01, FR-DATA-03). The time comes from the `UiState` success value, so it stays correct while old data is kept after an error. Each of the three screens puts its own `AttributionView` in its own layout; `MainActivity` does not draw one, because the two tabs have different fetch times and the shell would otherwise have to read the ViewModel of the visible tab.
- **Sparkline:** a small custom `View` drawing a line on a `Canvas`, with no chart library, because 100 rows are drawn at once.

---

## 12. Contracts between features

| # | Contract | Provider → user | Shape (exact signatures in `05`) |
|---|---|---|---|
| C1 | Models and repository interfaces | Vân Khánh → all | Repositories take a currency and a callback; success carries the data and its fetch time; failure carries an `AppError` |
| C2 | Formatters, `StateView`, `UiState`, `AttributionView` (footer bar), explanation dialog (CMP-12), style guide | Hải → all | Plain Kotlin formatters (no Android types); `UiState` with its four cases; `StateView` shows a `UiState`; `AttributionView` shows the footer bar (CMP-08) and takes the fetch time, or none (Detail), and each screen includes it in its own layout; the explanation dialog takes a title and a text (`02` CMP-12); one sample coin row as the style reference |
| C3 | Favorites | Bảo Linh → An Khánh, Kiên | `FavoriteStore` (check whether a coin is a favorite, add, remove, toggle, the list of IDs as LiveData; the exact members are in the code, `05` §1) and `FavoriteButton` |
| C4 | Coin row and adapter | An Khánh → Bảo Linh | Takes the coin data and reports the row tap. The star is a `FavoriteButton` (C3) bound to `FavoriteStore`: it toggles the store itself and reports when the limit of 50 stops an add, and a screen that removes rows (Watchlist) observes `favoriteIds`. The star is part of the row: always on Watchlist rows, on Market rows only if FR-ML-08 is built |
| C5 | Chart | Duy → Kiên | `PriceChartFragment.newInstance(coinId)`: needs only the coin ID, shows timeframe selector, chart and its own states |
| C6 | Open Detail | Kiên → An Khánh, Bảo Linh | Explicit `Intent`, string extra `coin_id` |
| C7 | Currency | Hải → all ViewModels, Kiên | Current currency, change notification, and the selector view (main top bar, Detail top bar) |
| C8 | Coin icons | Vân Khánh → An Khánh, Kiên | `CoinImageLoader` from `CryptoApp`, used with Volley's `NetworkImageView` in the layout; placeholder and error drawables come from the shared resources (Hải) |

The data source interfaces (section 3) are not a contract: only the repository implementations inside the Data layer use them.

---

## 13. Chart architecture and the sandbox

The chart is the riskiest part, so it is built to be developed and tested **alone but through the same contracts as the real app**.

**Structure inside `features/chart/`:**
- `PriceChartFragment`: the only class other features use (C5). Contains the timeframe selector, the chart and a `StateView`.
- `PriceChartViewModel`: current timeframe, state, request handling. Calls `ChartRepository`.
- `PriceChart` (interface) and its implementation: the chart drawing, wrapped so the rest of the feature does not depend on the chart library. If the library has to be replaced (fallback: a basic line chart), only the implementation changes.
- Area fill, reference line and axis labels (FR-CD-11, 12, 13) belong to the `PriceChart` implementation. If the chosen library cannot do one of them, that requirement is cut (see the cut order in `01`), not rebuilt by hand.
- `TimeframeSlicer` (plain Kotlin): applies the 1H rule from `01` (FR-CD-03) and is unit-tested.
- Chart library: **MPAndroidChart v3.1.0** (a Java library, JitPack), used from Kotlin. The S3 spike (Duy) reported that it works in a Kotlin + XML project. Implementation notes: x-axis values are `float` (use the point index or an offset, not a millisecond timestamp), the marker on touch, colours for the dark theme.

**Chart sandbox (debug builds only):**
- `ChartSandboxActivity` lives in the `debug` source set and has its own launcher entry. It hosts `PriceChartFragment` for a fixed coin ID, running on fake data from `assets/fake`, with the real `ChartRepository` interface, `CurrencySettings`, formatters and `StateView`.
- Duy tests all five timeframes, the "Not enough data" case (the short chart file), VND formatting and error scenarios here, without waiting for Detail or the network layer.
- When Kiên's `CoinDetailActivity` exists, it only places `PriceChartFragment` in a container. No chart code is written in Detail.
- Until C1 and C2 exist (end of day 2; C2 no later than day 3) and the repositories run on the fake data sources (day 3), Duy writes against the interfaces with small stubs in his own package and replaces them when the real ones land. Only the reload on a currency change (Should, FR-CUR-05) needs C7 (day 3).

---

## 14. How to build a feature (recipe for every owner)

1. Write the screen's `UiState`, `ViewModel` and its small factory first; expose `LiveData<UiState<…>>`.
2. Call only repository **interfaces** from the ViewModel; never Volley, JSON or a data source.
3. Put pure logic (filter, sort, slicing, mapping) in plain Kotlin classes and test them in JVM unit tests.
4. In the Fragment, observe the LiveData and render with the adapter and `StateView`; put the footer bar (`AttributionView`) in the screen's own layout.
5. Take every color, size and string from the shared resources and formatters; show coin icons with `NetworkImageView` and the `CoinImageLoader` (C8).
6. Run on fake data (including the error scenarios), then on real data.

---

## 15. Testing approach (details in `07-test-plan.md`)

| Layer | Tested by | Needs |
|---|---|---|
| Formatters, filter/sort, timeframe slicing | JVM unit tests | Nothing from Android |
| Mappers | JVM unit tests with the JSON files from `assets/fake` | `org.json` added as a test dependency |
| `ErrorMapper` | JVM unit tests with fake Volley errors (429, 5xx and the observed cases of section 8.3) | Volley on the test classpath |
| Repositories | JVM unit tests with a fake data source and a fake clock (refresh intervals, pause after a 429) | – |
| ViewModels | JVM unit tests | A rule that runs LiveData synchronously in tests (a Jetpack test library; confirm at setup) |
| Screens, coin icons | Manual checklist on the emulator and the demo device | – |

---

## 16. Decision log

| # | Decision | Why | Rejected alternative | Status |
|---|---|---|---|---|
| D1 | Native Android, Kotlin + XML Views, layered (UI / ViewModel / Domain / Data) | Kotlin accepted by the instructor, Jetpack Compose not accepted; separation of concerns; parallel work | Single-layer Activities that fetch and parse themselves | Decided |
| D2 | ViewModel + LiveData for screen state | Lifecycle safety, survives tab switching, testable | Plain listeners (each Fragment must register/unregister correctly) | Decided |
| D3 | Manual dependency injection through `CryptoApp` | Few classes, easy to explain | Hilt (extra setup, annotation processing) | Decided |
| D4 | Volley for HTTP, `StringRequest`, one `RequestQueue` | Simple, asynchronous out of the box; latest stable release 1.2.1 | Retrofit/OkHttp (more concepts to learn) | Decided; works on the API 24 emulator (S2) |
| D5 | `JSONObject`/`JSONArray` mappers | No extra library; nullable types for missing values | Gson/Moshi | Decided |
| D6 | `ExecutorService` + main-thread `Handler`, callbacks on the main thread | Platform classes only, no extra library; `AsyncTask` is deprecated | Kotlin coroutines (extra library, new concept); `AsyncTask`; parsing inside Volley's own thread (possible, but less explicit) | Decided; S2 measured 187–332 ms to parse 100 coins, which confirms parsing must stay off the main thread |
| D7 | No Volley cache: `RequestQueue(NoCache(), BasicNetwork(HurlStack()))`; in-memory data with refresh intervals | No market data on the device; one place decides freshness; cannot be forgotten per request | Volley disk cache; `setShouldCache(false)` on each request | Decided (updated 19 Oct 2026) |
| D8 | Icons: Volley `ImageLoader` + in-memory cache on the same `NoCache` queue, shown with `NetworkImageView` | One networking library | Glide/Picasso | Decided, revisit if slow. Checked 21 Oct 2026 in the Volley release notes and the `ImageLoader` source (section 8.1); not yet run on our queue. S2 loaded icons (about 17.5 KB each) and no credits were charged |
| D9 | Chart behind `PriceChart`; MPAndroidChart v3.1.0 baseline | Saves time on touch interaction | Custom Canvas chart (more work, more risk) | Decided (S3 passed; fallback if a later problem appears: basic line chart) |
| D10 | `SparklineView` drawn with `Canvas` | 100 rows; a library per row would be too heavy | Chart library per row | Decided |
| D11 | Favorites and currency in `SharedPreferences` (favorites as one JSON array) | Small data; order must be kept | SQLite | Decided |
| D12 | Fake and remote data sources behind the same data source interfaces, chosen at startup | No quota use in development; repeatable tests | Hand-written test data in code | Decided; the layer on top is D19 |
| D13 | Tabs are Fragments shown/hidden; Detail is its own Activity | Keeps scroll and state; simple back behaviour | Replace Fragments; Detail as Fragment | Decided |
| D14 | Chart is a Fragment with one input (`coin_id`) | Detail and chart developed separately | Detail passes data to a chart view | Decided |
| D15 | Network Security Configuration that trusts ISRG Root X1 and X2 for `coingecko.com` (and subdomains), plus the system certificates | Android 7.0 does not trust these roots, and minSdk is 24; without it every request fails on API 24 (S2) | "Trust all" certificates (insecure, never); raising minSdk to 25 or higher (does not cover X2 either, and drops devices); a different host | Decided 19 Oct 2026; verified in S2 |
| D16 | A failed TLS connection is reported as "No internet connection" (no separate category) | Volley reports it as `NoConnectionError`, the same as no network; a separate category would need reading the exception cause, for a rare case | A seventh error category for TLS | Decided 19 Oct 2026; known limitation recorded in `01`, section 4.7 |
| D17 | Tabs are switched with a pill toggle (`MaterialButtonToggleGroup`) in a shared top bar owned by `MainActivity`; no bottom navigation, no tab row, no swiping | With only two destinations a bottom bar looks empty and stacks with the footer bar; the toggle keeps one top bar for ⓘ, tabs and currency; swiping with ViewPager2 would need extra code and the lecture slides say Fragments beyond the off-screen limit are destroyed | Bottom navigation; TabLayout with ViewPager2; a separate tab row | Decided 20 Oct 2026; pill look and fit at 360 dp not verified yet |
| D18 | `Currency` (USD, VND) is a domain model in `domain/model`; `CurrencySettings` and the selector stay in `core/currency` | The repository interfaces (Domain) take a currency, and Domain must not depend on `core` | `Currency` inside `core/currency` (Domain would have to import from `core`) | Decided 20 Oct 2026 |
| D19 | The repositories (in-memory data, refresh intervals, pause after a 429) are the same classes in fake and real mode and sit on top of data source interfaces; the remote and the fake data source are the two implementations. The data source interfaces are in `data/`, used only by the repositories | The refresh rule (FR-DATA-01) and the pause after a 429 (FR-ERR-04) are Must and live in the repository; if the fake replaced the repository, they could only be tried on the real API (day 5 or later) and cost quota. The Android data layer guide makes the repository the only entry point of the data layer and allows an in-memory cache inside it | The fake implements the repository interface directly (one interface less, but the two rules cannot be tried on fake data); data source interfaces in `domain/` (ViewModels could call them, against the guide) | Decided 21 Oct 2026 |
| D20 | `CoinImageLoader` is a concrete class in `core/image` (not in `data/`, no interface), created in `CryptoApp`; layouts show icons with `NetworkImageView` (contract C8) | Features may use `core` but not `data`, and the icon is needed by the coin row and the Detail header; icons are pictures, not market data; nobody needs a fake of it (icons load from the network in both modes, `fake-data.md` §6); `NetworkImageView` is made for lists and handles views that are reused | `CoinImageLoader` in `data/image` (features may not import it); an interface with a fake (extra work, no user); Glide/Picasso (second networking stack, D4, D8) | Decided 21 Oct 2026 |
| D21 | The footer bar (`AttributionView`) belongs to each screen: Market, Watchlist and Detail each include it in their own layout; `MainActivity` does not draw one | The two tabs show different fetch times; with one footer in the shell, `MainActivity` would have to read the ViewModel of the visible tab, a coupling no contract covers. One view used in three layouts costs nothing extra | One footer in the shell for the two tabs (needs a channel from both ViewModels to `MainActivity`) | Decided 21 Oct 2026 |
| D22 | Each feature owner writes the ViewModel factory of their own screens, in their own package; `CryptoApp` only exposes the repositories and the other shared objects as properties | A shared factory in `app/` would have to know every ViewModel, so four owners would edit one file of the team lead (merge conflicts that break the build for everyone) and the team lead would wait for every ViewModel constructor. Per-feature factories keep the rule of one owner per folder (section 3); `viewModelFactory { initializer { … } }` keeps each one to a few lines | One `ViewModelFactory` in `app/` with a `when` over every ViewModel class | Decided 22 Oct 2026; the factory API is checked in the Android documentation (section 18), not yet compiled in the project |
| D23 | The three JSON mappers (`CoinMapper`, `CoinDetailMapper`, `ChartMapper`) are plain Kotlin objects in their own package `data/mapper`, used by both the remote and the fake data sources | Both kinds of data source must read JSON in the same way: the scenarios `invalid_json` and `html_description` (`fake-data.md` §5.2) work through files, so they test the real mapping code only if the fake data sources use the same mappers. A package of its own keeps the dependencies clear: `remote` → `mapper` and `fake` → `mapper`, and `fake` does not import the network code | Mappers inside `data/remote` (the fake data sources would import from the package of the network code); parsing inside each data source (two copies that can differ) | Decided 23 Oct 2026. The Android data layer guide recommends separate model classes for external data (section 18) but gives no rule for the package of the mapping code, so the package choice is the team's own |

---

## 17. Checks still open (results go to `04` or this file)

Closed by S2 (19 Oct 2026): the thread of Volley's listener and the parse time; the Volley exceptions for no connection, timeout, 401 and 404; HTTPS on API 24; the `buildConfig` option.

| Check | When | Affects |
|---|---|---|
| Volley errors for HTTP 429 and 5xx: only predicted (section 8.3); covered by an `ErrorMapper` unit test, not by the real API | Data layer | Section 8.3 |
| `CoinImageLoader` on our `NoCache` queue (expected to work, section 8.1); how the icons look (`RGB_565` has no transparency) and how fast memory-only icons load | When `CoinImageLoader` is written | D8, D20 |
| A6: icons are free, or only cache hits are free (all measured requests were cache hits) | Data layer or later spot check | `01` A6, D8 |
| Same HTTPS check on the demo emulator (API 36) | First integration build | `01` A5 |
| Content under the status bar; the ViewModel and LiveData libraries (`lifecycle-viewmodel`, `lifecycle-livedata`; not in `libs.versions.toml` on 22 Oct 2026, so the version must be added and checked with `:app:dependencies`); whether `viewModelFactory { initializer { … } }` compiles with that version; the test libraries | First step of the data layer work (`06`, section 4, day 2) | Sections 6, 9, D22 |
| Pill look of the toggle (button shape in Material components) and the top bar fit at 360 dp with English and Vietnamese labels | First build of the top bar (Hải) | Section 9, D17 |

---

## 18. References
Read on 13 Oct 2026:
- Android Developers, *Guide to app architecture*: https://developer.android.com/topic/libraries/architecture/
- Android Developers, *LiveData overview*: https://developer.android.com/topic/libraries/architecture/livedata
- Android Developers, *Threading in Android (background work)*: https://developer.android.com/guide/background/threading
- Volley releases: https://github.com/google/volley/releases
- MPAndroidChart: https://github.com/PhilJay/MPAndroidChart

Read on 19 Oct 2026:
- Let's Encrypt, *Certificate Compatibility* (ISRG Root X1 trusted from Android 7.1.1, ISRG Root X2 from Android 14; the root certificates to download): https://letsencrypt.org/docs/certificate-compatibility/
- Android Developers, *Network security configuration* (`domain-config`, `trust-anchors`, certificates in `res/raw`): https://developer.android.com/privacy-and-security/security-config
- Volley documentation and source (`RequestQueue`, `NoCache`, `BasicNetwork`, `DefaultRetryPolicy`): https://github.com/google/volley
- CoinGecko, *API Terms of Service* (5 Sept 2025), clauses 2.6 and 6: https://www.coingecko.com/en/api_terms

Read on 21 Oct 2026:
- Android Developers, *Data layer* (the repository is the only entry point of the data layer; other layers never use data sources directly; a repository can use an in-memory cache as its source of truth; naming of data sources): https://developer.android.com/jetpack/guide/data-layer
- Volley releases (1.2.0: default and error images for `NetworkImageView`; 1.2.1: Cronet separated, nothing else): https://github.com/google/volley/releases
- Volley source, `ImageLoader.java` (first-level in-memory image cache, `ImageRequest` sent to the queue, main-thread check, `RGB_565` decoding, `makeImageRequest` is `protected`): https://android.googlesource.com/platform/frameworks/volley/+/refs/heads/main/src/main/java/com/android/volley/toolbox/ImageLoader.java

Read on 21 Oct 2026 (resources):
- Android Developers, *Providing resources* ("you can name the file whatever you want and place different resource types in one file"): https://developer.android.com/guide/topics/resources/providing-resources
- Android Developers, *String resources* ("The filename is arbitrary. The `<string>` element's `name` is used as the resource ID."): https://developer.android.com/guide/topics/resources/string-resource

Read on 21 Oct 2026 (number formatting):
- Oracle, Java SE 17 API, `java.util.Formatter` (the `f` conversion rounds with the round half up algorithm; with no explicit locale the default locale is used): https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Formatter.html
- Oracle, Java SE 17 API, `java.text.DecimalFormat` (rounding mode is half even by default; a new instance uses the default FORMAT locale): https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/text/DecimalFormat.html
- Kotlin standard library, `String.format` (uses the default locale unless one is passed; syntax of `java.util.Formatter`): https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.text/format.html

Read on 22 Oct 2026 (ViewModel factories):
- Android Developers, *ViewModel factories* (a ViewModel with constructor dependencies needs a factory that implements `ViewModelProvider.Factory`; the `viewModelFactory { initializer { … } }` form; `CreationExtras`): https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-factories
- Android Developers, *Lifecycle releases* (artifacts `lifecycle-viewmodel` and `lifecycle-livedata`): https://developer.android.com/jetpack/androidx/releases/lifecycle
- Android Developers, *Activity releases* (a release note says the `lifecycle-runtime` dependency was updated to 2.10.0; which `activity` version it belongs to is not confirmed): https://developer.android.com/jetpack/androidx/releases/activity

Read on 23 Oct 2026 (mappers):
- Android Developers, *Data layer* (current address of the page above; it recommends separate model classes for data from outside the app, at least where a data source's data does not match what the rest of the app expects, and data sources behind interfaces so that fakes can be used in tests; it gives no rule for the package of the mapping code): https://developer.android.com/topic/architecture/data-layer
