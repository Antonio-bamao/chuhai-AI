#!/usr/bin/env python3
"""Inventory one-literal Java call families that may decrypt strings."""

from __future__ import annotations

import argparse
import csv
import json
import re
from dataclasses import asdict, dataclass
from pathlib import Path

try:
    from tools.java_source_scan import iter_java_lines
    from tools.string_decoder_registry import DECODER_SPECS
except ModuleNotFoundError:  # Direct execution from tools/
    from java_source_scan import iter_java_lines
    from string_decoder_registry import DECODER_SPECS


ALLOWED_STATUSES = {
    "decoded_static",
    "decoded_existing_plaintext",
    "not_string_decoder",
    "unsupported_shape",
    "dynamic_dump_required",
    "decode_error",
}

CALL = re.compile(
    r"(?P<family>[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)+)"
    r'\(\s*"(?P<literal>(?:\\.|[^"\\])*)"\s*\)'
)
UNICODE_ESCAPE = re.compile(r"\\u[0-9a-fA-F]{4}")
CLASS_DECL = re.compile(
    r"^\s*(?:(?:public|private|protected|static|final|abstract)\s+)*"
    r"(?:class|interface|enum)\s+(?P<name>[\w$]+)"
)
METHOD_DECL = re.compile(
    r"^\s*(?P<mods>(?:(?:public|private|protected|static|final|synchronized|"
    r"native|abstract|strictfp)\s+)*)"
    r"(?P<return>[\w$<>\[\].?]+)\s+"
    r"(?P<name>[\w$]+)\s*\(\s*(?:final\s+)?String\s+\w+\s*\)"
    r"\s*(?:throws [^{]+)?\{"
)


FEATURE_NAMES = (
    "returns_string",
    "static_method",
    "uses_to_char_array",
    "uses_xor_or_shift",
    "uses_base64",
    "uses_stack_trace",
    "uses_class_name",
    "uses_system_property",
    "uses_large_switch",
    "definition_missing",
    "known_decoder",
)


@dataclass(frozen=True)
class MethodDefinition:
    owner: str
    method: str
    path: str
    line: int
    return_type: str
    modifiers: str
    body: str

    @property
    def returns_string(self) -> bool:
        return self.return_type in {"String", "java.lang.String"}


@dataclass
class InventoryEntry:
    family: str
    call_count: int
    status: str
    features: dict[str, bool]
    call_samples: list[dict]
    definitions: list[dict]

    def to_dict(self) -> dict:
        return asdict(self)


def _extract_block(lines: list[str], start: int) -> str:
    block: list[str] = []
    balance = 0
    opened = False
    for line in lines[start:]:
        block.append(line)
        for char in line:
            if char == "{":
                balance += 1
                opened = True
            elif char == "}":
                balance -= 1
        if opened and balance <= 0:
            break
    return "\n".join(block)


def _scan_definitions(source_root: Path) -> dict[tuple[str, str], list[MethodDefinition]]:
    definitions: dict[tuple[str, str], list[MethodDefinition]] = {}
    for java_file in source_root.rglob("*.java"):
        lines = java_file.read_text(encoding="utf-8", errors="replace").splitlines()
        brace_depth = 0
        class_stack: list[tuple[int, str]] = []

        for index, line in enumerate(lines):
            while class_stack and brace_depth < class_stack[-1][0]:
                class_stack.pop()

            class_match = CLASS_DECL.match(line)
            if class_match:
                class_stack.append((brace_depth + 1, class_match.group("name")))

            method_match = METHOD_DECL.match(line)
            if method_match:
                owner = class_stack[-1][1] if class_stack else java_file.stem
                definition = MethodDefinition(
                    owner=owner,
                    method=method_match.group("name"),
                    path=str(java_file),
                    line=index + 1,
                    return_type=method_match.group("return"),
                    modifiers=" ".join(method_match.group("mods").split()),
                    body=_extract_block(lines, index),
                )
                definitions.setdefault((owner, definition.method), []).append(definition)

            brace_depth += line.count("{") - line.count("}")

    return definitions


def _features_for(
    family: str,
    definitions: list[MethodDefinition],
    known_decoders: set[str],
) -> dict[str, bool]:
    body = "\n".join(definition.body for definition in definitions)
    lowered = body.lower()
    return {
        "returns_string": bool(definitions)
        and all(definition.returns_string for definition in definitions),
        "static_method": bool(definitions)
        and all("static" in definition.modifiers.split() for definition in definitions),
        "uses_to_char_array": ".toCharArray(" in body,
        "uses_xor_or_shift": any(token in body for token in ("^", "<<", ">>", ">>>")),
        "uses_base64": "base64" in lowered,
        "uses_stack_trace": "getStackTrace" in body or "StackTraceElement" in body,
        "uses_class_name": any(
            token in body
            for token in ("getClassName(", ".getClass(", ".getName(")
        ),
        "uses_system_property": "System.getProperty(" in body,
        "uses_large_switch": body.count("case ") >= 8,
        "definition_missing": not definitions,
        "known_decoder": family in known_decoders,
    }


def _status_for(
    family: str,
    definitions: list[MethodDefinition],
    features: dict[str, bool],
    known_decoders: set[str],
) -> str:
    if family in known_decoders:
        return "decoded_static"
    if definitions and all(not definition.returns_string for definition in definitions):
        return "not_string_decoder"
    if not definitions or len(definitions) != 1:
        return "unsupported_shape"
    if (
        features["uses_stack_trace"]
        or features["uses_class_name"]
        or features["uses_system_property"]
    ):
        return "dynamic_dump_required"
    return "unsupported_shape"


def build_inventory(
    source_root: Path,
    known_decoders: set[str] | None = None,
) -> dict[str, InventoryEntry]:
    source_root = Path(source_root)
    known = set(DECODER_SPECS) if known_decoders is None else set(known_decoders)
    definitions = _scan_definitions(source_root)
    samples: dict[str, list[dict]] = {}
    counts: dict[str, int] = {}

    for java_file in source_root.rglob("*.java"):
        for java_line in iter_java_lines(source_root, java_file):
            for match in CALL.finditer(java_line.text):
                literal = match.group("literal")
                if not UNICODE_ESCAPE.search(literal):
                    continue
                family = match.group("family")
                counts[family] = counts.get(family, 0) + 1
                family_samples = samples.setdefault(family, [])
                if len(family_samples) < 5:
                    family_samples.append(
                        {
                            "path": str(java_file),
                            "line": java_line.line_no,
                            "caller": java_line.caller,
                            "literal": literal,
                        }
                    )

    inventory: dict[str, InventoryEntry] = {}
    for family in sorted(counts):
        owner_and_method = family.rsplit(".", 1)
        owner = owner_and_method[0].rsplit(".", 1)[-1]
        method = owner_and_method[1]
        family_definitions = definitions.get((owner, method), [])
        features = _features_for(family, family_definitions, known)
        status = _status_for(family, family_definitions, features, known)
        if status not in ALLOWED_STATUSES:
            raise AssertionError(f"invalid inventory status: {status}")
        inventory[family] = InventoryEntry(
            family=family,
            call_count=counts[family],
            status=status,
            features=features,
            call_samples=samples[family],
            definitions=[
                {
                    "owner": definition.owner,
                    "method": definition.method,
                    "path": definition.path,
                    "line": definition.line,
                    "return_type": definition.return_type,
                    "modifiers": definition.modifiers,
                }
                for definition in family_definitions
            ],
        )

    return inventory


def _write_json(path: Path, entries: list[InventoryEntry]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps([entry.to_dict() for entry in entries], ensure_ascii=True, indent=2),
        encoding="utf-8",
    )


def _write_csv(path: Path, entries: list[InventoryEntry]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8-sig", newline="") as stream:
        fieldnames = [
            "family",
            "call_count",
            "status",
            *FEATURE_NAMES,
            "definitions",
            "call_samples",
        ]
        writer = csv.DictWriter(stream, fieldnames=fieldnames)
        writer.writeheader()
        for entry in entries:
            writer.writerow(
                {
                    "family": entry.family,
                    "call_count": entry.call_count,
                    "status": entry.status,
                    **entry.features,
                    "definitions": json.dumps(entry.definitions, ensure_ascii=True),
                    "call_samples": json.dumps(entry.call_samples, ensure_ascii=True),
                }
            )


def _write_markdown(path: Path, entries: list[InventoryEntry]) -> None:
    status_counts: dict[str, int] = {}
    for entry in entries:
        status_counts[entry.status] = status_counts.get(entry.status, 0) + 1

    lines = [
        "# 字符串解码 Family 清单",
        "",
        "生成时间：2026-06-21",
        "",
        f"- Family 数：{len(entries):,}",
        f"- 调用点数：{sum(entry.call_count for entry in entries):,}",
    ]
    for status in sorted(status_counts):
        lines.append(f"- `{status}`：{status_counts[status]:,}")
    lines.extend(
        [
            "",
            "| Family | 调用数 | 状态 | 定义 | 关键特征 |",
            "|---|---:|---|---|---|",
        ]
    )
    for entry in sorted(entries, key=lambda item: (-item.call_count, item.family)):
        definition = (
            f"{entry.definitions[0]['path']}:{entry.definitions[0]['line']}"
            if entry.definitions
            else "missing"
        )
        enabled = [
            name
            for name, value in entry.features.items()
            if value and name not in {"known_decoder", "definition_missing"}
        ]
        lines.append(
            "| "
            + " | ".join(
                (
                    entry.family.replace("|", "\\|"),
                    str(entry.call_count),
                    f"`{entry.status}`",
                    definition.replace("|", "\\|"),
                    ", ".join(enabled).replace("|", "\\|") or "-",
                )
            )
            + " |"
        )
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-root", required=True, type=Path)
    parser.add_argument("--json", required=True, type=Path)
    parser.add_argument("--csv", required=True, type=Path)
    parser.add_argument("--markdown", required=True, type=Path)
    args = parser.parse_args()

    inventory = build_inventory(args.source_root.resolve())
    entries = list(inventory.values())
    _write_json(args.json, entries)
    _write_csv(args.csv, entries)
    _write_markdown(args.markdown, entries)
    print(
        json.dumps(
            {
                "families": len(entries),
                "calls": sum(entry.call_count for entry in entries),
                "json": str(args.json),
                "csv": str(args.csv),
                "markdown": str(args.markdown),
            },
            ensure_ascii=True,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
