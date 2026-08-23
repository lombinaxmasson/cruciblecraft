"""Contract tests for the T37 Assembler source freeze."""
from __future__ import annotations

import json
import sys
import tempfile
import unittest
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t37_assembler_source as builder  # noqa: E402


SOURCE_REVISION = builder.SOURCE_REVISION
HOST = builder.HOST
SOURCE_MAP = builder.SOURCE_MAP


def _family_id(number: int) -> str:
    return f"portfolio:track_a/cruciblecraft:assembler/{SOURCE_MAP}#{number:04d}"


def _template_key(number: int) -> str:
    return f"{SOURCE_MAP}#{number:04d}"


def _family_record(number: int) -> dict[str, Any]:
    family_id = _family_id(number)
    template_key = _template_key(number)
    return {
        "cc_host_map": HOST,
        "classification": "ordinary_optional",
        "expanded_count": 1,
        "family_id": family_id,
        "membership_kind": "semantic_template",
        "template_key": template_key,
    }


def _census(family_ids: list[str]) -> dict[str, Any]:
    return {
        "source_revision": SOURCE_REVISION,
        "t37_pilot": {
            "family_ids": family_ids,
            "host_map": HOST,
            "source_rows": len(family_ids),
        },
    }


def _families_doc(numbers: list[int]) -> dict[str, Any]:
    return {
        "source_revision": SOURCE_REVISION,
        "families": [_family_record(number) for number in numbers],
    }


def _recipe(
    *,
    eu: int,
    duration: int,
    inputs: list[dict[str, Any]],
    outputs: list[dict[str, Any]],
) -> dict[str, Any]:
    return {
        "euPerTick": eu,
        "duration": duration,
        "specialValue": 0,
        "enabled": True,
        "hidden": False,
        "fake": False,
        "canBeBuffered": True,
        "needsEmptyOutput": False,
        "noNbtChecks": True,
        "inputs": inputs,
        "outputs": outputs,
        "fluidInputs": [],
        "fluidOutputs": [],
        "chances": [10000],
        "maxChances": [10000],
    }


def _item(item_id: str, count: int, meta: int = 0) -> dict[str, Any]:
    return {"item": item_id, "count": count, "meta": meta}


def _singleton_recipe(index: int, output_item: str = "minecraft:chest") -> dict[str, Any]:
    return _recipe(
        eu=16,
        duration=16 + index,
        inputs=[_item("minecraft:iron_ingot", index + 1)],
        outputs=[_item(output_item, 1)],
    )


def _assembler_dump(recipes: list[dict[str, Any]]) -> dict[str, Any]:
    return {"nameInternal": SOURCE_MAP, "recipes": recipes}


def _pilot_dump_recipes(
    *,
    unmapped_output: str | None = None,
) -> list[dict[str, Any]]:
    """52 semantic groups: two large families then 50 singletons #0002-#0051."""
    recipes: list[dict[str, Any]] = []
    large_a = _recipe(
        eu=1,
        duration=8,
        inputs=[_item("minecraft:iron_ingot", 1)],
        outputs=[_item("minecraft:chest", 1)],
    )
    large_b = _recipe(
        eu=2,
        duration=8,
        inputs=[_item("minecraft:iron_ingot", 1)],
        outputs=[_item("minecraft:chest", 1)],
    )
    recipes.extend([dict(large_a) for _ in range(3)])
    recipes.extend([dict(large_b) for _ in range(2)])
    for index in range(50):
        output = unmapped_output if index == 0 and unmapped_output else "minecraft:chest"
        recipes.append(_singleton_recipe(index, output))
    return recipes


def _empty_catalogs() -> builder.Catalogs:
    return builder.Catalogs(
        prefix_item_to_form={},
        material_id_to_cc={},
        registered_forms={"iron": {"ingot"}},
        form_items={("iron", "ingot"): "minecraft:iron_ingot"},
        prefix_tags={},
        fluid_to_cc={},
        reachable={"item:minecraft:iron_ingot", "item:minecraft:chest"},
    )


class T37AssemblerSourceTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.work = builder.load_work_set()

    def test_work_set_is_exactly_assembler_0002_to_0051(self) -> None:
        self.assertEqual(50, len(self.work))
        numbers = [
            int(builder.TEMPLATE_RE.fullmatch(item.template_key).group(1))
            for item in self.work
        ]
        self.assertEqual(list(range(2, 52)), numbers)
        family_ids = [item.family_id for item in self.work]
        self.assertTrue(all("#0002" in family_ids[0] for _ in [0]))
        self.assertTrue(self.work[0].family_id.endswith("#0002"))
        self.assertTrue(self.work[-1].family_id.endswith("#0051"))
        self.assertFalse(any(item.family_id.endswith("#0000") for item in self.work))
        self.assertFalse(any(item.family_id.endswith("#0001") for item in self.work))
        self.assertEqual(50, len(set(family_ids)))

    def test_host_classification_template_and_singleton_against_t35(self) -> None:
        for item in self.work:
            record = item.record
            self.assertEqual(HOST, record["cc_host_map"])
            self.assertEqual("ordinary_optional", record["classification"])
            self.assertEqual("semantic_template", record["membership_kind"])
            self.assertEqual(1, int(record["expanded_count"]))
            self.assertEqual(item.template_key, record["template_key"])
            self.assertRegex(item.template_key, r"^gt\.recipe\.assembler#\d{4}$")
            self.assertTrue(item.family_id.endswith(item.template_key.rsplit("/", 1)[-1]))

    def test_assignment_is_50_with_no_missing_extra_or_duplicate(self) -> None:
        recipes = _pilot_dump_recipes()
        work = builder.load_work_set(
            _census([_family_id(number) for number in range(2, 52)]),
            _families_doc(list(range(2, 52))),
        )
        assigned, report = builder.assign_work_rows(recipes, work)
        self.assertEqual(50, len(assigned))
        self.assertEqual([], report["missing"])
        self.assertEqual([], report["extra"])
        self.assertEqual([], report["duplicate"])
        self.assertNotIn(f"{SOURCE_MAP}#0000", assigned)
        self.assertNotIn(f"{SOURCE_MAP}#0001", assigned)

    def test_check_is_read_only(self) -> None:
        if not builder.OUTPUT.is_file() or not builder.RECEIPT.is_file():
            self.skipTest("compact source not generated")
        before_out = builder.OUTPUT.read_bytes()
        before_receipt = builder.RECEIPT.read_bytes()
        before_review = (
            builder.REVIEW.read_bytes() if builder.REVIEW.is_file() else None
        )
        errors = builder.check()
        self.assertEqual([], errors)
        self.assertEqual(before_out, builder.OUTPUT.read_bytes())
        self.assertEqual(before_receipt, builder.RECEIPT.read_bytes())
        if before_review is not None:
            self.assertEqual(before_review, builder.REVIEW.read_bytes())

    def test_missing_dump_full_replay_fails_closed(self) -> None:
        missing = Path(tempfile.gettempdir()) / "t37-missing-gt-assembler.json"
        if missing.is_file():
            missing.unlink()
        with self.assertRaises(OSError) as raised:
            builder.load_assembler_recipes(missing)
        self.assertIn("missing GT6 dump", str(raised.exception))
        original = builder.ASSEMBLER_DUMP
        builder.ASSEMBLER_DUMP = missing
        try:
            self.assertEqual(1, builder.main(["--full-replay"]))
        finally:
            builder.ASSEMBLER_DUMP = original

    def test_stable_id_is_independent_of_dump_order(self) -> None:
        recipes_a = _pilot_dump_recipes()
        recipes_b = list(recipes_a)
        # Swap two singleton rows that share no identity with #0000/#0001.
        recipes_b[5], recipes_b[6] = recipes_b[6], recipes_b[5]
        work = builder.load_work_set(
            _census([_family_id(number) for number in range(2, 52)]),
            _families_doc(list(range(2, 52))),
        )
        catalogs = _empty_catalogs()
        rel_a, report_a, errors_a = builder.compile_relations(recipes_a, work, catalogs)
        rel_b, report_b, errors_b = builder.compile_relations(recipes_b, work, catalogs)
        self.assertEqual([], errors_a)
        self.assertEqual([], errors_b)
        self.assertEqual([], report_a["missing"])
        self.assertEqual([], report_b["missing"])
        ids_a = {row["family_id"]: row["stable_id"] for row in rel_a}
        ids_b = {row["family_id"]: row["stable_id"] for row in rel_b}
        self.assertEqual(ids_a, ids_b)
        index_a = {row["family_id"]: row["source_recipe_index"] for row in rel_a}
        index_b = {row["family_id"]: row["source_recipe_index"] for row in rel_b}
        self.assertNotEqual(index_a, index_b)

    def test_unmapped_operand_fails_closed(self) -> None:
        recipes = _pilot_dump_recipes(unmapped_output="evil:unknown_widget")
        work = builder.load_work_set(
            _census([_family_id(number) for number in range(2, 52)]),
            _families_doc(list(range(2, 52))),
        )
        relations, _report, errors = builder.compile_relations(
            recipes, work, _empty_catalogs()
        )
        self.assertTrue(any("blocked_unmapped" in error for error in errors))
        document = builder.build_document(relations, work, {
            "missing": [],
            "extra": [],
            "duplicate": [],
        }, errors)
        self.assertEqual("T37_ASSEMBLER_SOURCE_BLOCKED", document["status"])
        self.assertTrue(document["blockers"])


class T37AssemblerSourceWriteTest(unittest.TestCase):
    def test_check_does_not_write_tmp_outputs(self) -> None:
        recipes = _pilot_dump_recipes()
        work_ids = [_family_id(number) for number in range(2, 52)]
        census = _census(work_ids)
        families_doc = _families_doc(list(range(2, 52)))
        catalogs = _empty_catalogs()
        with tempfile.TemporaryDirectory() as tmp:
            tmp_path = Path(tmp)
            dump_path = tmp_path / "gt.recipe.assembler.json"
            dump_path.write_text(
                json.dumps(_assembler_dump(recipes), indent=2) + "\n",
                encoding="utf-8",
            )
            original = (
                builder.OUTPUT,
                builder.RECEIPT,
                builder.REVIEW,
                builder.ASSEMBLER_DUMP,
            )
            builder.OUTPUT = tmp_path / "t37_assembler_source.json"
            builder.RECEIPT = tmp_path / "t37_assembler_source_receipt.json"
            builder.REVIEW = tmp_path / "t37_assembler_source_review.json"
            builder.ASSEMBLER_DUMP = dump_path
            try:
                builder.write(
                    dump_path=dump_path,
                    census=census,
                    families_doc=families_doc,
                    catalogs=catalogs,
                )
                before = {
                    "out": builder.OUTPUT.read_bytes(),
                    "receipt": builder.RECEIPT.read_bytes(),
                    "review": builder.REVIEW.read_bytes(),
                }
                self.assertEqual([], builder.check())
                self.assertEqual(before["out"], builder.OUTPUT.read_bytes())
                self.assertEqual(before["receipt"], builder.RECEIPT.read_bytes())
                self.assertEqual(before["review"], builder.REVIEW.read_bytes())
            finally:
                (
                    builder.OUTPUT,
                    builder.RECEIPT,
                    builder.REVIEW,
                    builder.ASSEMBLER_DUMP,
                ) = original


if __name__ == "__main__":
    unittest.main()
