#!/usr/bin/env python3
"""Classify every raw GT6 recipe row against the current CC runtime recipes.

Each GT6 dump row is translated into CC identities with the same translator
the recipe waves use (``tools/recipe_bulk/dialects/gt6.compile_row`` +
``tools/recipe_bulk/emit``). The translated row is then compared, in CC
logical-identity space, with every CC runtime recipe row and every expanded
``material_rule`` on the CC RecipeMaps that serve that GT6 map.

The translator is calibrated in the same pass: hash-proven CC rows must
translate back to their own GT6 row. The agreement rate is recorded so the
classification can be trusted (or not) by the number, not by assertion.

``--write`` needs the local ``gt6_dump``. ``--check`` does not: it compares a
digest of the CC-side projection and the translator sources with the pin.
"""
from __future__ import annotations

import argparse
import gzip
import hashlib
import importlib.util
import json
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any, Iterable


ROOT = Path(__file__).resolve().parents[4]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))
if str(ROOT / "tools") not in sys.path:
    sys.path.insert(0, str(ROOT / "tools"))
WAVE_ROOT = ROOT / "tools" / "waves" / "portfolio" / "gt6-full-coverage-reassessment"
OUTPUT_PATH = WAVE_ROOT / "semantic_coverage.json"
SCOPE_PATH = WAVE_ROOT / "scope.json"
RECONCILIATION_PATH = WAVE_ROOT / "build_reconciliation.py"
COMPARATOR_PATH = ROOT / "tools" / "compare_gt6_recipes.py"
DUMP_MAPS = ROOT / "gt6_dump" / "gt6_recipe_dump" / "maps"
BUILDER_PATH = Path(__file__).resolve()
TRANSLATOR_SOURCES = (
    ROOT / "tools" / "recipe_bulk" / "dialects" / "gt6.py",
    ROOT / "tools" / "recipe_bulk" / "ordinary_source.py",
    ROOT / "tools" / "recipe_bulk" / "emit.py",
    ROOT / "tools" / "recipe_bulk" / "matrix.py",
    ROOT / "tools" / "gt6_resolve.py",
    COMPARATOR_PATH,
)

PREFIX_MATERIAL = "cruciblecraft:prefix_material"
TECHNOLOGICAL_ITEM = "gregtech:gt.multiitem.technological"
GT_RECIPE = "cruciblecraft:gt_recipe"
COMPACT_FAMILY = "cruciblecraft:compact_gt_recipe_family"

# Precedence order: the first class that applies wins.
CLASSES = (
    "legacy_exclusion_pending",
    "source_exact",
    "display_only",
    "translated_exact",
    "translated_io_only",
    "translated_item_io",
    "translatable_missing",
    "missing_material_form",
    "missing_fluid",
    "missing_object",
)


def stable_json(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def sha256_text(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def sha256_file(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def _load_module(name: str, path: Path):
    spec = importlib.util.spec_from_file_location(name, path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load {path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[name] = module
    spec.loader.exec_module(module)
    return module


def load_reconciliation():
    return _load_module("cruciblecraft_gt6_coverage_reconciliation", RECONCILIATION_PATH)


def load_comparator():
    return _load_module("cruciblecraft_gt6_recipe_comparator", COMPARATOR_PATH)


# --------------------------------------------------------------------------
# CC logical identity
# --------------------------------------------------------------------------


def logical_item(operand: Any) -> str | None:
    """Collapse slash ids and prefix-item components onto one identity."""
    if not isinstance(operand, dict):
        return None
    runtime = operand.get("item") or operand.get("id") or operand.get("items")
    if not runtime and operand.get("tag"):
        return "tag:" + str(operand["tag"])
    if not isinstance(runtime, str):
        return None
    components = operand.get("components") or {}
    material = components.get(PREFIX_MATERIAL)
    if material and runtime.startswith("cruciblecraft:"):
        return f"cruciblecraft:{material}/{runtime.split(':', 1)[1]}"
    extra = {key: value for key, value in components.items() if key != PREFIX_MATERIAL}
    return runtime + (stable_json(extra) if extra else "")


def _merge(entries: Iterable[tuple[Any, int]]) -> tuple[tuple[Any, int], ...]:
    counts: Counter[Any] = Counter()
    for key, count in entries:
        counts[key] += count
    return tuple(sorted(counts.items(), key=lambda item: str(item[0])))


def signature(ins, outs, fluid_ins, fluid_outs) -> tuple:
    return (_merge(ins), _merge(outs), _merge(fluid_ins), _merge(fluid_outs))


def runtime_relation_signature(relation: dict[str, Any]) -> tuple[tuple, tuple[int, int]]:
    counts = relation.get("item_input_counts") or []
    ins = [
        (logical_item(op), int(counts[index]) if index < len(counts) else 1)
        for index, op in enumerate(relation.get("item_inputs") or [])
    ]
    outs = [
        (logical_item(op), int(op.get("count") or 1))
        for op in relation.get("item_outputs") or []
        if isinstance(op, dict)
    ]
    fluid_ins = [(f.get("id"), int(f.get("amount") or 0)) for f in relation.get("fluid_inputs") or []]
    fluid_outs = [(f.get("id"), int(f.get("amount") or 0)) for f in relation.get("fluid_outputs") or []]
    numbers = (int(relation.get("duration") or 0), int(relation.get("eut") or 0))
    return signature(ins, outs, fluid_ins, fluid_outs), numbers


def _rule_identity(resource: Any, materials: dict[str, dict[str, Any]]) -> str:
    identity = str(resource.id)
    head, _, tail = identity.partition(":")
    if resource.kind == "fluid":
        return identity if head in ("minecraft", "cruciblecraft") else f"cruciblecraft:{identity}"
    if head in materials:
        override = (materials[head].get("form_items") or {}).get(tail)
        return str(override) if override else f"cruciblecraft:{head}/{tail}"
    return identity


def rule_signature(recipe: Any, materials: dict[str, dict[str, Any]]) -> tuple[tuple, tuple[int, int]]:
    ins = [(_rule_identity(r, materials), r.count) for r in recipe.inputs if r.kind == "item"]
    outs = [(_rule_identity(r, materials), r.count) for r in recipe.outputs if r.kind == "item"]
    fluid_ins = [(_rule_identity(r, materials), r.count) for r in recipe.inputs if r.kind == "fluid"]
    fluid_outs = [(_rule_identity(r, materials), r.count) for r in recipe.outputs if r.kind == "fluid"]
    return signature(ins, outs, fluid_ins, fluid_outs), (int(recipe.duration), int(recipe.eut))


# --------------------------------------------------------------------------
# CC projection
# --------------------------------------------------------------------------


def _runtime_documents(rec) -> Iterable[tuple[str, dict[str, Any], dict[str, Any]]]:
    from tools.recipe_bulk.matrix import authored_relations

    for path in rec.runtime_recipe_files():
        try:
            document = load_json(path)
        except (OSError, json.JSONDecodeError):
            continue
        if not isinstance(document, dict):
            continue
        kind = document.get("type")
        if kind == GT_RECIPE:
            target, relations = document.get("map"), [document]
        elif kind == COMPACT_FAMILY:
            target, relations = document.get("target_map"), authored_relations(document)
        else:
            continue
        if not isinstance(target, str) or ":" not in target:
            continue
        cc_map = target.split(":", 1)[1]
        for relation in relations:
            yield cc_map, relation, document


def build_cc_projection() -> dict[str, Any]:
    rec = load_reconciliation()
    comparator = load_comparator()
    comparator.clear_process_caches()
    materials = comparator.cached_cc_materials()

    index: dict[str, dict[tuple, set[tuple[int, int]]]] = defaultdict(lambda: defaultdict(set))
    runtime_rows = 0
    for cc_map, relation, _document in _runtime_documents(rec):
        sig, numbers = runtime_relation_signature(relation)
        index[cc_map][sig].add(numbers)
        runtime_rows += 1
    rule_rows = 0
    for recipe in comparator.cached_expanded_cc_recipes():
        if recipe.source != "cc" or not str(recipe.map_name).startswith("cruciblecraft:"):
            continue
        sig, numbers = rule_signature(recipe, materials)
        index[str(recipe.map_name).split(":", 1)[1]][sig].add(numbers)
        rule_rows += 1
    digest_rows = sorted(
        stable_json([cc_map, list(map(list, sig)), sorted(numbers)])
        for cc_map, sigs in index.items()
        for sig, numbers in sigs.items()
    )
    return {
        "index": index,
        "runtime_rows": runtime_rows,
        "rule_rows": rule_rows,
        "digest": sha256_text("\n".join(digest_rows)),
    }


def translator_fingerprint() -> dict[str, str]:
    return {
        path.relative_to(ROOT).as_posix(): sha256_file(path)
        for path in (*TRANSLATOR_SOURCES, BUILDER_PATH)
    }


# --------------------------------------------------------------------------
# GT6 translation
# --------------------------------------------------------------------------


def translator_maps() -> dict[str, Any]:
    """The waves' translator maps plus the live extruder shape catalog.

    ``gt6_resolve.extruder_shapes`` maps GT6 ``Shape_Extruder_*`` metas onto
    the live ``ExtruderShapeCatalog`` items. Shapes without a live CC item stay
    unmapped and classify as ``missing_object``.
    """
    from tools import gt6_resolve
    from tools.recipe_bulk.dialects import gt6 as dialect

    maps = dict(dialect._maps())
    overlay = dict(maps["items"])
    for row in gt6_resolve.extruder_shapes().values():
        if (
            isinstance(row, dict)
            and row.get("live")
            and row.get("cc_item")
            and isinstance(row.get("meta"), int)
        ):
            overlay[(TECHNOLOGICAL_ITEM, row["meta"])] = {
                "runtime_id": row["cc_item"],
                "mapping_class": "proven_equivalent",
            }
    maps["items"] = overlay
    return maps


def _failure_class(error: Exception) -> str:
    text = str(error)
    if "'fluid'" in text:
        return "missing_fluid"
    if "gregtech:gt.meta." in text:
        return "missing_material_form"
    return "missing_object"


def _failure_key(error: Exception) -> str:
    text = str(error)
    item = re.search(r"'item': '([^']+)'", text)
    if item:
        return item.group(1)
    fluid = re.search(r"'fluid': '([^']+)'", text)
    if fluid:
        return "fluid:" + fluid.group(1)
    obj = re.search(r"(gregtech:[a-z0-9_.]+)", text)
    if obj:
        return obj.group(1)
    return re.sub(r"\d+", "N", text)[:80]


def translate_row(raw: dict[str, Any], source_map: str, index: int, row_hash: str, maps: dict[str, Any]):
    """Return ((signature, numbers), None) or (None, error)."""
    from tools.recipe_bulk import emit
    from tools.recipe_bulk.dialects import gt6 as dialect

    try:
        relation, _errors = dialect.compile_row(
            raw,
            host="coverage",
            target_map="coverage",
            source_map=source_map,
            family_id="coverage",
            template_key=f"{source_map}#{index}",
            recipe_index=index,
            shadow_order=0,
            source_revision="coverage",
            source_row_sha256=row_hash,
            maps=maps,
        )
        ins = [
            (logical_item(emit.emit_item(op)), int(count))
            for op, count in zip(relation["item_inputs"], relation["item_input_counts"])
        ]
        outs = [
            (
                logical_item(emit.emit_item_output(op)),
                int((op.get("source") or {}).get("count") or 1),
            )
            for op in relation["item_outputs"]
        ]
        fluid_ins = []
        for op in relation["fluid_inputs"]:
            emitted = emit.emit_fluid(op)
            fluid_ins.append((emitted["id"], emitted["amount"]))
        fluid_outs = []
        for op in relation["fluid_outputs"]:
            emitted = emit.emit_fluid(op)
            fluid_outs.append((emitted["id"], emitted["amount"]))
    except (ValueError, KeyError) as error:
        return None, error
    numbers = (int(relation["duration"]), int(relation["eut"]))
    return (signature(ins, outs, fluid_ins, fluid_outs), numbers), None


# --------------------------------------------------------------------------
# Classification
# --------------------------------------------------------------------------


def _proven_rows(rec) -> tuple[dict[str, set[str]], dict[str, set[str]], dict[tuple[str, str], tuple]]:
    """Hash-proven (gt_map -> hashes), CC hosts per GT map, and CC signature per proven row."""
    rows, _rules = rec.scan_cc_recipes()
    recipe_maps = rec.load_recipe_maps()
    owners = rec.name_owners(recipe_maps)
    attribution = rec.resolve_attribution(rows, owners, refresh=True)
    traces, _untraced, _counts = rec.attribute_rows(rows, owners, attribution)
    proven = {name: set(trace.exact) for name, trace in traces.items()}
    hosts = {
        name: set(trace.exact_hosts) | set(trace.reference_hosts)
        for name, trace in traces.items()
    }
    cc_signatures: dict[tuple[str, str], tuple] = {}
    for cc_map, relation, document in _runtime_documents(rec):
        provenance = relation.get("provenance") or {}
        declared, _ref = rec.declared_source(
            provenance.get("selected_source_recipe"), document.get("family_id")
        )
        base = declared or owners.get(cc_map)
        if not base:
            continue
        for value in provenance.get("evidence_hashes") or []:
            if isinstance(value, str) and value in proven.get(base, ()):
                cc_signatures.setdefault((base, value), runtime_relation_signature(relation)[0])
    return proven, hosts, cc_signatures


def classify(projection: dict[str, Any]) -> dict[str, Any]:
    rec = load_reconciliation()
    recipe_maps = rec.load_recipe_maps()
    dump_rows = rec.load_dump_rows()
    proven, hosts, cc_proven_signatures = _proven_rows(rec)
    maps = translator_maps()
    index = projection["index"]
    item_index = {
        cc_map: {(sig[0], sig[1]) for sig in sigs}
        for cc_map, sigs in index.items()
    }

    per_map: dict[str, Counter[str]] = {}
    failures: dict[str, Counter[str]] = defaultdict(Counter)
    calibration = Counter()
    calibration_by_map: dict[str, Counter[str]] = defaultdict(Counter)
    calibration_samples: list[dict[str, Any]] = []
    for row in recipe_maps:
        source_map = str(row.get("name_internal") or "")
        count = int(row.get("recipe_count") or 0)
        if not source_map or not count:
            continue
        counts: Counter[str] = Counter()
        per_map[source_map] = counts
        if row.get("classification") == "out_of_scope":
            counts["legacy_exclusion_pending"] = count
            continue
        candidates = (
            {rec.map_key(source_map)}
            | hosts.get(source_map, set())
            | {cc for cc, gt in rec.CC_MAP_GT6_OWNERS.items() if gt == source_map}
        )
        recipes = load_json(DUMP_MAPS / f"{source_map}.json").get("recipes") or []
        hashes = dump_rows.get(source_map, [])
        proven_here = proven.get(source_map, set())
        for position, raw in enumerate(recipes):
            row_hash = hashes[position]
            is_proven = row_hash in proven_here
            if not is_proven and (raw.get("fake") is True or raw.get("hidden") is True):
                counts["display_only"] += 1
                continue
            translated, error = translate_row(raw, source_map, position, row_hash, maps)
            if is_proven:
                counts["source_exact"] += 1
                cc_sig = cc_proven_signatures.get((source_map, row_hash))
                if cc_sig is None:
                    continue
                calibration["pairs"] += 1
                if translated is None:
                    calibration["untranslatable"] += 1
                    calibration_by_map[source_map]["untranslatable"] += 1
                elif translated[0] == cc_sig:
                    calibration["equal"] += 1
                    calibration_by_map[source_map]["equal"] += 1
                else:
                    calibration["differs"] += 1
                    calibration_by_map[source_map]["differs"] += 1
                    if len(calibration_samples) < 20:
                        calibration_samples.append(
                            {"map": source_map, "index": position, "sha256": row_hash}
                        )
                continue
            if translated is None:
                bucket = _failure_class(error)
                counts[bucket] += 1
                failures[bucket][_failure_key(error)] += 1
                continue
            sig, numbers = translated
            hits = [index[cc_map][sig] for cc_map in candidates if sig in index.get(cc_map, {})]
            if hits:
                counts["translated_exact" if any(numbers in hit for hit in hits) else "translated_io_only"] += 1
            elif any((sig[0], sig[1]) in item_index.get(cc_map, ()) for cc_map in candidates):
                counts["translated_item_io"] += 1
            else:
                counts["translatable_missing"] += 1
        if sum(counts.values()) != count:
            raise ValueError(f"{source_map}: classified {sum(counts.values())} of {count} rows")

    summary = Counter()
    for counts in per_map.values():
        summary.update(counts)
    pairs = calibration["pairs"]
    return {
        "summary": {name: summary[name] for name in CLASSES},
        "total_rows": sum(summary.values()),
        "maps": {
            name: {cls: counts[cls] for cls in CLASSES if counts[cls]}
            for name, counts in sorted(per_map.items())
        },
        "failure_top": {
            bucket: [
                {"key": key, "rows": rows}
                for key, rows in counter.most_common(25)
            ]
            for bucket, counter in sorted(failures.items())
        },
        "calibration": {
            "pairs": pairs,
            "equal": calibration["equal"],
            "differs": calibration["differs"],
            "untranslatable": calibration["untranslatable"],
            "agreement": round(calibration["equal"] / pairs, 4) if pairs else None,
            "by_map": {
                name: dict(sorted(counter.items()))
                for name, counter in sorted(calibration_by_map.items())
                if counter.get("differs") or counter.get("untranslatable")
            },
            "differs_samples": calibration_samples,
        },
    }


def build_report() -> dict[str, Any]:
    if not DUMP_MAPS.is_dir():
        raise FileNotFoundError(
            f"{DUMP_MAPS.relative_to(ROOT)} is missing; semantic --write needs the local GT6 dump"
        )
    projection = build_cc_projection()
    result = classify(projection)
    scope = load_json(SCOPE_PATH)
    return {
        "schema_version": 2,
        "assessment_id": scope["assessment_id"],
        "status": "CURRENT_RAW_ROW_CLASSIFICATION",
        "source_revision": scope["source"]["revision"],
        "method": (
            "GT6 dump rows translated with recipe_bulk dialects/gt6.compile_row + emit, "
            "compared in CC logical-identity space against CC runtime rows and expanded "
            "material_rule recipes on the serving CC RecipeMaps."
        ),
        "classes": list(CLASSES),
        "translator": translator_fingerprint(),
        "cc_projection": {
            "runtime_rows": projection["runtime_rows"],
            "rule_rows": projection["rule_rows"],
            "digest": projection["digest"],
        },
        **result,
    }


def check_report() -> int:
    if not OUTPUT_PATH.is_file():
        print(f"{OUTPUT_PATH.relative_to(ROOT)} is missing; run --write")
        return 1
    pinned = load_json(OUTPUT_PATH)
    errors = []
    if pinned.get("schema_version") != 2:
        errors.append("semantic coverage schema changed")
    if pinned.get("translator") != translator_fingerprint():
        errors.append("translator or builder sources changed")
    projection = build_cc_projection()
    pinned_projection = pinned.get("cc_projection") or {}
    for key in ("runtime_rows", "rule_rows", "digest"):
        if pinned_projection.get(key) != projection[key]:
            errors.append(f"CC projection changed: {key}")
    if errors:
        print("semantic coverage is stale:")
        print("\n".join(f"- {error}" for error in errors))
        print(
            "run python tools/waves/portfolio/gt6-full-coverage-reassessment/"
            "build_semantic_coverage.py --write on a machine with gt6_dump"
        )
        return 1
    print("GT6 raw-row classification is current")
    return 0


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("use exactly one of --write or --check")
    if args.check:
        return check_report()
    report = build_report()
    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT_PATH.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {OUTPUT_PATH.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
