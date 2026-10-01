#!/usr/bin/env python3
"""Publish the small translatable remainders on maps that still have rule files.

The coverage hint says "补材料规则模板" because those maps already have
material rules. The remaining rows are not a 16-wide same-material cartesian,
so this card publishes the dump rows the Java hosts already accept. It does
not invent a new material_rule expansion.
"""
from __future__ import annotations

import importlib.util
import json
import sys
from pathlib import Path
from typing import Any, Callable

ROOT = Path(__file__).resolve().parents[4]
TOOLS = ROOT / "tools"
WAVE = ROOT / "tools" / "waves" / "recipe" / "gt6-material-rule-remainder"
CHEM_PATH = ROOT / "tools" / "waves" / "recipe" / "gt6-chemical-misc-bulk" / "build_chemical_misc.py"
SAMPLES = ROOT / "src" / "test" / "resources" / "gt6_material_rule_remainder_samples.json"
COHORT = "rule_remainder"
GENERATOR = "tools/waves/recipe/gt6-material-rule-remainder/build_rule_remainder.py"
UNBOUNDED = 2_147_483_647
SHAPE_TAG = ROOT / "src/generated/resources/data/cruciblecraft/tags/item/extruder_shapes.json"

for entry in (ROOT, TOOLS):
    if str(entry) not in sys.path:
        sys.path.insert(0, str(entry))

from tools import census_common as census  # noqa: E402


def load_chemical_misc():
    spec = importlib.util.spec_from_file_location("build_chemical_misc", CHEM_PATH)
    if spec is None or spec.loader is None:
        raise SystemExit(f"cannot load {CHEM_PATH}")
    module = importlib.util.module_from_spec(spec)
    sys.modules["build_chemical_misc_rule_remainder"] = module
    spec.loader.exec_module(module)
    module.WAVE = WAVE
    module.COHORT = COHORT
    module.SAMPLES = SAMPLES
    module._slug = lambda spec: f"{spec['machine'].replace('_', '-')}/rule-remainder"
    return module


def host_shapes() -> frozenset[str]:
    values = json.loads(SHAPE_TAG.read_text(encoding="utf-8"))["values"]
    return frozenset(str(value) for value in values)


def _shape_token(chem, operand: dict[str, Any], shapes: frozenset[str]) -> str:
    runtime = chem._runtime(operand)
    if runtime in shapes:
        return runtime
    material = str(operand.get("material") or "")
    form = str(operand.get("form") or "")
    if material and form:
        slash = f"cruciblecraft:{material}/{form}"
        if slash in shapes:
            return slash
    return runtime


def _emitted_chances(chem, relation: dict[str, Any], outputs: int) -> list[int]:
    """Match emit.py: pad short lists, and drop chances once item outputs are gone."""
    chances = [int(value) for value in (relation.get("output_chances") or [])]
    if outputs and len(chances) < outputs:
        chances = chances + [chem.GUARANTEED] * (outputs - len(chances))
    elif len(chances) > outputs:
        chances = chances[:outputs]
    return chances


def _chances_ok(chem, relation: dict[str, Any], outputs: int) -> bool:
    chances = _emitted_chances(chem, relation, outputs)
    return len(chances) == outputs and all(0 < chance <= chem.GUARANTEED for chance in chances)


def validators(chem) -> dict[str, Callable[[dict[str, Any]], str | None]]:
    shapes = host_shapes()

    def extruder(relation: dict[str, Any]) -> str | None:
        inputs = list(relation.get("item_inputs") or [])
        outputs = list(relation.get("item_outputs") or [])
        actions = chem._actions(relation)
        if relation.get("fluid_inputs") or relation.get("fluid_outputs"):
            return "component_recipe_shape"
        if len(inputs) != 2 or not outputs or len(outputs) > 2 or len(actions) != 2:
            return "component_recipe_shape"
        eut = int(relation.get("eut") or 0)
        if eut <= 0 or eut > 512:
            return "component_recipe_energy"
        if (
            actions[0][0] != "CONSUME"
            or actions[0][1] <= 0
            or actions[1] != ("PRESERVE", 0)
            or _shape_token(chem, inputs[1], shapes) not in shapes
        ):
            return "extruder_tool_shape"
        if any(kind == "WEAR" for kind, _count in actions):
            return "wear_tool_shape"
        if any(count < 0 or count > 64 for _kind, count in actions):
            return "component_recipe_amount"
        if any(chem._output_count(operand) <= 0 for operand in outputs):
            return "component_recipe_amount"
        if not _chances_ok(chem, relation, len(outputs)):
            return "component_recipe_amount"
        return None

    def press(relation: dict[str, Any]) -> str | None:
        ii, io, fi, fo = chem._sizes(relation)
        actions = chem._actions(relation)
        eut = int(relation.get("eut") or 0)
        if ii > 3 or io < 1 or io > 1 or fi or fo:
            return "component_recipe_shape"
        if eut <= 0 or eut > 256:
            return "component_recipe_energy"
        if any(kind == "WEAR" for kind, _count in actions):
            return "wear_tool_shape"
        for kind, count in actions:
            if count < 0 or count > 64 or (count == 0 and kind != "PRESERVE"):
                return "component_recipe_amount"
        if any(chem._output_count(operand) <= 0 for operand in relation.get("item_outputs") or []):
            return "component_recipe_amount"
        if not _chances_ok(chem, relation, io):
            return "component_recipe_amount"
        return None

    def squeezer(relation: dict[str, Any]) -> str | None:
        """Small host plus the large host after fluid-only rows are allowed."""
        ii, io, fi, fo = chem._sizes(relation)
        eut = int(relation.get("eut") or 0)
        if ii != 1 or io > 2 or fi or fo > 1 or (io == 0 and fo == 0) or eut <= 0 or eut > 4096:
            return "squeezer_recipe_shape"
        if not _chances_ok(chem, relation, io):
            return "squeezer_recipe_shape"
        for operand in relation.get("fluid_outputs") or []:
            amount = chem._fluid_amount(operand)
            if amount <= 0 or amount > UNBOUNDED:
                return "squeezer_recipe_shape"
        return None

    def crusher(relation: dict[str, Any]) -> str | None:
        ii, io, fi, fo = chem._sizes(relation)
        eut = int(relation.get("eut") or 0)
        if ii != 1 or io < 1 or io > 12 or fi or fo or eut <= 0 or eut > 1024:
            return "crusher_recipe_shape"
        if not _chances_ok(chem, relation, io):
            return "crusher_recipe_shape"
        return None

    return {
        "extruder": extruder,
        "press": press,
        "squeezer": squeezer,
        "injector": _injector(chem),
        "smelter": chem.chemical(1, 4, 1, 1, 4_000, UNBOUNDED, preserve=False, max_eut=1024),
        "bath": chem.chemical(
            6, 6, 1, 3, 2_880_000, UNBOUNDED, preserve=False, time_energy=True, max_eut=1024
        ),
        "crusher": crusher,
        "fermenter": chem.chemical(
            1, 1, 1, 1, 32_000, UNBOUNDED, preserve=True, max_eut=4096
        ),
        "massfab": chem.chemical(2, 1, 1, 1, 32_000, UNBOUNDED, preserve=True, max_eut=8192),
    }


def _injector(chem) -> Callable[[dict[str, Any]], str | None]:
    def check(relation: dict[str, Any]) -> str | None:
        ii, io, fi, fo = chem._sizes(relation)
        eut = int(relation.get("eut") or 0)
        if ii > 2 or io > 1 or fi > 2 or fo > 1 or eut <= 0 or eut > 8192:
            return "injector_recipe_shape"
        if ii == 0 and fi == 0:
            return "injector_recipe_shape"
        if io == 0 and fo == 0:
            return "injector_recipe_shape"
        for operand in relation.get("fluid_inputs") or []:
            amount = chem._fluid_amount(operand)
            if amount <= 0 or amount > 41_472:
                return "injector_recipe_amount"
        for operand in relation.get("fluid_outputs") or []:
            amount = chem._fluid_amount(operand)
            if amount <= 0 or amount > UNBOUNDED:
                return "injector_recipe_amount"
        return None

    return check


def specs(chem) -> list[dict[str, Any]]:
    checks = validators(chem)
    rows = (
        ("gt.recipe.extruder", "extruder", "cruciblecraft:extruder"),
        ("gt.recipe.press", "press", "cruciblecraft:press"),
        ("gt.recipe.squeezer", "squeezer", "cruciblecraft:squeezer"),
        ("gt.recipe.injector", "injector", "cruciblecraft:injector"),
        ("gt.recipe.smelter", "smelter", "cruciblecraft:smelter"),
        ("gt.recipe.bath", "bath", "cruciblecraft:bath"),
        ("gt.recipe.crusher", "crusher", "cruciblecraft:bronze_crusher"),
        ("gt.recipe.fermenter", "fermenter", None),
        ("gt.recipe.massfab", "massfab", None),
    )
    return [
        {
            "block": block,
            "dump": dump,
            "machine": machine,
            "validators": ((machine, checks[machine]),),
        }
        for dump, machine, block in rows
    ]


def retarget_families(chem, spec: dict[str, Any]) -> None:
    directory = chem._map_dir(spec)
    work_path = directory / "source_pack" / "work_set.json"
    if not work_path.is_file():
        return
    document = json.loads(work_path.read_text(encoding="utf-8"))
    for family in document.get("families") or []:
        family_id = str(family["family_id"]).replace("#bulk_", "#rule_remainder_", 1)
        family["family_id"] = family_id
        family["template_key"] = family_id
    work_path.write_text(json.dumps(document, separators=(",", ":")), encoding="utf-8")
    manifest_path = directory / "source_pack_manifest.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    digest = chem._sha256(work_path)
    for entry in manifest.get("files") or []:
        if entry.get("role") == "work_set":
            entry["sha256"] = digest
    census.write_stable(manifest_path, manifest)


def patch_lock(chem, spec: dict[str, Any]) -> None:
    path = chem._map_dir(spec) / "production_lock.json"
    if not path.is_file():
        return
    lock = json.loads(path.read_text(encoding="utf-8"))
    lock["generated_by"] = GENERATOR
    lock["note"] = (
        f"live compile for {chem._slug(spec)}; host-accepted translated rows not already live; "
        "missing objects and host rejections stay blocked; on-demand matrix publication"
    )
    census.write_stable(path, lock)


def forced_sample(chem, spec: dict[str, Any], counts: dict[str, int]) -> dict[str, Any] | None:
    if not counts["published"]:
        return None
    source_path = chem._map_dir(spec) / "source.json"
    relations = json.loads(source_path.read_text(encoding="utf-8"))["relations"]
    if not relations:
        return None
    relation = relations[0]
    sample = chem._sample("short", relation, spec["block"])
    if not relation.get("item_inputs"):
        sample["execute"] = False
    return {
        "block": spec["block"],
        "machine": spec["machine"],
        "publication_group": chem._group(spec),
        "published_rows": counts["published"],
        "samples": [sample],
        "target_map": chem._target(spec),
    }


def main() -> None:
    chem = load_chemical_misc()
    selected = specs(chem)
    wanted = {arg for arg in sys.argv[1:] if not arg.startswith("-")}
    if wanted:
        selected = [spec for spec in selected if spec["machine"] in wanted]
    if not selected:
        raise SystemExit("no matching maps")
    print("loading translator maps", flush=True)
    maps = chem.coverage.translator_maps()
    print("indexing live recipes", flush=True)
    live = chem.live_index({spec["machine"] for spec in selected})
    sample_maps = []
    for spec in selected:
        print(f"selecting {spec['dump']}", flush=True)
        counts = chem.select_map(spec, maps, live[spec["machine"]])
        retarget_families(chem, spec)
        sample = chem.finish_map(spec, counts)
        patch_lock(chem, spec)
        if sample and sample.get("samples"):
            sample["published_rows"] = counts["published"]
        elif counts["published"]:
            sample = forced_sample(chem, spec, counts)
        if sample and sample.get("samples"):
            sample_maps.append(sample)
        print(
            f"{spec['machine']} source {counts['source']} published {counts['published']} "
            f"existing {counts['existing']} blocked {counts['blocked']}",
            flush=True,
        )
    if sample_maps and not wanted:
        payload = {"maps": sample_maps, "schema_version": 1}
        census.write_stable(SAMPLES, payload)
        census.write_stable(WAVE / "samples.json", payload)
        print(f"samples {sum(len(row['samples']) for row in sample_maps)}", flush=True)


if __name__ == "__main__":
    main()
