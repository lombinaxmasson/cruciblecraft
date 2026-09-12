#!/usr/bin/env python3
"""Build prep-only Source Packs, D0 matrices, and GT6 art for issued machines.

Skips roll-former and cluster-mill, which have their own builders.
Does not write live ``src/recipe_generated`` or touch landing registries.
"""
from __future__ import annotations

import argparse
import sys
from typing import Any

import importlib.util
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))
if str(ROOT / "tools") not in sys.path:
    sys.path.insert(1, str(ROOT / "tools"))

from tools import census_common as census
from tools import technological_parts_foundation as parts
from tools.gt6_resolve import resolve

_COMMON_PATH = Path(__file__).with_name("machine_prep_common.py")
_COMMON_SPEC = importlib.util.spec_from_file_location(
    "machine_prep_common", _COMMON_PATH
)
assert _COMMON_SPEC is not None and _COMMON_SPEC.loader is not None
common = importlib.util.module_from_spec(_COMMON_SPEC)
_COMMON_SPEC.loader.exec_module(common)

SOURCE_REVISION = common.SOURCE_REVISION
LockNote = (
    "prep isolated compile; not a live RecipeMap import; not player_complete"
)

CIRCUITS = {
    1: "cruciblecraft:circuit_basic",
    2: "cruciblecraft:circuit_good",
    3: "cruciblecraft:circuit_advanced",
    4: "cruciblecraft:circuit_elite",
    5: "cruciblecraft:circuit_master",
    6: "cruciblecraft:circuit_ultimate",
}
MOTORS = dict(parts.MOTORS)
PISTONS = dict(parts.PISTONS)


def _write(path: Path, document: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    census.write_stable(path, document)


def _ok(gt6: str, cc: str) -> dict[str, str]:
    return {"cc": cc, "gt6": gt6, "status": "ok"}


def _blocked(gt6: str, reason: str) -> dict[str, str]:
    return {"cc": "", "gt6": gt6, "reason": reason, "status": "blocked"}


def _form(token: str, *, prefer: tuple[str, ...] = ()) -> dict[str, str]:
    result = resolve(token)
    status = result.get("status")
    form = result.get("form") or {}
    if result.get("kind") == "prefix_family":
        items = list(form.get("items") or [])
        for name in prefer:
            for row in items:
                if row.get("cc_material") == name and row.get("registered"):
                    return _ok(token, str(row["item"]))
        for row in items:
            if row.get("registered"):
                return _ok(token, str(row["item"]))
        return _blocked(token, "family member form missing")
    item = form.get("item")
    if status == "ok" and item:
        return _ok(token, str(item))
    return _blocked(token, str(status or "unmapped"))


def _circuit(tier: int) -> dict[str, str]:
    item = CIRCUITS[tier]
    return _ok(f"OD_CIRCUITS[{tier}]", item)


def _motor(tier: int) -> dict[str, str]:
    token = f"IL.MOTORS[{tier}]"
    item = MOTORS.get(tier)
    if item:
        return _ok(token, item)
    return _blocked(token, "compact motor module missing for this tier")


def _piston(tier: int) -> dict[str, str]:
    token = f"IL.PISTONS[{tier}]"
    item = PISTONS.get(tier)
    if item:
        return _ok(token, item)
    return _blocked(token, "compact piston module missing for this tier")


def _conveyor(tier: int) -> dict[str, str]:
    token = f"IL.CONVEYERS[{tier}]"
    item = parts.CONVEYERS.get(tier)
    if item:
        return _ok(token, item)
    return _blocked(
        token,
        "compact conveyor module missing; cover is not this slot",
    )


def _host_status(slots: dict[str, dict[str, str]]) -> str:
    if all(slot.get("status") == "ok" for slot in slots.values()):
        return "source_exact"
    return "explicitly_blocked"


def _matrix(slug: str, grid: list[str], hosts: list[dict[str, Any]], status: str) -> dict[str, Any]:
    return {
        "capability_slug": slug,
        "grid": grid,
        "hosts": hosts,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": status,
    }


def d0_slicer() -> dict[str, Any]:
    hosts = []
    materials = (
        (20381, "steel_galvanized", "SteelGalvanized", 1),
        (20382, "aluminium", "Al", 2),
        (20383, "stainless_steel", "StainlessSteel", 3),
        (20384, "chromium", "Cr", 4),
        (20385, "titanium", "Ti", 5),
    )
    for host, _cc, mt, tier in materials:
        slots = {
            "casing": _form(f"OP.casingMachine(MT.{mt})"),
            "circuit": _circuit(tier),
            "conveyor": _conveyor(tier),
            "piston": _piston(tier),
            "rod": _form(f"OP.stick(MT.{mt})"),
        }
        hosts.append(
            {
                "host": host,
                "material": slots["casing"]["cc"].split(":")[-1].split("/")[0]
                if slots["casing"]["cc"]
                else _cc,
                **slots,
                "status": _host_status(slots),
            }
        )
    return _matrix("machines/slicer", ["PRw", "YMC"], hosts, "prep_runtime_ready")


def d0_loom() -> dict[str, Any]:
    kinetic = []
    for host, mt, prefer in (
        (20211, "Bronze", ("bronze",)),
        (20212, "Steel", ("steel",)),
        (20213, "Ti", ("titanium",)),
        (20214, "TungstenSteel", ("tungstensteel",)),
    ):
        token_mat = "ANY.Steel" if host == 20212 else f"MT.{mt}"
        slots = {
            "casing": _form(
                f"OP.casingMachine({token_mat})",
                prefer=("steel",) if host == 20212 else (),
            ),
            "gear": _form(
                f"OP.gearGt({token_mat})",
                prefer=("steel",) if host == 20212 else (),
            ),
            "long_rod": _form(
                f"OP.stickLong({token_mat})",
                prefer=("steel",) if host == 20212 else (),
            ),
        }
        kinetic.append(
            {
                "drive": "kinetic",
                "host": host,
                **slots,
                "status": _host_status(slots),
            }
        )
    electric = []
    for host, mt, tier in (
        (20361, "SteelGalvanized", 1),
        (20362, "Al", 2),
        (20363, "StainlessSteel", 3),
        (20364, "Cr", 4),
        (20365, "Ti", 5),
    ):
        slots = {
            "casing": _form(f"OP.casingMachine(MT.{mt})"),
            "long_rod": _form(f"OP.stickLong(MT.{mt})"),
            "motor": _motor(tier),
        }
        electric.append(
            {
                "drive": "electric",
                "host": host,
                **slots,
                "status": _host_status(slots),
            }
        )
    return _matrix(
        "machines/loom",
        ["ShS", "GMG", "SwS"],
        kinetic + electric,
        "prep_runtime_ready",
    )


def d0_pressure_washer() -> dict[str, Any]:
    pipes = {
        20551: "OP.pipeSmall(MT.StainlessSteel)",
        20552: "OP.pipeMedium(MT.StainlessSteel)",
        20553: "OP.pipeLarge(MT.StainlessSteel)",
        20554: "OP.pipeHuge(MT.StainlessSteel)",
    }
    hosts = []
    for host, mt, prefer in (
        (20551, "Bronze", ()),
        (20552, "Steel", ("steel",)),
        (20553, "Ti", ()),
        (20554, "TungstenSteel", ()),
    ):
        token_mat = "ANY.Steel" if host == 20552 else f"MT.{mt}"
        slots = {
            "casing": _form(f"OP.casingMachine({token_mat})", prefer=prefer),
            "pipe": _form(pipes[host]),
            "rotor": _form("OP.rotor(MT.StainlessSteel)"),
            "small_gear": _form(f"OP.gearGtSmall({token_mat})", prefer=prefer),
        }
        hosts.append({"host": host, **slots, "status": _host_status(slots)})
    return _matrix("machines/pressure-washer", ["RPG", "wMG"], hosts, "prep_runtime_ready")


def d0_injector() -> dict[str, Any]:
    cables = {
        1: "OP.cableGt01(MT.Sn)",
        2: "OP.cableGt01(ANY.Cu)",
        3: "OP.cableGt01(MT.Au)",
        4: "OP.cableGt01(MT.Al)",
        5: "OP.cableGt01(MT.Pt)",
    }
    pipes = {
        1: "OP.pipeTiny(MT.StainlessSteel)",
        2: "OP.pipeSmall(MT.StainlessSteel)",
        3: "OP.pipeMedium(MT.StainlessSteel)",
        4: "OP.pipeLarge(MT.StainlessSteel)",
        5: "OP.pipeHuge(MT.StainlessSteel)",
    }
    hosts = []
    for host, mt, tier, prefer in (
        (20261, "SteelGalvanized", 1, ()),
        (20262, "Al", 2, ("copper", "annealed_copper")),
        (20263, "StainlessSteel", 3, ()),
        (20264, "Cr", 4, ()),
        (20265, "Ti", 5, ()),
    ):
        slots = {
            "cable": _form(cables[tier], prefer=prefer),
            "casing": _form(f"OP.casingMachine(MT.{mt})"),
            "circuit": _circuit(tier),
            "pipe": _form(pipes[tier]),
            "piston": _piston(tier),
        }
        hosts.append({"host": host, **slots, "status": _host_status(slots)})
    return _matrix("machines/injector", ["XPw", "CMW"], hosts, "prep_runtime_ready")


def d0_printer() -> dict[str, Any]:
    cable_tokens = {
        1: ("OP.cableGt01(MT.Sn)", ()),
        2: ("OP.cableGt01(ANY.Cu)", ("copper", "annealed_copper")),
        3: ("OP.cableGt01(MT.Au)", ()),
        4: ("OP.cableGt01(MT.Al)", ()),
        5: ("OP.cableGt01(MT.Pt)", ()),
    }
    hosts = []
    for host, mt, tier in (
        (20271, "SteelGalvanized", 1),
        (20272, "Al", 2),
        (20273, "StainlessSteel", 3),
        (20274, "Cr", 4),
        (20275, "Ti", 5),
    ):
        token, prefer = cable_tokens[tier]
        slots = {
            "cable": _form(token, prefer=prefer),
            "casing": _form(f"OP.casingMachine(MT.{mt})"),
            "circuit": _circuit(tier),
            "conveyor": _conveyor(tier),
            "pipe": _form("OP.pipeTiny(MT.StainlessSteel)"),
        }
        hosts.append({"host": host, **slots, "status": _host_status(slots)})
    return _matrix(
        "machines/printer",
        ["CPC", "wXh", "WMW"],
        hosts,
        "prep_runtime_ready",
    )


def d0_laminator() -> dict[str, Any]:
    hosts = []
    for host, token, prefer in (
        (20391, "ANY.Steel", ("steel",)),
        (20392, "MT.Invar", ()),
        (20393, "MT.Ti", ()),
        (20394, "MT.TungstenCarbide", ()),
    ):
        slots = {
            "casing": _form(f"OP.casingMachine({token})", prefer=prefer),
            "copper_double_plate": _form(
                "OP.plateDouble(ANY.Cu)", prefer=("copper", "annealed_copper")
            ),
            "rod": _form(f"OP.stick({token})", prefer=prefer),
            "small_gear": _form(f"OP.gearGtSmall({token})", prefer=prefer),
        }
        hosts.append({"host": host, **slots, "status": _host_status(slots)})
    return _matrix(
        "machines/laminator",
        ["SwS", "GMG", "SCS"],
        hosts,
        "prep_runtime_ready",
    )


def d0_melter() -> dict[str, Any]:
    slots = {
        "bricks": _ok("Blocks.brick_block", "minecraft:bricks"),
        "casing": _form("OP.casingMachine(ANY.Iron)", prefer=("iron", "steel")),
        "copper_double_plate": _form(
            "OP.plateDouble(ANY.Cu)", prefer=("copper", "annealed_copper")
        ),
        "crucible": _ok("aRegistry.getItem(1005)", "cruciblecraft:crucible"),
        "pipe": _form("OP.pipeMedium(ANY.Iron)", prefer=("iron", "steel")),
    }
    host = {"host": 22010, **slots, "status": _host_status(slots)}
    return _matrix(
        "machines/melter",
        ["wUh", "PMP", "BCB"],
        [host],
        "prep_runtime_ready",
    )


def d0_sanding() -> dict[str, Any]:
    hosts = []
    for host, token, prefer in (
        (20511, "MT.Bronze", ()),
        (20512, "ANY.Steel", ("steel",)),
        (20513, "MT.Ti", ()),
        (20514, "MT.TungstenSteel", ()),
    ):
        slots = {
            "casing": _form(f"OP.casingMachineDouble({token})", prefer=prefer),
            "gear": _form(f"OP.gearGt({token})", prefer=prefer),
            "sandstone": _ok("OD.sandstone", "minecraft:sandstone"),
            "small_gear": _form(f"OP.gearGtSmall({token})", prefer=prefer),
        }
        hosts.append({"host": host, **slots, "status": _host_status(slots)})
    return _matrix(
        "machines/sanding",
        ["SGS", "XXX", "wMh"],
        hosts,
        "prep_runtime_ready",
    )


def d0_oven() -> dict[str, Any]:
    hosts = []
    for host, token, prefer in (
        (20001, "ANY.Steel", ("steel",)),
        (20002, "MT.Invar", ()),
        (20003, "MT.Ti", ()),
        (20004, "MT.TungstenCarbide", ()),
    ):
        slots = {
            "bricks": _ok("Blocks.brick_block", "minecraft:bricks"),
            "casing": _form(f"OP.casingMachine({token})", prefer=prefer),
            "copper_double_plate": _form(
                "OP.plateDouble(ANY.Cu)",
                prefer=("copper", "annealed_copper"),
            ),
        }
        hosts.append({"host": host, **slots, "status": _host_status(slots)})
    return _matrix(
        "machines/oven",
        ["wMh", "BCB"],
        hosts,
        "prep_runtime_ready",
    )


def d0_nanofab() -> dict[str, Any]:
    hosts = []
    for host, mt, tier in (
        (20441, "SteelGalvanized", 1),
        (20442, "Al", 2),
        (20443, "StainlessSteel", 3),
        (20444, "Cr", 4),
        (20445, "Ti", 5),
    ):
        slots = {
            "argon_laser": _blocked(
                "IL.Comp_Laser_Gas_Ar",
                "laser gas component missing; Geiger/Canner Ar cell is not this slot",
            ),
            "casing": _form(f"OP.casingMachine(MT.{mt})"),
            "circuit": _circuit(6),
            "emitter": _blocked(
                f"IL.EMITTERS[{tier}]", "compact emitter module missing"
            ),
            "krypton_laser": _blocked("IL.Comp_Laser_Gas_Kr", "laser gas component missing"),
            "sapphire_processor": _blocked(
                "IL.Processor_Crystal_Sapphire",
                "sapphire crystal processor missing",
            ),
            "sensor": _blocked(
                f"IL.SENSORS[{tier}]", "compact sensor module missing"
            ),
            "xenon_laser": _blocked("IL.Comp_Laser_Gas_Xe", "laser gas component missing"),
        }
        hosts.append({"host": host, **slots, "status": _host_status(slots)})
    return _matrix("machines/nanofab", ["KAX", "ZMY", "CSC"], hosts, "prep_runtime_ready")


MACHINES: dict[str, dict[str, Any]] = {
    "slicer": {
        "slug": "slicer",
        "source_map": "gt.recipe.slicer",
        "target_map": "cruciblecraft:slicer",
        "host": "cruciblecraft:slicer",
        "source_rows": 33,
        "live_needle": "slicer",
        "art": {
            "gt6_folder": "slicer",
            "cc_folder": "slicer",
            "gui_source": "Slicer.png",
            "gui_dest_stems": ("slicer",),
            "manifest": "gt6_slicer_art_manifest.json",
        },
        "d0": d0_slicer,
        "extra_art": (),
    },
    "loom": {
        "slug": "loom",
        "source_map": "gt.recipe.loom",
        "target_map": "cruciblecraft:loom",
        "host": "cruciblecraft:loom",
        "source_rows": 1334,
        "live_needle": "loom",
        "art": {
            "gt6_folder": "loom",
            "cc_folder": "loom",
            "gui_source": "Loom.png",
            "gui_dest_stems": ("loom",),
            "manifest": "gt6_loom_art_manifest.json",
        },
        "d0": d0_loom,
        "extra_art": (
            {
                "gt6_folder": "electricloom",
                "cc_folder": "electricloom",
                "gui_source": "Loom.png",
                "gui_dest_stems": ("electricloom",),
                "manifest": "gt6_electricloom_art_manifest.json",
            },
        ),
    },
    "pressure-washer": {
        "slug": "pressure-washer",
        "source_map": "gt.recipe.pressurewasher",
        "target_map": "cruciblecraft:pressurewasher",
        "host": "cruciblecraft:pressurewasher",
        "source_rows": 312,
        "live_needle": "pressurewasher",
        "art": {
            "gt6_folder": "debarker",
            "cc_folder": "debarker",
            "gui_source": "PressureWasher.png",
            "gui_dest_stems": ("pressurewasher",),
            "manifest": "gt6_pressure_washer_art_manifest.json",
        },
        "d0": d0_pressure_washer,
        "extra_art": (),
    },
    "injector": {
        "slug": "injector",
        "source_map": "gt.recipe.injector",
        "target_map": "cruciblecraft:injector",
        "host": "cruciblecraft:injector",
        "source_rows": 638,
        "live_needle": "injector",
        "art": {
            "gt6_folder": "injector",
            "cc_folder": "injector",
            "gui_source": "Injector.png",
            "gui_dest_stems": ("injector",),
            "manifest": "gt6_injector_art_manifest.json",
        },
        "d0": d0_injector,
        "extra_art": (),
    },
    "printer": {
        "slug": "printer",
        "source_map": "gt.recipe.printer",
        "target_map": "cruciblecraft:printer",
        "host": "cruciblecraft:printer",
        "source_rows": 22,
        "live_needle": "printer",
        "art": {
            "gt6_folder": "printer",
            "cc_folder": "printer",
            "gui_source": "Printer.png",
            "gui_dest_stems": ("printer",),
            "manifest": "gt6_printer_art_manifest.json",
        },
        "d0": d0_printer,
        "extra_art": (),
    },
    "laminator": {
        "slug": "laminator",
        "source_map": "gt.recipe.laminator",
        "target_map": "cruciblecraft:laminator",
        "host": "cruciblecraft:laminator",
        "source_rows": 498,
        "live_needle": "laminator",
        "art": {
            "gt6_folder": "laminator",
            "cc_folder": "laminator",
            "gui_source": "Laminator.png",
            "gui_dest_stems": ("laminator",),
            "manifest": "gt6_laminator_art_manifest.json",
        },
        "d0": d0_laminator,
        "extra_art": (),
    },
    "melter": {
        "slug": "melter",
        "source_map": "gt.recipe.melter",
        "target_map": "cruciblecraft:melter",
        "host": "cruciblecraft:melter",
        "source_rows": 6756,
        "live_needle": "melter",
        "art": {
            "gt6_folder": "melter",
            "cc_folder": "melter",
            "gui_source": "Melter.png",
            "gui_dest_stems": ("melter",),
            "manifest": "gt6_melter_art_manifest.json",
        },
        "d0": d0_melter,
        "extra_art": (),
        "forbidden_art": ("multiblock_casing", "heat_exchanger", "/smelter/"),
    },
    "sanding": {
        "slug": "sanding",
        "source_map": "gt.recipe.sharpener",
        "target_map": "cruciblecraft:sanding",
        "host": "cruciblecraft:sanding",
        "source_rows": 7637,
        "live_needle": "sanding",
        "art": {
            "gt6_folder": "sander",
            "cc_folder": "sander",
            "gui_source": "sharpener.png",
            "gui_dest_stems": ("sanding",),
            "manifest": "gt6_sanding_art_manifest.json",
        },
        "d0": d0_sanding,
        "extra_art": (),
        "forbidden_art": (
            "multiblock_casing",
            "heat_exchanger",
            "/rollformer/",
            "/clustermill/",
            "/rollingmill/",
        ),
    },
    "oven": {
        "slug": "oven",
        "source_map": "mc.recipe.furnace",
        "target_map": "cruciblecraft:oven",
        "host": "cruciblecraft:oven",
        "source_rows": 0,
        "live_needle": "oven",
        "skip_source_pack": True,
        "art": {
            "gt6_folder": "oven",
            "cc_folder": "oven",
            "gui_source": "oven.png",
            "gui_dest_stems": ("oven",),
            "manifest": "gt6_oven_art_manifest.json",
        },
        "d0": d0_oven,
        "extra_art": (),
        "forbidden_art": (
            "multiblock_casing",
            "heat_exchanger",
            "/smelter/",
            "/melter/",
            "/furnace/",
        ),
    },
    "nanofab": {
        "slug": "nanofab",
        "source_map": "gt.recipe.nanofab",
        "target_map": "cruciblecraft:nanofab",
        "host": "cruciblecraft:nanofab",
        "source_rows": 64,
        "live_needle": "nanofab",
        "art": {
            "gt6_folder": "nanofab",
            "cc_folder": "nanofab",
            "gui_source": "Nanofab.png",
            "gui_dest_stems": ("nanofab",),
            "manifest": "gt6_nanofab_art_manifest.json",
        },
        "d0": d0_nanofab,
        "extra_art": (),
    },
}


def _copy_art(spec: dict[str, str]) -> None:
    common.copy_basicmachine_art(
        spec["gt6_folder"],
        spec["cc_folder"],
        gui_source=spec["gui_source"],
        gui_dest_stems=spec["gui_dest_stems"],
        manifest_name=spec["manifest"],
    )


def write_machine(name: str) -> dict[str, int]:
    machine = MACHINES[name]
    _copy_art(machine["art"])
    for extra in machine.get("extra_art") or ():
        _copy_art(extra)
    if machine.get("skip_source_pack"):
        counts = {"selected_rows": 0, "overflow_rows": 0, "source_rows": 0}
    else:
        counts = common.write_source_pack(
            slug=machine["slug"],
            source_map=machine["source_map"],
            target_map=machine["target_map"],
            host=machine["host"],
            source_rows=machine["source_rows"],
            source_pack_id=f"prep/machines-{machine['slug']}",
            family_id=(
                f"portfolio:track_a/{machine['target_map']}/"
                f"{machine['source_map']}#0000"
            ),
            template_key=f"{machine['source_map']}#0000",
            import_slug=f"prep/{machine['slug']}",
        )
    _write(common.wave_dir(machine["slug"]) / "d0_obtain_matrix.json", machine["d0"]())
    if name == "melter":
        _write(
            common.wave_dir("melter") / "runtime_notes.json",
            {
                "cheap_overclocking": True,
                "note": (
                    "GT6 NBT_PARALLEL=1000 and NBT_PARALLEL_DURATION stay on "
                    "the prep spec constants. ProcessingMachineSpec has no "
                    "parallel field; landing must not silently drop 1000."
                ),
                "parallel": 1000,
                "parallel_duration": True,
                "schema_version": 1,
                "source_revision": SOURCE_REVISION,
            },
        )
    if name == "loom":
        _write(
            common.wave_dir("loom") / "runtime_notes.json",
            {
                "electric_efficiency_permille": 5000,
                "note": (
                    "Electric loom NBT_EFFICIENCY=5000. Same dump as kinetic "
                    "loom; one RecipeMap compile."
                ),
                "schema_version": 1,
                "source_revision": SOURCE_REVISION,
            },
        )
    if name == "sanding":
        _write(
            common.wave_dir("sanding") / "runtime_notes.json",
            {
                "energy_accepted_sides": "UP",
                "eut": 16,
                "gt6_hosts": [20511, 20512, 20513, 20514],
                "note": (
                    "GT6 NBT_ENERGY_ACCEPTED_SIDES=SBIT_U. Dump machines also "
                    "list Grindstone 32703; that class is MultiTileEntityGrindStone "
                    "and stays out of this child. Missing tool_head_raw_* / "
                    "tool_head_* forms and closed remaps stay blocked; do not "
                    "stand in plates/dust."
                ),
                "out_of_scope": ["grindstone_32703"],
                "schema_version": 1,
                "source_revision": SOURCE_REVISION,
            },
        )
        _write(common.wave_dir("sanding") / "landing_gate.json", sanding_landing_gate())
    if name == "oven":
        _write(
            common.wave_dir("oven") / "runtime_notes.json",
            {
                "dump_rows": 0,
                "energy_accepted_sides": "DOWN",
                "eut": 16,
                "gt6_hosts": [20001, 20002, 20003, 20004],
                "note": (
                    "GT6 NBT_ENERGY_ACCEPTED_SIDES=SBIT_D. Dump mc.recipe.furnace "
                    "is empty; live map snapshots vanilla RecipeType.SMELTING. "
                    "Cooking-oil meat bonus and XP fluid stay blocked. Hosts "
                    "20001-20003 are not in the R0 ledger. Not player_complete."
                ),
                "out_of_scope": ["cooking_oil_xp", "vanilla_furnace_block"],
                "schema_version": 1,
                "source_revision": SOURCE_REVISION,
            },
        )
        _write(common.wave_dir("oven") / "landing_gate.json", oven_landing_gate())
    return counts


def sanding_landing_gate() -> dict[str, Any]:
    return {
        "capability_slug": "machines/sanding",
        "import_slug": "machines/sanding",
        "landing_blocked_by": None,
        "note": (
            "Promoted to unique-active machines/sanding. Live compile 7637 "
            "selected / 0 overflow blocked. Not player_complete."
        ),
        "promotion_checklist": [
            "Move 打磨机详细计划.md from prep/ to active/ and create capability.json workflow=active",
            "Register RecipeMap sanding, ProcessingMachineSpec, menu",
            "Add kind sanding and four Kinetic_T variants 20511-20514 to machine_kinds/tiers",
            "Add machine_delivery host texture_profile sander",
            "Add kinetic craft branch SGS/XXX/wMh with vanilla sandstone",
            "Live compile 7637 selected rows; keep 0 overflow blocked",
            "Update ProcessingMachineEnergyPlacement or GameTest to inject RU on UP",
            "Bump MachineRuntimeGameTests catalog count from 99 to 103",
            "Add SandingGameTests: four hosts, UP energy, 1-to-2 recipe, D0 craft",
        ],
        "required_test_ids": [
            "bronzeHostRunsFirstLiveRecipe",
            "fourHostsAreSurvivalCraftable",
            "liveMapPublishesSelectedRows",
            "playerSurfaceIsRegistered",
        ],
        "schema_version": 1,
        "selected_rows": 7637,
        "source_revision": SOURCE_REVISION,
        "variants": [
            {
                "id": "cruciblecraft:sanding",
                "material": "bronze",
                "sourceId": 20511,
            },
            {
                "id": "cruciblecraft:steel_sanding",
                "material": "steel",
                "sourceId": 20512,
            },
            {
                "id": "cruciblecraft:titanium_sanding",
                "material": "titanium",
                "sourceId": 20513,
            },
            {
                "id": "cruciblecraft:tungstensteel_sanding",
                "material": "tungstensteel",
                "sourceId": 20514,
            },
        ],
    }


def oven_landing_gate() -> dict[str, Any]:
    return {
        "capability_slug": "machines/oven",
        "import_slug": "machines/oven",
        "landing_blocked_by": None,
        "note": (
            "Promoted to unique-active machines/oven. Vanilla SMELTING snapshot "
            "into RM.Furnace. Empty dump is not a compact family. Not player_complete."
        ),
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "variants": [
            {
                "id": "cruciblecraft:oven",
                "material": "steel",
                "source_id": 20001,
            },
            {
                "id": "cruciblecraft:invar_oven",
                "material": "invar",
                "source_id": 20002,
            },
            {
                "id": "cruciblecraft:titanium_oven",
                "material": "titanium",
                "source_id": 20003,
            },
            {
                "id": "cruciblecraft:tungsten_carbide_oven",
                "material": "tungsten_carbide",
                "source_id": 20004,
            },
        ],
    }


def freeze_machine(name: str) -> None:
    machine = MACHINES[name]
    if machine.get("skip_source_pack"):
        return
    common.freeze_lock(
        common.wave_dir(machine["slug"]),
        f"prep/{machine['slug']}",
        f"prep isolated compile for machines/{machine['slug']}; "
        "not a live RecipeMap import; not player_complete",
    )


def check_machine(name: str) -> list[str]:
    machine = MACHINES[name]
    note = (
        f"prep isolated compile for machines/{machine['slug']}; "
        "not a live RecipeMap import; not player_complete"
    )
    if machine.get("skip_source_pack"):
        errors = common.check_art_manifest(
            machine["art"]["manifest"],
            min_art_imports=20,
            forbidden_art=machine.get(
                "forbidden_art",
                ("multiblock_casing", "heat_exchanger"),
            ),
        )
    else:
        errors = common.check_source_pack(
            slug=machine["slug"],
            source_rows=machine["source_rows"],
            import_slug=f"prep/{machine['slug']}",
            note=note,
            live_needle=machine["live_needle"],
            min_art_imports=20,
            art_manifest=machine["art"]["manifest"],
            forbidden_art=machine.get(
                "forbidden_art",
                ("multiblock_casing", "heat_exchanger"),
            ),
        )
    for extra in machine.get("extra_art") or ():
        errors.extend(
            common.check_art_manifest(
                extra["manifest"],
                min_art_imports=20,
                forbidden_art=("multiblock_casing", "heat_exchanger"),
            )
        )
    d0 = census.load_json(common.wave_dir(machine["slug"]) / "d0_obtain_matrix.json")
    if "programmed_circuit" in str(d0):
        errors.append("D0 must not use programmed_circuit stand-ins")
    if name == "melter":
        host = (d0.get("hosts") or [{}])[0]
        if "raw_ceramic_crucible" in str(host.get("crucible")):
            errors.append("melter U slot must not be the unfired raw crucible")
        notes = census.load_json(common.wave_dir("melter") / "runtime_notes.json")
        if notes.get("parallel") != 1000:
            errors.append("melter parallel 1000 must be recorded")
    if name == "sanding":
        hosts = d0.get("hosts") or []
        if len(hosts) != 4:
            errors.append("sanding D0 must freeze four Kinetic_T hosts")
        if any(row.get("status") != "source_exact" for row in hosts):
            errors.append("sanding D0 hosts must all be source_exact")
        for row in hosts:
            if row.get("sandstone", {}).get("cc") != "minecraft:sandstone":
                errors.append("sanding X slot must be vanilla sandstone")
            if "programmed_circuit" in str(row):
                errors.append("sanding D0 must not use programmed_circuit")
        notes = census.load_json(common.wave_dir("sanding") / "runtime_notes.json")
        if notes.get("energy_accepted_sides") != "UP":
            errors.append("sanding energy side must stay UP")
        if "grindstone_32703" not in (notes.get("out_of_scope") or []):
            errors.append("sanding must keep grindstone 32703 out of scope")
        spec = (
            ROOT
            / "src"
            / "main"
            / "java"
            / "com"
            / "masson"
            / "cruciblecraft"
            / "machine"
            / "processing"
            / "prep"
            / "SandingPrepSpec.java"
        )
        if not spec.is_file():
            errors.append("missing SandingPrepSpec")
        else:
            text = spec.read_text(encoding="utf-8")
            if "upEnergy" not in text or "KINETIC_ROTATION" not in text:
                errors.append("SandingPrepSpec must keep RU top-face energy")
            if "CONFIGURED_MACHINES" in text:
                errors.append("SandingPrepSpec must stay unregistered")
        gate_path = common.wave_dir("sanding") / "landing_gate.json"
        if not gate_path.is_file():
            errors.append("missing sanding landing_gate.json")
        else:
            gate = census.load_json(gate_path)
            expected = sanding_landing_gate()
            if gate != expected:
                errors.append("sanding landing_gate.json drifted")
    if name == "oven":
        hosts = d0.get("hosts") or []
        if len(hosts) != 4:
            errors.append("oven D0 must freeze four Heat_T hosts")
        if any(row.get("status") != "source_exact" for row in hosts):
            errors.append("oven D0 hosts must all be source_exact")
        for row in hosts:
            if row.get("bricks", {}).get("cc") != "minecraft:bricks":
                errors.append("oven B slot must be vanilla bricks")
            if "programmed_circuit" in str(row):
                errors.append("oven D0 must not use programmed_circuit")
            casing = str(row.get("casing", {}).get("cc") or "")
            if casing.endswith("machine_casing_double"):
                errors.append("oven M slot must be single machine_casing")
        notes = census.load_json(common.wave_dir("oven") / "runtime_notes.json")
        if notes.get("energy_accepted_sides") != "DOWN":
            errors.append("oven energy side must stay DOWN")
        if "cooking_oil_xp" not in (notes.get("out_of_scope") or []):
            errors.append("oven must keep cooking-oil / XP fluids out of scope")
        spec = (
            ROOT
            / "src"
            / "main"
            / "java"
            / "com"
            / "masson"
            / "cruciblecraft"
            / "machine"
            / "processing"
            / "prep"
            / "OvenPrepSpec.java"
        )
        if not spec.is_file():
            errors.append("missing OvenPrepSpec")
        else:
            text = spec.read_text(encoding="utf-8")
            if "HEAT" not in text or "ADJACENT" not in text:
                errors.append("OvenPrepSpec must keep HU adjacent energy")
            if "CONFIGURED_MACHINES" in text:
                errors.append("OvenPrepSpec must stay unregistered")
        gate_path = common.wave_dir("oven") / "landing_gate.json"
        if not gate_path.is_file():
            errors.append("missing oven landing_gate.json")
        else:
            gate = census.load_json(gate_path)
            expected = oven_landing_gate()
            if gate != expected:
                errors.append("oven landing_gate.json drifted")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--machine",
        choices=(*MACHINES, "all"),
        default="all",
    )
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--write", action="store_true")
    mode.add_argument("--check", action="store_true")
    mode.add_argument("--freeze-lock", action="store_true")
    mode.add_argument("--art-only", action="store_true")
    args = parser.parse_args(argv)
    names = list(MACHINES) if args.machine == "all" else [args.machine]
    try:
        if args.check:
            errors: list[str] = []
            for name in names:
                for error in check_machine(name):
                    errors.append(f"{name}: {error}")
            if errors:
                print("\n".join(errors), file=sys.stderr)
                return 1
            print("prep machine artifacts are current: " + ", ".join(names))
            return 0
        if args.freeze_lock:
            for name in names:
                freeze_machine(name)
                print(f"froze {name} prep production_lock")
            return 0
        if args.art_only:
            for name in names:
                machine = MACHINES[name]
                _copy_art(machine["art"])
                for extra in machine.get("extra_art") or ():
                    _copy_art(extra)
                print(f"copied {name} GT6 art")
            return 0
        for name in names:
            counts = write_machine(name)
            print(
                f"wrote {name} prep artifacts: "
                f"{counts['source_rows']} source / "
                f"{counts['selected_rows']} selected / "
                f"{counts['overflow_rows']} overflow"
            )
        return 0
    except Exception as error:
        print(str(error), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
