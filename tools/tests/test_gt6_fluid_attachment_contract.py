from __future__ import annotations

import unittest

from tools import gt6_fluid_attachment_contract as contract


class Gt6FluidAttachmentContractTest(unittest.TestCase):
    def test_player_surface_contract_is_green(self) -> None:
        self.assertEqual([], contract.check())

    def test_contract_keeps_the_gt6_kind_census_explicit(self) -> None:
        document = contract._load_json(contract.CONTRACT)
        self.assertEqual(47, document["expected_total"])
        self.assertEqual(
            {
                "FAUCET": 23,
                "TAP": 6,
                "FUNNEL": 6,
                "NOZZLE": 6,
                "CAP_NOZZLE": 6,
            },
            {
                kind: spec["expected"]
                for kind, spec in document["kinds"].items()
            },
        )

    def test_model_texture_ids_resolve_to_texture_root(self) -> None:
        path = contract._texture_path(
            "cruciblecraft:block/gt6_import/mte/faucet"
        )
        self.assertEqual(
            contract.RESOURCES
            / "assets"
            / "cruciblecraft"
            / "textures"
            / "block"
            / "gt6_import"
            / "mte"
            / "faucet.png",
            path,
        )


if __name__ == "__main__":
    unittest.main()
