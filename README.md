# Crypto Dashboard

An Android app for watching the cryptocurrency market and learning what the numbers mean. It is made for beginners and is **not a trading platform**: no accounts, no orders, no wallets.

University group project, Mobile Application Development, USTH (six members).

## What the app does

- **Market tab:** top 100 coins by market cap, search, sort chips, pull-to-refresh, and a small price sparkline per coin.
- **Watchlist tab:** your starred coins (one list, up to 50), kept on the device.
- **Coin Detail:** price chart (1H, 24H, 7D, 1M, 1Y) with a touch marker, statistics, description and a short "not financial advice" disclaimer.
- **Currencies:** USD and VND.
- **Beginner help:** small info dialogs that explain terms such as market cap and volume.
- Dark theme only, portrait only, English and Vietnamese texts, Android 7.0 (API 24) and newer.

Market data is never stored on the device. Only the favorites and the chosen currency are saved (SharedPreferences).

## Tech stack

| Area | Choice |
|---|---|
| Language and UI | Kotlin, XML Views (Fragments, RecyclerView, Material components); no Jetpack Compose |
| State | ViewModel + LiveData |
| Networking | Volley 1.2.1, one shared `RequestQueue` |
| Background work | `ExecutorService` + `Handler`; nothing blocks the main thread |
| Charts | MPAndroidChart 3.1.0 (from JitPack) |
| Data source | [CoinGecko](https://www.coingecko.com) Demo API |
| SDK | minSdk 24, targetSdk 36 |

## Architecture

Four layers; a layer only talks to the one below it.

```
UI (Fragments, Activities)  →  ViewModel  →  Domain (models, repository interfaces)  →  Data
```

| Package (`vn.edu.usth.cryptodashboard`) | What lives there |
|---|---|
| `app` | `CryptoApp` (creates the shared objects once), `MainActivity` (two tabs) |
| `features/marketlist`, `detail`, `watchlist`, `chart` | One folder per screen: Fragment/Activity, ViewModel, adapter |
| `core/*` | Shared UI and helpers: formatters, `UiState` and state views, currency settings, favorites, coin icons |
| `domain/*` | Plain Kotlin models and repository interfaces |
| `data/repository` | Repositories with in-memory data and refresh rules |
| `data/source`, `data/remote`, `data/fake` | Data source interfaces; the real CoinGecko sources (`RemoteLoader`, Volley); the fake sources that read JSON files |
| `data/mapper` | JSON → domain models |
| `data/thread` | Running work on the executor and posting results to the main thread |

The way data reaches the screen is the same in both modes: Fragment → ViewModel → Repository → data source → (Volley request or JSON file) → mapper → LiveData. `CryptoApp` chooses the fake or the real data sources once at startup.

## Build and run

1. Install Android Studio and open this folder as a project.
2. Run the `app` configuration on an emulator (API 24 or newer) or a device.

That is all: the app starts in **fake data mode**, which needs no key and no internet. From a terminal on Windows:

```
.\gradlew.bat :app:installDebug
```

(On macOS or Linux use `./gradlew`.)

### Settings (`local.properties`)

Create or edit `local.properties` in the project root. The file is never committed. Every key is optional.

```
USE_FAKE_DATA=true
FAKE_SCENARIO=normal
FAKE_DELAY_MS=800
# only needed when USE_FAKE_DATA=false
CG_API_KEY=
```

| Key | Default | Meaning |
|---|---|---|
| `USE_FAKE_DATA` | `true` | `true` reads the sample JSON files, `false` calls the real CoinGecko API |
| `FAKE_SCENARIO` | `normal` | Behaviour of the fake data (see below) |
| `FAKE_DELAY_MS` | `800` | Artificial delay of fake answers, in milliseconds |
| `CG_API_KEY` | empty | CoinGecko Demo API key, used only in real mode |

After changing a value, run the app again (the values are copied into `BuildConfig` at build time). Put a comment on its own line: text after a value becomes part of the value.

At startup the app writes one log line, `Data source: FAKE` or `Data source: REAL`. Filter Logcat on `Data source` to see which mode a build is in.

### Real data

1. Get a free Demo key from CoinGecko.
2. Set `USE_FAKE_DATA=false` and `CG_API_KEY=<your key>` in `local.properties`.
3. Run the app and check that Logcat says `Data source: REAL`.

The key is compiled into the APK, so **do not share a real-mode APK outside the team**, and never commit the key.

### Fake scenarios

`FAKE_SCENARIO` lets you see every state of the app without a network: `normal`, `empty`, `short_list`, `missing_fields`, `invalid_json`, `html_description`, `short_chart`, `no_connection`, `timeout`, `rate_limited`, `server_error`, `rejected` and `fail_then_ok` (the first request fails, the next one works). An unknown name stops the app at startup with a message. Details are in [`docs/fake-data.md`](docs/fake-data.md).

## Tests and CI

Unit tests (JVM, no emulator) and Android lint:

```
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug
```

Instrumented UI tests need a running emulator or device (`:app:connectedDebugAndroidTest`); a test that needs a special fake scenario is skipped unless `FAKE_SCENARIO` has that value. The manual checklist and the list of tests per requirement are in [`docs/07-test-plan.md`](docs/07-test-plan.md).

GitHub Actions runs the unit tests and lint on every pull request and every push to `main` (no API key, no emulator).

## Documentation

All design documents are in [`docs/`](docs/):

| File | Content |
|---|---|
| `01-requirements.md` | Goals, scope, functional and non-functional requirements |
| `02-ui-spec.md` | Screens, components and style roles |
| `03-architecture.md` | Layers, packages, threading, data flow, decision log |
| `04-api-contract.md` | CoinGecko requests and the fields the app reads |
| `05-interfaces.md` | Kotlin signatures of the contracts between the parts |
| `06-task-plan.md` | Owners, contracts, day plan |
| `07-test-plan.md` | Test cases by requirement |
| `fake-data.md` | Sample data files, scenarios, how to switch modes |

[`AGENTS.md`](AGENTS.md) holds the rules for the AI assistants used by the team.

## Team

| Member | Area |
|---|---|
| Vân Khánh (team lead) | Data layer, project setup, repositories, remote and fake data sources, CI, test checklist |
| Hải | UI foundation: theme, top bar, formatters, shared states, error mapping |
| An Khánh | Market list |
| Kiên | Coin Detail |
| Duy | Price chart |
| Bảo Linh | Watchlist and favorites |

## Data and attribution

**Powered by CoinGecko.** Market data comes from the CoinGecko API, and the app shows this line on every screen.

Sample data in `app/src/main/assets/fake/` are snapshots of CoinGecko API responses, used only for development and tests. Data provided by CoinGecko.

The app is for learning and market watching only. Nothing in it is financial advice.
