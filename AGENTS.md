# AGENTS.md – Crypto Dashboard (Android)

Instructions for AI coding assistants used in this repository. The person you are helping is one
of six students (USTH, Mobile Application Development). Some are new to Android. Your job is to
help them finish their own area, correctly and in a way they can understand and explain.

## 1. Project in brief
A market-monitoring and learning app for beginners (not a trading app). Market tab (top 100, search,
sort, pull-to-refresh), Watchlist tab (one list, max 50), Coin Detail (price chart 1H/24H/7D/1M/1Y,
statistics, description, disclaimer), USD/VND. Data from the CoinGecko Demo API.

## 2. Before you write any code
1. Find out which area the member owns (branch name `feature/...` or ask) and which task.
2. Read the docs in `docs/` that apply, do not answer from memory:
   - `01-requirements.md` (requirement IDs, priorities Must/Should/Could)
   - `02-ui-spec.md` (what each screen contains; style names in section 2.1)
   - `03-architecture.md` (layers, threading, packages, contracts, decision log)
   - `06-task-plan.md` (owners, contracts C1–C8, rules, Definition of Done)
   - `fake-data.md` (fake data files)
   - `04-api-contract.md` and `05-interfaces.md`, once they exist. If they are missing, say so; do not guess API fields or interface signatures.
3. Name the requirement IDs (FR-...) your change implements.
The docs are the source of truth. If the code and the docs disagree, stop and tell the member.

## 3. How to work with the member
- **Do not invent** requirements, API responses, JSON fields, file contents, class names or method
  signatures. If the docs do not say it, or you have not seen the file, ask, or ask the member to
  show you the file.
- **Ask before finalizing anything unclear**, including small choices (names, which approach).
- Work in small steps, one at a time. After each step, say which files you created or changed and
  how they connect.
- Say what is verified (in the docs or in code you have read) and what is an assumption.
- Explain in simple words. The member must be able to explain the feature (06, section 6). Prefer the
  simplest solution the team can understand and test. Do not generate large blocks the member cannot follow.
- Be honest: point out risks, scope creep and unrealistic plans, with a recommendation and reasons.
- Reply in English. Code, identifiers, comments and commit messages are in English.
- When something fails, find the cause. Do not hide it (no silent catch-all, no suppressed lint
  errors, no disabled StrictMode).

## 4. Fixed technical choices
- Kotlin + XML Views (Activities, Fragments, RecyclerView, Material components).
  **No Jetpack Compose** (not accepted by the instructor).
- minSdk 24, compileSdk 37, portrait only, dark theme only.
- ViewModel + LiveData per screen. Manual dependency injection through `CryptoApp`, which exposes the
  repositories and the other shared objects as properties. Each feature writes the small ViewModel
  factory of its own screens (`viewModelFactory { initializer { ... } }`); there is no shared factory.
  No Hilt.
- Volley for HTTP and `org.json` for JSON. No Retrofit, OkHttp, Gson or Moshi.
- Coin icons: Volley `NetworkImageView` with the `CoinImageLoader` from `CryptoApp` (contract C8). No Glide, Picasso or Coil.
- Threading: one `ExecutorService` + a `Handler` on the main looper. Repository callbacks always
  arrive on the main thread. No `AsyncTask`, no coroutines.
- Favorites (one JSON array string) and the chosen currency in `SharedPreferences`. No SQLite or Room.
- Chart: MPAndroidChart v3.1.0 behind the `PriceChart` interface. Sparkline: a custom `View` drawn on a `Canvas`.
- Do not add a library or change SDK versions or build settings without the member confirming
  with the team lead (Vân Khánh).

## 5. Architecture rules (details: 03)
- Layers: UI → ViewModel → Domain ← Data. Dependencies point down only.
- `features/*` depend on `domain` and `core`, never on `data`, and never on each other except
  through the contracts in section 8.
- Activities, Fragments and adapters contain no networking, JSON parsing or storage code.
- ViewModels hold no View and no Context. They call repository **interfaces** only.
- Filtering, sorting, slicing and mapping are plain Kotlin (no Android types) so JVM unit tests can run them.
- Every screen exposes `LiveData<UiState<T>>` with Loading, Success, Empty and Error.
- Ignore stale responses (request counter) when the user changes timeframe or currency.
- After a change of currency or timeframe, drop the old data and show Loading; if the new request fails, emit `Error` without old data. Keep old data in `Error` only when a refresh of the same request fails (`01` FR-ERR-03, `03` section 6).
- Fake and real data sources implement the same interfaces; `CryptoApp` chooses once at startup.
  No other class checks the fake/real flag.

## 6. UI rules (details: 03 section 11, 02)
- **No hard-coded colors, sizes or strings**, in layouts or in Kotlin. Use the shared style names
  (themes, colors, dimens, text styles; role names in `02` section 2.1) and string resources
  (`values/` and `values-vi/`; see section 8 for which strings file is yours).
  If a style name you need does not exist, do not hard-code a value: ask the member to ask Hải.
- Show prices, compact numbers, percentages and dates only through the shared formatters.
- Sizes in `dp`, text in `sp`. Use `ConstraintLayout`. No fixed width on containers, no fixed height on
  text views. Variable text sets `maxLines` + `ellipsize`. Touch targets at least 48×48 dp.
  Scrollable screens use `RecyclerView` or `NestedScrollView`.
- Loading, empty and error (with Retry) states use `StateView`.
- Check every screen at 360×640 dp and 411×891 dp with long names, 7-digit prices and large VND values.

## 7. Data, secrets and storage
- Develop on **fake data** (`fake-data.md`). Use the real API only at integration.
- Never write the API key into code, docs, logs, commits or chat. Use `<KEY>` as the placeholder.
  The key lives only in the developer's `local.properties` (not committed) and reaches the app through `BuildConfig`.
- The repository is public: no secrets, no personal data, no `local.properties`.
- **No market data stored on the device**: no disk cache, no saved responses. The Volley queue uses `NoCache`;
  data is kept in memory only. The only stored data are favorites and the currency.
- "Powered by CoinGecko" stays visible (footer bar).
- Never disable certificate checks or trust all certificates.
- Do not call the real API in loops, tests or CI (quota).

## 8. Ownership and contracts
| Area | Owner | Folders and files |
|---|---|---|
| Data layer, CryptoApp, coin icon loader, build files | Vân Khánh | `data/`, `domain/`, `core/image`, `app/CryptoApp`, `res/xml/`, `res/raw/`, Gradle files |
| UI foundation, MainActivity, formatters, StateView, currency, shared resources | Hải | `core/ui`, `core/format`, `core/currency`, `app/MainActivity`, and the shared values in `res/` (theme, colors, dimens, text styles, shared drawables, the common `strings.xml`) |
| Market list | An Khánh | `features/marketlist`, and the layouts, drawables and strings file of its screens |
| Coin detail | Kiên | `features/detail`, and the layouts, drawables and strings file of its screen |
| Price chart | Duy | `features/chart`, and the layouts, drawables and strings file of its screen |
| Watchlist, favorites | Bảo Linh | `features/watchlist`, `core/favorite`, and the layouts, drawables and strings file of its screen |

- **Layouts and drawables** of a screen belong to the owner of that screen (the shared coin row layout is An Khánh's, C4).
- **ViewModel factory:** the factory of a screen's ViewModel is in the feature's own package and belongs to the owner of that feature. It takes the repositories from `CryptoApp`; nobody edits a file of another member to register a ViewModel.
- **Strings:** put the strings of your screen in a strings file of your own, in `values/` and in `values-vi/`, and start
  each string name with the name of your feature so that names never clash. Words used by several screens go in the common
  `strings.xml`, which is Hải's: ask the member to ask Hải. Hải reviews the Vietnamese text.
- **`AndroidManifest.xml`:** declare your own Activity there. Every other change to it (permissions, the network
  security configuration, the application element) goes through Vân Khánh.

Contracts C1–C8 are defined in `06` section 3 and `03` section 12. Do not edit files in another
member's area. Tell the member what you need so they can ask the owner. A change to a contract needs
the owner and the team lead.

### Work that depends on another member
Many tasks use something another member is still building (a repository interface, a formatter, a style name,
`FavoriteStore`, the coin row, `PriceChartFragment`, `CurrencySettings`, the Detail intent, `CoinImageLoader`, ...).
1. Check whether it already exists on `main` (or in the branch the member points to). If it does, read it and use it as it is.
2. If it does not exist yet, **do not invent its name, signature or behavior, and do not write your own copy of it.**
   Tell the member which part is missing and who owns it (table above), and tell them to ask that owner
   for the planned signature or for a date.
3. A small temporary stub in the member's own package is allowed only if the member agrees. Mark it clearly
   as temporary so it is replaced when the real one arrives, and say that it is a stub.

## 9. Scope
Build only what `01-requirements.md` says, with the priority it gives. If the member asks for something
that is not in `01`, say so and ask whether it should be added to the requirements first.
Out-of-scope items are listed in `02` section 7.

## 10. Git
- One branch per area (`feature/market-list`, ...). Never commit directly to `main`.
- Pull request with the requirement IDs, reviewed by someone who is not the author. Merge at least every 2 days.
  `main` must always build and run.
- Commit style: `feat(market-list): add sort toggle (FR-ML-04)`.
- Do not commit or push unless the member asks.

## 11. Tests and Definition of Done
- The member writes JVM unit tests for the logic in their own area.
- A task is done when: it meets its requirement IDs; loading/empty/error states exist; unit tests pass;
  it works on fake and real data; there are no hard-coded values, no key in code and no lint errors; the two
  screen-size checks are done; it is reviewed and merged; the owner can explain it.

## 12. Documents
Do not edit files in `docs/`. If a document seems wrong or incomplete, tell the member to report it to
the team lead (Vân Khánh), who decides on changes to the documents. The one exception is a missing style
name (a color, text style, spacing, corner size or component size role): the member asks Hải (section 6).
