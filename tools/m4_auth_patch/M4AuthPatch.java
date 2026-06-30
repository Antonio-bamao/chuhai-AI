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
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

public final class M4AuthPatch {
    private static final String TARGET_CLASS = "com/sbf/util/http/SBFApi.class";
    private static final String UPDATE_CHECKER_CLASS = "com/sbf/util/http/SBFApi$5.class";
    private static final String TREE_NODE_CLASS = "com/sbf/main/tree/i.class";
    private static final String MENU_DISPATCH_CLASS = "com/sbf/main/sub/b.class";
    private static final String MODERN_MENU_DISPATCH_CLASS = "com/sbf/main/JSBFMain$4.class";
    private static final String MODERN_MENU_MOUSE_CLASS = "com/sbf/main/ext/j2026/h$2.class";
    private static final String SIDE_MENU_MOUSE_CLASS = "com/sbf/main/ext/j2026/d$2.class";
    private static final String SIDE_MENU_CALLBACK_CLASS = "com/sbf/main/ext/j2026/d$1.class";
    private static final String START_APP_CLASS = "com/sbf/main/StartApp.class";
    private static final String START_APP_LOGIN_CALLBACK_CLASS = "com/sbf/main/StartApp$1.class";
    private static final String START_APP_UI_CLASS = "com/sbf/main/StartApp$3.class";
    private static final String MIJAVA_CLASS = "com/sbf/main/jxbrowser/MiJava.class";
    private static final String JXBROWSER_CLASS = "com/sbf/main/jxbrowser/c.class";
    private static final String JXBROWSER_LOAD_THREAD_CLASS = "com/sbf/main/jxbrowser/c$3.class";
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
    private static final String M5_REQUEST_OBSERVER_CLASS =
            "com/sbf/main/jxbrowser/M5RequestObserver.class";
    private static final String GOOGLE_CR_HELPER_CLASS =
            "com/sbf/main/ext/gg/GoogleCRHelper.class";
    private static final String SPIDER_CALLBACK_CLASS =
            "com/sbf/main/cloud/spider/SpiderCallback.class";
    private static final String M5_YES_CAPTCHA_BRIDGE_CLASS =
            "com/sbf/main/ext/gg/M5YesCaptchaBridge.class";
    private static final String WEB_BRIDGE_TOKEN = "offline-local-token-1234567890";

    private static final String LOGIN_JSON =
            "{\"code\":200,\"msg\":\"offline login ok\","
                    + "\"sf\":\"41,aimirrorsystem,tiktok,HuoChaiAI,huochai-ai\","
                    + "\"data\":{"
                    + "\"token\":\"" + WEB_BRIDGE_TOKEN + "\","
                    + "\"userId\":1,\"tenantCode\":\"local\",\"nickname\":\"HuoChaiAI Local User\","
                    + "\"zone\":\"Asia/Shanghai\",\"time\":0,"
                    + "\"imConfig\":{},"
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
            "{\"code\":200,\"msg\":\"success\",\"data\":[]}";

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
        int argOffset = 0;
        if (args.length == 3 && "--real-product-menu-logging".equals(args[0])) {
            realProductMenuLogging = true;
            argOffset = 1;
        }
        if (args.length - argOffset != 2) {
            throw new IllegalArgumentException(
                    "usage: M4AuthPatch [--real-product-menu-logging] <input-jar> <output-jar>");
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
                || !result.patchedModernMenuDispatchDiagnostics
                || !result.patchedModernMenuMouseDiagnostics
                || !result.patchedSideMenuMouseDiagnostics
                || !result.patchedSideMenuCallbackDiagnostics
                || !result.patchedStartAppWebTokenBridge
                || !result.patchedStartAppLoginDisposeGuard
                || !result.patchedStartAppAutoLogin
                || !result.patchedJxBrowserDiagnostics
                || !result.patchedJxBrowserLoadDiagnostics
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
                || !result.addedM5RequestObserver
                || !result.addedM5YesCaptchaBridge
                || !result.patchedSpiderCallbackPostData
                || !result.patchedSpiderCallbackEndTask) {
            Files.deleteIfExists(temp);
            throw new IllegalStateException(
                    "failed to patch SBFApi auth/menu methods and diagnostics");
        }
        Files.move(temp, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        System.out.println(
                "patched "
                        + TARGET_CLASS
                        + " -> "
                        + output
                        + (realProductMenuLogging ? " [real-product-menu-logging]" : ""));
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
                        } else if (MODERN_MENU_DISPATCH_CLASS.equals(entry.getName())) {
                            bytes = patchModernMenuDispatchDiagnostics(bytes, result);
                        } else if (MODERN_MENU_MOUSE_CLASS.equals(entry.getName())) {
                            bytes = patchModernMenuMouseDiagnostics(bytes, result);
                        } else if (SIDE_MENU_MOUSE_CLASS.equals(entry.getName())) {
                            bytes = patchSideMenuMouseDiagnostics(bytes, result);
                        } else if (SIDE_MENU_CALLBACK_CLASS.equals(entry.getName())) {
                            bytes = patchSideMenuCallbackDiagnostics(bytes, result);
                        } else if (START_APP_CLASS.equals(entry.getName())) {
                            bytes = patchStartAppWebTokenBridge(bytes, result);
                        } else if (START_APP_LOGIN_CALLBACK_CLASS.equals(entry.getName())) {
                            bytes = patchStartAppLoginDisposeGuard(bytes, result);
                        } else if (START_APP_UI_CLASS.equals(entry.getName())) {
                            bytes = patchStartAppAutoLogin(bytes, result);
                        } else if (MIJAVA_CLASS.equals(entry.getName())) {
                            bytes = patchMiJavaDictBridge(bytes, result);
                        } else if (JXBROWSER_CLASS.equals(entry.getName())) {
                            bytes = patchJxBrowserDiagnostics(bytes, result);
                        } else if (JXBROWSER_LOAD_THREAD_CLASS.equals(entry.getName())) {
                            bytes = patchJxBrowserLoadDiagnostics(bytes, result);
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
                mv.visitVarInsn(Opcodes.ALOAD, 3);
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "com/sbf/main/jxbrowser/M5LocalSpiderBridge",
                        "localWebAssetBody",
                        "(Ljava/lang/String;)Ljava/lang/String;",
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
                    throw new IOException("missing product logo resource: " + resource);
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
                "localWebAssetBody",
                "(Ljava/lang/String;)Ljava/lang/String;",
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
        mv.visitVarInsn(Opcodes.ASTORE, 3);
        mv.visitJumpInsn(Opcodes.GOTO, hasBody);
        mv.visitLabel(checkRouters);
        mv.visitFrame(
                Opcodes.F_APPEND,
                2,
                new Object[] {"java/lang/String", "java/lang/String"},
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
                        + "var smartAiStub=m8SmartAi();if(smartAiStub!==null){console.log('M8_SMART_AI_XHR_STUB url='+u);return smartAiStub;}"
                        + "var kefuStub=m8Kefu();if(kefuStub!==null){console.log((kefuStub.indexOf('M8_AI_KEFU_WA_STUB')>=0?'M8_AI_KEFU_WA_STUB':'M8_AI_KEFU_XHR_STUB')+' url='+u);return kefuStub;}"
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
                    return writeJsonReturn(access, name, descriptor, signature, exceptions, LOGIN_JSON, 2);
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
                mv.visitLdcInsn(json);
                mv.visitMethodInsn(
                        Opcodes.INVOKESPECIAL,
                        jsonClass,
                        "<init>",
                        "(Ljava/lang/String;)V",
                        false);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(3, maxLocals);
                mv.visitEnd();
                return null;
            }
        };
        reader.accept(visitor, 0);
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
            autoLogin.add(new LdcInsnNode(LOGIN_JSON));
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
                        || !("(Ljava/lang/String;Lcom/sbf/main/jxbrowser/g$a;"
                                                + "Lcom/sbf/main/jxbrowser/g$b;Ljava/lang/String;"
                                                + "Lcom/db/entery/xdx/JDBZWConfig;"
                                                + "Lcom/sbf/main/jxbrowser/l;Z)"
                                                + "Lcom/teamdev/jxbrowser/browser/Browser;")
                                .equals(descriptor)) {
                    return mv;
                }
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitFieldInsn(
                            int opcode, String owner, String fieldName, String fieldDescriptor) {
                        if (opcode == Opcodes.GETSTATIC
                                && "com/teamdev/jxbrowser/engine/RenderingMode".equals(owner)
                                && "HARDWARE_ACCELERATED".equals(fieldName)) {
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
                        if (opcode == Opcodes.INVOKESTATIC
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

    private static void emitSoftwareRenderingOptions(MethodVisitor mv) {
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/teamdev/jxbrowser/engine/EngineOptions$Builder",
                "disableGpu",
                "()Lcom/teamdev/jxbrowser/engine/EngineOptions$Builder;",
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
                    writeLoadFinishedDiagnostics(mv);
                    return null;
                }
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"<init>".equals(name)
                        || !"(Ljava/lang/String;Ljava/lang/String;Lcom/sbf/main/jxbrowser/l;Z)V"
                                .equals(descriptor)) {
                    return mv;
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
                                && "g".equals(fieldName)
                                && "Lcom/teamdev/jxbrowser/browser/Browser;"
                                        .equals(fieldDescriptor)) {
                            emitBrowserCreated(this);
                            emitInstallWebDiagnostics(this);
                        }
                    }
                };
            }

            @Override
            public void visitEnd() {
                writeBitmapCaptureMethod(this);
                writeBrowserLayoutDiagnostics(this);
                writeInstallWebDiagnosticsMethod(this);
                super.visitEnd();
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
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

    private static void writeLoadFinishedDiagnostics(MethodVisitor mv) {
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
                "g",
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
                "g",
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

    private static void writeBrowserLayoutDiagnostics(ClassVisitor visitor) {
        MethodVisitor mv = visitor.visitMethod(Opcodes.ACC_PUBLIC, "doLayout", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL, "com/sbf/ui/i", "doLayout", "()V", false);
        emitBrowserViewValue(
                mv,
                "M4_V13_VIEW_PARENT=",
                "getParent",
                "()Ljava/awt/Container;");
        emitBrowserViewValue(
                mv,
                "M4_V13_VIEW_SIZE=",
                "getSize",
                "()Ljava/awt/Dimension;");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 1);
        mv.visitEnd();
    }

    private static void emitInstallWebDiagnostics(MethodVisitor mv) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(
                Opcodes.GETFIELD,
                "com/sbf/main/jxbrowser/c",
                "g",
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

    private static void emitBrowserViewValue(
            MethodVisitor mv, String prefix, String methodName, String methodDescriptor) {
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
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(
                Opcodes.GETFIELD,
                "com/sbf/main/jxbrowser/c",
                "h",
                "Lcom/teamdev/jxbrowser/view/swing/BrowserView;");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/teamdev/jxbrowser/view/swing/BrowserView",
                methodName,
                methodDescriptor,
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

    private static void emitBrowserCreated(MethodVisitor mv) {
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
                "g",
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
        mv.visitJumpInsn(Opcodes.IFEQ, done);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitVarInsn(Opcodes.ASTORE, 5);
        emitPrint(mv, "M5D11_COLLECT_TAB_JXBROWSER_URL_FROM_LINKURL");
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
        appendLiteral(mv, " selected=");
        appendSideMenuSelected(mv, listenerClass);
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

    private static void appendSideMenuSelected(MethodVisitor mv, String listenerClass) {
        loadSideMenuOwner(mv, listenerClass);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/sbf/main/ext/j2026/d",
                "g",
                "(Lcom/sbf/main/ext/j2026/d;)Z",
                false);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Z)Ljava/lang/StringBuilder;", false);
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
        boolean patchedStartAppWebTokenBridge;
        boolean patchedStartAppLoginDisposeGuard;
        boolean patchedStartAppAutoLogin;
        boolean patchedMiJavaDictBridge;
        boolean patchedLocalSpiderTaskGet;
        boolean patchedLocalSpiderTaskStatus;
        boolean patchedJxBrowserDiagnostics;
        boolean patchedJxBrowserLoadDiagnostics;
        boolean patchedJxBrowserEngine;
        boolean patchedLocalWebSchemeCallback;
        boolean patchedGoogleCRHelper;
        boolean patchedSpiderCallbackPostData;
        boolean patchedSpiderCallbackEndTask;
        boolean addedM5ConsoleObserver;
        boolean addedM5AuthBootstrapCallback;
        boolean addedM5InjectJsCallback;
        boolean addedM5LocalSpiderBridge;
        boolean addedM5RequestObserver;
        boolean addedM5YesCaptchaBridge;
    }
}
