import json
from pathlib import Path


ROOT = Path(__file__).parents[2]


def test_large_squeezer_source_contract_matches_structure():
    contract = json.loads(
        (ROOT / "tools/waves/machines/large-squeezer/source_contract.json").read_text(
            encoding="utf-8"
        )
    )
    structure = json.loads(
        (
            ROOT
            / "src/main/resources/data/cruciblecraft/multiblock_structures/large_squeezer.json"
        ).read_text(encoding="utf-8")
    )
    assert contract["source_meta"] == 17114
    assert contract["recipe_map"] == "RM.Squeezer"
    assert len(structure["structure"]) == 75
    assert sum(
        1
        for entry in structure["structure"]
        if structure["palette"][entry["predicate"]].get("port") == "item_fluid_in"
    ) == 25
    assert sum(
        1
        for entry in structure["structure"]
        if structure["palette"][entry["predicate"]].get("port") == "energy_input"
    ) == 2
