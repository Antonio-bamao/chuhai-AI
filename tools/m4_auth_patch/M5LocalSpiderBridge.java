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

    public static String upsertWhatsAppAccount(
            String baseDir, String profileId, String phone, String status, String statusJson) throws Exception {
        Class.forName("org.sqlite.JDBC");
        String normalizedProfileId = normalizeProfileId(profileId);
        String normalizedPhone = phone == null ? "" : phone.trim();
        String normalizedStatus = isBlank(status) ? "unknown" : status.trim();
        String normalizedStatusJson = isBlank(statusJson) ? "{}" : statusJson;
        Path profilePath = whatsappProfilePath(baseDir, normalizedProfileId);
        Files.createDirectories(profilePath);
        Path dbPath = whatsappAccountDbPath(baseDir);
        Files.createDirectories(dbPath.getParent());
        long now = System.currentTimeMillis();
        try (Connection conn = openSqlite(dbPath)) {
            ensureWhatsAppAccountTable(conn);
            try (PreparedStatement update =
                    conn.prepareStatement(
                            "update b_whatsapp_accounts set phone=?,status=?,last_status_json=?,"
                                    + "profile_path=?,updated_at=? where profile_id=?")) {
                update.setString(1, normalizedPhone);
                update.setString(2, normalizedStatus);
                update.setString(3, normalizedStatusJson);
                update.setString(4, profilePath.toAbsolutePath().toString());
                update.setLong(5, now);
                update.setString(6, normalizedProfileId);
                if (update.executeUpdate() == 0) {
                    try (PreparedStatement insert =
                            conn.prepareStatement(
                                    "insert into b_whatsapp_accounts "
                                            + "(profile_id,phone,status,last_status_json,profile_path,created_at,updated_at) "
                                            + "values (?,?,?,?,?,?,?)")) {
                        insert.setString(1, normalizedProfileId);
                        insert.setString(2, normalizedPhone);
                        insert.setString(3, normalizedStatus);
                        insert.setString(4, normalizedStatusJson);
                        insert.setString(5, profilePath.toAbsolutePath().toString());
                        insert.setLong(6, now);
                        insert.setLong(7, now);
                        insert.executeUpdate();
                    }
                }
            }
        }
        JSONObject data =
                new JSONObject()
                        .put("profileId", normalizedProfileId)
                        .put("phone", normalizedPhone)
                        .put("status", normalizedStatus)
                        .put("profilePath", profilePath.toAbsolutePath().toString())
                        .put("updatedAt", now);
        System.out.println(
                "M8B1A_WHATSAPP_ACCOUNT_UPSERT profileId="
                        + normalizedProfileId
                        + " phone="
                        + normalizedPhone
                        + " status="
                        + normalizedStatus);
        return new JSONObject().put("code", 200).put("msg", "success").put("data", data).toString();
    }

    public static String listWhatsAppAccounts(String baseDir) throws Exception {
        Class.forName("org.sqlite.JDBC");
        Path dbPath = whatsappAccountDbPath(baseDir);
        String activeProfileId = activeWhatsAppProfileId(baseDir);
        JSONArray rows = new JSONArray();
        if (Files.exists(dbPath)) {
            try (Connection conn = openSqlite(dbPath)) {
                ensureWhatsAppAccountTable(conn);
                try (Statement stmt = conn.createStatement();
                        ResultSet rs =
                                stmt.executeQuery(
                                        "select profile_id,phone,status,last_status_json,profile_path,created_at,updated_at "
                                                + "from b_whatsapp_accounts order by updated_at desc,profile_id asc")) {
                    while (rs.next()) {
                        rows.put(
                                new JSONObject()
                                        .put("profileId", rs.getString(1))
                                        .put("phone", rs.getString(2))
                                        .put("status", rs.getString(3))
                                        .put("lastStatusJson", rs.getString(4))
                                        .put("profilePath", rs.getString(5))
                                        .put("createdAt", rs.getLong(6))
                                        .put("updatedAt", rs.getLong(7))
                                        .put("active", rs.getString(1).equals(activeProfileId)));
                    }
                }
            }
        }
        System.out.println("M8B1A_WHATSAPP_ACCOUNT_LIST total=" + rows.length());
        return new JSONObject().put("code", 200).put("msg", "success").put("rows", rows).put("total", rows.length()).toString();
    }

    public static String setActiveWhatsAppProfile(String baseDir, String profileId) throws Exception {
        Class.forName("org.sqlite.JDBC");
        String normalizedProfileId = normalizeProfileId(profileId);
        Path profilePath = whatsappProfilePath(baseDir, normalizedProfileId);
        Files.createDirectories(profilePath);
        Path dbPath = whatsappAccountDbPath(baseDir);
        Files.createDirectories(dbPath.getParent());
        long now = System.currentTimeMillis();
        try (Connection conn = openSqlite(dbPath)) {
            ensureWhatsAppAccountTable(conn);
            ensureWhatsAppStateTable(conn);
            ensureWhatsAppAccountRow(conn, normalizedProfileId, profilePath, now);
            try (PreparedStatement update =
                    conn.prepareStatement(
                            "insert or replace into b_whatsapp_state (state_key,state_value,updated_at) "
                                    + "values (?,?,?)")) {
                update.setString(1, "active_profile_id");
                update.setString(2, normalizedProfileId);
                update.setLong(3, now);
                update.executeUpdate();
            }
        }
        JSONObject data =
                new JSONObject()
                        .put("profileId", normalizedProfileId)
                        .put("profilePath", profilePath.toAbsolutePath().toString())
                        .put("updatedAt", now);
        System.out.println("M8B1C_WHATSAPP_ACTIVE_PROFILE_SET profileId=" + normalizedProfileId);
        return new JSONObject().put("code", 200).put("msg", "success").put("data", data).toString();
    }

    public static String getActiveWhatsAppProfile(String baseDir) throws Exception {
        Class.forName("org.sqlite.JDBC");
        String profileId = activeWhatsAppProfileId(baseDir);
        if (isBlank(profileId)) {
            profileId = firstWhatsAppProfileId(baseDir);
        }
        if (isBlank(profileId)) {
            profileId = "wa-default";
        }
        Path profilePath = whatsappProfilePath(baseDir, profileId);
        Files.createDirectories(profilePath);
        JSONObject data =
                new JSONObject()
                        .put("profileId", profileId)
                        .put("profilePath", profilePath.toAbsolutePath().toString());
        System.out.println("M8B1C_WHATSAPP_ACTIVE_PROFILE_GET profileId=" + profileId);
        return new JSONObject().put("code", 200).put("msg", "success").put("data", data).toString();
    }

    public static String upsertWhatsAppMessage(
            String baseDir,
            String profileId,
            String conversationKey,
            String contactPhone,
            String contactName,
            String direction,
            String sender,
            String messageText,
            long messageTime,
            String externalId,
            String rawJson)
            throws Exception {
        Class.forName("org.sqlite.JDBC");
        String normalizedProfileId = normalizeProfileId(profileId);
        String normalizedConversationKey =
                normalizeMessageKey(firstNonBlank(conversationKey, firstNonBlank(contactPhone, contactName)), "chat");
        String normalizedContactPhone = contactPhone == null ? "" : contactPhone.trim();
        String normalizedContactName = contactName == null ? "" : contactName.trim();
        String title = firstNonBlank(normalizedContactName, normalizedContactPhone);
        if (isBlank(title)) {
            title = normalizedConversationKey;
        }
        String normalizedDirection = isBlank(direction) ? "inbound" : direction.trim();
        String normalizedSender = sender == null ? "" : sender.trim();
        String normalizedMessageText = messageText == null ? "" : messageText.trim();
        long normalizedMessageTime = messageTime > 0L ? messageTime : System.currentTimeMillis();
        String normalizedRawJson = isBlank(rawJson) ? "{}" : rawJson;
        String messageId =
                normalizeMessageKey(
                        firstNonBlank(
                                externalId,
                                normalizedConversationKey
                                        + "|"
                                        + normalizedSender
                                        + "|"
                                        + normalizedMessageTime
                                        + "|"
                                        + normalizedMessageText),
                        "msg");
        String contactKey =
                normalizeMessageKey(firstNonBlank(normalizedContactPhone, normalizedConversationKey), "contact");
        Path dbPath = whatsappMessageDbPath(baseDir);
        Files.createDirectories(dbPath.getParent());
        long now = System.currentTimeMillis();
        try (Connection conn = openSqlite(dbPath)) {
            ensureWhatsAppMessageTables(conn);
            upsertWhatsAppContact(
                    conn,
                    normalizedProfileId,
                    contactKey,
                    normalizedContactPhone,
                    normalizedContactName,
                    normalizedRawJson,
                    now);
            upsertWhatsAppConversation(
                    conn,
                    normalizedProfileId,
                    normalizedConversationKey,
                    contactKey,
                    title,
                    normalizedMessageText,
                    normalizedMessageTime,
                    normalizedRawJson,
                    now);
            try (PreparedStatement insert =
                    conn.prepareStatement(
                            "insert or ignore into b_whatsapp_messages "
                                    + "(profile_id,conversation_key,message_id,direction,sender,message_text,"
                                    + "message_time,raw_json,created_at) values (?,?,?,?,?,?,?,?,?)")) {
                insert.setString(1, normalizedProfileId);
                insert.setString(2, normalizedConversationKey);
                insert.setString(3, messageId);
                insert.setString(4, normalizedDirection);
                insert.setString(5, normalizedSender);
                insert.setString(6, normalizedMessageText);
                insert.setLong(7, normalizedMessageTime);
                insert.setString(8, normalizedRawJson);
                insert.setLong(9, now);
                insert.executeUpdate();
            }
        }
        JSONObject data =
                new JSONObject()
                        .put("profileId", normalizedProfileId)
                        .put("conversationKey", normalizedConversationKey)
                        .put("contactKey", contactKey)
                        .put("contactPhone", normalizedContactPhone)
                        .put("contactName", normalizedContactName)
                        .put("direction", normalizedDirection)
                        .put("sender", normalizedSender)
                        .put("messageText", normalizedMessageText)
                        .put("messageTime", normalizedMessageTime)
                        .put("messageId", messageId);
        System.out.println(
                "M8B1B_WHATSAPP_MESSAGE_UPSERT profileId="
                        + normalizedProfileId
                        + " conversationKey="
                        + normalizedConversationKey
                        + " messageId="
                        + messageId);
        return new JSONObject().put("code", 200).put("msg", "success").put("data", data).toString();
    }

    public static String listWhatsAppConversations(String baseDir, String profileId) throws Exception {
        Class.forName("org.sqlite.JDBC");
        String normalizedProfileId = normalizeProfileId(profileId);
        Path dbPath = whatsappMessageDbPath(baseDir);
        JSONArray rows = new JSONArray();
        if (Files.exists(dbPath)) {
            try (Connection conn = openSqlite(dbPath)) {
                ensureWhatsAppMessageTables(conn);
                try (PreparedStatement query =
                        conn.prepareStatement(
                                "select profile_id,conversation_key,contact_key,title,last_message_text,"
                                        + "last_message_time,unread_count,raw_json,created_at,updated_at "
                                        + "from b_whatsapp_conversations where profile_id=? "
                                        + "order by last_message_time desc,updated_at desc,conversation_key asc")) {
                    query.setString(1, normalizedProfileId);
                    try (ResultSet rs = query.executeQuery()) {
                        while (rs.next()) {
                            rows.put(
                                    new JSONObject()
                                            .put("profileId", rs.getString(1))
                                            .put("conversationKey", rs.getString(2))
                                            .put("contactKey", rs.getString(3))
                                            .put("title", rs.getString(4))
                                            .put("lastMessageText", rs.getString(5))
                                            .put("lastMessageTime", rs.getLong(6))
                                            .put("unreadCount", rs.getLong(7))
                                            .put("rawJson", rs.getString(8))
                                            .put("createdAt", rs.getLong(9))
                                            .put("updatedAt", rs.getLong(10)));
                        }
                    }
                }
            }
        }
        System.out.println(
                "M8B1B_WHATSAPP_CONVERSATION_LIST profileId="
                        + normalizedProfileId
                        + " total="
                        + rows.length());
        return new JSONObject().put("code", 200).put("msg", "success").put("rows", rows).put("total", rows.length()).toString();
    }

    public static String listWhatsAppMessages(
            String baseDir, String profileId, String conversationKey) throws Exception {
        Class.forName("org.sqlite.JDBC");
        String normalizedProfileId = normalizeProfileId(profileId);
        String normalizedConversationKey = normalizeMessageKey(conversationKey, "chat");
        Path dbPath = whatsappMessageDbPath(baseDir);
        JSONArray rows = new JSONArray();
        if (Files.exists(dbPath)) {
            try (Connection conn = openSqlite(dbPath)) {
                ensureWhatsAppMessageTables(conn);
                try (PreparedStatement query =
                        conn.prepareStatement(
                                "select profile_id,conversation_key,message_id,direction,sender,message_text,"
                                        + "message_time,raw_json,created_at from b_whatsapp_messages "
                                        + "where profile_id=? and conversation_key=? "
                                        + "order by message_time asc,created_at asc,message_id asc")) {
                    query.setString(1, normalizedProfileId);
                    query.setString(2, normalizedConversationKey);
                    try (ResultSet rs = query.executeQuery()) {
                        while (rs.next()) {
                            rows.put(
                                    new JSONObject()
                                            .put("profileId", rs.getString(1))
                                            .put("conversationKey", rs.getString(2))
                                            .put("messageId", rs.getString(3))
                                            .put("direction", rs.getString(4))
                                            .put("sender", rs.getString(5))
                                            .put("messageText", rs.getString(6))
                                            .put("messageTime", rs.getLong(7))
                                            .put("rawJson", rs.getString(8))
                                            .put("createdAt", rs.getLong(9)));
                        }
                    }
                }
            }
        }
        System.out.println(
                "M8B1B_WHATSAPP_MESSAGE_LIST profileId="
                        + normalizedProfileId
                        + " conversationKey="
                        + normalizedConversationKey
                        + " total="
                        + rows.length());
        return new JSONObject().put("code", 200).put("msg", "success").put("rows", rows).put("total", rows.length()).toString();
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
        byte[] body = localWebAssetBytes(url);
        return body == null ? null : new String(body, StandardCharsets.UTF_8);
    }

    public static byte[] localWebAssetBytes(String url) {
        try {
            if (d8OriginalBackendPassThrough(url)) {
                System.out.println("D8_ONLINE_WEB_PASSTHROUGH url=" + String.valueOf(url));
                return null;
            }
            String d1XLocalPage = localD1XLocalPage(url);
            if (d1XLocalPage != null) {
                System.out.println("D1_X_LOCAL_PAGE url=" + String.valueOf(url));
                return d1XLocalPage.getBytes(StandardCharsets.UTF_8);
            }
            String d2InsLocalPage = localD2InsLocalPage(url);
            if (d2InsLocalPage != null) {
                System.out.println("D2_INS_LOCAL_PAGE url=" + String.valueOf(url));
                return d2InsLocalPage.getBytes(StandardCharsets.UTF_8);
            }
            String d3FbLocalPage = localD3FbLocalPage(url);
            if (d3FbLocalPage != null) {
                System.out.println("D3_FB_LOCAL_PAGE url=" + String.valueOf(url));
                return d3FbLocalPage.getBytes(StandardCharsets.UTF_8);
            }
            String d4TkLocalPage = localD4TkLocalPage(url);
            if (d4TkLocalPage != null) {
                System.out.println("D4_TK_LOCAL_PAGE url=" + String.valueOf(url));
                return d4TkLocalPage.getBytes(StandardCharsets.UTF_8);
            }
            String d5TgLocalPage = localD5TgLocalPage(url);
            if (d5TgLocalPage != null) {
                System.out.println("D5_TG_LOCAL_PAGE url=" + String.valueOf(url));
                return d5TgLocalPage.getBytes(StandardCharsets.UTF_8);
            }
            String d5GeoLocalPage = localD5GeoLocalPage(url);
            if (d5GeoLocalPage != null) {
                System.out.println("D5_GEO_LOCAL_PAGE url=" + String.valueOf(url));
                return d5GeoLocalPage.getBytes(StandardCharsets.UTF_8);
            }
            String d5WaLocalPage = localD5WaLocalPage(url);
            if (d5WaLocalPage != null) {
                System.out.println("D5_WA_LOCAL_PAGE url=" + String.valueOf(url));
                return d5WaLocalPage.getBytes(StandardCharsets.UTF_8);
            }
            String c6CommercePage = localC6CommercePage(url);
            if (c6CommercePage != null) {
                System.out.println("C6_COMMERCE_LOCAL_PAGE url=" + String.valueOf(url));
                return c6CommercePage.getBytes(StandardCharsets.UTF_8);
            }
            if ("/static/img/cloudWords_background.fd301aa6.jpg"
                    .equals(normalizedUrlPath(url))) {
                System.out.println("C5_STATIC_IMAGE_STUB " + String.valueOf(url));
                return java.util.Base64.getDecoder().decode(
                        "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////2wBDAf//////////////////////////////////////////////////////////////////////////////////////wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAf/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIQAxAAAAF//8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABBQJ//8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/aAAgBAwEBPwF//8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/aAAgBAgEBPwF//8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQAGPwJ//8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPyF//9oADAMBAAIAAwAAAB//xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oACAEDAQE/EH//xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oACAECAQE/EH//xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oACAEBAAE/EH//2Q==");
            }
            if (url != null
                    && (url.indexOf("tos-public.volccdn.com") >= 0
                            || url.indexOf("tos.umd.production.min.js") >= 0)) {
                System.out.println("M5D8_LOCAL_WEB_ASSET_TOS_STUB " + String.valueOf(url));
                return "window.TOS=window.TOS||{};".getBytes(StandardCharsets.UTF_8);
            }
            String terminalJson = localWebTerminalJson(url);
            if (terminalJson != null) {
                System.out.println("M8D17_LOCAL_WEB_JSON url=" + String.valueOf(url));
                return terminalJson.getBytes(StandardCharsets.UTF_8);
            }
            String untrustedHostBlock = localC62ExternalRequestBlock(url, false);
            if (untrustedHostBlock != null) {
                System.out.println("C62_EXTERNAL_REQUEST_BLOCKED url=" + String.valueOf(url));
                return untrustedHostBlock.getBytes(StandardCharsets.UTF_8);
            }
            Path asset = localWebAssetPath(url);
            if (asset == null || !Files.exists(asset)) {
                String blocked = localC62ExternalRequestBlock(url, true);
                if (blocked != null) {
                    System.out.println("C62_EXTERNAL_REQUEST_BLOCKED url=" + String.valueOf(url));
                    return blocked.getBytes(StandardCharsets.UTF_8);
                }
                return null;
            }
            System.out.println("M5D8_LOCAL_WEB_ASSET " + asset.toAbsolutePath());
            byte[] raw = Files.readAllBytes(asset);
            String path = normalizedUrlPath(url).toLowerCase();
            if (!(path.endsWith(".html")
                    || path.endsWith(".js")
                    || path.endsWith(".css")
                    || path.startsWith("/pc/")
                    || path.startsWith("/aiagent/")
                    || path.startsWith("/es/")
                    || path.startsWith("/wsclaw/"))) {
                return raw;
            }
            String body = new String(raw, StandardCharsets.UTF_8);
            if ("aicloud.html".equals(asset.getFileName().toString())) {
                body =
                        body.replace(
                                "<script src=https://tos-public.volccdn.com/obj/volc-tos-public/@volcengine/tos-sdk@latest/browser/tos.umd.production.min.js></script>",
                                "<script>window.TOS=window.TOS||{};</script>");
            }
            body = patchLocalWebAssetBody(asset.getFileName().toString(), body);
            return body.getBytes(StandardCharsets.UTF_8);
        } catch (Throwable error) {
            System.out.println("M5D8_LOCAL_WEB_ASSET_FAILED url=" + String.valueOf(url)
                    + " error=" + String.valueOf(rootCause(error)));
            return null;
        }
    }

    private static boolean d8OriginalBackendPassThrough(String url) {
        if (!d8OnlineEnabled() || url == null) {
            return false;
        }
        try {
            java.net.URI uri = new java.net.URI(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    || host == null) {
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

    public static String normalizeC6CommerceRoute(String url) {
        if (url == null) {
            return null;
        }
        String path = normalizedUrlPath(url);
        if ("/pc/alipay/enterpriseAuth".equals(path)
                || "/pc/alipay/personal/auth".equals(path)
                || "/pc/userPayofflineOrder/my".equals(path)) {
            System.out.println("C6_RUNTIME_ROUTE_NORMALIZED recharge=" + path);
            return "/pc/c6/recharge";
        }
        if ("/views/overseasAds/dataBoard".equals(path)
                || "/views/overseasAds/adsPeople".equals(path)
                || "/views/overseasAds/addTask".equals(path)) {
            System.out.println("C6_RUNTIME_ROUTE_NORMALIZED advertising=" + path);
            return "/pc/c6/advertising";
        }
        return url;
    }

    private static String localD1XLocalPage(String url) {
        String path = normalizedUrlPath(url);
        String[] page = null;
        if ("/pc/local/x/account-login".equals(path)) {
            page = new String[] {"account-login", "X 账号登录", "账号信息未在本地保存，登录能力尚未接入。", "登录 X 账号"};
        } else if ("/pc/local/x/precise-search".equals(path)) {
            page = new String[] {"precise-search", "X 精准搜索", "未配置关键词或搜索任务，本页不创建任务。", "提交精准搜索"};
        } else if ("/pc/local/x/peer-followers".equals(path)) {
            page = new String[] {"peer-followers", "X 同行的粉丝搜索", "未配置同行主页或采集范围，本页不提交采集。", "提交粉丝搜索"};
        } else if ("/pc/local/x/active-filter".equals(path)) {
            page = new String[] {"active-filter", "X 筛选活跃", "没有本地账号或活跃判定结果，本页不执行筛选。", "开始活跃筛选"};
        } else if ("/pc/local/x/profile-database".equals(path)) {
            page = new String[] {"profile-database", "X 主页大数据库", "本地未保存主页数据，本页不启动采集。", "采集主页数据"};
        } else if ("/pc/local/x/android-agent".equals(path)) {
            page = new String[] {"android-agent", "X 安卓智能体", "安卓设备与智能体运行链未接入，本页不启动设备。", "启动安卓智能体"};
        } else if ("/pc/local/x/aicloud-fingerprint".equals(path)) {
            page = new String[] {"aicloud-fingerprint", "X AiCloud指纹", "本地没有可绑定的 AiCloud 指纹数据。", "绑定 AiCloud 指纹"};
        } else if ("/pc/local/x/adspower-fingerprint".equals(path)) {
            page = new String[] {"adspower-fingerprint", "X AdsPower指纹", "本地没有可绑定的 AdsPower 指纹数据。", "绑定 AdsPower 指纹"};
        } else if ("/pc/local/x/jump-push".equals(path)) {
            page = new String[] {"jump-push", "X 跳推系统", "未配置跳推账号或发送目标，本页不触发发送。", "开始跳推"};
        }
        if (page == null) {
            return null;
        }
        return "<!doctype html><html><head><meta charset=\"UTF-8\"><title>"
                + page[1]
                + "</title><style>body{margin:0;background:#f7f8fa;color:#303133;font:14px Arial,sans-serif;}main{max-width:760px;margin:64px auto;padding:32px;background:#fff;border:1px solid #ebeef5;border-radius:6px;}h1{margin:0 0 16px;font-size:22px;}p{line-height:1.7;color:#606266;}button{margin-top:12px;padding:9px 18px;border:0;border-radius:4px;background:#c0c4cc;color:#fff;}</style></head><body><main data-d1-x-route=\""
                + page[0]
                + "\"><h1>"
                + page[1]
                + "</h1><p>D1_X_LOCAL_PAGE</p><p>离线提示："
                + page[2]
                + "</p><button data-d1-action=\"guarded\" disabled>"
                + page[3]
                + "</button></main></body></html>";
    }

    private static String localD2InsLocalPage(String url) {
        String path = normalizedUrlPath(url);
        String[] page = null;
        if ("/pc/local/ins/account-login".equals(path)) {
            page = new String[] {"account-login", "Ins 帐号登录", "本地未保存 Ins 帐号信息，登录能力尚未接入。", "登录 Ins 帐号"};
        } else if ("/pc/local/ins/account-search".equals(path)) {
            page = new String[] {"account-search", "Ins 帐号搜索", "未配置帐号搜索条件，本页不提交搜索任务。", "提交帐号搜索"};
        } else if ("/pc/local/ins/post-search".equals(path)) {
            page = new String[] {"post-search", "Ins 帖子搜索", "未配置帖子关键词或搜索范围，本页不提交搜索任务。", "提交帖子搜索"};
        } else if ("/pc/local/ins/profile-mining".equals(path)) {
            page = new String[] {"profile-mining", "Ins 主页挖掘", "本地未保存主页挖掘范围，本页不启动采集。", "采集主页数据"};
        } else if ("/pc/local/ins/active-filter".equals(path)) {
            page = new String[] {"active-filter", "Ins 筛选活跃", "没有本地帐号或活跃判定结果，本页不执行筛选。", "开始活跃筛选"};
        } else if ("/pc/local/ins/api-broadcast".equals(path)) {
            page = new String[] {"api-broadcast", "Ins 接口群发", "未配置发送帐号或目标，本页不触发群发。", "开始接口群发"};
        } else if ("/pc/local/ins/android-agent".equals(path)) {
            page = new String[] {"android-agent", "Ins 安卓智能体", "安卓设备与智能体运行链未接入，本页不启动设备。", "启动安卓智能体"};
        } else if ("/pc/local/ins/aicloud-fingerprint".equals(path)) {
            page = new String[] {"aicloud-fingerprint", "Ins AiCloud指纹", "本地没有可绑定的 AiCloud 指纹数据。", "绑定 AiCloud 指纹"};
        } else if ("/pc/local/ins/adspower-fingerprint".equals(path)) {
            page = new String[] {"adspower-fingerprint", "Ins AdsPower指纹", "本地没有可绑定的 AdsPower 指纹数据。", "绑定 AdsPower 指纹"};
        }
        if (page == null) {
            return null;
        }
        return "<!doctype html><html><head><meta charset=\"UTF-8\"><title>"
                + page[1]
                + "</title><style>body{margin:0;background:#f7f8fa;color:#303133;font:14px Arial,sans-serif;}main{max-width:760px;margin:64px auto;padding:32px;background:#fff;border:1px solid #ebeef5;border-radius:6px;}h1{margin:0 0 16px;font-size:22px;}p{line-height:1.7;color:#606266;}button{margin-top:12px;padding:9px 18px;border:0;border-radius:4px;background:#c0c4cc;color:#fff;}</style></head><body><main data-d2-ins-route=\""
                + page[0]
                + "\"><h1>"
                + page[1]
                + "</h1><p>D2_INS_LOCAL_PAGE</p><p>离线提示："
                + page[2]
                + "</p><button data-d1-action=\"guarded\" disabled>"
                + page[3]
                + "</button></main></body></html>";
    }

    private static String localD3FbLocalPage(String url) {
        String path = normalizedUrlPath(url);
        String[] page = null;
        if ("/pc/local/fb/mirror-settings".equals(path)) {
            page = new String[] {"mirror-settings", "镜像系统设置", "本地没有可保存的镜像系统配置，本页不写入设置。", "保存镜像系统设置"};
        } else if ("/pc/local/fb/friends-collect".equals(path)) {
            page = new String[] {"friends-collect", "FB 好友采集", "未配置好友采集条件或范围，本页不提交采集。", "提交好友采集"};
        } else if ("/pc/local/fb/groups-collect".equals(path)) {
            page = new String[] {"groups-collect", "FB 小组采集", "未配置小组采集条件或范围，本页不提交采集。", "提交小组采集"};
        } else if ("/pc/local/fb/pages-collect".equals(path)) {
            page = new String[] {"pages-collect", "FB 主页采集", "未配置主页采集条件或范围，本页不提交采集。", "提交主页采集"};
        } else if ("/pc/local/fb/live-collect".equals(path)) {
            page = new String[] {"live-collect", "FB 直播采集", "未配置直播采集条件或范围，本页不提交采集。", "提交直播采集"};
        } else if ("/pc/local/fb/ads-collect".equals(path)) {
            page = new String[] {"ads-collect", "FB 广告采集", "未配置广告采集条件或范围，本页不提交采集。", "提交广告采集"};
        } else if ("/pc/local/fb/ad-comment-intercept".equals(path)) {
            page = new String[] {"ad-comment-intercept", "FB 广告评论截流", "本地没有广告评论或截流规则，本页不启动截流。", "开始广告评论截流"};
        } else if ("/pc/local/fb/video-intercept".equals(path)) {
            page = new String[] {"video-intercept", "FB 视频截流", "本地没有视频或截流规则，本页不启动截流。", "开始视频截流"};
        } else if ("/pc/local/fb/active-user-check".equals(path)) {
            page = new String[] {"active-user-check", "FB 活跃用户检测", "本地没有帐号或活跃检测结果，本页不执行检测。", "开始活跃用户检测"};
        } else if ("/pc/local/fb/inquiry-reply".equals(path)) {
            page = new String[] {"inquiry-reply", "FB 询盘回复", "未配置询盘或回复目标，本页不触发回复。", "开始询盘回复"};
        }
        if (page == null) {
            return null;
        }
        return "<!doctype html><html><head><meta charset=\"UTF-8\"><title>"
                + page[1]
                + "</title><style>body{margin:0;background:#f7f8fa;color:#303133;font:14px Arial,sans-serif;}main{max-width:760px;margin:64px auto;padding:32px;background:#fff;border:1px solid #ebeef5;border-radius:6px;}h1{margin:0 0 16px;font-size:22px;}p{line-height:1.7;color:#606266;}button{margin-top:12px;padding:9px 18px;border:0;border-radius:4px;background:#c0c4cc;color:#fff;}</style></head><body><main data-d3-fb-route=\""
                + page[0]
                + "\"><h1>"
                + page[1]
                + "</h1><p>D3_FB_LOCAL_PAGE</p><p>离线提示："
                + page[2]
                + "</p><button data-d1-action=\"guarded\" disabled>"
                + page[3]
                + "</button></main></body></html>";
    }

    private static String localD4TkLocalPage(String url) {
        String path = normalizedUrlPath(url); String[] page = null;
        if ("/pc/local/tiktok/ai-collect".equals(path)) page = new String[] {"ai-collect", "TK AI采集", "未配置采集条件或范围，本页不提交采集。", "提交 AI采集"};
        else if ("/pc/local/tiktok/ai-filter".equals(path)) page = new String[] {"ai-filter", "TK AI筛选", "本地没有可筛选的数据，本页不执行筛选。", "开始 AI筛选"};
        else if ("/pc/local/tiktok/mirror-system".equals(path)) page = new String[] {"mirror-system", "TK 镜像系统", "本地没有可保存的镜像设置，本页不写入配置。", "保存镜像设置"};
        else if ("/pc/local/tiktok/ios-multi-account".equals(path)) page = new String[] {"ios-multi-account", "TK IOS多号", "未接入 iOS 设备或帐号，本页不启动多号操作。", "启动 IOS多号"};
        else if ("/pc/local/tiktok/ai-super-account".equals(path)) page = new String[] {"ai-super-account", "TK AI超级号", "本地没有可用超级号，本页不启动帐号操作。", "启用 AI超级号"};
        else if ("/pc/local/tiktok/api-publish".equals(path)) page = new String[] {"api-publish", "TK API发布", "未配置发布帐号或内容，本页不触发发布。", "提交 API发布"};
        else if ("/pc/local/tiktok/ai-live".equals(path)) page = new String[] {"ai-live", "TK AI直播", "直播设备与任务未接入，本页不启动直播。", "启动 AI直播"};
        else if ("/pc/local/tiktok/cloud-collect".equals(path)) page = new String[] {"cloud-collect", "TK 云采集", "未配置云采集条件，本页不提交采集。", "提交云采集"};
        else if ("/pc/local/tiktok/trending".equals(path)) page = new String[] {"trending", "TK AI上热门", "未配置帐号或推广目标，本页不触发操作。", "开始上热门"};
        else if ("/pc/local/tiktok/cloud-filter".equals(path)) page = new String[] {"cloud-filter", "TK 云筛选", "本地没有可筛选的数据，本页不执行筛选。", "开始云筛选"};
        if (page == null) return null;
        return "<!doctype html><html><head><meta charset=\"UTF-8\"><title>" + page[1] + "</title><style>body{margin:0;background:#f7f8fa;color:#303133;font:14px Arial,sans-serif;}main{max-width:760px;margin:64px auto;padding:32px;background:#fff;border:1px solid #ebeef5;border-radius:6px;}h1{margin:0 0 16px;font-size:22px;}p{line-height:1.7;color:#606266;}button{margin-top:12px;padding:9px 18px;border:0;border-radius:4px;background:#c0c4cc;color:#fff;}</style></head><body><main data-d4-tk-route=\"" + page[0] + "\"><h1>" + page[1] + "</h1><p>D4_TK_LOCAL_PAGE</p><p>离线提示：" + page[2] + "</p><button data-d1-action=\"guarded\" disabled>" + page[3] + "</button></main></body></html>";
    }

    private static String localD5TgLocalPage(String url) {
        String path = normalizedUrlPath(url);
        String[] page = null;
        if ("/pc/local/tg/jump-push".equals(path)) {
            page = new String[] {"jump-push", "TG 跳推系统", "未配置跳推帐号或发送目标，本页不触发发送。", "启动跳推系统"};
        } else if ("/pc/local/tg/accounts".equals(path)) {
            page = new String[] {"accounts", "TG 帐号", "本地未保存 TG 帐号，帐号管理能力尚未接入。", "管理 TG 帐号"};
        } else if ("/pc/local/tg/ai-collect".equals(path)) {
            page = new String[] {"ai-collect", "TG AI 采集", "未配置采集条件或范围，本页不提交采集。", "提交 TG AI采集"};
        } else if ("/pc/local/tg/ai-data".equals(path)) {
            page = new String[] {"ai-data", "TG AI数据", "本地未保存 TG 数据，本页不读取或修改数据。", "查看 TG AI数据"};
        } else if ("/pc/local/tg/group-collect".equals(path)) {
            page = new String[] {"group-collect", "TG AI 群采集", "未配置群采集条件或范围，本页不提交采集。", "提交 TG AI群采集"};
        } else if ("/pc/local/tg/group-member-extract".equals(path)) {
            page = new String[] {"group-member-extract", "TG AI 群成员提取", "未配置 TG 群或成员范围，本页不提取成员。", "提取 TG 群成员"};
        } else if ("/pc/local/tg/ai-filter".equals(path)) {
            page = new String[] {"ai-filter", "TG AI筛选", "本地没有可筛选的数据，本页不执行筛选。", "开始 TG AI筛选"};
        } else if ("/pc/local/tg/ai-growth".equals(path)) {
            page = new String[] {"ai-growth", "TG AI裂变", "未配置帐号或裂变目标，本页不启动裂变。", "启动 TG AI裂变"};
        } else if ("/pc/local/tg/android-agent".equals(path)) {
            page = new String[] {"android-agent", "TG 安卓智能体", "安卓设备与智能体运行链未接入，本页不启动设备。", "启动 TG 安卓智能体"};
        } else if ("/pc/local/tg/aicloud-fingerprint".equals(path)) {
            page = new String[] {"aicloud-fingerprint", "TG AiCloud指纹", "本地没有可绑定的 TG AiCloud 指纹数据。", "绑定 TG AiCloud指纹"};
        } else if ("/pc/local/tg/adspower-fingerprint".equals(path)) {
            page = new String[] {"adspower-fingerprint", "TG AdsPower指纹", "本地没有可绑定的 TG AdsPower 指纹数据。", "绑定 TG AdsPower指纹"};
        }
        if (page == null) {
            return null;
        }
        return "<!doctype html><html><head><meta charset=\"UTF-8\"><title>" + page[1]
                + "</title><style>body{margin:0;background:#f7f8fa;color:#303133;font:14px Arial,sans-serif;}main{max-width:760px;margin:64px auto;padding:32px;background:#fff;border:1px solid #ebeef5;border-radius:6px;}h1{margin:0 0 16px;font-size:22px;}p{line-height:1.7;color:#606266;}button{margin-top:12px;padding:9px 18px;border:0;border-radius:4px;background:#c0c4cc;color:#fff;}</style></head><body><main data-d5-tg-route=\""
                + page[0] + "\"><h1>" + page[1] + "</h1><p>D5_TG_LOCAL_PAGE</p><p>离线提示："
                + page[2] + "</p><button data-d1-action=\"guarded\" disabled>" + page[3]
                + "</button></main></body></html>";
    }

    private static String localD5GeoLocalPage(String url) {
        String path = normalizedUrlPath(url);
        String[] page = null;
        if ("/pc/local/geo/google-seo".equals(path)) page = new String[] {"google-seo", "精准官网挖掘", "未配置官网挖掘关键词或地区，本页不发起搜索。", "开始官网挖掘"};
        else if ("/pc/local/geo/precise-number-mining".equals(path)) page = new String[] {"precise-number-mining", "精准号码挖掘", "未配置号码挖掘条件，本页不发起检索。", "开始号码挖掘"};
        else if ("/pc/local/geo/google-geo-media".equals(path)) page = new String[] {"google-geo-media", "Google GEO外媒体", "本地未保存 GEO 外媒体数据，本页不读取外部媒体。", "查看 GEO外媒体"};
        else if ("/pc/local/geo/global-number-collect".equals(path)) page = new String[] {"global-number-collect", "全球号码采集", "未配置号码采集条件或范围，本页不提交采集。", "开始全球号码采集"};
        else if ("/pc/local/geo/global-region-collect".equals(path)) page = new String[] {"global-region-collect", "全球地区采集", "未配置地区采集范围，本页不提交采集。", "开始全球地区采集"};
        else if ("/pc/local/geo/customs-data-mining".equals(path)) page = new String[] {"customs-data-mining", "海关数据挖掘", "本地没有可挖掘的海关数据，本页不发起查询。", "开始海关数据挖掘"};
        else if ("/pc/local/geo/global-company-data".equals(path)) page = new String[] {"global-company-data", "全球企业大数据", "本地未保存全球企业数据，本页不读取或修改数据。", "查看全球企业数据"};
        else if ("/pc/local/geo/global-big-data".equals(path)) page = new String[] {"global-big-data", "全球大数据", "本地未保存全球大数据，本页不读取或修改数据。", "查看全球大数据"};
        else if ("/pc/local/geo/number-ai-active-filter".equals(path)) page = new String[] {"number-ai-active-filter", "号码 AI筛选活跃", "本地没有可筛选的号码数据，本页不执行筛选。", "开始号码 AI筛选"};
        if (page == null) return null;
        return "<!doctype html><html><head><meta charset=\"UTF-8\"><title>" + page[1] + "</title><style>body{margin:0;background:#f7f8fa;color:#303133;font:14px Arial,sans-serif;}main{max-width:760px;margin:64px auto;padding:32px;background:#fff;border:1px solid #ebeef5;border-radius:6px;}h1{margin:0 0 16px;font-size:22px;}p{line-height:1.7;color:#606266;}button{margin-top:12px;padding:9px 18px;border:0;border-radius:4px;background:#c0c4cc;color:#fff;}</style></head><body><main data-d5-geo-route=\"" + page[0] + "\"><h1>" + page[1] + "</h1><p>D5_GEO_LOCAL_PAGE</p><p>离线提示：" + page[2] + "</p><button data-d1-action=\"guarded\" disabled>" + page[3] + "</button></main></body></html>";
    }

    private static String localD5WaLocalPage(String url) {
        String path = normalizedUrlPath(url);
        String[] page = null;
        if ("/pc/local/wa/overview".equals(path)) page = new String[] {"overview", "信息总览", "未配置客服账号或会话，本页不读取消息。", "查看客服概览"};
        else if ("/pc/local/wa/account-groups".equals(path)) page = new String[] {"account-groups", "账号分组", "未接入 WhatsApp 账号，本页不创建或调整分组。", "管理账号分组"};
        else if ("/pc/local/wa/account-list".equals(path)) page = new String[] {"account-list", "账号列表", "未登录或绑定 WhatsApp 账号，本页不读取账号。", "查看账号列表"};
        else if ("/pc/local/wa/contact-pool".equals(path)) page = new String[] {"contact-pool", "联系人数据池", "本地未配置联系人数据，本页不导入或读取联系人。", "查看联系人数据池"};
        else if ("/pc/local/wa/fan-broadcast".equals(path)) page = new String[] {"fan-broadcast", "爆粉群发", "未配置接收对象，本页不创建或发送群发任务。", "开始爆粉群发"};
        else if ("/pc/local/wa/group-broadcast".equals(path)) page = new String[] {"group-broadcast", "群聊群发", "未配置群聊或消息内容，本页不创建或发送群发任务。", "开始群聊群发"};
        else if ("/pc/local/wa/customer-service-list".equals(path)) page = new String[] {"customer-service-list", "客服列表", "未接入客服账号，本页不读取会话或消息。", "查看客服列表"};
        if (page == null) return null;
        return "<!doctype html><html><head><meta charset=\"UTF-8\"><title>" + page[1] + "</title><style>body{margin:0;background:#f7f8fa;color:#303133;font:14px Arial,sans-serif;}main{max-width:760px;margin:64px auto;padding:32px;background:#fff;border:1px solid #ebeef5;border-radius:6px;}h1{margin:0 0 16px;font-size:22px;}p{line-height:1.7;color:#606266;}button{margin-top:12px;padding:9px 18px;border:0;border-radius:4px;background:#c0c4cc;color:#fff;}</style></head><body><main data-d5-wa-route=\"" + page[0] + "\"><h1>" + page[1] + "</h1><p>D5_WA_LOCAL_PAGE</p><p>离线提示：" + page[2] + "</p><button data-d1-action=\"guarded\" disabled>" + page[3] + "</button></main></body></html>";
    }

    private static String localC6CommercePage(String url) {
        String path = normalizedUrlPath(normalizeC6CommerceRoute(url));
        if ("/pc/c6/recharge".equals(path)) {
            return "<!doctype html><html><head><meta charset=\"UTF-8\"><title>充值</title>"
                    + "<style>body{margin:0;background:#f7f8fa;color:#303133;font:14px Arial,sans-serif;}"
                    + "main{max-width:760px;margin:64px auto;padding:32px;background:#fff;border:1px solid #ebeef5;}"
                    + "h1{margin:0 0 16px;font-size:22px;}p{line-height:1.7;color:#606266;}"
                    + "button{margin-top:12px;padding:9px 18px;border:0;border-radius:4px;background:#c0c4cc;color:#fff;}"
                    + "</style></head><body><main data-c6-surface=\"recharge\">"
                    + "<h1>充值</h1><p>C6_RECHARGE_UI</p><p>当前离线，支付与订单功能不可用。</p>"
                    + "<button data-c6-action=\"disabled\" disabled>立即充值</button>"
                    + "</main></body></html>";
        }
        if ("/pc/c6/advertising".equals(path)) {
            return "<!doctype html><html><head><meta charset=\"UTF-8\"><title>广告获客</title>"
                    + "<style>body{margin:0;background:#f7f8fa;color:#303133;font:14px Arial,sans-serif;}"
                    + "main{max-width:760px;margin:64px auto;padding:32px;background:#fff;border:1px solid #ebeef5;}"
                    + "h1{margin:0 0 16px;font-size:22px;}p{line-height:1.7;color:#606266;}"
                    + "button{margin-top:12px;padding:9px 18px;border:0;border-radius:4px;background:#c0c4cc;color:#fff;}"
                    + "</style></head><body><main data-c6-surface=\"advertising\">"
                    + "<h1>广告获客</h1><p>C6_ADVERTISING_UI</p><p>当前离线，广告计划、授权与投放功能不可用。</p>"
                    + "<button data-c6-action=\"disabled\" disabled>创建广告计划</button>"
                    + "</main></body></html>";
        }
        return null;
    }

    public static String localC66RechargeDialogHtml() {
        return "<html><body><main data-c6-surface=\"recharge\">"
                + "<h1>充值</h1><p>C6_RECHARGE_UI</p>"
                + "<p>当前离线，支付与订单功能不可用。</p>"
                + "<button data-c6-action=\"disabled\" disabled>立即充值</button>"
                + "</main></body></html>";
    }

    public static void openC66RechargeDialog() {
        Runnable show = () -> {
            final javax.swing.JDialog dialog = new javax.swing.JDialog((java.awt.Frame) null, "充值", false);
            dialog.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
            javax.swing.JPanel panel = new javax.swing.JPanel();
            panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
            panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(24, 28, 24, 28));
            javax.swing.JLabel title = new javax.swing.JLabel("充值");
            title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 20f));
            panel.add(title);
            panel.add(javax.swing.Box.createVerticalStrut(14));
            panel.add(new javax.swing.JLabel("当前离线，余额、支付与订单功能不可用。"));
            panel.add(javax.swing.Box.createVerticalStrut(18));
            javax.swing.JButton recharge = new javax.swing.JButton("立即充值");
            recharge.setEnabled(false);
            recharge.putClientProperty("data-c6-action", "disabled");
            panel.add(recharge);
            dialog.setContentPane(panel);
            dialog.pack();
            dialog.setSize(Math.max(dialog.getWidth(), 430), Math.max(dialog.getHeight(), 190));
            dialog.setLocationByPlatform(true);
            // This ownerless local dialog must remain above the JxBrowser shell it was opened from.
            dialog.setAlwaysOnTop(true);
            dialog.setVisible(true);
            dialog.toFront();
            dialog.requestFocus();
            System.out.println("C66_RECHARGE_DIALOG_VISIBLE route=/pc/c6/recharge");
        };
        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            show.run();
        } else {
            javax.swing.SwingUtilities.invokeLater(show);
        }
    }

    private static String localC62ExternalRequestBlock(String url, boolean includeMirroredHosts) {
        if (url == null) {
            return null;
        }
        try {
            java.net.URI uri = new java.net.URI(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    || host == null
                    || "localhost".equalsIgnoreCase(host)
                    || "127.0.0.1".equals(host)
                    || "::1".equals(host)) {
                return null;
            }
            if (!includeMirroredHosts && isC62MirroredHost(host)) {
                return null;
            }
            String path = normalizedUrlPath(url).toLowerCase();
            if (path.endsWith(".js")) {
                return "/* C62_EXTERNAL_REQUEST_BLOCKED */";
            }
            if (path.endsWith(".css")) {
                return "/* C62_EXTERNAL_REQUEST_BLOCKED */";
            }
            if (path.endsWith(".html") || path.startsWith("/pc/") || path.startsWith("/views/")) {
                return "<!doctype html><html><body>C62_EXTERNAL_REQUEST_BLOCKED</body></html>";
            }
            return "{\"code\":503,\"msg\":\"C62_EXTERNAL_REQUEST_BLOCKED\",\"data\":null,\"rows\":[],\"total\":0}";
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isC62MirroredHost(String host) {
        String lower = host == null ? "" : host.toLowerCase();
        return "app.xdxsoft.com".equals(lower)
                || "xdxsoft.com".equals(lower)
                || "huochai.ai".equals(lower)
                || lower.endsWith(".huochai.ai");
    }

    private static String localWebTerminalJson(String url) {
        String path = normalizedUrlPath(url);
        if ("/prod-api/system/google_sites/lists".equals(path)) {
            return "{\"code\":200,\"msg\":\"success\",\"data\":[],\"rows\":[],\"total\":0}";
        }
        if (path.startsWith("/prod-api/system/pagebanner/getByCodeSoftware")) {
            return "{\"code\":200,\"msg\":\"success\",\"data\":{},\"rows\":[],\"total\":0}";
        }
        if (path.startsWith("/prod-api/es/bigData/code/")) {
            if (path.endsWith("/fb_page_data")) {
                return "{\"code\":200,\"msg\":\"success\",\"data\":{\"allowExport\":false,\"allowImport\":false,\"title\":\"Facebook Page data\",\"subTitle\":\"\",\"searchKeywords\":\"[]\",\"filterContact\":\"[]\",\"fields\":\"{\\\"bodyField\\\":[{\\\"label\\\":\\\"Page\\\",\\\"field\\\":\\\"page_name\\\",\\\"type\\\":\\\"title\\\"},{\\\"label\\\":\\\"Page URL\\\",\\\"field\\\":\\\"page_url\\\",\\\"type\\\":\\\"text\\\"}],\\\"tips\\\":[{\\\"label\\\":\\\"Followers\\\",\\\"field\\\":\\\"followers\\\",\\\"type\\\":\\\"text\\\"}]}\"},\"rows\":[],\"total\":0}";
            }
            if (path.endsWith("/twitter_new_data") || path.endsWith("/big_data_twitter_new")) {
                return "{\"code\":200,\"msg\":\"success\",\"data\":{\"allowExport\":false,\"allowImport\":false,\"title\":\"X precise search data\",\"subTitle\":\"\",\"searchKeywords\":\"[]\",\"filterContact\":\"[]\",\"fields\":\"{\\\"bodyField\\\":[{\\\"label\\\":\\\"Keywords\\\",\\\"field\\\":\\\"keywords\\\",\\\"type\\\":\\\"text\\\"},{\\\"label\\\":\\\"Phone\\\",\\\"field\\\":\\\"phone\\\",\\\"type\\\":\\\"text\\\"},{\\\"label\\\":\\\"X URL\\\",\\\"field\\\":\\\"link\\\",\\\"type\\\":\\\"title\\\"}],\\\"tips\\\":[{\\\"label\\\":\\\"Title\\\",\\\"field\\\":\\\"title\\\",\\\"type\\\":\\\"text\\\"}]}\"},\"rows\":[],\"total\":0}";
            }
            if (path.endsWith("/tiktok_new_data") || path.endsWith("/big_data_tiktok_new")) {
                return "{\"code\":200,\"msg\":\"success\",\"data\":{\"allowExport\":false,\"allowImport\":false,\"title\":\"TikTok data\",\"subTitle\":\"\",\"searchKeywords\":\"[]\",\"filterContact\":\"[]\",\"fields\":\"{\\\"bodyField\\\":[{\\\"label\\\":\\\"Keywords\\\",\\\"field\\\":\\\"keywords\\\",\\\"type\\\":\\\"text\\\"},{\\\"label\\\":\\\"Phone\\\",\\\"field\\\":\\\"phone\\\",\\\"type\\\":\\\"text\\\"},{\\\"label\\\":\\\"TikTok URL\\\",\\\"field\\\":\\\"link\\\",\\\"type\\\":\\\"title\\\"}],\\\"tips\\\":[{\\\"label\\\":\\\"Title\\\",\\\"field\\\":\\\"title\\\",\\\"type\\\":\\\"text\\\"}]}\"},\"rows\":[],\"total\":0}";
            }
            return "{\"code\":200,\"msg\":\"success\",\"data\":{\"allowExport\":false,\"allowImport\":false,\"title\":\"Instagram blogger data\",\"subTitle\":\"\",\"searchKeywords\":\"[]\",\"filterContact\":\"[]\",\"fields\":\"{\\\"bodyField\\\":[{\\\"label\\\":\\\"Instagram\\\",\\\"field\\\":\\\"user_name\\\",\\\"type\\\":\\\"title\\\"},{\\\"label\\\":\\\"Nick\\\",\\\"field\\\":\\\"nick_name\\\",\\\"type\\\":\\\"text\\\"},{\\\"label\\\":\\\"Homepage\\\",\\\"field\\\":\\\"homepage_url\\\",\\\"type\\\":\\\"text\\\"}],\\\"tips\\\":[{\\\"label\\\":\\\"Fans\\\",\\\"field\\\":\\\"fans_count\\\",\\\"type\\\":\\\"text\\\"}]}\"},\"rows\":[],\"total\":0}";
        }
        if ("/prod-api/es/bigDataConfig/userConfig".equals(path)) {
            return "{\"code\":200,\"msg\":\"success\",\"data\":{\"xdxChineseSwitch\":1,\"xdxEnSwitch\":1},\"rows\":[],\"total\":0}";
        }
        if (path.startsWith("/prod-api/es/collectTask/my")
                || path.startsWith("/prod-api/es/collectTask/list")
                || path.startsWith("/prod-api/es/bigData/list")
                || path.startsWith("/prod-api/es/bigData/data/")) {
            return "{\"code\":200,\"msg\":\"success\",\"data\":[],\"rows\":[],\"total\":0}";
        }
        if (path.startsWith("/prod-api/tg/groupTask/list")
                || path.startsWith("/prod-api/accessFlow/order/list")
                || path.startsWith("/prod-api/kefu/conversation/list")) {
            return "{\"code\":200,\"msg\":\"success\",\"data\":[],\"rows\":[],\"total\":0}";
        }
        return null;
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
        if ("chunk-2d0bd27f.b96f2a2b.js".equals(filename)) {
            String patched =
                    body.replaceAll(
                            "\"img\":\"https://aisrc\\.oss-cn-hangzhou\\.aliyuncs\\.com/hd/[^\"]+\"",
                            "\"img\":\"data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==\"");
            System.out.println(
                    "C3A3_TWITTER_AVATAR_LOCALIZED changed=" + String.valueOf(!patched.equals(body)));
            return patched;
        }
        if ("chunk-46942aaa.78ddab17.js".equals(filename)) {
            String patched =
                    body.replace(
                            "\"big_data_twitter_new\"==this.funcModuleCode?this.tableData=this.twitter_new_data",
                            "\"big_data_twitter_new\"==this.funcModuleCode?this.tableData=[]");
            patched =
                    patched.replace(
                            "\"big_data_tiktok_new\"==this.funcModuleCode?this.tableData=this.tiktok_new_data",
                            "\"big_data_tiktok_new\"==this.funcModuleCode?this.tableData=[]");
            System.out.println(
                    "C3A3_TWITTER_DEFAULT_EMPTY changed=" + String.valueOf(!patched.equals(body)));
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
        if (path.endsWith(".woff")) {
            return "font/woff";
        }
        if (path.endsWith(".png")) {
            return "image/png";
        }
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (path.endsWith(".html")
                || path.equals("/")
                || path.startsWith("/pc/")
                || path.startsWith("/views/overseasAds/")
                || path.startsWith("/aiAgent/")
                || path.startsWith("/es/")
                || path.startsWith("/wsClaw/")) {
            return "text/html;charset=UTF-8";
        }
        return "application/json;charset=UTF-8";
    }

    private static Path localWebAssetPath(String url) {
        String path = normalizedUrlPath(url);
        if (path.length() == 0
                || "/".equals(path)
                || path.startsWith("/pc/")
                || path.startsWith("/aiAgent/")
                || path.startsWith("/es/")
                || path.startsWith("/wsClaw/")
                || path.endsWith("/aicloud.html")) {
            return localWebMirrorDir().resolve("aicloud.html");
        }
        if ("/static/js/app.09d7ef80.js".equals(path)) {
            path = "/static/js/app.988d65c1.js";
        } else if ("/static/css/app.0299bcba.css".equals(path)) {
            path = "/static/css/app.99741a48.css";
        }
        String filename = path.substring(path.lastIndexOf('/') + 1);
        if (filename.indexOf("..") >= 0 || filename.length() == 0) {
            return null;
        }
        if (path.startsWith("/static/")) {
            Path fullMirrorAsset = localWebFullMirrorDir().resolve(path.substring(1));
            if (Files.exists(fullMirrorAsset)) {
                return fullMirrorAsset;
            }
            return localWebMirrorDir().resolve(filename);
        }
        return null;
    }

    private static Path localWebFullMirrorDir() {
        String baseDir = resolveAppBaseDir();
        Path[] candidates = {
            Paths.get(".").toAbsolutePath().normalize().resolve(".artifacts").resolve("working").resolve("m5-online-full"),
            Paths.get(".").toAbsolutePath().normalize().resolve("..").resolve("..").resolve(".artifacts").resolve("working").resolve("m5-online-full").normalize(),
            Paths.get(baseDir).toAbsolutePath().normalize().resolve("..").resolve("..").resolve(".artifacts").resolve("working").resolve("m5-online-full").normalize(),
            Paths.get(baseDir).toAbsolutePath().normalize().resolve("..").resolve(".artifacts").resolve("working").resolve("m5-online-full").normalize()
        };
        for (Path candidate : candidates) {
            if (Files.exists(candidate.resolve("static").resolve("js").resolve("chunk-49bd57a4.df38da93.js"))) {
                return candidate;
            }
        }
        return candidates[0];
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

    private static String activeWhatsAppProfileId(String baseDir) throws Exception {
        Path dbPath = whatsappAccountDbPath(baseDir);
        if (!Files.exists(dbPath)) {
            return "";
        }
        try (Connection conn = openSqlite(dbPath)) {
            ensureWhatsAppStateTable(conn);
            try (PreparedStatement query =
                    conn.prepareStatement(
                            "select state_value from b_whatsapp_state where state_key=?")) {
                query.setString(1, "active_profile_id");
                try (ResultSet rs = query.executeQuery()) {
                    if (rs.next()) {
                        return normalizeProfileId(rs.getString(1));
                    }
                }
            }
        }
        return "";
    }

    private static String firstWhatsAppProfileId(String baseDir) throws Exception {
        Path dbPath = whatsappAccountDbPath(baseDir);
        if (!Files.exists(dbPath)) {
            return "";
        }
        try (Connection conn = openSqlite(dbPath)) {
            ensureWhatsAppAccountTable(conn);
            try (Statement stmt = conn.createStatement();
                    ResultSet rs =
                            stmt.executeQuery(
                                    "select profile_id from b_whatsapp_accounts "
                                            + "order by updated_at desc,profile_id asc limit 1")) {
                return rs.next() ? normalizeProfileId(rs.getString(1)) : "";
            }
        }
    }

    private static void ensureWhatsAppAccountRow(
            Connection conn, String profileId, Path profilePath, long now) throws Exception {
        try (PreparedStatement query =
                conn.prepareStatement("select profile_id from b_whatsapp_accounts where profile_id=?")) {
            query.setString(1, profileId);
            try (ResultSet rs = query.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        try (PreparedStatement insert =
                conn.prepareStatement(
                        "insert into b_whatsapp_accounts "
                                + "(profile_id,phone,status,last_status_json,profile_path,created_at,updated_at) "
                                + "values (?,?,?,?,?,?,?)")) {
            insert.setString(1, profileId);
            insert.setString(2, "");
            insert.setString(3, "profile_ready");
            insert.setString(4, "{\"source\":\"m8b1c-profile-switch\"}");
            insert.setString(5, profilePath.toAbsolutePath().toString());
            insert.setLong(6, now);
            insert.setLong(7, now);
            insert.executeUpdate();
        }
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

    private static Path whatsappAccountDbPath(String baseDir) {
        return Paths.get(baseDir).resolve("data").resolve("db_b_whatsapp_accounts.data");
    }

    private static Path whatsappMessageDbPath(String baseDir) {
        return Paths.get(baseDir).resolve("data").resolve("db_b_whatsapp_messages.data");
    }

    private static Path whatsappProfilePath(String baseDir, String profileId) {
        return Paths.get(baseDir).resolve("bscache").resolve("wa-profiles").resolve(profileId);
    }

    private static String normalizeProfileId(String profileId) {
        String value = isBlank(profileId) ? "wa-default" : profileId.trim();
        StringBuilder safe = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if ((ch >= 'a' && ch <= 'z')
                    || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9')
                    || ch == '-'
                    || ch == '_') {
                safe.append(ch);
            } else {
                safe.append('-');
            }
        }
        return safe.length() == 0 ? "wa-default" : safe.toString();
    }

    private static String normalizeMessageKey(String value, String prefix) {
        String text = isBlank(value) ? "" : value.trim();
        StringBuilder safe = new StringBuilder(text.length() + 8);
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if ((ch >= 'a' && ch <= 'z')
                    || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9')
                    || ch == '-'
                    || ch == '_'
                    || ch == '+'
                    || ch == '@'
                    || ch == '.') {
                safe.append(ch);
            } else {
                safe.append('-');
            }
        }
        String normalized = safe.toString();
        if (isBlank(normalized)) {
            normalized = prefix + "-" + System.currentTimeMillis();
        }
        if (normalized.length() > 180) {
            normalized = normalized.substring(0, 140) + "-" + Integer.toHexString(text.hashCode());
        }
        return normalized;
    }

    private static void upsertWhatsAppContact(
            Connection conn,
            String profileId,
            String contactKey,
            String phone,
            String displayName,
            String rawJson,
            long now)
            throws Exception {
        try (PreparedStatement update =
                conn.prepareStatement(
                        "update b_whatsapp_contacts set phone=?,display_name=?,raw_json=?,updated_at=? "
                                + "where profile_id=? and contact_key=?")) {
            update.setString(1, phone);
            update.setString(2, displayName);
            update.setString(3, rawJson);
            update.setLong(4, now);
            update.setString(5, profileId);
            update.setString(6, contactKey);
            if (update.executeUpdate() > 0) {
                return;
            }
        }
        try (PreparedStatement insert =
                conn.prepareStatement(
                        "insert into b_whatsapp_contacts "
                                + "(profile_id,contact_key,phone,display_name,raw_json,created_at,updated_at) "
                                + "values (?,?,?,?,?,?,?)")) {
            insert.setString(1, profileId);
            insert.setString(2, contactKey);
            insert.setString(3, phone);
            insert.setString(4, displayName);
            insert.setString(5, rawJson);
            insert.setLong(6, now);
            insert.setLong(7, now);
            insert.executeUpdate();
        }
    }

    private static void upsertWhatsAppConversation(
            Connection conn,
            String profileId,
            String conversationKey,
            String contactKey,
            String title,
            String lastMessageText,
            long lastMessageTime,
            String rawJson,
            long now)
            throws Exception {
        try (PreparedStatement update =
                conn.prepareStatement(
                        "update b_whatsapp_conversations set contact_key=?,title=?,last_message_text=?,"
                                + "last_message_time=?,raw_json=?,updated_at=? "
                                + "where profile_id=? and conversation_key=?")) {
            update.setString(1, contactKey);
            update.setString(2, title);
            update.setString(3, lastMessageText);
            update.setLong(4, lastMessageTime);
            update.setString(5, rawJson);
            update.setLong(6, now);
            update.setString(7, profileId);
            update.setString(8, conversationKey);
            if (update.executeUpdate() > 0) {
                return;
            }
        }
        try (PreparedStatement insert =
                conn.prepareStatement(
                        "insert into b_whatsapp_conversations "
                                + "(profile_id,conversation_key,contact_key,title,last_message_text,"
                                + "last_message_time,unread_count,raw_json,created_at,updated_at) "
                                + "values (?,?,?,?,?,?,?,?,?,?)")) {
            insert.setString(1, profileId);
            insert.setString(2, conversationKey);
            insert.setString(3, contactKey);
            insert.setString(4, title);
            insert.setString(5, lastMessageText);
            insert.setLong(6, lastMessageTime);
            insert.setLong(7, 0L);
            insert.setString(8, rawJson);
            insert.setLong(9, now);
            insert.setLong(10, now);
            insert.executeUpdate();
        }
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

    private static void ensureWhatsAppAccountTable(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "create table if not exists b_whatsapp_accounts ("
                            + "profile_id varchar primary key,"
                            + "phone varchar,"
                            + "status varchar,"
                            + "last_status_json varchar,"
                            + "profile_path varchar,"
                            + "created_at bigint,"
                            + "updated_at bigint)");
        }
    }

    private static void ensureWhatsAppStateTable(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "create table if not exists b_whatsapp_state ("
                            + "state_key varchar primary key,"
                            + "state_value varchar,"
                            + "updated_at bigint)");
        }
    }

    private static void ensureWhatsAppMessageTables(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "create table if not exists b_whatsapp_contacts ("
                            + "profile_id varchar,"
                            + "contact_key varchar,"
                            + "phone varchar,"
                            + "display_name varchar,"
                            + "raw_json varchar,"
                            + "created_at bigint,"
                            + "updated_at bigint,"
                            + "primary key(profile_id,contact_key))");
            stmt.executeUpdate(
                    "create table if not exists b_whatsapp_conversations ("
                            + "profile_id varchar,"
                            + "conversation_key varchar,"
                            + "contact_key varchar,"
                            + "title varchar,"
                            + "last_message_text varchar,"
                            + "last_message_time bigint,"
                            + "unread_count bigint,"
                            + "raw_json varchar,"
                            + "created_at bigint,"
                            + "updated_at bigint,"
                            + "primary key(profile_id,conversation_key))");
            stmt.executeUpdate(
                    "create table if not exists b_whatsapp_messages ("
                            + "profile_id varchar,"
                            + "conversation_key varchar,"
                            + "message_id varchar,"
                            + "direction varchar,"
                            + "sender varchar,"
                            + "message_text varchar,"
                            + "message_time bigint,"
                            + "raw_json varchar,"
                            + "created_at bigint,"
                            + "primary key(profile_id,conversation_key,message_id))");
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
