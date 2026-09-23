"""Build the bounded Large Squeezer source-contract evidence.

Recipe rows are intentionally not duplicated: Large Squeezer consumes the
already-published shared RM.Squeezer map.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[4]
WAVE = Path(__file__).resolve().parent


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    contract = json.loads((WAVE / "source_contract.json").read_text(encoding="utf-8"))
    assert contract["source_meta"] == 17114
    assert contract["recipe_map"] == "RM.Squeezer"
    assert (ROOT / contract["structure"]).is_file()
    if args.check:
        print("large-squeezer: evidence current")


if __name__ == "__main__":
    main()
