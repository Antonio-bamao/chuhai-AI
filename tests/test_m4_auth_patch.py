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
                "-cp",
                str(ASM_JAR),
                "-d",
                str(self.classes),
                str(SOURCE),
            ],
            cwd=ROOT,
            check=True,
        )

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
        start = next(i for i, line in enumerate(lines) if method_header in line)
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
                    "com/sbf/main/jxbrowser/M5ConsoleObserver.class",
                    "com/sbf/main/jxbrowser/M5InjectJsCallback.class",
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
                "com/sbf/main/StartApp.class",
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
        web_token_bridge_block = self.javap_method_block(
            "public static java.lang.String f(java.lang.String);",
            "com.sbf.main.StartApp",
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
        self.assertIn("com/sbf/main/ext/j2026/h.e:()Ljava/lang/String;", modern_dispatch_block)
        self.assertIn("com/sbf/main/ext/j2026/h.h:()Ljava/lang/String;", modern_dispatch_block)
        self.assertIn("com/sbf/main/ext/j2026/h.i:()Ljava/lang/String;", modern_dispatch_block)
        self.assertIn("getLoingIsToken", web_token_bridge_block)
        self.assertIn("get_current_token", web_token_bridge_block)
        self.assertIn("M4_V19_WEB_TOKEN_BRIDGE url=", web_token_bridge_block)
        self.assertIn("offline-local-token-1234567890", web_token_bridge_block)
        self.assertIn("String.contains:(Ljava/lang/CharSequence;)Z", web_token_bridge_block)
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
        self.assertIn('\\"code\\":200', inject_js_callback_block)
        self.assertIn("InjectJsCallback$Response.proceed", inject_js_callback_block)
        self.assertIn("Frame.executeJavaScript", inject_js_callback_block)
        self.assertIn("M5_V21_GET_DICTS type=", dict_bridge_block)
        self.assertIn("[]", dict_bridge_block)
        self.assertIn("areturn", dict_bridge_block)
        self.assertIn("M4_V13_LOAD_URL=", browser_load_block)
        self.assertIn("M4_V18_NORMALIZED_URL=", browser_load_block)
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
                        if (products.length() == 0) {
                            throw new AssertionError("empty products");
                        }
                        JSONObject product = products.getJSONObject(0);
                        if (product.getInt("status") != 1 || product.getInt("remainingDays") < 0) {
                            throw new AssertionError("not enterable: " + product);
                        }
                        if (!product.has("name") || !product.has("displayName") || !product.has("themeStyle")) {
                            throw new AssertionError("product shape: " + product);
                        }
                        if (product.optString("logoSvg").length() == 0) {
                            throw new AssertionError("missing product logoSvg: " + product);
                        }
                        if (!"tiktok".equals(product.optString("code"))) {
                            throw new AssertionError("product code must map to an existing main logo: " + product);
                        }
                        JSONArray children = product.optJSONArray("children");
                        if (children == null || children.length() == 0) {
                            throw new AssertionError("missing product children: " + product);
                        }
                        JSONObject aigc = children.getJSONObject(0);
                        if (!"aigc".equals(aigc.optString("code"))) {
                            throw new AssertionError("missing aigc entry: " + children);
                        }
                        if (aigc.optInt("id") != 3457 || aigc.optString("name").length() == 0) {
                            throw new AssertionError("bad aigc entry: " + aigc);
                        }
                        JSONArray aigcChildren = aigc.optJSONArray("children");
                        if (aigcChildren == null || aigcChildren.length() == 0) {
                            throw new AssertionError("missing aigc children: " + aigc);
                        }
                        JSONObject firstAigcChild = aigcChildren.getJSONObject(0);
                        if (firstAigcChild.optString("code").length() == 0
                                || firstAigcChild.optString("linkUrl").length() == 0
                                || firstAigcChild.optInt("webFlg") != 1
                                || !"JSinglepage".equals(firstAigcChild.optString("localCode"))
                                || !"/pc/aicloud/my".equals(firstAigcChild.optString("linkUrl"))
                                || firstAigcChild.optString("linkUrl").contains("offline-home.html")) {
                            throw new AssertionError("bad aigc child entry: " + firstAigcChild);
                        }

                        JSONObject menus = SBFApi.k();
                        JSONArray menuEntries = menus.optJSONArray("scfs");
                        if (menuEntries == null) {
                            throw new AssertionError("missing top-level scfs: " + menus);
                        }
                        if (!menus.has("tas") || !menus.has("ucf")) {
                            throw new AssertionError("missing top-level menu metadata: " + menus);
                        }
                        if (menuEntries.length() == 0) {
                            throw new AssertionError("empty pc menus: " + menus);
                        }
                        JSONObject menuAigc = menuEntries.getJSONObject(0);
                        if (menuAigc.optInt("id") != 4795
                                || menuAigc.optInt("parentId") != 41
                                || menuAigc.optString("code").length() == 0
                                || menuAigc.optString("icon").length() == 0
                                || menuAigc.optString("name").length() == 0
                                || !"JSinglepage".equals(menuAigc.optString("localCode"))
                                || !"/pc/aicloud/my".equals(menuAigc.optString("linkUrl"))
                                || menuAigc.optString("linkUrl").contains("offline-home.html")) {
                            throw new AssertionError("bad j2026 menu item: " + menuAigc);
                        }
                        if (menuEntries.length() < 2) {
                            throw new AssertionError("missing flat j2026 child menu: " + menuEntries);
                        }
                        JSONObject firstMenuChild = menuEntries.getJSONObject(1);
                        if (firstMenuChild.optInt("parentId") != 4795
                                || !"JSinglepage".equals(firstMenuChild.optString("localCode"))
                                || !"/pc/aicloud/my".equals(firstMenuChild.optString("linkUrl"))) {
                            throw new AssertionError("bad flat j2026 child menu: " + firstMenuChild);
                        }
                        JSONObject secondMenuChild = menuEntries.getJSONObject(2);
                        if (secondMenuChild.optInt("parentId") != 4795
                                || !"JSinglepage".equals(secondMenuChild.optString("localCode"))
                                || !"/pc/aicloud/my".equals(secondMenuChild.optString("linkUrl"))) {
                            throw new AssertionError("bad second flat j2026 child menu: " + secondMenuChild);
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


if __name__ == "__main__":
    unittest.main()
