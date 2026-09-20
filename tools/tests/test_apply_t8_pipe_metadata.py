import importlib.util
import json
import unittest
from pathlib import Path


TOOLS = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location(
    "apply_t8_pipe_metadata",
    TOOLS / "apply_t8_pipe_metadata.py",
)
assert SPEC and SPEC.loader
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class ApplyT8PipeMetadataTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.projection = MODULE.expected_projection()
        cls.planned = MODULE.planned_documents()

    def test_projection_matches_readiness_and_material_files(self):
        projection = self.projection
        self.assertEqual(61, len(projection))
        self.assertEqual(
            {
                "fluid_by_specification",
                "item_by_specification",
                "generation_flags",
            },
            set(projection["copper"]),
        )
        self.assertEqual(
            600,
            projection["copper"]["fluid_by_specification"]["pipeMedium"][
                "capacity_mb"
            ],
        )
        self.assertEqual({}, projection["copper"]["item_by_specification"])
        self.assertEqual(
            {},
            projection["brass"]["fluid_by_specification"],
        )
        self.assertEqual(
            1,
            projection["brass"]["item_by_specification"]["pipeMedium"][
                "stacks_per_second"
            ],
        )
        self.assertNotIn("tin", projection)
        self.assertNotIn("iron", projection)
        planned = self.planned
        self.assertEqual(1776, len(planned))
        copper = json.loads(
            planned[MODULE.MATERIALS / "copper.json"]
        )
        self.assertEqual(
            projection["copper"]["fluid_by_specification"],
            copper["gt6_metadata"]["pipe_properties"][
                "fluid_by_specification"
            ],
        )

    def test_committed_material_projection_is_current(self):
        stale = [
            str(path)
            for path, content in self.planned.items()
            if path.read_text(encoding="utf-8") != content
        ]
        self.assertEqual([], stale)


if __name__ == "__main__":
    unittest.main()
