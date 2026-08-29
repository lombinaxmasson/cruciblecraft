#!/usr/bin/env python3
"""Shared mutually exclusive managed-builder CLI flags."""
from __future__ import annotations

import argparse
from typing import Sequence


def add_managed_modes(parser: argparse.ArgumentParser) -> argparse.ArgumentParser:
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--check", action="store_true")
    group.add_argument("--write", action="store_true")
    group.add_argument("--rebind-currentness-only", action="store_true")
    return parser


def parse_managed(
    description: str,
    argv: Sequence[str] | None = None,
) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=description)
    add_managed_modes(parser)
    return parser.parse_args(argv)
