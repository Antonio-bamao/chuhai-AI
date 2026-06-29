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
ASM_JAR = ROOT / ".artifacts" / "tools" / "threadtear-gui-3.0.1-all.jar"
JSON_JAR = ROOT / "data" / "lib" / "json-20170516.jar"
DATA_LIBS = ROOT / "data" / "lib" / "*"
SOURCE = ROOT / "tools" / "m4_auth_patch" / "M4AuthPatch.java"
CATALOG_SOURCE = ROOT / "tools" / "m4_auth_patch" / "M4RecoveryCatalog.java"
LOCAL_SPIDER_BRIDGE_SOURCE = ROOT / "tools" / "m4_auth_patch" / "M5LocalSpiderBridge.java"
YES_CAPTCHA_BRIDGE_SOURCE = ROOT / "tools" / "m4_auth_patch" / "M5YesCaptchaBridge.java"
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
                classpath(ASM_JAR, JSON_JAR),
                "-d",
                str(self.classes),
                str(CATALOG_SOURCE),
                str(LOCAL_SPIDER_BRIDGE_SOURCE),
                str(YES_CAPTCHA_BRIDGE_SOURCE),
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
                        if (whatsappCollectParent) {
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
                            if (!"JSinglepage".equals(item.getString("localCode"))
                                    || !"/ingsale/aggregationKefu/index".equals(item.getString("linkUrl"))
                                    || !item.optString("evidence").contains("recovery-route:aggregation-kefu")
                                    || item.getInt("webFlg") != 1) {
                                throw new AssertionError("WhatsApp AI kefu recovery route: " + item);
                            }
                        } else if (whatsappKefuChild) {
                            if (!"/ingsale/aggregationKefu/index".equals(item.getString("localCode"))
                                    || !"JSinglepage:/ingsale/aggregationKefu/index".equals(item.getString("linkUrl"))
                                    || !item.optString("evidence").contains("recovery-route-child:j2026-h-field-map:aggregation-kefu")
                                    || item.getInt("webFlg") != 1) {
                                throw new AssertionError("WhatsApp AI kefu child recovery route: " + item);
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
                    int[] expectedCounts = {18, 10, 10, 9, 9, 11, 9, 7};
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
                    String expectedLink = "/ingsale/aggregationKefu/index";
                    if (!"JSinglepage".equals(target.optString("localCode"))
                            || !expectedLink.equals(target.optString("linkUrl"))
                            || !target.optString("evidence").contains("recovery-route:aggregation-kefu")) {
                        throw new AssertionError("wrong WhatsApp AI kefu route: " + target);
                    }
                    if (!expectedLink.equals(child.optString("localCode"))
                            || !"JSinglepage:/ingsale/aggregationKefu/index".equals(child.optString("linkUrl"))
                            || child.optInt("parentId") != target.optInt("id")
                            || !child.optString("evidence").contains("recovery-route-child:j2026-h-field-map:aggregation-kefu")) {
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
        return subprocess.run(
            [
                str(JAVA),
                "-cp",
                classpath(self.classes, ASM_JAR),
                "M4AuthPatch",
                str(APP_JAR),
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
                    "com/sbf/main/ext/gg/M5YesCaptchaBridge.class",
                    "com/sbf/main/jxbrowser/M5AuthBootstrapCallback.class",
                    "com/sbf/main/jxbrowser/M5ConsoleObserver.class",
                    "com/sbf/main/jxbrowser/M5InjectJsCallback.class",
                    "com/sbf/main/jxbrowser/M5LocalSpiderBridge$LocalPipelineRunner.class",
                    "com/sbf/main/jxbrowser/M5LocalSpiderBridge.class",
                    "com/sbf/main/jxbrowser/M5RequestObserver.class",
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
                "com/sbf/main/StartApp$1.class",
                "com/sbf/main/StartApp$3.class",
                "com/sbf/main/StartApp.class",
                "com/sbf/main/cloud/spider/SpiderCallback.class",
                "com/sbf/main/ext/gg/GoogleCRHelper.class",
                "com/sbf/main/ext/j2026/d$1.class",
                "com/sbf/main/ext/j2026/d$2.class",
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
        login_success_block = self.javap_method_block(
            "public final void a(org.json.JSONObject);",
            "com.sbf.main.StartApp$1",
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
        self.assertIn(
            "com/sbf/main/StartApp$1.a:(Lorg/json/JSONObject;)V",
            auto_login_block,
        )
        self.assertIn("M4B_SKIP_LOGIN_DISPOSE", login_success_block)
        self.assertIn("com/sbf/main/StartApp.t", login_success_block)
        self.assertIn("M4_V13_BROWSER_CONSTRUCTOR url=", browser_constructor_block)
        self.assertIn("M4_V13_BROWSER_CREATED=", browser_constructor_block)
        self.assertIn("m5InstallWebDiagnostics", browser_constructor_block)
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
        self.assertIn("M5LocalSpiderBridge.localWebAssetBody", scheme_callback_block)
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
        self.assertIn("M8_AI_KEFU_XHR_STUB", inject_js_callback_block)
        self.assertIn("M8_AI_KEFU_MIJAVA_SHIM", inject_js_callback_block)
        self.assertIn("/ingsale/aggregationKefu/index", inject_js_callback_block)
        self.assertIn("Proxy", inject_js_callback_block)
        self.assertIn("regMessageEvent", inject_js_callback_block)
        self.assertIn("toOpenFileSelect", inject_js_callback_block)
        self.assertIn("uploadFileDoHK", inject_js_callback_block)
        self.assertIn('\\"code\\":200', inject_js_callback_block)
        self.assertIn("InjectJsCallback$Response.proceed", inject_js_callback_block)
        self.assertIn("Frame.executeJavaScript", inject_js_callback_block)
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
        self.assertIn("M4_V13_VIEW_PARENT=", browser_layout_block)
        self.assertIn("M4_V13_VIEW_SIZE=", browser_layout_block)
        self.assertIn("BrowserView.getParent:()Ljava/awt/Container;", browser_layout_block)
        self.assertIn("BrowserView.getSize:()Ljava/awt/Dimension;", browser_layout_block)
        self.assertIn("RenderingMode.OFF_SCREEN", engine_create_block)
        self.assertNotIn("RenderingMode.HARDWARE_ACCELERATED", engine_create_block)
        self.assertIn("EngineOptions$Builder.disableGpu", engine_create_block)
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
                        if (menuEntries.length() != 83) {
                            throw new AssertionError("expected 83 recovered menus: " + menuEntries.length());
                        }
                        for (int menuIndex = 0; menuIndex < menuEntries.length(); menuIndex++) {
                            JSONObject recoveredMenu = menuEntries.getJSONObject(menuIndex);
                            boolean whatsappCollectParent =
                                    "C4749_006".equals(recoveredMenu.optString("code"));
                            boolean whatsappCollectChild =
                                    "REC_WHATSAPP_COLLECT_USERS_ROUTE".equals(recoveredMenu.optString("code"));
                            boolean whatsappCollectTabChild =
                                    recoveredMenu.optString("code").startsWith("REC_WHATSAPP_COLLECT_TAB_");
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
                            if (whatsappCollectParent) {
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
                                if (!"JSinglepage".equals(recoveredMenu.optString("localCode"))
                                        || !"/ingsale/aggregationKefu/index".equals(recoveredMenu.optString("linkUrl"))
                                        || !recoveredMenu.optString("evidence").contains("recovery-route:aggregation-kefu")) {
                                    throw new AssertionError("bad WhatsApp AI kefu recovery route: " + recoveredMenu);
                                }
                            } else if (whatsappKefuChild) {
                                if (!"/ingsale/aggregationKefu/index".equals(recoveredMenu.optString("localCode"))
                                        || !"JSinglepage:/ingsale/aggregationKefu/index".equals(recoveredMenu.optString("linkUrl"))
                                        || !recoveredMenu.optString("evidence").contains("recovery-route-child:j2026-h-field-map:aggregation-kefu")) {
                                    throw new AssertionError("bad WhatsApp AI kefu child route: " + recoveredMenu);
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


if __name__ == "__main__":
    unittest.main()
