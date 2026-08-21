#!/usr/bin/env python3
"""Build the T22.5 B1 artifact: full classification of the
ordinary_optional recipe-row universe (146,841 expanded rows).

The universe is ledger-2's ordinary_optional units expanded over their
source_recipe_indexes.  Ledger-2 is READ-ONLY here: this artifact
supersedes only its classification vocabulary, complementing the
committed T21 snapshot.

Classification uses the T22 vocabulary (nine classes) with ordered,
deterministic rules; every class is a category definition with a
recheck point, never "cannot translate" and never a status name that
erases a count.

Modes:
  --check --reference-only   validate the committed artifact (no dump)
  --write --full-replay      classify from the GT6 dump and write

Item operands resolve via the A3 artifact; fluid operands via the A2
artifact.  Mixer template rows inherit their template's class (the
template skeleton and bindings live in the compact mixer index); the
86,394 non-mixer rows are classified individually from the dump.
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
OUTPUT = TOOLS / "t22_5_row_classification.json"
BUILDER = Path(__file__).resolve()

LEDGER_2 = TOOLS / "t21_template_denominator.json"
LEDGER_1 = TOOLS / "t21_source_denominator.json"
A2_ARTIFACT = TOOLS / "t22_5_fluid_mapping.json"
A3_ARTIFACT = TOOLS / "t22_5_item_classification.json"
MIXER_INDEX = TOOLS / "gt6_mixer_templates_index.json"
DUMP = ROOT / "gt6_dump" / "gt6_recipe_dump"
MATERIALS_DUMP = DUMP / "oredict" / "materials.json"

GT6_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"

# B1 vocabulary, in rule order.
CLASSES = (
    "v1_required",
    "already_covered",
    "ore_processing_byproduct",
    "generic_processing",
    "cross_mod_compat",
    "post_1_0_g10",
    "post_1_0_nuclear",
    "out_of_scope",
    "ordinary_optional",
)

RULES = [
    {
        "class": "v1_required",
        "order": 1,
        "definition": (
            "Promotion rule: a row whose recipe identity matches a "
            "ledger-2 v1_required unit. By universe construction this "
            "is zero — ledger-2 already classified those units out of "
            "the ordinary_optional universe."
        ),
        "discriminant": "identity match against ledger-2 v1_required "
        "unit source_recipe_indexes",
        "recheck_point": "a consumer proof shows an optional row is on "
        "the Beta main chain",
    },
    {
        "class": "already_covered",
        "order": 2,
        "definition": (
            "The process this source row represents is already covered "
            "by a published CC recipe chain, even though the literal "
            "GT6 row is not published verbatim."
        ),
        "discriminant": "any fluid operand has the A2 "
        "covered_by_petroleum_identity decision",
        "recheck_point": "T23 translation layer does full published-set "
        "identity matching",
    },
    {
        "class": "ore_processing_byproduct",
        "order": 3,
        "definition": (
            "Centrifuge rows that produce oil as an incidental ore-"
            "processing byproduct; covered by the T5/T20 ore chain, "
            "not petroleum chemistry."
        ),
        "discriminant": "map == gt.recipe.centrifuge AND input item "
        "crushedPurified AND output item crushedCentrifuged",
        "recheck_point": "T5/T20 ore chain stops covering these rows",
    },
    {
        "class": "generic_processing",
        "order": 4,
        "definition": (
            "Generic compression/electrolysis rows without a "
            "material-specific identity: compressor dust->plateGem "
            "compression and electrolyzer composition-generated rows."
        ),
        "discriminant": "(map == gt.recipe.compressor AND dust input "
        "AND plateGem output) OR (map == gt.recipe.electrolyzer AND "
        "ledger-1 shape == composition_generated)",
        "recheck_point": "a specific consumer promotes one of these "
        "rows",
    },
    {
        "class": "cross_mod_compat",
        "order": 5,
        "definition": (
            "Rows whose operands belong to other mods or to GT6 "
            "cross-mod identity: fluids without a CC fluid form "
            "(A2 no_cc_fluid), magic-compat fluids, or construction-"
            "foam color variants."
        ),
        "discriminant": "any fluid operand in A2 no_cc_fluid / "
        "out_of_scope_magic_compat / "
        "equivalence_construction_foam_color",
        "recheck_point": "CC gains a consumer or identity for the "
        "referenced mod content",
    },
    {
        "class": "post_1_0_g10",
        "order": 6,
        "definition": (
            "G10 peripheral rows: ammunition (bullet items), food, "
            "brewing, dyeing, dimension atmospheres."
        ),
        "discriminant": "item id contains bulletGt, or any fluid "
        "operand has an A2 out_of_scope_g10_* decision",
        "recheck_point": "G10 phase opens",
    },
    {
        "class": "post_1_0_nuclear",
        "order": 7,
        "definition": (
            "Rows whose candidate materials are all actinides "
            "(ATOMIC.ACTINIDE material tag), the same criterion as "
            "ledger-1."
        ),
        "discriminant": "all material metas in the recipe are "
        "actinide-tagged",
        "recheck_point": "nuclear phase opens",
    },
    {
        "class": "out_of_scope",
        "order": 8,
        "definition": (
            "Rows with operands outside the CC identity universe: A3 "
            "out-of-scope GT6 meta items (crates, machine items, "
            "uncompressed storage blocks), A2 out-of-scope fluids, or "
            "third-party-mod items."
        ),
        "discriminant": "any item in an A3 out_of_scope class, any "
        "fluid in the A2 out_of_scope disposition, or any item id "
        "whose namespace is not minecraft/gregtech/gregapi/"
        "cruciblecraft",
        "recheck_point": "CC implements one of the referenced layers",
    },
    {
        "class": "ordinary_optional",
        "order": 9,
        "definition": (
            "Remainder: ordinary chemistry rows without a v1 consumer. "
            "Post-1.0 portfolio."
        ),
        "discriminant": "no earlier rule matched",
        "recheck_point": "portfolio planning after v1",
    },
]

BUILTIN_NAMESPACES = {"minecraft", "gregtech", "gregapi", "cruciblecraft"}


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


# ---------------------------------------------------------------------------
# context
# ---------------------------------------------------------------------------


def load_context() -> dict[str, Any]:
    """Load the classification context from committed artifacts."""
    ledger_2 = _load(LEDGER_2)
    ledger_1 = _load(LEDGER_1)
    a2 = _load(A2_ARTIFACT)
    a3 = _load(A3_ARTIFACT)
    mixer_index = _load(MIXER_INDEX)
    actinides = set()
    candidate_names = set(ledger_1["encoding"]["material_dictionary"])
    candidate_ids: set[int] = set()
    for material in _load(MATERIALS_DUMP):
        tags = material.get("tags") or []
        if "ATOMIC.ACTINIDE" in tags:
            actinides.add(int(material["id"]))
        name = (material.get("nameInternal") or "").lower()
        if name in candidate_names:
            candidate_ids.add(int(material["id"]))

    a2_by_name = {row["fluid"]: row for row in a2["mapping"]}
    a2_unknown_by_name = {
        row["fluid"]: row for row in a2["unknown_identity"]["records"]
    }
    a3_by_item = {row["item"]: row["class"] for row in a3["records"]}
    a3_out_of_scope = {
        item for item, klass in a3_by_item.items()
        if klass.startswith("out_of_scope")
    }

    ledger_1_shapes: dict[tuple[int, int], str] = {}
    for row in ledger_1["rows"]:
        ledger_1_shapes[(row[0], row[1])] = row[4]

    return {
        "ledger_2": ledger_2,
        "ledger_1_shapes": ledger_1_shapes,
        "a2_by_name": a2_by_name,
        "a2_unknown_by_name": a2_unknown_by_name,
        "a3_out_of_scope": a3_out_of_scope,
        "mixer_templates": mixer_index["templates"],
        "actinides": actinides,
        "candidate_ids": candidate_ids,
    }


# ---------------------------------------------------------------------------
# classification primitives
# ---------------------------------------------------------------------------


def _fluids(recipe: dict[str, Any]) -> list[str]:
    out: list[str] = []
    for side in ("fluidInputs", "fluidOutputs"):
        for stack in recipe.get(side) or []:
            if isinstance(stack, dict) and isinstance(
                stack.get("fluid"), str
            ):
                out.append(stack["fluid"])
    return out


def _items(recipe: dict[str, Any]) -> list[dict[str, Any]]:
    out: list[dict[str, Any]] = []
    for side in ("inputs", "outputs"):
        for stack in recipe.get(side) or []:
            if isinstance(stack, dict) and stack.get("item"):
                out.append(stack)
    return out


def _input_items(recipe: dict[str, Any]) -> list[dict[str, Any]]:
    return [
        stack for stack in recipe.get("inputs") or []
        if isinstance(stack, dict) and stack.get("item")
    ]


def classify_recipe(
    map_name: str,
    recipe: dict[str, Any],
    ctx: dict[str, Any],
    ledger_1_shape: str | None = None,
) -> str:
    """Apply the ordered B1 rules to one recipe."""
    fluids = _fluids(recipe)
    items = _items(recipe)

    # v1_required promotion (rule 1): zero by universe construction;
    # validated by the caller against ledger-2 v1 units.
    # already_covered (rule 2)
    if any(
        ctx["a2_unknown_by_name"].get(name, {}).get("class")
        == "covered_by_petroleum_identity"
        for name in fluids
    ):
        return "already_covered"

    # ore_processing_byproduct (rule 3)
    if map_name == "gt.recipe.centrifuge":
        input_items = [
            str(s.get("item")) for s in _input_items(recipe)
        ]
        if any("crushedPurified" in item for item in input_items) and any(
            "crushedCentrifuged" in str(s.get("item")) for s in items
        ):
            return "ore_processing_byproduct"

    # generic_processing (rule 4)
    item_ids = [str(s.get("item")) for s in items]
    if map_name == "gt.recipe.compressor":
        if any("gt.meta.dust" in i for i in item_ids) and any(
            "plateGem" in i for i in item_ids
        ):
            return "generic_processing"
    if (
        map_name == "gt.recipe.electrolyzer"
        and ledger_1_shape == "composition_generated"
    ):
        return "generic_processing"

    # cross_mod_compat (rule 5)
    for name in fluids:
        a2 = ctx["a2_by_name"].get(name)
        if a2 is not None and a2["disposition"] == "no_cc_fluid":
            return "cross_mod_compat"
        unknown = ctx["a2_unknown_by_name"].get(name, {})
        if unknown.get("class") in (
            "out_of_scope_magic_compat",
            "equivalence_construction_foam_color",
        ):
            return "cross_mod_compat"

    # post_1_0_g10 (rule 6)
    if any("bulletGt" in i for i in item_ids) or any(
        ctx["a2_unknown_by_name"].get(name, {}).get("class", "").startswith(
            "out_of_scope_g10_"
        )
        for name in fluids
    ):
        return "post_1_0_g10"

    # post_1_0_nuclear (rule 7)
    metas = [
        int(s["meta"])
        for s in items
        if isinstance(s.get("meta"), int)
        and not isinstance(s.get("meta"), bool)
    ]
    touched_candidates = [
        m for m in metas if m in ctx["candidate_ids"]
    ]
    if touched_candidates and all(
        m in ctx["actinides"] for m in touched_candidates
    ):
        return "post_1_0_nuclear"

    # out_of_scope (rule 8)
    for s in items:
        item = str(s.get("item"))
        if item in ctx["a3_out_of_scope"]:
            return "out_of_scope"
        if ":" in item:
            namespace = item.split(":", 1)[0]
            if namespace not in BUILTIN_NAMESPACES:
                return "out_of_scope"
    for name in fluids:
        a2 = ctx["a2_by_name"].get(name)
        if a2 is not None and a2["disposition"] == "out_of_scope":
            return "out_of_scope"

    # ordinary_optional (rule 9): remainder
    return "ordinary_optional"


def classify_template(
    template: dict[str, Any],
    ctx: dict[str, Any],
) -> str:
    """Classify a mixer template from its skeleton (compact index)."""
    skeleton = template.get("skeleton") or {}
    recipe = {
        "fluidInputs": skeleton.get("fluidInputs"),
        "fluidOutputs": skeleton.get("fluidOutputs"),
        "inputs": skeleton.get("inputs"),
        "outputs": skeleton.get("outputs"),
    }
    # Template skeletons carry meta values that may be binding markers
    # ({"$support": ...}); the marker itself never drives a class, so
    # pass the skeleton through the same rules with meta normalization.
    return classify_recipe("gt.recipe.mixer", recipe, ctx)


# ---------------------------------------------------------------------------
# build
# ---------------------------------------------------------------------------


def build_from_dump() -> dict[str, Any]:
    ctx = load_context()
    ledger_2 = ctx["ledger_2"]
    classes = ledger_2["encoding"]["classes"]
    maps = ledger_2["encoding"]["maps"]

    units = [
        unit for unit in ledger_2["units"]
        if classes[unit[3]] == "ordinary_optional"
    ]
    expanded_rows = sum(len(unit[4]) for unit in units)
    expected_rows = ledger_2["counts"]["expanded_row_diagnostics"][
        "ordinary_optional"
    ]
    if expanded_rows != expected_rows:
        raise ValueError(
            "universe expansion mismatch: "
            f"{expanded_rows} rows vs ledger-2 {expected_rows}"
        )

    v1_identity: set[tuple[int, int]] = set()
    for unit in ledger_2["units"]:
        if classes[unit[3]] == "v1_required":
            v1_identity.update(
                (unit[1], index) for index in unit[4]
            )

    dump_cache: dict[int, list[Any]] = {}
    count_by_class: dict[str, int] = {c: 0 for c in CLASSES}
    per_map: dict[str, dict[str, int]] = {}
    non_mixer_rows: list[list[int]] = []
    mixer_template_rows: list[dict[str, Any]] = []

    for unit in units:
        map_index = unit[1]
        map_name = maps[map_index]
        row_count = len(unit[4])
        template_index = unit[2]

        if template_index is not None:
            template = ctx["mixer_templates"][template_index]
            klass = classify_template(template, ctx)
            mixer_template_rows.append(
                {
                    "template_id": template["template_id"],
                    "template_index": template_index,
                    "class": klass,
                    "rows": row_count,
                }
            )
        else:
            if map_index not in dump_cache:
                document = _load(DUMP / "maps" / f"{map_name}.json")
                dump_cache[map_index] = document.get("recipes") or []
            recipes = dump_cache[map_index]
            for recipe_index in unit[4]:
                recipe = recipes[recipe_index]
                shape = ctx["ledger_1_shapes"].get(
                    (map_index, recipe_index)
                )
                klass = classify_recipe(
                    map_name, recipe, ctx, ledger_1_shape=shape
                )
                if (
                    (map_index, recipe_index) in v1_identity
                    and klass == "ordinary_optional"
                ):
                    klass = "v1_required"
                non_mixer_rows.append(
                    [map_index, recipe_index, CLASSES.index(klass)]
                )
        count_by_class[klass] += row_count
        per_map.setdefault(map_name, {c: 0 for c in CLASSES})[
            klass
        ] += row_count

    # A zero bucket is a measured result and is kept verbatim — counts
    # are never erased by a status name (T22.5 B1 gate (b)).
    if sum(count_by_class.values()) != expanded_rows:
        raise ValueError("classification row sum != universe expansion")

    return {
        "schema_version": 1,
        "status": "T22_5_ROW_CLASSIFICATION_READY",
        "supersedes": {
            "target": "t21_template_denominator.json "
            "unit_classifications.ordinary_optional (classification "
            "vocabulary only)",
            "note": (
                "Ledger-2 is read-only. This artifact reclassifies the "
                "ordinary_optional expanded rows into the T22 nine-class "
                "vocabulary; counts are additive refinements, not "
                "rewrites."
            ),
        },
        "universe": {
            "ledger": _relative(LEDGER_2),
            "units": len(units),
            "expanded_rows": expanded_rows,
            "definition": (
                "ledger-2 ordinary_optional units expanded over "
                "source_recipe_indexes"
            ),
        },
        "rules": RULES,
        "classes": list(CLASSES),
        "counts": {
            "by_class": count_by_class,
            "unclassified": 0,
            "total": expanded_rows,
        },
        "per_map": dict(sorted(per_map.items())),
        "mixer_templates": mixer_template_rows,
        "non_mixer_rows": non_mixer_rows,
        "downstream_sync": {
            "t21_readiness.json": (
                "byte-identical asserted in tests (values-only load)"
            ),
            "t22_petroleum_denominator.json": (
                "untouched; its universe is petroleum_t22, disjoint "
                "from this one"
            ),
            "t22_family_manifest.json": (
                "family row sets must be a subset of the B1 classes; "
                "asserted in tests"
            ),
            "t22_readiness.json": "untouched",
            "C0": "per-map attribution feeds the playability audit",
            "C1": "class sums feed the four-column denominator",
            "full_verification_report.json": (
                "absorbed by the final T22.5 record"
            ),
        },
        "inputs": {
            _relative(LEDGER_2): _sha256(LEDGER_2),
            _relative(LEDGER_1): _sha256(LEDGER_1),
            _relative(A2_ARTIFACT): _sha256(A2_ARTIFACT),
            _relative(A3_ARTIFACT): _sha256(A3_ARTIFACT),
            _relative(MIXER_INDEX): _sha256(MIXER_INDEX),
            _relative(MATERIALS_DUMP): _sha256(MATERIALS_DUMP),
            _relative(BUILDER): _sha256(BUILDER),
        },
    }


def write() -> dict[str, Any]:
    for name in ("gt.recipe.bath.json", "gt.recipe.smelter.json",
                 "gt.recipe.mixer.json"):
        if not (DUMP / "maps" / name).is_file():
            raise OSError(
                f"missing dump map {name} (required for --write "
                "--full-replay)"
            )
    document = build_from_dump()
    OUTPUT.write_bytes(_stable(document).encode("utf-8"))
    return document


def check() -> list[str]:
    """Reference-only validation of the committed artifact."""
    errors: list[str] = []
    if not OUTPUT.is_file():
        errors.append(f"missing generated file: {_relative(OUTPUT)}")
        return errors
    on_disk = _load(OUTPUT)
    if on_disk.get("schema_version") != 1:
        errors.append("schema_version != 1")
    if on_disk.get("status") != "T22_5_ROW_CLASSIFICATION_READY":
        errors.append("status != T22_5_ROW_CLASSIFICATION_READY")
    counts = on_disk.get("counts") or {}
    if counts.get("unclassified") != 0:
        errors.append("unclassified != 0")
    if counts.get("total") != 146841:
        errors.append(
            f"counts.total != 146841 (got {counts.get('total')})"
        )
    by_class = counts.get("by_class") or {}
    if sum(by_class.values()) != 146841:
        errors.append("by_class sum != 146841")
    if set(by_class) != set(CLASSES):
        errors.append(
            f"by_class classes mismatch: {sorted(set(by_class))} vs "
            f"{sorted(CLASSES)}"
        )
    per_map = on_disk.get("per_map") or {}
    per_map_total = sum(
        sum(v.values()) for v in per_map.values()
    )
    if per_map_total != 146841:
        errors.append(f"per_map total != 146841 (got {per_map_total})")
    mixer_rows = sum(
        row["rows"] for row in on_disk.get("mixer_templates", [])
    )
    non_mixer_rows = len(on_disk.get("non_mixer_rows", []))
    if mixer_rows + non_mixer_rows != 146841:
        errors.append(
            "mixer + non-mixer row counts != 146841 "
            f"({mixer_rows} + {non_mixer_rows})"
        )
    ledger_2 = _load(LEDGER_2)
    expected = ledger_2["counts"]["expanded_row_diagnostics"][
        "ordinary_optional"
    ]
    if counts.get("total") != expected:
        errors.append("universe drift vs ledger-2")
    if on_disk.get("classes") != list(CLASSES):
        errors.append("classes list drifted")
    # Ledger-2 must not have been rewritten by this builder: compare its
    # committed hash against the hash this artifact recorded at write
    # time is covered by inputs staleness via rebuild; the read-only
    # property is asserted in the test module.
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--reference-only", action="store_true")
    parser.add_argument("--full-replay", action="store_true")
    args = parser.parse_args(argv)
    try:
        if args.check:
            errors = check()
            if errors:
                raise ValueError("; ".join(errors))
            document = _load(OUTPUT)
        elif args.write:
            if not args.full_replay:
                raise ValueError("--write requires --full-replay")
            document = write()
        else:
            parser.error("choose --check or --write")
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T22.5 row classification failed: {error}",
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
