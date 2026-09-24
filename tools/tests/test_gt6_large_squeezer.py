import json
import unittest
from pathlib import Path


ROOT = Path(__file__).parents[2]


class LargeSqueezerSourceContractTest(unittest.TestCase):
    def test_source_contract_matches_structure(self):
        contract = json.loads(
            (
                ROOT / "tools/waves/machines/large-squeezer/source_contract.json"
            ).read_text(encoding="utf-8")
        )
        structure = json.loads(
            (
                ROOT
                / "src/main/resources/data/cruciblecraft/multiblock_structures/large_squeezer.json"
            ).read_text(encoding="utf-8")
        )
        self.assertEqual(17114, contract["source_meta"])
        self.assertEqual("RM.Squeezer", contract["recipe_map"])
        self.assertEqual(75, len(structure["structure"]))
        self.assertEqual(
            25,
            sum(
                1
                for entry in structure["structure"]
                if structure["palette"][entry["predicate"]].get("port")
                == "item_fluid_in"
            ),
        )
        self.assertEqual(
            2,
            sum(
                1
                for entry in structure["structure"]
                if structure["palette"][entry["predicate"]].get("port")
                == "energy_input"
            ),
        )
