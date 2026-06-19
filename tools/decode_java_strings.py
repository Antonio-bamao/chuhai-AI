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

MASK32 = 0xFFFFFFFF


def i32(value: int) -> int:
    value &= MASK32
    return value - 0x100000000 if value & 0x80000000 else value


def urshift(value: int, count: int) -> int:
    return (value & MASK32) >> (count & 31)


def shl(value: int, count: int) -> int:
    return i32((value & MASK32) << (count & 31))


def ror(value: int, count: int) -> int:
    return i32(urshift(value, count) | shl(value, -count))


def java_hash(text: str) -> int:
    h = 0
    for ch in text:
        h = i32(31 * h + ord(ch))
    return h


def signed_byte(value: int) -> int:
    value &= 0xFF
    return value - 256 if value >= 128 else value


def long_to_signed_bytes(value: int) -> list[int]:
    if value < 0:
        value = (value + (1 << 64)) & ((1 << 64) - 1)
    return [signed_byte(value >> shift) for shift in (56, 48, 40, 32, 24, 16, 8, 0)]


def table_lookup(sbox: list[int], value: int) -> int:
    return i32(
        (sbox[value & 0xFF] & 0xFF)
        | ((sbox[(value >> 8) & 0xFF] & 0xFF) << 8)
        | ((sbox[(value >> 16) & 0xFF] & 0xFF) << 16)
        | (sbox[urshift(value, 24)] << 24)
    )


def build_decoder(seed_long: int, suffix_bytes: list[int], constants: list[int]) -> tuple:
    alog = [0] * 256
    sbox = [0] * 256
    t1 = [0] * 256
    t2 = [0] * 256
    t3 = [0] * 256
    t4 = [0] * 256
    rcon = [0] * 30

    x = 1
    for i in range(256):
        alog[i] = x
        x = i32(x ^ i32((x << 1) ^ (urshift(x, 7) * 283)))

    sbox[0] = 99
    for i in range(255):
        v = alog[255 - i]
        y = i32(v | (v << 8))
        sbox[alog[i]] = signed_byte(y ^ (y >> 4 ^ y >> 5 ^ y >> 6 ^ y >> 7) ^ 99)

    for i in range(256):
        v = sbox[i] & 0xFF
        x = i32((v << 1) ^ (urshift(v, 7) * 283))
        y = i32(((v ^ x) << 24) ^ (v << 16) ^ (v << 8) ^ x)
        t1[i] = y
        t2[i] = i32((y << 8) | urshift(y, -8))
        t3[i] = i32((y << 16) | urshift(y, -16))
        t4[i] = i32((y << 24) | urshift(y, -24))

    x = 1
    for i in range(30):
        rcon[i] = x
        x = i32((x << 1) ^ (urshift(x, 7) * 283))

    key = long_to_signed_bytes(seed_long) + [signed_byte(x) for x in suffix_bytes]
    nk = 4
    nr = nk + 6
    schedule = [0] * ((nr + 1) * 4)

    pos = 0
    idx = 0
    while pos < 16:
        schedule[((idx >> 2) * 4) + (idx & 3)] = i32(
            (key[pos] & 0xFF)
            | ((key[pos + 1] & 0xFF) << 8)
            | ((key[pos + 2] & 0xFF) << 16)
            | (key[pos + 3] << 24)
        )
        pos += 4
        idx += 1

    for i in range(nk, (nr + 1) << 2):
        temp = schedule[((i - 1) >> 2) * 4 + ((i - 1) & 3)]
        if i % nk == 0:
            temp = i32(table_lookup(sbox, ror(temp, 8)) ^ rcon[i // nk - 1])
        elif nk > 6 and i % nk == 4:
            temp = table_lookup(sbox, temp)
        schedule[(i >> 2) * 4 + (i & 3)] = i32(
            schedule[((i - nk) >> 2) * 4 + ((i - nk) & 3)] ^ temp
        )

    return sbox, t1, t2, t3, t4, schedule, constants


DECODERS = {
    "JSetupDialog$JLoginNew.N": build_decoder(
        -757823553775778407,
        [68, -97, 112, -7, -1, -62, -6, 5],
        [217755809, -516550560, 280491737, -783029300],
    ),
    "JTestFrame$JLoginNew$2.k": build_decoder(
        -2926258841240362302,
        [-14, -94, 64, -22, -25, -119, -36, -20],
        [1533495656, 711390347, -1942746190, 1457004211],
    ),
    "JLoginHTML$h.v": build_decoder(
        -8144699472934638634,
        [72, -86, 84, 126, -19, 34, 36, -62],
        [654727986, -954217567, -1223823750, -1573023736],
    ),
    "c$c$a.f": build_decoder(
        -5558785855947409150,
        [26, -65, 17, -15, 61, -83, 24, -76],
        [1661124947, 84476174, 873636781, -113518219],
    ),
    "Keepapi$AiBotHelper$1.C": build_decoder(
        3937958849870418103,
        [33, -34, 57, -6, -63, -92, 92, -54],
        [-1492814596, -722234167, -1721848852, 484333427],
    ),
    "MiJava$MiJava$181.W": build_decoder(
        6681889125754537689,
        [106, -43, -43, -74, 74, 84, 24, 55],
        [877174110, -1316375260, -2059877297, -2090820588],
    ),
    "d$JTrayDialog.n": build_decoder(
        787646961109417875,
        [-15, -63, -4, -14, 107, -116, -116, -95],
        [-924053382, 2117251666, -2025566316, -786342393],
    ),
    "d$MiJava$188$1.B": build_decoder(
        -5295390802134825839,
        [119, 30, 91, 109, -57, 97, 5, 115],
        [740895145, -1610822446, -544228048, 773248396],
    ),
}

CALL_PATTERNS = {
    "JSetupDialog$JLoginNew.N": re.compile(r'JSetupDialog\$JLoginNew\.N\("((?:\\.|[^"\\])*)"\)'),
    "JTestFrame$JLoginNew$2.k": re.compile(r'JTestFrame\$JLoginNew\$2\.k\("((?:\\.|[^"\\])*)"\)'),
    "JLoginHTML$h.v": re.compile(r'JLoginHTML\$h\.v\("((?:\\.|[^"\\])*)"\)'),
    "c$c$a.f": re.compile(r'c\$c\$a\.f\("((?:\\.|[^"\\])*)"\)'),
    "Keepapi$AiBotHelper$1.C": re.compile(r'Keepapi\$AiBotHelper\$1\.C\("((?:\\.|[^"\\])*)"\)'),
    "MiJava$MiJava$181.W": re.compile(r'MiJava\$MiJava\$181\.W\("((?:\\.|[^"\\])*)"\)'),
    "d$JTrayDialog.n": re.compile(r'd\$JTrayDialog\.n\("((?:\\.|[^"\\])*)"\)'),
    "d$MiJava$188$1.B": re.compile(r'd\$MiJava\$188\$1\.B\("((?:\\.|[^"\\])*)"\)'),
}

METHOD_DECL = re.compile(
    r"^\s*(?:public|private|protected|static|final|synchronized|native|abstract|/\*| "
    r"|strictfp|transient|volatile|<)*[\w$<>\[\]., ?]+\s+([\w$<>]+)\s*\([^;]*\)\s*(?:throws [^{]+)?\{"
)
PACKAGE_DECL = re.compile(r"^\s*package\s+([\w.]+);")


def java_unescape(text: str) -> str:
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


def decode_string(encrypted: str, caller: str, decoder: tuple) -> str:
    sbox, t1, t2, t3, t4, schedule, constants = decoder
    h = java_hash(caller)
    n4 = i32(h ^ constants[0])
    n5 = i32(h ^ constants[1])
    n6 = i32(h ^ constants[2])
    n7 = i32(h ^ constants[3])
    chars = [ord(ch) for ch in encrypted]

    for idx in range(len(chars)):
        if idx % 8 == 0:
            n14 = i32(n4 ^ schedule[0])
            n15 = i32(n5 ^ schedule[1])
            n16 = i32(n6 ^ schedule[2])
            n17 = i32(n7 ^ schedule[3])
            n13 = 4
            while n13 < 36:
                n12 = i32(t1[n14 & 0xFF] ^ t2[(n15 >> 8) & 0xFF] ^ t3[(n16 >> 16) & 0xFF] ^ t4[urshift(n17, 24)] ^ schedule[n13])
                n11 = i32(t1[n15 & 0xFF] ^ t2[(n16 >> 8) & 0xFF] ^ t3[(n17 >> 16) & 0xFF] ^ t4[urshift(n14, 24)] ^ schedule[n13 + 1])
                n10 = i32(t1[n16 & 0xFF] ^ t2[(n17 >> 8) & 0xFF] ^ t3[(n14 >> 16) & 0xFF] ^ t4[urshift(n15, 24)] ^ schedule[n13 + 2])
                n17 = i32(t1[n17 & 0xFF] ^ t2[(n14 >> 8) & 0xFF] ^ t3[(n15 >> 16) & 0xFF] ^ t4[urshift(n16, 24)] ^ schedule[n13 + 3])
                n13 += 4
                n14 = i32(t1[n12 & 0xFF] ^ t2[(n11 >> 8) & 0xFF] ^ t3[(n10 >> 16) & 0xFF] ^ t4[urshift(n17, 24)] ^ schedule[n13])
                n15 = i32(t1[n11 & 0xFF] ^ t2[(n10 >> 8) & 0xFF] ^ t3[(n17 >> 16) & 0xFF] ^ t4[urshift(n12, 24)] ^ schedule[n13 + 1])
                n16 = i32(t1[n10 & 0xFF] ^ t2[(n17 >> 8) & 0xFF] ^ t3[(n12 >> 16) & 0xFF] ^ t4[urshift(n11, 24)] ^ schedule[n13 + 2])
                n17 = i32(t1[n17 & 0xFF] ^ t2[(n12 >> 8) & 0xFF] ^ t3[(n11 >> 16) & 0xFF] ^ t4[urshift(n10, 24)] ^ schedule[n13 + 3])
                n13 += 4
            n10 = i32(t1[n14 & 0xFF] ^ t2[(n15 >> 8) & 0xFF] ^ t3[(n16 >> 16) & 0xFF] ^ t4[urshift(n17, 24)] ^ schedule[n13])
            n11 = i32(t1[n15 & 0xFF] ^ t2[(n16 >> 8) & 0xFF] ^ t3[(n17 >> 16) & 0xFF] ^ t4[urshift(n14, 24)] ^ schedule[n13 + 1])
            n12 = i32(t1[n16 & 0xFF] ^ t2[(n17 >> 8) & 0xFF] ^ t3[(n14 >> 16) & 0xFF] ^ t4[urshift(n15, 24)] ^ schedule[n13 + 2])
            n17 = i32(t1[n17 & 0xFF] ^ t2[(n14 >> 8) & 0xFF] ^ t3[(n15 >> 16) & 0xFF] ^ t4[urshift(n16, 24)] ^ schedule[n13 + 3])
            base = n13 + 4
            n4 = i32((sbox[n10 & 0xFF] & 0xFF) ^ ((sbox[(n11 >> 8) & 0xFF] & 0xFF) << 8) ^ ((sbox[(n12 >> 16) & 0xFF] & 0xFF) << 16) ^ (sbox[urshift(n17, 24)] << 24) ^ schedule[base])
            n5 = i32((sbox[n11 & 0xFF] & 0xFF) ^ ((sbox[(n12 >> 8) & 0xFF] & 0xFF) << 8) ^ ((sbox[(n17 >> 16) & 0xFF] & 0xFF) << 16) ^ (sbox[urshift(n10, 24)] << 24) ^ schedule[base + 1])
            n6 = i32((sbox[n12 & 0xFF] & 0xFF) ^ ((sbox[(n17 >> 8) & 0xFF] & 0xFF) << 8) ^ ((sbox[(n10 >> 16) & 0xFF] & 0xFF) << 16) ^ (sbox[urshift(n11, 24)] << 24) ^ schedule[base + 2])
            n7 = i32((sbox[n17 & 0xFF] & 0xFF) ^ ((sbox[(n10 >> 8) & 0xFF] & 0xFF) << 8) ^ ((sbox[(n11 >> 16) & 0xFF] & 0xFF) << 16) ^ (sbox[urshift(n12, 24)] << 24) ^ schedule[base + 3])

        slot = idx % 8
        if slot == 0:
            chars[idx] = (n4 >> 16) ^ chars[idx]
        elif slot == 1:
            chars[idx] = n4 ^ chars[idx]
        elif slot == 2:
            chars[idx] = (n5 >> 16) ^ chars[idx]
        elif slot == 3:
            chars[idx] = n5 ^ chars[idx]
        elif slot == 4:
            chars[idx] = (n6 >> 16) ^ chars[idx]
        elif slot == 5:
            chars[idx] = n6 ^ chars[idx]
        elif slot == 6:
            chars[idx] = (n7 >> 16) ^ chars[idx]
        else:
            chars[idx] = n7 ^ chars[idx]
        chars[idx] &= 0xFFFF

    return "".join(chr(ch) for ch in chars)


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
    brace_depth = 0
    method_stack: list[tuple[int, str]] = []
    rows: list[dict] = []

    for line_no, line in enumerate(lines, start=1):
        while method_stack and brace_depth < method_stack[-1][0]:
            method_stack.pop()
            current_method = method_stack[-1][1] if method_stack else "<clinit>"

        method_match = METHOD_DECL.match(line)
        if method_match and not line.lstrip().startswith(("if", "for", "while", "switch", "catch")):
            name = method_match.group(1)
            if name == java_file.stem:
                name = "<init>"
            current_method = name
            method_stack.append((brace_depth + 1, current_method))

        for decoder_name, pattern in CALL_PATTERNS.items():
            for match in pattern.finditer(line):
                encrypted_literal = match.group(1)
                encrypted = java_unescape(encrypted_literal)
                caller = f"{cls}{current_method}"
                try:
                    decoded = decode_string(encrypted, caller, DECODERS[decoder_name])
                    error = None
                except Exception as exc:  # pragma: no cover - defensive analysis output
                    decoded = ""
                    error = repr(exc)
                rows.append(
                    {
                        "decoder": decoder_name,
                        "path": str(java_file),
                        "line": line_no,
                        "caller": caller,
                        "encrypted_literal": encrypted_literal,
                        "decoded": decoded,
                        "error": error,
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

    decoded = sum(1 for row in rows if row["decoded"] and row["error"] is None)
    print(json.dumps({"rows": len(rows), "decoded": decoded, "out": str(args.out)}, ensure_ascii=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
