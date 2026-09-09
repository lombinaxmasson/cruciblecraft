from __future__ import annotations

import unittest

from tools import gt6_resolve


class GT6ResolveTest(unittest.TestCase):
    def test_aluminium_capcellcon_is_registered(self) -> None:
        document = gt6_resolve.resolve("OP.capcellcon(MT.Al)")
        self.assertEqual("ok", document["status"])
        self.assertEqual("capcellcon", document["prefix"]["cc_prefix"])
        self.assertEqual("aluminium", document["material"]["cc_material"])
        self.assertEqual(
            "cruciblecraft:aluminium/capcellcon",
            document["form"]["item"],
        )
        self.assertTrue(document["form"]["registered"])

    def test_quadruple_casing_is_registered(self) -> None:
        document = gt6_resolve.resolve("OP.casingMachineQuadruple(MT.Invar)")
        self.assertEqual("ok", document["status"])
        self.assertEqual("machine_casing_quadruple", document["prefix"]["cc_prefix"])
        self.assertEqual("invar", document["material"]["cc_material"])
        self.assertEqual(
            "cruciblecraft:invar/machine_casing_quadruple",
            document["form"]["item"],
        )

    def test_quadruple_casing_follows_ordinary_casings(self) -> None:
        bronze = gt6_resolve.resolve("OP.casingMachineQuadruple(MT.Bronze)")
        steel = gt6_resolve.resolve("OP.casingMachineQuadruple(MT.Steel)")
        titanium = gt6_resolve.resolve("OP.casingMachineQuadruple(MT.Ti)")
        self.assertEqual("ok", bronze["status"])
        self.assertEqual("ok", steel["status"])
        self.assertEqual("ok", titanium["status"])
        self.assertEqual(
            "cruciblecraft:bronze/machine_casing_quadruple",
            bronze["form"]["item"],
        )

    def test_invar_machine_casing_is_generated(self) -> None:
        document = gt6_resolve.resolve("OP.casingMachine(MT.Invar)")
        self.assertEqual("ok", document["status"])
        self.assertEqual("machine_casing", document["prefix"]["cc_prefix"])
        self.assertTrue(document["form"]["registered"])

    def test_any_copper_pipe_stays_in_family(self) -> None:
        document = gt6_resolve.resolve("OP.pipeSmall(ANY.Cu)")
        self.assertEqual("ok", document["status"])
        self.assertEqual("small_fluid_pipe", document["prefix"]["cc_prefix"])
        self.assertEqual("family", document["material"]["kind"])
        self.assertIn("copper", document["material"]["cc_materials"])
        self.assertNotIn("invar", document["material"]["cc_materials"])

    def test_ta4hfc5_maps_to_existing_material(self) -> None:
        document = gt6_resolve.resolve("MT.Ta4HfC5")
        self.assertEqual("ok", document["status"])
        self.assertEqual("tantalum_hafnium_carbide", document["cc_material"])

    def test_ccc_shape_has_source_art(self) -> None:
        document = gt6_resolve.resolve("Shape_Extruder_CCC")
        self.assertEqual("ok", document["status"])
        self.assertEqual("cruciblecraft:extruder_shape_ccc", document["cc_item"])
        self.assertEqual(10028, document["meta"])
        self.assertTrue(str(document.get("art", "")).endswith("10028.png"))


if __name__ == "__main__":
    unittest.main()
