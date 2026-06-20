import unittest

from tools.java_source_scan import java_unescape
from tools.string_decoder_registry import decode_registered


SAMPLES = [
    (
        "JSetupDialog$JLoginNew.N",
        "com.sbf.main.StartAppf",
        r"\ueca0\u309c\u751c\u33ee\u2473",
        "token",
    ),
    (
        "JSetupDialog$JLoginNew.N",
        "com.sbf.main.StartAppf",
        r"\uecb1\u308b\u7507\u33e2\u246f\uded4\uc82e\u80e3\u0573\ue160",
        "expireTime",
    ),
    (
        "JLoginHTML$h.v",
        "com.sbf.main.ext.j2026.ClawWorkspace<init>",
        r"\uaae8\u02d5\u4a12\ue864\u241c\uf7de\ufa70\u4318\u8f76\u51a2\u9b6d\u45cd\u9fb1",
        "ClawWorkspace",
    ),
]


class StringDecoderCoreTests(unittest.TestCase):
    def test_verified_samples(self):
        for name, caller, literal, expected in SAMPLES:
            with self.subTest(name=name, expected=expected):
                self.assertEqual(
                    decode_registered(name, caller, java_unescape(literal)),
                    expected,
                )


if __name__ == "__main__":
    unittest.main()
