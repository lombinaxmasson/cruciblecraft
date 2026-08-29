"""L3 material structure generator built from replay-verified GT6 domains."""

from __future__ import annotations

import argparse
import hashlib
import json
from collections import defaultdict
from functools import lru_cache
from pathlib import Path
from typing import Any, Iterable

try:
    from . import build_gt6_generation_bits as generation
    from .gt6_mapping import MAPPING_PATH, PREFIX_MAPPINGS
except ImportError:
    import build_gt6_generation_bits as generation
    from gt6_mapping import MAPPING_PATH, PREFIX_MAPPINGS


ROOT = Path(__file__).resolve().parents[1]
GENERATION_BITS_PATH = ROOT / "tools" / "gt6_generation_bits.json"
MATERIALS_PATH = (
    ROOT / "gt6_dump" / "gt6_recipe_dump" / "oredict" / "materials.json"
)
PREFIX_DIRECTORY = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "material_prefixes"
)
OUT = ROOT / "tools" / "gt6_l3_prefix_plan.json"
SHARED_GENERATION_FLAG_OWNERS = {
    "small_dust": "dust",
    "tiny_dust": "dust",
}
POST_IMPORT_T8_PREFIXES = {
    "tiny_fluid_pipe",
    "small_fluid_pipe",
    "fluid_pipe",
    "large_fluid_pipe",
    "huge_fluid_pipe",
    "item_pipe",
    "large_item_pipe",
    "huge_item_pipe",
}
POST_IMPORT_T10A_PREFIXES = {
    "double_ingot",
    "triple_ingot",
    "ingot_hot",
}
POST_IMPORT_T38_PREFIXES = {"dust_div72"}
POST_IMPORT_PREFIXES = (
    POST_IMPORT_T8_PREFIXES | POST_IMPORT_T10A_PREFIXES | POST_IMPORT_T38_PREFIXES
)


@lru_cache(maxsize=1)
def prefix_implication_closures() -> dict[str, frozenset[str]]:
    """Resolve the bundled prefix implication DAG to canonical form paths."""
    aliases: dict[str, str] = {}
    definitions: dict[str, dict[str, Any]] = {}
    for filename in json.loads(
        (PREFIX_DIRECTORY / "index.json").read_text(encoding="utf-8")
    ):
        definition = json.loads(
            (PREFIX_DIRECTORY / filename).read_text(encoding="utf-8")
        )
        canonical = definition["serialized_path"]
        if canonical in definitions:
            raise ValueError(f"duplicate prefix path: {canonical}")
        definitions[canonical] = definition
        for alias in (
            definition["id"],
            definition["id"].split(":", 1)[-1],
            canonical,
            *(definition.get("aliases") or []),
        ):
            previous = aliases.setdefault(alias, canonical)
            if previous != canonical:
                raise ValueError(
                    f"prefix alias conflict for {alias}: {previous}, {canonical}"
                )

    direct: dict[str, tuple[str, ...]] = {}
    for canonical, definition in definitions.items():
        targets = []
        for value in definition.get("implied_prefixes") or []:
            key = value if value in aliases else value.split(":")[-1]
            target = aliases.get(key)
            if target is None:
                raise ValueError(
                    f"unknown implied prefix {value!r} from {canonical}"
                )
            if target in targets:
                raise ValueError(
                    f"duplicate implied prefix {value!r} from {canonical}"
                )
            targets.append(target)
        direct[canonical] = tuple(targets)

    resolved: dict[str, frozenset[str]] = {}

    def visit(source: str, visiting: tuple[str, ...]) -> frozenset[str]:
        if source in resolved:
            return resolved[source]
        if source in visiting:
            cycle = " -> ".join((*visiting, source))
            raise ValueError(f"material prefix implication cycle: {cycle}")
        closure: set[str] = set()
        path = (*visiting, source)
        for target in direct[source]:
            closure.add(target)
            closure.update(visit(target, path))
        result = frozenset(closure)
        resolved[source] = result
        return result

    for canonical in definitions:
        visit(canonical, ())
    return resolved


def close_implied_prefixes(prefixes: Iterable[str]) -> set[str]:
    closures = prefix_implication_closures()
    selected = set(prefixes)
    for prefix in tuple(selected):
        if prefix not in closures:
            raise ValueError(f"unknown canonical prefix in closure: {prefix}")
        selected.update(closures[prefix])
    return selected


@lru_cache(maxsize=1)
def catalog_generation_flags() -> dict[str, str]:
    """Return generation flags for the complete runtime prefix catalog."""
    result: dict[str, str] = {}
    filenames = json.loads(
        (PREFIX_DIRECTORY / "index.json").read_text(encoding="utf-8")
    )
    for filename in filenames:
        definition = json.loads(
            (PREFIX_DIRECTORY / filename).read_text(encoding="utf-8")
        )
        prefix = definition["serialized_path"]
        flag = definition["generation_flag"]
        previous = result.setdefault(prefix, flag)
        if previous != flag:
            raise ValueError(f"prefix generation flag drifted for {prefix}")
    return result


def _stable_hash(value: Any) -> str:
    payload = json.dumps(
        value,
        sort_keys=True,
        separators=(",", ":"),
        ensure_ascii=False,
    )
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def _file_sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _stable_materials(
    materials: list[dict[str, Any]],
) -> tuple[frozenset[str], dict[str, dict[str, Any]]]:
    by_name: dict[str, dict[str, Any]] = {}
    for row in materials:
        name = row.get("nameInternal")
        if not isinstance(name, str) or not name:
            raise ValueError(f"GT6 material has no stable name: {row}")
        if name in by_name:
            raise ValueError(f"duplicate GT6 material name: {name}")
        by_name[name] = row
    stable = frozenset(
        name
        for name, row in by_name.items()
        if isinstance(row.get("id"), int) and row["id"] >= 0
    )
    return stable, by_name


def _tag_sets(
    stable: frozenset[str],
    by_name: dict[str, dict[str, Any]],
) -> dict[str, frozenset[str]]:
    tags = {
        tag
        for name in stable
        for tag in (by_name[name].get("tags") or [])
    }
    return {
        tag: frozenset(
            name
            for name in stable
            if tag in (by_name[name].get("tags") or [])
        )
        for tag in sorted(tags)
    }


def build_document(
    materials: list[dict[str, Any]],
    generation_bits: dict[str, Any],
) -> dict[str, Any]:
    if generation_bits.get("schema_version") != 3:
        raise ValueError("L3 requires generation-bits schema v3")
    stable, by_name = _stable_materials(materials)
    tag_sets = _tag_sets(stable, by_name)
    generation_flags = generation_bits["generation_tag_flags"]

    source_domains: dict[str, frozenset[str]] = {}
    source_rules: dict[str, dict[str, Any]] = {}
    for prefix, rule in generation_bits["prefix_generation_rules"].items():
        source_domains[prefix] = generation.replay_rule(rule, tag_sets)
        source_rules[prefix] = rule
    for prefix, explicit in generation_bits["explicit_prefix_domains"].items():
        materials_in_domain = frozenset(explicit["materials"])
        if not materials_in_domain <= stable:
            raise ValueError(f"explicit prefix domain contains unstable names: {prefix}")
        source_domains[prefix] = materials_in_domain
        source_rules[prefix] = explicit

    for mapping in PREFIX_MAPPINGS:
        if mapping["strategy"] != "derived_intersection":
            continue
        dependencies = mapping["domain_prefixes"]
        missing = [prefix for prefix in dependencies if prefix not in source_domains]
        if missing:
            raise ValueError(
                f"derived prefix {mapping['gt_prefix']} has missing domains: {missing}"
            )
        domain = frozenset.intersection(
            *(source_domains[prefix] for prefix in dependencies)
        )
        source_domains[mapping["gt_prefix"]] = domain
        source_rules[mapping["gt_prefix"]] = {
            "status": "derived_intersection",
            "domain_prefixes": list(dependencies),
        }

    sources_by_cc: dict[str, list[str]] = defaultdict(list)
    for mapping in PREFIX_MAPPINGS:
        cc_prefix = mapping["cc_prefix"]
        if cc_prefix is None or cc_prefix in POST_IMPORT_PREFIXES:
            continue
        gt_prefix = mapping["gt_prefix"]
        if gt_prefix not in source_domains:
            raise ValueError(f"mapped GT6 prefix has no generation domain: {gt_prefix}")
        sources_by_cc[cc_prefix].append(gt_prefix)

    prefix_plans: dict[str, dict[str, Any]] = {}
    for cc_prefix, source_prefixes in sorted(sources_by_cc.items()):
        target = frozenset().union(
            *(source_domains[source] for source in source_prefixes)
        )
        source_rule = source_rules[source_prefixes[0]]
        expression = source_rule.get("expression")
        direct_tag = (
            expression.get("tag")
            if len(source_prefixes) == 1
            and isinstance(expression, dict)
            and expression.get("op") == "tag"
            else None
        )
        if direct_tag is not None:
            flag = generation_flags[direct_tag]
            base = tag_sets[direct_tag]
            mode = "direct_tag"
        elif cc_prefix == "ore" and len(source_prefixes) == 1:
            # Keep GT6's inferred ore domain separate from its reviewed exceptions.
            # Iron therefore remains an explicit material include, and additional
            # world-ore exceptions such as Tungsten do not alter the GT6 domain.
            base = frozenset(
                generation.evaluate_expression(source_rule["expression"], tag_sets)
            ) - frozenset(source_rule.get("exclude_materials") or [])
            flag = "cruciblecraft:generates_ore"
            mode = "composite"
        else:
            flag = f"cruciblecraft:generates_{cc_prefix}"
            base = target
            mode = "composite"
        prefix_plans[cc_prefix] = {
            "mode": mode,
            "generation_flag": flag,
            "source_prefixes": sorted(source_prefixes),
            **({"source_tag": direct_tag} if direct_tag is not None else {}),
            "base_materials": sorted(base),
            "target_materials": sorted(target),
            "base_material_count": len(base),
            "target_material_count": len(target),
            "include_materials": sorted(target - base),
            "exclude_materials": sorted(base - target),
            "target_set_sha256": _stable_hash(sorted(target)),
        }

    for cc_prefix, owner in SHARED_GENERATION_FLAG_OWNERS.items():
        if cc_prefix not in prefix_plans or owner not in prefix_plans:
            raise ValueError(
                f"shared generation flag references unknown prefix: {cc_prefix} -> {owner}"
            )
        prefix_plans[cc_prefix]["mode"] = "shared"
        prefix_plans[cc_prefix]["generation_flag"] = prefix_plans[owner][
            "generation_flag"
        ]
        prefix_plans[cc_prefix]["shared_generation_flag_with"] = owner

    prefix_ids = _bundled_prefix_ids() - POST_IMPORT_PREFIXES
    if set(prefix_plans) != prefix_ids:
        raise ValueError(
            "L3 prefix coverage differs from bundled catalog; "
            f"missing={sorted(prefix_ids - set(prefix_plans))}, "
            f"extra={sorted(set(prefix_plans) - prefix_ids)}"
        )

    return {
        "schema_version": 1,
        "artifact_kind": "gt6_l3_prefix_plan",
        "source": {
            "materials": MATERIALS_PATH.relative_to(ROOT).as_posix(),
            "materials_sha256": _file_sha256(MATERIALS_PATH),
            "generation_bits": GENERATION_BITS_PATH.relative_to(ROOT).as_posix(),
            "generation_bits_sha256": _file_sha256(GENERATION_BITS_PATH),
            "prefix_mapping": MAPPING_PATH.relative_to(ROOT).as_posix(),
            "prefix_mapping_sha256": _file_sha256(MAPPING_PATH),
        },
        "prefixes": prefix_plans,
        "verification": {
            "stable_material_count": len(stable),
            "prefix_count": len(prefix_plans),
            "direct_tag_prefix_count": sum(
                plan["mode"] == "direct_tag"
                for plan in prefix_plans.values()
            ),
            "composite_prefix_count": sum(
                plan["mode"] == "composite"
                for plan in prefix_plans.values()
            ),
            "shared_prefix_count": sum(
                plan["mode"] == "shared"
                for plan in prefix_plans.values()
            ),
            "catalog_coverage_verified": True,
            "source_domains_replay_verified": True,
        },
    }


def encode_material_forms(
    source_name: str,
    desired_forms: Iterable[str],
    document: dict[str, Any],
) -> dict[str, list[str]]:
    declared = set(desired_forms)
    desired = close_implied_prefixes(declared)
    plans = document["prefixes"]
    unknown = desired - set(plans)
    if unknown:
        raise ValueError(
            f"{source_name} requests prefixes outside the L3 plan: {sorted(unknown)}"
        )
    if not desired:
        return {
            "generation_flags": [],
            "include_prefixes": [],
            "exclude_prefixes": [],
        }

    flags = {
        plan["generation_flag"]
        for plan in plans.values()
        if source_name in plan["base_materials"]
    }
    baseline = {
        prefix
        for prefix, plan in plans.items()
        if plan["generation_flag"] in flags
    }
    includes = declared - baseline
    expanded = close_implied_prefixes(baseline | includes)
    encoded = {
        "generation_flags": sorted(flags),
        "include_prefixes": sorted(includes),
        "exclude_prefixes": sorted(expanded - desired),
    }
    if resolve_material_forms(encoded, document) != desired:
        raise ValueError(f"L3 structure failed exact form replay for {source_name}")
    return encoded


def resolve_material_forms(
    structure: dict[str, list[str]],
    document: dict[str, Any],
) -> set[str]:
    flags = set(structure.get("generation_flags") or [])
    prefix_flags = {
        prefix: plan["generation_flag"]
        for prefix, plan in document["prefixes"].items()
    }
    for prefix, flag in catalog_generation_flags().items():
        previous = prefix_flags.setdefault(prefix, flag)
        if previous != flag:
            raise ValueError(
                f"L3 prefix generation flag differs from catalog: {prefix}"
            )
    resolved = {
        prefix
        for prefix, flag in prefix_flags.items()
        if flag in flags
    }
    resolved.update(structure.get("include_prefixes") or [])
    resolved = close_implied_prefixes(resolved)
    resolved.difference_update(structure.get("exclude_prefixes") or [])
    return resolved


def apply_material_structure(
    material: dict[str, Any],
    source_name: str,
    desired_forms: Iterable[str],
    document: dict[str, Any],
    *,
    metadata_only: bool = False,
) -> dict[str, Any]:
    result = dict(material)
    desired = tuple(desired_forms)
    for field in (
        "forms",
        "generation_flags",
        "include_prefixes",
        "exclude_prefixes",
        "allow_empty_forms",
        "metadata_only",
    ):
        result.pop(field, None)
    encoded = encode_material_forms(source_name, desired, document)
    result["generation_flags"] = encoded["generation_flags"]
    if encoded["include_prefixes"]:
        result["include_prefixes"] = encoded["include_prefixes"]
    if encoded["exclude_prefixes"]:
        result["exclude_prefixes"] = encoded["exclude_prefixes"]
    if not desired:
        if not metadata_only:
            raise ValueError(
                f"{source_name} has no material forms without explicit direct evidence"
            )
        result["metadata_only"] = True
    elif metadata_only:
        raise ValueError(f"{source_name} is metadata-only but has material forms")
    return result


def _bundled_prefix_ids() -> set[str]:
    index = json.loads(
        (PREFIX_DIRECTORY / "index.json").read_text(encoding="utf-8")
    )
    result = set()
    for filename in index:
        definition = json.loads(
            (PREFIX_DIRECTORY / filename).read_text(encoding="utf-8")
        )
        prefix_id = definition["id"]
        result.add(prefix_id.split(":", 1)[-1])
    return result


def prefix_definition_outputs(
    document: dict[str, Any],
) -> dict[Path, str]:
    index = json.loads(
        (PREFIX_DIRECTORY / "index.json").read_text(encoding="utf-8")
    )
    outputs: dict[Path, str] = {}
    for filename in index:
        path = PREFIX_DIRECTORY / filename
        definition = json.loads(path.read_text(encoding="utf-8"))
        cc_prefix = definition["id"].split(":", 1)[-1]
        if cc_prefix in POST_IMPORT_PREFIXES:
            outputs[path] = path.read_text(encoding="utf-8")
            continue
        definition["generation_flag"] = document["prefixes"][cc_prefix][
            "generation_flag"
        ]
        outputs[path] = json.dumps(
            definition,
            indent=2,
            ensure_ascii=False,
        ) + "\n"
    return outputs


def extract() -> dict[str, Any]:
    materials = json.loads(MATERIALS_PATH.read_text(encoding="utf-8"))
    generation_bits = json.loads(
        GENERATION_BITS_PATH.read_text(encoding="utf-8")
    )
    return build_document(materials, generation_bits)


def main(argv: Iterable[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args(argv)
    document = extract()
    outputs = {
        OUT: json.dumps(document, indent=2, ensure_ascii=False) + "\n",
        **prefix_definition_outputs(document),
    }
    if args.write:
        for path, content in outputs.items():
            path.write_text(content, encoding="utf-8", newline="\n")
    else:
        stale = [
            str(path)
            for path, content in outputs.items()
            if not path.is_file()
            or path.read_text(encoding="utf-8") != content
        ]
        if stale:
            raise ValueError("stale L3 prefix artifacts: " + ", ".join(stale))
    print(json.dumps(document["verification"], sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
