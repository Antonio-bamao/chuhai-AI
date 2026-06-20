"""Minimal classfile reader for method-name evidence from the original JAR."""

from __future__ import annotations

import struct
import zipfile
from functools import lru_cache
from pathlib import Path


def _method_names(class_bytes: bytes) -> tuple[str, ...]:
    if class_bytes[:4] != b"\xca\xfe\xba\xbe":
        raise ValueError("not a Java classfile")
    offset = 8

    def u1() -> int:
        nonlocal offset
        value = class_bytes[offset]
        offset += 1
        return value

    def u2() -> int:
        nonlocal offset
        value = struct.unpack_from(">H", class_bytes, offset)[0]
        offset += 2
        return value

    def u4() -> int:
        nonlocal offset
        value = struct.unpack_from(">I", class_bytes, offset)[0]
        offset += 4
        return value

    def skip_attributes(count: int) -> None:
        nonlocal offset
        for _ in range(count):
            u2()
            size = u4()
            offset += size

    constant_pool: list[str | None] = [None] * u2()
    index = 1
    while index < len(constant_pool):
        tag = u1()
        if tag == 1:
            size = u2()
            constant_pool[index] = class_bytes[offset : offset + size].decode(
                "utf-8",
                errors="replace",
            )
            offset += size
        elif tag in {3, 4}:
            offset += 4
        elif tag in {5, 6}:
            offset += 8
            index += 1
        elif tag in {7, 8, 16, 19, 20}:
            offset += 2
        elif tag in {9, 10, 11, 12, 17, 18}:
            offset += 4
        elif tag == 15:
            offset += 3
        else:
            raise ValueError(f"unsupported constant-pool tag {tag}")
        index += 1

    offset += 6
    interface_count = u2()
    offset += interface_count * 2
    for _ in range(u2()):
        offset += 6
        skip_attributes(u2())

    names: list[str] = []
    for _ in range(u2()):
        u2()
        name_index = u2()
        u2()
        attribute_count = u2()
        name = constant_pool[name_index]
        if name is not None and name not in names:
            names.append(name)
        skip_attributes(attribute_count)
    return tuple(names)


@lru_cache(maxsize=None)
def read_class_method_names(jar_path: Path, class_name: str) -> tuple[str, ...]:
    entry = class_name.replace(".", "/") + ".class"
    with zipfile.ZipFile(jar_path) as archive:
        try:
            class_bytes = archive.read(entry)
        except KeyError:
            return ()
    return _method_names(class_bytes)
