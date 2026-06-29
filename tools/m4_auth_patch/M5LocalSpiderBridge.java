package com.sbf.main.jxbrowser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

public final class M5LocalSpiderBridge {
    private static final String MODULE_WHATSAPP = "whatsapp";
    private static final String SPIDER_WHATSAPP_USERS = "whatsapp_users_lists";
    private static final String[] WHATSAPP_COLLECT_TAB_SPIDERS = {
        "whatsapp_users_lists",
        "wap_global_clue_users",
        "whatsapp_group_lists",
        "whatsapp_regional_collection"
    };
    private static final String SPIDER_RUNNER_MODE_EXTERNAL_SEARCH = "external_search";
    private static final int SQLITE_BUSY_RETRIES = 5;
    private static final long SQLITE_BUSY_RETRY_DELAY_MS = 800L;
    private static final long CLOUD_SPIDER_CONTEXT_TIMEOUT_MS = 30000L;
    private static final long CLOUD_SPIDER_ORIGINAL_GRACE_MS = 5000L;
    private static long lastTaskId;
    private static volatile Object localCloudSpiderContext;
    private static volatile Object localBrowserContext;
    private static volatile String localCloudSpiderCode;

    private M5LocalSpiderBridge() {
    }

    public static String getNewTask(String moduleCode) {
        System.out.println("M5A_LOCAL_SPIDER_QUEUE_EMPTY moduleCode=" + String.valueOf(moduleCode));
        return "[]";
    }

    public static String getNewTask(String baseDir, String moduleCode, int status) throws Exception {
        if (!MODULE_WHATSAPP.equals(moduleCode)) {
            return "[]";
        }
        Class.forName("org.sqlite.JDBC");
        Path dbPath = taskDbPath(baseDir, moduleCode);
        if (!Files.exists(dbPath)) {
            System.out.println("M5C_QUEUE_GET_NEW_TASK_EMPTY moduleCode=" + moduleCode + " status=" + status);
            return "[]";
        }
        JSONArray rows = new JSONArray();
        try (Connection conn = openSqlite(dbPath)) {
            ensureTaskTable(conn);
            JSONObject task = null;
            try (PreparedStatement query =
                    conn.prepareStatement(
                            "select task_seq,data,status from rpa_task "
                                    + "where module=? and type=? and status=0 "
                                    + "order by time asc,id asc limit 1")) {
                query.setString(1, moduleCode);
                query.setString(2, "m5_local_spider");
                try (ResultSet rs = query.executeQuery()) {
                    if (rs.next()) {
                        task = parseTaskData(rs.getString(2));
                    }
                }
            }
            if (task != null) {
                long taskId = task.optLong("taskId");
                int retryCount = task.optInt("retryCount", 0) + 1;
                task.put("retryCount", retryCount);
                task.put("status", 1);
                task.put("message", "running");
                task.put("updatedTime", System.currentTimeMillis());
                try (PreparedStatement update =
                        conn.prepareStatement(
                                "update rpa_task set status=1,error=?,data=?,time=? "
                                        + "where module=? and type=? and task_seq=?")) {
                    update.setString(1, "running");
                    update.setString(2, task.toString());
                    update.setLong(3, System.currentTimeMillis());
                    update.setString(4, moduleCode);
                    update.setString(5, "m5_local_spider");
                    update.setString(6, String.valueOf(taskId));
                    update.executeUpdate();
                }
                rows.put(queueRow(task, 1, "running"));
                System.out.println(
                        "M5C_QUEUE_GET_NEW_TASK_CLAIMED taskId="
                                + taskId
                                + " moduleCode="
                                + moduleCode
                                + " retryCount="
                                + retryCount);
            } else {
                System.out.println("M5C_QUEUE_GET_NEW_TASK_EMPTY moduleCode=" + moduleCode + " status=" + status);
            }
        }
        return rows.toString();
    }

    public static String platformOptions(String type) {
        JSONArray groups = new JSONArray();
        if ("area_code".equals(type)) {
            groups.put(
                    new JSONObject()
                            .put("label", "北美")
                            .put(
                                    "children",
                                    new JSONArray()
                                            .put(
                                                    new JSONObject()
                                                            .put("code", "+1")
                                                            .put("label", "美国/加拿大 +1")
                                                            .put("iconUrl", ""))));
        } else if ("platform".equals(type)) {
            groups.put(
                    new JSONObject()
                            .put("label", "搜索平台")
                            .put(
                                    "children",
                                    new JSONArray()
                                            .put(
                                                    new JSONObject()
                                                            .put("code", "facebook.com")
                                                            .put("label", "Facebook")
                                                            .put("iconUrl", ""))
                                            .put(
                                                    new JSONObject()
                                                            .put("code", "google.com")
                                                            .put("label", "Google")
                                                            .put("iconUrl", ""))));
        } else if ("keywords".equals(type)) {
            groups.put(
                    new JSONObject()
                            .put("label", "关键词")
                            .put(
                                    "children",
                                    new JSONArray()
                                            .put(
                                                    new JSONObject()
                                                            .put("code", "local-test")
                                                            .put("label", "local-test")
                                                            .put("iconUrl", ""))));
        }
        return new JSONObject().put("code", 200).put("msg", "success").put("data", groups).toString();
    }

    public static String submitTask(
            String baseDir,
            String moduleCode,
            String spiderCode,
            String spiderParamsJson,
            String taskConfigJson)
            throws Exception {
        requireSupportedSpider(moduleCode, spiderCode);
        Class.forName("org.sqlite.JDBC");
        long taskId = nextTaskId();
        JSONObject envelope = buildTaskEnvelope(
                baseDir, taskId, moduleCode, spiderCode, spiderParamsJson, taskConfigJson);
        Path dbPath = taskDbPath(baseDir, moduleCode);
        Files.createDirectories(dbPath.getParent());
        insertQueuedTaskWithRetry(
                dbPath,
                taskId,
                moduleCode,
                spiderCode,
                spiderParamsJson,
                taskConfigJson,
                envelope);
        System.out.println(
                "M5C_QUEUE_TASK_ENQUEUED taskId="
                        + taskId
                        + " moduleCode="
                        + moduleCode
                        + " spiderCode="
                        + spiderCode);
        startLocalPipeline(baseDir, moduleCode);
        return new JSONObject()
                .put("code", 200)
                .put("msg", "local task queued")
                .put("submitted", true)
                .put("localOnly", true)
                .put("taskId", taskId)
                .put("moduleCode", moduleCode)
                .put("spiderCode", spiderCode)
                .put("status", 0)
                .put("entry", "com.sbf.main.cloud.spider.a.a(Long)")
                .toString();
    }

    public static String localWebAssetBody(String url) {
        try {
            if (url != null
                    && (url.indexOf("tos-public.volccdn.com") >= 0
                            || url.indexOf("tos.umd.production.min.js") >= 0)) {
                System.out.println("M5D8_LOCAL_WEB_ASSET_TOS_STUB " + String.valueOf(url));
                return "window.TOS=window.TOS||{};";
            }
            Path asset = localWebAssetPath(url);
            if (asset == null || !Files.exists(asset)) {
                return null;
            }
            System.out.println("M5D8_LOCAL_WEB_ASSET " + asset.toAbsolutePath());
            String body = new String(Files.readAllBytes(asset), "UTF-8");
            if ("aicloud.html".equals(asset.getFileName().toString())) {
                body =
                        body.replace(
                                "<script src=https://tos-public.volccdn.com/obj/volc-tos-public/@volcengine/tos-sdk@latest/browser/tos.umd.production.min.js></script>",
                                "<script>window.TOS=window.TOS||{};</script>");
            }
            body = patchLocalWebAssetBody(asset.getFileName().toString(), body);
            return body;
        } catch (Throwable error) {
            System.out.println("M5D8_LOCAL_WEB_ASSET_FAILED url=" + String.valueOf(url)
                    + " error=" + String.valueOf(rootCause(error)));
            return null;
        }
    }

    private static String patchLocalWebAssetBody(String filename, String body) {
        if ("chunk-00b3289e.51ab7483.js".equals(filename)) {
            String patched =
                    body.replace(
                            "queryParams:{pageNum:1,pageSize:10}",
                            "queryParams:{pageNum:1,pageSize:50}");
            System.out.println(
                    "M5D9_PAGE_SIZE_PATCH dataIndex50=" + String.valueOf(!patched.equals(body)));
            return patched;
        }
        if (!"chunk-aab334e0.bf74703f.js".equals(filename)) {
            return body;
        }
        String patched = body;
        String progressStart = "taskProcessData:[";
        String progressEnd = "],queryParams:{pageNum:1,spiderCode:this.$route.query.modal,pageSize:10}";
        int start = patched.indexOf(progressStart);
        int end = start < 0 ? -1 : patched.indexOf(progressEnd, start);
        if (start >= 0 && end > start) {
            patched = patched.substring(0, start)
                    + "taskProcessData:[]"
                    + patched.substring(end + 1);
            System.out.println("M5D9_TASK_PROCESS_IDLE_PATCH clearedDemoProgress=true");
        } else {
            System.out.println("M5D9_TASK_PROCESS_IDLE_PATCH clearedDemoProgress=false");
        }

        String spinnerBefore =
                "a(\"img\",{staticClass:\"task-loading\",attrs:{src:a(\"cfcf\")}}),t._e(),t._e(),null!=t.curTaskInfo?";
        String spinnerAfter =
                "null!=t.curTaskInfo?a(\"img\",{staticClass:\"task-loading\",attrs:{src:a(\"cfcf\")}}):t._e(),t._e(),t._e(),null!=t.curTaskInfo?";
        if (patched.indexOf(spinnerBefore) >= 0) {
            patched = patched.replace(spinnerBefore, spinnerAfter);
            System.out.println("M5D9_TASK_PROCESS_IDLE_PATCH hideIdleSpinner=true");
        } else if (patched.indexOf(
                        "s(\"img\",{staticClass:\"task-loading\",attrs:{src:a(\"cfcf\")}}),t._e(),t._e(),null!=t.curTaskInfo?")
                >= 0) {
            patched =
                    patched.replace(
                            "s(\"img\",{staticClass:\"task-loading\",attrs:{src:a(\"cfcf\")}}),t._e(),t._e(),null!=t.curTaskInfo?",
                            "null!=t.curTaskInfo?s(\"img\",{staticClass:\"task-loading\",attrs:{src:a(\"cfcf\")}}):t._e(),t._e(),t._e(),null!=t.curTaskInfo?");
            System.out.println("M5D9_TASK_PROCESS_IDLE_PATCH hideIdleSpinner=true");
        } else {
            System.out.println("M5D9_TASK_PROCESS_IDLE_PATCH hideIdleSpinner=false");
        }
        String pageSizeBefore = "queryParams:{pageNum:1,spiderCode:this.$route.query.modal,pageSize:10}";
        String pageSizeAfter = "queryParams:{pageNum:1,spiderCode:this.$route.query.modal,pageSize:50}";
        if (patched.indexOf(pageSizeBefore) >= 0) {
            patched = patched.replace(pageSizeBefore, pageSizeAfter);
            System.out.println("M5D9_PAGE_SIZE_PATCH collectionTask50=true");
        } else {
            System.out.println("M5D9_PAGE_SIZE_PATCH collectionTask50=false");
        }
        return patched;
    }

    public static String localWebAssetContentType(String url) {
        String lower = url == null ? "" : url.toLowerCase();
        if (lower.indexOf("tos-public.volccdn.com") >= 0
                || lower.indexOf("tos.umd.production.min.js") >= 0) {
            return "application/javascript;charset=UTF-8";
        }
        String path = normalizedUrlPath(url);
        if (path.endsWith(".js")) {
            return "application/javascript;charset=UTF-8";
        }
        if (path.endsWith(".css")) {
            return "text/css;charset=UTF-8";
        }
        if (path.endsWith(".html") || path.equals("/") || path.startsWith("/pc/")) {
            return "text/html;charset=UTF-8";
        }
        return "application/json;charset=UTF-8";
    }

    private static Path localWebAssetPath(String url) {
        String path = normalizedUrlPath(url);
        if (path.length() == 0
                || "/".equals(path)
                || path.startsWith("/pc/")
                || path.endsWith("/aicloud.html")) {
            return localWebMirrorDir().resolve("aicloud.html");
        }
        String filename = path.substring(path.lastIndexOf('/') + 1);
        if (filename.indexOf("..") >= 0 || filename.length() == 0) {
            return null;
        }
        if (path.startsWith("/static/js/") || path.startsWith("/static/css/")) {
            return localWebMirrorDir().resolve(filename);
        }
        return null;
    }

    private static Path localWebMirrorDir() {
        String baseDir = resolveAppBaseDir();
        Path[] candidates = {
            Paths.get(".").toAbsolutePath().normalize().resolve(".artifacts").resolve("working").resolve("m5-online-js"),
            Paths.get(".").toAbsolutePath().normalize().resolve("..").resolve("..").resolve(".artifacts").resolve("working").resolve("m5-online-js").normalize(),
            Paths.get(baseDir).toAbsolutePath().normalize().resolve("..").resolve("..").resolve(".artifacts").resolve("working").resolve("m5-online-js").normalize(),
            Paths.get(baseDir).toAbsolutePath().normalize().resolve("..").resolve(".artifacts").resolve("working").resolve("m5-online-js").normalize()
        };
        for (Path candidate : candidates) {
            if (Files.exists(candidate.resolve("aicloud.html"))) {
                return candidate;
            }
        }
        return candidates[0];
    }

    private static String normalizedUrlPath(String url) {
        String text = url == null ? "" : url.trim();
        int scheme = text.indexOf("://");
        if (scheme >= 0) {
            int slash = text.indexOf('/', scheme + 3);
            text = slash >= 0 ? text.substring(slash) : "/";
        }
        int query = text.indexOf('?');
        if (query >= 0) {
            text = text.substring(0, query);
        }
        return text.length() == 0 ? "/" : text;
    }

    public static String listSpiderData(
            String baseDir, String moduleCode, String spiderCode, int pageNum, int pageSize)
            throws Exception {
        JSONObject page = readSpiderDataPage(baseDir, moduleCode, spiderCode, pageNum, pageSize, true);
        return page.toString();
    }

    public static String spiderConfig(String baseDir, String spiderCode) throws Exception {
        requireSupportedSpider(MODULE_WHATSAPP, spiderCode);
        Path configPath = Paths.get(baseDir)
                .resolve("res")
                .resolve("spider")
                .resolve(spiderCode + ".cnf");
        if (!Files.exists(configPath)) {
            configPath = Paths.get("data")
                    .resolve("app")
                    .resolve("res")
                    .resolve("spider")
                    .resolve(spiderCode + ".cnf")
                    .toAbsolutePath()
                    .normalize();
        }
        if (!Files.exists(configPath)) {
            throw new IllegalArgumentException("missing local spider config: " + spiderCode);
        }
        String body = new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8);
        JSONObject config = new JSONObject(body);
        config.put("code", spiderCode);
        config.put("moduleCode", MODULE_WHATSAPP);
        return config.toString();
    }

    public static String getSpiderTableDataInfo(String baseDir, String queryJson) throws Exception {
        JSONObject query = parseJsonObject(queryJson);
        String spiderCode = query.optString("code", query.optString("spiderCode", SPIDER_WHATSAPP_USERS));
        int pageNum = query.optInt("pageNum", 1);
        int pageSize = query.optInt("pageSize", 10);
        JSONObject page = readSpiderDataPage(baseDir, MODULE_WHATSAPP, spiderCode, pageNum, pageSize, false);
        return page.toString();
    }

    private static JSONObject readSpiderDataPage(
            String baseDir,
            String moduleCode,
            String spiderCode,
            int pageNum,
            int pageSize,
            boolean wrapJsonData)
            throws Exception {
        requireSupportedSpider(moduleCode, spiderCode);
        Class.forName("org.sqlite.JDBC");
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1) {
            pageSize = 10;
        }
        Path dbPath = spiderDataDbPath(baseDir, moduleCode, spiderCode);
        JSONArray rows = new JSONArray();
        long total = 0L;
        if (Files.exists(dbPath)) {
            try (Connection conn = openSqlite(dbPath)) {
                try (Statement stmt = conn.createStatement();
                        ResultSet rs = stmt.executeQuery("select count(*) from spider_data")) {
                    total = rs.next() ? rs.getLong(1) : 0L;
                }
                try (PreparedStatement query =
                        conn.prepareStatement(
                                "select json_data,time,id from spider_data "
                                        + "order by time desc,id desc limit ? offset ?")) {
                    query.setInt(1, pageSize);
                    query.setInt(2, (pageNum - 1) * pageSize);
                    try (ResultSet rs = query.executeQuery()) {
                        while (rs.next()) {
                            JSONObject data = normalizeSpiderRow(rs.getString(1), rs.getLong(2));
                            if (wrapJsonData) {
                                rows.put(new JSONObject().put("jsonData", data.toString()));
                            } else {
                                rows.put(data);
                            }
                        }
                    }
                }
            }
        }
        System.out.println("M5D8_LOCAL_SPIDER_DATA_LIST moduleCode=" + moduleCode
                + " spiderCode=" + spiderCode
                + " pageNum=" + pageNum
                + " pageSize=" + pageSize
                + " total=" + total
                + " rows=" + rows.length());
        return new JSONObject()
                .put("code", 200)
                .put("msg", "success")
                .put("total", total)
                .put("rows", rows);
    }

    private static JSONObject normalizeSpiderRow(String jsonData, long time) {
        JSONObject data = parseJsonObject(jsonData);
        String[] keys = {"title", "url", "body", "googSite", "keywords", "pltCode", "phone"};
        for (String key : keys) {
            if (!data.has(key) || data.isNull(key)) {
                data.put(key, "");
            }
        }
        if (!data.has("date") || data.isNull("date") || data.optString("date").length() == 0) {
            data.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(time)));
        }
        return data;
    }

    public static String getTask(String baseDir, long taskId) throws Exception {
        Class.forName("org.sqlite.JDBC");
        Path dbPath = taskDbPath(baseDir, MODULE_WHATSAPP);
        if (!Files.exists(dbPath)) {
            System.out.println("M5C_COLLECT_LOCAL_TASK_MISSING taskId=" + taskId);
            return "{}";
        }
        try (Connection conn = openSqlite(dbPath)) {
            ensureTaskTable(conn);
            try (PreparedStatement stmt =
                    conn.prepareStatement(
                            "select data from rpa_task where type=? and task_seq=?")) {
                stmt.setString(1, "m5_local_spider");
                stmt.setString(2, String.valueOf(taskId));
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("M5C_COLLECT_LOCAL_TASK_READ taskId=" + taskId);
                        return parseTaskData(rs.getString(1)).getJSONObject("envelope").toString();
                    }
                }
            }
        }
        System.out.println("M5C_COLLECT_LOCAL_TASK_MISSING taskId=" + taskId);
        return "{}";
    }

    public static String listTasks(String baseDir, String moduleCode, String spiderCode) throws Exception {
        requireSupportedSpider(moduleCode, spiderCode);
        Class.forName("org.sqlite.JDBC");
        Path dbPath = taskDbPath(baseDir, moduleCode);
        JSONArray rows = new JSONArray();
        long total = 0L;
        if (Files.exists(dbPath)) {
            try (Connection conn = openSqlite(dbPath)) {
                ensureTaskTable(conn);
                try (PreparedStatement count =
                        conn.prepareStatement(
                                "select count(*) from rpa_task where module=? and rpa=? and type=?")) {
                    count.setString(1, moduleCode);
                    count.setString(2, spiderCode);
                    count.setString(3, "m5_local_spider");
                    try (ResultSet rs = count.executeQuery()) {
                        total = rs.next() ? rs.getLong(1) : 0L;
                    }
                }
                try (PreparedStatement query =
                        conn.prepareStatement(
                                "select task_seq,module,rpa,baseParams,status,error,time,data "
                                        + "from rpa_task where module=? and rpa=? and type=? "
                                        + "order by time desc,id desc limit 50")) {
                    query.setString(1, moduleCode);
                    query.setString(2, spiderCode);
                    query.setString(3, "m5_local_spider");
                    try (ResultSet rs = query.executeQuery()) {
                        while (rs.next()) {
                            JSONObject task = parseTaskData(rs.getString(8));
                            rows.put(
                                    new JSONObject()
                                            .put("taskId", Long.parseLong(rs.getString(1)))
                                            .put("moduleCode", rs.getString(2))
                                            .put("spiderCode", rs.getString(3))
                                            .put("spiderParams", rs.getString(4))
                                            .put("status", rs.getInt(5))
                                            .put("message", rs.getString(6))
                                            .put("createdTime", rs.getLong(7))
                                            .put("updatedTime", task.optLong("updatedTime", rs.getLong(7)))
                                            .put("retryCount", task.optInt("retryCount", 0))
                                            .put("total", task.optLong("total", 0L)));
                        }
                    }
                }
            }
        }
        return new JSONObject().put("code", 200).put("msg", "success").put("rows", rows).put("total", total).toString();
    }

    public static void updateTaskStatus(
            String baseDir, long taskId, int status, String message, Long count) throws Exception {
        Class.forName("org.sqlite.JDBC");
        Path dbPath = taskDbPath(baseDir, MODULE_WHATSAPP);
        Files.createDirectories(dbPath.getParent());
        try (Connection conn = openSqlite(dbPath)) {
            ensureTaskTable(conn);
            JSONObject task = null;
            try (PreparedStatement query =
                    conn.prepareStatement("select data from rpa_task where type=? and task_seq=?")) {
                query.setString(1, "m5_local_spider");
                query.setString(2, String.valueOf(taskId));
                try (ResultSet rs = query.executeQuery()) {
                    if (rs.next()) {
                        task = parseTaskData(rs.getString(1));
                    }
                }
            }
            if (task == null) {
                task = new JSONObject().put("taskId", taskId);
            }
            task.put("status", status);
            task.put("message", message == null ? "" : message);
            task.put("total", count == null ? 0L : count.longValue());
            task.put("updatedTime", System.currentTimeMillis());
            try (PreparedStatement stmt =
                    conn.prepareStatement(
                            "update rpa_task set status=?,error=?,data=?,time=? where type=? and task_seq=?")) {
                stmt.setInt(1, status);
                stmt.setString(2, message == null ? "" : message);
                stmt.setString(3, task.toString());
                stmt.setLong(4, System.currentTimeMillis());
                stmt.setString(5, "m5_local_spider");
                stmt.setString(6, String.valueOf(taskId));
                stmt.executeUpdate();
            }
        }
        System.out.println(
                "M5C_COLLECT_LOCAL_TASK_STATUS taskId="
                        + taskId
                        + " status="
                        + status
                        + " message="
                        + String.valueOf(message)
                        + " count="
                        + String.valueOf(count));
    }

    public static void finishDispatchedTask(String baseDir, long taskId, boolean success, String message)
            throws Exception {
        Class.forName("org.sqlite.JDBC");
        Path dbPath = taskDbPath(baseDir, MODULE_WHATSAPP);
        if (!Files.exists(dbPath)) {
            return;
        }
        try (Connection conn = openSqlite(dbPath)) {
            ensureTaskTable(conn);
            int currentStatus = 0;
            JSONObject task = null;
            try (PreparedStatement query =
                    conn.prepareStatement("select status,data from rpa_task where type=? and task_seq=?")) {
                query.setString(1, "m5_local_spider");
                query.setString(2, String.valueOf(taskId));
                try (ResultSet rs = query.executeQuery()) {
                    if (rs.next()) {
                        currentStatus = rs.getInt(1);
                        task = parseTaskData(rs.getString(2));
                    }
                }
            }
            if (task == null || currentStatus != 1) {
                return;
            }
            int finalStatus = success ? 2 : -1;
            String finalMessage = message == null || message.length() == 0
                    ? (success ? "executor returned" : "executor failed")
                    : message;
            task.put("status", finalStatus);
            task.put("message", finalMessage);
            task.put("updatedTime", System.currentTimeMillis());
            try (PreparedStatement update =
                    conn.prepareStatement(
                            "update rpa_task set status=?,error=?,data=?,time=? where type=? and task_seq=?")) {
                update.setInt(1, finalStatus);
                update.setString(2, finalMessage);
                update.setString(3, task.toString());
                update.setLong(4, System.currentTimeMillis());
                update.setString(5, "m5_local_spider");
                update.setString(6, String.valueOf(taskId));
                update.executeUpdate();
            }
            System.out.println(
                    "M5C_QUEUE_TASK_FINISHED taskId="
                            + taskId
                            + " status="
                            + finalStatus
                            + " message="
                            + finalMessage);
        }
    }

    public static String cancelAllRun(String baseDir, String moduleCode) throws Exception {
        if (!MODULE_WHATSAPP.equals(moduleCode)) {
            return new JSONObject().put("code", 200).put("cancelled", 0).put("moduleCode", moduleCode).toString();
        }
        Class.forName("org.sqlite.JDBC");
        Path dbPath = taskDbPath(baseDir, moduleCode);
        int cancelled = 0;
        if (Files.exists(dbPath)) {
            try (Connection conn = openSqlite(dbPath)) {
                ensureTaskTable(conn);
                JSONArray taskIds = new JSONArray();
                try (PreparedStatement query =
                        conn.prepareStatement(
                                "select task_seq,data from rpa_task where module=? and type=? and status in (0,1)")) {
                    query.setString(1, moduleCode);
                    query.setString(2, "m5_local_spider");
                    try (ResultSet rs = query.executeQuery()) {
                        while (rs.next()) {
                            taskIds.put(rs.getString(1));
                        }
                    }
                }
                for (int i = 0; i < taskIds.length(); i++) {
                    String taskId = taskIds.getString(i);
                    JSONObject task = null;
                    try (PreparedStatement query =
                            conn.prepareStatement("select data from rpa_task where type=? and task_seq=?")) {
                        query.setString(1, "m5_local_spider");
                        query.setString(2, taskId);
                        try (ResultSet rs = query.executeQuery()) {
                            if (rs.next()) {
                                task = parseTaskData(rs.getString(1));
                            }
                        }
                    }
                    if (task == null) {
                        task = new JSONObject().put("taskId", Long.parseLong(taskId));
                    }
                    task.put("status", -2);
                    task.put("message", "cancelled");
                    task.put("cancelRequested", true);
                    task.put("updatedTime", System.currentTimeMillis());
                    try (PreparedStatement update =
                            conn.prepareStatement(
                                    "update rpa_task set status=-2,error=?,data=?,time=? where type=? and task_seq=?")) {
                        update.setString(1, "cancelled");
                        update.setString(2, task.toString());
                        update.setLong(3, System.currentTimeMillis());
                        update.setString(4, "m5_local_spider");
                        update.setString(5, taskId);
                        cancelled += update.executeUpdate();
                    }
                }
            }
        }
        System.out.println("M5C_QUEUE_CANCEL_ALL_RUN moduleCode=" + moduleCode + " cancelled=" + cancelled);
        return new JSONObject()
                .put("code", 200)
                .put("msg", "success")
                .put("moduleCode", moduleCode)
                .put("cancelled", cancelled)
                .toString();
    }

    public static String previewTask(String moduleCode, String spiderCode, String spiderParamsJson) {
        requireSupportedSpider(moduleCode, spiderCode);
        String taskId = "local-preview-" + Math.abs(String.valueOf(spiderParamsJson).hashCode());
        System.out.println("M5A_LOCAL_SPIDER_TASK_PREVIEW taskId=" + taskId);
        return "{"
                + "\"code\":200,"
                + "\"dryRun\":true,"
                + "\"submitted\":false,"
                + "\"taskId\":\"" + escapeJson(taskId) + "\","
                + "\"moduleCode\":\"" + escapeJson(moduleCode) + "\","
                + "\"spiderCode\":\"" + escapeJson(spiderCode) + "\","
                + "\"spiderParams\":" + quoteOrEmptyObject(spiderParamsJson)
                + "}";
    }

    public static String writeMockResult(
            String baseDir, String moduleCode, String spiderCode, String jsonData) throws Exception {
        requireSupportedSpider(moduleCode, spiderCode);
        JSONObject written = writeSpiderData(baseDir, moduleCode, spiderCode, jsonData);
        long count = written.optLong("total");
        System.out.println(
                "M5A_LOCAL_SPIDER_RESULT_WRITTEN moduleCode="
                        + moduleCode
                        + " spiderCode="
                        + spiderCode
                        + " total="
                        + count);
        return "{"
                + "\"code\":200,"
                + "\"submitted\":false,"
                + "\"localOnly\":true,"
                + "\"moduleCode\":\"" + escapeJson(moduleCode) + "\","
                + "\"spiderCode\":\"" + escapeJson(spiderCode) + "\","
                + "\"dbPath\":\"" + escapeJson(written.optString("dbPath")) + "\","
                + "\"total\":" + count
                + "}";
    }

    public static boolean postCollectedData(Object spider, String jsonData) {
        return postCollectedData(resolveAppBaseDir(), spider, jsonData);
    }

    public static boolean postCollectedData(String baseDir, Object spider, String jsonData) {
        try {
            long taskId = reflectedLong(spider, "d");
            String spiderCode = reflectedString(spider, "e");
            if (!SPIDER_WHATSAPP_USERS.equals(spiderCode)) {
                spiderCode = firstNonBlank(localCloudSpiderCode, SPIDER_WHATSAPP_USERS);
            }
            String moduleCode = reflectedString(spider, "i");
            if (!MODULE_WHATSAPP.equals(moduleCode)) {
                moduleCode = MODULE_WHATSAPP;
            }
            requireSupportedSpider(moduleCode, spiderCode);
            JSONObject written = writeSpiderData(baseDir, moduleCode, spiderCode, jsonData);
            long count = written.optLong("total");
            if (taskId > 0L) {
                updateTaskStatus(
                        baseDir, taskId, 1, "local postData: collected " + count, Long.valueOf(count));
            }
            System.out.println(
                    "M5D_POSTDATA_LOCAL_SINK taskId="
                            + taskId
                            + " moduleCode="
                            + moduleCode
                            + " spiderCode="
                            + spiderCode
                            + " total="
                            + count
                            + " dbPath="
                            + written.optString("dbPath"));
            return true;
        } catch (Throwable error) {
            System.out.println("M5D_POSTDATA_LOCAL_SINK_FAILED error=" + String.valueOf(rootCause(error)));
            rootCause(error).printStackTrace(System.out);
            return false;
        }
    }

    public static void endCollectedTask(Object spider) {
        endCollectedTask(resolveAppBaseDir(), spider);
    }

    public static void endCollectedTask(String baseDir, Object spider) {
        try {
            long taskId = reflectedLong(spider, "d");
            String spiderCode = reflectedString(spider, "e");
            if (!SPIDER_WHATSAPP_USERS.equals(spiderCode)) {
                spiderCode = firstNonBlank(localCloudSpiderCode, SPIDER_WHATSAPP_USERS);
            }
            long count = countSpiderDataRows(baseDir, MODULE_WHATSAPP, spiderCode);
            if (taskId > 0L) {
                updateTaskStatus(baseDir, taskId, 2, "local endTask: completed", Long.valueOf(count));
            }
            System.out.println(
                    "M5D_ENDTASK_LOCAL_SINK taskId="
                            + taskId
                            + " spiderCode="
                            + spiderCode
                            + " total="
                            + count);
        } catch (Throwable error) {
            System.out.println("M5D_ENDTASK_LOCAL_SINK_FAILED error=" + String.valueOf(rootCause(error)));
            rootCause(error).printStackTrace(System.out);
        }
    }

    private static JSONObject writeSpiderData(
            String baseDir, String moduleCode, String spiderCode, String jsonData) throws Exception {
        requireSupportedSpider(moduleCode, spiderCode);
        Class.forName("org.sqlite.JDBC");
        Path dbDir = Paths.get(baseDir).resolve("data").resolve(moduleCode + "data");
        Files.createDirectories(dbDir);
        Path dbPath = dbDir.resolve("db_spider_data_" + spiderCode + ".data");
        String url = "jdbc:sqlite:" + dbPath.toAbsolutePath().toString();
        long now = System.currentTimeMillis();
        long count;
        try (Connection conn = openSqlite(dbPath)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                        "create table if not exists spider_data ("
                                + "spider_modal varchar, "
                                + "spider_code varchar, "
                                + "json_data varchar, "
                                + "time bigint, "
                                + "id integer primary key autoincrement)");
            }
            try (PreparedStatement insert =
                    conn.prepareStatement(
                            "insert into spider_data(spider_modal, spider_code, json_data, time) "
                                    + "values(?,?,?,?)")) {
                insert.setString(1, moduleCode);
                insert.setString(2, spiderCode);
                insert.setString(3, jsonData == null ? "{}" : jsonData);
                insert.setLong(4, now);
                insert.executeUpdate();
            }
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("select count(*) from spider_data")) {
                count = rs.next() ? rs.getLong(1) : 0L;
            }
        }
        return new JSONObject()
                .put("code", 200)
                .put("submitted", false)
                .put("localOnly", true)
                .put("moduleCode", moduleCode)
                .put("spiderCode", spiderCode)
                .put("dbPath", dbPath.toAbsolutePath().toString())
                .put("total", count);
    }

    private static void requireSupportedSpider(String moduleCode, String spiderCode) {
        if (!MODULE_WHATSAPP.equals(moduleCode) || !isSupportedWhatsappCollectSpider(spiderCode)) {
            throw new IllegalArgumentException(
                    "M5 local spider bridge only supports whatsapp collect tab spiders");
        }
    }

    private static boolean isSupportedWhatsappCollectSpider(String spiderCode) {
        for (String supported : WHATSAPP_COLLECT_TAB_SPIDERS) {
            if (supported.equals(spiderCode)) {
                return true;
            }
        }
        return false;
    }

    private static synchronized long nextTaskId() {
        long now = System.currentTimeMillis();
        if (now <= lastTaskId) {
            now = lastTaskId + 1L;
        }
        lastTaskId = now;
        return now;
    }

    private static Connection openSqlite(Path dbPath) throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("pragma busy_timeout=10000");
        }
        return conn;
    }

    private static long countSpiderDataRows(String baseDir, String moduleCode, String spiderCode)
            throws Exception {
        requireSupportedSpider(moduleCode, spiderCode);
        Class.forName("org.sqlite.JDBC");
        Path dbPath = spiderDataDbPath(baseDir, moduleCode, spiderCode);
        if (!Files.exists(dbPath)) {
            return 0L;
        }
        try (Connection conn = openSqlite(dbPath);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("select count(*) from spider_data")) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    private static Path spiderDataDbPath(String baseDir, String moduleCode, String spiderCode) {
        return Paths.get(baseDir)
                .resolve("data")
                .resolve(moduleCode + "data")
                .resolve("db_spider_data_" + spiderCode + ".data");
    }

    private static String resolveAppBaseDir() {
        String startAppBase = reflectedStaticString("com.sbf.main.StartApp", "a");
        if (isBlank(startAppBase)) {
            startAppBase = reflectedStaticString("com.sbf.main.StartApp", "b");
        }
        if (!isBlank(startAppBase)) {
            return startAppBase;
        }
        return Paths.get("data").resolve("app").toAbsolutePath().normalize().toString();
    }

    private static String reflectedStaticString(String className, String fieldName) {
        try {
            Class<?> type = Class.forName(className);
            Field field = findField(type, fieldName);
            if (field == null) {
                return "";
            }
            field.setAccessible(true);
            Object value = field.get(null);
            return value == null ? "" : String.valueOf(value);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static long reflectedLong(Object target, String fieldName) {
        Object value = reflectedValue(target, fieldName);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value != null) {
            try {
                return Long.parseLong(String.valueOf(value));
            } catch (NumberFormatException ignored) {
            }
        }
        return 0L;
    }

    private static String reflectedString(Object target, String fieldName) {
        Object value = reflectedValue(target, fieldName);
        return value == null ? "" : String.valueOf(value);
    }

    private static Object reflectedValue(Object target, String fieldName) {
        if (target == null) {
            return null;
        }
        try {
            Field field = findField(target.getClass(), fieldName);
            if (field == null) {
                return null;
            }
            field.setAccessible(true);
            return field.get(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static String firstNonBlank(String first, String fallback) {
        return isBlank(first) ? fallback : first;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static void insertQueuedTaskWithRetry(
            Path dbPath,
            long taskId,
            String moduleCode,
            String spiderCode,
            String spiderParamsJson,
            String taskConfigJson,
            JSONObject envelope)
            throws Exception {
        Exception last = null;
        for (int attempt = 1; attempt <= SQLITE_BUSY_RETRIES; attempt++) {
            try {
                insertQueuedTask(
                        dbPath,
                        taskId,
                        moduleCode,
                        spiderCode,
                        spiderParamsJson,
                        taskConfigJson,
                        envelope);
                return;
            } catch (Exception error) {
                last = error;
                if (!isSqliteBusy(error) || attempt == SQLITE_BUSY_RETRIES) {
                    break;
                }
                System.out.println(
                        "M5C_QUEUE_SQLITE_BUSY_RETRY op=submitTask attempt="
                                + attempt
                                + " db="
                                + dbPath.toAbsolutePath());
                sleep(SQLITE_BUSY_RETRY_DELAY_MS);
            }
        }
        throw last;
    }

    private static void insertQueuedTask(
            Path dbPath,
            long taskId,
            String moduleCode,
            String spiderCode,
            String spiderParamsJson,
            String taskConfigJson,
            JSONObject envelope)
            throws Exception {
        try (Connection conn = openSqlite(dbPath)) {
            ensureTaskTable(conn);
            JSONObject taskData = taskData(
                    taskId,
                    moduleCode,
                    spiderCode,
                    spiderParamsJson,
                    taskConfigJson,
                    envelope,
                    0,
                    "queued",
                    0,
                    0L);
            try (PreparedStatement insert =
                    conn.prepareStatement(
                            "insert into rpa_task("
                                    + "module,data,uuid,task_seq,type,baseParams,rpa,rpa_title,"
                                    + "scheduledTime,scheduled_time,status,need_review,error,time) "
                                    + "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                long now = System.currentTimeMillis();
                insert.setString(1, moduleCode);
                insert.setString(2, taskData.toString());
                insert.setString(3, "m5c-" + taskId);
                insert.setString(4, String.valueOf(taskId));
                insert.setString(5, "m5_local_spider");
                insert.setString(6, spiderParamsJson == null ? "{}" : spiderParamsJson);
                insert.setString(7, spiderCode);
                insert.setString(8, "AI采集");
                insert.setString(9, "");
                insert.setLong(10, 0L);
                insert.setInt(11, 0);
                insert.setInt(12, 0);
                insert.setString(13, "queued");
                insert.setLong(14, now);
                insert.executeUpdate();
            }
        }
    }

    private static boolean isSqliteBusy(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null
                    && (message.indexOf("SQLITE_BUSY") >= 0
                            || message.indexOf("database is locked") >= 0)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static Path taskDbPath(String baseDir, String moduleCode) {
        return Paths.get(baseDir)
                .resolve("data")
                .resolve("db_jtable_jrpatask.data");
    }

    private static void ensureTaskTable(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "create table if not exists rpa_task ("
                            + "module varchar,"
                            + "data varchar,"
                            + "uuid varchar,"
                            + "task_seq varchar,"
                            + "type varchar,"
                            + "baseParams varchar,"
                            + "rpa varchar,"
                            + "rpa_title varchar,"
                            + "scheduledTime varchar,"
                            + "scheduled_time bigint,"
                            + "status bigint,"
                            + "need_review bigint,"
                            + "error varchar,"
                            + "time bigint,"
                            + "id integer primary key autoincrement)");
        }
    }

    private static JSONObject taskData(
            long taskId,
            String moduleCode,
            String spiderCode,
            String spiderParamsJson,
            String taskConfigJson,
            JSONObject envelope,
            int status,
            String message,
            int retryCount,
            long total) {
        long now = System.currentTimeMillis();
        return new JSONObject()
                .put("taskId", taskId)
                .put("moduleCode", moduleCode)
                .put("spiderCode", spiderCode)
                .put("spiderParams", spiderParamsJson == null ? "{}" : spiderParamsJson)
                .put("taskConfig", taskConfigJson == null ? "{}" : taskConfigJson)
                .put("envelope", envelope)
                .put("status", status)
                .put("message", message == null ? "" : message)
                .put("retryCount", retryCount)
                .put("total", total)
                .put("cancelRequested", false)
                .put("createdTime", now)
                .put("updatedTime", now);
    }

    private static JSONObject parseTaskData(String data) {
        String text = data == null ? "" : data.trim();
        if (text.startsWith("{")) {
            return new JSONObject(text);
        }
        return new JSONObject();
    }

    private static JSONObject queueRow(JSONObject task, int status, String message) {
        JSONObject envelope = task.optJSONObject("envelope");
        JSONObject row = new JSONObject()
                .put("taskId", task.optLong("taskId"))
                .put("moduleCode", task.optString("moduleCode"))
                .put("spiderCode", task.optString("spiderCode"))
                .put("spiderParams", task.optString("spiderParams"))
                .put("taskConfig", task.optString("taskConfig"))
                .put("status", status)
                .put("message", message)
                .put("retryCount", task.optInt("retryCount", 0));
        if (envelope != null) {
            row.put("task", envelope.optJSONObject("task"));
            row.put("spider", envelope.optJSONObject("spider"));
            row.put("data", envelope);
        }
        return row;
    }

    private static JSONObject buildTaskEnvelope(
            String baseDir,
            long taskId,
            String moduleCode,
            String spiderCode,
            String spiderParamsJson,
            String taskConfigJson)
            throws Exception {
        JSONObject config = readSpiderConfig(baseDir, spiderCode);
        JSONArray params = normalizeSpiderParams(spiderParamsJson);
        JSONObject taskConfig = parseJsonObject(taskConfigJson);
        JSONObject task = new JSONObject();
        task.put("taskId", taskId);
        task.put("taskConfig", taskConfig.toString());
        task.put("spiderParams", params);
        task.put("moduleCode", moduleCode);
        task.put("spiderCode", spiderCode);
        JSONArray hookurls = config.optJSONArray("hookurls") == null ? new JSONArray() : config.optJSONArray("hookurls");
        JSONArray steps = config.optJSONArray("steps") == null ? new JSONArray() : config.optJSONArray("steps");
        JSONArray fields = config.optJSONArray("fields") == null ? localFields() : config.optJSONArray("fields");
        String homeUrl = config.optString("homeUrl", "https://www.google.com");
        String injectionjs = config.optString("injectionjs");
        String postApis = config.optString("postApis");
        String sipderJson = config.optString("sipderJson");
        task.put("homeUrl", homeUrl);
        task.put("injectionjs", injectionjs);
        task.put("postApis", postApis);
        task.put("sipderJson", sipderJson);
        task.put("hookurls", hookurls.toString());
        task.put("steps", steps.toString());
        task.put("fields", fields.toString());
        JSONObject data = new JSONObject();
        data.put("taskId", taskId);
        data.put("moduleCode", moduleCode);
        data.put("spiderCode", spiderCode);
        data.put("taskConfig", taskConfig.toString());
        data.put("spiderParams", params);
        for (int i = 0; i < params.length(); i++) {
            JSONObject item = params.getJSONObject(i);
            String key = item.optString("key", item.optString("code"));
            data.put(key, item.optString("value", item.optString("code")));
        }
        task.put("data", data);

        JSONObject spider = new JSONObject();
        spider.put("code", spiderCode);
        spider.put("homeUrl", homeUrl);
        spider.put("injectionjs", injectionjs);
        spider.put("postApis", postApis);
        spider.put("sipderJson", sipderJson);
        spider.put("hookurls", hookurls);
        spider.put("steps", steps);
        spider.put("fields", fields);

        JSONObject taskInfo = new JSONObject();
        taskInfo.put("taskId", taskId);
        taskInfo.put("moduleCode", moduleCode);
        taskInfo.put("spiderCode", spiderCode);
        taskInfo.put("spiderParams", params);
        taskInfo.put("taskConfig", taskConfig.toString());
        taskInfo.put("spiderMode", configOrEnv(taskConfig, "spiderMode", "M5_SPIDER_MODE", "google"));
        taskInfo.put("cookie", configOrEnv(taskConfig, "cookie", "M5_SPIDER_COOKIE", ""));
        taskInfo.put("proxy", configOrEnv(taskConfig, "proxy", "M5_SPIDER_PROXY", ""));
        taskInfo.put("spider_app_code", configOrEnv(taskConfig, "spider_app_code", "M5_SPIDER_APP_CODE", moduleCode));
        taskInfo.put("spider_exe_code", configOrEnv(taskConfig, "spider_exe_code", "M5_SPIDER_EXE_CODE", spiderCode));

        return new JSONObject()
                .put("taskId", taskId)
                .put("moduleCode", moduleCode)
                .put("spiderCode", spiderCode)
                .put("spiderParams", params)
                .put("taskConfig", taskConfig.toString())
                .put("spiderMode", taskInfo.optString("spiderMode"))
                .put("cookie", taskInfo.optString("cookie"))
                .put("proxy", taskInfo.optString("proxy"))
                .put("spider_app_code", taskInfo.optString("spider_app_code"))
                .put("spider_exe_code", taskInfo.optString("spider_exe_code"))
                .put("data", data)
                .put("task_data", data)
                .put("task_info", taskInfo)
                .put("taskData", data)
                .put("taskInfo", taskInfo)
                .put("task", task)
                .put("spider", spider);
    }

    private static JSONObject readSpiderConfig(String baseDir, String spiderCode) throws Exception {
        Path path = findSpiderConfigPath(baseDir, spiderCode);
        if (path != null) {
            byte[] bytes = Files.readAllBytes(path);
            return new JSONObject(new String(bytes, "UTF-8"));
        }
        return new JSONObject()
                .put("code", spiderCode)
                .put("moduleCode", MODULE_WHATSAPP)
                .put("homeUrl", "https://www.google.com")
                .put("spiderParams", localSpiderParams())
                .put("fields", localFields())
                .put("hookurls", new JSONArray())
                .put("steps", new JSONArray())
                .put("injectionjs", "")
                .put("sipderJson", "");
    }

    private static Path findSpiderConfigPath(String baseDir, String spiderCode) {
        Path[] candidates = {
            Paths.get(baseDir).resolve("res").resolve("spider").resolve(spiderCode + ".cnf"),
            Paths.get(baseDir).resolve("data").resolve("app").resolve("res").resolve("spider").resolve(spiderCode + ".cnf"),
            Paths.get("data").resolve("app").resolve("res").resolve("spider").resolve(spiderCode + ".cnf")
        };
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static JSONArray normalizeSpiderParams(String spiderParamsJson) {
        String trimmed = spiderParamsJson == null ? "" : spiderParamsJson.trim();
        JSONArray params = new JSONArray();
        if (trimmed.startsWith("[")) {
            JSONArray input = new JSONArray(trimmed);
            for (int i = 0; i < input.length(); i++) {
                JSONObject item = input.getJSONObject(i);
                String key = item.optString("key", item.optString("code"));
                String value = item.optString("value", item.optString("code"));
                params.put(param(key, value));
            }
            return params;
        }
        JSONObject object = trimmed.startsWith("{") ? new JSONObject(trimmed) : new JSONObject();
        params.put(param("googSite", object.optString("googSite", "google.com")));
        params.put(param("areaCode", object.optString("areaCode", "+1")));
        params.put(param("pltCode", object.optString("pltCode", "facebook.com")));
        params.put(param("keywords", object.optString("keywords", "")));
        return params;
    }

    private static JSONObject param(String key, String value) {
        return new JSONObject()
                .put("key", key)
                .put("code", value == null ? "" : value)
                .put("value", value == null ? "" : value);
    }

    private static String normalizeJsonObjectText(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }
        return "{}";
    }

    private static JSONObject parseJsonObject(String value) {
        return new JSONObject(normalizeJsonObjectText(value));
    }

    private static String configOrEnv(JSONObject config, String key, String envKey, String fallback) {
        String value = config.optString(key, "");
        if (value == null || value.length() == 0) {
            value = System.getenv(envKey);
        }
        if (value == null || value.length() == 0) {
            return fallback == null ? "" : fallback;
        }
        return value;
    }

    private static JSONArray localSpiderParams() {
        return new JSONArray()
                .put(new JSONObject().put("dpIndex", "1").put("code", "googSite").put("name", "搜索站点").put("type", "select"))
                .put(new JSONObject().put("dpIndex", "2").put("code", "areaCode").put("name", "国家/区号").put("type", "select"))
                .put(new JSONObject().put("dpIndex", "3").put("code", "pltCode").put("name", "平台").put("type", "select"))
                .put(new JSONObject().put("dpIndex", "4").put("code", "keywords").put("name", "关键词").put("type", "keyWords"));
    }

    private static JSONArray localFields() {
        return new JSONArray()
                .put(new JSONObject().put("dpIndex", "1").put("code", "googSite").put("name", "站点").put("type", "text"))
                .put(new JSONObject().put("dpIndex", "2").put("code", "pltCode").put("name", "来源平台").put("type", "text"))
                .put(new JSONObject().put("dpIndex", "3").put("code", "keywords").put("name", "相关关键词").put("type", "text"))
                .put(new JSONObject().put("dpIndex", "0").put("code", "phone").put("name", "线索").put("type", "text"))
                .put(new JSONObject().put("dpIndex", "7").put("code", "date").put("name", "采集时间").put("type", "text"))
                .put(new JSONObject().put("dpIndex", "8").put("code", "url").put("name", "网址").put("type", "text_url"));
    }

    private static void startLocalPipeline(final String baseDir, final String moduleCode) {
        Thread thread = new Thread(new LocalPipelineRunner(baseDir, moduleCode), "m5c-local-spider-dispatch");
        thread.setDaemon(true);
        thread.start();
    }

    private static final class LocalPipelineRunner implements Runnable {
        private final String baseDir;
        private final String moduleCode;

        private LocalPipelineRunner(String baseDir, String moduleCode) {
            this.baseDir = baseDir;
            this.moduleCode = moduleCode;
        }

        @Override
        public void run() {
            long taskId = 0L;
            try {
                Thread.sleep(1000L);
                JSONArray next = new JSONArray(M5LocalSpiderBridge.getNewTask(baseDir, moduleCode, 0));
                if (next.length() == 0) {
                    return;
                }
                JSONObject row = next.getJSONObject(0);
                taskId = row.optLong("taskId");
                String spiderCode = row.optString("spiderCode", SPIDER_WHATSAPP_USERS);
                System.out.println(
                        "M5C_COLLECT_LOCAL_PIPELINE_ENTER taskId="
                                + taskId
                                + " target=com.sbf.main.cloud.spider.a.a(Long)");
                ensureCloudSpiderContext(spiderCode);
                Object runner = getRegisteredCloudSpiderRunner(spiderCode);
                runner.getClass().getMethod("a", Long.class).invoke(runner, Long.valueOf(taskId));
                System.out.println("M5C_COLLECT_LOCAL_PIPELINE_DISPATCHED taskId=" + taskId);
            } catch (Throwable error) {
                Throwable root = rootCause(error);
                if (taskId > 0L) {
                    try {
                        M5LocalSpiderBridge.finishDispatchedTask(baseDir, taskId, false, String.valueOf(root));
                    } catch (Throwable ignored) {
                    }
                }
                System.out.println(
                        "M5C_COLLECT_LOCAL_PIPELINE_FAILED moduleCode="
                                + moduleCode
                                + " error="
                                + String.valueOf(root));
                rootCause(error).printStackTrace(System.out);
            }
        }
    }

    private static Throwable rootCause(Throwable error) {
        Throwable current = error;
        while (current instanceof InvocationTargetException
                && ((InvocationTargetException) current).getTargetException() != null) {
            current = ((InvocationTargetException) current).getTargetException();
        }
        return current == null ? error : current;
    }

    private static synchronized void ensureCloudSpiderContext(final String spiderCode) throws Exception {
        if (spiderCode.equals(localCloudSpiderCode)
                && localCloudSpiderContext != null
                && isCloudSpiderRegistered(spiderCode)) {
            return;
        }
        String route =
                "https://app.xdxsoft.com/pc/cloudSpider?spiderCode="
                        + java.net.URLEncoder.encode(spiderCode, "UTF-8");
        try {
            Class<?> componentClass =
                    Class.forName("com.sbf.main.spide.cloud.JSpiderCloude");
            localCloudSpiderContext =
                    componentClass.getConstructor(String.class).newInstance(route);
            localCloudSpiderCode = spiderCode;
            System.out.println(
                    "M5D_CLOUD_SPIDER_CONTEXT_CREATED spiderCode="
                            + spiderCode
                            + " route="
                            + route);
        } catch (Throwable error) {
            System.out.println(
                    "M5D_CLOUD_SPIDER_CONTEXT_ORIGINAL_FAILED spiderCode="
                            + spiderCode
                            + " error="
                            + String.valueOf(rootCause(error)));
            localCloudSpiderContext = null;
        }

        long start = System.currentTimeMillis();
        long originalDeadline = start + CLOUD_SPIDER_ORIGINAL_GRACE_MS;
        while (System.currentTimeMillis() < originalDeadline) {
            if (isCloudSpiderRegistered(spiderCode)) {
                System.out.println(
                        "M5D_CLOUD_SPIDER_CONTEXT_READY spiderCode=" + spiderCode);
                return;
            }
            Thread.sleep(200L);
        }

        ensureDirectCloudSpiderContext(spiderCode);
        long deadline = start + CLOUD_SPIDER_CONTEXT_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (isCloudSpiderRegistered(spiderCode)) {
                System.out.println(
                        "M5D_CLOUD_SPIDER_CONTEXT_READY spiderCode=" + spiderCode);
                return;
            }
            Thread.sleep(200L);
        }
        throw new IllegalStateException(
                "cloud spider context registration timed out: " + spiderCode);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void ensureDirectCloudSpiderContext(String spiderCode) throws Exception {
        Class<?> runnerClass = Class.forName("com.sbf.main.cloud.spider.a");
        Object runner =
                runnerClass
                        .getConstructor(String.class)
                        .newInstance(SPIDER_RUNNER_MODE_EXTERNAL_SEARCH);
        Class<?> masterClass = Class.forName("com.sbf.main.cloud.spider.JCloudSpiderMaster");
        Object master = masterClass.getMethod("a").invoke(null);
        Field registryField = masterClass.getDeclaredField("d");
        registryField.setAccessible(true);
        Object registry = registryField.get(master);
        if (!(registry instanceof Map)) {
            throw new IllegalStateException("cloud spider registry unavailable");
        }
        Map runners = (Map) registry;
        runners.put(SPIDER_RUNNER_MODE_EXTERNAL_SEARCH, runner);
        runners.put(spiderCode, runner);
        localBrowserContext = runner;
        System.out.println(
                "M5D_CLOUD_SPIDER_CONTEXT_DIRECT_READY spiderCode=" + spiderCode);
    }

    private static Object getRegisteredCloudSpiderRunner(String spiderCode) throws Exception {
        Class<?> masterClass = Class.forName("com.sbf.main.cloud.spider.JCloudSpiderMaster");
        Object master = masterClass.getMethod("a").invoke(null);
        Field registryField = masterClass.getDeclaredField("d");
        registryField.setAccessible(true);
        Object registry = registryField.get(master);
        if (!(registry instanceof Map)) {
            throw new IllegalStateException("cloud spider registry unavailable");
        }
        Map<?, ?> runners = (Map<?, ?>) registry;
        Object runner = runners.get(spiderCode);
        if (runner != null) {
            return runner;
        }
        for (Map.Entry<?, ?> entry : runners.entrySet()) {
            if (spiderCode.equals(String.valueOf(entry.getKey()))) {
                return entry.getValue();
            }
        }
        if (runners.size() == 1) {
            return runners.values().iterator().next();
        }
        throw new IllegalStateException("cloud spider runner missing: " + spiderCode);
    }

    private static boolean isCloudSpiderRegistered(String spiderCode) {
        try {
            Class<?> masterClass =
                    Class.forName("com.sbf.main.cloud.spider.JCloudSpiderMaster");
            Object master = masterClass.getMethod("a").invoke(null);
            Object registered = masterClass.getMethod("a", String.class).invoke(master, spiderCode);
            if (Boolean.TRUE.equals(registered)) {
                return true;
            }
            Field registryField = masterClass.getDeclaredField("d");
            registryField.setAccessible(true);
            Object registry = registryField.get(master);
            return registry instanceof Map && ((Map<?, ?>) registry).containsKey(spiderCode);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String quoteOrEmptyObject(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }
        return "{}";
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\':
                    out.append("\\\\");
                    break;
                case '"':
                    out.append("\\\"");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    out.append(ch);
                    break;
            }
        }
        return out.toString();
    }
}
