# 04 – API Contract

**Project:** Crypto Dashboard (Android, native Kotlin) – USTH Mobile Application Development <br>
**Scope of this file:** the CoinGecko requests the app makes and the response fields the app reads. Only fields the app uses are listed; for each endpoint one line says how many other fields exist and are ignored. What the app must do is in `01-requirements.md`, how errors are classified is in `03-architecture.md` §8.3, the Kotlin interfaces are in `05-interfaces.md`.

Everything marked *captured* comes from real responses saved in `app/src/main/assets/fake/` (see `fake-data.md`). Everything marked *documented* comes from CoinGecko pages. Everything marked *not verified* is an assumption.

---

## 1. Common rules

| Item | Value |
|---|---|
| Base URL | `https://api.coingecko.com/api/v3` (a Demo key uses this host) |
| Method | `GET`, JSON response, `Content-Type: application/json; charset=utf-8` |
| Authentication | Header `x-cg-demo-api-key: <KEY>`; the key is read from `local.properties` through `BuildConfig` and never written into code, docs or the repository |
| Image host | `coin-images.coingecko.com` (coin icons) |
| Currencies | `usd` and `vnd`, lowercase, passed as `vs_currency`; values come from the provider and the app does no conversion |
| Rate limit | Demo plan: 100 calls per minute, every request counts, including 4xx and 5xx (*documented*) |
| Errors | Classified by HTTP status first, exception class second, body never read: see `03` §8.3 |

### 1.1 Reading the JSON (rules found in the captured data)

- **A number field can be an integer in one response and a decimal in another.** Example: `current_price` is an integer for 1 of 100 coins in USD and for 66 of 100 coins in VND. Read every price-like number as a `Double`.
- **`market_cap` can be larger than `Int`.** The largest value in `markets_vnd.json` is about 4.2 × 10^16. Read it as `Long` or `Double`.
- **A missing value is either an absent key or a JSON `null`.** Treat both the same (check with `isNull` before reading). A value of `0` is a real value, not missing (`01` FR-CD-06).
- **Timestamps in charts have 13 digits (milliseconds since 1970, UTC).**
- **The sparkline length varies:** 168 points for 99 of 100 coins, 133 for one coin. Never assume a fixed length.
- Unknown extra fields must be ignored: the provider can add fields.

---

## 2. Requests used and captured

All files were captured on 18 Oct 2026 with `curl.exe` from PowerShell (times are those of the capturing PC, whose clock was about 11 s behind the server). Request paths are relative to the base URL.

| Used by | Request | Captured file(s) | Time |
|---|---|---|---|
| Market list | `/coins/markets?vs_currency={cur}&order=market_cap_desc&per_page=100&page=1&sparkline=true` | `markets_usd.json`, `markets_vnd.json` | 01:27 and 01:31 |
| Watchlist | `/coins/markets?vs_currency={cur}&ids={id1,id2,…}&sparkline=true` | `markets_by_ids_usd.json`, `markets_by_ids_vnd.json` (ids: `bitcoin,ethereum,akedo`) | 01:32 |
| Coin Detail | `/coins/{id}?localization=false&tickers=false&market_data=true&sparkline=false` | `detail_bitcoin.json` (01:36), `detail_ethereum.json` (01:36), `detail_solana.json` (01:39) | 01:36–01:39 |
| Price chart | `/coins/{id}/market_chart?vs_currency={cur}&days={1\|7\|30\|365}` | `chart_bitcoin_{1d,7d,30d,365d}_{usd,vnd}.json` | USD 01:44, VND 02:08 |

Notes:
- The commands were copied from the chat that ran the capture and pasted into the terminal unchanged (confirmed by the team lead). The shell history was not available, so they were not checked against it.
- `order=market_cap_desc` and `page=1` are the provider's defaults: on 20 Oct 2026 a request without them returned the same 100 coins in the same order as the request with them. The team lead does not remember whether they were typed in the original commands, which makes no difference to the result. The app sends them explicitly.
- `akedo` (market cap rank 101) is in the by-ids files on purpose: a coin outside the top 100 can be on a Watchlist.
- Some captured files were reformatted in VS Code before being committed (whitespace only): `markets_usd`, `markets_vnd`, `detail_ethereum`, and the VND charts for 7d, 30d and 365d.

---

## 3. `/coins/markets` (Market list and Watchlist)

Response: a JSON array, one object per coin. Each object has **27 fields; the app reads 9, and ignores the other 18.**

| Field | JSON type in captured data | Missing? | Used for |
|---|---|---|---|
| `id` | string | no | key of a coin (favorites, Detail request) |
| `symbol` | string | no | row (FR-ML-01), search (FR-ML-03) |
| `name` | string | no | row, search |
| `image` | string (URL) | no | icon; the URL contains `/large/`, the same image as `image.large` on Detail |
| `current_price` | number, int or decimal | not in the sample | row price (sorting) |
| `market_cap` | number, int, can exceed `Int` | not in the sample | row, sorting |
| `market_cap_rank` | integer | not in the sample | rank in the row; does not change when the user sorts (FR-ML-01) |
| `price_change_percentage_24h` | decimal | not in the sample | 24h % in the row, sorting |
| `sparkline_in_7d.price` | array of numbers (168 or 133) | not in the sample | 7-day trend line (FR-ML-02) |

"Not in the sample" means: no missing value among the 100 coins of each captured file. It does not mean the provider guarantees a value. Missing values are covered by the edge-case files in `fake/edge/` (`fake-data.md` §3) and shown as "—" (FR-ML-09).

Observed behaviour:
- `per_page=100` returns exactly 100 coins (both files). The raw response is 395,553 bytes (measured on 19 Oct).
- `ids` with an id the provider does not know (`ids=bitcoin,khong-ton-tai-xyz`) returns **200 and an array without that coin**, not an error.
- **The provider does not keep the order of `ids`.** A request with `ids=solana,bitcoin,ethereum` (20 Oct 2026, USD) returned `bitcoin, ethereum, solana`, which is market-cap order. One test. The Watchlist therefore orders the rows itself, by the order in which the user added the coins (`01` FR-WL-03).
- A coin outside the top 100 can be requested with `ids` and its sparkline is returned.
- Response headers (captured for `markets_usd`): `Cache-Control: max-age=30, public, must-revalidate, s-maxage=60`; `cf-cache-status: HIT`; headers `total` and `per-page` present.

---

## 4. `/coins/{id}` (Coin Detail)

Response: a JSON object with **32 or 33 top-level fields and 47 fields inside `market_data`**. The app reads the fields below and ignores the rest. Fields keyed by currency are objects with 63 currency codes as keys; the app reads only the key of the selected currency (`usd` or `vnd`).

| Field | JSON type in captured data | Missing? | Used for |
|---|---|---|---|
| `id` | string | no | identity of the coin |
| `symbol` | string | no | header (FR-CD-01) |
| `name` | string | no | header |
| `image.large` | string (URL) | no | icon in the header |
| `description.en` | string, 1,634 to 2,738 characters in the three samples, contains `\n` line breaks | can be empty | description (FR-CD-07) |
| `market_data.current_price[cur]` | number, int or decimal | not in the sample | header price |
| `market_data.price_change_percentage_24h` | decimal, one value (not keyed by currency) | not in the sample | header 24h % |
| `market_data.market_cap[cur]` | number, int | not in the sample | statistics (FR-CD-06) |
| `market_data.total_volume[cur]` | number, int | not in the sample | statistics |
| `market_data.circulating_supply` | decimal | not in the sample | statistics |
| `market_data.total_supply` | decimal | not in the sample | statistics; row only when a value is returned |
| `market_data.max_supply` | decimal or `null` (`null` for ethereum and solana) | **yes** | statistics; row only when a value is returned |
| `market_data.ath[cur]` | number, int or decimal | not in the sample | statistics (all-time high) |
| `market_data.atl[cur]` | number, int or decimal | not in the sample | statistics (all-time low) |

Optional, only if FR-CD-09 (Could) is built:

| Field | JSON type | Used for |
|---|---|---|
| `market_data.ath_date[cur]` | string, ISO 8601 UTC (`2025-10-06T10:57:42.000Z`) | date of the all-time high |
| `market_data.atl_date[cur]` | string, ISO 8601 UTC | date of the all-time low |
| `market_data.ath_change_percentage[cur]` | decimal | distance from the all-time high in % |

Observed behaviour:
- **The 24h % on Detail is the single value `market_data.price_change_percentage_24h`**, the same kind of value the list reads (`price_change_percentage_24h` of `/coins/markets`). The two screens can still show different numbers for the same coin, because the list and Detail are fetched separately, at different moments, and each keeps its own data for up to 60 s (`01` FR-DATA-01). In the three captured files it equals `price_change_percentage_24h_in_currency` for `usd` and for `vnd` (bitcoin 0.15581, ethereum 0.03334, solana 0.0183). Other coins and other times were not checked.
- `ath_date` and `atl_date` in `/coins/{id}` are objects keyed by currency. In `/coins/markets` they are plain strings (and the app does not read them there).
- The `atl_date` of bitcoin differs between `usd` and `vnd` in the sample (the all-time low is a different day in each currency), so always read the key of the selected currency.
- The descriptions of all six real coins tested contain `\n` line breaks and **no HTML tags** (three of them are captured files). The app still removes markup (FR-CD-07); `fake/edge/detail_ethereum_edited.json` has an edited link for that test.
- `max_supply` is `null` for coins with no maximum; a `max_supply_infinite` field also exists (`true` for ethereum and solana) and is ignored: the app shows the row only when `max_supply` has a value.
- Ignored on purpose: 26 or 27 other top-level fields (for example `links` and `community_data`) and 38 other fields in `market_data` (35 if FR-CD-09 is built).

---

## 5. `/coins/{id}/market_chart` (price chart)

Response: an object with three arrays: `prices`, `market_caps`, `total_volumes`. **The app reads only `prices` and ignores the other two.** Each element of `prices` is a pair `[timestamp_ms, price]`: the timestamp is an integer with 13 digits, the price a decimal.

| Timeframe in the app | Request | Points in the captured file | Spacing (captured) |
|---|---|---|---|
| 1H | `days=1`, then keep the points within 60 minutes before the last point (`01` FR-CD-03) | 289 points in the file; 13 points in the last 60 minutes | about 5 minutes (smallest gap seen: 3.3 minutes) |
| 24H | `days=1` | 289 | about 5 minutes |
| 7D | `days=7` | 169 | about 1 hour |
| 1M | `days=30` | 721 | about 1 hour |
| 1Y | `days=365` | 366 | about 1 day |

Observed behaviour:
- `days=365` works on the Demo plan (HTTP 200 in the capture).
- `vs_currency=vnd` works for all four values of `days` (VND files are captured for 1d, 7d, 30d, 365d).
- The provider chooses the resolution from `days`; the app cannot ask for a finer one. With fewer than 2 points the chart shows "Not enough data" (FR-CD-03).

---

## 6. Observations about caching and credits

These are observations from 18 and 19 Oct 2026, not documented rules. Do not build on them.

- Responses with `cf-cache-status: HIT` seem to cost 0 credits and `EXPIRED` seems to cost 1; 401 and 404 cost 0 and a client-side timeout still cost 1 in the S2 test. The documentation says every request counts toward the rate limit, so keep the documented limit as the rule.
- Icon requests (about 17.5 KB each) cost no credits in the S2 test, but every measured request was a cache hit, so "icons are free" and "cache hits are free" cannot be told apart.
- Because the cost per call is unknown, the app limits itself: pull-to-refresh makes no request within 60 s of the last successful fetch (FR-ML-06, FR-WL-10), and no request is made for 60 s after an HTTP 429 (FR-ERR-04).

---

## 7. Errors

The classification table is in `03` §8.3 and is the only place where it is defined. Facts about the provider's errors:

- **Documented** (not observed): 400 invalid request; 401 missing or invalid key; 403 blocked; 408 timeout; 429 rate limit exceeded; 500 and 503 server errors; 1020 blocked by the CDN firewall. Sources: https://docs.coingecko.com/docs/common-errors-rate-limit and https://support.coingecko.com/hc/en-us/articles/4538787440153-Why-am-I-getting-errors-with-a-status-code-429 (read 18 Oct 2026). The pages give no example error body, no `Retry-After` header and no wait time.
- **Observed on the API 24 emulator (19 Oct):** a wrong key gave 401 and a 404 was also seen (the request was not recorded).
- **Observed on 20 Oct 2026:** a request with the wrong key `sai-key-2` on a URL never used before returned **401** with the body `{"status":{"timestamp":...,"error_code":10002,"error_message":"API Key Missing. ..."}}`. The message says "missing" although a wrong key was sent.
- **Observed on 18 Oct 2026:** the same kind of request with the wrong key `sai-key` returned 200 with data. The likely cause is a cached response: a repeat of the same request on 20 Oct 2026 with the wrong key returned 200 with `cf-cache-status: HIT`, while a new URL with a wrong key returned 401. This fits a cache serving the response without checking the key; it is an observation, not a documented rule.
- **Two error body shapes exist:** `{"error":"coin not found"}` and `{"status":{"error_code":10002,...}}` (the second one was seen with the 401 above). The app never reads an error body.
- **Not observed, only predicted:** 429 and 5xx; they are covered by the `ErrorMapper` unit test.

---

## 8. Open points

1. Whether the 24h % on Detail always equals the per-currency value for other coins (section 4).
2. 429 and 5xx behaviour, and whether icons cost credits when they are not cached (section 6, `03` §17).
