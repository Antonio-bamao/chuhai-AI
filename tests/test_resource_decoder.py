import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import unittest
import uuid


ROOT = Path(__file__).resolve().parents[1]
JDK_BIN = ROOT / ".artifacts" / "tools" / "jdk8u492-b09" / "jdk8u492-b09" / "bin"
JAVAC = JDK_BIN / "javac.exe"
JAVA = JDK_BIN / "java.exe"
APP_JAR = ROOT / ".artifacts" / "working" / "m1-02" / "App.jar"
SOURCE = ROOT / "tools" / "resource_decoder" / "ResourceDecoder.java"
TMP_ROOT = ROOT / ".artifacts" / "tmp-tests"


class ResourceDecoderTests(unittest.TestCase):
    def setUp(self):
        self.tmp_path = TMP_ROOT / ("resource-decoder-" + uuid.uuid4().hex)
        self.classes = self.tmp_path / "classes"
        self.output = self.tmp_path / "decoded"
        self.classes.mkdir(parents=True)
        subprocess.run(
            [
                str(JAVAC),
                "-cp",
                str(APP_JAR),
                "-d",
                str(self.classes),
                str(SOURCE),
            ],
            check=True,
            cwd=ROOT,
        )

    def tearDown(self):
        shutil.rmtree(self.tmp_path, ignore_errors=True)

    def run_decoder(self, *resources):
        classpath = str(self.classes) + ";" + str(APP_JAR)
        return subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath,
                "ResourceDecoder",
                str(APP_JAR),
                str(self.output),
                *resources,
            ],
            cwd=ROOT,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )

    def test_decodes_default_resources_and_writes_manifest(self):
        result = self.run_decoder()

        self.assertEqual(result.returncode, 0, result.stderr)
        expected = {
            "master.html",
            "msg.html",
            "fm.js",
            "country_ips.json",
            "html/Login.html",
            "html/product-selector.html",
            "html/ClawWorkspace.html",
        }
        manifest = json.loads((self.output / "manifest.json").read_text(encoding="utf-8"))
        self.assertEqual({entry["resource"] for entry in manifest["resources"]}, expected)

        for entry in manifest["resources"]:
            decoded = self.output / entry["resource"]
            data = decoded.read_bytes()
            self.assertGreater(len(data), 0)
            self.assertEqual(entry["bytes"], len(data))
            self.assertEqual(entry["sha256"], hashlib.sha256(data).hexdigest())

        self.assertIn("<html", (self.output / "master.html").read_text(encoding="utf-8").lower())
        self.assertIn("<html", (self.output / "html" / "Login.html").read_text(encoding="utf-8").lower())
        self.assertIsInstance(
            json.loads((self.output / "country_ips.json").read_text(encoding="utf-8")),
            list,
        )

    def test_rejects_resource_path_traversal(self):
        result = self.run_decoder("../master.html")

        self.assertNotEqual(result.returncode, 0)
        self.assertIn("unsafe resource path", result.stderr.lower())
        self.assertFalse((self.tmp_path / "master.html").exists())


if __name__ == "__main__":
    unittest.main()
