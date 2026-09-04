"""Authority write-guard tests."""
from __future__ import annotations

import unittest
from pathlib import Path

from tools.tests.support import authority_sandbox
from tools import census_common as census


class AuthorityWriteGuardTest(unittest.TestCase):
    def test_direct_write_to_readiness_is_forbidden(self) -> None:
        authority_sandbox.install_write_guard()
        with self.assertRaises(authority_sandbox.AuthorityWriteError):
            census.READINESS.write_text("{}\n", encoding="utf-8")

    def test_semantic_support_root_is_forbidden(self) -> None:
        authority_sandbox.install_write_guard()
        path = census.ROOT / "src/recipe_support_generated/resources/data/cruciblecraft/recipe"
        path = path / "player_path_support/bath_mte/probe.json"
        with self.assertRaises(authority_sandbox.AuthorityWriteError):
            path.write_text("{}\n", encoding="utf-8")

    def test_sandbox_copy_is_writable(self) -> None:
        authority_sandbox.install_write_guard()
        with authority_sandbox.sandbox_copy(census.READINESS) as copy:
            copy.write_text("{}\n", encoding="utf-8")
            self.assertEqual("{}\n", copy.read_text(encoding="utf-8"))
            self.assertNotEqual(copy.resolve(), census.READINESS.resolve())


if __name__ == "__main__":
    unittest.main()
