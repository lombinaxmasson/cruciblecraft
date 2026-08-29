"""Authority write-guard tests."""
from __future__ import annotations

import unittest
from pathlib import Path

from tools.tests.support import authority_sandbox
from tools import t35_common as t35


class AuthorityWriteGuardTest(unittest.TestCase):
    def test_direct_write_to_readiness_is_forbidden(self) -> None:
        authority_sandbox.install_write_guard()
        with self.assertRaises(authority_sandbox.AuthorityWriteError):
            t35.READINESS.write_text("{}\n", encoding="utf-8")

    def test_sandbox_copy_is_writable(self) -> None:
        authority_sandbox.install_write_guard()
        with authority_sandbox.sandbox_copy(t35.READINESS) as copy:
            copy.write_text("{}\n", encoding="utf-8")
            self.assertEqual("{}\n", copy.read_text(encoding="utf-8"))
            self.assertNotEqual(copy.resolve(), t35.READINESS.resolve())


if __name__ == "__main__":
    unittest.main()
