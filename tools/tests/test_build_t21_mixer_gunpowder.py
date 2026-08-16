import copy
import sys
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t21_mixer_gunpowder as builder  # noqa: E402


class T21MixerGunpowderTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.expected = builder.load(builder.EXPECTED)
        cls.manifest = builder.load(builder.MANIFEST)

    def test_compact_manifest_and_runtime_files_are_current(self):
        builder.validate_compact(self.expected, self.manifest)
        self.assertEqual([], builder.check_outputs(None))
        self.assertEqual(
            {
                "source_facts": 4,
                "authored_rules": 1,
                "datapack_files": 4,
                "logical_rows": 4,
                "eager_rows": 4,
                "lazy_rows": 0,
            },
            self.manifest["counts"],
        )

    def test_family_members_are_exact_and_content_consumed(self):
        self.assertEqual(
            ["carbon", "charcoal", "coal", "coal_coke"],
            [row["material"] for row in self.manifest["rows"]],
        )
        for row in self.expected["rows"]:
            recipe = row["recipe"]
            self.assertEqual("cruciblecraft:mixer", recipe["map"])
            self.assertEqual(64, recipe["duration"])
            self.assertEqual(16, recipe["eut"])
            self.assertEqual(
                {"count": 4, "id": "minecraft:gunpowder"},
                recipe["item_outputs"][0],
            )

    def test_full_template_replay_matches_independent_expected(self):
        full = builder.build_documents()
        self.assertEqual(self.expected, full[0])
        self.assertEqual(self.manifest, full[1])
        self.assertEqual([], builder.check_outputs(full))

    def test_wrong_expected_amount_is_detectable(self):
        changed = copy.deepcopy(self.expected)
        changed["rows"][0]["recipe"]["item_outputs"][0]["count"] = 3
        full = builder.build_documents()
        self.assertNotEqual(changed, full[0])


if __name__ == "__main__":
    unittest.main()
