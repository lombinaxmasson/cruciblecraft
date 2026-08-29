"""Reusable recipe bulk compiler: IR, resolver, finite templates, compact emit."""
from __future__ import annotations

from tools.recipe_bulk.emit import emit_family, family_filename, semantic_replay_key
from tools.recipe_bulk.resolver import resolve_operand
from tools.recipe_bulk.templates import expand_family

__all__ = [
    "emit_family",
    "expand_family",
    "family_filename",
    "resolve_operand",
    "semantic_replay_key",
]
