#!/usr/bin/env python3
"""Build the T28 GT6 hot-ingot lifecycle source-evidence ledger.

Scans the fixed-revision GregTech 6 Java tree for ingotHot identity, heat
damage, passive conversion, cooler maps, and freezer usage. Records the
current CrucibleCraft auto-conversion path without changing runtime.

The ledger is fail-closed: any unexpected ingotHot hit, Cooler map, or
freezer recipe that consumes ingotHot as a prefix conversion is an error.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t27_common as common  # noqa: E402

TOOLS = common.TOOLS
OUTPUT = TOOLS / "t28_hot_ingot_source_evidence.json"
BUILDER = Path(__file__).resolve()

GT6_JAVA = ROOT / "gt6_code" / "gregtech6" / "src" / "main" / "java"
OP_JAVA = GT6_JAVA / "gregapi" / "data" / "OP.java"
UT_JAVA = GT6_JAVA / "gregapi" / "util" / "UT.java"
RM_JAVA = GT6_JAVA / "gregapi" / "data" / "RM.java"
FREEZER_DUMP = ROOT / "gt6_dump" / "gt6_recipe_dump" / "maps" / "gt.recipe.freezer.json"
T13_RECIPE_MAPS = TOOLS / "t13_denominators" / "recipe_maps.json"
T27_PORTFOLIO_MAPS = TOOLS / "t27_portfolio" / "recipe_maps.json"

CC_COOLING_RULE = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "t10"
    / "cooling"
    / "hot_ingot_to_ingot.json"
)
CC_PATHS = {
    "material_item_cooling": ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "heat"
    / "MaterialItemCooling.java",
    "heat_maintenance_events": ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "heat"
    / "HeatMaintenanceEvents.java",
    "hot_ingot_processing": ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "heat"
    / "HotIngotProcessing.java",
    "item_heat": ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "heat"
    / "ItemHeat.java",
    "material_contact_heat": ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "heat"
    / "MaterialContactHeat.java",
    "mod_recipe_maps": ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "registry"
    / "ModRecipeMaps.java",
    "cooling_rule": CC_COOLING_RULE,
}

INGOT_HOT_RE = re.compile(r"\bingotHot\b")
COOLER_RES = (
    re.compile(r"\bRM\.Cooler\b"),
    re.compile(r"gt\.recipe\.cooler\b"),
    re.compile(r"\bRecipeMapCooler\b"),
)
FREEZER_FIELD_RE = re.compile(r"\bFreezer\s*=")
HEAT_DAMAGE_ASSIGN = "ingotHot.mHeatDamage = 3.0F"
HEAT_DAMAGE_READER = "getHeatDamageFromItem"
ALLOWED_INGOT_HOT_CLASSES = {
    "prefix_definition",
    "heat_damage_assignment",
    "item_registration",
    "recipe_exclusion",
}
CONVERSION_CLASSES = {"prefix_conversion", "unexpected"}


REQUIRED_CC_KEYS = (
    "heat_maintenance_events",
    "hot_ingot_processing",
    "item_heat",
    "material_contact_heat",
    "mod_recipe_maps",
)


def _require_file(path: Path) -> None:
    if not path.is_file():
        raise FileNotFoundError(common.relative(path))


def _read_java(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace").replace("\r", "")


COOLER_RE = re.compile("|".join(pattern.pattern for pattern in COOLER_RES))
FREEZER_JAVA_RE = re.compile(r"\bRM\.Freezer\b|\bgt\.recipe\.freezer\b")
INGOT_HOT_ITEM = "gregtech:gt.meta.ingotHot"
ORDINARY_INGOT_ITEM = "gregtech:gt.meta.ingot"


def _hit(path: Path, line: int, text: str) -> dict[str, Any]:
    return {
        "line": line,
        "path": common.relative(path),
        "text": text.strip(),
    }


def _hits(path: Path, pattern: re.Pattern[str]) -> list[dict[str, Any]]:
    found: list[dict[str, Any]] = []
    for index, line in enumerate(_read_java(path).splitlines(), start=1):
        if pattern.search(line):
            found.append(_hit(path, index, line))
    return found


def _scan_java_tree() -> dict[str, list[dict[str, Any]]]:
    if not GT6_JAVA.is_dir():
        raise FileNotFoundError(common.relative(GT6_JAVA))
    buckets: dict[str, list[dict[str, Any]]] = {
        "cooler": [],
        "freezer": [],
        "ingotHot": [],
    }
    for path in sorted(GT6_JAVA.rglob("*.java")):
        for index, line in enumerate(_read_java(path).splitlines(), start=1):
            if INGOT_HOT_RE.search(line):
                buckets["ingotHot"].append(_hit(path, index, line))
            if COOLER_RE.search(line):
                buckets["cooler"].append(_hit(path, index, line))
            if FREEZER_JAVA_RE.search(line):
                buckets["freezer"].append(_hit(path, index, line))
    return buckets


def classify_ingot_hot_hit(hit: dict[str, Any]) -> str:
    """Classify one Java hit. Unknown rows are conversion-class and fail closed."""
    path = hit["path"]
    text = hit["text"]
    if path.endswith("gregapi/data/OP.java") and 'create("ingotHot"' in text:
        return "prefix_definition"
    if path.endswith("gregapi/data/OP.java") and HEAT_DAMAGE_ASSIGN in text:
        return "heat_damage_assignment"
    if path.endswith("gregtech/loaders/a/Loader_Items.java") and "OP.ingotHot" in text:
        return "item_registration"
    if path.endswith("gregtech/loaders/c/Loader_Recipes_Handlers.java") and (
        "ingotHot.NOT" in text or "ingotHot)" in text or "ingotHot," in text
    ):
        return "recipe_exclusion"
    if any(
        token in text
        for token in (
            "ST.set",
            "setEntityItemStack",
            "replaceItem",
            "ingotHot ->",
            "OP.ingot,",
        )
    ):
        return "prefix_conversion"
    return "unexpected"


def _file_record(path: Path) -> dict[str, Any]:
    _require_file(path)
    return {
        "path": common.relative(path),
        "sha256": common.sha256_file(path),
    }


def _t13_freezer() -> dict[str, Any]:
    rows = common.t13_rows("recipe_maps", common.load_json(T13_RECIPE_MAPS))
    for row in rows:
        if row.get("normalized_row_key") == "gt.recipe.freezer":
            return row
    raise ValueError("T13 recipe_maps is missing gt.recipe.freezer")


def _t27_freezer() -> dict[str, Any]:
    document = common.load_json(T27_PORTFOLIO_MAPS)
    for row in document.get("records") or document.get("rows") or []:
        if row.get("canonical_id") == "gt.recipe.freezer":
            return row
    raise ValueError("T27 portfolio recipe_maps is missing gt.recipe.freezer")


def _item_ids(slots: Any) -> list[str]:
    if not isinstance(slots, list):
        return []
    return [str(slot.get("item") or "") for slot in slots if isinstance(slot, dict)]


def _display_names(slots: Any) -> list[str]:
    if not isinstance(slots, list):
        return []
    return [str(slot.get("displayName") or "") for slot in slots if isinstance(slot, dict)]


def classify_freezer_recipe(recipe: dict[str, Any]) -> str:
    inputs = _item_ids(recipe.get("inputs"))
    outputs = _item_ids(recipe.get("outputs"))
    in_hot = INGOT_HOT_ITEM in inputs
    out_hot = INGOT_HOT_ITEM in outputs
    out_ingot = ORDINARY_INGOT_ITEM in outputs
    if in_hot and out_ingot and not out_hot:
        return "prefix_conversion"
    if in_hot or out_hot:
        return "material_transform"
    return "unrelated"


def _scan_freezer_dump() -> dict[str, Any]:
    record = {
        "path": common.relative(FREEZER_DUMP),
        "present": FREEZER_DUMP.is_file(),
    }
    if not FREEZER_DUMP.is_file():
        record["note"] = (
            "Dump JSON is not in the working tree; freezer identity is "
            "pinned by the T13 recipe_maps row and Java RM.Freezer source."
        )
        record["material_transform_recipes"] = 0
        record["prefix_conversion_recipes"] = 0
        record["examples"] = []
        return record
    document = json.loads(FREEZER_DUMP.read_text(encoding="utf-8"))
    recipes = document.get("recipes") or []
    transforms: list[dict[str, Any]] = []
    conversions: list[dict[str, Any]] = []
    for recipe in recipes:
        if not isinstance(recipe, dict):
            continue
        kind = classify_freezer_recipe(recipe)
        if kind == "unrelated":
            continue
        row = {
            "inputs": _display_names(recipe.get("inputs")),
            "outputs": _display_names(recipe.get("outputs")),
        }
        if kind == "prefix_conversion":
            conversions.append(row)
        else:
            transforms.append(row)
    record["sha256"] = common.sha256_file(FREEZER_DUMP)
    record["recipe_count"] = len(recipes)
    record["material_transform_recipes"] = len(transforms)
    record["prefix_conversion_recipes"] = len(conversions)
    record["examples"] = transforms[:3]
    if conversions:
        raise ValueError(
            "fail-closed: freezer dump has ingotHot → ordinary ingot recipes: "
            + common.stable_json(conversions).strip()
        )
    return record


def _require_no_conversion(hits: list[dict[str, Any]]) -> list[dict[str, Any]]:
    classified: list[dict[str, Any]] = []
    unexpected: list[dict[str, Any]] = []
    for hit in hits:
        kind = classify_ingot_hot_hit(hit)
        row = dict(hit)
        row["classification"] = kind
        classified.append(row)
        if kind in CONVERSION_CLASSES:
            unexpected.append(row)
    if unexpected:
        raise ValueError(
            "fail-closed: unexpected ingotHot conversion-class hits: "
            + common.stable_json(unexpected).strip()
        )
    return classified


def _cc_auto_conversion() -> dict[str, Any]:
    rule_path = CC_PATHS["cooling_rule"]
    class_path = CC_PATHS["material_item_cooling"]
    maintain = _read_java(CC_PATHS["heat_maintenance_events"])
    maps = _read_java(CC_PATHS["mod_recipe_maps"])
    rule_present = rule_path.is_file()
    class_present = class_path.is_file()
    calls_cool = (
        "MaterialItemCooling.coolIfReady" in maintain
        or "ModRecipeMaps.COOLING.find" in maintain
    )
    leftover = bool(rule_present or class_present or calls_cool)
    if leftover and not (rule_present and class_present and calls_cool):
        raise ValueError(
            "partial hot-ingot retirement: cooling rule, MaterialItemCooling, "
            "and coolIfReady must be removed together"
        )
    retained = [
        "HotIngotProcessing.prepareOutputs",
        "HotIngotProcessing.initializeIfMissing",
        "MaterialContactHeat.damage",
        "ItemHeat.clearIfCooled",
        "MoldCastingRules.cool",
    ]
    if leftover:
        cooling_rule = common.load_json(rule_path)
        policy = cooling_rule.get("balance_policy") or {}
        if cooling_rule.get("target") != "cruciblecraft:cooling":
            raise ValueError("cooling rule target drifted")
        if policy.get("status") != "DESIGN_POLICY" or policy.get("open_item") != "O-36":
            raise ValueError("cooling rule is not the T10 DESIGN_POLICY O-36 leftover")
        return {
            "cooling_rule": {
                "balance_policy": "DESIGN_POLICY",
                "gt6_equivalence": policy.get("gt6_equivalence"),
                "open_item": "O-36",
                "path": common.relative(rule_path),
                "sha256": common.sha256_file(rule_path),
                "target": "cruciblecraft:cooling",
            },
            "present": True,
            "retained_paths": retained,
            "runtime_path": (
                "HeatMaintenanceEvents.maintain -> "
                "MaterialItemCooling.coolIfReady -> "
                "ModRecipeMaps.COOLING.find"
            ),
            "status": "present_pending_retirement",
        }
    if "MaterialItemCooling" in maintain:
        raise ValueError("HeatMaintenanceEvents still mentions MaterialItemCooling")
    if "COOLING.find" in maintain:
        raise ValueError("HeatMaintenanceEvents still queries COOLING")
    if "public static final RecipeMap COOLING" not in maps:
        raise ValueError("ModRecipeMaps.COOLING must remain as an empty leftover map")
    return {
        "cooling_rule": {
            "path": common.relative(rule_path),
            "present": False,
        },
        "present": False,
        "retained_paths": retained,
        "runtime_path": (
            "HeatMaintenanceEvents.maintain -> "
            "HotIngotProcessing.initializeIfMissing + ItemHeat.clearIfCooled"
        ),
        "status": "retired",
    }


def _cc_inputs() -> dict[str, Any]:
    records: dict[str, Any] = {}
    for key, path in sorted(CC_PATHS.items()):
        if path.is_file():
            records[key] = _file_record(path)
        else:
            records[key] = {
                "path": common.relative(path),
                "present": False,
            }
    return records


def build() -> dict[str, Any]:
    _require_file(OP_JAVA)
    _require_file(UT_JAVA)
    _require_file(RM_JAVA)
    for key in REQUIRED_CC_KEYS:
        _require_file(CC_PATHS[key])

    op_text = _read_java(OP_JAVA)
    if HEAT_DAMAGE_ASSIGN not in op_text:
        raise ValueError(f"{common.relative(OP_JAVA)} is missing {HEAT_DAMAGE_ASSIGN}")
    definition_line = next(
        (line for line in op_text.splitlines() if 'create("ingotHot"' in line),
        "",
    )
    if not definition_line:
        raise ValueError(f"{common.relative(OP_JAVA)} is missing ingotHot prefix create()")
    if "UNIFICATABLE" in definition_line:
        raise ValueError("ingotHot prefix unexpectedly carries UNIFICATABLE")

    scanned = _scan_java_tree()
    ingot_hot_hits = _require_no_conversion(scanned["ingotHot"])
    if scanned["cooler"]:
        raise ValueError(
            "fail-closed: GT6 Cooler recipe map hits: "
            + common.stable_json(scanned["cooler"]).strip()
        )

    freezer_field = _hits(RM_JAVA, FREEZER_FIELD_RE)
    if not freezer_field:
        raise ValueError("RM.Freezer field is missing")
    freezer_java_hits = scanned["freezer"]
    freezer_ingot_hot = [
        hit for hit in freezer_java_hits if INGOT_HOT_RE.search(hit["text"])
    ]
    if freezer_ingot_hot:
        raise ValueError(
            "fail-closed: RM.Freezer source mentions ingotHot: "
            + common.stable_json(freezer_ingot_hot).strip()
        )

    dump = _scan_freezer_dump()

    t13_freezer = _t13_freezer()
    t27_freezer = _t27_freezer()
    if t27_freezer.get("disposition") != "post_1_0":
        raise ValueError(
            "gt.recipe.freezer disposition is "
            f"{t27_freezer.get('disposition')!r}; T28 requires post_1_0"
        )
    if t13_freezer.get("classification") != "deferred_with_reason":
        raise ValueError("T13 freezer classification drifted")

    ut_hits = _hits(UT_JAVA, re.compile(HEAT_DAMAGE_READER))
    if not ut_hits:
        raise ValueError("UT.Entities.getHeatDamageFromItem is missing")

    cc_auto_conversion = _cc_auto_conversion()
    cc_inputs = _cc_inputs()

    heat_damage_hit = next(
        hit for hit in ingot_hot_hits if hit["classification"] == "heat_damage_assignment"
    )
    definition_hit = next(
        hit for hit in ingot_hot_hits if hit["classification"] == "prefix_definition"
    )

    files_hit = sorted({hit["path"] for hit in ingot_hot_hits})
    by_class: dict[str, int] = {}
    for hit in ingot_hot_hits:
        by_class[hit["classification"]] = by_class.get(hit["classification"], 0) + 1

    return {
        "cc_auto_conversion": cc_auto_conversion,
        "conclusion": {
            "gt6_generic_cooler_map": False,
            "gt6_passive_ingotHot_to_ingot": False,
            "product_decision": "strict_no_conversion",
            "replacement_is_absence_of_conversion": True,
        },
        "conversion_search": {
            "by_classification": by_class,
            "cooler_map_hits": 0,
            "files": files_hit,
            "hits": ingot_hot_hits,
            "hit_count": len(ingot_hot_hits),
            "negative_conclusion": (
                "Fixed-revision GT6 Java has no item-entity tick, player-inventory "
                "tick, unification, or recipe-map conversion from ingotHot to ingot. "
                "The only ingotHot recipe-map mentions exclude the prefix."
            ),
            "symbols": [
                "ingotHot",
                "OP.ingotHot",
                "RM.Cooler",
                "gt.recipe.cooler",
                "RM.Freezer",
                "gt.recipe.freezer",
            ],
        },
        "currentness": {
            "owned_inputs": {
                common.relative(BUILDER): common.sha256_file(BUILDER),
            }
        },
        "freezer": {
            "canonical_id": "gt.recipe.freezer",
            "disposition": "post_1_0",
            "dump": dump,
            "in_card_scope": False,
            "java_hits": len(freezer_java_hits),
            "java_ingotHot_hits": 0,
            "reason": (
                "Freezer is a material-transform map (for example iron family to "
                "FrozenIron), not a generic ingotHot → ingot cooler. T28 must not "
                "borrow it as a replacement."
            ),
            "t13_classification": t13_freezer.get("classification"),
            "t13_recipe_count": t13_freezer.get("recipe_count"),
            "t27_owner": t27_freezer.get("owner"),
        },
        "generated_by": "python tools/build_t28_hot_ingot_source_evidence.py --write",
        "heat_damage": {
            "application": [
                {
                    "note": "Player inventory scan applies prefix+material heat damage; it does not replace the stack.",
                    "path": "gt6_code/gregtech6/src/main/java/gregapi/GT_API_Proxy.java",
                    "symbol": "UT.Entities.getHeatDamageFromItem",
                }
            ],
            "prefix_assignment": heat_damage_hit,
            "reader": ut_hits[0],
            "value": 3.0,
        },
        "inputs": cc_inputs,
        "prefix_definition": definition_hit,
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "source_tree": {
            "op_java": _file_record(OP_JAVA),
            "rm_java": _file_record(RM_JAVA),
            "root": common.relative(GT6_JAVA),
            "ut_java": _file_record(UT_JAVA),
        },
        "status": "T28_HOT_INGOT_SOURCE_EVIDENCE",
    }


def write() -> dict[str, Any]:
    document = build()
    common.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    if not OUTPUT.is_file():
        return [f"missing generated file: {common.relative(OUTPUT)}"]
    expected = common.stable_json(build())
    actual = OUTPUT.read_text(encoding="utf-8")
    if actual != expected:
        return [f"{common.relative(OUTPUT)} is stale"]
    return []


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if bool(args.write) == bool(args.check):
        parser.error("choose exactly one of --write, --check")
    try:
        if args.write:
            document = write()
            print(
                f"wrote {common.relative(OUTPUT)} "
                f"status={document['status']} "
                f"passive_conversion={document['conclusion']['gt6_passive_ingotHot_to_ingot']}"
            )
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{common.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        print(str(exc), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
