#!/usr/bin/env python3
"""GT6 material ID structural classes + ownership helpers.

Encoding (GT6 material id space):
  id == 0                       reserved empty slot
  id % 10 == 0, id < 1000     element, Z = id // 10
  id % 10 != 0, id < 1000     isotope / particle (Z = id // 10)
  1000..1180, id%10==0        verified continuation: elements Z=100..118
  1000..1180, id%10!=0        transuranium isotope
  1181 <= id < 8000           fictional / sci-fi / magic / joke material band
  8000 <= id < 9000           alloy / compound
  id >= 9000                  organic / biological / misc
"""
from __future__ import annotations

from typing import Any

STRUCTURAL_CLASSES = (
    "empty",
    "element",
    "isotope",
    "fictional_material",
    "alloy_compound",
    "organic_misc",
    "no_id",
)

# Mods whose materials are "GT/vanilla native" for ownership decisions.
NATIVE_ORIGINAL_MODS = frozenset({
    "gregapi",
    "gregtech",
    "minecraft",
    None,
    "",
})

# Common isotopes to keep early (user: D/T/C14/U235; plus He3/U233 as usual GT set).
COMMON_ISOTOPE_IDS = frozenset({
    11,   # Deuterium
    12,   # Tritium
    21,   # Helium-3
    62,   # Carbon-14
    921,  # Uranium-235
    922,  # Uranium-233
})
COMMON_ISOTOPE_NAMES = frozenset({
    "Deuterium",
    "Tritium",
    "Helium3",
    "Carbon14",
    "Uranium235",
    "Uranium233",
})


def structural_class(source_id: Any) -> str:
    if not isinstance(source_id, int) or source_id < 0:
        return "no_id"
    if source_id == 0:
        return "empty"
    if source_id <= 1180:
        return "element" if source_id % 10 == 0 else "isotope"
    if source_id < 8000:
        return "fictional_material"
    if source_id < 9000:
        return "alloy_compound"
    return "organic_misc"


def atomic_number(source_id: Any) -> int | None:
    """Z only for verified periodic-table ids (1..118), never all midband slots."""
    if not isinstance(source_id, int) or source_id < 0:
        return None
    if source_id % 10 != 0:
        return None
    if 10 <= source_id <= 1180:
        return source_id // 10
    return None


def is_element_like(source_id: Any) -> bool:
    """Verified periodic-table slots only: id=10*Z for 1 <= Z <= 118."""
    return atomic_number(source_id) is not None


def is_native_mod(original_mod: Any) -> bool:
    if original_mod is None:
        return True
    return str(original_mod) in NATIVE_ORIGINAL_MODS


def is_common_isotope(source_id: Any, source_name: str | None = None) -> bool:
    if isinstance(source_id, int) and source_id in COMMON_ISOTOPE_IDS:
        return True
    if source_name and source_name in COMMON_ISOTOPE_NAMES:
        return True
    return False


def element_completeness(
    materials: list[dict[str, Any]],
    *,
    id_field: str = "source_id",
    z_min: int = 1,
    z_max: int = 118,
) -> dict[str, Any]:
    """Assert Z in [z_min, z_max] each have an element-like material id = 10*Z."""
    by_z: dict[int, list[dict[str, Any]]] = {}
    for m in materials:
        mid = m.get(id_field, m.get("id"))
        z = atomic_number(mid)
        if z is None:
            continue
        by_z.setdefault(z, []).append(m)
    present = sorted(z for z in by_z if z_min <= z <= z_max)
    missing = [z for z in range(z_min, z_max + 1) if z not in by_z]
    extras = sorted(z for z in by_z if z < z_min or z > z_max)
    return {
        "z_range": [z_min, z_max],
        "present_count": len(present),
        "missing_z": missing,
        "missing_count": len(missing),
        "extra_z_outside_range": extras,
        "ok": not missing,
        "by_z_sample": {
            str(z): [
                {
                    "source_id": (m.get(id_field, m.get("id"))),
                    "source_name": m.get("source_name") or m.get("nameInternal"),
                    "original_mod": m.get("original_mod") or m.get("originalMod"),
                }
                for m in rows[:3]
            ]
            for z, rows in list(sorted(by_z.items()))[:5]
        },
    }
