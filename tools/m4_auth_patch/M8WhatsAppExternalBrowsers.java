package com.sbf.main.jxbrowser;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONArray;
import org.json.JSONObject;

public final class M8WhatsAppExternalBrowsers {
    private static final String ADSPOWER_BASE = "http://local.adspower.net:50325";
    private static final int DEFAULT_BRIDGE_PORT = 17891;
    private static final Map<String, Process> PROCESSES = new HashMap<String, Process>();
    private static final Map<String, Thread> WATCHERS = new HashMap<String, Thread>();
    private static HttpServer bridgeServer;
    private static String bridgeBaseDir;
    private static int bridgePort = -1;

    private M8WhatsAppExternalBrowsers() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("usage: M8WhatsAppExternalBrowsers <baseDir> probe|serve|stop ...");
            return;
        }
        String baseDir = args[0];
        String command = args[1];
        if ("probe".equals(command)) {
            System.out.println(probe(baseDir));
            return;
        }
        if ("stop".equals(command)) {
            for (int i = 2; i < args.length; i++) {
                System.out.println(stop(baseDir, args[i]));
            }
            return;
        }
        if ("serve".equals(command)) {
            if ((args.length - 2) % 2 != 0) {
                throw new IllegalArgumentException("serve expects profile/phone pairs");
            }
            for (int i = 2; i < args.length; i += 2) {
                System.out.println(start(baseDir, args[i], args[i + 1]));
            }
            System.out.println("M8B1C3_EXTERNAL_BROWSER_SERVE_READY");
            while (true) {
                Thread.sleep(60000L);
            }
        }
        throw new IllegalArgumentException("unknown command: " + command);
    }

    public static synchronized String start(String baseDir, String profileId, String phone)
            throws Exception {
        String normalizedProfileId = normalizeProfileId(profileId);
        String normalizedPhone = phone == null ? "" : phone.trim();
        Path profilePath = externalProfilePath(baseDir, normalizedProfileId);
        Files.createDirectories(profilePath);
        startBridgeServer(baseDir);
        writeExtension(baseDir);

        JSONObject adspower = probeAdsPower();
        JSONObject status =
                new JSONObject()
                        .put("source", "m8b1c3-external-browser")
                        .put("provider", adspower.optBoolean("available") ? "adspower" : "external-browser")
                        .put("profilePath", profilePath.toAbsolutePath().toString())
                        .put("adspower", adspower)
                        .put("bridgePort", bridgePort());
        M5LocalSpiderBridge.upsertWhatsAppAccount(
                baseDir, normalizedProfileId, normalizedPhone, "profile_ready", status.toString());
        M5LocalSpiderBridge.setActiveWhatsAppProfile(baseDir, normalizedProfileId);

        int port = debugPort(normalizedProfileId);
        String url = whatsAppUrl(normalizedProfileId);
        JSONObject data =
                new JSONObject()
                        .put("profileId", normalizedProfileId)
                        .put("phone", normalizedPhone)
                        .put("profilePath", profilePath.toAbsolutePath().toString())
                        .put("debugPort", port)
                        .put("url", url)
                        .put("bridgePort", bridgePort())
                        .put("adspower", adspower);

        if (Boolean.getBoolean("m8.whatsapp.external.dryRun")) {
            data.put("provider", "chromium-dry-run").put("started", true);
            System.out.println(
                    "M8B1C3_EXTERNAL_BROWSER_DRY_RUN profileId="
                            + normalizedProfileId
                            + " port="
                            + port);
            return new JSONObject().put("code", 200).put("msg", "dry_run").put("data", data).toString();
        }

        if (adspower.optBoolean("available")) {
            JSONObject started = startAdsPower(normalizedProfileId);
            if (started.optInt("code", 500) == 200) {
                data.put("provider", "adspower").put("started", true).put("adspowerStart", started);
                return new JSONObject().put("code", 200).put("msg", "adspower_started").put("data", data).toString();
            }
            data.put("adspowerStart", started);
        }

        String browser = findBrowserExecutable();
        if (browser == null) {
            data.put("provider", "chromium").put("started", false);
            return new JSONObject()
                    .put("code", 503)
                    .put("msg", "no_chromium_browser_found")
                    .put("data", data)
                    .toString();
        }

        Process existing = PROCESSES.get(normalizedProfileId);
        if (existing == null) {
            List<String> command = new ArrayList<String>();
            command.add(browser);
            command.add("--remote-debugging-port=" + port);
            command.add("--user-data-dir=" + profilePath.toAbsolutePath().toString());
            command.add("--no-first-run");
            command.add("--no-default-browser-check");
            command.add("--disable-popup-blocking");
            command.add("--disable-extensions-except=" + extensionPath(baseDir).toAbsolutePath().toString());
            command.add("--load-extension=" + extensionPath(baseDir).toAbsolutePath().toString());
            command.add(url);
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(new File(baseDir));
            existing = builder.start();
            PROCESSES.put(normalizedProfileId, existing);
        }
        data.put("provider", "chromium").put("browserExe", browser).put("started", true);
        startCdpWatcher(baseDir, normalizedProfileId, port);
        System.out.println(
                "M8B1C3_EXTERNAL_BROWSER_START profileId="
                        + normalizedProfileId
                        + " port="
                        + port
                        + " browser="
                        + browser);
        return new JSONObject().put("code", 200).put("msg", "started").put("data", data).toString();
    }

    public static synchronized String stop(String baseDir, String profileId) throws Exception {
        String normalizedProfileId = normalizeProfileId(profileId);
        Process process = PROCESSES.remove(normalizedProfileId);
        if (process != null) {
            process.destroy();
        }
        Thread watcher = WATCHERS.remove(normalizedProfileId);
        if (watcher != null) {
            watcher.interrupt();
        }
        JSONObject status =
                new JSONObject()
                        .put("source", "m8b1c3-external-browser")
                        .put("provider", "external-browser")
                        .put("stopped", true);
        M5LocalSpiderBridge.upsertWhatsAppAccount(
                baseDir, normalizedProfileId, "", "stopped", status.toString());
        return new JSONObject()
                .put("code", 200)
                .put("msg", "stopped")
                .put("data", new JSONObject().put("profileId", normalizedProfileId))
                .toString();
    }

    public static synchronized String probe(String baseDir) throws Exception {
        startBridgeServer(baseDir);
        JSONObject adspower = probeAdsPower();
        return new JSONObject()
                .put("code", 200)
                .put("msg", adspower.optBoolean("available") ? "C_adspower_available" : "fallback_B_external_chromium")
                .put("data", new JSONObject().put("adspower", adspower).put("bridgePort", bridgePort()))
                .toString();
    }

    public static int debugPortForTest(String profileId) {
        return debugPort(normalizeProfileId(profileId));
    }

    public static String externalProfilePathForTest(String baseDir, String profileId) {
        return externalProfilePath(baseDir, normalizeProfileId(profileId)).toAbsolutePath().toString();
    }

    public static synchronized void shutdownForTest() {
        if (bridgeServer != null) {
            bridgeServer.stop(0);
            bridgeServer = null;
        }
        bridgePort = -1;
        PROCESSES.clear();
        for (Thread watcher : WATCHERS.values()) {
            watcher.interrupt();
        }
        WATCHERS.clear();
    }

    private static JSONObject startAdsPower(String profileId) {
        try {
            String url =
                    ADSPOWER_BASE
                            + "/api/v2/browser-profile/start?profile_id="
                            + URLEncoder.encode(profileId, "UTF-8");
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(1500);
            conn.setReadTimeout(2500);
            conn.setRequestMethod("GET");
            int status = conn.getResponseCode();
            String body = readResponse(conn);
            return new JSONObject().put("code", status == 200 ? 200 : status).put("body", body);
        } catch (Throwable error) {
            return new JSONObject().put("code", 503).put("error", String.valueOf(error));
        }
    }

    private static synchronized void startCdpWatcher(
            final String baseDir, final String profileId, final int port) {
        Thread existing = WATCHERS.get(profileId);
        if (existing != null && existing.isAlive()) {
            return;
        }
        Thread watcher =
                new Thread(
                        () -> {
                            while (!Thread.currentThread().isInterrupted()) {
                                try {
                                    String snapshot = evaluateCdp(port, captureScript());
                                    if (isChromeErrorSnapshot(snapshot)) {
                                        navigateCdp(port, whatsAppUrl(profileId));
                                        System.out.println(
                                                "M8B1C3_EXTERNAL_CDP_RENAVIGATE profileId="
                                                        + profileId);
                                    }
                                    applySnapshot(baseDir, profileId, snapshot);
                                } catch (Throwable error) {
                                    System.out.println(
                                            "M8B1C3_EXTERNAL_CDP_WATCH_FAIL profileId="
                                                    + profileId
                                                    + " error="
                                                    + error);
                                }
                                try {
                                    Thread.sleep(3000L);
                                } catch (InterruptedException interrupted) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                        },
                        "m8-wa-cdp-" + profileId);
        watcher.setDaemon(true);
        WATCHERS.put(profileId, watcher);
        watcher.start();
    }

    private static void applySnapshot(String baseDir, String profileId, String snapshotJson)
            throws Exception {
        if (isBlank(snapshotJson)) {
            return;
        }
        JSONObject snapshot = new JSONObject(snapshotJson);
        String status = snapshot.optString("status", "unknown");
        String phone = snapshot.optString("phone", "");
        M5LocalSpiderBridge.upsertWhatsAppAccount(
                baseDir,
                profileId,
                phone,
                status,
                new JSONObject()
                        .put("source", "m8b1c3-external-cdp")
                        .put("profileId", profileId)
                        .put(
                                "externalProfilePath",
                                externalProfilePath(baseDir, profileId).toAbsolutePath().toString())
                        .put("snapshot", snapshot)
                        .toString());
        JSONArray messages = snapshot.optJSONArray("messages");
        if (messages == null) {
            return;
        }
        for (int i = 0; i < messages.length(); i++) {
            JSONObject msg = messages.getJSONObject(i);
            M5LocalSpiderBridge.upsertWhatsAppMessage(
                    baseDir,
                    profileId,
                    msg.optString("conversationKey"),
                    msg.optString("contactPhone"),
                    msg.optString("contactName"),
                    msg.optString("direction", "inbound"),
                    msg.optString("sender"),
                    msg.optString("messageText"),
                    msg.optLong("messageTime", System.currentTimeMillis()),
                    msg.optString("externalId"),
                    msg.toString());
        }
    }

    private static String captureScript() {
        return "(function(){"
                + "function clean(s){return String(s||'').replace(/\\s+/g,' ').trim();}"
                + "function key(s,p){s=clean(s);var o='';for(var i=0;i<s.length;i++){var c=s.charAt(i);o+=/[A-Za-z0-9_+@.-]/.test(c)?c:'-';}return o||(p+'-'+Date.now());}"
                + "function hash(s){var h=0;s=String(s||'');for(var i=0;i<s.length;i++){h=((h<<5)-h+s.charCodeAt(i))|0;}return Math.abs(h).toString(16);}"
                + "function status(){try{var b=(document.body&&document.body.innerText)||'';var logged=!!document.querySelector('#pane-side,[data-testid=\"chat-list\"],[aria-label=\"Chat list\"],[aria-label=\"Chats\"]');var qr=!!document.querySelector('canvas,[data-ref],div[data-testid=\"qrcode\"]')||/扫描登录|Scan.*QR|Use your phone/i.test(b);var down=/computer.*not connected|phone.*not connected|disconnected|trying to reach phone/i.test(b);if(down){return 'disconnected';}if(logged){return 'logged_in';}if(qr){return 'qr';}return 'not_logged_in';}catch(e){return 'unknown';}}"
                + "function phone(){try{var candidates=[];var nodes=document.querySelectorAll('header span[title],header [data-testid=\"conversation-info-header-chat-title\"],header [dir=\"auto\"]');for(var i=0;i<nodes.length;i++){candidates.push(nodes[i].getAttribute('title')||nodes[i].innerText||'');}var lines=String((document.body&&document.body.innerText)||'').split(/\\n+/);for(var j=0;j<lines.length;j++){if(/^\\s*\\+\\d/.test(lines[j])){candidates.push(lines[j]);}}for(var k=0;k<candidates.length;k++){var s=clean(candidates[k]);var m=s.match(/\\+\\d[\\d\\s().-]{6,24}\\d/);if(!m){continue;}var p=clean(m[0]).replace(/\\s+(\\d{1,2}:\\d{2}|\\d{1,2}\\/\\d{1,2}\\/\\d{2,4})$/,'');return p;}return '';}catch(e){return '';}}"
                + "function lines(e){return String((e&&e.innerText)||'').split(/\\n+/).map(function(x){return clean(x);}).filter(Boolean);}"
                + "var messages=[];function save(conv,title,phoneNo,dir,sender,msg,ts,id,raw){msg=clean(msg);title=clean(title);if(!conv||!msg){return;}messages.push({conversationKey:conv,contactPhone:phoneNo||'',contactName:title||conv,direction:dir||'inbound',sender:sender||title||'',messageText:msg,messageTime:Math.floor(ts||Date.now()),externalId:id||('dom-'+hash(conv+'|'+msg+'|'+ts)),raw:raw||{}});}"
                + "try{var rows=document.querySelectorAll('#pane-side [role=\"row\"],#pane-side [role=\"listitem\"],#pane-side [data-testid=\"cell-frame-container\"]');for(var i=0;i<rows.length&&i<80;i++){var l=lines(rows[i]);if(l.length<2){continue;}var title=l[0];var preview='';for(var j=l.length-1;j>0;j--){if(!/^\\d{1,2}:\\d{2}/.test(l[j])&&!/^yesterday$/i.test(l[j])&&!/^\\d+$/.test(l[j])){preview=l[j];break;}}if(title&&preview&&title!==preview){save(key(title,'chat'),title,/\\+?\\d[\\d\\s-]{5,}/.test(title)?title:'','inbound',title,preview,Date.now(),'list-'+hash(title+'|'+preview),{surface:'cdp-chat-list'});}}}catch(e){}"
                + "try{var title='';var te=document.querySelector('header span[title],header [data-testid=\"conversation-info-header-chat-title\"],header [dir=\"auto\"]');if(te){title=clean(te.getAttribute('title')||te.innerText);}if(title){var conv=key(title,'chat');var nodes=document.querySelectorAll('[data-id],div.message-in,div.message-out,[data-testid=\"msg-container\"]');for(var n=0;n<nodes.length&&n<140;n++){var el=nodes[n];var txt='';var spans=el.querySelectorAll&&el.querySelectorAll('span.selectable-text');if(spans&&spans.length){var parts=[];for(var s=0;s<spans.length;s++){parts.push(clean(spans[s].innerText));}txt=clean(parts.join(' '));}else{txt=clean(el.innerText);}if(!txt||txt.length>2000){continue;}var cls=String(el.className||'');save(conv,title,'',cls.indexOf('message-out')>=0?'outbound':'inbound',title,txt,Date.now(),(el.getAttribute&&el.getAttribute('data-id'))||('open-'+hash(title+'|'+txt)),{surface:'cdp-open-chat'});}}}catch(e){}"
                + "return JSON.stringify({status:status(),phone:phone(),href:location.href,messages:messages});"
                + "})()";
    }

    private static boolean isChromeErrorSnapshot(String snapshotJson) {
        if (isBlank(snapshotJson)) {
            return false;
        }
        try {
            return new JSONObject(snapshotJson)
                    .optString("href", "")
                    .startsWith("chrome-error://chromewebdata/");
        } catch (Throwable ignored) {
            return snapshotJson.contains("chrome-error://chromewebdata/");
        }
    }

    private static void navigateCdp(int port, String url) throws Exception {
        JSONObject page = findCdpPage(port, true);
        if (page == null) {
            return;
        }
        cdpCall(
                page.getString("webSocketDebuggerUrl"),
                "Page.navigate",
                new JSONObject().put("url", url));
    }

    private static String evaluateCdp(int port, String expression) throws Exception {
        JSONObject page = findCdpPage(port, false);
        if (page == null) {
            return "";
        }
        JSONObject response =
                cdpCall(
                        page.getString("webSocketDebuggerUrl"),
                        "Runtime.evaluate",
                        new JSONObject()
                                .put("expression", expression)
                                .put("returnByValue", true)
                                .put("awaitPromise", true));
        return response
                .optJSONObject("result")
                .optJSONObject("result")
                .optString("value", "");
    }

    private static JSONObject findCdpPage(int port, boolean allowAnyPage) throws Exception {
        JSONArray targets = new JSONArray(httpGet("http://127.0.0.1:" + port + "/json"));
        JSONObject page = null;
        for (int i = 0; i < targets.length(); i++) {
            JSONObject target = targets.getJSONObject(i);
            if ("page".equals(target.optString("type"))
                    && target.optString("url").contains("web.whatsapp.com")) {
                page = target;
                break;
            }
            if (allowAnyPage && page == null && "page".equals(target.optString("type"))) {
                page = target;
            }
        }
        return page;
    }

    private static JSONObject cdpCall(String websocketUrl, String method, JSONObject params)
            throws Exception {
        URI uri = URI.create(websocketUrl);
        String path = uri.getRawPath();
        if (!isBlank(uri.getRawQuery())) {
            path += "?" + uri.getRawQuery();
        }
        try (Socket socket = new Socket(uri.getHost(), uri.getPort())) {
            socket.setSoTimeout(7000);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            byte[] nonce = new byte[16];
            new SecureRandom().nextBytes(nonce);
            String key = Base64.getEncoder().encodeToString(nonce);
            out.write(
                    ("GET "
                                    + path
                                    + " HTTP/1.1\r\nHost: "
                                    + uri.getHost()
                                    + ":"
                                    + uri.getPort()
                                    + "\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Version: 13\r\nSec-WebSocket-Key: "
                                    + key
                                    + "\r\n\r\n")
                            .getBytes(StandardCharsets.UTF_8));
            out.flush();
            readHttpHeaders(in);
            int id = NEXT_CDP_ID.incrementAndGet();
            writeWebSocketText(
                    out,
                    new JSONObject().put("id", id).put("method", method).put("params", params).toString());
            long deadline = System.currentTimeMillis() + 7000L;
            while (System.currentTimeMillis() < deadline) {
                String frame = readWebSocketText(in);
                if (isBlank(frame)) {
                    continue;
                }
                JSONObject message = new JSONObject(frame);
                if (message.optInt("id") == id) {
                    return message;
                }
            }
            throw new IllegalStateException("CDP response timeout");
        }
    }

    private static final AtomicInteger NEXT_CDP_ID = new AtomicInteger();

    private static String httpGet(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(1500);
        conn.setReadTimeout(3000);
        conn.setRequestMethod("GET");
        return readResponse(conn);
    }

    private static void readHttpHeaders(DataInputStream in) throws Exception {
        StringBuilder headers = new StringBuilder();
        int previous = -1;
        int current;
        while ((current = in.read()) >= 0) {
            headers.append((char) current);
            int length = headers.length();
            if (previous == '\r'
                    && current == '\n'
                    && length >= 4
                    && headers.charAt(length - 4) == '\r'
                    && headers.charAt(length - 3) == '\n') {
                break;
            }
            previous = current;
        }
        if (headers.indexOf("101") < 0) {
            throw new IllegalStateException("websocket handshake failed: " + headers);
        }
    }

    private static void writeWebSocketText(DataOutputStream out, String text) throws Exception {
        byte[] payload = text.getBytes(StandardCharsets.UTF_8);
        byte[] mask = new byte[4];
        new SecureRandom().nextBytes(mask);
        out.writeByte(0x81);
        if (payload.length < 126) {
            out.writeByte(0x80 | payload.length);
        } else if (payload.length <= 65535) {
            out.writeByte(0x80 | 126);
            out.writeShort(payload.length);
        } else {
            out.writeByte(0x80 | 127);
            out.writeLong(payload.length);
        }
        out.write(mask);
        for (int i = 0; i < payload.length; i++) {
            out.writeByte(payload[i] ^ mask[i % 4]);
        }
        out.flush();
    }

    private static String readWebSocketText(DataInputStream in) throws Exception {
        while (true) {
            int first = in.readUnsignedByte();
            int opcode = first & 0x0F;
            int second = in.readUnsignedByte();
            boolean masked = (second & 0x80) != 0;
            long length = second & 0x7F;
            if (length == 126) {
                length = in.readUnsignedShort();
            } else if (length == 127) {
                length = in.readLong();
            }
            byte[] mask = null;
            if (masked) {
                mask = new byte[4];
                in.readFully(mask);
            }
            if (length > 10 * 1024 * 1024L) {
                throw new IllegalStateException("websocket frame too large: " + length);
            }
            byte[] payload = new byte[(int) length];
            in.readFully(payload);
            if (masked) {
                for (int i = 0; i < payload.length; i++) {
                    payload[i] = (byte) (payload[i] ^ mask[i % 4]);
                }
            }
            if (opcode == 0x1) {
                return new String(payload, StandardCharsets.UTF_8);
            }
            if (opcode == 0x8) {
                throw new IllegalStateException("websocket closed");
            }
        }
    }

    private static JSONObject probeAdsPower() {
        JSONObject result = new JSONObject().put("baseUrl", ADSPOWER_BASE).put("available", false);
        String[] paths = {"/api/v2/browser-profile/list", "/api/v1/group/list"};
        for (int i = 0; i < paths.length; i++) {
            try {
                HttpURLConnection conn =
                        (HttpURLConnection) new URL(ADSPOWER_BASE + paths[i]).openConnection();
                conn.setConnectTimeout(1200);
                conn.setReadTimeout(1800);
                conn.setRequestMethod("GET");
                int status = conn.getResponseCode();
                String body = readResponse(conn);
                result.put("status", status).put("path", paths[i]).put("body", body);
                if (status == 200) {
                    result.put("available", true);
                    return result;
                }
            } catch (Throwable error) {
                result.put("error", String.valueOf(error)).put("path", paths[i]);
            }
        }
        return result;
    }

    private static synchronized void startBridgeServer(String baseDir) throws Exception {
        if (bridgeServer != null) {
            bridgeBaseDir = baseDir;
            return;
        }
        bridgeBaseDir = baseDir;
        int requestedPort = configuredBridgePort();
        try {
            bridgeServer = HttpServer.create(new InetSocketAddress("127.0.0.1", requestedPort), 0);
        } catch (BindException busy) {
            bridgeServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        }
        bridgePort = bridgeServer.getAddress().getPort();
        bridgeServer.createContext("/wa/ping", new JsonHandler());
        bridgeServer.createContext("/wa/account", new AccountHandler());
        bridgeServer.createContext("/wa/message", new MessageHandler());
        bridgeServer.setExecutor(null);
        bridgeServer.start();
        System.out.println("M8B1C3_EXTERNAL_BRIDGE_SERVER_READY port=" + bridgePort());
    }

    private static final class JsonHandler implements HttpHandler {
        public void handle(HttpExchange exchange) {
            write(exchange, new JSONObject().put("code", 200).put("msg", "pong").toString());
        }
    }

    private static final class AccountHandler implements HttpHandler {
        public void handle(HttpExchange exchange) {
            try {
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    write(exchange, "{}");
                    return;
                }
                JSONObject body = new JSONObject(readAll(exchange.getRequestBody()));
                String profileId = normalizeProfileId(body.optString("profileId"));
                String status = body.optString("status", "unknown");
                String phone = body.optString("phone", "");
                String result =
                        M5LocalSpiderBridge.upsertWhatsAppAccount(
                                bridgeBaseDir, profileId, phone, status, body.toString());
                write(exchange, result);
            } catch (Throwable error) {
                write(exchange, new JSONObject().put("code", 500).put("msg", String.valueOf(error)).toString());
            }
        }
    }

    private static final class MessageHandler implements HttpHandler {
        public void handle(HttpExchange exchange) {
            try {
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    write(exchange, "{}");
                    return;
                }
                JSONObject body = new JSONObject(readAll(exchange.getRequestBody()));
                String result =
                        M5LocalSpiderBridge.upsertWhatsAppMessage(
                                bridgeBaseDir,
                                normalizeProfileId(body.optString("profileId")),
                                body.optString("conversationKey"),
                                body.optString("contactPhone"),
                                body.optString("contactName"),
                                body.optString("direction", "inbound"),
                                body.optString("sender"),
                                body.optString("messageText"),
                                body.optLong("messageTime", System.currentTimeMillis()),
                                body.optString("externalId"),
                                body.toString());
                write(exchange, result);
            } catch (Throwable error) {
                write(exchange, new JSONObject().put("code", 500).put("msg", String.valueOf(error)).toString());
            }
        }
    }

    private static void write(HttpExchange exchange, String body) {
        try {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            Headers headers = exchange.getResponseHeaders();
            headers.add("Content-Type", "application/json;charset=UTF-8");
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Headers", "content-type");
            headers.add("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void writeExtension(String baseDir) throws Exception {
        Path extension = extensionPath(baseDir);
        Files.createDirectories(extension);
        Files.write(
                extension.resolve("manifest.json"),
                extensionManifest().getBytes(StandardCharsets.UTF_8));
        Files.write(
                extension.resolve("background.js"),
                extensionBackgroundScript().getBytes(StandardCharsets.UTF_8));
        Files.write(extension.resolve("content.js"), extensionScript().getBytes(StandardCharsets.UTF_8));
    }

    private static String extensionManifest() {
        return "{\n"
                + "  \"manifest_version\": 3,\n"
                + "  \"name\": \"M8 WhatsApp Local Bridge\",\n"
                + "  \"version\": \"1.0.0\",\n"
                + "  \"permissions\": [],\n"
                + "  \"host_permissions\": [\"https://web.whatsapp.com/*\", \"http://127.0.0.1:*/*\"],\n"
                + "  \"background\": {\"service_worker\": \"background.js\"},\n"
                + "  \"content_scripts\": [{\"matches\": [\"https://web.whatsapp.com/*\"], \"js\": [\"content.js\"], \"run_at\": \"document_idle\"}]\n"
                + "}\n";
    }

    private static String extensionBackgroundScript() {
        return "chrome.runtime.onMessage.addListener(function(msg,sender,sendResponse){\n"
                + "try{if(!msg||!msg.path){sendResponse({code:400});return false;}\n"
                + "fetch('http://127.0.0.1:" + bridgePort() + "'+msg.path,{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify(msg.body||{})})\n"
                + ".then(function(r){return r.text();}).then(function(t){sendResponse({code:200,body:t});}).catch(function(e){sendResponse({code:500,error:String(e)});});\n"
                + "}catch(e){sendResponse({code:500,error:String(e)});}return true;});\n";
    }

    private static String extensionScript() {
        return "(function(){\n"
                + "if(window.__m8b1c3ExternalBridge){return;}window.__m8b1c3ExternalBridge=true;\n"
                + "var port=" + bridgePort() + ";var seen={};\n"
                + "function clean(s){return String(s||'').replace(/\\s+/g,' ').trim();}\n"
                + "function key(s,p){s=clean(s);var o='';for(var i=0;i<s.length;i++){var c=s.charAt(i);o+=/[A-Za-z0-9_+@.-]/.test(c)?c:'-';}return o||(p+'-'+Date.now());}\n"
                + "function hash(s){var h=0;s=String(s||'');for(var i=0;i<s.length;i++){h=((h<<5)-h+s.charCodeAt(i))|0;}return Math.abs(h).toString(16);}\n"
                + "function pid(){try{var q=new URL(location.href).searchParams.get('m8Profile')||'';if(q){localStorage.setItem('__m8b1c3ProfileId',q);return q;}return localStorage.getItem('__m8b1c3ProfileId')||'wa-default';}catch(e){return 'wa-default';}}\n"
                + "function post(path,obj){try{obj.profileId=pid();if(chrome&&chrome.runtime&&chrome.runtime.sendMessage){chrome.runtime.sendMessage({path:path,body:obj},function(){});}else{fetch('http://127.0.0.1:'+port+path,{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify(obj)}).catch(function(){});}}catch(e){}}\n"
                + "function status(){try{var b=(document.body&&document.body.innerText)||'';var logged=!!document.querySelector('#pane-side,[data-testid=\"chat-list\"],[aria-label=\"Chat list\"],[aria-label=\"Chats\"]');var qr=!!document.querySelector('canvas,[data-ref],div[data-testid=\"qrcode\"]');var down=/computer.*not connected|phone.*not connected|disconnected|trying to reach phone/i.test(b);if(down){return 'disconnected';}if(logged){return 'logged_in';}if(qr){return 'qr';}return 'not_logged_in';}catch(e){return 'unknown';}}\n"
                + "function phone(){try{var title=document.title||'';var m=title.match(/\\+?\\d[\\d\\s-]{5,}/);return m?clean(m[0]):'';}catch(e){return '';}}\n"
                + "function lines(e){return String((e&&e.innerText)||'').split(/\\n+/).map(function(x){return clean(x);}).filter(Boolean);}\n"
                + "function save(conv,title,phoneNo,dir,sender,msg,ts,id,raw){msg=clean(msg);title=clean(title);if(!conv||!msg){return;}var mid=id||('dom-'+hash(conv+'|'+msg+'|'+ts));var d=pid()+'|'+conv+'|'+mid;if(seen[d]){return;}seen[d]=1;post('/wa/message',{conversationKey:conv,contactPhone:phoneNo||'',contactName:title||conv,direction:dir||'inbound',sender:sender||title||'',messageText:msg,messageTime:Math.floor(ts||Date.now()),externalId:mid,raw:raw||{}});}\n"
                + "function captureList(){try{var rows=document.querySelectorAll('#pane-side [role=\"row\"],#pane-side [role=\"listitem\"],#pane-side [data-testid=\"cell-frame-container\"]');for(var i=0;i<rows.length&&i<80;i++){var l=lines(rows[i]);if(l.length<2){continue;}var title=l[0];var preview='';for(var j=l.length-1;j>0;j--){if(!/^\\d{1,2}:\\d{2}/.test(l[j])&&!/^yesterday$/i.test(l[j])&&!/^\\d+$/.test(l[j])){preview=l[j];break;}}if(title&&preview&&title!==preview){save(key(title,'chat'),title,/\\+?\\d[\\d\\s-]{5,}/.test(title)?title:'','inbound',title,preview,Date.now(),'list-'+hash(title+'|'+preview),{surface:'external-chat-list'});}}}catch(e){}}\n"
                + "function currentTitle(){try{var e=document.querySelector('header span[title],header [data-testid=\"conversation-info-header-chat-title\"],header [dir=\"auto\"]');return clean(e&&(e.getAttribute('title')||e.innerText));}catch(e){return '';}}\n"
                + "function captureOpen(){try{var title=currentTitle();if(!title){return;}var conv=key(title,'chat');var nodes=document.querySelectorAll('[data-id],div.message-in,div.message-out,[data-testid=\"msg-container\"]');for(var i=0;i<nodes.length&&i<140;i++){var n=nodes[i];var msg='';var spans=n.querySelectorAll&&n.querySelectorAll('span.selectable-text');if(spans&&spans.length){var parts=[];for(var s=0;s<spans.length;s++){parts.push(clean(spans[s].innerText));}msg=clean(parts.join(' '));}else{msg=clean(n.innerText);}if(!msg||msg.length>2000){continue;}var cls=String(n.className||'');save(conv,title,'',cls.indexOf('message-out')>=0?'outbound':'inbound',title,msg,Date.now(),(n.getAttribute&&n.getAttribute('data-id'))||('open-'+hash(title+'|'+msg)),{surface:'external-open-chat'});}}catch(e){}}\n"
                + "function tick(){post('/wa/account',{status:status(),phone:phone(),href:location.href,source:'external-browser'});captureList();captureOpen();}\n"
                + "tick();setInterval(tick,3000);console.log('M8B1C3_EXTERNAL_BRIDGE_READY '+pid());\n"
                + "})();\n";
    }

    private static String findBrowserExecutable() {
        String configured = firstNonBlank(System.getProperty("m8.whatsapp.browser.exe"), System.getenv("M8_WHATSAPP_BROWSER_EXE"));
        if (!isBlank(configured) && Files.isRegularFile(Paths.get(configured))) {
            return configured;
        }
        String[] candidates = {
            "C:\\\\Program Files\\\\Google\\\\Chrome\\\\Application\\\\chrome.exe",
            "C:\\\\Program Files (x86)\\\\Google\\\\Chrome\\\\Application\\\\chrome.exe",
            "C:\\\\Program Files\\\\Microsoft\\\\Edge\\\\Application\\\\msedge.exe",
            "C:\\\\Program Files (x86)\\\\Microsoft\\\\Edge\\\\Application\\\\msedge.exe"
        };
        for (int i = 0; i < candidates.length; i++) {
            if (Files.isRegularFile(Paths.get(candidates[i]))) {
                return candidates[i];
            }
        }
        return null;
    }

    private static Path externalProfilePath(String baseDir, String profileId) {
        return Paths.get(baseDir).resolve("bscache").resolve("wa-external-profiles").resolve(profileId);
    }

    private static Path extensionPath(String baseDir) {
        return Paths.get(System.getProperty("java.io.tmpdir")).resolve("m8-wa-external-extension");
    }

    private static String whatsAppUrl(String profileId) throws Exception {
        return "https://web.whatsapp.com/?m8Profile=" + URLEncoder.encode(profileId, "UTF-8");
    }

    private static int debugPort(String profileId) {
        return 43000 + Math.abs(profileId.hashCode() % 2000);
    }

    private static int bridgePort() {
        return bridgePort > 0 ? bridgePort : configuredBridgePort();
    }

    private static int configuredBridgePort() {
        try {
            String value = System.getProperty("m8.whatsapp.bridge.port");
            if (!isBlank(value)) {
                int parsed = Integer.parseInt(value.trim());
                if (parsed >= 0 && parsed <= 65535) {
                    return parsed;
                }
            }
        } catch (Throwable ignored) {
        }
        return DEFAULT_BRIDGE_PORT;
    }

    private static String normalizeProfileId(String profileId) {
        String value = isBlank(profileId) ? "wa-default" : profileId.trim();
        StringBuilder safe = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            safe.append(Character.isLetterOrDigit(c) || c == '-' || c == '_' ? c : '-');
        }
        return safe.length() == 0 ? "wa-default" : safe.toString();
    }

    private static String firstNonBlank(String first, String fallback) {
        return isBlank(first) ? fallback : first;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String readResponse(HttpURLConnection conn) throws Exception {
        InputStream in = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
        return in == null ? "" : readAll(in);
    }

    private static String readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = in.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
