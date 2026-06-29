package com.sbf.main.ext.gg;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

public final class M5YesCaptchaBridge {
    private static final String DEFAULT_API_BASE = "https://api.yescaptcha.com";
    private static final String TASK_TYPE = "ReCaptchaV2Classification";
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 30000;
    private static final int POLL_ATTEMPTS = 24;
    private static final long POLL_DELAY_MS = 2500L;

    private M5YesCaptchaBridge() {
    }

    public static String solve(String imageBase64, String question) {
        try {
            String clientKey = loadClientKey();
            if (isBlank(clientKey)) {
                System.out.println("M5D_YESCAPTCHA_MISSING_KEY");
                return emptySolution("missing_client_key");
            }
            String apiBase = apiBase();
            JSONObject task = new JSONObject()
                    .put("type", TASK_TYPE)
                    .put("image", imageBase64 == null ? "" : imageBase64)
                    .put("question", question == null ? "" : question);
            JSONObject createBody = new JSONObject()
                    .put("clientKey", clientKey)
                    .put("task", task);
            System.out.println(
                    "M5D_YESCAPTCHA_CREATE clientKey="
                            + maskClientKey(clientKey)
                            + " type="
                            + TASK_TYPE
                            + " question="
                            + String.valueOf(question)
                            + " imageChars="
                            + (imageBase64 == null ? 0 : imageBase64.length()));
            JSONObject create = postJson(apiBase + "/createTask", createBody);
            System.out.println(
                    "M5D_YESCAPTCHA_CREATE_RESPONSE "
                            + responseSummary(create)
                            + " raw="
                            + truncate(create.toString(), 900));
            String ready = readySolutionFrom(create);
            if (ready != null) {
                System.out.println(
                        "M5D_YESCAPTCHA_READY immediate=true taskId="
                                + create.optLong("taskId", 0L)
                                + " solution="
                                + solutionSummary(ready)
                                + " raw="
                                + truncate(ready, 900));
                return ready;
            }
            long taskId = create.optLong("taskId", 0L);
            if (taskId <= 0L) {
                System.out.println(
                        "M5D_YESCAPTCHA_CREATE_FAILED errorId="
                                + create.optInt("errorId", -1)
                                + " errorCode="
                                + create.optString("errorCode"));
                return emptySolution(create.optString("errorCode", "create_failed"));
            }
            JSONObject resultBody = new JSONObject()
                    .put("clientKey", clientKey)
                    .put("taskId", taskId);
            for (int attempt = 1; attempt <= POLL_ATTEMPTS; attempt++) {
                sleep(POLL_DELAY_MS);
                JSONObject result = postJson(apiBase + "/getTaskResult", resultBody);
                System.out.println(
                        "M5D_YESCAPTCHA_POLL_RESULT taskId="
                                + taskId
                                + " attempt="
                                + attempt
                                + " "
                                + responseSummary(result)
                                + " raw="
                                + truncate(result.toString(), 900));
                ready = readySolutionFrom(result);
                if (ready != null) {
                    System.out.println(
                            "M5D_YESCAPTCHA_READY taskId="
                                    + taskId
                                    + " attempt="
                                    + attempt
                                    + " solution="
                                    + solutionSummary(ready)
                                    + " raw="
                                    + truncate(ready, 900));
                    return ready;
                }
                if (result.optInt("errorId", 0) != 0) {
                    System.out.println(
                            "M5D_YESCAPTCHA_RESULT_FAILED taskId="
                                    + taskId
                                    + " errorCode="
                                    + result.optString("errorCode"));
                    return emptySolution(result.optString("errorCode", "result_failed"));
                }
                System.out.println("M5D_YESCAPTCHA_POLLING taskId=" + taskId + " attempt=" + attempt);
            }
            System.out.println("M5D_YESCAPTCHA_TIMEOUT taskId=" + taskId);
            return emptySolution("timeout");
        } catch (Throwable error) {
            System.out.println(
                    "M5D_YESCAPTCHA_ERROR "
                            + error.getClass().getName()
                            + ": "
                            + String.valueOf(error.getMessage()));
            return emptySolution("exception");
        }
    }

    static String loadClientKeyForTest(Path baseDir) {
        return loadClientKey(baseDir);
    }

    static String maskClientKeyForTest(String clientKey) {
        return maskClientKey(clientKey);
    }

    static String normalizeReadyResultForTest(String json) {
        return normalizeReadyResult(new JSONObject(json));
    }

    static String readySolutionFromForTest(String json) {
        return readySolutionFrom(new JSONObject(json));
    }

    static String responseSummaryForTest(String json) {
        return responseSummary(new JSONObject(json));
    }

    private static String loadClientKey() {
        String property = trimToNull(System.getProperty("yescaptcha.clientKey"));
        if (property != null) {
            return property;
        }
        String env = trimToNull(System.getenv("YESCAPTCHA_CLIENT_KEY"));
        if (env != null) {
            return env;
        }
        for (Path root : runtimeRoots()) {
            String key = loadClientKey(root);
            if (!isBlank(key)) {
                return key;
            }
        }
        return "";
    }

    private static String loadClientKey(Path baseDir) {
        if (baseDir == null) {
            return "";
        }
        Path root = baseDir.toAbsolutePath().normalize();
        Path[] candidates = new Path[] {
            root.resolve(".env"),
            root.resolve("yescaptcha.properties"),
            root.resolve("config").resolve(".env"),
            root.resolve("config").resolve("yescaptcha.properties"),
            root.resolve("data").resolve("app").resolve("config").resolve(".env"),
            root.resolve("data").resolve("app").resolve("config").resolve("yescaptcha.properties")
        };
        for (Path candidate : candidates) {
            String key = loadClientKeyFromFile(candidate);
            if (!isBlank(key)) {
                return key;
            }
        }
        return "";
    }

    private static String loadClientKeyFromFile(Path path) {
        try {
            if (path == null || !Files.isRegularFile(path)) {
                return "";
            }
            BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.length() == 0 || trimmed.startsWith("#")) {
                        continue;
                    }
                    int equals = trimmed.indexOf('=');
                    if (equals <= 0) {
                        continue;
                    }
                    String name = trimmed.substring(0, equals).trim();
                    String value = stripQuotes(trimmed.substring(equals + 1).trim());
                    if ("YESCAPTCHA_CLIENT_KEY".equals(name)
                            || "yescaptcha.clientKey".equals(name)
                            || "clientKey".equals(name)) {
                        return value;
                    }
                }
            } finally {
                reader.close();
            }
        } catch (Throwable ignored) {
            return "";
        }
        return "";
    }

    private static Set<Path> runtimeRoots() {
        Set<Path> roots = new LinkedHashSet<Path>();
        addRoot(roots, Paths.get("").toAbsolutePath());
        try {
            Class<?> startApp = Class.forName("com.sbf.main.StartApp");
            Object value = startApp.getField("a").get(null);
            if (value != null) {
                Path path = Paths.get(String.valueOf(value));
                addRoot(roots, path);
                Path parent = path.getParent();
                if (parent != null) {
                    addRoot(roots, parent);
                }
            }
        } catch (Throwable ignored) {
        }
        return roots;
    }

    private static void addRoot(Set<Path> roots, Path path) {
        if (path != null) {
            roots.add(path.toAbsolutePath().normalize());
        }
    }

    private static String apiBase() {
        String configured = trimToNull(System.getProperty("yescaptcha.apiBase"));
        if (configured == null) {
            configured = trimToNull(System.getenv("YESCAPTCHA_API_BASE"));
        }
        if (configured == null) {
            configured = DEFAULT_API_BASE;
        }
        while (configured.endsWith("/")) {
            configured = configured.substring(0, configured.length() - 1);
        }
        return configured;
    }

    private static JSONObject postJson(String url, JSONObject body) throws Exception {
        byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        OutputStream out = conn.getOutputStream();
        try {
            out.write(payload);
        } finally {
            out.close();
        }
        int code = conn.getResponseCode();
        InputStream in = code >= 200 && code < 400 ? conn.getInputStream() : conn.getErrorStream();
        String response = readUtf8(in);
        if (code < 200 || code >= 400) {
            throw new IOException("HTTP " + code + " " + response);
        }
        return new JSONObject(response);
    }

    private static String readySolutionFrom(JSONObject payload) {
        if (payload == null) {
            return null;
        }
        if (payload.has("result")) {
            String result = payload.optString("result", "");
            if (!isBlank(result)) {
                return normalizeReadyResult(new JSONObject(result));
            }
        }
        String status = payload.optString("status", "");
        if ("ready".equalsIgnoreCase(status)) {
            return normalizeReadyResult(payload);
        }
        if (hasExplicitSolution(payload.optJSONObject("solution"))) {
            return normalizeReadyResult(payload);
        }
        return null;
    }

    private static String normalizeReadyResult(JSONObject payload) {
        JSONObject solution = payload.optJSONObject("solution");
        if (solution == null) {
            solution = new JSONObject();
        }
        JSONArray objects = solution.optJSONArray("objects");
        if (objects == null) {
            objects = new JSONArray();
        }
        boolean hasObject = solution.has("hasObject")
                ? solution.optBoolean("hasObject", objects.length() > 0)
                : objects.length() > 0;
        JSONObject normalizedSolution = new JSONObject()
                .put("objects", objects)
                .put("hasObject", hasObject);
        return new JSONObject().put("solution", normalizedSolution).toString();
    }

    private static boolean hasExplicitSolution(JSONObject solution) {
        if (solution == null) {
            return false;
        }
        JSONArray objects = solution.optJSONArray("objects");
        return solution.has("hasObject") || (objects != null && objects.length() > 0);
    }

    private static String emptySolution(String reason) {
        return new JSONObject()
                .put("solution", new JSONObject().put("objects", new JSONArray()).put("hasObject", false))
                .put("error", reason == null ? "" : reason)
                .toString();
    }

    private static String solutionSummary(String readyJson) {
        try {
            JSONObject solution = new JSONObject(readyJson).getJSONObject("solution");
            return "objects="
                    + solution.optJSONArray("objects")
                    + ",hasObject="
                    + solution.optBoolean("hasObject");
        } catch (Throwable ignored) {
            return "unparseable";
        }
    }

    private static String responseSummary(JSONObject payload) {
        if (payload == null) {
            return "payload=null";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("errorId=").append(payload.optInt("errorId", -1));
        if (payload.has("errorCode")) {
            builder.append(" errorCode=").append(payload.optString("errorCode"));
        }
        if (payload.has("status")) {
            builder.append(" status=").append(payload.optString("status"));
        }
        if (payload.has("taskId")) {
            builder.append(" taskId=").append(payload.optLong("taskId", 0L));
        }
        JSONObject solution = payload.optJSONObject("solution");
        if (solution != null) {
            builder.append(" solution=objects=")
                    .append(solution.optJSONArray("objects"))
                    .append(",hasObject=")
                    .append(solution.has("hasObject") ? String.valueOf(solution.optBoolean("hasObject")) : "missing");
        }
        return builder.toString();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private static String readUtf8(InputStream in) throws Exception {
        if (in == null) {
            return "";
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String maskClientKey(String clientKey) {
        if (clientKey == null || clientKey.length() == 0) {
            return "";
        }
        if (clientKey.length() <= 8) {
            return "****";
        }
        return clientKey.substring(0, 4) + "****" + clientKey.substring(clientKey.length() - 4);
    }

    private static String stripQuotes(String value) {
        if (value != null && value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private static void sleep(long ms) throws InterruptedException {
        Thread.sleep(ms);
    }

}
