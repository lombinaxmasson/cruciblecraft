#!/usr/bin/env python3
"""Config overlay — map the few GT6 fields that config can override at runtime.

Only these are runtime-overridable (GT_API + Stacksizes.cfg):

  Material.oreMultiplier            ← Materials.cfg  MultiplierOre_<default>
  Material.toolQuality              ← Materials.cfg  ToolQuality_<default>
  Material.toolSpeed                ← Materials.cfg  ToolSpeed_<default>      (toolTypes > 0)
  Material.toolDurability           ← Materials.cfg  ToolDurability_<default> (toolTypes > 0)
  Material.handleMaterial           ← Materials.cfg  ToolHandle_<default>     (toolTypes > 0)
  Material.oreProcessingMultiplier  ← OreProcessing.cfg  <Name>_<default=1>
  Prefix.configStackSize            ← Stacksizes.cfg  <prefix>_<default>
       (setConfigStacksize also writes Prefix.defaultStackSize)

Defaults are recovered from key suffixes (ConfigsGT setUseDefaultInNames /
Forge property name embedding). Status:

  overridden         — config live ≠ default (key suffix)
  tunable_unchanged  — mapped and live == default
  dump_mismatch      — oredict live ≠ config live
  missing_config     — oredict subject expected a mapped prop, cfg absent
  orphan_config      — cfg prop with no matching oredict subject
  skipped_invalid    — INVALID_MATERIAL / PREFIX_UNUSED (GT skips apply)
"""
from __future__ import annotations

import json
import re
import sys
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
DUMP = ROOT / "gt6_dump" / "gt6_recipe_dump"
OD = DUMP / "oredict"
CD = DUMP / "config_digest"
TOOLS = ROOT / "tools"

OUT_OVERLAY = TOOLS / "gt6_config_overlay.json"
OUT_REPORT = TOOLS / "gt6_config_overlay_report.json"

MATERIAL_FIELDS: tuple[tuple[str, str, str, bool], ...] = (
    # (oredict_field, config_prop_prefix, value_kind, require_tools)
    ("oreMultiplier", "MultiplierOre_", "int", False),
    ("toolQuality", "ToolQuality_", "int", False),
    ("toolSpeed", "ToolSpeed_", "float", True),
    ("toolDurability", "ToolDurability_", "int", True),
    ("handleMaterial", "ToolHandle_", "str", True),
)

STACK_RE = re.compile(r"^[SDI]:([^=]+)=(.+)$", re.MULTILINE)


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def dump_json(path: Path, obj: Any) -> None:
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8", newline="\n")


def has_tag(tags: Any, needle: str) -> bool:
    if not isinstance(tags, list):
        return False
    return needle in tags


def parse_default(prop_key: str, prefix: str, kind: str) -> Any:
    if not prop_key.startswith(prefix):
        return None
    raw = prop_key[len(prefix) :]
    if kind == "int":
        return int(raw)
    if kind == "float":
        return float(raw)
    return raw


def coerce(value: Any, kind: str) -> Any:
    if kind == "int":
        return int(value)
    if kind == "float":
        return float(value)
    return str(value)


def values_equal(a: Any, b: Any, kind: str) -> bool:
    if a is None or b is None:
        return a is b
    if kind == "float":
        # Forge DOUBLE props can round-trip with binary noise (e.g. 1.7 → 1.700000047…)
        return abs(float(a) - float(b)) < 1e-5
    if kind == "int":
        return int(a) == int(b)
    return str(a) == str(b)


def classify_pair(default: Any, config_live: Any, dump_live: Any, kind: str) -> str:
    if not values_equal(config_live, dump_live, kind):
        return "dump_mismatch"
    if not values_equal(default, config_live, kind):
        return "overridden"
    return "tunable_unchanged"


def find_prop_for_material(
    props: dict[str, Any] | None,
    prefix: str,
    kind: str,
    dump_live: Any,
) -> tuple[str, Any] | None:
    """Pick the Materials.cfg prop for one material.

    Forge lowercases categories, so case-colliding names (Moonstone/MoonStone)
    share a category but keep distinct keys via setUseDefaultInNames
    (MultiplierOre_2 vs MultiplierOre_1). Match by dump live / key default.
    """
    if not props:
        return None
    candidates: list[tuple[str, Any, Any]] = []
    for key, val in props.items():
        if not key.startswith(prefix):
            continue
        default = parse_default(key, prefix, kind)
        if default is None:
            continue
        candidates.append((key, val, default))
    if not candidates:
        return None
    if dump_live is not None:
        exact = [
            (k, v)
            for k, v, d in candidates
            if values_equal(coerce(v, kind), dump_live, kind)
            and values_equal(d, dump_live, kind)
        ]
        if len(exact) == 1:
            return exact[0]
        if len(exact) > 1:
            return exact[0]
        by_live = [
            (k, v)
            for k, v, _d in candidates
            if values_equal(coerce(v, kind), dump_live, kind)
        ]
        if len(by_live) == 1:
            return by_live[0]
        if len(by_live) > 1:
            return by_live[0]
    if len(candidates) == 1:
        k, v, _d = candidates[0]
        return k, v
    return None


def parse_stacksizes(path: Path) -> dict[str, dict[str, Any]]:
    """Return prefixName -> {key, default, live}."""
    text = path.read_text(encoding="utf-8")
    out: dict[str, dict[str, Any]] = {}
    in_stack = False
    for line in text.splitlines():
        s = line.strip()
        if s.startswith("stacksizes"):
            in_stack = True
            continue
        if in_stack and s == "}":
            break
        if not in_stack:
            continue
        m = STACK_RE.match(s)
        if not m:
            continue
        full, live_s = m.group(1), m.group(2)
        if "_" not in full:
            continue
        name, default_s = full.rsplit("_", 1)
        out[name] = {
            "key": full,
            "default": int(default_s),
            "live": int(live_s.strip()),
        }
    return out


def resolve_stacksizes_path() -> Path:
    local = CD / "stacksizes.cfg"
    if local.is_file():
        return local
    files = load(CD / "cfg_files.json")
    for entry in files:
        rel = str(entry.get("relative") or "").replace("\\", "/").lower()
        if rel.endswith("stacksizes.cfg"):
            p = Path(entry["path"])
            if p.is_file():
                return p
    raise FileNotFoundError("stacksizes.cfg not found under config_digest or cfg_files.json")


def material_skip_reason(m: dict[str, Any]) -> str | None:
    """Why GT effectively has no Materials/OreProcessing overlay for this row."""
    if has_tag(m.get("tags"), "PROPERTIES.INVALID_MATERIAL"):
        return "INVALID_MATERIAL — GT_API skips config apply"
    mid = m.get("id")
    if not isinstance(mid, int) or mid < 0:
        return "no stable MATERIAL_ARRAY id — config category usually absent"
    return None


def prefix_expected(p: dict[str, Any]) -> bool:
    return not has_tag(p.get("tags"), "PREFIX.PREFIX_UNUSED")


def build_material_entries(
    materials: list[dict[str, Any]],
    mat_cats: dict[str, dict[str, Any]],
    ore_props: dict[str, Any],
) -> tuple[list[dict[str, Any]], Counter]:
    entries: list[dict[str, Any]] = []
    counts: Counter = Counter()
    seen_mat_cats: set[str] = set()
    seen_ore_keys: set[str] = set()

    for m in materials:
        name = m.get("nameInternal") or ""
        mid = m.get("id")
        skip = material_skip_reason(m)
        cat_key = name.lower()
        props = mat_cats.get(cat_key)
        tool_types = int(m.get("toolTypes") or 0)

        if skip is not None:
            for field, prop_prefix, kind, require_tools in MATERIAL_FIELDS:
                if require_tools and tool_types <= 0:
                    continue
                entries.append({
                    "object": "Material",
                    "nameInternal": name,
                    "id": mid,
                    "field": field,
                    "status": "skipped_invalid",
                    "configFile": "Materials.cfg",
                    "configCategory": cat_key,
                    "configKey": None,
                    "default": None,
                    "configLive": None,
                    "dumpLive": m.get(field),
                    "note": skip,
                })
                counts["skipped_invalid"] += 1
            entries.append({
                "object": "Material",
                "nameInternal": name,
                "id": mid,
                "field": "oreProcessingMultiplier",
                "status": "skipped_invalid",
                "configFile": "OreProcessing.cfg",
                "configCategory": "oreprocessingoutputmultiplier",
                "configKey": None,
                "default": 1,
                "configLive": None,
                "dumpLive": m.get("oreProcessingMultiplier"),
                "note": skip,
            })
            counts["skipped_invalid"] += 1
            continue

        if props is not None:
            seen_mat_cats.add(cat_key)

        for field, prop_prefix, kind, require_tools in MATERIAL_FIELDS:
            if require_tools and tool_types <= 0:
                continue
            dump_live = m.get(field)
            hit = find_prop_for_material(props, prop_prefix, kind, dump_live)
            if hit is None:
                entries.append({
                    "object": "Material",
                    "nameInternal": name,
                    "id": mid,
                    "field": field,
                    "status": "missing_config",
                    "configFile": "Materials.cfg",
                    "configCategory": cat_key,
                    "configKey": None,
                    "default": None,
                    "configLive": None,
                    "dumpLive": dump_live,
                })
                counts["missing_config"] += 1
                continue
            key, raw_live = hit
            default = parse_default(key, prop_prefix, kind)
            config_live = coerce(raw_live, kind)
            status = classify_pair(default, config_live, dump_live, kind)
            entries.append({
                "object": "Material",
                "nameInternal": name,
                "id": mid,
                "field": field,
                "status": status,
                "configFile": "Materials.cfg",
                "configCategory": cat_key,
                "configKey": key,
                "default": default,
                "configLive": config_live,
                "dumpLive": dump_live if kind != "str" else str(dump_live) if dump_live is not None else None,
            })
            counts[status] += 1

        # OreProcessing — default always 1 in GT_API
        ore_key = f"{name}_1"
        dump_live = m.get("oreProcessingMultiplier")
        if ore_key in ore_props:
            seen_ore_keys.add(ore_key)
            config_live = int(ore_props[ore_key])
            default = 1
            # Prefer suffix if present (should always be 1)
            if "_" in ore_key:
                try:
                    default = int(ore_key.rsplit("_", 1)[1])
                except ValueError:
                    default = 1
            status = classify_pair(default, config_live, dump_live, "int")
            entries.append({
                "object": "Material",
                "nameInternal": name,
                "id": mid,
                "field": "oreProcessingMultiplier",
                "status": status,
                "configFile": "OreProcessing.cfg",
                "configCategory": "oreprocessingoutputmultiplier",
                "configKey": ore_key,
                "default": default,
                "configLive": config_live,
                "dumpLive": dump_live,
            })
            counts[status] += 1
        else:
            # try any key matching Name_*
            alt = None
            for k in ore_props:
                if k.rsplit("_", 1)[0] == name:
                    alt = k
                    break
            if alt is None:
                entries.append({
                    "object": "Material",
                    "nameInternal": name,
                    "id": mid,
                    "field": "oreProcessingMultiplier",
                    "status": "missing_config",
                    "configFile": "OreProcessing.cfg",
                    "configCategory": "oreprocessingoutputmultiplier",
                    "configKey": None,
                    "default": 1,
                    "configLive": None,
                    "dumpLive": dump_live,
                })
                counts["missing_config"] += 1
            else:
                seen_ore_keys.add(alt)
                default = int(alt.rsplit("_", 1)[1])
                config_live = int(ore_props[alt])
                status = classify_pair(default, config_live, dump_live, "int")
                entries.append({
                    "object": "Material",
                    "nameInternal": name,
                    "id": mid,
                    "field": "oreProcessingMultiplier",
                    "status": status,
                    "configFile": "OreProcessing.cfg",
                    "configCategory": "oreprocessingoutputmultiplier",
                    "configKey": alt,
                    "default": default,
                    "configLive": config_live,
                    "dumpLive": dump_live,
                })
                counts[status] += 1

    # Orphan MATERIAL categories / unused keys inside shared categories
    used_mat_keys: set[tuple[str, str]] = {
        (e["configCategory"], e["configKey"])
        for e in entries
        if e.get("configFile") == "Materials.cfg" and e.get("configKey")
    }
    for cat, props in mat_cats.items():
        for field, prop_prefix, kind, _require_tools in MATERIAL_FIELDS:
            for key, raw_live in props.items():
                if not key.startswith(prop_prefix):
                    continue
                if (cat, key) in used_mat_keys:
                    continue
                if cat in seen_mat_cats:
                    # Shared case-collision category: leftover key still maps to a
                    # sibling material name; only flag true orphans below.
                    continue
                entries.append({
                    "object": "Material",
                    "nameInternal": None,
                    "id": None,
                    "field": field,
                    "status": "orphan_config",
                    "configFile": "Materials.cfg",
                    "configCategory": cat,
                    "configKey": key,
                    "default": parse_default(key, prop_prefix, kind),
                    "configLive": coerce(raw_live, kind),
                    "dumpLive": None,
                    "note": "config category with no matching oredict material",
                })
                counts["orphan_config"] += 1
    # Keys in shared categories never matched to any material dump field
    for cat, props in mat_cats.items():
        if cat not in seen_mat_cats:
            continue
        for field, prop_prefix, kind, _require_tools in MATERIAL_FIELDS:
            for key, raw_live in props.items():
                if not key.startswith(prop_prefix):
                    continue
                if (cat, key) in used_mat_keys:
                    continue
                entries.append({
                    "object": "Material",
                    "nameInternal": None,
                    "id": None,
                    "field": field,
                    "status": "orphan_config",
                    "configFile": "Materials.cfg",
                    "configCategory": cat,
                    "configKey": key,
                    "default": parse_default(key, prop_prefix, kind),
                    "configLive": coerce(raw_live, kind),
                    "dumpLive": None,
                    "note": "Materials.cfg key unused after matching case-colliding materials",
                })
                counts["orphan_config"] += 1

    for key, raw_live in ore_props.items():
        if key in seen_ore_keys:
            continue
        name = key.rsplit("_", 1)[0]
        # may still belong to a material we processed under different default suffix
        if any(e.get("configKey") == key for e in entries):
            continue
        # if material exists under that name we already handled; otherwise orphan
        if any((e.get("nameInternal") == name and e.get("field") == "oreProcessingMultiplier") for e in entries):
            continue
        try:
            default = int(key.rsplit("_", 1)[1])
        except ValueError:
            default = None
        entries.append({
            "object": "Material",
            "nameInternal": name,
            "id": None,
            "field": "oreProcessingMultiplier",
            "status": "orphan_config",
            "configFile": "OreProcessing.cfg",
            "configCategory": "oreprocessingoutputmultiplier",
            "configKey": key,
            "default": default,
            "configLive": int(raw_live),
            "dumpLive": None,
            "note": "oreprocessing key with no matching expected material",
        })
        counts["orphan_config"] += 1

    return entries, counts


def build_prefix_entries(
    prefixes: list[dict[str, Any]],
    stack: dict[str, dict[str, Any]],
) -> tuple[list[dict[str, Any]], Counter]:
    entries: list[dict[str, Any]] = []
    counts: Counter = Counter()
    seen: set[str] = set()

    for p in prefixes:
        name = p.get("nameInternal") or ""
        dump_cfg = p.get("configStackSize")
        dump_def = p.get("defaultStackSize")
        if not prefix_expected(p):
            entries.append({
                "object": "Prefix",
                "nameInternal": name,
                "field": "configStackSize",
                "status": "skipped_invalid",
                "configFile": "Stacksizes.cfg",
                "configCategory": "stacksizes",
                "configKey": None,
                "default": None,
                "configLive": None,
                "dumpLive": dump_cfg,
                "dumpDefaultStackSize": dump_def,
                "note": "PREFIX_UNUSED — GT_API skips Stacksizes apply",
            })
            counts["skipped_invalid"] += 1
            continue

        hit = stack.get(name)
        if hit is None:
            entries.append({
                "object": "Prefix",
                "nameInternal": name,
                "field": "configStackSize",
                "status": "missing_config",
                "configFile": "Stacksizes.cfg",
                "configCategory": "stacksizes",
                "configKey": None,
                "default": None,
                "configLive": None,
                "dumpLive": dump_cfg,
                "dumpDefaultStackSize": dump_def,
            })
            counts["missing_config"] += 1
            continue

        seen.add(name)
        default = hit["default"]
        config_live = hit["live"]
        status = classify_pair(default, config_live, dump_cfg, "int")
        # After setConfigStacksize, defaultStackSize mirrors configStackSize.
        note = None
        if dump_def is not None and int(dump_def) != int(config_live):
            status = "dump_mismatch"
            note = "defaultStackSize should equal configStackSize after setConfigStacksize"
        entries.append({
            "object": "Prefix",
            "nameInternal": name,
            "field": "configStackSize",
            "status": status,
            "configFile": "Stacksizes.cfg",
            "configCategory": "stacksizes",
            "configKey": hit["key"],
            "default": default,
            "configLive": config_live,
            "dumpLive": dump_cfg,
            "dumpDefaultStackSize": dump_def,
            **({"note": note} if note else {}),
        })
        counts[status] += 1

    for name, hit in stack.items():
        if name in seen:
            continue
        entries.append({
            "object": "Prefix",
            "nameInternal": name,
            "field": "configStackSize",
            "status": "orphan_config",
            "configFile": "Stacksizes.cfg",
            "configCategory": "stacksizes",
            "configKey": hit["key"],
            "default": hit["default"],
            "configLive": hit["live"],
            "dumpLive": None,
            "note": "stacksizes key with no matching oredict prefix",
        })
        counts["orphan_config"] += 1

    return entries, counts


def main() -> int:
    materials = load(OD / "materials.json")
    prefixes = load(OD / "prefixes.json")
    configs = load(CD / "configs_gt.json")

    mat_cats = configs["MATERIAL"]["categories"]
    ore_props = configs["OREPROCESSING"]["categories"]["oreprocessingoutputmultiplier"]
    stack_path = resolve_stacksizes_path()
    stack = parse_stacksizes(stack_path)

    mat_entries, mat_counts = build_material_entries(materials, mat_cats, ore_props)
    pref_entries, pref_counts = build_prefix_entries(prefixes, stack)

    all_entries = mat_entries + pref_entries
    total = Counter()
    total.update(mat_counts)
    total.update(pref_counts)

    by_field: dict[str, Counter] = {}
    for e in all_entries:
        key = f"{e['object']}.{e['field']}"
        by_field.setdefault(key, Counter())[e["status"]] += 1

    overridden = [e for e in all_entries if e["status"] == "overridden"]
    mismatches = [e for e in all_entries if e["status"] == "dump_mismatch"]
    missing = [e for e in all_entries if e["status"] == "missing_config"]
    orphans = [e for e in all_entries if e["status"] == "orphan_config"]

    overlay = {
        "schema": "gt6_config_overlay/v1",
        "scope": {
            "note": "Only fields GT6 actually overrides from config at runtime. Melting/components/tags/amount/texture sets are source-fixed.",
            "mappings": [
                {"object": "Material", "field": "oreMultiplier", "configFile": "Materials.cfg", "keyPattern": "MultiplierOre_<default>"},
                {"object": "Material", "field": "toolQuality", "configFile": "Materials.cfg", "keyPattern": "ToolQuality_<default>"},
                {"object": "Material", "field": "toolSpeed", "configFile": "Materials.cfg", "keyPattern": "ToolSpeed_<default>", "when": "toolTypes > 0"},
                {"object": "Material", "field": "toolDurability", "configFile": "Materials.cfg", "keyPattern": "ToolDurability_<default>", "when": "toolTypes > 0"},
                {"object": "Material", "field": "handleMaterial", "configFile": "Materials.cfg", "keyPattern": "ToolHandle_<default>", "when": "toolTypes > 0"},
                {"object": "Material", "field": "oreProcessingMultiplier", "configFile": "OreProcessing.cfg", "keyPattern": "<nameInternal>_<default=1>", "category": "oreprocessingoutputmultiplier"},
                {"object": "Prefix", "field": "configStackSize", "configFile": "Stacksizes.cfg", "keyPattern": "<prefix>_<default>", "alsoAffects": "defaultStackSize"},
            ],
        },
        "inputs": {
            "materials": str(OD / "materials.json"),
            "prefixes": str(OD / "prefixes.json"),
            "configs_gt": str(CD / "configs_gt.json"),
            "stacksizes": str(stack_path),
        },
        "counts": {
            "entries": len(all_entries),
            "by_status": dict(total),
            "by_field": {k: dict(v) for k, v in sorted(by_field.items())},
            "materials_in_dump": len(materials),
            "prefixes_in_dump": len(prefixes),
            "material_cfg_categories": len(mat_cats),
            "oreprocessing_keys": len(ore_props),
            "stacksizes_keys": len(stack),
        },
        "entries": all_entries,
    }

    report = {
        "schema": "gt6_config_overlay_report/v1",
        "summary": {
            "overridden": len(overridden),
            "tunable_unchanged": total.get("tunable_unchanged", 0),
            "dump_mismatch": len(mismatches),
            "missing_config": len(missing),
            "orphan_config": len(orphans),
            "skipped_invalid": total.get("skipped_invalid", 0),
            "entries": len(all_entries),
        },
        "by_field": {k: dict(v) for k, v in sorted(by_field.items())},
        "overridden": overridden,
        "dump_mismatch": mismatches,
        "missing_config_sample": missing[:50],
        "missing_config_count": len(missing),
        "orphan_config_sample": orphans[:50],
        "orphan_config_count": len(orphans),
        "interpretation": {
            "overridden": "config live ≠ default embedded in key → player/pack changed this value",
            "tunable_unchanged": "mapped and equal → adjustable but currently at source default",
            "dump_mismatch": "oredict live ≠ config live → unexpected post-config mutation or dump bug",
            "missing_config": "expected apply target but no cfg prop (should be rare for valid materials/prefixes)",
            "orphan_config": "cfg prop with no matching dump subject",
            "skipped_invalid": "INVALID_MATERIAL / PREFIX_UNUSED — GT skips apply; dump may still show source fields",
            "source_fixed": "meltingPoint, components, tags, amount, texture sets, etc. are NOT config-overridable",
        },
    }

    dump_json(OUT_OVERLAY, overlay)
    dump_json(OUT_REPORT, report)

    print(f"wrote {OUT_OVERLAY}")
    print(f"wrote {OUT_REPORT}")
    print("summary:", json.dumps(report["summary"], ensure_ascii=False))
    if overridden:
        print(f"overridden ({len(overridden)}):")
        for e in overridden[:20]:
            print(f"  {e['object']}.{e['field']} {e.get('nameInternal')} default={e['default']} live={e['configLive']}")
    if mismatches:
        print(f"dump_mismatch ({len(mismatches)}):")
        for e in mismatches[:20]:
            print(f"  {e['object']}.{e['field']} {e.get('nameInternal')} cfg={e['configLive']} dump={e['dumpLive']}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
