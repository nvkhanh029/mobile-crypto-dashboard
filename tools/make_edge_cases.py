#!/usr/bin/env python3
"""
Create the edge-case fake-data files listed in docs/fake-data.md, section 3.

Usage (from the repository root):
    python tools/make_edge_cases.py                 # reads and writes app/src/main/assets/fake
    python tools/make_edge_cases.py <fake_dir>      # use another folder

Rules this script follows:
  * It only READS the captured real files (markets_usd.json, detail_bitcoin.json, ...).
    It never changes them.
  * It only WRITES the 7 file names below. Running it again gives the same result.
  * The one edge-case file that already exists (detail_ethereum_edited.json) is not touched.

Files written:
    markets_short_usd.json
    markets_empty.json
    markets_invalid.json
    markets_missing_fields_usd.json
    markets_by_ids_missing_usd.json
    detail_missing_fields.json
    chart_short_1d_usd.json
"""

import json
import sys
from pathlib import Path

# ---------------------------------------------------------------------------
# STEP 0 - Where are the files?
# ---------------------------------------------------------------------------
# By default the folder is <repo root>/app/src/main/assets/fake. 
# The repo root is the parent of the folder that contains this script (tools/).

if len(sys.argv) > 1:
    FAKE_DIR = Path(sys.argv[1])
else:
    FAKE_DIR = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "assets" / "fake"

if not FAKE_DIR.is_dir():
    sys.exit(f"Folder not found: {FAKE_DIR}")

EDGE_DIR = FAKE_DIR / "edge"
EDGE_DIR.mkdir(exist_ok=True)

def read_text(name: str) -> str:
    """Read a file as UTF-8 text."""
    return (FAKE_DIR / name).read_text(encoding="utf-8")


def read_json(name: str):
    """Read a file and parse it. Fails loudly if the captured file is missing."""
    return json.loads(read_text(name))


def write_json(name: str, data) -> None:
    """Write data as JSON. UTF-8, LF line ends (same on Windows and Linux), no BOM."""
    text = json.dumps(data, ensure_ascii=False, indent=2) + "\n"
    with open(EDGE_DIR / name, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)
    print(f"  wrote {name}")


print(f"Fake data folder: {FAKE_DIR}")

# ---------------------------------------------------------------------------
# STEP 1 - markets_short_usd.json  (tests FR-ML-01: fewer than 100 coins)
# ---------------------------------------------------------------------------
# Keep only the first 3 coins (bitcoin, ethereum, ...). The structure of each coin
# is identical to the real file.
markets = read_json("markets_usd.json")
write_json("markets_short_usd.json", markets[:3])

# ---------------------------------------------------------------------------
# STEP 2 - markets_empty.json  (empty state)
# ---------------------------------------------------------------------------
# Not based on a real file: the API can answer with an empty array.
write_json("markets_empty.json", [])

# ---------------------------------------------------------------------------
# STEP 3 - markets_invalid.json  (error "invalid data")
# ---------------------------------------------------------------------------
# Take the raw TEXT (not the parsed data) and cut it in the middle. The result 
# stops in the middle of a coin, so a JSON parser must throw an exception.+
raw = read_text("markets_usd.json")
cut = raw[: len(raw) // 2]
try:
    json.loads(cut)
    sys.exit("markets_invalid.json would still be valid JSON - change the cut position")
except json.JSONDecodeError:
    pass  # good: it is invalid, as intended
with open(EDGE_DIR / "markets_invalid.json", "w", encoding="utf-8", newline="\n") as f:
    f.write(cut)
print("  wrote markets_invalid.json")

# ---------------------------------------------------------------------------
# STEP 4 - markets_missing_fields_usd.json  (FR-ML-09, sort with missing values)
# ---------------------------------------------------------------------------
# A different coin loses a different field, so each kind of missing value can be
# tested on its own. Index in the list = position in the market-cap order.
markets = read_json("markets_usd.json")
missing_plan = [
    (4, "current_price"),                   # 5th coin: no price
    (9, "market_cap"),                      # 10th coin: no market cap
    (14, "price_change_percentage_24h"),    # 15th coin: no 24h change
    (19, "market_cap_rank"),                # 20th coin: no rank
    (24, "sparkline_in_7d"),                # 25th coin: no sparkline
]
for index, field in missing_plan:
    assert field in markets[index], f"{field} not found in coin {index}"
    markets[index][field] = None
    print(f"    coin #{index + 1} ({markets[index]['id']}): {field} = null")
write_json("markets_missing_fields_usd.json", markets)

# ---------------------------------------------------------------------------
# STEP 5 - markets_by_ids_missing_usd.json  (FR-WL-09: favorite coin has no data)
# ---------------------------------------------------------------------------
# The real API answers 200 and simply leaves out an id it does not return (seen in the
# S1 capture with an unknown id). Here we remove the LAST requested coin.
by_ids = read_json("markets_by_ids_usd.json")
removed = by_ids[-1]["id"]
write_json("markets_by_ids_missing_usd.json", by_ids[:-1])
print(f"    removed coin: {removed}")

# ---------------------------------------------------------------------------
# STEP 6 - detail_missing_fields.json  (FR-CD-06, FR-CD-07)
# ---------------------------------------------------------------------------
# Based on bitcoin, because bitcoin really has max_supply, so "null" is a real change.
# - max_supply, total_supply: null
# - ath_date / atl_date: in /coins/{id} these are objects keyed by currency, so the
#   values the app reads (usd, vnd) are set to null; other keys stay as captured.
# - description.en: empty text
detail = read_json("detail_bitcoin.json")
market_data = detail["market_data"]
market_data["max_supply"] = None
market_data["total_supply"] = None
for key in ("ath_date", "atl_date"):
    for currency in ("usd", "vnd"):
        assert currency in market_data[key], f"{key}.{currency} not found"
        market_data[key][currency] = None
detail["description"]["en"] = ""
write_json("detail_missing_fields.json", detail)

# ---------------------------------------------------------------------------
# STEP 7 - chart_short_1d_usd.json  (1H "Not enough data")
# ---------------------------------------------------------------------------
# Each chart file has 3 lists: prices, market_caps, total_volumes. Keep the last point
# of each. Timestamps stay as captured (we never shift them).
chart = read_json("chart_bitcoin_1d_usd.json")
short_chart = {key: values[-1:] for key, values in chart.items()}
write_json("chart_short_1d_usd.json", short_chart)

# ---------------------------------------------------------------------------
# STEP 8 - Self-check
# ---------------------------------------------------------------------------
print("Checking results...")
assert len(read_json("edge/markets_short_usd.json")) == 3
assert read_json("edge/markets_empty.json") == []
assert len(read_json("edge/markets_missing_fields_usd.json")) == 100
assert len(read_json("edge/markets_by_ids_missing_usd.json")) == len(by_ids) - 1
checked = read_json("edge/detail_missing_fields.json")
assert checked["market_data"]["max_supply"] is None
assert checked["description"]["en"] == ""
assert all(len(v) == 1 for v in read_json("edge/chart_short_1d_usd.json").values())
print("All good. 7 edge-case files are ready.")