package com.sbf.main.jxbrowser;

import com.teamdev.jxbrowser.browser.Browser;
import com.teamdev.jxbrowser.profile.Profile;
import com.teamdev.jxbrowser.profile.Profiles;
import com.teamdev.jxbrowser.view.swing.BrowserView;
import java.awt.BorderLayout;
import java.awt.Component;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class M8WhatsAppNativeProfiles {
    private static final String DEFAULT_PROFILE = "wa-default";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                    + "AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/124.0.0.0 Safari/537.36";
    private static final Map<c, Map<String, Browser>> BROWSERS = new WeakHashMap<c, Map<String, Browser>>();
    private static final Map<c, Map<String, BrowserView>> VIEWS =
            new WeakHashMap<c, Map<String, BrowserView>>();
    private static c host;

    private M8WhatsAppNativeProfiles() {}

    public static synchronized void registerHost(c candidate, String url) {
        if (candidate == null || url == null || !url.contains("web.whatsapp.com")) {
            return;
        }
        host = candidate;
        String profileId = profileFromUrl(url);
        try {
            rememberExisting(candidate, profileId);
            M5LocalSpiderBridge.setActiveWhatsAppProfile(baseDir(), profileId);
        } catch (Throwable error) {
            System.out.println("M8B1C_NATIVE_PROFILE_REGISTER_FAIL " + error);
        }
        System.out.println("M8B1C_NATIVE_PROFILE_REGISTER profileId=" + profileId + " url=" + url);
    }

    public static synchronized void registerCandidate(c candidate) {
        if (candidate == null) {
            return;
        }
        host = candidate;
        try {
            rememberExisting(candidate, DEFAULT_PROFILE);
        } catch (Throwable error) {
            System.out.println("M8B1C_NATIVE_PROFILE_REGISTER_CANDIDATE_FAIL " + error);
        }
        System.out.println("M8B1C_NATIVE_PROFILE_REGISTER_CANDIDATE");
    }

    public static synchronized String switchProfile(String requestedProfileId) {
        String profileId = normalizeProfileId(requestedProfileId);
        if (host == null) {
            return json(503, profileId, "no_whatsapp_host");
        }
        try {
            Browser browser = ensureBrowser(host, profileId);
            BrowserView view = viewMap(host).get(profileId);
            attach(host, browser, view);
            M5LocalSpiderBridge.setActiveWhatsAppProfile(baseDir(), profileId);
            System.out.println("M8B1C_NATIVE_PROFILE_SWITCH profileId=" + profileId);
            return json(200, profileId, "ok");
        } catch (Throwable error) {
            System.out.println("M8B1C_NATIVE_PROFILE_SWITCH_FAIL profileId=" + profileId + " error=" + error);
            error.printStackTrace(System.out);
            return json(500, profileId, String.valueOf(error));
        }
    }

    public static synchronized Browser ensureProfileBrowser(c target, String requestedProfileId) throws Exception {
        return ensureBrowser(target, normalizeProfileId(requestedProfileId));
    }

    public static synchronized void attachProfileView(c target, String requestedProfileId) throws Exception {
        String profileId = normalizeProfileId(requestedProfileId);
        Browser browser = ensureBrowser(target, profileId);
        attach(target, browser, viewMap(target).get(profileId));
    }

    private static Browser ensureBrowser(c target, String profileId) throws Exception {
        Map<String, Browser> browsers = browserMap(target);
        Browser existing = browsers.get(profileId);
        if (existing != null) {
            return existing;
        }
        if (DEFAULT_PROFILE.equals(profileId)) {
            Browser current = (Browser) field(target, Browser.class).get(target);
            BrowserView currentView = (BrowserView) field(target, BrowserView.class).get(target);
            if (current != null && currentView != null) {
                browsers.put(profileId, current);
                viewMap(target).put(profileId, currentView);
                return current;
            }
        }
        Browser base = (Browser) field(target, Browser.class).get(target);
        if (base == null) {
            throw new IllegalStateException("base browser missing");
        }
        Profiles profiles = base.engine().profiles();
        Profile profile = findOrCreateProfile(profiles, profileName(profileId));
        Browser browser = profile.newBrowser();
        browser.userAgent(USER_AGENT);
        installDiagnostics(browser);
        BrowserView view = BrowserView.newInstance(browser);
        view.setOpaque(false);
        browsers.put(profileId, browser);
        viewMap(target).put(profileId, view);
        browser.navigation().loadUrl(whatsAppUrl(profileId));
        System.out.println("M8B1C_NATIVE_PROFILE_BROWSER_READY profileId=" + profileId);
        return browser;
    }

    private static Profile findOrCreateProfile(Profiles profiles, String name) {
        List<Profile> list = profiles.list();
        for (Profile profile : list) {
            if (name.equals(profile.name())) {
                return profile;
            }
        }
        return profiles.newProfile(name);
    }

    private static void attach(c target, Browser browser, BrowserView view) throws Exception {
        if (target == null || browser == null || view == null) {
            throw new IllegalArgumentException("target/browser/view required");
        }
        Field browserField = field(target, Browser.class);
        Field viewField = field(target, BrowserView.class);
        BrowserView oldView = (BrowserView) viewField.get(target);
        if (oldView != null && oldView != view && oldView.getParent() == target) {
            target.remove((Component) oldView);
        }
        if (view.getParent() != target) {
            target.add(view, BorderLayout.CENTER);
        }
        browserField.set(target, browser);
        viewField.set(target, view);
        target.revalidate();
        target.repaint();
        view.requestFocusInWindow();
    }

    private static void rememberExisting(c target, String profileId) throws Exception {
        Browser browser = (Browser) field(target, Browser.class).get(target);
        BrowserView view = (BrowserView) field(target, BrowserView.class).get(target);
        if (browser != null && view != null) {
            browserMap(target).put(profileId, browser);
            viewMap(target).put(profileId, view);
        }
    }

    private static void installDiagnostics(Browser browser) {
        try {
            Method method = c.class.getDeclaredMethod("m5InstallWebDiagnostics", Browser.class);
            method.setAccessible(true);
            method.invoke(null, browser);
        } catch (Throwable error) {
            System.out.println("M8B1C_NATIVE_PROFILE_DIAG_INSTALL_FAIL " + error);
        }
    }

    private static Field field(c target, Class<?> type) throws Exception {
        Field[] fields = target.getClass().getDeclaredFields();
        for (Field candidate : fields) {
            if (type.isAssignableFrom(candidate.getType())) {
                candidate.setAccessible(true);
                return candidate;
            }
        }
        throw new NoSuchFieldException(type.getName());
    }

    private static Map<String, Browser> browserMap(c target) {
        Map<String, Browser> map = BROWSERS.get(target);
        if (map == null) {
            map = new HashMap<String, Browser>();
            BROWSERS.put(target, map);
        }
        return map;
    }

    private static Map<String, BrowserView> viewMap(c target) {
        Map<String, BrowserView> map = VIEWS.get(target);
        if (map == null) {
            map = new HashMap<String, BrowserView>();
            VIEWS.put(target, map);
        }
        return map;
    }

    private static String profileFromUrl(String url) {
        String marker = "m8Profile=";
        int index = url.indexOf(marker);
        if (index < 0) {
            return DEFAULT_PROFILE;
        }
        int start = index + marker.length();
        int end = url.indexOf('&', start);
        return normalizeProfileId(end < 0 ? url.substring(start) : url.substring(start, end));
    }

    private static String normalizeProfileId(String profileId) {
        String value = profileId == null ? "" : profileId.trim();
        if (value.length() == 0) {
            return DEFAULT_PROFILE;
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            out.append(Character.isLetterOrDigit(c) || c == '-' || c == '_' ? c : '-');
        }
        return out.length() == 0 ? DEFAULT_PROFILE : out.toString();
    }

    private static String profileName(String profileId) {
        return "m8-wa-" + profileId;
    }

    private static String whatsAppUrl(String profileId) throws Exception {
        return "https://web.whatsapp.com/?m8Profile="
                + URLEncoder.encode(profileId, "UTF-8")
                + "&lang=zh_cn";
    }

    private static String baseDir() {
        try {
            Object value = Class.forName("com.sbf.main.StartApp").getField("a").get(null);
            return value == null ? "" : String.valueOf(value);
        } catch (Throwable error) {
            return "";
        }
    }

    private static String json(int code, String profileId, String message) {
        return "{\"code\":"
                + code
                + ",\"profileId\":\""
                + escape(profileId)
                + "\",\"msg\":\""
                + escape(message)
                + "\"}";
    }

    private static String escape(String value) {
        return String.valueOf(value).replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
