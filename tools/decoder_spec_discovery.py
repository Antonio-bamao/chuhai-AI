"""Discover verified AES-table decoder specs from CFR Java source files."""

from __future__ import annotations

import re
from pathlib import Path

try:
    from tools.string_decoder_registry import DecoderSpec, make_decoder_spec
except ModuleNotFoundError:  # Direct execution from tools/
    from string_decoder_registry import DecoderSpec, make_decoder_spec


DECODER_METHOD = re.compile(
    r"\bstatic\s+final\s+String\s+(?P<name>[\w$]+)"
    r"\s*\(\s*String\s+\w+\s*\)\s*\{"
)
SEED = re.compile(
    r"private\s+static\s+final\s+long\s+\w+\(long\s+\w+\)\s*\{"
    r".*?long\s+\w+\s*=\s*(?P<value>-?\d+)L;",
    re.DOTALL,
)
SUFFIX_ASSIGNMENT = re.compile(
    r"\w+\[(?P<index>8|9|1[0-5])\]\s*=\s*(?P<value>-?\d+);"
)
INT_ARRAY = re.compile(r"new\s+int\[\]\s*\{(?P<values>[^{}]+)\}")
ENCRYPTED_CALL = re.compile(
    r"(?P<family>[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)+)"
    r'\(\s*"(?P<literal>(?:\\.|[^"\\])*)"\s*\)'
)
UNICODE_ESCAPE = re.compile(r"\\u[0-9a-fA-F]{4}")

STRUCTURAL_MARKERS = (
    "Thread.currentThread().getStackTrace()",
    ".getClassName()",
    ".getMethodName()",
    ".toCharArray()",
    "n13 < 36",
    "switch (n9 % 8)",
    "new byte[256]",
    "new int[30]",
)


def _extract_spec(java_file: Path) -> tuple[str, DecoderSpec] | None:
    text = java_file.read_text(encoding="utf-8", errors="replace")
    if not all(marker in text for marker in STRUCTURAL_MARKERS):
        return None

    methods = DECODER_METHOD.findall(text)
    seeds = SEED.findall(text)
    if len(methods) != 1 or len(seeds) != 1:
        return None

    suffix_values: dict[int, int] = {}
    for match in SUFFIX_ASSIGNMENT.finditer(text):
        suffix_values.setdefault(
            int(match.group("index")),
            int(match.group("value")),
        )
    if set(suffix_values) != set(range(8, 16)):
        return None

    constant_arrays: list[tuple[int, int, int, int]] = []
    for match in INT_ARRAY.finditer(text):
        values = tuple(int(value) for value in re.findall(r"-?\d+", match.group("values")))
        if len(values) == 4:
            constant_arrays.append(values)
    if len(constant_arrays) != 1:
        return None

    family = f"{java_file.stem}.{methods[0]}"
    pattern = re.escape(family) + r'\("((?:\\.|[^"\\])*)"\)'
    return (
        family,
        make_decoder_spec(
            family,
            pattern,
            int(seeds[0]),
            tuple(suffix_values[index] for index in range(8, 16)),
            constant_arrays[0],
        ),
    )


def discover_decoder_specs(source_root: Path) -> dict[str, DecoderSpec]:
    source_root = Path(source_root)
    candidates: dict[str, list[DecoderSpec]] = {}
    called_families: set[str] = set()
    java_files = list(source_root.rglob("*.java"))
    for java_file in java_files:
        text = java_file.read_text(encoding="utf-8", errors="replace")
        for match in ENCRYPTED_CALL.finditer(text):
            if UNICODE_ESCAPE.search(match.group("literal")):
                called_families.add(match.group("family"))

    for java_file in java_files:
        extracted = _extract_spec(java_file)
        if extracted is None:
            continue
        family, spec = extracted
        candidates.setdefault(family, []).append(spec)

    return {
        family: specs[0]
        for family, specs in sorted(candidates.items())
        if len(specs) == 1 and family in called_families
    }
