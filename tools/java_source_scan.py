"""Shared, read-only helpers for scanning CFR-produced Java sources."""

from __future__ import annotations

import re
from dataclasses import dataclass
from pathlib import Path
from typing import Iterator


PACKAGE_DECL = re.compile(r"^\s*package\s+([\w.]+);")
METHOD_DECL = re.compile(
    r"^\s*(?:public|private|protected|static|final|synchronized|native|abstract|/\*| "
    r"|strictfp|transient|volatile|<)*[\w$<>\[\]., ?]+\s+"
    r"(?P<name>[\w$<>]+)\s*\((?P<params>[^;]*)\)\s*(?:throws [^{]+)?\{"
)
BOOTSTRAP_METHOD_DECL = re.compile(
    r"^\s*(?:public|private|protected|static|final|synchronized|native|abstract|strictfp)\s+"
    r".*?\s+(?P<name>[\w$<>]+)\s*\((?P<params>[^;{}]*)\)\s*(?:throws [^{]+)?\{"
)
CONTROL_PREFIXES = ("if", "for", "while", "switch", "catch")


@dataclass(frozen=True)
class JavaLine:
    path: Path
    line_no: int
    text: str
    class_name: str
    method_name: str
    signature: str
    method_line: int

    @property
    def caller(self) -> str:
        return f"{self.class_name}{self.method_name}"


def java_unescape(text: str) -> str:
    """Decode the escape forms emitted inside CFR Java string literals."""

    def repl(match: re.Match[str]) -> str:
        token = match.group(0)
        if token.startswith("\\u"):
            return chr(int(token[2:], 16))
        table = {
            "\\b": "\b",
            "\\t": "\t",
            "\\n": "\n",
            "\\f": "\f",
            "\\r": "\r",
            '\\"': '"',
            "\\'": "'",
            "\\\\": "\\",
        }
        return table.get(token, token[1:])

    return re.sub(r"\\u[0-9a-fA-F]{4}|\\[btnfr\"'\\]", repl, text)


def class_name_for(source_root: Path, java_file: Path) -> str:
    rel = java_file.relative_to(source_root).with_suffix("")
    return ".".join(rel.parts)


def package_name(lines: list[str]) -> str | None:
    for line in lines[:20]:
        match = PACKAGE_DECL.match(line)
        if match:
            return match.group(1)
    return None


def iter_java_lines(
    source_root: Path,
    java_file: Path,
    method_decl: re.Pattern[str] = METHOD_DECL,
) -> Iterator[JavaLine]:
    """Yield source lines annotated with their enclosing class and method."""

    lines = java_file.read_text(encoding="utf-8", errors="replace").splitlines()
    class_name = class_name_for(source_root, java_file)
    package = package_name(lines)
    if package and not class_name.startswith(package + "."):
        class_name = f"{package}.{java_file.stem}"

    current_method = "<clinit>"
    current_signature = "<clinit>"
    current_method_line = 0
    brace_depth = 0
    method_stack: list[tuple[int, str, str, int]] = []

    for line_no, text in enumerate(lines, start=1):
        while method_stack and brace_depth < method_stack[-1][0]:
            method_stack.pop()
            if method_stack:
                _, current_method, current_signature, current_method_line = method_stack[-1]
            else:
                current_method = "<clinit>"
                current_signature = "<clinit>"
                current_method_line = 0

        method_match = method_decl.match(text)
        if method_match and not text.lstrip().startswith(CONTROL_PREFIXES):
            method_name = method_match.group("name")
            if method_name == java_file.stem:
                method_name = "<init>"
            params = " ".join(method_match.group("params").split())
            current_method = method_name
            current_signature = f"{method_name}({params})"
            current_method_line = line_no
            method_stack.append(
                (brace_depth + 1, current_method, current_signature, current_method_line)
            )

        yield JavaLine(
            path=java_file,
            line_no=line_no,
            text=text,
            class_name=class_name,
            method_name=current_method,
            signature=current_signature,
            method_line=current_method_line,
        )

        brace_depth += text.count("{") - text.count("}")
