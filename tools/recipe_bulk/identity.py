#!/usr/bin/python3
"""Deterministic fail-closed V1 build-time identity ledger from compact-wave facts."""
from __future__ import annotations

import hashlib
from typing import Any

from tools import io_common as files
from tools.recipe_bulk.waves import ASSEMBLER_COMPACT_OPERAND_MAP, identity_only_waves, recipe_wave

STATUS = "GLOBAL_BUILD_IDENTITY_LEDGER_V1"
MAPPING_CLASSES = (
    "material_form",
    "fluid",
    "exact_item",
    "component_circuit",
    "canonical_tag",
    "proven_source_derived_alias",
    "stone_object",
    "block_object",
    "storage_identity",
    "unbound_tag",
    "unproven_alias",
    "unproven_lossy_alias",
    "stateful_identity",
)

CIRCUIT_ITEM = "gregapi:gt.integrated_circuit"
CIRCUIT_CONFIG = "cruciblecraft:circuit_config"
FIREPROOF_COMPONENT = "cruciblecraft:fireproof"
LEDGER_PATH = files.TOOLS / "global_build_identity_ledger.json"


def _historical_commons():
    from tools import roaster_common as roaster
    from tools import centrifuge_common as centrifuge
    from tools import electrolyzer_common as electrolyzer
    from tools import assembler_wood_common as assembler_wood
    from tools import smelter_stone_common as smelter_stone
    from tools import storage_common as storage
    from tools import block_object_common as block_object

    return roaster, centrifuge, electrolyzer, assembler_wood, smelter_stone, storage, block_object


def _operand_maps():
    (
        roaster,
        centrifuge,
        electrolyzer,
        assembler_wood,
        smelter_stone,
        _storage,
        block_object,
    ) = _historical_commons()
    return (
        ("assembler/compact", ASSEMBLER_COMPACT_OPERAND_MAP),
        ("roaster/compact", roaster.OPERAND_RUNTIME_MAP),
        ("centrifuge/compact", centrifuge.OPERAND_RUNTIME_MAP),
        ("electrolyzer/compact", electrolyzer.OPERAND_RUNTIME_MAP),
        ("assembler/wood", assembler_wood.OPERAND_RUNTIME_MAP),
        ("smelter/stone", smelter_stone.OPERAND_RUNTIME_MAP),
        ("block/object", block_object.OPERAND_RUNTIME_MAP),
    )


def _required_forms():
    (
        roaster,
        centrifuge,
        electrolyzer,
        assembler_wood,
        smelter_stone,
        _storage,
        block_object,
    ) = _historical_commons()
    return (
        ("roaster/compact", roaster.REQUIRED_FORMS),
        ("centrifuge/compact", centrifuge.REQUIRED_FORMS),
        ("electrolyzer/compact", electrolyzer.REQUIRED_FORMS),
        ("assembler/wood", assembler_wood.REQUIRED_FORMS),
        ("smelter/stone", smelter_stone.REQUIRED_FORMS),
        ("block/object", block_object.REQUIRED_FORMS),
    )


def _compact_sources():
    (
        roaster,
        centrifuge,
        electrolyzer,
        assembler_wood,
        smelter_stone,
        _storage,
        block_object,
    ) = _historical_commons()
    return (
        ("assembler/compact", recipe_wave("assembler/compact").source_path),
        ("roaster/compact", roaster.SOURCE),
        ("centrifuge/compact", centrifuge.SOURCE),
        ("electrolyzer/compact", electrolyzer.SOURCE),
        ("assembler/wood", assembler_wood.SOURCE),
        ("smelter/stone", smelter_stone.SOURCE),
        ("block/object", block_object.SOURCE),
    )


def __getattr__(name: str):
    if name == "OPERAND_MAPS":
        return _operand_maps()
    if name == "REQUIRED_FORMS":
        return _required_forms()
    if name == "COMPACT_SOURCES":
        return _compact_sources()
    raise AttributeError(f"module {__name__!r} has no attribute {name!r}")


class IdentityConflictError(ValueError):
    """Two authorities mapped the same source key to different runtime identities."""


def _file_hash(path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest() if path.is_file() else ""


def _meta_suffix(meta: Any) -> str:
    return f"@{meta}" if meta is not None else ""


def _sorted_unique(values: list[str]) -> list[str]:
    return sorted(set(values))


class LedgerBuilder:
    def __init__(self) -> None:
        self.records: dict[str, dict[str, Any]] = {}
        self.blockers: dict[str, dict[str, Any]] = {}

    def commit(
        self,
        *,
        source_key: str,
        target_identity: str | None,
        target_kind: str,
        mapping_class: str,
        authority: str,
        input_path: str,
        input_hash: str,
        evidence: str,
        disposition: str,
        blocker_reason: str | None = None,
    ) -> None:
        if mapping_class not in MAPPING_CLASSES:
            raise ValueError(f"unsupported mapping_class {mapping_class}")
        payload = {
            "authorities": [authority],
            "blocker_reason": blocker_reason,
            "disposition": disposition,
            "evidence": [evidence],
            "input_hashes": {input_path: input_hash} if input_hash else {},
            "mapping_class": mapping_class,
            "source_key": source_key,
            "target_identity": target_identity,
            "target_kind": target_kind,
        }
        store = self.blockers if disposition == "blocker" else self.records
        existing = store.get(source_key)
        if existing is None:
            store[source_key] = payload
            return
        if existing["target_identity"] != target_identity:
            raise IdentityConflictError(
                f"{source_key}: {existing['target_identity']} vs {target_identity} "
                f"({existing['authorities']} vs {authority})"
            )
        if existing["mapping_class"] != mapping_class:
            raise IdentityConflictError(
                f"{source_key} mapping_class {existing['mapping_class']} vs {mapping_class}"
            )
        existing["authorities"] = _sorted_unique(existing["authorities"] + [authority])
        existing["evidence"] = _sorted_unique(existing["evidence"] + [evidence])
        if input_hash:
            existing["input_hashes"][input_path] = input_hash


def _add_material_forms(builder: LedgerBuilder) -> None:
    authority_path = files.TOOLS / "material_form_authority.json"
    authority_hash = _file_hash(authority_path)
    for wave_id, path in _required_forms():
        if not path.is_file():
            continue
        document = files.load_json(path)
        forms = document.get("required_forms") or {}
        file_hash = _file_hash(path)
        for material, material_forms in forms.items():
            for form in material_forms:
                runtime = f"cruciblecraft:{material}/{form}"
                builder.commit(
                    source_key=f"material_form:{material}/{form}",
                    target_identity=runtime,
                    target_kind="material_form",
                    mapping_class="material_form",
                    authority=wave_id,
                    input_path=files.relative(path),
                    input_hash=file_hash,
                    evidence=f"required_forms:{authority_hash}",
                    disposition="proven",
                )


def _operand_source_key(authority: str, row: dict[str, Any]) -> tuple[str, str, str] | None:
    source = row.get("source") or {}
    runtime = row.get("runtime") or {}
    kind = str(runtime.get("kind") or "")
    runtime_id = runtime.get("id")
    if kind == "crafted_item" or not runtime_id:
        return None
    if kind == "fluid" or source.get("fluid"):
        fluid = source.get("fluid")
        if not fluid:
            return None
        return f"fluid:{fluid}", str(runtime_id), "fluid"
    item = source.get("item")
    if not item:
        return None
    meta = source.get("meta")
    if kind == "component_item":
        component = runtime.get("component")
        value = runtime.get("value")
        extra = f"#{component}={value}"
        return (
            f"{authority}|item:{item}{_meta_suffix(meta)}{extra}",
            f"{runtime_id}#{component}={value}",
            "component_circuit",
        )
    extra = ""
    target = str(runtime_id)
    mapping_class = "exact_item"
    components = runtime.get("components") if isinstance(runtime.get("components"), dict) else {}
    if (
        runtime.get("component") == FIREPROOF_COMPONENT
        or FIREPROOF_COMPONENT in components
    ):
        extra = "#fireproof=1"
        target = f"{runtime_id}#{FIREPROOF_COMPONENT}=1"
        mapping_class = "component_circuit"
    return (
        f"{authority}|item:{item}{_meta_suffix(meta)}{extra}",
        target,
        mapping_class,
    )


def _add_operand_maps(builder: LedgerBuilder) -> None:
    for authority, path in _operand_maps():
        if not path.is_file():
            continue
        document = files.load_json(path)
        file_hash = _file_hash(path)
        for row in document.get("operands") or []:
            parsed = _operand_source_key(authority, row)
            if parsed is None:
                continue
            source_key, target, mapping_class = parsed
            target_kind = "fluid" if mapping_class == "fluid" else (
                "component" if mapping_class == "component_circuit" else "item"
            )
            builder.commit(
                source_key=source_key,
                target_identity=target,
                target_kind=target_kind,
                mapping_class=mapping_class,
                authority=authority,
                input_path=files.relative(path),
                input_hash=file_hash,
                evidence=str(row.get("class") or row.get("reason") or "operand_runtime_map"),
                disposition="proven",
            )


def _add_stone_catalog(builder: LedgerBuilder) -> None:
    (
        _roaster,
        _centrifuge,
        _electrolyzer,
        _assembler_wood,
        smelter_stone,
        _storage,
        _block_object,
    ) = _historical_commons()
    path = smelter_stone.STONE_CATALOG
    document = files.load_json(path)
    file_hash = _file_hash(path)
    for identity in (document.get("bundled") or {}).get("identities") or []:
        source_item = identity.get("source_item")
        for variant in identity.get("variants") or []:
            meta = variant.get("meta")
            runtime = variant.get("runtime_id")
            if not source_item or not runtime:
                continue
            builder.commit(
                source_key=f"stone:{source_item}{_meta_suffix(meta)}",
                target_identity=str(runtime),
                target_kind="item",
                mapping_class="stone_object",
                authority="smelter/stone",
                input_path=files.relative(path),
                input_hash=file_hash,
                evidence=str(identity.get("stone") or source_item),
                disposition="proven",
            )


def _add_block_catalog(builder: LedgerBuilder) -> None:
    (
        _roaster,
        _centrifuge,
        _electrolyzer,
        _assembler_wood,
        _smelter_stone,
        _storage,
        block_object,
    ) = _historical_commons()
    path = block_object.BLOCK_CATALOG
    document = files.load_json(path)
    file_hash = _file_hash(path)
    for identity in document.get("identities") or []:
        source_item = identity.get("source_item")
        meta = identity.get("meta")
        runtime = identity.get("runtime_id")
        if not source_item or not runtime:
            continue
        builder.commit(
            source_key=f"block:{source_item}{_meta_suffix(meta)}",
            target_identity=str(runtime),
            target_kind="item",
            mapping_class="block_object",
            authority="block/object",
            input_path=files.relative(path),
            input_hash=file_hash,
            evidence=str(identity.get("kind") or source_item),
            disposition="proven",
        )


def _add_storage(builder: LedgerBuilder) -> None:
    (
        _roaster,
        _centrifuge,
        _electrolyzer,
        _assembler_wood,
        _smelter_stone,
        storage,
        _block_object,
    ) = _historical_commons()
    path = storage.PRODUCTION_LOCK
    document = files.load_json(path)
    file_hash = _file_hash(path)
    for row in document.get("mappings") or []:
        site = row.get("source_site_id")
        expansion = row.get("expansion_key")
        runtime = row.get("runtime_id")
        if not site or not runtime:
            continue
        builder.commit(
            source_key=f"storage:{site}|{expansion}",
            target_identity=str(runtime),
            target_kind="item",
            mapping_class="storage_identity",
            authority="storage/lock",
            input_path=files.relative(path),
            input_hash=file_hash,
            evidence=str(row.get("family") or site),
            disposition="proven",
        )


def _iter_source_operands(relation: dict[str, Any]):
    for field in ("item_inputs", "item_outputs", "fluid_inputs", "fluid_outputs"):
        for operand in relation.get(field) or []:
            yield field, operand


def _add_source_blockers(builder: LedgerBuilder) -> None:
    recovery = files.TOOLS / "roaster_player_path_recovery.json"
    if recovery.is_file():
        document = files.load_json(recovery)
        file_hash = _file_hash(recovery)
        for row in document.get("rows") or document.get("aliases") or []:
            alias = row.get("alias") if isinstance(row, dict) else None
            if alias == "AnyDiamond" or (isinstance(row, dict) and "AnyDiamond" in str(row)):
                builder.commit(
                    source_key="roaster/compact|alias:AnyDiamond",
                    target_identity=None,
                    target_kind="item",
                    mapping_class="unproven_alias",
                    authority="roaster/compact",
                    input_path=files.relative(recovery),
                    input_hash=file_hash,
                    evidence="AnyDiamond",
                    disposition="blocker",
                    blocker_reason="unproven_alias",
                )
        text = recovery.read_text(encoding="utf-8")
        if "AnyDiamond" in text:
            builder.commit(
                source_key="roaster/compact|alias:AnyDiamond",
                target_identity=None,
                target_kind="item",
                mapping_class="unproven_alias",
                authority="roaster/compact",
                input_path=files.relative(recovery),
                input_hash=file_hash,
                evidence="roaster_player_path_recovery",
                disposition="blocker",
                blocker_reason="unproven_alias",
            )
    (
        _roaster,
        centrifuge,
        _electrolyzer,
        _assembler_wood,
        _smelter_stone,
        _storage,
        _block_object,
    ) = _historical_commons()
    for authority, path in _compact_sources():
        if not path.is_file():
            continue
        document = files.load_json(path)
        file_hash = _file_hash(path)
        relative = files.relative(path)
        for relation in document.get("relations") or []:
            for field, operand in _iter_source_operands(relation):
                tag = operand.get("tag")
                runtime = str(operand.get("runtime_id") or "")
                proven = runtime.startswith(("minecraft:", "cruciblecraft:"))
                if tag:
                    if proven:
                        builder.commit(
                            source_key=f"{authority}|tag:{tag}",
                            target_identity=runtime,
                            target_kind="tag",
                            mapping_class="canonical_tag",
                            authority=authority,
                            input_path=relative,
                            input_hash=file_hash,
                            evidence=field,
                            disposition="proven",
                        )
                    else:
                        builder.commit(
                            source_key=f"{authority}|tag:{tag}",
                            target_identity=None,
                            target_kind="tag",
                            mapping_class="unbound_tag",
                            authority=authority,
                            input_path=relative,
                            input_hash=file_hash,
                            evidence=field,
                            disposition="blocker",
                            blocker_reason="tag_only",
                        )
                alias = operand.get("alias")
                mapping = operand.get("mapping")
                if (
                    mapping == "source_derived_alias"
                    and isinstance(alias, str)
                    and alias
                    and not proven
                    and not alias.startswith(("minecraft:", "cruciblecraft:"))
                ):
                    builder.commit(
                        source_key=f"{authority}|alias:{alias}",
                        target_identity=None,
                        target_kind="item",
                        mapping_class="unproven_alias",
                        authority=authority,
                        input_path=relative,
                        input_hash=file_hash,
                        evidence=field,
                        disposition="blocker",
                        blocker_reason="unproven_alias",
                    )
                if authority in {"centrifuge/compact", "electrolyzer/compact", "assembler/wood"} and centrifuge.source_operand_is_unproven_lossy_alias(
                    operand
                ):
                    source = operand.get("source") or {}
                    item = source.get("item")
                    builder.commit(
                        source_key=(
                            f"{authority}|lossy:{item}{_meta_suffix(source.get('meta'))}"
                        ),
                        target_identity=None,
                        target_kind="item",
                        mapping_class="unproven_lossy_alias",
                        authority=authority,
                        input_path=relative,
                        input_hash=file_hash,
                        evidence=field,
                        disposition="blocker",
                        blocker_reason="unproven_lossy_alias",
                    )
    lock = files.load_json(centrifuge.PRODUCTION_LOCK)
    lock_hash = _file_hash(centrifuge.PRODUCTION_LOCK)
    for row in lock.get("phase_deferred") or []:
        for obj in row.get("source_objects") or []:
            item = obj.get("item")
            builder.commit(
                source_key=f"centrifuge/compact|stateful:{item}{_meta_suffix(obj.get('meta'))}",
                target_identity=None,
                target_kind="item",
                mapping_class="stateful_identity",
                authority="centrifuge/compact",
                input_path=files.relative(centrifuge.PRODUCTION_LOCK),
                input_hash=lock_hash,
                evidence=str(row.get("reason") or "phase_deferred"),
                disposition="blocker",
                blocker_reason=str(row.get("reason") or "stateful_identity"),
            )


def build() -> dict[str, Any]:
    builder = LedgerBuilder()
    _add_material_forms(builder)
    _add_operand_maps(builder)
    _add_stone_catalog(builder)
    _add_block_catalog(builder)
    _add_storage(builder)
    _add_source_blockers(builder)
    records = [builder.records[key] for key in sorted(builder.records)]
    blockers = [builder.blockers[key] for key in sorted(builder.blockers)]
    counts: dict[str, int] = {}
    for row in records + blockers:
        counts[row["mapping_class"]] = counts.get(row["mapping_class"], 0) + 1
    return {
        "blocker_count": len(blockers),
        "blockers": blockers,
        "counts": dict(sorted(counts.items())),
        "generated_by": "python tools/build_global_build_identity_ledger.py",
        "identity_only_waves": sorted(identity_only_waves()),
        "note": (
            "V1 build-time identity ledger. Does not mint runtime IDs. "
            "Unproven alias/tag/stateful facts stay typed blockers."
        ),
        "record_count": len(records),
        "records": records,
        "schema_version": 1,
        "status": STATUS,
    }


KIND_EVIDENCE = frozenset({"DESIGN_POLICY", "SOURCE_DERIVED", "SOURCE_BACKED"})

_INDEX_CACHE: dict[str, dict[str, dict[str, Any]]] | None = None


def meta_suffix(meta: Any) -> str:
    return _meta_suffix(meta)


def load_committed_ledger(path=None) -> dict[str, Any]:
    return files.load_json(path or LEDGER_PATH)


def index_ledger(document: dict[str, Any] | None = None) -> dict[str, dict[str, dict[str, Any]]]:
    global _INDEX_CACHE
    if document is None and _INDEX_CACHE is not None:
        return _INDEX_CACHE
    payload = document if document is not None else load_committed_ledger()
    index = {
        "records": {
            str(row["source_key"]): row for row in payload.get("records") or []
        },
        "blockers": {
            str(row["source_key"]): row for row in payload.get("blockers") or []
        },
    }
    if document is None:
        _INDEX_CACHE = index
    return index


def candidate_keys(wave_id: str | None, operand: dict[str, Any]) -> list[str]:
    source = operand.get("source") or {}
    item = source.get("item") or operand.get("item")
    meta = source.get("meta")
    fluid = source.get("fluid")
    tag = operand.get("tag")
    material = operand.get("material")
    form = operand.get("form")
    keys: list[str] = []
    if fluid:
        keys.append(f"fluid:{fluid}")
    fireproof = bool(operand.get("fireproof")) or operand.get("assembler_wood_component") in {
        FIREPROOF_COMPONENT,
        "cruciblecraft:fireproof",
    } or operand.get("component") == FIREPROOF_COMPONENT
    if wave_id and item == CIRCUIT_ITEM:
        keys.append(
            f"{wave_id}|item:{item}{_meta_suffix(meta)}#{CIRCUIT_CONFIG}={meta}"
        )
    if wave_id and fireproof and item:
        keys.append(
            f"{wave_id}|item:{item}{_meta_suffix(meta)}#{FIREPROOF_COMPONENT}=1"
        )
        keys.append(f"{wave_id}|item:{item}{_meta_suffix(meta)}#fireproof=1")
    if wave_id and item:
        keys.append(f"{wave_id}|item:{item}{_meta_suffix(meta)}")
    if item:
        keys.append(f"stone:{item}{_meta_suffix(meta)}")
        keys.append(f"block:{item}{_meta_suffix(meta)}")
    if wave_id and tag:
        keys.append(f"{wave_id}|tag:{tag}")
    if material and form:
        keys.append(f"material_form:{material}/{form}")
    return keys


def lookup_first(
    wave_id: str | None,
    operand: dict[str, Any],
    *,
    index: dict[str, dict[str, dict[str, Any]]] | None = None,
) -> tuple[str, dict[str, Any]] | None:
    store = index if index is not None else index_ledger()
    for key in candidate_keys(wave_id, operand):
        record = store["records"].get(key)
        if record is not None:
            return "proven", record
        blocker = store["blockers"].get(key)
        if blocker is not None:
            return "blocker", blocker
    return None


def source_kind_from_record(operand: dict[str, Any], record: dict[str, Any]) -> str:
    for field in ("assembler_wood_class", "smelter_stone_class", "block_object_class", "expression_class"):
        value = operand.get(field)
        if value in KIND_EVIDENCE:
            return str(value)
    source = operand.get("source") or {}
    item = source.get("item") or operand.get("item")
    if item == CIRCUIT_ITEM:
        return "DESIGN_POLICY"
    mapping = operand.get("mapping")
    if item == "gregtech:gt.block.planks":
        for evidence in record.get("evidence") or []:
            if evidence in KIND_EVIDENCE:
                return str(evidence)
    if mapping == "source_derived_alias":
        return "SOURCE_DERIVED"
    published = str(operand.get("runtime_id") or "").startswith(
        ("minecraft:", "cruciblecraft:")
    )
    if not published:
        if source.get("item") == "gregtech:gt.meta.dustDiv72":
            return "SOURCE_DERIVED"
        if operand.get("form") == "small_dust" and isinstance(operand.get("material"), str):
            return "SOURCE_DERIVED"
        if mapping == "canonical_tag" and operand.get("form") and operand.get("material"):
            return "SOURCE_DERIVED"
    if mapping in {
        "registered_material_form",
        "canonical_tag",
        "proven_equivalent",
        "exact_runtime_id",
    }:
        return "SOURCE_BACKED"
    for evidence in record.get("evidence") or []:
        if evidence in KIND_EVIDENCE:
            return str(evidence)
    return "SOURCE_BACKED"


def project_target_identity(target_identity: str) -> tuple[str, dict[str, Any] | None]:
    if "#" not in target_identity:
        return target_identity, None
    base, spec = target_identity.split("#", 1)
    if "=" not in spec:
        raise ValueError(f"malformed component target_identity: {target_identity}")
    name, raw_value = spec.split("=", 1)
    if not name or not raw_value:
        raise ValueError(f"malformed component target_identity: {target_identity}")
    value: Any = int(raw_value) if raw_value.lstrip("-").isdigit() else raw_value
    return base, {name: value}


def iter_source_operands(relation: dict[str, Any]):
    for field in ("item_inputs", "item_outputs", "fluid_inputs", "fluid_outputs"):
        for operand in relation.get(field) or []:
            yield field, operand

