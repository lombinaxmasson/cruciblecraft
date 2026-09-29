#!/usr/bin/env python3
"""GT6 technological-part family ledger for motors, pistons, conveyers, emitters, sensors.

Hosts share these arrays. This card freezes the 0–5 matrix and lands every
source-exact row after registering the real curved-plate, Ender-gem, and
wireGt03/wireGt05 support forms. IL.CONVEYERS / PUMPS / ROBOT_ARMS reclaim
the compact-electric cover items. Printer live families stay out of scope.
"""
from __future__ import annotations

import re
import shutil
from typing import Any

from tools import census_common as census

SLUG = "content/technological-parts-foundation"
SOURCE_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
GENERATED_BY = "python tools/build_technological_parts_foundation.py"
WAVE = census.TOOLS / "waves" / "content" / "technological-parts-foundation"
LEDGER = WAVE / "dependency_ledger.json"
READINESS = WAVE / "readiness.json"
TOPOLOGY = WAVE / "topology.json"
WAVE_JSON = WAVE / "wave.json"
CAPABILITY = (
    census.TOOLS / "capabilities" / "content" / "technological-parts-foundation"
    / "capability.json"
)
ART_MANIFEST = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "gt6_technological_parts_foundation_art_manifest.json"
)
GT6_ART_ROOT = (
    census.ROOT
    / "gt6_referencable_port_code"
    / "gregtech6_w"
    / "src"
    / "main"
    / "resources"
)
TECH_PARTS = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "technological_parts.json"
)
RECIPE_ROOT = (
    census.ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "recipe"
)
DATAGEN_RECIPE = (
    census.ROOT
    / "src"
    / "generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
)
GENERATED_RECIPE = DATAGEN_RECIPE / "machines"
LIVE_PRINTER = (
    census.ROOT
    / "src"
    / "recipe_generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
)
SPLIT_MODULE = re.compile(
    r"compact_electric_(?:conveyor|pump)_module_|compact_robot_arm_"
)

MOTORS = {
    0: "cruciblecraft:compact_electric_motor_ulv",
    1: "cruciblecraft:compact_electric_motor_lv",
    2: "cruciblecraft:compact_electric_motor_mv",
    3: "cruciblecraft:compact_electric_motor_hv",
    4: "cruciblecraft:compact_electric_motor_ev",
    5: "cruciblecraft:compact_electric_motor_iv",
}
PISTONS = {
    0: "cruciblecraft:compact_electric_piston_ulv",
    1: "cruciblecraft:compact_electric_piston_lv",
    2: "cruciblecraft:compact_electric_piston_mv",
    3: "cruciblecraft:compact_electric_piston_hv",
    4: "cruciblecraft:compact_electric_piston_ev",
    5: "cruciblecraft:compact_electric_piston_iv",
}
CONVEYERS = {
    0: "cruciblecraft:compact_electric_conveyor_ulv",
    1: "cruciblecraft:compact_electric_conveyor_lv",
    2: "cruciblecraft:compact_electric_conveyor_mv",
    3: "cruciblecraft:compact_electric_conveyor_hv",
    4: "cruciblecraft:compact_electric_conveyor_ev",
    5: "cruciblecraft:compact_electric_conveyor_iv",
}
TIERS = (
    (0, "ULV", "TinAlloy"),
    (1, "LV", "SteelGalvanized"),
    (2, "MV", "Al"),
    (3, "HV", "StainlessSteel"),
    (4, "EV", "Cr"),
    (5, "IV", "Ti"),
)
PUMPS = {
    tier: f"cruciblecraft:compact_electric_pump_{vn.lower()}"
    for tier, vn, _ in TIERS
}
ROBOT_ARMS = {
    tier: f"cruciblecraft:compact_electric_robot_arm_{vn.lower()}"
    for tier, vn, _ in TIERS
}
FIELD_GENERATORS = {
    tier: f"cruciblecraft:compact_force_field_emitter_{vn.lower()}"
    for tier, vn, _ in TIERS
}
FAMILY_BASE = {
    "motors": 12000,
    "pistons": 12060,
    "conveyers": 12040,
    "emitters": 12120,
    "sensors": 12140,
    "pumps": 12020,
    "robot_arms": 12080,
    "field_generators": 12100,
}
EMITTERS = {
    tier: f"cruciblecraft:compact_signal_emitter_{vn.lower()}"
    for tier, vn, _mt in TIERS
}
SENSORS = {
    tier: f"cruciblecraft:compact_sensor_{vn.lower()}"
    for tier, vn, _mt in TIERS
}
REUSED_PARTS = (
    {
        "family": "motors",
        "tier": 1,
        "source_id": 12001,
        "cc": MOTORS[1],
        "status": "reuse_canonical",
        "reason": "Already registered compact_electric_motor_lv (IL.MOTORS[1]).",
    },
    {
        "family": "motors",
        "tier": 4,
        "source_id": 12004,
        "cc": MOTORS[4],
        "status": "reuse_canonical",
        "reason": "Already registered compact_electric_motor_ev (IL.MOTORS[4]).",
    },
    {
        "family": "pistons",
        "tier": 4,
        "source_id": 12064,
        "cc": PISTONS[4],
        "status": "reuse_canonical",
        "reason": "Already registered compact_electric_piston_ev (IL.PISTONS[4]).",
    },
)
MATERIALS = {
    0: "tin_alloy",
    1: "steel_galvanized",
    2: "aluminium",
    3: "stainless_steel",
    4: "chromium",
    5: "titanium",
}
PART_NAMES = {
    "motors": ("Compact Electric Motor", "紧凑电动机"),
    "pistons": ("Compact Electric Piston", "紧凑电动活塞"),
    "conveyers": ("Compact Electric Conveyor", "紧凑电动传送带"),
    "emitters": ("Compact Signal Emitter", "紧凑信号发射器"),
    "sensors": ("Compact Sensor", "紧凑传感器"),
    "pumps": ("Compact Electric Pump", "紧凑电动泵"),
    "robot_arms": ("Compact Electric Robot Arm", "紧凑电动机械臂"),
    "field_generators": ("Compact Force Field Emitter", "紧凑力场发生器"),
}
REUSED_KEYS = {(row["family"], row["tier"]) for row in REUSED_PARTS}
LANDED_PARTS = []
for _family, _registry in (
    ("motors", MOTORS),
    ("pistons", PISTONS),
    ("conveyers", CONVEYERS),
    ("emitters", EMITTERS),
    ("sensors", SENSORS),
    ("pumps", PUMPS),
    ("robot_arms", ROBOT_ARMS),
    ("field_generators", FIELD_GENERATORS),
):
    for _tier, _vn, _mt in TIERS:
        if (_family, _tier) in REUSED_KEYS:
            continue
        _path = _registry[_tier].split(":", 1)[1]
        _recipes = [_path + ".json"]
        if _family == "motors" and _tier == 0:
            _recipes = [
                _path + "_iron_magnetic.json",
                _path + "_steel_magnetic.json",
            ]
        LANDED_PARTS.append(
            {
                "family": _family,
                "tier": _tier,
                "source_id": FAMILY_BASE[_family] + _tier,
                "cc": _registry[_tier],
                "registry_path": _path,
                "english_name": f"{PART_NAMES[_family][0]} ({_vn})",
                "chinese_name": f"{PART_NAMES[_family][1]}（{_vn}）",
                "gt6_png": f"{FAMILY_BASE[_family] + _tier}.png",
                "recipes": _recipes,
            }
        )
LANDED_PARTS = tuple(LANDED_PARTS)


def has_split_module_standin(blob: str) -> bool:
    return SPLIT_MODULE.search(blob) is not None


def unique_active_wave() -> str | None:
    if not CAPABILITY.is_file():
        return SLUG
    document = census.load_json(CAPABILITY)
    if document.get("workflow") == "active":
        return SLUG
    return None


def _row(
    family: str,
    tier: int,
    *,
    cc: str,
    status: str,
    reason: str,
) -> dict[str, Any]:
    vn = next(row[1] for row in TIERS if row[0] == tier)
    token = {
        "motors": "MOTORS",
        "pistons": "PISTONS",
        "conveyers": "CONVEYERS",
        "emitters": "EMITTERS",
        "sensors": "SENSORS",
        "pumps": "PUMPS",
        "robot_arms": "ROBOT_ARMS",
        "field_generators": "FIELD_GENERATORS",
    }[family]
    return {
        "cc": cc,
        "family": family,
        "gt6": f"IL.{token}[{tier}]",
        "reason": reason,
        "source_id": FAMILY_BASE[family] + tier,
        "status": status,
        "tier": tier,
        "vn": vn,
    }


def dependency_rows() -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    reused = {(row["family"], row["tier"]): row for row in REUSED_PARTS}
    landed = {(row["family"], row["tier"]): row for row in LANDED_PARTS}
    for family in (
        "motors",
        "pistons",
        "conveyers",
        "emitters",
        "sensors",
        "pumps",
        "robot_arms",
        "field_generators",
    ):
        for tier, _vn, _mt in TIERS:
            key = (family, tier)
            if key in landed:
                part = landed[key]
                rows.append(
                    _row(
                        family,
                        tier,
                        cc=str(part["cc"]),
                        status="source_exact",
                        reason="Landed this card from GT6 MultiItemTechnological.",
                    )
                )
                continue
            if key in reused:
                part = reused[key]
                rows.append(
                    _row(
                        family,
                        tier,
                        cc=str(part["cc"]),
                        status="reuse_canonical",
                        reason=str(part["reason"]),
                    )
                )
                continue
            rows.append(
                _row(
                    family,
                    tier,
                    cc="",
                    status="blocked",
                    reason="Source operand closure unexpectedly drifted.",
                )
            )
    return rows


def art_imports() -> list[dict[str, str]]:
    rows = []
    for part in LANDED_PARTS:
        path = part["registry_path"]
        rows.append(
            {
                "destination": (
                    f"assets/cruciblecraft/textures/item/gt6_import/{path}.png"
                ),
                "gt6_source": (
                    "src/main/resources/assets/gregtech/textures/items/"
                    f"gt.multiitem.technological/{part['gt6_png']}"
                ),
            }
        )
    return rows


def documents() -> dict[str, Any]:
    rows = dependency_rows()
    wave = unique_active_wave()
    closed = wave is None
    landed = [row for row in rows if row["status"] == "source_exact"]
    reused = [row for row in rows if row["status"] == "reuse_canonical"]
    blocked = [row for row in rows if row["status"] == "blocked"]
    return {
        "dependency_ledger": {
            "blocked_count": len(blocked),
            "families": [
                "motors",
                "pistons",
                "conveyers",
                "emitters",
                "sensors",
                "pumps",
                "robot_arms",
                "field_generators",
            ],
            "generated_by": GENERATED_BY,
            "landed_count": len(landed),
            "reused_count": len(reused),
            "rows": rows,
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "status": "TECHNOLOGICAL_PARTS_FOUNDATION",
            "wave_slug": SLUG,
        },
        "readiness": {
            "evidence": {
                "blocked_count": len(blocked),
                "landed_parts": [row["cc"] for row in landed],
                "owns_families": 8,
                "reused_parts": [row["cc"] for row in reused],
                "slicer_exact_hosts": [20381, 20382, 20383, 20384, 20385],
                "injector_exact_hosts": [20261, 20262, 20263, 20264, 20265],
                "loom_exact_hosts": [20361, 20362, 20363, 20364, 20365],
            },
            "generated_by": GENERATED_BY,
            "next_unassigned": closed,
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "status": (
                "TECHNOLOGICAL_PARTS_FOUNDATION_READY"
                if closed
                else "TECHNOLOGICAL_PARTS_FOUNDATION_ACTIVE"
            ),
            "unique_active_wave": wave,
            "wave_slug": SLUG,
        },
        "topology": {
            "append_only": False,
            "complete_family_count": 8,
            "generated_by": GENERATED_BY,
            "next_unassigned": closed,
            "remaining_recipe_gap": len(blocked),
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "status": "WAVE_READY",
            "unique_active_wave": wave,
            "wave_slug": SLUG,
        },
        "wave": {
            "depends_on": [
                "machines/injector",
                "machines/loom",
                "machines/slicer",
            ],
            "generated_by": GENERATED_BY,
            "program": SLUG,
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "unique_active_wave": wave,
        },
        "art_manifest": {
            "imports": art_imports(),
            "source": "gt6_referencable_port_code/gregtech6_w",
            "source_revision": SOURCE_REVISION,
        },
    }


def copy_art() -> None:
    dest_root = census.ROOT / "src" / "main" / "resources"
    for row in art_imports():
        source = census.ROOT / "gt6_referencable_port_code" / "gregtech6_w" / row["gt6_source"]
        destination = dest_root / row["destination"]
        if not source.is_file():
            raise FileNotFoundError(source)
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, destination)


def write() -> dict[str, Any]:
    WAVE.mkdir(parents=True, exist_ok=True)
    copy_art()
    built = documents()
    census.write_stable(LEDGER, built["dependency_ledger"])
    census.write_stable(READINESS, built["readiness"])
    census.write_stable(TOPOLOGY, built["topology"])
    census.write_stable(WAVE_JSON, built["wave"])
    census.write_stable(ART_MANIFEST, built["art_manifest"])
    return {
        "blocked_count": built["dependency_ledger"]["blocked_count"],
        "landed_count": built["dependency_ledger"]["landed_count"],
        "reused_count": built["dependency_ledger"]["reused_count"],
    }


def _d0_statuses(path, expected: dict[int, str]) -> list[str]:
    errors: list[str] = []
    if not path.is_file():
        return [f"missing {census.relative(path)}"]
    document = census.load_json(path)
    if has_split_module_standin(str(document)):
        errors.append(f"{census.relative(path)} must not use split module identities")
    if "programmed_circuit" in str(document):
        errors.append(f"{census.relative(path)} must not invent programmed_circuit")
    statuses = {row["host"]: row["status"] for row in document.get("hosts") or []}
    if statuses != expected:
        errors.append(f"{census.relative(path)} host statuses drifted")
    return errors


def check() -> list[str]:
    errors: list[str] = []
    expected = documents()
    required = (
        (LEDGER, expected["dependency_ledger"]),
        (READINESS, expected["readiness"]),
        (TOPOLOGY, expected["topology"]),
        (WAVE_JSON, expected["wave"]),
        (ART_MANIFEST, expected["art_manifest"]),
    )
    for path, document in required:
        if not path.is_file():
            errors.append(f"missing {census.relative(path)}")
            continue
        if census.load_json(path) != document:
            errors.append(f"{census.relative(path)} drifted")
    if not TECH_PARTS.is_file():
        errors.append("missing technological_parts.json")
        return errors
    catalog = census.load_json(TECH_PARTS)
    by_path = {row["registry_path"]: row for row in catalog.get("parts") or []}
    if len(catalog.get("parts") or []) < 73:
        errors.append(
            "technological_parts.json must keep the 73 foundation parts"
        )
    for part in LANDED_PARTS:
        row = by_path.get(part["registry_path"])
        if row is None:
            errors.append(f"missing catalog part {part['registry_path']}")
            continue
        if int(row.get("source_id") or 0) != part["source_id"]:
            errors.append(f"{part['registry_path']} source_id drifted")
        cover_prefix = {
            "conveyers": "compact_electric_conveyor_",
            "pumps": "compact_electric_pump_",
            "robot_arms": "compact_electric_robot_arm_",
        }.get(part["family"])
        if cover_prefix is not None and (
            not part["registry_path"].startswith(cover_prefix)
            or "_module_" in part["registry_path"]
        ):
            errors.append(
                f"{part['family']} must reuse compact-electric cover ids"
            )
        for recipe_name in part["recipes"]:
            recipe = RECIPE_ROOT / recipe_name
            if not recipe.is_file():
                recipe = DATAGEN_RECIPE / recipe_name
            if not recipe.is_file():
                errors.append(f"missing {recipe_name}")
            elif "programmed_circuit" in recipe.read_text(encoding="utf-8"):
                errors.append(f"{recipe_name} must not use programmed_circuit")
        dest = (
            census.ROOT
            / "src"
            / "main"
            / "resources"
            / f"assets/cruciblecraft/textures/item/gt6_import/{part['registry_path']}.png"
        )
        if not dest.is_file():
            errors.append(f"missing art {census.relative(dest)}")
    for name in (
        "slicer.json",
        "chromium_slicer.json",
        "injector.json",
    ):
        path = GENERATED_RECIPE / name
        if not path.is_file():
            errors.append(f"missing acquisition recipe {census.relative(path)}")
        elif has_split_module_standin(path.read_text(encoding="utf-8")):
            errors.append(f"{name} must not use split conveyor modules")
    errors.extend(
        _d0_statuses(
            census.ROOT / "tools" / "waves" / "machines" / "slicer" / "d0_obtain_matrix.json",
            {
                20381: "source_exact",
                20382: "source_exact",
                20383: "source_exact",
                20384: "source_exact",
                20385: "source_exact",
            },
        )
    )
    errors.extend(
        _d0_statuses(
            census.ROOT / "tools" / "waves" / "machines" / "injector" / "d0_obtain_matrix.json",
            {
                20261: "source_exact",
                20262: "source_exact",
                20263: "source_exact",
                20264: "source_exact",
                20265: "source_exact",
            },
        )
    )
    errors.extend(
        _d0_statuses(
            census.ROOT / "tools" / "waves" / "machines" / "loom" / "d0_obtain_matrix.json",
            {
                20211: "source_exact",
                20212: "source_exact",
                20213: "source_exact",
                20214: "source_exact",
                20361: "source_exact",
                20362: "source_exact",
                20363: "source_exact",
                20364: "source_exact",
                20365: "source_exact",
            },
        )
    )
    printer = (
        census.ROOT / "tools" / "waves" / "prep" / "printer" / "d0_obtain_matrix.json"
    )
    if printer.is_file():
        document = census.load_json(printer)
        if has_split_module_standin(str(document)):
            errors.append("printer prep D0 must not use split conveyor modules")
        statuses = {row["host"]: row["status"] for row in document.get("hosts") or []}
        if any(
            statuses.get(host) != "source_exact"
            for host in (20271, 20272, 20273, 20274, 20275)
        ):
            errors.append("printer prep hosts must be source_exact after reclaiming covers")
    if LIVE_PRINTER.is_dir():
        live = [
            path
            for path in LIVE_PRINTER.rglob("*printer*")
            if path.is_file()
        ]
        if live:
            errors.append("printer must not have a live recipe family")
    if CAPABILITY.is_file():
        capability = census.load_json(CAPABILITY)
        if capability.get("slug") != SLUG:
            errors.append("capability slug drifted")
    return errors
