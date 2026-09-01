#!/usr/bin/env python3
"""Enumerate the deferred ordinary ledger for recycling/deferred-ordinary-ledger-r0.

Rebuilds every deferred family once from sealed remainder + T42 recovery
evidence + host production locks + pinned GT6 dumps. Does not generate recipes.
"""
from __future__ import annotations

from collections import Counter
from pathlib import Path
from typing import Any

from tools import t35_common as t35
from tools import t42_common as t42
from tools import t42_owner_common as owner
from tools.recipe_bulk import ordinary_r0 as ordinary

SLUG = "recycling/deferred-ordinary-ledger-r0"
NEXT_CHILD = "recycling/smelter-mte-identity"
PROGRAM = "recycling/deferred-ordinary-runtime"
GENERATED_BY = "python tools/build_deferred_ordinary_ledger_r0.py"
SOURCE_REVISION = t35.SOURCE_REVISION

LEDGER_TOTAL = 1845
RECYCLING_TOTAL = 1843
INHERITED_RECYCLING = 1819
PROVEN_SMELTER = 1817
SMELTER_EDGE = 2
AUTOCLAVE_TAGGED = 24
CENTRIFUGE_NON_RECYCLING = 2

MTE_ITEM = "gregtech:gt.multitileentity"
SMELTER_EDGE_KEYS = ("gt.recipe.smelter#1829", "gt.recipe.smelter#1884")
CENTRIFUGE_ENVELOPE_KEY = "gt.recipe.centrifuge#0010"
CENTRIFUGE_CROSS_KEY = "gt.recipe.centrifuge#0207"
EDGE_BLOCKERS = frozenset(
    {
        "no_proven_material_identity_output",
        "non_registered_material_fluid_output",
    }
)
AUTOCLAVE_ORDINARY_BLOCKERS = frozenset(
    {
        "additional_fluid_input",
        "non_consumed_mte_input",
        "non_mte_item_input",
        "non_positive_mte_input",
    }
)

COHORTS = (
    "smelter_proven_mte_recovery",
    "smelter_recovery_edge",
    "autoclave_tagged_recycling",
    "centrifuge_execution_envelope",
    "centrifuge_cross_mod",
    "mislabeled_needs_reclass",
)

N300_EXCEPTIONS = (
    {
        "child": "recycling/deferred-ordinary-ledger-r0",
        "owns_families": 0,
        "exception_kind": "infrastructure_ledger_enumeration",
        "padding_forbidden": True,
        "reason": "infrastructure: enumerate and partition the deferred ledger",
    },
    {
        "child": "recycling/smelter-mte-identity",
        "owns_families": 0,
        "exception_kind": "infrastructure_identity_catalog",
        "padding_forbidden": True,
        "reason": "infrastructure: holdable Smelter MTE identity and acquisition",
    },
    {
        "child": "smelter/deferred-recycling-edge",
        "owns_families": 2,
        "exception_kind": "independent_evidence_incomplete_cohort",
        "padding_forbidden": True,
        "reason": "independent evidence-incomplete cohort; not the 1817 builder",
    },
    {
        "child": "autoclave/deferred-recycling",
        "owns_families": 24,
        "exception_kind": "different_host_operand_shape",
        "padding_forbidden": True,
        "reason": "different host and operand shape from Smelter MTE recovery",
    },
    {
        "child": "recycling/non-recycling-scope",
        "owns_families": 2,
        "exception_kind": "envelope_and_cross_mod_scope",
        "padding_forbidden": True,
        "reason": "centrifuge envelope / cross-mod; not a recycling production lock",
    },
)

REMAINDER_DEFERRED = (
    t35.TOOLS
    / "waves"
    / "recipe-portfolio"
    / "ordinary-remainder-closure"
    / "deferred_ledger.json"
)
MIXER_CENSUS = (
    t35.TOOLS / "waves" / "mixer" / "ordinary-closure" / "census_delta.json"
)
SMELTER_LOCK = (
    t35.TOOLS / "waves" / "smelter" / "ordinary-closure" / "production_lock.json"
)
AUTOCLAVE_LOCK = (
    t35.TOOLS / "waves" / "autoclave" / "ordinary-closure" / "production_lock.json"
)
CENTRIFUGE_LOCK = (
    t35.TOOLS / "waves" / "centrifuge" / "ordinary-closure" / "production_lock.json"
)
BATH_CATALOG_CANDIDATES = (
    t35.TOOLS / "bath_mte_identity_catalog.json",
    t35.TOOLS / "t46_bath_mte_identity_catalog.json",
    t35.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "bath_mte_identity_catalog.json",
)

_DUMP_CACHE: dict[str, list[dict[str, Any]]] = {}


def _lock_reclassified(path: Path) -> dict[str, dict[str, Any]]:
    document = t35.load_json(path)
    rows = {}
    for row in document.get("reclassified") or []:
        family_id = str(row["family_id"])
        rows[family_id] = row
    return rows


def _completed_ordinary_family_ids() -> set[str]:
    ids: set[str] = set()
    for path in ordinary.HOST_ORDINARY_LOCKS:
        if not path.is_file():
            continue
        document = t35.load_json(path)
        for row in (document.get("production") or {}).get("families") or []:
            ids.add(str(row["family_id"]))
    return ids


def _evidence_by_id() -> dict[str, dict[str, Any]]:
    document = t35.load_json(owner.RECOVERY_EVIDENCE)
    return {str(row["family_id"]): row for row in document.get("families") or []}


def _dump_recipes(map_name: str) -> list[dict[str, Any]]:
    cached = _DUMP_CACHE.get(map_name)
    if cached is None:
        cached = t42.load_map_recipes(map_name)
        _DUMP_CACHE[map_name] = cached
    return cached


def _relation_dump(relation_key: str) -> dict[str, Any] | None:
    if "#" not in relation_key:
        return None
    map_name, index_s = relation_key.rsplit("#", 1)
    try:
        index = int(index_s)
    except ValueError:
        return None
    recipes = _dump_recipes(map_name)
    if index < 0 or index >= len(recipes):
        return None
    return recipes[index]


def _parse_item_meta(token: str) -> tuple[str, int] | None:
    if "@" not in token:
        return None
    item, meta_s = token.rsplit("@", 1)
    try:
        return item, int(meta_s)
    except ValueError:
        return None


def _mte_metas(tokens: list[str]) -> list[int]:
    metas: list[int] = []
    for token in tokens:
        parsed = _parse_item_meta(token)
        if parsed and parsed[0] == MTE_ITEM:
            metas.append(parsed[1])
    return metas


def _dump_consumed_mte(recipe: dict[str, Any] | None) -> list[dict[str, Any]]:
    if not recipe:
        return []
    return [
        operand
        for operand in recipe.get("inputs") or []
        if str(operand.get("item") or "") == MTE_ITEM
        and int(operand.get("count") or 0) > 0
    ]


def _dump_output_mte(recipe: dict[str, Any] | None) -> list[dict[str, Any]]:
    if not recipe:
        return []
    return [
        operand
        for operand in recipe.get("outputs") or []
        if str(operand.get("item") or "") == MTE_ITEM
    ]


def _operand_ref(operand: dict[str, Any]) -> str:
    item = str(operand.get("item") or "")
    if item:
        meta = operand.get("meta")
        if meta is None:
            return item
        return f"{item}@{int(meta)}"
    fluid = str(operand.get("fluid") or "")
    amount = operand.get("amount")
    if fluid and amount is not None:
        return f"fluid:{fluid}={int(amount)}"
    return fluid or item


def prove_autoclave_ordinary_processing(
    evidence: dict[str, Any],
    lock_row: dict[str, Any],
    dump_recipe: dict[str, Any] | None,
) -> dict[str, Any] | None:
    """Prove later:recycling is the wrong owner for an autoclave family."""
    if str(lock_row.get("future_owner") or "") != "later:recycling":
        return None
    if evidence.get("eligible_phase_deferred"):
        return None
    if str(evidence.get("host") or "") != "cruciblecraft:autoclave":
        return None
    consumed = _dump_consumed_mte(dump_recipe)
    if consumed:
        return None
    item_inputs = list((dump_recipe or {}).get("inputs") or [])
    non_mte_inputs = [
        _operand_ref(operand)
        for operand in item_inputs
        if str(operand.get("item") or "") != MTE_ITEM
    ]
    fluid_inputs = [
        _operand_ref(operand)
        for operand in (dump_recipe or {}).get("fluidInputs") or []
    ]
    if not non_mte_inputs or not fluid_inputs:
        return None
    blockers = set(evidence.get("shape_blockers") or [])
    if not AUTOCLAVE_ORDINARY_BLOCKERS <= blockers:
        return None
    claimed_dismantling = "dismantling" in str(lock_row.get("reason") or "").lower()
    output_mtes = [_operand_ref(operand) for operand in _dump_output_mte(dump_recipe)]
    proof = {
        "claimed_dismantling": claimed_dismantling,
        "consumed_mte_inputs": 0,
        "dump_relation_key": str(
            (evidence.get("relations") or [{}])[0].get("relation_key") or ""
        ),
        "eligible_phase_deferred": False,
        "fluid_inputs": fluid_inputs,
        "non_mte_item_inputs": non_mte_inputs,
        "output_mte": output_mtes,
        "shape_blockers": sorted(blockers),
    }
    return {
        "evidence_hash": str(evidence.get("evidence_root_sha256") or ""),
        "new_label": "ordinary_autoclave_processing",
        "old_label": "later:recycling",
        "proof": proof,
        "recheck_condition": (
            "autoclave/deferred-recycling owns this family as ordinary "
            "autoclave processing; complete identity/B0 then compact exact, "
            "or write an independent post-1.x scope decision"
        ),
    }


def _lock_row_for(
    family_id: str,
    *,
    smelter: dict[str, dict[str, Any]],
    autoclave: dict[str, dict[str, Any]],
    centrifuge: dict[str, dict[str, Any]],
    proven_new: dict[str, dict[str, Any]],
) -> dict[str, Any]:
    for table in (smelter, autoclave, centrifuge, proven_new):
        row = table.get(family_id)
        if row:
            return row
    return {}


def _future_owner(lock_row: dict[str, Any], *, proven: bool) -> str:
    future = str(lock_row.get("future_owner") or "")
    if future:
        return future
    if proven:
        return "later:recycling"
    return ""


def _recheck(lock_row: dict[str, Any], *, proven: bool) -> str:
    recheck = str(lock_row.get("recheck_condition") or "")
    if recheck:
        return recheck
    if proven:
        return "recycling/deferred-ordinary-runtime owns this family"
    return ""


def _opening_ids(
    *,
    deferred_family_ids: list[str],
    smelter: dict[str, dict[str, Any]],
    autoclave: dict[str, dict[str, Any]],
    centrifuge: dict[str, dict[str, Any]],
    proven_new: list[dict[str, Any]],
) -> list[str]:
    ids: list[str] = []
    seen: set[str] = set()

    def _add(family_id: str) -> None:
        if family_id in seen:
            raise ValueError(f"duplicate deferred family {family_id}")
        seen.add(family_id)
        ids.append(family_id)

    for family_id in deferred_family_ids:
        _add(family_id)
    for key in SMELTER_EDGE_KEYS:
        row = next(
            (
                item
                for item in smelter.values()
                if str(item.get("template_key") or "") == key
            ),
            None,
        )
        if row is None:
            raise ValueError(f"missing smelter edge lock row {key}")
        _add(str(row["family_id"]))
    autoclave_rows = [
        row
        for row in autoclave.values()
        if str(row.get("future_owner") or "") == "later:recycling"
    ]
    if len(autoclave_rows) != AUTOCLAVE_TAGGED:
        raise ValueError(
            f"autoclave later:recycling count {len(autoclave_rows)} != {AUTOCLAVE_TAGGED}"
        )
    for row in sorted(autoclave_rows, key=lambda item: str(item["template_key"])):
        _add(str(row["family_id"]))
    for key in (CENTRIFUGE_ENVELOPE_KEY, CENTRIFUGE_CROSS_KEY):
        row = next(
            (
                item
                for item in centrifuge.values()
                if str(item.get("template_key") or "") == key
            ),
            None,
        )
        if row is None:
            raise ValueError(f"missing centrifuge lock row {key}")
        _add(str(row["family_id"]))
    if len(ids) != LEDGER_TOTAL:
        raise ValueError(f"opening id count {len(ids)} != {LEDGER_TOTAL}")
    proven_ids = {str(row["family_id"]) for row in proven_new}
    missing_new = proven_ids - set(ids)
    if missing_new:
        raise ValueError(
            "proven_new_deferred missing from opening ids: "
            + ", ".join(sorted(missing_new))
        )
    return ids


def _assign_cohort(
    *,
    family_id: str,
    template_key: str,
    evidence: dict[str, Any],
    lock_row: dict[str, Any],
    deferred_ids: set[str],
) -> tuple[str, dict[str, Any] | None]:
    if template_key == CENTRIFUGE_ENVELOPE_KEY:
        return "centrifuge_execution_envelope", None
    if template_key == CENTRIFUGE_CROSS_KEY:
        return "centrifuge_cross_mod", None
    if template_key in SMELTER_EDGE_KEYS:
        blockers = set(evidence.get("shape_blockers") or [])
        if not EDGE_BLOCKERS <= blockers:
            raise ValueError(f"{template_key} missing edge blockers {sorted(blockers)}")
        return "smelter_recovery_edge", None
    if (
        str(evidence.get("host") or "") == "cruciblecraft:smelter"
        and evidence.get("eligible_phase_deferred") is True
        and str(evidence.get("reason") or "") == "proven_consumed_mte_material_recovery"
        and family_id in deferred_ids
    ):
        return "smelter_proven_mte_recovery", None
    if str(evidence.get("host") or "") == "cruciblecraft:autoclave":
        relation_key = str(
            (evidence.get("relations") or [{}])[0].get("relation_key") or ""
        )
        dump_recipe = _relation_dump(relation_key)
        reclass = prove_autoclave_ordinary_processing(evidence, lock_row, dump_recipe)
        if reclass:
            return "mislabeled_needs_reclass", reclass
        return "autoclave_tagged_recycling", None
    raise ValueError(f"unpartitioned deferred family {family_id}")


def _family_row(
    family_id: str,
    *,
    evidence: dict[str, Any],
    lock_row: dict[str, Any],
    deferred_ids: set[str],
    surfaces: list[str],
) -> dict[str, Any]:
    template_key = str(
        evidence.get("template_key") or lock_row.get("template_key") or ""
    )
    relations = list(evidence.get("relations") or [])
    relation_keys = [str(row.get("relation_key") or "") for row in relations]
    evidence_hashes = [str(row.get("row_sha256") or "") for row in relations]
    lock_hashes = [str(value) for value in lock_row.get("relation_hashes") or []]
    proven = bool(evidence.get("eligible_phase_deferred"))
    opening_future = _future_owner(lock_row, proven=proven)
    cohort, reclass = _assign_cohort(
        family_id=family_id,
        template_key=template_key,
        evidence=evidence,
        lock_row=lock_row,
        deferred_ids=deferred_ids,
    )
    if reclass:
        future_owner = "autoclave/deferred-recycling"
        recheck = str(reclass["recheck_condition"])
    else:
        future_owner = opening_future
        recheck = _recheck(lock_row, proven=proven)
    input_objects: list[str] = []
    for relation in relations:
        input_objects.extend(str(token) for token in relation.get("input_objects") or [])
    return {
        "cohort": cohort,
        "eligible_phase_deferred": proven,
        "family_id": family_id,
        "future_owner": future_owner,
        "host": str(evidence.get("host") or lock_row.get("host") or ""),
        "input_objects": sorted(set(input_objects)),
        "opening_future_owner": opening_future,
        "output_materials": list(evidence.get("output_materials") or []),
        "reason": str(evidence.get("reason") or lock_row.get("reason") or ""),
        "recheck_condition": recheck,
        "reclass": reclass,
        "relation_count": int(evidence.get("relation_count") or lock_row.get("expanded_count") or 0),
        "relation_hashes": {
            "evidence_row_sha256": evidence_hashes,
            "lock_relation_hashes": lock_hashes,
        },
        "relation_keys": relation_keys,
        "shape_blockers": list(evidence.get("shape_blockers") or []),
        "source_surfaces": list(surfaces),
        "template_key": template_key,
    }


def load_bath_identities() -> dict[int, dict[str, Any]]:
    for path in BATH_CATALOG_CANDIDATES:
        if not path.is_file():
            continue
        document = t35.load_json(path)
        rows = document.get("identities") or []
        if rows:
            return {int(row["meta"]): row for row in rows}
    raise FileNotFoundError("Bath MTE identity catalog is missing")


def identity_candidate(universe: list[dict[str, Any]]) -> dict[str, Any]:
    proven = [
        row
        for row in universe
        if row["cohort"] == "smelter_proven_mte_recovery"
    ]
    bath = load_bath_identities()
    metas: dict[int, dict[str, Any]] = {}
    duplicate_meta: list[dict[str, Any]] = []
    missing_meta: list[str] = []
    for row in proven:
        found = _mte_metas(list(row.get("input_objects") or []))
        if len(found) != 1:
            missing_meta.append(row["family_id"])
            continue
        meta = found[0]
        payload = {
            "bath_runtime_id": (bath.get(meta) or {}).get("runtime_id"),
            "family_id": row["family_id"],
            "meta": meta,
            "output_materials": row.get("output_materials") or [],
            "overlap_with_bath": meta in bath,
            "relation_key": (row.get("relation_keys") or [""])[0],
            "source_item": MTE_ITEM,
            "template_key": row["template_key"],
        }
        existing = metas.get(meta)
        if existing:
            duplicate_meta.append(
                {
                    "family_id": row["family_id"],
                    "meta": meta,
                    "other_family_id": existing["family_id"],
                }
            )
            continue
        metas[meta] = payload
    overlap = [row for row in metas.values() if row["overlap_with_bath"]]
    unique_count = len(metas)
    return {
        "bath_catalog_meta_count": len(bath),
        "bath_overlap": overlap,
        "bath_overlap_count": len(overlap),
        "duplicate_meta": duplicate_meta,
        "generated_by": GENERATED_BY,
        "identities": [metas[meta] for meta in sorted(metas)],
        "missing_single_mte_input": missing_meta,
        "proven_family_count": len(proven),
        "schema_version": 1,
        "source_item": MTE_ITEM,
        "source_revision": SOURCE_REVISION,
        "status": "IDENTITY_CANDIDATE",
        "unique_meta_count": unique_count,
        "unique_meta_equals_proven_family_count": unique_count == len(proven)
        and not duplicate_meta
        and not missing_meta,
        "wave_slug": SLUG,
    }


def enumerate_universe() -> dict[str, Any]:
    errors: list[str] = []
    remainder = t35.load_json(REMAINDER_DEFERRED)
    mixer = t35.load_json(MIXER_CENSUS)
    disposition = t35.load_json(owner.DISPOSITION_LOCK)
    evidence_by_id = _evidence_by_id()
    smelter = _lock_reclassified(SMELTER_LOCK)
    autoclave = _lock_reclassified(AUTOCLAVE_LOCK)
    centrifuge = _lock_reclassified(CENTRIFUGE_LOCK)
    proven_new = list(remainder.get("proven_new_deferred") or [])
    proven_new_by_id = {str(row["family_id"]): row for row in proven_new}
    deferred_family_ids = [str(value) for value in disposition.get("deferred_family_ids") or []]
    mixer_count = int(
        (mixer.get("remaining_ordinary") or {}).get("deferred_recycling_count") or 0
    )

    if int(remainder.get("inherited_recycling", {}).get("count") or 0) != INHERITED_RECYCLING:
        errors.append("remainder inherited recycling drifted from 1819")
    if remainder.get("inherited_recycling", {}).get("silently_discarded") is not False:
        errors.append("remainder inherited recycling was silently discarded")
    if mixer_count != INHERITED_RECYCLING:
        errors.append(f"mixer deferred_recycling_count {mixer_count} != 1819")
    if len(deferred_family_ids) != PROVEN_SMELTER:
        errors.append(
            f"disposition deferred_family_ids {len(deferred_family_ids)} != 1817"
        )
    if int(remainder.get("closing_deferred_recycling") or 0) != RECYCLING_TOTAL:
        errors.append("remainder closing_deferred_recycling drifted from 1843")
    if int(remainder.get("closing_deferred_total") or 0) != LEDGER_TOTAL:
        errors.append("remainder closing_deferred_total drifted from 1845")
    if len(proven_new) != 26:
        errors.append(f"proven_new_deferred {len(proven_new)} != 26")

    try:
        opening_ids = _opening_ids(
            deferred_family_ids=deferred_family_ids,
            smelter=smelter,
            autoclave=autoclave,
            centrifuge=centrifuge,
            proven_new=proven_new,
        )
    except ValueError as error:
        errors.append(str(error))
        opening_ids = []

    completed = _completed_ordinary_family_ids()
    surfaces = [
        "tools/waves/recipe-portfolio/ordinary-remainder-closure/deferred_ledger.json",
        "tools/t42_owner_recovery_evidence.json",
        "tools/t42_owner_disposition_lock.json",
        "tools/waves/smelter/ordinary-closure/production_lock.json",
        "tools/waves/autoclave/ordinary-closure/production_lock.json",
        "tools/waves/centrifuge/ordinary-closure/production_lock.json",
        "tools/waves/mixer/ordinary-closure/census_delta.json",
        "gt6_dump/gt6_recipe_dump/maps/gt.recipe.smelter.json",
        "gt6_dump/gt6_recipe_dump/maps/gt.recipe.autoclave.json",
        "gt6_dump/gt6_recipe_dump/maps/gt.recipe.centrifuge.json",
    ]
    universe: list[dict[str, Any]] = []
    deferred_id_set = set(deferred_family_ids)
    for family_id in opening_ids:
        evidence = evidence_by_id.get(family_id)
        if evidence is None and family_id not in (
            next(
                (
                    row["family_id"]
                    for row in centrifuge.values()
                    if row.get("template_key") == CENTRIFUGE_ENVELOPE_KEY
                ),
                "",
            ),
            next(
                (
                    row["family_id"]
                    for row in centrifuge.values()
                    if row.get("template_key") == CENTRIFUGE_CROSS_KEY
                ),
                "",
            ),
        ):
            errors.append(f"missing recovery evidence for {family_id}")
            continue
        lock_row = _lock_row_for(
            family_id,
            smelter=smelter,
            autoclave=autoclave,
            centrifuge=centrifuge,
            proven_new=proven_new_by_id,
        )
        if evidence is None:
            evidence = {
                "eligible_phase_deferred": False,
                "family_id": family_id,
                "host": "cruciblecraft:centrifuge",
                "output_materials": [],
                "reason": str(lock_row.get("reason") or ""),
                "relation_count": int(lock_row.get("expanded_count") or 0),
                "relations": [
                    {
                        "input_objects": [],
                        "relation_key": "",
                        "row_sha256": hash_value,
                    }
                    for hash_value in lock_row.get("relation_hashes") or []
                ],
                "shape_blockers": [],
                "template_key": str(lock_row.get("template_key") or ""),
            }
        try:
            universe.append(
                _family_row(
                    family_id,
                    evidence=evidence,
                    lock_row=lock_row,
                    deferred_ids=deferred_id_set,
                    surfaces=surfaces,
                )
            )
        except ValueError as error:
            errors.append(str(error))

    leaked = [row["family_id"] for row in universe if row["family_id"] in completed]
    if leaked:
        errors.append(
            "completed ordinary families re-entered deferred ledger: "
            + ", ".join(leaked[:8])
        )

    counts = Counter(row["cohort"] for row in universe)
    for cohort in COHORTS:
        counts.setdefault(cohort, 0)
    reclass_rows = [row for row in universe if row.get("reclass")]
    expected = {
        "smelter_proven_mte_recovery": PROVEN_SMELTER,
        "smelter_recovery_edge": SMELTER_EDGE,
        "centrifuge_execution_envelope": 1,
        "centrifuge_cross_mod": 1,
    }
    for cohort, value in expected.items():
        if counts[cohort] != value:
            errors.append(f"{cohort}={counts[cohort]} expected {value}")
    autoclave_total = (
        counts["autoclave_tagged_recycling"] + counts["mislabeled_needs_reclass"]
    )
    if autoclave_total != AUTOCLAVE_TAGGED:
        errors.append(
            "autoclave cohort total "
            f"{autoclave_total} expected {AUTOCLAVE_TAGGED}"
        )
    if len(universe) != LEDGER_TOTAL:
        errors.append(f"universe {len(universe)} != {LEDGER_TOTAL}")
    if counts["smelter_proven_mte_recovery"] + counts["smelter_recovery_edge"] != INHERITED_RECYCLING:
        errors.append("inherited 1819 did not expand to 1817 proven + 2 edge")
    if (
        counts["smelter_proven_mte_recovery"]
        + counts["smelter_recovery_edge"]
        + autoclave_total
        + counts["centrifuge_execution_envelope"]
        + counts["centrifuge_cross_mod"]
        != LEDGER_TOTAL
    ):
        errors.append("1817+2+24+1+1 arithmetic failed")

    candidate = identity_candidate(universe) if universe else {
        "unique_meta_equals_proven_family_count": False,
        "proven_family_count": 0,
        "unique_meta_count": 0,
        "bath_overlap_count": 0,
        "identities": [],
        "duplicate_meta": [],
        "missing_single_mte_input": [],
    }
    if universe and not candidate.get("unique_meta_equals_proven_family_count"):
        errors.append(
            "identity candidate unique meta count does not equal proven family count"
        )

    enumerated_recycling = (
        counts["smelter_proven_mte_recovery"]
        + counts["smelter_recovery_edge"]
        + autoclave_total
    )
    return {
        "candidate": candidate,
        "cohort_counts": dict(sorted(counts.items())),
        "enumerated_recycling": enumerated_recycling,
        "errors": errors,
        "inherited_recycling": {
            "authority": "mixer/ordinary-closure census remaining_ordinary.deferred_recycling_count",
            "count": INHERITED_RECYCLING,
            "enumerated": True,
            "expanded": {
                "smelter_proven_mte_recovery": counts["smelter_proven_mte_recovery"],
                "smelter_recovery_edge": counts["smelter_recovery_edge"],
            },
            "silently_discarded": False,
        },
        "reclassification_delta": len(reclass_rows),
        "reclassified": [
            {
                "family_id": row["family_id"],
                "template_key": row["template_key"],
                **row["reclass"],
            }
            for row in reclass_rows
        ],
        "universe": universe,
    }


def partition_document(enumerated: dict[str, Any]) -> dict[str, Any]:
    buckets: dict[str, list[str]] = {cohort: [] for cohort in COHORTS}
    for row in enumerated["universe"]:
        buckets[row["cohort"]].append(row["family_id"])
    return {
        "cohorts": {
            cohort: {
                "count": len(family_ids),
                "family_ids": family_ids,
            }
            for cohort, family_ids in buckets.items()
        },
        "covers_exactly_once": True,
        "generated_by": GENERATED_BY,
        "ledger_total": LEDGER_TOTAL,
        "n300_exceptions": list(N300_EXCEPTIONS),
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "COHORT_PARTITION",
        "wave_slug": SLUG,
    }


def universe_document(enumerated: dict[str, Any]) -> dict[str, Any]:
    return {
        "cohort_counts": enumerated["cohort_counts"],
        "enumerated": True,
        "enumerated_recycling": enumerated["enumerated_recycling"],
        "families": enumerated["universe"],
        "family_count": len(enumerated["universe"]),
        "generated_by": GENERATED_BY,
        "inherited_recycling": enumerated["inherited_recycling"],
        "ledger_total": LEDGER_TOTAL,
        "next_child": NEXT_CHILD,
        "one_x_joint_exit": False,
        "program": PROGRAM,
        "reclassification_delta": enumerated["reclassification_delta"],
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "DEFERRED_UNIVERSE",
        "wave_slug": SLUG,
    }
