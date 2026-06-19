#!/usr/bin/env python3
"""Decode invokedynamic/bootstrap call targets from CFR output.

The obfuscator stores target class, method name, and descriptor in the second
string argument of bootstrap helpers such as StartApp.Sy(...). This script
recovers those targets for static call-chain analysis only.
"""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

from decode_java_strings import i32, java_unescape


BOOTSTRAP_CALL = re.compile(
    r"(?P<owner>[A-Za-z_$][\w$]*)\.(?P<method>[A-Za-z_$][\w$]*)"
    r'\("(?P<key>(?:\\.|[^"\\])*)",\s*"(?P<payload>(?:\\.|[^"\\])*)"'
)

PACKAGE_DECL = re.compile(r"^\s*package\s+([\w.]+);")
METHOD_DECL = re.compile(
    r"^\s*(?:public|private|protected|static|final|synchronized|native|abstract|strictfp)\s+"
    r".*?\s+(?P<name>[\w$<>]+)\s*\((?P<params>[^;{}]*)\)\s*(?:throws [^{]+)?\{"
)


def decode_payload(key_text: str, payload_text: str) -> str:
    key_chars = [ord(ch) for ch in key_text]
    if len(key_chars) < 8:
        raise ValueError(f"bootstrap key too short: {key_text!r}")

    key = [
        (key_chars[1] << 16) | key_chars[0],
        (key_chars[3] << 16) | key_chars[2],
        (key_chars[5] << 16) | key_chars[4],
        (key_chars[7] << 16) | key_chars[6],
    ]
    chars = [ord(ch) for ch in payload_text]

    n3 = n4 = n5 = n6 = 0
    for idx in range(len(chars)):
        if idx % 8 == 0:
            for round_idx in range(48):
                k = key[round_idx % 4] + round_idx
                n4 = i32(n4 + n3 + i32((n3 << 6) ^ (n3 >> 8)) + k)
                n3 = i32(n3 + n4 + i32((n4 << 6) ^ (n4 >> 8)) + k)
                n6 = i32(n6 + n5 + i32((n5 << 6) ^ (n5 >> 8)) + k)
                n5 = i32(n5 + n6 + i32((n6 << 6) ^ (n6 >> 8)) + k)

        slot = idx % 8
        if slot in (0, 4):
            chars[idx] = ((n3 >> 16) ^ chars[idx]) & 0xFFFF
        elif slot in (1, 5):
            chars[idx] = (n4 ^ chars[idx]) & 0xFFFF
        elif slot in (2, 6):
            chars[idx] = ((n5 >> 16) ^ chars[idx]) & 0xFFFF
        else:
            chars[idx] = (n6 ^ chars[idx]) & 0xFFFF

    return "".join(chr(ch) for ch in chars)


def parse_target(decoded: str) -> dict:
    chars = [ord(ch) for ch in decoded]
    if len(chars) < 3:
        raise ValueError("decoded payload too short")

    kind = chars[0]
    class_len = chars[1]
    class_start = 2
    class_end = class_start + class_len
    method_len_index = class_end
    method_start = method_len_index + 1
    method_len = chars[method_len_index]
    method_end = method_start + method_len

    target_class = decoded[class_start:class_end]
    target_method = decoded[method_start:method_end]
    descriptor = decoded[method_end:]
    return {
        "kind": kind,
        "kind_name": "static" if kind == 0x00B8 else "virtual",
        "target_class": target_class,
        "target_method": target_method,
        "descriptor": descriptor,
    }


def class_name_for(source_root: Path, java_file: Path) -> str:
    rel = java_file.relative_to(source_root).with_suffix("")
    return ".".join(rel.parts)


def package_name(lines: list[str]) -> str | None:
    for line in lines[:20]:
        match = PACKAGE_DECL.match(line)
        if match:
            return match.group(1)
    return None


def scan_file(source_root: Path, java_file: Path) -> list[dict]:
    text = java_file.read_text(encoding="utf-8", errors="replace")
    lines = text.splitlines()
    cls = class_name_for(source_root, java_file)
    pkg = package_name(lines)
    if pkg and not cls.startswith(pkg + "."):
        cls = f"{pkg}.{java_file.stem}"

    current_method = "<clinit>"
    current_signature = "<clinit>"
    current_method_line = 0
    brace_depth = 0
    method_stack: list[tuple[int, str, str, int]] = []
    rows: list[dict] = []

    for line_no, line in enumerate(lines, start=1):
        while method_stack and brace_depth < method_stack[-1][0]:
            method_stack.pop()
            if method_stack:
                _, current_method, current_signature, current_method_line = method_stack[-1]
            else:
                current_method = "<clinit>"
                current_signature = "<clinit>"
                current_method_line = 0

        method_match = METHOD_DECL.match(line)
        if method_match and not line.lstrip().startswith(("if", "for", "while", "switch", "catch")):
            name = method_match.group("name")
            if name == java_file.stem:
                name = "<init>"
            current_method = name
            params = " ".join(method_match.group("params").split())
            current_signature = f"{name}({params})"
            current_method_line = line_no
            method_stack.append((brace_depth + 1, current_method, current_signature, current_method_line))

        for match in BOOTSTRAP_CALL.finditer(line):
            owner = match.group("owner")
            method = match.group("method")
            if method in {"N", "k", "v"}:
                continue
            key = java_unescape(match.group("key"))
            payload = java_unescape(match.group("payload"))
            try:
                decoded = decode_payload(key, payload)
                target = parse_target(decoded)
                error = None
            except Exception as exc:  # pragma: no cover - analysis output
                decoded = ""
                target = {}
                error = repr(exc)
            rows.append(
                {
                    "path": str(java_file),
                    "line": line_no,
                    "caller": f"{cls}{current_method}",
                    "caller_method": current_method,
                    "caller_signature": current_signature,
                    "caller_method_line": current_method_line,
                    "bootstrap": f"{owner}.{method}",
                    "key": key,
                    "decoded_payload": decoded,
                    "error": error,
                    **target,
                }
            )

        brace_depth += line.count("{") - line.count("}")

    return rows


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-root", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    args = parser.parse_args()

    source_root = args.source_root.resolve()
    rows: list[dict] = []
    for java_file in source_root.rglob("*.java"):
        rows.extend(scan_file(source_root, java_file))

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(rows, ensure_ascii=True, indent=2), encoding="utf-8")

    decoded = sum(1 for row in rows if row["error"] is None)
    print(json.dumps({"rows": len(rows), "decoded": decoded, "out": str(args.out)}, ensure_ascii=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
