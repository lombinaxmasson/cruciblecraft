"""CI gate: every `write_text` call must declare `newline="\\n"`.

Python text mode translates "\\n" to `os.linesep` on write, so a call without
an explicit `newline=` emits CRLF on Windows and LF on Linux.  Every `--check`
path compares with `read_text()` (universal newlines) and cannot see the
difference; byte-level consumers (`read_bytes()`) can.  That combination
produced the T10 failure: `apply_t8_pipe_metadata --write` rewrote 1,774
material files as CRLF, `build_t10_preflight_projection` recorded the CRLF
`material_tree_sha256`, and `apply_t10_form_flags` rewrote them back to LF —
net churn zero, fixed-point detection fooled.
"""
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import check_text_write_newline  # noqa: E402


class TextWriteNewlineTest(unittest.TestCase):
    def test_no_write_text_without_explicit_newline(self):
        findings = []
        for script in sorted(check_text_write_newline.TOOLS.glob("*.py")):
            for lineno, snippet in check_text_write_newline.offenders(script):
                findings.append(f"tools/{script.name}:{lineno}  {snippet}")
        self.assertEqual([], findings, "\n".join(findings))

    def test_allowlist_entries_carry_a_reason(self):
        for key, reason in check_text_write_newline.ALLOWLIST.items():
            self.assertTrue(
                reason.strip(),
                f"ALLOWLIST entry {key!r} needs a reason",
            )


if __name__ == "__main__":
    unittest.main()
