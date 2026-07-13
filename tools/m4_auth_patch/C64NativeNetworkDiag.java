package com.sbf.main;

public final class C64NativeNetworkDiag {
    private C64NativeNetworkDiag() {}

    public static String observe(String url) {
        if (url == null || (!url.contains("xdxsoft.com") && !url.contains("47.97.27.111"))) {
            return url;
        }
        System.out.println("C64_NATIVE_URL_OBSERVED url=" + url);
        new Exception("C64_NATIVE_URL_CALLER").printStackTrace(System.out);
        return url;
    }

    public static String localResponse(String url) {
        if (d8OnlineEnabled() && isD8OriginalBackendUrl(url)) {
            System.out.println("D8_NATIVE_GATEWAY_PASSTHROUGH url=" + url);
            return null;
        }
        if (url == null || (!url.contains("app.xdxsoft.com") && !url.contains("47.97.27.111"))) {
            return null;
        }
        if (url.contains("/api/v1/client/pc/checkuser")) {
            return response(
                    "checkuser",
                    "{\"code\":200,\"msg\":\"offline authorization accepted\","
                            + "\"data\":{\"authorized\":true,\"enabled\":true,\"blocked\":false}}");
        }
        if (url.contains("/api/v1/client/pc/lisBanWords")) {
            return response(
                    "lisBanWords",
                    "{\"code\":200,\"msg\":\"offline ban-word list\",\"data\":[],\"rows\":[],\"total\":0}");
        }
        if (url.contains("/system/appfiles/code/hcai_app_ime")
                || url.contains("/system/appfiles/code/atxagent")
                || url.contains("/system/appfiles/code/scrcpy-web-v4")) {
            return response(
                    "appfile",
                    "{\"code\":200,\"msg\":\"offline appfile unavailable\","
                            + "\"data\":{\"code\":200,\"data\":{}}}");
        }
        return response(
                "fallback",
                "{\"code\":200,\"msg\":\"offline native gateway\",\"data\":[],\"rows\":[],\"total\":0}");
    }

    private static boolean isD8OriginalBackendUrl(String url) {
        if (url == null) {
            return false;
        }
        try {
            java.net.URI uri = new java.net.URI(url);
            String host = uri.getHost();
            if (host == null) {
                return false;
            }
            String lower = host.toLowerCase();
            return "xdxsoft.com".equals(lower)
                    || lower.endsWith(".xdxsoft.com")
                    || "huochai.ai".equals(lower)
                    || lower.endsWith(".huochai.ai")
                    || "mierp.net".equals(lower)
                    || lower.endsWith(".mierp.net")
                    || "47.97.27.111".equals(lower)
                    || "163.181.39.184".equals(lower);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean d8OnlineEnabled() {
        return Boolean.getBoolean("huochai.d8.online");
    }

    private static String response(String contract, String json) {
        System.out.println("C65_NATIVE_GATEWAY_STUB contract=" + contract);
        return json;
    }
}
