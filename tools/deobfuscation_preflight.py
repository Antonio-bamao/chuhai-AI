#!/usr/bin/env python3
"""Reject deobfuscation manifests that may load or execute target classes."""

from __future__ import annotations

import argparse
import json
from pathlib import Path


FORBIDDEN_MARKERS = (
    "me.nov.threadtear.asm.vm.VM",
    "ClassLoader",
    "loadClass",
    "java.lang.reflect",
    "Method.invoke",
)


def _strings(value: object):
    if isinstance(value, str):
        yield value
    elif isinstance(value, dict):
        for key, item in value.items():
            yield str(key)
            yield from _strings(item)
    elif isinstance(value, list):
        for item in value:
            yield from _strings(item)


def validate_manifest(manifest: dict) -> list[str]:
    errors: list[str] = []
    if manifest.get("tool") != "threadtear":
        errors.append("tool must be threadtear")
    if manifest.get("bytecode_only") is not True:
        errors.append("bytecode_only must be explicitly true")
    executions = manifest.get("executions")
    if not isinstance(executions, list) or not executions:
        errors.append("executions must be a non-empty list")

    combined = "\n".join(_strings(manifest))
    for marker in FORBIDDEN_MARKERS:
        if marker.casefold() in combined.casefold():
            errors.append(f"forbidden runtime-loading marker: {marker}")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("manifest", type=Path)
    args = parser.parse_args()
    manifest = json.loads(args.manifest.read_text(encoding="utf-8"))
    errors = validate_manifest(manifest)
    print(
        json.dumps(
            {"ok": not errors, "errors": errors},
            ensure_ascii=True,
            indent=2,
        )
    )
    return 0 if not errors else 2


if __name__ == "__main__":
    raise SystemExit(main())
