"""builder_cli mutual exclusion tests."""
from __future__ import annotations

import io
import unittest
from contextlib import redirect_stderr

from tools import builder_cli


class BuilderCliTest(unittest.TestCase):
    def test_requires_exactly_one_mode(self) -> None:
        with self.assertRaises(SystemExit), redirect_stderr(io.StringIO()):
            builder_cli.parse_managed("test", [])
        args = builder_cli.parse_managed("test", ["--check"])
        self.assertTrue(args.check)
        self.assertFalse(args.write)
        self.assertFalse(args.rebind_currentness_only)


if __name__ == "__main__":
    unittest.main()
