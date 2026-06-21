import subprocess
import textwrap
from pathlib import Path
import shutil
import uuid
import unittest


ROOT = Path(__file__).resolve().parents[1]
JAVAC = ROOT / ".artifacts" / "tools" / "jdk8u492-b09" / "jdk8u492-b09" / "bin" / "javac.exe"
JAVA = ROOT / ".artifacts" / "tools" / "jdk8u492-b09" / "jdk8u492-b09" / "bin" / "java.exe"
HOOKS = ROOT / "tools" / "dynamic_dump_agent" / "codex" / "dumpagent" / "DumpHooks.java"
AGENT = ROOT / "tools" / "dynamic_dump_agent" / "codex" / "dumpagent" / "DynamicStringDumpAgent.java"
THREADTEAR = ROOT / ".artifacts" / "tools" / "threadtear-gui-3.0.1-all.jar"
FIXTURE = ROOT / "tests" / "fixtures" / "java_agent"
ASCII_TMP_ROOT = Path(ROOT.drive + "\\codex-agent-tests")


class DynamicDumpAgentHookTests(unittest.TestCase):
    def test_transformer_does_not_read_target_method_locals(self):
        source = AGENT.read_text(encoding="utf-8")

        self.assertNotIn("Opcodes.SWAP", source)
        self.assertNotIn("Opcodes.ASTORE", source)
        self.assertNotIn("Opcodes.ALOAD, 0", source)
        self.assertIn("recordOutput", source)

    def test_record_does_not_propagate_writer_failures(self):
        tmp_path = ROOT / ".artifacts" / "tmp-tests" / ("dump-agent-hooks-" + uuid.uuid4().hex)
        tmp_path.mkdir(parents=True)
        try:
            smoke = tmp_path / "RecordFailureSmoke.java"
            smoke.write_text(
                textwrap.dedent(
                    """
                    import java.io.PrintWriter;
                    import java.io.StringWriter;
                    import java.lang.reflect.Field;

                    public class RecordFailureSmoke {
                      public static final class ThrowingWriter extends PrintWriter {
                        public ThrowingWriter() {
                          super(new StringWriter());
                        }

                        @Override
                        public void println(String value) {
                          throw new RuntimeException("writer failed");
                        }
                      }

                      public static void main(String[] args) throws Exception {
                        Field writer = codex.dumpagent.DumpHooks.class.getDeclaredField("writer");
                        writer.setAccessible(true);
                        writer.set(null, new ThrowingWriter());
                        codex.dumpagent.DumpHooks.record(null, null, "family");
                      }
                    }
                    """
                ),
                encoding="utf-8",
            )
            out = tmp_path / "classes"
            out.mkdir()
            subprocess.run(
                [str(JAVAC), "-d", str(out), str(HOOKS), str(smoke)],
                check=True,
                cwd=ROOT,
            )
            result = subprocess.run(
                [str(JAVA), "-cp", str(out), "RecordFailureSmoke"],
                cwd=ROOT,
                text=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
            )
            self.assertEqual(result.returncode, 0, result.stderr)
        finally:
            shutil.rmtree(tmp_path, ignore_errors=True)

    def test_agent_smoke_runs_with_verifier_enabled(self):
        tmp_path = ASCII_TMP_ROOT / ("dump-agent-smoke-" + uuid.uuid4().hex)
        tmp_path.mkdir(parents=True)
        try:
            classes = tmp_path / "classes"
            agent_classes = tmp_path / "agent-classes"
            classes.mkdir()
            agent_classes.mkdir()
            subprocess.run(
                [
                    str(JAVAC),
                    "-d",
                    str(classes),
                    str(FIXTURE / "fixture" / "DecoderTarget.java"),
                    str(FIXTURE / "fixture" / "AgentSmoke.java"),
                ],
                check=True,
                cwd=ROOT,
            )
            subprocess.run(
                [
                    str(JAVAC),
                    "-cp",
                    str(THREADTEAR),
                    "-d",
                    str(agent_classes),
                    str(HOOKS),
                    str(AGENT),
                ],
                check=True,
                cwd=ROOT,
            )
            manifest = tmp_path / "MANIFEST.MF"
            threadtear_copy = tmp_path / THREADTEAR.name
            shutil.copy2(THREADTEAR, threadtear_copy)
            manifest.write_text(
                "\n".join(
                    [
                        "Manifest-Version: 1.0",
                        "Premain-Class: codex.dumpagent.DynamicStringDumpAgent",
                        "Can-Redefine-Classes: false",
                        "Can-Retransform-Classes: false",
                        f"Class-Path: {threadtear_copy.name}",
                        "",
                    ]
                ),
                encoding="ascii",
            )
            agent_jar = tmp_path / "dynamic-string-dump-agent.jar"
            subprocess.run(
                [
                    str(ROOT / ".artifacts" / "tools" / "jdk8u492-b09" / "jdk8u492-b09" / "bin" / "jar.exe"),
                    "cfm",
                    str(agent_jar),
                    str(manifest),
                    "-C",
                    str(agent_classes),
                    "codex",
                ],
                check=True,
                cwd=ROOT,
            )
            targets = tmp_path / "targets.tsv"
            targets.write_text(
                "owner_class\tmethod_name\tfamily\tpriority\n"
                "fixture/DecoderTarget\tx\tDecoderTarget.x\tcritical\n",
                encoding="utf-8",
            )
            out = tmp_path / "strings.jsonl"
            result = subprocess.run(
                [
                    str(JAVA),
                    f"-javaagent:{agent_jar}=targets={targets},out={out},priorities=critical",
                    "-cp",
                    str(classes),
                    "fixture.AgentSmoke",
                ],
                cwd=ROOT,
                text=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
            )
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertIn("cipher-plain", result.stdout)
            dump = out.read_text(encoding="utf-8")
            self.assertIn('"family":"DecoderTarget.x"', dump)
            self.assertIn('"output":"cipher-plain"', dump)
        finally:
            shutil.rmtree(tmp_path, ignore_errors=True)


if __name__ == "__main__":
    unittest.main()
