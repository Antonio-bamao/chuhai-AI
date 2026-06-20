"""Registry of statically verified Java string decoder families."""

from __future__ import annotations

import re
from dataclasses import dataclass
from functools import lru_cache

try:
    from tools.string_decoder_core import build_decoder, decode_string
except ModuleNotFoundError:  # Direct imports from scripts in tools/
    from string_decoder_core import build_decoder, decode_string


@dataclass(frozen=True)
class DecoderSpec:
    name: str
    call_pattern: re.Pattern[str]
    seed_long: int
    suffix_bytes: tuple[int, ...]
    constants: tuple[int, int, int, int]


def _spec(
    name: str,
    pattern: str,
    seed_long: int,
    suffix_bytes: tuple[int, ...],
    constants: tuple[int, int, int, int],
) -> DecoderSpec:
    return DecoderSpec(
        name=name,
        call_pattern=re.compile(pattern),
        seed_long=seed_long,
        suffix_bytes=suffix_bytes,
        constants=constants,
    )


DECODER_SPECS: dict[str, DecoderSpec] = {
    spec.name: spec
    for spec in (
        _spec(
            "JSetupDialog$JLoginNew.N",
            r'JSetupDialog\$JLoginNew\.N\("((?:\\.|[^"\\])*)"\)',
            -757823553775778407,
            (68, -97, 112, -7, -1, -62, -6, 5),
            (217755809, -516550560, 280491737, -783029300),
        ),
        _spec(
            "JTestFrame$JLoginNew$2.k",
            r'JTestFrame\$JLoginNew\$2\.k\("((?:\\.|[^"\\])*)"\)',
            -2926258841240362302,
            (-14, -94, 64, -22, -25, -119, -36, -20),
            (1533495656, 711390347, -1942746190, 1457004211),
        ),
        _spec(
            "JLoginHTML$h.v",
            r'JLoginHTML\$h\.v\("((?:\\.|[^"\\])*)"\)',
            -8144699472934638634,
            (72, -86, 84, 126, -19, 34, 36, -62),
            (654727986, -954217567, -1223823750, -1573023736),
        ),
        _spec(
            "c$c$a.f",
            r'c\$c\$a\.f\("((?:\\.|[^"\\])*)"\)',
            -5558785855947409150,
            (26, -65, 17, -15, 61, -83, 24, -76),
            (1661124947, 84476174, 873636781, -113518219),
        ),
        _spec(
            "Keepapi$AiBotHelper$1.C",
            r'Keepapi\$AiBotHelper\$1\.C\("((?:\\.|[^"\\])*)"\)',
            3937958849870418103,
            (33, -34, 57, -6, -63, -92, 92, -54),
            (-1492814596, -722234167, -1721848852, 484333427),
        ),
        _spec(
            "MiJava$MiJava$181.W",
            r'MiJava\$MiJava\$181\.W\("((?:\\.|[^"\\])*)"\)',
            6681889125754537689,
            (106, -43, -43, -74, 74, 84, 24, 55),
            (877174110, -1316375260, -2059877297, -2090820588),
        ),
        _spec(
            "d$JTrayDialog.n",
            r'd\$JTrayDialog\.n\("((?:\\.|[^"\\])*)"\)',
            787646961109417875,
            (-15, -63, -4, -14, 107, -116, -116, -95),
            (-924053382, 2117251666, -2025566316, -786342393),
        ),
        _spec(
            "d$MiJava$188$1.B",
            r'd\$MiJava\$188\$1\.B\("((?:\\.|[^"\\])*)"\)',
            -5295390802134825839,
            (119, 30, 91, 109, -57, 97, 5, 115),
            (740895145, -1610822446, -544228048, 773248396),
        ),
        _spec(
            "AdsCallback$SGAICloudPanel$2.I",
            r'AdsCallback\$SGAICloudPanel\$2\.I\("((?:\\.|[^"\\])*)"\)',
            7474384408623362700,
            (-26, 104, -88, -33, -61, 113, -100, 85),
            (-1473982832, -746230665, 1493149019, -693720993),
        ),
    )
}


@lru_cache(maxsize=None)
def _decoder_for(name: str) -> tuple:
    spec = DECODER_SPECS[name]
    return build_decoder(
        spec.seed_long,
        list(spec.suffix_bytes),
        list(spec.constants),
    )


def decode_registered(name: str, caller: str, encrypted: str) -> str:
    return decode_string(encrypted, caller, _decoder_for(name))
