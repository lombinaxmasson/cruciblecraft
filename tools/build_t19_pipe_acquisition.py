#!/usr/bin/env python3
"""Build T19c's 25-row nonmetal pipe acquisition closure."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
POLICY = TOOLS / "t19_pipe_acquisition_policy.json"
OUTPUT = TOOLS / "t19_pipe_acquisition.json"
GT6_PIPE_SOURCE = TOOLS / "gt6_pipe_source.json"
T8_READINESS = TOOLS / "t8_pipe_readiness.json"
GATE = (
    ROOT
    / "src/main/resources/data/cruciblecraft/"
    / "material_registration_gate.json"
)
MATERIAL_ROOT = (
    ROOT / "src/main/resources/data/cruciblecraft/materials"
)
PREFIX_ROOT = (
    ROOT / "src/main/resources/data/cruciblecraft/material_prefixes"
)
GENERATED = ROOT / "src/generated/resources"
GENERATED_PACK = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/material/gen/"
    / "GeneratedMaterialPack.java"
)
PIPE_CATALOG = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/logistics/pipe/"
    / "PipeCatalog.java"
)
RECIPE_CATALOG = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/logistics/pipe/"
    / "PipeAcquisitionRecipeCatalog.java"
)

GAUGES = {
    "tiny": ("tiny_fluid_pipe", "pipeTiny"),
    "small": ("small_fluid_pipe", "pipeSmall"),
    "normal": ("fluid_pipe", "pipeMedium"),
    "large": ("large_fluid_pipe", "pipeLarge"),
    "huge": ("huge_fluid_pipe", "pipeHuge"),
}
MATERIALS = {
    "wood",
    "carbon",
    "plastic",
    "rubber",
    "wood_treated",
}
SOURCE_ROWS = {
    1887: (
        "tiny",
        [" s", " W ", "r "],
        "OD.slabWood",
        "minecraft:wooden_slabs",
    ),
    1888: (
        "small",
        [" s", " W ", "r "],
        "OD.plankAnyWood",
        "minecraft:planks",
    ),
    1889: (
        "normal",
        [" s", "WWW", "r "],
        "OD.plankAnyWood",
        "minecraft:planks",
    ),
    1890: (
        "large",
        ["WWs", "W W", "rWW"],
        "OD.plankAnyWood",
        "minecraft:planks",
    ),
    1891: (
        "huge",
        [" s", "W W", "r "],
        "OD.beamWood",
        "minecraft:logs",
    ),
}


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def recursive_rows(value: Any):
    if isinstance(value, dict):
        yield value
        for nested in value.values():
            yield from recursive_rows(nested)
    elif isinstance(value, list):
        for nested in value:
            yield from recursive_rows(nested)


def validate_source(policy: dict[str, Any]) -> None:
    source = load(GT6_PIPE_SOURCE)["gt6_source"]
    evidence = policy["source_evidence"]
    source_file = source["files"][evidence["path"]]
    if (
        evidence["repository"] != source["repository"]
        or evidence["revision"] != source["revision"]
        or evidence["sha256"] != source_file["sha256"]
        or evidence["line_range"] != [1887, 1891]
    ):
        raise ValueError("T19c Wood source evidence drifted")


def validate_policy(policy: dict[str, Any]) -> list[dict[str, Any]]:
    if (
        policy.get("schema_version") != 1
        or policy.get("status") != "T19_PIPE_ACQUISITION_POLICY"
    ):
        raise ValueError("T19c pipe acquisition policy header drifted")
    validate_source(policy)
    rows = policy.get("rows") or []
    if len(rows) != 25:
        raise ValueError("T19c policy must contain exactly 25 rows")
    expected = {
        (material, gauge)
        for material in MATERIALS
        for gauge in GAUGES
    }
    actual = {
        (str(row.get("material")), str(row.get("gauge")))
        for row in rows
    }
    if actual != expected or len(actual) != len(rows):
        raise ValueError("T19c material/gauge Cartesian set drifted")
    if set(policy.get("operand_producers") or {}) != {
        identity
        for row in rows
        for identity in (row.get("key") or {}).values()
    }:
        raise ValueError("T19c operand producer domain drifted")

    source_lines = set()
    for row in rows:
        material = str(row["material"])
        gauge = str(row["gauge"])
        output_form, specification = GAUGES[gauge]
        if (
            row.get("output_form") != output_form
            or row.get("specification") != specification
            or row.get("output")
            != f"cruciblecraft:{material}/{output_form}"
        ):
            raise ValueError(
                f"{material}/{gauge}: output/specification drifted"
            )
        pattern = row.get("pattern") or []
        if (
            not 1 <= len(pattern) <= 3
            or len({len(value) for value in pattern}) != 1
            or not 1 <= len(pattern[0]) <= 3
        ):
            raise ValueError(
                f"{material}/{gauge}: invalid shaped pattern"
            )
        used = {
            symbol for value in pattern for symbol in value if symbol != " "
        }
        if used != set((row.get("key") or {})):
            raise ValueError(
                f"{material}/{gauge}: pattern/key mismatch"
            )
        if not 1 <= int(row.get("output_count", 0)) <= 64:
            raise ValueError(
                f"{material}/{gauge}: invalid output count"
            )
        if material == "wood":
            if row.get("classification") != "GT6_SOURCE_CRAFTING":
                raise ValueError("Wood row lost GT6 source classification")
            source_line = int(row.get("source_line", 0))
            expected_source = SOURCE_ROWS.get(source_line)
            if expected_source is None or (
                gauge,
                row.get("source_pattern"),
                row.get("source_operand"),
                row["key"].get("W"),
            ) != expected_source:
                raise ValueError(
                    f"wood/{gauge}: source line/pattern/operand drifted"
                )
            source_lines.add(source_line)
        else:
            if (
                row.get("classification")
                != "DESIGN_POLICY_NON_GT6"
                or not str(row.get("reason") or "").strip()
                or not str(row.get("operand_pattern") or "").strip()
                or any(
                    field in row
                    for field in (
                        "source_line",
                        "source_pattern",
                        "source_operand",
                    )
                )
            ):
                raise ValueError(
                    f"{material}/{gauge}: design policy is incomplete"
                )
    if source_lines != set(SOURCE_ROWS):
        raise ValueError("Wood source line set drifted")
    return rows


def validate_producers(
    policy: dict[str, Any],
    registered: dict[str, list[str]],
) -> dict[str, dict[str, Any]]:
    result = {}
    for identity, producer in sorted(
        policy["operand_producers"].items()
    ):
        kind = producer["kind"]
        row: dict[str, Any] = {"kind": kind, "reachable": True}
        if kind == "vanilla_tag":
            if identity not in {
                "minecraft:wooden_slabs",
                "minecraft:planks",
                "minecraft:logs",
            } or not str(producer.get("representative") or "").startswith(
                "minecraft:"
            ):
                raise ValueError(
                    f"{identity}: invalid vanilla producer"
                )
            row["representative"] = producer["representative"]
        else:
            path = ROOT / producer["path"]
            document = load(path)
            row["path"] = relative(path)
            row["sha256"] = sha256(path)
            if kind == "fixed_recipe_output":
                output = document.get("result") or {}
                if output.get("id") != identity:
                    raise ValueError(
                        f"{identity}: fixed producer output drifted"
                    )
            elif kind == "compact_material_rule_output":
                matches = [
                    candidate
                    for candidate in recursive_rows(document)
                    if candidate.get("stable_id")
                    == producer["stable_id"]
                ]
                material = identity.split(":", 1)[1].split("/", 1)[0]
                if (
                    len(matches) != 1
                    or matches[0].get("material") != material
                    or matches[0].get("output", {}).get("prefix")
                    != "plate"
                    or not set(producer["required_registered_forms"])
                    .issubset(registered[material])
                ):
                    raise ValueError(
                        f"{identity}: compact producer drifted"
                    )
            elif kind == "material_rule_output":
                if (
                    document.get("material") != producer["material"]
                    or document.get("item_outputs", [{}])[0].get(
                        "prefix"
                    )
                    != f"cruciblecraft:{producer['prefix']}"
                    or producer["prefix"]
                    not in registered[producer["material"]]
                ):
                    raise ValueError(
                        f"{identity}: material-rule producer drifted"
                    )
            elif kind == "gt_recipe_output":
                outputs = document.get("item_outputs") or []
                if not any(
                    output.get("id") == identity for output in outputs
                ):
                    raise ValueError(
                        f"{identity}: GT producer output drifted"
                    )
            else:
                raise ValueError(
                    f"{identity}: unknown producer kind {kind}"
                )
        result[identity] = row
    return result


def ingredient_identity(value: dict[str, Any]) -> str:
    if set(value) == {"item"}:
        return str(value["item"])
    if set(value) == {"tag"}:
        return str(value["tag"])
    raise ValueError(f"unsupported shaped ingredient: {value}")


def build(
    policy_override: dict[str, Any] | None = None,
) -> dict[str, Any]:
    policy = load(POLICY) if policy_override is None else policy_override
    rows = validate_policy(policy)
    gate = load(GATE)
    registered = gate["materials"]
    producers = validate_producers(policy, registered)
    generated_pack_source = GENERATED_PACK.read_text(encoding="utf-8")
    if not all(
        token in generated_pack_source
        for token in (
            "/tags/item/",
            "/tags/block/",
            "/models/item/",
            "selfDropLootTable",
        )
    ):
        raise ValueError(
            "GeneratedMaterialPack output contract drifted"
        )

    resources: dict[str, str] = {}
    result_rows = []
    unreachable = []
    for policy_row in rows:
        material_id = policy_row["material"]
        output_form = policy_row["output_form"]
        output = policy_row["output"]
        material = load(MATERIAL_ROOT / f"{material_id}.json")
        specification = policy_row["specification"]
        pipe = (
            material.get("gt6_metadata", {})
            .get("pipe_properties", {})
            .get("fluid_by_specification", {})
            .get(specification)
        )
        if pipe is None or pipe.get("recipe") is not False:
            raise ValueError(
                f"{material_id}/{output_form}: GT6 recipe flag changed"
            )
        if output_form not in registered.get(material_id, []):
            raise ValueError(
                f"{material_id}/{output_form}: output is not registered"
            )
        recipe_path = ROOT / policy_row["recipe"]
        recipe = load(recipe_path)
        result = recipe.get("result") or {}
        actual_key = {
            symbol: ingredient_identity(ingredient)
            for symbol, ingredient in (recipe.get("key") or {}).items()
        }
        if (
            recipe.get("type") != "minecraft:crafting_shaped"
            or recipe.get("pattern") != policy_row["pattern"]
            or actual_key != policy_row["key"]
            or result.get("id") != output
            or int(result.get("count", 1))
            != int(policy_row["output_count"])
        ):
            raise ValueError(
                f"{material_id}/{output_form}: generated recipe drifted"
            )
        registry_path = f"{material_id}/{output_form}"
        blockstate = (
            GENERATED
            / f"assets/cruciblecraft/blockstates/{registry_path}.json"
        )
        block_model = (
            GENERATED
            / f"assets/cruciblecraft/models/{registry_path}.json"
        )
        loot = (
            GENERATED
            / (
                "data/cruciblecraft/loot_table/blocks/"
                f"{registry_path}.json"
            )
        )
        required_files = [recipe_path, blockstate, block_model, loot]
        missing = [
            relative(path) for path in required_files if not path.is_file()
        ]
        if missing:
            unreachable.extend(missing)
        for path in required_files:
            if path.is_file():
                resources[relative(path)] = sha256(path)
        prefix_path = PREFIX_ROOT / f"{output_form}.json"
        prefix = load(prefix_path)
        if prefix.get("id") != f"cruciblecraft:{output_form}":
            raise ValueError(
                f"{output_form}: material prefix identity drifted"
            )
        resources[relative(prefix_path)] = sha256(prefix_path)
        tag_path = (
            f"{prefix['tag_directory']}/{material['tag_name']}"
        )
        result_rows.append(
            {
                **policy_row,
                "gt6_recipe_flag": False,
                "producer_operands": {
                    symbol: producers[identity]
                    for symbol, identity in policy_row["key"].items()
                },
                "reachability": {
                    "block_registered": True,
                    "item_registered": True,
                    "recipe": relative(recipe_path),
                    "loot": relative(loot),
                    "blockstate": relative(blockstate),
                    "block_model": relative(block_model),
                    "item_model": (
                        "runtime-generated:assets/cruciblecraft/"
                        f"models/item/{registry_path}.json"
                    ),
                    "block_tag": f"c:{tag_path}",
                    "item_tag": f"c:{tag_path}",
                    "reachable": not missing,
                },
            }
        )
    if unreachable:
        raise ValueError(
            "T19c pipe acquisition resources are unreachable: "
            + ", ".join(sorted(unreachable))
        )
    # O-27 T19c acquisition closure: prove the runtime rows equal the T8
    # pipe-domain acquisition forms (forward edge 13 -> 48).  T8 records only
    # its own derived forms, so this check keeps the builder graph acyclic.
    t8 = load(T8_READINESS)
    acquisition = t8.get("nonmetal_fluid_pipe_acquisition") or {}
    if acquisition.get("status") != "CLOSED_T19C":
        raise ValueError("T19c closure requires a CLOSED_T19C T8 pipe domain")
    expected_forms = {
        (str(row["material"]), str(row["gauge"]))
        for row in (acquisition.get("forms") or [])
    }
    actual_forms = {
        (str(row["material"]), str(row["gauge"]))
        for row in result_rows
    }
    if len(actual_forms) != 25 or actual_forms != expected_forms:
        raise ValueError("T19c rows differ from the T8 pipe-domain "
                         "acquisition forms")
    classifications = {
        name: sum(
            row["classification"] == name for row in result_rows
        )
        for name in (
            "GT6_SOURCE_CRAFTING",
            "DESIGN_POLICY_NON_GT6",
        )
    }
    return {
        "schema_version": 1,
        "status": "T19_PIPE_ACQUISITION_READY",
        "source_evidence": policy["source_evidence"],
        "classification_policy": policy["classification_policy"],
        "counts": {
            "rows": 25,
            "materials": 5,
            "gauges": 5,
            "gt6_source_crafting": classifications[
                "GT6_SOURCE_CRAFTING"
            ],
            "design_policy_non_gt6": classifications[
                "DESIGN_POLICY_NON_GT6"
            ],
            "vanilla_shaped_recipes": 25,
            "registered_blocks": 25,
            "registered_items": 25,
            "loot_tables": 25,
            "block_tags": 25,
            "item_tags": 25,
            "block_models": 25,
            "item_models": 25,
            "unreachable": 0,
        },
        "rows": result_rows,
        "unreachable": [],
        "currentness": {
            "owned_inputs": {
                relative(POLICY): sha256(POLICY),
                relative(Path(__file__).resolve()): sha256(
                    Path(__file__).resolve()
                ),
                relative(GT6_PIPE_SOURCE): sha256(GT6_PIPE_SOURCE),
                relative(T8_READINESS): sha256(T8_READINESS),
                (
                    relative(GATE)
                    + "#t19c_registered_output_forms"
                ): hashlib.sha256(
                    json.dumps(
                        {
                            material: sorted(
                                form
                                for form in registered[material]
                                if form
                                in {
                                    output_form
                                    for output_form, _ in GAUGES.values()
                                }
                            )
                            for material in sorted(MATERIALS)
                        },
                        sort_keys=True,
                        separators=(",", ":"),
                    ).encode("utf-8")
                ).hexdigest(),
                relative(GENERATED_PACK): sha256(GENERATED_PACK),
                relative(PIPE_CATALOG): sha256(PIPE_CATALOG),
                relative(RECIPE_CATALOG): sha256(RECIPE_CATALOG),
            },
            "resources": resources,
        },
    }


def check(document: dict[str, Any] | None = None) -> list[str]:
    expected = stable(build() if document is None else document)
    if not OUTPUT.is_file():
        return [f"missing {relative(OUTPUT)}"]
    if OUTPUT.read_text(encoding="utf-8") != expected:
        return [f"stale {relative(OUTPUT)}"]
    return []


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()
    document = build()
    if args.check:
        errors = check(document)
        if errors:
            print("\n".join(errors))
            return 1
        print("T19c pipe acquisition is current")
        return 0
    if args.write:
        OUTPUT.write_bytes(stable(document).encode("utf-8"))
        print(f"wrote {relative(OUTPUT)}")
        return 0
    print(stable(document), end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
