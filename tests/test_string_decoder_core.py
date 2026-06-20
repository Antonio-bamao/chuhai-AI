import unittest
from pathlib import Path

from tools.decoder_spec_discovery import discover_decoder_specs
from tools.java_source_scan import java_unescape
from tools.string_decoder_registry import decode_registered, register_decoder_specs


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
    (
        "MiJava$191$1$0.y",
        "com.sbf.main.jxbrowser.a$1run",
        r"\ue1c4\u240a\u8a2a\u8869",
        "data",
    ),
    (
        "InsApiHelper$InsUserAgent.b",
        "com.sbf.main.rpa.func.ins.a<init>",
        r"\ufed7\ufca3\u2cd6\ua533",
        "name",
    ),
    (
        "a$MiJava$60.B",
        "com.sbf.main.jxbrowser.a$1a",
        r"\u8e8c\uea6e\u48cc\ua771\ub30b\u0c8d\u4fe5\u3481\u5b2e\u2225\u4376\u9243",
        "access_token",
    ),
    (
        "f$JVKSpiderHelper2$1.T",
        "com.sbf.main.rpa.func.a$1run",
        r"\u9b33\ud8be\uf2ee\u3e2a\uda4f\u3c6e\u6454\u820b\u07c7\u53e9\u4bf4",
        "Menu0002001",
    ),
    (
        "a$a$11.Y",
        "com.sbf.main.ext.open.zw.ads.a$2run",
        r"\u2fb1\ub4d9\u4a9b\u14db",
        "code",
    ),
    (
        "SafeProcessManager$SafeProcessManager.N",
        "com.sbf.util.sm.SafeProcessManager<clinit>",
        r"\ub0f4\ua35a\u5f96\ue368\u3633\u560e\ufc99\ua0cb\ue6f2\u9d56\u4b5e\u60d6",
        "HTTPDebugger",
    ),
    (
        "d$StringHelper.o",
        "com.sbf.util.BanWordsUtila",
        r"\u074b\ud868\ufd7c\uf00c",
        "http",
    ),
    (
        "ADBrowser$4$0.I",
        "com.sbf.main.ext.ads.ADBrowser$2run",
        r"\u578f\u25c3\u2b69\u78f3\ue63a\uec5f\u0add",
        "browser",
    ),
    (
        "SecureRSAUtil$SBFApi$1.m",
        "com.sbf.util.http.AESCBCHelpera",
        r"\u512f\u3ff1\u273c\ua9fc\u2f97",
        "utf-8",
    ),
    (
        "f$j$1.w",
        "com.sbf.main.rpa.func.aa",
        r"\u1cb6\ubc22\u5648\u5964\u76da\uda74\uec24\uff6e\ucb6a\u4a64",
        "start.....",
    ),
]


class StringDecoderCoreTests(unittest.TestCase):
    def test_verified_samples(self):
        source_root = Path(".artifacts/decompiled/cfr-app-20260620-0215")
        register_decoder_specs(discover_decoder_specs(source_root))
        for name, caller, literal, expected in SAMPLES:
            with self.subTest(name=name, expected=expected):
                self.assertEqual(
                    decode_registered(name, caller, java_unescape(literal)),
                    expected,
                )


if __name__ == "__main__":
    unittest.main()
