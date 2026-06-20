"""Core primitives for the verified AES-table string decoder family."""

from __future__ import annotations


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
    value = 0
    for char in text:
        value = i32(31 * value + ord(char))
    return value


def signed_byte(value: int) -> int:
    value &= 0xFF
    return value - 256 if value >= 128 else value


def long_to_signed_bytes(value: int) -> list[int]:
    if value < 0:
        value = (value + (1 << 64)) & ((1 << 64) - 1)
    return [
        signed_byte(value >> shift)
        for shift in (56, 48, 40, 32, 24, 16, 8, 0)
    ]


def table_lookup(sbox: list[int], value: int) -> int:
    return i32(
        (sbox[value & 0xFF] & 0xFF)
        | ((sbox[(value >> 8) & 0xFF] & 0xFF) << 8)
        | ((sbox[(value >> 16) & 0xFF] & 0xFF) << 16)
        | (sbox[urshift(value, 24)] << 24)
    )


def build_decoder(
    seed_long: int,
    suffix_bytes: list[int],
    constants: list[int],
) -> tuple:
    alog = [0] * 256
    sbox = [0] * 256
    t1 = [0] * 256
    t2 = [0] * 256
    t3 = [0] * 256
    t4 = [0] * 256
    rcon = [0] * 30

    value = 1
    for index in range(256):
        alog[index] = value
        value = i32(value ^ i32((value << 1) ^ (urshift(value, 7) * 283)))

    sbox[0] = 99
    for index in range(255):
        value = alog[255 - index]
        mixed = i32(value | (value << 8))
        sbox[alog[index]] = signed_byte(
            mixed ^ (mixed >> 4 ^ mixed >> 5 ^ mixed >> 6 ^ mixed >> 7) ^ 99
        )

    for index in range(256):
        value = sbox[index] & 0xFF
        doubled = i32((value << 1) ^ (urshift(value, 7) * 283))
        mixed = i32(
            ((value ^ doubled) << 24)
            ^ (value << 16)
            ^ (value << 8)
            ^ doubled
        )
        t1[index] = mixed
        t2[index] = i32((mixed << 8) | urshift(mixed, -8))
        t3[index] = i32((mixed << 16) | urshift(mixed, -16))
        t4[index] = i32((mixed << 24) | urshift(mixed, -24))

    value = 1
    for index in range(30):
        rcon[index] = value
        value = i32((value << 1) ^ (urshift(value, 7) * 283))

    key = long_to_signed_bytes(seed_long) + [
        signed_byte(value) for value in suffix_bytes
    ]
    nk = 4
    nr = nk + 6
    schedule = [0] * ((nr + 1) * 4)

    position = 0
    index = 0
    while position < 16:
        schedule[((index >> 2) * 4) + (index & 3)] = i32(
            (key[position] & 0xFF)
            | ((key[position + 1] & 0xFF) << 8)
            | ((key[position + 2] & 0xFF) << 16)
            | (key[position + 3] << 24)
        )
        position += 4
        index += 1

    for index in range(nk, (nr + 1) << 2):
        temp = schedule[((index - 1) >> 2) * 4 + ((index - 1) & 3)]
        if index % nk == 0:
            temp = i32(
                table_lookup(sbox, ror(temp, 8)) ^ rcon[index // nk - 1]
            )
        elif nk > 6 and index % nk == 4:
            temp = table_lookup(sbox, temp)
        schedule[(index >> 2) * 4 + (index & 3)] = i32(
            schedule[((index - nk) >> 2) * 4 + ((index - nk) & 3)] ^ temp
        )

    return sbox, t1, t2, t3, t4, schedule, constants


def decode_string(encrypted: str, caller: str, decoder: tuple) -> str:
    sbox, t1, t2, t3, t4, schedule, constants = decoder
    caller_hash = java_hash(caller)
    n4 = i32(caller_hash ^ constants[0])
    n5 = i32(caller_hash ^ constants[1])
    n6 = i32(caller_hash ^ constants[2])
    n7 = i32(caller_hash ^ constants[3])
    chars = [ord(char) for char in encrypted]

    for index in range(len(chars)):
        if index % 8 == 0:
            n14 = i32(n4 ^ schedule[0])
            n15 = i32(n5 ^ schedule[1])
            n16 = i32(n6 ^ schedule[2])
            n17 = i32(n7 ^ schedule[3])
            n13 = 4
            while n13 < 36:
                n12 = i32(
                    t1[n14 & 0xFF]
                    ^ t2[(n15 >> 8) & 0xFF]
                    ^ t3[(n16 >> 16) & 0xFF]
                    ^ t4[urshift(n17, 24)]
                    ^ schedule[n13]
                )
                n11 = i32(
                    t1[n15 & 0xFF]
                    ^ t2[(n16 >> 8) & 0xFF]
                    ^ t3[(n17 >> 16) & 0xFF]
                    ^ t4[urshift(n14, 24)]
                    ^ schedule[n13 + 1]
                )
                n10 = i32(
                    t1[n16 & 0xFF]
                    ^ t2[(n17 >> 8) & 0xFF]
                    ^ t3[(n14 >> 16) & 0xFF]
                    ^ t4[urshift(n15, 24)]
                    ^ schedule[n13 + 2]
                )
                n17 = i32(
                    t1[n17 & 0xFF]
                    ^ t2[(n14 >> 8) & 0xFF]
                    ^ t3[(n15 >> 16) & 0xFF]
                    ^ t4[urshift(n16, 24)]
                    ^ schedule[n13 + 3]
                )
                n13 += 4
                n14 = i32(
                    t1[n12 & 0xFF]
                    ^ t2[(n11 >> 8) & 0xFF]
                    ^ t3[(n10 >> 16) & 0xFF]
                    ^ t4[urshift(n17, 24)]
                    ^ schedule[n13]
                )
                n15 = i32(
                    t1[n11 & 0xFF]
                    ^ t2[(n10 >> 8) & 0xFF]
                    ^ t3[(n17 >> 16) & 0xFF]
                    ^ t4[urshift(n12, 24)]
                    ^ schedule[n13 + 1]
                )
                n16 = i32(
                    t1[n10 & 0xFF]
                    ^ t2[(n17 >> 8) & 0xFF]
                    ^ t3[(n12 >> 16) & 0xFF]
                    ^ t4[urshift(n11, 24)]
                    ^ schedule[n13 + 2]
                )
                n17 = i32(
                    t1[n17 & 0xFF]
                    ^ t2[(n12 >> 8) & 0xFF]
                    ^ t3[(n11 >> 16) & 0xFF]
                    ^ t4[urshift(n10, 24)]
                    ^ schedule[n13 + 3]
                )
                n13 += 4
            n10 = i32(
                t1[n14 & 0xFF]
                ^ t2[(n15 >> 8) & 0xFF]
                ^ t3[(n16 >> 16) & 0xFF]
                ^ t4[urshift(n17, 24)]
                ^ schedule[n13]
            )
            n11 = i32(
                t1[n15 & 0xFF]
                ^ t2[(n16 >> 8) & 0xFF]
                ^ t3[(n17 >> 16) & 0xFF]
                ^ t4[urshift(n14, 24)]
                ^ schedule[n13 + 1]
            )
            n12 = i32(
                t1[n16 & 0xFF]
                ^ t2[(n17 >> 8) & 0xFF]
                ^ t3[(n14 >> 16) & 0xFF]
                ^ t4[urshift(n15, 24)]
                ^ schedule[n13 + 2]
            )
            n17 = i32(
                t1[n17 & 0xFF]
                ^ t2[(n14 >> 8) & 0xFF]
                ^ t3[(n15 >> 16) & 0xFF]
                ^ t4[urshift(n16, 24)]
                ^ schedule[n13 + 3]
            )
            base = n13 + 4
            n4 = i32(
                (sbox[n10 & 0xFF] & 0xFF)
                ^ ((sbox[(n11 >> 8) & 0xFF] & 0xFF) << 8)
                ^ ((sbox[(n12 >> 16) & 0xFF] & 0xFF) << 16)
                ^ (sbox[urshift(n17, 24)] << 24)
                ^ schedule[base]
            )
            n5 = i32(
                (sbox[n11 & 0xFF] & 0xFF)
                ^ ((sbox[(n12 >> 8) & 0xFF] & 0xFF) << 8)
                ^ ((sbox[(n17 >> 16) & 0xFF] & 0xFF) << 16)
                ^ (sbox[urshift(n10, 24)] << 24)
                ^ schedule[base + 1]
            )
            n6 = i32(
                (sbox[n12 & 0xFF] & 0xFF)
                ^ ((sbox[(n17 >> 8) & 0xFF] & 0xFF) << 8)
                ^ ((sbox[(n10 >> 16) & 0xFF] & 0xFF) << 16)
                ^ (sbox[urshift(n11, 24)] << 24)
                ^ schedule[base + 2]
            )
            n7 = i32(
                (sbox[n17 & 0xFF] & 0xFF)
                ^ ((sbox[(n10 >> 8) & 0xFF] & 0xFF) << 8)
                ^ ((sbox[(n11 >> 16) & 0xFF] & 0xFF) << 16)
                ^ (sbox[urshift(n12, 24)] << 24)
                ^ schedule[base + 3]
            )

        slot = index % 8
        if slot == 0:
            chars[index] = (n4 >> 16) ^ chars[index]
        elif slot == 1:
            chars[index] = n4 ^ chars[index]
        elif slot == 2:
            chars[index] = (n5 >> 16) ^ chars[index]
        elif slot == 3:
            chars[index] = n5 ^ chars[index]
        elif slot == 4:
            chars[index] = (n6 >> 16) ^ chars[index]
        elif slot == 5:
            chars[index] = n6 ^ chars[index]
        elif slot == 6:
            chars[index] = (n7 >> 16) ^ chars[index]
        else:
            chars[index] = n7 ^ chars[index]
        chars[index] &= 0xFFFF

    return "".join(chr(char) for char in chars)
