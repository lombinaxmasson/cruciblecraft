from __future__ import annotations

import unittest
from collections import Counter

from tools import build_t30_hopper_source_evidence as builder
from tools import t27_common as common
from tools import t35_common as t35


class T30HopperSourceEvidenceTest(unittest.TestCase):
    def test_committed_artifact_is_current_and_check_is_read_only(self) -> None:
        before_evidence = builder.OUTPUT.read_bytes()
        catalog = builder._catalog_path(builder._policy())
        before_catalog = catalog.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before_evidence, builder.OUTPUT.read_bytes())
        self.assertEqual(before_catalog, catalog.read_bytes())

    def test_sixty_source_backed_rows_and_slot_distribution(self) -> None:
        document = builder.build()
        self.assertEqual("T30_HOPPER_SOURCE_EVIDENCE", document["status"])
        self.assertEqual(common.SOURCE_REVISION, document["source_revision"])
        self.assertEqual(60, document["counts"]["rows"])
        self.assertEqual(60, document["counts"]["tokens"])
        self.assertEqual(60, document["counts"]["cc_materials"])
        self.assertEqual(60, document["counts"]["plate_present"])
        self.assertEqual(0, document["counts"]["unresolved"])
        rows = document["rows"]
        self.assertEqual(60, len(rows))
        self.assertEqual(60, len({row["token"] for row in rows}))
        self.assertEqual(60, len({row["cc_material"] for row in rows}))
        self.assertTrue(all(row["material_json_present"] for row in rows))
        self.assertTrue(all(row["plate_present"] for row in rows))
        self.assertEqual(
            document["expected_slot_distribution"],
            {
                str(slots): count
                for slots, count in sorted(
                    Counter(row["slots"] for row in rows).items()
                )
            },
        )
        self.assertEqual(
            {
                "1": 1,
                "2": 2,
                "3": 3,
                "4": 6,
                "5": 5,
                "6": 6,
                "7": 3,
                "8": 3,
                "9": 6,
                "12": 3,
                "14": 1,
                "18": 4,
                "27": 6,
                "36": 11,
            },
            document["expected_slot_distribution"],
        )

    def test_any_w_maps_to_source_tungsten_not_a_guess(self) -> None:
        document = builder.build()
        by_token = {row["token"]: row for row in document["rows"]}
        tungsten = by_token["ANY.W"]
        self.assertEqual("MT.W", tungsten["representative_token"])
        self.assertEqual("Any Tungsten", tungsten["any_label"])
        self.assertEqual("cruciblecraft:tungsten", tungsten["cc_material"])
        self.assertEqual(36, tungsten["slots"])
        self.assertEqual(36, tungsten["queue_slots"])
        lead = by_token["MT.Pb"]
        self.assertEqual("cruciblecraft:lead", lead["cc_material"])
        self.assertEqual(1, lead["slots"])
        self.assertEqual(2, lead["queue_slots"])
        bismuth = by_token["MT.Bi"]
        self.assertEqual("cruciblecraft:bismuth", bismuth["cc_material"])
        self.assertEqual(2, bismuth["slots"])
        self.assertEqual(2, bismuth["queue_slots"])
        infinity = by_token["MT.Infinity"]
        self.assertEqual("cruciblecraft:infinity", infinity["cc_material"])
        self.assertEqual(36, infinity["slots"])
        self.assertEqual(8000 + lead["aID"], lead["hopper_gt6_id"])
        self.assertEqual(8200 + lead["aID"], lead["queue_gt6_id"])

    def test_runtime_catalog_is_flat_sixty_and_byte_stable(self) -> None:
        document = builder.build()
        catalog = builder.catalog_document(document)
        self.assertEqual(1, catalog["schema_version"])
        self.assertEqual(common.SOURCE_REVISION, catalog["source_revision"])
        self.assertEqual(60, len(catalog["variants"]))
        self.assertEqual(
            ["material", "slots"],
            sorted(catalog["variants"][0]),
        )
        ids = [row["material"] for row in catalog["variants"]]
        self.assertEqual(60, len(set(ids)))
        self.assertTrue(all(item.startswith("cruciblecraft:") for item in ids))
        self.assertNotIn("cruciblecraft:hopper", ids)
        catalog_path = builder._catalog_path(builder._policy())
        self.assertEqual(
            [],
            t35.check_compact(
                catalog_path,
                catalog,
                encode=common.stable_json,
            ),
        )


if __name__ == "__main__":
    unittest.main()
