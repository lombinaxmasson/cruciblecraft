"""CI gate: the committed builder graph must stay acyclic and drift-free.

`tools/check_builder_graph.py` reconstructs the real read/write graph from
the committed artifacts.  Back edges (an early artifact pinning the hash of a
later builder's output) make one forward pass of
`tools/rebuild_artifacts.py` mathematically unable to converge, and manual
mirror drift silently falsifies provenance.  This test makes both defects
hard failures in the closure suite instead of a manual one-off diagnosis.
"""
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import check_builder_graph  # noqa: E402


class BuilderGraphTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.findings = check_builder_graph.collect_findings()

    def _message(self, bucket):
        return "\n".join(
            "  {reader} (#{reader_index}): {artifact} pins {pins}".format(
                **row
            )
            for row in self.findings[bucket]
        )

    def test_no_back_edges(self):
        self.assertEqual([], self.findings["back_edge"],
                         self._message("back_edge"))

    def test_no_mirror_drift(self):
        self.assertEqual([], self.findings["manual_mirror"])

    def test_no_off_policy_pins(self):
        # resolved via pre_chain_builders declared in the policy
        self.assertEqual([], self.findings["off_policy"])

    def test_output_declarations_are_complete(self):
        self.assertEqual([], self.findings["output_declaration"])


if __name__ == "__main__":
    unittest.main()
