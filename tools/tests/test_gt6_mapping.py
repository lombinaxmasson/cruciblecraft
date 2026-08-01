from __future__ import annotations

import unittest

from tools import gt6_mapping


class GT6MappingTest(unittest.TestCase):
    def test_generation_tags_are_transcribed_to_namespaced_flags(self) -> None:
        self.assertEqual(
            "gt6:itemgenerator/plates",
            gt6_mapping.generation_tag_to_flag("ITEMGENERATOR.PLATES"),
        )
        self.assertEqual(
            "gt6:properties/common_ore",
            gt6_mapping.generation_tag_to_flag("PROPERTIES.COMMON_ORE"),
        )
        with self.assertRaises(ValueError):
            gt6_mapping.generation_tag_to_flag("ITEMGENERATOR.PLATES".lower())

    def test_ambiguous_gt6_prefixes_have_explicit_strategies(self) -> None:
        self.assertEqual("block", gt6_mapping.GT6_PREFIX_TO_CC["blockIngot"])
        self.assertEqual("block", gt6_mapping.GT6_PREFIX_TO_CC["blockGem"])
        self.assertNotIn("block", gt6_mapping.GT6_PREFIX_TO_CC)
        self.assertEqual(
            "compatibility_absorption",
            gt6_mapping.PREFIX_STRATEGIES["block"],
        )
        self.assertEqual(
            "compatibility_absorption",
            gt6_mapping.PREFIX_STRATEGIES["wire"],
        )
        self.assertEqual(
            "explicit_enumeration",
            gt6_mapping.PREFIX_STRATEGIES["wireGt01"],
        )
        self.assertEqual("wire", gt6_mapping.GT6_PREFIX_TO_CC["wireGt01"])


if __name__ == "__main__":
    unittest.main()
