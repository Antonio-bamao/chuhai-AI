package com.sbf.main.jxbrowser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

public final class M8WhatsAppDripCampaigns {
    private static final Pattern PHONE_CANDIDATE =
            Pattern.compile("(?:\\+|00)?\\d[\\d\\s().-]{6,24}\\d");

    private M8WhatsAppDripCampaigns() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 5 || !"dry-run".equals(args[1])) {
            System.out.println(
                    "usage: M8WhatsAppDripCampaigns <baseDir> dry-run <sourceDb> <profilesJsonOrCsv> <message> [optionsJson]");
            return;
        }
        String options = args.length >= 6 ? args[5] : "{}";
        System.out.println(dryRun(args[0], args[2], args[3], args[4], options));
    }

    public static String dryRun(
            String baseDir,
            String sourceDbPath,
            String profilesJsonOrCsv,
            String messageText,
            String optionsJson)
            throws Exception {
        Class.forName("org.sqlite.JDBC");
        JSONObject options = parseObject(optionsJson);
        Path dripDb = dripDbPath(baseDir);
        Files.createDirectories(dripDb.getParent());
        String campaignId = "drip-" + System.currentTimeMillis();
        JSONArray profiles = parseProfiles(baseDir, profilesJsonOrCsv);
        if (profiles.length() == 0) {
            return new JSONObject()
                    .put("code", 409)
                    .put("dryRun", true)
                    .put("msg", "no_logged_in_profiles")
                    .toString();
        }

        JSONObject importSummary;
        JSONObject runSummary;
        try (Connection conn = openSqlite(dripDb)) {
            ensureTables(conn);
            createCampaign(conn, campaignId, messageText, sourceDbPath);
            importSummary = importRecipients(conn, campaignId, sourceDbPath);
            assignQueues(conn, campaignId, profiles);
            runSummary = runDryQueue(conn, baseDir, campaignId, options);
            updateCampaignSummary(conn, campaignId, importSummary, runSummary);
        }
        JSONObject result =
                new JSONObject()
                        .put("code", 200)
                        .put("dryRun", true)
                        .put("campaignId", campaignId)
                        .put("profiles", profiles)
                        .put("importSummary", importSummary)
                        .put("runSummary", runSummary)
                        .put("dbPath", dripDb.toAbsolutePath().toString());
        System.out.println(
                "M8B2A_DRIP_DRY_RUN_DONE campaignId="
                        + campaignId
                        + " rawRows="
                        + importSummary.optInt("rawRows")
                        + " assigned="
                        + importSummary.optInt("assignedRecipients")
                        + " wouldSend="
                        + runSummary.optInt("wouldSend")
                        + " trueSendAttempts="
                        + runSummary.optInt("trueSendAttempts"));
        return result.toString();
    }

    private static JSONObject importRecipients(Connection conn, String campaignId, String sourceDbPath)
            throws Exception {
        Path source = Paths.get(sourceDbPath);
        if (!Files.isRegularFile(source)) {
            throw new IllegalArgumentException("source db not found: " + sourceDbPath);
        }
        int rawRows = 0;
        int extracted = 0;
        int valid = 0;
        int duplicates = 0;
        int invalid = 0;
        Set<String> seen = new HashSet<String>();
        try (Connection sourceConn = openSqlite(source)) {
            try (Statement stmt = sourceConn.createStatement();
                    ResultSet rs =
                            stmt.executeQuery(
                                    "select id,json_data,time from spider_data order by id asc")) {
                while (rs.next()) {
                    rawRows++;
                    long sourceId = rs.getLong(1);
                    JSONObject row = parseObject(rs.getString(2));
                    List<String> candidates =
                            extractCandidates(firstNonBlank(row.optString("phone"), row.optString("phoneRaw")));
                    if (candidates.isEmpty()) {
                        candidates.add(firstNonBlank(row.optString("phone"), row.optString("phoneRaw")));
                    }
                    for (String candidate : candidates) {
                        extracted++;
                        String normalized = normalizePhone(candidate);
                        JSONObject raw =
                                new JSONObject()
                                        .put("sourceRowId", sourceId)
                                        .put("rawPhone", candidate)
                                        .put("source", row);
                        if (isBlank(normalized)) {
                            invalid++;
                            insertRecipient(
                                    conn, campaignId, sourceId, candidate, "", "invalid", raw.toString());
                            continue;
                        }
                        if (seen.contains(normalized)) {
                            duplicates++;
                            insertRecipient(
                                    conn,
                                    campaignId,
                                    sourceId,
                                    candidate,
                                    normalized,
                                    "duplicate",
                                    raw.toString());
                            continue;
                        }
                        seen.add(normalized);
                        valid++;
                        insertRecipient(
                                conn, campaignId, sourceId, candidate, normalized, "valid", raw.toString());
                    }
                }
            }
        }
        JSONObject summary =
                new JSONObject()
                        .put("rawRows", rawRows)
                        .put("extractedCandidates", extracted)
                        .put("validRecipients", valid)
                        .put("duplicateRecipients", duplicates)
                        .put("invalidRecipients", invalid)
                        .put("assignedRecipients", valid);
        event(conn, campaignId, "", "", "IMPORT_SUMMARY", "import and clean complete", summary);
        System.out.println(
                "M8B2A_DRIP_IMPORT_SUMMARY rawRows="
                        + rawRows
                        + " extractedCandidates="
                        + extracted
                        + " valid="
                        + valid
                        + " duplicate="
                        + duplicates
                        + " invalid="
                        + invalid
                        + " assigned="
                        + valid);
        return summary;
    }

    private static void assignQueues(Connection conn, String campaignId, JSONArray profiles)
            throws Exception {
        List<String> profileIds = new ArrayList<String>();
        for (int i = 0; i < profiles.length(); i++) {
            String profile = profiles.optString(i, "").trim();
            if (!isBlank(profile)) {
                profileIds.add(profile);
                upsertAccountLimit(conn, profile);
            }
        }
        if (profileIds.isEmpty()) {
            throw new IllegalArgumentException("empty profile list");
        }
        int sequence = 0;
        try (PreparedStatement query =
                        conn.prepareStatement(
                                "select normalized_phone,raw_json from b_drip_recipients "
                                        + "where campaign_id=? and status='valid' order by recipient_id asc");
                PreparedStatement insert =
                        conn.prepareStatement(
                                "insert or replace into b_drip_queue_items "
                                        + "(campaign_id,profile_id,normalized_phone,sequence_no,status,dry_run,raw_json,created_at,updated_at) "
                                        + "values(?,?,?,?,?,?,?,?,?)")) {
            query.setString(1, campaignId);
            try (ResultSet rs = query.executeQuery()) {
                while (rs.next()) {
                    String profile = profileIds.get(sequence % profileIds.size());
                    long now = System.currentTimeMillis();
                    insert.setString(1, campaignId);
                    insert.setString(2, profile);
                    insert.setString(3, rs.getString(1));
                    insert.setInt(4, sequence + 1);
                    insert.setString(5, "queued");
                    insert.setInt(6, 1);
                    insert.setString(7, rs.getString(2));
                    insert.setLong(8, now);
                    insert.setLong(9, now);
                    insert.executeUpdate();
                    sequence++;
                }
            }
        }
        try (PreparedStatement stmt =
                conn.prepareStatement(
                        "insert or replace into b_drip_queues "
                                + "(campaign_id,profile_id,status,total_items,created_at,updated_at) "
                                + "select campaign_id,profile_id,'queued',count(*),?,? "
                                + "from b_drip_queue_items where campaign_id=? group by campaign_id,profile_id")) {
            long now = System.currentTimeMillis();
            stmt.setLong(1, now);
            stmt.setLong(2, now);
            stmt.setString(3, campaignId);
            stmt.executeUpdate();
        }
        event(
                conn,
                campaignId,
                "",
                "",
                "QUEUE_ASSIGNED",
                "assigned valid recipients to per-profile queues",
                new JSONObject().put("profiles", profiles).put("assignedRecipients", sequence));
        System.out.println("M8B2A_DRIP_QUEUE_ASSIGNED campaignId=" + campaignId + " total=" + sequence);
    }

    private static JSONObject runDryQueue(
            Connection conn, String baseDir, String campaignId, JSONObject options) throws Exception {
        int sleepMin = Math.max(0, options.optInt("sleepMinMs", 60000));
        int sleepMax = Math.max(sleepMin, options.optInt("sleepMaxMs", 180000));
        int dailyLimit = Math.max(1, options.optInt("dailyLimit", 50));
        int hourlyLimit = Math.max(1, options.optInt("hourlyLimit", 10));
        boolean simulateRisk = options.optBoolean("simulateRiskSamples", false);
        Random random =
                new Random(
                        Long.getLong(
                                "m8.drip.random.seed",
                                Long.valueOf(System.currentTimeMillis()).longValue()));
        int checked = 0;
        int wouldSend = 0;
        int riskSkipped = 0;
        int precheckSkipped = 0;
        int limitSkipped = 0;
        int statusWritten = 0;
        int trueSendAttempts = 0;
        try (PreparedStatement query =
                conn.prepareStatement(
                        "select item_id,profile_id,normalized_phone,sequence_no from b_drip_queue_items "
                                + "where campaign_id=? order by sequence_no asc")) {
            query.setString(1, campaignId);
            try (ResultSet rs = query.executeQuery()) {
                while (rs.next()) {
                    long itemId = rs.getLong(1);
                    String profileId = rs.getString(2);
                    String phone = rs.getString(3);
                    checked++;
                    JSONObject precheck = precheck(baseDir, profileId);
                    event(conn, campaignId, profileId, phone, "PRECHECK", "login/page/risk precheck", precheck);
                    if (!precheck.optBoolean("ok")) {
                        precheckSkipped++;
                        statusWritten++;
                        updateItem(conn, itemId, "skipped_precheck", precheck);
                        continue;
                    }
                    JSONObject limit = limitCheck(conn, profileId, dailyLimit, hourlyLimit);
                    event(conn, campaignId, profileId, phone, "LIMIT_CHECK", "dry-run limit check", limit);
                    if (!limit.optBoolean("ok")) {
                        limitSkipped++;
                        statusWritten++;
                        updateItem(conn, itemId, "skipped_limit", limit);
                        continue;
                    }
                    JSONObject risk = riskCheck(phone, simulateRisk ? checked : 0);
                    event(conn, campaignId, profileId, phone, "RISK_CHECK", "dry-run number risk check", risk);
                    if (!risk.optBoolean("ok")) {
                        riskSkipped++;
                        statusWritten++;
                        updateItem(conn, itemId, "skipped_risk", risk);
                        event(conn, campaignId, profileId, phone, "RISK_SKIP", risk.optString("reason"), risk);
                        continue;
                    }
                    int sleepMs = sleepMin + (sleepMax == sleepMin ? 0 : random.nextInt(sleepMax - sleepMin + 1));
                    JSONObject would =
                            new JSONObject()
                                    .put("dryRun", true)
                                    .put("profileId", profileId)
                                    .put("destPhone", phone)
                                    .put("sleepMs", sleepMs)
                                    .put("singleAccountConcurrency", 1)
                                    .put("trueSend", false);
                    event(conn, campaignId, profileId, phone, "WOULD_SEND", "dry-run would send only", would);
                    updateItem(conn, itemId, "dry_run_would_send", would);
                    wouldSend++;
                    statusWritten++;
                    if (sleepMs > 0) {
                        Thread.sleep(Math.min(sleepMs, 25));
                    }
                    event(conn, campaignId, profileId, phone, "SLEEP", "dry-run random sleep applied", would);
                }
            }
        }
        JSONObject summary =
                new JSONObject()
                        .put("checked", checked)
                        .put("wouldSend", wouldSend)
                        .put("riskSkipped", riskSkipped)
                        .put("precheckSkipped", precheckSkipped)
                        .put("limitSkipped", limitSkipped)
                        .put("statusWritten", statusWritten)
                        .put("trueSendAttempts", trueSendAttempts);
        event(conn, campaignId, "", "", "RUN_SUMMARY", "dry-run queue complete", summary);
        System.out.println("M8B2A_DRIP_RUN_SUMMARY " + summary.toString());
        return summary;
    }

    private static JSONObject precheck(String baseDir, String profileId) throws Exception {
        Path accountsDb = Paths.get(baseDir).resolve("data").resolve("db_b_whatsapp_accounts.data");
        JSONObject result = new JSONObject().put("profileId", profileId);
        if (!Files.isRegularFile(accountsDb)) {
            return result.put("ok", false).put("reason", "account_db_missing");
        }
        try (Connection conn = openSqlite(accountsDb);
                PreparedStatement stmt =
                        conn.prepareStatement(
                                "select status,last_status_json from b_whatsapp_accounts where profile_id=?")) {
            stmt.setString(1, profileId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return result.put("ok", false).put("reason", "account_missing");
                }
                String status = rs.getString(1);
                String statusJson = rs.getString(2);
                boolean loggedIn = "logged_in".equals(status);
                boolean pageReady =
                        statusJson != null
                                && statusJson.contains("web.whatsapp.com")
                                && statusJson.contains("logged_in");
                return result.put("ok", loggedIn && pageReady)
                        .put("loginStatus", status)
                        .put("pageReady", pageReady)
                        .put("reason", loggedIn && pageReady ? "ok" : "not_ready");
            }
        }
    }

    private static JSONObject limitCheck(
            Connection conn, String profileId, int dailyLimit, int hourlyLimit) throws Exception {
        upsertAccountLimit(conn, profileId);
        return new JSONObject()
                .put("ok", true)
                .put("profileId", profileId)
                .put("dailyLimit", dailyLimit)
                .put("hourlyLimit", hourlyLimit)
                .put("warmupStage", "dry-run")
                .put("healthScore", 100)
                .put("singleAccountConcurrency", 1)
                .put("consecutiveFailureFuse", 3);
    }

    private static JSONObject riskCheck(String phone, int simulatedIndex) {
        if (simulatedIndex == 1) {
            return new JSONObject().put("ok", false).put("reason", "blocked").put("destPhone", phone);
        }
        if (simulatedIndex == 2) {
            return new JSONObject()
                    .put("ok", false)
                    .put("reason", "possible_migration")
                    .put("destPhone", phone);
        }
        return new JSONObject().put("ok", true).put("reason", "ok").put("destPhone", phone);
    }

    private static void createCampaign(
            Connection conn, String campaignId, String messageText, String sourceDbPath) throws Exception {
        try (PreparedStatement stmt =
                conn.prepareStatement(
                        "insert into b_drip_campaigns "
                                + "(campaign_id,name,message_text,dry_run,status,source_db,created_at,updated_at) "
                                + "values(?,?,?,?,?,?,?,?)")) {
            long now = System.currentTimeMillis();
            stmt.setString(1, campaignId);
            stmt.setString(2, "B-2-A dry-run");
            stmt.setString(3, messageText == null ? "" : messageText);
            stmt.setInt(4, 1);
            stmt.setString(5, "created");
            stmt.setString(6, sourceDbPath);
            stmt.setLong(7, now);
            stmt.setLong(8, now);
            stmt.executeUpdate();
        }
        event(conn, campaignId, "", "", "TASK_CREATED", "dry-run campaign created", new JSONObject());
    }

    private static void updateCampaignSummary(
            Connection conn, String campaignId, JSONObject importSummary, JSONObject runSummary) throws Exception {
        try (PreparedStatement stmt =
                conn.prepareStatement(
                        "update b_drip_campaigns set status=?,raw_rows=?,extracted_candidates=?,"
                                + "valid_recipients=?,duplicate_recipients=?,invalid_recipients=?,"
                                + "assigned_recipients=?,would_send=?,risk_skipped=?,true_send_attempts=?,updated_at=? "
                                + "where campaign_id=?")) {
            stmt.setString(1, "dry_run_done");
            stmt.setInt(2, importSummary.optInt("rawRows"));
            stmt.setInt(3, importSummary.optInt("extractedCandidates"));
            stmt.setInt(4, importSummary.optInt("validRecipients"));
            stmt.setInt(5, importSummary.optInt("duplicateRecipients"));
            stmt.setInt(6, importSummary.optInt("invalidRecipients"));
            stmt.setInt(7, importSummary.optInt("assignedRecipients"));
            stmt.setInt(8, runSummary.optInt("wouldSend"));
            stmt.setInt(9, runSummary.optInt("riskSkipped"));
            stmt.setInt(10, runSummary.optInt("trueSendAttempts"));
            stmt.setLong(11, System.currentTimeMillis());
            stmt.setString(12, campaignId);
            stmt.executeUpdate();
        }
    }

    private static void insertRecipient(
            Connection conn,
            String campaignId,
            long sourceRowId,
            String rawPhone,
            String normalizedPhone,
            String status,
            String rawJson)
            throws Exception {
        try (PreparedStatement stmt =
                conn.prepareStatement(
                        "insert into b_drip_recipients "
                                + "(campaign_id,source_row_id,raw_phone,normalized_phone,status,raw_json,created_at) "
                                + "values(?,?,?,?,?,?,?)")) {
            stmt.setString(1, campaignId);
            stmt.setLong(2, sourceRowId);
            stmt.setString(3, rawPhone == null ? "" : rawPhone);
            stmt.setString(4, normalizedPhone == null ? "" : normalizedPhone);
            stmt.setString(5, status);
            stmt.setString(6, rawJson);
            stmt.setLong(7, System.currentTimeMillis());
            stmt.executeUpdate();
        }
    }

    private static void updateItem(Connection conn, long itemId, String status, JSONObject data)
            throws Exception {
        try (PreparedStatement stmt =
                conn.prepareStatement(
                        "update b_drip_queue_items set status=?,precheck_json=?,updated_at=? where item_id=?")) {
            stmt.setString(1, status);
            stmt.setString(2, data.toString());
            stmt.setLong(3, System.currentTimeMillis());
            stmt.setLong(4, itemId);
            stmt.executeUpdate();
        }
    }

    private static void event(
            Connection conn,
            String campaignId,
            String profileId,
            String phone,
            String type,
            String message,
            JSONObject raw)
            throws Exception {
        try (PreparedStatement stmt =
                conn.prepareStatement(
                        "insert into b_drip_events "
                                + "(campaign_id,profile_id,normalized_phone,event_type,message,raw_json,created_at) "
                                + "values(?,?,?,?,?,?,?)")) {
            stmt.setString(1, campaignId);
            stmt.setString(2, profileId == null ? "" : profileId);
            stmt.setString(3, phone == null ? "" : phone);
            stmt.setString(4, type);
            stmt.setString(5, message);
            stmt.setString(6, raw == null ? "{}" : raw.toString());
            stmt.setLong(7, System.currentTimeMillis());
            stmt.executeUpdate();
        }
        System.out.println(
                "M8B2A_DRIP_EVENT type="
                        + type
                        + " profileId="
                        + (profileId == null ? "" : profileId)
                        + " phone="
                        + (phone == null ? "" : phone)
                        + " msg="
                        + message);
    }

    private static JSONArray parseProfiles(String baseDir, String profilesJsonOrCsv) throws Exception {
        JSONArray result = new JSONArray();
        String value = profilesJsonOrCsv == null ? "" : profilesJsonOrCsv.trim();
        if (value.startsWith("[")) {
            JSONArray array = new JSONArray(value);
            for (int i = 0; i < array.length(); i++) {
                String profile = array.optString(i, "").trim();
                if (!isBlank(profile)) {
                    result.put(profile);
                }
            }
            return result;
        }
        if (!isBlank(value)) {
            String[] parts = value.split(",");
            for (int i = 0; i < parts.length; i++) {
                String profile = parts[i].trim();
                if (!isBlank(profile)) {
                    result.put(profile);
                }
            }
            return result;
        }
        Path accountsDb = Paths.get(baseDir).resolve("data").resolve("db_b_whatsapp_accounts.data");
        if (!Files.isRegularFile(accountsDb)) {
            return result;
        }
        try (Connection conn = openSqlite(accountsDb);
                Statement stmt = conn.createStatement();
                ResultSet rs =
                        stmt.executeQuery(
                                "select profile_id from b_whatsapp_accounts where status='logged_in' order by profile_id")) {
            while (rs.next()) {
                result.put(rs.getString(1));
            }
        }
        return result;
    }

    private static List<String> extractCandidates(String raw) {
        List<String> out = new ArrayList<String>();
        if (raw == null) {
            return out;
        }
        Matcher matcher = PHONE_CANDIDATE.matcher(raw);
        while (matcher.find()) {
            out.add(matcher.group());
        }
        return out;
    }

    private static String normalizePhone(String raw) {
        if (isBlank(raw)) {
            return "";
        }
        String text = raw.trim();
        boolean plus = text.startsWith("+");
        boolean zeroZero = text.startsWith("00");
        String digits = text.replaceAll("\\D", "");
        if (zeroZero && digits.length() > 2) {
            digits = digits.substring(2);
            plus = true;
        }
        if (!plus) {
            return "";
        }
        if (digits.length() < 8 || digits.length() > 15 || allSameDigit(digits)) {
            return "";
        }
        return "+" + digits;
    }

    private static boolean allSameDigit(String digits) {
        if (digits.length() == 0) {
            return true;
        }
        char first = digits.charAt(0);
        for (int i = 1; i < digits.length(); i++) {
            if (digits.charAt(i) != first) {
                return false;
            }
        }
        return true;
    }

    private static void upsertAccountLimit(Connection conn, String profileId) throws Exception {
        try (PreparedStatement stmt =
                conn.prepareStatement(
                        "insert or ignore into b_drip_account_limits "
                                + "(profile_id,daily_limit,hourly_limit,warmup_stage,health_score,"
                                + "consecutive_failures,frozen_reason,updated_at) values(?,?,?,?,?,?,?,?)")) {
            stmt.setString(1, profileId);
            stmt.setInt(2, 50);
            stmt.setInt(3, 10);
            stmt.setString(4, "dry-run");
            stmt.setInt(5, 100);
            stmt.setInt(6, 0);
            stmt.setString(7, "");
            stmt.setLong(8, System.currentTimeMillis());
            stmt.executeUpdate();
        }
    }

    private static void ensureTables(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "create table if not exists b_drip_campaigns ("
                            + "campaign_id varchar primary key,name varchar,message_text varchar,dry_run bigint,"
                            + "status varchar,source_db varchar,raw_rows bigint,extracted_candidates bigint,"
                            + "valid_recipients bigint,duplicate_recipients bigint,invalid_recipients bigint,"
                            + "assigned_recipients bigint,would_send bigint,risk_skipped bigint,"
                            + "true_send_attempts bigint,created_at bigint,updated_at bigint)");
            stmt.executeUpdate(
                    "create table if not exists b_drip_recipients ("
                            + "recipient_id integer primary key autoincrement,campaign_id varchar,"
                            + "source_row_id bigint,raw_phone varchar,normalized_phone varchar,"
                            + "status varchar,raw_json varchar,created_at bigint)");
            stmt.executeUpdate(
                    "create table if not exists b_drip_queues ("
                            + "campaign_id varchar,profile_id varchar,status varchar,total_items bigint,"
                            + "created_at bigint,updated_at bigint,primary key(campaign_id,profile_id))");
            stmt.executeUpdate(
                    "create table if not exists b_drip_queue_items ("
                            + "item_id integer primary key autoincrement,campaign_id varchar,profile_id varchar,"
                            + "normalized_phone varchar,sequence_no bigint,status varchar,dry_run bigint,"
                            + "precheck_json varchar,raw_json varchar,created_at bigint,updated_at bigint,"
                            + "unique(campaign_id,normalized_phone))");
            stmt.executeUpdate(
                    "create table if not exists b_drip_events ("
                            + "event_id integer primary key autoincrement,campaign_id varchar,profile_id varchar,"
                            + "normalized_phone varchar,event_type varchar,message varchar,raw_json varchar,"
                            + "created_at bigint)");
            stmt.executeUpdate(
                    "create table if not exists b_drip_account_limits ("
                            + "profile_id varchar primary key,daily_limit bigint,hourly_limit bigint,"
                            + "warmup_stage varchar,health_score bigint,consecutive_failures bigint,"
                            + "frozen_reason varchar,updated_at bigint)");
        }
    }

    private static Path dripDbPath(String baseDir) {
        return Paths.get(baseDir).resolve("data").resolve("db_b_whatsapp_drip.data");
    }

    private static Connection openSqlite(Path path) throws Exception {
        return DriverManager.getConnection("jdbc:sqlite:" + path.toString());
    }

    private static JSONObject parseObject(String json) {
        try {
            return isBlank(json) ? new JSONObject() : new JSONObject(json);
        } catch (Throwable ignored) {
            return new JSONObject();
        }
    }

    private static String firstNonBlank(String first, String second) {
        return !isBlank(first) ? first : (second == null ? "" : second);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
