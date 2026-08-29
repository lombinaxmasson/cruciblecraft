"""Contract tests for the T38 Roaster compact source freeze."""
from __future__ import annotations

import copy
import tempfile
import unittest
from pathlib import Path
from typing import Any

from tools import build_t38_roaster_source as builder


def _family(number: int, expanded_count: int) -> builder.WorkFamily:
    key = f"{builder.SOURCE_MAP}#{number:04d}"
    return builder.WorkFamily(
        family_id=f"portfolio:track_a/cruciblecraft:roaster/{key}",
        template_key=key,
        record={
            "expanded_count": expanded_count,
            "cc_host_map": builder.HOST,
            "classification": "ordinary_optional",
            "membership_kind": "semantic_template",
            "template_key": key,
        },
    )


def _recipe(*, eu: int, duration: int, output: str = "minecraft:chest") -> dict[str, Any]:
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
        "inputs": [{"item": "minecraft:iron_ingot", "count": 1, "meta": 0}],
        "outputs": [{"item": output, "count": 1, "meta": 0}],
        "fluidInputs": [],
        "fluidOutputs": [],
        "chances": [10000],
        "maxChances": [10000],
    }


def _catalogs() -> builder.Catalogs:
    return builder.Catalogs(
        prefix_item_to_form={},
        material_id_to_cc={},
        registered_forms={"iron": {"ingot"}},
        form_items={("iron", "ingot"): "minecraft:iron_ingot"},
        prefix_tags={},
        fluid_to_cc={},
        reachable={"item:minecraft:iron_ingot", "item:minecraft:chest"},
    )


class T38RoasterSourceTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.work = builder.load_work_set()

    def test_work_set_is_exactly_29_roaster_ids_without_0002(self) -> None:
        self.assertEqual(29, len(self.work))
        self.assertEqual(
            [0, 1, *range(3, 30)],
            [
                int(builder.TEMPLATE_RE.fullmatch(item.template_key).group(1))
                for item in self.work
            ],
        )
        self.assertEqual(
            [
                "gt.recipe.roaster#0000",
                "gt.recipe.roaster#0001",
                *[f"gt.recipe.roaster#{number:04d}" for number in range(3, 30)],
            ],
            [item.template_key for item in self.work],
        )
        self.assertNotIn("gt.recipe.roaster#0002", [item.template_key for item in self.work])

    def test_t35_expanded_counts_match_each_frozen_family_and_total_73(self) -> None:
        self.assertEqual(73, sum(int(item.record["expanded_count"]) for item in self.work))
        for item in self.work:
            evidence = item.record.get("membership_evidence") or {}
            self.assertEqual(
                int(item.record["expanded_count"]),
                int(evidence["ordinary_optional_rows"]),
                item.family_id,
            )
            self.assertEqual(item.template_key, evidence["template_id"])

    def test_mixed_template_keeps_only_ordinary_and_zero_ordinary_group_is_unassigned(self) -> None:
        # Sizes deliberately force template ids #0000 (3 rows), #0001 (2 rows),
        # and #0002 (one non-ordinary row).
        recipes = [
            _recipe(eu=1, duration=10),
            _recipe(eu=1, duration=11),
            _recipe(eu=1, duration=12),
            _recipe(eu=2, duration=13),
            _recipe(eu=2, duration=14),
            _recipe(eu=3, duration=15),
        ]
        work = [_family(0, 1), _family(1, 1)]
        assigned, report = builder.assign_work_rows(recipes, work, {1, 4})
        self.assertEqual(2, report["assigned"])
        self.assertEqual([], report["missing"])
        self.assertEqual([], report["extra"])
        self.assertEqual([], report["duplicate"])
        self.assertEqual([1], [row[0] for row in assigned["gt.recipe.roaster#0000"]])
        self.assertEqual([4], [row[0] for row in assigned["gt.recipe.roaster#0001"]])
        self.assertNotIn("gt.recipe.roaster#0002", assigned)

    def test_assignment_is_73_without_missing_extra_or_duplicate(self) -> None:
        if not builder.dump_exists():
            self.skipTest("GT6 Roaster dump is unavailable")
        recipes, _digest = builder.load_roaster_recipes()
        ordinary, excluded = builder.load_ordinary_recipe_indices()
        _assigned, report = builder.assign_work_rows(recipes, self.work, ordinary)
        self.assertEqual(42, excluded)
        self.assertEqual(73, report["assigned"])
        self.assertEqual([], report["missing"])
        self.assertEqual([], report["extra"])
        self.assertEqual([], report["duplicate"])

    def test_multi_row_family_retains_all_ordinary_relations(self) -> None:
        recipes = [
            _recipe(eu=1, duration=10),
            _recipe(eu=1, duration=11),
            _recipe(eu=1, duration=12),
            _recipe(eu=2, duration=13),
        ]
        family = _family(0, 2)
        assigned, report = builder.assign_work_rows(recipes, [family], {0, 2})
        self.assertEqual(2, report["assigned"])
        self.assertEqual([], report["missing"])
        rows = assigned[family.template_key]
        relations = [
            builder.compile_relation(
                work=family,
                recipe=recipe,
                recipe_index=recipe_index,
                shadow_order=order,
                catalogs=_catalogs(),
            )[0]
            for order, (recipe_index, recipe) in enumerate(rows)
        ]
        self.assertEqual([0, 1], [row["shadow_order"] for row in relations])
        self.assertEqual({family.family_id}, {row["family_id"] for row in relations})

    def test_stable_id_is_independent_of_dump_order(self) -> None:
        work = _family(0, 1)
        recipe = _recipe(eu=1, duration=10)
        first, first_errors = builder.compile_relation(
            work=work,
            recipe=recipe,
            recipe_index=0,
            shadow_order=0,
            catalogs=_catalogs(),
        )
        # The identical ordinary row moved in the dump retains its content id.
        second, second_errors = builder.compile_relation(
            work=work,
            recipe=copy.deepcopy(recipe),
            recipe_index=1,
            shadow_order=0,
            catalogs=_catalogs(),
        )
        self.assertEqual([], first_errors)
        self.assertEqual([], second_errors)
        self.assertNotEqual(first["source_recipe_index"], second["source_recipe_index"])
        self.assertEqual(first["stable_id"], second["stable_id"])

    def test_missing_dump_full_replay_fails_closed(self) -> None:
        missing = Path(tempfile.gettempdir()) / "t38-missing-gt-roaster.json"
        if missing.is_file():
            missing.unlink()
        with self.assertRaises(OSError):
            builder.load_roaster_recipes(missing)
        original = builder.ROASTER_DUMP
        builder.ROASTER_DUMP = missing
        try:
            self.assertEqual(1, builder.main(["--full-replay"]))
        finally:
            builder.ROASTER_DUMP = original

    def test_check_is_read_only_when_committed_artifacts_exist(self) -> None:
        if not builder.OUTPUT.is_file() or not builder.RECEIPT.is_file():
            self.skipTest("compact source has not been generated")
        before = {
            path: path.read_bytes()
            for path in (builder.OUTPUT, builder.RECEIPT, builder.REVIEW)
            if path.is_file()
        }
        self.assertEqual([], builder.check())
        self.assertEqual(before, {path: path.read_bytes() for path in before})

    def test_non_ordinary_rows_never_enter_relations(self) -> None:
        recipes = [
            _recipe(eu=1, duration=10),
            _recipe(eu=1, duration=11),
            _recipe(eu=1, duration=12),
        ]
        family = _family(0, 1)
        assigned, report = builder.assign_work_rows(recipes, [family], {1})
        self.assertEqual(1, report["assigned"])
        relation, errors = builder.compile_relation(
            work=family,
            recipe=assigned[family.template_key][0][1],
            recipe_index=assigned[family.template_key][0][0],
            shadow_order=0,
            catalogs=_catalogs(),
        )
        self.assertEqual([], errors)
        self.assertEqual(1, relation["source_recipe_index"])


if __name__ == "__main__":
    unittest.main()
