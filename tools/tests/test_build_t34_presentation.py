from __future__ import annotations

import copy
import json
import unittest

from tools import build_t34_presentation as builder


class T34PresentationTest(unittest.TestCase):
    def _policy(self) -> dict[str, object]:
        return json.loads(builder.POLICY.read_text(encoding="utf-8"))

    @staticmethod
    def _member(policy: dict[str, object], identity: str) -> dict[str, object]:
        resources = policy["resources"]
        assert isinstance(resources, list)
        return next(
            member
            for member in resources
            if isinstance(member, dict) and member["id"] == identity
        )

    def test_committed_receipt_is_stale_and_ready_requires_no_errors(self) -> None:
        document = builder.build()
        self.assertIn(
            document["status"],
            {
                "T34_GT6_PRESENTATION_READY",
                "T34_GT6_PRESENTATION_INCOMPLETE",
            },
        )
        self.assertEqual(
            document["status"] == "T34_GT6_PRESENTATION_READY",
            not document["validation_errors"],
            msg="\n".join(document["validation_errors"]),
        )
        before = builder.OUTPUT.read_bytes()
        self.assertEqual(["tools/t34_presentation.json is stale"], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())

    def test_full_denominator_records_explicit_non_equivalence(self) -> None:
        policy = self._policy()
        resources = policy["resources"]
        assert isinstance(resources, list)
        self.assertEqual(
            builder.REQUIRED_MEMBER_IDS,
            {member["id"] for member in resources if isinstance(member, dict)},
        )
        firebox = self._member(policy, "cruciblecraft:firebox")
        self.assertIn("not GT6-identical", firebox["relationship"])
        standalone = self._member(policy, "cruciblecraft:small_gas_generator")
        self.assertEqual("not_registered", standalone["disposition"])
        self.assertEqual("burning_gas", standalone["source_family"])

    def test_registered_gas_box_receipt_pins_source_per_destination(self) -> None:
        document = builder.build()
        gas_box = next(
            resource
            for resource in document["resources"]
            if resource["id"] == "cruciblecraft:burning_gas_generator"
        )
        self.assertTrue(gas_box["imports"])
        for imported in gas_box["imports"]:
            self.assertEqual("MultiTileEntityGeneratorGas", imported["source_class"])
            self.assertEqual(
                "gt6_code/gregtech6/src/main/java/gregtech/tileentity/energy/generators/MultiTileEntityGeneratorGas.java",
                imported["source_java"],
            )
            self.assertEqual(64, len(imported["source_sha256"]))
            self.assertEqual(64, len(imported["source_java_sha256"]))

    def test_diesel_cannot_bind_a_burning_family(self) -> None:
        policy = copy.deepcopy(self._policy())
        diesel = self._member(policy, "cruciblecraft:fuel_engine")
        diesel["source_family"] = "burning_liquid"
        errors, _, _ = builder.validate(policy)
        self.assertIn(
            "cruciblecraft:fuel_engine must bind GT6 family motor_liquid, not burning_liquid",
            errors,
        )

    def test_registered_gas_box_must_bind_burning_gas(self) -> None:
        policy = copy.deepcopy(self._policy())
        gas_box = self._member(policy, "cruciblecraft:burning_gas_generator")
        gas_box["source_family"] = "motor_liquid"
        errors, _, _ = builder.validate(policy)
        self.assertIn(
            "cruciblecraft:burning_gas_generator must bind GT6 family burning_gas, not motor_liquid",
            errors,
        )

    def test_not_registered_row_cannot_bind_a_destination(self) -> None:
        policy = copy.deepcopy(self._policy())
        standalone = self._member(policy, "cruciblecraft:small_gas_generator")
        standalone["cc_binding"] = {
            "models": [
                "src/generated/resources/assets/cruciblecraft/models/block/burning_gas_generator.json"
            ]
        }
        errors, _, _ = builder.validate(policy)
        self.assertIn(
            "cruciblecraft:small_gas_generator not_registered row cannot bind a CC model or resource",
            errors,
        )

    def test_pinned_gt6_hashes_must_remain_current(self) -> None:
        policy = copy.deepcopy(self._policy())
        errors, _, _ = builder.validate(policy)
        self.assertFalse(
            [error for error in errors if "source family burning_gas raw hash" in error]
        )
        raw_files = policy["source_families"]["burning_gas"]["raw_files"]
        assert isinstance(raw_files, dict)
        raw_files["machines/generators/burning_gas/colored/front.png"] = "0" * 64
        errors, _, _ = builder.validate(policy)
        self.assertIn(
            "source family burning_gas raw hash is not current: machines/generators/burning_gas/colored/front.png",
            errors,
        )


if __name__ == "__main__":
    unittest.main()
