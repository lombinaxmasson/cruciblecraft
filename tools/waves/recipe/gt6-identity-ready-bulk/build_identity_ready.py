#!/usr/bin/env python3
"""Publish identity-ready GT6 rows that the live hosts already accept.

Sluice is a new dump wave. Shredder, extruder, bath and the smaller maps keep
their sealed waves and add one on-demand group for rows that translate and
pass the Java host validator. Missing forms, missing fluids, host rejections
and live input collisions stay blocked. published + existing + blocked = source.
"""
from __future__ import annotations

import importlib.util
import json
import sys
from pathlib import Path
from typing import Any, Callable

ROOT = Path(__file__).resolve().parents[4]
TOOLS = ROOT / "tools"
WAVE = ROOT / "tools" / "waves" / "recipe" / "gt6-identity-ready-bulk"
CHEM_PATH = ROOT / "tools" / "waves" / "recipe" / "gt6-chemical-misc-bulk" / "build_chemical_misc.py"
SAMPLES = ROOT / "src" / "test" / "resources" / "gt6_identity_ready_samples.json"
COHORT = "identity_ready"
GENERATOR = "tools/waves/recipe/gt6-identity-ready-bulk/build_identity_ready.py"
UNBOUNDED = 2_147_483_647

for entry in (ROOT, TOOLS):
    if str(entry) not in sys.path:
        sys.path.insert(0, str(entry))

from tools import census_common as census  # noqa: E402


def load_chemical_misc():
    spec = importlib.util.spec_from_file_location("build_chemical_misc", CHEM_PATH)
    if spec is None or spec.loader is None:
        raise SystemExit(f"cannot load {CHEM_PATH}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    module.WAVE = WAVE
    module.COHORT = COHORT
    module.SAMPLES = SAMPLES
    module._slug = lambda spec: f"{spec['machine'].replace('_', '-')}/identity-ready"
    return module


def _shape(runtime: str) -> bool:
    return runtime.startswith("cruciblecraft:extruder_shape_")


def validators(chem) -> dict[str, Callable[[dict[str, Any]], str | None]]:
    def sluice(relation: dict[str, Any]) -> str | None:
        ii, io, fi, fo = chem._sizes(relation)
        eut = int(relation.get("eut") or 0)
        chances = chem._chances(relation)
        if (
            ii != 1
            or io < 1
            or io > 9
            or fi != 1
            or fo > 1
            or eut <= 0
            or eut > 1024
            or len(chances) != io
            or any(chance <= 0 or chance > chem.GUARANTEED for chance in chances)
        ):
            return "sluice_recipe_shape"
        for operand in relation.get("fluid_inputs") or []:
            amount = chem._fluid_amount(operand)
            if amount <= 0 or amount > 4_000:
                return "sluice_recipe_shape"
        for operand in relation.get("fluid_outputs") or []:
            amount = chem._fluid_amount(operand)
            if amount <= 0 or amount > UNBOUNDED:
                return "sluice_recipe_shape"
        return None

    def shredder(relation: dict[str, Any]) -> str | None:
        ii, io, fi, fo = chem._sizes(relation)
        eut = int(relation.get("eut") or 0)
        if ii > 1 or io > 4 or fi or fo or eut <= 0 or eut > 1024:
            return "configured_recipe_shape"
        return None

    def melter(relation: dict[str, Any]) -> str | None:
        return _open(relation, 1, 1, 1, 1, 1024, 4_000, "melter_recipe_shape", "melter_recipe_amount")

    def injector(relation: dict[str, Any]) -> str | None:
        return _open(relation, 2, 1, 2, 1, 8192, 4_000, "injector_recipe_shape", "injector_recipe_amount")

    def implosion(relation: dict[str, Any]) -> str | None:
        ii, io, fi, fo = chem._sizes(relation)
        actions = chem._actions(relation)
        chances = chem._chances(relation)
        if (
            ii != 3
            or io != 1
            or fi
            or fo
            or int(relation.get("eut") or 0) != 0
            or int(relation.get("duration") or 0) != 256
            or chances != [chem.GUARANTEED]
            or len(actions) != 3
            or actions[0][0] != "CONSUME"
            or actions[1][0] != "CONSUME"
            or actions[2] != ("PRESERVE", 0)
            or actions[0][1] <= 0
            or actions[1][1] <= 0
        ):
            return "implosion_compressor_recipe_shape"
        return None

    def coke_oven(relation: dict[str, Any]) -> str | None:
        ii, io, _fi, fo = chem._sizes(relation)
        actions = chem._actions(relation)
        if ii < 1 or io < 1 or io > 9 or fo > 1 or not actions or actions[0][1] <= 0:
            return "coke_oven_recipe_shape"
        for operand in relation.get("fluid_outputs") or []:
            amount = chem._fluid_amount(operand)
            if amount <= 0 or amount > 64_000:
                return "coke_oven_recipe_amount"
        return None

    def extruder(relation: dict[str, Any]) -> str | None:
        ii, io, fi, fo = chem._sizes(relation)
        eut = int(relation.get("eut") or 0)
        actions = chem._actions(relation)
        inputs = list(relation.get("item_inputs") or [])
        if ii != 2 or io > 2 or fi or fo:
            return "component_recipe_shape"
        if eut <= 0 or eut > 256:
            return "component_recipe_energy"
        if (
            len(actions) != 2
            or actions[0][0] != "CONSUME"
            or actions[0][1] <= 0
            or actions[1] != ("PRESERVE", 0)
            or len(inputs) != 2
            or not _shape(chem._runtime(inputs[1]))
        ):
            return "extruder_tool_shape"
        if any(kind == "WEAR" for kind, _count in actions):
            return "wear_tool_shape"
        if any(count < 0 or count > 64 for _kind, count in actions):
            return "component_recipe_amount"
        if any(chem._output_count(operand) <= 0 for operand in relation.get("item_outputs") or []):
            return "component_recipe_amount"
        if any(chance <= 0 or chance > chem.GUARANTEED for chance in chem._chances(relation)):
            return "component_recipe_amount"
        return None

    def _open(
        relation: dict[str, Any],
        item_in: int,
        item_out: int,
        fluid_in: int,
        fluid_out: int,
        max_eut: int,
        fluid_cap: int,
        shape: str,
        amount: str,
    ) -> str | None:
        ii, io, fi, fo = chem._sizes(relation)
        eut = int(relation.get("eut") or 0)
        if ii > item_in or io > item_out or fi > fluid_in or fo > fluid_out or eut <= 0 or eut > max_eut:
            return shape
        if ii == 0 and fi == 0:
            return shape
        if io == 0 and fo == 0:
            return shape
        for operand in relation.get("fluid_inputs") or []:
            value = chem._fluid_amount(operand)
            if value <= 0 or value > fluid_cap:
                return amount
        for operand in relation.get("fluid_outputs") or []:
            value = chem._fluid_amount(operand)
            if value <= 0 or value > UNBOUNDED:
                return amount
        return None

    return {
        "sluice": sluice,
        "shredder": shredder,
        "bath": chem.chemical(
            6, 6, 1, 3, 4_000, UNBOUNDED, preserve=False, time_energy=True, max_eut=1024
        ),
        "melter": melter,
        "injector": injector,
        "implosion_compressor": implosion,
        "canner": chem.chemical(2, 1, 1, 1, 128_000, UNBOUNDED, preserve=True, max_eut=1024),
        "coke_oven": coke_oven,
        "autoclave": chem.chemical(
            2, 3, 1, 1, 4_000_000, UNBOUNDED, preserve=True, time_energy=True, max_eut=1024
        ),
        "extruder": extruder,
    }


def specs(chem) -> list[dict[str, Any]]:
    checks = validators(chem)
    rows = (
        ("gt.recipe.autoclave", "autoclave", "cruciblecraft:autoclave"),
        ("gt.recipe.cokeoven", "coke_oven", None),
        ("gt.recipe.canner", "canner", "cruciblecraft:canner"),
        ("gt.recipe.implosioncompressor", "implosion_compressor", None),
        ("gt.recipe.injector", "injector", "cruciblecraft:injector"),
        ("gt.recipe.melter", "melter", "cruciblecraft:melter"),
        ("gt.recipe.bath", "bath", "cruciblecraft:bath"),
        ("gt.recipe.shredder", "shredder", "cruciblecraft:shredder"),
        ("gt.recipe.sluice", "sluice", "cruciblecraft:sluice"),
        ("gt.recipe.extruder", "extruder", "cruciblecraft:extruder"),
    )
    built = []
    for dump, machine, block in rows:
        built.append(
            {
                "block": block,
                "dump": dump,
                "machine": machine,
                "validators": ((machine, checks[machine]),),
            }
        )
    return built


def retarget_families(chem, spec: dict[str, Any]) -> None:
    """Keep authored family ids off the sealed ``#bulk_`` namespace."""
    directory = chem._map_dir(spec)
    work_path = directory / "source_pack" / "work_set.json"
    if not work_path.is_file():
        return
    document = json.loads(work_path.read_text(encoding="utf-8"))
    for family in document.get("families") or []:
        family_id = str(family["family_id"]).replace("#bulk_", "#identity_ready_", 1)
        family["family_id"] = family_id
        family["template_key"] = family_id
    work_path.write_text(
        json.dumps(document, separators=(",", ":")),
        encoding="utf-8",
    )
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
