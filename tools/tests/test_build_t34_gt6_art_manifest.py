from __future__ import annotations

import copy
import hashlib
import json
import tempfile
import unittest
from pathlib import Path

from tools import build_t34_gt6_art_manifest as builder


class T34Gt6ArtManifestTest(unittest.TestCase):
    def _fixture(self) -> tuple[Path, dict, dict[str, dict]]:
        root = Path(tempfile.mkdtemp())
        self.addCleanup(lambda: __import__("shutil").rmtree(root))
        policy = json.loads(builder.POLICY.read_text(encoding="utf-8"))
        manifest_assets: list[dict] = []
        assets_by_target: dict[str, dict] = {}

        for target in policy["targets"]:
            identity = target["id"]
            slug = identity.split(":", 1)[1]
            source = target["source"]
            source_path = (
                source["source_path_prefix"]
                + ("overlay_active/" if identity == "cruciblecraft:fuel_engine" else "")
                + f"{slug}.png"
            )
            source_file = (
                root / policy["gt6_asset_root"] / source_path
            )
            source_file.parent.mkdir(parents=True, exist_ok=True)
            payload = identity.encode("utf-8")
            source_file.write_bytes(payload)
            cc_path = (
                root
                / "src/main/resources/assets/cruciblecraft/textures/block/t34"
                / f"{slug}.png"
            )
            cc_path.parent.mkdir(parents=True, exist_ok=True)
            cc_path.write_bytes(payload)
            asset = {
                "target": identity,
                "source_path": source_path,
                "cc_path": str(cc_path.relative_to(root)).replace("\\", "/"),
                "sha256": hashlib.sha256(payload).hexdigest(),
            }
            if identity == "cruciblecraft:fuel_engine":
                source_meta = Path(f"{source_file}.mcmeta")
                source_meta.write_text('{"animation":{"frametime":2}}', encoding="utf-8")
                Path(f"{cc_path}.mcmeta").write_bytes(source_meta.read_bytes())
            manifest_assets.append(asset)
            assets_by_target[identity] = asset

            class_path = root / source["class_reference"]
            class_path.parent.mkdir(parents=True, exist_ok=True)
            class_path.write_text(class_path.stem, encoding="utf-8")
            loader_path = root / source["loader_reference"]
            loader_path.parent.mkdir(parents=True, exist_ok=True)
            with loader_path.open("a", encoding="utf-8") as handle:
                handle.write(f"{source['loader_anchor']}\n")

        opening_manifest = root / policy["source_manifests"][0]["path"]
        opening_manifest.parent.mkdir(parents=True, exist_ok=True)
        opening_manifest.write_text(
            json.dumps({"schema_version": 1, "assets": manifest_assets}),
            encoding="utf-8",
        )

        written_models: set[Path] = set()
        for target in policy["targets"]:
            identity = target["id"]
            asset = assets_by_target[identity]
            texture = (
                "cruciblecraft:block/t34/"
                + identity.split(":", 1)[1]
            )
            resources = target["resources"]
            for value in [*resources["block_models"], resources["item_model"]]:
                path = root / value
                path.parent.mkdir(parents=True, exist_ok=True)
                if path in written_models:
                    continue
                written_models.add(path)
                shared_mold_model = (
                    "models/block/ceramic_mold" in value
                )
                path.write_text(
                    json.dumps(
                        {"textures": {} if shared_mold_model else {"all": texture}}
                    ),
                    encoding="utf-8",
                )
            if resources["blockstate"] is None:
                continue
            required_models = resources["state_contract"].get("required_models") or []
            required_keys = resources["state_contract"].get("required_variant_keys") or [
                "" if index == 0 else f"state={index}"
                for index in range(len(required_models))
            ]
            variants = {
                key: {"model": required_models[index % len(required_models)]}
                for index, key in enumerate(required_keys)
            }
            state_path = root / resources["blockstate"]
            state_path.parent.mkdir(parents=True, exist_ok=True)
            state_path.write_text(json.dumps({"variants": variants}), encoding="utf-8")

        return root, policy, assets_by_target

    def test_exact_raw_hash_success(self) -> None:
        root, policy, _ = self._fixture()
        evidence = builder.validate(policy, root)
        self.assertEqual(len(policy["targets"]), len(evidence["artifacts"]))

    def test_missing_or_mismatched_hash_is_rejected(self) -> None:
        root, policy, assets = self._fixture()
        mismatch = root / assets["cruciblecraft:firebrick"]["cc_path"]
        mismatch.write_bytes(b"wrong bytes")
        with self.assertRaisesRegex(ValueError, "SHA-256 mismatch"):
            builder.validate(policy, root)

    def test_forbidden_wrong_family_mapping_is_rejected(self) -> None:
        root, policy, assets = self._fixture()
        changed = copy.deepcopy(policy)
        fuel = next(
            target
            for target in changed["targets"]
            if target["id"] == "cruciblecraft:fuel_engine"
        )
        fuel["source"]["source_path_prefix"] = "textures/blocks/machines/generators/"
        asset = assets["cruciblecraft:fuel_engine"]
        wrong_source = "textures/blocks/machines/generators/burning_solid/fuel_engine.png"
        wrong_path = root / changed["gt6_asset_root"] / wrong_source
        wrong_path.parent.mkdir(parents=True, exist_ok=True)
        wrong_path.write_bytes((root / asset["cc_path"]).read_bytes())
        asset["source_path"] = wrong_source
        asset["sha256"] = hashlib.sha256(wrong_path.read_bytes()).hexdigest()
        manifest = root / changed["source_manifests"][0]["path"]
        manifest_data = json.loads(manifest.read_text(encoding="utf-8"))
        for row in manifest_data["assets"]:
            if row["target"] == "cruciblecraft:fuel_engine":
                row.update(asset)
        manifest.write_text(json.dumps(manifest_data), encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "forbidden source family"):
            builder.validate(changed, root)

    def test_missing_active_mcmeta_is_rejected(self) -> None:
        root, policy, assets = self._fixture()
        cc_path = root / assets["cruciblecraft:fuel_engine"]["cc_path"]
        Path(f"{cc_path}.mcmeta").unlink()
        with self.assertRaisesRegex(ValueError, "active-family .mcmeta"):
            builder.validate(policy, root)

    def test_missing_model_path_is_rejected(self) -> None:
        root, policy, _ = self._fixture()
        firebrick = next(
            target
            for target in policy["targets"]
            if target["id"] == "cruciblecraft:firebrick"
        )
        model = root / firebrick["resources"]["block_models"][0]
        model.unlink()
        with self.assertRaisesRegex(ValueError, "missing model path"):
            builder.validate(policy, root)

    def test_gas_generator_identity_requirement_is_enforced(self) -> None:
        root, policy, _ = self._fixture()
        changed = copy.deepcopy(policy)
        gas = next(
            target
            for target in changed["targets"]
            if target["id"] == "cruciblecraft:burning_gas_generator"
        )
        gas["source"]["gt6_identity"] = "Small Gas Generator"
        with self.assertRaisesRegex(
            ValueError, "burning_gas_generator identity requirement"
        ):
            builder.validate(changed, root)

    def test_committed_receipt_is_current_and_check_is_read_only(self) -> None:
        self.assertEqual([], builder.check())


if __name__ == "__main__":
    unittest.main()
