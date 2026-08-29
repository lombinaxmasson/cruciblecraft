"""Contract tests for the T43 GT stone catalog."""
from __future__ import annotations

import unittest

from tools import build_t43_stone_catalog as builder
from tools import build_t43_work_set as work_set
from tools import t43_common as common


class T43StoneCatalogTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        if not common.WORK_SET.is_file():
            work_set.main(["--write"])
        cls.document = builder.build_catalog()

    def test_catalog_has_119_identities_and_does_not_fold_slabs(self) -> None:
        self.assertEqual(119, self.document["identity_count"])
        self.assertEqual(17, self.document["full_identity_count"])
        self.assertEqual(102, self.document["slab_identity_count"])
        slabs = [row for row in self.document["identities"] if row["kind"] == "slab"]
        self.assertEqual(102, len(slabs))
        self.assertTrue(all(row["slab_variant"] in range(6) for row in slabs))
        runtime_ids = [
            variant["runtime_id"]
            for row in self.document["identities"]
            for variant in row["variants"]
        ]
        self.assertEqual(len(runtime_ids), len(set(runtime_ids)))
        self.assertTrue(all(item.startswith("cruciblecraft:gt_stone/") for item in runtime_ids))

    def test_write_then_check_is_zero_drift(self) -> None:
        self.assertEqual(0, builder.main(["--write"]))
        self.assertEqual(0, builder.main(["--check"]))
