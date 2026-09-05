#!/usr/bin/python3
"""Slug-based wave identity for the active recipe compiler."""
from __future__ import annotations

import re
from dataclasses import dataclass
from typing import Literal

SCHEMA_FORWARD = "forward-v2"
SCHEMA_SEMANTIC = "semantic-v3"
SchemaName = Literal["forward-v2", "semantic-v3"]

MILESTONE_TOKEN = re.compile(r"^T\d{1,2}$")
SEMANTIC_SLUG = re.compile(
    r"^[a-z][a-z0-9]*(?:-[a-z0-9]+)*/[a-z][a-z0-9]*(?:-[a-z0-9]+)*$"
)
KNOWN_SEMANTIC_SLUGS = (
    "runtime-load/allocation-split",
    "smelter/ordinary-closure",
    "mixer/ordinary-closure",
    "ordinary-wave/closeout-integrity-repair",
    "ordinary-remainder/operand-foundation",
    "drying/ordinary-closure",
    "electrolyzer/ordinary-closure",
    "centrifuge/ordinary-closure",
    "autoclave/ordinary-closure",
    "compressor/ordinary-closure",
    "recipe-portfolio/ordinary-remainder-closure",
    "naming/active-semantic-migration",
    "recycling/deferred-ordinary-ledger-r0",
    "recycling/smelter-mte-identity",
    "smelter/deferred-recycling",
    "smelter/deferred-recycling-edge",
    "autoclave/deferred-recycling",
    "recycling/non-recycling-scope",
    "recycling/deferred-ordinary-runtime",
    "portfolio/one-x-exit-r0",
    "portfolio/census-disposition-replay",
    "portfolio/energy-matrix-replay",
    "portfolio/storage-currentness-replay",
    "portfolio/load-ceiling-interpretation",
    "portfolio/one-x-joint-exit",
    "portfolio/source-capability-map-r0",
    "portfolio/source-capability-inventory",
    "portfolio/source-capability-growth-order",
    "portfolio/source-capability-map",
    "portfolio/generic-recipe-generator-r0",
    "portfolio/generic-recipe-import-core",
    "portfolio/generic-recipe-import-proof",
    "portfolio/generic-recipe-generator",
    "portfolio/logistics-cover-net-r0",
    "portfolio/exclusion-reclaim-r0",
    "assembler/compact",
    "roaster/compact",
    "centrifuge/compact",
    "electrolyzer/compact",
    "assembler/wood",
    "smelter/stone",
    "block/object",
    "storage/lock",
    "bath/mte",
    "bath/remainder",
    "bath/identity",
    "bath/tiny-purified",
    "portfolio/non-ore-worldgen-r0",
    "portfolio/vanilla-replace-r0",
    "portfolio/crops-food-bees-r0",
    "runtime/item-network-core",
    "runtime/fluid-network-basic-transfer",
    "runtime/generic-network-core",
    "runtime/logistics-core",
    "runtime/display-cpu",
    "runtime/converter-catalog",
    "runtime/batteries",
)


class WaveSlugError(ValueError):
    """Wave token is not valid for the requested schema."""


@dataclass(frozen=True)
class WaveRef:
    token: str
    schema: SchemaName
    is_legacy: bool
    wave_slug: str | None
    wave_id: str | None

    def compile_key(self) -> str:
        if self.is_legacy:
            assert self.wave_id is not None
            return self.wave_id
        assert self.wave_slug is not None
        return self.wave_slug


def is_legacy_wave_id(token: str) -> bool:
    return False


def is_milestone_token(token: str) -> bool:
    return bool(MILESTONE_TOKEN.fullmatch(token))


def is_semantic_slug(token: str) -> bool:
    return bool(SEMANTIC_SLUG.fullmatch(token))


def gradle_namespace(slug: str) -> str:
    parsed = parse_wave_token(slug, schema=SCHEMA_SEMANTIC)
    assert parsed.wave_slug is not None
    return "cruciblecraft_wave_" + parsed.wave_slug.replace("/", "_").replace("-", "_")


def parse_wave_token(
    token: str,
    *,
    schema: SchemaName | None = None,
) -> WaveRef:
    raw = str(token or "").strip()
    if not raw:
        raise WaveSlugError("wave token is empty")
    if raw == "all":
        resolved = schema or SCHEMA_FORWARD
        return WaveRef(
            token=raw,
            schema=resolved,
            is_legacy=resolved != SCHEMA_SEMANTIC,
            wave_slug=None if resolved != SCHEMA_SEMANTIC else raw,
            wave_id=raw if resolved != SCHEMA_SEMANTIC else None,
        )
    if is_milestone_token(raw):
        raise WaveSlugError(
            f"active recipe schemas do not compile milestone token {raw!r}; "
            "use a host/cohort slug"
        )
    if not is_semantic_slug(raw):
        raise WaveSlugError(
            f"semantic schema requires host/cohort slug, got {raw!r}"
        )
    return WaveRef(
        token=raw,
        schema=SCHEMA_SEMANTIC,
        is_legacy=False,
        wave_slug=raw,
        wave_id=None,
    )


def reject_milestone_for_new_schema(token: str) -> None:
    parsed = parse_wave_token(token, schema=SCHEMA_SEMANTIC)
    if parsed.is_legacy:
        raise WaveSlugError("milestone tokens are not active recipe identities")
