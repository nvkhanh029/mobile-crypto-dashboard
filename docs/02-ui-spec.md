# 02 – UI Specification

**Project:** Crypto Dashboard (Android, native Kotlin, XML Views) <br>
**Related:** `01-requirements.md` (requirement IDs), `03-architecture.md` (how it is built, layout rules in §11), `06-task-plan.md` (owners), `fake-data.md` (test data). <br>
**Owners of this file:** Hải (UI foundation) keeps it in line with the styles; each feature owner follows the part for their screen.

---

## 0. How to use this document

This document says **what each screen contains and how it behaves**. It does not define colours, sizes or font sizes (those are named resources owned by Hải, see §2.1) and it does not define code structure (see `03-architecture.md`).

Rules for everyone, including AI coding assistants:

1. **Build only what is listed here.** An element, screen, menu item or option that is not in this document is out of scope. §7 lists the main things that are deliberately not built.
2. **Every element traces to a requirement** in `01`. If something you need is missing, ask the owner of this file; do not invent it.
3. **Text shown to the user comes from string resources.** The English wording in this document is the intended meaning; the exact strings live in `strings.xml` (`values/` for English, `values-vi/` for Vietnamese).
4. **No hard-coded colours, sizes or strings**, and follow the layout rules in `03-architecture.md` §11 (dp/sp, flexible widths, ellipsis, 48 dp touch targets, check at 360×640 dp and 411×891 dp).
5. **Layout reference.** The look is that of a clean, dark, list-first market app: a compact row per item with a trend line and a coloured change label, a chart with timeframe chips, and a plain statistics list. The components are Android Material components. No assets, icons or wording are copied from any other app.

Conventions: screens are `SCR-nn`, shared components are `CMP-nn`. "Tap" means a single tap. "Row" means a coin row (CMP-01).

---

## 1. Screen map

```
                      ┌────────────────────────────────────────────┐
                      │ MainActivity (SCR-01)                      │
                      │  top bar · content · footer bar             │
                      │   ┌───────────────┐   ┌─────────────────┐  │
                      │   │ Market tab    │   │ Watchlist tab   │  │
                      │   │ SCR-02        │   │ SCR-03          │  │
                      │   └──────┬────────┘   └───────┬─────────┘  │
                      └──────────┼────────────────────┼────────────┘
                     tap a row   │                    │  tap a row
                                 ▼                    ▼
                      ┌────────────────────────────────────────────┐
                      │ CoinDetailActivity (SCR-04)                │
                      │  hosts PriceChartFragment (SCR-05)         │
                      └────────────────────────────────────────────┘
                              Back → the tab it came from
```

- The app opens on **Market**.
- Detail is a separate screen opened with the coin ID (contract C6). Back returns to the same tab with search text, sort and scroll position unchanged (FR-NAV-03).
- There is no other screen: no settings, no about, no sign-in.

---

## 2. Global rules

### 2.1 Theme (roles only)
One dark theme (FR-UI-01). Hải defines these **roles** once in resources; layouts refer to roles, never to values.

| Kind | Roles |
|---|---|
| Colour | `background`, `surface` (cards, chips, bars), `textPrimary`, `textSecondary`, `divider`, `accent` (selected chip, links, progress), `positive`, `negative`, `neutral`, `onPositive`, `onNegative`, `onNeutral` (text on a coloured label), `onAccent` (text on `accent`: the selected timeframe chip, CMP-10, and the selected segment of the view switch, CMP-13) |
| Text style | `TitleBar`, `RowTitle`, `RowSubtitle`, `Price`, `PriceLarge`, `ChangeLabel`, `Body`, `Caption` (footer text, size ≥ 10 sp per FR-INFO-01), `StatLabel`, `StatValue` |
| Spacing | `space_xs`, `space_s`, `space_m`, `space_l` (a small named scale) and one `touch_min` = 48 dp |
| Corner size | `corner_xs`, `corner_s`, `corner_m`, `corner_l`, `corner_pill` |
| Component size | `divider_thickness`, `sparkline_width` and `sparkline_height` (CMP-03), `coin_icon_size`, `chart_height` (SCR-05) |

All text and icons meet contrast ≥ 4.5:1 against their background (NFR-08).

### 2.2 Direction colours
- `positive` = value went up, `negative` = went down, `neutral` = unchanged or unknown.
- **Colour is never the only signal:** every change value carries a sign (`+` or `-`) and the text itself (FR-ML-02, NFR-08).

### 2.3 Text and numbers
- All numbers use the fixed formats of `01` §5.2 (same on every screen and in both languages).
- A missing value is always `—`. The words "null", "NaN" and "0" (for missing) never appear (FR-ML-09, FR-CD-06).
- A coin symbol is shown in upper case.

### 2.4 The four states (FR-ERR-01)
Every screen that loads data has these states. Components for them are in CMP-09.

| State | Meaning | Shown as |
|---|---|---|
| Loading | Waiting for the first data (or for new data after a currency change) | Progress indicator in the content area |
| Success | Data is on screen | The content |
| Empty | The request worked but there is nothing to show | Message (and an action where stated) |
| Error | The request failed | If **no data** is on screen: message + **Retry** filling the content area. If data **is** on screen: the data stays and a **Snackbar** shows the message with **Retry** (FR-ERR-03). |

Error messages by category are in `01` §4.7 (no connection, timeout, rate limited, server error, request rejected, invalid data).

### 2.5 Data source
The UI does not show which data source is active. The app writes it to the log at startup (NFR-12).

### 2.6 Orientation and size
Portrait only (NFR-11). Layouts work from 360 dp width upward (NFR-13).

---

## 3. Shared components

### CMP-01 Coin row  *(FR-ML-01, FR-ML-02, FR-ML-09, FR-WL-01, FR-WL-03, FR-WL-09)*
Used by Market and Watchlist. One layout, one adapter (contract C4).

```
┌───────────────────────────────────────────────────────────┐
│ 1  ◉  Bitcoin                  ╱╲_╱╲╱      $67,234.50  ☆  │
│       BTC · MC $1.30T                      [ +2.35% ]     │
└───────────────────────────────────────────────────────────┘
 rank icon name / symbol·cap     sparkline   price / change  star
```

| Element | Content and rules |
|---|---|
| Rank | Provider's rank, small, secondary text. Does not change when the list is sorted. Missing → `—`. |
| Icon | Coin image. Placeholder shown while loading or if loading fails. |
| Name | `RowTitle`, one line, **shortened with an ellipsis first** when space runs out. |
| Second line | `SYMBOL · MC <market cap>` in `RowSubtitle` (for example `BTC · MC $1.30T`, compact format of §5.2). Missing market cap → `BTC · MC —`. |
| Sparkline | CMP-03. |
| Price | `Price` style, current currency, format of §5.2. **Never truncated.** Missing → `—`. |
| Change label | CMP-02 under the price. **Never truncated.** |
| Star | CMP-04. Always shown on Watchlist rows, where it removes the coin (FR-WL-01). Shown on Market rows only if FR-ML-08 is built. It is never dropped to save space: if the row is crowded at 360 dp, the name shrinks first. |

Behaviour:
- Tapping the row (not the star) opens Detail with the coin ID (FR-NAV-02).
- Dividers separate rows. Minimum row height 48 dp.
- **Missing data:** each missing field shows `—`; a missing sparkline leaves an empty placeholder of the same size; the row never crashes or shows "null".
- **Variant "Data unavailable"** (Watchlist only, FR-WL-09): the row shows the coin ID (or the name if known) in the title position, the text "Data unavailable" in the second line, and a filled star that removes the coin. No sparkline, price or change label. Other rows are unaffected.

### CMP-02 Change label  *(FR-ML-01, FR-CD-01)*
- Rounded rectangle filled with `positive`, `negative` or `neutral`, text in the matching `on…` role, style `ChangeLabel`.
- Text: 24h change, signed, 2 decimals (`+2.35%`, `-0.40%`, `0.00%`).
- `0.00%` and missing (`—`) use `neutral`.
- Percent values do not change with the currency (FR-CUR-03).

### CMP-03 Sparkline  *(FR-ML-02)*
- Line only, no axes, no labels, no touch. Drawn on a Canvas (`03` §11).
- Colour: `positive` if the last value is higher than the first, `negative` if lower, `neutral` if equal or fewer than 2 points. (This is the 7-day trend; the change label is the 24h change, so the two can differ.)
- The same width on every row (one named dimension) so lines align down the list.

### CMP-04 Favorite star  *(FR-ML-08, FR-CD-08, FR-WL-01, FR-WL-08)*
- Filled star = in the watchlist; outline star = not.
- Touch target at least 48×48 dp. Content description "Add to watchlist" / "Remove from watchlist".
- Tapping toggles the coin in `FavoriteStore`; every screen showing that coin updates immediately.
- Adding when 50 coins are already saved does not add and shows the Snackbar "Watchlist is full (50)".
- On the Watchlist screen, tapping the filled star removes the coin and its row disappears (no undo in the base scope).

### CMP-05 Sort chips  *(FR-ML-04, FR-ML-05, FR-ML-09)*
One row of three chips under the search field: **Market cap**, **Price**, **24h %**.

```
[ Market cap ↓ ] [ Price ] [ 24h % ]
```

- Exactly one chip is active. **Default: Market cap, descending.**
- The active chip is highlighted and shows an arrow: ↓ descending, ↑ ascending (content description says "sorted descending/ascending").
- Tapping the **active** chip reverses its direction. Tapping **another** chip makes it active, descending.
- Sorting applies to the list after the search filter (FR-ML-05). Ties are ordered by rank ascending. Rows with a missing value in the sort field go last in both directions (FR-ML-09).
- Rank numbers on the rows are not changed by sorting (FR-ML-01).
- The sort is kept when the user switches tabs or returns from Detail; it is not saved across app restarts.
- The three chips fit on one row at 360 dp without scrolling.

### CMP-06 Search field  *(FR-ML-03)*
- Single-line field with a search icon at the start, hint "Search coins", and a clear (×) button at the end that appears only when there is text.
- Filters by name or symbol, case-insensitive, "contains", leading/trailing spaces ignored, applied 300 ms after the user stops typing.
- No match → the list area shows the empty state "No coins found". The field and chips stay visible. Clearing the text restores the full list.
- The keyboard's action key hides the keyboard. The text is kept when the user switches tabs or returns from Detail.

### CMP-07 Currency selector  *(FR-CUR-01…05)*
- A button at the end (right side) of the top bar showing the current code with a drop-down arrow: `USD ▾` or `VND ▾`. It is part of the main top bar, so it is on both the Market and the Watchlist tab, and (Should, FR-CUR-05) in the top bar of Detail.
- Tapping opens a small menu with two items, USD and VND, the current one checked.
- Choosing an item saves it and applies it to Market, Watchlist and Detail at once. The list area shows the loading state, then data in the new currency. Search text and sort are kept. Data of the other currency is never shown as the current one.
- On Detail the screen shows its loading state as at first open, then the price, statistics and chart in the new currency (the chart reloads in the chart area); the description and the favorite state do not change. If Detail has no selector (FR-CUR-05 not built), it follows the saved choice.
- Returning to a list after the currency was changed on Detail reloads that list in the new currency; search text and sort are kept, the scroll position is not guaranteed.
- Prices carry their currency symbol (`$`, `₫`), so the currency is always visible.

### CMP-08 Footer bar  *(FR-INFO-01, FR-DATA-03)*
A fixed, single-line bar at the bottom of the screen on Market, Watchlist and Detail. List and scroll content end above it; it never covers data.

```
Powered by CoinGecko                         🕒 14:32 13/10
```

- **Left:** "Powered by CoinGecko", style `Caption`, size ≥ 10 sp, never truncated. The whole left area (at least 48 dp high) opens https://www.coingecko.com in the browser. Nothing else on the screen suggests endorsement.
- **Right (Market and Watchlist only):** a clock icon (content description "Last updated") and the fetch time of the data on screen, format `HH:mm dd/MM` in the device time zone. Hidden until data has loaded. It keeps showing the time of the old data when a refresh fails.
- **Detail:** only the left part.

### CMP-09 State components  *(FR-ERR-01…04)*
**StateView** fills the content area of its screen (or only the chart area for SCR-05):

| Variant | Content |
|---|---|
| Loading | Centred progress indicator. |
| Empty | Message; an optional button (Watchlist: "Browse Market"). |
| Error | Message for the error category and a **Retry** button. |

**Inline error:** a Snackbar with the message and **Retry**, used when data is already on screen. A newer message replaces an older one.

**Rate limit:** after HTTP 429 no new network request is made for at least 60 s (FR-ERR-04). Tapping Retry inside that time shows the same "Too many requests, try again in a minute" message again and makes no request.

**Pull-to-refresh** (Market and Watchlist) shows the standard refresh indicator and is separate from StateView. If the last successful fetch was less than 60 s ago, the indicator stops without a request (FR-ML-06, FR-WL-10).

### CMP-10 Timeframe chips  *(FR-CD-03)*
- One row of five equal-width chips: **1H, 24H, 7D, 1M, 1Y**. Exactly one selected, default **24H**. The selected chip is filled with `accent`.
- Tapping a chip selects it and loads that timeframe in the chart area only. The chips stay usable while loading.

### CMP-11 Statistics list  *(FR-CD-06, FR-CD-10)*
One column of rows, label at the start (`StatLabel`) and value at the end (`StatValue`), a divider between rows. The value is never truncated; the label may be shortened.

| Row | Format |
|---|---|
| Market cap | compact, current currency |
| 24h trading volume | compact, current currency |
| Circulating supply | compact number, no currency symbol |
| Total supply | compact number; **row shown only when the provider returns a value** |
| Max supply | compact number; **row shown only when the provider returns a value** |
| All-time high | price format, current currency |
| All-time low | price format, current currency |

Missing values in the always-listed rows show `—`. A real value of 0 shows `0`. (Optional, Could: ATH/ATL date and distance from ATH, FR-CD-09.)

Explanations (FR-CD-10): every row has an ⓘ icon right after its label. Tapping the label area (at least 48 dp high, including the icon) opens the explanation dialog of that statistic (CMP-12). The label is shortened before the icon is; the icon and the value are never truncated. The icon's content description is "Explain <label>".

### CMP-12 Explanation dialog  *(FR-INFO-03, FR-CD-10)*
A Material dialog with a title, a short text and one button **Got it**. One component, two uses; Hải builds it and the screens only choose the title and the text.

| Use | Opened from | Title | Content |
|---|---|---|---|
| How to read this list | the ⓘ icon at the start of the main top bar (both tabs) | "How to read this list" | the five lines of the first table below |
| Statistic explanation | the ⓘ icon in a CMP-11 row | the statistic name | the one sentence of the second table below |

Behaviour:
- "Got it", a tap outside the dialog or the system Back closes it. Nothing else changes: the list, the scroll position, the search text and the sort are kept.
- It needs no network and no data, so it also opens in the error and empty states.
- All text comes from string resources, English and Vietnamese (FR-UI-02). The English wording below is the intended meaning.
- The wording only explains. It never says that a value is good or bad, or what to buy or sell.
- Only one dialog is shown at a time.

"How to read this list" (one line per item; the Star line is always included, because Watchlist rows and Detail always have a star):

| Item | Text |
|---|---|
| Rank | The number on the left is the coin's rank by market cap. 1 is the largest. |
| MC | MC is the market cap: the total value of all coins in circulation (price × circulating supply). |
| Change label | The coloured label is the price change over the last 24 hours. Green with + means up, red with - means down. |
| Trend line | The small line is the price trend over the last 7 days. It can point a different way than the 24-hour change. |
| Star | Tap the star to add or remove the coin in your Watchlist. |

Statistic explanations:

| Statistic | Text |
|---|---|
| Market cap | The total value of all coins in circulation: price × circulating supply. |
| 24h trading volume | The total value of this coin that was traded in the last 24 hours. |
| Circulating supply | The number of coins that are available and trading in the market now. |
| Total supply | The number of coins created so far, minus any coins permanently removed from circulation. |
| Max supply | The maximum number of coins that can ever exist. |
| All-time high | The highest price recorded for this coin. |
| All-time low | The lowest price recorded for this coin. |

### CMP-13 View switch  *(FR-NAV-01)*
A small pill-shaped toggle with two segments, **Market** and **Watchlist**, in the middle of the main top bar (SCR-01). It is built from Material components.

- Exactly one segment is selected, and it cannot be deselected. The selected segment is highlighted; the other is plain. The app starts with Market selected.
- Tapping the other segment shows that tab. Tapping the selected segment does nothing.
- Each tab keeps its state when the user switches (search text, sort, scroll position, FR-NAV-01).
- Each segment is at least 48 dp high. The two labels are one short word each, so that the top bar fits the ⓘ icon, the switch and the currency selector at 360 dp (NFR-13), in English and in Vietnamese.
- The tabs are switched only by tapping the segments; swiping between the tabs is not supported.

---

## 4. Screens

### SCR-01 Main screen (shell)  *(FR-NAV-01, FR-CUR-02, FR-UI-01, FR-INFO-03)*
Owner: Hải. Owns the top bar and hosts the two tab screens.

```
┌──────────────────────────────┐
│ ⓘ   [Market|Watchlist] USD ▾ │  top bar
├──────────────────────────────┤
│                              │
│  tab content (SCR-02/03)     │
│                              │
├──────────────────────────────┤
│ footer bar (CMP-08)          │
└──────────────────────────────┘
```

- **Top bar**, shared by both tabs: the ⓘ icon at the start (CMP-12, FR-INFO-03), the view switch **Market | Watchlist** in the middle (CMP-13) and the currency selector at the end (CMP-07). The top bar has no title text: the selected segment of the switch names the tab.
- There is no bottom navigation and no separate tab row.
- Switching tabs keeps each tab's state: search text, sort, scroll position (FR-NAV-01).
- The system Back button on a tab leaves the app (default behaviour).
- Content does not draw under the status bar (check at project setup).

### SCR-02 Market tab  *(FR-ML-01…09, FR-DATA-01…04)*
Owner: An Khánh.

```
┌──────────────────────────────┐
│ ⓘ   [Market|Watchlist] USD ▾ │  top bar (SCR-01)
│ [🔍 Search coins        ✕]   │  CMP-06
│ [Market cap ↓] [Price] [24h%]│  CMP-05
│ 1 ◉ Bitcoin   ╱╲╱   $67,234  │
│   BTC · MC $1.30T  [+2.35%]  │  CMP-01 rows
│ 2 ◉ Ethereum  ╲╱╲   $2,345   │
│   ...                        │
├──────────────────────────────┤
│ Powered by CoinGecko  🕒 14:32│  CMP-08
└──────────────────────────────┘
```

Top to bottom: the main top bar (SCR-01), search field, sort chips, the list (pull-to-refresh), footer bar.

Behaviour:
- On first display the list loads the top 100 coins in the current currency. If fewer than 100 are returned, those are shown without error (FR-ML-01).
- Search, sort and refresh as in CMP-06, CMP-05, CMP-09. Scroll position is preserved on refresh, search clearing, tab switch and return from Detail.
- Changing the currency: CMP-07.
- Data held in memory is reused inside the intervals of FR-DATA-01; the screen never calls the network just because it became visible again within the interval.

States:

| State | Search + chips | List area | Footer time |
|---|---|---|---|
| First load / currency change | visible | StateView Loading | hidden / old time until new data |
| Success | visible | rows | fetch time |
| Search has no match | visible | Empty "No coins found" | fetch time |
| Provider returned an empty list | visible | Empty "No coins to show" + Retry | hidden |
| Error, no data | visible | StateView Error + Retry (FR-DATA-04) | hidden |
| Error, data on screen | visible | rows stay; Snackbar + Retry | old time |

### SCR-03 Watchlist tab  *(FR-WL-01…05, 08…10)*
Owner: Bảo Linh.

Top to bottom: the main top bar (SCR-01; the Watchlist segment is selected), the list (pull-to-refresh), footer bar. **No search field and no sort chips.** The currency selector in the top bar works as on Market (CMP-07).

Behaviour:
- Shows the coins in the order they were added, **oldest first** (FR-WL-03). One watchlist only.
- Rows are CMP-01 with the same content as Market, with the star filled. Data is fetched for the favorite coins themselves, so a coin outside the top 100 is shown (FR-WL-04).
- Tap a row opens Detail. Tap the star removes the coin and its row disappears immediately, without a network call.
- A coin added on another screen is fetched the next time the Watchlist is shown (a new set of coins is a new request).
- Pull-to-refresh follows the 60 s rule (FR-WL-10).
- The favorites survive closing the app (FR-WL-02).

States:

| State | Content |
|---|---|
| No favorites | Empty: "Your watchlist is empty" with a button **Browse Market** that switches to the Market tab (FR-WL-05). No request is made. |
| Loading | StateView Loading |
| Success | rows; a coin the provider returned nothing for uses the "Data unavailable" row (FR-WL-09) |
| Error, no data | StateView Error + Retry |
| Error, data on screen | rows stay; Snackbar + Retry |

### SCR-04 Coin Detail  *(FR-CD-01, 06, 07, 08, 10, FR-CUR-05, FR-INFO-02, FR-NAV-02/03)*
Owner: Kiên. Opened with the string extra `coin_id` (C6). Uses the currency saved for the list.

```
┌──────────────────────────────┐
│ ←  Bitcoin          USD ▾  ☆ │  top bar + CMP-07, CMP-04
├──────────────────────────────┤
│ ◉ Bitcoin  BTC               │
│ $67,234.50   [ +2.35% ]      │  header
│ [1H][24H][7D][1M][1Y]        │  CMP-10   ┐
│  chart                       │  SCR-05   ┘ PriceChartFragment
│ Market cap ⓘ        $1.30T   │
│ 24h trading volume ⓘ $32.50B │  CMP-11
│ ...                          │
│ About Bitcoin                │
│ description (4 lines) ...    │
│ Read more                    │
│ For information only. Not    │
│ financial advice.            │
├──────────────────────────────┤
│ Powered by CoinGecko         │  CMP-08
└──────────────────────────────┘
```

Content scrolls; the footer bar is fixed. Top to bottom of the scrolling area:

1. **Header** (FR-CD-01): icon, name, symbol, current price (`PriceLarge`), 24h change label (CMP-02).
2. **Chart block:** `PriceChartFragment.newInstance(coinId)` (SCR-05). Detail does not contain chart code.
3. **Statistics:** CMP-11.
4. **Description** (FR-CD-07): plain text (markup removed, links not clickable), collapsed to about 4 lines with **Read more** / **Show less**; the control appears only if the text is longer than 4 lines. No description → "No description available." Always English.
5. **Disclaimer** (FR-INFO-02): "For information only. Not financial advice."

Top bar: back arrow, the coin name as title, the currency selector (CMP-07, Should, FR-CUR-05) and the favorite star (CMP-04, FR-CD-08).

Behaviour:
- Back returns to the previous tab exactly as it was (FR-NAV-03).
- No pull-to-refresh on this screen; data is refreshed when the screen is opened, within the 60 s rule (FR-DATA-01). Changing the currency here: CMP-07.
- The chart loads and fails independently of the rest of the screen.

States:

| State | Content |
|---|---|
| Loading (no data yet) | StateView Loading under the top bar |
| Success | all blocks |
| Error, no data (including an unknown coin) | StateView Error + Retry under the top bar; the chart block is not shown |
| Error, data on screen | blocks stay; Snackbar + Retry |
| Missing fields | `—` in the affected stat rows; total/max supply rows hidden; description message |

### SCR-05 Price chart area  *(FR-CD-02…05, FR-CD-11…13)*
Owner: Duy. Hosted by Detail, also hosted by the debug-only chart sandbox (`03` §13).

Contents: timeframe chips (CMP-10) above a chart of a fixed height from a named dimension, taking the full width.

Chart:
- Line chart of price for the selected timeframe, in the selected currency (FR-CD-02).
- **Colour** (S, FR-CD-05): `positive` if the last price of the period ≥ the first, `negative` otherwise. If FR-CD-05 is not built, one fixed colour.
- **Area fill** (S, FR-CD-11): the same colour at low opacity from the line to the bottom.
- **Reference line** (C, FR-CD-12): dashed horizontal line at the first price of the period.
- **Axis labels** (S, FR-CD-13): 3–4 price labels along the right edge (price format of §5.2); time labels along the bottom with the time format below.
- **Touch** (FR-CD-04): touching or dragging shows a vertical marker and a dot at the nearest data point and a label with its price (same format as the header) and its time. The label stays inside the chart bounds. Lifting the finger hides everything. While the marker is active the page does not scroll.

| Timeframe | Time format (axis labels and marker) |
|---|---|
| 1H, 24H | `HH:mm` |
| 7D | `dd/MM HH:mm` |
| 1M, 1Y | `dd/MM/yyyy` |

States (shown **in the chart area only**; chips and the rest of the screen stay usable):

| State | Content |
|---|---|
| Loading | progress indicator (also when switching timeframe) |
| Success | the chart |
| Empty | "Not enough data" (fewer than 2 points, for example 1H) |
| Error | message for the category and **Retry**, inside the chart area |

If the user switches timeframe quickly, only the response for the currently selected timeframe is displayed (FR-CD-03).

---

## 5. Where each state appears

| Screen / area | Loading | Empty | Error, no data | Error, data on screen |
|---|---|---|---|---|
| Market | StateView in list area | "No coins found" (search) / "No coins to show" | StateView + Retry | Snackbar + Retry |
| Watchlist | StateView in list area | "Your watchlist is empty" + Browse Market | StateView + Retry | Snackbar + Retry |
| Detail | StateView under top bar | — | StateView + Retry | Snackbar + Retry |
| Chart area | progress in chart area | "Not enough data" | message + Retry in chart area | — |

---

## 6. Content cases every layout must handle

Use these when checking a layout at 360×640 dp and 411×891 dp (the fake files are in `fake-data.md`):

| Case | Example |
|---|---|
| Long coin name | a 30–40 character name: the name shortens, price and change label stay whole |
| Short and long symbols | `BTC`, `USDT`, a 6-character symbol |
| Very small price | `$0.00001234` (8 decimals) |
| Large VND values | `1,650,000 ₫` price, `32.50T ₫` market cap |
| Rank | `1` and `100` |
| Change | positive, negative, zero, missing |
| Missing fields | use `markets_missing_fields_usd.json` and `detail_missing_fields.json` |
| Few results | `markets_short_usd.json` (3 coins), `markets_empty.json` |
| Long description | many lines, with line breaks; and an empty description |
| Watchlist | empty, 1 coin, 50 coins, a coin without data |

---

## 7. Out of scope (do not build)

- More than one watchlist, named or renamed watchlists, a screen to manage or reorder watchlists, an "add to which list" sheet.
- Choosing what the label on the right of a row shows (the row always shows price and 24h change; market cap is on the second line).
- A horizontal strip of coins above the Detail screen; swiping between coins.
- Detail as a bottom sheet (Detail is its own screen).
- Share or copy-link actions.
- Volume bars under the chart; timeframes other than 1H, 24H, 7D, 1M, 1Y.
- A settings screen, an about screen, a sign-in, a splash design beyond the system default.
- Voice search. Search beyond the loaded top 100.
- Light theme, a theme toggle, landscape, tablet layouts, an in-app language switch.
- News, portfolio, price alerts, notifications, trading.
- Pull-to-refresh on Detail; automatic retry (FR-ERR-04).
- A bottom navigation bar, a separate tab row under the top bar, swiping between the two tabs.

---

## 8. Requirement coverage

| Requirement | Where |
|---|---|
| FR-NAV-01, 02, 03 | SCR-01, CMP-13, SCR-02/03 (row tap), SCR-04 (Back) |
| FR-ML-01, 02, 09 | CMP-01, CMP-02, CMP-03 |
| FR-ML-03 | CMP-06 |
| FR-ML-04, 05 | CMP-05 |
| FR-ML-06 | CMP-09, SCR-02 |
| FR-ML-08 | CMP-04, CMP-01 |
| FR-CD-01, 06, 07, 08 | SCR-04, CMP-11, CMP-04 |
| FR-CD-10 | CMP-11, CMP-12 |
| FR-CD-02…05, 11…13 | SCR-05, CMP-10 |
| FR-WL-01…05, 08…10 | SCR-03, CMP-04, CMP-01 |
| FR-CUR-01…05 | CMP-07, SCR-01, SCR-04 |
| FR-DATA-01…04 | SCR-02/03/04 states, CMP-08, CMP-09 |
| FR-ERR-01…04 | §2.4, CMP-09, §5 |
| FR-INFO-01, 02 | CMP-08, SCR-04 |
| FR-INFO-03 | CMP-12, SCR-01 |
| FR-UI-01, 02 | §2.1, §0 rule 3 |
| NFR-08, 11, 12, 13 | §2.1, §2.5, §2.6, §0 rule 4 |

Could-priority items (FR-ML-10, FR-CD-09, FR-WL-06, 07) are not specified here beyond the note in CMP-11; they are specified when scheduled.

---

## 9. Decisions that apply

These are not stated in `01`; they hold for this document.

1. A coin row has two lines: name; then `SYMBOL · MC <market cap>`. Price and change label are on the right.
2. Search field and sort chips stay visible in every state of the Market tab; the StateView fills the list area. That area is the "full-screen error state" of FR-DATA-04.
3. The currency selector is part of the main top bar (both tabs) and, as a Should, of the Detail top bar.
4. The footer bar belongs to each screen (on the tabs it shows that tab's fetch time) and sits at the bottom of the screen.
5. Inline errors use a Snackbar with a Retry action.
6. The statistics are a single-column list (long VND values do not fit in two columns at 360 dp).
7. The star on a Watchlist row is never dropped; on a Market row it exists only if FR-ML-08 is built. If a row is too crowded at 360 dp, the coin name shrinks first (CMP-01).
8. Back on a tab leaves the app; no custom back handling.
9. Detail has no pull-to-refresh. Its currency selector is a Should (FR-CUR-05).
10. Sort and search text are kept while the app is running and are not saved across restarts.
11. The two tabs are switched with a pill toggle in the top bar (CMP-13); there is no bottom navigation, no separate tab row and no swiping between tabs.
