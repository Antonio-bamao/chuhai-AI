package com.sbf.main.jxbrowser;

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

public final class M5LocalSpiderBridge {
    private static final String MODULE_WHATSAPP = "whatsapp";
    private static final String SPIDER_WHATSAPP_USERS = "whatsapp_users_lists";
    private static long lastTaskId;

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
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath())) {
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
        JSONObject envelope = buildTaskEnvelope(baseDir, moduleCode, spiderCode, spiderParamsJson, taskConfigJson);
        Path dbPath = taskDbPath(baseDir, moduleCode);
        Files.createDirectories(dbPath.getParent());
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath())) {
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

    public static String getTask(String baseDir, long taskId) throws Exception {
        Class.forName("org.sqlite.JDBC");
        Path dbPath = taskDbPath(baseDir, MODULE_WHATSAPP);
        if (!Files.exists(dbPath)) {
            System.out.println("M5C_COLLECT_LOCAL_TASK_MISSING taskId=" + taskId);
            return "{}";
        }
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath())) {
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
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath())) {
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
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath())) {
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
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath())) {
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
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath())) {
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
        Class.forName("org.sqlite.JDBC");
        Path dbDir = Paths.get(baseDir).resolve("data").resolve(moduleCode + "data");
        Files.createDirectories(dbDir);
        Path dbPath = dbDir.resolve("db_spider_data_" + spiderCode + ".data");
        String url = "jdbc:sqlite:" + dbPath.toAbsolutePath().toString();
        long now = System.currentTimeMillis();
        long count;
        try (Connection conn = DriverManager.getConnection(url)) {
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
                + "\"dbPath\":\"" + escapeJson(dbPath.toAbsolutePath().toString()) + "\","
                + "\"total\":" + count
                + "}";
    }

    private static void requireSupportedSpider(String moduleCode, String spiderCode) {
        if (!MODULE_WHATSAPP.equals(moduleCode) || !SPIDER_WHATSAPP_USERS.equals(spiderCode)) {
            throw new IllegalArgumentException(
                    "M5 local spider bridge only supports whatsapp/whatsapp_users_lists");
        }
    }

    private static synchronized long nextTaskId() {
        long now = System.currentTimeMillis();
        if (now <= lastTaskId) {
            now = lastTaskId + 1L;
        }
        lastTaskId = now;
        return now;
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
            String moduleCode,
            String spiderCode,
            String spiderParamsJson,
            String taskConfigJson)
            throws Exception {
        JSONObject config = readSpiderConfig(baseDir, spiderCode);
        JSONArray params = normalizeSpiderParams(spiderParamsJson);
        JSONObject task = new JSONObject();
        task.put("taskConfig", normalizeJsonObjectText(taskConfigJson));
        task.put("spiderParams", params.toString());
        task.put("moduleCode", moduleCode);
        task.put("spiderCode", spiderCode);
        JSONObject data = new JSONObject();
        data.put("moduleCode", moduleCode);
        data.put("spiderCode", spiderCode);
        data.put("spiderParams", params.toString());
        for (int i = 0; i < params.length(); i++) {
            JSONObject item = params.getJSONObject(i);
            data.put(item.optString("code"), item.optString("value"));
        }
        task.put("data", data);

        JSONObject spider = new JSONObject();
        spider.put("code", spiderCode);
        spider.put("homeUrl", config.optString("homeUrl", "https://www.google.com"));
        spider.put("injectionjs", config.optString("injectionjs"));
        spider.put("postApis", config.optString("postApis"));
        spider.put("sipderJson", config.optString("sipderJson"));
        spider.put("hookurls", config.optJSONArray("hookurls") == null ? new JSONArray() : config.optJSONArray("hookurls"));
        spider.put("steps", config.optJSONArray("steps") == null ? new JSONArray() : config.optJSONArray("steps"));
        spider.put("fields", config.optJSONArray("fields") == null ? localFields() : config.optJSONArray("fields"));
        return new JSONObject().put("task", task).put("spider", spider);
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
                String code = item.optString("code", item.optString("key"));
                String value = item.optString("value", item.optString("code"));
                params.put(param(code, value));
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

    private static JSONObject param(String code, String value) {
        return new JSONObject()
                .put("key", code)
                .put("code", code)
                .put("value", value == null ? "" : value);
    }

    private static String normalizeJsonObjectText(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }
        return "{}";
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
                Class<?> runnerClass = Class.forName("com.sbf.main.cloud.spider.a");
                Object runner = runnerClass.getConstructor(String.class).newInstance(spiderCode);
                runnerClass.getMethod("a", Long.class).invoke(runner, Long.valueOf(taskId));
                M5LocalSpiderBridge.finishDispatchedTask(baseDir, taskId, true, "executor returned");
                System.out.println("M5C_COLLECT_LOCAL_PIPELINE_RETURN taskId=" + taskId);
            } catch (Throwable error) {
                if (taskId > 0L) {
                    try {
                        M5LocalSpiderBridge.finishDispatchedTask(baseDir, taskId, false, String.valueOf(error));
                    } catch (Throwable ignored) {
                    }
                }
                System.out.println(
                        "M5C_COLLECT_LOCAL_PIPELINE_FAILED moduleCode="
                                + moduleCode
                                + " error="
                                + String.valueOf(error));
            }
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
