import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.objectweb.asm.Handle;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class M4AuthPatch {
    private static final String TARGET_CLASS = "com/sbf/util/http/SBFApi.class";
    private static final String UPDATE_CHECKER_CLASS = "com/sbf/util/http/SBFApi$5.class";
    private static final String TREE_NODE_CLASS = "com/sbf/main/tree/i.class";
    private static final String MENU_DISPATCH_CLASS = "com/sbf/main/sub/b.class";
    private static final String MODERN_MENU_DISPATCH_CLASS = "com/sbf/main/JSBFMain$4.class";
    private static final String MODERN_MENU_DISPATCH_CLASS_V2 = "com/sbf/main/JSBFMain$5.class";
    private static final String MODERN_MENU_MOUSE_CLASS = "com/sbf/main/ext/j2026/h$2.class";
    private static final String SIDE_MENU_MOUSE_CLASS = "com/sbf/main/ext/j2026/d$2.class";
    private static final String SIDE_MENU_CALLBACK_CLASS = "com/sbf/main/ext/j2026/d$1.class";
    private static final String START_APP_CLASS = "com/sbf/main/StartApp.class";
    private static final String START_APP_LOGIN_CALLBACK_CLASS = "com/sbf/main/StartApp$1.class";
    private static final String START_APP_PRODUCT_SELECTOR_CALLBACK_CLASS =
            "com/sbf/main/StartApp$1$3.class";
    private static final String START_APP_LOGIN_CALLBACK_CLASS_V2 = "com/sbf/main/StartApp$5.class";
    private static final String START_APP_PRODUCT_SELECTOR_CALLBACK_CLASS_V2 =
            "com/sbf/main/StartApp$5$3.class";
    private static final String START_APP_BOOTSTRAP_RUN_CLASS_V2 =
            "com/sbf/main/StartApp$7.class";
    private static final String START_APP_UI_CLASS = "com/sbf/main/StartApp$3.class";
    private static final String LOGIN_HTML_CLASS = "com/sbf/main/ext/j2026/JLoginHTML.class";
    private static final String MIJAVA_CLASS = "com/sbf/main/jxbrowser/MiJava.class";
    private static final String JXBROWSER_CLASS = "com/sbf/main/jxbrowser/c.class";
    private static final String JXBROWSER_LOAD_THREAD_CLASS = "com/sbf/main/jxbrowser/c$3.class";
    private static final String JXBROWSER_READY_LOAD_THREAD_CLASS =
            "com/sbf/main/jxbrowser/c$4.class";
    private static final String JXBROWSER_ENGINE_CLASS = "com/sbf/main/jxbrowser/g.class";
    private static final String JXBROWSER_SCHEME_CALLBACK_CLASS = "com/sbf/main/jxbrowser/b.class";
    private static final String M5_CONSOLE_OBSERVER_CLASS =
            "com/sbf/main/jxbrowser/M5ConsoleObserver.class";
    private static final String M5_AUTH_BOOTSTRAP_CALLBACK_CLASS =
            "com/sbf/main/jxbrowser/M5AuthBootstrapCallback.class";
    private static final String M5_INJECT_JS_CALLBACK_CLASS =
            "com/sbf/main/jxbrowser/M5InjectJsCallback.class";
    private static final String M5_LOCAL_SPIDER_BRIDGE_CLASS =
            "com/sbf/main/jxbrowser/M5LocalSpiderBridge.class";
    private static final String M5_LOCAL_SPIDER_BRIDGE_RUNNER_CLASS =
            "com/sbf/main/jxbrowser/M5LocalSpiderBridge$LocalPipelineRunner.class";
    private static final String M8_WHATSAPP_NATIVE_PROFILES_CLASS =
            "com/sbf/main/jxbrowser/M8WhatsAppNativeProfiles.class";
    private static final String M8_WHATSAPP_EXTERNAL_BROWSERS_CLASS =
            "com/sbf/main/jxbrowser/M8WhatsAppExternalBrowsers.class";
    private static final String M8_WHATSAPP_EXTERNAL_BROWSERS_ACCOUNT_HANDLER_CLASS =
            "com/sbf/main/jxbrowser/M8WhatsAppExternalBrowsers$AccountHandler.class";
    private static final String M8_WHATSAPP_EXTERNAL_BROWSERS_JSON_HANDLER_CLASS =
            "com/sbf/main/jxbrowser/M8WhatsAppExternalBrowsers$JsonHandler.class";
    private static final String M8_WHATSAPP_EXTERNAL_BROWSERS_MESSAGE_HANDLER_CLASS =
            "com/sbf/main/jxbrowser/M8WhatsAppExternalBrowsers$MessageHandler.class";
    private static final String M8_WHATSAPP_DRIP_CAMPAIGNS_CLASS =
            "com/sbf/main/jxbrowser/M8WhatsAppDripCampaigns.class";
    private static final String M5_REQUEST_OBSERVER_CLASS =
            "com/sbf/main/jxbrowser/M5RequestObserver.class";
    private static final String GOOGLE_CR_HELPER_CLASS =
            "com/sbf/main/ext/gg/GoogleCRHelper.class";
    private static final String SPIDER_CALLBACK_CLASS =
            "com/sbf/main/cloud/spider/SpiderCallback.class";
    private static final String M5_YES_CAPTCHA_BRIDGE_CLASS =
            "com/sbf/main/ext/gg/M5YesCaptchaBridge.class";
    private static final String M8D7_DEFAULT_MENU_DISPATCH_CLASS =
            "com/sbf/main/M8D7DefaultMenuDispatch.class";
    private static final String M8D14_EXE_DIAG_CLASS = "com/sbf/main/M8D14ExeDiag.class";
    private static final String DTHELPER_CLASS = "com/sbf/util/http/DTHelper.class";
    private static final String GLOBAL_RECHARGE_LISTENER_CLASS = "com/sbf/main/JSBFMain$6.class";
    private static final String C64_NATIVE_NETWORK_DIAG_CLASS = "com/sbf/main/C64NativeNetworkDiag.class";
    private static final String WEB_BRIDGE_TOKEN = "offline-local-token-1234567890";

    private static final String LOGIN_JSON_PREFIX =
            "{\"code\":200,\"msg\":\"offline login ok\","
                    + "\"token\":\"" + WEB_BRIDGE_TOKEN + "\","
                    + "\"sf\":\"41,aimirrorsystem,tiktok,HuoChaiAI,huochai-ai\","
                    + "\"data\":{"
                    + "\"token\":\"" + WEB_BRIDGE_TOKEN + "\","
                    + "\"userId\":1,\"tenantCode\":\"local\",\"nickname\":\"HuoChaiAI Local User\","
                    + "\"zone\":\"Asia/Shanghai\",\"time\":";
    private static final String LOGIN_JSON_SUFFIX =
            ",\"imConfig\":{},"
                    + "\"ucf\":{\"mnq_license_num\":999,\"ads_browsers_license_num\":999,"
                    + "\"open_mnq_ndk_license\":1,\"kefu_whatsapp_mass_sending_flg\":1}"
                    + "}}";

    private static final String GET_INFO_JSON =
            "{\"result\":{\"code\":200,\"msg\":\"offline ok\",\"data\":{"
                    + "\"user\":{\"id\":1,\"userId\":1,\"userName\":\"local@test.com\","
                    + "\"nickName\":\"HuoChaiAI Local User\","
                    + "\"nickname\":\"HuoChaiAI Local User\",\"ename\":\"Local\","
                    + "\"deptname\":\"Local\",\"phonenumber\":\"\",\"avatar\":\"\","
                    + "\"developerFlg\":1,\"tenantCode\":\"local\",\"certified\":1,\"EAdmin\":1},"
                    + "\"userId\":1,\"tenantCode\":\"local\",\"userName\":\"local@test.com\","
                    + "\"nickName\":\"HuoChaiAI Local User\",\"nickname\":\"HuoChaiAI Local User\","
                    + "\"certified\":1,\"EAdmin\":1,\"humanFlag\":1,"
                    + "\"periodTime\":\"2099-12-31 23:59:59\","
                    + "\"overdue\":0,\"roles\":["
                    + "\"enterprise_user_self_open\",\"tz_show_rpa_center\",\"aaa_ai_video_source\""
                    + "],\"ucf\":{\"mnq_license_num\":999,\"ads_browsers_license_num\":999,"
                    + "\"open_mnq_ndk_license\":1,\"kefu_whatsapp_mass_sending_flg\":1},"
                    + "\"im\":{\"ip\":\"127.0.0.1\",\"port\":{\"udp\":7901}}"
                    + "}}}";

    private static final String PC_MENUS_JSON = M4RecoveryCatalog.pcMenusJson();

    private static final String SPIDER_MODULES_JSON = "[]";

    private static final String WEB_BOOTSTRAP_GET_INFO_JSON =
            "{\"code\":200,\"msg\":\"success\",\"data\":{"
                    + "\"user\":{\"userId\":1,\"userName\":\"local@test.com\","
                    + "\"nickName\":\"HuoChaiAI Local User\",\"avatar\":\"\"},"
                    + "\"roles\":[\"admin\"],\"permissions\":[\"*:*:*\"]"
                    + "}}";

    private static final String WEB_BRIDGE_GET_INFO_JSON =
            "{\"user\":{\"userId\":1,\"userName\":\"local@test.com\","
                    + "\"nickName\":\"HuoChaiAI Local User\",\"avatar\":\"\"},"
                    + "\"roles\":[\"admin\"],\"permissions\":[\"*:*:*\"],"
                    + "\"periodTime\":\"2099-12-31 23:59:59\",\"overdue\":0}";

    private static final String LOCAL_WHATSAPP_USERS_CONFIG_JSON =
            "{\"code\":\"whatsapp_users_lists\",\"moduleCode\":\"whatsapp\",\"fields\":["
                    + "{\"dpIndex\":\"1\",\"code\":\"googSite\",\"name\":\"站点\",\"type\":\"text\"},"
                    + "{\"dpIndex\":\"2\",\"code\":\"pltCode\",\"name\":\"来源平台\",\"type\":\"text\"},"
                    + "{\"dpIndex\":\"3\",\"code\":\"keywords\",\"name\":\"相关关键词\",\"type\":\"text\"},"
                    + "{\"dpIndex\":\"0\",\"code\":\"phone\",\"name\":\"线索\",\"type\":\"text\"},"
                    + "{\"dpIndex\":\"7\",\"code\":\"date\",\"name\":\"采集时间\",\"type\":\"text\"},"
                    + "{\"dpIndex\":\"8\",\"code\":\"url\",\"name\":\"网址\",\"type\":\"text_url\"}"
                    + "],\"spiderParams\":["
                    + "{\"dpIndex\":\"1\",\"code\":\"googSite\",\"name\":\"搜索站点\",\"type\":\"select\"},"
                    + "{\"dpIndex\":\"2\",\"code\":\"areaCode\",\"name\":\"国家/区号\",\"type\":\"select\"},"
                    + "{\"dpIndex\":\"3\",\"code\":\"pltCode\",\"name\":\"平台\",\"type\":\"select\"},"
                    + "{\"dpIndex\":\"4\",\"code\":\"keywords\",\"name\":\"关键词\",\"type\":\"keyWords\"}"
                    + "]}";

    private static final String LOCAL_WHATSAPP_USERS_HTTP_CONFIG_JSON =
            "{\"code\":200,\"msg\":\"success\",\"data\":{"
                    + "\"code\":\"whatsapp_users_lists\","
                    + "\"moduleCode\":\"whatsapp\","
                    + "\"fields\":\"["
                    + "{\\\"dpIndex\\\":\\\"1\\\",\\\"code\\\":\\\"googSite\\\",\\\"name\\\":\\\"站点\\\",\\\"type\\\":\\\"text\\\"},"
                    + "{\\\"dpIndex\\\":\\\"2\\\",\\\"code\\\":\\\"pltCode\\\",\\\"name\\\":\\\"来源平台\\\",\\\"type\\\":\\\"text\\\"},"
                    + "{\\\"dpIndex\\\":\\\"3\\\",\\\"code\\\":\\\"keywords\\\",\\\"name\\\":\\\"相关关键词\\\",\\\"type\\\":\\\"text\\\"},"
                    + "{\\\"dpIndex\\\":\\\"0\\\",\\\"code\\\":\\\"phone\\\",\\\"name\\\":\\\"线索\\\",\\\"type\\\":\\\"text\\\"},"
                    + "{\\\"dpIndex\\\":\\\"7\\\",\\\"code\\\":\\\"date\\\",\\\"name\\\":\\\"采集时间\\\",\\\"type\\\":\\\"text\\\"},"
                    + "{\\\"dpIndex\\\":\\\"8\\\",\\\"code\\\":\\\"url\\\",\\\"name\\\":\\\"网址\\\",\\\"type\\\":\\\"text_url\\\"}"
                    + "]\","
                    + "\"spiderParams\":\"["
                    + "{\\\"dpIndex\\\":\\\"2\\\",\\\"code\\\":\\\"areaCode\\\",\\\"name\\\":\\\"选择国家区号\\\",\\\"type\\\":\\\"telArea\\\"},"
                    + "{\\\"dpIndex\\\":\\\"3\\\",\\\"code\\\":\\\"pltCode\\\",\\\"name\\\":\\\"选择相关平台\\\",\\\"type\\\":\\\"platform\\\"},"
                    + "{\\\"dpIndex\\\":\\\"4\\\",\\\"code\\\":\\\"keywords\\\",\\\"name\\\":\\\"关键词\\\",\\\"type\\\":\\\"keyWords\\\"}"
                    + "]\""
                    + "}}";

    private static final String WEB_BOOTSTRAP_CLOUD_HOST_LIST_JSON =
            "{\"code\":200,\"msg\":\"success\",\"data\":["
                    + "{\"authCode\":\"local\",\"title\":\"本机\",\"online\":1}"
                    + "]}";

    private static final String WEB_BOOTSTRAP_ROUTERS_JSON =
            "{\"code\":200,\"msg\":\"success\",\"data\":["
                    + "{\"name\":\"C5BigDataTask\",\"path\":\"/es/bigData/bigDataTask\",\"hidden\":false,\"component\":\"es/bigDataTask\",\"meta\":{\"title\":\"Big Data Task\",\"noCache\":true}},"
                    + "{\"name\":\"C5TelegramGroupTask\",\"path\":\"/pc/tg/index\",\"hidden\":false,\"component\":\"tg/index\",\"meta\":{\"title\":\"Telegram Group Task\",\"noCache\":true}},"
                    + "{\"name\":\"C5GeoGoogleSeo\",\"path\":\"/pc/dataCollect/googleseo\",\"hidden\":false,\"component\":\"dataCollect/googleseo\",\"meta\":{\"title\":\"GEO Website Search\",\"noCache\":true}},"
                    + "{\"name\":\"C5KefuConversation\",\"path\":\"/pc/kefu/conversation\",\"hidden\":false,\"component\":\"kefu/conversation\",\"meta\":{\"title\":\"Customer Service\",\"noCache\":true}}"
                    + "]}";

    private static final String WEB_BOOTSTRAP_AICLOUD_MYLIST_JSON =
            "{\"code\":200,\"msg\":\"success\",\"rows\":[],\"total\":0}";

    private static final String WEB_BOOTSTRAP_YES_NO_DICT_JSON =
            "{\"code\":200,\"msg\":\"success\",\"data\":["
                    + "{\"label\":\"启用\",\"value\":\"1\",\"dictLabel\":\"启用\",\"dictValue\":\"1\"},"
                    + "{\"label\":\"禁用\",\"value\":\"0\",\"dictLabel\":\"禁用\",\"dictValue\":\"0\"}"
                    + "]}";

    private M4AuthPatch() {
    }

    private static ClassWriter computeFramesWriter(ClassReader reader) {
        return new ClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                return "java/lang/Object";
            }
        };
    }

    public static void main(String[] args) throws Exception {
        boolean realProductMenuLogging = false;
        boolean c6CommerceOverlay = false;
        boolean c66CommerceOverlay = false;
        boolean c67AdvertisingOverlay = false;
        boolean d1XLocalPagesOverlay = false;
        boolean d2InsLocalPagesOverlay = false;
        boolean d3FbLocalPagesOverlay = false;
        boolean d4TkLocalPagesOverlay = false;
        boolean d5TgLocalPagesOverlay = false;
        boolean d5GeoLocalPagesOverlay = false;
        boolean d5WaLocalPagesOverlay = false;
        boolean d8OnlineOverlay = false;
        int argOffset = 0;
        if (args.length == 3 && "--real-product-menu-logging".equals(args[0])) {
            realProductMenuLogging = true;
            argOffset = 1;
        } else if (args.length == 3 && "--c6-overlay".equals(args[0])) {
            c6CommerceOverlay = true;
            argOffset = 1;
        } else if (args.length == 3 && "--c66-overlay".equals(args[0])) {
            c66CommerceOverlay = true;
            argOffset = 1;
        } else if (args.length == 3 && "--c67-overlay".equals(args[0])) {
            c67AdvertisingOverlay = true;
            argOffset = 1;
        } else if (args.length == 3 && "--d1-x-overlay".equals(args[0])) {
            d1XLocalPagesOverlay = true;
            argOffset = 1;
        } else if (args.length == 3 && "--d2-ins-overlay".equals(args[0])) {
            d2InsLocalPagesOverlay = true;
            argOffset = 1;
        } else if (args.length == 3 && "--d3-fb-overlay".equals(args[0])) {
            d3FbLocalPagesOverlay = true;
            argOffset = 1;
        } else if (args.length == 3 && "--d4-tk-overlay".equals(args[0])) {
            d4TkLocalPagesOverlay = true;
            argOffset = 1;
        } else if (args.length == 3 && "--d5-tg-overlay".equals(args[0])) {
            d5TgLocalPagesOverlay = true;
            argOffset = 1;
        } else if (args.length == 3 && "--d5-geo-overlay".equals(args[0])) {
            d5GeoLocalPagesOverlay = true;
            argOffset = 1;
        } else if (args.length == 3 && "--d5-wa-overlay".equals(args[0])) {
            d5WaLocalPagesOverlay = true;
            argOffset = 1;
        } else if (args.length == 3 && "--d8-online-overlay".equals(args[0])) {
            d8OnlineOverlay = true;
            argOffset = 1;
        }
        if (args.length - argOffset != 2) {
            throw new IllegalArgumentException(
                    "usage: M4AuthPatch [--real-product-menu-logging|--c6-overlay|--c66-overlay|--c67-overlay|--d1-x-overlay|--d2-ins-overlay|--d3-fb-overlay|--d4-tk-overlay|--d5-tg-overlay|--d5-geo-overlay|--d5-wa-overlay|--d8-online-overlay] <input-jar> <output-jar>");
        }
        Path input = Paths.get(args[argOffset]).toAbsolutePath().normalize();
        Path output = Paths.get(args[argOffset + 1]).toAbsolutePath().normalize();
        if (input.equals(output)) {
            throw new IllegalArgumentException("input and output must be different paths");
        }
        Path parent = output.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (c6CommerceOverlay) {
            writeC6CommerceOverlay(input, output);
            return;
        }
        if (c66CommerceOverlay) {
            writeC66CommerceOverlay(input, output);
            return;
        }
        if (c67AdvertisingOverlay) {
            writeC67AdvertisingOverlay(input, output);
            return;
        }
        if (d1XLocalPagesOverlay) {
            writeD1XLocalPagesOverlay(input, output);
            return;
        }
        if (d2InsLocalPagesOverlay) {
            writeD2InsLocalPagesOverlay(input, output);
            return;
        }
        if (d3FbLocalPagesOverlay) {
            writeD3FbLocalPagesOverlay(input, output);
            return;
        }
        if (d4TkLocalPagesOverlay) {
            writeD4TkLocalPagesOverlay(input, output);
            return;
        }
        if (d5TgLocalPagesOverlay) {
            writeD5TgLocalPagesOverlay(input, output);
            return;
        }
        if (d5GeoLocalPagesOverlay) {
            writeD5GeoLocalPagesOverlay(input, output);
            return;
        }
        if (d5WaLocalPagesOverlay) {
            writeD5WaLocalPagesOverlay(input, output);
            return;
        }
        if (d8OnlineOverlay) {
            writeD8OnlineOverlay(input, output);
            return;
        }

        Path temp = output.resolveSibling(output.getFileName().toString() + ".tmp");
        Files.deleteIfExists(temp);
        String productModuleJson =
                M4RecoveryCatalog.productModulesJson(decodeRecoveryProductLogos(input));
        PatchResult result = patchJar(input, temp, realProductMenuLogging, productModuleJson);
        if (!result.patchedLogin
                || !result.patchedGetInfo
                || !result.patchedProductModules
                || !result.patchedPcMenus
                || !result.patchedSpiderModules
                || !result.patchedLocalSpiderGetNewTask
                || !result.patchedLocalSpiderCancelAllRun
                || !result.patchedUpdateChecker
                || !result.patchedTreeDiagnostics
                || !result.patchedMenuDispatchDiagnostics
                || !result.patchedModernMenuMouseDiagnostics
                || !result.patchedSideMenuMouseDiagnostics
                || !result.patchedSideMenuCallbackDiagnostics
                || !result.patchedStartAppExeDiagBootstrap
                || !result.patchedStartAppWebTokenBridge
                || !result.patchedProductSelectorEnterBridge
                || !result.patchedTrueExeLoginBridge
                || !result.patchedJxBrowserDiagnostics
                || !result.patchedJxBrowserEngine
                || !result.patchedLocalWebSchemeCallback
                || !result.patchedGoogleCRHelper
                || !result.patchedMiJavaDictBridge
                || !result.patchedLocalSpiderTaskGet
                || !result.patchedLocalSpiderTaskStatus
                || !result.addedM5ConsoleObserver
                || !result.addedM5AuthBootstrapCallback
                || !result.addedM5InjectJsCallback
                || !result.addedM5LocalSpiderBridge
                || !result.addedM8WhatsAppNativeProfiles
                || !result.addedM8WhatsAppExternalBrowsers
                || !result.addedM5RequestObserver
                || !result.addedM5YesCaptchaBridge
                || !result.addedM8D7DefaultMenuDispatch
                || !result.addedM8D14ExeDiag
                || !result.patchedSpiderCallbackPostData
                || !result.patchedSpiderCallbackEndTask) {
            Files.deleteIfExists(temp);
            throw new IllegalStateException(
                    "failed to patch SBFApi auth/menu methods and diagnostics: "
                            + result.missingFlags());
        }
        Files.move(temp, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        System.out.println(
                "patched "
                        + TARGET_CLASS
                        + " -> "
                        + output
                        + (realProductMenuLogging ? " [real-product-menu-logging]" : ""));
    }

    private static void writeC6CommerceOverlay(Path input, Path output) throws IOException {
        Path temp = output.resolveSibling(output.getFileName().toString() + ".tmp");
        Files.deleteIfExists(temp);
        byte[] bridgeBytes = readGeneratedSupportClass(M5_LOCAL_SPIDER_BRIDGE_CLASS);
        byte[] nativeNetworkDiagBytes = readGeneratedSupportClass(C64_NATIVE_NETWORK_DIAG_CLASS);
        boolean replacedBridge = false;
        boolean patchedEngine = false;
        boolean patchedNativeUpdate = false;
        boolean patchedNativeStartup = false;
        boolean patchedNativeNetworkDiagnostics = false;
        boolean patchedGlobalRechargeListener = false;
        Set<String> names = new HashSet<String>();
        try (JarFile jar = new JarFile(input.toFile());
                OutputStream fileOut = Files.newOutputStream(temp);
                JarOutputStream jarOut = new JarOutputStream(fileOut)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!names.add(entry.getName())) {
                    continue;
                }
                jarOut.putNextEntry(copyEntryMetadata(entry));
                if (!entry.isDirectory()) {
                    byte[] bytes;
                    if (M5_LOCAL_SPIDER_BRIDGE_CLASS.equals(entry.getName())) {
                        bytes = bridgeBytes;
                        replacedBridge = true;
                    } else if (JXBROWSER_ENGINE_CLASS.equals(entry.getName())) {
                        try (InputStream in = jar.getInputStream(entry)) {
                            bytes = patchJxBrowserOfflineNetworkSwitches(readAll(in));
                        }
                        patchedEngine = true;
                    } else if (TARGET_CLASS.equals(entry.getName())) {
                        try (InputStream in = jar.getInputStream(entry)) {
                            bytes = patchC64NativeUpdateCheck(readAll(in));
                        }
                        patchedNativeUpdate = true;
                    } else if (START_APP_CLASS.equals(entry.getName())) {
                        try (InputStream in = jar.getInputStream(entry)) {
                            bytes = patchC64NativeStartupAuthorization(readAll(in));
                        }
                        patchedNativeStartup = true;
                    } else if (DTHELPER_CLASS.equals(entry.getName())) {
                        try (InputStream in = jar.getInputStream(entry)) {
                            bytes = patchC65NativeStartupGateway(
                                    patchC64NativeUrlDiagnostics(readAll(in)));
                        }
                        patchedNativeNetworkDiagnostics = true;
                    } else if (GLOBAL_RECHARGE_LISTENER_CLASS.equals(entry.getName())) {
                        try (InputStream in = jar.getInputStream(entry)) {
                            bytes = patchC66GlobalRechargeListener(readAll(in));
                        }
                        patchedGlobalRechargeListener = true;
                    } else {
                        try (InputStream in = jar.getInputStream(entry)) {
                            bytes = readAll(in);
                        }
                    }
                    jarOut.write(bytes);
                }
                jarOut.closeEntry();
            }
            if (names.add(C64_NATIVE_NETWORK_DIAG_CLASS)) {
                writeGeneratedClass(jarOut, C64_NATIVE_NETWORK_DIAG_CLASS, nativeNetworkDiagBytes);
            }
        }
        if (!replacedBridge
                || !patchedEngine
                || !patchedNativeUpdate
                || !patchedNativeStartup
                || !patchedNativeNetworkDiagnostics
                || !patchedGlobalRechargeListener) {
            Files.deleteIfExists(temp);
            throw new IllegalStateException(
                    "C6 overlay requires "
                            + M5_LOCAL_SPIDER_BRIDGE_CLASS
                            + " and "
                            + JXBROWSER_ENGINE_CLASS
                            + ", "
                            + TARGET_CLASS
                            + " and "
                            + START_APP_CLASS
                            + " and "
                            + DTHELPER_CLASS
                            + " and "
                            + GLOBAL_RECHARGE_LISTENER_CLASS
                            + " in input jar");
        }
        Files.move(temp, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        System.out.println(
                "C6_COMMERCE_OVERLAY localBridge="
                        + M5_LOCAL_SPIDER_BRIDGE_CLASS
                        + " offlineEngine="
                        + JXBROWSER_ENGINE_CLASS
                        + " nativeUpdate="
                        + TARGET_CLASS
                        + " nativeStartup="
                        + START_APP_CLASS
                        + " nativeNetworkDiagnostics="
                        + DTHELPER_CLASS
                        + " nativeHostGateway="
                        + DTHELPER_CLASS
                        + " globalRecharge="
                        + GLOBAL_RECHARGE_LISTENER_CLASS
                        + " input="
                        + input
                        + " output="
                        + output);
    }

    private static void writeC66CommerceOverlay(Path input, Path output) throws IOException {
        Path temp = output.resolveSibling(output.getFileName().toString() + ".tmp");
        Files.deleteIfExists(temp);
        byte[] bridgeBytes = readGeneratedSupportClass(M5_LOCAL_SPIDER_BRIDGE_CLASS);
        boolean replacedBridge = false;
        boolean patchedGlobalRechargeListener = false;
        Set<String> names = new HashSet<String>();
        try (JarFile jar = new JarFile(input.toFile());
                OutputStream fileOut = Files.newOutputStream(temp);
                JarOutputStream jarOut = new JarOutputStream(fileOut)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!names.add(entry.getName())) {
                    continue;
                }
                jarOut.putNextEntry(copyEntryMetadata(entry));
                if (!entry.isDirectory()) {
                    byte[] bytes;
                    if (M5_LOCAL_SPIDER_BRIDGE_CLASS.equals(entry.getName())) {
                        bytes = bridgeBytes;
                        replacedBridge = true;
                    } else if (GLOBAL_RECHARGE_LISTENER_CLASS.equals(entry.getName())) {
                        try (InputStream in = jar.getInputStream(entry)) {
                            bytes = patchC66GlobalRechargeListener(readAll(in));
                        }
                        patchedGlobalRechargeListener = true;
                    } else {
                        try (InputStream in = jar.getInputStream(entry)) {
                            bytes = readAll(in);
                        }
                    }
                    jarOut.write(bytes);
                }
                jarOut.closeEntry();
            }
        }
        if (!replacedBridge || !patchedGlobalRechargeListener) {
            Files.deleteIfExists(temp);
            throw new IllegalStateException(
                    "C66 overlay requires "
                            + M5_LOCAL_SPIDER_BRIDGE_CLASS
                            + " and "
                            + GLOBAL_RECHARGE_LISTENER_CLASS
                            + " in input jar");
        }
        Files.move(temp, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        System.out.println(
                "C66_COMMERCE_OVERLAY localBridge="
                        + M5_LOCAL_SPIDER_BRIDGE_CLASS
                        + " globalRecharge="
                        + GLOBAL_RECHARGE_LISTENER_CLASS
                        + " input="
                        + input
                        + " output="
                        + output);
    }

    private static void writeC67AdvertisingOverlay(Path input, Path output) throws IOException {
        Path temp = output.resolveSibling(output.getFileName().toString() + ".tmp");
        Files.deleteIfExists(temp);
        byte[] bridgeBytes = readGeneratedSupportClass(M5_LOCAL_SPIDER_BRIDGE_CLASS);
        boolean replacedPcMenus = false;
        boolean patchedModernDispatch = false;
        boolean replacedBridge = false;
        Set<String> names = new HashSet<String>();
        try (JarFile jar = new JarFile(input.toFile());
                OutputStream fileOut = Files.newOutputStream(temp);
                JarOutputStream jarOut = new JarOutputStream(fileOut)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!names.add(entry.getName())) {
                    continue;
                }
                jarOut.putNextEntry(copyEntryMetadata(entry));
                if (!entry.isDirectory()) {
                    byte[] bytes;
                    if (TARGET_CLASS.equals(entry.getName())) {
                        try (InputStream in = jar.getInputStream(entry)) {
                            bytes = patchC67PcMenus(readAll(in));
                        }
                        replacedPcMenus = true;
                    } else if (M5_LOCAL_SPIDER_BRIDGE_CLASS.equals(entry.getName())) {
                        bytes = bridgeBytes;
                        replacedBridge = true;
                    } else if (MODERN_MENU_DISPATCH_CLASS_V2.equals(entry.getName())) {
                        try (InputStream in = jar.getInputStream(entry)) {
                            bytes = patchModernMenuDispatchDiagnostics(readAll(in), new PatchResult());
                        }
                        patchedModernDispatch = true;
                    } else {
                        try (InputStream in = jar.getInputStream(entry)) {
                            bytes = readAll(in);
                        }
                    }
                    jarOut.write(bytes);
                }
                jarOut.closeEntry();
            }
        }
        if (!replacedPcMenus || !patchedModernDispatch || !replacedBridge) {
            Files.deleteIfExists(temp);
            throw new IllegalStateException(
                    "C67 overlay requires "
                            + TARGET_CLASS
                            + " and "
                            + M5_LOCAL_SPIDER_BRIDGE_CLASS
                            + " and "
                            + MODERN_MENU_DISPATCH_CLASS_V2
                            + " in input jar");
        }
        Files.move(temp, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        System.out.println(
                "C67_ADVERTISING_OVERLAY pcMenus="
                        + TARGET_CLASS
                        + " localBridge="
                        + M5_LOCAL_SPIDER_BRIDGE_CLASS
                        + " modernDispatch="
                        + MODERN_MENU_DISPATCH_CLASS_V2
                        + " input="
                        + input
                        + " output="
                        + output);
    }

    private static byte[] patchC67PcMenus(byte[] original) {
        return patchPcMenusOverlay(
                original, "C67_ADVERTISING_MENU_DISPATCH route=/views/overseasAds/dataBoard", "C67");
    }

    private static byte[] patchPcMenusOverlay(byte[] original, String marker, String overlayName) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(reader, 0);
        final boolean[] patched = {false};
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                if (!"k".equals(name) || !"()Lorg/json/JSONObject;".equals(descriptor)) {
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }
                patched[0] = true;
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                mv.visitCode();
                emitPrint(mv, marker);
                mv.visitTypeInsn(Opcodes.NEW, "org/json/JSONObject");
                mv.visitInsn(Opcodes.DUP);
                mv.visitLdcInsn(PC_MENUS_JSON);
                mv.visitMethodInsn(
                        Opcodes.INVOKESPECIAL,
                        "org/json/JSONObject",
                        "<init>",
                        "(Ljava/lang/String;)V",
                        false);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(3, 0);
                mv.visitEnd();
                return null;
            }
        };
        reader.accept(visitor, 0);
        if (!patched[0]) {
            throw new IllegalStateException(overlayName + " PC menu method k() was not found");
        }
        return writer.toByteArray();
    }

    private static void writeD8OnlineOverlay(Path input, Path output) throws IOException {
        Path temp = output.resolveSibling(output.getFileName().toString() + ".tmp");
        Files.deleteIfExists(temp);
        byte[] bridgeBytes = patchD8OnlineEnabledFlag(
                readGeneratedSupportClass(M5_LOCAL_SPIDER_BRIDGE_CLASS));
        byte[] nativeNetworkDiagBytes = patchD8OnlineEnabledFlag(
                readGeneratedSupportClass(C64_NATIVE_NETWORK_DIAG_CLASS));
        boolean replacedBridge = false;
        boolean replacedNativeNetworkDiag = false;
        boolean patchedNativeStartup = false;
        Set<String> names = new HashSet<String>();
        try (JarFile jar = new JarFile(input.toFile());
                OutputStream fileOut = Files.newOutputStream(temp);
                JarOutputStream jarOut = new JarOutputStream(fileOut)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!names.add(entry.getName())) {
                    continue;
                }
                jarOut.putNextEntry(copyEntryMetadata(entry));
                if (!entry.isDirectory()) {
                    byte[] bytes;
                    if (M5_LOCAL_SPIDER_BRIDGE_CLASS.equals(entry.getName())) {
                        bytes = bridgeBytes;
                        replacedBridge = true;
                    } else if (C64_NATIVE_NETWORK_DIAG_CLASS.equals(entry.getName())) {
                        bytes = nativeNetworkDiagBytes;
                        replacedNativeNetworkDiag = true;
                    } else if (START_APP_CLASS.equals(entry.getName())) {
                        try (InputStream in = jar.getInputStream(entry)) {
                            bytes = patchD8OnlineStartupAuthorization(readAll(in));
                        }
                        patchedNativeStartup = true;
                    } else {
                        try (InputStream in = jar.getInputStream(entry)) {
                            bytes = readAll(in);
                        }
                    }
                    jarOut.write(bytes);
                }
                jarOut.closeEntry();
            }
        }
        if (!replacedBridge || !replacedNativeNetworkDiag || !patchedNativeStartup) {
            Files.deleteIfExists(temp);
            throw new IllegalStateException(
                    "D8 online overlay requires "
                            + M5_LOCAL_SPIDER_BRIDGE_CLASS
                            + ", "
                            + C64_NATIVE_NETWORK_DIAG_CLASS
                            + " and "
                            + START_APP_CLASS
                            + " in input jar");
        }
        Files.move(temp, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        System.out.println(
                "D8_ONLINE_OVERLAY originalHosts=xdxsoft,huochai,mierp localBridge="
                        + M5_LOCAL_SPIDER_BRIDGE_CLASS
                        + " nativeGateway="
                        + C64_NATIVE_NETWORK_DIAG_CLASS
                        + " nativeStartup="
                        + START_APP_CLASS
                        + " input="
                        + input
                        + " output="
                        + output);
    }

    private static void writeD1XLocalPagesOverlay(Path input, Path output) throws IOException {
        Path temp = output.resolveSibling(output.getFileName().toString() + ".tmp");
        Files.deleteIfExists(temp);
        byte[] bridgeBytes = readGeneratedSupportClass(M5_LOCAL_SPIDER_BRIDGE_CLASS);
        boolean replacedPcMenus = false;
        boolean replacedBridge = false;
        Set<String> names = new HashSet<String>();
        try (JarFile jar = new JarFile(input.toFile());
                OutputStream fileOut = Files.newOutputStream(temp);
                JarOutputStream jarOut = new JarOutputStream(fileOut)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!names.add(entry.getName())) {
                    continue;
                }
                jarOut.putNextEntry(copyEntryMetadata(entry));
                if (!entry.isDirectory()) {
                    byte[] bytes;
                    if (TARGET_CLASS.equals(entry.getName())) {
                        try (InputStream in = jar.getInputStream(entry)) {
                            bytes = patchPcMenusOverlay(
                                    readAll(in),
                                    "D1_X_MENU_DISPATCH localLeaves=/pc/local/x/*",
                                    "D1 X local pages");
                        }
                        replacedPcMenus = true;
                    } else if (M5_LOCAL_SPIDER_BRIDGE_CLASS.equals(entry.getName())) {
                        bytes = bridgeBytes;
                        replacedBridge = true;
                    } else {
                        try (InputStream in = jar.getInputStream(entry)) {
                            bytes = readAll(in);
                        }
                    }
                    jarOut.write(bytes);
                }
                jarOut.closeEntry();
            }
        }
        if (!replacedPcMenus || !replacedBridge) {
            Files.deleteIfExists(temp);
            throw new IllegalStateException(
                    "D1 X overlay requires " + TARGET_CLASS + " and " + M5_LOCAL_SPIDER_BRIDGE_CLASS);
        }
        Files.move(temp, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        System.out.println(
                "D1_X_LOCAL_PAGES_OVERLAY pcMenus="
                        + TARGET_CLASS
                        + " localBridge="
                        + M5_LOCAL_SPIDER_BRIDGE_CLASS
                        + " input="
                        + input
                        + " output="
                        + output);
    }

    private static void writeD2InsLocalPagesOverlay(Path input, Path output) throws IOException {
        Path temp = output.resolveSibling(output.getFileName().toString() + ".tmp");
        Files.deleteIfExists(temp);
        byte[] bridgeBytes = readGeneratedSupportClass(M5_LOCAL_SPIDER_BRIDGE_CLASS);
        boolean replacedPcMenus = false;
        boolean replacedBridge = false;
        Set<String> names = new HashSet<String>();
        try (JarFile jar = new JarFile(input.toFile());
                OutputStream fileOut = Files.newOutputStream(temp);
                JarOutputStream jarOut = new JarOutputStream(fileOut)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!names.add(entry.getName())) {
                    continue;
                }
                jarOut.putNextEntry(copyEntryMetadata(entry));
                if (!entry.isDirectory()) {
                    byte[] bytes;
                    if (TARGET_CLASS.equals(entry.getName())) {
                        try (InputStream in = jar.getInputStream(entry)) {
                            bytes = patchPcMenusOverlay(
                                    readAll(in),
                                    "D2_INS_MENU_DISPATCH localLeaves=/pc/local/ins/*",
                                    "D2 Instagram local pages");
                        }
                        replacedPcMenus = true;
                    } else if (M5_LOCAL_SPIDER_BRIDGE_CLASS.equals(entry.getName())) {
                        bytes = bridgeBytes;
                        replacedBridge = true;
                    } else {
                        try (InputStream in = jar.getInputStream(entry)) {
                            bytes = readAll(in);
                        }
                    }
                    jarOut.write(bytes);
                }
                jarOut.closeEntry();
            }
        }
        if (!replacedPcMenus || !replacedBridge) {
            Files.deleteIfExists(temp);
            throw new IllegalStateException(
                    "D2 Instagram overlay requires " + TARGET_CLASS + " and " + M5_LOCAL_SPIDER_BRIDGE_CLASS);
        }
        Files.move(temp, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        System.out.println(
                "D2_INS_LOCAL_PAGES_OVERLAY pcMenus="
                        + TARGET_CLASS
                        + " localBridge="
                        + M5_LOCAL_SPIDER_BRIDGE_CLASS
                        + " input="
                        + input
                        + " output="
                        + output);
    }

    private static void writeD3FbLocalPagesOverlay(Path input, Path output) throws IOException {
        Path temp = output.resolveSibling(output.getFileName().toString() + ".tmp");
        Files.deleteIfExists(temp);
        byte[] bridgeBytes = readGeneratedSupportClass(M5_LOCAL_SPIDER_BRIDGE_CLASS);
        boolean replacedPcMenus = false;
        boolean replacedBridge = false;
        Set<String> names = new HashSet<String>();
        try (JarFile jar = new JarFile(input.toFile());
                OutputStream fileOut = Files.newOutputStream(temp);
                JarOutputStream jarOut = new JarOutputStream(fileOut)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!names.add(entry.getName())) {
                    continue;
                }
                jarOut.putNextEntry(copyEntryMetadata(entry));
                if (!entry.isDirectory()) {
                    byte[] bytes;
                    if (TARGET_CLASS.equals(entry.getName())) {
                        try (InputStream in = jar.getInputStream(entry)) {
                            bytes = patchPcMenusOverlay(
                                    readAll(in),
                                    "D3_FB_MENU_DISPATCH localLeaves=/pc/local/fb/*",
                                    "D3 Facebook local pages");
                        }
                        replacedPcMenus = true;
                    } else if (M5_LOCAL_SPIDER_BRIDGE_CLASS.equals(entry.getName())) {
                        bytes = bridgeBytes;
                        replacedBridge = true;
                    } else {
                        try (InputStream in = jar.getInputStream(entry)) {
                            bytes = readAll(in);
                        }
                    }
                    jarOut.write(bytes);
                }
                jarOut.closeEntry();
            }
        }
        if (!replacedPcMenus || !replacedBridge) {
            Files.deleteIfExists(temp);
            throw new IllegalStateException(
                    "D3 Facebook overlay requires " + TARGET_CLASS + " and " + M5_LOCAL_SPIDER_BRIDGE_CLASS);
        }
        Files.move(temp, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        System.out.println(
                "D3_FB_LOCAL_PAGES_OVERLAY pcMenus="
                        + TARGET_CLASS
                        + " localBridge="
                        + M5_LOCAL_SPIDER_BRIDGE_CLASS
                        + " input="
                        + input
                        + " output="
                        + output);
    }

    private static void writeD4TkLocalPagesOverlay(Path input, Path output) throws IOException {
        Path temp = output.resolveSibling(output.getFileName().toString() + ".tmp"); Files.deleteIfExists(temp);
        byte[] bridgeBytes = readGeneratedSupportClass(M5_LOCAL_SPIDER_BRIDGE_CLASS); boolean menus = false, bridge = false;
        Set<String> names = new HashSet<String>();
        try (JarFile jar = new JarFile(input.toFile()); OutputStream fileOut = Files.newOutputStream(temp); JarOutputStream jarOut = new JarOutputStream(fileOut)) {
            Enumeration<JarEntry> entries = jar.entries(); while (entries.hasMoreElements()) { JarEntry entry = entries.nextElement(); if (!names.add(entry.getName())) continue; jarOut.putNextEntry(copyEntryMetadata(entry));
                if (!entry.isDirectory()) { byte[] bytes; if (TARGET_CLASS.equals(entry.getName())) { try (InputStream in = jar.getInputStream(entry)) { bytes = patchPcMenusOverlay(readAll(in), "D4_TK_MENU_DISPATCH localLeaves=/pc/local/tiktok/*", "D4 TikTok local pages"); } menus = true; }
                    else if (M5_LOCAL_SPIDER_BRIDGE_CLASS.equals(entry.getName())) { bytes = bridgeBytes; bridge = true; } else { try (InputStream in = jar.getInputStream(entry)) { bytes = readAll(in); } } jarOut.write(bytes); } jarOut.closeEntry(); }
        }
        if (!menus || !bridge) { Files.deleteIfExists(temp); throw new IllegalStateException("D4 TikTok overlay requires " + TARGET_CLASS + " and " + M5_LOCAL_SPIDER_BRIDGE_CLASS); }
        Files.move(temp, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        System.out.println("D4_TK_LOCAL_PAGES_OVERLAY pcMenus=" + TARGET_CLASS + " localBridge=" + M5_LOCAL_SPIDER_BRIDGE_CLASS + " input=" + input + " output=" + output);
    }

    private static void writeD5TgLocalPagesOverlay(Path input, Path output) throws IOException {
        Path temp = output.resolveSibling(output.getFileName().toString() + ".tmp");
        Files.deleteIfExists(temp);
        byte[] bridgeBytes = readGeneratedSupportClass(M5_LOCAL_SPIDER_BRIDGE_CLASS);
        boolean menus = false;
        boolean bridge = false;
        Set<String> names = new HashSet<String>();
        try (JarFile jar = new JarFile(input.toFile());
                OutputStream fileOut = Files.newOutputStream(temp);
                JarOutputStream jarOut = new JarOutputStream(fileOut)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!names.add(entry.getName())) {
                    continue;
                }
                jarOut.putNextEntry(copyEntryMetadata(entry));
                if (!entry.isDirectory()) {
                    byte[] bytes;
                    if (TARGET_CLASS.equals(entry.getName())) {
                        try (InputStream in = jar.getInputStream(entry)) {
                            bytes = patchPcMenusOverlay(readAll(in),
                                    "D5_TG_MENU_DISPATCH localLeaves=/pc/local/tg/*",
                                    "D5 Telegram local pages");
                        }
                        menus = true;
                    } else if (M5_LOCAL_SPIDER_BRIDGE_CLASS.equals(entry.getName())) {
                        bytes = bridgeBytes;
                        bridge = true;
                    } else {
                        try (InputStream in = jar.getInputStream(entry)) {
                            bytes = readAll(in);
                        }
                    }
                    jarOut.write(bytes);
                }
                jarOut.closeEntry();
            }
        }
        if (!menus || !bridge) {
            Files.deleteIfExists(temp);
            throw new IllegalStateException("D5 Telegram overlay requires " + TARGET_CLASS
                    + " and " + M5_LOCAL_SPIDER_BRIDGE_CLASS);
        }
        Files.move(temp, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        System.out.println("D5_TG_LOCAL_PAGES_OVERLAY pcMenus=" + TARGET_CLASS
                + " localBridge=" + M5_LOCAL_SPIDER_BRIDGE_CLASS + " input=" + input
                + " output=" + output);
    }

    private static void writeD5GeoLocalPagesOverlay(Path input, Path output) throws IOException {
        Path temp = output.resolveSibling(output.getFileName().toString() + ".tmp");
        Files.deleteIfExists(temp);
        byte[] bridgeBytes = readGeneratedSupportClass(M5_LOCAL_SPIDER_BRIDGE_CLASS);
        boolean menus = false;
        boolean bridge = false;
        Set<String> names = new HashSet<String>();
        try (JarFile jar = new JarFile(input.toFile());
                OutputStream fileOut = Files.newOutputStream(temp);
                JarOutputStream jarOut = new JarOutputStream(fileOut)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!names.add(entry.getName())) {
                    continue;
                }
                jarOut.putNextEntry(copyEntryMetadata(entry));
                if (!entry.isDirectory()) {
                    byte[] bytes;
                    if (TARGET_CLASS.equals(entry.getName())) {
                        try (InputStream in = jar.getInputStream(entry)) {
                            bytes = patchPcMenusOverlay(readAll(in),
                                    "D5_GEO_MENU_DISPATCH localLeaves=/pc/local/geo/*",
                                    "D5 GEO local pages");
                        }
                        menus = true;
                    } else if (M5_LOCAL_SPIDER_BRIDGE_CLASS.equals(entry.getName())) {
                        bytes = bridgeBytes;
                        bridge = true;
                    } else {
                        try (InputStream in = jar.getInputStream(entry)) {
                            bytes = readAll(in);
                        }
                    }
                    jarOut.write(bytes);
                }
                jarOut.closeEntry();
            }
        }
        if (!menus || !bridge) {
            Files.deleteIfExists(temp);
            throw new IllegalStateException("D5 GEO overlay requires " + TARGET_CLASS
                    + " and " + M5_LOCAL_SPIDER_BRIDGE_CLASS);
        }
        Files.move(temp, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        System.out.println("D5_GEO_LOCAL_PAGES_OVERLAY pcMenus=" + TARGET_CLASS
                + " localBridge=" + M5_LOCAL_SPIDER_BRIDGE_CLASS + " input=" + input
                + " output=" + output);
    }

    private static void writeD5WaLocalPagesOverlay(Path input, Path output) throws IOException {
        Path temp = output.resolveSibling(output.getFileName().toString() + ".tmp");
        Files.deleteIfExists(temp);
        byte[] bridgeBytes = readGeneratedSupportClass(M5_LOCAL_SPIDER_BRIDGE_CLASS);
        boolean menus = false;
        boolean bridge = false;
        Set<String> names = new HashSet<String>();
        try (JarFile jar = new JarFile(input.toFile());
                OutputStream fileOut = Files.newOutputStream(temp);
                JarOutputStream jarOut = new JarOutputStream(fileOut)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!names.add(entry.getName())) continue;
                jarOut.putNextEntry(copyEntryMetadata(entry));
                if (!entry.isDirectory()) {
                    byte[] bytes;
                    if (TARGET_CLASS.equals(entry.getName())) {
                        try (InputStream in = jar.getInputStream(entry)) {
                            bytes = patchPcMenusOverlay(readAll(in),
                                    "D5_WA_MENU_DISPATCH localLeaves=/pc/local/wa/*",
                                    "D5 WhatsApp customer service local pages");
                        }
                        menus = true;
                    } else if (M5_LOCAL_SPIDER_BRIDGE_CLASS.equals(entry.getName())) {
                        bytes = bridgeBytes;
                        bridge = true;
                    } else {
                        try (InputStream in = jar.getInputStream(entry)) { bytes = readAll(in); }
                    }
                    jarOut.write(bytes);
                }
                jarOut.closeEntry();
            }
        }
        if (!menus || !bridge) {
            Files.deleteIfExists(temp);
            throw new IllegalStateException("D5 WA overlay requires " + TARGET_CLASS + " and " + M5_LOCAL_SPIDER_BRIDGE_CLASS);
        }
        Files.move(temp, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        System.out.println("D5_WA_LOCAL_PAGES_OVERLAY pcMenus=" + TARGET_CLASS
                + " localBridge=" + M5_LOCAL_SPIDER_BRIDGE_CLASS + " input=" + input
                + " output=" + output);
    }


    private static PatchResult patchJar(
            Path input,
            Path output,
            boolean realProductMenuLogging,
            String productModuleJson)
            throws IOException {
        PatchResult result = new PatchResult();
        Set<String> names = new HashSet<String>();
        try (JarFile jar = new JarFile(input.toFile());
                OutputStream fileOut = Files.newOutputStream(output);
                JarOutputStream jarOut = new JarOutputStream(fileOut)) {
            String trueExeLoginCallbackClass = selectTrueExeLoginCallbackClass(jar);
            boolean useDelayedTrueExeLoginBridge =
                    "com/sbf/main/StartApp$5".equals(trueExeLoginCallbackClass)
                            && jar.getJarEntry(START_APP_BOOTSTRAP_RUN_CLASS_V2) != null;
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!names.add(entry.getName())) {
                    continue;
                }
                JarEntry outEntry = copyEntryMetadata(entry);
                jarOut.putNextEntry(outEntry);
                if (!entry.isDirectory()) {
                    try (InputStream in = jar.getInputStream(entry)) {
                        byte[] bytes = readAll(in);
                        if (TARGET_CLASS.equals(entry.getName())) {
                            bytes =
                                    patchSbfApi(
                                            bytes,
                                            result,
                                            realProductMenuLogging,
                                            productModuleJson);
                        } else if (UPDATE_CHECKER_CLASS.equals(entry.getName())) {
                            bytes = patchUpdateChecker(bytes, result);
                        } else if (TREE_NODE_CLASS.equals(entry.getName())) {
                            bytes = patchTreeNodeDiagnostics(bytes, result);
                        } else if (MENU_DISPATCH_CLASS.equals(entry.getName())) {
                            bytes = patchMenuDispatchDiagnostics(bytes, result);
                        } else if (MODERN_MENU_DISPATCH_CLASS.equals(entry.getName())
                                || MODERN_MENU_DISPATCH_CLASS_V2.equals(entry.getName())) {
                            bytes = patchModernMenuDispatchDiagnostics(bytes, result);
                        } else if (MODERN_MENU_MOUSE_CLASS.equals(entry.getName())) {
                            bytes = patchModernMenuMouseDiagnostics(bytes, result);
                        } else if (SIDE_MENU_MOUSE_CLASS.equals(entry.getName())) {
                            bytes = patchSideMenuMouseDiagnostics(bytes, result);
                        } else if (SIDE_MENU_CALLBACK_CLASS.equals(entry.getName())) {
                            bytes = patchSideMenuCallbackDiagnostics(bytes, result);
                        } else if (START_APP_CLASS.equals(entry.getName())) {
                            bytes = patchStartApp(bytes, result);
                        } else if (START_APP_LOGIN_CALLBACK_CLASS.equals(entry.getName())) {
                            bytes = patchStartAppLoginDisposeGuard(bytes, result);
                        } else if (START_APP_PRODUCT_SELECTOR_CALLBACK_CLASS.equals(entry.getName())
                                || START_APP_PRODUCT_SELECTOR_CALLBACK_CLASS_V2.equals(entry.getName())) {
                            bytes = patchProductSelectorEnterBridge(bytes, result);
                        } else if (START_APP_UI_CLASS.equals(entry.getName())) {
                            bytes = patchStartAppAutoLogin(bytes, result);
                        } else if (LOGIN_HTML_CLASS.equals(entry.getName())) {
                            bytes =
                                    patchTrueExeLoginBridge(
                                            bytes,
                                            result,
                                            trueExeLoginCallbackClass,
                                            !useDelayedTrueExeLoginBridge);
                        } else if (START_APP_BOOTSTRAP_RUN_CLASS_V2.equals(entry.getName())
                                && useDelayedTrueExeLoginBridge) {
                            bytes =
                                    patchDelayedTrueExeLoginBridge(
                                            bytes, result, trueExeLoginCallbackClass);
                        } else if (MIJAVA_CLASS.equals(entry.getName())) {
                            bytes = patchMiJavaDictBridge(bytes, result);
                        } else if (JXBROWSER_CLASS.equals(entry.getName())) {
                            bytes = patchJxBrowserDiagnostics(bytes, result);
                            bytes = patchJxBrowserLoadDiagnostics(bytes, result);
                        } else if (JXBROWSER_LOAD_THREAD_CLASS.equals(entry.getName())) {
                            bytes = patchJxBrowserLoadDiagnostics(bytes, result);
                            bytes = patchJxBrowserViewAttachDispatch(bytes, result);
                        } else if (JXBROWSER_READY_LOAD_THREAD_CLASS.equals(entry.getName())) {
                            bytes = patchJxBrowserReadyLoadDispatch(bytes, result);
                        } else if (JXBROWSER_ENGINE_CLASS.equals(entry.getName())) {
                            bytes = patchJxBrowserEngine(bytes, result);
                        } else if (JXBROWSER_SCHEME_CALLBACK_CLASS.equals(entry.getName())) {
                            bytes = patchJxBrowserSchemeCallback(bytes, result);
                        } else if (GOOGLE_CR_HELPER_CLASS.equals(entry.getName())) {
                            bytes = patchGoogleCRHelper(bytes, result);
                        } else if (SPIDER_CALLBACK_CLASS.equals(entry.getName())) {
                            bytes = patchSpiderCallback(bytes, result);
                        }
                        jarOut.write(bytes);
                    }
                }
                jarOut.closeEntry();
            }
            if (names.add(M5_CONSOLE_OBSERVER_CLASS)) {
                writeGeneratedClass(
                        jarOut, M5_CONSOLE_OBSERVER_CLASS, generateM5ConsoleObserver());
                result.addedM5ConsoleObserver = true;
            }
            if (names.add(M5_AUTH_BOOTSTRAP_CALLBACK_CLASS)) {
                writeGeneratedClass(
                        jarOut, M5_AUTH_BOOTSTRAP_CALLBACK_CLASS, generateM5AuthBootstrapCallback());
                result.addedM5AuthBootstrapCallback = true;
            }
            if (names.add(M5_INJECT_JS_CALLBACK_CLASS)) {
                writeGeneratedClass(
                        jarOut, M5_INJECT_JS_CALLBACK_CLASS, generateM5InjectJsCallback());
                result.addedM5InjectJsCallback = true;
            }
            if (names.add(M5_LOCAL_SPIDER_BRIDGE_CLASS)) {
                writeGeneratedClass(
                        jarOut,
                        M5_LOCAL_SPIDER_BRIDGE_CLASS,
                        readGeneratedSupportClass(M5_LOCAL_SPIDER_BRIDGE_CLASS));
                result.addedM5LocalSpiderBridge = true;
            }
            if (names.add(M5_LOCAL_SPIDER_BRIDGE_RUNNER_CLASS)) {
                writeGeneratedClass(
                        jarOut,
                        M5_LOCAL_SPIDER_BRIDGE_RUNNER_CLASS,
                        readGeneratedSupportClass(M5_LOCAL_SPIDER_BRIDGE_RUNNER_CLASS));
            }
            if (names.add(M8_WHATSAPP_NATIVE_PROFILES_CLASS)) {
                writeGeneratedClass(
                        jarOut,
                        M8_WHATSAPP_NATIVE_PROFILES_CLASS,
                        readGeneratedSupportClass(M8_WHATSAPP_NATIVE_PROFILES_CLASS));
                result.addedM8WhatsAppNativeProfiles = true;
            }
            if (names.add(M8_WHATSAPP_EXTERNAL_BROWSERS_CLASS)) {
                writeGeneratedClass(
                        jarOut,
                        M8_WHATSAPP_EXTERNAL_BROWSERS_CLASS,
                        readGeneratedSupportClass(M8_WHATSAPP_EXTERNAL_BROWSERS_CLASS));
                result.addedM8WhatsAppExternalBrowsers = true;
            }
            if (names.add(M8_WHATSAPP_EXTERNAL_BROWSERS_ACCOUNT_HANDLER_CLASS)) {
                writeGeneratedClass(
                        jarOut,
                        M8_WHATSAPP_EXTERNAL_BROWSERS_ACCOUNT_HANDLER_CLASS,
                        readGeneratedSupportClass(M8_WHATSAPP_EXTERNAL_BROWSERS_ACCOUNT_HANDLER_CLASS));
            }
            if (names.add(M8_WHATSAPP_EXTERNAL_BROWSERS_JSON_HANDLER_CLASS)) {
                writeGeneratedClass(
                        jarOut,
                        M8_WHATSAPP_EXTERNAL_BROWSERS_JSON_HANDLER_CLASS,
                        readGeneratedSupportClass(M8_WHATSAPP_EXTERNAL_BROWSERS_JSON_HANDLER_CLASS));
            }
            if (names.add(M8_WHATSAPP_EXTERNAL_BROWSERS_MESSAGE_HANDLER_CLASS)) {
                writeGeneratedClass(
                        jarOut,
                        M8_WHATSAPP_EXTERNAL_BROWSERS_MESSAGE_HANDLER_CLASS,
                        readGeneratedSupportClass(M8_WHATSAPP_EXTERNAL_BROWSERS_MESSAGE_HANDLER_CLASS));
            }
            if (names.add(M8_WHATSAPP_DRIP_CAMPAIGNS_CLASS)) {
                writeGeneratedClass(
                        jarOut,
                        M8_WHATSAPP_DRIP_CAMPAIGNS_CLASS,
                        readGeneratedSupportClass(M8_WHATSAPP_DRIP_CAMPAIGNS_CLASS));
                result.addedM8WhatsAppDripCampaigns = true;
            }
            if (names.add(M5_REQUEST_OBSERVER_CLASS)) {
                writeGeneratedClass(
                        jarOut, M5_REQUEST_OBSERVER_CLASS, generateM5RequestObserver());
                result.addedM5RequestObserver = true;
            }
            if (names.add(M5_YES_CAPTCHA_BRIDGE_CLASS)) {
                writeGeneratedClass(
                        jarOut,
                        M5_YES_CAPTCHA_BRIDGE_CLASS,
                        readGeneratedSupportClass(M5_YES_CAPTCHA_BRIDGE_CLASS));
                result.addedM5YesCaptchaBridge = true;
            }
            if (names.add(M8D7_DEFAULT_MENU_DISPATCH_CLASS)) {
                writeGeneratedClass(
                        jarOut,
                        M8D7_DEFAULT_MENU_DISPATCH_CLASS,
                        readGeneratedSupportClass(M8D7_DEFAULT_MENU_DISPATCH_CLASS));
                result.addedM8D7DefaultMenuDispatch = true;
            }
            if (names.add(M8D14_EXE_DIAG_CLASS)) {
                writeGeneratedClass(
                        jarOut, M8D14_EXE_DIAG_CLASS, readGeneratedSupportClass(M8D14_EXE_DIAG_CLASS));
                result.addedM8D14ExeDiag = true;
            }
        }
        return result;
    }

    private static byte[] patchJxBrowserSchemeCallback(byte[] original, PatchResult result) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                if ("on".equals(name) && "(Ljava/lang/Object;)Ljava/lang/Object;".equals(descriptor)) {
                    result.patchedLocalWebSchemeCallback = true;
                    return writeLocalWebSchemeCallback(access, name, descriptor, signature, exceptions);
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }

            private MethodVisitor writeLocalWebSchemeCallback(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                Label fallback = new Label();
                mv.visitCode();
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitTypeInsn(
                        Opcodes.CHECKCAST,
                        "com/teamdev/jxbrowser/net/callback/InterceptUrlRequestCallback$Params");
                mv.visitVarInsn(Opcodes.ASTORE, 2);
                mv.visitVarInsn(Opcodes.ALOAD, 2);
                mv.visitMethodInsn(
                        Opcodes.INVOKEINTERFACE,
                        "com/teamdev/jxbrowser/net/callback/InterceptUrlRequestCallback$Params",
                        "urlRequest",
                        "()Lcom/teamdev/jxbrowser/net/UrlRequest;",
                        true);
                mv.visitMethodInsn(
                        Opcodes.INVOKEINTERFACE,
                        "com/teamdev/jxbrowser/net/UrlRequest",
                        "url",
                        "()Ljava/lang/String;",
                        true);
                mv.visitVarInsn(Opcodes.ASTORE, 3);
                Label checkFullMirrorCss = new Label();
                Label fullMirrorPublicPathReady = new Label();
                mv.visitVarInsn(Opcodes.ALOAD, 3);
                mv.visitLdcInsn("/static/js/app.ae0af1a5.js");
                mv.visitMethodInsn(
                        Opcodes.INVOKEVIRTUAL,
                        "java/lang/String",
                        "contains",
                        "(Ljava/lang/CharSequence;)Z",
                        false);
                mv.visitJumpInsn(Opcodes.IFEQ, checkFullMirrorCss);
                mv.visitLdcInsn("https://app.xdxsoft.com/static/js/app.988d65c1.js");
                mv.visitVarInsn(Opcodes.ASTORE, 3);
                emitPrint(mv, "M8B_WSCLAW_LOCAL_PUBLIC_PATH js=app.988d65c1.js");
                mv.visitJumpInsn(Opcodes.GOTO, fullMirrorPublicPathReady);
                mv.visitLabel(checkFullMirrorCss);
                mv.visitVarInsn(Opcodes.ALOAD, 3);
                mv.visitLdcInsn("/static/css/app.b4573062.css");
                mv.visitMethodInsn(
                        Opcodes.INVOKEVIRTUAL,
                        "java/lang/String",
                        "contains",
                        "(Ljava/lang/CharSequence;)Z",
                        false);
                mv.visitJumpInsn(Opcodes.IFEQ, fullMirrorPublicPathReady);
                mv.visitLdcInsn("https://app.xdxsoft.com/static/css/app.99741a48.css");
                mv.visitVarInsn(Opcodes.ASTORE, 3);
                emitPrint(mv, "M8B_WSCLAW_LOCAL_PUBLIC_PATH css=app.99741a48.css");
                mv.visitLabel(fullMirrorPublicPathReady);
                mv.visitVarInsn(Opcodes.ALOAD, 3);
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "com/sbf/main/jxbrowser/M5LocalSpiderBridge",
                        "localWebAssetBytes",
                        "(Ljava/lang/String;)[B",
                        false);
                mv.visitVarInsn(Opcodes.ASTORE, 4);
                mv.visitVarInsn(Opcodes.ALOAD, 4);
                mv.visitJumpInsn(Opcodes.IFNULL, fallback);
                emitStringBuilderPrint(
                        mv,
                        "M5D8_LOCAL_WEB_ASSET_ADD_SCHEME url=",
                        Opcodes.ALOAD,
                        3,
                        "java/lang/StringBuilder",
                        "append",
                        "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
                mv.visitFieldInsn(
                        Opcodes.GETSTATIC,
                        "com/teamdev/jxbrowser/net/HttpStatus",
                        "OK",
                        "Lcom/teamdev/jxbrowser/net/HttpStatus;");
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "com/teamdev/jxbrowser/net/UrlRequestJob$Options",
                        "newBuilder",
                        "(Lcom/teamdev/jxbrowser/net/HttpStatus;)"
                                + "Lcom/teamdev/jxbrowser/net/UrlRequestJob$Options$Builder;",
                        false);
                mv.visitLdcInsn("Content-Type");
                mv.visitVarInsn(Opcodes.ALOAD, 3);
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "com/sbf/main/jxbrowser/M5LocalSpiderBridge",
                        "localWebAssetContentType",
                        "(Ljava/lang/String;)Ljava/lang/String;",
                        false);
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "com/teamdev/jxbrowser/net/HttpHeader",
                        "of",
                        "(Ljava/lang/String;Ljava/lang/String;)Lcom/teamdev/jxbrowser/net/HttpHeader;",
                        false);
                mv.visitMethodInsn(
                        Opcodes.INVOKEVIRTUAL,
                        "com/teamdev/jxbrowser/net/UrlRequestJob$Options$Builder",
                        "addHttpHeader",
                        "(Lcom/teamdev/jxbrowser/net/HttpHeader;)"
                                + "Lcom/teamdev/jxbrowser/net/UrlRequestJob$Options$Builder;",
                        false);
                mv.visitVarInsn(Opcodes.ASTORE, 5);
                mv.visitVarInsn(Opcodes.ALOAD, 2);
                mv.visitVarInsn(Opcodes.ALOAD, 5);
                mv.visitMethodInsn(
                        Opcodes.INVOKEVIRTUAL,
                        "com/teamdev/jxbrowser/net/UrlRequestJob$Options$Builder",
                        "build",
                        "()Lcom/teamdev/jxbrowser/net/UrlRequestJob$Options;",
                        false);
                mv.visitMethodInsn(
                        Opcodes.INVOKEINTERFACE,
                        "com/teamdev/jxbrowser/net/callback/InterceptUrlRequestCallback$Params",
                        "newUrlRequestJob",
                        "(Lcom/teamdev/jxbrowser/net/UrlRequestJob$Options;)"
                                + "Lcom/teamdev/jxbrowser/net/UrlRequestJob;",
                        true);
                mv.visitVarInsn(Opcodes.ASTORE, 6);
                mv.visitVarInsn(Opcodes.ALOAD, 6);
                mv.visitVarInsn(Opcodes.ALOAD, 4);
                mv.visitMethodInsn(
                        Opcodes.INVOKEINTERFACE,
                        "com/teamdev/jxbrowser/net/UrlRequestJob",
                        "write",
                        "([B)V",
                        true);
                mv.visitVarInsn(Opcodes.ALOAD, 6);
                mv.visitMethodInsn(
                        Opcodes.INVOKEINTERFACE,
                        "com/teamdev/jxbrowser/net/UrlRequestJob",
                        "complete",
                        "()V",
                        true);
                mv.visitVarInsn(Opcodes.ALOAD, 6);
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "com/teamdev/jxbrowser/net/callback/InterceptUrlRequestCallback$Response",
                        "intercept",
                        "(Lcom/teamdev/jxbrowser/net/UrlRequestJob;)"
                                + "Lcom/teamdev/jxbrowser/net/callback/InterceptUrlRequestCallback$Response;",
                        true);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitLabel(fallback);
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitVarInsn(Opcodes.ALOAD, 2);
                mv.visitMethodInsn(
                        Opcodes.INVOKESPECIAL,
                        "com/sbf/main/jxbrowser/b",
                        "a",
                        "(Lcom/teamdev/jxbrowser/net/callback/InterceptUrlRequestCallback$Params;)"
                                + "Lcom/teamdev/jxbrowser/net/callback/InterceptUrlRequestCallback$Response;",
                        false);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(4, 7);
                mv.visitEnd();
                return null;
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static Map<String, String> decodeRecoveryProductLogos(Path input) throws IOException {
        String[] productCodes = {
            "whatsapp", "tiktok", "facebook", "instagram", "twitter",
            "telegram", "geo", "wskefu", "aishope"
        };
        Map<String, String> logos = new LinkedHashMap<String, String>();
        URL[] classPath = {input.toUri().toURL()};
        try (URLClassLoader loader =
                        new URLClassLoader(classPath, M4AuthPatch.class.getClassLoader());
                JarFile jar = new JarFile(input.toFile())) {
            Class<?> decoderClass = Class.forName("ch.r", true, loader);
            Constructor<?> constructor = decoderClass.getConstructor(InputStream.class);
            for (String productCode : productCodes) {
                String resource = "svg/main_logo_" + productCode + ".svg";
                JarEntry entry = jar.getJarEntry(resource);
                if (entry == null) {
                    return logos;
                }
                try (InputStream raw = jar.getInputStream(entry);
                        InputStream decoded = (InputStream) constructor.newInstance(raw)) {
                    String svg = new String(readAll(decoded), StandardCharsets.UTF_8);
                    int svgStart = svg.indexOf("<svg");
                    if (svgStart < 0) {
                        throw new IOException("decoded product logo is not SVG: " + resource);
                    }
                    logos.put(productCode, svg.substring(svgStart));
                }
            }
        } catch (ClassNotFoundException error) {
            return logos;
        } catch (ReflectiveOperationException error) {
            throw new IOException("failed to decode product logo resources", error);
        }
        return logos;
    }

    private static void writeGeneratedClass(JarOutputStream jarOut, String name, byte[] bytes)
            throws IOException {
        JarEntry entry = new JarEntry(name);
        entry.setTime(0L);
        jarOut.putNextEntry(entry);
        jarOut.write(bytes);
        jarOut.closeEntry();
    }

    private static byte[] readGeneratedSupportClass(String name) throws IOException {
        try (InputStream in = M4AuthPatch.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) {
                throw new IOException("missing generated support class resource: " + name);
            }
            return readAll(in);
        }
    }

    private static String selectTrueExeLoginCallbackClass(JarFile jar) throws IOException {
        if (hasMethod(
                jar,
                START_APP_LOGIN_CALLBACK_CLASS_V2,
                "a",
                "(Lorg/json/JSONObject;)V")
                && jar.getJarEntry(START_APP_PRODUCT_SELECTOR_CALLBACK_CLASS_V2) != null) {
            return "com/sbf/main/StartApp$5";
        }
        return "com/sbf/main/StartApp$1";
    }

    private static boolean hasMethod(
            JarFile jar, String classEntryName, String methodName, String methodDescriptor)
            throws IOException {
        JarEntry entry = jar.getJarEntry(classEntryName);
        if (entry == null) {
            return false;
        }
        ClassNode classNode = new ClassNode();
        try (InputStream in = jar.getInputStream(entry)) {
            new ClassReader(readAll(in)).accept(classNode, 0);
        }
        for (MethodNode method : classNode.methods) {
            if (methodName.equals(method.name) && methodDescriptor.equals(method.desc)) {
                return true;
            }
        }
        return false;
    }

    private static byte[] generateM5AuthBootstrapCallback() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(
                Opcodes.V1_8,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                "com/sbf/main/jxbrowser/M5AuthBootstrapCallback",
                null,
                "java/lang/Object",
                new String[] {
                    "com/teamdev/jxbrowser/net/callback/InterceptUrlRequestCallback",
                    "com/teamdev/jxbrowser/net/callback/NetworkCallback"
                });
        writeDefaultConstructor(cw);
        MethodVisitor mv =
                cw.visitMethod(
                        Opcodes.ACC_PUBLIC,
                        "on",
                        "(Lcom/teamdev/jxbrowser/net/callback/InterceptUrlRequestCallback$Params;)"
                                + "Lcom/teamdev/jxbrowser/net/callback/InterceptUrlRequestCallback$Response;",
                        null,
                        null);
        org.objectweb.asm.Label start = new org.objectweb.asm.Label();
        org.objectweb.asm.Label end = new org.objectweb.asm.Label();
        org.objectweb.asm.Label handler = new org.objectweb.asm.Label();
        org.objectweb.asm.Label checkRouters = new org.objectweb.asm.Label();
        org.objectweb.asm.Label hasBody = new org.objectweb.asm.Label();
        org.objectweb.asm.Label proceed = new org.objectweb.asm.Label();
        mv.visitTryCatchBlock(start, end, handler, "java/lang/Throwable");
        mv.visitCode();
        mv.visitLabel(start);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/net/callback/InterceptUrlRequestCallback$Params",
                "urlRequest",
                "()Lcom/teamdev/jxbrowser/net/UrlRequest;",
                true);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/net/UrlRequest",
                "url",
                "()Ljava/lang/String;",
                true);
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitVarInsn(Opcodes.ASTORE, 3);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/jxbrowser/M5LocalSpiderBridge",
                "localWebAssetBytes",
                "(Ljava/lang/String;)[B",
                false);
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ASTORE, 3);
        mv.visitJumpInsn(Opcodes.IFNONNULL, hasBody);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitLdcInsn("/prod-api/getInfo");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "contains",
                "(Ljava/lang/CharSequence;)Z",
                false);
        mv.visitJumpInsn(Opcodes.IFEQ, checkRouters);
        mv.visitLdcInsn(WEB_BOOTSTRAP_GET_INFO_JSON);
        mv.visitFieldInsn(
                Opcodes.GETSTATIC,
                "java/nio/charset/StandardCharsets",
                "UTF_8",
                "Ljava/nio/charset/Charset;");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "getBytes",
                "(Ljava/nio/charset/Charset;)[B",
                false);
        mv.visitVarInsn(Opcodes.ASTORE, 3);
        mv.visitJumpInsn(Opcodes.GOTO, hasBody);
        mv.visitLabel(checkRouters);
        mv.visitFrame(
                Opcodes.F_APPEND,
                2,
                new Object[] {"java/lang/String", "[B"},
                0,
                null);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitLdcInsn("/prod-api/getRouters");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "contains",
                "(Ljava/lang/CharSequence;)Z",
                false);
        mv.visitJumpInsn(Opcodes.IFEQ, proceed);
        mv.visitLdcInsn(WEB_BOOTSTRAP_ROUTERS_JSON);
        mv.visitFieldInsn(
                Opcodes.GETSTATIC,
                "java/nio/charset/StandardCharsets",
                "UTF_8",
                "Ljava/nio/charset/Charset;");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "getBytes",
                "(Ljava/nio/charset/Charset;)[B",
                false);
        mv.visitVarInsn(Opcodes.ASTORE, 3);
        mv.visitLabel(hasBody);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        emitStringBuilderPrint(
                mv,
                "M5_V24_AUTH_BOOTSTRAP_INTERCEPT url=",
                Opcodes.ALOAD,
                2,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
        mv.visitFieldInsn(
                Opcodes.GETSTATIC,
                "com/teamdev/jxbrowser/net/HttpStatus",
                "OK",
                "Lcom/teamdev/jxbrowser/net/HttpStatus;");
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/teamdev/jxbrowser/net/UrlRequestJob$Options",
                "newBuilder",
                "(Lcom/teamdev/jxbrowser/net/HttpStatus;)"
                        + "Lcom/teamdev/jxbrowser/net/UrlRequestJob$Options$Builder;",
                true);
        mv.visitLdcInsn("Content-Type");
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/jxbrowser/M5LocalSpiderBridge",
                "localWebAssetContentType",
                "(Ljava/lang/String;)Ljava/lang/String;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/teamdev/jxbrowser/net/HttpHeader",
                "of",
                "(Ljava/lang/String;Ljava/lang/String;)Lcom/teamdev/jxbrowser/net/HttpHeader;",
                true);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/teamdev/jxbrowser/net/UrlRequestJob$Options$Builder",
                "addHttpHeader",
                "(Lcom/teamdev/jxbrowser/net/HttpHeader;)"
                        + "Lcom/teamdev/jxbrowser/net/UrlRequestJob$Options$Builder;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/teamdev/jxbrowser/net/UrlRequestJob$Options$Builder",
                "build",
                "()Lcom/teamdev/jxbrowser/net/UrlRequestJob$Options;",
                false);
        mv.visitVarInsn(Opcodes.ASTORE, 4);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/net/callback/InterceptUrlRequestCallback$Params",
                "newUrlRequestJob",
                "(Lcom/teamdev/jxbrowser/net/UrlRequestJob$Options;)"
                        + "Lcom/teamdev/jxbrowser/net/UrlRequestJob;",
                true);
        mv.visitVarInsn(Opcodes.ASTORE, 5);
        mv.visitVarInsn(Opcodes.ALOAD, 5);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/net/UrlRequestJob",
                "write",
                "([B)V",
                true);
        mv.visitVarInsn(Opcodes.ALOAD, 5);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/net/UrlRequestJob",
                "complete",
                "()V",
                true);
        mv.visitVarInsn(Opcodes.ALOAD, 5);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/teamdev/jxbrowser/net/callback/InterceptUrlRequestCallback$Response",
                "intercept",
                "(Lcom/teamdev/jxbrowser/net/UrlRequestJob;)"
                        + "Lcom/teamdev/jxbrowser/net/callback/InterceptUrlRequestCallback$Response;",
                true);
        mv.visitLabel(end);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitLabel(proceed);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/teamdev/jxbrowser/net/callback/InterceptUrlRequestCallback$Response",
                "proceed",
                "()Lcom/teamdev/jxbrowser/net/callback/InterceptUrlRequestCallback$Response;",
                true);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitLabel(handler);
        mv.visitFrame(
                Opcodes.F_FULL,
                2,
                new Object[] {
                    "com/sbf/main/jxbrowser/M5AuthBootstrapCallback",
                    "com/teamdev/jxbrowser/net/callback/InterceptUrlRequestCallback$Params"
                },
                1,
                new Object[] {"java/lang/Throwable"});
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        emitStringBuilderPrint(
                mv,
                "M5_V24_AUTH_BOOTSTRAP_FAILED ",
                Opcodes.ALOAD,
                2,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/Object;)Ljava/lang/StringBuilder;");
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/Throwable",
                "printStackTrace",
                "(Ljava/io/PrintStream;)V",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/teamdev/jxbrowser/net/callback/InterceptUrlRequestCallback$Response",
                "proceed",
                "()Lcom/teamdev/jxbrowser/net/callback/InterceptUrlRequestCallback$Response;",
                true);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        MethodVisitor bridge =
                cw.visitMethod(
                        Opcodes.ACC_PUBLIC | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC,
                        "on",
                        "(Ljava/lang/Object;)Ljava/lang/Object;",
                        null,
                        null);
        bridge.visitCode();
        bridge.visitVarInsn(Opcodes.ALOAD, 0);
        bridge.visitVarInsn(Opcodes.ALOAD, 1);
        bridge.visitTypeInsn(
                Opcodes.CHECKCAST,
                "com/teamdev/jxbrowser/net/callback/InterceptUrlRequestCallback$Params");
        bridge.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/sbf/main/jxbrowser/M5AuthBootstrapCallback",
                "on",
                "(Lcom/teamdev/jxbrowser/net/callback/InterceptUrlRequestCallback$Params;)"
                        + "Lcom/teamdev/jxbrowser/net/callback/InterceptUrlRequestCallback$Response;",
                false);
        bridge.visitInsn(Opcodes.ARETURN);
        bridge.visitMaxs(0, 0);
        bridge.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] patchMiJavaDictBridge(byte[] original, PatchResult result) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        reader.accept(
                new ClassVisitor(Opcodes.ASM9, writer) {
                    private boolean hasGetDicts;
                    private boolean hasM5WriteLocalMockResult;
                    private boolean hasM5SubmitLocalCollectTask;
                    private boolean hasM5ListLocalCollectTasks;
                    private boolean hasM5ListLocalSpiderData;
                    private boolean hasM5GetLocalSpiderConfig;
                    private boolean hasM8UpsertWhatsAppAccount;
                    private boolean hasM8ListWhatsAppAccounts;
                    private boolean hasM8UpsertWhatsAppMessage;
                    private boolean hasM8ListWhatsAppConversations;
                    private boolean hasM8ListWhatsAppMessages;
                    private boolean hasM8SetActiveWhatsAppProfile;
                    private boolean hasM8GetActiveWhatsAppProfile;
                    private boolean hasM8SwitchWhatsAppNativeProfile;
                    private boolean hasM8ProbeWhatsAppExternalBrowser;
                    private boolean hasM8StartWhatsAppExternalBrowser;
                    private boolean hasM8StopWhatsAppExternalBrowser;

                    @Override
                    public MethodVisitor visitMethod(
                            int access,
                            String name,
                            String descriptor,
                            String signature,
                            String[] exceptions) {
                        if ("getDicts".equals(name)
                                && "(Ljava/lang/String;)Ljava/lang/String;".equals(descriptor)) {
                            hasGetDicts = true;
                        }
                        if ("m5WriteLocalMockResult".equals(name)
                                && "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"
                                        .equals(descriptor)) {
                            hasM5WriteLocalMockResult = true;
                        }
                        if ("m5SubmitLocalCollectTask".equals(name)
                                && "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"
                                        .equals(descriptor)) {
                            hasM5SubmitLocalCollectTask = true;
                        }
                        if ("m5ListLocalCollectTasks".equals(name)
                                && "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"
                                        .equals(descriptor)) {
                            hasM5ListLocalCollectTasks = true;
                        }
                        if ("m5ListLocalSpiderData".equals(name)
                                && "(Ljava/lang/String;Ljava/lang/String;II)Ljava/lang/String;"
                                        .equals(descriptor)) {
                            hasM5ListLocalSpiderData = true;
                        }
                        if ("m5GetLocalSpiderConfig".equals(name)
                                && "(Ljava/lang/String;)Ljava/lang/String;".equals(descriptor)) {
                            hasM5GetLocalSpiderConfig = true;
                        }
                        if ("m8UpsertWhatsAppAccount".equals(name)
                                && "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"
                                        .equals(descriptor)) {
                            hasM8UpsertWhatsAppAccount = true;
                        }
                        if ("m8ListWhatsAppAccounts".equals(name)
                                && "()Ljava/lang/String;".equals(descriptor)) {
                            hasM8ListWhatsAppAccounts = true;
                        }
                        if ("m8UpsertWhatsAppMessage".equals(name)
                                && "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JLjava/lang/String;Ljava/lang/String;)Ljava/lang/String;"
                                        .equals(descriptor)) {
                            hasM8UpsertWhatsAppMessage = true;
                        }
                        if ("m8ListWhatsAppConversations".equals(name)
                                && "(Ljava/lang/String;)Ljava/lang/String;".equals(descriptor)) {
                            hasM8ListWhatsAppConversations = true;
                        }
                        if ("m8ListWhatsAppMessages".equals(name)
                                && "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;".equals(descriptor)) {
                            hasM8ListWhatsAppMessages = true;
                        }
                        if ("m8SetActiveWhatsAppProfile".equals(name)
                                && "(Ljava/lang/String;)Ljava/lang/String;".equals(descriptor)) {
                            hasM8SetActiveWhatsAppProfile = true;
                        }
                        if ("m8GetActiveWhatsAppProfile".equals(name)
                                && "()Ljava/lang/String;".equals(descriptor)) {
                            hasM8GetActiveWhatsAppProfile = true;
                        }
                        if ("m8SwitchWhatsAppNativeProfile".equals(name)
                                && "(Ljava/lang/String;)Ljava/lang/String;".equals(descriptor)) {
                            hasM8SwitchWhatsAppNativeProfile = true;
                        }
                        if ("m8ProbeWhatsAppExternalBrowser".equals(name)
                                && "()Ljava/lang/String;".equals(descriptor)) {
                            hasM8ProbeWhatsAppExternalBrowser = true;
                        }
                        if ("m8StartWhatsAppExternalBrowser".equals(name)
                                && "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"
                                        .equals(descriptor)) {
                            hasM8StartWhatsAppExternalBrowser = true;
                        }
                        if ("m8StopWhatsAppExternalBrowser".equals(name)
                                && "(Ljava/lang/String;)Ljava/lang/String;".equals(descriptor)) {
                            hasM8StopWhatsAppExternalBrowser = true;
                        }
                        if ("getInfo".equals(name)
                                && "(Lcom/teamdev/jxbrowser/js/JsFunction;)V".equals(descriptor)) {
                            MethodVisitor mv =
                                    super.visitMethod(access, name, descriptor, signature, exceptions);
                            writeMiJavaGetInfoBridgeMethod(mv);
                            result.patchedMiJavaDictBridge = true;
                            return null;
                        }
                        if ("getCloudSpiderConfig".equals(name)
                                && "(Ljava/lang/String;Lcom/teamdev/jxbrowser/js/JsFunction;)V"
                                        .equals(descriptor)) {
                            MethodVisitor mv =
                                    super.visitMethod(access, name, descriptor, signature, exceptions);
                            writeMiJavaGetCloudSpiderConfigBridgeMethod(mv);
                            result.patchedMiJavaDictBridge = true;
                            return null;
                        }
                        if ("getSpiderDataList".equals(name)
                                && "(Ljava/lang/String;Ljava/lang/String;IILcom/teamdev/jxbrowser/js/JsFunction;)V"
                                        .equals(descriptor)) {
                            MethodVisitor mv =
                                    super.visitMethod(access, name, descriptor, signature, exceptions);
                            writeMiJavaGetSpiderDataListBridgeMethod(mv);
                            result.patchedMiJavaDictBridge = true;
                            return null;
                        }
                        if ("getSpiderTableDataInfo".equals(name)
                                && "(Ljava/lang/String;Lcom/teamdev/jxbrowser/js/JsFunction;)V"
                                        .equals(descriptor)) {
                            MethodVisitor mv =
                                    super.visitMethod(access, name, descriptor, signature, exceptions);
                            writeMiJavaGetSpiderTableDataInfoBridgeMethod(mv);
                            result.patchedMiJavaDictBridge = true;
                            return null;
                        }
                        if ("doZwFilterWhataspp".equals(name)
                                && "(Ljava/lang/String;Ljava/lang/String;Lcom/teamdev/jxbrowser/js/JsFunction;)V"
                                        .equals(descriptor)) {
                            MethodVisitor mv =
                                    super.visitMethod(access, name, descriptor, signature, exceptions);
                            writeMiJavaWsFilterExecutionGateMethod(mv);
                            result.patchedMiJavaDictBridge = true;
                            return null;
                        }
                        return super.visitMethod(access, name, descriptor, signature, exceptions);
                    }

                    @Override
                    public void visitEnd() {
                        if (!hasGetDicts) {
                            MethodVisitor mv =
                                    super.visitMethod(
                                            Opcodes.ACC_PUBLIC,
                                            "getDicts",
                                            "(Ljava/lang/String;)Ljava/lang/String;",
                                            null,
                                            null);
                            mv.visitAnnotation("Lcom/teamdev/jxbrowser/js/JsAccessible;", true)
                                    .visitEnd();
                            mv.visitCode();
                            mv.visitFieldInsn(
                                    Opcodes.GETSTATIC,
                                    "java/lang/System",
                                    "out",
                                    "Ljava/io/PrintStream;");
                            mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
                            mv.visitInsn(Opcodes.DUP);
                            mv.visitLdcInsn("M5_V21_GET_DICTS type=");
                            mv.visitMethodInsn(
                                    Opcodes.INVOKESPECIAL,
                                    "java/lang/StringBuilder",
                                    "<init>",
                                    "(Ljava/lang/String;)V",
                                    false);
                            mv.visitVarInsn(Opcodes.ALOAD, 1);
                            appendString(mv);
                            printlnBuilder(mv);
                            mv.visitLdcInsn("[]");
                            mv.visitInsn(Opcodes.ARETURN);
                            mv.visitMaxs(0, 0);
                            mv.visitEnd();
                            result.patchedMiJavaDictBridge = true;
                        }
                        if (!hasM5WriteLocalMockResult) {
                            MethodVisitor mv =
                                    super.visitMethod(
                                            Opcodes.ACC_PUBLIC,
                                            "m5WriteLocalMockResult",
                                            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                                            null,
                                            null);
                            writeMiJavaLocalMockResultMethod(mv);
                            result.patchedMiJavaDictBridge = true;
                        }
                        if (!hasM5SubmitLocalCollectTask) {
                            MethodVisitor mv =
                                    super.visitMethod(
                                            Opcodes.ACC_PUBLIC,
                                            "m5SubmitLocalCollectTask",
                                            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                                            null,
                                            null);
                            writeMiJavaLocalCollectTaskSubmitMethod(mv);
                            result.patchedMiJavaDictBridge = true;
                        }
                        if (!hasM5ListLocalCollectTasks) {
                            MethodVisitor mv =
                                    super.visitMethod(
                                            Opcodes.ACC_PUBLIC,
                                            "m5ListLocalCollectTasks",
                                            "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                                            null,
                                            null);
                            writeMiJavaLocalCollectTaskListMethod(mv);
                            result.patchedMiJavaDictBridge = true;
                        }
                        if (!hasM5ListLocalSpiderData) {
                            MethodVisitor mv =
                                    super.visitMethod(
                                            Opcodes.ACC_PUBLIC,
                                            "m5ListLocalSpiderData",
                                            "(Ljava/lang/String;Ljava/lang/String;II)Ljava/lang/String;",
                                            null,
                                            null);
                            writeMiJavaLocalSpiderDataListMethod(mv);
                            result.patchedMiJavaDictBridge = true;
                        }
                        if (!hasM5GetLocalSpiderConfig) {
                            MethodVisitor mv =
                                    super.visitMethod(
                                            Opcodes.ACC_PUBLIC,
                                            "m5GetLocalSpiderConfig",
                                            "(Ljava/lang/String;)Ljava/lang/String;",
                                            null,
                                            null);
                            writeMiJavaLocalSpiderConfigMethod(mv);
                            result.patchedMiJavaDictBridge = true;
                        }
                        if (!hasM8UpsertWhatsAppAccount) {
                            MethodVisitor mv =
                                    super.visitMethod(
                                            Opcodes.ACC_PUBLIC,
                                            "m8UpsertWhatsAppAccount",
                                            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                                            null,
                                            null);
                            writeMiJavaUpsertWhatsAppAccountMethod(mv);
                            result.patchedMiJavaDictBridge = true;
                        }
                        if (!hasM8ListWhatsAppAccounts) {
                            MethodVisitor mv =
                                    super.visitMethod(
                                            Opcodes.ACC_PUBLIC,
                                            "m8ListWhatsAppAccounts",
                                            "()Ljava/lang/String;",
                                            null,
                                            null);
                            writeMiJavaListWhatsAppAccountsMethod(mv);
                            result.patchedMiJavaDictBridge = true;
                        }
                        if (!hasM8UpsertWhatsAppMessage) {
                            MethodVisitor mv =
                                    super.visitMethod(
                                            Opcodes.ACC_PUBLIC,
                                            "m8UpsertWhatsAppMessage",
                                            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JLjava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                                            null,
                                            null);
                            writeMiJavaUpsertWhatsAppMessageMethod(mv);
                            result.patchedMiJavaDictBridge = true;
                        }
                        if (!hasM8ListWhatsAppConversations) {
                            MethodVisitor mv =
                                    super.visitMethod(
                                            Opcodes.ACC_PUBLIC,
                                            "m8ListWhatsAppConversations",
                                            "(Ljava/lang/String;)Ljava/lang/String;",
                                            null,
                                            null);
                            writeMiJavaListWhatsAppConversationsMethod(mv);
                            result.patchedMiJavaDictBridge = true;
                        }
                        if (!hasM8ListWhatsAppMessages) {
                            MethodVisitor mv =
                                    super.visitMethod(
                                            Opcodes.ACC_PUBLIC,
                                            "m8ListWhatsAppMessages",
                                            "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                                            null,
                                            null);
                            writeMiJavaListWhatsAppMessagesMethod(mv);
                            result.patchedMiJavaDictBridge = true;
                        }
                        if (!hasM8SetActiveWhatsAppProfile) {
                            MethodVisitor mv =
                                    super.visitMethod(
                                            Opcodes.ACC_PUBLIC,
                                            "m8SetActiveWhatsAppProfile",
                                            "(Ljava/lang/String;)Ljava/lang/String;",
                                            null,
                                            null);
                            writeMiJavaSetActiveWhatsAppProfileMethod(mv);
                            result.patchedMiJavaDictBridge = true;
                        }
                        if (!hasM8GetActiveWhatsAppProfile) {
                            MethodVisitor mv =
                                    super.visitMethod(
                                            Opcodes.ACC_PUBLIC,
                                            "m8GetActiveWhatsAppProfile",
                                            "()Ljava/lang/String;",
                                            null,
                                            null);
                            writeMiJavaGetActiveWhatsAppProfileMethod(mv);
                            result.patchedMiJavaDictBridge = true;
                        }
                        if (!hasM8SwitchWhatsAppNativeProfile) {
                            MethodVisitor mv =
                                    super.visitMethod(
                                            Opcodes.ACC_PUBLIC,
                                            "m8SwitchWhatsAppNativeProfile",
                                            "(Ljava/lang/String;)Ljava/lang/String;",
                                            null,
                                            null);
                            writeMiJavaSwitchWhatsAppNativeProfileMethod(mv);
                            result.patchedMiJavaDictBridge = true;
                        }
                        if (!hasM8ProbeWhatsAppExternalBrowser) {
                            MethodVisitor mv =
                                    super.visitMethod(
                                            Opcodes.ACC_PUBLIC,
                                            "m8ProbeWhatsAppExternalBrowser",
                                            "()Ljava/lang/String;",
                                            null,
                                            null);
                            writeMiJavaProbeWhatsAppExternalBrowserMethod(mv);
                            result.patchedMiJavaDictBridge = true;
                        }
                        if (!hasM8StartWhatsAppExternalBrowser) {
                            MethodVisitor mv =
                                    super.visitMethod(
                                            Opcodes.ACC_PUBLIC,
                                            "m8StartWhatsAppExternalBrowser",
                                            "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                                            null,
                                            null);
                            writeMiJavaStartWhatsAppExternalBrowserMethod(mv);
                            result.patchedMiJavaDictBridge = true;
                        }
                        if (!hasM8StopWhatsAppExternalBrowser) {
                            MethodVisitor mv =
                                    super.visitMethod(
                                            Opcodes.ACC_PUBLIC,
                                            "m8StopWhatsAppExternalBrowser",
                                            "(Ljava/lang/String;)Ljava/lang/String;",
                                            null,
                                            null);
                            writeMiJavaStopWhatsAppExternalBrowserMethod(mv);
                            result.patchedMiJavaDictBridge = true;
                        }
                        super.visitEnd();
                    }
                },
                0);
        return writer.toByteArray();
    }

    private static void writeMiJavaGetInfoBridgeMethod(MethodVisitor mv) {
        mv.visitAnnotation("Lcom/teamdev/jxbrowser/js/JsAccessible;", true).visitEnd();
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/js/JsFunction",
                "frame",
                "()Lcom/teamdev/jxbrowser/frame/Frame;",
                true);
        mv.visitLdcInsn("window");
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/frame/Frame",
                "executeJavaScript",
                "(Ljava/lang/String;)Ljava/lang/Object;",
                true);
        mv.visitTypeInsn(Opcodes.CHECKCAST, "com/teamdev/jxbrowser/js/JsObject");
        mv.visitInsn(Opcodes.ICONST_1);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        mv.visitInsn(Opcodes.DUP);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitLdcInsn(WEB_BRIDGE_GET_INFO_JSON);
        mv.visitInsn(Opcodes.AASTORE);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/js/JsFunction",
                "invoke",
                "(Lcom/teamdev/jxbrowser/js/JsObject;[Ljava/lang/Object;)Ljava/lang/Object;",
                true);
        mv.visitInsn(Opcodes.POP);
        emitPrint(mv, "M5A_V49_MIJAVA_GET_INFO_BRIDGE_JSON");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void writeMiJavaGetCloudSpiderConfigBridgeMethod(MethodVisitor mv) {
        mv.visitAnnotation("Lcom/teamdev/jxbrowser/js/JsAccessible;", true).visitEnd();
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/js/JsFunction",
                "frame",
                "()Lcom/teamdev/jxbrowser/frame/Frame;",
                true);
        mv.visitLdcInsn("window");
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/frame/Frame",
                "executeJavaScript",
                "(Ljava/lang/String;)Ljava/lang/Object;",
                true);
        mv.visitTypeInsn(Opcodes.CHECKCAST, "com/teamdev/jxbrowser/js/JsObject");
        mv.visitInsn(Opcodes.ICONST_1);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        mv.visitInsn(Opcodes.DUP);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "com/sbf/main/StartApp", "a", "Ljava/lang/String;");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/jxbrowser/M5LocalSpiderBridge",
                "spiderConfig",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                false);
        mv.visitInsn(Opcodes.AASTORE);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/js/JsFunction",
                "invoke",
                "(Lcom/teamdev/jxbrowser/js/JsObject;[Ljava/lang/Object;)Ljava/lang/Object;",
                true);
        mv.visitInsn(Opcodes.POP);
        emitPrint(mv, "M5D11_LOCAL_DATACOLLECT_CONFIG_JSON");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void writeMiJavaLocalSpiderConfigMethod(MethodVisitor mv) {
        mv.visitAnnotation("Lcom/teamdev/jxbrowser/js/JsAccessible;", true).visitEnd();
        mv.visitCode();
        mv.visitFieldInsn(Opcodes.GETSTATIC, "com/sbf/main/StartApp", "a", "Ljava/lang/String;");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/jxbrowser/M5LocalSpiderBridge",
                "spiderConfig",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void writeMiJavaGetSpiderDataListBridgeMethod(MethodVisitor mv) {
        mv.visitAnnotation("Lcom/teamdev/jxbrowser/js/JsAccessible;", true).visitEnd();
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 5);
        mv.visitVarInsn(Opcodes.ALOAD, 5);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/js/JsFunction",
                "frame",
                "()Lcom/teamdev/jxbrowser/frame/Frame;",
                true);
        mv.visitLdcInsn("window");
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/frame/Frame",
                "executeJavaScript",
                "(Ljava/lang/String;)Ljava/lang/Object;",
                true);
        mv.visitTypeInsn(Opcodes.CHECKCAST, "com/teamdev/jxbrowser/js/JsObject");
        mv.visitInsn(Opcodes.ICONST_1);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        mv.visitInsn(Opcodes.DUP);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "com/sbf/main/StartApp", "a", "Ljava/lang/String;");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ILOAD, 3);
        mv.visitVarInsn(Opcodes.ILOAD, 4);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/jxbrowser/M5LocalSpiderBridge",
                "listSpiderData",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II)Ljava/lang/String;",
                false);
        mv.visitInsn(Opcodes.AASTORE);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/js/JsFunction",
                "invoke",
                "(Lcom/teamdev/jxbrowser/js/JsObject;[Ljava/lang/Object;)Ljava/lang/Object;",
                true);
        mv.visitInsn(Opcodes.POP);
        emitPrint(mv, "M5D8_MIJAVA_GET_SPIDER_DATA_LIST_LOCAL");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void writeMiJavaGetSpiderTableDataInfoBridgeMethod(MethodVisitor mv) {
        mv.visitAnnotation("Lcom/teamdev/jxbrowser/js/JsAccessible;", true).visitEnd();
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/js/JsFunction",
                "frame",
                "()Lcom/teamdev/jxbrowser/frame/Frame;",
                true);
        mv.visitLdcInsn("window");
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/frame/Frame",
                "executeJavaScript",
                "(Ljava/lang/String;)Ljava/lang/Object;",
                true);
        mv.visitTypeInsn(Opcodes.CHECKCAST, "com/teamdev/jxbrowser/js/JsObject");
        mv.visitInsn(Opcodes.ICONST_1);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        mv.visitInsn(Opcodes.DUP);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "com/sbf/main/StartApp", "a", "Ljava/lang/String;");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/jxbrowser/M5LocalSpiderBridge",
                "getSpiderTableDataInfo",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                false);
        mv.visitInsn(Opcodes.AASTORE);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/js/JsFunction",
                "invoke",
                "(Lcom/teamdev/jxbrowser/js/JsObject;[Ljava/lang/Object;)Ljava/lang/Object;",
                true);
        mv.visitInsn(Opcodes.POP);
        emitPrint(mv, "M5D8_MIJAVA_GET_SPIDER_TABLE_DATA_INFO_LOCAL");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void writeMiJavaWsFilterExecutionGateMethod(MethodVisitor mv) {
        mv.visitAnnotation("Lcom/teamdev/jxbrowser/js/JsAccessible;", true).visitEnd();
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/js/JsFunction",
                "frame",
                "()Lcom/teamdev/jxbrowser/frame/Frame;",
                true);
        mv.visitLdcInsn("window");
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/frame/Frame",
                "executeJavaScript",
                "(Ljava/lang/String;)Ljava/lang/Object;",
                true);
        mv.visitTypeInsn(Opcodes.CHECKCAST, "com/teamdev/jxbrowser/js/JsObject");
        mv.visitInsn(Opcodes.ICONST_2);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        mv.visitInsn(Opcodes.DUP);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitInsn(Opcodes.ICONST_M1);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/Integer",
                "valueOf",
                "(I)Ljava/lang/Integer;",
                false);
        mv.visitInsn(Opcodes.AASTORE);
        mv.visitInsn(Opcodes.DUP);
        mv.visitInsn(Opcodes.ICONST_1);
        mv.visitLdcInsn("需登录 WhatsApp；执行新筛选待单独接入");
        mv.visitInsn(Opcodes.AASTORE);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/js/JsFunction",
                "invoke",
                "(Lcom/teamdev/jxbrowser/js/JsObject;[Ljava/lang/Object;)Ljava/lang/Object;",
                true);
        mv.visitInsn(Opcodes.POP);
        emitPrint(mv, "M5C_AI_FILTER_EXECUTION_GATED");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void writeMiJavaLocalSpiderDataListMethod(MethodVisitor mv) {
        mv.visitAnnotation("Lcom/teamdev/jxbrowser/js/JsAccessible;", true).visitEnd();
        mv.visitCode();
        mv.visitFieldInsn(Opcodes.GETSTATIC, "com/sbf/main/StartApp", "a", "Ljava/lang/String;");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ILOAD, 3);
        mv.visitVarInsn(Opcodes.ILOAD, 4);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/jxbrowser/M5LocalSpiderBridge",
                "listSpiderData",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II)Ljava/lang/String;",
                false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void writeMiJavaLocalMockResultMethod(MethodVisitor mv) {
        org.objectweb.asm.Label start = new org.objectweb.asm.Label();
        org.objectweb.asm.Label end = new org.objectweb.asm.Label();
        org.objectweb.asm.Label handler = new org.objectweb.asm.Label();
        mv.visitAnnotation("Lcom/teamdev/jxbrowser/js/JsAccessible;", true).visitEnd();
        mv.visitTryCatchBlock(start, end, handler, "java/lang/Throwable");
        mv.visitCode();
        mv.visitLabel(start);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("M5A_LOCAL_DATACOLLECT_MOCK_WRITE moduleCode=");
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/StringBuilder",
                "<init>",
                "(Ljava/lang/String;)V",
                false);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        appendString(mv);
        mv.visitLdcInsn(" spiderCode=");
        appendString(mv);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        appendString(mv);
        printlnBuilder(mv);
        mv.visitFieldInsn(
                Opcodes.GETSTATIC, "com/sbf/main/StartApp", "a", "Ljava/lang/String;");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/jxbrowser/M5LocalSpiderBridge",
                "writeMockResult",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                false);
        mv.visitLabel(end);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitLabel(handler);
        mv.visitFrame(
                Opcodes.F_FULL,
                4,
                new Object[] {
                    "com/sbf/main/jxbrowser/MiJava",
                    "java/lang/String",
                    "java/lang/String",
                    "java/lang/String"
                },
                1,
                new Object[] {"java/lang/Throwable"});
        mv.visitVarInsn(Opcodes.ASTORE, 4);
        emitStringBuilderPrint(
                mv,
                "M5A_LOCAL_DATACOLLECT_MOCK_WRITE_FAILED ",
                Opcodes.ALOAD,
                4,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/Object;)Ljava/lang/StringBuilder;");
        mv.visitLdcInsn("{\"code\":500,\"submitted\":false,\"localOnly\":true}");
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void writeMiJavaLocalCollectTaskSubmitMethod(MethodVisitor mv) {
        mv.visitAnnotation("Lcom/teamdev/jxbrowser/js/JsAccessible;", true).visitEnd();
        mv.visitCode();
        emitPrint(mv, "M5C_COLLECT_LOCAL_TASK_SUBMIT");
        mv.visitFieldInsn(
                Opcodes.GETSTATIC, "com/sbf/main/StartApp", "a", "Ljava/lang/String;");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/jxbrowser/M5LocalSpiderBridge",
                "submitTask",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void writeMiJavaLocalCollectTaskListMethod(MethodVisitor mv) {
        mv.visitAnnotation("Lcom/teamdev/jxbrowser/js/JsAccessible;", true).visitEnd();
        mv.visitCode();
        emitPrint(mv, "M5C_COLLECT_LOCAL_TASK_LIST");
        mv.visitFieldInsn(
                Opcodes.GETSTATIC, "com/sbf/main/StartApp", "a", "Ljava/lang/String;");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/jxbrowser/M5LocalSpiderBridge",
                "listTasks",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void writeMiJavaUpsertWhatsAppAccountMethod(MethodVisitor mv) {
        mv.visitAnnotation("Lcom/teamdev/jxbrowser/js/JsAccessible;", true).visitEnd();
        mv.visitCode();
        emitPrint(mv, "M8B1A_WHATSAPP_ACCOUNT_BRIDGE_UPSERT");
        mv.visitFieldInsn(
                Opcodes.GETSTATIC, "com/sbf/main/StartApp", "a", "Ljava/lang/String;");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/jxbrowser/M5LocalSpiderBridge",
                "upsertWhatsAppAccount",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void writeMiJavaListWhatsAppAccountsMethod(MethodVisitor mv) {
        mv.visitAnnotation("Lcom/teamdev/jxbrowser/js/JsAccessible;", true).visitEnd();
        mv.visitCode();
        emitPrint(mv, "M8B1A_WHATSAPP_ACCOUNT_BRIDGE_LIST");
        mv.visitFieldInsn(
                Opcodes.GETSTATIC, "com/sbf/main/StartApp", "a", "Ljava/lang/String;");
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/jxbrowser/M5LocalSpiderBridge",
                "listWhatsAppAccounts",
                "(Ljava/lang/String;)Ljava/lang/String;",
                false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void writeMiJavaUpsertWhatsAppMessageMethod(MethodVisitor mv) {
        mv.visitAnnotation("Lcom/teamdev/jxbrowser/js/JsAccessible;", true).visitEnd();
        mv.visitCode();
        emitPrint(mv, "M8B1B_WHATSAPP_MESSAGE_BRIDGE_UPSERT");
        mv.visitFieldInsn(
                Opcodes.GETSTATIC, "com/sbf/main/StartApp", "a", "Ljava/lang/String;");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitVarInsn(Opcodes.ALOAD, 5);
        mv.visitVarInsn(Opcodes.ALOAD, 6);
        mv.visitVarInsn(Opcodes.ALOAD, 7);
        mv.visitVarInsn(Opcodes.LLOAD, 8);
        mv.visitVarInsn(Opcodes.ALOAD, 10);
        mv.visitVarInsn(Opcodes.ALOAD, 11);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/jxbrowser/M5LocalSpiderBridge",
                "upsertWhatsAppMessage",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JLjava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void writeMiJavaListWhatsAppConversationsMethod(MethodVisitor mv) {
        mv.visitAnnotation("Lcom/teamdev/jxbrowser/js/JsAccessible;", true).visitEnd();
        mv.visitCode();
        emitPrint(mv, "M8B1B_WHATSAPP_CONVERSATION_BRIDGE_LIST");
        mv.visitFieldInsn(
                Opcodes.GETSTATIC, "com/sbf/main/StartApp", "a", "Ljava/lang/String;");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/jxbrowser/M5LocalSpiderBridge",
                "listWhatsAppConversations",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void writeMiJavaListWhatsAppMessagesMethod(MethodVisitor mv) {
        mv.visitAnnotation("Lcom/teamdev/jxbrowser/js/JsAccessible;", true).visitEnd();
        mv.visitCode();
        emitPrint(mv, "M8B1B_WHATSAPP_MESSAGE_BRIDGE_LIST");
        mv.visitFieldInsn(
                Opcodes.GETSTATIC, "com/sbf/main/StartApp", "a", "Ljava/lang/String;");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/jxbrowser/M5LocalSpiderBridge",
                "listWhatsAppMessages",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void writeMiJavaSetActiveWhatsAppProfileMethod(MethodVisitor mv) {
        mv.visitAnnotation("Lcom/teamdev/jxbrowser/js/JsAccessible;", true).visitEnd();
        mv.visitCode();
        emitPrint(mv, "M8B1C_WHATSAPP_ACTIVE_PROFILE_BRIDGE_SET");
        mv.visitFieldInsn(
                Opcodes.GETSTATIC, "com/sbf/main/StartApp", "a", "Ljava/lang/String;");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/jxbrowser/M5LocalSpiderBridge",
                "setActiveWhatsAppProfile",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void writeMiJavaGetActiveWhatsAppProfileMethod(MethodVisitor mv) {
        mv.visitAnnotation("Lcom/teamdev/jxbrowser/js/JsAccessible;", true).visitEnd();
        mv.visitCode();
        emitPrint(mv, "M8B1C_WHATSAPP_ACTIVE_PROFILE_BRIDGE_GET");
        mv.visitFieldInsn(
                Opcodes.GETSTATIC, "com/sbf/main/StartApp", "a", "Ljava/lang/String;");
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/jxbrowser/M5LocalSpiderBridge",
                "getActiveWhatsAppProfile",
                "(Ljava/lang/String;)Ljava/lang/String;",
                false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void writeMiJavaSwitchWhatsAppNativeProfileMethod(MethodVisitor mv) {
        mv.visitAnnotation("Lcom/teamdev/jxbrowser/js/JsAccessible;", true).visitEnd();
        mv.visitCode();
        emitPrint(mv, "M8B1C_WHATSAPP_NATIVE_PROFILE_BRIDGE_SWITCH");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/jxbrowser/c",
                "m8SwitchActiveWhatsAppProfile",
                "(Ljava/lang/String;)Ljava/lang/String;",
                false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void writeMiJavaProbeWhatsAppExternalBrowserMethod(MethodVisitor mv) {
        mv.visitAnnotation("Lcom/teamdev/jxbrowser/js/JsAccessible;", true).visitEnd();
        mv.visitCode();
        emitPrint(mv, "M8B1C3_WHATSAPP_EXTERNAL_BROWSER_BRIDGE_PROBE");
        mv.visitFieldInsn(
                Opcodes.GETSTATIC, "com/sbf/main/StartApp", "a", "Ljava/lang/String;");
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/jxbrowser/M8WhatsAppExternalBrowsers",
                "probe",
                "(Ljava/lang/String;)Ljava/lang/String;",
                false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void writeMiJavaStartWhatsAppExternalBrowserMethod(MethodVisitor mv) {
        mv.visitAnnotation("Lcom/teamdev/jxbrowser/js/JsAccessible;", true).visitEnd();
        mv.visitCode();
        emitPrint(mv, "M8B1C3_WHATSAPP_EXTERNAL_BROWSER_BRIDGE_START");
        mv.visitFieldInsn(
                Opcodes.GETSTATIC, "com/sbf/main/StartApp", "a", "Ljava/lang/String;");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/jxbrowser/M8WhatsAppExternalBrowsers",
                "start",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void writeMiJavaStopWhatsAppExternalBrowserMethod(MethodVisitor mv) {
        mv.visitAnnotation("Lcom/teamdev/jxbrowser/js/JsAccessible;", true).visitEnd();
        mv.visitCode();
        emitPrint(mv, "M8B1C3_WHATSAPP_EXTERNAL_BROWSER_BRIDGE_STOP");
        mv.visitFieldInsn(
                Opcodes.GETSTATIC, "com/sbf/main/StartApp", "a", "Ljava/lang/String;");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/jxbrowser/M8WhatsAppExternalBrowsers",
                "stop",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static byte[] generateM5ConsoleObserver() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(
                Opcodes.V1_8,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                "com/sbf/main/jxbrowser/M5ConsoleObserver",
                null,
                "java/lang/Object",
                new String[] {"com/teamdev/jxbrowser/event/Observer"});
        writeDefaultConstructor(cw);
        MethodVisitor mv =
                cw.visitMethod(
                        Opcodes.ACC_PUBLIC,
                        "on",
                        "(Lcom/teamdev/jxbrowser/browser/event/ConsoleMessageReceived;)V",
                        null,
                        null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/browser/event/ConsoleMessageReceived",
                "consoleMessage",
                "()Lcom/teamdev/jxbrowser/js/ConsoleMessage;",
                true);
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("M5_V20_WEB_CONSOLE level=");
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/StringBuilder",
                "<init>",
                "(Ljava/lang/String;)V",
                false);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/js/ConsoleMessage",
                "level",
                "()Lcom/teamdev/jxbrowser/js/ConsoleMessageLevel;",
                true);
        appendObject(mv);
        appendLiteral(mv, " source=");
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/js/ConsoleMessage",
                "source",
                "()Ljava/lang/String;",
                true);
        appendString(mv);
        appendLiteral(mv, ":");
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/js/ConsoleMessage",
                "lineNumber",
                "()I",
                true);
        appendInt(mv);
        appendLiteral(mv, " msg=");
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/js/ConsoleMessage",
                "message",
                "()Ljava/lang/String;",
                true);
        appendString(mv);
        printlnBuilder(mv);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        writeObserverBridge(
                cw,
                "com/sbf/main/jxbrowser/M5ConsoleObserver",
                "com/teamdev/jxbrowser/browser/event/ConsoleMessageReceived",
                "(Lcom/teamdev/jxbrowser/browser/event/ConsoleMessageReceived;)V");
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] generateM5InjectJsCallback() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                return "java/lang/Object";
            }
        };
        cw.visit(
                Opcodes.V1_8,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                "com/sbf/main/jxbrowser/M5InjectJsCallback",
                null,
                "java/lang/Object",
                new String[] {"com/teamdev/jxbrowser/browser/callback/InjectJsCallback"});
        writeDefaultConstructor(cw);
        MethodVisitor mv =
                cw.visitMethod(
                        Opcodes.ACC_PUBLIC,
                        "on",
                        "(Lcom/teamdev/jxbrowser/browser/callback/InjectJsCallback$Params;)"
                                + "Lcom/teamdev/jxbrowser/browser/callback/InjectJsCallback$Response;",
                        null,
                        null);
        org.objectweb.asm.Label start = new org.objectweb.asm.Label();
        org.objectweb.asm.Label end = new org.objectweb.asm.Label();
        org.objectweb.asm.Label handler = new org.objectweb.asm.Label();
        org.objectweb.asm.Label after = new org.objectweb.asm.Label();
        mv.visitTryCatchBlock(start, end, handler, "java/lang/Throwable");
        mv.visitCode();
        mv.visitLabel(start);
        emitM5DataCollectMiJavaBridge(mv);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/browser/callback/InjectJsCallback$Params",
                "frame",
                "()Lcom/teamdev/jxbrowser/frame/Frame;",
                true);
        mv.visitLdcInsn(
                "(function(){"
                        + "if(window.__m5JsonDiagInstalled){return;}"
                        + "window.__m5JsonDiagInstalled=true;"
                        + "var __m5GetInfoBody=" + jsSingleQuoted(WEB_BOOTSTRAP_GET_INFO_JSON) + ";"
                        + "var __m5RoutersBody=" + jsSingleQuoted(WEB_BOOTSTRAP_ROUTERS_JSON) + ";"
                        + "var __m5AicloudMylistBody=" + jsSingleQuoted(WEB_BOOTSTRAP_AICLOUD_MYLIST_JSON) + ";"
                        + "var __m5YesNoDictBody=" + jsSingleQuoted(WEB_BOOTSTRAP_YES_NO_DICT_JSON) + ";"
                        + "var __m5SpiderConfigBody=" + jsSingleQuoted(LOCAL_WHATSAPP_USERS_HTTP_CONFIG_JSON) + ";"
                        + "var __m5CloudHostListBody=" + jsSingleQuoted(WEB_BOOTSTRAP_CLOUD_HOST_LIST_JSON) + ";"
                        + "var __m5AreaOptionsBody='{\\\"code\\\":200,\\\"msg\\\":\\\"success\\\",\\\"data\\\":[{\\\"label\\\":\\\"北美\\\",\\\"children\\\":[{\\\"code\\\":\\\"+1\\\",\\\"label\\\":\\\"美国/加拿大 +1\\\",\\\"iconUrl\\\":\\\"\\\"}]}]}';"
                        + "var __m5PlatformOptionsBody='{\\\"code\\\":200,\\\"msg\\\":\\\"success\\\",\\\"data\\\":[{\\\"label\\\":\\\"搜索平台\\\",\\\"children\\\":[{\\\"code\\\":\\\"facebook.com\\\",\\\"label\\\":\\\"Facebook\\\",\\\"iconUrl\\\":\\\"\\\"},{\\\"code\\\":\\\"google.com\\\",\\\"label\\\":\\\"Google\\\",\\\"iconUrl\\\":\\\"\\\"}]}]}';"
                        + "var __m5KeywordsOptionsBody='{\\\"code\\\":200,\\\"msg\\\":\\\"success\\\",\\\"data\\\":[{\\\"label\\\":\\\"关键词\\\",\\\"children\\\":[{\\\"code\\\":\\\"local-test\\\",\\\"label\\\":\\\"local-test\\\",\\\"iconUrl\\\":\\\"\\\"}]}]}';"
                        + "function __m8EnsureTerminalCopyBridge(){try{if(window.__m8TerminalCopyBridgeInstalled){return;}window.__m8TerminalCopyBridgeInstalled=true;"
                        + "var native=null;try{native=window.mijava||window.java||null;}catch(e){native=null;}"
                        + "var shim={label:function(key,fallback){return fallback||key||'';},copyToClipboard:function(text,cb){console.log('M8B_COPY_TO_CLIPBOARD_TERMINAL');setTimeout(function(){try{if(cb){cb('\\u590d\\u5236\\u6210\\u529f');}}catch(e){console.error('M8B_COPY_TO_CLIPBOARD_CALLBACK_FAIL '+e);}},0);return null;}};"
                        + "function delegate(prop){return function(){try{var v=native&&native[prop];if(typeof v==='function'){return v.apply(native,arguments);}if(v!=null){return v;}}catch(e){}return null;};}"
                        + "window.mijava=(typeof Proxy==='function')?new Proxy(shim,{get:function(t,p){if(p in t){return t[p];}return delegate(p);},set:function(t,p,v){t[p]=v;return true;}}):shim;window.java=window.mijava;"
                        + "}catch(e){console.error('M8B_COPY_TO_CLIPBOARD_BRIDGE_FAIL '+e);}}__m8EnsureTerminalCopyBridge();"
                        + "function __m8C5GatePlatformUi(){try{var p=String(location.pathname||'');if(p.indexOf('/es/bigData/bigDataTask')<0&&p.indexOf('/pc/tg/index')<0&&p.indexOf('/pc/dataCollect/googleseo')<0&&p.indexOf('/pc/kefu/conversation')<0){return;}"
                        + "function gate(){try{var nodes=document.querySelectorAll('button,[role=button],.el-button');var words=['\\u63d0\\u4ea4','\\u65b0\\u589e','\\u65b0\\u5efa','\\u6dfb\\u52a0','\\u521b\\u5efa','\\u5bfc\\u51fa','\\u5220\\u9664','\\u4fee\\u6539','\\u91c7\\u96c6'];for(var i=0;i<nodes.length;i++){var el=nodes[i],t=String(el.innerText||el.textContent||'').replace(/\\s+/g,'');var hit=false;for(var j=0;j<words.length;j++){if(t.indexOf(words[j])>=0){hit=true;break;}}if(hit){el.disabled=true;el.setAttribute('aria-disabled','true');el.setAttribute('title','C5 UI only');el.style.pointerEvents='none';el.style.opacity='.7';el.style.cursor='not-allowed';el.style.backgroundColor='#c0c4cc';el.style.borderColor='#c0c4cc';if(!el.__c5Blocked){el.__c5Blocked=true;el.addEventListener('click',function(e){e.preventDefault();e.stopImmediatePropagation();},true);}}}}catch(e){}}gate();setTimeout(gate,300);setTimeout(gate,1200);setTimeout(gate,3000);setTimeout(gate,8000);if(typeof MutationObserver==='function'){new MutationObserver(function(){setTimeout(gate,0);}).observe(document.documentElement,{childList:true,subtree:true});}console.log('C5_PLATFORM_UI_GATED path='+p);}catch(e){console.error('C5_PLATFORM_UI_GATE_FAIL '+e);}}__m8C5GatePlatformUi();"
                        + "function __m8EnsureAiKefuMiJavaShim(){try{if(String(location.href).indexOf('/ingsale/aggregationKefu/index')<0){return;}"
                        + "if(window.__m8AiKefuMiJavaShimInstalled){return;}window.__m8AiKefuMiJavaShimInstalled=true;"
                        + "var native=null;try{native=window.mijava||window.java||null;}catch(e){native=null;}window.__m8NativeMijava=native;"
                        + "function noop(){return null;}function noops(){return '';}"
                        + "function finalEvent(n){return JSON.stringify({type:String(n||''),code:200,data:{connected:false,online:false,loginStatus:0,status:0,rows:[],total:0},message:JSON.stringify({rows:[],total:0,status:'CLOSED',channel:'WHATSAPP_API'})});}"
                        + "function terminalGuard(){try{var n=0;var tick=function(){try{if(String(location.href).indexOf('/ingsale/aggregationKefu/index')<0){return;}var els=document.querySelectorAll('.right-loading,.page');for(var i=0;i<els.length;i++){var e=els[i];while(e){var v=e.__vue__;if(v&&Object.prototype.hasOwnProperty.call(v,'pageLoading')&&('msgTab' in v)){if(v.pageLoading===1||document.querySelector('.right-loading')){v.pageLoading=0;v.leftDataLoading=true;v.msgTab=[];v.leftCount=0;console.log('M8_AI_KEFU_WA_TERMINAL_GUARD');}break;}e=e.parentElement;}}}catch(x){}n++;if(n<20){setTimeout(tick,500);}};setTimeout(tick,500);}catch(e){}}"
                        + "var shim={regMessageEvent:function(n,cb){console.log('M8_AI_KEFU_MIJAVA_SHIM regMessageEvent '+n);console.log('M8_AI_KEFU_MIJAVA_EVENT_FINAL '+n);return finalEvent(n);},"
                        + "toOpenFileSelect:function(){console.log('M8_AI_KEFU_MIJAVA_SHIM toOpenFileSelect');return '{}';},"
                        + "uploadFileDoHK:function(p,n,o){console.log('M8_AI_KEFU_MIJAVA_SHIM uploadFileDoHK');try{if(o&&o.complate){o.complate('');}}catch(e){}return null;},"
                        + "toTranslationText:function(t,lang,cb){try{if(cb){cb(String(t||''));}}catch(e){}return String(t||'');},"
                        + "dowloadFile:noop,showGG2BaiduMap:noop,showKEFUAngle:noop,logout:noop,isSpacie:function(){return true;}};"
                        + "function delegate(prop){return function(){try{var v=native&&native[prop];if(typeof v==='function'){return v.apply(native,arguments);}if(v!=null){return v;}}catch(e){}return null;};}"
                        + "window.mijava=(typeof Proxy==='function')?new Proxy(shim,{get:function(t,p){if(p in t){return t[p];}return delegate(p);},set:function(t,p,v){t[p]=v;return true;}}):shim;terminalGuard();"
                        + "console.log('M8_AI_KEFU_MIJAVA_SHIM');}catch(e){console.error('M8_AI_KEFU_MIJAVA_SHIM_FAIL '+e);}}__m8EnsureAiKefuMiJavaShim();"
                        + "function __m8EnsureOnelineAiBotShim(){try{if(String(location.href).indexOf('/pc/aigc/aichat_dialog')<0){return;}"
                        + "if(window.__m8OnelineAiBotShimInstalled){return;}window.__m8OnelineAiBotShimInstalled=true;"
                        + "var native=null;try{native=window.mijava||window.java||null;}catch(e){native=null;}window.__m8OnelineNativeMijava=native;"
                        + "var terminalText='\\u672c\\u5730 AI \\u751f\\u6210\\u6682\\u672a\\u63a5\\u5165,\\u771f\\u5b9e\\u751f\\u6210\\u529f\\u80fd\\u5355\\u72ec\\u7acb\\u9879';"
                        + "var shim={aibotChat:function(prompt,cb){console.log('M8_ONELINE_AIBOT_SHIM prompt='+String(prompt||''));"
                        + "setTimeout(function(){try{if(cb){cb(JSON.stringify({event:'cmpl',text:terminalText}));}}catch(e){console.error('M8_ONELINE_AIBOT_SHIM_CMPL_FAIL '+e);}},0);"
                        + "setTimeout(function(){try{if(cb){cb(JSON.stringify({event:'all_done',text:''}));}}catch(e){console.error('M8_ONELINE_AIBOT_SHIM_DONE_FAIL '+e);}},20);return null;}};"
                        + "function delegate(prop){return function(){try{var v=native&&native[prop];if(typeof v==='function'){return v.apply(native,arguments);}if(v!=null){return v;}}catch(e){}return null;};}"
                        + "window.mijava=(typeof Proxy==='function')?new Proxy(shim,{get:function(t,p){if(p in t){return t[p];}return delegate(p);},set:function(t,p,v){t[p]=v;return true;}}):shim;window.java=window.mijava;"
                        + "console.log('M8_ONELINE_AIBOT_SHIM');}catch(e){console.error('M8_ONELINE_AIBOT_SHIM_FAIL '+e);}}__m8EnsureOnelineAiBotShim();"
                        + "function __m8EnsureSmartAiMiJavaShim(){try{if(String(location.href).indexOf('/aiAgent/smartAi')<0){return;}"
                        + "if(window.__m8SmartAiMiJavaShimInstalled){return;}window.__m8SmartAiMiJavaShimInstalled=true;"
                        + "var native=null;try{native=window.mijava||window.java||null;}catch(e){native=null;}window.__m8SmartAiNativeMijava=native;"
                        + "function noop(){return null;}var shim={dowloadFile:function(u){console.log('M8_SMART_AI_MIJAVA_SHIM dowloadFile '+String(u||''));return null;},downloadFile:function(u){console.log('M8_SMART_AI_MIJAVA_SHIM downloadFile '+String(u||''));return null;}};"
                        + "function delegate(prop){return function(){try{var v=native&&native[prop];if(typeof v==='function'){return v.apply(native,arguments);}if(v!=null){return v;}}catch(e){}return null;};}"
                        + "window.mijava=(typeof Proxy==='function')?new Proxy(shim,{get:function(t,p){if(p in t){return t[p];}return delegate(p);},set:function(t,p,v){t[p]=v;return true;}}):shim;window.java=window.mijava;"
                        + "console.log('M8_SMART_AI_MIJAVA_SHIM agent_template.txt');}catch(e){console.error('M8_SMART_AI_MIJAVA_SHIM_FAIL '+e);}}__m8EnsureSmartAiMiJavaShim();"
                        + "function __m8EnsureWhatsAppWebLoginProbe(){try{if(String(location.host)!=='web.whatsapp.com'){return;}"
                        + "if(window.__m8b1aWhatsAppWebLoginProbeInstalled){return;}window.__m8b1aWhatsAppWebLoginProbeInstalled=true;"
                        + "var profileId='wa-default';var lastReload=0;"
                        + "function parseJson(s){try{return JSON.parse(String(s||'{}'));}catch(e){return {};}}"
                        + "function currentProfile(){try{var q=new URL(location.href).searchParams.get('m8Profile')||'';if(q){localStorage.setItem('__m8b1cNativeProfileId',q);profileId=q;return q;}var v=localStorage.getItem('__m8b1cNativeProfileId')||'';if(v){profileId=v;return v;}}catch(e){}return profileId||'wa-default';}"
                        + "function setProfile(p){try{p=String(p||'').trim()||'wa-default';localStorage.setItem('__m8b1cNativeProfileId',p);profileId=p;if(window.mijava&&window.mijava.m8SetActiveWhatsAppProfile){console.log('M8B1C_PROFILE_SWITCH '+String(window.mijava.m8SetActiveWhatsAppProfile(p)||''));}}catch(e){console.error('M8B1C_PROFILE_SWITCH_FAIL '+e);}return profileId;}"
                        + "function byId(id){return document.getElementById(id);}"
                        + "function phoneFromStorage(){try{var ks=['last-wid','last-wid-md','me','debugInfo'];for(var i=0;i<ks.length;i++){var v=localStorage.getItem(ks[i])||'';var m=String(v).match(/(\\d{6,})/);if(m){return '+'+m[1];}}}catch(e){}return '';}"
                        + "function phone(){var p=phoneFromStorage();if(p){return p;}var el=byId('__m8b1aPhone');return el?String(el.value||'').trim():'';}"
                        + "function status(){try{var body=(document.body&&document.body.innerText)||'';var logged=!!document.querySelector('#pane-side,[data-testid=\"chat-list\"],[aria-label=\"Chat list\"],[aria-label=\"Chats\"]');var qr=!!document.querySelector('canvas,[data-ref],div[data-testid=\"qrcode\"]');var down=/computer.*not connected|phone.*not connected|disconnected|trying to reach phone/i.test(body);if(down){return 'disconnected';}if(logged){return 'logged_in';}if(qr){return 'qr';}return 'not_logged_in';}catch(e){return 'unknown';}}"
                        + "function ensureBar(){var bar=byId('__m8b1aWAStatus');if(bar){return bar;}bar=document.createElement('div');bar.id='__m8b1aWAStatus';bar.style.cssText='position:fixed;z-index:2147483647;right:12px;top:12px;background:#101820;color:#fff;border:1px solid #2dd4bf;border-radius:6px;padding:8px 10px;font:12px Arial,sans-serif;box-shadow:0 6px 18px rgba(0,0,0,.25);display:flex;gap:8px;align-items:center;';bar.innerHTML='<b>WA</b><span id=\"__m8b1aState\">checking</span><input id=\"__m8b1aPhone\" placeholder=\"phone\" style=\"width:118px;border:1px solid #456;border-radius:4px;padding:3px 5px;background:#fff;color:#111\"><button id=\"__m8b1aSave\" style=\"border:0;border-radius:4px;padding:4px 7px;background:#2dd4bf;color:#06201b;cursor:pointer\">save</button>';document.documentElement.appendChild(bar);var btn=byId('__m8b1aSave');if(btn){btn.onclick=function(){save(true);};}return bar;}"
                        + "function save(manual){try{var st=status();var ph=phone();var pid=currentProfile();var payload={source:'m8b1a-whatsapp-web',href:location.href,status:st,phone:ph,profileId:pid,manual:!!manual,ts:Date.now()};if(window.mijava&&window.mijava.m8UpsertWhatsAppAccount){var r=window.mijava.m8UpsertWhatsAppAccount(pid,ph,st,JSON.stringify(payload));console.log('M8B1A_WHATSAPP_ACCOUNT_SAVE '+String(r||''));}}catch(e){console.error('M8B1A_WHATSAPP_ACCOUNT_SAVE_FAIL '+e);}}"
                        + "function tick(){try{ensureBar();var st=status();var s=byId('__m8b1aState');if(s){s.textContent=st;}console.log('M8B1A_WHATSAPP_WEB_STATUS '+st);if(st==='logged_in'||st==='qr'||st==='disconnected'){save(false);}if(st==='disconnected'&&Date.now()-lastReload>15000){lastReload=Date.now();setTimeout(function(){try{location.reload();}catch(e){}},3000);}}catch(e){console.error('M8B1A_WHATSAPP_WEB_STATUS_FAIL '+e);}}"
                        + "function listAccounts(){try{if(window.mijava&&window.mijava.m8ListWhatsAppAccounts){console.log('M8B1A_WHATSAPP_ACCOUNT_LIST '+String(window.mijava.m8ListWhatsAppAccounts()||''));}}catch(e){console.error('M8B1A_WHATSAPP_ACCOUNT_LIST_FAIL '+e);}}"
                        + "tick();listAccounts();setInterval(tick,3000);console.log('M8B1A_WHATSAPP_WEB_LOGIN_PROBE_READY https://web.whatsapp.com');}catch(e){console.error('M8B1A_WHATSAPP_WEB_LOGIN_PROBE_FAIL '+e);}}__m8EnsureWhatsAppWebLoginProbe();"
                        + "function __m8EnsureWhatsAppSessionBridge(){try{if(String(location.host)!=='web.whatsapp.com'){return;}"
                        + "if(window.__m8b1bWhatsAppSessionBridgeInstalled){return;}window.__m8b1bWhatsAppSessionBridgeInstalled=true;"
                        + "var profileId='wa-default';var captureProfileId='';var selectedConversation='';var seen={};"
                        + "function byId(id){return document.getElementById(id);}function txt(e){return String((e&&e.innerText)||'').replace(/\\s+/g,' ').trim();}"
                        + "function lines(e){return String((e&&e.innerText)||'').split(/\\n+/).map(function(x){return x.trim();}).filter(Boolean);}"
                        + "function clean(s){return String(s||'').replace(/\\s+/g,' ').trim();}"
                        + "function key(s,p){s=clean(s);var out='';for(var i=0;i<s.length;i++){var c=s.charAt(i);out+=/[A-Za-z0-9_+@.-]/.test(c)?c:'-';}return out||(p+'-'+Date.now());}"
                        + "function hash(s){var h=0;s=String(s||'');for(var i=0;i<s.length;i++){h=((h<<5)-h+s.charCodeAt(i))|0;}return Math.abs(h).toString(16);}"
                        + "function parseJson(s){try{return JSON.parse(String(s||'{}'));}catch(e){return {};}}"
                        + "function nativeProfile(){try{var q=new URL(location.href).searchParams.get('m8Profile')||'';if(q){localStorage.setItem('__m8b1cNativeProfileId',q);captureProfileId=q;return q;}var v=localStorage.getItem('__m8b1cNativeProfileId')||'';if(v){captureProfileId=v;return v;}}catch(e){}return captureProfileId||profileId||'wa-default';}"
                        + "captureProfileId=nativeProfile();profileId=captureProfileId;"
                        + "function currentProfile(){try{var v=localStorage.getItem('__m8b1cProfileId')||'';if(v){return v;}if(window.mijava&&window.mijava.m8GetActiveWhatsAppProfile){var o=parseJson(window.mijava.m8GetActiveWhatsAppProfile());v=o&&o.data&&o.data.profileId;if(v){localStorage.setItem('__m8b1cProfileId',v);return v;}}}catch(e){}return profileId;}"
                        + "function captureProfile(){return captureProfileId||nativeProfile()||'wa-default';}"
                        + "function probeExternal(){try{if(window.mijava&&window.mijava.m8ProbeWhatsAppExternalBrowser){var r=String(window.mijava.m8ProbeWhatsAppExternalBrowser()||'');console.log('M8B1C3_EXTERNAL_BROWSER_PROBE '+r);return r;}}catch(e){console.error('M8B1C3_EXTERNAL_BROWSER_PROBE_FAIL '+e);}return '';}"
                        + "function openExternal(p,ph){try{p=String(p||currentProfile()||'wa-default').trim()||'wa-default';ph=String(ph||'').trim();localStorage.setItem('__m8b1cProfileId',p);profileId=p;if(window.mijava&&window.mijava.m8StartWhatsAppExternalBrowser){var r=String(window.mijava.m8StartWhatsAppExternalBrowser(p,ph)||'');console.log('M8B1C3_EXTERNAL_BROWSER_OPEN '+r);renderAccounts();return r;}}catch(e){console.error('M8B1C3_EXTERNAL_BROWSER_OPEN_FAIL '+e);}return '';}"
                        + "function stopExternal(p){try{p=String(p||currentProfile()||'wa-default').trim()||'wa-default';if(window.mijava&&window.mijava.m8StopWhatsAppExternalBrowser){var r=String(window.mijava.m8StopWhatsAppExternalBrowser(p)||'');console.log('M8B1C3_EXTERNAL_BROWSER_STOP '+r);renderAccounts();return r;}}catch(e){console.error('M8B1C3_EXTERNAL_BROWSER_STOP_FAIL '+e);}return '';}"
                        + "function switchProfile(p){try{p=String(p||'').trim()||'wa-default';localStorage.setItem('__m8b1cProfileId',p);profileId=p;selectedConversation='';lastNativeSwitch='';var r='';if(window.mijava&&window.mijava.m8SetActiveWhatsAppProfile){r=String(window.mijava.m8SetActiveWhatsAppProfile(p)||'');}if(window.mijava&&window.mijava.m8StartWhatsAppExternalBrowser){openExternal(p,'');}else if(window.mijava&&window.mijava.m8SwitchWhatsAppNativeProfile){r=String(window.mijava.m8SwitchWhatsAppNativeProfile(p)||'');}console.log('M8B1C_PROFILE_SWITCH display='+p+' capture='+captureProfile()+' native='+r);renderConversations();}catch(e){console.error('M8B1C_PROFILE_SWITCH_FAIL '+e);}}"
                        + "var lastNativeSwitch='';function ensureNativeProfile(){try{if(window.mijava&&window.mijava.m8StartWhatsAppExternalBrowser){return;}var p=currentProfile();var cp=captureProfile();if(p&&cp&&p!==cp&&p!==lastNativeSwitch&&window.mijava&&window.mijava.m8SwitchWhatsAppNativeProfile){lastNativeSwitch=p;var r=String(window.mijava.m8SwitchWhatsAppNativeProfile(p)||'');console.log('M8B1C_PROFILE_NATIVE_ENSURE display='+p+' capture='+cp+' native='+r);}}catch(e){console.error('M8B1C_PROFILE_NATIVE_ENSURE_FAIL '+e);}}"
                        + "function emit(payload){try{var cbs=window.__m8b1bRegMessageCallbacks||[];for(var i=0;i<cbs.length;i++){try{cbs[i](payload);}catch(x){}}}catch(e){}}"
                        + "window.__m8b1bRegMessageCallbacks=window.__m8b1bRegMessageCallbacks||[];window.__m8b1bRegMessageEvent=function(n,cb){if(typeof cb==='function'){window.__m8b1bRegMessageCallbacks.push(cb);}return JSON.stringify({code:200,type:String(n||'message'),connected:true});};"
                        + "function save(conv,title,phone,dir,sender,msg,ts,id,raw){try{msg=clean(msg);title=clean(title);if(!conv||!msg||!window.mijava||!window.mijava.m8UpsertWhatsAppMessage){return;}var pid=captureProfile();var mid=id||('dom-'+hash(conv+'|'+msg+'|'+ts));var dedupe=pid+'|'+conv+'|'+mid;if(seen[dedupe]){return;}seen[dedupe]=1;var payload={source:'m8b1b-whatsapp-web',href:location.href,profileId:pid,title:title,ts:ts,raw:raw||{}};var r=window.mijava.m8UpsertWhatsAppMessage(pid,conv,phone||'',title||conv,dir||'inbound',sender||phone||title||'',msg,Math.floor(ts||Date.now()),mid,JSON.stringify(payload));console.log('M8B1B_WHATSAPP_MESSAGE_CAPTURE '+String(r||''));emit(payload);}catch(e){console.error('M8B1B_WHATSAPP_MESSAGE_CAPTURE_FAIL '+e);}}"
                        + "function currentTitle(){try{var e=document.querySelector('header span[title],header [data-testid=\"conversation-info-header-chat-title\"],header [dir=\"auto\"]');return clean(e&&(e.getAttribute('title')||e.innerText));}catch(e){return '';}}"
                        + "function captureList(){try{var rows=document.querySelectorAll('#pane-side [role=\"row\"],#pane-side [role=\"listitem\"],#pane-side [data-testid=\"cell-frame-container\"]');for(var i=0;i<rows.length&&i<60;i++){var l=lines(rows[i]);if(l.length<2){continue;}var title=clean(l[0]);var preview='';for(var j=l.length-1;j>0;j--){if(!/^\\d{1,2}:\\d{2}/.test(l[j])&&!/^yesterday$/i.test(l[j])&&!/^\\d+$/.test(l[j])){preview=l[j];break;}}if(!title||!preview||title===preview){continue;}var conv=key(title,'chat');save(conv,title,/\\+?\\d[\\d\\s-]{5,}/.test(title)?title:'','inbound',title,preview,Date.now(),'list-'+hash(title+'|'+preview),{surface:'chat-list'});}}catch(e){console.error('M8B1B_CAPTURE_LIST_FAIL '+e);}}"
                        + "function captureOpen(){try{var title=currentTitle();if(!title){return;}var conv=key(title,'chat');selectedConversation=conv;var nodes=document.querySelectorAll('[data-id],div.message-in,div.message-out,[data-testid=\"msg-container\"]');for(var i=0;i<nodes.length&&i<120;i++){var n=nodes[i];var msg='';var spans=n.querySelectorAll&&n.querySelectorAll('span.selectable-text');if(spans&&spans.length){var parts=[];for(var s=0;s<spans.length;s++){parts.push(clean(spans[s].innerText));}msg=clean(parts.join(' '));}else{msg=txt(n);}if(!msg||msg.length>2000){continue;}var cls=String(n.className||'');var dir=cls.indexOf('message-out')>=0?'outbound':'inbound';var id=n.getAttribute&&n.getAttribute('data-id');save(conv,title,'',dir,title,msg,Date.now(),id||('open-'+hash(title+'|'+msg)),{surface:'open-chat'});}}catch(e){console.error('M8B1B_CAPTURE_OPEN_FAIL '+e);}}"
                        + "function rowsFrom(r){var o=parseJson(r);return o.rows||o.data||[];}"
                        + "function renderAccounts(){try{ensurePanel();var sel=byId('__m8b1cAccountSelect');if(!sel||!window.mijava||!window.mijava.m8ListWhatsAppAccounts){return;}var rows=rowsFrom(window.mijava.m8ListWhatsAppAccounts());var cur=currentProfile();sel.innerHTML='';if(!rows.length){rows=[{profileId:cur,phone:'',status:'profile_ready'}];}rows.forEach(function(a){var opt=document.createElement('option');opt.value=String(a.profileId||'wa-default');opt.textContent=String((a.phone||a.profileId||'wa-default')+' '+(a.status||''));if(opt.value===cur||a.active){opt.selected=true;profileId=opt.value;}sel.appendChild(opt);});console.log('M8B1C_ACCOUNT_SELECT_RENDER total='+rows.length+' active='+currentProfile());}catch(e){console.error('M8B1C_ACCOUNT_SELECT_RENDER_FAIL '+e);}}"
                        + "function ensurePanel(){var p=byId('__m8b1bPanel');if(p){return p;}p=document.createElement('div');p.id='__m8b1bPanel';p.style.cssText='position:fixed;right:12px;bottom:12px;width:520px;height:430px;z-index:2147483646;background:#f8fafc;color:#111827;border:1px solid #94a3b8;border-radius:6px;box-shadow:0 14px 35px rgba(0,0,0,.28);font:12px Arial,sans-serif;display:flex;flex-direction:column;overflow:hidden;';p.innerHTML='<div style=\"height:34px;background:#0f172a;color:#fff;display:flex;align-items:center;gap:6px;padding:0 8px\"><b>Local Kefu</b><select id=\"__m8b1cAccountSelect\" style=\"min-width:145px;max-width:180px;border:1px solid #475569;border-radius:4px;background:#fff;color:#111;padding:3px\"></select><button id=\"__m8b1c3AddExternal\" style=\"border:0;border-radius:4px;padding:4px 7px;background:#bae6fd;color:#082f49;cursor:pointer\">add</button><button id=\"__m8b1c3OpenExternal\" style=\"border:0;border-radius:4px;padding:4px 7px;background:#2dd4bf;color:#06201b;cursor:pointer\">open</button><button id=\"__m8b1c3StopExternal\" style=\"border:0;border-radius:4px;padding:4px 7px;background:#fecaca;color:#7f1d1d;cursor:pointer\">stop</button><button id=\"__m8b1bRefresh\" style=\"margin-left:auto;border:0;border-radius:4px;padding:4px 7px;background:#e2e8f0;color:#111827;cursor:pointer\">refresh</button></div><div style=\"display:flex;min-height:0;flex:1\"><div id=\"__m8b1bConversations\" style=\"width:185px;border-right:1px solid #cbd5e1;overflow:auto;background:#fff\"></div><div id=\"__m8b1bMessages\" style=\"flex:1;overflow:auto;padding:8px;background:#f8fafc\"></div></div><div style=\"border-top:1px solid #cbd5e1;padding:6px 8px;color:#64748b;background:#fff\">read only - no send</div>';document.documentElement.appendChild(p);var b=byId('__m8b1bRefresh');if(b){b.onclick=function(){probeExternal();renderAccounts();renderConversations();};}var s=byId('__m8b1cAccountSelect');if(s){s.onchange=function(){switchProfile(this.value);};}var oe=byId('__m8b1c3OpenExternal');if(oe){oe.onclick=function(){openExternal(currentProfile(),'');};}var se=byId('__m8b1c3StopExternal');if(se){se.onclick=function(){stopExternal(currentProfile());};}var ae=byId('__m8b1c3AddExternal');if(ae){ae.onclick=function(){var p=prompt('profile_id','wa-'+Date.now());if(!p){return;}var ph=prompt('phone optional','')||'';openExternal(p,ph);};}console.log('M8B1B_CUSTOMER_PANEL_READY');return p;}"
                        + "function renderMessages(conv){try{ensurePanel();selectedConversation=conv;var box=byId('__m8b1bMessages');if(!box||!window.mijava||!window.mijava.m8ListWhatsAppMessages){return;}var rows=rowsFrom(window.mijava.m8ListWhatsAppMessages(currentProfile(),conv));box.innerHTML='';if(!rows.length){box.innerHTML='<div style=\"color:#64748b\">No messages</div>';return;}rows.forEach(function(m){var d=document.createElement('div');var out=String(m.direction||'')==='outbound';d.style.cssText='margin:0 0 7px '+(out?'42px':'0')+';padding:6px 8px;border-radius:6px;background:'+(out?'#dcfce7':'#fff')+';border:1px solid #e2e8f0;word-break:break-word;';d.textContent=String(m.messageText||'');box.appendChild(d);});box.scrollTop=box.scrollHeight;console.log('M8B1B_PANEL_RENDER messages='+rows.length+' profileId='+currentProfile());}catch(e){console.error('M8B1B_PANEL_RENDER_MESSAGES_FAIL '+e);}}"
                        + "function renderConversations(){try{ensurePanel();var list=byId('__m8b1bConversations');if(!list||!window.mijava||!window.mijava.m8ListWhatsAppConversations){return;}var rows=rowsFrom(window.mijava.m8ListWhatsAppConversations(currentProfile()));list.innerHTML='';if(!rows.length){list.innerHTML='<div style=\"padding:10px;color:#64748b\">No chats yet</div>';var box=byId('__m8b1bMessages');if(box){box.innerHTML='<div style=\"color:#64748b\">No messages</div>';}return;}rows.forEach(function(c,idx){var d=document.createElement('button');d.type='button';d.style.cssText='display:block;width:100%;border:0;border-bottom:1px solid #e2e8f0;background:'+(c.conversationKey===selectedConversation?'#ecfeff':'#fff')+';padding:8px;text-align:left;cursor:pointer;';d.innerHTML='<div style=\"font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis\"></div><div style=\"color:#64748b;white-space:nowrap;overflow:hidden;text-overflow:ellipsis\"></div>';d.children[0].textContent=String(c.title||c.conversationKey||'chat');d.children[1].textContent=String(c.lastMessageText||'');d.onclick=function(){renderMessages(String(c.conversationKey||''));};list.appendChild(d);if((!selectedConversation&&idx===0)||c.conversationKey===selectedConversation){selectedConversation=String(c.conversationKey||'');}});if(selectedConversation){renderMessages(selectedConversation);}console.log('M8B1B_PANEL_RENDER conversations='+rows.length+' profileId='+currentProfile());}catch(e){console.error('M8B1B_PANEL_RENDER_CONVERSATIONS_FAIL '+e);}}"
                        + "function tick(){ensureNativeProfile();captureList();captureOpen();renderAccounts();renderConversations();}ensurePanel();tick();setInterval(tick,4000);console.log('M8B1B_WHATSAPP_SESSION_BRIDGE_READY');}catch(e){console.error('M8B1B_WHATSAPP_SESSION_BRIDGE_FAIL '+e);}}__m8EnsureWhatsAppSessionBridge();"
                        + "function __m8GateSuperEnvironmentUi(){var route='/pc/sender/senderGlobalControls/mysuperenvironment';"
                        + "function gate(){try{if(location.pathname.indexOf(route)<0){return;}var buttons=document.querySelectorAll('.search-btn button');for(var i=0;i<buttons.length&&i<2;i++){var button=buttons[i];if(!button.disabled){button.disabled=true;button.setAttribute('disabled','disabled');button.setAttribute('title','Local UI only; original SaaS action is not connected');if(button.classList){button.classList.add('is-disabled');}}}if(!window.__m8SuperEnvironmentUiGated){window.__m8SuperEnvironmentUiGated=true;console.log('M8B_SUPER_ENV_UI_GATED route='+location.pathname);}}catch(e){console.error('M8B_SUPER_ENV_UI_GATE_FAILED '+e);}}"
                        + "gate();if(window.MutationObserver&&document.documentElement){var observer=new MutationObserver(gate);observer.observe(document.documentElement,{childList:true,subtree:true});}}"
                        + "__m8GateSuperEnvironmentUi();"
                        + "function __m5BootstrapBody(u,body,method){u=String(u||'');method=String(method||'GET').toUpperCase();"
                        + "function qp(k,d){try{var x=new URL(u,location.href).searchParams.get(k);return x||d;}catch(e){return d;}}"
                        + "function sc(prefix,d){try{var x=new URL(u,location.href);var p=x.pathname;var i=p.indexOf(prefix);if(i>=0){var s=p.substring(i+prefix.length).split('/')[0];return decodeURIComponent(s||d);}return x.searchParams.get('spiderCode')||x.searchParams.get('modal')||d;}catch(e){return d;}}"
                        + "function cfg(o){try{['fields','spiderParams','hookurls','steps'].forEach(function(k){if(o&&o[k]!=null&&typeof o[k]!=='string'){o[k]=JSON.stringify(o[k]);}});}catch(e){}return o;}"
                        + "function js(o){return JSON.stringify(o);}"
                        + "function path(){try{return new URL(u,location.href).pathname;}catch(e){return u;}}"
                        + "function m8SmartAi(){var p=path();if(p.indexOf('/volcengine/')<0){return null;}"
                        + "var rows=window.__m8SmartAiRows||(window.__m8SmartAiRows=[]);"
                        + "function agentFromBody(){var raw=body;var parsed=null;try{parsed=raw?JSON.parse(raw):null;}catch(e){parsed=null;}parsed=parsed||{};return {id:'local-smart-ai',name:parsed.name||'\\u672c\\u5730\\u667a\\u80fd\\u4f53',headIcon:'',userValue:parsed.userValue||'\\u672c\\u5730 UI \\u7a7a\\u6001\\u5360\\u4f4d',description:parsed.description||'\\u771f\\u5b9e\\u667a\\u80fd\\u4f53\\u751f\\u6210\\u4e0e\\u5bf9\\u8bdd\\u5355\\u72ec\\u7acb\\u9879'};}"
                        + "if(p.indexOf('/volcengine/market/aiChat/')>=0){return js({code:200,msg:'\\u672c\\u5730\\u667a\\u80fd\\u4f53\\u4f53\\u9a8c\\u6682\\u672a\\u63a5\\u5165,\\u771f\\u5b9e\\u6267\\u884c\\u5355\\u72ec\\u7acb\\u9879',data:null});}"
                        + "if(p.indexOf('/volcengine/market/random')>=0){return js({code:200,msg:'M8_SMART_AI_XHR_STUB random',data:JSON.stringify({name:'\\u672c\\u5730\\u667a\\u80fd\\u4f53',userValue:'\\u672c\\u5730\\u667a\\u80fd\\u4f53\\u5185\\u5bb9\\u5360\\u4f4d',description:'\\u771f\\u5b9e\\u968f\\u673a\\u751f\\u6210\\u5355\\u72ec\\u7acb\\u9879'})});}"
                        + "if(p.indexOf('/volcengine/market/model/update')>=0){if(rows.length){var updated=agentFromBody();updated.id=rows[0].id;rows[0]=updated;}return js({code:200,msg:'M8_SMART_AI_XHR_STUB update',data:null});}"
                        + "if(p.indexOf('/volcengine/market/delete/')>=0){window.__m8SmartAiRows=[];return js({code:200,msg:'M8_SMART_AI_XHR_STUB delete',data:null});}"
                        + "if(p.indexOf('/volcengine/market/my')>=0){if(method==='POST'){var created=agentFromBody();window.__m8SmartAiRows=[created];return js({code:200,msg:'M8_SMART_AI_XHR_STUB create',data:created});}return js({code:200,msg:'M8_SMART_AI_XHR_STUB list',rows:rows,total:rows.length,data:rows});}"
                        + "if(p.indexOf('/volcengine/trains/tokens')>=0){return js({code:200,msg:'M8_SMART_AI_XHR_STUB tokens',data:{tokens:0}});}"
                        + "if(p.indexOf('/volcengine/trains/recharge')>=0){return js({code:200,msg:'M8_SMART_AI_XHR_STUB recharge gated',data:{tokens:0,charged:false,localOnly:true}});}"
                        + "return js({code:200,msg:'M8_SMART_AI_XHR_STUB gated',data:null,rows:[],total:0});}"
                        + "function m8C5Platform(){var p=path(),page=String(location.pathname||'');var big=page.indexOf('/es/bigData/bigDataTask')>=0&&(p.indexOf('/es/collectTask/')>=0||p.indexOf('/es/bigData/')>=0);var tg=page.indexOf('/pc/tg/index')>=0&&p.indexOf('/tg/groupTask')>=0;var geo=page.indexOf('/pc/dataCollect/googleseo')>=0&&p.indexOf('/accessFlow/order')>=0;var kefu=page.indexOf('/pc/kefu/conversation')>=0&&p.indexOf('/kefu/conversation')>=0;if(!big&&!tg&&!geo&&!kefu){return null;}if(method==='GET'){return js({code:200,msg:'C5_PLATFORM_XHR_STUB',data:[],rows:[],total:0});}return js({code:503,msg:'C5_PLATFORM_UI_ONLY',data:null,rows:[],total:0,localOnly:true});}"
                        + "function m8Kefu(){var p=path();"
                        + "if(p.indexOf('/ws/luopan/clientStatus')>=0){return js({code:200,msg:'M8_AI_KEFU_WA_STUB',data:{connected:false,online:false,loggedIn:false,loginStatus:0,status:0,clientStatus:0},rows:[],total:0});}"
                        + "if(p.indexOf('/ws/luopan/userClient')>=0){return js({code:200,msg:'M8_AI_KEFU_WA_STUB',data:[],rows:[],total:0});}"
                        + "if(p.indexOf('/ws/luopan/clientLogoutStatus')>=0){return js({code:200,msg:'M8_AI_KEFU_WA_STUB',data:{logout:true,connected:false,online:false,loginStatus:0,status:0}});}"
                        + "if(p.indexOf('/ws/luopan/contact/list')>=0||p.indexOf('/ws/luopan/recentContacts')>=0||p.indexOf('/ws/luopan/search/list')>=0||p.indexOf('/ws/luopan/list')>=0||p.indexOf('/ws/luopan/group/list')>=0){return js({code:'200',msg:'M8_AI_KEFU_WA_STUB',rows:[],total:0,data:[]});}"
                        + "if(p.indexOf('/ws/luopan/contact/message/')>=0||p.indexOf('/ws/luopan/payLog/list')>=0){return js({code:'200',msg:'M8_AI_KEFU_WA_STUB',rows:[],total:0,data:[]});}"
                        + "if(p.indexOf('/ws/luopan/client/qrcode/')>=0||p.indexOf('/ws/luopan/client/loginCode/')>=0||p.indexOf('/ws/luopan/submit2Login')>=0||p.indexOf('/ws/luopan/client/batch')>=0||p.indexOf('/ws/luopan/client/logout/')>=0||p.indexOf('/ws/luopan/proxy')>=0){return js({code:200,msg:'M8_AI_KEFU_WA_STUB not_logged_in',data:null,rows:[],total:0});}"
                        + "if(p.indexOf('/ws/luopan')>=0){return js({code:200,msg:'M8_AI_KEFU_WA_STUB',data:[],rows:[],total:0});}"
                        + "if(p.indexOf('/kefu/pageInfo/page')>=0){return js({code:200,msg:'success',data:[]});}"
                        + "if(p.indexOf('/kefu/conversation/getUnread')>=0){return js({code:200,msg:'success',data:0});}"
                        + "if(p.indexOf('/kefu/conversation/member/')>=0){return js({code:'200',msg:'success',rows:[],total:0});}"
                        + "if(p.indexOf('/kefu/kefuUser/list')>=0){return js({code:200,msg:'success',rows:[],total:0});}"
                        + "if(p.indexOf('/kefu/tag/data')>=0){return js({code:200,msg:'success',data:[]});}"
                        + "if(p.indexOf('/kefu/conversation/tenantConfig')>=0){return js({code:200,msg:'success',data:{allowDelete:1}});}"
                        + "if(p.indexOf('/kefu/conversation/message/')>=0){return js({code:200,msg:'success',rows:[],total:0,firstMessage:null});}"
                        + "if(p.indexOf('/kefu/conversation/send')>=0){return js({code:200,msg:'M8_AI_KEFU_XHR_STUB send gated',data:{code:200,message:'local ui only'}});}"
                        + "if(p.indexOf('/kefu/conversation/read/')>=0){return js({code:200,msg:'success',data:null});}"
                        + "if(p.indexOf('/kefu/conversation/')>=0){return js({code:200,msg:'success',data:{}});}"
                        + "if(p.indexOf('/kefu/conversation')>=0){return js({code:200,msg:'success',rows:[],total:0,data:[]});}"
                        + "if(p.indexOf('/kefu/accounts')>=0){return js({code:200,msg:'success',rows:[],total:0,data:[]});}"
                        + "if(p.indexOf('/kefu/userConfig')>=0){return js({code:200,msg:'success',data:{autoReplyMode:0}});}"
                        + "if(p.indexOf('/kefu/kefuUser')>=0){return js({code:200,msg:'success',rows:[],total:0,data:[]});}"
                        + "if(p.indexOf('/kefu/tag')>=0){return js({code:200,msg:'success',data:[]});}"
                        + "if(p.indexOf('/kefu/')>=0){return js({code:200,msg:'M8_AI_KEFU_XHR_STUB',rows:[],total:0,data:null});}"
                        + "if(p.indexOf('/helplook/')>=0){return js({code:200,msg:'M8_AI_KEFU_XHR_STUB',exist:0,data:null,rows:[],total:0});}"
                        + "if(p.indexOf('/world/tg/v2/platformToken')>=0){return js({code:200,msg:'success',data:{token:'',platformToken:''}});}"
                        + "if(p.indexOf('/upmee/api/getConversationList')>=0){return js({code:200,msg:'success',data:{data:{conversations:[],has_more:false,cursor:null}},total:0});}"
                        + "if(p.indexOf('/upmee/api/getMessageList')>=0){return js({code:200,msg:'success',data:{data:{private_messages:[],has_more:false,cursor:null}},total:0});}"
                        + "if(p.indexOf('/upmee/api/instandMessages')>=0){return js({code:200,msg:'success',data:{media_badge:{}}});}"
                        + "if(p.indexOf('/upmee/api/sendMessage')>=0){return js({code:200,msg:'M8_AI_KEFU_XHR_STUB send gated',data:{code:200,message:'local ui only'}});}"
                        + "if(p.indexOf('/upmee/')>=0){return js({code:200,msg:'M8_AI_KEFU_XHR_STUB',data:[],rows:[],total:0});}"
                        + "if(p.indexOf('/system/userconfig/getOneByUserNameAndCode')>=0){return js({code:200,msg:'success',data:{configValue:'0'}});}"
                        + "return null;}"
                        + "function m8Claw(){var p=path();var claw=p.indexOf('/wsClaw/')>=0;var license=p.indexOf('/system/longxia_license')>=0;"
                        + "if(!claw&&!license){return null;}"
                        + "if(p.indexOf('/dataAllAccount')>=0){return js({code:200,msg:'success',data:[]});}"
                        + "if(p.indexOf('/checkAccountExist')>=0){return js({code:200,msg:'success',exist:false,data:{exist:false}});}"
                        + "if(p.substring(p.length-5)==='/list'){return js({code:200,msg:'success',rows:[],total:0});}"
                        + "if(method==='GET'){return js({code:200,msg:'success',data:{}});}"
                        + "var result={};try{result=(typeof body==='string'&&body)?JSON.parse(body):(body||{});}catch(e){result={};}"
                        + "return js({code:200,msg:'success',data:result});}"
                        + "function m8SuperEnvironment(){var p=path();var controls=p.indexOf('/sender/senderGlobalControls')>=0;var buyweight=p.indexOf('/sender/senderGlobalControlsBuyweight')>=0;var logs=p.indexOf('/ws/superSenderGlobalControlsSendLog')>=0;var remote=p.indexOf('/superchannel/machiningcenter')>=0;"
                        + "if(!controls&&!buyweight&&!logs&&!remote){return null;}"
                        + "if(method==='GET'&&p.indexOf('/sender/senderGlobalControls/mylist')>=0){return js({code:200,msg:'M8B_SUPER_ENV_XHR_STUB',rows:[],total:0,data:[]});}"
                        + "if(method==='GET'){return js({code:200,msg:'M8B_SUPER_ENV_XHR_STUB',rows:[],total:0,data:[]});}"
                        + "return js({code:503,msg:'M8B_SUPER_ENV_UI_ONLY',rows:[],total:0,data:null,localOnly:true});}"
                        + "var c5PlatformStub=m8C5Platform();if(c5PlatformStub!==null){console.log('C5_PLATFORM_XHR_STUB url='+u+' method='+method);return c5PlatformStub;}"
                        + "var smartAiStub=m8SmartAi();if(smartAiStub!==null){console.log('M8_SMART_AI_XHR_STUB url='+u);return smartAiStub;}"
                        + "var kefuStub=m8Kefu();if(kefuStub!==null){console.log((kefuStub.indexOf('M8_AI_KEFU_WA_STUB')>=0?'M8_AI_KEFU_WA_STUB':'M8_AI_KEFU_XHR_STUB')+' url='+u);return kefuStub;}"
                        + "var clawStub=m8Claw();if(clawStub!==null){console.log('M8B_WSCLAW_XHR_STUB url='+u+' method='+method);return clawStub;}"
                        + "var superEnvironmentStub=m8SuperEnvironment();if(superEnvironmentStub!==null){console.log('M8B_SUPER_ENV_XHR_STUB url='+u+' method='+method);return superEnvironmentStub;}"
                        + "if(u.indexOf('/system/user/profile')>=0){return js({code:200,msg:'success',data:{userId:1,userName:'local@test.com',nickName:'HuoChaiAI Local User',nickname:'HuoChaiAI Local User',avatar:'',phonenumber:'',invitationCode:'LOCAL-OFFLINE'}});}"
                        + "if(u.indexOf('/ads/inivitationCode/balance')>=0){return js({code:200,msg:'success',data:{invitationCode:'LOCAL-OFFLINE',balance:0}});}"
                        + "if(u.indexOf('/prod-api/getInfo')>=0){return __m5GetInfoBody;}"
                        + "if(u.indexOf('/prod-api/getRouters')>=0){return __m5RoutersBody;}"
                        + "if(u.indexOf('/prod-api/mnq/mnqAuthAccounts/mylist')>=0){return __m5AicloudMylistBody;}"
                        + "if(u.indexOf('/prod-api/system/dict/data/type/yes_no_1_0')>=0){return __m5YesNoDictBody;}"
                        + "if(u.indexOf('/rpa/cloudHost/lists')>=0){return __m5CloudHostListBody;}"
                        + "if(u.indexOf('/cloud/spider/code/')>=0){try{var cc=sc('/cloud/spider/code/','whatsapp_users_lists');if(window.mijava&&window.mijava.m5GetLocalSpiderConfig){return JSON.stringify({code:200,msg:'success',data:cfg(JSON.parse(window.mijava.m5GetLocalSpiderConfig(cc)))});}}catch(e){console.error('M5D11_LOCAL_SPIDER_CONFIG_HTTP_FAILED '+e);}return __m5SpiderConfigBody;}"
                        + "if(u.indexOf('/dataCollect/platform/list')>=0){if(u.indexOf('type=area_code')>=0){return __m5AreaOptionsBody;}if(u.indexOf('type=platform')>=0){return __m5PlatformOptionsBody;}return __m5KeywordsOptionsBody;}"
                        + "if(u.indexOf('/cloud/spider/data/')>=0){try{var dc=sc('/cloud/spider/data/',qp('spiderCode',qp('modal','whatsapp_users_lists')));if(window.mijava&&window.mijava.m5ListLocalSpiderData){return window.mijava.m5ListLocalSpiderData('whatsapp',dc,parseInt(qp('pageNum','1'),10)||1,parseInt(qp('pageSize','10'),10)||10);}}catch(e){console.error('M5D8_LOCAL_SPIDER_DATA_HTTP_FAILED '+e);}return '{\\\"code\\\":200,\\\"msg\\\":\\\"success\\\",\\\"rows\\\":[],\\\"total\\\":0}';}"
                        + "if(method==='POST'&&u.indexOf('/cloud/task')>=0){try{var p=typeof body==='string'&&body?JSON.parse(body):(body||{});var m=p.moduleCode||'whatsapp';var s=p.spiderCode||'whatsapp_users_lists';var sp=p.spiderParams||{};var tc=p.taskConfig||{};if(window.mijava&&window.mijava.m5SubmitLocalCollectTask){return window.mijava.m5SubmitLocalCollectTask(m,s,JSON.stringify(sp),JSON.stringify(tc));}}catch(e){console.error('M5C_COLLECT_LOCAL_TASK_POST_FAILED '+e);}return '{\\\"code\\\":500,\\\"submitted\\\":false,\\\"msg\\\":\\\"local task submit failed\\\"}';}"
                        + "return null;}"
                        + "function __m5PatchXhrValue(x,k,v){try{Object.defineProperty(x,k,{value:v,configurable:true});}catch(e){try{x[k]=v;}catch(y){}}}"
                        + "function __m5PatchXhrHeaders(x){try{x.getAllResponseHeaders=function(){return 'content-type: application/json;charset=UTF-8\\r\\n';};x.getResponseHeader=function(n){return String(n||'').toLowerCase()==='content-type'?'application/json;charset=UTF-8':null;};}catch(e){}}"
                        + "if(window.fetch&&!window.fetch.__m5BootstrapWrapped){"
                        + "var __m5OrigFetch=window.fetch;"
                        + "var __m5Fetch=function(input,init){var u=(typeof input==='string')?input:(input&&input.url);init=init||{};var b=__m5BootstrapBody(u,init.body,init.method||(input&&input.method));"
                        + "if(b!==null){console.log('M5_V26_WEB_BOOTSTRAP_FETCH url='+u);return Promise.resolve(new Response(b,{status:200,statusText:'OK',headers:{'Content-Type':'application/json;charset=UTF-8'}}));}"
                        + "return __m5OrigFetch.apply(this,arguments);};"
                        + "__m5Fetch.__m5BootstrapWrapped=true;window.fetch=__m5Fetch;}"
                        + "if(window.XMLHttpRequest&&window.XMLHttpRequest.prototype&&!window.XMLHttpRequest.prototype.__m5BootstrapWrapped){"
                        + "var __m5XhrOpen=window.XMLHttpRequest.prototype.open;"
                        + "var __m5XhrSend=window.XMLHttpRequest.prototype.send;"
                        + "window.XMLHttpRequest.prototype.open=function(method,url){this.__m5BootstrapMethod=method;this.__m5BootstrapUrl=url;return __m5XhrOpen.apply(this,arguments);};"
                        + "window.XMLHttpRequest.prototype.send=function(body){var b=__m5BootstrapBody(this.__m5BootstrapUrl,body,this.__m5BootstrapMethod);"
                        + "if(b!==null){var x=this;console.log('M5_V26_WEB_BOOTSTRAP_XHR url='+this.__m5BootstrapUrl);"
                        + "__m5PatchXhrValue(x,'readyState',4);__m5PatchXhrValue(x,'status',200);__m5PatchXhrValue(x,'statusText','OK');"
                        + "__m5PatchXhrValue(x,'responseText',b);__m5PatchXhrValue(x,'response',b);__m5PatchXhrValue(x,'responseURL',String(x.__m5BootstrapUrl||''));__m5PatchXhrHeaders(x);"
                        + "setTimeout(function(){try{if(x.onreadystatechange){x.onreadystatechange();}if(x.dispatchEvent){x.dispatchEvent(new Event('readystatechange'));}if(x.onload){x.onload();}if(x.dispatchEvent){x.dispatchEvent(new Event('load'));}if(x.onloadend){x.onloadend();}if(x.dispatchEvent){x.dispatchEvent(new Event('loadend'));}}catch(e){console.error('M5_V26_WEB_BOOTSTRAP_XHR_FAIL '+e);}},0);return;}"
                        + "return __m5XhrSend.apply(this,arguments);};"
                        + "window.XMLHttpRequest.prototype.__m5BootstrapWrapped=true;}"
                        + "var __m5OrigJsonParse=JSON.parse;"
                        + "JSON.parse=function(v){"
                        + "if(typeof v==='undefined'){try{console.error('M5_V23_JSON_PARSE_UNDEFINED stack='+(new Error()).stack);}catch(e){}}"
                        + "return __m5OrigJsonParse.apply(this,arguments);"
                        + "};"
                        + "window.addEventListener('unhandledrejection',function(e){try{var r=e&&e.reason;console.error('M5_V23_UNHANDLED_REJECTION reason='+(r&&(r.stack||r.message)||r));}catch(x){}});"
                        + "window.addEventListener('error',function(e){try{console.error('M5_V23_WINDOW_ERROR msg='+e.message+' source='+e.filename+':'+e.lineno+':'+e.colno+' error='+(e.error&&(e.error.stack||e.error.message)||e.error));}catch(x){}});"
                        + "console.log('M5_V23_JSON_DIAG_INSTALLED');"
                        + "})();");
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/frame/Frame",
                "executeJavaScript",
                "(Ljava/lang/String;)Ljava/lang/Object;",
                true);
        mv.visitInsn(Opcodes.POP);
        mv.visitLabel(end);
        mv.visitJumpInsn(Opcodes.GOTO, after);
        mv.visitLabel(handler);
        mv.visitFrame(
                Opcodes.F_FULL,
                2,
                new Object[] {
                    "com/sbf/main/jxbrowser/M5InjectJsCallback",
                    "com/teamdev/jxbrowser/browser/callback/InjectJsCallback$Params"
                },
                1,
                new Object[] {"java/lang/Throwable"});
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        emitStringBuilderPrint(
                mv,
                "M5_V23_JS_HOOK_FAILED ",
                Opcodes.ALOAD,
                2,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/Object;)Ljava/lang/StringBuilder;");
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/Throwable",
                "printStackTrace",
                "(Ljava/io/PrintStream;)V",
                false);
        mv.visitLabel(after);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/teamdev/jxbrowser/browser/callback/InjectJsCallback$Response",
                "proceed",
                "()Lcom/teamdev/jxbrowser/browser/callback/InjectJsCallback$Response;",
                true);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        MethodVisitor bridge =
                cw.visitMethod(
                        Opcodes.ACC_PUBLIC | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC,
                        "on",
                        "(Ljava/lang/Object;)Ljava/lang/Object;",
                        null,
                        null);
        bridge.visitCode();
        bridge.visitVarInsn(Opcodes.ALOAD, 0);
        bridge.visitVarInsn(Opcodes.ALOAD, 1);
        bridge.visitTypeInsn(
                Opcodes.CHECKCAST,
                "com/teamdev/jxbrowser/browser/callback/InjectJsCallback$Params");
        bridge.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/sbf/main/jxbrowser/M5InjectJsCallback",
                "on",
                "(Lcom/teamdev/jxbrowser/browser/callback/InjectJsCallback$Params;)"
                        + "Lcom/teamdev/jxbrowser/browser/callback/InjectJsCallback$Response;",
                false);
        bridge.visitInsn(Opcodes.ARETURN);
        bridge.visitMaxs(0, 0);
        bridge.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static void emitM5DataCollectMiJavaBridge(MethodVisitor mv) {
        org.objectweb.asm.Label bridgeStart = new org.objectweb.asm.Label();
        org.objectweb.asm.Label bridgeEnd = new org.objectweb.asm.Label();
        org.objectweb.asm.Label bridgeHandler = new org.objectweb.asm.Label();
        org.objectweb.asm.Label afterBridge = new org.objectweb.asm.Label();
        org.objectweb.asm.Label notJsObject = new org.objectweb.asm.Label();
        org.objectweb.asm.Label alreadyInjected = new org.objectweb.asm.Label();
        mv.visitTryCatchBlock(bridgeStart, bridgeEnd, bridgeHandler, "java/lang/Throwable");
        mv.visitLabel(bridgeStart);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/browser/callback/InjectJsCallback$Params",
                "frame",
                "()Lcom/teamdev/jxbrowser/frame/Frame;",
                true);
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitLdcInsn("window");
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/frame/Frame",
                "executeJavaScript",
                "(Ljava/lang/String;)Ljava/lang/Object;",
                true);
        mv.visitVarInsn(Opcodes.ASTORE, 3);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitTypeInsn(Opcodes.INSTANCEOF, "com/teamdev/jxbrowser/js/JsObject");
        mv.visitJumpInsn(Opcodes.IFEQ, notJsObject);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitTypeInsn(Opcodes.CHECKCAST, "com/teamdev/jxbrowser/js/JsObject");
        mv.visitVarInsn(Opcodes.ASTORE, 3);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitLdcInsn("mijava");
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/js/JsObject",
                "hasProperty",
                "(Ljava/lang/String;)Z",
                true);
        mv.visitJumpInsn(Opcodes.IFNE, alreadyInjected);
        mv.visitTypeInsn(Opcodes.NEW, "com/sbf/main/jxbrowser/MiJava");
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/frame/Frame",
                "browser",
                "()Lcom/teamdev/jxbrowser/browser/Browser;",
                true);
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "com/sbf/main/jxbrowser/MiJava",
                "<init>",
                "(Lcom/teamdev/jxbrowser/browser/Browser;Lcom/sbf/main/jxbrowser/g$b;Ljava/lang/String;)V",
                false);
        mv.visitVarInsn(Opcodes.ASTORE, 4);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitLdcInsn("mijava");
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/js/JsObject",
                "putProperty",
                "(Ljava/lang/String;Ljava/lang/Object;)Z",
                true);
        mv.visitInsn(Opcodes.POP);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitLdcInsn("java");
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/js/JsObject",
                "putProperty",
                "(Ljava/lang/String;Ljava/lang/Object;)Z",
                true);
        mv.visitInsn(Opcodes.POP);
        emitPrint(mv, "M5A_V48_MIJAVA_BRIDGE_INJECTED");
        mv.visitLabel(alreadyInjected);
        mv.visitLabel(notJsObject);
        mv.visitLabel(bridgeEnd);
        mv.visitJumpInsn(Opcodes.GOTO, afterBridge);
        mv.visitLabel(bridgeHandler);
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        emitStringBuilderPrint(
                mv,
                "M5A_V48_MIJAVA_BRIDGE_FAILED ",
                Opcodes.ALOAD,
                2,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/Object;)Ljava/lang/StringBuilder;");
        mv.visitLabel(afterBridge);
    }

    private static byte[] patchGoogleCRHelper(byte[] original, PatchResult result) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(
                Opcodes.V1_8,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                "com/sbf/main/ext/gg/GoogleCRHelper",
                null,
                "java/lang/Object",
                null);
        writeDefaultConstructor(cw);
        MethodVisitor mv =
                cw.visitMethod(
                        Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                        "a",
                        "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                        null,
                        null);
        mv.visitCode();
        emitPrint(mv, "M5D_YESCAPTCHA_GOOGLE_CR_TASK");
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/ext/gg/M5YesCaptchaBridge",
                "solve",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        MethodVisitor main =
                cw.visitMethod(
                        Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                        "main",
                        "([Ljava/lang/String;)V",
                        null,
                        null);
        main.visitCode();
        main.visitInsn(Opcodes.RETURN);
        main.visitMaxs(0, 0);
        main.visitEnd();
        cw.visitEnd();
        result.patchedGoogleCRHelper = true;
        return cw.toByteArray();
    }

    private static byte[] patchSpiderCallback(byte[] original, final PatchResult result) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = computeFramesWriter(reader);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                if ("postData".equals(name) && "(Ljava/lang/String;)V".equals(descriptor)) {
                    MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                    result.patchedSpiderCallbackPostData = true;
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override
                        public void visitCode() {
                            super.visitCode();
                            emitPrint(this, "M5D_POSTDATA_LOCAL_CALLBACK");
                            visitVarInsn(Opcodes.ALOAD, 0);
                            visitFieldInsn(
                                    Opcodes.GETFIELD,
                                    "com/sbf/main/cloud/spider/SpiderCallback",
                                    "spider",
                                    "Lcom/sbf/main/cloud/spider/b;");
                            visitVarInsn(Opcodes.ALOAD, 1);
                            visitMethodInsn(
                                    Opcodes.INVOKESTATIC,
                                    "com/sbf/main/jxbrowser/M5LocalSpiderBridge",
                                    "postCollectedData",
                                    "(Ljava/lang/Object;Ljava/lang/String;)Z",
                                    false);
                            visitInsn(Opcodes.POP);
                        }
                    };
                }
                if ("endTask".equals(name) && "()V".equals(descriptor)) {
                    MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                    result.patchedSpiderCallbackEndTask = true;
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override
                        public void visitCode() {
                            super.visitCode();
                            emitPrint(this, "M5D_ENDTASK_LOCAL_CALLBACK");
                            visitVarInsn(Opcodes.ALOAD, 0);
                            visitFieldInsn(
                                    Opcodes.GETFIELD,
                                    "com/sbf/main/cloud/spider/SpiderCallback",
                                    "spider",
                                    "Lcom/sbf/main/cloud/spider/b;");
                            visitMethodInsn(
                                    Opcodes.INVOKESTATIC,
                                    "com/sbf/main/jxbrowser/M5LocalSpiderBridge",
                                    "endCollectedTask",
                                    "(Ljava/lang/Object;)V",
                                    false);
                        }
                    };
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        };
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private static void writeSpiderCallbackPostDataMethod(MethodVisitor mv) {
        mv.visitCode();
        emitPrint(mv, "M5D_POSTDATA_LOCAL_CALLBACK");
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(
                Opcodes.GETFIELD,
                "com/sbf/main/cloud/spider/SpiderCallback",
                "spider",
                "Lcom/sbf/main/cloud/spider/b;");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/jxbrowser/M5LocalSpiderBridge",
                "postCollectedData",
                "(Ljava/lang/Object;Ljava/lang/String;)Z",
                false);
        mv.visitInsn(Opcodes.POP);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void writeSpiderCallbackEndTaskMethod(MethodVisitor mv) {
        mv.visitCode();
        emitPrint(mv, "M5D_ENDTASK_LOCAL_CALLBACK");
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(
                Opcodes.GETFIELD,
                "com/sbf/main/cloud/spider/SpiderCallback",
                "spider",
                "Lcom/sbf/main/cloud/spider/b;");
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/jxbrowser/M5LocalSpiderBridge",
                "endCollectedTask",
                "(Ljava/lang/Object;)V",
                false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static byte[] generateM5RequestObserver() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(
                Opcodes.V1_8,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                "com/sbf/main/jxbrowser/M5RequestObserver",
                null,
                "java/lang/Object",
                new String[] {"com/teamdev/jxbrowser/event/Observer"});
        writeDefaultConstructor(cw);
        MethodVisitor mv =
                cw.visitMethod(
                        Opcodes.ACC_PUBLIC,
                        "on",
                        "(Lcom/teamdev/jxbrowser/net/event/RequestCompleted;)V",
                        null,
                        null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/net/event/RequestCompleted",
                "urlRequest",
                "()Lcom/teamdev/jxbrowser/net/UrlRequest;",
                true);
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("M5_V20_WEB_REQUEST code=");
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/StringBuilder",
                "<init>",
                "(Ljava/lang/String;)V",
                false);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/net/event/RequestCompleted",
                "responseCode",
                "()I",
                true);
        appendInt(mv);
        appendLiteral(mv, " status=");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/net/event/RequestCompleted",
                "status",
                "()Lcom/teamdev/jxbrowser/net/UrlRequestStatus;",
                true);
        appendObject(mv);
        appendLiteral(mv, " error=");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/net/event/RequestCompleted",
                "errorCode",
                "()Lcom/teamdev/jxbrowser/net/NetError;",
                true);
        appendObject(mv);
        appendLiteral(mv, " type=");
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/net/UrlRequest",
                "resourceType",
                "()Lcom/teamdev/jxbrowser/net/ResourceType;",
                true);
        appendObject(mv);
        appendLiteral(mv, " method=");
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/net/UrlRequest",
                "method",
                "()Ljava/lang/String;",
                true);
        appendString(mv);
        appendLiteral(mv, " url=");
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/net/UrlRequest",
                "url",
                "()Ljava/lang/String;",
                true);
        appendString(mv);
        printlnBuilder(mv);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        writeObserverBridge(
                cw,
                "com/sbf/main/jxbrowser/M5RequestObserver",
                "com/teamdev/jxbrowser/net/event/RequestCompleted",
                "(Lcom/teamdev/jxbrowser/net/event/RequestCompleted;)V");
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static void writeDefaultConstructor(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void writeObserverBridge(
            ClassWriter cw, String ownerInternalName, String eventInternalName, String typedDescriptor) {
        MethodVisitor mv =
                cw.visitMethod(
                        Opcodes.ACC_PUBLIC | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC,
                        "on",
                        "(Lcom/teamdev/jxbrowser/event/Event;)V",
                        null,
                        null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitTypeInsn(Opcodes.CHECKCAST, eventInternalName);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                ownerInternalName,
                "on",
                typedDescriptor,
                false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void appendInt(MethodVisitor mv) {
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false);
    }

    private static void appendString(MethodVisitor mv) {
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                false);
    }

    private static void appendObject(MethodVisitor mv) {
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/Object;)Ljava/lang/StringBuilder;",
                false);
    }

    private static void printlnBuilder(MethodVisitor mv) {
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "toString",
                "()Ljava/lang/String;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/io/PrintStream",
                "println",
                "(Ljava/lang/String;)V",
                false);
    }

    private static JarEntry copyEntryMetadata(JarEntry entry) {
        JarEntry copy = new JarEntry(entry.getName());
        copy.setTime(entry.getTime());
        if (entry.getComment() != null) {
            copy.setComment(entry.getComment());
        }
        return copy;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static String jsSingleQuoted(String value) {
        return "'"
                + value.replace("\\", "\\\\")
                        .replace("'", "\\'")
                        .replace("\r", "\\r")
                        .replace("\n", "\\n")
                + "'";
    }

    private static byte[] patchSbfApi(
            byte[] original,
            PatchResult result,
            boolean realProductMenuLogging,
            String productModuleJson) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer =
                new ClassWriter(reader, realProductMenuLogging ? ClassWriter.COMPUTE_MAXS : 0);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                if ("h".equals(name) && "(Ljava/lang/String;)Lorg/json/JSONObject;".equals(descriptor)) {
                    result.patchedGetInfo = true;
                    return writeJsonReturn(access, name, descriptor, signature, exceptions, GET_INFO_JSON, 1);
                }
                if ("k".equals(name)
                        && "(Ljava/lang/String;Ljava/lang/String;)Lorg/json/JSONObject;".equals(descriptor)) {
                    result.patchedLogin = true;
                    return writeRuntimeLoginJsonReturn(
                            access, name, descriptor, signature, exceptions, 2);
                }
                if ("C".equals(name) && "()Lorg/json/JSONObject;".equals(descriptor)) {
                    result.patchedProductModules = true;
                    if (realProductMenuLogging) {
                        return wrapJsonObjectReturnWithEvidenceLog(
                                access,
                                name,
                                descriptor,
                                signature,
                                exceptions,
                                "M4_EVIDENCE_PRODUCT_MODULE_REAL_JSON=");
                    }
                    return writeJsonReturn(
                            access,
                            name,
                            descriptor,
                            signature,
                            exceptions,
                            productModuleJson,
                            0);
                }
                if ("k".equals(name) && "()Lorg/json/JSONObject;".equals(descriptor)) {
                    result.patchedPcMenus = true;
                    if (realProductMenuLogging) {
                        return wrapPcMenusRawAndReturnWithEvidenceLog(
                                access,
                                name,
                                descriptor,
                                signature,
                                exceptions,
                                "M4_EVIDENCE_PC_MENUS_REAL_JSON=");
                    }
                    return writeJsonReturn(
                            access,
                            name,
                            descriptor,
                            signature,
                            exceptions,
                            PC_MENUS_JSON,
                            0,
                            "M4_DIAG_MENU_K_CALLED resp=" + PC_MENUS_JSON);
                }
                if ("M".equals(name) && "(Ljava/lang/String;)Lorg/json/JSONArray;".equals(descriptor)) {
                    result.patchedSpiderModules = true;
                    return writeJsonReturn(
                            access,
                            name,
                            descriptor,
                            signature,
                            exceptions,
                            "org/json/JSONArray",
                            SPIDER_MODULES_JSON,
                            1);
                }
                if ("a".equals(name) && "(Ljava/lang/String;I)Lorg/json/JSONArray;".equals(descriptor)) {
                    result.patchedLocalSpiderGetNewTask = true;
                    return writeLocalSpiderGetNewTaskReturn(access, name, descriptor, signature, exceptions);
                }
                if ("L".equals(name) && "(Ljava/lang/String;)V".equals(descriptor)) {
                    result.patchedLocalSpiderCancelAllRun = true;
                    return writeLocalSpiderCancelAllRunReturn(access, name, descriptor, signature, exceptions);
                }
                if ("c".equals(name) && "(Ljava/lang/Long;)Lorg/json/JSONObject;".equals(descriptor)) {
                    result.patchedLocalSpiderTaskGet = true;
                    return writeLocalSpiderTaskGetReturn(access, name, descriptor, signature, exceptions);
                }
                if ("a".equals(name)
                        && "(Ljava/lang/Long;ILjava/lang/String;Ljava/lang/Long;)V"
                                .equals(descriptor)) {
                    result.patchedLocalSpiderTaskStatus = true;
                    return writeLocalSpiderTaskStatusReturn(access, name, descriptor, signature, exceptions);
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }

            private MethodVisitor writeLocalSpiderGetNewTaskReturn(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                mv.visitCode();
                emitPrint(mv, "M5C_QUEUE_SBFAPI_GET_NEW_TASK");
                mv.visitTypeInsn(Opcodes.NEW, "org/json/JSONArray");
                mv.visitInsn(Opcodes.DUP);
                mv.visitFieldInsn(
                        Opcodes.GETSTATIC, "com/sbf/main/StartApp", "a", "Ljava/lang/String;");
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitVarInsn(Opcodes.ILOAD, 1);
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "com/sbf/main/jxbrowser/M5LocalSpiderBridge",
                        "getNewTask",
                        "(Ljava/lang/String;Ljava/lang/String;I)Ljava/lang/String;",
                        false);
                mv.visitMethodInsn(
                        Opcodes.INVOKESPECIAL,
                        "org/json/JSONArray",
                        "<init>",
                        "(Ljava/lang/String;)V",
                        false);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(5, 2);
                mv.visitEnd();
                return null;
            }

            private MethodVisitor writeLocalSpiderCancelAllRunReturn(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                mv.visitCode();
                emitPrint(mv, "M5C_QUEUE_SBFAPI_CANCEL_ALL_RUN");
                mv.visitFieldInsn(
                        Opcodes.GETSTATIC, "com/sbf/main/StartApp", "a", "Ljava/lang/String;");
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "com/sbf/main/jxbrowser/M5LocalSpiderBridge",
                        "cancelAllRun",
                        "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                        false);
                mv.visitInsn(Opcodes.POP);
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(2, 1);
                mv.visitEnd();
                return null;
            }

            private MethodVisitor writeLocalSpiderTaskGetReturn(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                mv.visitCode();
                emitPrint(mv, "M5C_COLLECT_SBFAPI_GET_LOCAL_TASK");
                mv.visitTypeInsn(Opcodes.NEW, "org/json/JSONObject");
                mv.visitInsn(Opcodes.DUP);
                mv.visitFieldInsn(
                        Opcodes.GETSTATIC, "com/sbf/main/StartApp", "a", "Ljava/lang/String;");
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitMethodInsn(
                        Opcodes.INVOKEVIRTUAL,
                        "java/lang/Long",
                        "longValue",
                        "()J",
                        false);
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "com/sbf/main/jxbrowser/M5LocalSpiderBridge",
                        "getTask",
                        "(Ljava/lang/String;J)Ljava/lang/String;",
                        false);
                mv.visitMethodInsn(
                        Opcodes.INVOKESPECIAL,
                        "org/json/JSONObject",
                        "<init>",
                        "(Ljava/lang/String;)V",
                        false);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(5, 1);
                mv.visitEnd();
                return null;
            }

            private MethodVisitor writeLocalSpiderTaskStatusReturn(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                mv.visitCode();
                emitPrint(mv, "M5C_COLLECT_SBFAPI_STATUS_LOCAL");
                mv.visitFieldInsn(
                        Opcodes.GETSTATIC, "com/sbf/main/StartApp", "a", "Ljava/lang/String;");
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitMethodInsn(
                        Opcodes.INVOKEVIRTUAL,
                        "java/lang/Long",
                        "longValue",
                        "()J",
                        false);
                mv.visitVarInsn(Opcodes.ILOAD, 1);
                mv.visitVarInsn(Opcodes.ALOAD, 2);
                mv.visitVarInsn(Opcodes.ALOAD, 3);
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "com/sbf/main/jxbrowser/M5LocalSpiderBridge",
                        "updateTaskStatus",
                        "(Ljava/lang/String;JILjava/lang/String;Ljava/lang/Long;)V",
                        false);
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(6, 4);
                mv.visitEnd();
                return null;
            }

            private MethodVisitor wrapPcMenusRawAndReturnWithEvidenceLog(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions,
                    String returnLogPrefix) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    private boolean afterPcMenusRequestEncrypt;
                    private boolean afterPcMenusResultOptString;
                    private int rawBodyStringStores;

                    @Override
                    public void visitInvokeDynamicInsn(
                            String dynamicName,
                            String dynamicDescriptor,
                            Handle bootstrapMethodHandle,
                            Object... bootstrapMethodArguments) {
                        super.visitInvokeDynamicInsn(
                                dynamicName,
                                dynamicDescriptor,
                                bootstrapMethodHandle,
                                bootstrapMethodArguments);
                        if ("RSvgDpUx".equals(dynamicName)
                                && "(Ljava/lang/Object;)Ljava/lang/String;"
                                        .equals(dynamicDescriptor)) {
                            afterPcMenusRequestEncrypt = true;
                        }
                    }

                    @Override
                    public void visitMethodInsn(
                            int opcode,
                            String owner,
                            String methodName,
                            String methodDescriptor,
                            boolean isInterface) {
                        super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                        if (opcode == Opcodes.INVOKEVIRTUAL
                                && "org/json/JSONObject".equals(owner)
                                && "optString".equals(methodName)
                                && "(Ljava/lang/String;)Ljava/lang/String;".equals(methodDescriptor)) {
                            afterPcMenusResultOptString = true;
                            rawBodyStringStores = 0;
                        }
                    }

                    @Override
                    public void visitVarInsn(int opcode, int varIndex) {
                        super.visitVarInsn(opcode, varIndex);
                        if (afterPcMenusRequestEncrypt && opcode == Opcodes.ASTORE && varIndex == 0) {
                            emitStringLocalLog(this, "M4_EVIDENCE_PC_MENUS_REQUEST_URL=", 4);
                            emitStringLocalLog(this, "M4_EVIDENCE_PC_MENUS_REQUEST_JSON=", 5);
                            emitStringLocalLog(this, "M4_EVIDENCE_PC_MENUS_REQUEST_BODY=", 0);
                            emitStaticFieldLog(
                                    this,
                                    "M4_EVIDENCE_PC_MENUS_STATIC_A=",
                                    "com/sbf/util/http/SBFApi",
                                    "a",
                                    "Ljava/lang/String;");
                            emitStaticFieldLog(
                                    this,
                                    "M4_EVIDENCE_PC_MENUS_STATIC_K=",
                                    "com/sbf/util/http/SBFApi",
                                    "k",
                                    "Ljava/lang/String;");
                            emitStaticFieldLog(
                                    this,
                                    "M4_EVIDENCE_PC_MENUS_STATIC_L=",
                                    "com/sbf/util/http/SBFApi",
                                    "l",
                                    "Ljava/lang/String;");
                            emitStaticFieldLog(
                                    this,
                                    "M4_EVIDENCE_PC_MENUS_HEADER_E=",
                                    "com/sbf/main/JSBFMain",
                                    "E",
                                    "Ljava/lang/String;");
                            afterPcMenusRequestEncrypt = false;
                        }
                        if (afterPcMenusResultOptString && opcode == Opcodes.ASTORE && varIndex == 0) {
                            rawBodyStringStores++;
                            if (rawBodyStringStores == 3) {
                                emitStringLocalLog(this, "M4_EVIDENCE_PC_MENUS_RAW_BODY=", 0);
                                afterPcMenusResultOptString = false;
                            }
                        }
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        if (opcode == Opcodes.ARETURN) {
                            emitEvidenceJsonReturnLog(this, returnLogPrefix);
                        }
                        super.visitInsn(opcode);
                    }
                };
            }

            private MethodVisitor wrapJsonObjectReturnWithEvidenceLog(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions,
                    String logPrefix) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitInsn(int opcode) {
                        if (opcode == Opcodes.ARETURN) {
                            emitEvidenceJsonReturnLog(this, logPrefix);
                        }
                        super.visitInsn(opcode);
                    }
                };
            }

            private MethodVisitor writeJsonReturn(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions,
                    String json,
                    int maxLocals) {
                return writeJsonReturn(access, name, descriptor, signature, exceptions, json, maxLocals, null);
            }

            private MethodVisitor writeJsonReturn(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions,
                    String json,
                    int maxLocals,
                    String logLine) {
                return writeJsonReturn(
                        access,
                        name,
                        descriptor,
                        signature,
                        exceptions,
                        "org/json/JSONObject",
                        json,
                        maxLocals,
                        logLine);
            }

            private MethodVisitor writeJsonReturn(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions,
                    String jsonClass,
                    String json,
                    int maxLocals) {
                return writeJsonReturn(access, name, descriptor, signature, exceptions, jsonClass, json, maxLocals, null);
            }

            private MethodVisitor writeJsonReturn(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions,
                    String jsonClass,
                    String json,
                    int maxLocals,
                    String logLine) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                mv.visitCode();
                if (logLine != null) {
                    emitPrint(mv, logLine);
                    if (logLine.startsWith("M4_DIAG_MENU_K_CALLED")) {
                        emitCallerStack(mv);
                    }
                }
                mv.visitTypeInsn(Opcodes.NEW, jsonClass);
                mv.visitInsn(Opcodes.DUP);
                emitJsonString(mv, json);
                mv.visitMethodInsn(
                        Opcodes.INVOKESPECIAL,
                        jsonClass,
                        "<init>",
                        "(Ljava/lang/String;)V",
                        false);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(5, maxLocals);
                mv.visitEnd();
                return null;
            }

            private void emitJsonString(MethodVisitor mv, String json) {
                final int maxChunkChars = 16000;
                if (json.length() <= maxChunkChars) {
                    mv.visitLdcInsn(json);
                    return;
                }
                mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
                mv.visitInsn(Opcodes.DUP);
                mv.visitMethodInsn(
                        Opcodes.INVOKESPECIAL,
                        "java/lang/StringBuilder",
                        "<init>",
                        "()V",
                        false);
                for (int offset = 0; offset < json.length(); offset += maxChunkChars) {
                    int end = Math.min(json.length(), offset + maxChunkChars);
                    mv.visitLdcInsn(json.substring(offset, end));
                    mv.visitMethodInsn(
                            Opcodes.INVOKEVIRTUAL,
                            "java/lang/StringBuilder",
                            "append",
                            "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                            false);
                }
                mv.visitMethodInsn(
                        Opcodes.INVOKEVIRTUAL,
                        "java/lang/StringBuilder",
                        "toString",
                        "()Ljava/lang/String;",
                        false);
            }

            private MethodVisitor writeRuntimeLoginJsonReturn(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions,
                    int maxLocals) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                mv.visitCode();
                mv.visitTypeInsn(Opcodes.NEW, "org/json/JSONObject");
                mv.visitInsn(Opcodes.DUP);
                emitRuntimeLoginJson(mv);
                mv.visitMethodInsn(
                        Opcodes.INVOKESPECIAL,
                        "org/json/JSONObject",
                        "<init>",
                        "(Ljava/lang/String;)V",
                        false);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(5, maxLocals);
                mv.visitEnd();
                return null;
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static byte[] patchStartApp(byte[] original, PatchResult result) {
        byte[] patched = patchStartAppWebTokenBridge(original, result);
        return patchStartAppExeDiagBootstrap(patched, result);
    }

    private static byte[] patchStartAppExeDiagBootstrap(byte[] original, PatchResult result) {
        ClassReader reader = new ClassReader(original);
        ClassNode classNode = new ClassNode(Opcodes.ASM9);
        reader.accept(classNode, ClassReader.EXPAND_FRAMES);
        MethodNode clinit = null;
        for (MethodNode method : classNode.methods) {
            if ("<clinit>".equals(method.name) && "()V".equals(method.desc)) {
                clinit = method;
                break;
            }
        }
        if (clinit == null) {
            clinit = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            clinit.instructions.add(new InsnNode(Opcodes.RETURN));
            classNode.methods.add(clinit);
        }
        InsnList install = new InsnList();
        install.add(
                new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "com/sbf/main/M8D14ExeDiag",
                        "install",
                        "()V",
                        false));
        clinit.instructions.insert(install);
        result.patchedStartAppExeDiagBootstrap = true;
        ClassWriter writer = computeFramesWriter(reader);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static byte[] patchStartAppWebTokenBridge(byte[] original, PatchResult result) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = computeFramesWriter(reader);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"f".equals(name) || !"(Ljava/lang/String;)Ljava/lang/String;".equals(descriptor)) {
                    return mv;
                }
                result.patchedStartAppWebTokenBridge = true;
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        emitWebTokenBridgeFastPath(this);
                    }
                };
            }
        };
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private static byte[] patchStartAppAutoLogin(byte[] original, PatchResult result) {
        ClassReader reader = new ClassReader(original);
        ClassNode classNode = new ClassNode(Opcodes.ASM9);
        reader.accept(classNode, ClassReader.EXPAND_FRAMES);
        for (MethodNode method : classNode.methods) {
            if (!"run".equals(method.name) || !"()V".equals(method.desc)) {
                continue;
            }
            org.objectweb.asm.tree.AbstractInsnNode loginWindow = null;
            for (org.objectweb.asm.tree.AbstractInsnNode instruction =
                            method.instructions.getFirst();
                    instruction != null;
                    instruction = instruction.getNext()) {
                if (instruction.getOpcode() == Opcodes.NEW
                        && instruction instanceof TypeInsnNode
                        && "com/sbf/main/ext/j2026/JLoginHTML"
                                .equals(((TypeInsnNode) instruction).desc)) {
                    loginWindow = instruction;
                    break;
                }
            }
            if (loginWindow == null) {
                continue;
            }
            while (loginWindow != null) {
                org.objectweb.asm.tree.AbstractInsnNode next = loginWindow.getNext();
                method.instructions.remove(loginWindow);
                loginWindow = next;
            }
            InsnList autoLogin = new InsnList();
            autoLogin.add(
                    new org.objectweb.asm.tree.FieldInsnNode(
                            Opcodes.GETSTATIC,
                            "java/lang/System",
                            "out",
                            "Ljava/io/PrintStream;"));
            autoLogin.add(new LdcInsnNode("M4B_AUTO_LOGIN"));
            autoLogin.add(
                    new MethodInsnNode(
                            Opcodes.INVOKEVIRTUAL,
                            "java/io/PrintStream",
                            "println",
                            "(Ljava/lang/String;)V",
                            false));
            autoLogin.add(new TypeInsnNode(Opcodes.NEW, "com/sbf/main/StartApp$1"));
            autoLogin.add(new InsnNode(Opcodes.DUP));
            autoLogin.add(
                    new MethodInsnNode(
                            Opcodes.INVOKESPECIAL,
                            "com/sbf/main/StartApp$1",
                            "<init>",
                            "()V",
                            false));
            autoLogin.add(new TypeInsnNode(Opcodes.NEW, "org/json/JSONObject"));
            autoLogin.add(new InsnNode(Opcodes.DUP));
            addRuntimeLoginJson(autoLogin);
            autoLogin.add(
                    new MethodInsnNode(
                            Opcodes.INVOKESPECIAL,
                            "org/json/JSONObject",
                            "<init>",
                            "(Ljava/lang/String;)V",
                            false));
            autoLogin.add(
                    new MethodInsnNode(
                            Opcodes.INVOKEVIRTUAL,
                            "com/sbf/main/StartApp$1",
                            "a",
                            "(Lorg/json/JSONObject;)V",
                            false));
            autoLogin.add(new InsnNode(Opcodes.RETURN));
            method.instructions.add(autoLogin);
            result.patchedStartAppAutoLogin = true;
            break;
        }
        ClassWriter writer = computeFramesWriter(reader);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static byte[] patchTrueExeLoginBridge(
            byte[] original,
            PatchResult result,
            String loginCallbackClass,
            boolean emitLoginCallback) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = computeFramesWriter(reader);
        ClassVisitor visitor =
                new ClassVisitor(Opcodes.ASM9, writer) {
                    @Override
                    public MethodVisitor visitMethod(
                            int access,
                            String name,
                            String descriptor,
                            String signature,
                            String[] exceptions) {
                        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                        if (!"<init>".equals(name) || !"(Lcom/sbf/main/ext/j;)V".equals(descriptor)) {
                            return mv;
                        }
                        result.patchedTrueExeLoginBridge = true;
                        return new MethodVisitor(Opcodes.ASM9, mv) {
                            @Override
                            public void visitInsn(int opcode) {
                                if (opcode == Opcodes.RETURN) {
                                    if (emitLoginCallback) {
                                        emitTrueExeLoginBridge(
                                                this,
                                                "M8D2_TRUE_EXE_LOGIN_BRIDGE",
                                                loginCallbackClass);
                                    } else {
                                        emitPrint(this, "M8D2_TRUE_EXE_LOGIN_BRIDGE");
                                    }
                                }
                                super.visitInsn(opcode);
                            }
                        };
                    }
                };
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private static byte[] patchDelayedTrueExeLoginBridge(
            byte[] original, PatchResult result, String loginCallbackClass) {
        ClassReader reader = new ClassReader(original);
        ClassNode classNode = new ClassNode(Opcodes.ASM9);
        reader.accept(classNode, ClassReader.EXPAND_FRAMES);
        for (MethodNode method : classNode.methods) {
            if (!"run".equals(method.name) || !"()V".equals(method.desc)) {
                continue;
            }
            org.objectweb.asm.tree.AbstractInsnNode loginVisibleCall =
                    findStartAppLoginVisibleCall(method);
            if (loginVisibleCall == null) {
                continue;
            }
            InsnList delayedLogin = new InsnList();
            delayedLogin.add(
                    new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "java/lang/Thread",
                            "yield",
                            "()V",
                            false));
            emitTrueExeLoginBridge(
                    delayedLogin,
                    "M8D4_DELAYED_TRUE_EXE_LOGIN_BRIDGE",
                    loginCallbackClass);
            method.instructions.insert(loginVisibleCall, delayedLogin);
            result.patchedDelayedTrueExeLoginBridge = true;
            break;
        }
        ClassWriter writer = computeFramesWriter(reader);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static org.objectweb.asm.tree.AbstractInsnNode findStartAppLoginVisibleCall(
            MethodNode method) {
        for (org.objectweb.asm.tree.AbstractInsnNode instruction =
                        method.instructions.getFirst();
                instruction != null;
                instruction = instruction.getNext()) {
            if (instruction.getOpcode() != Opcodes.INVOKEDYNAMIC) {
                continue;
            }
            org.objectweb.asm.tree.AbstractInsnNode visibleFlag =
                    previousRealInstruction(instruction);
            org.objectweb.asm.tree.AbstractInsnNode loginWindow =
                    previousRealInstruction(visibleFlag);
            if (visibleFlag != null
                    && visibleFlag.getOpcode() == Opcodes.ICONST_1
                    && loginWindow instanceof FieldInsnNode
                    && loginWindow.getOpcode() == Opcodes.GETSTATIC
                    && "com/sbf/main/StartApp".equals(((FieldInsnNode) loginWindow).owner)
                    && "w".equals(((FieldInsnNode) loginWindow).name)
                    && "Lcom/sbf/main/ext/j2026/JLoginHTML;"
                            .equals(((FieldInsnNode) loginWindow).desc)) {
                return instruction;
            }
        }
        return null;
    }

    private static org.objectweb.asm.tree.AbstractInsnNode previousRealInstruction(
            org.objectweb.asm.tree.AbstractInsnNode instruction) {
        if (instruction == null) {
            return null;
        }
        org.objectweb.asm.tree.AbstractInsnNode current = instruction.getPrevious();
        while (current != null
                && (current.getType() == org.objectweb.asm.tree.AbstractInsnNode.LABEL
                        || current.getType() == org.objectweb.asm.tree.AbstractInsnNode.LINE
                        || current.getType() == org.objectweb.asm.tree.AbstractInsnNode.FRAME)) {
            current = current.getPrevious();
        }
        return current;
    }

    private static void emitTrueExeLoginBridge(
            MethodVisitor mv, String marker, String loginCallbackClass) {
        emitPrint(mv, marker);
        emitPrint(mv, "M4B_AUTO_LOGIN");
        mv.visitTypeInsn(Opcodes.NEW, loginCallbackClass);
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                loginCallbackClass,
                "<init>",
                "()V",
                false);
        mv.visitTypeInsn(Opcodes.NEW, "org/json/JSONObject");
        mv.visitInsn(Opcodes.DUP);
        emitRuntimeLoginJson(mv);
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "org/json/JSONObject",
                "<init>",
                "(Ljava/lang/String;)V",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                loginCallbackClass,
                "a",
                "(Lorg/json/JSONObject;)V",
                false);
    }

    private static void emitTrueExeLoginBridge(
            InsnList instructions, String marker, String loginCallbackClass) {
        addPrint(instructions, marker);
        addPrint(instructions, "M4B_AUTO_LOGIN");
        instructions.add(new TypeInsnNode(Opcodes.NEW, loginCallbackClass));
        instructions.add(new InsnNode(Opcodes.DUP));
        instructions.add(
                new MethodInsnNode(
                        Opcodes.INVOKESPECIAL,
                        loginCallbackClass,
                        "<init>",
                        "()V",
                        false));
        instructions.add(new TypeInsnNode(Opcodes.NEW, "org/json/JSONObject"));
        instructions.add(new InsnNode(Opcodes.DUP));
        addRuntimeLoginJson(instructions);
        instructions.add(
                new MethodInsnNode(
                        Opcodes.INVOKESPECIAL,
                        "org/json/JSONObject",
                        "<init>",
                        "(Ljava/lang/String;)V",
                        false));
        instructions.add(
                new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        loginCallbackClass,
                        "a",
                        "(Lorg/json/JSONObject;)V",
                false));
    }

    private static void emitRuntimeLoginJson(MethodVisitor mv) {
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn(LOGIN_JSON_PREFIX);
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/StringBuilder",
                "<init>",
                "(Ljava/lang/String;)V",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/System",
                "currentTimeMillis",
                "()J",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(J)Ljava/lang/StringBuilder;",
                false);
        mv.visitLdcInsn(LOGIN_JSON_SUFFIX);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "toString",
                "()Ljava/lang/String;",
                false);
    }

    private static void addRuntimeLoginJson(InsnList instructions) {
        instructions.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
        instructions.add(new InsnNode(Opcodes.DUP));
        instructions.add(new LdcInsnNode(LOGIN_JSON_PREFIX));
        instructions.add(
                new MethodInsnNode(
                        Opcodes.INVOKESPECIAL,
                        "java/lang/StringBuilder",
                        "<init>",
                        "(Ljava/lang/String;)V",
                        false));
        instructions.add(
                new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "java/lang/System",
                        "currentTimeMillis",
                        "()J",
                        false));
        instructions.add(
                new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        "java/lang/StringBuilder",
                        "append",
                        "(J)Ljava/lang/StringBuilder;",
                        false));
        instructions.add(new LdcInsnNode(LOGIN_JSON_SUFFIX));
        instructions.add(
                new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        "java/lang/StringBuilder",
                        "append",
                        "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                        false));
        instructions.add(
                new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        "java/lang/StringBuilder",
                        "toString",
                        "()Ljava/lang/String;",
                        false));
    }

    private static void addPrint(InsnList instructions, String message) {
        instructions.add(
                new FieldInsnNode(
                        Opcodes.GETSTATIC,
                        "java/lang/System",
                        "out",
                        "Ljava/io/PrintStream;"));
        instructions.add(new LdcInsnNode(message));
        instructions.add(
                new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        "java/io/PrintStream",
                        "println",
                        "(Ljava/lang/String;)V",
                        false));
    }

    private static byte[] patchProductSelectorEnterBridge(byte[] original, PatchResult result) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = computeFramesWriter(reader);
        ClassVisitor visitor =
                new ClassVisitor(Opcodes.ASM9, writer) {
                    @Override
                    public MethodVisitor visitMethod(
                            int access,
                            String name,
                            String descriptor,
                            String signature,
                            String[] exceptions) {
                        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                        if (!"a".equals(name) || !"(Lorg/json/JSONObject;)V".equals(descriptor)) {
                            return mv;
                        }
                        result.patchedProductSelectorEnterBridge = true;
                        return new AdviceAdapter(Opcodes.ASM9, mv, access, name, descriptor) {
                            private boolean sawStartAppMainField;
                            private boolean sawStartAppMainVisibleTrue;
                            private int productSelectorJsonLocal;

                            @Override
                            protected void onMethodEnter() {
                                productSelectorJsonLocal =
                                        newLocal(
                                                org.objectweb.asm.Type.getObjectType(
                                                        "org/json/JSONObject"));
                                loadArg(0);
                                storeLocal(productSelectorJsonLocal);
                                emitProductSelectorEnterHardGate(this, productSelectorJsonLocal);
                            }

                            @Override
                            public void visitFieldInsn(
                                    int opcode,
                                    String owner,
                                    String fieldName,
                                    String fieldDescriptor) {
                                super.visitFieldInsn(opcode, owner, fieldName, fieldDescriptor);
                                sawStartAppMainField =
                                        opcode == Opcodes.GETSTATIC
                                                && "com/sbf/main/StartApp".equals(owner)
                                                && "x".equals(fieldName)
                                                && "Lcom/sbf/main/JSBFMain;".equals(fieldDescriptor);
                                if (!sawStartAppMainField) {
                                    sawStartAppMainVisibleTrue = false;
                                }
                            }

                            @Override
                            public void visitInsn(int opcode) {
                                super.visitInsn(opcode);
                                if (sawStartAppMainField && opcode == Opcodes.ICONST_1) {
                                    sawStartAppMainVisibleTrue = true;
                                    sawStartAppMainField = false;
                                } else if (opcode != Opcodes.NOP) {
                                    sawStartAppMainField = false;
                                }
                            }

                            @Override
                            public void visitInvokeDynamicInsn(
                                    String dynamicName,
                                    String dynamicDescriptor,
                                    Handle bootstrapMethodHandle,
                                    Object... bootstrapMethodArguments) {
                                super.visitInvokeDynamicInsn(
                                        dynamicName,
                                        dynamicDescriptor,
                                        bootstrapMethodHandle,
                                        bootstrapMethodArguments);
                                if (sawStartAppMainVisibleTrue
                                        && "(Ljava/lang/Object;Z)V".equals(dynamicDescriptor)) {
                                    emitDefaultMenuDispatchAfterProductEnter(
                                            this, productSelectorJsonLocal);
                                }
                                sawStartAppMainField = false;
                                sawStartAppMainVisibleTrue = false;
                            }
                        };
                    }
                };
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private static void emitProductSelectorEnterHardGate(
            AdviceAdapter mv, int productSelectorJsonLocal) {
        Label done = new Label();
        mv.loadLocal(productSelectorJsonLocal);
        mv.visitJumpInsn(Opcodes.IFNULL, done);
        int productCodeLocal = storeProductCode(mv, productSelectorJsonLocal);
        emitSupportedDefaultDispatchProductCheck(mv, productCodeLocal, done);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("M8D3_LOCAL_PRODUCT_ENTER code=");
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/StringBuilder",
                "<init>",
                "(Ljava/lang/String;)V",
                false);
        mv.loadLocal(productSelectorJsonLocal);
        mv.visitLdcInsn("code");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "org/json/JSONObject",
                "optString",
                "(Ljava/lang/String;)Ljava/lang/String;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "toString",
                "()Ljava/lang/String;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/io/PrintStream",
                "println",
                "(Ljava/lang/String;)V",
                false);
        mv.visitLabel(done);
    }

    private static void emitDefaultMenuDispatchAfterProductEnter(
            AdviceAdapter mv, int productSelectorJsonLocal) {
        Label done = new Label();
        mv.loadLocal(productSelectorJsonLocal);
        mv.visitJumpInsn(Opcodes.IFNULL, done);
        int productCodeLocal = storeProductCode(mv, productSelectorJsonLocal);
        emitSupportedDefaultDispatchProductCheck(mv, productCodeLocal, done);
        emitPrint(mv, "M8D7_DEFAULT_MENU_DISPATCH_AFTER_ENTER");
        mv.visitFieldInsn(
                Opcodes.GETSTATIC,
                "com/sbf/main/StartApp",
                "x",
                "Lcom/sbf/main/JSBFMain;");
        mv.loadLocal(productCodeLocal);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/M8D7DefaultMenuDispatch",
                "dispatch",
                "(Lcom/sbf/main/JSBFMain;Ljava/lang/String;)V",
                false);
        mv.visitLabel(done);
    }

    private static int storeProductCode(AdviceAdapter mv, int productSelectorJsonLocal) {
        int productCodeLocal = mv.newLocal(org.objectweb.asm.Type.getType(String.class));
        mv.loadLocal(productSelectorJsonLocal);
        mv.visitLdcInsn("code");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "org/json/JSONObject",
                "optString",
                "(Ljava/lang/String;)Ljava/lang/String;",
                false);
        mv.storeLocal(productCodeLocal);
        return productCodeLocal;
    }

    private static void emitSupportedDefaultDispatchProductCheck(
            AdviceAdapter mv, int productCodeLocal, Label done) {
        Label matched = new Label();
        String[] supported = {
            "whatsapp", "tiktok", "facebook", "instagram",
            "twitter", "telegram", "geo", "wskefu"
        };
        for (int index = 0; index < supported.length; index++) {
            mv.visitLdcInsn(supported[index]);
            mv.loadLocal(productCodeLocal);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/String",
                    "equals",
                    "(Ljava/lang/Object;)Z",
                    false);
            if (index + 1 < supported.length) {
                mv.visitJumpInsn(Opcodes.IFNE, matched);
            } else {
                mv.visitJumpInsn(Opcodes.IFEQ, done);
            }
        }
        mv.visitLabel(matched);
    }

    private static byte[] patchStartAppLoginDisposeGuard(byte[] original, PatchResult result) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = computeFramesWriter(reader);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"a".equals(name) || !"(Lorg/json/JSONObject;)V".equals(descriptor)) {
                    return mv;
                }
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    private boolean guardNextDynamicCall;
                    private org.objectweb.asm.Label nullLoginWindow;
                    private org.objectweb.asm.Label afterDispose;

                    @Override
                    public void visitFieldInsn(
                            int opcode, String owner, String fieldName, String fieldDescriptor) {
                        super.visitFieldInsn(opcode, owner, fieldName, fieldDescriptor);
                        if (opcode == Opcodes.GETSTATIC
                                && "com/sbf/main/StartApp".equals(owner)
                                && "t".equals(fieldName)
                                && "Lcom/sbf/main/ext/j2026/JLoginHTML;"
                                        .equals(fieldDescriptor)) {
                            nullLoginWindow = new org.objectweb.asm.Label();
                            afterDispose = new org.objectweb.asm.Label();
                            super.visitInsn(Opcodes.DUP);
                            super.visitJumpInsn(Opcodes.IFNULL, nullLoginWindow);
                            guardNextDynamicCall = true;
                        }
                    }

                    @Override
                    public void visitInvokeDynamicInsn(
                            String dynamicName,
                            String dynamicDescriptor,
                            Handle bootstrapMethodHandle,
                            Object... bootstrapMethodArguments) {
                        super.visitInvokeDynamicInsn(
                                dynamicName,
                                dynamicDescriptor,
                                bootstrapMethodHandle,
                                bootstrapMethodArguments);
                        if (!guardNextDynamicCall) {
                            return;
                        }
                        super.visitJumpInsn(Opcodes.GOTO, afterDispose);
                        super.visitLabel(nullLoginWindow);
                        super.visitInsn(Opcodes.POP);
                        super.visitFieldInsn(
                                Opcodes.GETSTATIC,
                                "java/lang/System",
                                "out",
                                "Ljava/io/PrintStream;");
                        super.visitLdcInsn("M4B_SKIP_LOGIN_DISPOSE");
                        super.visitMethodInsn(
                                Opcodes.INVOKEVIRTUAL,
                                "java/io/PrintStream",
                                "println",
                                "(Ljava/lang/String;)V",
                                false);
                        super.visitLabel(afterDispose);
                        guardNextDynamicCall = false;
                        result.patchedStartAppLoginDisposeGuard = true;
                    }
                };
            }
        };
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private static void emitWebTokenBridgeFastPath(MethodVisitor mv) {
        org.objectweb.asm.Label fallThrough = new org.objectweb.asm.Label();
        org.objectweb.asm.Label returnToken = new org.objectweb.asm.Label();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitJumpInsn(Opcodes.IFNULL, fallThrough);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitLdcInsn("getLoingIsToken");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "contains",
                "(Ljava/lang/CharSequence;)Z",
                false);
        mv.visitJumpInsn(Opcodes.IFNE, returnToken);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitLdcInsn("get_current_token");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "contains",
                "(Ljava/lang/CharSequence;)Z",
                false);
        mv.visitJumpInsn(Opcodes.IFEQ, fallThrough);
        mv.visitLabel(returnToken);
        emitStringBuilderPrint(
                mv,
                "M4_V19_WEB_TOKEN_BRIDGE url=",
                Opcodes.ALOAD,
                0,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
        mv.visitLdcInsn(WEB_BRIDGE_TOKEN);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitLabel(fallThrough);
    }

    private static byte[] patchTreeNodeDiagnostics(byte[] original, PatchResult result) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"<init>".equals(name)
                        || !"(IILorg/json/JSONObject;ZLcom/sbf/main/tree/i$a;Lorg/json/JSONArray;Ljava/lang/String;)V"
                                .equals(descriptor)) {
                    return mv;
                }
                result.patchedTreeDiagnostics = true;
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    private boolean injected;

                    @Override
                    public void visitMethodInsn(
                            int opcode, String owner, String methodName, String methodDescriptor, boolean isInterface) {
                        super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                        if (!injected && opcode == Opcodes.INVOKESPECIAL && "<init>".equals(methodName)) {
                            injected = true;
                            emitTreeDiagnostics(this);
                        }
                    }
                };
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static byte[] patchMenuDispatchDiagnostics(byte[] original, PatchResult result) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"a".equals(name) || !"(Lcom/sbf/main/tree/i;)V".equals(descriptor)) {
                    return mv;
                }
                result.patchedMenuDispatchDiagnostics = true;
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        emitDispatchEnter(this);
                    }

                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        if (opcode == Opcodes.NEW) {
                            if ("com/sbf/main/ext/m".equals(type)) {
                                emitPrint(this, "M4_DIAG_BRANCH_JSinglepage");
                            } else if ("com/sbf/main/jxbrowser/c".equals(type)) {
                                emitPrint(this, "M4_DIAG_BRANCH_JxBrowser");
                            } else if ("com/sbf/main/sub/zw/JZWBrowserMaster".equals(type)) {
                                emitPrint(this, "M4_DIAG_BRANCH_ZWBrowser");
                            }
                        }
                        super.visitTypeInsn(opcode, type);
                    }
                };
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static byte[] patchModernMenuDispatchDiagnostics(byte[] original, PatchResult result) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = computeFramesWriter(reader);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"a".equals(name)
                        || !"(Ljavax/swing/JComponent;Ljava/lang/String;)V".equals(descriptor)) {
                    return mv;
                }
                result.patchedModernMenuDispatchDiagnostics = true;
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        emitModernDispatchDiagnostics(this);
                    }

                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        if (opcode == Opcodes.NEW) {
                            if ("com/sbf/main/jxbrowser/c".equals(type)) {
                                emitModernCollectTabJxBrowserUrlFix(this);
                                emitPrint(this, "M4_V12_NEW_JXBROWSER");
                            } else if ("com/sbf/main/ext/j2026/ui/c".equals(type)) {
                                emitPrint(this, "M4_V12_NEW_J2026_UI_C");
                            } else if ("com/sbf/main/ext/open/JOPENFrame".equals(type)) {
                                emitPrint(this, "M4_V12_NEW_JOPEN");
                            }
                        }
                        super.visitTypeInsn(opcode, type);
                    }
                };
            }
        };
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private static byte[] patchModernMenuMouseDiagnostics(byte[] original, PatchResult result) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"mouseClicked".equals(name) || !"(Ljava/awt/event/MouseEvent;)V".equals(descriptor)) {
                    return mv;
                }
                result.patchedModernMenuMouseDiagnostics = true;
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    private int invokedynamicCount = 0;

                    @Override
                    public void visitCode() {
                        super.visitCode();
                        emitModernMenuMouseDiagnostics(this, "M5A_V43_MENU_MOUSE_CLICKED");
                    }

                    @Override
                    public void visitInvokeDynamicInsn(
                            String dynamicName,
                            String dynamicDescriptor,
                            Handle bootstrapMethodHandle,
                            Object... bootstrapMethodArguments) {
                        invokedynamicCount++;
                        if (invokedynamicCount == 2) {
                            emitModernMenuMouseDiagnostics(this, "M5A_V43_MENU_MOUSE_CALLBACK");
                        }
                        super.visitInvokeDynamicInsn(
                                dynamicName,
                                dynamicDescriptor,
                                bootstrapMethodHandle,
                                bootstrapMethodArguments);
                    }

                    @Override
                    public void visitJumpInsn(int opcode, Label label) {
                        super.visitJumpInsn(opcode, label);
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        if (opcode == Opcodes.RETURN && invokedynamicCount == 1) {
                            emitModernMenuMouseDiagnostics(this, "M5A_V43_MENU_MOUSE_BLOCKED");
                        }
                        super.visitInsn(opcode);
                    }
                };
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static byte[] patchSideMenuMouseDiagnostics(byte[] original, PatchResult result) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"mouseClicked".equals(name)
                        || !"(Ljava/awt/event/MouseEvent;)V".equals(descriptor)) {
                    return mv;
                }
                result.patchedSideMenuMouseDiagnostics = true;
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    private int invokedynamicCount = 0;

                    @Override
                    public void visitCode() {
                        super.visitCode();
                        emitSideMenuDiagnostics(
                                this,
                                "M5A_V44_SIDE_MENU_MOUSE_CLICKED",
                                "com/sbf/main/ext/j2026/d$2");
                    }

                    @Override
                    public void visitInvokeDynamicInsn(
                            String dynamicName,
                            String dynamicDescriptor,
                            Handle bootstrapMethodHandle,
                            Object... bootstrapMethodArguments) {
                        invokedynamicCount++;
                        if (invokedynamicCount == 2) {
                            emitSideMenuDiagnostics(
                                    this,
                                    "M5A_V44_SIDE_MENU_SELECT_CALL",
                                    "com/sbf/main/ext/j2026/d$2");
                        }
                        super.visitInvokeDynamicInsn(
                                dynamicName,
                                dynamicDescriptor,
                                bootstrapMethodHandle,
                                bootstrapMethodArguments);
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        if (opcode == Opcodes.RETURN && invokedynamicCount == 1) {
                            emitSideMenuDiagnostics(
                                    this,
                                    "M5A_V44_SIDE_MENU_MOUSE_BLOCKED",
                                    "com/sbf/main/ext/j2026/d$2");
                        }
                        super.visitInsn(opcode);
                    }
                };
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static byte[] patchSideMenuCallbackDiagnostics(byte[] original, PatchResult result) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"run".equals(name) || !"()V".equals(descriptor)) {
                    return mv;
                }
                result.patchedSideMenuCallbackDiagnostics = true;
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitMethodInsn(
                            int opcode,
                            String owner,
                            String methodName,
                            String methodDescriptor,
                            boolean isInterface) {
                        if (opcode == Opcodes.INVOKEINTERFACE
                                && "com/sbf/main/ext/j2026/d$a".equals(owner)
                                && "a".equals(methodName)
                                && "(Lcom/sbf/main/ext/j2026/d;)V".equals(methodDescriptor)) {
                            emitSideMenuDiagnostics(
                                    this,
                                    "M5A_V44_SIDE_MENU_CALLBACK",
                                    "com/sbf/main/ext/j2026/d$1");
                        }
                        super.visitMethodInsn(
                                opcode, owner, methodName, methodDescriptor, isInterface);
                    }
                };
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static byte[] patchUpdateChecker(byte[] original, PatchResult result) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(reader, 0);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                if ("run".equals(name) && "()V".equals(descriptor)) {
                    result.patchedUpdateChecker = true;
                    MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                    mv.visitCode();
                    mv.visitInsn(Opcodes.RETURN);
                    mv.visitMaxs(0, 1);
                    mv.visitEnd();
                    return null;
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static byte[] patchJxBrowserLoadDiagnostics(byte[] original, PatchResult result) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = computeFramesWriter(reader);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitMethodInsn(
                            int opcode,
                            String owner,
                            String methodName,
                            String methodDescriptor,
                            boolean isInterface) {
                        if (opcode == Opcodes.INVOKEINTERFACE
                                && "com/teamdev/jxbrowser/navigation/Navigation".equals(owner)
                                && "loadUrl".equals(methodName)
                                && "(Ljava/lang/String;)V".equals(methodDescriptor)) {
                            result.patchedJxBrowserLoadDiagnostics = true;
                            visitVarInsn(Opcodes.ASTORE, 3);
                            visitVarInsn(Opcodes.ASTORE, 4);
                            emitNormalizeRuntimeBusinessUrl(this, 3);
                            emitStringBuilderPrint(
                                    this,
                                    "M4_V18_NORMALIZED_URL=",
                                    Opcodes.ALOAD,
                                    3,
                                    "java/lang/StringBuilder",
                                    "append",
                                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
                            emitStringBuilderPrint(
                                    this,
                                    "M4_V13_LOAD_URL=",
                                    Opcodes.ALOAD,
                                    3,
                                    "java/lang/StringBuilder",
                                    "append",
                                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
                            visitVarInsn(Opcodes.ALOAD, 4);
                            visitVarInsn(Opcodes.ALOAD, 3);
                        }
                        super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                    }
                };
            }
        };
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private static byte[] patchJxBrowserEngine(byte[] original, PatchResult result) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"a".equals(name)
                        || (!("(Ljava/lang/String;Lcom/sbf/main/jxbrowser/g$a;"
                                                        + "Lcom/sbf/main/jxbrowser/g$b;"
                                                        + "Ljava/lang/String;"
                                                        + "Lcom/db/entery/xdx/JDBZWConfig;"
                                                        + "Lcom/sbf/main/jxbrowser/l;Z)"
                                                        + "Lcom/teamdev/jxbrowser/browser/Browser;")
                                        .equals(descriptor)
                                && !("(Ljava/lang/String;"
                                                        + "Lcom/teamdev/jxbrowser/browser/Browser;"
                                                        + "Lcom/sbf/main/jxbrowser/g$a;"
                                                        + "Lcom/sbf/main/jxbrowser/g$b;"
                                                        + "Ljava/lang/String;"
                                                        + "Lcom/db/entery/xdx/JDBZWConfig;"
                                                        + "Lcom/sbf/main/jxbrowser/l;)V")
                                        .equals(descriptor))) {
                    return mv;
                }
                final boolean browserSetupMethod =
                        ("(Ljava/lang/String;"
                                        + "Lcom/teamdev/jxbrowser/browser/Browser;"
                                        + "Lcom/sbf/main/jxbrowser/g$a;"
                                        + "Lcom/sbf/main/jxbrowser/g$b;"
                                        + "Ljava/lang/String;"
                                        + "Lcom/db/entery/xdx/JDBZWConfig;"
                                        + "Lcom/sbf/main/jxbrowser/l;)V")
                                .equals(descriptor);
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    private boolean injectingUserAgent;

                    @Override
                    public void visitFieldInsn(
                            int opcode, String owner, String fieldName, String fieldDescriptor) {
                        if (opcode == Opcodes.GETSTATIC
                                && "com/teamdev/jxbrowser/engine/RenderingMode".equals(owner)
                                && ("HARDWARE_ACCELERATED".equals(fieldName)
                                        || "OFF_SCREEN".equals(fieldName))) {
                            result.patchedJxBrowserEngine = true;
                            super.visitFieldInsn(
                                    opcode, owner, "OFF_SCREEN", fieldDescriptor);
                            return;
                        }
                        super.visitFieldInsn(opcode, owner, fieldName, fieldDescriptor);
                    }

                    @Override
                    public void visitMethodInsn(
                            int opcode,
                            String owner,
                            String methodName,
                            String methodDescriptor,
                            boolean isInterface) {
                        super.visitMethodInsn(
                                opcode, owner, methodName, methodDescriptor, isInterface);
                        if (!injectingUserAgent
                                && browserSetupMethod
                                && opcode == Opcodes.INVOKEINTERFACE
                                && "com/teamdev/jxbrowser/browser/Browser".equals(owner)
                                && "userAgent".equals(methodName)
                                && "(Ljava/lang/String;)V".equals(methodDescriptor)) {
                            injectingUserAgent = true;
                            emitForceBrowserUserAgent(this);
                            injectingUserAgent = false;
                        } else if (opcode == Opcodes.INVOKESTATIC
                                && "com/teamdev/jxbrowser/engine/EngineOptions".equals(owner)
                                && "newBuilder".equals(methodName)) {
                            emitSoftwareRenderingOptions(this);
                        } else if (opcode == Opcodes.INVOKEVIRTUAL
                                && "com/teamdev/jxbrowser/engine/EngineOptions$Builder"
                                        .equals(owner)
                                && "build".equals(methodName)
                                && "()Lcom/teamdev/jxbrowser/engine/EngineOptions;"
                                        .equals(methodDescriptor)) {
                            emitEngineOptionsDiagnostics(this);
                        }
                    }
                };
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static void emitForceBrowserUserAgent(MethodVisitor mv) {
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitLdcInsn(modernChromeUserAgent());
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/browser/Browser",
                "userAgent",
                "(Ljava/lang/String;)V",
                true);
        emitPrint(mv, "M8B1A_BROWSER_USER_AGENT_FORCED");
    }

    private static void emitSoftwareRenderingOptions(MethodVisitor mv) {
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/teamdev/jxbrowser/engine/EngineOptions$Builder",
                "disableGpu",
                "()Lcom/teamdev/jxbrowser/engine/EngineOptions$Builder;",
                false);
        mv.visitLdcInsn(modernChromeUserAgent());
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/teamdev/jxbrowser/engine/EngineOptions$Builder",
                "userAgent",
                "(Ljava/lang/String;)Lcom/teamdev/jxbrowser/engine/EngineOptions$Builder;",
                false);
        String[] switches = {
            "--disable-gpu-compositing",
            "--disable-d3d11",
            "--use-gl=swiftshader",
            "--use-angle=swiftshader"
        };
        for (String chromiumSwitch : switches) {
            mv.visitLdcInsn(chromiumSwitch);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "com/teamdev/jxbrowser/engine/EngineOptions$Builder",
                    "addSwitch",
                    "(Ljava/lang/String;)"
                            + "Lcom/teamdev/jxbrowser/engine/EngineOptions$Builder;",
                    false);
        }
    }

    private static byte[] patchJxBrowserOfflineNetworkSwitches(byte[] original) {
        if (new String(original, StandardCharsets.ISO_8859_1)
                .contains("--disable-background-networking")) {
            return original;
        }
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitMethodInsn(
                            int opcode,
                            String owner,
                            String methodName,
                            String methodDescriptor,
                            boolean isInterface) {
                        super.visitMethodInsn(
                                opcode, owner, methodName, methodDescriptor, isInterface);
                        if (opcode == Opcodes.INVOKESTATIC
                                && "com/teamdev/jxbrowser/engine/EngineOptions".equals(owner)
                                && "newBuilder".equals(methodName)) {
                            emitOfflineChromiumNetworkSwitches(this);
                        }
                    }
                };
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static byte[] patchC64NativeUpdateCheck(byte[] original) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(reader, 0);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                if ("g".equals(name)
                        && "(Ljava/lang/String;I)Lorg/json/JSONObject;".equals(descriptor)) {
                    MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                    mv.visitCode();
                    emitPrint(mv, "C64_MIERP_UPDATE_STUB");
                    mv.visitTypeInsn(Opcodes.NEW, "org/json/JSONObject");
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitLdcInsn(
                            "{\"code\":200,\"msg\":\"no update\",\"hasUpdate\":false,"
                                    + "\"data\":{\"hasUpdate\":false,\"needUpdate\":false,"
                                    + "\"version\":\"\",\"url\":\"\"}}");
                    mv.visitMethodInsn(
                            Opcodes.INVOKESPECIAL,
                            "org/json/JSONObject",
                            "<init>",
                            "(Ljava/lang/String;)V",
                            false);
                    mv.visitInsn(Opcodes.ARETURN);
                    mv.visitMaxs(3, 2);
                    mv.visitEnd();
                    return null;
                }
                if ("n".equals(name)
                        && ("()V".equals(descriptor)
                                || "(Ljava/lang/String;)V".equals(descriptor))) {
                    MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                    mv.visitCode();
                    emitPrint(
                            mv,
                            "()V".equals(descriptor)
                                    ? "C64_APPFILE_DOWNLOAD_STUB"
                                    : "C65_APPFILE_FLOW_STUB");
                    mv.visitInsn(Opcodes.RETURN);
                    mv.visitMaxs(2, "()V".equals(descriptor) ? 0 : 1);
                    mv.visitEnd();
                    return null;
                }
                if ("ax".equals(name) && "(Ljava/lang/String;)Lorg/json/JSONObject;".equals(descriptor)) {
                    MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                    mv.visitCode();
                    emitPrint(mv, "C65_APPFILE_RESPONSE_ADAPTER");
                    mv.visitTypeInsn(Opcodes.NEW, "org/json/JSONObject");
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitLdcInsn("{}");
                    mv.visitMethodInsn(
                            Opcodes.INVOKESPECIAL,
                            "org/json/JSONObject",
                            "<init>",
                            "(Ljava/lang/String;)V",
                            false);
                    mv.visitInsn(Opcodes.ARETURN);
                    mv.visitMaxs(3, 1);
                    mv.visitEnd();
                    return null;
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static byte[] patchD8OnlineEnabledFlag(byte[] original) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(reader, 0);
        final boolean[] patched = new boolean[] {false};
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                if (!"d8OnlineEnabled".equals(name) || !"()Z".equals(descriptor)) {
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }
                patched[0] = true;
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                mv.visitCode();
                mv.visitInsn(Opcodes.ICONST_1);
                mv.visitInsn(Opcodes.IRETURN);
                mv.visitMaxs(1, 0);
                mv.visitEnd();
                return null;
            }
        };
        reader.accept(visitor, 0);
        if (!patched[0]) {
            throw new IllegalStateException("D8 online support flag was not found");
        }
        return writer.toByteArray();
    }

    private static byte[] patchD8OnlineStartupAuthorization(byte[] original) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = computeFramesWriter(reader);
        final boolean[] replaced = new boolean[] {false};
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"f".equals(name) || !"(Ljava/lang/String;)Ljava/lang/String;".equals(descriptor)) {
                    return mv;
                }
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitLdcInsn(Object value) {
                        if ("https://app.xdxsoft.com/".equals(value)) {
                            replaced[0] = true;
                            super.visitLdcInsn("https://offline.invalid/");
                            return;
                        }
                        if ("http://app.xdxsoft.com/".equals(value)) {
                            replaced[0] = true;
                            super.visitLdcInsn("http://offline.invalid/");
                            return;
                        }
                        super.visitLdcInsn(value);
                    }
                };
            }
        };
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        if (!replaced[0]) {
            throw new IllegalStateException("D8 online startup authorization short-circuit was not found");
        }
        return writer.toByteArray();
    }

    private static byte[] patchC64NativeStartupAuthorization(byte[] original) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = computeFramesWriter(reader);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"f".equals(name) || !"(Ljava/lang/String;)Ljava/lang/String;".equals(descriptor)) {
                    return mv;
                }
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        Label fallThrough = new Label();
                        Label returnLocalToken = new Label();
                        mv.visitVarInsn(Opcodes.ALOAD, 0);
                        mv.visitJumpInsn(Opcodes.IFNULL, fallThrough);
                        mv.visitVarInsn(Opcodes.ALOAD, 0);
                        mv.visitLdcInsn("https://app.xdxsoft.com/");
                        mv.visitMethodInsn(
                                Opcodes.INVOKEVIRTUAL,
                                "java/lang/String",
                                "startsWith",
                                "(Ljava/lang/String;)Z",
                                false);
                        mv.visitJumpInsn(Opcodes.IFNE, returnLocalToken);
                        mv.visitVarInsn(Opcodes.ALOAD, 0);
                        mv.visitLdcInsn("http://app.xdxsoft.com/");
                        mv.visitMethodInsn(
                                Opcodes.INVOKEVIRTUAL,
                                "java/lang/String",
                                "startsWith",
                                "(Ljava/lang/String;)Z",
                                false);
                        mv.visitJumpInsn(Opcodes.IFEQ, fallThrough);
                        mv.visitLabel(returnLocalToken);
                        emitStringBuilderPrint(
                                mv,
                                "C64_NATIVE_STARTUP_AUTH_STUB url=",
                                Opcodes.ALOAD,
                                0,
                                "java/lang/StringBuilder",
                                "append",
                                "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
                        mv.visitLdcInsn(WEB_BRIDGE_TOKEN);
                        mv.visitInsn(Opcodes.ARETURN);
                        mv.visitLabel(fallThrough);
                    }
                };
            }
        };
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private static byte[] patchC64NativeUrlDiagnostics(byte[] original) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitMethodInsn(
                            int opcode,
                            String owner,
                            String methodName,
                            String methodDescriptor,
                            boolean isInterface) {
                        if (opcode == Opcodes.INVOKEVIRTUAL
                                && "okhttp3/Request$Builder".equals(owner)
                                && "url".equals(methodName)
                                && "(Ljava/lang/String;)Lokhttp3/Request$Builder;"
                                        .equals(methodDescriptor)) {
                            super.visitMethodInsn(
                                    Opcodes.INVOKESTATIC,
                                    "com/sbf/main/C64NativeNetworkDiag",
                                    "observe",
                                    "(Ljava/lang/String;)Ljava/lang/String;",
                                    false);
                        }
                        super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                    }
                };
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static byte[] patchC65NativeStartupGateway(byte[] original) {
        final String gatewayDescriptor =
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZLjava/util/HashMap;"
                        + "Ljava/util/HashMap;ZZ)Lorg/json/JSONObject;";
        final boolean[] patched = new boolean[] {false};
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = computeFramesWriter(reader);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"a".equals(name) || !gatewayDescriptor.equals(descriptor)) {
                    return mv;
                }
                patched[0] = true;
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        Label continueOriginal = new Label();
                        mv.visitVarInsn(Opcodes.ALOAD, 0);
                        mv.visitMethodInsn(
                                Opcodes.INVOKESTATIC,
                                "com/sbf/main/C64NativeNetworkDiag",
                                "localResponse",
                                "(Ljava/lang/String;)Ljava/lang/String;",
                                false);
                        mv.visitVarInsn(Opcodes.ASTORE, 8);
                        mv.visitVarInsn(Opcodes.ALOAD, 8);
                        mv.visitJumpInsn(Opcodes.IFNULL, continueOriginal);
                        mv.visitTypeInsn(Opcodes.NEW, "org/json/JSONObject");
                        mv.visitInsn(Opcodes.DUP);
                        mv.visitVarInsn(Opcodes.ALOAD, 8);
                        mv.visitMethodInsn(
                                Opcodes.INVOKESPECIAL,
                                "org/json/JSONObject",
                                "<init>",
                                "(Ljava/lang/String;)V",
                                false);
                        mv.visitInsn(Opcodes.ARETURN);
                        mv.visitLabel(continueOriginal);
                    }
                };
            }
        };
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        if (!patched[0]) {
            throw new IllegalStateException("C65 native gateway DTHelper JSON exit was not found");
        }
        return writer.toByteArray();
    }

    private static byte[] patchC66GlobalRechargeListener(byte[] original) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(reader, 0);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                if (!"a".equals(name) || !"(I)V".equals(descriptor)) {
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                mv.visitCode();
                emitPrint(mv, "C66_RECHARGE_ENTRY route=/pc/c6/recharge module=C6_RECHARGE_UI");
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "com/sbf/main/jxbrowser/M5LocalSpiderBridge",
                        "openC66RechargeDialog",
                        "()V",
                        false);
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(2, 2);
                mv.visitEnd();
                return null;
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static void emitOfflineChromiumNetworkSwitches(MethodVisitor mv) {
        String[] switches = {
            "--disable-background-networking",
            "--disable-component-update",
            "--disable-domain-reliability",
            "--disable-client-side-phishing-detection",
            "--no-pings",
            "--safebrowsing-disable-auto-update",
            "--disable-sync",
            "--no-first-run"
        };
        for (String chromiumSwitch : switches) {
            mv.visitLdcInsn(chromiumSwitch);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "com/teamdev/jxbrowser/engine/EngineOptions$Builder",
                    "addSwitch",
                    "(Ljava/lang/String;)"
                            + "Lcom/teamdev/jxbrowser/engine/EngineOptions$Builder;",
                    false);
        }
    }

    private static String modernChromeUserAgent() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) "
                + "Chrome/124.0.0.0 Safari/537.36";
    }

    private static void emitEngineOptionsDiagnostics(MethodVisitor mv) {
        mv.visitVarInsn(Opcodes.ASTORE, 30);
        mv.visitFieldInsn(
                Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("M4_V14_RENDER_MODE=");
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/StringBuilder",
                "<init>",
                "(Ljava/lang/String;)V",
                false);
        mv.visitVarInsn(Opcodes.ALOAD, 30);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/teamdev/jxbrowser/engine/EngineOptions",
                "renderingMode",
                "()Lcom/teamdev/jxbrowser/engine/RenderingMode;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/Object;)Ljava/lang/StringBuilder;",
                false);
        mv.visitLdcInsn(" userAgent=");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                false);
        mv.visitVarInsn(Opcodes.ALOAD, 30);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/teamdev/jxbrowser/engine/EngineOptions",
                "userAgent",
                "()Ljava/util/Optional;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/Object;)Ljava/lang/StringBuilder;",
                false);
        mv.visitLdcInsn(" switches=");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                false);
        mv.visitVarInsn(Opcodes.ALOAD, 30);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/teamdev/jxbrowser/engine/EngineOptions",
                "switches",
                "()Ljava/util/List;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/Object;)Ljava/lang/StringBuilder;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "toString",
                "()Ljava/lang/String;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/io/PrintStream",
                "println",
                "(Ljava/lang/String;)V",
                false);
        mv.visitVarInsn(Opcodes.ALOAD, 30);
    }

    private static byte[] patchJxBrowserDiagnostics(byte[] original, PatchResult result) {
        String browserRouteFieldName = detectBrowserRouteFieldName(original);
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            private String browserFieldName = "g";
            private String browserViewFieldName = "h";

            @Override
            public FieldVisitor visitField(
                    int access, String name, String descriptor, String signature, Object value) {
                if ("Lcom/teamdev/jxbrowser/browser/Browser;".equals(descriptor)) {
                    browserFieldName = name;
                } else if ("Lcom/teamdev/jxbrowser/view/swing/BrowserView;".equals(descriptor)) {
                    browserViewFieldName = name;
                }
                return super.visitField(access, name, descriptor, signature, value);
            }

            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                if ("a".equals(name)
                        && "(Lcom/teamdev/jxbrowser/navigation/event/NavigationFinished;)V"
                                .equals(descriptor)) {
                    result.patchedJxBrowserDiagnostics = true;
                    MethodVisitor mv =
                            super.visitMethod(access, name, descriptor, signature, exceptions);
                    writeNavigationFinishedDiagnostics(mv);
                    return null;
                }
                if ("a".equals(name)
                        && "(Lcom/teamdev/jxbrowser/navigation/internal/rpc/LoadFinished;)V"
                                .equals(descriptor)) {
                    MethodVisitor mv =
                            super.visitMethod(access, name, descriptor, signature, exceptions);
                    writeLoadFinishedDiagnostics(mv, browserFieldName);
                    return null;
                }
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                boolean isBrowserConstructor =
                        "<init>".equals(name)
                                && ("(Ljava/lang/String;Ljava/lang/String;Lcom/sbf/main/jxbrowser/l;Z)V"
                                                .equals(descriptor)
                                        || "(Ljava/lang/String;Ljava/lang/String;Lcom/sbf/main/jxbrowser/l;ZLcom/sbf/main/jxbrowser/g$a;)V"
                                                .equals(descriptor));
                if (!isBrowserConstructor) {
                    if ("c".equals(name) && "(Ljava/lang/String;)V".equals(descriptor)) {
                        return new MethodVisitor(Opcodes.ASM9, mv) {
                            @Override
                            public void visitCode() {
                                super.visitCode();
                                mv.visitVarInsn(Opcodes.ALOAD, 0);
                                mv.visitVarInsn(Opcodes.ALOAD, 1);
                                mv.visitMethodInsn(
                                        Opcodes.INVOKESTATIC,
                                        "com/sbf/main/jxbrowser/M8WhatsAppNativeProfiles",
                                        "registerHost",
                                        "(Lcom/sbf/main/jxbrowser/c;Ljava/lang/String;)V",
                                        false);
                            }

                            @Override
                            public void visitFieldInsn(
                                    int opcode, String owner, String fieldName, String fieldDescriptor) {
                                super.visitFieldInsn(opcode, owner, fieldName, fieldDescriptor);
                                if (opcode == Opcodes.PUTFIELD
                                        && "com/sbf/main/jxbrowser/c".equals(owner)
                                        && browserFieldName.equals(fieldName)
                                        && "Lcom/teamdev/jxbrowser/browser/Browser;"
                                                .equals(fieldDescriptor)) {
                                    emitBrowserCreated(this, browserFieldName);
                                    emitInstallWebDiagnostics(this, browserFieldName);
                                }
                            }
                        };
                    }
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override
                        public void visitFieldInsn(
                                int opcode, String owner, String fieldName, String fieldDescriptor) {
                            super.visitFieldInsn(opcode, owner, fieldName, fieldDescriptor);
                            if (opcode == Opcodes.PUTFIELD
                                    && "com/sbf/main/jxbrowser/c".equals(owner)
                                    && browserFieldName.equals(fieldName)
                                    && "Lcom/teamdev/jxbrowser/browser/Browser;"
                                            .equals(fieldDescriptor)) {
                                emitBrowserCreated(this, browserFieldName);
                                emitInstallWebDiagnostics(this, browserFieldName);
                            }
                        }
                    };
                }
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        emitStringBuilderPrint(
                                this,
                                "M4_V13_BROWSER_CONSTRUCTOR url=",
                                Opcodes.ALOAD,
                                2,
                                "java/lang/StringBuilder",
                                "append",
                                "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
                    }

                    @Override
                    public void visitFieldInsn(
                            int opcode, String owner, String fieldName, String fieldDescriptor) {
                        super.visitFieldInsn(opcode, owner, fieldName, fieldDescriptor);
                        if (opcode == Opcodes.PUTFIELD
                                && "com/sbf/main/jxbrowser/c".equals(owner)
                                && browserFieldName.equals(fieldName)
                                && "Lcom/teamdev/jxbrowser/browser/Browser;"
                                        .equals(fieldDescriptor)) {
                            emitBrowserCreated(this, browserFieldName);
                            emitInstallWebDiagnostics(this, browserFieldName);
                        }
                    }
                };
            }

            @Override
            public void visitEnd() {
                writeBitmapCaptureMethod(this);
                writeBrowserLayoutDiagnostics(this, browserViewFieldName);
                writeM8AttachBrowserViewMethod(this);
                writeM8WhatsAppNativeProfileMethods(this);
                writeInstallWebDiagnosticsMethod(this);
                super.visitEnd();
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static byte[] patchJxBrowserViewAttachDispatch(byte[] original, PatchResult result) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = computeFramesWriter(reader);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            private String outerFieldName = "a";

            @Override
            public FieldVisitor visitField(
                    int access, String name, String descriptor, String signature, Object value) {
                if ("Lcom/sbf/main/jxbrowser/c;".equals(descriptor)) {
                    outerFieldName = name;
                }
                return super.visitField(access, name, descriptor, signature, value);
            }

            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"run".equals(name) || !"()V".equals(descriptor)) {
                    return mv;
                }
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitInsn(int opcode) {
                        if (opcode == Opcodes.RETURN) {
                            result.patchedJxBrowserViewAttach = true;
                            visitVarInsn(Opcodes.ALOAD, 0);
                            visitFieldInsn(
                                    Opcodes.GETFIELD,
                                    "com/sbf/main/jxbrowser/c$3",
                                    outerFieldName,
                                    "Lcom/sbf/main/jxbrowser/c;");
                            visitMethodInsn(
                                    Opcodes.INVOKEVIRTUAL,
                                    "com/sbf/main/jxbrowser/c",
                                    "m8AttachBrowserView",
                                    "()V",
                                    false);
                        }
                        super.visitInsn(opcode);
                    }
                };
            }
        };
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private static byte[] patchJxBrowserReadyLoadDispatch(byte[] original, PatchResult result) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = computeFramesWriter(reader);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"run".equals(name) || !"()V".equals(descriptor)) {
                    return mv;
                }
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitMethodInsn(
                            int opcode,
                            String owner,
                            String methodName,
                            String methodDescriptor,
                            boolean isInterface) {
                        if (opcode == Opcodes.INVOKEINTERFACE
                                && "com/teamdev/jxbrowser/navigation/Navigation".equals(owner)
                                && "loadUrl".equals(methodName)
                                && "(Ljava/lang/String;)V".equals(methodDescriptor)) {
                            result.patchedJxBrowserLoadDiagnostics = true;
                            visitInsn(Opcodes.POP2);
                            visitVarInsn(Opcodes.ALOAD, 0);
                            visitFieldInsn(
                                    Opcodes.GETFIELD,
                                    "com/sbf/main/jxbrowser/c$4",
                                    "a",
                                    "Lcom/sbf/main/jxbrowser/c;");
                            visitVarInsn(Opcodes.ALOAD, 1);
                            visitMethodInsn(
                                    Opcodes.INVOKEVIRTUAL,
                                    "com/sbf/main/jxbrowser/c",
                                    "c",
                                    "(Ljava/lang/String;)V",
                                    false);
                            return;
                        }
                        super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                    }
                };
            }
        };
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private static String detectBrowserRouteFieldName(byte[] original) {
        ClassNode node = new ClassNode();
        new ClassReader(original).accept(node, 0);
        for (MethodNode method : node.methods) {
            if (!"<init>".equals(method.name)
                    || !("(Ljava/lang/String;Ljava/lang/String;Lcom/sbf/main/jxbrowser/l;Z)V"
                                    .equals(method.desc)
                            || "(Ljava/lang/String;Ljava/lang/String;Lcom/sbf/main/jxbrowser/l;ZLcom/sbf/main/jxbrowser/g$a;)V"
                                    .equals(method.desc))) {
                continue;
            }
            String fieldName = fieldAssignedFromSecondConstructorString(method.instructions);
            if (fieldName != null) {
                return fieldName;
            }
        }
        for (MethodNode method : node.methods) {
            if ((method.access & Opcodes.ACC_STATIC) == 0
                    || !"(Lcom/sbf/main/jxbrowser/c;)Ljava/lang/String;".equals(method.desc)) {
                continue;
            }
            for (AbstractInsnNode insn = method.instructions.getFirst();
                    insn != null;
                    insn = insn.getNext()) {
                if (insn.getOpcode() == Opcodes.GETFIELD && insn instanceof FieldInsnNode) {
                    FieldInsnNode field = (FieldInsnNode) insn;
                    if ("com/sbf/main/jxbrowser/c".equals(field.owner)
                            && "Ljava/lang/String;".equals(field.desc)) {
                        return field.name;
                    }
                }
            }
        }
        return null;
    }

    private static String fieldAssignedFromSecondConstructorString(InsnList instructions) {
        for (AbstractInsnNode insn = instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() != Opcodes.ALOAD || !(insn instanceof VarInsnNode)) {
                continue;
            }
            VarInsnNode loadThis = (VarInsnNode) insn;
            if (loadThis.var != 0) {
                continue;
            }
            AbstractInsnNode loadRoute = nextRealInsn(insn.getNext());
            if (loadRoute == null
                    || loadRoute.getOpcode() != Opcodes.ALOAD
                    || !(loadRoute instanceof VarInsnNode)
                    || ((VarInsnNode) loadRoute).var != 2) {
                continue;
            }
            AbstractInsnNode putField = nextRealInsn(loadRoute.getNext());
            if (putField == null
                    || putField.getOpcode() != Opcodes.PUTFIELD
                    || !(putField instanceof FieldInsnNode)) {
                continue;
            }
            FieldInsnNode field = (FieldInsnNode) putField;
            if ("com/sbf/main/jxbrowser/c".equals(field.owner)
                    && "Ljava/lang/String;".equals(field.desc)) {
                return field.name;
            }
        }
        return null;
    }

    private static AbstractInsnNode nextRealInsn(AbstractInsnNode insn) {
        while (insn != null
                && (insn.getType() == AbstractInsnNode.LABEL
                        || insn.getType() == AbstractInsnNode.LINE
                        || insn.getType() == AbstractInsnNode.FRAME)) {
            insn = insn.getNext();
        }
        return insn;
    }

    private static void emitLoadCreatedBrowserRoute(MethodVisitor mv, String routeFieldName) {
        if (routeFieldName == null) {
            return;
        }
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(
                Opcodes.GETFIELD,
                "com/sbf/main/jxbrowser/c",
                routeFieldName,
                "Ljava/lang/String;");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/sbf/main/jxbrowser/c",
                "c",
                "(Ljava/lang/String;)V",
                false);
    }

    private static void writeNavigationFinishedDiagnostics(MethodVisitor mv) {
        mv.visitCode();
        org.objectweb.asm.Label success = new org.objectweb.asm.Label();
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/navigation/event/NavigationFinished",
                "isErrorPage",
                "()Z",
                true);
        mv.visitJumpInsn(Opcodes.IFEQ, success);
        emitNavigationResult(mv, "M4_V13_LOAD_FAILED url=");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitLabel(success);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        emitNavigationResult(mv, "M4_V13_NAV_FINISHED url=");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 2);
        mv.visitEnd();
    }

    private static void emitNavigationResult(MethodVisitor mv, String prefix) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn(prefix);
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/StringBuilder",
                "<init>",
                "(Ljava/lang/String;)V",
                false);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/navigation/event/NavigationFinished",
                "url",
                "()Ljava/lang/String;",
                true);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                false);
        mv.visitLdcInsn(" error=");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                false);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/navigation/event/NavigationFinished",
                "error",
                "()Lcom/teamdev/jxbrowser/net/NetError;",
                true);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/Object;)Ljava/lang/StringBuilder;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "toString",
                "()Ljava/lang/String;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/io/PrintStream",
                "println",
                "(Ljava/lang/String;)V",
                false);
    }

    private static void writeLoadFinishedDiagnostics(MethodVisitor mv, String browserFieldName) {
        mv.visitCode();
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("M4_V13_LOAD_FINISHED url=");
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/StringBuilder",
                "<init>",
                "(Ljava/lang/String;)V",
                false);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(
                Opcodes.GETFIELD,
                "com/sbf/main/jxbrowser/c",
                browserFieldName,
                "Lcom/teamdev/jxbrowser/browser/Browser;");
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/browser/Browser",
                "url",
                "()Ljava/lang/String;",
                true);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "toString",
                "()Ljava/lang/String;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/io/PrintStream",
                "println",
                "(Ljava/lang/String;)V",
                false);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(
                Opcodes.GETFIELD,
                "com/sbf/main/jxbrowser/c",
                browserFieldName,
                "Lcom/teamdev/jxbrowser/browser/Browser;");
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/jxbrowser/c",
                "m4CaptureBitmap",
                "(Lcom/teamdev/jxbrowser/browser/Browser;)V",
                false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 2);
        mv.visitEnd();
    }

    private static void writeBitmapCaptureMethod(ClassVisitor visitor) {
        MethodVisitor mv =
                visitor.visitMethod(
                        Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                        "m4CaptureBitmap",
                        "(Lcom/teamdev/jxbrowser/browser/Browser;)V",
                        null,
                        null);
        org.objectweb.asm.Label start = new org.objectweb.asm.Label();
        org.objectweb.asm.Label end = new org.objectweb.asm.Label();
        org.objectweb.asm.Label handler = new org.objectweb.asm.Label();
        mv.visitTryCatchBlock(start, end, handler, "java/lang/Throwable");
        mv.visitCode();
        mv.visitLabel(start);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/browser/Browser",
                "bitmap",
                "()Lcom/teamdev/jxbrowser/ui/Bitmap;",
                true);
        mv.visitVarInsn(Opcodes.ASTORE, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/ui/Bitmap",
                "size",
                "()Lcom/teamdev/jxbrowser/ui/Size;",
                true);
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/teamdev/jxbrowser/view/swing/graphics/BitmapImage",
                "toToolkit",
                "(Lcom/teamdev/jxbrowser/ui/Bitmap;)Ljava/awt/image/BufferedImage;",
                false);
        mv.visitVarInsn(Opcodes.ASTORE, 3);
        mv.visitTypeInsn(Opcodes.NEW, "java/io/File");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("C:\\m2dump\\m4-jxb-capture.png");
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/io/File",
                "<init>",
                "(Ljava/lang/String;)V",
                false);
        mv.visitVarInsn(Opcodes.ASTORE, 4);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitLdcInsn("png");
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "javax/imageio/ImageIO",
                "write",
                "(Ljava/awt/image/RenderedImage;Ljava/lang/String;Ljava/io/File;)Z",
                false);
        mv.visitInsn(Opcodes.POP);
        mv.visitFieldInsn(
                Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("M4_V14_CAPTURE size=");
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/StringBuilder",
                "<init>",
                "(Ljava/lang/String;)V",
                false);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/Object;)Ljava/lang/StringBuilder;",
                false);
        mv.visitLdcInsn(" pixelBytes=");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                false);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/ui/Bitmap",
                "pixels",
                "()[B",
                true);
        mv.visitInsn(Opcodes.ARRAYLENGTH);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(I)Ljava/lang/StringBuilder;",
                false);
        mv.visitLdcInsn(" path=");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                false);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/io/File",
                "getAbsolutePath",
                "()Ljava/lang/String;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                false);
        mv.visitLdcInsn(" pngBytes=");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                false);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL, "java/io/File", "length", "()J", false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(J)Ljava/lang/StringBuilder;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "toString",
                "()Ljava/lang/String;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/io/PrintStream",
                "println",
                "(Ljava/lang/String;)V",
                false);
        mv.visitLabel(end);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitLabel(handler);
        mv.visitFrame(
                Opcodes.F_FULL,
                1,
                new Object[] {"com/teamdev/jxbrowser/browser/Browser"},
                1,
                new Object[] {"java/lang/Throwable"});
        mv.visitVarInsn(Opcodes.ASTORE, 1);
        mv.visitFieldInsn(
                Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("M4_V14_CAPTURE_FAILED ");
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/StringBuilder",
                "<init>",
                "(Ljava/lang/String;)V",
                false);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/Object;)Ljava/lang/StringBuilder;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "toString",
                "()Ljava/lang/String;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/io/PrintStream",
                "println",
                "(Ljava/lang/String;)V",
                false);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitFieldInsn(
                Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/Throwable",
                "printStackTrace",
                "(Ljava/io/PrintStream;)V",
                false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void writeBrowserLayoutDiagnostics(ClassVisitor visitor, String browserViewFieldName) {
        MethodVisitor mv = visitor.visitMethod(Opcodes.ACC_PUBLIC, "doLayout", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL, "com/sbf/ui/i", "doLayout", "()V", false);
        emitBrowserViewRef(mv, browserViewFieldName);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 1);
        mv.visitEnd();
    }

    private static void writeM8AttachBrowserViewMethod(ClassVisitor visitor) {
        MethodVisitor mv =
                visitor.visitMethod(
                        Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                        "m8AttachBrowserView",
                        "()V",
                        null,
                        null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/jxbrowser/M8WhatsAppNativeProfiles",
                "registerCandidate",
                "(Lcom/sbf/main/jxbrowser/c;)V",
                false);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "com/sbf/main/jxbrowser/c",
                "j",
                "()V",
                false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    private static void writeM8WhatsAppNativeProfileMethods(ClassVisitor visitor) {
        MethodVisitor mv =
                visitor.visitMethod(
                        Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                        "m8SwitchActiveWhatsAppProfile",
                        "(Ljava/lang/String;)Ljava/lang/String;",
                        null,
                        null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/jxbrowser/M8WhatsAppNativeProfiles",
                "switchProfile",
                "(Ljava/lang/String;)Ljava/lang/String;",
                false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv =
                visitor.visitMethod(
                        Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
                        "m8EnsureWhatsAppProfileBrowser",
                        "(Ljava/lang/String;)Lcom/teamdev/jxbrowser/browser/Browser;",
                        null,
                        new String[] {"java/lang/Exception"});
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/jxbrowser/M8WhatsAppNativeProfiles",
                "ensureProfileBrowser",
                "(Lcom/sbf/main/jxbrowser/c;Ljava/lang/String;)"
                        + "Lcom/teamdev/jxbrowser/browser/Browser;",
                false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv =
                visitor.visitMethod(
                        Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
                        "m8AttachWhatsAppProfileView",
                        "(Ljava/lang/String;)V",
                        null,
                        new String[] {"java/lang/Exception"});
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/jxbrowser/M8WhatsAppNativeProfiles",
                "attachProfileView",
                "(Lcom/sbf/main/jxbrowser/c;Ljava/lang/String;)V",
                false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }

    private static void emitInstallWebDiagnostics(MethodVisitor mv, String browserFieldName) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(
                Opcodes.GETFIELD,
                "com/sbf/main/jxbrowser/c",
                browserFieldName,
                "Lcom/teamdev/jxbrowser/browser/Browser;");
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/jxbrowser/c",
                "m5InstallWebDiagnostics",
                "(Lcom/teamdev/jxbrowser/browser/Browser;)V",
                false);
    }

    private static void writeInstallWebDiagnosticsMethod(ClassVisitor visitor) {
        MethodVisitor mv =
                visitor.visitMethod(
                        Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                        "m5InstallWebDiagnostics",
                        "(Lcom/teamdev/jxbrowser/browser/Browser;)V",
                        null,
                        null);
        org.objectweb.asm.Label start = new org.objectweb.asm.Label();
        org.objectweb.asm.Label end = new org.objectweb.asm.Label();
        org.objectweb.asm.Label handler = new org.objectweb.asm.Label();
        mv.visitTryCatchBlock(start, end, handler, "java/lang/Throwable");
        mv.visitCode();
        mv.visitLabel(start);
        emitStringBuilderPrint(
                mv,
                "M5_V20_WEB_DIAG_INSTALL browser=",
                Opcodes.ALOAD,
                0,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/Object;)Ljava/lang/StringBuilder;");
        emitStringBuilderPrint(
                mv,
                "M5_V23_JS_HOOK_INSTALL browser=",
                Opcodes.ALOAD,
                0,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/Object;)Ljava/lang/StringBuilder;");
        emitPrint(mv, "M5D8_LOCAL_WEB_ASSET_ADD_SCHEME_ACTIVE");
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitLdcInsn(org.objectweb.asm.Type.getType(
                "Lcom/teamdev/jxbrowser/browser/callback/InjectJsCallback;"));
        mv.visitTypeInsn(Opcodes.NEW, "com/sbf/main/jxbrowser/M5InjectJsCallback");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "com/sbf/main/jxbrowser/M5InjectJsCallback",
                "<init>",
                "()V",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/browser/Browser",
                "set",
                "(Ljava/lang/Class;Lcom/teamdev/jxbrowser/callback/Callback;)"
                        + "Lcom/teamdev/jxbrowser/callback/Callback;",
                true);
        mv.visitInsn(Opcodes.POP);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitLdcInsn(org.objectweb.asm.Type.getType(
                "Lcom/teamdev/jxbrowser/browser/event/ConsoleMessageReceived;"));
        mv.visitTypeInsn(Opcodes.NEW, "com/sbf/main/jxbrowser/M5ConsoleObserver");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "com/sbf/main/jxbrowser/M5ConsoleObserver",
                "<init>",
                "()V",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/browser/Browser",
                "on",
                "(Ljava/lang/Class;Lcom/teamdev/jxbrowser/event/Observer;)"
                        + "Lcom/teamdev/jxbrowser/event/Subscription;",
                true);
        mv.visitInsn(Opcodes.POP);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/browser/Browser",
                "profile",
                "()Lcom/teamdev/jxbrowser/profile/Profile;",
                true);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/profile/Profile",
                "network",
                "()Lcom/teamdev/jxbrowser/net/Network;",
                true);
        mv.visitLdcInsn(org.objectweb.asm.Type.getType(
                "Lcom/teamdev/jxbrowser/net/event/RequestCompleted;"));
        mv.visitTypeInsn(Opcodes.NEW, "com/sbf/main/jxbrowser/M5RequestObserver");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "com/sbf/main/jxbrowser/M5RequestObserver",
                "<init>",
                "()V",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/teamdev/jxbrowser/net/Network",
                "on",
                "(Ljava/lang/Class;Lcom/teamdev/jxbrowser/event/Observer;)"
                        + "Lcom/teamdev/jxbrowser/event/Subscription;",
                true);
        mv.visitInsn(Opcodes.POP);
        mv.visitLabel(end);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitLabel(handler);
        mv.visitFrame(
                Opcodes.F_FULL,
                1,
                new Object[] {"com/teamdev/jxbrowser/browser/Browser"},
                1,
                new Object[] {"java/lang/Throwable"});
        mv.visitVarInsn(Opcodes.ASTORE, 1);
        emitStringBuilderPrint(
                mv,
                "M5_V20_WEB_DIAG_INSTALL_FAILED ",
                Opcodes.ALOAD,
                1,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/Object;)Ljava/lang/StringBuilder;");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/Throwable",
                "printStackTrace",
                "(Ljava/io/PrintStream;)V",
                false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void emitBrowserViewRef(MethodVisitor mv, String browserViewFieldName) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("M4_V13_VIEW=");
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/StringBuilder",
                "<init>",
                "(Ljava/lang/String;)V",
                false);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(
                Opcodes.GETFIELD,
                "com/sbf/main/jxbrowser/c",
                browserViewFieldName,
                "Lcom/teamdev/jxbrowser/view/swing/BrowserView;");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/Object;)Ljava/lang/StringBuilder;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "toString",
                "()Ljava/lang/String;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/io/PrintStream",
                "println",
                "(Ljava/lang/String;)V",
                false);
    }

    private static void emitStringBuilderPrint(
            MethodVisitor mv,
            String prefix,
            int loadOpcode,
            int local,
            String appendOwner,
            String appendName,
            String appendDescriptor) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn(prefix);
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/StringBuilder",
                "<init>",
                "(Ljava/lang/String;)V",
                false);
        mv.visitVarInsn(loadOpcode, local);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL, appendOwner, appendName, appendDescriptor, false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "toString",
                "()Ljava/lang/String;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/io/PrintStream",
                "println",
                "(Ljava/lang/String;)V",
                false);
    }

    private static void emitNormalizeRuntimeBusinessUrl(MethodVisitor mv, int urlLocal) {
        org.objectweb.asm.Label done = new org.objectweb.asm.Label();
        org.objectweb.asm.Label notJSinglepage = new org.objectweb.asm.Label();
        mv.visitVarInsn(Opcodes.ALOAD, urlLocal);
        mv.visitJumpInsn(Opcodes.IFNULL, done);
        mv.visitVarInsn(Opcodes.ALOAD, urlLocal);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/jxbrowser/M5LocalSpiderBridge",
                "normalizeC6CommerceRoute",
                "(Ljava/lang/String;)Ljava/lang/String;",
                false);
        mv.visitVarInsn(Opcodes.ASTORE, urlLocal);
        mv.visitVarInsn(Opcodes.ALOAD, urlLocal);
        mv.visitLdcInsn("JSinglepage:/ws/wsfilter/home");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "startsWith",
                "(Ljava/lang/String;)Z",
                false);
        org.objectweb.asm.Label notWsFilterJSinglepage = new org.objectweb.asm.Label();
        mv.visitJumpInsn(Opcodes.IFEQ, notWsFilterJSinglepage);
        mv.visitLdcInsn("/ws/wsfilter/home");
        mv.visitVarInsn(Opcodes.ASTORE, urlLocal);
        mv.visitJumpInsn(Opcodes.GOTO, notJSinglepage);
        mv.visitLabel(notWsFilterJSinglepage);
        mv.visitVarInsn(Opcodes.ALOAD, urlLocal);
        mv.visitLdcInsn("JSinglepage:/pc/aicloud/my");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "startsWith",
                "(Ljava/lang/String;)Z",
                false);
        org.objectweb.asm.Label notAiCloudJSinglepage = new org.objectweb.asm.Label();
        mv.visitJumpInsn(Opcodes.IFEQ, notAiCloudJSinglepage);
        mv.visitLdcInsn("/pc/aicloud/my");
        mv.visitVarInsn(Opcodes.ASTORE, urlLocal);
        mv.visitJumpInsn(Opcodes.GOTO, notJSinglepage);
        mv.visitLabel(notAiCloudJSinglepage);
        mv.visitVarInsn(Opcodes.ALOAD, urlLocal);
        mv.visitLdcInsn("JSinglepage:/");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "startsWith",
                "(Ljava/lang/String;)Z",
                false);
        org.objectweb.asm.Label notExplicitJSinglepage = new org.objectweb.asm.Label();
        mv.visitJumpInsn(Opcodes.IFEQ, notExplicitJSinglepage);
        mv.visitVarInsn(Opcodes.ALOAD, urlLocal);
        mv.visitLdcInsn("JSinglepage:");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "length",
                "()I",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "substring",
                "(I)Ljava/lang/String;",
                false);
        mv.visitVarInsn(Opcodes.ASTORE, urlLocal);
        mv.visitJumpInsn(Opcodes.GOTO, notJSinglepage);
        mv.visitLabel(notExplicitJSinglepage);
        mv.visitVarInsn(Opcodes.ALOAD, urlLocal);
        mv.visitLdcInsn("JSinglepage");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "startsWith",
                "(Ljava/lang/String;)Z",
                false);
        mv.visitJumpInsn(Opcodes.IFEQ, notJSinglepage);
        mv.visitLdcInsn(
                "/pc/dataCollect/collectionTask?modal=whatsapp_users_lists&moduleCode=whatsapp");
        mv.visitVarInsn(Opcodes.ASTORE, urlLocal);
        mv.visitLabel(notJSinglepage);
        mv.visitVarInsn(Opcodes.ALOAD, urlLocal);
        mv.visitLdcInsn("/");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "startsWith",
                "(Ljava/lang/String;)Z",
                false);
        mv.visitJumpInsn(Opcodes.IFEQ, done);
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("https://");
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/StringBuilder",
                "<init>",
                "(Ljava/lang/String;)V",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/util/http/SBFApi",
                "c",
                "()Ljava/lang/String;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                false);
        mv.visitVarInsn(Opcodes.ALOAD, urlLocal);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "toString",
                "()Ljava/lang/String;",
                false);
        mv.visitVarInsn(Opcodes.ASTORE, urlLocal);
        mv.visitLabel(done);
    }

    private static void emitBrowserCreated(MethodVisitor mv, String browserFieldName) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("M4_V13_BROWSER_CREATED=");
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/StringBuilder",
                "<init>",
                "(Ljava/lang/String;)V",
                false);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(
                Opcodes.GETFIELD,
                "com/sbf/main/jxbrowser/c",
                browserFieldName,
                "Lcom/teamdev/jxbrowser/browser/Browser;");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/Object;)Ljava/lang/StringBuilder;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "toString",
                "()Ljava/lang/String;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/io/PrintStream",
                "println",
                "(Ljava/lang/String;)V",
                false);
    }

    private static void emitPrint(MethodVisitor mv, String message) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitLdcInsn(message);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    }

    private static void emitEvidenceJsonReturnLog(MethodVisitor mv, String logPrefix) {
        mv.visitInsn(Opcodes.DUP);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitInsn(Opcodes.SWAP);
        mv.visitLdcInsn(logPrefix);
        mv.visitInsn(Opcodes.SWAP);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/String",
                "valueOf",
                "(Ljava/lang/Object;)Ljava/lang/String;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "concat",
                "(Ljava/lang/String;)Ljava/lang/String;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/io/PrintStream",
                "println",
                "(Ljava/lang/String;)V",
                false);
    }

    private static void emitStringLocalLog(MethodVisitor mv, String logPrefix, int varIndex) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn(logPrefix);
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/StringBuilder",
                "<init>",
                "(Ljava/lang/String;)V",
                false);
        mv.visitVarInsn(Opcodes.ALOAD, varIndex);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/String",
                "valueOf",
                "(Ljava/lang/Object;)Ljava/lang/String;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "toString",
                "()Ljava/lang/String;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/io/PrintStream",
                "println",
                "(Ljava/lang/String;)V",
                false);
    }

    private static void emitStaticFieldLog(
            MethodVisitor mv,
            String logPrefix,
            String owner,
            String fieldName,
            String fieldDescriptor) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn(logPrefix);
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/StringBuilder",
                "<init>",
                "(Ljava/lang/String;)V",
                false);
        mv.visitFieldInsn(Opcodes.GETSTATIC, owner, fieldName, fieldDescriptor);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/String",
                "valueOf",
                "(Ljava/lang/Object;)Ljava/lang/String;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "toString",
                "()Ljava/lang/String;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/io/PrintStream",
                "println",
                "(Ljava/lang/String;)V",
                false);
    }

    private static void emitCallerStack(MethodVisitor mv) {
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/Exception");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("M4_DIAG_MENU_K_CALLER");
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL, "java/lang/Exception", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/Exception",
                "printStackTrace",
                "(Ljava/io/PrintStream;)V",
                false);
    }

    private static void emitModernDispatchDiagnostics(MethodVisitor mv) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("M4_V12_DISPATCH name=");
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
        appendModernString(mv, "e");
        appendLiteral(mv, " id=");
        appendModernInt(mv, "f");
        appendLiteral(mv, " code=");
        appendModernString(mv, "g");
        appendLiteral(mv, " localCode=");
        appendModernString(mv, "h");
        appendLiteral(mv, " linkUrl=");
        appendModernString(mv, "i");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    }

    private static void emitModernCollectTabJxBrowserUrlFix(MethodVisitor mv) {
        org.objectweb.asm.Label done = new org.objectweb.asm.Label();
        org.objectweb.asm.Label notCollectTab = new org.objectweb.asm.Label();
        org.objectweb.asm.Label notClawTab = new org.objectweb.asm.Label();
        org.objectweb.asm.Label notSuperTab = new org.objectweb.asm.Label();
        org.objectweb.asm.Label notBigDataTab = new org.objectweb.asm.Label();
        org.objectweb.asm.Label notTelegramTab = new org.objectweb.asm.Label();
        org.objectweb.asm.Label notGeoTab = new org.objectweb.asm.Label();
        org.objectweb.asm.Label notAdvertisingTab = new org.objectweb.asm.Label();
        mv.visitVarInsn(Opcodes.ALOAD, 5);
        mv.visitLdcInsn("JSinglepage");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "equals",
                "(Ljava/lang/Object;)Z",
                false);
        mv.visitJumpInsn(Opcodes.IFEQ, done);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitJumpInsn(Opcodes.IFNULL, done);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitLdcInsn("/pc/dataCollect/collectionTask");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "startsWith",
                "(Ljava/lang/String;)Z",
                false);
        mv.visitJumpInsn(Opcodes.IFEQ, notCollectTab);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitVarInsn(Opcodes.ASTORE, 5);
        emitPrint(mv, "M5D11_COLLECT_TAB_JXBROWSER_URL_FROM_LINKURL");
        mv.visitJumpInsn(Opcodes.GOTO, done);
        mv.visitLabel(notCollectTab);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitLdcInsn("/wsClaw/");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "startsWith",
                "(Ljava/lang/String;)Z",
                false);
        mv.visitJumpInsn(Opcodes.IFEQ, notClawTab);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitVarInsn(Opcodes.ASTORE, 5);
        emitPrint(mv, "M8B_WSCLAW_TAB_JXBROWSER_URL_FROM_LINKURL");
        mv.visitJumpInsn(Opcodes.GOTO, done);
        mv.visitLabel(notClawTab);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitLdcInsn("/pc/sender/senderGlobalControls/mysuperenvironment");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "startsWith",
                "(Ljava/lang/String;)Z",
                false);
        mv.visitJumpInsn(Opcodes.IFEQ, notSuperTab);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitVarInsn(Opcodes.ASTORE, 5);
        emitPrint(mv, "M8B_SUPER_ENV_TAB_JXBROWSER_URL_FROM_LINKURL");
        mv.visitJumpInsn(Opcodes.GOTO, done);
        mv.visitLabel(notSuperTab);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitLdcInsn("/es/bigData/bigDataTask");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "startsWith",
                "(Ljava/lang/String;)Z",
                false);
        mv.visitJumpInsn(Opcodes.IFEQ, notBigDataTab);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitVarInsn(Opcodes.ASTORE, 5);
        emitPrint(mv, "C5_PLATFORM_TAB_JXBROWSER_URL_FROM_LINKURL");
        mv.visitJumpInsn(Opcodes.GOTO, done);
        mv.visitLabel(notBigDataTab);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitLdcInsn("/pc/tg/index");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "startsWith",
                "(Ljava/lang/String;)Z",
                false);
        mv.visitJumpInsn(Opcodes.IFEQ, notTelegramTab);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitVarInsn(Opcodes.ASTORE, 5);
        emitPrint(mv, "C5_PLATFORM_TAB_JXBROWSER_URL_FROM_LINKURL");
        mv.visitJumpInsn(Opcodes.GOTO, done);
        mv.visitLabel(notTelegramTab);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitLdcInsn("/pc/dataCollect/googleseo");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "startsWith",
                "(Ljava/lang/String;)Z",
                false);
        mv.visitJumpInsn(Opcodes.IFEQ, notGeoTab);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitVarInsn(Opcodes.ASTORE, 5);
        emitPrint(mv, "C5_PLATFORM_TAB_JXBROWSER_URL_FROM_LINKURL");
        mv.visitJumpInsn(Opcodes.GOTO, done);
        mv.visitLabel(notGeoTab);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitLdcInsn("/views/overseasAds/");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "startsWith",
                "(Ljava/lang/String;)Z",
                false);
        mv.visitJumpInsn(Opcodes.IFEQ, notAdvertisingTab);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitVarInsn(Opcodes.ASTORE, 5);
        emitPrint(mv, "C67_ADVERTISING_TAB_JXBROWSER_URL_FROM_LINKURL");
        mv.visitJumpInsn(Opcodes.GOTO, done);
        mv.visitLabel(notAdvertisingTab);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitLdcInsn("/pc/kefu/conversation");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "startsWith",
                "(Ljava/lang/String;)Z",
                false);
        mv.visitJumpInsn(Opcodes.IFEQ, done);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitVarInsn(Opcodes.ASTORE, 5);
        emitPrint(mv, "C5_PLATFORM_TAB_JXBROWSER_URL_FROM_LINKURL");
        mv.visitLabel(done);
    }

    private static void emitModernMenuMouseDiagnostics(MethodVisitor mv, String marker) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn(marker + " name=");
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
        appendModernMouseString(mv, "e");
        appendLiteral(mv, " id=");
        appendModernMouseInt(mv, "f");
        appendLiteral(mv, " code=");
        appendModernMouseString(mv, "g");
        appendLiteral(mv, " localCode=");
        appendModernMouseString(mv, "h");
        appendLiteral(mv, " linkUrl=");
        appendModernMouseString(mv, "i");
        appendLiteral(mv, " hasChildren=");
        appendModernMouseBoolean(mv, "k");
        appendLiteral(mv, " treeEndFlg=");
        appendModernMouseBoolean(mv, "l");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    }

    private static void emitSideMenuDiagnostics(MethodVisitor mv, String marker, String listenerClass) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn(marker + " name=");
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
        appendSideMenuName(mv, listenerClass);
        appendLiteral(mv, " id=");
        appendSideMenuInt(mv, listenerClass, "c");
        appendLiteral(mv, " code=");
        appendSideMenuString(mv, listenerClass, "d");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    }

    private static void emitTreeDiagnostics(MethodVisitor mv) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("M4_DIAG_TREE_INIT raw=");
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/Object;)Ljava/lang/StringBuilder;",
                false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);

        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("M4_DIAG_TREE_FIELDS id=");
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
        appendJsonInt(mv, "id");
        appendLiteral(mv, " parentId=");
        appendJsonInt(mv, "parentId");
        appendLiteral(mv, " localCode=");
        appendJsonString(mv, "localCode");
        appendLiteral(mv, " code=");
        appendJsonString(mv, "code");
        appendLiteral(mv, " linkUrl=");
        appendJsonString(mv, "linkUrl");
        appendLiteral(mv, " treeEndFlg=");
        appendJsonInt(mv, "treeEndFlg");
        appendLiteral(mv, " webFlg=");
        appendJsonInt(mv, "webFlg");
        appendLiteral(mv, " displayIndex=");
        appendJsonInt(mv, "displayIndex");
        appendLiteral(mv, " perms=");
        appendJsonString(mv, "perms");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    }

    private static void emitDispatchEnter(MethodVisitor mv) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("M4_DIAG_DISPATCH_ENTER id=");
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
        appendTreeInt(mv, "e");
        appendLiteral(mv, " parentId=");
        appendTreeInt(mv, "f");
        appendLiteral(mv, " code=");
        appendTreeString(mv, "g");
        appendLiteral(mv, " localCode=");
        appendTreeString(mv, "h");
        appendLiteral(mv, " linkUrl=");
        appendTreeString(mv, "l");
        appendLiteral(mv, " webFlg=");
        appendTreeBoolean(mv, "m");
        appendLiteral(mv, " treeEndFlg=");
        appendTreeBoolean(mv, "n");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    }

    private static void appendLiteral(MethodVisitor mv, String value) {
        mv.visitLdcInsn(value);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                false);
    }

    private static void appendJsonInt(MethodVisitor mv, String key) {
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitLdcInsn(key);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL, "org/json/JSONObject", "optInt", "(Ljava/lang/String;)I", false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false);
    }

    private static void appendJsonString(MethodVisitor mv, String key) {
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitLdcInsn(key);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "org/json/JSONObject",
                "optString",
                "(Ljava/lang/String;)Ljava/lang/String;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                false);
    }

    private static void appendTreeInt(MethodVisitor mv, String methodName) {
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/sbf/main/tree/i", methodName, "()I", false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false);
    }

    private static void appendTreeString(MethodVisitor mv, String methodName) {
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL, "com/sbf/main/tree/i", methodName, "()Ljava/lang/String;", false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                false);
    }

    private static void appendTreeBoolean(MethodVisitor mv, String methodName) {
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/sbf/main/tree/i", methodName, "()Z", false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Z)Ljava/lang/StringBuilder;", false);
    }

    private static void appendModernString(MethodVisitor mv, String methodName) {
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitTypeInsn(Opcodes.CHECKCAST, "com/sbf/main/ext/j2026/h");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/sbf/main/ext/j2026/h",
                methodName,
                "()Ljava/lang/String;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                false);
    }

    private static void appendModernInt(MethodVisitor mv, String methodName) {
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitTypeInsn(Opcodes.CHECKCAST, "com/sbf/main/ext/j2026/h");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL, "com/sbf/main/ext/j2026/h", methodName, "()I", false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false);
    }

    private static void appendModernMouseString(MethodVisitor mv, String methodName) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(
                Opcodes.GETFIELD, "com/sbf/main/ext/j2026/h$2", "a", "Lcom/sbf/main/ext/j2026/h;");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/sbf/main/ext/j2026/h",
                methodName,
                "()Ljava/lang/String;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                false);
    }

    private static void appendModernMouseInt(MethodVisitor mv, String methodName) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(
                Opcodes.GETFIELD, "com/sbf/main/ext/j2026/h$2", "a", "Lcom/sbf/main/ext/j2026/h;");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL, "com/sbf/main/ext/j2026/h", methodName, "()I", false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false);
    }

    private static void appendModernMouseBoolean(MethodVisitor mv, String methodName) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(
                Opcodes.GETFIELD, "com/sbf/main/ext/j2026/h$2", "a", "Lcom/sbf/main/ext/j2026/h;");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL, "com/sbf/main/ext/j2026/h", methodName, "()Z", false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Z)Ljava/lang/StringBuilder;", false);
    }

    private static void loadSideMenuOwner(MethodVisitor mv, String listenerClass) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(
                Opcodes.GETFIELD, listenerClass, "a", "Lcom/sbf/main/ext/j2026/d;");
    }

    private static void appendSideMenuName(MethodVisitor mv, String listenerClass) {
        loadSideMenuOwner(mv, listenerClass);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/sbf/main/ext/j2026/d",
                "getName",
                "()Ljava/lang/String;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                false);
    }

    private static void appendSideMenuString(MethodVisitor mv, String listenerClass, String methodName) {
        loadSideMenuOwner(mv, listenerClass);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/sbf/main/ext/j2026/d",
                methodName,
                "()Ljava/lang/String;",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                false);
    }

    private static void appendSideMenuInt(MethodVisitor mv, String listenerClass, String methodName) {
        loadSideMenuOwner(mv, listenerClass);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL, "com/sbf/main/ext/j2026/d", methodName, "()I", false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false);
    }

    private static final class PatchResult {
        boolean patchedLogin;
        boolean patchedGetInfo;
        boolean patchedProductModules;
        boolean patchedPcMenus;
        boolean patchedSpiderModules;
        boolean patchedLocalSpiderGetNewTask;
        boolean patchedLocalSpiderCancelAllRun;
        boolean patchedUpdateChecker;
        boolean patchedTreeDiagnostics;
        boolean patchedMenuDispatchDiagnostics;
        boolean patchedModernMenuDispatchDiagnostics;
        boolean patchedModernMenuMouseDiagnostics;
        boolean patchedSideMenuMouseDiagnostics;
        boolean patchedSideMenuCallbackDiagnostics;
        boolean patchedStartAppExeDiagBootstrap;
        boolean patchedStartAppWebTokenBridge;
        boolean patchedStartAppLoginDisposeGuard;
        boolean patchedProductSelectorEnterBridge;
        boolean patchedStartAppAutoLogin;
        boolean patchedTrueExeLoginBridge;
        boolean patchedDelayedTrueExeLoginBridge;
        boolean patchedMiJavaDictBridge;
        boolean patchedLocalSpiderTaskGet;
        boolean patchedLocalSpiderTaskStatus;
        boolean patchedJxBrowserDiagnostics;
        boolean patchedJxBrowserLoadDiagnostics;
        boolean patchedJxBrowserViewAttach;
        boolean patchedJxBrowserEngine;
        boolean patchedLocalWebSchemeCallback;
        boolean patchedGoogleCRHelper;
        boolean patchedSpiderCallbackPostData;
        boolean patchedSpiderCallbackEndTask;
        boolean addedM5ConsoleObserver;
        boolean addedM5AuthBootstrapCallback;
        boolean addedM5InjectJsCallback;
        boolean addedM5LocalSpiderBridge;
        boolean addedM8WhatsAppNativeProfiles;
        boolean addedM8WhatsAppExternalBrowsers;
        boolean addedM8WhatsAppDripCampaigns;
        boolean addedM5RequestObserver;
        boolean addedM5YesCaptchaBridge;
        boolean addedM8D7DefaultMenuDispatch;
        boolean addedM8D14ExeDiag;

        String missingFlags() {
            StringBuilder missing = new StringBuilder();
            appendMissing(missing, "patchedLogin", patchedLogin);
            appendMissing(missing, "patchedGetInfo", patchedGetInfo);
            appendMissing(missing, "patchedProductModules", patchedProductModules);
            appendMissing(missing, "patchedPcMenus", patchedPcMenus);
            appendMissing(missing, "patchedSpiderModules", patchedSpiderModules);
            appendMissing(missing, "patchedLocalSpiderGetNewTask", patchedLocalSpiderGetNewTask);
            appendMissing(missing, "patchedLocalSpiderCancelAllRun", patchedLocalSpiderCancelAllRun);
            appendMissing(missing, "patchedUpdateChecker", patchedUpdateChecker);
            appendMissing(missing, "patchedTreeDiagnostics", patchedTreeDiagnostics);
            appendMissing(missing, "patchedMenuDispatchDiagnostics", patchedMenuDispatchDiagnostics);
            appendMissing(missing, "patchedModernMenuDispatchDiagnostics", patchedModernMenuDispatchDiagnostics);
            appendMissing(missing, "patchedModernMenuMouseDiagnostics", patchedModernMenuMouseDiagnostics);
            appendMissing(missing, "patchedSideMenuMouseDiagnostics", patchedSideMenuMouseDiagnostics);
            appendMissing(missing, "patchedSideMenuCallbackDiagnostics", patchedSideMenuCallbackDiagnostics);
            appendMissing(missing, "patchedStartAppExeDiagBootstrap", patchedStartAppExeDiagBootstrap);
            appendMissing(missing, "patchedStartAppWebTokenBridge", patchedStartAppWebTokenBridge);
            appendMissing(missing, "patchedStartAppLoginDisposeGuard", patchedStartAppLoginDisposeGuard);
            appendMissing(missing, "patchedProductSelectorEnterBridge", patchedProductSelectorEnterBridge);
            appendMissing(missing, "patchedStartAppAutoLogin", patchedStartAppAutoLogin);
            appendMissing(missing, "patchedTrueExeLoginBridge", patchedTrueExeLoginBridge);
            appendMissing(missing, "patchedDelayedTrueExeLoginBridge", patchedDelayedTrueExeLoginBridge);
            appendMissing(missing, "patchedJxBrowserDiagnostics", patchedJxBrowserDiagnostics);
            appendMissing(missing, "patchedJxBrowserLoadDiagnostics", patchedJxBrowserLoadDiagnostics);
            appendMissing(missing, "patchedJxBrowserViewAttach", patchedJxBrowserViewAttach);
            appendMissing(missing, "patchedJxBrowserEngine", patchedJxBrowserEngine);
            appendMissing(missing, "patchedLocalWebSchemeCallback", patchedLocalWebSchemeCallback);
            appendMissing(missing, "patchedGoogleCRHelper", patchedGoogleCRHelper);
            appendMissing(missing, "patchedMiJavaDictBridge", patchedMiJavaDictBridge);
            appendMissing(missing, "patchedLocalSpiderTaskGet", patchedLocalSpiderTaskGet);
            appendMissing(missing, "patchedLocalSpiderTaskStatus", patchedLocalSpiderTaskStatus);
            appendMissing(missing, "addedM5ConsoleObserver", addedM5ConsoleObserver);
            appendMissing(missing, "addedM5AuthBootstrapCallback", addedM5AuthBootstrapCallback);
            appendMissing(missing, "addedM5InjectJsCallback", addedM5InjectJsCallback);
            appendMissing(missing, "addedM5LocalSpiderBridge", addedM5LocalSpiderBridge);
            appendMissing(missing, "addedM8WhatsAppNativeProfiles", addedM8WhatsAppNativeProfiles);
            appendMissing(missing, "addedM8WhatsAppExternalBrowsers", addedM8WhatsAppExternalBrowsers);
            appendMissing(missing, "addedM8WhatsAppDripCampaigns", addedM8WhatsAppDripCampaigns);
            appendMissing(missing, "addedM5RequestObserver", addedM5RequestObserver);
            appendMissing(missing, "addedM5YesCaptchaBridge", addedM5YesCaptchaBridge);
            appendMissing(missing, "addedM8D7DefaultMenuDispatch", addedM8D7DefaultMenuDispatch);
            appendMissing(missing, "addedM8D14ExeDiag", addedM8D14ExeDiag);
            return missing.length() == 0 ? "<none>" : missing.toString();
        }

        private static void appendMissing(StringBuilder missing, String name, boolean patched) {
            if (!patched) {
                if (missing.length() > 0) {
                    missing.append(", ");
                }
                missing.append(name);
            }
        }
    }
}
