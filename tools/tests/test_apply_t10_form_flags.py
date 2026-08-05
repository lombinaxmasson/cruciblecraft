import importlib.util
import json
import unittest
from pathlib import Path


TOOLS = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location(
    "apply_t10_form_flags",
    TOOLS / "apply_t10_form_flags.py",
)
assert SPEC and SPEC.loader
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class ApplyT10FormFlagsTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.projection = MODULE.expected_projection()
        cls.planned = MODULE.planned_documents()

    def test_projection_matches_pinned_route_materials(self):
        projection = self.projection
        self.assertEqual(323, sum(
            MODULE.MULTI_FLAG in flags for flags in projection.values()
        ))
        self.assertEqual(321, sum(
            MODULE.HOT_FLAG in flags for flags in projection.values()
        ))
        self.assertEqual(
            {MODULE.MULTI_FLAG, MODULE.HOT_FLAG},
            projection["copper"],
        )
        planned = self.planned
        self.assertEqual(1773, len(planned))
        copper = json.loads(planned[MODULE.MATERIALS / "copper.json"])
        self.assertTrue(
            {MODULE.MULTI_FLAG, MODULE.HOT_FLAG}
            <= set(copper["generation_flags"])
        )

    def test_committed_material_projection_is_current(self):
        stale = [
            str(path)
            for path, content in self.planned.items()
            if path.read_text(encoding="utf-8") != content
        ]
        self.assertEqual(
            [],
            stale,
        )


if __name__ == "__main__":
    unittest.main()
