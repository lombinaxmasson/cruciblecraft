import importlib.util
import json
import unittest
from pathlib import Path


TOOLS = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location(
    "build_t10_container_readiness",
    TOOLS / "build_t10_container_readiness.py",
)
assert SPEC and SPEC.loader
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class BuildT10ContainerReadinessTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.fluid_gate, cls.cell_gate, cls.readiness = MODULE.build()

    def test_domains_fluid_additions_and_cell_gate_close_exactly(self):
        fluid_gate = self.fluid_gate
        cell_gate = self.cell_gate
        readiness = self.readiness
        self.assertEqual("READY", readiness["status"])
        self.assertEqual(61, readiness["counts"]["fluid_domain"])
        self.assertEqual(48, readiness["counts"]["gas_domain"])
        self.assertEqual(14, readiness["counts"]["containers_only_denied"])
        self.assertEqual(93, len(fluid_gate["fluids"]))
        self.assertEqual(109, len(cell_gate["fluids"]))
        self.assertEqual(
            MODULE.EXPECTED_DENYLIST,
            set(readiness["domains"]["containers_only_denied"]),
        )
        rows = {row["material"]: row for row in cell_gate["fluids"]}
        self.assertEqual("fluid", rows["chlorine"]["kind"])
        self.assertEqual("gas", rows["fluorine"]["kind"])
        self.assertEqual("cruciblecraft:steam", rows["steam"]["id"])
        self.assertEqual("cruciblecraft:creosote", rows["creosote"]["id"])
        self.assertNotIn("milk", rows)

    def test_committed_outputs_are_current(self):
        expected = {
            MODULE.T10_FLUID_GATE: MODULE.stable_json(self.fluid_gate),
            MODULE.CELL_GATE: MODULE.stable_json(self.cell_gate),
            MODULE.READINESS: MODULE.stable_json(self.readiness),
        }
        self.assertEqual(
            {},
            {
                str(path): "content drift"
                for path, content in expected.items()
                if path.read_text(encoding="utf-8") != content
            },
        )


if __name__ == "__main__":
    unittest.main()
