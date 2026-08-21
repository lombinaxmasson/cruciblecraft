#!/usr/bin/env python3
"""Capture T31 R5b compatibility smoke evidence.

Writes tools/t31_compat_evidence.json. Does not bump mod_version.
"""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t27_common as common  # noqa: E402
from tools import t31_common as t31  # noqa: E402

OUTPUT = ROOT / "tools" / "t31_compat_evidence.json"
SAMPLES = ROOT / "tools" / "t31_compat_samples"
REPORT = ROOT / "tools" / "full_verification_report.json"
GRADLE = ROOT / ("gradlew.bat" if sys.platform == "win32" else "gradlew")
ADOPTIUM = Path(r"C:\Program Files\Eclipse Adoptium\jdk-21.0.6.7-hotspot")
DONE = "Done ("
CLIENT_MARKERS = (
    "Sound engine started",
    "Backend library: LWJGL",
    "OpenGL Vendor",
    "Reloading ResourceManager: vanilla, mod resources",
)
SKIP_BE = "Skipping BlockEntity"
GAME_DIRS = {
    "dedicated_cc_only": ROOT / "run-t31-compat-server",
    "dedicated_optional": ROOT / "run-t31-compat-optional",
    "client_cc_only": ROOT / "run-t31-compat-client",
    "client_optional": ROOT / "run-t31-compat-client-optional",
}


def gradle_env() -> dict[str, str]:
    env = os.environ.copy()
    env["PYTHONUTF8"] = "1"
    if ADOPTIUM.is_dir():
        env["JAVA_HOME"] = str(ADOPTIUM)
        env["PATH"] = str(ADOPTIUM / "bin") + os.pathsep + env.get("PATH", "")
    return env


def required_game_test_count() -> int:
    directory = ROOT / "src/main/java/com/masson/cruciblecraft/gametest"
    return sum(
        path.read_text(encoding="utf-8").count("@GameTest(")
        for path in directory.glob("*.java")
    )


def last_required_gametest_pass() -> bool:
    if not REPORT.is_file():
        return False
    document = json.loads(REPORT.read_text(encoding="utf-8"))
    game_tests = (document.get("tests") or {}).get("production_game_tests") or {}
    return (
        game_tests.get("result") == "PASS"
        and game_tests.get("required_tests") == required_game_test_count()
    )


def _prepare_server_dir(path: Path) -> None:
    path.mkdir(parents=True, exist_ok=True)
    (path / "eula.txt").write_text("eula=true\n", encoding="utf-8", newline="\n")
    properties = path / "server.properties"
    if not properties.is_file():
        properties.write_text(
            "online-mode=false\n"
            "sync-chunk-writes=true\n"
            "motd=T31 compat\n",
            encoding="utf-8",
            newline="\n",
        )


def _prepare_client_dir(path: Path) -> None:
    path.mkdir(parents=True, exist_ok=True)


def _classify(text: str) -> dict:
    lower = text.lower()
    return {
        "emi_loaded": "Creating FMLModContainer instance for [dev.emi.emi" in text,
        "jade_loaded": "Creating FMLModContainer instance for [snownee.jade" in text
        or "com.masson.cruciblecraft.compat.jade.CrucibleJadePlugin" in text,
        "kubejs_loaded": "Creating FMLModContainer instance for [" in text
        and "kubejs" in lower
        and "latvian" in lower,
        "cruciblecraft_loaded": "com.masson.cruciblecraft" in text
        and "Creating FMLModContainer" in text,
        "cruciblecraft_published": "Published recipe epoch" in text,
        "done_line": any(DONE in line for line in text.splitlines()),
        "skipped_block_entity": SKIP_BE in text,
        "failed_to_start": "Failed to start the minecraft server" in text,
    }


def _combined_text(log_path: Path, game_dir: Path) -> str:
    parts: list[str] = []
    if log_path.is_file():
        parts.append(log_path.read_text(encoding="utf-8", errors="replace"))
    latest = game_dir / "logs" / "latest.log"
    if latest.is_file():
        parts.append(latest.read_text(encoding="utf-8", errors="replace"))
    return "\n".join(parts)


def _reset_run_logs(game_dir: Path) -> None:
    logs = game_dir / "logs"
    if not logs.is_dir():
        return
    for path in (logs / "latest.log",):
        for _ in range(10):
            try:
                path.unlink(missing_ok=True)
                break
            except PermissionError:
                time.sleep(0.5)


def _run_logged(
    args: list[str],
    log_path: Path,
    *,
    game_dir: Path,
    ready: tuple[str, ...],
    timeout_s: int,
    settle_s: int = 0,
) -> dict:
    SAMPLES.mkdir(parents=True, exist_ok=True)
    log_path.parent.mkdir(parents=True, exist_ok=True)
    _reset_run_logs(game_dir)
    env = gradle_env()
    log = log_path.open("w", encoding="utf-8", errors="replace")
    try:
        proc = subprocess.Popen(
            args,
            cwd=ROOT,
            env=env,
            stdout=log,
            stderr=subprocess.STDOUT,
        )
        deadline = time.time() + timeout_s
        text = ""
        reached = False
        ready_at: float | None = None
        try:
            while time.time() < deadline:
                time.sleep(2)
                log.flush()
                text = _combined_text(log_path, game_dir)
                if any(marker in text for marker in ready):
                    if settle_s <= 0 or "Published recipe epoch" in text:
                        reached = True
                        break
                    if ready_at is None:
                        ready_at = time.time()
                    elif time.time() - ready_at >= settle_s:
                        reached = True
                        break
                if proc.poll() is not None:
                    break
        finally:
            if proc.poll() is None:
                if sys.platform == "win32":
                    subprocess.run(
                        ["taskkill", "/PID", str(proc.pid), "/T", "/F"],
                        check=False,
                        capture_output=True,
                    )
                    time.sleep(3)
                else:
                    proc.terminate()
                    try:
                        proc.wait(timeout=15)
                    except subprocess.TimeoutExpired:
                        proc.kill()
            proc.wait()
    finally:
        log.close()
    text = _combined_text(log_path, game_dir)
    classified = _classify(text)
    world = game_dir / "world"
    return {
        "command": args,
        "log": common.relative(log_path),
        "game_dir": common.relative(game_dir),
        "reached_ready": reached,
        "exit_code": proc.returncode,
        "world_present": world.is_dir() and (world / "level.dat").is_file(),
        **classified,
    }


def _junit() -> dict:
    SAMPLES.mkdir(parents=True, exist_ok=True)
    log_path = SAMPLES / "junit.log"
    command = [
        str(GRADLE),
        "--console=plain",
        "test",
        "--tests",
        "com.masson.cruciblecraft.network.MaterialConfigurationHandshakeTest",
        "--tests",
        "com.masson.cruciblecraft.content.blockentity.HopperBlockEntityTest",
        "--tests",
        "com.masson.cruciblecraft.content.blockentity.DustFunnelBlockEntityTest",
        "--tests",
        "com.masson.cruciblecraft.content.blockentity.ProcessingMachineIdentityPersistenceTest",
        "--tests",
        "com.masson.cruciblecraft.compat.emi.EmiDisplayPlanTest",
    ]
    if ADOPTIUM.is_dir():
        command.insert(1, f"-Dorg.gradle.java.home={ADOPTIUM}")
    with log_path.open("w", encoding="utf-8", errors="replace") as log:
        completed = subprocess.run(
            command,
            cwd=ROOT,
            env=gradle_env(),
            stdout=log,
            stderr=subprocess.STDOUT,
            check=False,
        )
    return {
        "command": command,
        "log": common.relative(log_path),
        "exit_code": completed.returncode,
        "passed": completed.returncode == 0,
    }


def _gradle_run(task: str, compat: str) -> list[str]:
    command = [str(GRADLE), "--console=plain", task, f"-Pt31Compat={compat}"]
    if ADOPTIUM.is_dir():
        command.insert(1, f"-Dorg.gradle.java.home={ADOPTIUM}")
    return command


def _hopper_mentions_handshake() -> bool:
    hopper_source = (
        ROOT
        / "src/main/java/com/masson/cruciblecraft/content/menu/HopperMenu.java"
    ).read_text(encoding="utf-8")
    return "handshake" in hopper_source.lower()


def _run_dedicated_cc_only() -> tuple[dict, dict]:
    _prepare_server_dir(GAME_DIRS["dedicated_cc_only"])
    print("dedicated_cc_only", flush=True)
    dedicated = _run_logged(
        _gradle_run("runServer", "ccOnly"),
        SAMPLES / "dedicated_cc_only.log",
        game_dir=GAME_DIRS["dedicated_cc_only"],
        ready=(DONE,),
        timeout_s=420,
        settle_s=20,
    )
    print("dedicated_cc_only_reload", flush=True)
    reload = _run_logged(
        _gradle_run("runServer", "ccOnly"),
        SAMPLES / "dedicated_cc_only_reload.log",
        game_dir=GAME_DIRS["dedicated_cc_only"],
        ready=(DONE,),
        timeout_s=420,
        settle_s=20,
    )
    return dedicated, reload


def build_evidence() -> dict:
    _prepare_server_dir(GAME_DIRS["dedicated_cc_only"])
    _prepare_server_dir(GAME_DIRS["dedicated_optional"])
    _prepare_client_dir(GAME_DIRS["client_cc_only"])
    _prepare_client_dir(GAME_DIRS["client_optional"])
    print("junit", flush=True)
    junit = _junit()
    dedicated, reload = _run_dedicated_cc_only()
    print("dedicated_optional", flush=True)
    optional = _run_logged(
        _gradle_run("runServer", "optional"),
        SAMPLES / "dedicated_optional.log",
        game_dir=GAME_DIRS["dedicated_optional"],
        ready=(DONE,),
        timeout_s=420,
        settle_s=20,
    )
    print("client_cc_only", flush=True)
    client = _run_logged(
        _gradle_run("runClient", "ccOnly"),
        SAMPLES / "client_cc_only.log",
        game_dir=GAME_DIRS["client_cc_only"],
        ready=CLIENT_MARKERS,
        timeout_s=420,
    )
    print("client_optional", flush=True)
    client_optional = _run_logged(
        _gradle_run("runClient", "optional"),
        SAMPLES / "client_optional.log",
        game_dir=GAME_DIRS["client_optional"],
        ready=CLIENT_MARKERS,
        timeout_s=420,
    )
    return {
        "schema_version": 1,
        "mod_version": t31.gradle_mod_version(),
        "java_home": str(ADOPTIUM) if ADOPTIUM.is_dir() else os.environ.get("JAVA_HOME"),
        "junit": junit,
        "dedicated_cc_only": dedicated,
        "dedicated_cc_only_reload": reload,
        "dedicated_optional": optional,
        "client_cc_only": client,
        "client_optional": client_optional,
        "hopper_menu_mentions_handshake": _hopper_mentions_handshake(),
        "required_game_test_count": required_game_test_count(),
        "last_required_gametest_pass": last_required_gametest_pass(),
    }


def derived_axes(evidence: dict) -> dict:
    junit_ok = bool(evidence.get("junit", {}).get("passed"))
    dedicated = evidence.get("dedicated_cc_only") or {}
    reload = evidence.get("dedicated_cc_only_reload") or {}
    optional = evidence.get("dedicated_optional") or {}
    client = evidence.get("client_cc_only") or {}
    client_opt = evidence.get("client_optional") or {}
    return {
        "save_reload": junit_ok
        and evidence.get("last_required_gametest_pass") is True
        and bool(reload.get("done_line"))
        and bool(dedicated.get("world_present"))
        and not reload.get("skipped_block_entity")
        and not reload.get("failed_to_start"),
        "dedicated_cc_only": bool(dedicated.get("done_line"))
        and bool(dedicated.get("cruciblecraft_loaded"))
        and not dedicated.get("emi_loaded")
        and not dedicated.get("jade_loaded")
        and not dedicated.get("kubejs_loaded")
        and not dedicated.get("failed_to_start"),
        "client_cc_only": bool(client.get("reached_ready"))
        and bool(client.get("cruciblecraft_loaded"))
        and not client.get("emi_loaded")
        and not client.get("kubejs_loaded"),
        "emi": bool(client_opt.get("emi_loaded")) and junit_ok,
        "jade": bool(client_opt.get("jade_loaded") or optional.get("jade_loaded")),
        "kubejs": bool(dedicated.get("done_line"))
        and not dedicated.get("kubejs_loaded")
        and not client.get("kubejs_loaded"),
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument(
        "--only",
        choices=("all", "dedicated-cc"),
        default="all",
    )
    args = parser.parse_args()
    if not args.write:
        parser.error("pass --write")
    if args.only == "dedicated-cc":
        if not OUTPUT.is_file():
            parser.error("tools/t31_compat_evidence.json missing; run without --only")
        evidence = json.loads(OUTPUT.read_text(encoding="utf-8"))
        dedicated, reload = _run_dedicated_cc_only()
        evidence["dedicated_cc_only"] = dedicated
        evidence["dedicated_cc_only_reload"] = reload
        evidence["hopper_menu_mentions_handshake"] = _hopper_mentions_handshake()
        evidence["required_game_test_count"] = required_game_test_count()
        evidence["last_required_gametest_pass"] = last_required_gametest_pass()
    else:
        evidence = build_evidence()
    evidence["axes"] = derived_axes(evidence)
    common.write_stable(OUTPUT, evidence)
    print(json.dumps(evidence["axes"], indent=2, sort_keys=True), flush=True)
    if not all(evidence["axes"].values()):
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
