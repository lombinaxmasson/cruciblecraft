import json
import unittest
from pathlib import Path


TOOLS = Path(__file__).resolve().parents[1]


class ComponentSelectorPolicyTest(unittest.TestCase):
    def test_machine_specific_selector_decisions_are_locked(self):
        policy = json.loads(
            (TOOLS / "component_selector_policy.json").read_text(
                encoding="utf-8"
            )
        )
        maps = policy["maps"]
        self.assertEqual(
            ("T3", "concrete_shape", "explicit_sparse_relation"),
            (
                maps["extruder"]["delivery"],
                maps["extruder"]["selector"],
                maps["extruder"]["support"],
            ),
        )
        self.assertEqual(
            ("T2", "byproduct"),
            (maps["bath"]["delivery"], maps["bath"]["selector"]),
        )
        self.assertEqual(
            ("T2", "processing_target"),
            (maps["shredder"]["delivery"], maps["shredder"]["selector"]),
        )
        self.assertEqual(
            ("T5", "deferred", "deferred"),
            (
                maps["mixer"]["delivery"],
                maps["mixer"]["selector"],
                maps["mixer"]["status"],
            ),
        )
        self.assertEqual(
            {"extruder"},
            {
                name
                for name, decision in maps.items()
                if decision["selector"] == "concrete_shape"
            },
        )


if __name__ == "__main__":
    unittest.main()
