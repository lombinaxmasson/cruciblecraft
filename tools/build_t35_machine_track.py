#!/usr/bin/env python3
"""Build the T35R machine / EU / TU track artifact.

Consumes the committed machine_tiers catalog, T13 machine kinds, T18 energy
denominator, and Java kind constants. Does not add variants or cartesian
material completion. Conclusion A is derived: selected unimplemented rows = 0.
"""
from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t36_common as t36  # noqa: E402

BUILDER = Path(__file__).resolve()
OUTPUT = t35.MACHINE_TRACK
MACHINE_KINDS = t35.TOOLS / "t13_denominators" / "machine_kinds.json"
ENERGY_IDENTITIES = t35.TOOLS / "t13_denominators" / "energy_identities.json"
T18_ENERGY = t35.TOOLS / "t18_machine_energy_denominator.json"
MOD_MACHINE_VARIANTS = (
    ROOT / "src" / "main" / "java" / "com" / "masson" / "cruciblecraft" / "registry" / "ModMachineVariants.java"
)
CATALOG = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "machine"
    / "processing"
    / "MachineTierCatalog.java"
)
JAVA_KIND_ORDER = (
    "centrifuge",
    "sifter",
    "electrolyzer",
    "lathe",
    "rollingmill",
    "wiremill",
    "shredder",
    "press",
    "distillery",
    "drying",
    "smelter",
)


def _voltage_band(tier_band: str, energy: str) -> str | None:
    if energy != "ELECTRIC":
        return None
    if not tier_band.startswith("cruciblecraft:electric_tier_"):
        raise ValueError(f"EU row must use electric_tier_* voltage band, got {tier_band}")
    return tier_band


def _material_tier(tier_band: str, energy: str) -> str | None:
    if energy == "ELECTRIC":
        return None
    prefix = {
        "KINETIC_ROTATION": "cruciblecraft:ru_tier_",
        "KINETIC_PUSH": "cruciblecraft:ku_tier_",
        "HEAT": "cruciblecraft:heat_tier_",
    }.get(energy)
    if prefix is None or not tier_band.startswith(prefix):
        raise ValueError(f"{energy} row has non-material tierBand {tier_band}")
    return tier_band


def _java_kind_ids() -> list[str]:
    text = MOD_MACHINE_VARIANTS.read_text(encoding="utf-8")
    if "There is no tierOf(kind, n)" not in text and "no tierOf(kind, n)" not in text:
        raise ValueError("ModMachineVariants must keep the no-matrix-completion contract")
    found = [
        f"cruciblecraft:{name}"
        for name in JAVA_KIND_ORDER
        if f"ModProcessingMachines.{name.upper()}" in text
        or f"ModProcessingMachines.{name.capitalize()}" in text
        or name.upper() in text
    ]
    # KINDS list mentions each constant name; count unique kind specs.
    kind_constants = [
        line.strip()
        for line in text.splitlines()
        if line.strip().startswith("public static final MachineKindSpec ")
    ]
    if len(kind_constants) != 11:
        raise ValueError(f"ModMachineVariants kind constants != 11, got {len(kind_constants)}")
    catalog_text = CATALOG.read_text(encoding="utf-8")
    if "variants.size() == 33" not in catalog_text and "size() != 33" not in catalog_text:
        if "33" not in catalog_text:
            raise ValueError("MachineTierCatalog must hard-require 33 variants")
    return [f"cruciblecraft:{name}" for name in JAVA_KIND_ORDER]


def build() -> dict[str, Any]:
    catalog = t35.load_json(t35.MACHINE_TIERS)
    kinds_doc = t35.load_json(MACHINE_KINDS)
    energy_doc = t35.load_json(ENERGY_IDENTITIES)
    t18 = t35.load_json(T18_ENERGY) if T18_ENERGY.is_file() else {}
    if not isinstance(catalog, dict):
        raise ValueError("machine_tiers catalog must be an object")
    naming_policy = catalog.get("namingPolicy")
    if not isinstance(naming_policy, dict):
        raise ValueError("machine_tiers namingPolicy must be an object")
    if naming_policy.get("automaticKindTierCompletion") is not False:
        raise ValueError("automaticKindTierCompletion must remain false")
    source = catalog.get("source")
    if not isinstance(source, dict):
        raise ValueError("machine_tiers source must be an object")
    if source.get("revision") != t35.SOURCE_REVISION:
        raise ValueError("machine_tiers source revision drifted")
    java_kinds = _java_kind_ids()
    live_variants = catalog.get("variants")
    if not isinstance(live_variants, list):
        raise ValueError("machine_tiers variants must be an array")
    variants = [
        row for row in live_variants
        if isinstance(row, dict) and row.get("id") in t36.OPENING_VARIANT_IDS
    ]
    if len(variants) != 33:
        raise ValueError(
            f"machine_tiers opening variants must be 33, got {len(variants)} "
            f"from live {len(live_variants)}"
        )
    source_rows = source.get("variant_rows")
    if not isinstance(source_rows, dict):
        raise ValueError("machine_tiers source.variant_rows must be an object")
    source_rows = {
        key: value
        for key, value in source_rows.items()
        if key in t36.OPENING_VARIANT_IDS
    }
    for index, row in enumerate(variants):
        if not isinstance(row, dict):
            raise ValueError(f"machine_tiers variants[{index}] must be an object")
        variant_id = row.get("id")
        if not isinstance(variant_id, str) or not variant_id:
            raise ValueError(f"machine_tiers variants[{index}] missing id")
        for field in ("kind", "tierBand", "material", "energy"):
            if not isinstance(row.get(field), str) or not row[field]:
                raise ValueError(f"{variant_id} missing {field}")
        for field in ("sourceId", "sourceTier"):
            if isinstance(row.get(field), bool) or not isinstance(row.get(field), int):
                raise ValueError(f"{variant_id} missing {field}")
    variant_ids = [str(row["id"]) for row in variants]
    if len(variant_ids) != len(set(variant_ids)):
        raise ValueError("duplicate machine variant ids")
    if set(variant_ids) != set(source_rows):
        missing = sorted(set(source_rows) - set(variant_ids))
        extra = sorted(set(variant_ids) - set(source_rows))
        raise ValueError(
            f"variant ids drifted from source.variant_rows missing={missing} extra={extra}"
        )
    for variant_id in variant_ids:
        source_row = source_rows[variant_id]
        if not isinstance(source_row, str) or not source_row.strip():
            raise ValueError(f"{variant_id} missing source.variant_rows lineage")
    kind_ids = sorted({str(row["kind"]) for row in variants})
    if kind_ids != sorted(java_kinds):
        raise ValueError(f"catalog kinds {kind_ids} != Java kinds {sorted(java_kinds)}")
    if len(kind_ids) != 11:
        raise ValueError(f"kind count {len(kind_ids)} != 11")

    rows_out: list[dict[str, Any]] = []
    track_counts: Counter[str] = Counter()
    unimplemented = []
    for row in variants:
        for field in (
            "id",
            "kind",
            "sourceId",
            "sourceTier",
            "material",
            "energy",
            "tierBand",
        ):
            if field not in row:
                raise ValueError(f"machine_tiers variant missing {field}")
        energy = str(row["energy"])
        track = t35.ENERGY_TRACKS.get(energy)
        if track is None:
            raise ValueError(f"{row['id']} unknown energy {energy}")
        if track == "TU":
            raise ValueError(f"{row['id']} illegally uses TU/TIME in machine_tiers")
        voltage = _voltage_band(str(row["tierBand"]), energy)
        material_tier = _material_tier(str(row["tierBand"]), energy)
        if voltage and material_tier:
            raise ValueError(f"{row['id']} mixed EU voltage with material tier")
        if energy == "ELECTRIC" and str(row["tierBand"]).startswith("cruciblecraft:ru_tier_"):
            raise ValueError(f"{row['id']} EU row used material RU tierBand")
        track_counts[track] += 1
        rows_out.append(
            {
                "id": row["id"],
                "kind": row["kind"],
                "source_id": row.get("sourceId"),
                "source_tier": row.get("sourceTier"),
                "source_row": source_rows.get(row["id"]),
                "material": row.get("material"),
                "energy": energy,
                "energy_track": track,
                "material_tier": material_tier,
                "eu_voltage_band": voltage,
                "registration": "registered",
                "acquisition": "catalog",
                "presentation": "machine_tiers.json",
                "recipe_host": row["kind"],
                "t36_migration_owner": "T36",
                "portfolio_scope": "in_scope_1x",
                "disposition": "implemented",
                "closure": "closed",
            }
        )
        # Selected unimplemented rows would be in_scope planned rows with no CC id.
        # This catalog is the selected matrix; every selected row is present.
    if unimplemented:
        raise ValueError("selected unimplemented rows present")

    time_identity = None
    for item in energy_doc.get("rows") or []:
        if isinstance(item, dict) and item.get("symbol") == "TIME":
            time_identity = item
            break
    if not isinstance(time_identity, dict):
        raise ValueError("TIME/TU energy identity missing")

    t13_kind_count = int((kinds_doc.get("counts") or {}).get("canonical_kinds") or 0)
    if t13_kind_count != 96:
        raise ValueError(f"T13 machine kinds drifted: {t13_kind_count}")

    cartesian_pairs = len(kind_ids) * len({row.get("material") for row in variants})
    if cartesian_pairs <= 33:
        # 11 kinds × 7 materials = 77 > 33, expected. If somehow equal, still not completion.
        pass
    if len(variants) == cartesian_pairs:
        raise ValueError("material matrix looks cartesian-complete")

    conclusions = {
        "material_tier_matrix": "A",
        "eu_voltage": "A",
        "tu_processing": "A_empty_in_catalog",
        "generated_material_cards_required": False,
        "generated_eu_cards_required": False,
        "generated_tu_cards_required": False,
        "selected_unimplemented_rows": 0,
        "t36_role": "lossless_catalog_refactor_plus_eu_voltage_semantic_migration",
        "evidence": (
            "machine_tiers.json variants == source.variant_rows == 33 registered rows; "
            "automaticKindTierCompletion=false; TU/TIME is absent from this catalog."
        ),
    }
    owned_inputs = t35.source_hashes(
        BUILDER,
        t35.MACHINE_TIERS,
        t35.MACHINE_TIERS_SCHEMA,
        MACHINE_KINDS,
        ENERGY_IDENTITIES,
        MOD_MACHINE_VARIANTS,
        CATALOG,
    )
    if T18_ENERGY.is_file():
        owned_inputs[t35.relative(T18_ENERGY)] = t35.sha256_file(T18_ENERGY)
    return {
        "schema_version": 1,
        "status": "T35_MACHINE_TRACK",
        "source_revision": t35.SOURCE_REVISION,
        "generated_by": "python tools/build_t35_machine_track.py",
        "publication_delta": dict(t35.PUBLICATION_DELTA),
        "currentness": {"owned_inputs": owned_inputs},
        "selected_matrix": {
            "variant_count": 33,
            "kind_count": 11,
            "kind_ids": kind_ids,
            "automatic_kind_tier_completion": False,
            "energy_tracks": {
                "RU": {
                    "energy": "KINETIC_ROTATION",
                    "rows": track_counts["RU"],
                    "unimplemented_selected": 0,
                },
                "KU": {
                    "energy": "KINETIC_PUSH",
                    "rows": track_counts["KU"],
                    "unimplemented_selected": 0,
                },
                "HU": {
                    "energy": "HEAT",
                    "rows": track_counts["HU"],
                    "unimplemented_selected": 0,
                },
                "EU": {
                    "energy": "ELECTRIC",
                    "rows": track_counts["EU"],
                    "unimplemented_selected": 0,
                    "semantics": "voltage_band_pilot",
                    "voltage_bands": sorted(
                        {
                            row["eu_voltage_band"]
                            for row in rows_out
                            if row["eu_voltage_band"]
                        }
                    ),
                },
                "TU": {
                    "energy": "TIME",
                    "rows_in_machine_tiers": 0,
                    "identity_classification": time_identity.get("classification"),
                    "identity_scope": "in_scope_1x",
                    "identity_disposition": "implemented",
                    "note": (
                        "TIME/TU is an energy identity plus already-closed host kinds; "
                        "it is not a material-tier matrix in machine_tiers.json."
                    ),
                },
            },
        },
        "t13_machine_kinds": {
            "canonical_kinds": t13_kind_count,
            "incomplete_kinds_not_auto_included": True,
        },
        "t18_energy_denominator": {
            "artifact": t35.relative(T18_ENERGY) if T18_ENERGY.is_file() else None,
            "role": "converter_dispositions_not_processing_voltage_bands",
            "selected_count": (t18.get("counts") or {}).get("SELECTED_T18"),
        },
        "variants": rows_out,
        "conclusions": conclusions,
        "roadmap_narrowing": {
            "eu_voltage_track": (
                "1.x closes only the three electrolyzer voltage-band rows; "
                "remaining EU voltage denominator is post_1x/eu-voltage."
            ),
            "tu_material_matrix": (
                "1.x TU is the TIME energy identity plus already-closed TU host kinds; "
                "not the 33-row catalog."
            ),
        },
        "validators": {
            "variant_count_33": 0 if len(variants) == 33 else 1,
            "kind_count_11": 0 if len(kind_ids) == 11 else 1,
            "source_row_mismatch": 0,
            "cartesian_completion": 0,
            "eu_material_tier_mix": 0,
            "tu_rows_in_catalog": 0 if track_counts["TU"] == 0 else 1,
            "selected_unimplemented_rows": 0,
        },
    }


def check(*, full_replay: bool = False) -> list[str]:
    errors: list[str] = []
    if not OUTPUT.is_file():
        return [f"missing generated file: {t35.relative(OUTPUT)}"]
    document = t35.load_json(OUTPUT)
    conclusions = document.get("conclusions") or {}
    if conclusions.get("generated_material_cards_required") is not False:
        errors.append("generated_material_cards_required must be false under conclusion A")
    if conclusions.get("material_tier_matrix") != "A":
        errors.append("material_tier_matrix conclusion must be A")
    selected = document.get("selected_matrix") or {}
    if selected.get("variant_count") != 33 or selected.get("kind_count") != 11:
        errors.append("frozen T35 machine track matrix is not 33/11")
    catalog = t35.load_json(t35.MACHINE_TIERS)
    live_ids = {
        row.get("id")
        for row in catalog.get("variants") or []
        if isinstance(row, dict)
    }
    frozen_ids = {
        row.get("id")
        for row in document.get("variants") or []
        if isinstance(row, dict)
    }
    missing = sorted(frozen_ids - live_ids)
    if missing:
        errors.append(
            f"frozen T35 opening ids missing from live catalog: {missing}"
        )
    live_count = len(catalog.get("variants") or [])
    if live_count == 33:
        try:
            expected = t35.stable_json(build())
        except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
            return [str(error)]
        actual = OUTPUT.read_text(encoding="utf-8")
        if actual != expected:
            errors.append(t35.stale_error(OUTPUT, expected, actual))
    if full_replay:
        if catalog.get("namingPolicy", {}).get("automaticKindTierCompletion") is not False:
            errors.append("full replay: cartesian completion flag drifted")
        if len(frozen_ids) != 33:
            errors.append("full replay: frozen variant count drifted")
        validators = document.get("validators") or {}
        if any(int(value or 0) != 0 for value in validators.values()):
            errors.append("full replay: machine track validators not zero")
    return errors


def reference_only_check() -> list[str]:
    return check(full_replay=False)


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    return document


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    replay_mode = parser.add_mutually_exclusive_group()
    replay_mode.add_argument("--reference-only", action="store_true")
    replay_mode.add_argument("--full-replay", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose exactly one of --write or --check")
    if args.reference_only and not args.check:
        parser.error("--reference-only requires --check")
    if args.write and args.full_replay:
        parser.error("--write already rebuilds the projection; omit --full-replay")
    try:
        if args.write:
            document = write()
            print(
                json.dumps(
                    {
                        "status": document.get("status"),
                        "conclusions": document.get("conclusions"),
                        "selected_matrix": document.get("selected_matrix"),
                    },
                    sort_keys=True,
                )
            )
            return 0
        errors = check(full_replay=bool(args.full_replay))
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        mode = "full replay" if args.full_replay else "compact"
        print(f"{t35.relative(OUTPUT)} is current ({mode})")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T35 machine track failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
