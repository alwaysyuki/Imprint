#!/usr/bin/env python3
"""
Imprint Multi-Version Build Script
Builds and verifies all 15 requested Minecraft version targets:
1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11,
26.1, 26.2, 26.3
"""

import os
import sys
import subprocess
import shutil
import zipfile
import json
from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parent
DIST_DIR = ROOT_DIR / "dist"
DIST_DIR.mkdir(exist_ok=True)

JAVA_21 = "/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
JAVA_26 = "/opt/homebrew/opt/openjdk@26/libexec/openjdk.jdk/Contents/Home"

MC1_TARGETS = [
    {"mc": "1.21",    "yarn": "1.21+build.9",    "fapi": "0.102.0+1.21"},
    {"mc": "1.21.1",  "yarn": "1.21.1+build.3",  "fapi": "0.116.17+1.21.1"},
    {"mc": "1.21.2",  "yarn": "1.21.2+build.1",  "fapi": "0.106.1+1.21.2"},
    {"mc": "1.21.3",  "yarn": "1.21.3+build.2",  "fapi": "0.114.1+1.21.3"},
    {"mc": "1.21.4",  "yarn": "1.21.4+build.8",  "fapi": "0.119.4+1.21.4"},
    {"mc": "1.21.5",  "yarn": "1.21.5+build.1",  "fapi": "0.128.2+1.21.5"},
    {"mc": "1.21.6",  "yarn": "1.21.6+build.1",  "fapi": "0.128.2+1.21.6"},
    {"mc": "1.21.7",  "yarn": "1.21.7+build.8",  "fapi": "0.129.0+1.21.7"},
    {"mc": "1.21.8",  "yarn": "1.21.8+build.1",  "fapi": "0.136.1+1.21.8"},
    {"mc": "1.21.9",  "yarn": "1.21.9+build.1",  "fapi": "0.134.1+1.21.9"},
    {"mc": "1.21.10", "yarn": "1.21.10+build.3", "fapi": "0.138.4+1.21.10"},
    {"mc": "1.21.11", "yarn": "1.21.11+build.6", "fapi": "0.141.6+1.21.11"},
]

MC26_TARGETS = [
    {"mc": "26.1", "fapi": "0.145.1+26.1"},
    {"mc": "26.2", "fapi": "0.152.1+26.2"},
    {"mc": "26.3", "fapi": "0.161.0+26.3"},
]

def build_mc1_version(target):
    mc = target["mc"]
    yarn = target["yarn"]
    fapi = target["fapi"]
    jar_name = f"imprint-1.0.0+{mc}.jar"
    dest_jar = DIST_DIR / jar_name

    print(f"\n==========================================")
    print(f"  Building Imprint for Minecraft {mc}")
    print(f"  Yarn: {yarn} | Fabric API: {fapi}")
    print(f"==========================================")

    env = os.environ.copy()
    env["JAVA_HOME"] = JAVA_21

    cmd = [
        str(ROOT_DIR / "gradlew"),
        "remapJar",
        f"-Pminecraft_version={mc}",
        f"-Pyarn_mappings={yarn}",
        f"-Pfabric_api_version={fapi}",
    ]

    res = subprocess.run(cmd, cwd=str(ROOT_DIR), env=env)
    if res.returncode != 0:
        print(f"ERROR: Build failed for Minecraft {mc}", file=sys.stderr)
        return False

    built_jar = ROOT_DIR / "build" / "libs" / jar_name
    if not built_jar.exists():
        print(f"ERROR: Expected JAR not found: {built_jar}", file=sys.stderr)
        return False

    shutil.copy2(built_jar, dest_jar)
    print(f"SUCCESS: Copied {jar_name} -> {dest_jar}")
    return True

def build_mc26_version(target):
    mc = target["mc"]
    fapi = target["fapi"]
    jar_name = f"imprint-1.0.0+{mc}.jar"
    dest_jar = DIST_DIR / jar_name

    print(f"\n==========================================")
    print(f"  Building Imprint for Minecraft {mc}")
    print(f"  Fabric API: {fapi}")
    print(f"==========================================")

    env = os.environ.copy()
    env["JAVA_HOME"] = JAVA_26

    cmd = [
        str(ROOT_DIR / "gradlew"),
        "-p", "mc26",
        "jar",
        f"-Pminecraft_version={mc}",
        f"-Pfabric_api_version={fapi}",
    ]

    res = subprocess.run(cmd, cwd=str(ROOT_DIR), env=env)
    if res.returncode != 0:
        print(f"ERROR: Build failed for Minecraft {mc}", file=sys.stderr)
        return False

    built_jar = ROOT_DIR / "mc26" / "build" / "libs" / jar_name
    if not built_jar.exists():
        print(f"ERROR: Expected JAR not found: {built_jar}", file=sys.stderr)
        return False

    shutil.copy2(built_jar, dest_jar)
    print(f"SUCCESS: Copied {jar_name} -> {dest_jar}")
    return True

def verify_jar(jar_path):
    print(f"Verifying {jar_path.name}...")
    with zipfile.ZipFile(jar_path, "r") as z:
        names = set(z.namelist())
        assert "fabric.mod.json" in names, "Missing fabric.mod.json"
        assert "dev/superior/imprint/ImprintMod.class" in names, "Missing ImprintMod.class"
        assert "assets/imprint/icon.png" in names, "Missing assets/imprint/icon.png"
        
        with z.open("fabric.mod.json") as f:
            mod_json = json.loads(f.read().decode("utf-8"))
            assert mod_json.get("id") == "imprint", f"Invalid mod id: {mod_json.get('id')}"
            assert mod_json.get("authors") == ["Superior"], f"Invalid authors: {mod_json.get('authors')}"
    print(f"Verified OK: {jar_path.name} (size: {jar_path.stat().st_size:,} bytes)")
    return True

def main():
    target_arg = sys.argv[1] if len(sys.argv) > 1 else "all"
    results = {}

    for t in MC1_TARGETS:
        mc = t["mc"]
        if target_arg != "all" and target_arg != mc:
            continue
        success = build_mc1_version(t)
        results[mc] = success

    for t in MC26_TARGETS:
        mc = t["mc"]
        if target_arg != "all" and target_arg != mc:
            continue
        success = build_mc26_version(t)
        results[mc] = success

    print("\n==========================================")
    print("           VERIFICATION SUMMARY           ")
    print("==========================================")
    all_ok = True
    for mc, ok in results.items():
        if not ok:
            print(f"❌ {mc:8s}: BUILD FAILED")
            all_ok = False
            continue
        jar = DIST_DIR / f"imprint-1.0.0+{mc}.jar"
        try:
            verify_jar(jar)
            print(f"✅ {mc:8s}: {jar.name} ({jar.stat().st_size:,} bytes)")
        except Exception as e:
            print(f"❌ {mc:8s}: Verification failed: {e}")
            all_ok = False

    if not all_ok:
        sys.exit(1)
    print("\nAll target JARs built and verified successfully!")

if __name__ == "__main__":
    main()
