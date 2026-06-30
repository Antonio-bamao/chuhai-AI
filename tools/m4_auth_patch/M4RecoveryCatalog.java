public final class M4RecoveryCatalog {
    private static final int PRODUCT_ID_BASE = 9101;
    private static final int WHATSAPP_PRODUCT_ID = PRODUCT_ID_BASE;
    private static final int WHATSAPP_AI_COLLECT_MENU_ID = WHATSAPP_PRODUCT_ID * 100 + 5;
    private static final int WHATSAPP_AI_COLLECT_ROUTE_CHILD_ID_BASE =
            WHATSAPP_AI_COLLECT_MENU_ID * 100;
    private static final int WHATSAPP_AI_DATA_MENU_ID = WHATSAPP_PRODUCT_ID * 100 + 6;
    private static final int WHATSAPP_AI_DATA_ROUTE_CHILD_ID =
            WHATSAPP_AI_DATA_MENU_ID * 100 + 1;
    private static final int WHATSAPP_ONELINE_MENU_ID = WHATSAPP_PRODUCT_ID * 100 + 1;
    private static final int WHATSAPP_ONELINE_ROUTE_CHILD_ID =
            WHATSAPP_ONELINE_MENU_ID * 100 + 1;
    private static final int WHATSAPP_AI_FILTER_MENU_ID = WHATSAPP_PRODUCT_ID * 100 + 7;
    private static final int WHATSAPP_AI_FILTER_ROUTE_CHILD_ID =
            WHATSAPP_AI_FILTER_MENU_ID * 100 + 1;
    private static final int WHATSAPP_AI_KEFU_MENU_ID = WHATSAPP_PRODUCT_ID * 100 + 11;
    private static final int WHATSAPP_AI_KEFU_ROUTE_CHILD_ID =
            WHATSAPP_AI_KEFU_MENU_ID * 100 + 1;
    private static final String EXPIRATION = "2099-12-31 23:59:59";

    private static final ProductSpec[] PRODUCTS = {
        new ProductSpec("whatsapp", "WhatsApp AI龙虾系统", "#25D366", "#128C7E", true),
        new ProductSpec("tiktok", "TK AI龙虾系统", "#111111", "#25F4EE", true),
        new ProductSpec("facebook", "FB AI龙虾系统", "#1877F2", "#0866FF", true),
        new ProductSpec("instagram", "Ins AI龙虾系统", "#E1306C", "#833AB4", true),
        new ProductSpec("twitter", "X AI龙虾系统", "#111111", "#536471", true),
        new ProductSpec("telegram", "TG AI龙虾系统", "#229ED9", "#168ACD", true),
        new ProductSpec("geo", "海外GEO AI龙虾系统", "#0EA5E9", "#14B8A6", true),
        new ProductSpec("wskefu", "WhatsApp AI龙虾客服", "#16A34A", "#0F766E", true),
        new ProductSpec("aishope", "独立站 AI龙虾系统", "#F97316", "#7C3AED", false)
    };

    private static final MenuSpec[][] MENUS = {
        {
            oneLineRoute("REC_WHATSAPP_ONELINE", "一句话", "svg/whatsapp_menu_icon_1.svg"),
            recovered("REC_WHATSAPP_AGENT_MODEL", "智能体模型", "svg/whatsapp_menu_icon_2.svg"),
            recovered("REC_WHATSAPP_CLAW", "AI龙虾", "svg/whatsapp_menu_icon_3.svg"),
            recovered("REC_WHATSAPP_SUPER", "超级号", "svg/whatsapp_menu_icon_4.svg"),
            spiderRoute("C4749_006", "AI采集", "svg/whatsapp_menu_icon_5.svg", "whatsapp_users_lists"),
            original("C4749_007", "AI数据", "svg/whatsapp_menu_icon_6.svg"),
            wsFilterRoute("C4749_009", "AI筛选", "svg/whatsapp_menu_icon_7.svg"),
            original("C4749_005", "AI群发", "svg/whatsapp_menu_icon_8.svg"),
            original("C4749_", "API", "svg/whatsapp_menu_icon_9.svg"),
            original("C3460_001", "广告", "svg/whatsapp_menu_icon_8.svg"),
            kefuRoute("C4749_011", "AI客服", "svg/whatsapp_menu_icon_9.svg")
        },
        {
            original("C3461_002", "TK AI采集", "svg/menu_tk_1.svg"),
            original("C3461_003", "TK AI筛选", "svg/menu_tk_2.svg"),
            original("C3461_004", "TK 镜像系统", "svg/menu_tk_3.svg"),
            original("C3461_005", "TK IOS多号", "svg/menu_tk_4.svg"),
            original("C3461_006", "TK AI超级号", "svg/menu_tk_5.svg"),
            original("C3461_007", "TK API发布", "svg/menu_tk_6.svg"),
            original("C3461_008", "TK AI直播", "svg/menu_tk_7.svg"),
            original("C3461_010", "TK 云采集", "svg/menu_tk_8.svg"),
            original("C3461_011", "TK AI上热门", "svg/menu_tk_9.svg"),
            original("C3461_012", "TK 云筛选", "svg/menu_tk_10.svg")
        },
        {
            original("C4747_000", "镜像系统设置", "svg/facebook_menu_icon_1.svg"),
            original("C4747_001", "FB 好友采集", "svg/facebook_menu_icon_2.svg"),
            original("C4747_002", "FB 小组采集", "svg/facebook_menu_icon_3.svg"),
            original("C4747_003", "FB 主页采集", "svg/facebook_menu_icon_4.svg"),
            original("C4747_004", "FB 直播采集", "svg/facebook_menu_icon_5.svg"),
            original("C4747_005", "FB 广告采集", "svg/facebook_menu_icon_6.svg"),
            original("C4747_006", "FB 广告评论截流", "svg/facebook_menu_icon_7.svg"),
            original("C4747_007", "FB 视频截流", "svg/facebook_menu_icon_8.svg"),
            original("C4747_008", "FB 活跃用户检测", "svg/facebook_menu_icon_9.svg"),
            original("C4747_009", "FB 询盘回复", "svg/facebook_menu_icon_10.svg")
        },
        {
            original("C4131_002", "Ins 帐号登录", "svg/ins_menu_icon_1.svg"),
            original("C4131_003", "Ins 帐号搜索", "svg/ins_menu_icon_2.svg"),
            original("C4131_004", "Ins 帖子搜索", "svg/ins_menu_icon_3.svg"),
            original("C4131_005", "Ins 主页挖掘", "svg/ins_menu_icon_4.svg"),
            original("C4131_006", "Ins 筛选活跃", "svg/ins_menu_icon_5.svg"),
            original("C4131_007", "Ins 接口群发", "svg/ins_menu_icon_6.svg"),
            original("C4131_008", "Ins 安卓智能体", "svg/ins_menu_icon_7.svg"),
            original("C4131_009", "Ins AiCloud指纹", "svg/ins_menu_icon_8.svg"),
            original("C4131_010", "Ins AdsPower指纹", "svg/ins_menu_icon_9.svg")
        },
        {
            original("C4133_002", "X 账号登录", "svg/twitter_menu_icon_1.svg"),
            original("C4133_003", "X 精准搜索", "svg/twitter_menu_icon_2.svg"),
            original("C4133_004", "X 同行的粉丝搜索", "svg/twitter_menu_icon_3.svg"),
            original("C4133_005", "X 筛选活跃", "svg/twitter_menu_icon_4.svg"),
            original("C4133_006", "X 主页大数据库", "svg/twitter_menu_icon_5.svg"),
            original("C4133_007", "X 安卓智能体", "svg/twitter_menu_icon_6.svg"),
            original("C4133_008", "X AiCloud指纹", "svg/twitter_menu_icon_7.svg"),
            original("C4133_009", "X AdsPower指纹", "svg/twitter_menu_icon_8.svg"),
            original("C4133_017", "X 跳推系统", "svg/twitter_menu_icon_9.svg")
        },
        {
            original("C4135_001", "TG 跳推系统", "svg/tg_menu_icon_1.svg"),
            original("C4135_002", "TG 帐号", "svg/tg_menu_icon_2.svg"),
            original("C4135_003", "TG AI 采集", "svg/tg_menu_icon_3.svg"),
            original("C4135_004", "TG AI数据", "svg/tg_menu_icon_4.svg"),
            original("C4135_005", "TG AI 群采集", "svg/tg_menu_icon_5.svg"),
            original("C4135_006", "TG AI 群成员提取", "svg/tg_menu_icon_6.svg"),
            original("C4135_007", "TG AI筛选", "svg/tg_menu_icon_7.svg"),
            original("C4135_008", "TG AI裂变", "svg/tg_menu_icon_8.svg"),
            original("C4135_009", "TG 安卓智能体", "svg/tg_menu_icon_9.svg"),
            original("C4135_010", "TG AiCloud指纹", "svg/tg_menu_icon_10.svg"),
            original("C4135_011", "TG AdsPower指纹", "svg/tg_menu_icon_11.svg")
        },
        {
            original("C4134_002", "精准官网挖掘", "svg/geo_ai_menu_icon_1.svg"),
            original("C4134_003", "精准号码挖掘", "svg/geo_ai_menu_icon_2.svg"),
            original("C4134_006", "Google GEO外媒体", "svg/geo_ai_menu_icon_3.svg"),
            original("C4137_001", "全球号码采集", "svg/geo_ai_menu_icon_4.svg"),
            original("C4137_002", "全球地区采集", "svg/geo_ai_menu_icon_5.svg"),
            original("C4137_003", "海关数据挖掘", "svg/geo_ai_menu_icon_6.svg"),
            original("C4137_004", "全球企业大数据", "svg/geo_ai_menu_icon_7.svg"),
            original("C4137_005", "全球大数据", "svg/geo_ai_menu_icon_8.svg"),
            original("C4137_006", "号码 AI筛选活跃", "svg/geo_ai_menu_icon_9.svg")
        },
        {
            original("C4936_000", "信息总览", "svg/wskf_menu_icon_1.svg"),
            original("C4936_001", "账号分组", "svg/wskf_menu_icon_2.svg"),
            original("C4936_002", "账号列表", "svg/wskf_menu_icon_3.svg"),
            original("C4936_004", "联系人数据池", "svg/wskf_menu_icon_4.svg"),
            original("C4936_005", "爆粉群发", "svg/wskf_menu_icon_5.svg"),
            original("C4936_006", "群聊群发", "svg/wskf_menu_icon_6.svg"),
            original("C4936_007", "客服列表", "svg/wskf_menu_icon_7.svg")
        },
        {}
    };

    private M4RecoveryCatalog() {
    }

    public static String productModulesJson() {
        return productModulesJson(null);
    }

    public static String productModulesJson(java.util.Map<String, String> productLogos) {
        StringBuilder json = new StringBuilder(32768);
        json.append("{\"code\":200,\"msg\":\"offline ok\",\"data\":[");
        for (int index = 0; index < PRODUCTS.length; index++) {
            if (index > 0) {
                json.append(',');
            }
            appendProduct(json, PRODUCTS[index], PRODUCT_ID_BASE + index, productLogos);
        }
        json.append("]}");
        return json.toString();
    }

    public static String pcMenusJson() {
        StringBuilder json = new StringBuilder(65536);
        json.append("{\"scfs\":[");
        boolean first = true;
        for (int productIndex = 0; productIndex < 8; productIndex++) {
            int productId = PRODUCT_ID_BASE + productIndex;
            MenuSpec[] productMenus = MENUS[productIndex];
            for (int menuIndex = 0; menuIndex < productMenus.length; menuIndex++) {
                if (!first) {
                    json.append(',');
                }
                appendMenu(json, productMenus[menuIndex], productId, menuIndex + 1);
                if (isWhatsappOneLineMenu(productId, productMenus[menuIndex])) {
                    json.append(',');
                    appendWhatsappOneLineRouteChild(json);
                }
                if (isWhatsappCollectMenu(productId, productMenus[menuIndex])) {
                    json.append(',');
                    appendWhatsappCollectRouteChildren(json);
                }
                if (isWhatsappAiDataMenu(productId, productMenus[menuIndex])) {
                    json.append(',');
                    appendWhatsappAiDataRouteChild(json);
                }
                if (isWhatsappAiFilterMenu(productId, productMenus[menuIndex])) {
                    json.append(',');
                    appendWhatsappAiFilterRouteChild(json);
                }
                if (isWhatsappAiKefuMenu(productId, productMenus[menuIndex])) {
                    json.append(',');
                    appendWhatsappAiKefuRouteChild(json);
                }
                first = false;
            }
        }
        json.append("],\"tas\":\"[]\",\"ucf\":{")
                .append("\"mnq_license_num\":999,\"ads_browsers_license_num\":999,")
                .append("\"open_mnq_ndk_license\":1,\"kefu_whatsapp_mass_sending_flg\":1")
                .append("},\"scfsVersion\":1,\"sellInZwNum\":0,\"sellOutZwNum\":0}");
        return json.toString();
    }

    private static void appendProduct(
            StringBuilder json,
            ProductSpec product,
            int id,
            java.util.Map<String, String> productLogos) {
        json.append('{');
        appendNumber(json, "id", id);
        appendNumber(json, "sid", id);
        appendNumber(json, "fid", id);
        appendString(json, "code", product.code);
        appendString(json, "name", product.displayName);
        appendString(json, "displayName", product.displayName);
        appendNumber(json, "status", product.enterable ? 1 : 2);
        appendNumber(json, "remainingDays", product.enterable ? 99999 : 0);
        appendString(json, "expirationTime", product.enterable ? EXPIRATION : "");
        String logoSvg = productLogos == null ? null : productLogos.get(product.code);
        if (logoSvg == null || logoSvg.trim().length() == 0) {
            logoSvg =
                    "<svg viewBox=\"0 0 24 24\" xmlns=\"http://www.w3.org/2000/svg\">"
                            + "<circle cx=\"12\" cy=\"12\" r=\"11\" fill=\""
                            + product.primaryColor
                            + "\"/></svg>";
        }
        appendString(json, "logoSvg", logoSvg);
        appendString(json, "themeStyle", "recovery-default");
        appendString(json, "themeColor", product.primaryColor);
        appendString(json, "primaryColor", product.primaryColor);
        appendString(json, "secondaryColor", product.secondaryColor);
        appendString(json, "primary_color", product.primaryColor);
        appendString(json, "secondary_color", product.secondaryColor);
        appendString(json, "bgcolor", "#0B0F14");
        appendString(json, "menuBgColor", "#FFFFFF");
        appendString(json, "menuTextColor", "#222222");
        appendString(json, "menuActiveBgColor", "#E6F4FF");
        appendString(json, "menuActiveTextColor", product.primaryColor);
        appendString(json, "buttonColor", product.primaryColor);
        appendString(json, "buttonTextColor", "#FFFFFF");
        appendString(json, "menuAreaBackground", "#111827");
        appendString(json, "menuItemDefaultTextColor", "#D1D5DB");
        appendString(json, "menuItemHoverTextColor", "#FFFFFF");
        appendString(json, "menuItemHoverBackgroundColor", "#1F2937");
        appendString(json, "menuItemSelectedTextColor", "#FFFFFF");
        appendString(json, "menuItemSelectedBackgroundColor", product.primaryColor);
        appendString(json, "topBarBackground", "#0B1220");
        appendString(json, "topBarDefaultTextColor", "#E5E7EB");
        appendString(json, "topMenuItemHoverTextColor", "#FFFFFF");
        appendString(json, "topMenuItemHoverBackgroundColor", "#1F2937");
        appendString(json, "topMenuItemSelectedTextColor", "#FFFFFF");
        appendString(json, "topMenuItemSelectedBackgroundColor", product.primaryColor);
        appendString(json, "defaultBtnFontColor", "#FFFFFF");
        appendString(json, "defaultBtnBackgroundColor", product.primaryColor);
        json.append("\"children\":[");
        MenuSpec[] productMenus = MENUS[id - PRODUCT_ID_BASE];
        for (int menuIndex = 0; menuIndex < productMenus.length; menuIndex++) {
            if (menuIndex > 0) {
                json.append(',');
            }
            appendMenu(json, productMenus[menuIndex], id, menuIndex + 1);
            if (isWhatsappOneLineMenu(id, productMenus[menuIndex])) {
                json.append(',');
                appendWhatsappOneLineRouteChild(json);
            }
            if (isWhatsappCollectMenu(id, productMenus[menuIndex])) {
                json.append(',');
                appendWhatsappCollectRouteChildren(json);
            }
            if (isWhatsappAiDataMenu(id, productMenus[menuIndex])) {
                json.append(',');
                appendWhatsappAiDataRouteChild(json);
            }
            if (isWhatsappAiFilterMenu(id, productMenus[menuIndex])) {
                json.append(',');
                appendWhatsappAiFilterRouteChild(json);
            }
            if (isWhatsappAiKefuMenu(id, productMenus[menuIndex])) {
                json.append(',');
                appendWhatsappAiKefuRouteChild(json);
            }
        }
        json.append(']');
        json.append('}');
    }

    private static void appendMenu(
            StringBuilder json, MenuSpec menu, int productId, int menuIndex) {
        int recoveryId = productId * 100 + menuIndex;
        json.append('{');
        appendNumber(json, "id", recoveryId);
        appendNumber(json, "sid", productId);
        appendNumber(json, "fid", productId);
        appendNumber(json, "productId", productId);
        appendNumber(json, "parentId", productId);
        appendString(json, "code", menu.code);
        appendString(json, "name", menu.name);
        appendString(json, "displayName", menu.name);
        appendString(json, "icon", iconResourceName(menu.icon));
        appendString(json, "localCode", menu.localCode);
        appendString(json, "linkUrl", menu.linkUrl);
        appendNumber(json, "webFlg", 1);
        appendNumber(json, "treeEndFlg", 1);
        appendNumber(json, "displayIndex", menuIndex);
        appendNumber(json, "sort", menuIndex);
        appendString(json, "evidence", menu.evidence);
        json.append("\"status\":1");
        json.append('}');
    }

    private static boolean isWhatsappCollectMenu(int productId, MenuSpec menu) {
        return productId == WHATSAPP_PRODUCT_ID && "C4749_006".equals(menu.code);
    }

    private static boolean isWhatsappOneLineMenu(int productId, MenuSpec menu) {
        return productId == WHATSAPP_PRODUCT_ID && "REC_WHATSAPP_ONELINE".equals(menu.code);
    }

    private static boolean isWhatsappAiDataMenu(int productId, MenuSpec menu) {
        return productId == WHATSAPP_PRODUCT_ID && "C4749_007".equals(menu.code);
    }

    private static boolean isWhatsappAiFilterMenu(int productId, MenuSpec menu) {
        return productId == WHATSAPP_PRODUCT_ID && "C4749_009".equals(menu.code);
    }

    private static boolean isWhatsappAiKefuMenu(int productId, MenuSpec menu) {
        return productId == WHATSAPP_PRODUCT_ID && "C4749_011".equals(menu.code);
    }

    private static void appendWhatsappCollectRouteChildren(StringBuilder json) {
        appendWhatsappCollectRouteChild(
                json,
                1,
                "REC_WHATSAPP_COLLECT_TAB_GLOBAL_NUMBER",
                "全球号码采集",
                "whatsapp_users_lists");
        json.append(',');
        appendWhatsappCollectRouteChild(
                json,
                2,
                "REC_WHATSAPP_COLLECT_TAB_WS_NUMBER",
                "WS号码采集",
                "wap_global_clue_users");
        json.append(',');
        appendWhatsappCollectRouteChild(
                json,
                3,
                "REC_WHATSAPP_COLLECT_TAB_WS_GROUP",
                "WS小组采集",
                "whatsapp_group_lists");
        json.append(',');
        appendWhatsappCollectRouteChild(
                json,
                4,
                "REC_WHATSAPP_COLLECT_TAB_WS_REGION",
                "WS地区采集",
                "whatsapp_regional_collection");
    }

    private static void appendWhatsappCollectRouteChild(
            StringBuilder json, int displayIndex, String code, String name, String spiderCode) {
        String route =
                "/pc/dataCollect/collectionTask?modal="
                        + spiderCode
                        + "&moduleCode=whatsapp";
        json.append('{');
        appendNumber(json, "id", WHATSAPP_AI_COLLECT_ROUTE_CHILD_ID_BASE + displayIndex);
        appendNumber(json, "sid", WHATSAPP_PRODUCT_ID);
        appendNumber(json, "fid", WHATSAPP_PRODUCT_ID);
        appendNumber(json, "productId", WHATSAPP_PRODUCT_ID);
        appendNumber(json, "parentId", WHATSAPP_AI_COLLECT_MENU_ID);
        appendString(json, "code", code);
        appendString(json, "name", name);
        appendString(json, "displayName", name);
        appendString(json, "icon", "whatsapp_menu_icon_5");
        appendString(json, "localCode", route);
        appendString(json, "linkUrl", "JSinglepage");
        appendNumber(json, "webFlg", 1);
        appendNumber(json, "treeEndFlg", 1);
        appendNumber(json, "displayIndex", displayIndex);
        appendNumber(json, "sort", displayIndex);
        appendString(
                json,
                "evidence",
                "m5d11-menu-tab:dataCollect:" + spiderCode);
        json.append("\"status\":1");
        json.append('}');
    }

    private static void appendWhatsappAiDataRouteChild(StringBuilder json) {
        json.append('{');
        appendNumber(json, "id", WHATSAPP_AI_DATA_ROUTE_CHILD_ID);
        appendNumber(json, "sid", WHATSAPP_PRODUCT_ID);
        appendNumber(json, "fid", WHATSAPP_PRODUCT_ID);
        appendNumber(json, "productId", WHATSAPP_PRODUCT_ID);
        appendNumber(json, "parentId", WHATSAPP_AI_DATA_MENU_ID);
        appendString(json, "code", "REC_WHATSAPP_AI_DATA_ROUTE");
        appendString(json, "name", "AI数据");
        appendString(json, "displayName", "AI数据");
        appendString(json, "icon", "whatsapp_menu_icon_6");
        appendString(
                json,
                "localCode",
                "/pc/aicloud/my");
        appendString(json, "linkUrl", "JSinglepage:/pc/aicloud/my");
        appendNumber(json, "webFlg", 1);
        appendNumber(json, "treeEndFlg", 1);
        appendNumber(json, "displayIndex", 1);
        appendNumber(json, "sort", 1);
        appendString(
                json,
                "evidence",
                "recovery-route-child:j2026-h-field-map:aicloud-my");
        json.append("\"status\":1");
        json.append('}');
    }

    private static void appendWhatsappOneLineRouteChild(StringBuilder json) {
        json.append('{');
        appendNumber(json, "id", WHATSAPP_ONELINE_ROUTE_CHILD_ID);
        appendNumber(json, "sid", WHATSAPP_PRODUCT_ID);
        appendNumber(json, "fid", WHATSAPP_PRODUCT_ID);
        appendNumber(json, "productId", WHATSAPP_PRODUCT_ID);
        appendNumber(json, "parentId", WHATSAPP_ONELINE_MENU_ID);
        appendString(json, "code", "REC_WHATSAPP_ONELINE_ROUTE");
        appendString(json, "name", "一句话");
        appendString(json, "displayName", "一句话");
        appendString(json, "icon", "whatsapp_menu_icon_1");
        appendString(json, "localCode", "/pc/aigc/aichat_dialog");
        appendString(json, "linkUrl", "JSinglepage:/pc/aigc/aichat_dialog");
        appendNumber(json, "webFlg", 1);
        appendNumber(json, "treeEndFlg", 1);
        appendNumber(json, "displayIndex", 1);
        appendNumber(json, "sort", 1);
        appendString(
                json,
                "evidence",
                "recovery-route-child:j2026-h-field-map:aichat-dialog");
        json.append("\"status\":1");
        json.append('}');
    }

    private static void appendWhatsappAiFilterRouteChild(StringBuilder json) {
        json.append('{');
        appendNumber(json, "id", WHATSAPP_AI_FILTER_ROUTE_CHILD_ID);
        appendNumber(json, "sid", WHATSAPP_PRODUCT_ID);
        appendNumber(json, "fid", WHATSAPP_PRODUCT_ID);
        appendNumber(json, "productId", WHATSAPP_PRODUCT_ID);
        appendNumber(json, "parentId", WHATSAPP_AI_FILTER_MENU_ID);
        appendString(json, "code", "REC_WHATSAPP_AI_FILTER_ROUTE");
        appendString(json, "name", "AI筛选");
        appendString(json, "displayName", "AI筛选");
        appendString(json, "icon", "whatsapp_menu_icon_7");
        appendString(json, "localCode", "/ws/wsfilter/home");
        appendString(json, "linkUrl", "JSinglepage:/ws/wsfilter/home");
        appendNumber(json, "webFlg", 1);
        appendNumber(json, "treeEndFlg", 1);
        appendNumber(json, "displayIndex", 1);
        appendNumber(json, "sort", 1);
        appendString(
                json,
                "evidence",
                "recovery-route-child:j2026-h-field-map:wsfilter-home");
        json.append("\"status\":1");
        json.append('}');
    }

    private static void appendWhatsappAiKefuRouteChild(StringBuilder json) {
        json.append('{');
        appendNumber(json, "id", WHATSAPP_AI_KEFU_ROUTE_CHILD_ID);
        appendNumber(json, "sid", WHATSAPP_PRODUCT_ID);
        appendNumber(json, "fid", WHATSAPP_PRODUCT_ID);
        appendNumber(json, "productId", WHATSAPP_PRODUCT_ID);
        appendNumber(json, "parentId", WHATSAPP_AI_KEFU_MENU_ID);
        appendString(json, "code", "REC_WHATSAPP_AI_KEFU_ROUTE");
        appendString(json, "name", "AI客服");
        appendString(json, "displayName", "AI客服");
        appendString(json, "icon", "whatsapp_menu_icon_9");
        appendString(json, "localCode", "/ingsale/aggregationKefu/index");
        appendString(json, "linkUrl", "JSinglepage:/ingsale/aggregationKefu/index");
        appendNumber(json, "webFlg", 1);
        appendNumber(json, "treeEndFlg", 1);
        appendNumber(json, "displayIndex", 1);
        appendNumber(json, "sort", 1);
        appendString(
                json,
                "evidence",
                "recovery-route-child:j2026-h-field-map:aggregation-kefu");
        json.append("\"status\":1");
        json.append('}');
    }

    private static String iconResourceName(String iconPath) {
        String icon = iconPath;
        int slash = icon.lastIndexOf('/');
        if (slash >= 0) {
            icon = icon.substring(slash + 1);
        }
        if (icon.endsWith(".svg")) {
            icon = icon.substring(0, icon.length() - 4);
        }
        return icon;
    }

    private static void appendString(StringBuilder json, String key, String value) {
        appendFieldPrefix(json, key);
        json.append('"').append(escape(value)).append('"').append(',');
    }

    private static void appendNumber(StringBuilder json, String key, int value) {
        appendFieldPrefix(json, key);
        json.append(value).append(',');
    }

    private static void appendFieldPrefix(StringBuilder json, String key) {
        json.append('"').append(key).append("\":");
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            switch (ch) {
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    escaped.append(ch);
                    break;
            }
        }
        return escaped.toString();
    }

    private static MenuSpec original(String code, String name, String icon) {
        return new MenuSpec(code, name, icon, "JSinglepage", "/pc/aicloud/my", "original-i18n");
    }

    private static MenuSpec recovered(String code, String name, String icon) {
        return new MenuSpec(code, name, icon, "JSinglepage", "/pc/aicloud/my", "recovery-value");
    }

    private static MenuSpec spiderRoute(String code, String name, String icon, String spiderCode) {
        return new MenuSpec(
                code,
                name,
                icon,
                "JSinglepage",
                "/pc/dataCollect/collectionTask?modal="
                        + spiderCode
                        + "&moduleCode=whatsapp",
                "recovery-route:dataCollect:" + spiderCode);
    }

    private static MenuSpec wsFilterRoute(String code, String name, String icon) {
        return new MenuSpec(
                code,
                name,
                icon,
                "JSinglepage",
                "/ws/wsfilter/home",
                "recovery-route:wsfilter-home");
    }

    private static MenuSpec kefuRoute(String code, String name, String icon) {
        return new MenuSpec(
                code,
                name,
                icon,
                "JSinglepage",
                "/ingsale/aggregationKefu/index",
                "recovery-route:aggregation-kefu");
    }

    private static MenuSpec oneLineRoute(String code, String name, String icon) {
        return new MenuSpec(
                code,
                name,
                icon,
                "JSinglepage",
                "/pc/aigc/aichat_dialog",
                "recovery-route:aichat-dialog");
    }

    private static final class ProductSpec {
        private final String code;
        private final String displayName;
        private final String primaryColor;
        private final String secondaryColor;
        private final boolean enterable;

        private ProductSpec(
                String code,
                String displayName,
                String primaryColor,
                String secondaryColor,
                boolean enterable) {
            this.code = code;
            this.displayName = displayName;
            this.primaryColor = primaryColor;
            this.secondaryColor = secondaryColor;
            this.enterable = enterable;
        }
    }

    private static final class MenuSpec {
        private final String code;
        private final String name;
        private final String icon;
        private final String localCode;
        private final String linkUrl;
        private final String evidence;

        private MenuSpec(
                String code,
                String name,
                String icon,
                String localCode,
                String linkUrl,
                String evidence) {
            this.code = code;
            this.name = name;
            this.icon = icon;
            this.localCode = localCode;
            this.linkUrl = linkUrl;
            this.evidence = evidence;
        }
    }
}
