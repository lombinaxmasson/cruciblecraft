#!/usr/bin/python3
"""Slug-based wave identity for semantic v3 cards.

Legacy T37–T49 tokens remain valid only under the frozen historical schema.
New schema rejects any TXX token, including T50.
"""
from __future__ import annotations

import re
from dataclasses import dataclass
from typing import Literal

SCHEMA_LEGACY = "historical-v1"
SCHEMA_FORWARD = "forward-v2"
SCHEMA_SEMANTIC = "semantic-v3"
SchemaName = Literal["historical-v1", "forward-v2", "semantic-v3"]

LEGACY_WAVE = re.compile(r"^T(?:3[7-9]|4[0-9])$")
MILESTONE_TOKEN = re.compile(r"^T\d{1,2}$")
SEMANTIC_SLUG = re.compile(
    r"^[a-z][a-z0-9]*(?:-[a-z0-9]+)*/[a-z][a-z0-9]*(?:-[a-z0-9]+)*$"
)
FORBIDDEN_NEW_TOKENS = frozenset({"T50", "t50"})

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
    return bool(LEGACY_WAVE.fullmatch(token))


def is_milestone_token(token: str) -> bool:
    return bool(MILESTONE_TOKEN.fullmatch(token)) or token in FORBIDDEN_NEW_TOKENS


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
    if schema == SCHEMA_SEMANTIC or (
        schema is None and (is_semantic_slug(raw) or is_milestone_token(raw))
        and not is_legacy_wave_id(raw)
    ):
        if is_milestone_token(raw) or raw.upper() == "T50":
            raise WaveSlugError(
                f"semantic schema rejects milestone token {raw!r}; "
                "use a host/cohort slug and do not issue T50"
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
    if is_legacy_wave_id(raw):
        resolved = schema or SCHEMA_FORWARD
        if resolved == SCHEMA_SEMANTIC:
            raise WaveSlugError(f"semantic schema rejects legacy token {raw!r}")
        return WaveRef(
            token=raw,
            schema=resolved,
            is_legacy=True,
            wave_slug=None,
            wave_id=raw,
        )
    if is_milestone_token(raw):
        raise WaveSlugError(
            f"legacy schema does not compile {raw!r}; T50 is not issued"
        )
    if is_semantic_slug(raw):
        return WaveRef(
            token=raw,
            schema=SCHEMA_SEMANTIC,
            is_legacy=False,
            wave_slug=raw,
            wave_id=None,
        )
    raise WaveSlugError(f"unknown wave token {raw!r}")


def reject_t50_for_new_schema(token: str) -> None:
    parsed = parse_wave_token(token, schema=SCHEMA_SEMANTIC)
    if parsed.wave_id == "T50" or parsed.token.upper() == "T50":
        raise WaveSlugError("T50 is frozen history only")
