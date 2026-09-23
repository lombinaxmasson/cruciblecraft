"""Static landing checks for the GT6 Large Squeezer contract."""

from __future__ import annotations

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[4]
CONTRACT = Path(__file__).with_name("source_contract.json")
STRUCTURE = ROOT / "src/main/resources/data/cruciblecraft/multiblock_structures/large_squeezer.json"


def validate() -> None:
    contract = json.loads(CONTRACT.read_text(encoding="utf-8"))
    structure = json.loads(STRUCTURE.read_text(encoding="utf-8"))
    assert contract["source_meta"] == 17114
    assert contract["recipe_map"] == "RM.Squeezer"
    assert contract["structure_contract"]["wall_blocks"] == 65
    assert len(structure["structure"]) == 75
    assert structure["source"]["class"].endswith("MultiTileEntitySqueezer")


if __name__ == "__main__":
    validate()
    print("large-squeezer: source contract ok")
