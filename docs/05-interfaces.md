# 05 – Interfaces

**Project:** Crypto Dashboard (Android, native Kotlin, XML Views) | **Version:** 1.0 <br>
**Related:** `03-architecture.md` (why the structure is like this), `04-api-contract.md` (the JSON these models come from), `06-task-plan.md` (contracts C1 to C8 and owners), `fake-data.md` (fake data sources). <br>
**Purpose of this file:** the exact Kotlin signatures that members code against. The reasons are in `03`; this file only states the shapes.

---

## 1. Conventions

- Package root: `vn.edu.usth.cryptodashboard`. Models are in `domain/model`, repository interfaces and `RepositoryCallback` in `domain/repository`.
- Domain code uses no Android types (`03` §2).
- **Missing values are `null`, never 0.** An absent JSON key and a JSON `null` both become `null` (`04` §1.1). A real value of 0 stays 0 (FR-CD-06, FR-ML-09).
- Every money amount, supply and percentage is `Double?`, because the API returns integers and decimals for the same field and `market_cap` can exceed `Int` (`04` §1.1). The formatters of `core/format` take `Double?`.
- **Required fields:** `id`, `symbol` and `name` of a coin, and `prices` of a chart response; every other field is nullable. What a mapper does with a missing or wrong value is in section 4.
- **Threading rule (C1):** every repository callback, `onSuccess` and `onError`, is called on the main thread. The caller can update LiveData and Views directly. Network and JSON work happen on other threads inside the data layer (`03` §4, §5).
- A repository call cannot be cancelled. A ViewModel that sends a new request ignores the answer of an older one (`03` §6, stale responses).
- **This file does not copy signatures that exist in code.** The shared UI parts (`UiState`, `StateView`, `AttributionView`, the explanation dialog, the formatters), `CurrencySettings`, `FavoriteStore` and the coin row with its adapter are defined by their owners in their own files (C2, C3, C4, C7); read the code on `main`. The contract of the coin row (C4) is in `03` §12. C5 and C6 are complete in `03` §12.

---

## 2. Shared types (`domain/model`)

```kotlin
enum class Currency { USD, VND }

sealed interface AppError {
    object NoConnection : AppError
    object Timeout : AppError
    object RateLimited : AppError
    object ServerError : AppError
    object RequestRejected : AppError
    object InvalidData : AppError
}
```

`AppError` has the six categories of `01` §4.7. How a failure is mapped to a category is defined only in `03` §8.3.

```kotlin
data class Coin(
    val id: String,
    val symbol: String,
    val name: String,
    val imageUrl: String?,
    val currentPrice: Double?,
    val marketCap: Double?,
    val marketCapRank: Int?,
    val priceChangePercentage24h: Double?,
    val sparkline7d: List<Double>?,
)

data class CoinDetail(
    val id: String,
    val symbol: String,
    val name: String,
    val imageUrl: String?,
    val description: String?,
    val currentPrice: Double?,
    val priceChangePercentage24h: Double?,
    val marketCap: Double?,
    val totalVolume: Double?,
    val circulatingSupply: Double?,
    val totalSupply: Double?,
    val maxSupply: Double?,
    val allTimeHigh: Double?,
    val allTimeLow: Double?,
)

data class ChartPoint(
    val timestampMillis: Long,
    val price: Double,
)

enum class Timeframe(val days: Int) {
    H1(1),
    H24(1),
    D7(7),
    M1(30),
    Y1(365),
}
```

Notes:
- `Coin` is used by Market and Watchlist. `marketCapRank` is the provider's rank and does not change when the user sorts (FR-ML-01).
- `Coin.sparkline7d` has 168 points for most coins, but the length varies, so never assume a fixed length (`04` §1.1).
- `CoinDetail.description` is **plain text**: the mapper removes markup, so the screen only shows it (FR-CD-07). `null` means no description (a missing key, `null` or an empty string).
- `CoinDetail.totalSupply` and `maxSupply`: a `null` means the statistics row is not shown (FR-CD-06).
- `CoinDetail` has no ATH/ATL date and no distance from the all-time high. They are added if FR-CD-09 (Could) is built.
- `ChartPoint.timestampMillis` is milliseconds since 1970, UTC.
- `Timeframe.days` is the value of the `days` request parameter (`04` §5). The constants are named after the labels 1H, 24H, 7D, 1M, 1Y; Kotlin does not allow a name that starts with a digit. The text on screen comes from string resources, not from the constant names.

---

## 3. Repositories (`domain/repository`)

```kotlin
interface RepositoryCallback<T> {
    fun onSuccess(data: T, fetchedAtMillis: Long)
    fun onError(error: AppError)
}

interface MarketRepository {
    fun getTopCoins(currency: Currency, callback: RepositoryCallback<List<Coin>>)
    fun getCoinsByIds(coinIds: List<String>, currency: Currency, callback: RepositoryCallback<List<Coin>>)
}

interface CoinDetailRepository {
    fun getCoinDetail(coinId: String, currency: Currency, callback: RepositoryCallback<CoinDetail>)
}

interface ChartRepository {
    fun getChart(coinId: String, timeframe: Timeframe, currency: Currency, callback: RepositoryCallback<List<ChartPoint>>)
}
```

Rules:
- **`fetchedAtMillis`** is the time the data was fetched, as milliseconds since 1970. When a repository returns data it already holds because it is younger than its refresh interval (FR-DATA-01), it returns the original fetch time, not the time of the call. The ViewModel passes it on as `UiState.Success.updatedAtMillis`, which the footer shows (FR-DATA-03).
- **`getTopCoins`:** the first 100 coins in market-cap order, as the provider returns them (FR-ML-01). Fewer than 100 is not an error.
- **`getCoinsByIds`:** the coins of the given IDs, in the provider's market-cap order, not in the order of `coinIds` (`04` §3). An ID the provider has no data for is **missing from the list, without an error** (FR-WL-09). The Watchlist ViewModel puts the rows in the order the coins were added (FR-WL-03) and shows "Data unavailable" for an ID that is not in the answer.
- **`getChart`:** the points of the requested timeframe in time order, in the requested currency. `H1` returns the whole 1-day dataset, the same as `H24` (the two share one stored response, `03` §8.2); cutting the last 60 minutes is the job of `TimeframeSlicer` (FR-CD-03, `03` §13). A list with fewer than 2 points is not an error: the chart shows "Not enough data".
- Within the refresh interval of FR-DATA-01 a repository makes no network call. For 60 s after an `AppError.RateLimited` it makes none either (FR-ERR-04). Both rules live in the repository implementation, so they work the same on fake data (`03` D19).
- Changing the currency or the timeframe is a new request: the repository treats each combination of coin, timeframe and currency as a separate piece of data.

---

## 4. Data sources (`data/source`)

The data source interfaces are internal to the Data layer: only the repository implementations use them, and they are not a contract between members (`03` §2, §12). `DataSourceCallback` and the four interfaces are in `data/source`. Each interface has a remote implementation in `data/remote` and a fake implementation in `data/fake`; both turn JSON into models with the mappers of `data/mapper` (below). The names follow *type of data + type of source + DataSource* (`03` §3).

```kotlin
interface DataSourceCallback<T> {
    fun onSuccess(data: T)
    fun onError(error: AppError)
}

interface MarketListDataSource {
    fun getTopCoins(currency: Currency, callback: DataSourceCallback<List<Coin>>)
}

interface CoinsByIdsDataSource {
    fun getCoinsByIds(coinIds: List<String>, currency: Currency, callback: DataSourceCallback<List<Coin>>)
}

interface CoinDetailDataSource {
    fun getCoinDetail(coinId: String, currency: Currency, callback: DataSourceCallback<CoinDetail>)
}

interface ChartDataSource {
    fun getChart(coinId: String, days: Int, currency: Currency, callback: DataSourceCallback<List<ChartPoint>>)
}
```

| Kind of data | Interface | Remote implementation | Fake implementation |
|---|---|---|---|
| Top 100 | `MarketListDataSource` | `MarketListRemoteDataSource` | `MarketListFakeDataSource` |
| Coins by ID | `CoinsByIdsDataSource` | `CoinsByIdsRemoteDataSource` | `CoinsByIdsFakeDataSource` |
| Coin detail | `CoinDetailDataSource` | `CoinDetailRemoteDataSource` | `CoinDetailFakeDataSource` |
| Chart | `ChartDataSource` | `ChartRemoteDataSource` | `ChartFakeDataSource` |

Rules:
- **Same threading rule as the repositories:** `onSuccess` and `onError` are called on the main thread (`03` §4, §5).
- A data source returns models only. It does not know about refresh intervals, the pause after a 429 or the fetch time: the repository decides when to call a data source and stamps `fetchedAtMillis` when the answer arrives.
- A data source reports a failure as an `AppError`. The remote implementations map Volley and JSON failures with `ErrorMapper` (`03` §8.3); the fake implementations create the `AppError` directly (`fake-data.md` §4).
- `getChart` takes `days` (the value of `Timeframe.days`), not a `Timeframe`: the request has no 1H, and the repository gives `H1` and `H24` the same stored answer.
- `getCoinsByIds` follows the same rule as the repository: an ID without data is missing from the list, without an error.
- What the fake implementations answer for each request and each scenario is in `fake-data.md` §2 and §5.2.

### JSON mappers (`data/mapper`)

The remote and the fake implementations turn the JSON text into models with three plain Kotlin objects. They use no Android types and read the JSON with `org.json` (`03` §8.4, D23).

```kotlin
object CoinMapper {
    fun parseList(json: String): List<Coin>
}

object CoinDetailMapper {
    fun parse(json: String, currency: Currency): CoinDetail
}

object ChartMapper {
    fun parse(json: String): List<ChartPoint>
}
```

Rules:
- **Input:** the whole response text. `CoinMapper.parseList` reads a `/coins/markets` array (the market list and coins by ID); `CoinDetailMapper.parse` reads a `/coins/{id}` object, and `currency` selects the key of the fields that are keyed by currency (`04` §4); `ChartMapper.parse` reads a `/market_chart` object and uses only `prices` (`04` §5).
- **Failure:** a mapper throws `org.json.JSONException` when the text is not valid JSON, when a required field (section 1) is missing or `null`, or when anything it reads has a JSON type other than the one `04` lists for it (a number written as a string, such as `"12"`, has the wrong type). An optional field that is absent or `null` is not a failure: it becomes `null`. The data source catches the exception on the background thread and reports `AppError.InvalidData`; nobody shows the message of the exception (`03` §8.3).
- **Missing values** become `null` as in section 1. `CoinDetailMapper` removes the markup of the description (section 2).
- A mapper runs on the background thread of the data source, never on the main thread (`03` §5).

---

## 5. Coin icons (`core/image`, contract C8)

```kotlin
class CoinImageLoader(requestQueue: RequestQueue) : ImageLoader(requestQueue, /* in-memory image cache */)
```

`CoinImageLoader` extends Volley's `ImageLoader` and keeps the icons in memory only (`03` §8.1, D20). `CryptoApp` creates one instance on the shared `RequestQueue` and hands it out; a layout shows an icon with Volley's `NetworkImageView`:

```kotlin
iconView.setDefaultImageResId(R.drawable.ic_coin_placeholder)
iconView.setErrorImageResId(R.drawable.ic_coin_placeholder)
iconView.setImageUrl(coin.imageUrl, coinImageLoader)
```

Rules:
- Call `setDefaultImageResId` and `setErrorImageResId` **before** `setImageUrl`, with the placeholder drawables from the shared resources (Hải).
- `imageUrl` can be `null` or empty: `NetworkImageView` then cancels any earlier request and shows the default image, or nothing if none is set. It does not throw (Volley source, `NetworkImageView.loadImageIfNecessary`, read 22 Oct 2026 on the `main` branch, not on the 1.2.1 tag).
- Call `setImageUrl` on the main thread. Binding a row in an adapter already is.
- A layout never loads an icon in any other way (`03` §11).
- Not yet run on our queue: that `ImageLoader` works with the `NoCache` queue and how icons with a transparent background look (`03` §17). Neither can change this signature or the calls above.

---

## 6. Shared objects (`app/CryptoApp`) and ViewModel factories

`CryptoApp` creates the shared objects once at startup (`03` §7, §10) and exposes these properties:

```kotlin
class CryptoApp : Application() {
    val marketRepository: MarketRepository
    val coinDetailRepository: CoinDetailRepository
    val chartRepository: ChartRepository
    val coinImageLoader: CoinImageLoader
    val currencySettings: CurrencySettings
    val favoriteStore: FavoriteStore
}
```

The first three types are in section 3 and `CoinImageLoader` is in section 5. `CurrencySettings` (C7, Hải) and `FavoriteStore` (C3, Bảo Linh) are defined in their own files.

Rules:
- The repository properties have the interface types. A member never knows whether the data is fake or real (`03` D19).
- The `RequestQueue`, the `ExecutorService` and the `Handler` are not exposed: only the data layer uses them.
- **Getting the objects:** in a Fragment `requireActivity().application as CryptoApp`; in an Activity `application as CryptoApp`. The manifest declares `CryptoApp` as the application class (Vân Khánh).
- A ViewModel never receives `CryptoApp` or a `Context`: its factory passes only the objects it needs (`03` §3, `AGENTS.md` §5).
- **Each feature writes the factory of its own ViewModel** in its own package (`03` §6, D22). This is an example of the shape; the ViewModel and its constructor belong to the owner of the feature:

```kotlin
fun marketViewModelFactory(app: CryptoApp): ViewModelProvider.Factory = viewModelFactory {
    initializer { MarketViewModel(app.marketRepository, app.currencySettings) }
}

// in the Fragment
val app = requireActivity().application as CryptoApp
val viewModel = ViewModelProvider(this, marketViewModelFactory(app)).get(MarketViewModel::class.java)
```

A ViewModel that needs an ID (Detail, chart) takes it as a constructor parameter of its factory function, for example `detailViewModelFactory(app, coinId)`; the `coin_id` comes from the Intent (C6) or from `PriceChartFragment.newInstance(coinId)` (C5).

- Not yet compiled in the project: the `viewModelFactory { initializer { … } }` form comes from the Android documentation (`03` §18); the lifecycle libraries are not in `libs.versions.toml` yet (`03` §17). The imports are added by Android Studio.

---

## 7. Fake data sources (`data/fake`)

The four fake data sources of section 4 implement the same interfaces as the remote ones. `Scenario`, `FakeFileReader` and the four fake data sources are in `data/fake`; `MainThreadPoster` is in `data/thread`, because the repositories and the remote data sources use it too (`03` §3, D23). They are plain Kotlin classes: they use no `Context` and no `Handler`, so the same classes run in the app and in JVM unit tests (`fake-data.md` §5.3). They get everything they need through their constructor:

```kotlin
enum class Scenario {
    NORMAL, MISSING_FIELDS, SHORT_LIST, EMPTY, INVALID_JSON, HTML_DESCRIPTION,
    SHORT_CHART, NO_CONNECTION, TIMEOUT, RATE_LIMITED, SERVER_ERROR, REJECTED,
    FAIL_THEN_OK;

    companion object {
        fun fromName(name: String): Scenario   // "rate_limited" -> RATE_LIMITED
    }
}

fun interface FakeFileReader {
    fun read(path: String): String             // path inside assets, e.g. "fake/markets_usd.json"
}

fun interface MainThreadPoster {
    fun post(delayMs: Long, action: Runnable)
}

class MarketListFakeDataSource(
    reader: FakeFileReader,
    scenario: Scenario,
    delayMs: Long,
    executor: java.util.concurrent.Executor,
    mainThread: MainThreadPoster,
) : MarketListDataSource
```

`CoinsByIdsFakeDataSource`, `CoinDetailFakeDataSource` and `ChartFakeDataSource` have the same five constructor parameters.

| Parameter | In the app (`CryptoApp`) | In a JVM unit test |
|---|---|---|
| `reader` | `AssetFakeFileReader`: reads from `assets` through the `AssetManager` | reads `src/main/assets` from the disk |
| `delayMs` | `BuildConfig.FAKE_DELAY_MS` | 0 |
| `executor` | the shared background `ExecutorService` | any `Executor`, for example one that runs the task at once |
| `mainThread` | `HandlerMainThreadPoster`: posts to the main looper with `Handler.postDelayed` | runs the action at once and ignores the delay |

Rules:
- **Threading:** the file is read and parsed (with the mappers of section 4) on `executor`; `onSuccess` and `onError` are delivered with `mainThread.post(delayMs, …)`, so they arrive on the main thread after the delay (`03` §5).
- **Scenario value:** `Scenario.fromName` turns the text of `FAKE_SCENARIO` into the constant of the same name in upper case (`Locale.US`). A text that matches no constant throws `IllegalArgumentException("Unknown FAKE_SCENARIO: …")`, so the app stops at startup instead of quietly running `normal`.
- **What a scenario changes:** only what `fake-data.md` §5.2 lists; every other request is answered as in `NORMAL`. The five error scenarios (`NO_CONNECTION`, `TIMEOUT`, `RATE_LIMITED`, `SERVER_ERROR`, `REJECTED`) make all four data sources answer with the matching `AppError` (`fake-data.md` §4).
- **`FAIL_THEN_OK`:** each data source counts its own requests. Its first request fails with `AppError.ServerError` and every later request succeeds as in `NORMAL`. The count is per data source, not per coin and not for the whole app, so every screen can be tried on its own.
- **Files:** `fake/` for captured files and `fake/edge/` for edge-case files (`fake-data.md` §2, §3). A missing file is a programming error, not an `AppError`: the reader throws and the test or the app fails at once.
- **Test helpers:** the disk reader and the immediate poster of the table are written by Vân Khánh in the test source set; their names are not part of this contract. The working directory of the Gradle test task is the project directory (`app/`) by default (Gradle documentation, read 22 Oct 2026); that the Android Gradle Plugin keeps it is not verified. If it does not, one line in the build file fixes it and nothing in this section changes.
