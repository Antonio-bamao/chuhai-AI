# M5C 菜单可行性地图

范围：本轮只测绘用户指定菜单，不包含已闭环的 `AI采集`。本文件复用 `.context/m4-product-menu-evidence.md`、`.context/m5a-menu-route-discovery.md`、`.context/m5a-business-dependency-inventory.md` 和 M5B 证据；未改 `.cnf`、`cloud.spider.b`、`libmytrpc` 或业务代码。

档位定义：

- A：本地数据类，可先用本地 SQLite/本地桥接恢复。
- B：需要已登录 WhatsApp 账号。
- C：需要 LLM/AI 后端。
- D：需要原厂 SaaS 计费/授权或外部商业 API，恢复时应先桩掉。

外部强依赖编号：

- ① 登录态 WhatsApp 账号
- ② LLM/AI 端点
- ③ 原厂 SaaS 计费/授权
- ④ 第三方 API

## 菜单依赖地图

| 菜单 | 入口类/路由 | 数据来源 | 外部强依赖 | 可行性档位 | 恢复成本预估 |
|---|---|---|---|---|---|
| 一句话 | 当前是恢复占位 `REC_WHATSAPP_ONELINE`，菜单模型走 `com.sbf.main.tree.i`，点击进入 `com.sbf.main.sub.b.a(i)` / `com.sbf.main.sub.g.a(k,String)` 分发；现值仍是 `localCode=JSinglepage`、`linkUrl=/pc/aicloud/my`。M5A 只看到侧栏 callback，未看到真实 URL 加载或业务 JS 请求。 | 未发现本地 SQLite 表或可确认的 `SBFApi/postApis/getApis` 合约；从产品语义看可能是 AI 文案/一键生成页，但原始路由缺失。 | ②、③（推断）；当前无 ① 证据。 | D | 不能像 `AI采集` 那样只补本地桩。需先找回原始路由/前端包；若只是展示入口可桩空页，业务恢复至少需要 AI/授权桩。 |
| 智能体模型 | 当前是恢复占位 `REC_WHATSAPP_AGENT_MODEL`，仍落 `JSinglepage + /pc/aicloud/my`。候选原生入口包括 `ai_arm_box` / `ai_arm_box_new` 分支，加载 `JAIARMBoxFrame`、`JAIARMBoxPanelFrame`、`armbox` 上传/安装 APK 相关类，但未证明与该菜单一一对应。 | 未发现该菜单的本地 SQLite 表。候选数据来自智能体/ARM Box 设备配置、安装包上传、云任务或模型配置。 | ②、③、④（智能体设备/云端/安装包生态）。 | C | 不属于本地数据补桩；至少需要智能体模型配置服务或设备桥接。可先做静态/只读模型列表桩，完整恢复成本高。 |
| AI龙虾 | 当前是恢复占位 `REC_WHATSAPP_CLAW`，仍落 `JSinglepage + /pc/aicloud/my`；未在已读证据中找到稳定原始 Java localCode、业务路由或页面包。 | 未发现本地 SQLite 表或明确远端 API 合约。 | ②、③（推断，取决于原产品定义）。 | D | 先恢复入口没有排期价值；需继续找原始菜单 JSON/前端 chunk。短期只能桩说明页，不建议作为首批恢复。 |
| 超级号 | 当前是恢复占位 `REC_WHATSAPP_SUPER`，仍落 `JSinglepage + /pc/aicloud/my`。候选线索包括 `PhoneFission`、`superwhatsapp`、`ws_super_niayonhao` 以及群发/耐发号相关文案，但未证明为该菜单真实路由。 | 可能涉及账号池、号码/耐发号资源、群发任务队列；候选接口包含 `MiJava$52` 中 `/api/v1/superwhatsapp/getvcf/`，但不是菜单直证。 | ①、③、④；若含 AI 养号/文案则还需 ②。 | D | 不像 `AI采集` 的本地结果页。需要账号资源/任务服务/授权桩，短期只能做入口占位或资源列表假数据。 |
| AI数据 | 原始菜单码 `C4749_007`，父菜单应保持 `JSinglepage + /pc/aicloud/my`。实测 `/pc/aicloud/my` 加载前端 chunk `.artifacts/working/m5-online-js/chunk-dea9eb98.0b47177e.js`，组件名 `MnqAuthAccounts`；j2026 运行时需恢复值子路由 `REC_WHATSAPP_AI_DATA_ROUTE` 才能触发内容分发，但该子路由也必须打开 `/pc/aicloud/my`，不能指向 dataCollect。 | HTTP 接口：`/prod-api/mnq/mnqAuthAccounts/mylist?pageNum=1&pageSize=10`，期望 `{rows,total}`，行字段为 `id/authCode/passwd/number/machineCode/status/remark`；字典接口 `/prod-api/system/dict/data/type/yes_no_1_0`，期望启用/禁用字典数组。不是 `MiJava.getSpiderDataList`，不读 `spider_data`。 | ③ 原厂 SaaS/后端数据库用于真实授权码/机器码数据；首屏空态可本地桩。 | D | 已纠偏：UI 可用合理空态恢复，但真实数据源不是 AI采集本地表。不能复用 `spider_data`，除非未来找到组件/接口层面的同源证据。 |
| AI筛选 | 当前原始菜单码 `C4749_009`，恢复值仍是 `JSinglepage + /pc/aicloud/my`。候选真实入口是 `localCode=WSFilter` / `WapFilter` 分支，加载 `com.sbf.main.ext.ws.d`，页面/分组路由含 `/ws/wsfilter/group_index`，UI 文案为 “WhatsApp筛号AI系统”。 | 本地侧有 `JWSFriends` 等 WS 筛选面板实体；可能还有筛选任务队列。远端/第三方线索包括 `sms.foncao.com/filter/task`、`xdx_zw_whatasapp_filter_task_queue*`。 | ①、④；云账号/任务下发可能需要 ③。 | B | 可以先恢复本地 UI、导入号码、结果表等轻量路径；真实“是否开通/头像/活跃”等筛号必须有 WA 登录和任务执行链，成本中等偏高。 |
| AI群发 | 当前原始菜单码 `C4749_005`，恢复值仍是 `JSinglepage + /pc/aicloud/my`。候选执行入口是 `com.sbf.main.jxbrowser.MiJava$52` 的群发任务桥，任务类型含 `WhatsappMassSending`、`VideoCall`、`JoinGroup`、`ImportAddressBook` 等，并进入 `WaitSendMessages2` 队列；发送侧还涉及 `ADBrowser` 对 `web.whatsapp.com` 的 DOM 操作。 | JS 任务 payload：`taskSeq/contentType/contentTxt/sender/destType/alldatas/...`；本地等待发送队列 `WaitSendMessages2`；导通讯录候选接口 `/api/v1/superwhatsapp/getvcf/`。未发现可单独支撑菜单的纯本地 SQLite 表。 | ①、③、④；AI 文案群发时还需 ②。 | D | 不建议按本地桩直接恢复执行。需要发送队列、登录态检测、频控/风控、授权桩；可先做只读任务列表或禁用发送按钮。 |
| API | 当前菜单码在恢复目录中是 `C4749_`，疑似不完整，仍落 `JSinglepage + /pc/aicloud/my`。候选入口来自 `com.sbf.main.kefu.e` 平台注册中的 `wapapi` / “WhatsApp API”、`viberapi`，以及 Google Voice API 调用代码。 | 第三方/原厂 API 凭据、cookie/token 或原 SaaS 网关；候选接口包括 `/api/v1/superwhatsapp/getvcf/`、Google Voice `clients6.google.com/voice/.../sendsms`。无本地 SQLite 主数据证据。 | ③、④；部分 WhatsApp API 场景还需 ①。 | D | 需要 API 网关、凭据、配额和授权模型；本地只能做文档/配置页桩，不能作为首批功能恢复。 |
| 广告 | 当前原始菜单码 `C3460_001`，恢复值仍是 `JSinglepage + /pc/aicloud/my`。候选真实入口包括 `com.sbf.main.theme.ad.f`，路由 `/views/overseasAds/dataBoard`，以及 `ADSHelper` / `ADBrowser`。 | AdsPower 本地 API：`http://local.adspower.net:50325`、`/api/v2/browser-profile/create`；原厂回调：`/prod-api/api/v1/ads/autoTask/callback`；还涉及广告任务队列、账号余额、投放看板。 | ①（WhatsApp 云链/客服链路时）、③、④（AdsPower/广告平台/WhatsApp/Facebook 等）。 | D | 高成本。需要 AdsPower 环境、广告投放服务、回调、余额/授权桩；短期只适合桩 dashboard 和余额展示。 |
| AI客服 | 当前原始菜单码 `C4749_011`，恢复值仍是 `JSinglepage + /pc/aicloud/my`。候选入口包括产品级 `wskefu -> kefu`、`com.sbf.main.kefu.n2025.c`、`n2026.*`、`WSKEFU`，底层打开 `https://web.whatsapp.com` 并提供 “AI客服库/AI推荐答案”。 | WhatsApp 浏览器会话、本地/远端客服队列、`xdx_zw_aicoze_task_queue*`、AI客服模型库。无单一可复用本地 SQLite 结果表证据。 | ①、②、③、④。 | C | 手动客服壳可按 B 处理，但菜单名中的 AI 推荐/自动回复需要 AI 后端和队列服务；完整恢复成本高，先做只读会话/推荐答案桩更稳。 |
| AI大脑 | 入口在 `AIFloatingAssistant`、`JBrowserFrame2026`、`JAiBotDialog`；加载 `https://app.xdxsoft.com/pc/aigc/aichat_dialog` 或 `/pc/aigc/aichat_dialog`。M5A 首屏只观察到 `/prod-api/system/user/profile`、`/prod-api/ads/inivitationCode/balance` 等请求，未发送真实对话。 | 远端 AIGC 对话页、用户 profile、余额/邀请码接口；真实 chat endpoint 尚未在现有证据中捕获。 | ②、③、④；无 ① 证据。 | C | 首屏可用本地 response 桩撑开；真实聊天需要模型服务、会话存储、余额/计费授权。不是本地 SQLite 数据类。 |
| 充值 | 不属于当前 WhatsApp 侧栏恢复目录中的一个稳定子菜单，但支付/充值入口在主程序存在：`JSBFMain` 中 `/pc/alipay/enterpriseAuth`、`/pc/alipay/personal/auth`、`/pc/userPayofflineOrder/my`，以及 `theme.ad.f` 的 `广告充值/pay_money/帐号余额`。外部支付页含 `https://ws.wandange.com/xpay/order/offlinepay/transform`、Alipay cashier/QR 查询。 | 原厂订单/支付后端、Alipay/xpay、余额查询；未发现本地 SQLite 可替代账本。 | ③、④。 | D | 不应恢复真实支付。后续只建议做余额/充值入口禁用桩或离线说明页；真实支付需要合规支付服务和回调验签。 |

## A 档可立即开做

当前没有新的 A 档菜单。`AI数据` 已纠偏为 `MnqAuthAccounts` 授权码/机器码页，不是 `AI采集` 的本地 `spider_data` 结果视图。

## 排期建议

- 第一批：`AI数据` 已纠偏完成。目标不是本地结果页，而是恢复 `/pc/aicloud/my` 的 `MnqAuthAccounts` 原组件，并用 `mylist/dict` 本地空态桩保证 UI 可打开。
- 第二批：`AI筛选`。先做本地 UI 与导入/结果查看；真实筛号另排 WA 登录和任务执行链。
- 第三批：`AI客服`、`AI大脑`。先做首屏和只读桩，再接 AI 后端。
- 延后/先桩掉：`一句话`、`智能体模型`、`AI龙虾`、`超级号`、`AI群发`、`API`、`广告`、`充值`。这些目前缺原始路由或强依赖授权/第三方服务，不适合和 `AI采集` 一样只补本地桩。
