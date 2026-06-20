#!/usr/bin/env python3
"""Decode first-pass Java string obfuscation from the CFR output.

This tool intentionally works on decompiled sources and writes analysis output.
It does not modify the original client assets.
"""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

try:
    from tools.caller_resolution import resolve_registered_caller
    from tools.decoder_spec_discovery import discover_decoder_specs
    from tools.java_source_scan import iter_java_lines, java_unescape
    from tools.string_decoder_registry import (
        DECODER_SPECS,
        decode_registered,
        register_decoder_specs,
    )
except ModuleNotFoundError:  # Direct execution: python tools/decode_java_strings.py
    from caller_resolution import resolve_registered_caller
    from decoder_spec_discovery import discover_decoder_specs
    from java_source_scan import iter_java_lines, java_unescape
    from string_decoder_registry import (
        DECODER_SPECS,
        decode_registered,
        register_decoder_specs,
    )

UNICODE_ESCAPE = re.compile(r"\\u[0-9a-fA-F]{4}")
CANDIDATE_CALL = re.compile(
    r"(?P<family>[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)+)"
    r'\(\s*"(?P<literal>(?:\\.|[^"\\])*)"\s*\)'
)


def scan_file(
    source_root: Path,
    java_file: Path,
    inventory: dict[str, dict],
    jar_path: Path | None,
) -> list[dict]:
    rows: list[dict] = []

    for java_line in iter_java_lines(source_root, java_file):
        for match in CANDIDATE_CALL.finditer(java_line.text):
            decoder_name = match.group("family")
            if decoder_name not in DECODER_SPECS and decoder_name not in inventory:
                continue

            encrypted_literal = match.group("literal")
            decoded = ""
            error = None
            caller = java_line.caller
            inventory_row = inventory.get(decoder_name, {})
            evidence = {
                "caller_basis": "cfr_lexical_scope",
                "caller_confidence": "medium",
                "definitions": inventory_row.get("definitions", []),
                "features": inventory_row.get("features", {}),
            }

            if not UNICODE_ESCAPE.search(encrypted_literal):
                decoded = java_unescape(encrypted_literal)
                status = "decoded_existing_plaintext"
                evidence["reason"] = "literal contains no Unicode escape"
            elif decoder_name in DECODER_SPECS:
                try:
                    encrypted = java_unescape(encrypted_literal)
                    if jar_path is None:
                        decoded = decode_registered(
                            decoder_name,
                            java_line.caller,
                            encrypted,
                        )
                        status = "decoded_static"
                    else:
                        resolution = resolve_registered_caller(
                            jar_path,
                            java_line.class_name,
                            java_line.method_name,
                            decoder_name,
                            encrypted,
                        )
                        evidence["caller_candidates_tested"] = (
                            resolution.candidates_tested
                        )
                        evidence["caller_score"] = resolution.score
                        evidence["caller_margin"] = resolution.margin
                        if resolution.resolved:
                            evidence["lexical_caller"] = java_line.caller
                            evidence["caller_confidence"] = (
                                "high"
                                if resolution.caller != java_line.caller
                                else "medium"
                            )
                            decoded = resolution.decoded
                            status = "decoded_static"
                            caller = resolution.caller
                        else:
                            status = "dynamic_dump_required"
                            evidence["reason"] = (
                                "classfile caller candidates did not yield "
                                "one high-confidence plaintext"
                            )
                except Exception as exc:  # pragma: no cover - defensive analysis output
                    status = "decode_error"
                    error = repr(exc)
            else:
                status = inventory_row.get("status", "unsupported_shape")
                evidence["reason"] = "decoder family is not uniquely registered"

            rows.append(
                {
                    "decoder": decoder_name,
                    "path": java_file.relative_to(source_root).as_posix(),
                    "line": java_line.line_no,
                    "caller": caller,
                    "encrypted_literal": encrypted_literal,
                    "decoded": decoded,
                    "status": status,
                    "error": error,
                    "evidence": evidence,
                }
            )

    return rows


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-root", required=True, type=Path)
    parser.add_argument("--inventory", type=Path)
    parser.add_argument("--jar", type=Path)
    parser.add_argument("--out", required=True, type=Path)
    args = parser.parse_args()

    source_root = args.source_root.resolve()
    register_decoder_specs(discover_decoder_specs(source_root))
    inventory_rows = (
        json.loads(args.inventory.read_text(encoding="utf-8"))
        if args.inventory
        else []
    )
    inventory = {row["family"]: row for row in inventory_rows}
    jar_path = args.jar.resolve() if args.jar else None
    rows: list[dict] = []
    for java_file in source_root.rglob("*.java"):
        rows.extend(scan_file(source_root, java_file, inventory, jar_path))

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(rows, ensure_ascii=True, indent=2), encoding="utf-8")

    statuses: dict[str, int] = {}
    for row in rows:
        statuses[row["status"]] = statuses.get(row["status"], 0) + 1
    print(
        json.dumps(
            {"rows": len(rows), "statuses": statuses, "out": str(args.out)},
            ensure_ascii=True,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
