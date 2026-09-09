"""SOURCE_BACKED GT6 multiitem icons for remaining identity items.

Bind by (source_item, exact meta) to gregtech6_w `{family}/{meta}.png`.
Do not invent overlays. Do not rewrite sealed identity catalog JSON.
"""
from __future__ import annotations

from pathlib import Path
from typing import Any

from tools import census_common as census

ROOT = census.ROOT
TOOLS = census.TOOLS
SOURCE_REVISION = census.SOURCE_REVISION

GT6_W_ITEMS = (
    ROOT
    / "gt6_referencable_port_code"
    / "gregtech6_w"
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "gregtech"
    / "textures"
    / "items"
)
TEXTURE_ROOT = (
    ROOT / "src/main/resources/assets/cruciblecraft/textures/item"
)
MODEL_ROOT = ROOT / "src/main/resources/assets/cruciblecraft/models/item"
BUNDLED_BATH = (
    ROOT / "src/main/resources/data/cruciblecraft/bath_identity_catalog.json"
)
BUNDLED_SEMANTIC = (
    ROOT / "src/main/resources/data/cruciblecraft/semantic_object_catalog.json"
)
MANIFEST = TOOLS / "multiitem_art_manifest.json"
PLACEHOLDER_TEXTURE = "minecraft:item/iron_ingot"
EXPECTED_BIND_COUNT = 252
STATUS = "MULTIITEM_ART_MANIFEST"

SOURCE_FAMILY = {
    "gregtech:gt.multiitem.books": "gt.multiitem.books",
    "gregtech:gt.multiitem.bottles": "gt.multiitem.bottles",
    "gregtech:gt.multiitem.bumblebee": "gt.multiitem.bumblebee",
    "gregtech:gt.multiitem.cans": "gt.multiitem.cans",
    "gregtech:gt.multiitem.food": "gt.multiitem.food",
    "gregtech:gt.multiitem.randomtools": "gt.multiitem.randomtools",
    "gregtech:gt.multiitem.technological": "gt.multiitem.technological",
}


def source_png(source_item: str, meta: int) -> Path:
    family = SOURCE_FAMILY[source_item]
    return GT6_W_ITEMS / family / f"{meta}.png"


def source_rel(source_item: str, meta: int) -> str:
    family = SOURCE_FAMILY[source_item]
    return f"textures/items/{family}/{meta}.png"


def texture_id(registry_path: str) -> str:
    return f"cruciblecraft:item/{registry_path}"


def dest_png(registry_path: str) -> Path:
    return TEXTURE_ROOT / f"{registry_path}.png"


def dest_rel(registry_path: str) -> str:
    return census.relative(dest_png(registry_path))


def model_path(registry_path: str) -> Path:
    return MODEL_ROOT / f"{registry_path}.json"


def bindable(identity: dict[str, Any]) -> bool:
    source_item = str(identity.get("source_item") or "")
    meta = identity.get("meta")
    registry_path = str(identity.get("registry_path") or "")
    return (
        source_item in SOURCE_FAMILY
        and isinstance(meta, int)
        and bool(registry_path)
        and str(identity.get("kind") or "multiitem") == "multiitem"
    )


def model_layer0(identity: dict[str, Any]) -> str:
    if bindable(identity):
        return texture_id(str(identity["registry_path"]))
    return str(identity.get("texture") or PLACEHOLDER_TEXTURE)


def catalog_rows() -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    seen: set[str] = set()
    for catalog, path in (
        ("bath", BUNDLED_BATH),
        ("semantic", BUNDLED_SEMANTIC),
    ):
        document = census.load_json(path)
        for identity in document.get("identities") or []:
            if not bindable(identity):
                continue
            runtime = str(identity["runtime_id"])
            if runtime in seen:
                continue
            seen.add(runtime)
            rows.append(
                {
                    "catalog": catalog,
                    "meta": int(identity["meta"]),
                    "registry_path": str(identity["registry_path"]),
                    "runtime_id": runtime,
                    "source_item": str(identity["source_item"]),
                }
            )
    rows.sort(key=lambda row: str(row["runtime_id"]))
    return rows


def _sha_or_none(path: Path) -> str | None:
    if path.is_file():
        return census.sha256_file(path)
    return None


def binding_record(row: dict[str, Any]) -> dict[str, Any]:
    source_item = str(row["source_item"])
    meta = int(row["meta"])
    registry_path = str(row["registry_path"])
    dest = dest_png(registry_path)
    source = source_png(source_item, meta)
    sha = _sha_or_none(dest) or _sha_or_none(source)
    if sha is None:
        raise ValueError(
            "missing SOURCE_BACKED multiitem texture: "
            f"{source_item}@{meta} ({source_rel(source_item, meta)})"
        )
    return {
        "catalog": row["catalog"],
        "cc_path": dest_rel(registry_path),
        "meta": meta,
        "registry_path": registry_path,
        "runtime_id": str(row["runtime_id"]),
        "sha256": sha,
        "source_item": source_item,
        "source_path": source_rel(source_item, meta),
        "texture": texture_id(registry_path),
    }


def build_manifest() -> dict[str, Any]:
    identities = [binding_record(row) for row in catalog_rows()]
    families: dict[str, int] = {}
    for row in identities:
        families[str(row["source_item"])] = families.get(str(row["source_item"]), 0) + 1
    return {
        "evidence_class": "SOURCE_BACKED",
        "generated_by": "python tools/build_multiitem_art.py",
        "gt6_asset_root": (
            "gt6_referencable_port_code/gregtech6_w/"
            "src/main/resources/assets/gregtech"
        ),
        "identities": identities,
        "identity_count": len(identities),
        "note": (
            "Bath ∪ semantic multiitem icons copied from gregtech6_w by "
            "(source_item, exact meta). Catalog JSON texture fields stay "
            "iron_ingot so bath/identity identity hashes do not move. Models and PNGs "
            "are the presentation authority. MTE and leftover prefixes are "
            "out of scope."
        ),
        "schema_version": 1,
        "source_family_counts": dict(sorted(families.items())),
        "source_revision": SOURCE_REVISION,
        "status": STATUS,
    }


def write_model(registry_path: str, layer0: str) -> None:
    path = model_path(registry_path)
    path.parent.mkdir(parents=True, exist_ok=True)
    census.write_stable(
        path,
        {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": layer0},
        },
    )


def copy_source_backed(rows: list[dict[str, Any]] | None = None) -> list[str]:
    if not GT6_W_ITEMS.is_dir():
        raise ValueError(
            "gregtech6_w item textures missing; "
            f"expected {census.relative(GT6_W_ITEMS)}"
        )
    written: list[str] = []
    for row in rows or catalog_rows():
        source_item = str(row["source_item"])
        meta = int(row["meta"])
        registry_path = str(row["registry_path"])
        source = source_png(source_item, meta)
        if not source.is_file():
            raise ValueError(
                f"missing SOURCE_BACKED multiitem texture: {census.relative(source)}"
            )
        dest = dest_png(registry_path)
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(source.read_bytes())
        write_model(registry_path, texture_id(registry_path))
        written.append(census.relative(dest))
    return written


def check_models_and_pngs(document: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    identities = list(document.get("identities") or [])
    if int(document.get("identity_count") or 0) != EXPECTED_BIND_COUNT:
        errors.append(
            f"multiitem art count is {document.get('identity_count')}, "
            f"expected {EXPECTED_BIND_COUNT}"
        )
    if len(identities) != EXPECTED_BIND_COUNT:
        errors.append(
            f"multiitem art rows are {len(identities)}, expected {EXPECTED_BIND_COUNT}"
        )
    seen: set[str] = set()
    for row in identities:
        runtime = str(row.get("runtime_id") or "")
        registry_path = str(row.get("registry_path") or "")
        texture = str(row.get("texture") or "")
        if runtime in seen:
            errors.append(f"duplicate multiitem art runtime {runtime}")
        seen.add(runtime)
        if texture == PLACEHOLDER_TEXTURE:
            errors.append(f"{runtime} still uses {PLACEHOLDER_TEXTURE}")
        if texture != texture_id(registry_path):
            errors.append(f"{runtime} texture drifted")
        dest = ROOT / str(row.get("cc_path") or dest_rel(registry_path))
        if not dest.is_file():
            errors.append(f"missing copied multiitem PNG: {census.relative(dest)}")
            continue
        digest = census.sha256_file(dest)
        if digest != str(row.get("sha256") or ""):
            errors.append(f"multiitem PNG hash drifted: {census.relative(dest)}")
        model = model_path(registry_path)
        if not model.is_file():
            errors.append(f"missing multiitem model: {census.relative(model)}")
            continue
        committed = census.load_json(model)
        layer0 = str((committed.get("textures") or {}).get("layer0") or "")
        if layer0 != texture:
            errors.append(f"{runtime} model layer0 is {layer0}, expected {texture}")
    return errors
