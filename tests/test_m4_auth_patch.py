import json
import shutil
import subprocess
import textwrap
import unittest
import uuid
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
JDK_BIN = ROOT / ".artifacts" / "tools" / "jdk8u492-b09" / "jdk8u492-b09" / "bin"
JAVAC = JDK_BIN / "javac.exe"
JAVA = JDK_BIN / "java.exe"
JAVAP = JDK_BIN / "javap.exe"
APP_JAR = ROOT / ".artifacts" / "working" / "m1-02" / "App.jar"
CD53_APP_DLL = ROOT / ".artifacts" / "working" / "m8-d3" / "App.cd53.input.dll"
ASM_JAR = ROOT / ".artifacts" / "tools" / "threadtear-gui-3.0.1-all.jar"
JSON_JAR = ROOT / "data" / "lib" / "json-20170516.jar"
DATA_LIBS = ROOT / "data" / "lib" / "*"
SOURCE = ROOT / "tools" / "m4_auth_patch" / "M4AuthPatch.java"
CATALOG_SOURCE = ROOT / "tools" / "m4_auth_patch" / "M4RecoveryCatalog.java"
LOCAL_SPIDER_BRIDGE_SOURCE = ROOT / "tools" / "m4_auth_patch" / "M5LocalSpiderBridge.java"
WHATSAPP_NATIVE_PROFILES_SOURCE = (
    ROOT / "tools" / "m4_auth_patch" / "M8WhatsAppNativeProfiles.java"
)
WHATSAPP_EXTERNAL_BROWSERS_SOURCE = (
    ROOT / "tools" / "m4_auth_patch" / "M8WhatsAppExternalBrowsers.java"
)
WHATSAPP_DRIP_CAMPAIGNS_SOURCE = (
    ROOT / "tools" / "m4_auth_patch" / "M8WhatsAppDripCampaigns.java"
)
YES_CAPTCHA_BRIDGE_SOURCE = ROOT / "tools" / "m4_auth_patch" / "M5YesCaptchaBridge.java"
M8D7_DEFAULT_MENU_DISPATCH_SOURCE = (
    ROOT / "tools" / "m4_auth_patch" / "M8D7DefaultMenuDispatch.java"
)
M8D14_EXE_DIAG_SOURCE = ROOT / "tools" / "m4_auth_patch" / "M8D14ExeDiag.java"
C64_NATIVE_NETWORK_DIAG_SOURCE = ROOT / "tools" / "m4_auth_patch" / "C64NativeNetworkDiag.java"
TMP_ROOT = ROOT / ".artifacts" / "tmp-tests"


def classpath(*parts):
    return ";".join(str(part) for part in parts)


class M4AuthPatchTests(unittest.TestCase):
    def setUp(self):
        self.tmp_path = TMP_ROOT / ("m4-auth-patch-" + uuid.uuid4().hex)
        self.classes = self.tmp_path / "classes"
        self.probe_classes = self.tmp_path / "probe-classes"
        self.output_jar = self.tmp_path / "App-m4-auth-patched.jar"
        self.classes.mkdir(parents=True)
        self.probe_classes.mkdir()

    def tearDown(self):
        shutil.rmtree(self.tmp_path, ignore_errors=True)

    def compile_patcher(self):
        subprocess.run(
            [
                str(JAVAC),
                "-encoding",
                "UTF-8",
                "-cp",
                classpath(ASM_JAR, JSON_JAR, CD53_APP_DLL, DATA_LIBS),
                "-d",
                str(self.classes),
                str(CATALOG_SOURCE),
                str(LOCAL_SPIDER_BRIDGE_SOURCE),
                str(WHATSAPP_NATIVE_PROFILES_SOURCE),
                str(WHATSAPP_EXTERNAL_BROWSERS_SOURCE),
                str(WHATSAPP_DRIP_CAMPAIGNS_SOURCE),
                str(YES_CAPTCHA_BRIDGE_SOURCE),
                str(M8D7_DEFAULT_MENU_DISPATCH_SOURCE),
                str(M8D14_EXE_DIAG_SOURCE),
                str(C64_NATIVE_NETWORK_DIAG_SOURCE),
                str(SOURCE),
            ],
            cwd=ROOT,
            check=True,
        )

    def compile_and_run_catalog_probe(self, class_name, source):
        probe_source = self.tmp_path / (class_name + ".java")
        probe_source.write_text(textwrap.dedent(source).strip(), encoding="utf-8")
        subprocess.run(
            [
                str(JAVAC),
                "-encoding",
                "UTF-8",
                "-cp",
                str(JSON_JAR),
                "-d",
                str(self.probe_classes),
                str(CATALOG_SOURCE),
                str(probe_source),
            ],
            cwd=ROOT,
            check=True,
        )
        return subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.probe_classes, JSON_JAR),
                class_name,
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )

    def compile_and_run_yescaptcha_bridge_probe(self, class_name, source, *args):
        probe_dir = self.tmp_path / "yescaptcha-probe-src" / "com" / "sbf" / "main" / "ext" / "gg"
        probe_dir.mkdir(parents=True)
        probe_source = probe_dir / (class_name + ".java")
        probe_source.write_text(textwrap.dedent(source).strip(), encoding="utf-8")
        subprocess.run(
            [
                str(JAVAC),
                "-encoding",
                "UTF-8",
                "-cp",
                str(JSON_JAR),
                "-d",
                str(self.probe_classes),
                str(YES_CAPTCHA_BRIDGE_SOURCE),
                str(probe_source),
            ],
            cwd=ROOT,
            check=True,
        )
        return subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.probe_classes, JSON_JAR),
                "com.sbf.main.ext.gg." + class_name,
                *[str(arg) for arg in args],
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )

    def test_recovery_catalog_has_nine_products(self):
        probe = self.compile_and_run_catalog_probe(
            "M4RecoveryProductProbe",
            """
            import org.json.JSONArray;
            import org.json.JSONObject;

            public class M4RecoveryProductProbe {
                public static void main(String[] args) {
                    JSONArray products =
                            new JSONObject(M4RecoveryCatalog.productModulesJson())
                                    .getJSONArray("data");
                    String[] codes = {
                        "whatsapp", "tiktok", "facebook", "instagram", "twitter",
                        "telegram", "geo", "wskefu", "aishope"
                    };
                    if (products.length() != codes.length) {
                        throw new AssertionError("product count: " + products.length());
                    }
                    for (int i = 0; i < codes.length; i++) {
                        JSONObject product = products.getJSONObject(i);
                        if (product.getInt("id") != 9101 + i) {
                            throw new AssertionError("recovery id: " + product);
                        }
                        if (!codes[i].equals(product.getString("code"))) {
                            throw new AssertionError("code: " + product);
                        }
                        if (i < 8 && product.getInt("status") != 1) {
                            throw new AssertionError("enterable: " + product);
                        }
                        if (i == 8
                                && (product.getInt("status") == 0
                                        || product.getInt("status") == 1)) {
                            throw new AssertionError("aishope must be unopened: " + product);
                        }
                        if (product.optString("logoSvg").length() == 0
                                || product.optString("primaryColor").length() == 0
                                || product.optString("secondaryColor").length() == 0) {
                            throw new AssertionError("product shape: " + product);
                        }
                    }
                    System.out.println("M4_RECOVERY_PRODUCTS_OK");
                }
            }
            """,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("M4_RECOVERY_PRODUCTS_OK", probe.stdout)

    def test_yescaptcha_bridge_loads_local_key_and_normalizes_solution(self):
        runtime = self.tmp_path / "runtime"
        (runtime / "config").mkdir(parents=True)
        (runtime / "config" / ".env").write_text(
            "YESCAPTCHA_CLIENT_KEY=abcd1234567890\n", encoding="utf-8"
        )
        probe = self.compile_and_run_yescaptcha_bridge_probe(
            "M5YesCaptchaBridgeProbe",
            """
            package com.sbf.main.ext.gg;

            import java.nio.file.Paths;
            import org.json.JSONObject;

            public class M5YesCaptchaBridgeProbe {
                public static void main(String[] args) throws Exception {
                    String key = M5YesCaptchaBridge.loadClientKeyForTest(Paths.get(args[0]));
                    if (!"abcd1234567890".equals(key)) {
                        throw new AssertionError("key not loaded: " + key);
                    }
                    if (!"abcd****7890".equals(M5YesCaptchaBridge.maskClientKeyForTest(key))) {
                        throw new AssertionError("mask failed");
                    }
                    String normalized =
                            M5YesCaptchaBridge.normalizeReadyResultForTest(
                                    "{\\"solution\\":{\\"objects\\":[1,3],\\"hasObject\\":true}}");
                    JSONObject solution = new JSONObject(normalized).getJSONObject("solution");
                    if (solution.getJSONArray("objects").length() != 2
                            || !solution.getBoolean("hasObject")) {
                        throw new AssertionError("solution shape: " + normalized);
                    }
                    System.out.println("M5D_YESCAPTCHA_BRIDGE_PROBE_OK");
                }
            }
            """,
            runtime,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("M5D_YESCAPTCHA_BRIDGE_PROBE_OK", probe.stdout)

    def test_yescaptcha_bridge_does_not_treat_task_id_as_ready(self):
        probe = self.compile_and_run_yescaptcha_bridge_probe(
            "M5YesCaptchaBridgeReadyProbe",
            """
            package com.sbf.main.ext.gg;

            public class M5YesCaptchaBridgeReadyProbe {
                public static void main(String[] args) throws Exception {
                    String createOnly = M5YesCaptchaBridge.readySolutionFromForTest(
                            "{\\"errorId\\":0,\\"taskId\\":123456}");
                    if (createOnly != null) {
                        throw new AssertionError("taskId-only create response must poll: " + createOnly);
                    }
                    String emptyBareSolution = M5YesCaptchaBridge.readySolutionFromForTest(
                            "{\\"errorId\\":0,\\"solution\\":{}}");
                    if (emptyBareSolution != null) {
                        throw new AssertionError("bare empty solution must not be immediate ready");
                    }
                    String ready = M5YesCaptchaBridge.readySolutionFromForTest(
                            "{\\"errorId\\":0,\\"status\\":\\"ready\\",\\"solution\\":{\\"objects\\":[1,3,9],\\"hasObject\\":true}}");
                    if (ready == null || !ready.contains("\\\"objects\\\"")) {
                        throw new AssertionError("ready solution was not normalized: " + ready);
                    }
                    String summary = M5YesCaptchaBridge.responseSummaryForTest(
                            "{\\"errorId\\":0,\\"status\\":\\"ready\\",\\"solution\\":{\\"objects\\":[1,3,9],\\"hasObject\\":true}}");
                    if (!summary.contains("status=ready")
                            || !summary.contains("objects=[1,3,9]")
                            || !summary.contains("hasObject=true")) {
                        throw new AssertionError("summary missing ready fields: " + summary);
                    }
                    System.out.println("M5D_YESCAPTCHA_READY_PROBE_OK");
                }
            }
            """,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("M5D_YESCAPTCHA_READY_PROBE_OK", probe.stdout)

    def test_whatsapp_external_browser_manager_dry_run_creates_isolated_profile(self):
        probe_source = self.tmp_path / "M8B1C3ExternalBrowserProbe.java"
        probe_source.write_text(
            textwrap.dedent(
                """
                import com.sbf.main.jxbrowser.M8WhatsAppExternalBrowsers;
                import com.sbf.main.jxbrowser.M5LocalSpiderBridge;
                import org.json.JSONObject;

                public class M8B1C3ExternalBrowserProbe {
                    public static void main(String[] args) throws Exception {
                        System.setProperty("m8.whatsapp.external.dryRun", "true");
                        System.setProperty("m8.whatsapp.bridge.port", "0");
                        String baseDir = args[0];
                        JSONObject started =
                                new JSONObject(
                                        M8WhatsAppExternalBrowsers.start(
                                                baseDir, "wa-001", "+15550000001"));
                        if (started.getInt("code") != 200) {
                            throw new AssertionError("start failed: " + started);
                        }
                        JSONObject data = started.getJSONObject("data");
                        if (!"wa-001".equals(data.getString("profileId"))) {
                            throw new AssertionError("wrong profile: " + data);
                        }
                        if (!data.getString("profilePath").contains("wa-external-profiles")) {
                            throw new AssertionError("not external profile path: " + data);
                        }
                        if (data.getInt("debugPort") < 43000) {
                            throw new AssertionError("bad debug port: " + data);
                        }
                        if (!data.getString("url").contains("m8Profile=wa-001")) {
                            throw new AssertionError("missing profile url marker: " + data);
                        }
                        JSONObject accounts =
                                new JSONObject(M5LocalSpiderBridge.listWhatsAppAccounts(baseDir));
                        if (accounts.getJSONArray("rows").length() != 1) {
                            throw new AssertionError("account row missing: " + accounts);
                        }
                        JSONObject row = accounts.getJSONArray("rows").getJSONObject(0);
                        if (!"profile_ready".equals(row.getString("status"))
                                || !row.getString("lastStatusJson").contains("external-browser")) {
                            throw new AssertionError("wrong account status: " + row);
                        }
                        if (!row.getString("lastStatusJson").contains("wa-external-profiles")) {
                            throw new AssertionError("external profile path missing from status: " + row);
                        }
                        JSONObject stopped =
                                new JSONObject(M8WhatsAppExternalBrowsers.stop(baseDir, "wa-001"));
                        if (stopped.getInt("code") != 200) {
                            throw new AssertionError("stop failed: " + stopped);
                        }
                        M8WhatsAppExternalBrowsers.shutdownForTest();
                        System.out.println("M8B1C3_EXTERNAL_BROWSER_MANAGER_OK");
                    }
                }
                """
            ).strip(),
            encoding="utf-8",
        )
        subprocess.run(
            [
                str(JAVAC),
                "-encoding",
                "UTF-8",
                "-cp",
                classpath(JSON_JAR, DATA_LIBS),
                "-d",
                str(self.probe_classes),
                str(LOCAL_SPIDER_BRIDGE_SOURCE),
                str(WHATSAPP_EXTERNAL_BROWSERS_SOURCE),
                str(probe_source),
            ],
            cwd=ROOT,
            check=True,
        )
        probe = subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.probe_classes, JSON_JAR, DATA_LIBS),
                "M8B1C3ExternalBrowserProbe",
                str(self.tmp_path / "runtime"),
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("M8B1C3_EXTERNAL_BROWSER_MANAGER_OK", probe.stdout)

    def test_whatsapp_drip_campaign_dry_run_imports_cleans_assigns_and_never_sends(self):
        probe_source = self.tmp_path / "M8B2ADripCampaignProbe.java"
        probe_source.write_text(
            textwrap.dedent(
                """
                import com.sbf.main.jxbrowser.M5LocalSpiderBridge;
                import com.sbf.main.jxbrowser.M8WhatsAppDripCampaigns;
                import java.nio.file.Files;
                import java.nio.file.Path;
                import java.nio.file.Paths;
                import java.sql.Connection;
                import java.sql.DriverManager;
                import java.sql.PreparedStatement;
                import java.sql.ResultSet;
                import java.sql.Statement;
                import org.json.JSONArray;
                import org.json.JSONObject;

                public class M8B2ADripCampaignProbe {
                    private static void insertSpiderRow(Connection conn, String phone) throws Exception {
                        JSONObject row = new JSONObject()
                                .put("phone", phone)
                                .put("phoneRaw", phone)
                                .put("phoneValidationStatus", "ACCEPT")
                                .put("phoneValidationReason", "KNOWN_COUNTRY_CODE")
                                .put("keywords", "soccer jersey")
                                .put("pltCode", "facebook.com")
                                .put("url", "https://example.test/" + Math.abs(phone.hashCode()));
                        try (PreparedStatement stmt = conn.prepareStatement(
                                "insert into spider_data(spider_modal,spider_code,json_data,time) values(?,?,?,?)")) {
                            stmt.setString(1, "whatsapp");
                            stmt.setString(2, "whatsapp_users_lists");
                            stmt.setString(3, row.toString());
                            stmt.setLong(4, System.currentTimeMillis());
                            stmt.executeUpdate();
                        }
                    }

                    public static void main(String[] args) throws Exception {
                        String baseDir = args[0];
                        M5LocalSpiderBridge.upsertWhatsAppAccount(
                                baseDir,
                                "wa-a",
                                "+10000000001",
                                "logged_in",
                                "{\\"snapshot\\":{\\"href\\":\\"https://web.whatsapp.com/\\",\\"status\\":\\"logged_in\\"}}");
                        M5LocalSpiderBridge.upsertWhatsAppAccount(
                                baseDir,
                                "wa-b",
                                "+10000000002",
                                "logged_in",
                                "{\\"snapshot\\":{\\"href\\":\\"https://web.whatsapp.com/\\",\\"status\\":\\"logged_in\\"}}");

                        Path sourceDb = Paths.get(baseDir, "input", "db_spider_data_whatsapp_users_lists.data");
                        Files.createDirectories(sourceDb.getParent());
                        Class.forName("org.sqlite.JDBC");
                        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + sourceDb.toString())) {
                            try (Statement stmt = conn.createStatement()) {
                                stmt.executeUpdate("create table spider_data (spider_modal varchar, spider_code varchar, json_data varchar, time bigint, id integer primary key autoincrement)");
                            }
                            insertSpiderRow(conn, "+15164262910");
                            insertSpiderRow(conn, "+15164262910");
                            insertSpiderRow(conn, "+14036300125,  +14036306816");
                            insertSpiderRow(conn, "not-a-phone");
                            insertSpiderRow(conn, "001234567890");
                            insertSpiderRow(conn, "+11111111111");
                        }

                        JSONObject result = new JSONObject(
                                M8WhatsAppDripCampaigns.dryRun(
                                        baseDir,
                                        sourceDb.toString(),
                                        "[\\"wa-a\\",\\"wa-b\\"]",
                                        "dry run hello",
                                        "{\\"simulateRiskSamples\\":true,\\"sleepMinMs\\":1,\\"sleepMaxMs\\":2,\\"dailyLimit\\":99,\\"hourlyLimit\\":99}"));
                        if (result.optInt("code") != 200 || !result.optBoolean("dryRun")) {
                            throw new AssertionError("bad result: " + result);
                        }
                        JSONObject importSummary = result.getJSONObject("importSummary");
                        if (importSummary.optInt("rawRows") != 6
                                || importSummary.optInt("extractedCandidates") != 7
                                || importSummary.optInt("validRecipients") != 4
                                || importSummary.optInt("duplicateRecipients") != 1
                                || importSummary.optInt("invalidRecipients") != 2
                                || importSummary.optInt("assignedRecipients") != 4) {
                            throw new AssertionError("bad import summary: " + importSummary);
                        }
                        JSONObject runSummary = result.getJSONObject("runSummary");
                        if (runSummary.optInt("trueSendAttempts") != 0
                                || runSummary.optInt("riskSkipped") != 2
                                || runSummary.optInt("wouldSend") != 2
                                || runSummary.optInt("statusWritten") != 4) {
                            throw new AssertionError("bad run summary: " + runSummary);
                        }
                        if (result.toString().contains("window.sendMessage")
                                || result.toString().contains("sendHasLink")
                                || result.toString().contains("my_sendImageAndText")) {
                            throw new AssertionError("dry-run result leaked true send hook: " + result);
                        }

                        Path dripDb = Paths.get(baseDir, "data", "db_b_whatsapp_drip.data");
                        if (!Files.isRegularFile(dripDb)) {
                            throw new AssertionError("drip db missing: " + dripDb);
                        }
                        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dripDb.toString())) {
                            try (Statement stmt = conn.createStatement();
                                    ResultSet rs = stmt.executeQuery("select profile_id,count(*) from b_drip_queue_items group by profile_id order by profile_id")) {
                                JSONArray counts = new JSONArray();
                                while (rs.next()) {
                                    counts.put(rs.getString(1) + ":" + rs.getInt(2));
                                }
                                if (!counts.toString().contains("wa-a:2")
                                        || !counts.toString().contains("wa-b:2")) {
                                    throw new AssertionError("bad queue split: " + counts);
                                }
                            }
                            try (Statement stmt = conn.createStatement();
                                    ResultSet rs = stmt.executeQuery("select count(*) from b_drip_events where event_type='WOULD_SEND'")) {
                                if (!rs.next() || rs.getInt(1) != 2) {
                                    throw new AssertionError("would-send event count mismatch");
                                }
                            }
                            try (Statement stmt = conn.createStatement();
                                    ResultSet rs = stmt.executeQuery("select count(*) from b_drip_events where event_type='TRUE_SEND_ATTEMPT'")) {
                                if (!rs.next() || rs.getInt(1) != 0) {
                                    throw new AssertionError("true send event must be zero");
                                }
                            }
                        }
                        System.out.println("M8B2A_DRIP_DRY_RUN_OK");
                    }
                }
                """
            ).strip(),
            encoding="utf-8",
        )
        subprocess.run(
            [
                str(JAVAC),
                "-encoding",
                "UTF-8",
                "-cp",
                classpath(JSON_JAR, DATA_LIBS),
                "-d",
                str(self.probe_classes),
                str(LOCAL_SPIDER_BRIDGE_SOURCE),
                str(WHATSAPP_DRIP_CAMPAIGNS_SOURCE),
                str(probe_source),
            ],
            cwd=ROOT,
            check=True,
        )
        probe = subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.probe_classes, JSON_JAR, DATA_LIBS),
                "M8B2ADripCampaignProbe",
                str(self.tmp_path / "runtime"),
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("M8B2A_DRIP_DRY_RUN_OK", probe.stdout)

    def test_spider_callback_post_data_is_redirected_to_local_sink(self):
        self.compile_patcher()

        result = self.run_patcher()

        self.assertEqual(result.returncode, 0, result.stderr)
        post_data_block = self.javap_method_block(
            "public void postData(java.lang.String);",
            "com.sbf.main.cloud.spider.SpiderCallback",
        )
        end_task_block = self.javap_method_block(
            "public void endTask();",
            "com.sbf.main.cloud.spider.SpiderCallback",
        )
        self.assertIn("M5D_POSTDATA_LOCAL_CALLBACK", post_data_block)
        self.assertIn("com/sbf/main/jxbrowser/M5LocalSpiderBridge.postCollectedData", post_data_block)
        self.assertIn("M5D_ENDTASK_LOCAL_CALLBACK", end_task_block)
        self.assertIn("com/sbf/main/jxbrowser/M5LocalSpiderBridge.endCollectedTask", end_task_block)

    def test_recovery_catalog_has_product_specific_menus(self):
        probe = self.compile_and_run_catalog_probe(
            "M4RecoveryMenuProbe",
            """
            import java.util.HashMap;
            import java.util.HashSet;
            import java.util.Map;
            import java.util.Set;
            import org.json.JSONArray;
            import org.json.JSONObject;

            public class M4RecoveryMenuProbe {
                public static void main(String[] args) {
                    JSONObject menus = new JSONObject(M4RecoveryCatalog.pcMenusJson());
                    JSONArray entries = menus.getJSONArray("scfs");
                    Map<Integer, Integer> counts = new HashMap<Integer, Integer>();
                    Set<String> whatsappNames = new HashSet<String>();
                    for (int i = 0; i < entries.length(); i++) {
                        JSONObject item = entries.getJSONObject(i);
                        int productId = item.getInt("productId");
                        int id = item.getInt("id");
                        if (id < productId * 100) {
                            throw new AssertionError("not a recovery id: " + item);
                        }
                        if (item.getString("code").startsWith("C2850000")
                                || item.getString("name").contains("AIGC Video")
                                || item.getString("name").contains("Graphic Video")) {
                            throw new AssertionError("temporary AIGC menu: " + item);
                        }
                        if (item.getString("icon").contains("/")
                                || item.getString("icon").endsWith(".svg")) {
                            throw new AssertionError("non-resource icon: " + item);
                        }
                        boolean whatsappCollectParent =
                                "C4749_006".equals(item.getString("code"));
                        boolean whatsappCollectChild =
                                "REC_WHATSAPP_COLLECT_USERS_ROUTE".equals(item.getString("code"));
                        boolean whatsappCollectTabChild =
                                item.getString("code").startsWith("REC_WHATSAPP_COLLECT_TAB_");
                        boolean whatsappOneLineParent =
                                "REC_WHATSAPP_ONELINE".equals(item.getString("code"));
                        boolean whatsappOneLineChild =
                                "REC_WHATSAPP_ONELINE_ROUTE".equals(item.getString("code"));
                        boolean whatsappAgentModelParent =
                                "REC_WHATSAPP_AGENT_MODEL".equals(item.getString("code"));
                        boolean whatsappAgentModelChild =
                                "REC_WHATSAPP_AGENT_MODEL_ROUTE".equals(item.getString("code"));
                        boolean whatsappClawParent =
                                "REC_WHATSAPP_CLAW".equals(item.getString("code"));
                        boolean whatsappClawTabChild =
                                item.getString("code").startsWith("REC_WHATSAPP_CLAW_TAB_");
                        boolean whatsappSuperParent =
                                "REC_WHATSAPP_SUPER".equals(item.getString("code"));
                        boolean whatsappSuperChild =
                                "REC_WHATSAPP_SUPER_ENV_ROUTE".equals(item.getString("code"));
                        boolean whatsappDataParent =
                                "C4749_007".equals(item.getString("code"));
                        boolean whatsappDataChild =
                                "REC_WHATSAPP_AI_DATA_ROUTE".equals(item.getString("code"));
                        boolean whatsappFilterParent =
                                "C4749_009".equals(item.getString("code"));
                        boolean whatsappFilterChild =
                                "REC_WHATSAPP_AI_FILTER_ROUTE".equals(item.getString("code"));
                        boolean whatsappKefuParent =
                                "C4749_011".equals(item.getString("code"));
                        boolean whatsappKefuChild =
                                "REC_WHATSAPP_AI_KEFU_ROUTE".equals(item.getString("code"));
                        boolean d3FbLocalParent =
                                item.optString("evidence").startsWith("d3-fb-local:");
                        boolean d3FbLocalChild =
                                item.optString("evidence").startsWith("d3-fb-local-child:");
                        boolean d4TkLocalParent = item.optString("evidence").startsWith("d4-tk-local:");
                        boolean d4TkLocalChild = item.optString("evidence").startsWith("d4-tk-local-child:");
                        boolean d5TgLocalParent = item.optString("evidence").startsWith("d5-tg-local:");
                        boolean d5TgLocalChild = item.optString("evidence").startsWith("d5-tg-local-child:");
                        boolean d5GeoLocalParent = item.optString("evidence").startsWith("d5-geo-local:");
                        boolean d5GeoLocalChild = item.optString("evidence").startsWith("d5-geo-local-child:");
                        boolean d5WaLocalParent = item.optString("evidence").startsWith("d5-wa-local:");
                        boolean d5WaLocalChild = item.optString("evidence").startsWith("d5-wa-local-child:");
                        boolean d2InsLocalParent =
                                item.optString("evidence").startsWith("d2-ins-local:");
                        boolean d2InsLocalChild =
                                item.optString("evidence").startsWith("d2-ins-local-child:");
                        boolean d1XLocalParent =
                                item.optString("evidence").startsWith("d1-x-local:");
                        boolean d1XLocalChild =
                                item.optString("evidence").startsWith("d1-x-local-child:");
                        boolean whatsappAdvertisingParent =
                                "C3460_001".equals(item.getString("code"));
                        boolean whatsappAdvertisingChild =
                                "REC_WHATSAPP_ADVERTISING_ROUTE".equals(item.getString("code"));
                        boolean c5PlatformParent =
                                item.optString("evidence").startsWith("c5-platform-route:");
                        boolean c5PlatformChild =
                                item.optString("evidence").startsWith("c5-platform-route-child:");
                        if (whatsappOneLineParent) {
                            if (!"JSinglepage".equals(item.getString("localCode"))
                                    || !"/pc/aigc/aichat_dialog".equals(item.getString("linkUrl"))
                                    || !item.optString("evidence").contains("recovery-route:aichat-dialog")
                                    || item.getInt("webFlg") != 1) {
                                throw new AssertionError("WhatsApp one-line recovery route: " + item);
                            }
                        } else if (whatsappOneLineChild) {
                            if (!"/pc/aigc/aichat_dialog".equals(item.getString("localCode"))
                                    || !"JSinglepage:/pc/aigc/aichat_dialog".equals(item.getString("linkUrl"))
                                    || !item.optString("evidence").contains("recovery-route-child:j2026-h-field-map:aichat-dialog")
                                    || item.getInt("webFlg") != 1) {
                                throw new AssertionError("WhatsApp one-line child recovery route: " + item);
                            }
                        } else if (whatsappAgentModelParent) {
                            if (!"JSinglepage".equals(item.getString("localCode"))
                                    || !"/aiAgent/smartAi".equals(item.getString("linkUrl"))
                                    || !item.optString("evidence").contains("recovery-route:smart-ai")
                                    || item.getInt("webFlg") != 1) {
                                throw new AssertionError("WhatsApp smartAi recovery route: " + item);
                            }
                        } else if (whatsappAgentModelChild) {
                            if (!"/aiAgent/smartAi".equals(item.getString("localCode"))
                                    || !"JSinglepage:/aiAgent/smartAi".equals(item.getString("linkUrl"))
                                    || !item.optString("evidence").contains("recovery-route-child:j2026-h-field-map:smart-ai")
                                    || item.getInt("webFlg") != 1) {
                                throw new AssertionError("WhatsApp smartAi child recovery route: " + item);
                            }
                        } else if (whatsappClawParent) {
                            if (!"JSinglepage".equals(item.getString("localCode"))
                                    || !"/wsClaw/browser".equals(item.getString("linkUrl"))
                                    || !item.optString("evidence").contains("recovery-route:ws-claw")
                                    || item.getInt("webFlg") != 1) {
                                throw new AssertionError("WhatsApp claw recovery route: " + item);
                            }
                        } else if (whatsappClawTabChild) {
                            if (!item.getString("localCode").startsWith("/wsClaw/")
                                    || !"JSinglepage".equals(item.getString("linkUrl"))
                                    || !item.optString("evidence").contains("m8-6-b-menu-tab:wsClaw:")
                                    || item.getInt("webFlg") != 1) {
                                throw new AssertionError("WhatsApp claw tab child recovery route: " + item);
                            }
                        } else if (whatsappSuperParent) {
                            if (!"JSinglepage".equals(item.getString("localCode"))
                                    || !"/pc/sender/senderGlobalControls/mysuperenvironment".equals(item.getString("linkUrl"))
                                    || !item.optString("evidence").contains("recovery-route:super-environment")
                                    || item.getInt("webFlg") != 1) {
                                throw new AssertionError("WhatsApp super environment recovery route: " + item);
                            }
                        } else if (whatsappSuperChild) {
                            if (!"/pc/sender/senderGlobalControls/mysuperenvironment".equals(item.getString("localCode"))
                                    || !"JSinglepage".equals(item.getString("linkUrl"))
                                    || !item.optString("evidence").contains("m8-8-b-menu-tab:senderGlobalControls:mysuperenvironment")
                                    || item.getInt("webFlg") != 1) {
                                throw new AssertionError("WhatsApp super environment child recovery route: " + item);
                            }
                        } else if (whatsappCollectParent) {
                            if (!"JSinglepage".equals(item.getString("localCode"))
                                    || !"/pc/dataCollect/collectionTask?modal=whatsapp_users_lists&moduleCode=whatsapp".equals(item.getString("linkUrl"))
                                    || !item.optString("evidence").contains("recovery-route")
                                    || item.getInt("webFlg") != 1) {
                                throw new AssertionError("WhatsApp collect recovery route: " + item);
                            }
                        } else if (whatsappCollectChild) {
                            throw new AssertionError("old single collect child route must be replaced by tab children: " + item);
                        } else if (whatsappCollectTabChild) {
                            if (!item.getString("localCode").startsWith("/pc/dataCollect/collectionTask?modal=")
                                    || !"JSinglepage".equals(item.getString("linkUrl"))
                                    || !item.optString("evidence").contains("m5d11-menu-tab:dataCollect:")
                                    || item.getInt("webFlg") != 1) {
                                throw new AssertionError("WhatsApp collect tab child recovery route: " + item);
                            }
                        } else if (whatsappDataParent) {
                            if (!"JSinglepage".equals(item.getString("localCode"))
                                    || !"/pc/aicloud/my".equals(item.getString("linkUrl"))
                                    || !"original-i18n".equals(item.optString("evidence"))
                                    || item.getInt("webFlg") != 1) {
                                throw new AssertionError("WhatsApp AI data must stay on original AiCloud route: " + item);
                            }
                        } else if (whatsappDataChild) {
                            if (!"/pc/aicloud/my".equals(item.getString("localCode"))
                                    || !"JSinglepage:/pc/aicloud/my".equals(item.getString("linkUrl"))
                                    || !item.optString("evidence").contains("recovery-route-child:j2026-h-field-map:aicloud-my")
                                    || item.getInt("webFlg") != 1) {
                                throw new AssertionError("WhatsApp AI data child must open AiCloud route: " + item);
                            }
                        } else if (whatsappFilterParent) {
                            if (!"JSinglepage".equals(item.getString("localCode"))
                                    || !"/ws/wsfilter/home".equals(item.getString("linkUrl"))
                                    || !item.optString("evidence").contains("recovery-route:wsfilter-home")
                                    || item.getInt("webFlg") != 1) {
                                throw new AssertionError("WhatsApp AI filter recovery route: " + item);
                            }
                        } else if (whatsappFilterChild) {
                            if (!"/ws/wsfilter/home".equals(item.getString("localCode"))
                                    || !"JSinglepage:/ws/wsfilter/home".equals(item.getString("linkUrl"))
                                    || !item.optString("evidence").contains("recovery-route-child:j2026-h-field-map:wsfilter-home")
                                    || item.getInt("webFlg") != 1) {
                                throw new AssertionError("WhatsApp AI filter child recovery route: " + item);
                            }
                        } else if (whatsappKefuParent) {
                            if (!"https://web.whatsapp.com".equals(item.getString("localCode"))
                                    || !"https://web.whatsapp.com".equals(item.getString("linkUrl"))
                                    || !item.optString("evidence").contains("recovery-route:aggregation-kefu")
                                    || item.getInt("webFlg") != 1) {
                                throw new AssertionError("WhatsApp AI kefu recovery route: " + item);
                            }
                        } else if (whatsappKefuChild) {
                            if (!"https://web.whatsapp.com".equals(item.getString("localCode"))
                                    || !"https://web.whatsapp.com".equals(item.getString("linkUrl"))
                                    || !item.optString("evidence").contains("recovery-route-child:j2026-h-field-map:whatsapp-web")
                                    || item.getInt("webFlg") != 1) {
                                throw new AssertionError("WhatsApp AI kefu child recovery route: " + item);
                            }
                        } else if (d4TkLocalParent) {
                            if (!"JSinglepage".equals(item.getString("localCode")) || !item.getString("linkUrl").startsWith("/pc/local/tiktok/") || item.getString("linkUrl").contains("http")) throw new AssertionError("D-4 TK parent route: " + item);
                        } else if (d4TkLocalChild) {
                            if (!item.getString("localCode").startsWith("/pc/local/tiktok/") || !("JSinglepage:" + item.getString("localCode")).equals(item.getString("linkUrl")) || item.getInt("treeEndFlg") != 1) throw new AssertionError("D-4 TK child route: " + item);
                        } else if (d5TgLocalParent) {
                            if (!"JSinglepage".equals(item.getString("localCode")) || !item.getString("linkUrl").startsWith("/pc/local/tg/") || item.getString("linkUrl").contains("http")) throw new AssertionError("D-5 TG parent route: " + item);
                        } else if (d5TgLocalChild) {
                            if (!item.getString("localCode").startsWith("/pc/local/tg/") || !("JSinglepage:" + item.getString("localCode")).equals(item.getString("linkUrl")) || item.getInt("treeEndFlg") != 1) throw new AssertionError("D-5 TG child route: " + item);
                        } else if (d5GeoLocalParent) {
                            if (!"JSinglepage".equals(item.getString("localCode")) || !item.getString("linkUrl").startsWith("/pc/local/geo/") || item.getString("linkUrl").contains("http")) throw new AssertionError("D-5 GEO parent route: " + item);
                        } else if (d5GeoLocalChild) {
                            if (!item.getString("localCode").startsWith("/pc/local/geo/") || !("JSinglepage:" + item.getString("localCode")).equals(item.getString("linkUrl")) || item.getInt("treeEndFlg") != 1) throw new AssertionError("D-5 GEO child route: " + item);
                        } else if (d5WaLocalParent) {
                            if (!"JSinglepage".equals(item.getString("localCode")) || !item.getString("linkUrl").startsWith("/pc/local/wa/") || item.getString("linkUrl").contains("http")) throw new AssertionError("D-5 WA parent route: " + item);
                        } else if (d5WaLocalChild) {
                            if (!item.getString("localCode").startsWith("/pc/local/wa/") || !("JSinglepage:" + item.getString("localCode")).equals(item.getString("linkUrl")) || item.getInt("treeEndFlg") != 1) throw new AssertionError("D-5 WA child route: " + item);
                        } else if (d3FbLocalParent) {
                            if (!"JSinglepage".equals(item.getString("localCode"))
                                    || !item.getString("linkUrl").startsWith("/pc/local/fb/")
                                    || item.getString("linkUrl").contains("http")
                                    || !item.optString("evidence").startsWith("d3-fb-local:")
                                    || item.getInt("webFlg") != 1) {
                                throw new AssertionError("D-3 FB local parent route: " + item);
                            }
                        } else if (d3FbLocalChild) {
                            if (!item.getString("localCode").startsWith("/pc/local/fb/")
                                    || !("JSinglepage:" + item.getString("localCode")).equals(item.getString("linkUrl"))
                                    || item.getInt("treeEndFlg") != 1
                                    || item.getInt("webFlg") != 1) {
                                throw new AssertionError("D-3 FB local child route: " + item);
                            }
                        } else if (d2InsLocalParent) {
                            if (!"JSinglepage".equals(item.getString("localCode"))
                                    || !item.getString("linkUrl").startsWith("/pc/local/ins/")
                                    || item.getString("linkUrl").contains("http")
                                    || !item.optString("evidence").startsWith("d2-ins-local:")
                                    || item.getInt("webFlg") != 1) {
                                throw new AssertionError("D-2 Ins local parent route: " + item);
                            }
                        } else if (d2InsLocalChild) {
                            if (!item.getString("localCode").startsWith("/pc/local/ins/")
                                    || !("JSinglepage:" + item.getString("localCode")).equals(item.getString("linkUrl"))
                                    || item.getInt("treeEndFlg") != 1
                                    || item.getInt("webFlg") != 1) {
                                throw new AssertionError("D-2 Ins local child route: " + item);
                            }
                        } else if (d1XLocalParent) {
                            if (!"JSinglepage".equals(item.getString("localCode"))
                                    || !item.getString("linkUrl").startsWith("/pc/local/x/")
                                    || item.getString("linkUrl").contains("http")
                                    || !item.optString("evidence").startsWith("d1-x-local:")
                                    || item.getInt("webFlg") != 1) {
                                throw new AssertionError("D-1 X local parent route: " + item);
                            }
                        } else if (d1XLocalChild) {
                            if (!item.getString("localCode").startsWith("/pc/local/x/")
                                    || !("JSinglepage:" + item.getString("localCode")).equals(item.getString("linkUrl"))
                                    || item.getInt("treeEndFlg") != 1
                                    || item.getInt("webFlg") != 1) {
                                throw new AssertionError("D-1 X local child route: " + item);
                            }
                        } else if (whatsappAdvertisingParent) {
                            if (!"JSinglepage".equals(item.getString("localCode"))
                                    || !"/views/overseasAds/dataBoard".equals(item.getString("linkUrl"))
                                    || !item.optString("evidence").contains("c67-advertising")) {
                                throw new AssertionError("WhatsApp advertising parent recovery route: " + item);
                            }
                        } else if (whatsappAdvertisingChild) {
                            if (!"/views/overseasAds/dataBoard".equals(item.getString("localCode"))
                                    || !"JSinglepage".equals(item.getString("linkUrl"))
                                    || item.getInt("treeEndFlg") != 1) {
                                throw new AssertionError("WhatsApp advertising child recovery route: " + item);
                            }
                        } else if (c5PlatformParent) {
                            if (!"JSinglepage".equals(item.getString("localCode"))
                                    || !item.getString("linkUrl").startsWith("/")
                                    || item.getString("linkUrl").contains("http")
                                    || item.getInt("webFlg") != 1) {
                                throw new AssertionError("C5 platform parent recovery route: " + item);
                            }
                        } else if (c5PlatformChild) {
                            if (!item.getString("localCode").startsWith("/")
                                    || !"JSinglepage".equals(item.getString("linkUrl"))
                                    || item.getInt("treeEndFlg") != 1
                                    || item.getInt("webFlg") != 1) {
                                throw new AssertionError("C5 platform child recovery route: " + item);
                            }
                        } else if (!"JSinglepage".equals(item.getString("localCode"))
                                || !"/pc/aicloud/my".equals(item.getString("linkUrl"))
                                || item.getInt("webFlg") != 1) {
                            throw new AssertionError("recovery entry contract: " + item);
                        }
                        Integer count = counts.get(productId);
                        counts.put(productId, count == null ? 1 : count + 1);
                        if (productId == 9101) {
                            whatsappNames.add(item.getString("name"));
                        }
                    }
                    int[] expectedCounts = {25, 20, 20, 18, 18, 22, 18, 14};
                    for (int i = 0; i < expectedCounts.length; i++) {
                        int productId = 9101 + i;
                        if (!Integer.valueOf(expectedCounts[i]).equals(counts.get(productId))) {
                            throw new AssertionError(
                                    "menu count for " + productId + ": " + counts.get(productId));
                        }
                    }
                    String[] expectedWhatsapp = {
                        "一句话", "智能体模型", "AI龙虾", "超级号", "AI采集", "AI数据",
                        "AI筛选", "AI群发", "API", "广告", "AI客服"
                    };
                    for (String name : expectedWhatsapp) {
                        if (!whatsappNames.contains(name)) {
                            throw new AssertionError("missing WhatsApp menu: " + name);
                        }
                    }
                    JSONArray products =
                            new JSONObject(M4RecoveryCatalog.productModulesJson())
                                    .getJSONArray("data");
                    for (int i = 0; i < 8; i++) {
                        if (products.getJSONObject(i).getJSONArray("children").length() == 0) {
                            throw new AssertionError("missing product children: " + products.getJSONObject(i));
                        }
                    }
                    if (products.getJSONObject(8).getJSONArray("children").length() != 0) {
                        throw new AssertionError("aishope must not have enterable menus");
                    }
                    System.out.println("M4_RECOVERY_MENUS_OK");
                }
            }
            """,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("M4_RECOVERY_MENUS_OK", probe.stdout)

    def test_recovery_catalog_routes_whatsapp_agent_model_to_smart_ai_component(self):
        probe = self.compile_and_run_catalog_probe(
            "M8RecoveryWhatsAppAgentModelRouteProbe",
            """
            import org.json.JSONArray;
            import org.json.JSONObject;

            public class M8RecoveryWhatsAppAgentModelRouteProbe {
                public static void main(String[] args) {
                    JSONArray entries =
                            new JSONObject(M4RecoveryCatalog.pcMenusJson()).getJSONArray("scfs");
                    JSONObject target = null;
                    JSONObject child = null;
                    for (int i = 0; i < entries.length(); i++) {
                        JSONObject item = entries.getJSONObject(i);
                        if ("REC_WHATSAPP_AGENT_MODEL".equals(item.optString("code"))) {
                            target = item;
                        }
                        if ("REC_WHATSAPP_AGENT_MODEL_ROUTE".equals(item.optString("code"))) {
                            child = item;
                        }
                    }
                    if (target == null) {
                        throw new AssertionError("missing WhatsApp agent model menu");
                    }
                    if (child == null) {
                        throw new AssertionError("missing WhatsApp agent model route child");
                    }
                    String expectedLink = "/aiAgent/smartAi";
                    if (!"JSinglepage".equals(target.optString("localCode"))
                            || !expectedLink.equals(target.optString("linkUrl"))
                            || !"智能体模型".equals(target.optString("name"))
                            || !"REC_WHATSAPP_AGENT_MODEL".equals(target.optString("code"))
                            || target.optString("code").startsWith("C4749_")
                            || !target.optString("evidence").contains("recovery-route:smart-ai")) {
                        throw new AssertionError("wrong WhatsApp agent model route: " + target);
                    }
                    if (!expectedLink.equals(child.optString("localCode"))
                            || !"JSinglepage:/aiAgent/smartAi".equals(child.optString("linkUrl"))
                            || child.optInt("parentId") != target.optInt("id")
                            || child.optInt("productId") != 9101
                            || !"智能体模型".equals(child.optString("name"))
                            || !child.optString("evidence").contains("recovery-route-child:j2026-h-field-map:smart-ai")) {
                        throw new AssertionError("wrong WhatsApp agent model route child: " + child);
                    }
                    System.out.println("M8_WHATSAPP_AGENT_MODEL_ROUTE_OK");
                }
            }
            """,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("M8_WHATSAPP_AGENT_MODEL_ROUTE_OK", probe.stdout)

    def test_recovery_catalog_marks_whatsapp_ai_collect_as_data_collect_recovery_route(self):
        probe = self.compile_and_run_catalog_probe(
            "M4RecoveryWhatsAppCollectRouteProbe",
            """
            import org.json.JSONArray;
            import org.json.JSONObject;

            public class M4RecoveryWhatsAppCollectRouteProbe {
                public static void main(String[] args) {
                    JSONArray entries =
                            new JSONObject(M4RecoveryCatalog.pcMenusJson()).getJSONArray("scfs");
                    JSONObject target = null;
                    int routeChildCount = 0;
                    for (int i = 0; i < entries.length(); i++) {
                        JSONObject item = entries.getJSONObject(i);
                        if ("C4749_006".equals(item.optString("code"))) {
                            target = item;
                        } else if (item.optString("code").startsWith("REC_WHATSAPP_COLLECT_TAB_")) {
                            routeChildCount++;
                        }
                    }
                    if (target == null) {
                        throw new AssertionError("missing WhatsApp AI collect menu");
                    }
                    if (target.optInt("productId") != 9101
                            || !"AI采集".equals(target.optString("name"))) {
                        throw new AssertionError("wrong WhatsApp collect menu: " + target);
                    }
                    if (!"JSinglepage".equals(target.optString("localCode"))) {
                        throw new AssertionError("missing JSinglepage dataCollect opener recovery value: " + target);
                    }
                    String expectedLink =
                            "/pc/dataCollect/collectionTask?modal=whatsapp_users_lists&moduleCode=whatsapp";
                    if (!expectedLink.equals(target.optString("linkUrl"))) {
                        throw new AssertionError("missing WhatsApp spider route: " + target);
                    }
                    if (!target.optString("evidence").contains("recovery-route")) {
                        throw new AssertionError("route must be marked as recovered evidence: " + target);
                    }
                    if (routeChildCount != 4) {
                        throw new AssertionError("missing WhatsApp collect tab children: " + routeChildCount);
                    }
                    System.out.println("M4_WHATSAPP_COLLECT_ROUTE_OK");
                }
            }
            """,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("M4_WHATSAPP_COLLECT_ROUTE_OK", probe.stdout)

    def test_recovery_catalog_routes_facebook_modules_to_explicit_local_pages(self):
        probe = self.compile_and_run_catalog_probe(
            "D3FacebookLocalRoutesProbe",
            """
            import org.json.JSONArray;
            import org.json.JSONObject;

            public class D3FacebookLocalRoutesProbe {
                public static void main(String[] args) {
                    JSONArray entries =
                            new JSONObject(M4RecoveryCatalog.pcMenusJson()).getJSONArray("scfs");
                    int parentCount = 0;
                    int childCount = 0;
                    for (int i = 0; i < entries.length(); i++) {
                        JSONObject item = entries.getJSONObject(i);
                        if (item.optString("evidence").startsWith("d3-fb-local:")) {
                            parentCount++;
                            if (item.optInt("productId") != 9103
                                    || !"JSinglepage".equals(item.optString("localCode"))
                                    || !item.optString("linkUrl").startsWith("/pc/local/fb/")
                                    || item.optString("linkUrl").contains("http")) {
                                throw new AssertionError("wrong Facebook D-3 parent route: " + item);
                            }
                        } else if (item.optString("evidence").startsWith("d3-fb-local-child:")) {
                            childCount++;
                            if (item.optInt("productId") != 9103
                                    || !item.optString("localCode").startsWith("/pc/local/fb/")
                                    || !("JSinglepage:" + item.optString("localCode")).equals(item.optString("linkUrl"))
                                    || item.optInt("treeEndFlg") != 1) {
                                throw new AssertionError("wrong Facebook D-3 child route: " + item);
                            }
                        }
                    }
                    if (parentCount != 10 || childCount != 10) {
                        throw new AssertionError("missing Facebook D-3 local routes: " + parentCount + "/" + childCount);
                    }
                    System.out.println("D3_FACEBOOK_LOCAL_ROUTES_OK");
                }
            }
            """,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("D3_FACEBOOK_LOCAL_ROUTES_OK", probe.stdout)

    def test_recovery_catalog_routes_instagram_modules_to_explicit_local_pages(self):
        probe = self.compile_and_run_catalog_probe(
            "D2InstagramLocalRoutesProbe",
            """
            import org.json.JSONArray;
            import org.json.JSONObject;

            public class D2InstagramLocalRoutesProbe {
                public static void main(String[] args) {
                    JSONArray entries =
                            new JSONObject(M4RecoveryCatalog.pcMenusJson()).getJSONArray("scfs");
                    int insLocalCount = 0;
                    for (int i = 0; i < entries.length(); i++) {
                        JSONObject item = entries.getJSONObject(i);
                        if (item.optString("code").startsWith("C4131_")) {
                            insLocalCount++;
                            if (item.optInt("productId") != 9104
                                    || !"JSinglepage".equals(item.optString("localCode"))
                                    || !item.optString("linkUrl").startsWith("/pc/local/ins/")
                                    || !item.optString("evidence").startsWith("d2-ins-local:")) {
                                throw new AssertionError("Instagram module must use a D-2 local leaf: " + item);
                            }
                        }
                    }
                    if (insLocalCount != 9) {
                        throw new AssertionError("expected nine Instagram local leaves: " + insLocalCount);
                    }
                    System.out.println("C2A_INSTAGRAM_LOCAL_ROUTES_OK");
                }
            }
            """,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("C2A_INSTAGRAM_LOCAL_ROUTES_OK", probe.stdout)

    def test_recovery_catalog_routes_x_modules_to_explicit_local_pages(self):
        probe = self.compile_and_run_catalog_probe(
            "C3ATwitterCollectRouteProbe",
            """
            import org.json.JSONArray;
            import org.json.JSONObject;

            public class C3ATwitterCollectRouteProbe {
                public static void main(String[] args) {
                    JSONArray entries =
                            new JSONObject(M4RecoveryCatalog.pcMenusJson()).getJSONArray("scfs");
                    JSONObject preciseSearch = null;
                    for (int i = 0; i < entries.length(); i++) {
                        JSONObject item = entries.getJSONObject(i);
                        if ("C4133_003".equals(item.optString("code"))) {
                            preciseSearch = item;
                        }
                        if (item.optString("code").startsWith("C4133_")) {
                            if (!"JSinglepage".equals(item.optString("localCode"))
                                    || !item.optString("linkUrl").startsWith("/pc/local/x/")
                                    || !item.optString("evidence").startsWith("d1-x-local:")) {
                                throw new AssertionError("X module must use a D-1 local leaf: " + item);
                            }
                        }
                    }
                    if (preciseSearch == null) {
                        throw new AssertionError("missing X precise search menu");
                    }
                    String expectedLink = "/pc/local/x/precise-search";
                    if (preciseSearch.optInt("productId") != 9105
                            || !"X 精准搜索".equals(preciseSearch.optString("name"))
                            || !"JSinglepage".equals(preciseSearch.optString("localCode"))
                            || !expectedLink.equals(preciseSearch.optString("linkUrl"))
                            || preciseSearch.optString("linkUrl").contains("http")
                            || !preciseSearch.optString("evidence").contains("d1-x-local:precise-search")) {
                        throw new AssertionError("wrong D-1 X precise search route: " + preciseSearch);
                    }
                    System.out.println("C3A_TWITTER_COLLECT_ROUTE_OK");
                }
            }
            """,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("C3A_TWITTER_COLLECT_ROUTE_OK", probe.stdout)

    def test_recovery_catalog_adds_whatsapp_collect_top_tab_children(self):
        probe = self.compile_and_run_catalog_probe(
            "M5D11WhatsAppCollectTabsProbe",
            """
            import java.util.LinkedHashMap;
            import java.util.Map;
            import org.json.JSONArray;
            import org.json.JSONObject;

            public class M5D11WhatsAppCollectTabsProbe {
                public static void main(String[] args) {
                    JSONArray entries =
                            new JSONObject(M4RecoveryCatalog.pcMenusJson()).getJSONArray("scfs");
                    JSONObject parent = null;
                    Map<String, JSONObject> children = new LinkedHashMap<String, JSONObject>();
                    for (int i = 0; i < entries.length(); i++) {
                        JSONObject item = entries.getJSONObject(i);
                        if ("C4749_006".equals(item.optString("code"))) {
                            parent = item;
                        }
                    }
                    if (parent == null) {
                        throw new AssertionError("missing WhatsApp AI collect parent");
                    }
                    for (int i = 0; i < entries.length(); i++) {
                        JSONObject item = entries.getJSONObject(i);
                        if (item.optInt("parentId") == parent.optInt("id")
                                && item.optString("code").startsWith("REC_WHATSAPP_COLLECT_TAB_")) {
                            children.put(item.optString("name"), item);
                        }
                    }
                    String[][] expected = {
                        {"全球号码采集", "REC_WHATSAPP_COLLECT_TAB_GLOBAL_NUMBER", "whatsapp_users_lists"},
                        {"WS号码采集", "REC_WHATSAPP_COLLECT_TAB_WS_NUMBER", "wap_global_clue_users"},
                        {"WS小组采集", "REC_WHATSAPP_COLLECT_TAB_WS_GROUP", "whatsapp_group_lists"},
                        {"WS地区采集", "REC_WHATSAPP_COLLECT_TAB_WS_REGION", "whatsapp_regional_collection"}
                    };
                    if (children.size() != expected.length) {
                        throw new AssertionError("wrong collect tab child count: " + children);
                    }
                    for (int i = 0; i < expected.length; i++) {
                        JSONObject child = children.get(expected[i][0]);
                        if (child == null) {
                            throw new AssertionError("missing collect tab child: " + expected[i][0]);
                        }
                        String expectedPath =
                                "/pc/dataCollect/collectionTask?modal="
                                        + expected[i][2]
                                        + "&moduleCode=whatsapp";
                        if (!expected[i][1].equals(child.optString("code"))
                                || child.optInt("parentId") != parent.optInt("id")
                                || child.optInt("productId") != 9101
                                || child.optInt("displayIndex") != i + 1
                                || child.optInt("sort") != i + 1
                                || child.optInt("treeEndFlg") != 1
                                || child.optInt("webFlg") != 1
                                || !expectedPath.equals(child.optString("localCode"))
                                || !"JSinglepage".equals(child.optString("linkUrl"))
                                || !child.optString("evidence").contains("m5d11-menu-tab:dataCollect:" + expected[i][2])) {
                            throw new AssertionError("wrong collect tab child: " + child);
                        }
                    }
                    System.out.println("M5D11_WHATSAPP_COLLECT_TABS_OK");
                }
            }
            """,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("M5D11_WHATSAPP_COLLECT_TABS_OK", probe.stdout)

    def test_recovery_catalog_adds_whatsapp_claw_three_tab_children(self):
        probe = self.compile_and_run_catalog_probe(
            "M8BRecoveryWhatsAppClawTabsProbe",
            """
            import java.util.LinkedHashMap;
            import java.util.Map;
            import org.json.JSONArray;
            import org.json.JSONObject;

            public class M8BRecoveryWhatsAppClawTabsProbe {
                public static void main(String[] args) {
                    JSONArray entries =
                            new JSONObject(M4RecoveryCatalog.pcMenusJson()).getJSONArray("scfs");
                    JSONObject parent = null;
                    Map<String, JSONObject> children = new LinkedHashMap<String, JSONObject>();
                    for (int i = 0; i < entries.length(); i++) {
                        JSONObject item = entries.getJSONObject(i);
                        if ("REC_WHATSAPP_CLAW".equals(item.optString("code"))) {
                            parent = item;
                        }
                    }
                    if (parent == null) {
                        throw new AssertionError("missing WhatsApp claw parent");
                    }
                    if (!"JSinglepage".equals(parent.optString("localCode"))
                            || !"/wsClaw/browser".equals(parent.optString("linkUrl"))
                            || !"AI龙虾".equals(parent.optString("name"))
                            || !parent.optString("evidence").contains("recovery-route:ws-claw")) {
                        throw new AssertionError("wrong WhatsApp claw parent: " + parent);
                    }
                    for (int i = 0; i < entries.length(); i++) {
                        JSONObject item = entries.getJSONObject(i);
                        if (item.optInt("parentId") == parent.optInt("id")
                                && item.optString("code").startsWith("REC_WHATSAPP_CLAW_TAB_")) {
                            children.put(item.optString("name"), item);
                        }
                        if ("REC_WHATSAPP_CLAW_LICENSE".equals(item.optString("code"))
                                || "/pc/longxialicense/manager".equals(item.optString("localCode"))
                                || "/pc/longxialicense/manager".equals(item.optString("linkUrl"))) {
                            throw new AssertionError("license manager must not be a menu or tab: " + item);
                        }
                    }
                    String[][] expected = {
                        {"指纹浏览器", "REC_WHATSAPP_CLAW_TAB_BROWSER", "/wsClaw/browser"},
                        {"虚拟账号", "REC_WHATSAPP_CLAW_TAB_ACCOUNT", "/wsClaw/account"},
                        {"ADS服务器", "REC_WHATSAPP_CLAW_TAB_SERVER", "/wsClaw/server"}
                    };
                    if (children.size() != expected.length) {
                        throw new AssertionError("wrong claw tab child count: " + children);
                    }
                    for (int i = 0; i < expected.length; i++) {
                        JSONObject child = children.get(expected[i][0]);
                        if (child == null
                                || !expected[i][1].equals(child.optString("code"))
                                || !expected[i][2].equals(child.optString("localCode"))
                                || !"JSinglepage".equals(child.optString("linkUrl"))
                                || child.optInt("displayIndex") != i + 1
                                || child.optInt("sort") != i + 1
                                || child.optInt("treeEndFlg") != 1
                                || child.optInt("webFlg") != 1
                                || !child.optString("evidence").contains("m8-6-b-menu-tab:wsClaw:")) {
                            throw new AssertionError("wrong claw tab child: " + child);
                        }
                    }
                    System.out.println("M8B_WHATSAPP_CLAW_TABS_OK");
                }
            }
            """,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("M8B_WHATSAPP_CLAW_TABS_OK", probe.stdout)

    def test_recovery_catalog_adds_whatsapp_super_environment_child_route(self):
        probe = self.compile_and_run_catalog_probe(
            "M8BRecoveryWhatsAppSuperEnvironmentProbe",
            """
            import org.json.JSONArray;
            import org.json.JSONObject;

            public class M8BRecoveryWhatsAppSuperEnvironmentProbe {
                public static void main(String[] args) {
                    JSONArray entries =
                            new JSONObject(M4RecoveryCatalog.pcMenusJson()).getJSONArray("scfs");
                    JSONObject parent = null;
                    JSONObject child = null;
                    for (int i = 0; i < entries.length(); i++) {
                        JSONObject item = entries.getJSONObject(i);
                        if ("REC_WHATSAPP_SUPER".equals(item.optString("code"))) {
                            parent = item;
                        } else if ("REC_WHATSAPP_SUPER_ENV_ROUTE".equals(item.optString("code"))) {
                            child = item;
                        }
                    }
                    if (parent == null || child == null) {
                        throw new AssertionError(
                                "missing WhatsApp super environment route: parent="
                                        + parent + " child=" + child);
                    }
                    String route = "/pc/sender/senderGlobalControls/mysuperenvironment";
                    if (!"JSinglepage".equals(parent.optString("localCode"))
                            || !route.equals(parent.optString("linkUrl"))
                            || !parent.optString("evidence")
                                    .contains("recovery-route:super-environment")) {
                        throw new AssertionError("wrong WhatsApp super parent: " + parent);
                    }
                    if (child.optInt("parentId") != parent.optInt("id")
                            || !route.equals(child.optString("localCode"))
                            || !"JSinglepage".equals(child.optString("linkUrl"))
                            || child.optInt("displayIndex") != 1
                            || child.optInt("sort") != 1
                            || child.optInt("treeEndFlg") != 1
                            || child.optInt("webFlg") != 1
                            || !child.optString("evidence")
                                    .contains("m8-8-b-menu-tab:senderGlobalControls:mysuperenvironment")) {
                        throw new AssertionError("wrong WhatsApp super environment child: " + child);
                    }
                    System.out.println("M8B_WHATSAPP_SUPER_ENV_ROUTE_OK");
                }
            }
            """,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("M8B_WHATSAPP_SUPER_ENV_ROUTE_OK", probe.stdout)

    def test_recovery_catalog_adds_c5_platform_route_children(self):
        probe = self.compile_and_run_catalog_probe(
            "C5RecoveryPlatformRouteChildrenProbe",
            """
            import java.util.LinkedHashMap;
            import java.util.Map;
            import org.json.JSONArray;
            import org.json.JSONObject;

            public class C5RecoveryPlatformRouteChildrenProbe {
                public static void main(String[] args) {
                    JSONArray entries =
                            new JSONObject(M4RecoveryCatalog.pcMenusJson()).getJSONArray("scfs");
                    Map<String, JSONObject> byCode = new LinkedHashMap<String, JSONObject>();
                    for (int i = 0; i < entries.length(); i++) {
                        JSONObject item = entries.getJSONObject(i);
                        byCode.put(item.optString("code"), item);
                    }
                    String[][] expected = {
                        {"C3460_001", "REC_WHATSAPP_ADVERTISING_ROUTE", "/views/overseasAds/dataBoard"}
                    };
                    for (int i = 0; i < expected.length; i++) {
                        JSONObject parent = byCode.get(expected[i][0]);
                        JSONObject child = byCode.get(expected[i][1]);
                        if (parent == null || child == null) {
                            throw new AssertionError(
                                    "missing C5 route pair " + expected[i][0] + "/" + expected[i][1]);
                        }
                        if (!"JSinglepage".equals(parent.optString("localCode"))
                                || !expected[i][2].equals(parent.optString("linkUrl"))) {
                            throw new AssertionError("bad C5 parent route: " + parent);
                        }
                        if (child.optInt("parentId") != parent.optInt("id")
                                || !expected[i][2].equals(child.optString("localCode"))
                                || !"JSinglepage".equals(child.optString("linkUrl"))
                                || child.optInt("treeEndFlg") != 1
                                || child.optInt("webFlg") != 1
                                || !child.optString("evidence").contains("c5-platform-route-child:")) {
                            throw new AssertionError("bad C5 child route: " + child);
                        }
                    }
                    System.out.println("C5_PLATFORM_ROUTE_CHILDREN_OK");
                }
            }
            """,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("C5_PLATFORM_ROUTE_CHILDREN_OK", probe.stdout)

    def test_recovery_catalog_keeps_whatsapp_ai_data_on_original_aicloud_route(self):
        probe = self.compile_and_run_catalog_probe(
            "M4RecoveryWhatsAppAiDataRouteProbe",
            """
            import org.json.JSONArray;
            import org.json.JSONObject;

            public class M4RecoveryWhatsAppAiDataRouteProbe {
                public static void main(String[] args) {
                    JSONArray entries =
                            new JSONObject(M4RecoveryCatalog.pcMenusJson()).getJSONArray("scfs");
                    JSONObject target = null;
                    JSONObject routeChild = null;
                    for (int i = 0; i < entries.length(); i++) {
                        JSONObject item = entries.getJSONObject(i);
                        if ("C4749_007".equals(item.optString("code"))) {
                            target = item;
                        } else if ("REC_WHATSAPP_AI_DATA_ROUTE".equals(item.optString("code"))) {
                            routeChild = item;
                        }
                    }
                    if (target == null) {
                        throw new AssertionError("missing WhatsApp AI data menu");
                    }
                    if (target.optInt("productId") != 9101
                            || !"AI数据".equals(target.optString("name"))) {
                        throw new AssertionError("wrong WhatsApp AI data menu: " + target);
                    }
                    if (!"JSinglepage".equals(target.optString("localCode"))) {
                        throw new AssertionError("missing JSinglepage opener recovery value: " + target);
                    }
                    String expectedLink = "/pc/aicloud/my";
                    if (!expectedLink.equals(target.optString("linkUrl"))) {
                        throw new AssertionError("AI data must use original AiCloud route: " + target);
                    }
                    if (!"original-i18n".equals(target.optString("evidence"))) {
                        throw new AssertionError("parent route must remain original-i18n evidence: " + target);
                    }
                    if (routeChild == null) {
                        throw new AssertionError("missing WhatsApp AI data child route");
                    }
                    if (routeChild.optInt("parentId") != target.optInt("id")
                            || routeChild.optInt("productId") != 9101
                            || !"AI数据".equals(routeChild.optString("name"))
                            || !expectedLink.equals(routeChild.optString("localCode"))
                            || !"JSinglepage:/pc/aicloud/my".equals(routeChild.optString("linkUrl"))
                            || !routeChild.optString("evidence").contains("recovery-route-child:j2026-h-field-map:aicloud-my")) {
                        throw new AssertionError("wrong WhatsApp AI data child route: " + routeChild);
                    }
                    System.out.println("M4_WHATSAPP_AI_DATA_AICLOUD_ROUTE_OK");
                }
            }
            """,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("M4_WHATSAPP_AI_DATA_AICLOUD_ROUTE_OK", probe.stdout)

    def test_recovery_catalog_routes_whatsapp_ai_filter_to_original_web_component(self):
        probe = self.compile_and_run_catalog_probe(
            "M5CRecoveryWhatsAppAiFilterRouteProbe",
            """
            import org.json.JSONArray;
            import org.json.JSONObject;

            public class M5CRecoveryWhatsAppAiFilterRouteProbe {
                public static void main(String[] args) {
                    JSONArray entries =
                            new JSONObject(M4RecoveryCatalog.pcMenusJson()).getJSONArray("scfs");
                    JSONObject target = null;
                    JSONObject routeChild = null;
                    for (int i = 0; i < entries.length(); i++) {
                        JSONObject item = entries.getJSONObject(i);
                        if ("C4749_009".equals(item.optString("code"))) {
                            target = item;
                        } else if ("REC_WHATSAPP_AI_FILTER_ROUTE".equals(item.optString("code"))) {
                            routeChild = item;
                        }
                    }
                    if (target == null || routeChild == null) {
                        throw new AssertionError("missing WhatsApp AI filter route pair");
                    }
                    String expectedLink = "/ws/wsfilter/home";
                    if (!"JSinglepage".equals(target.optString("localCode"))
                            || !expectedLink.equals(target.optString("linkUrl"))
                            || !target.optString("evidence").contains("recovery-route:wsfilter-home")) {
                        throw new AssertionError("wrong WhatsApp AI filter parent: " + target);
                    }
                    if (routeChild.optInt("parentId") != target.optInt("id")
                            || routeChild.optInt("productId") != 9101
                            || !expectedLink.equals(routeChild.optString("localCode"))
                            || !"JSinglepage:/ws/wsfilter/home".equals(routeChild.optString("linkUrl"))
                            || !routeChild.optString("evidence").contains("recovery-route-child:j2026-h-field-map:wsfilter-home")) {
                        throw new AssertionError("wrong WhatsApp AI filter child: " + routeChild);
                    }
                    System.out.println("M5C_WHATSAPP_AI_FILTER_ROUTE_OK");
                }
            }
            """,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("M5C_WHATSAPP_AI_FILTER_ROUTE_OK", probe.stdout)

    def test_recovery_catalog_routes_whatsapp_ai_kefu_to_aggregation_component(self):
        probe = self.compile_and_run_catalog_probe(
            "M8RecoveryWhatsAppAiKefuRouteProbe",
            """
            import org.json.JSONArray;
            import org.json.JSONObject;

            public class M8RecoveryWhatsAppAiKefuRouteProbe {
                public static void main(String[] args) {
                    JSONArray entries =
                            new JSONObject(M4RecoveryCatalog.pcMenusJson()).getJSONArray("scfs");
                    JSONObject target = null;
                    JSONObject child = null;
                    for (int i = 0; i < entries.length(); i++) {
                        JSONObject item = entries.getJSONObject(i);
                        if ("C4749_011".equals(item.optString("code"))) {
                            target = item;
                        }
                        if ("REC_WHATSAPP_AI_KEFU_ROUTE".equals(item.optString("code"))) {
                            child = item;
                        }
                    }
                    if (target == null) {
                        throw new AssertionError("missing WhatsApp AI kefu menu");
                    }
                    if (child == null) {
                        throw new AssertionError("missing WhatsApp AI kefu route child");
                    }
                    String expectedLink = "https://web.whatsapp.com";
                    if (!expectedLink.equals(target.optString("localCode"))
                            || !expectedLink.equals(target.optString("linkUrl"))
                            || !target.optString("evidence").contains("recovery-route:aggregation-kefu")) {
                        throw new AssertionError("wrong WhatsApp AI kefu route: " + target);
                    }
                    if (!expectedLink.equals(child.optString("localCode"))
                            || !expectedLink.equals(child.optString("linkUrl"))
                            || child.optInt("parentId") != target.optInt("id")
                            || !child.optString("evidence").contains("recovery-route-child:j2026-h-field-map:whatsapp-web")) {
                        throw new AssertionError("wrong WhatsApp AI kefu route child: " + child);
                    }
                    System.out.println("M8_WHATSAPP_AI_KEFU_ROUTE_OK");
                }
            }
            """,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("M8_WHATSAPP_AI_KEFU_ROUTE_OK", probe.stdout)

    def test_local_web_asset_bridge_prefers_full_mirror_for_ai_kefu_chunks(self):
        self.compile_patcher()
        probe_source = self.tmp_path / "M8FullMirrorAssetProbe.java"
        probe_source.write_text(
            textwrap.dedent(
                """
                import com.sbf.main.jxbrowser.M5LocalSpiderBridge;

                public class M8FullMirrorAssetProbe {
                    public static void main(String[] args) throws Exception {
                        String body = M5LocalSpiderBridge.localWebAssetBody(
                                "https://app.xdxsoft.com/static/js/chunk-49bd57a4.df38da93.js");
                        String contentType = M5LocalSpiderBridge.localWebAssetContentType(
                                "https://app.xdxsoft.com/static/js/chunk-49bd57a4.df38da93.js");
                        if (!contentType.contains("application/javascript")) {
                            throw new AssertionError("wrong content type: " + contentType);
                        }
                        if (!body.contains("aggregationKefu")
                                && !body.contains("/kefu/conversation/getUnread")
                                && !body.contains("/upmee/api/getConversationList")) {
                            throw new AssertionError("full mirror did not serve AI kefu chunk");
                        }
                        String css = M5LocalSpiderBridge.localWebAssetBody(
                                "https://app.xdxsoft.com/static/css/chunk-49bd57a4.5c79b182.css");
                        if (css.length() == 0) {
                            throw new AssertionError("full mirror did not serve AI kefu css");
                        }
                        System.out.println("M8_FULL_MIRROR_AI_KEFU_ASSETS_OK");
                    }
                }
                """
            ).strip(),
            encoding="utf-8",
        )
        subprocess.run(
            [
                str(JAVAC),
                "-encoding",
                "UTF-8",
                "-cp",
                classpath(self.classes, JSON_JAR, DATA_LIBS),
                "-d",
                str(self.probe_classes),
                str(probe_source),
            ],
            cwd=ROOT,
            check=True,
        )
        probe = subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.probe_classes, self.classes, JSON_JAR, DATA_LIBS),
                "M8FullMirrorAssetProbe",
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("M8_FULL_MIRROR_AI_KEFU_ASSETS_OK", probe.stdout)

    def test_whatsapp_login_account_bridge_persists_profile_mapping(self):
        self.compile_patcher()
        runtime = self.tmp_path / "runtime"
        runtime.mkdir()
        probe_source = self.tmp_path / "M8B1AWhatsAppAccountBridgeProbe.java"
        probe_source.write_text(
            textwrap.dedent(
                """
                import com.sbf.main.jxbrowser.M5LocalSpiderBridge;
                import java.nio.file.Files;
                import java.nio.file.Path;
                import java.nio.file.Paths;
                import org.json.JSONArray;
                import org.json.JSONObject;

                public class M8B1AWhatsAppAccountBridgeProbe {
                    public static void main(String[] args) throws Exception {
                        String baseDir = args[0];
                        String saved = M5LocalSpiderBridge.upsertWhatsAppAccount(
                                baseDir,
                                "wa-default",
                                "+15551234567",
                                "logged_in",
                                "{\\"source\\":\\"probe\\",\\"loggedIn\\":true}");
                        JSONObject savedJson = new JSONObject(saved);
                        if (savedJson.optInt("code") != 200) {
                            throw new AssertionError("save failed: " + saved);
                        }
                        JSONObject data = savedJson.getJSONObject("data");
                        if (!"wa-default".equals(data.optString("profileId"))
                                || !"+15551234567".equals(data.optString("phone"))
                                || !"logged_in".equals(data.optString("status"))) {
                            throw new AssertionError("wrong saved shape: " + data);
                        }
                        Path profilePath = Paths.get(data.getString("profilePath"));
                        if (!Files.isDirectory(profilePath)) {
                            throw new AssertionError("profile path not created: " + profilePath);
                        }
                        JSONObject listed = new JSONObject(M5LocalSpiderBridge.listWhatsAppAccounts(baseDir));
                        JSONArray rows = listed.getJSONArray("rows");
                        if (rows.length() != 1
                                || !"wa-default".equals(rows.getJSONObject(0).optString("profileId"))) {
                            throw new AssertionError("account not listed: " + listed);
                        }
                        System.out.println("M8B1A_WHATSAPP_ACCOUNT_BRIDGE_OK");
                    }
                }
                """
            ).strip(),
            encoding="utf-8",
        )
        subprocess.run(
            [
                str(JAVAC),
                "-encoding",
                "UTF-8",
                "-cp",
                classpath(self.classes, JSON_JAR, DATA_LIBS),
                "-d",
                str(self.probe_classes),
                str(probe_source),
            ],
            cwd=ROOT,
            check=True,
        )
        probe = subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.probe_classes, self.classes, JSON_JAR, DATA_LIBS),
                "M8B1AWhatsAppAccountBridgeProbe",
                str(runtime),
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("M8B1A_WHATSAPP_ACCOUNT_BRIDGE_OK", probe.stdout)

    def test_whatsapp_message_bridge_persists_conversations_and_messages(self):
        self.compile_patcher()
        runtime = self.tmp_path / "runtime"
        runtime.mkdir()
        probe_source = self.tmp_path / "M8B1BWhatsAppMessageBridgeProbe.java"
        probe_source.write_text(
            textwrap.dedent(
                """
                import com.sbf.main.jxbrowser.M5LocalSpiderBridge;
                import java.nio.file.Files;
                import java.nio.file.Path;
                import java.nio.file.Paths;
                import org.json.JSONArray;
                import org.json.JSONObject;

                public class M8B1BWhatsAppMessageBridgeProbe {
                    public static void main(String[] args) throws Exception {
                        String baseDir = args[0];
                        String saved = M5LocalSpiderBridge.upsertWhatsAppMessage(
                                baseDir,
                                "wa-default",
                                "chat-15559876543",
                                "+15559876543",
                                "Alice Local",
                                "inbound",
                                "+15559876543",
                                "hello from real whatsapp",
                                1720000000000L,
                                "wa-msg-1",
                                "{\\"source\\":\\"probe\\"}");
                        JSONObject savedJson = new JSONObject(saved);
                        if (savedJson.optInt("code") != 200) {
                            throw new AssertionError("save failed: " + saved);
                        }
                        JSONObject data = savedJson.getJSONObject("data");
                        if (!"chat-15559876543".equals(data.optString("conversationKey"))
                                || !"wa-msg-1".equals(data.optString("messageId"))) {
                            throw new AssertionError("wrong saved shape: " + data);
                        }
                        Path dbPath = Paths.get(baseDir, "data", "db_b_whatsapp_messages.data");
                        if (!Files.isRegularFile(dbPath)) {
                            throw new AssertionError("message db missing: " + dbPath);
                        }
                        JSONObject conversations = new JSONObject(
                                M5LocalSpiderBridge.listWhatsAppConversations(baseDir, "wa-default"));
                        JSONArray conversationRows = conversations.getJSONArray("rows");
                        if (conversationRows.length() != 1
                                || !"Alice Local".equals(conversationRows.getJSONObject(0).optString("title"))
                                || !"hello from real whatsapp".equals(conversationRows.getJSONObject(0).optString("lastMessageText"))) {
                            throw new AssertionError("conversation not listed: " + conversations);
                        }
                        JSONObject messages = new JSONObject(
                                M5LocalSpiderBridge.listWhatsAppMessages(baseDir, "wa-default", "chat-15559876543"));
                        JSONArray messageRows = messages.getJSONArray("rows");
                        if (messageRows.length() != 1
                                || !"inbound".equals(messageRows.getJSONObject(0).optString("direction"))
                                || !"hello from real whatsapp".equals(messageRows.getJSONObject(0).optString("messageText"))) {
                            throw new AssertionError("message not listed: " + messages);
                        }
                        System.out.println("M8B1B_WHATSAPP_MESSAGE_BRIDGE_OK");
                    }
                }
                """
            ).strip(),
            encoding="utf-8",
        )
        subprocess.run(
            [
                str(JAVAC),
                "-encoding",
                "UTF-8",
                "-cp",
                classpath(self.classes, JSON_JAR, DATA_LIBS),
                "-d",
                str(self.probe_classes),
                str(probe_source),
            ],
            cwd=ROOT,
            check=True,
        )
        probe = subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.probe_classes, self.classes, JSON_JAR, DATA_LIBS),
                "M8B1BWhatsAppMessageBridgeProbe",
                str(runtime),
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("M8B1B_WHATSAPP_MESSAGE_BRIDGE_OK", probe.stdout)

    def test_whatsapp_multi_profile_bridge_isolates_accounts_and_messages(self):
        self.compile_patcher()
        runtime = self.tmp_path / "runtime"
        runtime.mkdir()
        probe_source = self.tmp_path / "M8B1CWhatsAppMultiProfileProbe.java"
        probe_source.write_text(
            textwrap.dedent(
                """
                import com.sbf.main.jxbrowser.M5LocalSpiderBridge;
                import org.json.JSONArray;
                import org.json.JSONObject;

                public class M8B1CWhatsAppMultiProfileProbe {
                    public static void main(String[] args) throws Exception {
                        String baseDir = args[0];
                        M5LocalSpiderBridge.upsertWhatsAppAccount(
                                baseDir, "wa-001", "+15550000001", "logged_in", "{\\"source\\":\\"probe-1\\"}");
                        M5LocalSpiderBridge.upsertWhatsAppAccount(
                                baseDir, "wa-002", "+15550000002", "logged_in", "{\\"source\\":\\"probe-2\\"}");
                        M5LocalSpiderBridge.upsertWhatsAppMessage(
                                baseDir, "wa-001", "same-contact", "+19990000000", "Shared Contact",
                                "inbound", "+19990000000", "message for account one",
                                1720000000001L, "same-msg", "{\\"source\\":\\"probe-1\\"}");
                        M5LocalSpiderBridge.upsertWhatsAppMessage(
                                baseDir, "wa-002", "same-contact", "+19990000000", "Shared Contact",
                                "inbound", "+19990000000", "message for account two",
                                1720000000002L, "same-msg", "{\\"source\\":\\"probe-2\\"}");

                        JSONObject active = new JSONObject(
                                M5LocalSpiderBridge.setActiveWhatsAppProfile(baseDir, "wa-002"));
                        if (!"wa-002".equals(active.getJSONObject("data").optString("profileId"))) {
                            throw new AssertionError("active profile not set: " + active);
                        }
                        JSONObject current = new JSONObject(M5LocalSpiderBridge.getActiveWhatsAppProfile(baseDir));
                        if (!"wa-002".equals(current.getJSONObject("data").optString("profileId"))) {
                            throw new AssertionError("active profile not read: " + current);
                        }
                        JSONObject accounts = new JSONObject(M5LocalSpiderBridge.listWhatsAppAccounts(baseDir));
                        JSONArray accountRows = accounts.getJSONArray("rows");
                        if (accountRows.length() != 2) {
                            throw new AssertionError("expected two accounts: " + accounts);
                        }

                        JSONArray one = new JSONObject(
                                M5LocalSpiderBridge.listWhatsAppMessages(baseDir, "wa-001", "same-contact"))
                                .getJSONArray("rows");
                        JSONArray two = new JSONObject(
                                M5LocalSpiderBridge.listWhatsAppMessages(baseDir, "wa-002", "same-contact"))
                                .getJSONArray("rows");
                        if (one.length() != 1
                                || two.length() != 1
                                || !"message for account one".equals(one.getJSONObject(0).optString("messageText"))
                                || !"message for account two".equals(two.getJSONObject(0).optString("messageText"))) {
                            throw new AssertionError("messages are not isolated: one=" + one + " two=" + two);
                        }
                        System.out.println("M8B1C_WHATSAPP_MULTI_PROFILE_BRIDGE_OK");
                    }
                }
                """
            ).strip(),
            encoding="utf-8",
        )
        subprocess.run(
            [
                str(JAVAC),
                "-encoding",
                "UTF-8",
                "-cp",
                classpath(self.classes, JSON_JAR, DATA_LIBS),
                "-d",
                str(self.probe_classes),
                str(probe_source),
            ],
            cwd=ROOT,
            check=True,
        )
        probe = subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.probe_classes, self.classes, JSON_JAR, DATA_LIBS),
                "M8B1CWhatsAppMultiProfileProbe",
                str(runtime),
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("M8B1C_WHATSAPP_MULTI_PROFILE_BRIDGE_OK", probe.stdout)

    def test_local_web_asset_bridge_serves_smart_ai_chunks_from_full_mirror(self):
        self.compile_patcher()
        probe_source = self.tmp_path / "M8SmartAiFullMirrorAssetProbe.java"
        probe_source.write_text(
            textwrap.dedent(
                """
                import com.sbf.main.jxbrowser.M5LocalSpiderBridge;

                public class M8SmartAiFullMirrorAssetProbe {
                    public static void main(String[] args) throws Exception {
                        String main = M5LocalSpiderBridge.localWebAssetBody(
                                "https://app.xdxsoft.com/static/js/chunk-567555bc.7b08c58f.js");
                        String shared = M5LocalSpiderBridge.localWebAssetBody(
                                "https://app.xdxsoft.com/static/js/chunk-76ba7557.5b4ba796.js");
                        String css = M5LocalSpiderBridge.localWebAssetBody(
                                "https://app.xdxsoft.com/static/css/chunk-567555bc.2ce5b02a.css");
                        if (main == null
                                || !main.contains("smartAi")
                                || !main.contains("我的智能体")
                                || !main.contains("mijava.dowloadFile")) {
                            throw new AssertionError("full mirror did not serve smartAi main chunk");
                        }
                        if (shared == null
                                || !shared.contains("/volcengine/market/my")
                                || !shared.contains("/volcengine/market/aiChat/")
                                || !shared.contains("/volcengine/trains/tokens")) {
                            throw new AssertionError("full mirror did not serve smartAi shared chunk");
                        }
                        if (css == null || !css.contains(".smartAi")) {
                            throw new AssertionError("full mirror did not serve smartAi css");
                        }
                        System.out.println("M8_SMART_AI_FULL_MIRROR_ASSETS_OK");
                    }
                }
                """
            ).strip(),
            encoding="utf-8",
        )
        subprocess.run(
            [
                str(JAVAC),
                "-encoding",
                "UTF-8",
                "-cp",
                classpath(self.classes, JSON_JAR, DATA_LIBS),
                "-d",
                str(self.probe_classes),
                str(probe_source),
            ],
            cwd=ROOT,
            check=True,
        )
        probe = subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.probe_classes, self.classes, JSON_JAR, DATA_LIBS),
                "M8SmartAiFullMirrorAssetProbe",
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("M8_SMART_AI_FULL_MIRROR_ASSETS_OK", probe.stdout)

    def test_m8d17_local_web_bridge_covers_route_asset_and_xhr_families(self):
        self.compile_patcher()
        probe_source = self.tmp_path / "M8D17OfflineFamilyProbe.java"
        probe_source.write_text(
            textwrap.dedent(
                """
                import com.sbf.main.jxbrowser.M5LocalSpiderBridge;
                import java.nio.charset.StandardCharsets;

                public class M8D17OfflineFamilyProbe {
                    private static byte[] requireBytes(String url) throws Exception {
                        byte[] body = M5LocalSpiderBridge.localWebAssetBytes(url);
                        if (body == null || body.length == 0) {
                            throw new AssertionError("missing local response: " + url);
                        }
                        return body;
                    }

                    private static String requireText(String url, String needle) throws Exception {
                        String body = new String(requireBytes(url), StandardCharsets.UTF_8);
                        if (!body.contains(needle)) {
                            throw new AssertionError("local response missing " + needle + ": " + url);
                        }
                        return body;
                    }

                    private static String forbidText(String url, String needle) throws Exception {
                        String body = new String(requireBytes(url), StandardCharsets.UTF_8);
                        if (body.contains(needle)) {
                            throw new AssertionError("local response contains forbidden " + needle + ": " + url);
                        }
                        return body;
                    }

                    public static void main(String[] args) throws Exception {
                        requireText("https://app.xdxsoft.com/aiAgent/smartAi?st=1", "<html");
                        requireText("https://app.xdxsoft.com/wsClaw/browser?st=1", "<html");
                        requireText("https://app.xdxsoft.com/wsClaw/account?st=1", "<html");
                        requireText("https://app.xdxsoft.com/wsClaw/server?st=1", "<html");
                        requireText(
                                "https://app.xdxsoft.com/es/bigData/bigDataTask?code=ins_blogger_data",
                                "<html");
                        requireText(
                                "https://app.xdxsoft.com/es/bigData/bigDataTask?code=big_data_twitter_new",
                                "<html");

                        requireText(
                                "https://app.xdxsoft.com/static/js/app.09d7ef80.js",
                                "./wsClaw/browser");
                        requireText(
                                "https://app.xdxsoft.com/static/css/app.0299bcba.css",
                                ".app-container");

                        requireText(
                                "https://app.xdxsoft.com/prod-api/system/google_sites/lists",
                                "\\\"code\\\":200");
                        requireText(
                                "https://app.xdxsoft.com/prod-api/system/pagebanner/getByCodeSoftware"
                                        + "?code=whatsapp_users_lists&software=whatsapp",
                                "\\\"code\\\":200");
                        requireText(
                                "https://app.xdxsoft.com/prod-api/es/bigData/code/ins_blogger_data",
                                "\\\"code\\\":200");
                        requireText(
                                "https://app.xdxsoft.com/prod-api/es/collectTask/my?code=ins_blogger_data",
                                "\\\"code\\\":200");
                        requireText(
                                "https://app.xdxsoft.com/prod-api/es/bigData/code/big_data_twitter_new",
                                "\\\"code\\\":200");
                        requireText(
                                "https://app.xdxsoft.com/prod-api/es/bigData/code/big_data_twitter_new",
                                "Keywords");
                        requireText(
                                "https://app.xdxsoft.com/prod-api/es/bigData/code/big_data_twitter_new",
                                "X URL");
                        requireText(
                                "https://app.xdxsoft.com/prod-api/es/collectTask/my?code=big_data_twitter_new",
                                "\\\"code\\\":200");
                        requireText(
                                "https://app.xdxsoft.com/prod-api/es/collectTask/my?code=big_data_twitter_new",
                                "\\\"rows\\\":[]");
                        requireText(
                                "https://app.xdxsoft.com/prod-api/es/bigDataConfig/userConfig",
                                "\\\"xdxChineseSwitch\\\":1");
                        requireText(
                                "https://app.xdxsoft.com/prod-api/es/bigDataConfig/userConfig",
                                "\\\"xdxEnSwitch\\\":1");
                        forbidText(
                                "https://app.xdxsoft.com/prod-api/es/bigDataConfig/userConfig",
                                "\\\"data\\\":[]");
                        forbidText(
                                "https://app.xdxsoft.com/static/js/chunk-2d0bd27f.b96f2a2b.js",
                                "aisrc.oss-cn-hangzhou.aliyuncs.com");
                        requireText(
                                "https://app.xdxsoft.com/static/js/chunk-2d0bd27f.b96f2a2b.js",
                                "data:image/gif;base64");
                        requireText(
                                "https://app.xdxsoft.com/static/js/chunk-46942aaa.78ddab17.js",
                                "\\\"big_data_twitter_new\\\"==this.funcModuleCode?this.tableData=[]");

                        requireBytes(
                                "https://app.xdxsoft.com/static/fonts/element-icons.535877f5.woff");
                        requireBytes(
                                "https://app.xdxsoft.com/static/img/s-notice.760b1115.png");
                        if (!M5LocalSpiderBridge.localWebAssetContentType(
                                        "https://app.xdxsoft.com/static/fonts/element-icons.535877f5.woff")
                                .contains("font/woff")) {
                            throw new AssertionError("wrong WOFF content type");
                        }
                        if (!M5LocalSpiderBridge.localWebAssetContentType(
                                        "https://app.xdxsoft.com/static/img/s-notice.760b1115.png")
                                .contains("image/png")) {
                            throw new AssertionError("wrong PNG content type");
                        }
                        System.out.println("M8D17_OFFLINE_FAMILIES_OK");
                    }
                }
                """
            ).strip(),
            encoding="utf-8",
        )
        subprocess.run(
            [
                str(JAVAC),
                "-encoding",
                "UTF-8",
                "-cp",
                classpath(self.classes, JSON_JAR, DATA_LIBS),
                "-d",
                str(self.probe_classes),
                str(probe_source),
            ],
            cwd=ROOT,
            check=True,
        )
        probe = subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.probe_classes, self.classes, JSON_JAR, DATA_LIBS),
                "M8D17OfflineFamilyProbe",
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("M8D17_OFFLINE_FAMILIES_OK", probe.stdout)

    def test_whatsapp_claw_assets_and_terminal_local_stubs_are_patched(self):
        self.compile_patcher()
        probe_source = self.tmp_path / "M8BWhatsAppClawAssetsProbe.java"
        probe_source.write_text(
            textwrap.dedent(
                """
                import com.sbf.main.jxbrowser.M5LocalSpiderBridge;

                public class M8BWhatsAppClawAssetsProbe {
                    private static void requireContains(String url, String... needles)
                            throws Exception {
                        String body = M5LocalSpiderBridge.localWebAssetBody(url);
                        if (body == null || body.length() == 0) {
                            throw new AssertionError("missing mirrored asset: " + url);
                        }
                        for (String needle : needles) {
                            if (!body.contains(needle)) {
                                throw new AssertionError(
                                        "asset " + url + " is missing " + needle);
                            }
                        }
                    }

                    public static void main(String[] args) throws Exception {
                        requireContains(
                                "https://app.xdxsoft.com/static/js/app.988d65c1.js",
                                "./wsClaw/browser",
                                "\\"4c23\\"",
                                "chunk-3353acce",
                                "./wsClaw/account",
                                "\\"3e2f\\"",
                                "chunk-2beceb10",
                                "./wsClaw/server",
                                "\\"f67c\\"",
                                "chunk-b2d575a6");
                        requireContains(
                                "https://app.xdxsoft.com/static/js/chunk-3353acce.95b8bf91.js",
                                "/wsClaw/browser/list",
                                "dataAllAccount",
                                "bindBatch");
                        requireContains(
                                "https://app.xdxsoft.com/static/js/chunk-2beceb10.77b6e5ea.js",
                                "/wsClaw/account/list",
                                "checkAccountExist");
                        requireContains(
                                "https://app.xdxsoft.com/static/js/chunk-b2d575a6.f6ef85a8.js",
                                "/wsClaw/server/list");
                        requireContains(
                                "https://app.xdxsoft.com/static/css/app.99741a48.css",
                                ".app-container");
                        System.out.println("M8B_WHATSAPP_CLAW_MIRROR_ASSETS_OK");
                    }
                }
                """
            ).strip(),
            encoding="utf-8",
        )
        subprocess.run(
            [
                str(JAVAC),
                "-encoding",
                "UTF-8",
                "-cp",
                classpath(self.classes, JSON_JAR, DATA_LIBS),
                "-d",
                str(self.probe_classes),
                str(probe_source),
            ],
            cwd=ROOT,
            check=True,
        )
        probe = subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.probe_classes, self.classes, JSON_JAR, DATA_LIBS),
                "M8BWhatsAppClawAssetsProbe",
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("M8B_WHATSAPP_CLAW_MIRROR_ASSETS_OK", probe.stdout)

        result = self.run_patcher()
        self.assertEqual(result.returncode, 0, result.stderr)
        inject_js_callback_block = self.javap_method_block(
            "public com.teamdev.jxbrowser.browser.callback.InjectJsCallback$Response on(com.teamdev.jxbrowser.browser.callback.InjectJsCallback$Params);",
            "com.sbf.main.jxbrowser.M5InjectJsCallback",
        )
        modern_dispatch_block = self.javap_method_block(
            "public final void a(javax.swing.JComponent, java.lang.String);",
            "com.sbf.main.JSBFMain$4",
        )
        self.assertIn("M8B_WSCLAW_TAB_JXBROWSER_URL_FROM_LINKURL", modern_dispatch_block)
        scheme_callback_block = self.javap_method_block(
            "public final java.lang.Object on(java.lang.Object);",
            "com.sbf.main.jxbrowser.b",
        )
        for marker in (
            "M8B_WSCLAW_LOCAL_PUBLIC_PATH",
            "/static/js/app.ae0af1a5.js",
            "/static/js/app.988d65c1.js",
            "/static/css/app.b4573062.css",
            "/static/css/app.99741a48.css",
        ):
            self.assertIn(marker, scheme_callback_block)
        for marker in (
            "M8B_WSCLAW_XHR_STUB",
            "/wsClaw/",
            "p.indexOf('/system/longxia_license')",
            "dataAllAccount",
            "checkAccountExist",
            "exist:false",
            "rows:[],total:0",
            "code:200,msg:'success',data:",
            "M8B_COPY_TO_CLIPBOARD_TERMINAL",
            "copyToClipboard",
        ):
            self.assertIn(marker, inject_js_callback_block)

    def test_whatsapp_super_environment_assets_and_offline_gate_are_patched(self):
        self.compile_patcher()
        probe_source = self.tmp_path / "M8BWhatsAppSuperEnvironmentAssetsProbe.java"
        probe_source.write_text(
            textwrap.dedent(
                """
                import com.sbf.main.jxbrowser.M5LocalSpiderBridge;

                public class M8BWhatsAppSuperEnvironmentAssetsProbe {
                    private static void requireContains(String url, String... needles)
                            throws Exception {
                        String body = M5LocalSpiderBridge.localWebAssetBody(url);
                        if (body == null || body.length() == 0) {
                            throw new AssertionError("missing mirrored asset: " + url);
                        }
                        for (String needle : needles) {
                            if (!body.contains(needle)) {
                                throw new AssertionError(
                                        "asset " + url + " is missing " + needle);
                            }
                        }
                    }

                    public static void main(String[] args) throws Exception {
                        requireContains(
                                "https://app.xdxsoft.com/pc/sender/senderGlobalControls/mysuperenvironment",
                                "<html");
                        requireContains(
                                "https://app.xdxsoft.com/static/js/app.988d65c1.js",
                                "./sender/senderGlobalControls/mysuperenvironment",
                                "\\\"3c50\\\"",
                                "chunk-609d7442");
                        requireContains(
                                "https://app.xdxsoft.com/static/js/chunk-609d7442.f2862f81.js",
                                "\\\"3c50\\\"",
                                "/sender/senderGlobalControls/mylist",
                                "dataList=t.rows",
                                "el-table");
                        System.out.println("M8B_WHATSAPP_SUPER_ENV_MIRROR_ASSETS_OK");
                    }
                }
                """
            ).strip(),
            encoding="utf-8",
        )
        subprocess.run(
            [
                str(JAVAC),
                "-encoding",
                "UTF-8",
                "-cp",
                classpath(self.classes, JSON_JAR, DATA_LIBS),
                "-d",
                str(self.probe_classes),
                str(probe_source),
            ],
            cwd=ROOT,
            check=True,
        )
        probe = subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.probe_classes, self.classes, JSON_JAR, DATA_LIBS),
                "M8BWhatsAppSuperEnvironmentAssetsProbe",
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("M8B_WHATSAPP_SUPER_ENV_MIRROR_ASSETS_OK", probe.stdout)

        result = self.run_patcher()
        self.assertEqual(result.returncode, 0, result.stderr)
        inject_js_callback_block = self.javap_method_block(
            "public com.teamdev.jxbrowser.browser.callback.InjectJsCallback$Response on(com.teamdev.jxbrowser.browser.callback.InjectJsCallback$Params);",
            "com.sbf.main.jxbrowser.M5InjectJsCallback",
        )
        modern_dispatch_block = self.javap_method_block(
            "public final void a(javax.swing.JComponent, java.lang.String);",
            "com.sbf.main.JSBFMain$4",
        )
        for marker in (
            "M8B_SUPER_ENV_TAB_JXBROWSER_URL_FROM_LINKURL",
            "/pc/sender/senderGlobalControls/mysuperenvironment",
        ):
            self.assertIn(marker, modern_dispatch_block)
        for marker in (
            "M8B_SUPER_ENV_XHR_STUB",
            "/sender/senderGlobalControls/mylist",
            "rows:[],total:0,data:[]",
            "M8B_SUPER_ENV_UI_ONLY",
            "M8B_SUPER_ENV_UI_GATED",
        ):
            self.assertIn(marker, inject_js_callback_block)

    def test_c5_platform_assets_offline_contracts_and_dispatch_are_patched(self):
        self.compile_patcher()
        probe_source = self.tmp_path / "C5PlatformAssetsProbe.java"
        probe_source.write_text(
            textwrap.dedent(
                """
                import com.sbf.main.jxbrowser.M5LocalSpiderBridge;

                public class C5PlatformAssetsProbe {
                    private static void requireContains(String url, String... needles)
                            throws Exception {
                        String body = M5LocalSpiderBridge.localWebAssetBody(url);
                        if (body == null || body.length() == 0) {
                            throw new AssertionError("missing C5 local response: " + url);
                        }
                        for (String needle : needles) {
                            if (!body.contains(needle)) {
                                throw new AssertionError(url + " missing " + needle);
                            }
                        }
                    }

                    public static void main(String[] args) throws Exception {
                        requireContains(
                                "https://app.xdxsoft.com/es/bigData/bigDataTask?code=big_data_tiktok_new",
                                "<html");
                        requireContains("https://app.xdxsoft.com/pc/tg/index", "<html");
                        requireContains("https://app.xdxsoft.com/pc/dataCollect/googleseo", "<html");
                        requireContains("https://app.xdxsoft.com/pc/kefu/conversation", "<html");
                        requireContains(
                                "https://app.xdxsoft.com/static/js/app.988d65c1.js",
                                "./es/bigDataTask",
                                "\\\"6052\\\"",
                                "chunk-52e25406",
                                "./tg/index",
                                "\\\"9c87\\\"",
                                "chunk-7a16220a",
                                "./dataCollect/googleseo",
                                "\\\"5e2e\\\"",
                                "chunk-d42d890c",
                                "./kefu/conversation",
                                "\\\"84ba\\\"",
                                "chunk-277dc7d5");
                        requireContains(
                                "https://app.xdxsoft.com/static/js/chunk-52e25406.ac8807d6.js",
                                "6052:function",
                                "/es/bigData/code/");
                        requireContains(
                                "https://app.xdxsoft.com/static/js/chunk-7a16220a.401d12a5.js",
                                "\\\"9c87\\\"",
                                "/tg/groupTask/list");
                        requireContains(
                                "https://app.xdxsoft.com/static/js/chunk-d42d890c.5447382c.js",
                                "\\\"5e2e\\\"",
                                "/accessFlow/order/list");
                        requireContains(
                                "https://app.xdxsoft.com/static/js/chunk-277dc7d5.ccdf1920.js",
                                "\\\"84ba\\\"",
                                "/kefu/conversation/list");
                        requireContains(
                                "https://app.xdxsoft.com/prod-api/es/bigData/code/fb_page_data",
                                "Facebook Page");
                        requireContains(
                                "https://app.xdxsoft.com/prod-api/es/bigData/code/ins_blogger_data",
                                "Instagram");
                        requireContains(
                                "https://app.xdxsoft.com/prod-api/es/bigData/code/big_data_twitter_new",
                                "X URL");
                        requireContains(
                                "https://app.xdxsoft.com/prod-api/es/bigData/code/big_data_tiktok_new",
                                "TikTok URL");
                        requireContains(
                                "https://app.xdxsoft.com/prod-api/tg/groupTask/list",
                                "\\\"rows\\\":[]");
                        requireContains(
                                "https://app.xdxsoft.com/prod-api/accessFlow/order/list",
                                "\\\"rows\\\":[]");
                        requireContains(
                                "https://app.xdxsoft.com/prod-api/kefu/conversation/list",
                                "\\\"rows\\\":[]");
                        requireContains(
                                "https://app.xdxsoft.com/static/js/chunk-46942aaa.78ddab17.js",
                                "\\\"big_data_tiktok_new\\\"==this.funcModuleCode?this.tableData=[]",
                                "\\\"big_data_twitter_new\\\"==this.funcModuleCode?this.tableData=[]");
                        byte[] background = M5LocalSpiderBridge.localWebAssetBytes(
                                "https://app.xdxsoft.com/static/img/cloudWords_background.fd301aa6.jpg");
                        if (background == null || background.length < 100) {
                            throw new AssertionError("missing C5 local background fallback");
                        }
                        if (!"image/jpeg".equals(M5LocalSpiderBridge.localWebAssetContentType(
                                "https://app.xdxsoft.com/static/img/cloudWords_background.fd301aa6.jpg"))) {
                            throw new AssertionError("wrong C5 background content type");
                        }
                        System.out.println("C5_PLATFORM_ASSETS_CONTRACTS_OK");
                    }
                }
                """
            ).strip(),
            encoding="utf-8",
        )
        subprocess.run(
            [
                str(JAVAC),
                "-encoding",
                "UTF-8",
                "-cp",
                classpath(self.classes, JSON_JAR, DATA_LIBS),
                "-d",
                str(self.probe_classes),
                str(probe_source),
            ],
            cwd=ROOT,
            check=True,
        )
        probe = subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.probe_classes, self.classes, JSON_JAR, DATA_LIBS),
                "C5PlatformAssetsProbe",
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("C5_PLATFORM_ASSETS_CONTRACTS_OK", probe.stdout)

        result = self.run_patcher()
        self.assertEqual(result.returncode, 0, result.stderr)
        inject_js_callback_block = self.javap_method_block(
            "public com.teamdev.jxbrowser.browser.callback.InjectJsCallback$Response on(com.teamdev.jxbrowser.browser.callback.InjectJsCallback$Params);",
            "com.sbf.main.jxbrowser.M5InjectJsCallback",
        )
        modern_dispatch_block = self.javap_method_block(
            "public final void a(javax.swing.JComponent, java.lang.String);",
            "com.sbf.main.JSBFMain$4",
        )
        default_dispatch = "\n".join(
            (
                self.javap_method_block(
                    "private static java.lang.String normalizeProductCode(java.lang.String);",
                    "com.sbf.main.M8D7DefaultMenuDispatch",
                ),
                self.javap_method_block(
                    "private static boolean isDefaultNode(com.sbf.main.ext.j2026.d, java.lang.String);",
                    "com.sbf.main.M8D7DefaultMenuDispatch",
                ),
                self.javap_method_block(
                    "private static com.sbf.main.ext.j2026.d buildDefaultNode(com.sbf.main.ext.j2026.d$a, java.lang.String);",
                    "com.sbf.main.M8D7DefaultMenuDispatch",
                ),
            )
        )
        for marker in (
            "label:function(key,fallback)",
            "C5_PLATFORM_UI_GATED",
            "MutationObserver",
            "C5_PLATFORM_XHR_STUB",
            "/tg/groupTask",
            "/accessFlow/order",
            "/kefu/conversation",
            "C5_PLATFORM_UI_ONLY",
            "es/bigDataTask",
            "tg/index",
            "dataCollect/googleseo",
            "kefu/conversation",
        ):
            self.assertIn(marker, inject_js_callback_block)
        for marker in (
            "C5_PLATFORM_TAB_JXBROWSER_URL_FROM_LINKURL",
            "/es/bigData/bigDataTask",
            "/pc/tg/index",
            "/pc/dataCollect/googleseo",
            "/pc/kefu/conversation",
        ):
            self.assertIn(marker, modern_dispatch_block)
        for marker in (
            "tiktok",
            "telegram",
            "geo",
            "wskefu",
            "REC_TIKTOK_BIG_DATA_ROUTE",
            "REC_TELEGRAM_GROUP_COLLECT_ROUTE",
            "REC_GEO_GOOGLE_SEO_ROUTE",
            "REC_WSKEFU_CONVERSATION_ROUTE",
            "C4747_003",
            "C4131_005",
            "C4133_003",
            "C3461_002",
            "C4135_005",
            "C4134_002",
            "C4936_000",
        ):
            self.assertIn(marker, default_dispatch)

    def test_local_spider_bridge_supports_collect_tab_configs_and_empty_data(self):
        self.compile_patcher()
        probe_source = self.tmp_path / "M5D11CollectTabBridgeProbe.java"
        probe_source.write_text(
            textwrap.dedent(
                """
                import com.sbf.main.jxbrowser.M5LocalSpiderBridge;
                import java.nio.file.Path;
                import java.nio.file.Paths;
                import org.json.JSONObject;

                public class M5D11CollectTabBridgeProbe {
                    public static void main(String[] args) throws Exception {
                        Path baseDir = Paths.get(args[0]);
                        String[] spiderCodes = {
                            "whatsapp_users_lists",
                            "wap_global_clue_users",
                            "whatsapp_group_lists",
                            "whatsapp_regional_collection"
                        };
                        for (int i = 0; i < spiderCodes.length; i++) {
                            JSONObject config = new JSONObject(
                                    M5LocalSpiderBridge.spiderConfig(baseDir.toString(), spiderCodes[i]));
                            if (!spiderCodes[i].equals(config.optString("code"))
                                    || !"whatsapp".equals(config.optString("moduleCode"))
                                    || config.optJSONArray("fields") == null
                                    || config.optJSONArray("fields").length() == 0) {
                                throw new AssertionError("bad tab spider config: " + config);
                            }
                            JSONObject page = new JSONObject(
                                    M5LocalSpiderBridge.listSpiderData(
                                            baseDir.toString(), "whatsapp", spiderCodes[i], 1, 50));
                            if (page.optInt("code") != 200 || page.optJSONArray("rows") == null) {
                                throw new AssertionError("bad tab data page: " + page);
                            }
                            if (!"whatsapp_users_lists".equals(spiderCodes[i])
                                    && (page.optLong("total") != 0L
                                            || page.optJSONArray("rows").length() != 0)) {
                                throw new AssertionError("non-primary tabs must remain empty without fake rows: " + page);
                            }
                        }
                        System.out.println("M5D11_COLLECT_TAB_BRIDGE_OK");
                    }
                }
                """
            ).strip(),
            encoding="utf-8",
        )
        subprocess.run(
            [
                str(JAVAC),
                "-encoding",
                "UTF-8",
                "-cp",
                classpath(self.classes, JSON_JAR, DATA_LIBS),
                "-d",
                str(self.probe_classes),
                str(probe_source),
            ],
            cwd=ROOT,
            check=True,
        )
        probe = subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.probe_classes, self.classes, JSON_JAR, DATA_LIBS),
                "M5D11CollectTabBridgeProbe",
                str(ROOT / "data" / "app"),
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("M5D11_COLLECT_TAB_BRIDGE_OK", probe.stdout)

    def run_patcher(self):
        return self.run_patcher_on(APP_JAR)

    def run_patcher_on(self, input_jar):
        return subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.classes, ASM_JAR),
                "M4AuthPatch",
                str(input_jar),
                str(self.output_jar),
            ],
            cwd=ROOT,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )

    def run_c6_overlay(self, input_jar):
        return subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.classes, ASM_JAR),
                "M4AuthPatch",
                "--c6-overlay",
                str(input_jar),
                str(self.output_jar),
            ],
            cwd=ROOT,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )

    def run_c66_overlay(self, input_jar):
        return subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.classes, ASM_JAR),
                "M4AuthPatch",
                "--c66-overlay",
                str(input_jar),
                str(self.output_jar),
            ],
            cwd=ROOT,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )

    def run_c67_overlay(self, input_jar):
        return subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.classes, ASM_JAR),
                "M4AuthPatch",
                "--c67-overlay",
                str(input_jar),
                str(self.output_jar),
            ],
            cwd=ROOT,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )

    def run_d1_x_overlay(self, input_jar):
        return subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.classes, ASM_JAR),
                "M4AuthPatch",
                "--d1-x-overlay",
                str(input_jar),
                str(self.output_jar),
            ],
            cwd=ROOT,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )

    def run_d2_ins_overlay(self, input_jar):
        return subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.classes, ASM_JAR),
                "M4AuthPatch",
                "--d2-ins-overlay",
                str(input_jar),
                str(self.output_jar),
            ],
            cwd=ROOT,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )

    def run_d3_fb_overlay(self, input_jar):
        return subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.classes, ASM_JAR),
                "M4AuthPatch",
                "--d3-fb-overlay",
                str(input_jar),
                str(self.output_jar),
            ],
            cwd=ROOT,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )

    def run_d5_tg_overlay(self, input_jar):
        return subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.classes, ASM_JAR),
                "M4AuthPatch",
                "--d5-tg-overlay",
                str(input_jar),
                str(self.output_jar),
            ],
            cwd=ROOT,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )

    def run_d5_geo_overlay(self, input_jar):
        return subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.classes, ASM_JAR),
                "M4AuthPatch",
                "--d5-geo-overlay",
                str(input_jar),
                str(self.output_jar),
            ],
            cwd=ROOT,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )

    def run_d5_wa_overlay(self, input_jar):
        return subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.classes, ASM_JAR),
                "M4AuthPatch",
                "--d5-wa-overlay",
                str(input_jar),
                str(self.output_jar),
            ],
            cwd=ROOT,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )

    def run_d8_online_overlay(self, input_jar):
        return subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.classes, ASM_JAR),
                "M4AuthPatch",
                "--d8-online-overlay",
                str(input_jar),
                str(self.output_jar),
            ],
            cwd=ROOT,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )

    def run_patcher_evidence_mode(self):
        return subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.classes, ASM_JAR),
                "M4AuthPatch",
                "--real-product-menu-logging",
                str(APP_JAR),
                str(self.output_jar),
            ],
            cwd=ROOT,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )

    def javap_method_block(self, method_header, class_name="com.sbf.util.http.SBFApi"):
        result = subprocess.run(
            [
                str(JAVAP),
                "-classpath",
                str(self.output_jar),
                "-c",
                "-p",
                class_name,
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=True,
        )
        lines = result.stdout.splitlines()
        matches = [i for i, line in enumerate(lines) if method_header in line]
        self.assertTrue(matches, f"missing javap method header: {method_header}")
        start = matches[0]
        end = next(
            (
                i
                for i in range(start + 1, len(lines))
                if lines[i].startswith("  ")
                and not lines[i].startswith("    ")
                and lines[i].strip().endswith(";")
            ),
            len(lines),
        )
        return "\n".join(lines[start:end])

    def javap_class_output(self, class_name):
        result = subprocess.run(
            [
                str(JAVAP),
                "-classpath",
                str(self.output_jar),
                "-p",
                class_name,
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=True,
        )
        return result.stdout

    def verify_class_loads(self, class_name):
        return subprocess.run(
            [
                str(JAVA),
                "-Xverify:all",
                "-cp",
                classpath(self.output_jar, DATA_LIBS),
                class_name,
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )

    def test_cd53_product_enter_callback_passes_jvm_verification(self):
        self.assertTrue(CD53_APP_DLL.exists(), CD53_APP_DLL)
        self.compile_patcher()
        result = self.run_patcher_on(CD53_APP_DLL)
        self.assertEqual(result.returncode, 0, result.stderr)

        verify = self.verify_class_loads("com.sbf.main.StartApp$5$3")

        self.assertNotEqual(verify.returncode, 0)
        self.assertNotIn("VerifyError", verify.stderr)
        self.assertIn("main", verify.stderr.lower())

    def test_m8d_support_helpers_are_single_class_artifacts(self):
        self.compile_patcher()

        main_classes = self.classes / "com" / "sbf" / "main"
        generated_dispatch = sorted(
            path.name for path in main_classes.glob("M8D7DefaultMenuDispatch*.class")
        )
        generated_diag = sorted(
            path.name for path in main_classes.glob("M8D14ExeDiag*.class")
        )

        self.assertEqual(generated_dispatch, ["M8D7DefaultMenuDispatch.class"])
        self.assertEqual(generated_diag, ["M8D14ExeDiag.class"])

    def test_patches_cd53_true_exe_product_enter_path(self):
        self.assertTrue(CD53_APP_DLL.exists(), CD53_APP_DLL)
        self.compile_patcher()

        result = self.run_patcher_on(CD53_APP_DLL)

        self.assertEqual(result.returncode, 0, result.stderr)
        true_exe_login_block = self.javap_method_block(
            "public com.sbf.main.ext.j2026.JLoginHTML(com.sbf.main.ext.j);",
            "com.sbf.main.ext.j2026.JLoginHTML",
        )
        delayed_login_block = self.javap_method_block(
            "public final void run();",
            "com.sbf.main.StartApp$7",
        )
        start_app_clinit_block = self.javap_method_block(
            "static {};",
            "com.sbf.main.StartApp",
        )
        product_enter_block = self.javap_method_block(
            "public final void a(org.json.JSONObject);",
            "com.sbf.main.StartApp$5$3",
        )
        default_dispatch_block = self.javap_method_block(
            "public static void dispatch(com.sbf.main.JSBFMain);",
            "com.sbf.main.M8D7DefaultMenuDispatch",
        )
        legacy_default_node_block = self.javap_method_block(
            "private static com.sbf.main.ext.j2026.d buildDefaultNode(com.sbf.main.ext.j2026.d$a);",
            "com.sbf.main.M8D7DefaultMenuDispatch",
        )
        product_default_node_block = self.javap_method_block(
            "private static com.sbf.main.ext.j2026.d buildDefaultNode(com.sbf.main.ext.j2026.d$a, java.lang.String);",
            "com.sbf.main.M8D7DefaultMenuDispatch",
        )
        default_callback_block = self.javap_method_block(
            "private static com.sbf.main.ext.j2026.d$a callbackOf(com.sbf.main.JSBFMain) throws java.lang.ReflectiveOperationException;",
            "com.sbf.main.M8D7DefaultMenuDispatch",
        )
        default_dispatch_on_edt_block = self.javap_method_block(
            "private static void dispatchOnEdt(com.sbf.main.JSBFMain, java.lang.String);",
            "com.sbf.main.M8D7DefaultMenuDispatch",
        )
        modern_dispatch_block = self.javap_method_block(
            "public final void a(javax.swing.JComponent, java.lang.String);",
            "com.sbf.main.JSBFMain$5",
        )
        browser_constructor_block = self.javap_method_block(
            "private com.sbf.main.jxbrowser.c(java.lang.String, java.lang.String, com.sbf.main.jxbrowser.l, boolean, com.sbf.main.jxbrowser.g$a);",
            "com.sbf.main.jxbrowser.c",
        )
        browser_load_finished_block = self.javap_method_block(
            "private void a(com.teamdev.jxbrowser.navigation.internal.rpc.LoadFinished);",
            "com.sbf.main.jxbrowser.c",
        )
        browser_layout_block = self.javap_method_block(
            "public void doLayout();",
            "com.sbf.main.jxbrowser.c",
        )
        browser_create_block = self.javap_method_block(
            "static void b(com.sbf.main.jxbrowser.c);",
            "com.sbf.main.jxbrowser.c",
        )
        browser_load_url_block = self.javap_method_block(
            "public final void c(java.lang.String);",
            "com.sbf.main.jxbrowser.c",
        )
        browser_ready_load_block = self.javap_method_block(
            "public final void run();",
            "com.sbf.main.jxbrowser.c$4",
        )
        browser_load_thread_block = self.javap_method_block(
            "public final void run();",
            "com.sbf.main.jxbrowser.c$3",
        )
        browser_attach_view_block = self.javap_method_block(
            "public final void m8AttachBrowserView();",
            "com.sbf.main.jxbrowser.c",
        )
        default_dispatch_class = self.javap_class_output(
            "com.sbf.main.M8D7DefaultMenuDispatch"
        )
        exe_diag_install_block = self.javap_method_block(
            "public static synchronized void install();",
            "com.sbf.main.M8D14ExeDiag",
        )
        self.assertIn("M8D2_TRUE_EXE_LOGIN_BRIDGE", true_exe_login_block)
        self.assertIn(
            "com/sbf/main/M8D14ExeDiag.install:()V",
            start_app_clinit_block,
        )
        self.assertIn("M8D14_EXE_DIAG_READY", exe_diag_install_block)
        self.assertIn("m8-d14-exe-diag.log", exe_diag_install_block)
        self.assertNotIn(
            "com/sbf/main/StartApp$5.a:(Lorg/json/JSONObject;)V",
            true_exe_login_block,
        )
        self.assertIn("M8D4_DELAYED_TRUE_EXE_LOGIN_BRIDGE", delayed_login_block)
        self.assertIn("M4B_AUTO_LOGIN", delayed_login_block)
        self.assertIn(
            '{\\"code\\":200,\\"msg\\":\\"offline login ok\\",\\"token\\":\\"offline-local-token-1234567890\\"',
            delayed_login_block,
        )
        self.assertIn("java/lang/System.currentTimeMillis:()J", delayed_login_block)
        self.assertNotIn('\\"time\\":0', delayed_login_block)
        self.assertIn("com/sbf/main/StartApp$5", delayed_login_block)
        self.assertIn(
            "com/sbf/main/StartApp$5.a:(Lorg/json/JSONObject;)V",
            delayed_login_block,
        )
        self.assertNotIn("com/sbf/main/StartApp$1.a:(Lorg/json/JSONObject;)V", delayed_login_block)
        self.assertIn("M8D3_LOCAL_PRODUCT_ENTER", product_enter_block)
        self.assertIn("M8D7_DEFAULT_MENU_DISPATCH_AFTER_ENTER", product_enter_block)
        self.assertIn("whatsapp", product_enter_block)
        self.assertIn("facebook", product_enter_block)
        self.assertIn("instagram", product_enter_block)
        self.assertIn("twitter", product_enter_block)
        self.assertIn("tiktok", product_enter_block)
        self.assertIn("telegram", product_enter_block)
        self.assertIn("geo", product_enter_block)
        self.assertIn("wskefu", product_enter_block)
        self.assertIn("org/json/JSONObject.optString", product_enter_block)
        self.assertIn("com/sbf/main/JSBFMain", product_enter_block)
        self.assertIn(
            "com/sbf/main/M8D7DefaultMenuDispatch.dispatch:(Lcom/sbf/main/JSBFMain;Ljava/lang/String;)V",
            product_enter_block,
        )
        self.assertIn("whatsapp", legacy_default_node_block)
        self.assertIn("REC_WHATSAPP_ONELINE_ROUTE", product_default_node_block)
        self.assertIn("91010101", product_default_node_block)
        self.assertIn("REC_FACEBOOK_PAGE_COLLECT_ROUTE", product_default_node_block)
        self.assertIn("/es/bigData/bigDataTask?code=fb_page_data", product_default_node_block)
        self.assertIn("REC_INSTAGRAM_BLOGGER_COLLECT_ROUTE", product_default_node_block)
        self.assertIn("/es/bigData/bigDataTask?code=ins_blogger_data", product_default_node_block)
        self.assertIn("REC_TWITTER_PRECISE_SEARCH_ROUTE", product_default_node_block)
        self.assertIn("/es/bigData/bigDataTask?code=big_data_twitter_new", product_default_node_block)
        self.assertIn("REC_TIKTOK_BIG_DATA_ROUTE", product_default_node_block)
        self.assertIn("REC_TELEGRAM_GROUP_COLLECT_ROUTE", product_default_node_block)
        self.assertIn("REC_GEO_GOOGLE_SEO_ROUTE", product_default_node_block)
        self.assertIn("REC_WSKEFU_CONVERSATION_ROUTE", product_default_node_block)
        self.assertIn("java/lang/Class.getDeclaredField", default_callback_block)
        self.assertIn("bt", default_callback_block)
        self.assertIn("com/sbf/main/ext/j2026/d.b:()V", default_dispatch_on_edt_block)
        self.assertNotIn(
            "com/sbf/main/ext/j2026/d$a.a:(Lcom/sbf/main/ext/j2026/d;)V",
            default_dispatch_on_edt_block,
        )
        self.assertIn("M4_V12_DISPATCH", modern_dispatch_block)
        self.assertIn(
            "M5D11_COLLECT_TAB_JXBROWSER_URL_FROM_LINKURL",
            modern_dispatch_block,
        )
        self.assertIn("M4_V13_BROWSER_CONSTRUCTOR url=", browser_constructor_block)
        self.assertIn("Field h:Lcom/teamdev/jxbrowser/browser/Browser;", browser_create_block)
        self.assertNotIn("Field g:Lcom/teamdev/jxbrowser/browser/Browser;", browser_create_block)
        self.assertIn("M4_V13_BROWSER_CREATED=", browser_create_block)
        self.assertIn("m5InstallWebDiagnostics", browser_create_block)
        self.assertNotIn("Method c:(Ljava/lang/String;)V", browser_create_block)
        self.assertIn("com/sbf/main/jxbrowser/c.c:(Ljava/lang/String;)V", browser_ready_load_block)
        self.assertNotIn("Navigation.loadUrl:(Ljava/lang/String;)V", browser_ready_load_block)
        self.assertIn(
            "com/sbf/main/jxbrowser/c.m8AttachBrowserView:()V",
            browser_load_thread_block,
        )
        self.assertIn(
            "Method j:()V",
            browser_attach_view_block,
        )
        self.assertIn("M4_V13_LOAD_URL=", browser_load_url_block)
        self.assertIn("M4_V18_NORMALIZED_URL=", browser_load_url_block)
        self.assertIn("Navigation.loadUrl:(Ljava/lang/String;)V", browser_load_url_block)
        self.assertIn("Field h:Lcom/teamdev/jxbrowser/browser/Browser;", browser_load_finished_block)
        self.assertNotIn("Field g:Lcom/teamdev/jxbrowser/browser/Browser;", browser_load_finished_block)
        self.assertIn("Lcom/teamdev/jxbrowser/view/swing/BrowserView;", browser_layout_block)
        self.assertNotIn("com/sbf/main/jxbrowser/c", default_dispatch_class)

    def test_patches_auth_methods_and_adds_menu_diagnostics(self):
        self.compile_patcher()

        result = self.run_patcher()

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertTrue(self.output_jar.exists())
        self.assertNotEqual(APP_JAR.read_bytes(), self.output_jar.read_bytes())

        with zipfile.ZipFile(APP_JAR) as original, zipfile.ZipFile(self.output_jar) as patched:
            added = sorted(set(patched.namelist()) - set(original.namelist()))
            self.assertEqual(
                added,
                [
                    "com/sbf/main/M8D14ExeDiag.class",
                    "com/sbf/main/M8D7DefaultMenuDispatch.class",
                    "com/sbf/main/ext/gg/M5YesCaptchaBridge.class",
                    "com/sbf/main/jxbrowser/M5AuthBootstrapCallback.class",
                    "com/sbf/main/jxbrowser/M5ConsoleObserver.class",
                    "com/sbf/main/jxbrowser/M5InjectJsCallback.class",
                    "com/sbf/main/jxbrowser/M5LocalSpiderBridge$LocalPipelineRunner.class",
                    "com/sbf/main/jxbrowser/M5LocalSpiderBridge.class",
                    "com/sbf/main/jxbrowser/M5RequestObserver.class",
                    "com/sbf/main/jxbrowser/M8WhatsAppDripCampaigns.class",
                    "com/sbf/main/jxbrowser/M8WhatsAppExternalBrowsers$AccountHandler.class",
                    "com/sbf/main/jxbrowser/M8WhatsAppExternalBrowsers$JsonHandler.class",
                    "com/sbf/main/jxbrowser/M8WhatsAppExternalBrowsers$MessageHandler.class",
                    "com/sbf/main/jxbrowser/M8WhatsAppExternalBrowsers.class",
                    "com/sbf/main/jxbrowser/M8WhatsAppNativeProfiles.class",
                ],
            )
            changed = [
                name
                for name in original.namelist()
                if original.read(name) != patched.read(name)
            ]
        self.assertEqual(
            changed,
            [
                "com/sbf/main/JSBFMain$4.class",
                "com/sbf/main/StartApp$1$3.class",
                "com/sbf/main/StartApp$1.class",
                "com/sbf/main/StartApp$3.class",
                "com/sbf/main/StartApp.class",
                "com/sbf/main/cloud/spider/SpiderCallback.class",
                "com/sbf/main/ext/gg/GoogleCRHelper.class",
                "com/sbf/main/ext/j2026/d$1.class",
                "com/sbf/main/ext/j2026/d$2.class",
                "com/sbf/main/ext/j2026/JLoginHTML.class",
                "com/sbf/main/ext/j2026/h$2.class",
                "com/sbf/main/jxbrowser/b.class",
                "com/sbf/main/jxbrowser/c$3.class",
                "com/sbf/main/jxbrowser/c.class",
                "com/sbf/main/jxbrowser/g.class",
                "com/sbf/main/jxbrowser/MiJava.class",
                "com/sbf/main/sub/b.class",
                "com/sbf/main/tree/i.class",
                "com/sbf/util/http/SBFApi$5.class",
                "com/sbf/util/http/SBFApi.class",
            ],
        )

        h_block = self.javap_method_block("public static org.json.JSONObject h(java.lang.String);")
        c_block = self.javap_method_block("public static org.json.JSONObject C();")
        menu_block = self.javap_method_block("public static org.json.JSONObject k();")
        login_block = self.javap_method_block(
            "public static org.json.JSONObject k(java.lang.String, java.lang.String);"
        )
        spider_modules_block = self.javap_method_block(
            "public static org.json.JSONArray M(java.lang.String);"
        )
        update_checker_block = self.javap_method_block(
            "public final void run();",
            "com.sbf.util.http.SBFApi$5",
        )
        tree_constructor_block = self.javap_method_block(
            "public com.sbf.main.tree.i(int, int, org.json.JSONObject, boolean, com.sbf.main.tree.i$a, org.json.JSONArray, java.lang.String);",
            "com.sbf.main.tree.i",
        )
        menu_dispatch_block = self.javap_method_block(
            "public final void a(com.sbf.main.tree.i);",
            "com.sbf.main.sub.b",
        )
        modern_dispatch_block = self.javap_method_block(
            "public final void a(javax.swing.JComponent, java.lang.String);",
            "com.sbf.main.JSBFMain$4",
        )
        modern_mouse_block = self.javap_method_block(
            "public final void mouseClicked(java.awt.event.MouseEvent);",
            "com.sbf.main.ext.j2026.h$2",
        )
        side_menu_mouse_block = self.javap_method_block(
            "public final void mouseClicked(java.awt.event.MouseEvent);",
            "com.sbf.main.ext.j2026.d$2",
        )
        side_menu_callback_block = self.javap_method_block(
            "public final void run();",
            "com.sbf.main.ext.j2026.d$1",
        )
        web_token_bridge_block = self.javap_method_block(
            "public static java.lang.String f(java.lang.String);",
            "com.sbf.main.StartApp",
        )
        auto_login_block = self.javap_method_block(
            "public final void run();",
            "com.sbf.main.StartApp$3",
        )
        true_exe_login_block = self.javap_method_block(
            "public com.sbf.main.ext.j2026.JLoginHTML(com.sbf.main.ext.j);",
            "com.sbf.main.ext.j2026.JLoginHTML",
        )
        login_success_block = self.javap_method_block(
            "public final void a(org.json.JSONObject);",
            "com.sbf.main.StartApp$1",
        )
        product_enter_block = self.javap_method_block(
            "public final void a(org.json.JSONObject);",
            "com.sbf.main.StartApp$1$3",
        )
        browser_load_block = self.javap_method_block(
            "public final void run();",
            "com.sbf.main.jxbrowser.c$3",
        )
        browser_constructor_block = self.javap_method_block(
            "public com.sbf.main.jxbrowser.c(java.lang.String, java.lang.String, com.sbf.main.jxbrowser.l, boolean);",
            "com.sbf.main.jxbrowser.c",
        )
        browser_navigation_finished_block = self.javap_method_block(
            "private void a(com.teamdev.jxbrowser.navigation.event.NavigationFinished);",
            "com.sbf.main.jxbrowser.c",
        )
        browser_load_finished_block = self.javap_method_block(
            "private void a(com.teamdev.jxbrowser.navigation.internal.rpc.LoadFinished);",
            "com.sbf.main.jxbrowser.c",
        )
        browser_capture_block = self.javap_method_block(
            "private static void m4CaptureBitmap(com.teamdev.jxbrowser.browser.Browser);",
            "com.sbf.main.jxbrowser.c",
        )
        browser_layout_block = self.javap_method_block(
            "public void doLayout();",
            "com.sbf.main.jxbrowser.c",
        )
        browser_web_diag_block = self.javap_method_block(
            "private static void m5InstallWebDiagnostics(com.teamdev.jxbrowser.browser.Browser);",
            "com.sbf.main.jxbrowser.c",
        )
        native_profile_switch_block = self.javap_method_block(
            "public static java.lang.String m8SwitchActiveWhatsAppProfile(java.lang.String);",
            "com.sbf.main.jxbrowser.c",
        )
        native_profile_ensure_block = self.javap_method_block(
            "private final com.teamdev.jxbrowser.browser.Browser m8EnsureWhatsAppProfileBrowser(java.lang.String) throws java.lang.Exception;",
            "com.sbf.main.jxbrowser.c",
        )
        native_profile_helper_switch_block = self.javap_method_block(
            "public static synchronized java.lang.String switchProfile(java.lang.String);",
            "com.sbf.main.jxbrowser.M8WhatsAppNativeProfiles",
        )
        native_profile_helper_ensure_block = self.javap_method_block(
            "private static com.teamdev.jxbrowser.browser.Browser ensureBrowser(com.sbf.main.jxbrowser.c, java.lang.String) throws java.lang.Exception;",
            "com.sbf.main.jxbrowser.M8WhatsAppNativeProfiles",
        )
        native_profile_helper_find_block = self.javap_method_block(
            "private static com.teamdev.jxbrowser.profile.Profile findOrCreateProfile(com.teamdev.jxbrowser.profile.Profiles, java.lang.String);",
            "com.sbf.main.jxbrowser.M8WhatsAppNativeProfiles",
        )
        scheme_callback_block = self.javap_method_block(
            "public final java.lang.Object on(java.lang.Object);",
            "com.sbf.main.jxbrowser.b",
        )
        console_observer_block = self.javap_method_block(
            "public void on(com.teamdev.jxbrowser.browser.event.ConsoleMessageReceived);",
            "com.sbf.main.jxbrowser.M5ConsoleObserver",
        )
        request_observer_block = self.javap_method_block(
            "public void on(com.teamdev.jxbrowser.net.event.RequestCompleted);",
            "com.sbf.main.jxbrowser.M5RequestObserver",
        )
        inject_js_callback_block = self.javap_method_block(
            "public com.teamdev.jxbrowser.browser.callback.InjectJsCallback$Response on(com.teamdev.jxbrowser.browser.callback.InjectJsCallback$Params);",
            "com.sbf.main.jxbrowser.M5InjectJsCallback",
        )
        dict_bridge_block = self.javap_method_block(
            "public java.lang.String getDicts(java.lang.String);",
            "com.sbf.main.jxbrowser.MiJava",
        )
        mijava_native_profile_switch_block = self.javap_method_block(
            "public java.lang.String m8SwitchWhatsAppNativeProfile(java.lang.String);",
            "com.sbf.main.jxbrowser.MiJava",
        )
        external_browser_start_block = self.javap_method_block(
            "public static synchronized java.lang.String start(java.lang.String, java.lang.String, java.lang.String) throws java.lang.Exception;",
            "com.sbf.main.jxbrowser.M8WhatsAppExternalBrowsers",
        )
        mijava_external_browser_start_block = self.javap_method_block(
            "public java.lang.String m8StartWhatsAppExternalBrowser(java.lang.String, java.lang.String);",
            "com.sbf.main.jxbrowser.MiJava",
        )
        mijava_get_info_block = self.javap_method_block(
            "public void getInfo(com.teamdev.jxbrowser.js.JsFunction);",
            "com.sbf.main.jxbrowser.MiJava",
        )
        mijava_get_cloud_spider_config_block = self.javap_method_block(
            "public void getCloudSpiderConfig(java.lang.String, com.teamdev.jxbrowser.js.JsFunction);",
            "com.sbf.main.jxbrowser.MiJava",
        )
        mijava_get_local_spider_config_block = self.javap_method_block(
            "public java.lang.String m5GetLocalSpiderConfig(java.lang.String);",
            "com.sbf.main.jxbrowser.MiJava",
        )
        sbfapi_get_local_task_block = self.javap_method_block(
            "public static org.json.JSONObject c(java.lang.Long);"
        )
        sbfapi_update_local_task_status_block = self.javap_method_block(
            "public static void a(java.lang.Long, int, java.lang.String, java.lang.Long);"
        )
        ws_filter_list_block = self.javap_method_block(
            "public void getWsFilterDataList(java.lang.String, com.teamdev.jxbrowser.js.JsFunction);",
            "com.sbf.main.jxbrowser.MiJava",
        )
        ws_filter_status_block = self.javap_method_block(
            "public void checkWSfilterStatus(com.teamdev.jxbrowser.js.JsFunction);",
            "com.sbf.main.jxbrowser.MiJava",
        )
        ws_filter_browsers_block = self.javap_method_block(
            "public void doGetAllOpenBrowserInWhatsapp(com.teamdev.jxbrowser.js.JsFunction);",
            "com.sbf.main.jxbrowser.MiJava",
        )
        ws_filter_execute_block = self.javap_method_block(
            "public void doZwFilterWhataspp(java.lang.String, java.lang.String, com.teamdev.jxbrowser.js.JsFunction);",
            "com.sbf.main.jxbrowser.MiJava",
        )
        google_cr_helper_block = self.javap_method_block(
            "public static java.lang.String a(java.lang.String, java.lang.String);",
            "com.sbf.main.ext.gg.GoogleCRHelper",
        )
        google_cr_helper_class = self.javap_class_output("com.sbf.main.ext.gg.GoogleCRHelper")
        yes_captcha_bridge_block = self.javap_method_block(
            "public static java.lang.String solve(java.lang.String, java.lang.String);",
            "com.sbf.main.ext.gg.M5YesCaptchaBridge",
        )
        yes_captcha_config_block = self.javap_method_block(
            "private static java.lang.String loadClientKey();",
            "com.sbf.main.ext.gg.M5YesCaptchaBridge",
        )
        ws_filter_list_worker_block = self.javap_method_block(
            "public final void run();",
            "com.sbf.main.jxbrowser.MiJava$171",
        )
        ws_filter_browsers_worker_block = self.javap_method_block(
            "public final void run();",
            "com.sbf.main.jxbrowser.MiJava$168",
        )
        engine_create_block = self.javap_method_block(
            "public static synchronized com.teamdev.jxbrowser.browser.Browser a(java.lang.String, com.sbf.main.jxbrowser.g$a, com.sbf.main.jxbrowser.g$b, java.lang.String, com.db.entery.xdx.JDBZWConfig, com.sbf.main.jxbrowser.l, boolean);",
            "com.sbf.main.jxbrowser.g",
        )
        browser_setup_block = self.javap_method_block(
            "public static void a(java.lang.String, com.teamdev.jxbrowser.browser.Browser, com.sbf.main.jxbrowser.g$a, com.sbf.main.jxbrowser.g$b, java.lang.String, com.db.entery.xdx.JDBZWConfig, com.sbf.main.jxbrowser.l);",
            "com.sbf.main.jxbrowser.g",
        )
        for block in (h_block, c_block, menu_block, login_block):
            self.assertIn("new", block)
            self.assertIn("org/json/JSONObject", block)
            self.assertIn("ldc", block)
            self.assertIn("invokespecial", block)
            self.assertIn("areturn", block)
            self.assertNotIn("invokedynamic", block)
        self.assertIn("new", spider_modules_block)
        self.assertIn("org/json/JSONArray", spider_modules_block)
        self.assertIn("ldc", spider_modules_block)
        self.assertIn("invokespecial", spider_modules_block)
        self.assertIn("areturn", spider_modules_block)
        self.assertNotIn("invokedynamic", spider_modules_block)
        self.assertIn("return", update_checker_block)
        self.assertNotIn("invokedynamic", update_checker_block)
        self.assertNotIn("org/json/JSONObject", update_checker_block)
        self.assertIn("M4_DIAG_MENU_K_CALLED resp=", menu_block)
        self.assertIn("M4_DIAG_MENU_K_CALLER", menu_block)
        self.assertIn("java/lang/Exception", menu_block)
        self.assertIn("printStackTrace", menu_block)
        self.assertIn("M4_DIAG_TREE_INIT raw=", tree_constructor_block)
        self.assertIn("M4_DIAG_TREE_FIELDS", tree_constructor_block)
        self.assertIn("M4_DIAG_DISPATCH_ENTER", menu_dispatch_block)
        self.assertIn("M4_DIAG_BRANCH_JSinglepage", menu_dispatch_block)
        self.assertIn("M4_DIAG_BRANCH_JxBrowser", menu_dispatch_block)
        self.assertIn("M4_V12_DISPATCH", modern_dispatch_block)
        self.assertIn("M4_V12_NEW_JXBROWSER", modern_dispatch_block)
        self.assertIn("M5D11_COLLECT_TAB_JXBROWSER_URL_FROM_LINKURL", modern_dispatch_block)
        self.assertIn("/pc/dataCollect/collectionTask", modern_dispatch_block)
        self.assertIn("com/sbf/main/ext/j2026/h.e:()Ljava/lang/String;", modern_dispatch_block)
        self.assertIn("com/sbf/main/ext/j2026/h.h:()Ljava/lang/String;", modern_dispatch_block)
        self.assertIn("com/sbf/main/ext/j2026/h.i:()Ljava/lang/String;", modern_dispatch_block)
        self.assertIn("M5A_V43_MENU_MOUSE_CLICKED", modern_mouse_block)
        self.assertIn("M5A_V43_MENU_MOUSE_BLOCKED", modern_mouse_block)
        self.assertIn("M5A_V43_MENU_MOUSE_CALLBACK", modern_mouse_block)
        self.assertIn("com/sbf/main/ext/j2026/h.e:()Ljava/lang/String;", modern_mouse_block)
        self.assertIn("com/sbf/main/ext/j2026/h.k:()Z", modern_mouse_block)
        self.assertIn("com/sbf/main/ext/j2026/h.l:()Z", modern_mouse_block)
        self.assertIn("M5A_V44_SIDE_MENU_MOUSE_CLICKED", side_menu_mouse_block)
        self.assertIn("M5A_V44_SIDE_MENU_MOUSE_BLOCKED", side_menu_mouse_block)
        self.assertIn("M5A_V44_SIDE_MENU_SELECT_CALL", side_menu_mouse_block)
        self.assertIn("com/sbf/main/ext/j2026/d.getName:()Ljava/lang/String;", side_menu_mouse_block)
        self.assertIn("com/sbf/main/ext/j2026/d.c:()I", side_menu_mouse_block)
        self.assertIn("com/sbf/main/ext/j2026/d.d:()Ljava/lang/String;", side_menu_mouse_block)
        self.assertIn("M5A_V44_SIDE_MENU_CALLBACK", side_menu_callback_block)
        self.assertIn("Field a:Lcom/sbf/main/ext/j2026/d;", side_menu_callback_block)
        self.assertNotIn("com/sbf/main/ext/j2026/d.g:(Lcom/sbf/main/ext/j2026/d;)Z", side_menu_callback_block)
        self.assertIn(
            "com/sbf/main/ext/j2026/d$a.a:(Lcom/sbf/main/ext/j2026/d;)V",
            side_menu_callback_block,
        )
        self.assertIn("getLoingIsToken", web_token_bridge_block)
        self.assertIn("get_current_token", web_token_bridge_block)
        self.assertIn("M4_V19_WEB_TOKEN_BRIDGE url=", web_token_bridge_block)
        self.assertIn("offline-local-token-1234567890", web_token_bridge_block)
        self.assertIn("String.contains:(Ljava/lang/CharSequence;)Z", web_token_bridge_block)
        self.assertIn("M4B_AUTO_LOGIN", auto_login_block)
        self.assertIn("com/sbf/main/StartApp$1", auto_login_block)
        self.assertIn("org/json/JSONObject", auto_login_block)
        self.assertIn("java/lang/System.currentTimeMillis:()J", auto_login_block)
        self.assertNotIn('\\"time\\":0', auto_login_block)
        self.assertIn("java/lang/System.currentTimeMillis:()J", login_block)
        self.assertNotIn('\\"time\\":0', login_block)
        self.assertIn(
            "com/sbf/main/StartApp$1.a:(Lorg/json/JSONObject;)V",
            auto_login_block,
        )
        self.assertIn("M8D2_TRUE_EXE_LOGIN_BRIDGE", true_exe_login_block)
        self.assertIn("M4B_AUTO_LOGIN", true_exe_login_block)
        self.assertIn("com/sbf/main/StartApp$1", true_exe_login_block)
        self.assertIn("org/json/JSONObject", true_exe_login_block)
        self.assertIn(
            "com/sbf/main/StartApp$1.a:(Lorg/json/JSONObject;)V",
            true_exe_login_block,
        )
        self.assertIn("M4B_SKIP_LOGIN_DISPOSE", login_success_block)
        self.assertIn("com/sbf/main/StartApp.t", login_success_block)
        self.assertIn("M8D3_LOCAL_PRODUCT_ENTER", product_enter_block)
        self.assertIn("whatsapp", product_enter_block)
        self.assertIn("org/json/JSONObject.optString", product_enter_block)
        self.assertIn("com/sbf/main/JSBFMain", product_enter_block)
        self.assertIn("M4_V13_BROWSER_CONSTRUCTOR url=", browser_constructor_block)
        self.assertIn("M4_V13_BROWSER_CREATED=", browser_constructor_block)
        self.assertIn("m5InstallWebDiagnostics", browser_constructor_block)
        self.assertIn("Lcom/teamdev/jxbrowser/browser/Browser;", browser_constructor_block)
        self.assertIn("M5_V20_WEB_DIAG_INSTALL browser=", browser_web_diag_block)
        self.assertIn("ConsoleMessageReceived", browser_web_diag_block)
        self.assertIn("RequestCompleted", browser_web_diag_block)
        self.assertIn("Network.on", browser_web_diag_block)
        self.assertIn("InjectJsCallback", browser_web_diag_block)
        self.assertIn("Browser.set", browser_web_diag_block)
        self.assertIn("M5_V23_JS_HOOK_INSTALL browser=", browser_web_diag_block)
        self.assertNotIn("InterceptUrlRequestCallback", browser_web_diag_block)
        self.assertNotIn("Network.set", browser_web_diag_block)
        self.assertIn("M5D8_LOCAL_WEB_ASSET_ADD_SCHEME_ACTIVE", browser_web_diag_block)
        self.assertIn("M5D8_LOCAL_WEB_ASSET_ADD_SCHEME url=", scheme_callback_block)
        self.assertIn("M5LocalSpiderBridge.localWebAssetBytes", scheme_callback_block)
        self.assertIn("M5LocalSpiderBridge.localWebAssetContentType", scheme_callback_block)
        self.assertIn("UrlRequestJob.write", scheme_callback_block)
        self.assertIn("M5_V20_WEB_DIAG_INSTALL_FAILED", browser_web_diag_block)
        self.assertIn("M5_V20_WEB_CONSOLE level=", console_observer_block)
        self.assertIn("ConsoleMessage.message", console_observer_block)
        self.assertIn("M5_V20_WEB_REQUEST code=", request_observer_block)
        self.assertIn("UrlRequest.url", request_observer_block)
        self.assertIn("RequestCompleted.errorCode", request_observer_block)
        self.assertIn("M5_V23_JSON_PARSE_UNDEFINED stack=", inject_js_callback_block)
        self.assertIn("M5_V23_JSON_DIAG_INSTALLED", inject_js_callback_block)
        self.assertIn("M5_V26_WEB_BOOTSTRAP_XHR url=", inject_js_callback_block)
        self.assertIn("M5_V26_WEB_BOOTSTRAP_FETCH url=", inject_js_callback_block)
        self.assertIn("M5A_V48_MIJAVA_BRIDGE_INJECTED", inject_js_callback_block)
        self.assertIn("M5A_V48_MIJAVA_BRIDGE_FAILED", inject_js_callback_block)
        self.assertIn("com/sbf/main/jxbrowser/MiJava", inject_js_callback_block)
        self.assertIn("com/teamdev/jxbrowser/js/JsObject.putProperty", inject_js_callback_block)
        self.assertIn("mijava", inject_js_callback_block)
        self.assertIn("java", inject_js_callback_block)
        self.assertIn("/prod-api/getInfo", inject_js_callback_block)
        self.assertIn("/system/user/profile", inject_js_callback_block)
        self.assertIn("/ads/inivitationCode/balance", inject_js_callback_block)
        self.assertIn("LOCAL-OFFLINE", inject_js_callback_block)
        self.assertIn("/prod-api/getRouters", inject_js_callback_block)
        self.assertIn("/prod-api/mnq/mnqAuthAccounts/mylist", inject_js_callback_block)
        self.assertIn("/prod-api/system/dict/data/type/yes_no_1_0", inject_js_callback_block)
        self.assertIn("permissions", inject_js_callback_block)
        self.assertIn("*:*:*", inject_js_callback_block)
        self.assertIn("rows", inject_js_callback_block)
        self.assertIn("total", inject_js_callback_block)
        self.assertIn("dictLabel", inject_js_callback_block)
        self.assertIn("dictValue", inject_js_callback_block)
        self.assertIn("/kefu/pageInfo/page", inject_js_callback_block)
        self.assertIn("/kefu/conversation/getUnread", inject_js_callback_block)
        self.assertIn("/kefu/conversation/member/", inject_js_callback_block)
        self.assertIn("/kefu/conversation/send", inject_js_callback_block)
        self.assertIn("/system/userconfig/getOneByUserNameAndCode", inject_js_callback_block)
        self.assertIn("/helplook/", inject_js_callback_block)
        self.assertIn("/world/tg/v2/platformToken", inject_js_callback_block)
        self.assertIn("/upmee/", inject_js_callback_block)
        self.assertIn("/ws/luopan/clientStatus", inject_js_callback_block)
        self.assertIn("/ws/luopan/userClient", inject_js_callback_block)
        self.assertIn("/ws/luopan/clientLogoutStatus", inject_js_callback_block)
        self.assertIn("/ws/luopan/contact/list", inject_js_callback_block)
        self.assertIn("M8_AI_KEFU_WA_STUB", inject_js_callback_block)
        self.assertIn("M8_AI_KEFU_XHR_STUB", inject_js_callback_block)
        self.assertIn("M8_AI_KEFU_MIJAVA_SHIM", inject_js_callback_block)
        self.assertIn("M8_AI_KEFU_MIJAVA_EVENT_FINAL", inject_js_callback_block)
        self.assertIn("https://web.whatsapp.com", inject_js_callback_block)
        self.assertIn("M8B1A_WHATSAPP_WEB_STATUS", inject_js_callback_block)
        self.assertIn("m8UpsertWhatsAppAccount", inject_js_callback_block)
        self.assertIn("m8ListWhatsAppAccounts", inject_js_callback_block)
        self.assertIn("m8UpsertWhatsAppMessage", inject_js_callback_block)
        self.assertIn("m8ListWhatsAppConversations", inject_js_callback_block)
        self.assertIn("m8ListWhatsAppMessages", inject_js_callback_block)
        self.assertIn("m8SetActiveWhatsAppProfile", inject_js_callback_block)
        self.assertIn("m8GetActiveWhatsAppProfile", inject_js_callback_block)
        self.assertIn("M8B1B_WHATSAPP_MESSAGE_CAPTURE", inject_js_callback_block)
        self.assertIn("M8B1B_CUSTOMER_PANEL_READY", inject_js_callback_block)
        self.assertIn("M8B1C_PROFILE_SWITCH", inject_js_callback_block)
        self.assertIn("m8SwitchWhatsAppNativeProfile", inject_js_callback_block)
        self.assertIn("m8StartWhatsAppExternalBrowser", inject_js_callback_block)
        self.assertIn("m8StopWhatsAppExternalBrowser", inject_js_callback_block)
        self.assertIn("M8B1C3_EXTERNAL_BROWSER_OPEN", inject_js_callback_block)
        self.assertIn("__m8b1c3OpenExternal", inject_js_callback_block)
        self.assertIn("m8Profile", inject_js_callback_block)
        self.assertIn("__m8b1cAccountSelect", inject_js_callback_block)
        self.assertIn("__m8b1cProfileId", inject_js_callback_block)
        self.assertIn("__m8b1bConversations", inject_js_callback_block)
        self.assertIn("__m8b1bMessages", inject_js_callback_block)
        self.assertIn("M8_ONELINE_AIBOT_SHIM", inject_js_callback_block)
        self.assertIn("/pc/aigc/aichat_dialog", inject_js_callback_block)
        self.assertIn("aibotChat", inject_js_callback_block)
        self.assertIn("cmpl", inject_js_callback_block)
        self.assertIn("all_done", inject_js_callback_block)
        self.assertIn("\\u672c\\u5730 AI \\u751f\\u6210", inject_js_callback_block)
        self.assertIn("M8_SMART_AI_MIJAVA_SHIM", inject_js_callback_block)
        self.assertIn("M8_SMART_AI_XHR_STUB", inject_js_callback_block)
        self.assertIn("__m8SmartAiRows", inject_js_callback_block)
        self.assertIn("agentFromBody", inject_js_callback_block)
        self.assertIn("/aiAgent/smartAi", inject_js_callback_block)
        self.assertIn("agent_template.txt", inject_js_callback_block)
        self.assertIn("/volcengine/market/my", inject_js_callback_block)
        self.assertIn("/volcengine/market/model/update", inject_js_callback_block)
        self.assertIn("/volcengine/market/delete/", inject_js_callback_block)
        self.assertIn("/volcengine/market/random", inject_js_callback_block)
        self.assertIn("/volcengine/market/aiChat/", inject_js_callback_block)
        self.assertIn("/volcengine/trains/tokens", inject_js_callback_block)
        self.assertIn("/volcengine/trains/recharge", inject_js_callback_block)
        self.assertIn("\\u672c\\u5730\\u667a\\u80fd\\u4f53\\u4f53\\u9a8c", inject_js_callback_block)
        self.assertIn("Proxy", inject_js_callback_block)
        self.assertIn("regMessageEvent", inject_js_callback_block)
        self.assertIn("toOpenFileSelect", inject_js_callback_block)
        self.assertIn("uploadFileDoHK", inject_js_callback_block)
        self.assertIn('\\"code\\":200', inject_js_callback_block)
        self.assertIn("InjectJsCallback$Response.proceed", inject_js_callback_block)
        self.assertIn("Frame.executeJavaScript", inject_js_callback_block)
        self.assertIn("M8WhatsAppNativeProfiles.switchProfile", native_profile_switch_block)
        self.assertIn("M8WhatsAppNativeProfiles.ensureProfileBrowser", native_profile_ensure_block)
        self.assertIn("M8B1C_NATIVE_PROFILE_SWITCH", native_profile_helper_switch_block)
        self.assertIn("ensureBrowser", native_profile_helper_switch_block)
        self.assertIn("attach", native_profile_helper_switch_block)
        self.assertIn("Profiles.newProfile", native_profile_helper_find_block)
        self.assertIn("Profile.newBrowser", native_profile_helper_ensure_block)
        self.assertIn("BrowserView.newInstance", native_profile_helper_ensure_block)
        self.assertIn("installDiagnostics", native_profile_helper_ensure_block)
        self.assertIn("whatsAppUrl", native_profile_helper_ensure_block)
        self.assertIn("m8SwitchActiveWhatsAppProfile", mijava_native_profile_switch_block)
        self.assertIn("M8B1C3_EXTERNAL_BROWSER_START", external_browser_start_block)
        self.assertIn("M8WhatsAppExternalBrowsers.start", mijava_external_browser_start_block)
        self.assertIn("M5_V21_GET_DICTS type=", dict_bridge_block)
        self.assertIn("[]", dict_bridge_block)
        self.assertIn("areturn", dict_bridge_block)
        self.assertIn("M5A_V49_MIJAVA_GET_INFO_BRIDGE_JSON", mijava_get_info_block)
        self.assertIn("permissions", mijava_get_info_block)
        self.assertIn("*:*:*", mijava_get_info_block)
        self.assertIn("com/teamdev/jxbrowser/js/JsFunction.invoke", mijava_get_info_block)
        self.assertNotIn("com/sbf/main/StartApp.m", mijava_get_info_block)
        self.assertIn("M5D11_LOCAL_DATACOLLECT_CONFIG_JSON", mijava_get_cloud_spider_config_block)
        self.assertIn("M5LocalSpiderBridge.spiderConfig", mijava_get_cloud_spider_config_block)
        self.assertIn("M5LocalSpiderBridge.spiderConfig", mijava_get_local_spider_config_block)
        self.assertIn("m5GetLocalSpiderConfig", inject_js_callback_block)
        self.assertIn("function cfg(o)", inject_js_callback_block)
        self.assertIn("'fields','spiderParams','hookurls','steps'", inject_js_callback_block)
        self.assertIn("M5D11_LOCAL_SPIDER_CONFIG_HTTP_FAILED", inject_js_callback_block)
        self.assertIn("com/sbf/main/jxbrowser/MiJava$171", ws_filter_list_block)
        self.assertIn("com/sbf/main/jxbrowser/MiJava$202", ws_filter_status_block)
        self.assertIn(
            "doGetAllOpenBrowserInWhatsapp:(Ljava/lang/String;Lcom/teamdev/jxbrowser/js/JsFunction;)V",
            ws_filter_browsers_block,
        )
        self.assertIn("M5C_AI_FILTER_EXECUTION_GATED", ws_filter_execute_block)
        self.assertIn("WhatsApp", ws_filter_execute_block)
        self.assertIn("com/teamdev/jxbrowser/js/JsFunction.invoke", ws_filter_execute_block)
        self.assertNotIn("com/sbf/main/jxbrowser/MiJava$170", ws_filter_execute_block)
        self.assertIn("com/db/WhereInfo.limit", ws_filter_list_worker_block)
        self.assertIn("com/db/WhereInfo.currentPage", ws_filter_list_worker_block)
        self.assertIn("com/db/DAOBase.countOf", ws_filter_list_worker_block)
        self.assertIn("com/db/DAOBase.queryLimit", ws_filter_list_worker_block)
        self.assertIn("com/db/Result.getCount", ws_filter_list_worker_block)
        self.assertIn("com/db/Result.getList", ws_filter_list_worker_block)
        self.assertIn("org/json/JSONArray", ws_filter_browsers_worker_block)
        self.assertIn("com/teamdev/jxbrowser/js/JsFunction.invoke", ws_filter_browsers_worker_block)
        local_mock_method_block = self.javap_method_block(
            "public java.lang.String m5WriteLocalMockResult(java.lang.String, java.lang.String, java.lang.String);",
            "com.sbf.main.jxbrowser.MiJava",
        )
        local_submit_method_block = self.javap_method_block(
            "public java.lang.String m5SubmitLocalCollectTask(java.lang.String, java.lang.String, java.lang.String, java.lang.String);",
            "com.sbf.main.jxbrowser.MiJava",
        )
        local_list_tasks_method_block = self.javap_method_block(
            "public java.lang.String m5ListLocalCollectTasks(java.lang.String, java.lang.String);",
            "com.sbf.main.jxbrowser.MiJava",
        )
        self.assertIn("M5A_LOCAL_DATACOLLECT_MOCK_WRITE", local_mock_method_block)
        self.assertIn("com/sbf/main/StartApp.a", local_mock_method_block)
        self.assertIn("com/sbf/main/jxbrowser/M5LocalSpiderBridge.writeMockResult", local_mock_method_block)
        self.assertIn("M5C_COLLECT_LOCAL_TASK_SUBMIT", local_submit_method_block)
        self.assertIn("com/sbf/main/jxbrowser/M5LocalSpiderBridge.submitTask", local_submit_method_block)
        self.assertIn("M5C_COLLECT_LOCAL_TASK_LIST", local_list_tasks_method_block)
        self.assertIn("com/sbf/main/jxbrowser/M5LocalSpiderBridge.listTasks", local_list_tasks_method_block)
        sbfapi_get_new_task_block = self.javap_method_block(
            "public static org.json.JSONArray a(java.lang.String, int);"
        )
        sbfapi_cancel_all_run_block = self.javap_method_block("public static void L(java.lang.String);")
        self.assertIn("M5C_QUEUE_SBFAPI_GET_NEW_TASK", sbfapi_get_new_task_block)
        self.assertIn("com/sbf/main/jxbrowser/M5LocalSpiderBridge.getNewTask", sbfapi_get_new_task_block)
        self.assertIn("M5C_QUEUE_SBFAPI_CANCEL_ALL_RUN", sbfapi_cancel_all_run_block)
        self.assertIn("com/sbf/main/jxbrowser/M5LocalSpiderBridge.cancelAllRun", sbfapi_cancel_all_run_block)
        self.assertIn("M5C_COLLECT_SBFAPI_GET_LOCAL_TASK", sbfapi_get_local_task_block)
        self.assertIn("com/sbf/main/jxbrowser/M5LocalSpiderBridge.getTask", sbfapi_get_local_task_block)
        self.assertIn("org/json/JSONObject", sbfapi_get_local_task_block)
        self.assertIn("M5C_COLLECT_SBFAPI_STATUS_LOCAL", sbfapi_update_local_task_status_block)
        self.assertIn("com/sbf/main/jxbrowser/M5LocalSpiderBridge.updateTaskStatus", sbfapi_update_local_task_status_block)
        self.assertIn("M5D_YESCAPTCHA_GOOGLE_CR_TASK", google_cr_helper_block)
        self.assertIn("com/sbf/main/ext/gg/M5YesCaptchaBridge.solve", google_cr_helper_block)
        self.assertNotIn("com/sbf/main/ext/gg/l.e", google_cr_helper_class)
        self.assertIn("YESCAPTCHA_CLIENT_KEY", yes_captcha_config_block)
        self.assertIn("platform", inject_js_callback_block)
        self.assertIn("facebook.com", inject_js_callback_block)
        self.assertIn("google.com", inject_js_callback_block)
        self.assertIn("m5SubmitLocalCollectTask", inject_js_callback_block)
        self.assertIn("/cloud/spider/code/", inject_js_callback_block)
        self.assertIn("/dataCollect/platform/list", inject_js_callback_block)
        self.assertIn("/cloud/spider/data/", inject_js_callback_block)
        self.assertIn("/cloud/task", inject_js_callback_block)
        self.assertIn("submitted", inject_js_callback_block)
        self.assertIn("false", inject_js_callback_block)
        self.assertIn("whatsapp_users_lists", inject_js_callback_block)
        self.assertNotIn("__m5LocalSpider", inject_js_callback_block)
        self.assertNotIn("__m5CollectFullShape", inject_js_callback_block)
        self.assertNotIn("__m5AutoSeedDataCollect", inject_js_callback_block)
        self.assertNotIn("M5A_LOCAL_DATACOLLECT_AUTO_SEED", inject_js_callback_block)
        self.assertNotIn("m5WriteLocalMockResult", inject_js_callback_block)
        self.assertNotIn("m5ListLocalCollectTasks", inject_js_callback_block)
        self.assertNotIn("local-ui-mock", inject_js_callback_block)
        self.assertIn("M4_V13_LOAD_URL=", browser_load_block)
        self.assertIn("M4_V18_NORMALIZED_URL=", browser_load_block)
        self.assertIn("JSinglepage", browser_load_block)
        self.assertIn("JSinglepage:/pc/aicloud/my", browser_load_block)
        self.assertIn("/pc/aicloud/my", browser_load_block)
        self.assertIn("JSinglepage:/ws/wsfilter/home", browser_load_block)
        self.assertIn("/ws/wsfilter/home", browser_load_block)
        self.assertIn("JSinglepage:/", browser_load_block)
        self.assertIn("String.substring:(I)Ljava/lang/String;", browser_load_block)
        self.assertIn("/pc/dataCollect/collectionTask?modal=whatsapp_users_lists&moduleCode=whatsapp", browser_load_block)
        self.assertIn('String.startsWith:(Ljava/lang/String;)Z', browser_load_block)
        self.assertIn("com/sbf/util/http/SBFApi.c:()Ljava/lang/String;", browser_load_block)
        self.assertIn("https://", browser_load_block)
        self.assertIn("Navigation.loadUrl:(Ljava/lang/String;)V", browser_load_block)
        self.assertIn("M4_V13_LOAD_FAILED url=", browser_navigation_finished_block)
        self.assertIn("NavigationFinished.url:()Ljava/lang/String;", browser_navigation_finished_block)
        self.assertIn("NavigationFinished.error:()Lcom/teamdev/jxbrowser/net/NetError;", browser_navigation_finished_block)
        self.assertIn("M4_V13_LOAD_FINISHED url=", browser_load_finished_block)
        self.assertIn("Browser.url:()Ljava/lang/String;", browser_load_finished_block)
        self.assertIn("Lcom/teamdev/jxbrowser/browser/Browser;", browser_load_finished_block)
        self.assertIn("M4_V13_VIEW=", browser_layout_block)
        self.assertIn("Lcom/teamdev/jxbrowser/view/swing/BrowserView;", browser_layout_block)
        self.assertNotIn("ifnonnull", browser_layout_block)
        self.assertNotIn("BrowserView.getParent:()Ljava/awt/Container;", browser_layout_block)
        self.assertNotIn("BrowserView.getSize:()Ljava/awt/Dimension;", browser_layout_block)
        self.assertIn("RenderingMode.OFF_SCREEN", engine_create_block)
        self.assertNotIn("RenderingMode.HARDWARE_ACCELERATED", engine_create_block)
        self.assertIn("EngineOptions$Builder.disableGpu", engine_create_block)
        self.assertIn("EngineOptions$Builder.userAgent", engine_create_block)
        self.assertIn("Browser.userAgent", browser_setup_block)
        self.assertIn("M8B1A_BROWSER_USER_AGENT_FORCED", browser_setup_block)
        self.assertIn("Chrome/124.0.0.0", engine_create_block)
        for chromium_switch in (
            "--disable-gpu-compositing",
            "--disable-d3d11",
            "--use-gl=swiftshader",
            "--use-angle=swiftshader",
        ):
            self.assertIn(chromium_switch, engine_create_block)
        self.assertNotIn("--disable-software-rasterizer", engine_create_block)
        self.assertIn("M4_V14_RENDER_MODE=", engine_create_block)
        self.assertIn("EngineOptions.renderingMode", engine_create_block)
        self.assertIn("EngineOptions.userAgent", engine_create_block)
        self.assertIn("EngineOptions.switches", engine_create_block)
        self.assertIn("m4CaptureBitmap", browser_load_finished_block)
        self.assertIn("Browser.bitmap:()Lcom/teamdev/jxbrowser/ui/Bitmap;", browser_capture_block)
        self.assertIn(
            "com/teamdev/jxbrowser/view/swing/graphics/BitmapImage.toToolkit",
            browser_capture_block,
        )
        self.assertIn("javax/imageio/ImageIO.write", browser_capture_block)
        self.assertIn(r"C:\m2dump\m4-jxb-capture.png", browser_capture_block)
        self.assertIn("M4_V14_CAPTURE", browser_capture_block)
        self.assertIn("M4_V14_CAPTURE_FAILED", browser_capture_block)

        probe_source = self.tmp_path / "M4AuthPatchProbe.java"
        probe_source.write_text(
            textwrap.dedent(
                """
                import com.sbf.util.http.SBFApi;
                import java.awt.Color;
                import org.json.JSONArray;
                import org.json.JSONObject;

                public class M4AuthPatchProbe {
                    public static void main(String[] args) throws Exception {
                        JSONObject login = SBFApi.k("test@example.com", "offline-password");
                        if (login.getInt("code") != 200) {
                            throw new AssertionError("login code");
                        }
                        if (!login.optString("sf").contains("41")) {
                            throw new AssertionError("login sf: " + login);
                        }
                        JSONObject loginData = login.getJSONObject("data");
                        if (loginData.optString("token").length() <= 10) {
                            throw new AssertionError("login token");
                        }
                        if (!loginData.has("ucf") || !loginData.has("imConfig")) {
                            throw new AssertionError("login data shape: " + loginData);
                        }
                        String webToken = com.sbf.main.StartApp.f("https://app.xdxsoft.com/prod-api/getLoingIsToken");
                        if (!"offline-local-token-1234567890".equals(webToken)) {
                            throw new AssertionError("web token bridge: " + webToken);
                        }
                        String webToken2 = com.sbf.main.StartApp.f("https://app.xdxsoft.com/prod-api/get_current_token");
                        if (!"offline-local-token-1234567890".equals(webToken2)) {
                            throw new AssertionError("web token bridge alt: " + webToken2);
                        }
                        java.lang.reflect.Method getDicts =
                                com.sbf.main.jxbrowser.MiJava.class.getDeclaredMethod("getDicts", String.class);
                        if (!String.class.equals(getDicts.getReturnType())) {
                            throw new AssertionError("bad getDicts return type: " + getDicts);
                        }

                        JSONArray spiderModules = SBFApi.M("spider_modules");
                        if (spiderModules == null) {
                            throw new AssertionError("spider modules null");
                        }
                        if (spiderModules.length() != 0) {
                            throw new AssertionError("spider modules should be empty offline: " + spiderModules);
                        }

                        JSONObject info = SBFApi.h("offline-token");
                        JSONObject result = info.getJSONObject("result");
                        if (result.getInt("code") != 200) {
                            throw new AssertionError("getInfo code");
                        }
                        JSONObject data = result.getJSONObject("data");
                        if (!data.has("user") || !data.has("roles") || !data.has("periodTime")) {
                            throw new AssertionError("getInfo data shape: " + data);
                        }
                        JSONObject im = data.optJSONObject("im");
                        if (im == null
                                || im.optString("ip").length() == 0
                                || im.optJSONObject("port") == null
                                || im.optJSONObject("port").optInt("udp") <= 0) {
                            throw new AssertionError("getInfo im shape: " + data);
                        }
                        if (data.getJSONObject("user").optString("nickname").length() == 0) {
                            throw new AssertionError("user nickname");
                        }

                        JSONObject modules = SBFApi.C();
                        if (modules.getInt("code") != 200) {
                            throw new AssertionError("module code");
                        }
                        JSONArray products = modules.getJSONArray("data");
                        if (products.length() != 9) {
                            throw new AssertionError("expected nine products: " + products);
                        }
                        JSONObject product = products.getJSONObject(0);
                        if (product.getInt("status") != 1 || product.getInt("remainingDays") < 0) {
                            throw new AssertionError("not enterable: " + product);
                        }
                        if (!product.has("name") || !product.has("displayName") || !product.has("themeStyle")) {
                            throw new AssertionError("product shape: " + product);
                        }
                        if (!product.optString("logoSvg").trim().startsWith("<svg")) {
                            throw new AssertionError("product logoSvg must be inline SVG: " + product);
                        }
                        String[] productCodes = {
                            "whatsapp", "tiktok", "facebook", "instagram", "twitter",
                            "telegram", "geo", "wskefu", "aishope"
                        };
                        for (int productIndex = 0; productIndex < productCodes.length; productIndex++) {
                            JSONObject candidate = products.getJSONObject(productIndex);
                            if (candidate.optInt("id") != 9101 + productIndex
                                    || !productCodes[productIndex].equals(candidate.optString("code"))) {
                                throw new AssertionError("bad recovered product: " + candidate);
                            }
                            JSONArray children = candidate.optJSONArray("children");
                            if (productIndex < 8 && (children == null || children.length() == 0)) {
                                throw new AssertionError("missing product menus: " + candidate);
                            }
                            if (productIndex == 8
                                    && (candidate.optInt("status") == 0
                                            || candidate.optInt("status") == 1
                                            || children == null
                                            || children.length() != 0)) {
                                throw new AssertionError("aishope must stay unopened: " + candidate);
                            }
                        }

                        JSONObject menus = SBFApi.k();
                        JSONArray menuEntries = menus.optJSONArray("scfs");
                        if (menuEntries == null) {
                            throw new AssertionError("missing top-level scfs: " + menus);
                        }
                        if (!menus.has("tas") || !menus.has("ucf")) {
                            throw new AssertionError("missing top-level menu metadata: " + menus);
                        }
                        if (menuEntries.length() != 155) {
                            throw new AssertionError("expected 155 recovered menus: " + menuEntries.length());
                        }
                        for (int menuIndex = 0; menuIndex < menuEntries.length(); menuIndex++) {
                            JSONObject recoveredMenu = menuEntries.getJSONObject(menuIndex);
                            boolean whatsappCollectParent =
                                    "C4749_006".equals(recoveredMenu.optString("code"));
                            boolean whatsappCollectChild =
                                    "REC_WHATSAPP_COLLECT_USERS_ROUTE".equals(recoveredMenu.optString("code"));
                            boolean whatsappCollectTabChild =
                                    recoveredMenu.optString("code").startsWith("REC_WHATSAPP_COLLECT_TAB_");
                            boolean whatsappOneLineParent =
                                    "REC_WHATSAPP_ONELINE".equals(recoveredMenu.optString("code"));
                            boolean whatsappOneLineChild =
                                    "REC_WHATSAPP_ONELINE_ROUTE".equals(recoveredMenu.optString("code"));
                            boolean whatsappAgentModelParent =
                                    "REC_WHATSAPP_AGENT_MODEL".equals(recoveredMenu.optString("code"));
                            boolean whatsappAgentModelChild =
                                    "REC_WHATSAPP_AGENT_MODEL_ROUTE".equals(recoveredMenu.optString("code"));
                            boolean whatsappClawParent =
                                    "REC_WHATSAPP_CLAW".equals(recoveredMenu.optString("code"));
                            boolean whatsappClawTabChild =
                                    recoveredMenu.optString("code").startsWith("REC_WHATSAPP_CLAW_TAB_");
                            boolean whatsappSuperParent =
                                    "REC_WHATSAPP_SUPER".equals(recoveredMenu.optString("code"));
                            boolean whatsappSuperChild =
                                    "REC_WHATSAPP_SUPER_ENV_ROUTE".equals(recoveredMenu.optString("code"));
                            boolean whatsappDataParent =
                                    "C4749_007".equals(recoveredMenu.optString("code"));
                            boolean whatsappDataChild =
                                    "REC_WHATSAPP_AI_DATA_ROUTE".equals(recoveredMenu.optString("code"));
                            boolean whatsappFilterParent =
                                    "C4749_009".equals(recoveredMenu.optString("code"));
                            boolean whatsappFilterChild =
                                    "REC_WHATSAPP_AI_FILTER_ROUTE".equals(recoveredMenu.optString("code"));
                            boolean whatsappKefuParent =
                                    "C4749_011".equals(recoveredMenu.optString("code"));
                            boolean whatsappKefuChild =
                                    "REC_WHATSAPP_AI_KEFU_ROUTE".equals(recoveredMenu.optString("code"));
                            boolean d3FbLocalParent =
                                    recoveredMenu.optString("evidence").startsWith("d3-fb-local:");
                            boolean d3FbLocalChild =
                                    recoveredMenu.optString("evidence").startsWith("d3-fb-local-child:");
                            boolean d4TkLocalParent = recoveredMenu.optString("evidence").startsWith("d4-tk-local:");
                            boolean d4TkLocalChild = recoveredMenu.optString("evidence").startsWith("d4-tk-local-child:");
                            boolean d5TgLocalParent = recoveredMenu.optString("evidence").startsWith("d5-tg-local:");
                            boolean d5TgLocalChild = recoveredMenu.optString("evidence").startsWith("d5-tg-local-child:");
                            boolean d5GeoLocalParent = recoveredMenu.optString("evidence").startsWith("d5-geo-local:");
                            boolean d5GeoLocalChild = recoveredMenu.optString("evidence").startsWith("d5-geo-local-child:");
                            boolean d5WaLocalParent = recoveredMenu.optString("evidence").startsWith("d5-wa-local:");
                            boolean d5WaLocalChild = recoveredMenu.optString("evidence").startsWith("d5-wa-local-child:");
                            boolean d2InsLocalParent =
                                    recoveredMenu.optString("evidence").startsWith("d2-ins-local:");
                            boolean d2InsLocalChild =
                                    recoveredMenu.optString("evidence").startsWith("d2-ins-local-child:");
                            boolean d1XLocalParent =
                                    recoveredMenu.optString("evidence").startsWith("d1-x-local:");
                            boolean d1XLocalChild =
                                    recoveredMenu.optString("evidence").startsWith("d1-x-local-child:");
                            boolean whatsappAdvertisingParent =
                                    "C3460_001".equals(recoveredMenu.optString("code"));
                            boolean whatsappAdvertisingChild =
                                    "REC_WHATSAPP_ADVERTISING_ROUTE".equals(recoveredMenu.optString("code"));
                            boolean c5PlatformParent =
                                    recoveredMenu.optString("evidence").startsWith("c5-platform-route:");
                            boolean c5PlatformChild =
                                    recoveredMenu.optString("evidence").startsWith("c5-platform-route-child:");
                            if (recoveredMenu.optInt("productId") < 9101
                                    || recoveredMenu.optInt("productId") > 9108
                                    || recoveredMenu.optString("code").startsWith("C2850000")
                                    || recoveredMenu.optString("name").contains("AIGC Video")
                                    || recoveredMenu.optString("name").contains("Graphic Video")
                                    || recoveredMenu.optString("icon").contains("/")
                                    || recoveredMenu.optString("icon").endsWith(".svg")
                                    || recoveredMenu.optString("linkUrl").contains("offline-home.html")) {
                                throw new AssertionError("bad recovered menu: " + recoveredMenu);
                            }
                            if (whatsappOneLineParent) {
                                if (!"JSinglepage".equals(recoveredMenu.optString("localCode"))
                                        || !"/pc/aigc/aichat_dialog".equals(recoveredMenu.optString("linkUrl"))
                                        || !recoveredMenu.optString("evidence").contains("recovery-route:aichat-dialog")) {
                                    throw new AssertionError("bad WhatsApp one-line recovery route: " + recoveredMenu);
                                }
                            } else if (whatsappOneLineChild) {
                                if (!"/pc/aigc/aichat_dialog".equals(recoveredMenu.optString("localCode"))
                                        || !"JSinglepage:/pc/aigc/aichat_dialog".equals(recoveredMenu.optString("linkUrl"))
                                        || !recoveredMenu.optString("evidence").contains("recovery-route-child:j2026-h-field-map:aichat-dialog")) {
                                    throw new AssertionError("bad WhatsApp one-line child route: " + recoveredMenu);
                                }
                            } else if (whatsappAgentModelParent) {
                                if (!"JSinglepage".equals(recoveredMenu.optString("localCode"))
                                        || !"/aiAgent/smartAi".equals(recoveredMenu.optString("linkUrl"))
                                        || !recoveredMenu.optString("evidence").contains("recovery-route:smart-ai")) {
                                    throw new AssertionError("bad WhatsApp smartAi recovery route: " + recoveredMenu);
                                }
                            } else if (whatsappAgentModelChild) {
                                if (!"/aiAgent/smartAi".equals(recoveredMenu.optString("localCode"))
                                        || !"JSinglepage:/aiAgent/smartAi".equals(recoveredMenu.optString("linkUrl"))
                                        || !recoveredMenu.optString("evidence").contains("recovery-route-child:j2026-h-field-map:smart-ai")) {
                                    throw new AssertionError("bad WhatsApp smartAi child route: " + recoveredMenu);
                                }
                            } else if (whatsappClawParent) {
                                if (!"JSinglepage".equals(recoveredMenu.optString("localCode"))
                                        || !"/wsClaw/browser".equals(recoveredMenu.optString("linkUrl"))
                                        || !recoveredMenu.optString("evidence").contains("recovery-route:ws-claw")) {
                                    throw new AssertionError("bad WhatsApp claw recovery route: " + recoveredMenu);
                                }
                            } else if (whatsappClawTabChild) {
                                if (!recoveredMenu.optString("localCode").startsWith("/wsClaw/")
                                        || !"JSinglepage".equals(recoveredMenu.optString("linkUrl"))
                                        || !recoveredMenu.optString("evidence").contains("m8-6-b-menu-tab:wsClaw:")) {
                                    throw new AssertionError("bad WhatsApp claw tab child route: " + recoveredMenu);
                                }
                            } else if (whatsappSuperParent) {
                                if (!"JSinglepage".equals(recoveredMenu.optString("localCode"))
                                        || !"/pc/sender/senderGlobalControls/mysuperenvironment".equals(recoveredMenu.optString("linkUrl"))
                                        || !recoveredMenu.optString("evidence").contains("recovery-route:super-environment")) {
                                    throw new AssertionError("bad WhatsApp super environment route: " + recoveredMenu);
                                }
                            } else if (whatsappSuperChild) {
                                if (!"/pc/sender/senderGlobalControls/mysuperenvironment".equals(recoveredMenu.optString("localCode"))
                                        || !"JSinglepage".equals(recoveredMenu.optString("linkUrl"))
                                        || !recoveredMenu.optString("evidence").contains("m8-8-b-menu-tab:senderGlobalControls:mysuperenvironment")) {
                                    throw new AssertionError("bad WhatsApp super environment child route: " + recoveredMenu);
                                }
                            } else if (whatsappCollectParent) {
                                if (!"JSinglepage".equals(recoveredMenu.optString("localCode"))
                                        || !"/pc/dataCollect/collectionTask?modal=whatsapp_users_lists&moduleCode=whatsapp".equals(recoveredMenu.optString("linkUrl"))
                                        || !recoveredMenu.optString("evidence").contains("recovery-route")) {
                                    throw new AssertionError("bad WhatsApp collect recovery route: " + recoveredMenu);
                                }
                            } else if (whatsappCollectChild) {
                                throw new AssertionError("old WhatsApp collect child route must be replaced: " + recoveredMenu);
                            } else if (whatsappCollectTabChild) {
                                if (!recoveredMenu.optString("localCode").startsWith("/pc/dataCollect/collectionTask?modal=")
                                        || !"JSinglepage".equals(recoveredMenu.optString("linkUrl"))
                                        || !recoveredMenu.optString("evidence").contains("m5d11-menu-tab:dataCollect:")) {
                                    throw new AssertionError("bad WhatsApp collect tab child route: " + recoveredMenu);
                                }
                            } else if (whatsappDataParent) {
                                if (!"JSinglepage".equals(recoveredMenu.optString("localCode"))
                                        || !"/pc/aicloud/my".equals(recoveredMenu.optString("linkUrl"))
                                        || !"original-i18n".equals(recoveredMenu.optString("evidence"))) {
                                    throw new AssertionError("bad WhatsApp AI data original route: " + recoveredMenu);
                                }
                            } else if (whatsappDataChild) {
                                if (!"/pc/aicloud/my".equals(recoveredMenu.optString("localCode"))
                                        || !"JSinglepage:/pc/aicloud/my".equals(recoveredMenu.optString("linkUrl"))
                                        || !recoveredMenu.optString("evidence").contains("recovery-route-child:j2026-h-field-map:aicloud-my")) {
                                    throw new AssertionError("bad WhatsApp AI data child route: " + recoveredMenu);
                                }
                            } else if (whatsappFilterParent) {
                                if (!"JSinglepage".equals(recoveredMenu.optString("localCode"))
                                        || !"/ws/wsfilter/home".equals(recoveredMenu.optString("linkUrl"))
                                        || !recoveredMenu.optString("evidence").contains("recovery-route:wsfilter-home")) {
                                    throw new AssertionError("bad WhatsApp AI filter recovery route: " + recoveredMenu);
                                }
                            } else if (whatsappFilterChild) {
                                if (!"/ws/wsfilter/home".equals(recoveredMenu.optString("localCode"))
                                        || !"JSinglepage:/ws/wsfilter/home".equals(recoveredMenu.optString("linkUrl"))
                                        || !recoveredMenu.optString("evidence").contains("recovery-route-child:j2026-h-field-map:wsfilter-home")) {
                                    throw new AssertionError("bad WhatsApp AI filter child route: " + recoveredMenu);
                                }
                            } else if (whatsappKefuParent) {
                                if (!"https://web.whatsapp.com".equals(recoveredMenu.optString("localCode"))
                                        || !"https://web.whatsapp.com".equals(recoveredMenu.optString("linkUrl"))
                                        || !recoveredMenu.optString("evidence").contains("recovery-route:aggregation-kefu")) {
                                    throw new AssertionError("bad WhatsApp AI kefu recovery route: " + recoveredMenu);
                                }
                            } else if (whatsappKefuChild) {
                                if (!"https://web.whatsapp.com".equals(recoveredMenu.optString("localCode"))
                                        || !"https://web.whatsapp.com".equals(recoveredMenu.optString("linkUrl"))
                                        || !recoveredMenu.optString("evidence").contains("recovery-route-child:j2026-h-field-map:whatsapp-web")) {
                                    throw new AssertionError("bad WhatsApp AI kefu child route: " + recoveredMenu);
                                }
                            } else if (d4TkLocalParent) {
                                if (!"JSinglepage".equals(recoveredMenu.optString("localCode")) || !recoveredMenu.optString("linkUrl").startsWith("/pc/local/tiktok/") || recoveredMenu.optString("linkUrl").contains("http")) throw new AssertionError("bad D-4 TikTok local parent route: " + recoveredMenu);
                            } else if (d4TkLocalChild) {
                                if (!recoveredMenu.optString("localCode").startsWith("/pc/local/tiktok/") || !("JSinglepage:" + recoveredMenu.optString("localCode")).equals(recoveredMenu.optString("linkUrl")) || recoveredMenu.optInt("treeEndFlg") != 1) throw new AssertionError("bad D-4 TikTok local child route: " + recoveredMenu);
                            } else if (d5TgLocalParent) {
                                if (!"JSinglepage".equals(recoveredMenu.optString("localCode")) || !recoveredMenu.optString("linkUrl").startsWith("/pc/local/tg/") || recoveredMenu.optString("linkUrl").contains("http")) throw new AssertionError("bad D-5 Telegram local parent route: " + recoveredMenu);
                            } else if (d5TgLocalChild) {
                                if (!recoveredMenu.optString("localCode").startsWith("/pc/local/tg/") || !("JSinglepage:" + recoveredMenu.optString("localCode")).equals(recoveredMenu.optString("linkUrl")) || recoveredMenu.optInt("treeEndFlg") != 1) throw new AssertionError("bad D-5 Telegram local child route: " + recoveredMenu);
                            } else if (d5GeoLocalParent) {
                                if (!"JSinglepage".equals(recoveredMenu.optString("localCode")) || !recoveredMenu.optString("linkUrl").startsWith("/pc/local/geo/") || recoveredMenu.optString("linkUrl").contains("http")) throw new AssertionError("bad D-5 GEO local parent route: " + recoveredMenu);
                            } else if (d5GeoLocalChild) {
                                if (!recoveredMenu.optString("localCode").startsWith("/pc/local/geo/") || !("JSinglepage:" + recoveredMenu.optString("localCode")).equals(recoveredMenu.optString("linkUrl")) || recoveredMenu.optInt("treeEndFlg") != 1) throw new AssertionError("bad D-5 GEO local child route: " + recoveredMenu);
                            } else if (d5WaLocalParent) {
                                if (!"JSinglepage".equals(recoveredMenu.optString("localCode")) || !recoveredMenu.optString("linkUrl").startsWith("/pc/local/wa/") || recoveredMenu.optString("linkUrl").contains("http")) throw new AssertionError("bad D-5 WA local parent route: " + recoveredMenu);
                            } else if (d5WaLocalChild) {
                                if (!recoveredMenu.optString("localCode").startsWith("/pc/local/wa/") || !("JSinglepage:" + recoveredMenu.optString("localCode")).equals(recoveredMenu.optString("linkUrl")) || recoveredMenu.optInt("treeEndFlg") != 1) throw new AssertionError("bad D-5 WA local child route: " + recoveredMenu);
                            } else if (d3FbLocalParent) {
                                if (!"JSinglepage".equals(recoveredMenu.optString("localCode"))
                                        || !recoveredMenu.optString("linkUrl").startsWith("/pc/local/fb/")
                                        || recoveredMenu.optString("linkUrl").contains("http")
                                        || !recoveredMenu.optString("evidence").startsWith("d3-fb-local:")) {
                                    throw new AssertionError("bad D-3 Facebook local parent route: " + recoveredMenu);
                                }
                            } else if (d3FbLocalChild) {
                                if (!recoveredMenu.optString("localCode").startsWith("/pc/local/fb/")
                                        || !("JSinglepage:" + recoveredMenu.optString("localCode")).equals(recoveredMenu.optString("linkUrl"))
                                        || recoveredMenu.optInt("treeEndFlg") != 1) {
                                    throw new AssertionError("bad D-3 Facebook local child route: " + recoveredMenu);
                                }
                            } else if (d2InsLocalParent) {
                                if (!"JSinglepage".equals(recoveredMenu.optString("localCode"))
                                        || !recoveredMenu.optString("linkUrl").startsWith("/pc/local/ins/")
                                        || recoveredMenu.optString("linkUrl").contains("http")
                                        || !recoveredMenu.optString("evidence").startsWith("d2-ins-local:")) {
                                    throw new AssertionError("bad D-2 Ins local parent route: " + recoveredMenu);
                                }
                            } else if (d2InsLocalChild) {
                                if (!recoveredMenu.optString("localCode").startsWith("/pc/local/ins/")
                                        || !("JSinglepage:" + recoveredMenu.optString("localCode")).equals(recoveredMenu.optString("linkUrl"))
                                        || recoveredMenu.optInt("treeEndFlg") != 1) {
                                    throw new AssertionError("bad D-2 Ins local child route: " + recoveredMenu);
                                }
                            } else if (d1XLocalParent) {
                                if (!"JSinglepage".equals(recoveredMenu.optString("localCode"))
                                        || !recoveredMenu.optString("linkUrl").startsWith("/pc/local/x/")
                                        || recoveredMenu.optString("linkUrl").contains("http")
                                        || !recoveredMenu.optString("evidence").startsWith("d1-x-local:")) {
                                    throw new AssertionError("bad D-1 X local parent route: " + recoveredMenu);
                                }
                            } else if (d1XLocalChild) {
                                if (!recoveredMenu.optString("localCode").startsWith("/pc/local/x/")
                                        || !("JSinglepage:" + recoveredMenu.optString("localCode")).equals(recoveredMenu.optString("linkUrl"))
                                        || recoveredMenu.optInt("treeEndFlg") != 1) {
                                    throw new AssertionError("bad D-1 X local child route: " + recoveredMenu);
                                }
                            } else if (whatsappAdvertisingParent) {
                                if (!"JSinglepage".equals(recoveredMenu.optString("localCode"))
                                        || !"/views/overseasAds/dataBoard".equals(recoveredMenu.optString("linkUrl"))
                                        || !recoveredMenu.optString("evidence").contains("c67-advertising")) {
                                    throw new AssertionError("bad WhatsApp advertising parent route: " + recoveredMenu);
                                }
                            } else if (whatsappAdvertisingChild) {
                                if (!"/views/overseasAds/dataBoard".equals(recoveredMenu.optString("localCode"))
                                        || !"JSinglepage".equals(recoveredMenu.optString("linkUrl"))
                                        || recoveredMenu.optInt("treeEndFlg") != 1) {
                                    throw new AssertionError("bad WhatsApp advertising child route: " + recoveredMenu);
                                }
                            } else if (c5PlatformParent) {
                                if (!"JSinglepage".equals(recoveredMenu.optString("localCode"))
                                        || !recoveredMenu.optString("linkUrl").startsWith("/")
                                        || recoveredMenu.optString("linkUrl").contains("http")) {
                                    throw new AssertionError("bad C5 platform parent route: " + recoveredMenu);
                                }
                            } else if (c5PlatformChild) {
                                if (!recoveredMenu.optString("localCode").startsWith("/")
                                        || !"JSinglepage".equals(recoveredMenu.optString("linkUrl"))
                                        || recoveredMenu.optInt("treeEndFlg") != 1) {
                                    throw new AssertionError("bad C5 platform child route: " + recoveredMenu);
                                }
                            } else if (!"JSinglepage".equals(recoveredMenu.optString("localCode"))
                                    || !"/pc/aicloud/my".equals(recoveredMenu.optString("linkUrl"))) {
                                throw new AssertionError("bad recovered menu route: " + recoveredMenu);
                            }
                        }
                        String[] themeColorKeys = {
                            "primary_color",
                            "secondary_color",
                            "menuAreaBackground",
                            "menuItemDefaultTextColor",
                            "menuItemHoverTextColor",
                            "menuItemHoverBackgroundColor",
                            "menuItemSelectedTextColor",
                            "menuItemSelectedBackgroundColor",
                            "topBarBackground",
                            "topBarDefaultTextColor",
                            "topMenuItemHoverTextColor",
                            "topMenuItemHoverBackgroundColor",
                            "topMenuItemSelectedTextColor",
                            "topMenuItemSelectedBackgroundColor",
                            "defaultBtnFontColor",
                            "defaultBtnBackgroundColor"
                        };
                        for (String key : themeColorKeys) {
                            String color = product.optString(key);
                            if (color.length() == 0) {
                                throw new AssertionError("missing product theme color " + key + ": " + product);
                            }
                            Color.decode(color);
                        }
                        System.out.println(new JSONObject().put("ok", true));
                    }
                }
                """
            ).strip(),
            encoding="utf-8",
        )
        subprocess.run(
            [
                str(JAVAC),
                "-encoding",
                "UTF-8",
                "-cp",
                classpath(self.output_jar, JSON_JAR),
                "-d",
                str(self.probe_classes),
                str(probe_source),
            ],
            cwd=ROOT,
            check=True,
        )
        probe = subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.probe_classes, self.output_jar, JSON_JAR, DATA_LIBS),
                "M4AuthPatchProbe",
            ],
            cwd=ROOT,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("M4_DIAG_MENU_K_CALLED resp=", probe.stdout)
        self.assertIn("M4_DIAG_MENU_K_CALLER", probe.stdout)
        self.assertEqual(json.loads(probe.stdout.splitlines()[-1]), {"ok": True})

    def test_local_spider_bridge_writes_mock_result_and_submits_local_collect_tasks(self):
        self.compile_patcher()

        result = self.run_patcher()

        self.assertEqual(result.returncode, 0, result.stderr)
        with zipfile.ZipFile(self.output_jar) as patched:
            self.assertIn(
                "com/sbf/main/jxbrowser/M5LocalSpiderBridge.class",
                patched.namelist(),
            )

        probe_source = self.tmp_path / "M5LocalSpiderBridgeProbe.java"
        probe_source.write_text(
            textwrap.dedent(
                """
                import com.sbf.main.jxbrowser.M5LocalSpiderBridge;
                import java.nio.file.Files;
                import java.nio.file.Path;
                import java.nio.file.Paths;
                import java.sql.Connection;
                import java.sql.DriverManager;
                import java.sql.ResultSet;
                import java.sql.Statement;
                import org.json.JSONArray;
                import org.json.JSONObject;

                public class M5LocalSpiderBridgeProbe {
                    private static final class FakeSpider {
                        private final Long d;
                        private final String e;
                        private final String i;

                        private FakeSpider(long taskId, String spiderCode, String moduleCode) {
                            this.d = Long.valueOf(taskId);
                            this.e = spiderCode;
                            this.i = moduleCode;
                        }
                    }

                    public static void main(String[] args) throws Exception {
                        Path baseDir = Paths.get(args[0]);
                        String emptyQueue = M5LocalSpiderBridge.getNewTask(baseDir.toString(), "whatsapp", 0);
                        JSONArray queue = new JSONArray(emptyQueue);
                        if (queue.length() != 0) {
                            throw new AssertionError("local queue must start empty: " + queue);
                        }

                        JSONObject preview = new JSONObject(
                                M5LocalSpiderBridge.previewTask(
                                        "whatsapp",
                                        "whatsapp_users_lists",
                                        "{\\"googSite\\":\\"google.com\\",\\"keywords\\":\\"local-test\\"}"));
                        if (!preview.optBoolean("dryRun")
                                || preview.optBoolean("submitted")
                                || !preview.optString("taskId").startsWith("local-preview-")) {
                            throw new AssertionError("preview must not submit: " + preview);
                        }

                        JSONObject writeResult = new JSONObject(
                                M5LocalSpiderBridge.writeMockResult(
                                        baseDir.toString(),
                                        "whatsapp",
                                        "whatsapp_users_lists",
                                        "{\\"phone\\":\\"+10000000000\\",\\"source\\":\\"local-mock\\"}"));
                        if (writeResult.optInt("code") != 200
                                || writeResult.optInt("total") != 1
                                || writeResult.optBoolean("submitted")) {
                            throw new AssertionError("write result shape: " + writeResult);
                        }

                        JSONObject submit = new JSONObject(
                                M5LocalSpiderBridge.submitTask(
                                        baseDir.toString(),
                                        "whatsapp",
                                        "whatsapp_users_lists",
                                        "{\\"googSite\\":\\"google.com\\",\\"areaCode\\":\\"+1\\",\\"pltCode\\":\\"facebook.com\\",\\"keywords\\":\\"soccer jersey\\"}",
                                        "{\\"cloudServer\\":\\"local\\",\\"spiderMode\\":\\"google\\",\\"cookie\\":\\"AEC=test-cookie\\",\\"proxy\\":\\"socks5://127.0.0.1:12324\\",\\"spider_app_code\\":\\"whatsapp\\",\\"spider_exe_code\\":\\"whatsapp_users_lists\\"}"));
                        if (submit.optInt("code") != 200
                                || !submit.optBoolean("submitted")
                                || submit.optLong("taskId") <= 0
                                || !submit.optString("entry").contains("cloud.spider.a.a")) {
                            throw new AssertionError("submit result shape: " + submit);
                        }

                        JSONObject tasks = new JSONObject(
                                M5LocalSpiderBridge.listTasks(
                                        baseDir.toString(), "whatsapp", "whatsapp_users_lists"));
                        if (tasks.optInt("total") != 1
                                || tasks.getJSONArray("rows").getJSONObject(0).optLong("taskId")
                                        != submit.optLong("taskId")
                                || tasks.getJSONArray("rows").getJSONObject(0).optInt("status") != 0
                                || tasks.getJSONArray("rows").getJSONObject(0).optInt("retryCount") != 0) {
                            throw new AssertionError("local task list shape: " + tasks);
                        }
                        Path jtaskDb = baseDir.resolve("data").resolve("db_jtable_jrpatask.data");
                        if (!Files.exists(jtaskDb)) {
                            throw new AssertionError("missing reused JTask db: " + jtaskDb);
                        }
                        Path legacyQueueDb = baseDir.resolve("data")
                                .resolve("whatsappdata")
                                .resolve("db_local_spider_tasks.data");
                        if (Files.exists(legacyQueueDb)) {
                            throw new AssertionError("must not create parallel local task db: " + legacyQueueDb);
                        }

                        JSONArray claimed = new JSONArray(
                                M5LocalSpiderBridge.getNewTask(baseDir.toString(), "whatsapp", 0));
                        if (claimed.length() != 1
                                || claimed.getJSONObject(0).optLong("taskId") != submit.optLong("taskId")
                                || !"whatsapp_users_lists".equals(claimed.getJSONObject(0).optString("spiderCode"))
                                || !claimed.getJSONObject(0).toString().contains("facebook.com")
                                || !claimed.getJSONObject(0).toString().contains("soccer jersey")) {
                            throw new AssertionError("claimed task shape: " + claimed);
                        }
                        JSONObject running = new JSONObject(
                                M5LocalSpiderBridge.listTasks(
                                        baseDir.toString(), "whatsapp", "whatsapp_users_lists"));
                        if (running.getJSONArray("rows").getJSONObject(0).optInt("status") != 1
                                || running.getJSONArray("rows").getJSONObject(0).optInt("retryCount") != 1
                                || !running.getJSONArray("rows").getJSONObject(0).optString("message").contains("running")) {
                            throw new AssertionError("running task list shape: " + running);
                        }
                        M5LocalSpiderBridge.finishDispatchedTask(
                                baseDir.toString(), submit.optLong("taskId"), true, "executor returned");
                        JSONObject finished = new JSONObject(
                                M5LocalSpiderBridge.listTasks(
                                        baseDir.toString(), "whatsapp", "whatsapp_users_lists"));
                        if (finished.getJSONArray("rows").getJSONObject(0).optInt("status") != 2
                                || !finished.getJSONArray("rows").getJSONObject(0).optString("message").contains("executor returned")) {
                            throw new AssertionError("finished task list shape: " + finished);
                        }
                        boolean collected = M5LocalSpiderBridge.postCollectedData(
                                baseDir.toString(),
                                new FakeSpider(
                                        submit.optLong("taskId"),
                                        "whatsapp_users_lists",
                                        "whatsapp"),
                                "{\\"phone\\":\\"+19998887777\\",\\"source\\":\\"google-real\\",\\"url\\":\\"https://www.facebook.com/example\\"}");
                        if (!collected) {
                            throw new AssertionError("postCollectedData must report local write success");
                        }
                        JSONObject collectedList = new JSONObject(
                                M5LocalSpiderBridge.listTasks(
                                        baseDir.toString(), "whatsapp", "whatsapp_users_lists"));
                        if (collectedList.getJSONArray("rows").getJSONObject(0).optLong("total") != 2
                                || !collectedList.getJSONArray("rows").getJSONObject(0).optString("message").contains("local postData")) {
                            throw new AssertionError("collected task list shape: " + collectedList);
                        }
                        M5LocalSpiderBridge.endCollectedTask(
                                baseDir.toString(),
                                new FakeSpider(
                                        submit.optLong("taskId"),
                                        "whatsapp_users_lists",
                                        "whatsapp"));
                        JSONObject endCollected = new JSONObject(
                                M5LocalSpiderBridge.listTasks(
                                        baseDir.toString(), "whatsapp", "whatsapp_users_lists"));
                        if (endCollected.getJSONArray("rows").getJSONObject(0).optInt("status") != 2
                                || !endCollected.getJSONArray("rows").getJSONObject(0).optString("message").contains("local endTask")) {
                            throw new AssertionError("endCollected task list shape: " + endCollected);
                        }
                        JSONArray secondClaim = new JSONArray(
                                M5LocalSpiderBridge.getNewTask(baseDir.toString(), "whatsapp", 0));
                        if (secondClaim.length() != 0) {
                            throw new AssertionError("running task must not be claimed twice: " + secondClaim);
                        }

                        JSONObject envelope = new JSONObject(
                                M5LocalSpiderBridge.getTask(baseDir.toString(), submit.optLong("taskId")));
                        JSONObject task = envelope.optJSONObject("task");
                        JSONObject spider = envelope.optJSONObject("spider");
                        JSONObject taskData = envelope.optJSONObject("task_data");
                        JSONObject taskInfo = envelope.optJSONObject("task_info");
                        if (task == null
                                || spider == null
                                || taskData == null
                                || taskInfo == null
                                || !"whatsapp_users_lists".equals(spider.optString("code"))
                                || task.optJSONArray("spiderParams") == null
                                || task.optJSONArray("spiderParams").length() != 4
                                || !task.optJSONArray("spiderParams").toString().contains("facebook.com")
                                || !task.optJSONArray("spiderParams").toString().contains("soccer jersey")
                                || !task.optString("steps").contains("ggSite")
                                || !task.optString("steps").contains("allkeywords")
                                || !task.optString("fields").contains("phone")
                                || !task.optString("taskConfig").contains("local")) {
                            throw new AssertionError("task envelope shape: " + envelope);
                        }
                        JSONObject googSiteParam = task.optJSONArray("spiderParams").getJSONObject(0);
                        if (!"googSite".equals(googSiteParam.optString("key"))
                                || !"google.com".equals(googSiteParam.optString("code"))) {
                            throw new AssertionError("spiderParams must use original key/code value shape: "
                                    + task.optJSONArray("spiderParams"));
                        }
                        if (!"whatsapp_users_lists".equals(taskData.optString("spiderCode"))
                                || !"whatsapp".equals(taskData.optString("moduleCode"))
                                || !taskData.optString("taskConfig").contains("local")
                                || taskData.optJSONArray("spiderParams") == null
                                || !"google.com".equals(taskData.optString("googSite"))
                                || !"+1".equals(taskData.optString("areaCode"))
                                || !"facebook.com".equals(taskData.optString("pltCode"))
                                || !"soccer jersey".equals(taskData.optString("keywords"))) {
                            throw new AssertionError("task_data must preserve puncture params: " + envelope);
                        }
                        if (!"whatsapp_users_lists".equals(taskInfo.optString("spiderCode"))
                                || !"whatsapp".equals(taskInfo.optString("moduleCode"))
                                || !"google".equals(taskInfo.optString("spiderMode"))
                                || !"AEC=test-cookie".equals(taskInfo.optString("cookie"))
                                || !"socks5://127.0.0.1:12324".equals(taskInfo.optString("proxy"))
                                || !"whatsapp".equals(taskInfo.optString("spider_app_code"))
                                || !"whatsapp_users_lists".equals(taskInfo.optString("spider_exe_code"))) {
                            throw new AssertionError("task_info must be runner-shaped: " + envelope);
                        }
                        M5LocalSpiderBridge.updateTaskStatus(
                                baseDir.toString(), submit.optLong("taskId"), 2, "done", Long.valueOf(3));
                        JSONObject updated = new JSONObject(
                                M5LocalSpiderBridge.listTasks(
                                        baseDir.toString(), "whatsapp", "whatsapp_users_lists"));
                        if (updated.getJSONArray("rows").getJSONObject(0).optInt("status") != 2
                                || updated.getJSONArray("rows").getJSONObject(0).optLong("total") != 3
                                || !updated.getJSONArray("rows").getJSONObject(0).optString("message").contains("done")) {
                            throw new AssertionError("updated task list shape: " + updated);
                        }

                        JSONObject cancelSubmit = new JSONObject(
                                M5LocalSpiderBridge.submitTask(
                                        baseDir.toString(),
                                        "whatsapp",
                                        "whatsapp_users_lists",
                                        "{\\"googSite\\":\\"google.com\\",\\"areaCode\\":\\"+1\\",\\"pltCode\\":\\"google.com\\",\\"keywords\\":\\"cancel-me\\"}",
                                        "{\\"cloudServer\\":\\"local\\"}"));
                        JSONObject cancel = new JSONObject(M5LocalSpiderBridge.cancelAllRun(baseDir.toString(), "whatsapp"));
                        if (cancel.optInt("code") != 200 || cancel.optInt("cancelled") < 1) {
                            throw new AssertionError("cancelAllRun result shape: " + cancel);
                        }
                        JSONArray afterCancelClaim = new JSONArray(
                                M5LocalSpiderBridge.getNewTask(baseDir.toString(), "whatsapp", 0));
                        if (afterCancelClaim.length() != 0) {
                            throw new AssertionError("cancelled task must not be claimable: " + afterCancelClaim);
                        }
                        JSONObject cancelledList = new JSONObject(
                                M5LocalSpiderBridge.listTasks(
                                        baseDir.toString(), "whatsapp", "whatsapp_users_lists"));
                        if (cancelledList.getJSONArray("rows").getJSONObject(0).optLong("taskId")
                                        != cancelSubmit.optLong("taskId")
                                || cancelledList.getJSONArray("rows").getJSONObject(0).optInt("status") != -2) {
                            throw new AssertionError("cancelled task list shape: " + cancelledList);
                        }

                        JSONObject areaOptions = new JSONObject(M5LocalSpiderBridge.platformOptions("area_code"));
                        JSONObject platformOptions = new JSONObject(M5LocalSpiderBridge.platformOptions("platform"));
                        if (!areaOptions.toString().contains("+1")
                                || !platformOptions.toString().contains("facebook.com")
                                || !platformOptions.toString().contains("google.com")) {
                            throw new AssertionError("missing local platform options");
                        }

                        Path db = baseDir.resolve("data")
                                .resolve("whatsappdata")
                                .resolve("db_spider_data_whatsapp_users_lists.data");
                        if (!Files.exists(db)) {
                            throw new AssertionError("missing spider data db: " + db);
                        }
                        Class.forName("org.sqlite.JDBC");
                        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + db.toAbsolutePath());
                                Statement stmt = conn.createStatement();
                                ResultSet rs = stmt.executeQuery(
                                        "select spider_modal, spider_code, json_data from spider_data order by id asc")) {
                            if (!rs.next()) {
                                throw new AssertionError("missing inserted spider row");
                            }
                            if (!"whatsapp".equals(rs.getString(1))
                                    || !"whatsapp_users_lists".equals(rs.getString(2))
                                    || !rs.getString(3).contains("local-mock")) {
                                throw new AssertionError("bad inserted row");
                            }
                            if (rs.next()) {
                                if (!rs.getString(3).contains("google-real")) {
                                    throw new AssertionError("bad collected row: " + rs.getString(3));
                                }
                            } else {
                                throw new AssertionError("missing collected postData row");
                            }
                            if (rs.next()) {
                                throw new AssertionError("unexpected extra spider rows");
                            }
                        }

                        System.out.println("M5_LOCAL_SPIDER_BRIDGE_OK");
                    }
                }
                """
            ).strip(),
            encoding="utf-8",
        )
        subprocess.run(
            [
                str(JAVAC),
                "-encoding",
                "UTF-8",
                "-cp",
                classpath(self.output_jar, JSON_JAR),
                "-d",
                str(self.probe_classes),
                str(probe_source),
            ],
            cwd=ROOT,
            check=True,
        )
        probe = subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.probe_classes, self.output_jar, JSON_JAR, DATA_LIBS),
                "M5LocalSpiderBridgeProbe",
                str(self.tmp_path / "runtime"),
            ],
            cwd=ROOT,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("M5_LOCAL_SPIDER_BRIDGE_OK", probe.stdout)

    def test_real_product_menu_logging_mode_preserves_original_json_calls(self):
        self.compile_patcher()

        result = self.run_patcher_evidence_mode()

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertTrue(self.output_jar.exists())
        product_block = self.javap_method_block("public static org.json.JSONObject C();")
        menu_block = self.javap_method_block("public static org.json.JSONObject k();")
        login_block = self.javap_method_block(
            "public static org.json.JSONObject k(java.lang.String, java.lang.String);"
        )
        get_info_block = self.javap_method_block("public static org.json.JSONObject h(java.lang.String);")

        self.assertIn("M4_EVIDENCE_PRODUCT_MODULE_REAL_JSON=", product_block)
        self.assertIn("M4_EVIDENCE_PC_MENUS_REAL_JSON=", menu_block)
        self.assertIn("M4_EVIDENCE_PC_MENUS_RAW_BODY=", menu_block)
        self.assertIn("M4_EVIDENCE_PC_MENUS_REQUEST_URL=", menu_block)
        self.assertIn("M4_EVIDENCE_PC_MENUS_REQUEST_JSON=", menu_block)
        self.assertIn("M4_EVIDENCE_PC_MENUS_REQUEST_BODY=", menu_block)
        self.assertIn("M4_EVIDENCE_PC_MENUS_STATIC_A=", menu_block)
        self.assertIn("M4_EVIDENCE_PC_MENUS_STATIC_K=", menu_block)
        self.assertIn("M4_EVIDENCE_PC_MENUS_STATIC_L=", menu_block)
        self.assertIn("M4_EVIDENCE_PC_MENUS_HEADER_E=", menu_block)
        self.assertIn("invokedynamic", product_block)
        self.assertIn("invokedynamic", menu_block)
        self.assertNotIn("M4_DIAG_MENU_K_CALLED resp=", menu_block)
        self.assertNotIn("C28500001", product_block)
        self.assertNotIn("C28500001", menu_block)
        self.assertIn("offline-local-token-1234567890", login_block)
        self.assertIn("im", get_info_block)
        self.assertIn("udp", get_info_block)

    def test_local_pipeline_initializes_original_cloud_spider_context(self):
        source = LOCAL_SPIDER_BRIDGE_SOURCE.read_text(encoding="utf-8")

        self.assertIn(
            'Class.forName("com.sbf.main.spide.cloud.JSpiderCloude")',
            source,
        )
        self.assertIn(
            '"https://app.xdxsoft.com/pc/cloudSpider?spiderCode="',
            source,
        )
        self.assertIn('masterClass.getMethod("a").invoke(null)', source)
        self.assertIn("registryField.get(master)", source)
        self.assertIn("ensureDirectCloudSpiderContext(spiderCode)", source)
        self.assertIn("M5D_CLOUD_SPIDER_CONTEXT_ORIGINAL_FAILED", source)
        self.assertIn('Class.forName("com.sbf.main.cloud.spider.a")', source)
        self.assertIn("SPIDER_RUNNER_MODE_EXTERNAL_SEARCH", source)
        self.assertIn("getConstructor(String.class)", source)
        self.assertIn(".newInstance(SPIDER_RUNNER_MODE_EXTERNAL_SEARCH)", source)
        self.assertNotIn('runnerClass.getConstructor(String.class).newInstance("google")', source)
        self.assertIn("runners.put(SPIDER_RUNNER_MODE_EXTERNAL_SEARCH, runner)", source)
        self.assertIn("runners.put(spiderCode, runner)", source)
        self.assertIn("getRegisteredCloudSpiderRunner(spiderCode)", source)
        self.assertIn("localCloudSpiderContext", source)
        self.assertIn("ensureCloudSpiderContext(spiderCode)", source)
        self.assertIn("InvocationTargetException", source)
        self.assertIn("rootCause(error).printStackTrace(System.out)", source)

    def test_c6_commerce_local_pages_are_stable_empty_states(self):
        self.compile_patcher()
        probe_source = self.tmp_path / "C6CommerceLocalPagesProbe.java"
        probe_source.write_text(
            textwrap.dedent(
                """
                import com.sbf.main.jxbrowser.M5LocalSpiderBridge;

                public class C6CommerceLocalPagesProbe {
                    private static void requireContains(String url, String... needles)
                            throws Exception {
                        String body = M5LocalSpiderBridge.localWebAssetBody(url);
                        if (body == null || body.length() == 0) {
                            throw new AssertionError("missing C6 local page: " + url);
                        }
                        for (String needle : needles) {
                            if (!body.contains(needle)) {
                                throw new AssertionError(url + " missing " + needle);
                            }
                        }
                        if (body.matches("(?s).*\\\\b(balance|余额)\\\\s*[:：]\\\\s*[-+]?\\\\d+.*")) {
                            throw new AssertionError(url + " contains a synthetic balance");
                        }
                    }

                    public static void main(String[] args) throws Exception {
                        requireContains(
                                "https://app.xdxsoft.com/pc/c6/recharge",
                                "C6_RECHARGE_UI",
                                "data-c6-action=\\\"disabled\\\"",
                                "支付与订单功能不可用");
                        requireContains(
                                "https://app.xdxsoft.com/pc/c6/advertising",
                                "C6_ADVERTISING_UI",
                                "data-c6-action=\\\"disabled\\\"",
                                "广告计划、授权与投放功能不可用");
                        System.out.println("C6_COMMERCE_LOCAL_PAGES_OK");
                    }
                }
                """
            ).strip(),
            encoding="utf-8",
        )
        subprocess.run(
            [
                str(JAVAC),
                "-encoding",
                "UTF-8",
                "-cp",
                classpath(self.classes, JSON_JAR, DATA_LIBS),
                "-d",
                str(self.probe_classes),
                str(probe_source),
            ],
            cwd=ROOT,
            check=True,
        )
        probe = subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.probe_classes, self.classes, JSON_JAR, DATA_LIBS),
                "C6CommerceLocalPagesProbe",
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("C6_COMMERCE_LOCAL_PAGES_OK", probe.stdout)

    def test_c6_commerce_original_routes_resolve_to_exact_empty_pages(self):
        self.compile_patcher()
        probe_source = self.tmp_path / "C6CommerceRouteProbe.java"
        probe_source.write_text(
            textwrap.dedent(
                """
                import com.sbf.main.jxbrowser.M5LocalSpiderBridge;

                public class C6CommerceRouteProbe {
                    private static void requireMarker(String url, String marker) throws Exception {
                        String body = M5LocalSpiderBridge.localWebAssetBody(url);
                        if (body == null || !body.contains(marker)) {
                            throw new AssertionError(url + " did not resolve to " + marker);
                        }
                    }

                    public static void main(String[] args) throws Exception {
                        requireMarker(
                                "https://app.xdxsoft.com/pc/alipay/enterpriseAuth",
                                "C6_RECHARGE_UI");
                        requireMarker(
                                "https://app.xdxsoft.com/pc/alipay/personal/auth",
                                "C6_RECHARGE_UI");
                        requireMarker(
                                "https://app.xdxsoft.com/pc/userPayofflineOrder/my",
                                "C6_RECHARGE_UI");
                        requireMarker(
                                "https://app.xdxsoft.com/views/overseasAds/dataBoard",
                                "C6_ADVERTISING_UI");
                        requireMarker(
                                "https://app.xdxsoft.com/views/overseasAds/adsPeople",
                                "C6_ADVERTISING_UI");
                        requireMarker(
                                "https://app.xdxsoft.com/views/overseasAds/addTask",
                                "C6_ADVERTISING_UI");
                        if (M5LocalSpiderBridge.localWebAssetBody(
                                        "https://app.xdxsoft.com/pc/aicloud/my")
                                .contains("C6_RECHARGE_UI")) {
                            throw new AssertionError("C6 recharge mapping escaped its exact routes");
                        }
                        System.out.println("C6_COMMERCE_ROUTE_MAPPING_OK");
                    }
                }
                """
            ).strip(),
            encoding="utf-8",
        )
        subprocess.run(
            [
                str(JAVAC),
                "-encoding",
                "UTF-8",
                "-cp",
                classpath(self.classes, JSON_JAR, DATA_LIBS),
                "-d",
                str(self.probe_classes),
                str(probe_source),
            ],
            cwd=ROOT,
            check=True,
        )
        probe = subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.probe_classes, self.classes, JSON_JAR, DATA_LIBS),
                "C6CommerceRouteProbe",
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("C6_COMMERCE_ROUTE_MAPPING_OK", probe.stdout)

    def test_c62_blocks_unknown_external_http_requests_without_breaking_local_assets(self):
        self.compile_patcher()
        probe_source = self.tmp_path / "C62ExternalRequestBlockProbe.java"
        probe_source.write_text(
            textwrap.dedent(
                """
                import com.sbf.main.jxbrowser.M5LocalSpiderBridge;

                public class C62ExternalRequestBlockProbe {
                    private static void requireBlocked(String url) throws Exception {
                        String body = M5LocalSpiderBridge.localWebAssetBody(url);
                        if (body == null || !body.contains("C62_EXTERNAL_REQUEST_BLOCKED")) {
                            throw new AssertionError("external request escaped local bridge: " + url);
                        }
                    }

                    public static void main(String[] args) throws Exception {
                        requireBlocked("https://app.xdxsoft.com/static/js/c62-not-mirrored.js");
                        requireBlocked("https://39.101.114.44/unknown");
                        requireBlocked("http://163.181.39.181/unknown");

                        String known = M5LocalSpiderBridge.localWebAssetBody(
                                "https://app.xdxsoft.com/pc/alipay/enterpriseAuth");
                        if (known == null || !known.contains("C6_RECHARGE_UI")) {
                            throw new AssertionError("known C6 route was not preserved");
                        }
                        if (M5LocalSpiderBridge.localWebAssetBody(
                                        "http://127.0.0.1:50325/local-probe")
                                != null) {
                            throw new AssertionError("loopback must remain outside the external blocker");
                        }
                        System.out.println("C62_EXTERNAL_REQUEST_BLOCK_OK");
                    }
                }
                """
            ).strip(),
            encoding="utf-8",
        )
        subprocess.run(
            [
                str(JAVAC),
                "-encoding",
                "UTF-8",
                "-cp",
                classpath(self.classes, JSON_JAR, DATA_LIBS),
                "-d",
                str(self.probe_classes),
                str(probe_source),
            ],
            cwd=ROOT,
            check=True,
        )
        probe = subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.probe_classes, self.classes, JSON_JAR, DATA_LIBS),
                "C62ExternalRequestBlockProbe",
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("C62_EXTERNAL_REQUEST_BLOCK_OK", probe.stdout)

    def test_c6_candidate_normalizes_commerce_routes_before_browser_load(self):
        self.compile_patcher()
        result = self.run_patcher()
        self.assertEqual(result.returncode, 0, result.stderr)
        javap = subprocess.run(
            [
                str(JAVAP),
                "-classpath",
                str(self.output_jar),
                "-c",
                "-p",
                "com.sbf.main.jxbrowser.c",
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=True,
        )
        self.assertIn(
            "normalizeC6CommerceRoute",
            javap.stdout,
        )
        bridge_javap = subprocess.run(
            [
                str(JAVAP),
                "-classpath",
                str(self.output_jar),
                "-c",
                "-p",
                "com.sbf.main.jxbrowser.M5LocalSpiderBridge",
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=True,
        )
        self.assertIn("C6_RUNTIME_ROUTE_NORMALIZED", bridge_javap.stdout)

    def test_c67_existing_advertising_menu_dispatches_to_the_overseas_ads_route(self):
        probe = self.compile_and_run_catalog_probe(
            "C67AdvertisingMenuProbe",
            """
            import org.json.JSONArray;
            import org.json.JSONObject;

            public class C67AdvertisingMenuProbe {
                public static void main(String[] args) throws Exception {
                    JSONArray menus = new JSONObject(M4RecoveryCatalog.pcMenusJson()).getJSONArray("scfs");
                    JSONObject advertising = null;
                    for (int index = 0; index < menus.length(); index++) {
                        JSONObject menu = menus.getJSONObject(index);
                        if ("C3460_001".equals(menu.optString("code"))) {
                            advertising = menu;
                            break;
                        }
                    }
                    if (advertising == null) {
                        throw new AssertionError("missing existing advertising menu");
                    }
                    if (!"JSinglepage".equals(advertising.optString("localCode"))
                            || !"/views/overseasAds/dataBoard".equals(advertising.optString("linkUrl"))
                            || !advertising.optString("evidence").contains("c67-advertising")) {
                        throw new AssertionError("advertising parent did not dispatch to original ads route: " + advertising);
                    }
                    JSONObject advertisingChild = null;
                    for (int index = 0; index < menus.length(); index++) {
                        JSONObject menu = menus.getJSONObject(index);
                        if (advertising.optInt("id") == menu.optInt("parentId")
                                && "REC_WHATSAPP_ADVERTISING_ROUTE".equals(menu.optString("code"))) {
                            advertisingChild = menu;
                            break;
                        }
                    }
                    if (advertisingChild == null
                            || !"/views/overseasAds/dataBoard".equals(advertisingChild.optString("localCode"))
                            || !"JSinglepage".equals(advertisingChild.optString("linkUrl"))) {
                        throw new AssertionError("advertising route child is missing: " + advertisingChild);
                    }
                    System.out.println("C67_ADVERTISING_MENU_DISPATCH_OK");
                }
            }
            """,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("C67_ADVERTISING_MENU_DISPATCH_OK", probe.stdout)

    def test_c67_advertising_route_is_served_as_html(self):
        self.compile_patcher()
        probe_source = self.tmp_path / "C67AdvertisingContentTypeProbe.java"
        probe_source.write_text(
            textwrap.dedent(
                """
                import com.sbf.main.jxbrowser.M5LocalSpiderBridge;

                public class C67AdvertisingContentTypeProbe {
                    public static void main(String[] args) {
                        String contentType = M5LocalSpiderBridge.localWebAssetContentType(
                                "https://app.xdxsoft.com/views/overseasAds/dataBoard");
                        if (!contentType.startsWith("text/html")) {
                            throw new AssertionError("advertising route must be HTML: " + contentType);
                        }
                        System.out.println("C67_ADVERTISING_CONTENT_TYPE_OK");
                    }
                }
                """
            ).strip(),
            encoding="utf-8",
        )
        subprocess.run(
            [
                str(JAVAC),
                "-encoding",
                "UTF-8",
                "-cp",
                classpath(self.classes, JSON_JAR, DATA_LIBS),
                "-d",
                str(self.probe_classes),
                str(probe_source),
            ],
            check=True,
            cwd=ROOT,
        )
        result = subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.probe_classes, self.classes, JSON_JAR, DATA_LIBS),
                "C67AdvertisingContentTypeProbe",
            ],
            capture_output=True,
            text=True,
            check=False,
            cwd=ROOT,
        )
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("C67_ADVERTISING_CONTENT_TYPE_OK", result.stdout)

    def test_d1_x_catalog_uses_nine_distinct_explicit_local_leaves(self):
        probe = self.compile_and_run_catalog_probe(
            "D1XCatalogProbe",
            """
            import java.util.HashMap;
            import java.util.HashSet;
            import java.util.Map;
            import java.util.Set;
            import org.json.JSONArray;
            import org.json.JSONObject;

            public class D1XCatalogProbe {
                public static void main(String[] args) throws Exception {
                    String[] codes = {"C4133_002", "C4133_003", "C4133_004", "C4133_005", "C4133_006", "C4133_007", "C4133_008", "C4133_009", "C4133_017"};
                    JSONArray menus = new JSONObject(M4RecoveryCatalog.pcMenusJson()).getJSONArray("scfs");
                    Map<String, JSONObject> parents = new HashMap<String, JSONObject>();
                    for (int i = 0; i < menus.length(); i++) {
                        JSONObject item = menus.getJSONObject(i);
                        for (String code : codes) {
                            if (code.equals(item.optString("code"))) {
                                parents.put(code, item);
                            }
                        }
                    }
                    if (parents.size() != codes.length) {
                        throw new AssertionError("missing X parents: " + parents.keySet());
                    }
                    Set<String> routes = new HashSet<String>();
                    for (String code : codes) {
                        JSONObject parent = parents.get(code);
                        String route = parent.optString("linkUrl");
                        if (!"JSinglepage".equals(parent.optString("localCode"))
                                || !route.startsWith("/pc/local/x/")
                                || !parent.optString("evidence").contains("d1-x-local")) {
                            throw new AssertionError("D-1 X parent route missing: " + parent);
                        }
                        routes.add(route);
                        boolean childFound = false;
                        for (int i = 0; i < menus.length(); i++) {
                            JSONObject child = menus.getJSONObject(i);
                            if (child.optInt("parentId") == parent.optInt("id")
                                    && child.optString("localCode").equals(route)
                                    && child.optString("linkUrl").equals("JSinglepage:" + route)) {
                                childFound = true;
                            }
                        }
                        if (!childFound) {
                            throw new AssertionError("D-1 X explicit leaf missing for " + code);
                        }
                    }
                    if (routes.size() != codes.length) {
                        throw new AssertionError("D-1 X routes are not distinct: " + routes);
                    }
                    System.out.println("D1_X_CATALOG_OK");
                }
            }
            """,
        )
        self.assertEqual(probe.returncode, 0, probe.stdout + probe.stderr)
        self.assertIn("D1_X_CATALOG_OK", probe.stdout)

    def test_d1_x_bridge_serves_distinct_local_pages_with_guarded_operations(self):
        self.compile_patcher()
        probe_source = self.tmp_path / "D1XLocalPagesProbe.java"
        probe_source.write_text(
            textwrap.dedent(
                """
                import com.sbf.main.jxbrowser.M5LocalSpiderBridge;

                public class D1XLocalPagesProbe {
                    public static void main(String[] args) {
                        String[][] pages = {
                            {"account-login", "X 账号登录", "登录 X 账号"},
                            {"precise-search", "X 精准搜索", "提交精准搜索"},
                            {"peer-followers", "X 同行的粉丝搜索", "提交粉丝搜索"},
                            {"active-filter", "X 筛选活跃", "开始活跃筛选"},
                            {"profile-database", "X 主页大数据库", "采集主页数据"},
                            {"android-agent", "X 安卓智能体", "启动安卓智能体"},
                            {"aicloud-fingerprint", "X AiCloud指纹", "绑定 AiCloud 指纹"},
                            {"adspower-fingerprint", "X AdsPower指纹", "绑定 AdsPower 指纹"},
                            {"jump-push", "X 跳推系统", "开始跳推"}
                        };
                        for (String[] page : pages) {
                            String route = "/pc/local/x/" + page[0];
                            String body = M5LocalSpiderBridge.localWebAssetBody("https://app.xdxsoft.com" + route);
                            if (body == null || !body.contains("data-d1-x-route=\\\"" + page[0] + "\\\"")
                                    || !body.contains("D1_X_LOCAL_PAGE") || !body.contains(page[1])
                                    || !body.contains(page[2]) || !body.contains("离线提示")
                                    || !body.contains("data-d1-action=\\\"guarded\\\" disabled")) {
                                throw new AssertionError("missing guarded D-1 local surface for " + route + ": " + body);
                            }
                            if (body.contains("任务批次号") || body.contains("+10000000000")
                                    || body.contains("fingerprintId") || body.contains("mock")) {
                                throw new AssertionError("fake data leaked into " + route);
                            }
                        }
                        System.out.println("D1_X_LOCAL_PAGES_OK");
                    }
                }
                """
            ).strip(),
            encoding="utf-8",
        )
        subprocess.run(
            [str(JAVAC), "-encoding", "UTF-8", "-cp", classpath(self.classes, JSON_JAR, DATA_LIBS), "-d", str(self.probe_classes), str(probe_source)],
            cwd=ROOT,
            check=True,
        )
        probe = subprocess.run(
            [str(JAVA), "-cp", classpath(self.probe_classes, self.classes, JSON_JAR, DATA_LIBS), "D1XLocalPagesProbe"],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        self.assertEqual(probe.returncode, 0, probe.stdout + probe.stderr)
        self.assertIn("D1_X_LOCAL_PAGES_OK", probe.stdout)

    def test_d2_ins_catalog_uses_nine_distinct_explicit_local_leaves(self):
        probe = self.compile_and_run_catalog_probe(
            "D2InsCatalogProbe",
            """
            import java.util.HashMap;
            import java.util.HashSet;
            import java.util.Map;
            import java.util.Set;
            import org.json.JSONArray;
            import org.json.JSONObject;

            public class D2InsCatalogProbe {
                public static void main(String[] args) throws Exception {
                    String[][] expected = {
                        {"C4131_002", "account-login"},
                        {"C4131_003", "account-search"},
                        {"C4131_004", "post-search"},
                        {"C4131_005", "profile-mining"},
                        {"C4131_006", "active-filter"},
                        {"C4131_007", "api-broadcast"},
                        {"C4131_008", "android-agent"},
                        {"C4131_009", "aicloud-fingerprint"},
                        {"C4131_010", "adspower-fingerprint"}
                    };
                    JSONArray menus = new JSONObject(M4RecoveryCatalog.pcMenusJson()).getJSONArray("scfs");
                    Map<String, JSONObject> parents = new HashMap<String, JSONObject>();
                    for (int i = 0; i < menus.length(); i++) {
                        JSONObject item = menus.getJSONObject(i);
                        for (String[] entry : expected) {
                            if (entry[0].equals(item.optString("code"))) {
                                parents.put(entry[0], item);
                            }
                        }
                    }
                    if (parents.size() != expected.length) {
                        throw new AssertionError("missing Ins parents: " + parents.keySet());
                    }
                    Set<String> routes = new HashSet<String>();
                    for (String[] entry : expected) {
                        JSONObject parent = parents.get(entry[0]);
                        String route = "/pc/local/ins/" + entry[1];
                        if (!"JSinglepage".equals(parent.optString("localCode"))
                                || !route.equals(parent.optString("linkUrl"))
                                || !parent.optString("evidence").startsWith("d2-ins-local:")
                                || parent.optString("linkUrl").contains("http")
                                || parent.optString("linkUrl").contains("/pc/aicloud/my")
                                || parent.optString("linkUrl").contains("/es/bigData/bigDataTask")) {
                            throw new AssertionError("D-2 Ins parent route missing: " + parent);
                        }
                        routes.add(route);
                        boolean childFound = false;
                        for (int i = 0; i < menus.length(); i++) {
                            JSONObject child = menus.getJSONObject(i);
                            if (child.optInt("parentId") == parent.optInt("id")
                                    && route.equals(child.optString("localCode"))
                                    && ("JSinglepage:" + route).equals(child.optString("linkUrl"))
                                    && child.optInt("treeEndFlg") == 1
                                    && child.optString("evidence").startsWith("d2-ins-local-child:")) {
                                childFound = true;
                            }
                        }
                        if (!childFound) {
                            throw new AssertionError("D-2 Ins explicit leaf missing: " + entry[0]);
                        }
                    }
                    if (routes.size() != expected.length) {
                        throw new AssertionError("D-2 Ins routes are not distinct: " + routes);
                    }
                    System.out.println("D2_INS_CATALOG_OK");
                }
            }
            """,
        )
        self.assertEqual(probe.returncode, 0, probe.stdout + probe.stderr)
        self.assertIn("D2_INS_CATALOG_OK", probe.stdout)

    def test_d2_ins_bridge_serves_distinct_local_pages_with_guarded_operations(self):
        self.compile_patcher()
        probe_source = self.tmp_path / "D2InsLocalPagesProbe.java"
        probe_source.write_text(
            textwrap.dedent(
                """
                import com.sbf.main.jxbrowser.M5LocalSpiderBridge;

                public class D2InsLocalPagesProbe {
                    public static void main(String[] args) {
                        String[][] pages = {
                            {"account-login", "Ins 帐号登录", "登录 Ins 帐号"},
                            {"account-search", "Ins 帐号搜索", "提交帐号搜索"},
                            {"post-search", "Ins 帖子搜索", "提交帖子搜索"},
                            {"profile-mining", "Ins 主页挖掘", "采集主页数据"},
                            {"active-filter", "Ins 筛选活跃", "开始活跃筛选"},
                            {"api-broadcast", "Ins 接口群发", "开始接口群发"},
                            {"android-agent", "Ins 安卓智能体", "启动安卓智能体"},
                            {"aicloud-fingerprint", "Ins AiCloud指纹", "绑定 AiCloud 指纹"},
                            {"adspower-fingerprint", "Ins AdsPower指纹", "绑定 AdsPower 指纹"}
                        };
                        for (String[] page : pages) {
                            String route = "/pc/local/ins/" + page[0];
                            String body = M5LocalSpiderBridge.localWebAssetBody("https://app.xdxsoft.com" + route);
                            if (body == null || !body.contains("data-d2-ins-route=\\\"" + page[0] + "\\\"")
                                    || !body.contains("D2_INS_LOCAL_PAGE") || !body.contains(page[1])
                                    || !body.contains(page[2]) || !body.contains("离线提示")
                                    || !body.contains("data-d1-action=\\\"guarded\\\" disabled")) {
                                throw new AssertionError("missing guarded D-2 Ins surface for " + route + ": " + body);
                            }
                            if (body.contains("任务批次号") || body.contains("+10000000000")
                                    || body.contains("fingerprintId") || body.contains("mock")) {
                                throw new AssertionError("fake data leaked into " + route);
                            }
                        }
                        System.out.println("D2_INS_LOCAL_PAGES_OK");
                    }
                }
                """
            ).strip(),
            encoding="utf-8",
        )
        subprocess.run(
            [str(JAVAC), "-encoding", "UTF-8", "-cp", classpath(self.classes, JSON_JAR, DATA_LIBS), "-d", str(self.probe_classes), str(probe_source)],
            cwd=ROOT,
            check=True,
        )
        probe = subprocess.run(
            [str(JAVA), "-cp", classpath(self.probe_classes, self.classes, JSON_JAR, DATA_LIBS), "D2InsLocalPagesProbe"],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        self.assertEqual(probe.returncode, 0, probe.stdout + probe.stderr)
        self.assertIn("D2_INS_LOCAL_PAGES_OK", probe.stdout)

    def test_d3_fb_catalog_uses_ten_distinct_explicit_local_leaves(self):
        probe = self.compile_and_run_catalog_probe(
            "D3FbCatalogProbe",
            """
            import java.util.HashMap;
            import java.util.HashSet;
            import java.util.Map;
            import java.util.Set;
            import org.json.JSONArray;
            import org.json.JSONObject;

            public class D3FbCatalogProbe {
                public static void main(String[] args) throws Exception {
                    String[][] expected = {
                        {"C4747_000", "mirror-settings"}, {"C4747_001", "friends-collect"},
                        {"C4747_002", "groups-collect"}, {"C4747_003", "pages-collect"},
                        {"C4747_004", "live-collect"}, {"C4747_005", "ads-collect"},
                        {"C4747_006", "ad-comment-intercept"}, {"C4747_007", "video-intercept"},
                        {"C4747_008", "active-user-check"}, {"C4747_009", "inquiry-reply"}
                    };
                    JSONArray menus = new JSONObject(M4RecoveryCatalog.pcMenusJson()).getJSONArray("scfs");
                    Map<String, JSONObject> parents = new HashMap<String, JSONObject>();
                    for (int i = 0; i < menus.length(); i++) {
                        JSONObject item = menus.getJSONObject(i);
                        for (String[] entry : expected) {
                            if (entry[0].equals(item.optString("code"))) {
                                parents.put(entry[0], item);
                            }
                        }
                    }
                    if (parents.size() != expected.length) {
                        throw new AssertionError("missing FB parents: " + parents.keySet());
                    }
                    Set<String> routes = new HashSet<String>();
                    for (String[] entry : expected) {
                        JSONObject parent = parents.get(entry[0]);
                        String route = "/pc/local/fb/" + entry[1];
                        if (!"JSinglepage".equals(parent.optString("localCode"))
                                || !route.equals(parent.optString("linkUrl"))
                                || !parent.optString("evidence").startsWith("d3-fb-local:")
                                || parent.optString("linkUrl").contains("http")
                                || parent.optString("linkUrl").contains("/pc/aicloud/my")
                                || parent.optString("linkUrl").contains("/es/bigData/bigDataTask")) {
                            throw new AssertionError("D-3 FB parent route missing: " + parent);
                        }
                        routes.add(route);
                        boolean childFound = false;
                        for (int i = 0; i < menus.length(); i++) {
                            JSONObject child = menus.getJSONObject(i);
                            if (child.optInt("parentId") == parent.optInt("id")
                                    && route.equals(child.optString("localCode"))
                                    && ("JSinglepage:" + route).equals(child.optString("linkUrl"))
                                    && child.optInt("treeEndFlg") == 1
                                    && child.optString("evidence").startsWith("d3-fb-local-child:")) {
                                childFound = true;
                            }
                        }
                        if (!childFound) {
                            throw new AssertionError("D-3 FB explicit leaf missing: " + entry[0]);
                        }
                    }
                    if (routes.size() != expected.length) {
                        throw new AssertionError("D-3 FB routes are not distinct: " + routes);
                    }
                    System.out.println("D3_FB_CATALOG_OK");
                }
            }
            """,
        )
        self.assertEqual(probe.returncode, 0, probe.stdout + probe.stderr)
        self.assertIn("D3_FB_CATALOG_OK", probe.stdout)

    def test_d3_fb_bridge_serves_distinct_local_pages_with_guarded_operations(self):
        self.compile_patcher()
        probe_source = self.tmp_path / "D3FbLocalPagesProbe.java"
        probe_source.write_text(
            textwrap.dedent(
                """
                import com.sbf.main.jxbrowser.M5LocalSpiderBridge;

                public class D3FbLocalPagesProbe {
                    public static void main(String[] args) {
                        String[][] pages = {
                            {"mirror-settings", "镜像系统设置", "保存镜像系统设置"},
                            {"friends-collect", "FB 好友采集", "提交好友采集"},
                            {"groups-collect", "FB 小组采集", "提交小组采集"},
                            {"pages-collect", "FB 主页采集", "提交主页采集"},
                            {"live-collect", "FB 直播采集", "提交直播采集"},
                            {"ads-collect", "FB 广告采集", "提交广告采集"},
                            {"ad-comment-intercept", "FB 广告评论截流", "开始广告评论截流"},
                            {"video-intercept", "FB 视频截流", "开始视频截流"},
                            {"active-user-check", "FB 活跃用户检测", "开始活跃用户检测"},
                            {"inquiry-reply", "FB 询盘回复", "开始询盘回复"}
                        };
                        for (String[] page : pages) {
                            String route = "/pc/local/fb/" + page[0];
                            String body = M5LocalSpiderBridge.localWebAssetBody("https://app.xdxsoft.com" + route);
                            if (body == null || !body.contains("data-d3-fb-route=\\\"" + page[0] + "\\\"")
                                    || !body.contains("D3_FB_LOCAL_PAGE") || !body.contains(page[1])
                                    || !body.contains(page[2]) || !body.contains("离线提示")
                                    || !body.contains("data-d1-action=\\\"guarded\\\" disabled")) {
                                throw new AssertionError("missing guarded D-3 local surface for " + route + ": " + body);
                            }
                            if (body.contains("任务批次号") || body.contains("+10000000000")
                                    || body.contains("fingerprintId") || body.contains("mock")) {
                                throw new AssertionError("fake data leaked into " + route);
                            }
                        }
                        System.out.println("D3_FB_LOCAL_PAGES_OK");
                    }
                }
                """
            ).strip(),
            encoding="utf-8",
        )
        subprocess.run(
            [str(JAVAC), "-encoding", "UTF-8", "-cp", classpath(self.classes, JSON_JAR, DATA_LIBS), "-d", str(self.probe_classes), str(probe_source)],
            cwd=ROOT,
            check=True,
        )
        probe = subprocess.run(
            [str(JAVA), "-cp", classpath(self.probe_classes, self.classes, JSON_JAR, DATA_LIBS), "D3FbLocalPagesProbe"],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        self.assertEqual(probe.returncode, 0, probe.stdout + probe.stderr)
        self.assertIn("D3_FB_LOCAL_PAGES_OK", probe.stdout)

    def test_d1_x_overlay_changes_only_catalog_and_local_bridge_on_c67_candidate(self):
        self.compile_patcher()
        c67_candidate = ROOT / ".artifacts" / "working" / "c67-advertising-candidate" / "App.c67.candidate.dll"
        self.assertTrue(c67_candidate.exists(), c67_candidate)
        result = self.run_d1_x_overlay(c67_candidate)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("D1_X_LOCAL_PAGES_OVERLAY", result.stdout)
        changed = {
            "com/sbf/util/http/SBFApi.class",
            "com/sbf/main/jxbrowser/M5LocalSpiderBridge.class",
        }
        with zipfile.ZipFile(c67_candidate) as base_jar, zipfile.ZipFile(self.output_jar) as overlay_jar:
            self.assertEqual(base_jar.namelist(), overlay_jar.namelist())
            for entry_name in base_jar.namelist():
                if entry_name in changed or entry_name.endswith("/"):
                    continue
                self.assertEqual(base_jar.read(entry_name), overlay_jar.read(entry_name), entry_name)
        bridge_javap = subprocess.run(
            [str(JAVAP), "-classpath", str(self.output_jar), "-c", "-p", "com.sbf.main.jxbrowser.M5LocalSpiderBridge"],
            cwd=ROOT, text=True, encoding="utf-8", errors="replace", stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True,
        )
        self.assertIn("D1_X_LOCAL_PAGE", bridge_javap.stdout)

    def test_d2_ins_overlay_changes_only_catalog_and_local_bridge_on_d1_candidate(self):
        self.compile_patcher()
        d1_live_baseline = ROOT / "data" / "app" / "App.dll"
        self.assertTrue(d1_live_baseline.exists(), d1_live_baseline)
        result = self.run_d2_ins_overlay(d1_live_baseline)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("D2_INS_LOCAL_PAGES_OVERLAY", result.stdout)
        changed = {
            "com/sbf/util/http/SBFApi.class",
            "com/sbf/main/jxbrowser/M5LocalSpiderBridge.class",
        }
        with zipfile.ZipFile(d1_live_baseline) as base_jar, zipfile.ZipFile(self.output_jar) as overlay_jar:
            self.assertEqual(base_jar.namelist(), overlay_jar.namelist())
            for entry_name in base_jar.namelist():
                if entry_name in changed or entry_name.endswith("/"):
                    continue
                self.assertEqual(base_jar.read(entry_name), overlay_jar.read(entry_name), entry_name)
        bridge_javap = subprocess.run(
            [str(JAVAP), "-classpath", str(self.output_jar), "-c", "-p", "com.sbf.main.jxbrowser.M5LocalSpiderBridge"],
            cwd=ROOT, text=True, encoding="utf-8", errors="replace", stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True,
        )
        self.assertIn("D2_INS_LOCAL_PAGE", bridge_javap.stdout)

    def test_d3_fb_overlay_changes_only_catalog_and_local_bridge_on_d2_candidate(self):
        self.compile_patcher()
        d2_live_baseline = ROOT / "data" / "app" / "App.dll"
        self.assertTrue(d2_live_baseline.exists(), d2_live_baseline)
        result = self.run_d3_fb_overlay(d2_live_baseline)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("D3_FB_LOCAL_PAGES_OVERLAY", result.stdout)
        changed = {
            "com/sbf/util/http/SBFApi.class",
            "com/sbf/main/jxbrowser/M5LocalSpiderBridge.class",
        }
        with zipfile.ZipFile(d2_live_baseline) as base_jar, zipfile.ZipFile(self.output_jar) as overlay_jar:
            self.assertEqual(base_jar.namelist(), overlay_jar.namelist())
            for entry_name in base_jar.namelist():
                if entry_name in changed or entry_name.endswith("/"):
                    continue
                self.assertEqual(base_jar.read(entry_name), overlay_jar.read(entry_name), entry_name)
        bridge_javap = subprocess.run(
            [str(JAVAP), "-classpath", str(self.output_jar), "-c", "-p", "com.sbf.main.jxbrowser.M5LocalSpiderBridge"],
            cwd=ROOT, text=True, encoding="utf-8", errors="replace", stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True,
        )
        self.assertIn("D3_FB_LOCAL_PAGE", bridge_javap.stdout)

    def test_generic_patcher_accepts_the_current_recovery_menu_payload(self):
        self.compile_patcher()
        result = self.run_patcher()
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_c67_overlay_updates_the_advertising_contract_on_the_c66_candidate(self):
        self.compile_patcher()
        c66_candidate = (
            ROOT
            / ".artifacts"
            / "working"
            / "c66-commerce-candidate"
            / "App.c66.candidate.dll"
        )
        self.assertTrue(c66_candidate.exists())
        result = self.run_c67_overlay(c66_candidate)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("C67_ADVERTISING_OVERLAY", result.stdout)

        sbf_api_entry = "com/sbf/util/http/SBFApi.class"
        dispatch_entry = "com/sbf/main/JSBFMain$5.class"
        bridge_entry = "com/sbf/main/jxbrowser/M5LocalSpiderBridge.class"
        with zipfile.ZipFile(c66_candidate) as base_jar, zipfile.ZipFile(self.output_jar) as overlay_jar:
            self.assertEqual(base_jar.namelist(), overlay_jar.namelist())
            self.assertNotEqual(base_jar.read(sbf_api_entry), overlay_jar.read(sbf_api_entry))
            self.assertNotEqual(base_jar.read(dispatch_entry), overlay_jar.read(dispatch_entry))
            self.assertNotEqual(base_jar.read(bridge_entry), overlay_jar.read(bridge_entry))
            for entry_name in base_jar.namelist():
                if entry_name in (sbf_api_entry, dispatch_entry, bridge_entry) or entry_name.endswith("/"):
                    continue
                self.assertEqual(base_jar.read(entry_name), overlay_jar.read(entry_name), entry_name)

        javap = subprocess.run(
            [
                str(JAVAP),
                "-classpath",
                str(self.output_jar),
                "-c",
                "-p",
                "com.sbf.util.http.SBFApi",
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=True,
        )
        self.assertIn("C67_ADVERTISING_MENU_DISPATCH", javap.stdout)
        self.assertIn("/views/overseasAds/dataBoard", javap.stdout)

        dispatch_javap = subprocess.run(
            [
                str(JAVAP),
                "-classpath",
                str(self.output_jar),
                "-c",
                "-p",
                "com.sbf.main.JSBFMain$5",
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=True,
        )
        self.assertIn("C67_ADVERTISING_TAB_JXBROWSER_URL_FROM_LINKURL", dispatch_javap.stdout)

        bridge_javap = subprocess.run(
            [
                str(JAVAP),
                "-classpath",
                str(self.output_jar),
                "-c",
                "-p",
                "com.sbf.main.jxbrowser.M5LocalSpiderBridge",
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=True,
        )
        self.assertIn("/views/overseasAds/", bridge_javap.stdout)

    def test_c6_overlay_updates_only_the_declared_offline_boundary_classes(self):
        self.compile_patcher()
        c65_candidate = (
            ROOT
            / ".artifacts"
            / "working"
            / "c65-native-host-gateway"
            / "App.c65.candidate.dll"
        )
        self.assertTrue(c65_candidate.exists())
        result = self.run_c66_overlay(c65_candidate)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("C66_COMMERCE_OVERLAY", result.stdout)

        bridge_entry = "com/sbf/main/jxbrowser/M5LocalSpiderBridge.class"
        recharge_listener_entry = "com/sbf/main/JSBFMain$6.class"
        with zipfile.ZipFile(c65_candidate) as base_jar, zipfile.ZipFile(self.output_jar) as overlay_jar:
            self.assertEqual(base_jar.namelist(), overlay_jar.namelist())
            self.assertNotEqual(base_jar.read(bridge_entry), overlay_jar.read(bridge_entry))
            self.assertNotEqual(
                base_jar.read(recharge_listener_entry), overlay_jar.read(recharge_listener_entry)
            )
            for entry_name in base_jar.namelist():
                if entry_name in (bridge_entry, recharge_listener_entry) or entry_name.endswith("/"):
                    continue
                self.assertEqual(base_jar.read(entry_name), overlay_jar.read(entry_name), entry_name)

        probe_source = self.tmp_path / "C6OverlayProbe.java"
        probe_source.write_text(
            textwrap.dedent(
                """
                import com.sbf.main.jxbrowser.M5LocalSpiderBridge;

                public class C6OverlayProbe {
                    public static void main(String[] args) throws Exception {
                        String recharge = M5LocalSpiderBridge.localWebAssetBody(
                                "https://app.xdxsoft.com/pc/alipay/enterpriseAuth");
                        String advertising = M5LocalSpiderBridge.localWebAssetBody(
                                "https://app.xdxsoft.com/views/overseasAds/dataBoard");
                        if (recharge == null || !recharge.contains("C6_RECHARGE_UI")) {
                            throw new AssertionError("missing C6 recharge overlay");
                        }
                        if (advertising == null || !advertising.contains("C6_ADVERTISING_UI")) {
                            throw new AssertionError("missing C6 advertising overlay");
                        }
                        System.out.println("C6_OVERLAY_BRIDGE_OK");
                    }
                }
                """
            ).strip(),
            encoding="utf-8",
        )
        subprocess.run(
            [
                str(JAVAC),
                "-encoding",
                "UTF-8",
                "-cp",
                classpath(self.output_jar, JSON_JAR, DATA_LIBS),
                "-d",
                str(self.probe_classes),
                str(probe_source),
            ],
            cwd=ROOT,
            check=True,
        )
        probe = subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.probe_classes, self.output_jar, JSON_JAR, DATA_LIBS),
                "C6OverlayProbe",
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("C6_OVERLAY_BRIDGE_OK", probe.stdout)

    def test_c62_overlay_adds_chromium_startup_network_killswitches(self):
        self.compile_patcher()
        c5_candidate = (
            ROOT
            / ".artifacts"
            / "working"
            / "c5-platform-ui"
            / "App-c5-platform-ui-candidate.dll"
        )
        self.assertTrue(c5_candidate.exists())
        result = self.run_c6_overlay(c5_candidate)
        self.assertEqual(result.returncode, 0, result.stderr)
        engine_entry = "com/sbf/main/jxbrowser/g.class"
        with zipfile.ZipFile(c5_candidate) as base_jar, zipfile.ZipFile(self.output_jar) as overlay_jar:
            self.assertNotEqual(
                base_jar.read(engine_entry),
                overlay_jar.read(engine_entry),
            )
        javap = subprocess.run(
            [
                str(JAVAP),
                "-classpath",
                str(self.output_jar),
                "-c",
                "-p",
                "com.sbf.main.jxbrowser.g",
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=True,
        )
        for chromium_switch in (
            "--disable-background-networking",
            "--disable-component-update",
            "--disable-domain-reliability",
            "--no-pings",
            "--safebrowsing-disable-auto-update",
        ):
            self.assertIn(chromium_switch, javap.stdout)

    def test_c64_overlay_stubs_native_startup_update_and_authorization(self):
        self.compile_patcher()
        c5_candidate = (
            ROOT
            / ".artifacts"
            / "working"
            / "c5-platform-ui"
            / "App-c5-platform-ui-candidate.dll"
        )
        self.assertTrue(c5_candidate.exists())
        result = self.run_c6_overlay(c5_candidate)
        self.assertEqual(result.returncode, 0, result.stderr)

        sbf_api = subprocess.run(
            [
                str(JAVAP),
                "-classpath",
                str(self.output_jar),
                "-c",
                "-p",
                "com.sbf.util.http.SBFApi",
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=True,
        )
        start_app = subprocess.run(
            [
                str(JAVAP),
                "-classpath",
                str(self.output_jar),
                "-c",
                "-p",
                "com.sbf.main.StartApp",
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=True,
        )
        self.assertIn("C64_MIERP_UPDATE_STUB", sbf_api.stdout)
        self.assertIn("C64_APPFILE_DOWNLOAD_STUB", sbf_api.stdout)
        self.assertIn("hasUpdate", sbf_api.stdout)
        self.assertIn("C64_NATIVE_STARTUP_AUTH_STUB", start_app.stdout)
        self.assertIn("app.xdxsoft.com", start_app.stdout)
        self.assertIn("offline-local-token-1234567890", start_app.stdout)

        probe_source = self.tmp_path / "C64NativeStartupProbe.java"
        probe_source.write_text(
            textwrap.dedent(
                """
                import com.sbf.main.StartApp;
                import java.lang.reflect.Method;
                import org.json.JSONObject;

                public class C64NativeStartupProbe {
                    public static void main(String[] args) throws Exception {
                        String token = StartApp.f("https://app.xdxsoft.com/api/v1/native/startup/license");
                        if (!"offline-local-token-1234567890".equals(token)) {
                            throw new AssertionError("native startup token: " + token);
                        }
                        Method update = Class.forName("com.sbf.util.http.SBFApi")
                                .getDeclaredMethod("g", String.class, int.class);
                        update.setAccessible(true);
                        JSONObject result = (JSONObject) update.invoke(null, "1.0.0", 0);
                        if (result.optInt("code") != 200 || result.optBoolean("hasUpdate", true)) {
                            throw new AssertionError("update contract: " + result);
                        }
                        Class.forName("com.sbf.util.http.SBFApi").getDeclaredMethod("n").invoke(null);
                        System.out.println("C64_NATIVE_STARTUP_STUB_OK");
                    }
                }
                """
            ).strip(),
            encoding="utf-8",
        )
        subprocess.run(
            [
                str(JAVAC),
                "-encoding",
                "UTF-8",
                "-cp",
                classpath(self.output_jar, JSON_JAR, DATA_LIBS),
                "-d",
                str(self.probe_classes),
                str(probe_source),
            ],
            cwd=ROOT,
            check=True,
        )
        probe = subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.probe_classes, self.output_jar, JSON_JAR, DATA_LIBS),
                "C64NativeStartupProbe",
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("C64_NATIVE_STARTUP_STUB_OK", probe.stdout)

    def test_c64_overlay_instruments_native_http_urls_for_diagnosis(self):
        self.compile_patcher()
        c5_candidate = (
            ROOT
            / ".artifacts"
            / "working"
            / "c5-platform-ui"
            / "App-c5-platform-ui-candidate.dll"
        )
        self.assertTrue(c5_candidate.exists())
        result = self.run_c6_overlay(c5_candidate)
        self.assertEqual(result.returncode, 0, result.stderr)
        diagnostic_entry = "com/sbf/main/C64NativeNetworkDiag.class"
        with zipfile.ZipFile(self.output_jar) as overlay_jar:
            self.assertIn(diagnostic_entry, overlay_jar.namelist())
        dth = subprocess.run(
            [
                str(JAVAP),
                "-classpath",
                str(self.output_jar),
                "-c",
                "-p",
                "com.sbf.util.http.DTHelper",
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=True,
        )
        self.assertIn("C64NativeNetworkDiag.observe", dth.stdout)

    def test_c65_overlay_routes_all_xdxsoft_json_calls_through_local_contracts(self):
        self.compile_patcher()
        c5_candidate = (
            ROOT
            / ".artifacts"
            / "working"
            / "c5-platform-ui"
            / "App-c5-platform-ui-candidate.dll"
        )
        self.assertTrue(c5_candidate.exists())
        result = self.run_c6_overlay(c5_candidate)
        self.assertEqual(result.returncode, 0, result.stderr)

        dth = subprocess.run(
            [
                str(JAVAP),
                "-classpath",
                str(self.output_jar),
                "-c",
                "-p",
                "com.sbf.util.http.DTHelper",
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=True,
        )
        self.assertIn("C64NativeNetworkDiag.localResponse", dth.stdout)
        sbf_api = subprocess.run(
            [
                str(JAVAP),
                "-classpath",
                str(self.output_jar),
                "-c",
                "-p",
                "com.sbf.util.http.SBFApi",
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=True,
        )
        self.assertIn("C65_APPFILE_FLOW_STUB", sbf_api.stdout)
        self.assertIn("C65_APPFILE_RESPONSE_ADAPTER", sbf_api.stdout)

        probe_source = self.tmp_path / "C65NativeGatewayProbe.java"
        probe_source.write_text(
            textwrap.dedent(
                """
                import com.sbf.util.http.DTHelper;
                import org.json.JSONObject;

                public class C65NativeGatewayProbe {
                    public static void main(String[] args) {
                        String base = "https://app.xdxsoft.com/prod-api/";
                        JSONObject checkUser = DTHelper.c(base + "api/v1/client/pc/checkuser");
                        JSONObject bannedWords = DTHelper.c(base + "api/v1/client/pc/lisBanWords");
                        JSONObject appFile = DTHelper.c(base + "system/appfiles/code/hcai_app_ime");
                        if (checkUser.optInt("code") != 200
                                || !checkUser.optJSONObject("data").optBoolean("authorized")) {
                            throw new AssertionError("checkuser contract: " + checkUser);
                        }
                        if (bannedWords.optInt("code") != 200
                                || bannedWords.optJSONArray("data").length() != 0) {
                            throw new AssertionError("lisBanWords contract: " + bannedWords);
                        }
                        if (appFile.optInt("code") != 200
                                || appFile.optJSONObject("data").optInt("code") != 200
                                || appFile.optJSONObject("data").optJSONObject("data") == null) {
                            throw new AssertionError("appfile contract: " + appFile);
                        }
                        try {
                            java.lang.reflect.Method ax = Class.forName("com.sbf.util.http.SBFApi")
                                    .getDeclaredMethod("ax", String.class);
                            ax.setAccessible(true);
                            JSONObject asset = (JSONObject) ax.invoke(null, "hcai_app_ime");
                            if (asset == null) {
                                throw new AssertionError("appfile ax contract");
                            }
                        } catch (Exception exception) {
                            throw new AssertionError("appfile ax contract", exception);
                        }
                        try {
                            Class.forName("com.sbf.util.http.SBFApi")
                                    .getDeclaredMethod("n", String.class)
                                    .invoke(null, "hcai_app_ime");
                        } catch (Exception exception) {
                            throw new AssertionError("appfile flow contract", exception);
                        }
                        System.out.println("C65_NATIVE_GATEWAY_OK");
                    }
                }
                """
            ).strip(),
            encoding="utf-8",
        )
        subprocess.run(
            [
                str(JAVAC),
                "-encoding",
                "UTF-8",
                "-cp",
                classpath(self.output_jar, JSON_JAR, DATA_LIBS),
                "-d",
                str(self.probe_classes),
                str(probe_source),
            ],
            cwd=ROOT,
            check=True,
        )
        probe = subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.probe_classes, self.output_jar, JSON_JAR, DATA_LIBS),
                "C65NativeGatewayProbe",
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("C65_NATIVE_GATEWAY_OK", probe.stdout)

    def test_c66_overlay_replaces_global_recharge_listener_with_local_dialog(self):
        self.compile_patcher()
        c5_candidate = (
            ROOT
            / ".artifacts"
            / "working"
            / "c5-platform-ui"
            / "App-c5-platform-ui-candidate.dll"
        )
        self.assertTrue(c5_candidate.exists())
        result = self.run_c6_overlay(c5_candidate)
        self.assertEqual(result.returncode, 0, result.stderr)

        listener = subprocess.run(
            [
                str(JAVAP),
                "-classpath",
                str(self.output_jar),
                "-c",
                "-p",
                "com.sbf.main.JSBFMain$6",
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=True,
        )
        self.assertIn("C66_RECHARGE_ENTRY", listener.stdout)
        self.assertIn("M5LocalSpiderBridge.openC66RechargeDialog", listener.stdout)

        probe_source = self.tmp_path / "C66RechargeDialogProbe.java"
        probe_source.write_text(
            textwrap.dedent(
                """
                import com.sbf.main.jxbrowser.M5LocalSpiderBridge;

                public class C66RechargeDialogProbe {
                    public static void main(String[] args) {
                        String html = M5LocalSpiderBridge.localC66RechargeDialogHtml();
                        if (html == null
                                || !html.contains("C6_RECHARGE_UI")
                                || !html.contains("data-c6-action=\\\"disabled\\\"")
                                || !html.contains("支付与订单功能不可用")) {
                            throw new AssertionError("missing C66 recharge empty state: " + html);
                        }
                        System.out.println("C66_RECHARGE_DIALOG_OK");
                    }
                }
                """
            ).strip(),
            encoding="utf-8",
        )
        subprocess.run(
            [
                str(JAVAC),
                "-encoding",
                "UTF-8",
                "-cp",
                classpath(self.output_jar, JSON_JAR, DATA_LIBS),
                "-d",
                str(self.probe_classes),
                str(probe_source),
            ],
            cwd=ROOT,
            check=True,
        )
        probe = subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.probe_classes, self.output_jar, JSON_JAR, DATA_LIBS),
                "C66RechargeDialogProbe",
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        self.assertEqual(probe.returncode, 0, probe.stderr)
        self.assertIn("C66_RECHARGE_DIALOG_OK", probe.stdout)
    def test_d5_tg_catalog_uses_eleven_distinct_explicit_local_leaves(self):
        probe = self.compile_and_run_catalog_probe(
            "D5TgCatalogProbe",
            """
            import java.util.HashMap;
            import java.util.HashSet;
            import java.util.Map;
            import java.util.Set;
            import org.json.JSONArray;
            import org.json.JSONObject;

            public class D5TgCatalogProbe {
                public static void main(String[] args) throws Exception {
                    String[][] expected = {
                        {"C4135_001", "jump-push"}, {"C4135_002", "accounts"},
                        {"C4135_003", "ai-collect"}, {"C4135_004", "ai-data"},
                        {"C4135_005", "group-collect"}, {"C4135_006", "group-member-extract"},
                        {"C4135_007", "ai-filter"}, {"C4135_008", "ai-growth"},
                        {"C4135_009", "android-agent"}, {"C4135_010", "aicloud-fingerprint"},
                        {"C4135_011", "adspower-fingerprint"}
                    };
                    JSONArray menus = new JSONObject(M4RecoveryCatalog.pcMenusJson()).getJSONArray("scfs");
                    Map<String, JSONObject> parents = new HashMap<String, JSONObject>();
                    for (int i = 0; i < menus.length(); i++) {
                        JSONObject item = menus.getJSONObject(i);
                        for (String[] entry : expected) {
                            if (entry[0].equals(item.optString("code"))) {
                                parents.put(entry[0], item);
                            }
                        }
                    }
                    if (parents.size() != expected.length) {
                        throw new AssertionError("missing TG parents: " + parents.keySet());
                    }
                    Set<String> routes = new HashSet<String>();
                    for (String[] entry : expected) {
                        JSONObject parent = parents.get(entry[0]);
                        String route = "/pc/local/tg/" + entry[1];
                        if (!"JSinglepage".equals(parent.optString("localCode"))
                                || !route.equals(parent.optString("linkUrl"))
                                || !parent.optString("evidence").startsWith("d5-tg-local:")
                                || parent.optString("linkUrl").contains("http")
                                || parent.optString("linkUrl").contains("/pc/aicloud/my")
                                || parent.optString("linkUrl").contains("/pc/tg/index")) {
                            throw new AssertionError("D-5 TG parent route missing: " + parent);
                        }
                        routes.add(route);
                        boolean childFound = false;
                        for (int i = 0; i < menus.length(); i++) {
                            JSONObject child = menus.getJSONObject(i);
                            if (child.optInt("parentId") == parent.optInt("id")
                                    && route.equals(child.optString("localCode"))
                                    && ("JSinglepage:" + route).equals(child.optString("linkUrl"))
                                    && child.optInt("treeEndFlg") == 1
                                    && child.optString("evidence").startsWith("d5-tg-local-child:")) {
                                childFound = true;
                            }
                        }
                        if (!childFound) {
                            throw new AssertionError("D-5 TG explicit leaf missing: " + entry[0]);
                        }
                    }
                    if (routes.size() != expected.length) {
                        throw new AssertionError("D-5 TG routes are not distinct: " + routes);
                    }
                    System.out.println("D5_TG_CATALOG_OK");
                }
            }
            """,
        )
        self.assertEqual(probe.returncode, 0, probe.stdout + probe.stderr)
        self.assertIn("D5_TG_CATALOG_OK", probe.stdout)

    def test_d5_tg_bridge_serves_distinct_local_pages_with_guarded_operations(self):
        self.compile_patcher()
        probe_source = self.tmp_path / "D5TgLocalPagesProbe.java"
        probe_source.write_text(
            textwrap.dedent(
                """
                import com.sbf.main.jxbrowser.M5LocalSpiderBridge;

                public class D5TgLocalPagesProbe {
                    public static void main(String[] args) {
                        String[][] pages = {
                            {"jump-push", "TG 跳推系统", "启动跳推系统"},
                            {"accounts", "TG 帐号", "管理 TG 帐号"},
                            {"ai-collect", "TG AI 采集", "提交 TG AI采集"},
                            {"ai-data", "TG AI数据", "查看 TG AI数据"},
                            {"group-collect", "TG AI 群采集", "提交 TG AI群采集"},
                            {"group-member-extract", "TG AI 群成员提取", "提取 TG 群成员"},
                            {"ai-filter", "TG AI筛选", "开始 TG AI筛选"},
                            {"ai-growth", "TG AI裂变", "启动 TG AI裂变"},
                            {"android-agent", "TG 安卓智能体", "启动 TG 安卓智能体"},
                            {"aicloud-fingerprint", "TG AiCloud指纹", "绑定 TG AiCloud指纹"},
                            {"adspower-fingerprint", "TG AdsPower指纹", "绑定 TG AdsPower指纹"}
                        };
                        for (String[] page : pages) {
                            String route = "/pc/local/tg/" + page[0];
                            String body = M5LocalSpiderBridge.localWebAssetBody("https://app.xdxsoft.com" + route);
                            if (body == null || !body.contains("data-d5-tg-route=\\\"" + page[0] + "\\\"")
                                    || !body.contains("D5_TG_LOCAL_PAGE") || !body.contains(page[1])
                                    || !body.contains(page[2]) || !body.contains("离线提示")
                                    || !body.contains("data-d1-action=\\\"guarded\\\" disabled")) {
                                throw new AssertionError("missing guarded D-5 TG surface for " + route + ": " + body);
                            }
                            if (body.contains("任务批次号") || body.contains("+10000000000")
                                    || body.contains("fingerprintId") || body.contains("mock")) {
                                throw new AssertionError("fake data leaked into " + route);
                            }
                        }
                        System.out.println("D5_TG_LOCAL_PAGES_OK");
                    }
                }
                """
            ).strip(),
            encoding="utf-8",
        )
        subprocess.run(
            [str(JAVAC), "-encoding", "UTF-8", "-cp", classpath(self.classes, JSON_JAR, DATA_LIBS), "-d", str(self.probe_classes), str(probe_source)],
            cwd=ROOT,
            check=True,
        )
        probe = subprocess.run(
            [str(JAVA), "-cp", classpath(self.probe_classes, self.classes, JSON_JAR, DATA_LIBS), "D5TgLocalPagesProbe"],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        self.assertEqual(probe.returncode, 0, probe.stdout + probe.stderr)
        self.assertIn("D5_TG_LOCAL_PAGES_OK", probe.stdout)

    def test_d5_tg_overlay_changes_only_catalog_and_local_bridge_on_live_baseline(self):
        self.compile_patcher()
        live_baseline = ROOT / "data" / "app" / "App.dll"
        self.assertTrue(live_baseline.exists(), live_baseline)
        result = self.run_d5_tg_overlay(live_baseline)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("D5_TG_LOCAL_PAGES_OVERLAY", result.stdout)
        changed = {
            "com/sbf/util/http/SBFApi.class",
            "com/sbf/main/jxbrowser/M5LocalSpiderBridge.class",
        }
        with zipfile.ZipFile(live_baseline) as base_jar, zipfile.ZipFile(self.output_jar) as overlay_jar:
            self.assertEqual(base_jar.namelist(), overlay_jar.namelist())
            for entry_name in base_jar.namelist():
                if entry_name in changed or entry_name.endswith("/"):
                    continue
                self.assertEqual(base_jar.read(entry_name), overlay_jar.read(entry_name), entry_name)
        bridge_javap = subprocess.run(
            [str(JAVAP), "-classpath", str(self.output_jar), "-c", "-p", "com.sbf.main.jxbrowser.M5LocalSpiderBridge"],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=True,
        )
        self.assertIn("D5_TG_LOCAL_PAGE", bridge_javap.stdout)

    def test_d5_geo_catalog_uses_nine_distinct_explicit_local_leaves(self):
        probe = self.compile_and_run_catalog_probe(
            "D5GeoCatalogProbe",
            """
            import java.util.HashMap;
            import java.util.HashSet;
            import java.util.Map;
            import java.util.Set;
            import org.json.JSONArray;
            import org.json.JSONObject;

            public class D5GeoCatalogProbe {
                public static void main(String[] args) throws Exception {
                    String[][] expected = {
                        {"C4134_002", "google-seo"}, {"C4134_003", "precise-number-mining"},
                        {"C4134_006", "google-geo-media"}, {"C4137_001", "global-number-collect"},
                        {"C4137_002", "global-region-collect"}, {"C4137_003", "customs-data-mining"},
                        {"C4137_004", "global-company-data"}, {"C4137_005", "global-big-data"},
                        {"C4137_006", "number-ai-active-filter"}
                    };
                    JSONArray menus = new JSONObject(M4RecoveryCatalog.pcMenusJson()).getJSONArray("scfs");
                    Map<String, JSONObject> parents = new HashMap<String, JSONObject>();
                    for (int i = 0; i < menus.length(); i++) {
                        JSONObject item = menus.getJSONObject(i);
                        for (String[] entry : expected) {
                            if (entry[0].equals(item.optString("code"))) {
                                parents.put(entry[0], item);
                            }
                        }
                    }
                    if (parents.size() != expected.length) {
                        throw new AssertionError("missing GEO parents: " + parents.keySet());
                    }
                    Set<String> routes = new HashSet<String>();
                    for (String[] entry : expected) {
                        JSONObject parent = parents.get(entry[0]);
                        String route = "/pc/local/geo/" + entry[1];
                        if (!"JSinglepage".equals(parent.optString("localCode"))
                                || !route.equals(parent.optString("linkUrl"))
                                || !parent.optString("evidence").startsWith("d5-geo-local:")
                                || parent.optString("linkUrl").contains("http")
                                || parent.optString("linkUrl").contains("/pc/aicloud/my")
                                || parent.optString("linkUrl").contains("/pc/dataCollect/googleseo")) {
                            throw new AssertionError("D-5 GEO parent route missing: " + parent);
                        }
                        routes.add(route);
                        boolean childFound = false;
                        for (int i = 0; i < menus.length(); i++) {
                            JSONObject child = menus.getJSONObject(i);
                            if (child.optInt("parentId") == parent.optInt("id")
                                    && route.equals(child.optString("localCode"))
                                    && ("JSinglepage:" + route).equals(child.optString("linkUrl"))
                                    && child.optInt("treeEndFlg") == 1
                                    && child.optString("evidence").startsWith("d5-geo-local-child:")) {
                                childFound = true;
                            }
                        }
                        if (!childFound) {
                            throw new AssertionError("D-5 GEO explicit leaf missing: " + entry[0]);
                        }
                    }
                    if (routes.size() != expected.length) {
                        throw new AssertionError("D-5 GEO routes are not distinct: " + routes);
                    }
                    System.out.println("D5_GEO_CATALOG_OK");
                }
            }
            """,
        )
        self.assertEqual(probe.returncode, 0, probe.stdout + probe.stderr)
        self.assertIn("D5_GEO_CATALOG_OK", probe.stdout)

    def test_d5_geo_bridge_serves_distinct_local_pages_with_guarded_operations(self):
        self.compile_patcher()
        probe_source = self.tmp_path / "D5GeoLocalPagesProbe.java"
        probe_source.write_text(
            textwrap.dedent(
                """
                import com.sbf.main.jxbrowser.M5LocalSpiderBridge;

                public class D5GeoLocalPagesProbe {
                    public static void main(String[] args) {
                        String[][] pages = {
                            {"google-seo", "精准官网挖掘", "开始官网挖掘"},
                            {"precise-number-mining", "精准号码挖掘", "开始号码挖掘"},
                            {"google-geo-media", "Google GEO外媒体", "查看 GEO外媒体"},
                            {"global-number-collect", "全球号码采集", "开始全球号码采集"},
                            {"global-region-collect", "全球地区采集", "开始全球地区采集"},
                            {"customs-data-mining", "海关数据挖掘", "开始海关数据挖掘"},
                            {"global-company-data", "全球企业大数据", "查看全球企业数据"},
                            {"global-big-data", "全球大数据", "查看全球大数据"},
                            {"number-ai-active-filter", "号码 AI筛选活跃", "开始号码 AI筛选"}
                        };
                        for (String[] page : pages) {
                            String route = "/pc/local/geo/" + page[0];
                            String body = M5LocalSpiderBridge.localWebAssetBody("https://app.xdxsoft.com" + route);
                            if (body == null || !body.contains("data-d5-geo-route=\\\"" + page[0] + "\\\"")
                                    || !body.contains("D5_GEO_LOCAL_PAGE") || !body.contains(page[1])
                                    || !body.contains(page[2]) || !body.contains("离线提示")
                                    || !body.contains("data-d1-action=\\\"guarded\\\" disabled")) {
                                throw new AssertionError("missing guarded D-5 GEO surface for " + route + ": " + body);
                            }
                            if (body.contains("任务批次号") || body.contains("+10000000000")
                                    || body.contains("fingerprintId") || body.contains("mock")) {
                                throw new AssertionError("fake data leaked into " + route);
                            }
                        }
                        System.out.println("D5_GEO_LOCAL_PAGES_OK");
                    }
                }
                """
            ).strip(),
            encoding="utf-8",
        )
        subprocess.run(
            [str(JAVAC), "-encoding", "UTF-8", "-cp", classpath(self.classes, JSON_JAR, DATA_LIBS), "-d", str(self.probe_classes), str(probe_source)],
            cwd=ROOT,
            check=True,
        )
        probe = subprocess.run(
            [str(JAVA), "-cp", classpath(self.probe_classes, self.classes, JSON_JAR, DATA_LIBS), "D5GeoLocalPagesProbe"],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        self.assertEqual(probe.returncode, 0, probe.stdout + probe.stderr)
        self.assertIn("D5_GEO_LOCAL_PAGES_OK", probe.stdout)

    def test_d5_geo_overlay_changes_only_catalog_and_local_bridge_on_tg_live_baseline(self):
        self.compile_patcher()
        live_baseline = ROOT / "data" / "app" / "App.dll"
        self.assertTrue(live_baseline.exists(), live_baseline)
        result = self.run_d5_geo_overlay(live_baseline)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("D5_GEO_LOCAL_PAGES_OVERLAY", result.stdout)
        changed = {
            "com/sbf/util/http/SBFApi.class",
            "com/sbf/main/jxbrowser/M5LocalSpiderBridge.class",
        }
        with zipfile.ZipFile(live_baseline) as base_jar, zipfile.ZipFile(self.output_jar) as overlay_jar:
            self.assertEqual(base_jar.namelist(), overlay_jar.namelist())
            for entry_name in base_jar.namelist():
                if entry_name in changed or entry_name.endswith("/"):
                    continue
                self.assertEqual(base_jar.read(entry_name), overlay_jar.read(entry_name), entry_name)
        bridge_javap = subprocess.run(
            [str(JAVAP), "-classpath", str(self.output_jar), "-c", "-p", "com.sbf.main.jxbrowser.M5LocalSpiderBridge"],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=True,
        )
        self.assertIn("D5_GEO_LOCAL_PAGE", bridge_javap.stdout)

    def test_d5_wa_catalog_uses_seven_distinct_explicit_local_leaves(self):
        probe = self.compile_and_run_catalog_probe(
            "D5WaCatalogProbe",
            """
            import java.util.HashMap;
            import java.util.HashSet;
            import java.util.Map;
            import java.util.Set;
            import org.json.JSONArray;
            import org.json.JSONObject;

            public class D5WaCatalogProbe {
                public static void main(String[] args) throws Exception {
                    String[][] expected = {
                        {"C4936_000", "overview"}, {"C4936_001", "account-groups"},
                        {"C4936_002", "account-list"}, {"C4936_004", "contact-pool"},
                        {"C4936_005", "fan-broadcast"}, {"C4936_006", "group-broadcast"},
                        {"C4936_007", "customer-service-list"}
                    };
                    JSONArray menus = new JSONObject(M4RecoveryCatalog.pcMenusJson()).getJSONArray("scfs");
                    Map<String, JSONObject> parents = new HashMap<String, JSONObject>();
                    for (int i = 0; i < menus.length(); i++) {
                        JSONObject item = menus.getJSONObject(i);
                        for (String[] entry : expected) {
                            if (entry[0].equals(item.optString("code"))) parents.put(entry[0], item);
                        }
                    }
                    if (parents.size() != expected.length) throw new AssertionError("missing WA parents: " + parents.keySet());
                    Set<String> routes = new HashSet<String>();
                    for (String[] entry : expected) {
                        JSONObject parent = parents.get(entry[0]);
                        String route = "/pc/local/wa/" + entry[1];
                        if (!"JSinglepage".equals(parent.optString("localCode"))
                                || !route.equals(parent.optString("linkUrl"))
                                || !parent.optString("evidence").startsWith("d5-wa-local:")
                                || parent.optString("linkUrl").contains("http")
                                || parent.optString("linkUrl").contains("/pc/aicloud/my")
                                || parent.optString("linkUrl").contains("/pc/kefu/conversation")) {
                            throw new AssertionError("D-5 WA parent route missing: " + parent);
                        }
                        routes.add(route);
                        boolean childFound = false;
                        for (int i = 0; i < menus.length(); i++) {
                            JSONObject child = menus.getJSONObject(i);
                            if (child.optInt("parentId") == parent.optInt("id")
                                    && route.equals(child.optString("localCode"))
                                    && ("JSinglepage:" + route).equals(child.optString("linkUrl"))
                                    && child.optInt("treeEndFlg") == 1
                                    && child.optString("evidence").startsWith("d5-wa-local-child:")) childFound = true;
                        }
                        if (!childFound) throw new AssertionError("D-5 WA explicit leaf missing: " + entry[0]);
                    }
                    if (routes.size() != expected.length) throw new AssertionError("D-5 WA routes are not distinct: " + routes);
                    System.out.println("D5_WA_CATALOG_OK");
                }
            }
            """,
        )
        self.assertEqual(probe.returncode, 0, probe.stdout + probe.stderr)
        self.assertIn("D5_WA_CATALOG_OK", probe.stdout)

    def test_d5_wa_bridge_serves_distinct_local_pages_with_guarded_operations(self):
        self.compile_patcher()
        probe_source = self.tmp_path / "D5WaLocalPagesProbe.java"
        probe_source.write_text(
            textwrap.dedent(
                """
                import com.sbf.main.jxbrowser.M5LocalSpiderBridge;

                public class D5WaLocalPagesProbe {
                    public static void main(String[] args) {
                        String[][] pages = {
                            {"overview", "信息总览", "查看客服概览"},
                            {"account-groups", "账号分组", "管理账号分组"},
                            {"account-list", "账号列表", "查看账号列表"},
                            {"contact-pool", "联系人数据池", "查看联系人数据池"},
                            {"fan-broadcast", "爆粉群发", "开始爆粉群发"},
                            {"group-broadcast", "群聊群发", "开始群聊群发"},
                            {"customer-service-list", "客服列表", "查看客服列表"}
                        };
                        for (String[] page : pages) {
                            String route = "/pc/local/wa/" + page[0];
                            String body = M5LocalSpiderBridge.localWebAssetBody("https://app.xdxsoft.com" + route);
                            if (body == null || !body.contains("data-d5-wa-route=\\\"" + page[0] + "\\\"")
                                    || !body.contains("D5_WA_LOCAL_PAGE") || !body.contains(page[1])
                                    || !body.contains(page[2]) || !body.contains("离线提示")
                                    || !body.contains("data-d1-action=\\\"guarded\\\" disabled")) {
                                throw new AssertionError("missing guarded D-5 WA surface for " + route + ": " + body);
                            }
                            if (body.contains("二维码") || body.contains("发送消息") || body.contains("mock")) {
                                throw new AssertionError("forbidden customer-service capability leaked into " + route);
                            }
                        }
                        System.out.println("D5_WA_LOCAL_PAGES_OK");
                    }
                }
                """
            ).strip(),
            encoding="utf-8",
        )
        subprocess.run(
            [str(JAVAC), "-encoding", "UTF-8", "-cp", classpath(self.classes, JSON_JAR, DATA_LIBS), "-d", str(self.probe_classes), str(probe_source)],
            cwd=ROOT,
            check=True,
        )
        probe = subprocess.run(
            [str(JAVA), "-cp", classpath(self.probe_classes, self.classes, JSON_JAR, DATA_LIBS), "D5WaLocalPagesProbe"],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        self.assertEqual(probe.returncode, 0, probe.stdout + probe.stderr)
        self.assertIn("D5_WA_LOCAL_PAGES_OK", probe.stdout)

    def test_d5_wa_overlay_changes_only_catalog_and_local_bridge_on_geo_live_baseline(self):
        self.compile_patcher()
        live_baseline = ROOT / "data" / "app" / "App.dll"
        self.assertTrue(live_baseline.exists(), live_baseline)
        result = self.run_d5_wa_overlay(live_baseline)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("D5_WA_LOCAL_PAGES_OVERLAY", result.stdout)
        changed = {"com/sbf/util/http/SBFApi.class", "com/sbf/main/jxbrowser/M5LocalSpiderBridge.class"}
        with zipfile.ZipFile(live_baseline) as base_jar, zipfile.ZipFile(self.output_jar) as overlay_jar:
            self.assertEqual(base_jar.namelist(), overlay_jar.namelist())
            for entry_name in base_jar.namelist():
                if entry_name in changed or entry_name.endswith("/"):
                    continue
                self.assertEqual(base_jar.read(entry_name), overlay_jar.read(entry_name), entry_name)
        bridge_javap = subprocess.run(
            [str(JAVAP), "-classpath", str(self.output_jar), "-c", "-p", "com.sbf.main.jxbrowser.M5LocalSpiderBridge"],
            cwd=ROOT, text=True, encoding="utf-8", errors="replace", stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True,
        )
        self.assertIn("D5_WA_LOCAL_PAGE", bridge_javap.stdout)

    def test_d8_online_overlay_allows_only_original_backend_hosts(self):
        self.compile_patcher()
        live_baseline = ROOT / "data" / "app" / "App.dll"
        self.assertTrue(live_baseline.exists(), live_baseline)
        result = self.run_d8_online_overlay(live_baseline)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("D8_ONLINE_OVERLAY", result.stdout)

        changed = {
            "com/sbf/main/StartApp.class",
            "com/sbf/main/C64NativeNetworkDiag.class",
            "com/sbf/main/jxbrowser/M5LocalSpiderBridge.class",
        }
        with zipfile.ZipFile(live_baseline) as base_jar, zipfile.ZipFile(self.output_jar) as overlay_jar:
            self.assertEqual(base_jar.namelist(), overlay_jar.namelist())
            for entry_name in base_jar.namelist():
                if entry_name in changed or entry_name.endswith("/"):
                    continue
                self.assertEqual(base_jar.read(entry_name), overlay_jar.read(entry_name), entry_name)

        probe_source = self.tmp_path / "D8OnlineOverlayProbe.java"
        probe_source.write_text(
            textwrap.dedent(
                """
                import com.sbf.main.C64NativeNetworkDiag;
                import com.sbf.main.jxbrowser.M5LocalSpiderBridge;

                public class D8OnlineOverlayProbe {
                    public static void main(String[] args) {
                        String originalApi = M5LocalSpiderBridge.localWebAssetBody(
                                "https://app.xdxsoft.com/prod-api/system/google_sites/lists");
                        if (originalApi != null) {
                            throw new AssertionError("original API stayed locally intercepted: " + originalApi);
                        }
                        String originalRoute = M5LocalSpiderBridge.localWebAssetBody(
                                "https://app.xdxsoft.com/views/overseasAds/dataBoard");
                        if (originalRoute != null) {
                            throw new AssertionError("original route stayed locally intercepted: " + originalRoute);
                        }
                        if (C64NativeNetworkDiag.localResponse(
                                "https://app.xdxsoft.com/api/v1/client/pc/checkuser") != null) {
                            throw new AssertionError("original native gateway stayed locally intercepted");
                        }
                        String thirdParty = M5LocalSpiderBridge.localWebAssetBody(
                                "https://www.facebook.com/unsafe");
                        if (thirdParty == null || !thirdParty.contains("C62_EXTERNAL_REQUEST_BLOCKED")) {
                            throw new AssertionError("third-party browser request was not blocked: " + thirdParty);
                        }
                        System.out.println("D8_ONLINE_ALLOWLIST_OK");
                    }
                }
                """
            ).strip(),
            encoding="utf-8",
        )
        subprocess.run(
            [
                str(JAVAC),
                "-encoding",
                "UTF-8",
                "-cp",
                classpath(self.output_jar, JSON_JAR, DATA_LIBS),
                "-d",
                str(self.probe_classes),
                str(probe_source),
            ],
            cwd=ROOT,
            check=True,
        )
        probe = subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.probe_classes, self.output_jar, JSON_JAR, DATA_LIBS),
                "D8OnlineOverlayProbe",
            ],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        self.assertEqual(probe.returncode, 0, probe.stdout + probe.stderr)
        self.assertIn("D8_ONLINE_ALLOWLIST_OK", probe.stdout)

        startup_javap = subprocess.run(
            [str(JAVAP), "-classpath", str(self.output_jar), "-c", "-p", "com.sbf.main.StartApp"],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
            errors="replace",
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=True,
        )
        self.assertIn("offline.invalid", startup_javap.stdout)


if __name__ == "__main__":
    unittest.main()
