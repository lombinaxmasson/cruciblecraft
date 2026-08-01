import importlib.util
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "tools/run_material_registry_stress.py"
SPEC = importlib.util.spec_from_file_location("run_material_registry_stress", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class MaterialRegistryStressArtifactTest(unittest.TestCase):
    def test_committed_measurements_satisfy_budget(self):
        report = json.loads(MODULE.REPORT.read_text(encoding="utf-8"))
        budget = json.loads(MODULE.BUDGET.read_text(encoding="utf-8"))
        self.assertEqual([], MODULE.validate(report, budget))
        self.assertEqual(
            0,
            report["scenarios"]["metadata_only"]["registered_synthetic_items"],
        )
        self.assertEqual(
            20_000,
            report["scenarios"]["single_dust"]["registered_synthetic_items"],
        )


if __name__ == "__main__":
    unittest.main()
