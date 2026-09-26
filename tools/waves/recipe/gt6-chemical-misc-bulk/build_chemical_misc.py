#!/usr/bin/env python3
"""Import fourteen GT6 chemical, thermal and misc maps as on-demand exact families.

Rows that the runtime already proves (dump hash or identical translated IO) stay
on their existing waves and are counted as ``existing``. The rest go through the
same host validators Java runs at load; rows those reject, rows that miss an
object, and rows whose inputs collide with a live recipe stay blocked.
``published + existing + blocked = source`` per map.
"""
from __future__ import annotations

import hashlib
import json
import shutil
import sys
import zipfile
from collections import defaultdict
from pathlib import Path
from typing import Any, Callable

ROOT = Path(__file__).resolve().parents[4]
TOOLS = ROOT / "tools"
COVERAGE = TOOLS / "waves" / "portfolio" / "gt6-full-coverage-reassessment"
for entry in (ROOT, TOOLS, COVERAGE):
    if str(entry) not in sys.path:
        sys.path.insert(0, str(entry))

import build_semantic_coverage as coverage  # noqa: E402

from tools import census_common as census  # noqa: E402
from tools import centrifuge_common as centrifuge  # noqa: E402
from tools.bath_remainder_identities import VANILLA_COLOR_META_ITEMS  # noqa: E402
from tools.build_assembler_source import _source_row_hash  # noqa: E402
from tools.recipe_bulk import source_import  # noqa: E402
from tools.recipe_bulk.compile import _consume_identity  # noqa: E402
from tools.recipe_bulk.dialects import gt6  # noqa: E402
from tools.recipe_bulk.emit import emit_action, emit_fluid, emit_item, emit_item_output  # noqa: E402
from tools.recipe_bulk.membership import membership_root  # noqa: E402
from tools.recipe_bulk.pilot import reviewed_lock  # noqa: E402
from tools.waves.prep.machine_prep_common import (  # noqa: E402
    missing_runtime_item_operands,
    prefix_serialized_names,
    registered_runtime_ids,
    ungated_material_form_operands,
)

SOURCE_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
WAVE = ROOT / "tools" / "waves" / "recipe" / "gt6-chemical-misc-bulk"
DUMP_ROOT = ROOT / "gt6_dump" / "gt6_recipe_dump" / "maps"
POLICY_ROOT = (
    ROOT / "src/recipe_generated/resources/data/cruciblecraft/recipe/publication_policy"
)
SAMPLES = ROOT / "src" / "test" / "resources" / "gt6_chemical_misc_samples.json"
COHORT = "chemical_misc"
HOLDER = 2048
# Shape groups smaller than this share mixed plain-relation holders instead of
# becoming one tiny matrix holder each.
MATRIX_MIN_ROWS = 16
MIXED_HOLDER = 512
# Longer samples only check the live match; the GameTest runs the rest.
SAMPLE_MAX_TICKS = 400
GUARANTEED = 10000
BRONZE_TANK = 32_000
GT6_PANEL_TANK = 64_000
GT6_SMELTER_FLUID_OUTPUT = 13_032
GT6_CENTRIFUGE_FLUID_INPUT = 200_000


# --------------------------------------------------------------------------
# Host validators. Each mirrors a Java validator in ModProcessingMachines; a
# map lists every machine spec that serves it, and a row must pass them all.
# --------------------------------------------------------------------------


def _chances(relation: dict[str, Any]) -> list[int]:
    """Match emit.py: pad short chance lists and drop empty-slot padding."""
    outputs = list(relation.get("item_outputs") or [])
    chances = [int(value) for value in (relation.get("output_chances") or [])]
    if not outputs:
        return chances
    if len(chances) < len(outputs):
        return chances + [GUARANTEED] * (len(outputs) - len(chances))
    return chances[: len(outputs)]


def _fluid_amount(operand: dict[str, Any]) -> int:
    source = operand.get("source") or {}
    return int(source.get("amount") or operand.get("amount") or 0)


def _output_count(operand: dict[str, Any]) -> int:
    source = operand.get("source") or {}
    return int(source.get("count") or operand.get("count") or 1)


def _sizes(relation: dict[str, Any]) -> tuple[int, int, int, int]:
    return (
        len(relation.get("item_inputs") or []),
        len(relation.get("item_outputs") or []),
        len(relation.get("fluid_inputs") or []),
        len(relation.get("fluid_outputs") or []),
    )


def _actions(relation: dict[str, Any]) -> list[tuple[str, int]]:
    actions = list(relation.get("item_input_actions") or [])
    counts = [int(value) for value in (relation.get("item_input_counts") or [])]
    return [
        (str(action.get("kind") or "CONSUME").upper(), count)
        for action, count in zip(actions, counts, strict=False)
    ]


def chemical(
    item_in: int,
    item_out: int,
    fluid_in: int,
    fluid_out: int,
    fluid_in_cap: int,
    fluid_out_cap: int,
    *,
    preserve: bool,
    time_energy: bool = False,
    max_eut: int = 1024,
) -> Callable[[dict[str, Any]], str | None]:
    """``validateChemicalRecipe``."""

    def check(relation: dict[str, Any]) -> str | None:
        ii, io, fi, fo = _sizes(relation)
        if ii > item_in or io > item_out or fi > fluid_in or fo > fluid_out:
            return "chemical_recipe_shape"
        for kind, count in _actions(relation):
            if kind == "CONSUME" and count > 0:
                continue
            if preserve and kind == "PRESERVE" and count == 0:
                continue
            return "chemical_recipe_input_action"
        eut = int(relation.get("eut") or 0)
        duration = int(relation.get("duration") or 0)
        time_zero = time_energy and eut == 0 and duration > 0
        if not time_zero and (eut <= 0 or eut > max_eut):
            return "chemical_recipe_energy"
        if any(count > 64 for _kind, count in _actions(relation)):
            return "chemical_recipe_amount"
        if any(_output_count(op) <= 0 for op in relation.get("item_outputs") or []):
            return "chemical_recipe_amount"
        if any(c <= 0 or c > GUARANTEED for c in _chances(relation)):
            return "chemical_recipe_amount"
        for op in relation.get("fluid_inputs") or []:
            if _fluid_amount(op) <= 0 or _fluid_amount(op) > fluid_in_cap:
                return "chemical_recipe_amount"
        for op in relation.get("fluid_outputs") or []:
            if _fluid_amount(op) <= 0 or _fluid_amount(op) > fluid_out_cap:
                return "chemical_recipe_amount"
        return None

    return check


def mixer(relation: dict[str, Any]) -> str | None:
    """``validateMixer``: bronze envelope when it fits, GT6 panel otherwise."""
    ii, io, fi, fo = _sizes(relation)
    fluids = list(relation.get("fluid_inputs") or []) + list(relation.get("fluid_outputs") or [])
    if (
        ii <= 4
        and io <= 1
        and fi <= 3
        and fo <= 2
        and all(_fluid_amount(op) <= BRONZE_TANK for op in fluids)
    ):
        return chemical(4, 1, 3, 2, BRONZE_TANK, BRONZE_TANK, preserve=True)(relation)
    return chemical(6, 1, 6, 2, GT6_PANEL_TANK, GT6_PANEL_TANK, preserve=True)(relation)


def generifier(relation: dict[str, Any]) -> str | None:
    """GENERIFIER: one item to one item, or one fluid to one fluid, at 0 EU/t."""
    ii, io, fi, fo = _sizes(relation)
    eut = int(relation.get("eut") or 0)
    duration = int(relation.get("duration") or 0)
    item_row = ii == 1 and io == 1 and fi == 0 and fo == 0 and duration > 0
    fluid_row = ii == 0 and io == 0 and fi == 1 and fo == 1 and duration == 1
    if eut != 0 or not (item_row or fluid_row):
        return "generifier_recipe_shape"
    if item_row and _actions(relation) != [("CONSUME", _actions(relation)[0][1])]:
        return "generifier_recipe_shape"
    if item_row and not (0 < _actions(relation)[0][1] <= 64):
        return "generifier_recipe_shape"
    return None


def loom(relation: dict[str, Any]) -> str | None:
    """``validateLoomRecipe``."""
    ii, io, fi, fo = _sizes(relation)
    eut = int(relation.get("eut") or 0)
    if ii == 0 or ii > 6 or io != 1 or fi or fo or eut <= 0 or eut > 8192:
        return "loom_recipe_shape"
    return None


def laser_engraver(relation: dict[str, Any]) -> str | None:
    """``validateLaserRecipe``: a consumed workpiece and a preserved lens."""
    ii, io, fi, fo = _sizes(relation)
    eut = int(relation.get("eut") or 0)
    duration = int(relation.get("duration") or 0)
    if ii != 2 or io != 1 or fi or fo or eut not in (16, 256) or duration <= 0:
        return "laser_recipe_shape"
    actions = _actions(relation)
    if (
        len(actions) != 2
        or actions[0][1] <= 0
        or actions[1][1] != 0
        or actions[0][0] != "CONSUME"
        or actions[1][0] != "PRESERVE"
    ):
        return "laser_recipe_lens"
    return None


def nanofab(relation: dict[str, Any]) -> str | None:
    """``validateNanofabRecipe``."""
    ii, io, fi, fo = _sizes(relation)
    eut = int(relation.get("eut") or 0)
    if ii > 2 or io > 1 or fi > 1 or fo > 1 or eut <= 0 or eut > 8192:
        return "nanofab_recipe_shape"
    fluids = list(relation.get("fluid_inputs") or []) + list(relation.get("fluid_outputs") or [])
    if any(_fluid_amount(op) > 4000 for op in fluids):
        return "nanofab_recipe_amount"
    return None


MAPS: tuple[dict[str, Any], ...] = (
    {
        "dump": "gt.recipe.mixer",
        "machine": "mixer",
        "block": "cruciblecraft:mixer",
        "validators": (
            ("mixer", mixer),
            (
                "electric_mixer",
                chemical(6, 1, 6, 2, GT6_PANEL_TANK, GT6_PANEL_TANK, preserve=True),
            ),
        ),
    },
    {
        "dump": "gt.recipe.generifier",
        "machine": "generifier",
        "block": "cruciblecraft:generifier",
        "validators": (("generifier", generifier),),
    },
    {
        "dump": "gt.recipe.smelter",
        "machine": "smelter",
        "block": "cruciblecraft:smelter",
        "validators": (("smelter", chemical(1, 4, 1, 1, 4_000, GT6_SMELTER_FLUID_OUTPUT, preserve=False)),),
    },
    {
        "dump": "gt.recipe.centrifuge",
        "machine": "centrifuge",
        "block": "cruciblecraft:centrifuge",
        "validators": (
            (
                "centrifuge",
                chemical(1, 6, 1, 6, GT6_CENTRIFUGE_FLUID_INPUT, GT6_PANEL_TANK, preserve=False, max_eut=4096),
            ),
        ),
    },
    {
        "dump": "gt.recipe.fermenter",
        "machine": "fermenter",
        "block": None,
        "validators": (
            (
                "fermenter",
                chemical(1, 1, 1, 1, 32_000, 32_000, preserve=True, max_eut=4096),
            ),
        ),
    },
    {
        "dump": "gt.recipe.freezer",
        "machine": "freezer",
        "block": "cruciblecraft:steel_galvanized_freezer",
        "validators": (("freezer", chemical(1, 1, 1, 1, 32_000, 32_000, preserve=True)),),
    },
    {
        "dump": "gt.recipe.distillery",
        "machine": "distillery",
        "block": "cruciblecraft:distillery",
        "validators": (("distillery", chemical(2, 2, 2, 3, 8_000, 8_000, preserve=True)),),
    },
    {
        "dump": "gt.recipe.polarizer",
        "machine": "polarizer",
        "block": "cruciblecraft:steel_galvanized_polarizer",
        "validators": (("polarizer", chemical(1, 1, 0, 0, 32_000, 32_000, preserve=False)),),
    },
    {
        "dump": "gt.recipe.loom",
        "machine": "loom",
        "block": "cruciblecraft:loom",
        "validators": (("loom", loom), ("electricloom", loom)),
    },
    {
        "dump": "gt.recipe.laserengraver",
        "machine": "laser_engraver",
        "block": "cruciblecraft:laser_engraver",
        "validators": (("laser_engraver", laser_engraver),),
    },
    {
        "dump": "gt.recipe.magneticseparator",
        "machine": "magnetic_separator",
        "block": "cruciblecraft:steel_galvanized_magnetic_separator",
        "validators": (
            ("magnetic_separator", chemical(1, 6, 1, 1, 32_000, 32_000, preserve=False)),
        ),
    },
    {
        "dump": "gt.recipe.compressor",
        "machine": "compressor",
        "block": "cruciblecraft:compressor",
        "validators": (("compressor", chemical(1, 1, 0, 0, 32_000, 32_000, preserve=False)),),
    },
    {
        "dump": "gt.recipe.electrolyzer",
        "machine": "electrolyzer",
        "block": "cruciblecraft:electrolyzer",
        "validators": (
            ("electrolyzer", chemical(2, 6, 2, 3, 32_000, 32_000, preserve=True)),
        ),
    },
    {
        "dump": "gt.recipe.nanofab",
        "machine": "nanofab",
        "block": "cruciblecraft:nanofab",
        "validators": (("nanofab", nanofab),),
    },
)


def _slug(spec: dict[str, Any]) -> str:
    return f"{spec['machine'].replace('_', '-')}/chemical-misc"


def _target(spec: dict[str, Any]) -> str:
    return f"cruciblecraft:{spec['machine']}"


def _group(spec: dict[str, Any]) -> str:
    return f"{_target(spec)}/{COHORT}"


def _map_dir(spec: dict[str, Any]) -> Path:
    return WAVE / spec["machine"]


def _rel(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


# --------------------------------------------------------------------------
# What the runtime already has, per CC map, excluding this card's output.
# --------------------------------------------------------------------------


def _own_output(path: Path) -> bool:
    return f"/{COHORT}/" in path.as_posix() or path.name.endswith(f"_{COHORT}.json")


def _input_key(sig: tuple) -> tuple:
    return (sig[0], sig[2])


CIRCUIT = "cruciblecraft:programmed_circuit"


def _circuit_blind_key(sig: tuple) -> tuple | None:
    """Input key with circuit numbers dropped, or None when no circuit is an input.

    A live ingredient without ``circuit_config`` accepts every circuit, so it
    captures numbered rows that Java's exact shadow check does not compare.
    """
    items = sig[0]
    if not any(str(name).startswith(CIRCUIT) for name, _count in items):
        return None
    blind = tuple(
        sorted(
            ((CIRCUIT if str(name).startswith(CIRCUIT) else name), count)
            for name, count in items
        )
    )
    return (blind, sig[2])


def live_index(cc_maps: set[str]) -> dict[str, dict[str, Any]]:
    """Proven rows, logical IO signatures and input keys per CC map."""
    from tools.recipe_bulk.matrix import authored_relations

    rec = coverage.load_reconciliation()
    every_file = rec.runtime_recipe_files
    rec.runtime_recipe_files = lambda: (p for p in every_file() if not _own_output(p))
    comparator = coverage.load_comparator()
    comparator.clear_process_caches()
    materials = comparator.cached_cc_materials()
    index: dict[str, dict[str, Any]] = {
        name: {
            "hashes": set(),
            "refs": set(),
            "io": defaultdict(set),
            "inputs": {},
            "any_circuit": {},
        }
        for name in cc_maps
    }
    rows, _rules = rec.scan_cc_recipes()
    for row in rows:
        entry = index.get(row.cc_map)
        if entry is None:
            continue
        entry["hashes"].update(row.full)
        if row.dump_ref is not None:
            entry["refs"].add(row.dump_ref)
    for path in rec.runtime_recipe_files():
        try:
            document = coverage.load_json(path)
        except (OSError, json.JSONDecodeError):
            continue
        if not isinstance(document, dict):
            continue
        kind = document.get("type")
        if kind == coverage.GT_RECIPE:
            target, relations = document.get("map"), [document]
        elif kind == coverage.COMPACT_FAMILY:
            target, relations = document.get("target_map"), authored_relations(document)
        else:
            continue
        if not isinstance(target, str) or ":" not in target:
            continue
        entry = index.get(target.split(":", 1)[1])
        if entry is None:
            continue
        label = path.relative_to(ROOT).as_posix()
        for relation in relations:
            sig, numbers = coverage.runtime_relation_signature(relation)
            entry["io"][sig].add(numbers)
            entry["inputs"].setdefault(_input_key(sig), label)
            if any(name == CIRCUIT for name, _count in sig[0]):
                entry["any_circuit"].setdefault(_circuit_blind_key(sig), label)
    for recipe in comparator.cached_expanded_cc_recipes():
        name = str(recipe.map_name)
        if recipe.source != "cc" or not name.startswith("cruciblecraft:"):
            continue
        entry = index.get(name.split(":", 1)[1])
        if entry is None:
            continue
        sig, numbers = coverage.rule_signature(recipe, materials)
        entry["io"][sig].add(numbers)
        rule = recipe.raw_hint or recipe.family
        entry["inputs"].setdefault(_input_key(sig), f"material_rule {rule} ({recipe.material})")
    return index


# --------------------------------------------------------------------------
# Row filters shared with the prefix-regular card.
# --------------------------------------------------------------------------


def _runtime(operand: dict[str, Any]) -> str:
    return str(operand.get("runtime_id") or operand.get("value") or "")


def _uses_lossy_alias(relation: dict[str, Any]) -> bool:
    for operand in list(relation.get("item_inputs") or []) + list(
        relation.get("item_outputs") or []
    ):
        if not operand.get("alias"):
            continue
        source = operand.get("source") or {}
        meta = str(source.get("meta") or 0)
        if not meta.isdigit():
            continue
        if (str(source.get("item") or ""), int(meta)) in centrifuge.FIXTURE_ONLY_LOSSY_ITEM_ALIASES:
            return True
    return False


def _color_wildcard_item(relation: dict[str, Any]) -> str:
    for key in ("item_inputs", "item_outputs"):
        for operand in relation.get(key) or []:
            source = operand.get("source") or {}
            item = str(source.get("item") or "")
            if source.get("meta") == "*" and item in VANILLA_COLOR_META_ITEMS:
                return item
    return ""


_VANILLA_ITEM_MODELS: set[str] | None = None


def _vanilla_item_models() -> set[str]:
    """1.21.1 item ids from the local client-extra jar; legacy 1.7 ids are not in it."""
    global _VANILLA_ITEM_MODELS
    if _VANILLA_ITEM_MODELS is None:
        jars = sorted((ROOT / "build" / "moddev" / "artifacts").glob("neoforge-*-client-extra-*.jar"))
        if not jars:
            raise SystemExit("build/moddev client-extra jar is missing; run a Gradle build first")
        prefix = "assets/minecraft/models/item/"
        with zipfile.ZipFile(jars[-1]) as archive:
            _VANILLA_ITEM_MODELS = {
                "minecraft:" + name[len(prefix) : -5]
                for name in archive.namelist()
                if name.startswith(prefix) and name.endswith(".json")
            }
    return _VANILLA_ITEM_MODELS


def _unknown_vanilla_item(relation: dict[str, Any]) -> str:
    models = _vanilla_item_models()
    for key in ("item_inputs", "item_outputs"):
        for operand in relation.get(key) or []:
            runtime = _runtime(operand)
            if runtime.startswith("minecraft:") and runtime not in models:
                return runtime
    return ""


_REGISTERED_FLUIDS: set[str] | None = None


def _registered_fluid_ids() -> set[str]:
    global _REGISTERED_FLUIDS
    if _REGISTERED_FLUIDS is None:
        gate = census.load_json(
            ROOT / "src" / "main" / "resources" / "census" / "runtime_registry_gate.json"
        )
        _REGISTERED_FLUIDS = set((gate.get("categories") or {}).get("fluids") or [])
    return _REGISTERED_FLUIDS


def _unregistered_fluid(relation: dict[str, Any]) -> str:
    fluids = _registered_fluid_ids()
    for key in ("fluid_inputs", "fluid_outputs"):
        for operand in relation.get(key) or []:
            runtime = _runtime(operand)
            if runtime.startswith("minecraft:"):
                continue
            if runtime not in fluids:
                return runtime
    return ""


def _identity(relation: dict[str, Any], slug: str) -> str:
    emitted = {
        "duration": int(relation.get("duration") or 0),
        "eut": int(relation.get("eut") or 0),
        "fluid_inputs": [emit_fluid(op) for op in relation.get("fluid_inputs") or []],
        "fluid_outputs": [emit_fluid(op) for op in relation.get("fluid_outputs") or []],
        "item_input_actions": [
            emit_action(action) for action in relation.get("item_input_actions") or []
        ],
        "item_input_counts": list(relation.get("item_input_counts") or []),
        "item_inputs": [emit_item(op) for op in relation.get("item_inputs") or []],
        "item_outputs": [emit_item_output(op) for op in relation.get("item_outputs") or []],
        "output_chances": _chances(relation),
        "special_value": int(relation.get("special_value") or 0),
    }
    return _consume_identity(emitted, wave_id=slug)


def _shape(relation: dict[str, Any]) -> tuple[Any, ...]:
    return (
        int(relation.get("duration") or 0),
        int(relation.get("eut") or 0),
        int(relation.get("special_value") or 0),
        tuple(int(value) for value in relation.get("item_input_counts") or []),
        json.dumps(relation.get("item_input_actions") or [], sort_keys=True),
        tuple(int(value) for value in relation.get("output_chances") or []),
        _sizes(relation),
        bool(relation.get("can_be_buffered", True)),
    )


def _block(blocked: list[dict[str, str]], index: int, digest: str, reason: str) -> None:
    blocked.append(
        {"reason": reason, "source_recipe_index": str(index), "source_row_sha256": digest}
    )


# --------------------------------------------------------------------------
# Selection
# --------------------------------------------------------------------------


def select_map(
    spec: dict[str, Any], maps: dict[str, Any], live: dict[str, Any]
) -> dict[str, int]:
    directory = _map_dir(spec)
    pack = directory / "source_pack"
    pack.mkdir(parents=True, exist_ok=True)
    dump_path = DUMP_ROOT / f"{spec['dump']}.json"
    slice_path = pack / "dump_slice.json"
    if not slice_path.is_file() or slice_path.stat().st_size != dump_path.stat().st_size:
        shutil.copyfile(dump_path, slice_path)
    recipes = json.loads(slice_path.read_text(encoding="utf-8"))["recipes"]
    slug = _slug(spec)
    target = _target(spec)
    label = spec["machine"]
    groups: dict[tuple[Any, ...], list[tuple[int, str]]] = defaultdict(list)
    blocked: list[dict[str, str]] = []
    existing: list[dict[str, str]] = []
    seen_digest: set[str] = set()
    seen_identity: set[str] = set()
    seen_inputs: dict[tuple, str] = dict(live["inputs"])
    for index, recipe in enumerate(recipes):
        digest = _source_row_hash(recipe)
        if digest in seen_digest:
            _block(blocked, index, digest, "duplicate source row")
            continue
        seen_digest.add(digest)
        if digest in live["hashes"] or (spec["dump"], index) in live["refs"]:
            existing.append(
                {"class": "source_exact", "source_recipe_index": str(index), "source_row_sha256": digest}
            )
            continue
        try:
            relation, errors = gt6.compile_row(
                recipe,
                host=target,
                target_map=target,
                source_map=spec["dump"],
                family_id=f"{spec['dump']}#{COHORT}",
                template_key=f"{spec['dump']}#{COHORT}",
                recipe_index=index,
                shadow_order=0,
                source_revision=SOURCE_REVISION,
                source_row_sha256=digest,
                maps=maps,
            )
        except ValueError as failure:
            _block(blocked, index, digest, str(failure))
            continue
        if errors or source_import._relation_unmapped(relation):
            _block(blocked, index, digest, errors[0] if errors else "unmapped operand")
            continue
        translated, error = coverage.translate_row(recipe, spec["dump"], index, digest, maps)
        if translated is None:
            _block(blocked, index, digest, str(error))
            continue
        sig, numbers = translated
        if numbers in live["io"].get(sig, ()):
            existing.append(
                {"class": "translated_exact", "source_recipe_index": str(index), "source_row_sha256": digest}
            )
            continue
        if _uses_lossy_alias(relation):
            _block(blocked, index, digest, "lossy fixture alias is not a GT6 operand")
            continue
        unsupported = list(relation.get("unsupported_semantics") or [])
        if unsupported:
            _block(blocked, index, digest, f"{label} host rejected {unsupported[0]}")
            continue
        rejection = next(
            (
                f"{machine} host rejected {reason}"
                for machine, check in spec["validators"]
                if (reason := check(relation))
            ),
            None,
        )
        if rejection:
            _block(blocked, index, digest, rejection)
            continue
        unregistered = missing_runtime_item_operands(relation)
        unregistered.extend(ungated_material_form_operands(relation))
        unknown_vanilla = _unknown_vanilla_item(relation)
        if unknown_vanilla:
            unregistered.append(f"unregistered runtime item {unknown_vanilla}")
        fluid = _unregistered_fluid(relation)
        if fluid:
            unregistered.append(f"unregistered runtime fluid {fluid}")
        if unregistered:
            _block(blocked, index, digest, unregistered[0])
            continue
        wildcard = _color_wildcard_item(relation)
        if wildcard:
            _block(blocked, index, digest, f"unsplit vanilla color wildcard {wildcard}")
            continue
        try:
            identity = _identity(relation, slug)
        except ValueError as failure:
            _block(blocked, index, digest, str(failure))
            continue
        if identity in seen_identity:
            _block(blocked, index, digest, "duplicate runtime identity")
            continue
        key = _input_key(sig)
        if key in seen_inputs:
            owner = seen_inputs[key]
            if sig in live["io"]:
                reason = f"live recipe {owner} has the same IO with other duration or EU/t"
            elif owner.isdigit():
                reason = f"shadowed input signature by source row {owner}"
            else:
                reason = f"shadowed input signature by live recipe {owner}"
            _block(blocked, index, digest, reason)
            continue
        blind = _circuit_blind_key(sig)
        if blind is not None and blind in live["any_circuit"]:
            _block(
                blocked,
                index,
                digest,
                "live recipe " + live["any_circuit"][blind]
                + " takes an unnumbered programmed circuit with the same inputs",
            )
            continue
        seen_identity.add(identity)
        seen_inputs[key] = str(index)
        groups[_shape(relation)].append((index, digest))
    chunks: list[list[tuple[int, str]]] = []
    pooled: list[tuple[int, str]] = []
    for rows in groups.values():
        if len(rows) < MATRIX_MIN_ROWS:
            pooled.extend(rows)
            continue
        chunks.extend(rows[start : start + HOLDER] for start in range(0, len(rows), HOLDER))
    pooled.sort()
    chunks.extend(
        pooled[start : start + MIXED_HOLDER] for start in range(0, len(pooled), MIXED_HOLDER)
    )
    families: list[dict[str, Any]] = []
    for family_index, chunk in enumerate(chunks):
        family_id = f"{spec['dump']}#bulk_{family_index:04d}"
        families.append(
            {
                "family_id": family_id,
                "relations": [
                    {
                        "shadow_order": order,
                        "source_recipe_index": row_index,
                        "source_row_sha256": row_digest,
                    }
                    for order, (row_index, row_digest) in enumerate(chunk)
                ],
                "template_key": family_id,
            }
        )
    published = sum(len(chunk) for chunk in chunks)
    family_index = len(chunks)
    if published + len(existing) + len(blocked) != len(recipes):
        raise SystemExit(f"{slug}: accounting does not cover every source row")
    work_path = pack / "work_set.json"
    work_path.write_text(
        json.dumps(
            {
                "accounting": {
                    "blocked_rows": len(blocked),
                    "existing_rows": len(existing),
                    "published_rows": published,
                    "selection_rule": (
                        "host-accepted translated rows not already proven live; "
                        "missing objects, host rejections and live input collisions stay blocked"
                    ),
                    "source_rows": len(recipes),
                },
                "families": families,
            },
            separators=(",", ":"),
        ),
        encoding="utf-8",
    )
    census.write_stable(
        directory / "blocked.json",
        {"rows": blocked, "schema_version": 1, "source_rows": len(recipes)},
    )
    census.write_stable(
        directory / "existing.json",
        {"rows": existing, "schema_version": 1, "source_rows": len(recipes)},
    )
    manifest_path = directory / "source_pack_manifest.json"
    census.write_stable(
        manifest_path,
        {
            "files": [
                {"path": _rel(slice_path), "role": "dump_slice", "sha256": _sha256(slice_path)},
                {"path": _rel(work_path), "role": "work_set", "sha256": _sha256(work_path)},
            ],
            "full_replay": {"required_for_first_generation": True, "skip_is_not_pass": True},
            "provenance_policy": {"append_only": True, "forbid_gt6u": True},
            "schema_version": 1,
            "source_dialect": "gt6",
            "source_pack_id": slug,
            "source_revision": SOURCE_REVISION,
            "source_system": "gt6",
        },
    )
    census.write_stable(
        directory / "recipe_import.json",
        {
            "family_membership_source": {"kind": "work_set", "path": _rel(work_path)},
            "host": target,
            "import_slug": slug,
            "operand_authorities": [
                {"id": "identity_ledger_v3", "kind": "data"},
                {"id": "material_form_authority", "kind": "data"},
                {"adapter": "gt6", "kind": "source_dialect"},
            ],
            "output_paths": {
                "lock_candidate": _rel(directory / "lock_candidate.json"),
                "receipt": _rel(directory / "source_receipt.json"),
                "review": _rel(directory / "source_review.json"),
                "source": _rel(directory / "source.json"),
            },
            "representation_policy": {"allowed": ["exact", "exact_multi"]},
            "schema_version": 1,
            "selection_rule": {"kind": "work_set_members"},
            "source_maps": [spec["dump"]],
            "source_pack": _rel(manifest_path),
            "stable_id_policy": {
                "algorithm": "source_system_revision_family_relation",
                "include_card_number": False,
            },
            "target_map": target,
        },
    )
    print(
        f"{slug}: source {len(recipes)} published {published} existing {len(existing)} "
        f"blocked {len(blocked)} families {family_index}",
        flush=True,
    )
    return {
        "blocked": len(blocked),
        "existing": len(existing),
        "families": family_index,
        "published": published,
        "source": len(recipes),
    }


def finish_map(spec: dict[str, Any], counts: dict[str, int]) -> dict[str, Any] | None:
    directory = _map_dir(spec)
    slug = _slug(spec)
    group = _group(spec)
    policy_path = POLICY_ROOT / f"{spec['machine']}_{COHORT}.json"
    if not counts["published"]:
        for stale in (
            directory / "lock_candidate.json",
            directory / "production_lock.json",
            policy_path,
        ):
            stale.unlink(missing_ok=True)
        _write_proof(directory, counts)
        return None
    source_import.write_import(directory / "recipe_import.json")
    source = json.loads((directory / "source.json").read_text(encoding="utf-8"))
    relations = list(source["relations"])
    candidate = census.load_json(directory / "lock_candidate.json")
    lock = reviewed_lock(candidate, publication_group=group, cohort=COHORT)
    lock["generated_by"] = "tools/waves/recipe/gt6-chemical-misc-bulk/build_chemical_misc.py"
    lock["note"] = (
        f"live compile for {slug}; host-accepted translated rows not already live; "
        "missing objects stay blocked; on-demand matrix publication"
    )
    census.write_stable(directory / "production_lock.json", lock)
    family_ids = [str(row["template_key"]) for row in lock["production"]["families"]]
    stable_ids = [sid for row in lock["production"]["families"] for sid in row["stable_ids"]]
    census.write_stable(
        policy_path,
        {
            "cache_ceiling": 16,
            "eager_stable_ids": [],
            "family_count": len(family_ids),
            "membership_root_sha256": membership_root(family_ids, stable_ids),
            "policy_type": "on_demand",
            "publication_group": group,
            "relation_count": len(stable_ids),
            "routing_schema_version": "compact-shard-v1",
            "target_map": _target(spec),
            "type": "cruciblecraft:compact_publication_policy",
        },
    )
    if len(relations) != counts["published"]:
        raise SystemExit(f"{slug}: source.json has {len(relations)} of {counts['published']} rows")
    _write_proof(directory, counts)
    print(f"{slug}: locked {len(relations)}", flush=True)
    return _samples(spec, relations)


def _write_proof(directory: Path, counts: dict[str, int]) -> None:
    census.write_stable(
        directory / "coverage_proof.json",
        {
            "blocked_rows": counts["blocked"],
            "existing_rows": counts["existing"],
            "published_rows": counts["published"],
            "schema_version": 1,
            "source_rows": counts["source"],
        },
    )


# --------------------------------------------------------------------------
# GameTest samples
# --------------------------------------------------------------------------


def _simple(relation: dict[str, Any]) -> bool:
    """Rows a GameTest can insert: registered items, at least one item input."""
    if not relation.get("item_inputs"):
        return False
    registered = registered_runtime_ids()
    shared = {f"cruciblecraft:{name}" for name in prefix_serialized_names()}
    fluids = _registered_fluid_ids()
    for operand in list(relation.get("item_inputs") or []) + list(
        relation.get("item_outputs") or []
    ):
        if "tag" in emit_item(operand):
            return False
        emitted = emit_item_output(operand)
        runtime = str(emitted["id"])
        if runtime.startswith("minecraft:") or runtime in registered:
            continue
        if not (emitted.get("components") and runtime in shared):
            return False
    for operand in relation.get("fluid_inputs") or []:
        runtime = _runtime(operand)
        if not runtime.startswith("minecraft:") and runtime not in fluids:
            return False
    return True


def _stack_spec(operand: dict[str, Any], count: int) -> dict[str, Any]:
    emitted = emit_item_output(operand)
    spec: dict[str, Any] = {"count": count, "id": str(emitted["id"])}
    components = emitted.get("components") or {}
    material = str(components.get("cruciblecraft:prefix_material") or "")
    if material:
        spec["material"] = material
    if "cruciblecraft:circuit_config" in components:
        spec["circuit"] = int(components["cruciblecraft:circuit_config"])
    return spec


def _sample(role: str, relation: dict[str, Any], block: str | None) -> dict[str, Any]:
    counts = [int(value) for value in relation.get("item_input_counts") or []]
    duration = int(relation.get("duration") or 0)
    chances = _chances(relation)
    guaranteed = [
        _stack_spec(output, _output_count(output))
        for index, output in enumerate(relation.get("item_outputs") or [])
        if (chances[index] if index < len(chances) else GUARANTEED) == GUARANTEED
    ]
    return {
        "duration": duration,
        "eut": int(relation.get("eut") or 0),
        "execute": block is not None and duration <= SAMPLE_MAX_TICKS,
        "fluids": [
            {"amount": _fluid_amount(fluid), "id": _runtime(fluid)}
            for fluid in relation.get("fluid_inputs") or []
        ],
        "guaranteed_outputs": guaranteed,
        "items": [
            _stack_spec(item, max(1, counts[index] if index < len(counts) else 1))
            for index, item in enumerate(relation.get("item_inputs") or [])
        ],
        "role": role,
        "special_value": int(relation.get("special_value") or 0),
    }


def _samples(spec: dict[str, Any], relations: list[dict[str, Any]]) -> dict[str, Any]:
    simple = [row for row in relations if _simple(row)]
    pool = sorted(
        simple,
        key=lambda row: (
            0 if _sample("", row, None)["guaranteed_outputs"] else 1,
            int(row.get("duration") or 0),
            int(row.get("source_recipe_index") or 0),
        ),
    )
    picked = pool[:1]
    quick = [row for row in pool if int(row.get("duration") or 0) <= SAMPLE_MAX_TICKS]
    widest = max(quick or pool, key=lambda row: len(row.get("item_inputs") or []), default=None)
    if widest is not None and widest not in picked:
        picked.append(widest)
    return {
        "block": spec["block"],
        "machine": spec["machine"],
        "publication_group": _group(spec),
        "samples": [
            _sample(role, row, spec["block"])
            for role, row in zip(("short", "widest"), picked, strict=False)
        ],
        "target_map": _target(spec),
    }


def _write_samples(sample_maps: list[dict[str, Any]]) -> None:
    payload = {"maps": sample_maps, "schema_version": 1}
    census.write_stable(SAMPLES, payload)
    census.write_stable(WAVE / "samples.json", payload)
    print(f"samples {sum(len(row['samples']) for row in sample_maps)}", flush=True)


def rewrite_samples() -> None:
    sample_maps = []
    for spec in MAPS:
        source = _map_dir(spec) / "source.json"
        if not source.is_file():
            continue
        relations = json.loads(source.read_text(encoding="utf-8"))["relations"]
        sample = _samples(spec, relations)
        if sample["samples"]:
            sample_maps.append(sample)
    _write_samples(sample_maps)


def main() -> None:
    if "--samples-only" in sys.argv:
        rewrite_samples()
        return
    wanted = {arg for arg in sys.argv[1:] if not arg.startswith("-")}
    selected = [spec for spec in MAPS if not wanted or spec["machine"] in wanted]
    if not selected:
        raise SystemExit("no matching maps")
    maps = coverage.translator_maps()
    live = live_index({spec["machine"] for spec in selected})
    sample_maps = []
    for spec in selected:
        counts = select_map(spec, maps, live[spec["machine"]])
        sample = finish_map(spec, counts)
        if sample and sample["samples"]:
            sample_maps.append(sample)
    if not wanted:
        _write_samples(sample_maps)


if __name__ == "__main__":
    main()
