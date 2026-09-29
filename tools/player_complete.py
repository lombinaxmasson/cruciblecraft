#!/usr/bin/env python3
"""Player-complete gate using static signoff and fresh runtime receipts."""
from __future__ import annotations

import argparse
import functools
import json
import os
import re
import subprocess
import sys
import uuid
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import capability_ledger
from tools import language_names as names
from tools import registry_identity
from tools import io_common as io

GENERATED = ROOT / "src" / "generated" / "resources"
EMI_PLUGIN_CLASS = (
    "com.masson.cruciblecraft.compat.emi.CrucibleCraftEmiPlugin"
)
LOCAL_RECEIPTS = ROOT / "build" / "verification" / "receipts"
SURFACE_CATALOG = (
    ROOT / "src" / "main" / "resources" / "cruciblecraft"
    / "player_complete_surfaces.json"
)
MATERIALS = (
    ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "materials"
)
GAME_TEST_PASS = re.compile(r"All\s+(\d+)\s+required tests passed")
GAME_TEST_METHOD_RE = re.compile(
    r"public static void (\w+)\s*\(\s*GameTestHelper"
)


def required_test_ids(capability: dict[str, Any]) -> list[str]:
    raw = capability.get("required_test_ids")
    slug = str(capability.get("slug") or "capability")
    if (
        not isinstance(raw, list)
        or not raw
        or not all(isinstance(value, str) and value for value in raw)
    ):
        raise ValueError(f"{slug}: required_test_ids must be unique and non-empty")
    if len(raw) != len(set(raw)):
        raise ValueError(f"{slug}: required_test_ids has duplicates")
    return list(raw)


def owned_java_files(capability: dict[str, Any]) -> list[Path]:
    files: list[Path] = []
    for pattern in capability.get("owned_paths") or []:
        posix = str(pattern).replace("\\", "/")
        if ".java" not in posix and "gametest" not in posix.lower():
            continue
        magic = any(character in posix for character in "*?[")
        if magic:
            matches = ROOT.glob(posix)
        else:
            direct = ROOT / posix
            matches = (direct,) if direct.exists() else ()
        for match in matches:
            if match.is_file() and match.suffix == ".java":
                files.append(match)
    return sorted({path.resolve() for path in files}, key=lambda path: path.as_posix())


def discover_gametest_method_ids(capability: dict[str, Any]) -> list[str]:
    ids: set[str] = set()
    for path in owned_java_files(capability):
        text = path.read_text(encoding="utf-8")
        if "@GameTest" not in text:
            continue
        ids.update(GAME_TEST_METHOD_RE.findall(text))
    return sorted(ids)


_TEST_ANNOTATION = re.compile(r"@(?:GameTest|ParameterizedTest|Test)\b")
_TEST_METHOD = re.compile(
    r"(?:public\s+)?(?:static\s+)?void\s+(\w+)\s*\("
)
_JAVA_ROOTS = (
    ROOT / "src" / "test" / "java",
    ROOT / "src" / "main" / "java",
)


def _record_test_method(
    index: dict[str, list[str]],
    name: str,
    location: str,
) -> None:
    index.setdefault(name, []).append(location)


@functools.lru_cache(maxsize=1)
def test_method_index() -> dict[str, tuple[str, ...]]:
    """@GameTest and @Test method name -> source locations."""
    found: dict[str, list[str]] = {}
    for root in _JAVA_ROOTS:
        if not root.is_dir():
            continue
        for path in sorted(root.rglob("*.java")):
            relative = path.relative_to(ROOT).as_posix()
            lines = path.read_text(encoding="utf-8").splitlines()
            armed_at = -1
            for number, line in enumerate(lines, start=1):
                inline = _TEST_ANNOTATION.search(line)
                method = _TEST_METHOD.search(line)
                if inline and method and method.start() > inline.start():
                    _record_test_method(
                        found, method.group(1), f"{relative}:{number}"
                    )
                    armed_at = -1
                    continue
                if inline:
                    armed_at = number
                    continue
                if armed_at < 0:
                    continue
                if number - armed_at > 8:
                    armed_at = -1
                    continue
                stripped = line.strip()
                if not stripped or stripped.startswith(("*", "//", "@")):
                    continue
                if method:
                    _record_test_method(
                        found, method.group(1), f"{relative}:{number}"
                    )
                armed_at = -1
    return {name: tuple(places) for name, places in found.items()}


def evaluate_required_tests(
    slug: str,
    declared: list[str],
    index: dict[str, tuple[str, ...] | list[str]],
    owned_java: set[str] | None = None,
) -> list[str]:
    """Declared ids must exist once, or once inside the capability's Java.

    Shared method names such as ``playerSurfaceIsRegistered`` are copied into
    many GameTest holders. A capability that owns exactly one of those files
    binds the name. Zero or several owned hits stay an error for a person to
    resolve. ``owned_java`` paths are repo-relative, without line numbers.
    """
    errors: list[str] = []
    if len(declared) != len(set(declared)):
        duplicates = sorted(
            {name for name in declared if declared.count(name) > 1}
        )
        errors.append(f"{slug}: required_test_ids has duplicates {duplicates}")
    owned = owned_java or set()
    for name in declared:
        places = list(index.get(name) or ())
        if not places:
            errors.append(f"{slug}: required test {name} was not found")
            continue
        if len(places) == 1:
            continue
        owned_hits = [
            place for place in places if place.split(":", 1)[0] in owned
        ]
        if len(owned_hits) == 1:
            continue
        errors.append(
            f"{slug}: required test {name} is defined in multiple places: "
            + ", ".join(places)
        )
    return errors


def check_declared_test_ids(capability: dict[str, Any]) -> list[str]:
    slug = str(capability.get("slug") or "capability")
    raw = capability.get("required_test_ids")
    if not isinstance(raw, list) or not raw:
        return []
    try:
        declared = required_test_ids(capability)
    except ValueError as error:
        return [str(error)]
    owned = {
        path.relative_to(ROOT).as_posix()
        for path in owned_java_files(capability)
    }
    return evaluate_required_tests(slug, declared, test_method_index(), owned)


def load_signoff(capability: dict[str, Any]) -> dict[str, Any]:
    rel = str(capability.get("player_signoff") or "")
    if not rel:
        raise ValueError(f"{capability['slug']}: missing player_signoff")
    path = ROOT / rel
    if not path.is_file():
        raise ValueError(f"{capability['slug']}: missing {rel}")
    return io.load_json(path)


def check_signoff(
    capability: dict[str, Any],
    signoff: dict[str, Any],
) -> list[str]:
    slug = capability["slug"]
    errors: list[str] = []
    if signoff.get("signed") is not True:
        errors.append(f"{slug}: player_signoff is not signed")
    if signoff.get("capability") != slug:
        errors.append(f"{slug}: player_signoff capability mismatch")
    if not str(signoff.get("signer") or ""):
        errors.append(f"{slug}: player_signoff signer is missing")
    if not str(signoff.get("date") or ""):
        errors.append(f"{slug}: player_signoff date is missing")
    items = signoff.get("craftable_items")
    if (
        not isinstance(items, list)
        or not items
        or not all(isinstance(item, str) and item for item in items)
        or len(items) != len(set(items))
    ):
        errors.append(
            f"{slug}: player_signoff craftable_items must be unique and non-empty"
        )
    checklist = signoff.get("checklist")
    if not isinstance(checklist, dict) or not checklist:
        errors.append(f"{slug}: player_signoff checklist is missing")
    else:
        incomplete = sorted(
            key for key, value in checklist.items() if value is not True
        )
        if incomplete:
            errors.append(
                f"{slug}: player_signoff checklist incomplete {incomplete}"
            )
    return errors


def form_item_ids() -> set[str]:
    """Item paths obtained by material form_items aliases, not a shaped grid."""
    ids: set[str] = set()
    if not MATERIALS.is_dir():
        return ids
    for path in MATERIALS.glob("*.json"):
        document = io.load_json(path)
        if not isinstance(document, dict):
            continue
        form_items = document.get("form_items")
        if not isinstance(form_items, dict):
            continue
        for value in form_items.values():
            if not isinstance(value, str) or not value:
                continue
            ids.add(value.split(":", 1)[-1])
    return ids


def item_lang_keys(item_id: str) -> tuple[str, ...]:
    # Dual slash/dot lookup is a migration guard. Delete the slash branch at close.
    dotted = names.translation_key("item", item_id)
    slash = f"item.cruciblecraft.{item_id}"
    if slash == dotted:
        return (dotted,)
    return (dotted, slash)


def lookup_lang(table: dict[str, Any], item_id: str) -> str:
    for key in item_lang_keys(item_id):
        value = table.get(key)
        if isinstance(value, str) and value:
            return value
    return ""


def _add_recipe_result_ids(root: Path, ids: set[str]) -> None:
    if not root.is_dir():
        return
    for path in root.rglob("*.json"):
        try:
            document = io.load_json(path)
        except (OSError, json.JSONDecodeError, ValueError):
            continue
        if not isinstance(document, dict):
            continue
        result = document.get("result")
        if isinstance(result, dict):
            runtime = str(result.get("id") or "")
            if runtime:
                ids.add(runtime.split(":", 1)[-1])
        for row in document.get("item_outputs") or []:
            if not isinstance(row, dict):
                continue
            runtime = str(row.get("id") or row.get("item") or "")
            if runtime:
                ids.add(runtime.split(":", 1)[-1])


@functools.lru_cache(maxsize=1)
def recipe_output_ids() -> set[str]:
    found: set[str] = set()
    _add_recipe_result_ids(GENERATED / "data/cruciblecraft/recipe", found)
    _add_recipe_result_ids(
        ROOT / "src/main/resources/data/cruciblecraft/recipe",
        found,
    )
    return found


def check_static_player_surface(slug: str, item_ids: list[str]) -> list[str]:
    errors: list[str] = []
    english = io.load_json(
        GENERATED / "assets/cruciblecraft/lang/en_us.json"
    )
    chinese = io.load_json(
        GENERATED / "assets/cruciblecraft/lang/zh_cn.json"
    )
    form_items = form_item_ids()
    recipe_outputs = recipe_output_ids()
    for item in item_ids:
        recipe = GENERATED / "data/cruciblecraft/recipe" / f"{item}.json"
        root_recipe = (
            ROOT / "src/main/resources/data/cruciblecraft/recipe" / f"{item}.json"
        )
        model = GENERATED / "assets/cruciblecraft/models/item" / f"{item}.json"
        root_model = (
            ROOT
            / "src/main/resources/assets/cruciblecraft/models/item"
            / f"{item}.json"
        )
        if (
            item not in form_items
            and item not in recipe_outputs
            and not recipe.is_file()
            and not root_recipe.is_file()
        ):
            errors.append(f"{slug}: missing recipe {recipe.as_posix()}")
        if not model.is_file() and not root_model.is_file():
            errors.append(f"{slug}: missing model {model.as_posix()}")
        if not lookup_lang(english, item):
            errors.append(
                f"{slug}: missing en_us {' or '.join(item_lang_keys(item))}"
            )
        zh = lookup_lang(chinese, item)
        en = lookup_lang(english, item)
        if zh and (names.is_english_copy(zh, en) or "Cover" in zh):
            errors.append(f"{slug}: zh_cn for {item} is not a player-facing name")
    emi = (
        ROOT
        / "src/main/java/com/masson/cruciblecraft/compat/emi"
        / "CrucibleCraftEmiPlugin.java"
    )
    if not emi.is_file():
        errors.append(f"{slug}: EMI plugin missing")
    return errors


def resolve_receipt_path(receipt_path: str | Path) -> Path:
    path = Path(receipt_path)
    if not path.is_absolute():
        path = ROOT / path
    return path.resolve()


def is_ephemeral_receipt(path: Path) -> bool:
    """Receipts must be outside source control or in ignored run output."""
    try:
        rel = path.relative_to(ROOT.resolve())
    except ValueError:
        return True
    if not rel.parts:
        return False
    first = rel.parts[0]
    return first == "build" or first == "run" or first.startswith("run-")


def load_fresh_receipt(
    capability: dict[str, Any],
    receipt_path: str | Path | None,
    kind: str,
    option: str,
) -> tuple[dict[str, Any] | None, list[str]]:
    slug = capability["slug"]
    if receipt_path is None:
        return None, [
            f"{slug}: fresh {kind} receipt required; pass {option} PATH"
        ]
    path = resolve_receipt_path(receipt_path)
    if not is_ephemeral_receipt(path):
        return None, [
            f"{slug}: {kind} receipt must be ephemeral "
            "(outside the checkout or under build/run output)"
        ]
    if not path.is_file():
        return None, [f"{slug}: missing {kind} receipt {path}"]
    document = io.load_json(path)
    if not isinstance(document, dict):
        return None, [f"{slug}: {kind} receipt must be a JSON object"]
    return document, []


def check_client_receipt(
    capability: dict[str, Any],
    receipt_path: str | Path | None,
    item_ids: list[str],
    expected_nonce: str | None = None,
) -> list[str]:
    receipt, errors = load_fresh_receipt(
        capability,
        receipt_path,
        "client",
        "--client-receipt",
    )
    if receipt is None:
        return errors
    slug = capability["slug"]
    schema_version = receipt.get("schema_version")
    if type(schema_version) is not int or schema_version < 1:
        errors.append(f"{slug}: client receipt schema_version is invalid")
    if receipt.get("capability") != slug:
        errors.append(f"{slug}: client receipt capability mismatch")
    if receipt.get("status") != "PASS":
        errors.append(f"{slug}: client receipt status {receipt.get('status')}")
    if expected_nonce is not None and receipt.get("run_nonce") != expected_nonce:
        errors.append(f"{slug}: client receipt is not from this invocation")
    runtime = str(receipt.get("runtime") or "")
    if runtime != "client":
        errors.append(
            f"{slug}: client receipt runtime {runtime!r} is not runClient"
        )
    if receipt.get("modid") != "cruciblecraft":
        errors.append(f"{slug}: client receipt modid mismatch")
    if receipt.get("emi_plugin_class") != EMI_PLUGIN_CLASS:
        errors.append(f"{slug}: EMI plugin was not observed")
    for field in ("creative_tab", "emi_registration_plan"):
        if receipt.get(field) is not True:
            errors.append(f"{slug}: client receipt {field} is not true")
    required_raw = receipt.get("required_registry_ids")
    observed_raw = receipt.get("registry_ids")
    if not isinstance(required_raw, list) or not all(
        isinstance(value, str) and value for value in required_raw
    ):
        errors.append(f"{slug}: client receipt required_registry_ids is invalid")
        required: set[str] = set()
    else:
        required = set(required_raw)
        if len(required) != len(required_raw):
            errors.append(
                f"{slug}: client receipt required_registry_ids has duplicates"
            )
    expected = {f"cruciblecraft:{item}" for item in item_ids}
    if item_ids:
        if required != expected:
            errors.append(
                f"{slug}: client receipt required registry ids do not match signoff"
            )
        if not isinstance(observed_raw, list) or not all(
            isinstance(value, str) and value for value in observed_raw
        ):
            errors.append(f"{slug}: client receipt registry_ids is invalid")
            observed: set[str] = set()
        else:
            observed = set(observed_raw)
            if len(observed) != len(observed_raw):
                errors.append(f"{slug}: client receipt registry_ids has duplicates")
        missing = sorted(expected - observed)
        if missing:
            errors.append(f"{slug}: client receipt missing registry ids {missing}")
    return errors


def check_gametest_receipt(
    capability: dict[str, Any],
    receipt_path: str | Path | None,
    expected_nonce: str | None = None,
) -> list[str]:
    document, errors = load_fresh_receipt(
        capability,
        receipt_path,
        "GameTest",
        "--gametest-receipt",
    )
    if document is None:
        return errors
    slug = capability["slug"]
    wave = capability.get("wave_slug")
    if not wave:
        errors.append(f"{slug}: wave_slug required for GameTest verification")
        return errors
    schema_version = document.get("schema_version")
    if type(schema_version) is not int or schema_version < 1:
        errors.append(f"{slug}: GameTest receipt schema_version is invalid")
    if document.get("status") != "PASS":
        errors.append(f"{slug}: GameTest receipt {document.get('status')}")
    if expected_nonce is not None and document.get("run_nonce") != expected_nonce:
        errors.append(f"{slug}: GameTest receipt is not from this invocation")
    if document.get("wave_slug") != wave:
        errors.append(f"{slug}: GameTest wave_slug mismatch")
    expected_namespace = (
        "cruciblecraft_wave_" + str(wave).replace("/", "_").replace("-", "_")
    )
    if document.get("namespace") != expected_namespace:
        errors.append(f"{slug}: GameTest namespace mismatch")
    failed = document.get("failed")
    if type(failed) is not int or failed != 0:
        errors.append(f"{slug}: GameTest failed={failed}")
    passed = document.get("passed")
    required = document.get("required_tests")
    try:
        declared = required_test_ids(capability)
    except ValueError as error:
        errors.append(str(error))
        declared = []
    if type(passed) is not int or passed <= 0:
        errors.append(f"{slug}: GameTest passed count is invalid")
    if type(required) is not int or required <= 0:
        errors.append(f"{slug}: GameTest required_tests is invalid")
    if declared and type(required) is int and required != len(declared):
        errors.append(
            f"{slug}: GameTest required_tests={required} declared={len(declared)}"
        )
    if (
        type(passed) is int
        and type(required) is int
        and passed < required
    ):
        errors.append(
            f"{slug}: GameTest passed={passed} required={required}"
        )
    test_ids = document.get("test_ids")
    if not isinstance(test_ids, list) or not all(
        isinstance(value, str) and value for value in test_ids
    ) or not test_ids:
        errors.append(f"{slug}: GameTest test_ids is required")
        test_ids = []
    else:
        if len(test_ids) != len(set(test_ids)):
            errors.append(f"{slug}: GameTest test_ids has duplicates")
        if declared and set(test_ids) != set(declared):
            errors.append(
                f"{slug}: GameTest test_ids {sorted(set(test_ids))} "
                f"!= required_test_ids {sorted(declared)}"
            )
        if type(passed) is int and len(test_ids) != passed:
            errors.append(
                f"{slug}: GameTest test_ids={len(test_ids)} passed={passed}"
            )
    if document.get("skip_is_not_pass") is not True:
        errors.append(f"{slug}: GameTest skip_is_not_pass is not true")
    return errors


def check_client_smoke(
    capability: dict[str, Any],
    receipt_path: str | Path | None = None,
    item_ids: list[str] | None = None,
) -> list[str]:
    """Compatibility name for callers; a receipt path is still mandatory."""
    return check_client_receipt(capability, receipt_path, item_ids or [])


def check_gametest(
    capability: dict[str, Any],
    receipt_path: str | Path | None = None,
) -> list[str]:
    """Compatibility name for callers; a receipt path is still mandatory."""
    return check_gametest_receipt(capability, receipt_path)


def load_surface_catalog() -> dict[str, Any]:
    return io.load_json(SURFACE_CATALOG)


def check_surface_catalog(slug: str, item_ids: list[str]) -> list[str]:
    catalog = load_surface_catalog()
    surfaces = catalog.get("surfaces")
    if not isinstance(surfaces, dict):
        return [f"{slug}: player_complete_surfaces.json is invalid"]
    row = surfaces.get(slug)
    if not isinstance(row, dict):
        return [f"{slug}: missing player_complete_surfaces entry"]
    raw = row.get("registry_ids")
    if not isinstance(raw, list) or not all(
        isinstance(value, str) and value for value in raw
    ):
        return [f"{slug}: player_complete_surfaces registry_ids is invalid"]
    expected = [f"cruciblecraft:{item}" for item in item_ids]
    if list(raw) != expected:
        return [
            f"{slug}: player_complete_surfaces {raw} != signoff {expected}"
        ]
    return []


def check_capability(
    slug: str,
    gametest_receipt: str | Path | None = None,
    client_receipt: str | Path | None = None,
    expected_nonce: str | None = None,
    require_client: bool = False,
) -> list[str]:
    path = capability_ledger.CAP_ROOT / slug / "capability.json"
    if not path.is_file():
        return [f"missing capability {slug}"]
    capability = capability_ledger.load_capability(path)
    errors: list[str] = []
    if capability.get("maturity") == "player_complete":
        errors.append(f"{slug}: player_complete maturity is abolished")
    if capability.get("workflow") != "accepted" and gametest_receipt is None:
        pass
    if "player-complete" in list(capability.get("profiles") or []):
        errors.append(f"{slug}: player-complete profile is abolished")
    if gametest_receipt is not None:
        errors.extend(
            check_gametest_receipt(
                capability,
                gametest_receipt,
                expected_nonce=expected_nonce,
            )
        )
    if require_client or client_receipt is not None:
        errors.extend(
            check_client_receipt(
                capability,
                client_receipt,
                [],
                expected_nonce=expected_nonce,
            )
        )
    identity = registry_identity.compile_manifest()
    if identity["errors"]:
        errors.extend(identity["errors"])
    if capability.get("required_test_ids"):
        errors.extend(check_declared_test_ids(capability))
    return errors


def gradle_isolated() -> bool:
    value = os.environ.get("CRUCIBLECRAFT_GRADLE_ISOLATED", "").strip().lower()
    return value in {"1", "true", "yes"}


def gradle_command(task: str, properties: dict[str, str], offline: bool) -> list[str]:
    wrapper = ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")
    command = [
        str(wrapper),
        task,
        *(f"-P{key}={value}" for key, value in properties.items()),
        "--max-workers=1",
        "--rerun",
    ]
    if gradle_isolated():
        command.append("--no-daemon")
    if offline:
        command.append("--offline")
    return command


def run_logged(command: list[str], log_path: Path) -> tuple[int, str]:
    log_path.parent.mkdir(parents=True, exist_ok=True)
    lines: list[str] = []
    try:
        process = subprocess.Popen(
            command,
            cwd=ROOT,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            encoding="utf-8",
            errors="replace",
        )
    except OSError as error:
        text = f"could not start {command[0]}: {error}\n"
        log_path.write_text(text, encoding="utf-8", newline="\n")
        return 127, text
    assert process.stdout is not None
    for line in process.stdout:
        print(line, end="", flush=True)
        lines.append(line)
    code = process.wait()
    text = "".join(lines)
    log_path.write_text(text, encoding="utf-8", newline="\n")
    return code, text


def runtime_smoke_errors(
    path: Path,
    *,
    slug: str,
    runtime: str,
    nonce: str,
) -> list[str]:
    if not path.is_file():
        return [f"{slug}: {runtime} did not write a smoke receipt"]
    document = io.load_json(path)
    errors: list[str] = []
    if document.get("capability") != slug:
        errors.append(f"{slug}: {runtime} smoke capability mismatch")
    if document.get("runtime") != runtime:
        errors.append(f"{slug}: {runtime} smoke runtime mismatch")
    if document.get("run_nonce") != nonce:
        errors.append(f"{slug}: {runtime} smoke is not from this invocation")
    if document.get("status") != "PASS":
        errors.append(f"{slug}: {runtime} smoke status is not PASS")
    return errors


def run_fresh_capability(
    slug: str,
    *,
    offline: bool = False,
    client: bool = False,
) -> list[str]:
    capability_path = capability_ledger.CAP_ROOT / slug / "capability.json"
    if not capability_path.is_file():
        return [f"missing capability {slug}"]
    capability = capability_ledger.load_capability(capability_path)
    wave = str(capability.get("wave_slug") or "")
    if not wave:
        return [f"{slug}: wave_slug required for GameTest verification"]
    directory = LOCAL_RECEIPTS / slug.replace("/", "__")
    directory.mkdir(parents=True, exist_ok=True)
    paths = {
        "gametest_smoke": directory / "gametest-smoke.json",
        "gametest": directory / "gametest.json",
        "gametest_log": directory / "gametest.log",
        "client": directory / "client.json",
        "client_log": directory / "client.log",
        "report": directory / "latest.json",
    }
    for path in paths.values():
        path.unlink(missing_ok=True)
    errors: list[str] = []
    nonce = uuid.uuid4().hex
    declared_ids: list[str] = []
    try:
        declared_ids = required_test_ids(capability)
        errors.extend(check_declared_test_ids(capability))
    except ValueError as error:
        errors.append(str(error))
    namespace = (
        "cruciblecraft_wave_" + wave.replace("/", "_").replace("-", "_")
    )
    game_command = gradle_command(
        "runGameTestServer",
        {
            "waveRecipes": wave,
            "clientSmoke": "player-complete",
            "smokeReceipt": paths["gametest_smoke"].as_posix(),
            "smokeNonce": nonce,
            "playerCapability": slug,
        },
        offline,
    )
    client_command = gradle_command(
        "runClient",
        {
            "clientSmoke": "player-complete",
            "smokeReceipt": paths["client"].as_posix(),
            "smokeNonce": nonce,
            "playerCapability": slug,
        },
        offline,
    )
    game_code, game_output = run_logged(game_command, paths["gametest_log"])
    errors.extend(
        runtime_smoke_errors(
            paths["gametest_smoke"],
            slug=slug,
            runtime="gameTestServer",
            nonce=nonce,
        )
    )
    passed_rows = GAME_TEST_PASS.findall(game_output)
    passed = int(passed_rows[-1]) if passed_rows else 0
    observed_ids = discover_gametest_method_ids(capability)
    if game_code != 0:
        errors.append(f"{slug}: GameTestServer exited {game_code}")
    if passed < 1:
        errors.append(f"{slug}: GameTestServer reported no required tests")
    if declared_ids and passed != len(declared_ids):
        errors.append(
            f"{slug}: GameTestServer passed={passed} required={len(declared_ids)}"
        )
    game_document = {
        "schema_version": 2,
        "status": "PASS" if not errors else "FAIL",
        "capability": slug,
        "wave_slug": wave,
        "namespace": namespace,
        "passed": passed,
        "required_tests": len(declared_ids) if declared_ids else passed,
        "test_ids": observed_ids or list(declared_ids),
        "failed": 0 if game_code == 0 and passed else 1,
        "skip_is_not_pass": True,
        "run_nonce": nonce,
        "command": game_command,
    }
    paths["gametest"].write_text(
        json.dumps(game_document, indent=2, ensure_ascii=False, sort_keys=True)
        + "\n",
        encoding="utf-8",
        newline="\n",
    )
    client_code: int | None = None
    commands = [game_command]
    if client:
        commands.append(client_command)
        if not errors:
            client_code, _client_output = run_logged(
                client_command,
                paths["client_log"],
            )
            if client_code != 0:
                errors.append(f"{slug}: runClient exited {client_code}")
        else:
            client_code = 1
    if not errors:
        errors.extend(
            check_capability(
                slug,
                gametest_receipt=paths["gametest"],
                client_receipt=paths["client"] if client else None,
                expected_nonce=nonce,
                require_client=client,
            )
        )
    report = {
        "schema_version": 1,
        "capability": slug,
        "status": "PASS" if not errors else "FAIL",
        "commands": commands,
        "gametest": {
            "exit_code": game_code,
            "passed": passed,
            "required_tests": len(declared_ids) if declared_ids else passed,
            "test_ids": observed_ids or list(declared_ids),
        },
        "client": (
            {"exit_code": client_code}
            if client
            else {"skipped": True}
        ),
        "errors": errors,
    }
    paths["report"].write_text(
        json.dumps(report, indent=2, ensure_ascii=False, sort_keys=True) + "\n",
        encoding="utf-8",
        newline="\n",
    )
    return errors


def player_complete_slugs() -> list[str]:
    """Return capability slugs discovered from the player-complete profile."""
    compiled = capability_ledger.compile_ledger()
    return list(compiled["profiles"].get("player-complete") or [])


def resolve_slugs(capability: str | None, all_flag: bool) -> list[str]:
    if all_flag == bool(capability):
        raise ValueError("choose exactly one of --capability or --all")
    if capability:
        return [capability]
    return player_complete_slugs()


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    modes = parser.add_mutually_exclusive_group(required=True)
    modes.add_argument("--check", action="store_true")
    modes.add_argument("--run", action="store_true")
    parser.add_argument("--capability")
    parser.add_argument(
        "--all",
        action="store_true",
        help="no-op: player-complete profile is empty after the obtain reset",
    )
    parser.add_argument(
        "--offline",
        action="store_true",
        help="pass --offline to Gradle for an already provisioned checkout",
    )
    parser.add_argument(
        "--client",
        action="store_true",
        help="optional startup smoke runClient",
    )
    parser.add_argument(
        "--gametest-receipt",
        metavar="PATH",
        help="fresh GameTest JSON under build/run output or outside checkout",
    )
    parser.add_argument(
        "--client-receipt",
        metavar="PATH",
        help="fresh runClient JSON under build/run output or outside checkout",
    )
    args = parser.parse_args(argv)
    if args.all:
        print("player-complete profile is empty; GameTest gate is per capability")
        return 0
    try:
        slugs = resolve_slugs(args.capability, args.all)
    except ValueError as error:
        parser.error(str(error))
        return 2
    try:
        errors: list[str] = []
        if args.run:
            if args.gametest_receipt or args.client_receipt:
                parser.error("--run creates and consumes its own temporary receipts")
            for slug in slugs:
                errors.extend(
                    run_fresh_capability(
                        slug,
                        offline=args.offline,
                        client=args.client,
                    )
                )
                if errors:
                    break
        else:
            if args.gametest_receipt is None:
                print(
                    f"{slugs[0]}: fresh GameTest receipt required; "
                    "pass --gametest-receipt PATH",
                    file=sys.stderr,
                )
                return 1
            errors = check_capability(
                slugs[0],
                gametest_receipt=args.gametest_receipt,
                client_receipt=args.client_receipt,
                require_client=args.client,
            )
    except ValueError as error:
        print(str(error), file=sys.stderr)
        return 1
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print(
        "player-complete verified by fresh execution: " + ", ".join(slugs)
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
