#!/usr/bin/env python3
"""Build the T22.5 C0 artifact: per-machine playability coverage audit.

Three states per registered RecipeMap:
  registered_playable    authored recipes or material rules or manifest
                         expansions give it a playable v1 role
  registered_zero_logical  nothing: authored = 0, rules = 0, manifest
                         expansions = 0 -> v1 blocker with owner
  not_implemented        (only for GT6 maps CC never registered; the 32
                         registered maps cannot be in this state)

Counts come from expanders/manifests and authored-datapack scans —
never from re-counting files AS logical rows.  A zero is a derived
value: authored == 0 AND rules == 0 AND manifest expansions == 0.

The runtime guard is the GameTest
``t22_5RegisteredMapsHaveLogicalRecipes`` which asserts every map's
runtime recipe presence matches this audit's zero/non-zero claim.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path
from typing import Any


def _resolve_root() -> Path:
    return Path(__file__).resolve().parents[1]


ROOT = _resolve_root()
TOOLS = ROOT / "tools"
OUTPUT = TOOLS / "t22_5_machine_playability.json"
BUILDER = Path(__file__).resolve()

PUBLICATION_BASELINE = (
    ROOT / "src/main/resources/data/cruciblecraft/"
    "t22_publication_baseline.json"
)
RECIPE_DIRS = (
    ROOT / "src/generated/resources/data/cruciblecraft/recipe",
    ROOT / "src/main/resources/data/cruciblecraft/recipe",
    ROOT / "src/t5_chemical_generated/resources",
    ROOT / "src/t11_hydrocarbon_generated/resources",
    ROOT / "src/t21_chemical_generated/resources",
    ROOT / "src/t22_petroleum_generated/resources",
    ROOT / "src/component_rule_generated/resources/data/cruciblecraft/recipe",
    ROOT / "src/ore_chain_generated/resources",
)
COMPONENT_MANIFEST = TOOLS / "component_rule_manifest.json"
T7_READINESS = TOOLS / "t7_material_tag_readiness.json"
T8_READINESS = TOOLS / "t8_pipe_readiness.json"
T21_BASELINE = (
    ROOT / "src/main/resources/data/cruciblecraft/"
    "t21_publication_baseline.json"
)

# One-sentence v1 role per registered map (qualitative audit content).
V1_ROLES = {
    "cruciblecraft:anvil": "锻造主链：板材/杆材/螺栓的手工成型入口（T16 646 行展开由运行时 MaterialRule 承载）",
    "cruciblecraft:anvil_bend_big": "预留的大弯曲操作槽位",
    "cruciblecraft:anvil_bend_small": "预留的小弯曲操作槽位",
    "cruciblecraft:assembler": "组件装配主链（线缆/齿轮/转子，T3/T4 规则展开）",
    "cruciblecraft:autoclave": "高压化学容器（T5 化学链）",
    "cruciblecraft:bath": "洗矿 + T5 钨链化学（MaterialRule crushed_to_washed + 6 条钨链配方）",
    "cruciblecraft:bender": "板材折弯（组件规则展开）",
    "cruciblecraft:centrifuge": "矿物提纯主链（crushed -> purified/centrifuged，371 authored）",
    "cruciblecraft:coke_oven": "焦炉：煤炭 -> 焦炭与杂酚油",
    "cruciblecraft:compressor": "粉料压缩（dust -> plateGem 等）",
    "cruciblecraft:cooling": "热锭冷却（T10 hot ingot -> ingot MaterialRule）",
    "cruciblecraft:crusher": "矿物粉碎主链（raw ore -> crushed，494 authored + T20 矿链）",
    "cruciblecraft:cutter": "润滑油消费端（板材切割，组件规则展开）",
    "cruciblecraft:distillery": "蒸馏主链（T11 原油 / T22 石油化工，3 authored + T5 生成）",
    "cruciblecraft:drying": "干燥（T5 化学链）",
    "cruciblecraft:electrolyzer": "电解化学主链（T5 钨链，62 authored）",
    "cruciblecraft:extruder": "管线/型材挤压（T8 管线 257 展开 + 20 authored 模板）",
    "cruciblecraft:fuels_engine": "燃料引擎消费端（T22 润滑油）",
    "cruciblecraft:fuels_gas": "燃气消费端（T22）",
    "cruciblecraft:generifier": "气体重整（T22 煤油重整）",
    "cruciblecraft:lathe": "车削（组件规则展开）",
    "cruciblecraft:mixer": "化学混合主链（T21 碳族 gunpowder + 38 authored + T5/T22）",
    "cruciblecraft:mortar": "手动研磨（T7 691 条 mortar 规则展开）",
    "cruciblecraft:press": "冲压（组件规则展开）",
    "cruciblecraft:rollbender": "辊弯（组件规则展开）",
    "cruciblecraft:rollingmill": "轧制（组件规则展开）",
    "cruciblecraft:shredder": "矿物破碎（357 authored，T20 矿链）",
    "cruciblecraft:sifter": "矿物筛分（357 authored，T20 矿链）",
    "cruciblecraft:sluice": "矿物淘洗（357 authored，T20 矿链）",
    "cruciblecraft:smelter": "熔炼主链（202 authored + T16/T17 热金属规则展开）",
    "cruciblecraft:welder": "焊接（组件规则展开）",
    "cruciblecraft:wiremill": "线材轧制（组件规则展开）",
}


def _load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def _stable(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def scan_authored() -> tuple[dict[str, int], dict[str, int]]:
    """authored gt_recipe rows (map field) and material_rule files
    (target field) per map across all committed recipe dirs."""
    authored: dict[str, int] = {}
    rules: dict[str, int] = {}
    for base in RECIPE_DIRS:
        if not base.is_dir():
            continue
        for path in base.rglob("*.json"):
            try:
                document = _load(path)
            except (OSError, json.JSONDecodeError):
                continue
            if (
                document.get("type") == "cruciblecraft:gt_recipe"
                and isinstance(document.get("map"), str)
            ):
                authored[document["map"]] = (
                    authored.get(document["map"], 0) + 1
                )
            elif (
                document.get("type") == "cruciblecraft:material_rule"
                and isinstance(document.get("target"), str)
            ):
                rules[document["target"]] = (
                    rules.get(document["target"], 0) + 1
                )
    return authored, rules


def manifest_expansions() -> dict[str, int]:
    """Rule-expansion attribution from committed manifests only."""
    expanded: dict[str, int] = {}
    component = _load(COMPONENT_MANIFEST)
    for map_name, block in component.get("per_map", {}).items():
        expanded[f"cruciblecraft:{map_name}"] = block.get(
            "expanded_recipes", 0
        )
    t7 = _load(T7_READINESS)
    expanded["cruciblecraft:mortar"] = t7.get("post_t7_mortar_recipes", 0)
    t8 = _load(T8_READINESS)
    expanded["cruciblecraft:extruder"] = (
        expanded.get("cruciblecraft:extruder", 0)
        + t8.get("material_expansion_count", 0)
    )
    t21 = _load(T21_BASELINE)
    for delta in t21.get("registered_deltas", t21.get(
        "delta_ledger_policy", {}
    ).get("registered_deltas", [])):
        if delta.get("phase", "").startswith("T21"):
            expanded["cruciblecraft:mixer"] = (
                expanded.get("cruciblecraft:mixer", 0)
                + delta.get("logical", 0)
            )
    return expanded


def build() -> dict[str, Any]:
    baseline = _load(PUBLICATION_BASELINE)
    map_ids = list(baseline["recipe_map_ids"])
    if len(map_ids) != 32:
        raise ValueError(f"expected 32 recipe_map_ids, got {len(map_ids)}")
    authored, rules = scan_authored()
    expansions = manifest_expansions()
    unknown_roles = set(map_ids) - set(V1_ROLES)
    if unknown_roles:
        raise ValueError(f"missing v1_role for {sorted(unknown_roles)}")

    records = []
    blockers = []
    for map_id in map_ids:
        authored_n = authored.get(map_id, 0)
        rule_files = rules.get(map_id, 0)
        expanded_n = expansions.get(map_id, 0)
        logical = authored_n + expanded_n
        has_any = authored_n > 0 or rule_files > 0 or expanded_n > 0
        status = (
            "registered_playable"
            if has_any
            else "registered_zero_logical"
        )
        record = {
            "map": map_id,
            "status": status,
            "authored_gt_recipes": authored_n,
            "material_rule_files": rule_files,
            "rule_expanded_from_manifests": expanded_n,
            "logical_lower_bound": logical,
            "counting_note": (
                "logical lower bound = authored + manifest expansions; "
                "runtime MaterialRule expansion may add more. A zero "
                "here is derived: authored == 0 AND rule files == 0 AND "
                "manifest expansions == 0."
            ),
            "v1_role": V1_ROLES[map_id],
        }
        if status == "registered_zero_logical":
            record.update(
                {
                    "blocker": True,
                    "owner": "T22.5 C0 (registered RecipeMap with zero "
                    "recipes — player can build it and cannot use it)",
                }
            )
            blockers.append(map_id)
        records.append(record)

    return {
        "schema_version": 1,
        "status": "T22_5_MACHINE_PLAYABILITY_READY",
        "universe": {
            "source": _relative(PUBLICATION_BASELINE),
            "registered_maps": len(map_ids),
        },
        "counting_method": {
            "authored": (
                "one pass over committed datapack dirs counting "
                "cruciblecraft:gt_recipe entries by map field and "
                "cruciblecraft:material_rule files by target field"
            ),
            "expansions": (
                "component_rule_manifest.per_map, "
                "t7 post_t7_mortar_recipes, t8 pipe expansions, "
                "T21 publication delta"
            ),
            "zero_is_derived": True,
        },
        "records": records,
        "counts": {
            "registered_maps": len(map_ids),
            "registered_playable": len(records) - len(blockers),
            "registered_zero_logical": len(blockers),
            "blockers": blockers,
        },
        "runtime_guard": (
            "GameTest t22_5RegisteredMapsHaveLogicalRecipes asserts "
            "per-map runtime recipe presence matches this audit."
        ),
        "inputs": {
            _relative(PUBLICATION_BASELINE): _sha256(
                PUBLICATION_BASELINE
            ),
            _relative(COMPONENT_MANIFEST): _sha256(COMPONENT_MANIFEST),
            _relative(T7_READINESS): _sha256(T7_READINESS),
            _relative(T8_READINESS): _sha256(T8_READINESS),
            _relative(T21_BASELINE): _sha256(T21_BASELINE),
            _relative(BUILDER): _sha256(BUILDER),
        },
    }


def check() -> list[str]:
    errors: list[str] = []
    if not OUTPUT.is_file():
        errors.append(f"missing generated file: {_relative(OUTPUT)}")
        return errors
    on_disk = _load(OUTPUT)
    if on_disk.get("schema_version") != 1:
        errors.append("schema_version != 1")
    if on_disk.get("status") != "T22_5_MACHINE_PLAYABILITY_READY":
        errors.append("status != T22_5_MACHINE_PLAYABILITY_READY")
    expected = build()
    for row in expected["records"]:
        committed = next(
            (r for r in on_disk.get("records", [])
             if r.get("map") == row["map"]),
            None,
        )
        if committed is None:
            errors.append(f"missing record for {row['map']}")
            continue
        for key in (
            "authored_gt_recipes",
            "material_rule_files",
            "rule_expanded_from_manifests",
            "logical_lower_bound",
        ):
            if committed.get(key) != row[key]:
                errors.append(
                    f"{row['map']} {key} drifted: "
                    f"{committed.get(key)} != {row[key]}"
                )
        if committed.get("status") == "registered_zero_logical":
            if (
                row["authored_gt_recipes"]
                or row["material_rule_files"]
                or row["rule_expanded_from_manifests"]
            ):
                errors.append(
                    f"{row['map']} is zero-logical but derivation "
                    "found content"
                )
            if not committed.get("blocker") or not committed.get("owner"):
                errors.append(f"{row['map']} blocker lacks owner")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--reference-only", action="store_true")
    args = parser.parse_args(argv)
    try:
        if args.check:
            errors = check()
            if errors:
                raise ValueError("; ".join(errors))
            document = _load(OUTPUT)
        elif args.write:
            document = build()
            OUTPUT.write_text(
                _stable(document), encoding="utf-8", newline="\n"
            )
        else:
            parser.error("choose --check or --write")
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T22.5 machine playability failed: {error}",
              file=sys.stderr)
        return 1
    print(
        json.dumps(
            {
                "schema_version": document.get("schema_version"),
                "status": document.get("status"),
                "counts": document.get("counts"),
            },
            sort_keys=True,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
