"""Resolve caller-hash decoder inputs using original classfile method names."""

from __future__ import annotations

import re
import unicodedata
from dataclasses import dataclass
from pathlib import Path

try:
    from tools.classfile_methods import read_class_method_names
    from tools.string_decoder_registry import decode_registered
except ModuleNotFoundError:  # Direct execution from tools/
    from classfile_methods import read_class_method_names
    from string_decoder_registry import decode_registered


COMMON_TEXT = re.compile(r"^[\w\s.,:;/\\@#%+*=!?&()\[\]{}'\"<>|~`-]*$", re.UNICODE)


@dataclass(frozen=True)
class CallerResolution:
    resolved: bool
    caller: str
    decoded: str
    score: float
    margin: float
    candidates_tested: int


def is_valid_text(text: str) -> bool:
    return not any(
        0xD800 <= ord(char) <= 0xDFFF
        or (ord(char) < 32 and char not in "\r\n\t")
        for char in text
    )


def plaintext_score(text: str) -> float:
    if text == "":
        return 3.0
    points = 0.0
    ascii_printable = 0
    for char in text:
        codepoint = ord(char)
        if 0xD800 <= codepoint <= 0xDFFF:
            points -= 12.0
        elif char in "\r\n\t":
            points += 2.0
        elif 32 <= codepoint < 127:
            points += 4.0
            ascii_printable += 1
        elif 0x3400 <= codepoint <= 0x9FFF:
            points += 3.5
        else:
            category = unicodedata.category(char)
            if category.startswith(("L", "N")):
                points += 1.0
            elif category.startswith(("P", "S", "Z")):
                points += 1.5
            else:
                points -= 6.0
    score = points / len(text)
    if COMMON_TEXT.fullmatch(text):
        score += 0.75
    if ascii_printable / len(text) >= 0.8:
        score += 0.5
    return score


def resolve_registered_caller(
    jar_path: Path,
    class_name: str,
    lexical_method: str,
    family: str,
    encrypted: str,
) -> CallerResolution:
    lexical_caller = f"{class_name}{lexical_method}"
    lexical_decoded = decode_registered(family, lexical_caller, encrypted)
    lexical_score = plaintext_score(lexical_decoded)
    if is_valid_text(lexical_decoded) and lexical_score >= 4.0:
        return CallerResolution(
            True,
            lexical_caller,
            lexical_decoded,
            lexical_score,
            999.0,
            1,
        )

    method_names = list(read_class_method_names(jar_path.resolve(), class_name))
    if lexical_method not in method_names:
        method_names.append(lexical_method)

    results: list[tuple[float, str, str]] = []
    for method_name in method_names:
        caller = f"{class_name}{method_name}"
        decoded = decode_registered(family, caller, encrypted)
        results.append((plaintext_score(decoded), caller, decoded))
    results.sort(key=lambda item: (-item[0], item[1]))

    valid_results = [result for result in results if is_valid_text(result[2])]
    ranked = valid_results or results
    best_score, best_caller, best_decoded = ranked[0]
    second_score = ranked[1][0] if len(ranked) > 1 else float("-inf")
    margin = best_score - second_score
    resolved = (
        is_valid_text(best_decoded)
        and best_score >= 3.0
        and margin >= 0.75
    )
    return CallerResolution(
        resolved,
        best_caller,
        best_decoded if resolved else "",
        best_score,
        margin,
        len(results),
    )
