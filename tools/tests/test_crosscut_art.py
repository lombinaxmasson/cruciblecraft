#!/usr/bin/env python3
"""Art manifests must point at real GT6 copies, not stand-in textures."""
from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from tools import crosscut_lint as lint


def _write_manifest(
    root: Path,
    rows: list[dict[str, str]],
    source: str = "gt6_referencable_port_code/gregtech6_w",
) -> None:
    directory = root / "src" / "main" / "resources" / "assets" / "cruciblecraft"
    directory.mkdir(parents=True)
    (directory / "real.png").write_bytes(b"png")
    document: dict[str, object] = {"imports": rows}
    if source:
        document["source"] = source
    (directory / "gt6_demo_art_manifest.json").write_text(
        json.dumps(document),
        encoding="utf-8",
    )


class CrosscutArtTest(unittest.TestCase):
    def test_current_tree_has_no_art_alias(self) -> None:
        errors = lint.guarded("art", lint.art_alias_errors())
        self.assertEqual([], errors)

    def test_stale_allowlist_entry_fails(self) -> None:
        errors = lint.apply_allowlist([], ["gone"])
        self.assertTrue(any("no longer a violation" in error for error in errors))

    def test_stand_in_destination_fails(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            _write_manifest(
                root,
                [
                    {
                        "destination": "assets/cruciblecraft/real.png",
                        "gt6_source": "assets/gregtech/textures/demo.png",
                    },
                    {
                        "destination": "assets/cruciblecraft/textures/block/multiblock_casing.png",
                        "gt6_source": "assets/gregtech/textures/machines/bath/front.png",
                    },
                ],
            )
            errors = lint.art_alias_errors(root)
        self.assertTrue(any("aliases multiblock_casing" in error for error in errors))
        self.assertTrue(any("destination missing" in error for error in errors))
        self.assertFalse(any("real.png" in error and "missing" in error for error in errors))

    def test_non_gt6_source_fails(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            _write_manifest(
                root,
                [
                    {
                        "destination": "assets/cruciblecraft/real.png",
                        "gt6_source": "assets/minecraft/textures/block/furnace.png",
                    }
                ],
                source="",
            )
            errors = lint.art_alias_errors(root)
        self.assertTrue(any("not under gregtech6_w" in error for error in errors))


if __name__ == "__main__":
    unittest.main()
