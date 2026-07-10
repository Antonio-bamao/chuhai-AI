package com.sbf.main;

import com.sbf.main.ext.j2026.d;
import com.sbf.main.ext.j2026.d$a;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import javax.swing.SwingUtilities;
import org.json.JSONArray;
import org.json.JSONObject;

public final class M8D7DefaultMenuDispatch {
    private static final String FACEBOOK_PAGE_COLLECT_ROUTE =
            "/es/bigData/bigDataTask?code=fb_page_data";
    private static final String INSTAGRAM_BLOGGER_COLLECT_ROUTE =
            "/es/bigData/bigDataTask?code=ins_blogger_data";
    private static final String TWITTER_PRECISE_SEARCH_ROUTE =
            "/es/bigData/bigDataTask?code=big_data_twitter_new";
    private static final String TIKTOK_BIG_DATA_ROUTE =
            "/es/bigData/bigDataTask?code=big_data_tiktok_new";
    private static final String TELEGRAM_GROUP_COLLECT_ROUTE = "/pc/tg/index";
    private static final String GEO_GOOGLE_SEO_ROUTE = "/pc/dataCollect/googleseo";
    private static final String WSKEFU_CONVERSATION_ROUTE = "/pc/kefu/conversation";
    private static volatile String dispatchedProductCode;

    private M8D7DefaultMenuDispatch() {
    }

    public static void dispatch(JSBFMain main) {
        dispatch(main, "whatsapp");
    }

    public static void dispatch(JSBFMain main, String productCode) {
        if (main == null) {
            return;
        }
        String normalizedProductCode = normalizeProductCode(productCode);
        synchronized (M8D7DefaultMenuDispatch.class) {
            if (normalizedProductCode.equals(dispatchedProductCode)) {
                return;
            }
            dispatchedProductCode = normalizedProductCode;
        }
        Thread worker =
                new Thread(
                        () -> {
                            try {
                                Thread.sleep(700L);
                            } catch (InterruptedException ignored) {
                                Thread.currentThread().interrupt();
                            }
                            SwingUtilities.invokeLater(
                                    () -> dispatchOnEdt(main, normalizedProductCode));
                        },
                        "M8D7-default-menu-dispatch");
        worker.setDaemon(true);
        worker.start();
    }

    private static String normalizeProductCode(String productCode) {
        if ("tiktok".equals(productCode)) {
            return "tiktok";
        }
        if ("instagram".equals(productCode)) {
            return "instagram";
        }
        if ("twitter".equals(productCode) || "x".equals(productCode)) {
            return "twitter";
        }
        if ("telegram".equals(productCode)) {
            return "telegram";
        }
        if ("geo".equals(productCode)) {
            return "geo";
        }
        if ("wskefu".equals(productCode)) {
            return "wskefu";
        }
        return "facebook".equals(productCode) ? "facebook" : "whatsapp";
    }

    private static void dispatchOnEdt(JSBFMain main, String productCode) {
        try {
            d$a callback = callbackOf(main);
            if (callback == null) {
                System.out.println("M8D7_DEFAULT_MENU_DISPATCH_SKIPPED callback=null");
                return;
            }
            d node = findDefaultNode(main, productCode);
            if (node == null) {
                node = buildDefaultNode(callback, productCode);
            }
            System.out.println(
                    "M8D7_DEFAULT_MENU_DISPATCH name="
                            + node.getName()
                            + " id="
                            + node.c()
                            + " code="
                            + node.d());
            node.b();
        } catch (Throwable error) {
            System.out.println(
                    "M8D7_DEFAULT_MENU_DISPATCH_FAILED error="
                            + error.getClass().getName());
        }
    }

    private static d$a callbackOf(JSBFMain main) throws ReflectiveOperationException {
        Field field = JSBFMain.class.getDeclaredField("bt");
        field.setAccessible(true);
        return (d$a) field.get(main);
    }

    private static d findDefaultNode(Container root, String productCode) {
        Component[] components = root.getComponents();
        for (int i = 0; i < components.length; i++) {
            Component component = components[i];
            if (component instanceof d) {
                d node = (d) component;
                if (isDefaultNode(node, productCode)) {
                    return node;
                }
            }
            if (component instanceof Container) {
                d found = findDefaultNode((Container) component, productCode);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static boolean isDefaultNode(d node, String productCode) {
        if ("facebook".equals(productCode)) {
            return "C4747_003".equals(node.d());
        }
        if ("instagram".equals(productCode)) {
            return "C4131_005".equals(node.d());
        }
        if ("twitter".equals(productCode)) {
            return "C4133_003".equals(node.d());
        }
        if ("tiktok".equals(productCode)) {
            return "C3461_002".equals(node.d());
        }
        if ("telegram".equals(productCode)) {
            return "C4135_005".equals(node.d());
        }
        if ("geo".equals(productCode)) {
            return "C4134_002".equals(node.d());
        }
        if ("wskefu".equals(productCode)) {
            return "C4936_000".equals(node.d());
        }
        return "REC_WHATSAPP_ONELINE".equals(node.d()) || "\u4e00\u53e5\u8bdd".equals(node.getName());
    }

    private static d buildDefaultNode(d$a callback) {
        return buildDefaultNode(callback, "whatsapp");
    }

    private static d buildDefaultNode(d$a callback, String productCode) {
        if ("facebook".equals(productCode)) {
            return buildPlatformNode(
                    callback,
                    91030401,
                    910304,
                    9103,
                    "C4747_003",
                    "REC_FACEBOOK_PAGE_COLLECT_ROUTE",
                    "FB \u4e3b\u9875\u91c7\u96c6",
                    FACEBOOK_PAGE_COLLECT_ROUTE,
                    4,
                    "facebook_menu_icon_4");
        }
        if ("instagram".equals(productCode)) {
            return buildPlatformNode(
                    callback,
                    91040401,
                    910404,
                    9104,
                    "C4131_005",
                    "REC_INSTAGRAM_BLOGGER_COLLECT_ROUTE",
                    "Ins \u4e3b\u9875\u6316\u6398",
                    INSTAGRAM_BLOGGER_COLLECT_ROUTE,
                    4,
                    "ins_menu_icon_4");
        }
        if ("twitter".equals(productCode)) {
            return buildPlatformNode(
                    callback,
                    91050201,
                    910502,
                    9105,
                    "C4133_003",
                    "REC_TWITTER_PRECISE_SEARCH_ROUTE",
                    "X \u7cbe\u51c6\u641c\u7d22",
                    TWITTER_PRECISE_SEARCH_ROUTE,
                    2,
                    "twitter_menu_icon_2");
        }
        if ("tiktok".equals(productCode)) {
            return buildPlatformNode(
                    callback, 91020101, 910201, 9102, "C3461_002", "REC_TIKTOK_BIG_DATA_ROUTE",
                    "TK AI\u91c7\u96c6", TIKTOK_BIG_DATA_ROUTE, 1, "menu_tk_1");
        }
        if ("telegram".equals(productCode)) {
            return buildPlatformNode(
                    callback, 91060501, 910605, 9106, "C4135_005", "REC_TELEGRAM_GROUP_COLLECT_ROUTE",
                    "TG AI \u7fa4\u91c7\u96c6", TELEGRAM_GROUP_COLLECT_ROUTE, 5, "tg_menu_icon_5");
        }
        if ("geo".equals(productCode)) {
            return buildPlatformNode(
                    callback, 91070101, 910701, 9107, "C4134_002", "REC_GEO_GOOGLE_SEO_ROUTE",
                    "\u7cbe\u51c6\u5b98\u7f51\u6316\u6398", GEO_GOOGLE_SEO_ROUTE, 1,
                    "geo_ai_menu_icon_1");
        }
        if ("wskefu".equals(productCode)) {
            return buildPlatformNode(
                    callback, 91080101, 910801, 9108, "C4936_000", "REC_WSKEFU_CONVERSATION_ROUTE",
                    "\u4fe1\u606f\u603b\u89c8", WSKEFU_CONVERSATION_ROUTE, 1, "wskf_menu_icon_1");
        }
        JSONArray children = new JSONArray();
        children.put(
                menuItem(
                        91010101,
                        910101,
                        "REC_WHATSAPP_ONELINE_ROUTE",
                        "\u4e00\u53e5\u8bdd",
                        "/pc/aigc/aichat_dialog",
                        "JSinglepage:/pc/aigc/aichat_dialog",
                        1,
                        true,
                        9101,
                        "whatsapp_menu_icon_1"));
        return new d(
                910101,
                menuItem(
                        910101,
                        9101,
                        "REC_WHATSAPP_ONELINE",
                        "\u4e00\u53e5\u8bdd",
                        "JSinglepage",
                        "/pc/aigc/aichat_dialog",
                        1,
                        false,
                        9101,
                        "whatsapp_menu_icon_1"),
                children,
                callback);
    }

    private static d buildPlatformNode(
            d$a callback,
            int id,
            int parentId,
            int productId,
            String parentCode,
            String childCode,
            String name,
            String route,
            int displayIndex,
            String icon) {
        JSONArray children = new JSONArray();
        children.put(
                menuItem(
                        id,
                        parentId,
                        childCode,
                        name,
                        route,
                        "JSinglepage",
                        1,
                        true,
                        productId,
                        icon));
        return new d(
                parentId,
                menuItem(
                        parentId,
                        productId,
                        parentCode,
                        name,
                        "JSinglepage",
                        route,
                        displayIndex,
                        false,
                        productId,
                        icon),
                children,
                callback);
    }

    private static JSONObject menuItem(
            int id,
            int parentId,
            String code,
            String name,
            String localCode,
            String linkUrl,
            int displayIndex,
            boolean treeEnd,
            int productId,
            String icon) {
        JSONObject item = new JSONObject();
        item.put("id", id);
        item.put("sid", productId);
        item.put("fid", productId);
        item.put("productId", productId);
        item.put("parentId", parentId);
        item.put("code", code);
        item.put("name", name);
        item.put("displayName", name);
        item.put("icon", icon);
        item.put("localCode", localCode);
        item.put("linkUrl", linkUrl);
        item.put("webFlg", 1);
        item.put("treeEndFlg", treeEnd ? 1 : 0);
        item.put("displayIndex", displayIndex);
        item.put("sort", displayIndex);
        item.put("status", 1);
        return item;
    }
}
