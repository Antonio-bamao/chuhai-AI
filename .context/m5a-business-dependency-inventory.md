# M5A 业务依赖分类清单

> 目标：在不扩大本地拦截、不伪造通用 `/prod-api/*` 的前提下，先把业务能力按依赖类型分层。这个文档是 M5A 第一版静态分类，后续每个菜单的真实点击、抓包和日志会在同一表上把“待验证”收敛为“可直连保留”或“需重建兼容后端”。

## 1. 当前结论

- M4B 已让授权、登录、产品、菜单和 Web 首屏门槛本地通过，但这不等于采集、群发、云手机、投屏、视频等业务动作已经恢复。
- 直接连接第三方平台或本机 native 工具的功能，优先保留原客户端逻辑，不应改成离线假数据。
- 依赖原 `app.xdxsoft.com/prod-api` 数据库、任务队列、云设备池、OSS 或支付/订单的功能，需要按接口逐项重建兼容后端。
- 当前第一版以静态证据分类，不执行真实群发、采集写入、支付、上传或云设备调度等有副作用动作。

## 2. 证据来源

- 反编译包结构：`.artifacts/decompiled/cfr-app-20260620-0215-annotated/com/sbf/main/` 下存在 `spide`、`rpa`、`kefu`、`mnq`、`cloud`、`ext`、`jxbrowser`、`video`、`im`、`mq`、`proxy`、`ads`、`ocx` 等业务包。
- native/工具资产：`data/app/adb/adb.exe`、`data/lib/ffmpeg.exe`、`data/lib/scrcpy-server.jar`、`data/tools/vecore/*.exe`、`data/tools/vecore/*.dll`、`data/lib/jxbrowser-win64-7.41.3.jar`、`data/lib/selenium-*.jar`、`data/lib/playwright-1.46.0.jar`、`data/lib/javacv/opencv/ffmpeg` 相关库。
- 已验证服务端接缝：`.context/seams.md` 记录 `/getInfo`、`/system/function_module/listmy/41`、`/api/v1/client/pc/menus`、`/prod-api/getInfo`、`/prod-api/getRouters`、`/prod-api/mnq/mnqAuthAccounts/mylist`、`/prod-api/system/dict/data/type/yes_no_1_0`。
- 既有运行证据：`C:\m2dump\m5-v33-host.log`、`C:\m2dump\m4-v40-host.log`、`C:\m2dump\m4-v40-offline-proxy.log` 已证明 Web 首屏和产品/菜单可进入；尚未证明真实业务动作。
- 字符串证据：`.context/string-map-sample-verification.md` 已确认 `https://trans.siyutui.com/bigdata/dpool`、`https://zhiwen.888005.xyz` 等外部服务入口；`.context/string-decode-report.md` 记录 `StartApp$5` 截图上传 OSS 和状态上报链路。

## 3. 依赖类型定义

| 类型 | 含义 | M5 处理方式 |
| --- | --- | --- |
| 客户端直连第三方 | 客户端通过浏览器、WebDriver、ADB、HTTP 或平台页面直接访问 WhatsApp/TikTok/Facebook/Google 等外部平台 | 保留原逻辑，做低风险只读/登录态验证 |
| 原后端代理 | 客户端请求原 `prod-api` 或 Java API，由服务端代发、签名、聚合或转发第三方 | 记录契约；失效后进入 M5B 兼容后端 |
| 原后端数据库 | 账号、任务、授权码、联系人、结果表、字典、路由等 CRUD 依赖原数据库 | 需要本地或外置兼容存储 |
| 云资源/算力 | 云手机池、云浏览器、OSS、队列、视频/AI 算力、远程任务执行 | 无法只靠客户端恢复；需逐项替换资源或重建服务 |
| native 依赖 | ADB、scrcpy、FFmpeg、VECore、OpenCV、JxBrowser、Selenium、Playwright 等本机工具/库 | 优先保留，验证本机可用性和路径 |

## 4. 模块分类矩阵

| 模块/能力 | 当前分类 | 证据 | 初步判断 | M5A 下一步 |
| --- | --- | --- | --- | --- |
| 登录、授权、产品门槛 | 原后端数据库 → 已本地兼容 | `/getInfo`、`/system/function_module/listmy/41`、`/api/v1/client/pc/menus` 已在 M4B 本地化 | 启动门槛可本地恢复，不代表业务数据可用 | 保持冻结，不再猜远端 token |
| Web 首屏 `/pc/aicloud/my` | 原后端数据库 + Web 前端 | `getInfo/getRouters/mylist/dict` 由定点 JS hook 补形状；`C:\m2dump\m5-v33-host.log` 显示 `chunk-dea9eb98` 加载和四个定点 XHR | 首屏能显示空表；真实增删改查仍依赖后端 | 已完成只读首屏验证；继续只记录页面请求，不泛化拦截 |
| WhatsApp 采集/筛选/群发/API/客服 | 客户端直连第三方 + 原后端任务/数据库 + IM | 菜单已恢复；包内有 `kefu`、`msg`、`im`、`mq`，也有浏览器/自动化依赖；但 v40 菜单当前统一 `JSinglepage + /pc/aicloud/my` | 当前只能验证 WhatsApp 外壳和菜单，不能证明 WhatsApp 真实业务入口已打开；采集/登录可能可直连，任务/联系人/群发状态和客服会话大概率需后端/本地库 | 先恢复或定位 WhatsApp 菜单真实 `localCode/linkUrl`，再只读打开账号/列表页；不执行群发 |
| TikTok/FB/Instagram/X/TG 采集类 | 客户端直连第三方 + 浏览器自动化 + 可能后端存储 | 平台资源、菜单、`jxbrowser`、`selenium`、`playwright`、`proxy` 包存在 | 平台页面访问可保留；批量任务和结果保存需验证后端依赖 | 逐平台做只读打开和网络日志分类 |
| GEO/全球号码/海关数据/企业大数据 | 原后端代理 + 外部数据服务 | 已确认 `https://trans.siyutui.com/bigdata/dpool`；GEO 菜单含全球号码/地区/企业大数据；但 v40 菜单当前统一 `JSinglepage + /pc/aicloud/my` | 高概率依赖数据服务和服务端配额/查询代理；当前还没有真实 GEO 页面入口可点 | 优先定位 GEO 菜单真实入口和请求契约，不承诺客户端单独恢复 |
| AiCloud/AdsPower 指纹 | 第三方/本机服务 + 原后端数据库 | 菜单含 AiCloud/AdsPower 指纹；首屏已见 `/prod-api/mnq/mnqAuthAccounts/mylist` | 本机或第三方浏览器能力可保留；账号列表/授权码依赖后端存储 | 先验证本地浏览器/指纹工具是否可启动 |
| 云手机/模拟器/群控 | native + 云资源/设备池 + 原后端任务 | `mnq`、`ext/cloud`、`JCloudMobile*`、`JMNQ*`、`adb.exe`、`scrcpy-server.jar`；`adb.exe version` 可执行 | 本地 ADB/scrcpy 能力可保留；云设备池无法只靠客户端恢复 | 分离“本地设备连接”和“云设备调度”；后续只读执行 `adb devices`，不连接/创建云设备 |
| 投屏/RTMP/视频处理 | native + 可能 OSS/上传 | `ext/rtmp`、`VideoStreamingServer`、`video`、`ffmpeg.exe`、`vecore`、OpenCV/JavaCV 资源；`ffmpeg.exe -version` 可执行 | 本地转码/剪辑/投屏组件可能可用；上传/云合成依赖服务 | 已确认 FFmpeg 可执行；暂不启动 VECore 服务进程，先只记录文件与版本 |
| OSS 截图/状态上报/版本更新 | 云资源 + 原后端上报 | `StartApp$5` 截图上传 OSS；v8 曾遇到 `gqkoss/qqkoss.oss-cn-hangzhou.aliyuncs.com` | 非核心业务，离线/断网时应 fail-open 或关闭噪音 | 保持不阻断主业务，必要时降级 |
| 支付、审核、套餐、企业认证 | 原后端数据库 + 支付服务 | `seams.md` 已记录支付宝、离线订单和 xpay 路径 | 已被产品目标排除为门槛，不作为恢复前提 | 保持绕过，不做支付恢复 |
| IM/消息/ActiveMQ/UDP | 原后端消息服务 + 本地 MQ | `data/lib/activemq-all`、`im.ip/im.port.udp`、`mq` 包；v33 修复 IM 配置形状 | 本地启动可过；真实客服/群发状态同步需消息服务 | 记录端口与 topic，再决定本地 MQ 兼容 |

## 5. 八系统第一版风险排序

| 优先级 | 系统 | 为什么先/后 |
| --- | --- | --- |
| P1 | WhatsApp | 用户验收截图最明确，菜单最多；但群发有副作用，必须只读打开、禁止真实发送。 |
| P1 | AiCloud/指纹首屏 | 已有 v33/v40 证据和具体 `/prod-api/mnq/...` 接口，可最快闭合“后端数据库 vs 本机工具”。 |
| P2 | GEO | 数据服务/后端代理风险高，越早抓契约越能判断 M5B 工作量。 |
| P2 | TikTok/Facebook/Instagram/X/TG | 多数可能走浏览器自动化；先验证能否打开平台和代理/指纹入口。 |
| P3 | 云手机/投屏/视频 | native 资产多，云资源不确定；需要更谨慎地拆本地能力和云调度。 |
| P3 | 客服/IM/群发执行 | 容易触发外发消息或任务写入，必须在只读契约清楚后再碰。 |

## 6. 下一轮验收规则

1. 只做只读/低副作用动作：打开页面、加载列表、读取空表、捕获网络请求和日志。
2. 不执行真实发送、批量采集、批量关注、付费、上传或云设备创建。
3. 每个菜单记录三件事：入口是否打开、请求去了哪里、失败是 401/403/空体/缺本机 native/缺云资源。
4. 能直连第三方的保持原客户端逻辑；只有明确依赖遗失后端的接口进入 M5B。
5. 继续避免通用 `/prod-api/*` 拦截；每个兼容响应必须由具体页面 chunk、Java 调用点或运行日志证明。

## 7. 2026-06-23 第一轮只读验收记录

| 验收项 | 动作 | 结果 | 分类影响 |
| --- | --- | --- | --- |
| v40 菜单入口能力 | 核对 `M4RecoveryCatalog.appendMenu` 与测试契约 | 76 个菜单当前统一为 `localCode=JSinglepage`、`linkUrl=/pc/aicloud/my` | WhatsApp/GEO 当前不能直接做真实业务页面验收；需先定位或恢复各菜单真实入口 |
| AiCloud 首屏 | 复核 `C:\m2dump\m5-v33-host.log` / `m5-v27-host.log` | 真实 URL `https://app.xdxsoft.com/pc/aicloud/my?...` 加载；`/prod-api/getInfo`、`getRouters`、`yes_no_1_0`、`mnqAuthAccounts/mylist` 被定点接住；`chunk-dea9eb98` 加载成功 | 首屏和空表可用；账号列表/授权码数据仍属于原后端数据库 |
| WhatsApp | 核对菜单与 i18n/源码关键词 | 外壳和 11 个菜单已显示，但真实业务入口仍是保守 `/pc/aicloud/my`；字典存在 WhatsApp API、群发、接粉、拉群、客服等大量业务文案 | 暂不能声明 WhatsApp 业务可用；下一步是路由发现，不是群发测试 |
| GEO/大数据 | 核对 GEO 菜单与字符串证据 | 已有 `https://trans.siyutui.com/bigdata/dpool` 外部数据服务证据和海关/全球企业/全球号码文案；但 v40 无真实 GEO 页面入口 | 高概率后端/外部服务依赖；先抓契约再决定 M5B |
| ADB | 运行 `data\app\adb\adb.exe version` | 输出 `Android Debug Bridge version 1.0.36` | 本地 Android/设备控制基础工具可执行；云设备池未验证 |
| FFmpeg | 运行 `data\lib\ffmpeg.exe -version` | 输出 `ffmpeg version N-87130-g2b9fd15` | 本地视频处理基础工具可执行；VECore/上传/云合成未验证 |
| 浏览器自动化依赖 | 列出 JxBrowser/Selenium/Playwright JAR | `jxbrowser-7.41.3`、`jxbrowser-win64`、10 个 Selenium 3.141.59 JAR、`playwright-1.46.0.jar` 存在 | 浏览器自动化依赖在本地；平台登录/采集动作仍需只读验证 |

第一轮结论：

- 现在可以确认“本地工具和 AiCloud 首屏”具备继续验收基础。
- 现在不能把 WhatsApp/GEO 菜单点击当作真实业务验收，因为 M4A 为保护主界面统一使用了保守入口 `/pc/aicloud/my`。
- M5A 的下一步应是“真实业务入口恢复/路由发现”：从原始菜单响应残留、反编译分发代码和 i18n key 反查每个高优先菜单的 `localCode/linkUrl`，先恢复只读页面入口，再做低风险联网验证。

## 8. 2026-06-23 菜单路由发现记录

详见 `.context/m5a-menu-route-discovery.md`。

| 发现项 | 证据 | 结论 | 对 M5A 的影响 |
| --- | --- | --- | --- |
| 菜单模型字段 | `com.sbf.main.tree.i` 与 `string_map.json` | 菜单项读取 `localCode`、`code`、`name`、`icon`、`linkUrl`、`webFlg` 等字段 | 后续恢复真实入口必须围绕 `localCode/linkUrl`，不能只看显示 code |
| 主侧边栏点击分发器 | `com.sbf.main.sub.b.a(i)` 与 `string_map.json` | 已解出 `ZWBrowser`、`AiBotChat`、`ai_mnq_manager`、`ai_arm_box`、`TkSpiderPanel`、`JBigDataMaster`、`JRealAndroidMaster`、`PhoneFission`、`JSinglepage` 等打开器 | 证明原客户端存在多类业务入口；v40 统一 AiCloud 是保守恢复值，不是最终业务路由 |
| 产品级入口 | `JSBFMain` 解码值 | `wskefu` 会映射到 `kefu`；还存在 `getcustomer`、`rpa`、`aicloud`、`rpamarkting` 等产品/命令级入口 | 产品 code 不等于菜单路由；WhatsApp/GEO 需要独立恢复侧边栏入口 |
| WhatsApp/GEO 具体 code | 全局检索 `C4749_*`、`C4134_*`、`C4137_*` | 目前只在恢复目录和证据文档中出现，未发现原始 `localCode/linkUrl` 残留 | 暂不修改 v40 菜单；继续从 Web chunk/资源/缓存找可证明 URL |

本轮边界：

- 已确认为什么 v40 能显示菜单却不能用于真实业务验收。
- 已确认下一步应优先找真实 URL/打开器证据，而不是直接开始群发、采集或 GEO 查询。
- 没有把 `JBigDataMaster`、`ZWBrowser`、`PhoneFission` 等打开器强行绑定到未知菜单。

## 9. 2026-06-23 本地 spider 脚本与 dataCollect 入口发现

详见 `.context/m5a-menu-route-discovery.md` 第 7 节。

| 发现项 | 证据 | 依赖分类影响 | 下一步 |
| --- | --- | --- | --- |
| 本地 spider 配置 | `data/app/res/spider/*.cnf` 存在 WhatsApp、TikTok、Facebook 等 21 个明文采集配置 | 多平台采集不是完全服务端黑盒；至少保留了本地脚本/浏览器自动化资产 | 先按配置名梳理菜单候选，不直接执行采集 |
| WhatsApp 采集配置 | `whatsapp_users_lists`、`whatsapp_regional_collection`、`whatsapp_group_lists`、`wap_global_clue_users` 均含 `moduleCode=whatsapp` 与 Google/平台搜索逻辑 | WhatsApp 采集可细分为客户端直连第三方 + 本地脚本自动化 + 原后端任务/结果存储混合 | 优先恢复只读任务页入口，记录请求和表单，不提交任务 |
| TikTok/Facebook 配置 | TikTok 配置指向 `www.tiktok.com` 页面/API；Facebook 配置指向 `facebook.com` 页面和 GraphQL/API | 平台采集有直连第三方证据，但批量任务、代理、账号态和结果保存需继续分层 | 后续逐平台只读打开，禁止关注/私信/批量动作 |
| `JSpiderCloude` 数据采集页 | 反编译代码构造 `/pc/dataCollect/collectionTask/data_index?spiderCode=...&moduleCode=...` | 这是高置信数据采集入口族，适合后续作为恢复值候选 | 先用测试集中定义候选恢复值，再做页面打开验收 |
| `SBFApi.H(String)` 本地兜底 | 先请求 `/cloud/spider/code/<code>`，失败后读 `/res/spider/<code>.cnf` | 兼容后端不一定要推倒重建全部采集脚本；可优先利用本地缓存配置 | M5B 若重建 spider 配置接口，应保持远端配置与本地兜底形状兼容 |
| spider v2 任务接口 | `/api/v1/client/pc/spider/v2/upstatus/get/cancelAllRun/getNewTask` | 任务状态、任务拉取、取消和结果处理仍高概率依赖原后端任务队列/数据库 | M5A 只记录契约；M5B 再决定本地队列/兼容存储 |

阶段结论：

- 采集类业务不应简单归为“客户端全丢”或“后端全丢”。当前更准确的拆法是：页面/脚本/第三方访问可部分保留，任务编排、持久化、OSS、验证码/AI 辅助和队列需要逐项验证。
- 下一步可围绕 WhatsApp `AI采集/AI数据/AI筛选` 建立高置信只读入口候选，但必须把 `spiderCode/moduleCode/localCode/linkUrl` 中无法证明的值标注为“恢复值”。

## 10. 2026-06-23 v41 WhatsApp AI采集只读候选

| 项 | 结果 |
| --- | --- |
| 覆盖范围 | 仅 WhatsApp `C4749_006 / AI采集`。 |
| 恢复入口 | `localCode=pc/dataCollect/collectionTask`，`linkUrl=/pc/dataCollect/collectionTask/data_index?spiderCode=whatsapp_users_lists&moduleCode=whatsapp`。 |
| 证据属性 | `recovery-route:dataCollect:whatsapp_users_lists`，明确是恢复值，不是原始真实菜单响应值。 |
| 测试 | 新增测试先红后绿；完整 `python -m unittest discover -s tests -v` 通过 `27/27`。 |
| 候选产物 | `.artifacts/working/m5a-whatsapp-collect-route-v41/App-m5a-v41-whatsapp-collect-route.jar`，SHA-256 `661AD0474127637FF3890DB61B95A6EAE66D09DA41C82C217BD334E3C5FA10FE`。 |

分类影响：

- WhatsApp `AI采集` 现在具备一个可点击验证的高置信候选入口，下一步可以从“菜单壳”进入“页面打开/请求分类”。
- 这一步仍不证明采集任务提交、任务队列、结果保存、OSS 上传或验证码/代理辅助已恢复。
- `AI数据`、`AI筛选` 暂不绑定 spider 配置，避免把结果页/筛选器误配成采集脚本。

## 11. 2026-06-23 v41 WhatsApp AI采集宿主只读验收

| 验收项 | 动作 | 结果 | 分类影响 |
| --- | --- | --- | --- |
| 启动方式 | 在项目内 `data/app` 工作目录下，用项目自带 Java 8 直接启动 v41 候选 JAR；未覆盖 `data/app/App.dll`，未触碰桌面原始安装包 | 自动进入九产品选择器，`data/app/App.dll` 保持 SHA-256 `9084FABCE357AAD8B18D06D0FB708DE4E92E1B5D63686CEA1DED49E19F73A99B` | 本次证据属于项目内 JAR 直接启动，不是最终双击包验收 |
| 进入 WhatsApp | 点击 WhatsApp `进入系统` | 主界面显示 WhatsApp 11 项侧边栏，`AI采集` 菜单可见 | WhatsApp 外壳和菜单仍可进入 |
| 点击 `AI采集` | 只点击左侧 `AI采集`，不输入关键词、不创建任务、不上传、不群发 | 菜单高亮到 `AI采集`，但右侧内容区域保持空白；没有出现采集任务页、表单、表格或错误提示 | v41 只证明菜单字段可下发，尚不能证明页面层恢复 |
| 日志筛查 | 过滤 `stdout.log` 中的 `dataCollect/collectionTask/whatsapp_users_lists/M4_V13_LOAD_URL/M4_V18_NORMALIZED_URL/M5_V26_WEB_BOOTSTRAP_XHR/getNewTask/upstatus/cancelAllRun/submit/save` | `dataCollect=6`、`collectionTask=4`、`whatsapp_users_lists=4` 均只来自菜单 JSON；`LoadUrl=0`、`Normalized=0`、`BootstrapXHR=0`、`getNewTask/upstatus/cancelAllRun/submit/save=0` | 没有触发 JxBrowser 导航、Web XHR 或 spider 任务接口；未产生副作用请求 |
| 测试产物 | 截图和日志保存在 `.artifacts/runtime/m5a-v41-host-readonly/` | `screen-03-whatsapp-main-entered.png` 显示 WhatsApp 主界面；`screen-04-ai-collect-after-click.png` 显示 `AI采集` 高亮但右侧空白；`filtered-evidence.log` 只含菜单 JSON 证据 | 后续可复核 UI 状态和日志计数；`.artifacts` 仍为本地忽略产物 |

本轮结论：

- v41 的 `localCode=pc/dataCollect/collectionTask` 与 `linkUrl=/pc/dataCollect/collectionTask/data_index?...` 进入了运行时菜单 JSON，但当前主侧边栏点击后没有走到已验证的 JxBrowser 加载边界。
- 该候选不能作为“WhatsApp AI采集页面已恢复”的验收结果，只能作为“菜单字段恢复值已下发”的证据。
- 下一步应优先定位 `pc/dataCollect/collectionTask` 在 `sub.g`、`f.d` 或 `JSBFMain$4` 中的真实分发前置条件，或改用能触发普通 Web 加载器的候选组合继续只读验证。

## 12. 2026-06-23 v42 WhatsApp AI采集 JSinglepage 候选只读验收

| 验收项 | 动作 | 结果 | 分类影响 |
| --- | --- | --- | --- |
| 候选入口 | 将 `C4749_006 / AI采集` 改为 `localCode=JSinglepage`、`linkUrl=/pc/dataCollect/collectionTask/data_index?spiderCode=whatsapp_users_lists&moduleCode=whatsapp` | 测试先红后绿；v42 JAR SHA-256 `97FBC8CE920E38851A9BB2534E05C9E3B797C7D3A8DBDCA7ACAF3D71E271E` | 这是恢复值候选，不是原始真实菜单响应值 |
| 宿主启动 | 在项目内 `data/app` 工作目录直接运行 v42 JAR；未覆盖 `data/app/App.dll`，未触碰桌面原始安装包 | 进入 WhatsApp 后 `AI采集` 可见；`data/app/App.dll` 哈希保持 `9084FABCE357AAD8B18D06D0FB708DE4E92E1B5D63686CEA1DED49E19F73A99B` | 仍是 JAR 直接启动只读证据，不是最终双击包验收 |
| 点击 `AI采集` | 单击与双击复测，仅点击菜单，不输入关键词、不创建任务、不上传、不群发 | 左侧 `AI采集` 高亮，右侧仍为空白；无页面首屏 | 仍不能开始采集页面请求分类 |
| 日志筛查 | 过滤 `M4_DIAG_*`、`M4_V13_LOAD_URL`、`M4_V18_NORMALIZED_URL`、`M5_V26_WEB_BOOTSTRAP_XHR` 和 spider 任务接口 | `M4_DIAG_DISPATCH_ENTER=0`、`M4_DIAG_MODERN_DISPATCH_ENTER=0`、`LoadUrl=0`、`Normalized=0`、`BootstrapXHR=0`、`getNewTask/upstatus/cancelAllRun/submit/save=0` | 未进入内容创建分发器，且无副作用请求 |
| 测试产物 | `.artifacts/runtime/m5a-v42-host-readonly/` 与 `.artifacts/runtime/m5a-v42-host-readonly-doubleclick/` | `stderr.log` 均为 0 字节；截图显示同一空白右侧状态 | 可靠负结果，可用于下一轮定位 |

本轮结论：

- v42 排除了“只要把 dataCollect 路由放到 JSinglepage 就能打开页面”的假设。
- 当前阻断不是 Web 页面加载失败，而是左侧菜单点击后没有进入已插桩的内容创建分发器。
- 下一步应先给 `com.sbf.main.ext.j2026.h$2.mouseClicked()`、`h.a(null)` 回调和 `treeEndFlg/children` 条件加诊断，再决定是否调整菜单树结构或回调接线。

## 13. 2026-06-24 v43-v47 WhatsApp AI采集页面层只读验收

| 阶段 | 动作 | 结果 | 分类影响 |
| --- | --- | --- | --- |
| v43 | 插桩 `com.sbf.main.ext.j2026.h$2` | 点击 `AI采集` 时 `M5A_V43_MENU_MOUSE_CLICKED=0` | 排除 `h$2` 作为 WhatsApp 侧边栏真实点击处理器 |
| v44 | 插桩 `com.sbf.main.ext.j2026.d$2` 与 `d$1` | `MOUSE_CLICKED=1`、`SELECT_CALL=1`、`CALLBACK=2`，但 `M4_V12_DISPATCH=0` | 点击链成立，阻断在父子菜单分发语义 |
| v45 | 给 `C4749_006` 增加恢复值子路由 `REC_WHATSAPP_COLLECT_USERS_ROUTE` | `M4_V12_DISPATCH=1`，但 `M4_V12_NEW_JXBROWSER=0` | 子路由语义正确，字段映射仍需修正 |
| v46 | 修正 j2026 字段映射 | `M4_V12_NEW_JXBROWSER=1`，但最终加载 `JSinglepage?st=...` | JxBrowser 创建成功，URL 仍是占位值 |
| v47 | 增加仅限当前恢复子路由的 `JSinglepage` 归一化桥接 | 最终 URL 为 `https://app.xdxsoft.com/pc/dataCollect/collectionTask/data_index?spiderCode=whatsapp_users_lists&moduleCode=whatsapp`，主框架/静态资源 200，触发 `/prod-api/getInfo`、`/prod-api/getRouters`，控制台报 `mijava is not defined` | 页面层入口恢复；后续阻断转为 Web bridge/后端初始化依赖 |

v47 只读宿主证据：

| 检查点 | 结果 |
| --- | --- |
| 候选产物 | `.artifacts/working/m5a-v47-whatsapp-collect-jsinglepage-bridge/App-m5a-v47-whatsapp-collect-jsinglepage-bridge.jar`，SHA-256 `B80C10D0454D4A9983D53B92E0D07F2F1611B665B1D13B489713D07D6027333F` |
| 验证目录 | `.artifacts/runtime/m5a-v47-whatsapp-collect-jsinglepage-bridge-host-readonly/` |
| 启动方式 | 项目内 `data/app` 工作目录直接运行 v47 JAR；未覆盖 `data/app/App.dll`，未触碰桌面原始安装包 |
| 页面状态 | `screen-02-ai-collect-after-click.png` 和额外等待截图显示 `AI采集` 高亮、右侧 dataCollect 页面加载动画 |
| URL/请求 | `M4_V18_NORMALIZED_URL` 与 `M4_V13_LOAD_URL` 均为 `https://app.xdxsoft.com/pc/dataCollect/collectionTask/data_index?spiderCode=whatsapp_users_lists&moduleCode=whatsapp`；`M5_V20_WEB_REQUEST=13` |
| XHR/控制台 | `M5_V26_WEB_BOOTSTRAP_XHR=2`，分别为 `/prod-api/getInfo`、`/prod-api/getRouters`；控制台 `ReferenceError: mijava is not defined` |
| 副作用接口 | `getNewTask=0`、`upstatus=0`、`cancelAllRun=0`、`submit=0`、`save=0` |
| 环境恢复 | 停止 Java 进程后清理运行缓存；`data/app/App.dll` SHA-256 保持 `9084FABCE357AAD8B18D06D0FB708DE4E92E1B5D63686CEA1DED49E19F73A99B` |

分类结论：

- WhatsApp `AI采集` 入口已从菜单外壳推进到真实 Web 页面层，但仍未进入任务创建或执行链路。
- dataCollect 页面至少依赖 Web bridge `mijava` 和 `/prod-api/getInfo/getRouters` 初始化；后续可能还会依赖原后端任务列表、结果保存、OSS、代理/验证码/AI 辅助和 spider v2 队列。
- M5A 下一步应只读解析 dataCollect 页面 chunk 与 `mijava` 调用，逐项记录必要契约；若需本地兼容，进入 M5B 按具体接口重建，不允许直接伪造通用 `/prod-api/*`。

## 14. 2026-06-24 dataCollect 页面 chunk 与 MiJava bridge 契约

本轮只读解析 v47 运行时实际加载的 dataCollect 页面资源，不点击下载、清空、提交、保存或任何采集动作。页面资源下载到 `.artifacts/analysis/m5a-datacollect-page/`，仅作为本地分析缓存。

| 契约项 | 证据 | 结论 |
| --- | --- | --- |
| dataCollect 页面 chunk | `static/js/chunk-00b3289e.51ab7483.js`，组件名 `data_index` | 该页面从 `this.$route.query.spiderCode/moduleCode` 读取参数，v47 URL 中的 `whatsapp_users_lists` 与 `whatsapp` 已进入前端组件上下文。 |
| 首屏初始化 | `created -> initConfig()`，直接调用 `mijava.getCloudSpiderConfig(this.spiderCode, callback)` | 首屏不是先提交任务，而是先通过 Java bridge 取 spider 配置字段；缺少 `mijava` 会在页面加载阶段直接中断。 |
| 本地结果列表 | `getList()` 调用 `mijava.getSpiderDataList(moduleCode, spiderCode, pageNum, pageSize, callback)` | 页面默认读取本地已采集数据列表，期望返回形状含 `total` 和 `rows`，每行含 `jsonData`。 |
| 实时追加 | `mounted` 注册 `window.reloadData(json)`，将 `jsonData` 反序列化后插入表格 | Spider 运行期可能通过宿主回调把新数据推给页面，但 v47 只读验证未触发。 |
| 导出 | `toPackageDow()` 调用 `mijava.toPackageDowloadData(moduleCode, spiderCode, fields, callback)` | 导出会读取本地结果并生成文件，属于有副作用动作；M5A 只记录契约，不点击。 |
| 清空 | `toClearAll()` 确认后调用 `mijava.toClearDataAll(moduleCode, spiderCode, callback)` | 清空会删除本地结果，属于破坏性动作；M5A 禁止点击。 |
| `/prod-api/getInfo/getRouters` | v47 XHR 只捕获这两个 bootstrap 请求 | 它们是 RuoYi/Vue 全局鉴权和动态路由初始化，不是 dataCollect 结果列表或任务提交接口。 |
| 任务提交接口 | v47 日志 `getNewTask/upstatus/cancelAllRun/submit/save=0`；chunk 内未发现创建任务 API | 当前页面停在 bridge 初始化前，尚未进入 spider v2 任务队列或提交链路。 |

MiJava 方法反查：

| 方法 | Java 证据 | 行为分类 |
| --- | --- | --- |
| `getCloudSpiderConfig(String, JsFunction)` | `MiJava.java:5996`，`MiJava$160.run()` 取配置后把字符串传回 JS；方法签名由 `javap` 确认 | 配置读取。结合 `SBFApi.H(String)` 既有证据，配置源为远端 `/cloud/spider/code/<code>` 优先，本地 `/res/spider/<code>.cnf` 兜底。 |
| `getSpiderDataList(String,String,int,int,JsFunction)` | `MiJava.java:6006`，`MiJava$162` 使用 `DAOBase`、`WhereInfo`、`countOf`、`queryLimit`，返回 `total/rows` JSON | 本地结果列表读取，不是远端任务提交。 |
| `toClearDataAll(String,String,JsFunction)` | `MiJava.java:6016`，`MiJava$163` 调用 DAO `clearTableAll()` 后回调 | 本地结果清空，有副作用。 |
| `toPackageDowloadData(String,String,String,JsFunction)` | `MiJava.java:6022`，`MiJava$164` 使用 `JSpiderData`、`ExcelExportHelper`、`File`、分页读取本地结果并回调进度/文件路径 | 本地结果导出，有文件写入副作用。 |

分类结论：

- WhatsApp `AI采集` 当前卡住的直接原因不是“没有提交采集任务”或“任务接口失败”，而是 dataCollect 页面首屏需要宿主注入 `window.mijava`。
- dataCollect 首屏的最小契约至少包括：`mijava.getCloudSpiderConfig`、`mijava.getSpiderDataList`、`window.reloadData`；下载和清空属于后续操作，不应在 M5A 点击。
- `getInfo/getRouters` 仍可复用 v33/v40 的本地 bootstrap 形状，但不能把 `/prod-api/*` 扩成通用本地代理；每个新增接口都必须由页面 chunk 或运行日志证明。

## 15. 2026-06-24 v48-v49 dataCollect bridge 只读验证

本轮只围绕 v47 暴露出的 dataCollect 首屏依赖继续推进，不输入关键词、不创建采集任务、不导出、不清空、不上传、不群发。验证仍使用项目内 `data/app` 工作目录直接运行候选 JAR，未覆盖 `data/app/App.dll`，未触碰桌面原始安装包。

| 阶段 | 动作 | 结果 | 分类影响 |
| --- | --- | --- | --- |
| v48 | 在通用 `M5InjectJsCallback` 中注入真实 `com.sbf.main.jxbrowser.MiJava`，暴露为 `window.mijava` 与 `window.java` | `M5A_V48_MIJAVA_BRIDGE_INJECTED=1`，`mijava is not defined=0`；页面继续加载 `chunk-17c57094`，但控制台报 `Cannot read properties of undefined (reading 'some')` | `mijava` 缺口解决；新阻断转为 Web 权限状态契约 |
| v48 只读定位 | 解析 `app.988d65c1.js` 报错偏移 | `hasPermiOr -> i()` 内部读取 `store.getters.permissions.some(...)`；前端 Java bridge 分支调用 `window.mijava.getInfo(callback)` 后直接读取顶层 `i.user/i.roles/i.permissions` | 原 `MiJava.getInfo` 返回 `StartApp.m.toString()`，其形状来自启动链嵌套数据，不符合 Web bridge 分支扁平契约 |
| v49 | 仅 patch `MiJava.getInfo(JsFunction)`，返回顶层 `user/roles/permissions` JSON，并保留真实 `JsFunction.invoke(window, payload)` 调用方式 | 字节码含 `M5A_V49_MIJAVA_GET_INFO_BRIDGE_JSON`、`permissions`、`*:*:*`，且不再引用 `StartApp.m`；完整测试 `27/27 OK` | 只补 Web 权限初始化，不新增通用接口代理 |
| v49 宿主只读 | 直接运行 `.artifacts/working/m5a-v49-datacollect-bridge-getinfo/App-m5a-v49-datacollect-bridge-getinfo.jar`，进入 WhatsApp 后只点击 `AI采集` | 页面最终显示 `AI采集` 标签与“暂无数据”空表；`LEVEL_ERROR=0`、`Cannot read properties of undefined=0`、`mijava is not defined=0` | dataCollect 页面空表层恢复；仍未证明任务创建、任务执行、结果保存或上传 |

v49 关键证据：

| 检查点 | 结果 |
| --- | --- |
| 候选产物 | `.artifacts/working/m5a-v49-datacollect-bridge-getinfo/App-m5a-v49-datacollect-bridge-getinfo.jar`，SHA-256 `26694D706D8141EF8131891285A4ADAB02A0D7E6F70BBF509D27395220F652D0` |
| 验证目录 | `.artifacts/runtime/m5a-v49-datacollect-bridge-getinfo-host-readonly-rerun/` |
| URL | `M4_V18_NORMALIZED_URL=https://app.xdxsoft.com/pc/dataCollect/collectionTask/data_index?spiderCode=whatsapp_users_lists&moduleCode=whatsapp` |
| Bridge | `M5A_V48_MIJAVA_BRIDGE_INJECTED=1`、`M5A_V49_MIJAVA_GET_INFO_BRIDGE_JSON=1`、`M5A_V48_MIJAVA_BRIDGE_FAILED=0` |
| Web bootstrap | `/prod-api/getInfo=0`，因为前端检测到 `window.mijava.getInfo` 后走 Java bridge 分支；`/prod-api/getRouters=1` 仍由定点 XHR hook 接住 |
| 静态资源 | `chunk-00b3289e.51ab7483.js` 与 `chunk-17c57094.8c2a9a84.js` 均 200；后者仅为 `theme:"#059D81"` 模块，不含任务接口 |
| 控制台 | `LEVEL_ERROR=0`，无 `mijava is not defined`，无 `.some` 权限数组错误 |
| 副作用接口 | `getNewTask=0`、`upstatus=0`、`cancelAllRun=0`、`submit=0`、`save=0`、`toPackageDowloadData=0`、`toClearDataAll=0` |
| 环境恢复 | 停止 Java/Chromium 进程后清理 `data/app/activemq-data` 与 `data/app/bscache`；`data/app/App.dll` SHA-256 保持 `9084FABCE357AAD8B18D06D0FB708DE4E92E1B5D63686CEA1DED49E19F73A99B` |

分类结论：

- v49 证明 WhatsApp `AI采集` 恢复值入口、JxBrowser 页面层、`MiJava` bridge 注入、Web 权限 bootstrap 和本地结果空表读取链路可以在只读边界内跑通。
- “暂无数据”更符合 `getSpiderDataList` 从本地 `JSpiderData` 数据库读取当前无结果，而不是远端任务接口失败；这是基于 v49 UI 与第 14 节静态方法证据的推断，仍需后续只读确认 DAO 表和返回 JSON。
- 仍不能宣称采集业务恢复：关键词提交、任务创建、spider v2 队列、结果写入、OSS/代理/验证码/AI 辅助、导出和清空都没有执行或验收。
- 下一步应继续 M5A 只读分类 `getCloudSpiderConfig` 的远端优先/本地兜底行为、`getSpiderDataList` 的本地表结构和 spider v2 队列入口，必要时再进入 M5B 逐项兼容后端。

## 16. 2026-06-25 getCloudSpiderConfig/getSpiderDataList 只读边界确认

本轮不修改产物、不提交采集任务、不点击导出或清空，只把 v49 暴露出的两个只读 bridge 调用拆清楚。

| 项 | 证据 | 依赖分类 |
| --- | --- | --- |
| `getCloudSpiderConfig(spiderCode)` 调用链 | `MiJava.getCloudSpiderConfig(String, JsFunction)` 启动 `MiJava$160`；bootstrap 解码目标为 `com.sbf.util.http.SBFApi.H(String)`；`SBFApi.H` 请求 `/cloud/spider/code/<spiderCode>`，`code==200` 时取 `data` 并写入 `StartApp.a + /res/spider/<spiderCode>.cnf`；异常时读取同一本地 `.cnf`，不存在则返回空 `JSONObject` | 配置读取是“原后端远端优先 + 本地资源兜底/缓存”。v49 只读页面能进入空表，不代表远端配置服务可用或已重建。 |
| 本地配置缓存路径 | 解码字符串包含 `/res/spider/` 与 `.cnf`；现有 `data/app/res/spider/*.cnf` 已提供 WhatsApp/TikTok/Facebook 等本地 spider 配置 | 本地兜底属于客户端资源，可用于只读首屏字段；不能据此提交任务。 |
| `getSpiderDataList(moduleCode, spiderCode, pageNum, pageSize)` 调用链 | `MiJava$162` 以 `moduleCode + "_" + spiderCode` 命中 `MiJava.hashDba`，未命中时调用 `com.sbf.main.a.b(moduleCode, spiderCode)` 创建 DAO；随后 `WhereInfo.get()`、`limit(pageSize)`、`currentPage=pageNum`、`order("time", false)`、`countOf`、`queryLimit`，返回 `{"total":count,"rows":list}` | 本地结果列表读取，不访问 spider v2 队列，不创建任务。 |
| DAO 物理表 | `com.sbf.main.a.b` 使用 `DBHelper.init(StartApp.a + /data/ + moduleCode)` 与 `newDBDao("db_spider_data_" + spiderCode, JSpiderData.class, null)`；v49 运行目录实测 SQLite 文件为 `.artifacts/working/m5a-v49-datacollect-bridge-getinfo/data/whatsappdata/db_spider_data_whatsapp_users_lists.data`，表 `spider_data(spider_modal, spider_code, json_data, time, id)`，`COUNT=0` | “暂无数据”由本地 `spider_data` 空表解释；这是结果存储读路径，不是采集执行路径。 |
| `toClearDataAll` | `MiJava$163` 使用同一 DAO 并调用 `clearTableAll()` | 本地破坏性动作，M5A 禁止触发。 |
| `toPackageDowloadData` | `MiJava$164` 使用同一 DAO 分页读取、按 fields 映射并通过 `ExcelExportHelper` 写文件/打开文件 | 本地文件写入动作，M5A 禁止触发。 |
| spider v2 队列 | `SBFApi` 中 `/api/v1/client/pc/spider/v2/upstatus/` 写 `taskId/status/msg/total`；`/spider/v2/cancelAllRun/` 写 `moduleCode`；`/spider/v2/getNewTask/` 写 `status/moduleCode` 并返回 `data`；这些端点未被 dataCollect 空表 `getList()` 调用 | 队列/任务状态属于原后端任务系统边界；当前只读页面层没有进入该边界。 |

当前结论：

- dataCollect 首屏只读链路分成两类：配置字段取自远端优先/本地兜底，结果列表取自本地 DAO。
- v49 的“暂无数据”已经能由本地 SQLite `spider_data` 表为空解释；没有证据显示它来自 spider v2 接口失败。
- spider v2 的 `getNewTask/upstatus/cancelAllRun` 是后续采集执行队列边界；M5A 继续禁止关键词提交、创建任务、采集、导出、清空、上传和群发。

## 17. 2026-06-25 v50 低风险真实页面回归：一句话负结果与 AI大脑首屏

> 2026-06-25 校正：用户明确要求不要继续 AI大脑模块，后续可能删除该功能。本节和第 18 节仅作为历史运行证据保留；相关 v51 代码/测试改动不作为当前主线基线。当前 M5A 主线回到 WhatsApp `AI采集` 业务模块。

本轮从 v50 完整双击包继续 M5A，不再深挖 WhatsApp dataCollect。为保留日志，实际运行方式为从 `.artifacts/working/m4b-v50-local-launcher-package/data/app` 直接启动包内 Java 和 `App.dll`，stdout/stderr 写入 `.artifacts/runtime/m5a-v50-whatsapp-yijuhua-readonly/`；未修改项目根 `data/app`，未触碰桌面原始安装包。

| 流程 | 动作 | 结果 | 分类影响 |
| --- | --- | --- | --- |
| WhatsApp `一句话` | 启动 v50，进入 WhatsApp 后停留在默认 `一句话` 菜单；不输入、不点击发送 | 截图显示左侧 `一句话` 高亮但右侧空白；日志中 `REC_WHATSAPP_ONELINE=2`、`M5A_V44_SIDE_MENU_CALLBACK=1`，但 `M4_V12_DISPATCH=0`、`M4_V13_LOAD_URL=0`、`M4_V18_NORMALIZED_URL=0`、`M5_V26_WEB_BOOTSTRAP_XHR=0` | 该菜单仍是恢复值 `JSinglepage + /pc/aicloud/my`，不能作为 WhatsApp 原始业务流程闭合点；不继续在这个入口上钻。 |
| `AI大脑` | 在 WhatsApp 主界面左下点击 `AI大脑`，只打开聊天面板，不输入、不发送 | 成功加载 `https://app.xdxsoft.com/pc/aigc/aichat_dialog?st=...&lang=zh_cn`；截图显示 `火柴 Claw / AI智能大脑` 在线聊天面板和输入框 | 这是本轮低风险真实页面闭合点：真实前端资源和聊天 UI 可加载，但 profile/邀请码资料响应形状仍缺字段。 |

`AI大脑` 请求与错误证据：

| 检查点 | 结果 |
| --- | --- |
| 页面 URL | `M4_V18_NORMALIZED_URL=https://app.xdxsoft.com/pc/aigc/aichat_dialog?...`，`M4_V13_LOAD_URL=` 同 URL，`M4_V13_LOAD_FINISHED` 成功 |
| 静态资源 | 主框架、`app.988d65c1.js`、`chunk-libs.a18eb98a.js`、`chunk-elementUI.6bce4c17.js`、`chunk-17c57094.8c2a9a84.js`、`chunk-4806d588.15f97967.js`、CSS 和字体均至少一次 200 |
| 鉴权/bridge | `M4_V19_WEB_TOKEN_BRIDGE` 命中 `/prod-api/getLoingIsToken` 9 次；`M5A_V49_MIJAVA_GET_INFO_BRIDGE_JSON` 出现；`M5_V26_WEB_BOOTSTRAP_XHR url=/prod-api/getRouters` 出现 |
| XHR | 应用域名下只记录 2 个 GET XHR：`/prod-api/ads/inivitationCode/balance`、`/prod-api/system/user/profile` |
| 副作用 | 应用域名下 `POST/PUT/DELETE=0`；日志唯一 `method=POST` 是 Chromium 对 `optimizationguide-pa.googleapis.com` 的内部请求，不属于本客户端业务动作 |
| 控制台缺口 | `LEVEL_ERROR=5`，集中为 `avatar`、`invitationCode`、`phonenumber` undefined；说明 `system/user/profile` 与 `ads/inivitationCode/balance` 当前返回形状不足，但未阻止聊天面板壳层渲染 |
| 截图 | `.artifacts/runtime/m5a-v50-whatsapp-yijuhua-readonly/screen-whatsapp-yijuhua-after-enter.png`，SHA-256 `EED0BC467F734A4DA46602D1EFC2A1831A176E1C2C5D368E5961B1373054C28E`；`.artifacts/runtime/m5a-v50-aicloud-readonly/screen-aicloud-after-click.png`，SHA-256 `60226716005192CED6CAF193DC663C5E9C10B5AB3517D529AFF2618245E40971` |
| 产物哈希 | 完整包 `App.dll` 启动后保持 SHA-256 `26694D706D8141EF8131891285A4ADAB02A0D7E6F70BBF509D27395220F652D0` |

分类结论：

- `AI大脑` 属于“真实在线 Web 前端 + 本地鉴权 bridge + 原后端资料/邀请码接口”的混合流程；页面壳层和静态资源可直连保留。
- 历史判断：若当时继续 `AI大脑`，只应针对运行日志已证明的 `system/user/profile` 与 `ads/inivitationCode/balance` 补最小兼容响应；但该支线已停止，不进入后续计划。
- 本轮没有发送聊天消息，因此还没有触发 AI 对话生成、计费、上传、TOS/OSS 或模型接口；聊天发送属于下一层更高风险流程，需单独确认后再做。

## 18. 2026-06-25 v51 AI大脑 profile/邀请码最小响应补形状

> 2026-06-25 校正：本节产物和验证不再推进，不进入后续计划。当前代码层已不保留该支线的新增响应形状；如果将来删除 AI大脑功能，本节可作为删除前的历史取证记录。

本轮只补第 17 节运行日志已证明的两个 GET 接口：`/prod-api/system/user/profile` 与 `/prod-api/ads/inivitationCode/balance`。不扩展为通用 `/prod-api/*` 代理，不发送聊天消息。

| 项 | 结果 |
| --- | --- |
| 候选产物 | `.artifacts/working/m5a-v51-aicloud-profile-balance/App-m5a-v51-aicloud-profile-balance.jar` |
| SHA-256 | `06B9EDA686C17F8DE46EA337D6B448B237FBB03A9673F41BF7742F866B2B08BB` |
| 新增响应 | `system/user/profile` 返回 `user/avatar/phonenumber/email/roles/permissions` 等最小资料形状；`ads/inivitationCode/balance` 返回 `invitationCode/inivitationCode/balance/amount/total` |
| 测试 | 先新增失败断言，要求注入脚本含两个 endpoint 和 `avatar/phonenumber/invitationCode` 字段；红灯失败在缺 `/prod-api/system/user/profile`，实现后目标测试和完整 `27/27` 单测通过 |
| 产物检查 | `M5InjectJsCallback.class` 含 `/prod-api/system/user/profile`、`/prod-api/ads/inivitationCode/balance`、`avatar`、`phonenumber`、`invitationCode`、`M5_V26_WEB_BOOTSTRAP_XHR` |
| 宿主只读验证 | 独立运行目录 `.artifacts/working/m5a-v51-aicloud-profile-balance-run/`，stdout/stderr 在 `.artifacts/runtime/m5a-v51-aicloud-profile-balance-readonly/` |
| 页面结果 | `AI大脑` 仍加载 `https://app.xdxsoft.com/pc/aigc/aichat_dialog?...`，截图 `.artifacts/runtime/m5a-v51-aicloud-profile-balance-readonly/screen-aicloud-v51-after-profile-balance.png`，SHA-256 `729F21B2F4EBE6AB0AC753E128DB4F1D2224BA770B7E1B6DC6089E4E6F15D93E` |
| 日志结果 | `/prod-api/system/user/profile=1`、`/prod-api/ads/inivitationCode/balance=1`、`M5_V26_WEB_BOOTSTRAP_XHR=3`、`LEVEL_ERROR=0`、`UNHANDLED_REJECTION=0` |
| 副作用 | 应用域名下 `POST/PUT/DELETE=0`；唯一 `method=POST` 仍是 Chromium 对 `optimizationguide-pa.googleapis.com` 的内部请求，不属于业务动作 |

分类结论：

- `AI大脑` 首屏已从“壳层可见但资料字段报错”推进到“壳层可见且无控制台错误”的低风险闭合点。
- 当前恢复边界仍止于页面首屏；未验证发送消息、AI 生成、计费、附件上传、TOS/OSS 或模型接口。
- 该支线已停止：不再解析发送按钮，不进入 AI 对话生成或计费链路。

## 19. 2026-06-25 WhatsApp AI采集任务创建链路只读校准

本节回应“什么时候才能进入业务模块”的校准：WhatsApp `AI采集` 已经进入业务模块的 dataCollect 列表页，但尚未进入任务创建/执行层。当前不能继续把首屏只读当成业务闭环。

| 层级 | 已确认 | 还差什么 | 风险边界 |
| --- | --- | --- | --- |
| 页面入口 | `REC_WHATSAPP_COLLECT_USERS_ROUTE` 恢复子路由可打开 `/pc/dataCollect/collectionTask/data_index?spiderCode=whatsapp_users_lists&moduleCode=whatsapp` | 原始真实菜单 JSON 仍不可得；当前仍是恢复值 | 可继续使用恢复值做验证，但不能宣称原始菜单字段已找回。 |
| 首屏读路径 | `chunk-00b3289e.51ab7483.js` 只调用 `getCloudSpiderConfig`、`getSpiderDataList`，页面按钮只有下载、清空、刷新 | 首屏没有新增/提交任务按钮；任务创建不在该 chunk 中 | 下载、清空是本地写/删动作，仍禁止点击。 |
| spider 配置 | `whatsapp_users_lists.cnf` 要求 `googSite/areaCode/pltCode/keywords`，脚本会打开 Google 并拼 `site:<pltCode> <keywords> intext:whatsapp <areaCode>` | 需要确认任务参数从哪里进入 spider 宿主：原后端队列、云任务页，还是本地兼容队列 | 一旦输入关键词并启动脚本，就会访问 Google、处理验证码和写结果，不再是只读。 |
| 采集执行 | `.cnf` 内调用 `spider.doHref`、`spider.toCheckDom`、`spider.postData(JSON.stringify(data))`、`spider.endTask()` | 需要拆 `SpiderCallback.postData/endTask` 如何写 `JSpiderData` 和如何触发 `window.reloadData` | `postData` 是结果写入边界；可静态拆，不能运行真实采集。 |
| 验证码/AI 辅助 | `.cnf` 内调用 `mijava.creatGoogleCRTask(base64, qid, callback)` | 需要分类它依赖原后端、第三方识别还是本地能力 | 涉及图片上传/识别/费用或服务端能力，必须作为 M5B 兼容项处理。 |
| 队列状态 | `SBFApi` 保留 `/api/v1/client/pc/spider/v2/getNewTask/upstatus/cancelAllRun/get` | 需要决定是否重建最小本地任务队列，让 spider 宿主有任务可拉取 | `getNewTask/upstatus/cancelAllRun` 是任务队列/状态写边界，本轮未触发。 |

当前结论：

- 业务模块已经进入到 WhatsApp `AI采集` 的 dataCollect 结果页；不是还停在外壳。
- 真正缺的是“任务来源 -> spider 宿主执行 -> `spider.postData` 写本地表 -> 页面 `reloadData/getSpiderDataList` 看到结果”的小闭环。
- 下一步不应继续 AI大脑，也不应继续无限首屏只读；应静态拆 `SpiderCallback.postData/endTask/reloadData` 和 spider v2 队列调用后，决定是否做一个本地最小任务队列/结果写入兼容层。该层属于 M5A 高风险流程或 M5B 起点，必须仍以“不提交真实采集任务”为边界。

### 19.1 结果写入与任务创建边界补充

本轮继续只读确认了一段必要链路，但仍未运行 spider：

| 点位 | 证据 | 结论 |
| --- | --- | --- |
| 结果写本地表 | `JSpiderCloude.a(JSONObject)` 创建 `JSpiderData`，设置 `spiderCode/spiderModal/time/jsonData`，调用 DAO `addOrUpdate`，随后对结果页执行 `window.reloadData` | `postData` 之后的本地 `spider_data` 写入与前端增量刷新路径已确认。 |
| dataCollect 页签 | `JSpiderCloude.c(String)` 中 `tablePanel1` 打开 `/pc/dataCollect/collectionTask/data_index?spiderCode=<code>&moduleCode=<module>`；`tablePanel2` 打开 `/pc/cloud/task/myindex?spiderCode=<code>` | 结果表和云任务页是两个不同入口；当前 v49 只恢复了结果表。 |
| 任务表单 | `JSpiderCloude$9` 组装 `newTaskForm`，字段包括 `spiderCode/spiderParams/cloudServer/moduleCode/taskConfig`，成功后生成 `taskId`，并继续组装 `taskId/data/spiderMode/cookie/proxy` | 这是创建/提交采集任务边界；静态可读，但不能在 M5A 只读范围内触发。 |
| spider 运行器 | `JSpiderCloude$2` 初始化 `JCloudSpiderMaster` 并把 `moduleCode/spiderMode/spiderAppCode` 等运行参数传入 | 任务进入真实 spider 执行层后会访问外部站点、处理代理/验证码、写结果和上报状态。 |

更新后的判断：若要继续，前置步骤不是 AI大脑，也不是继续路由猜测，而是先设计“本地最小任务队列 + 本地结果写入”的离线/可控兼容方案；在没有这个方案前，不应点击任务创建或输入真实关键词。

### 19.2 v52 本地任务/结果桥实现

本轮开始把上面的兼容方案做成可测试代码，但边界仍限定在本地 mock，不运行真实 spider。

| 项 | 结果 |
| --- | --- |
| 新增类 | `com.sbf.main.jxbrowser.M5LocalSpiderBridge`，由 `M4AuthPatch` 作为 support class 写入补丁 JAR。 |
| 队列行为 | `getNewTask("whatsapp")` 固定返回空数组，并打印 `M5A_LOCAL_SPIDER_QUEUE_EMPTY`；不向 `/api/v1/client/pc/spider/v2/getNewTask/` 拉任务。 |
| 任务预览 | `previewTask("whatsapp", "whatsapp_users_lists", params)` 返回 `dryRun=true`、`submitted=false` 和 `local-preview-*`，只用于确认参数形状，不创建云任务。 |
| 本地结果写入 | `writeMockResult(baseDir, "whatsapp", "whatsapp_users_lists", json)` 创建或打开 `data/whatsappdata/db_spider_data_whatsapp_users_lists.data`，建表 `spider_data(spider_modal, spider_code, json_data, time, id)` 并插入一条 mock 行。 |
| 产物 | `.artifacts/working/m5a-v52-local-spider-bridge/App-m5a-v52-local-spider-bridge.jar`，SHA-256 `23C59F9ADE422317725402C9C0B1CA7B0AF3506A3FA25F739FD01FF5D1E06204`。 |
| 测试 | `python -m unittest discover -s tests -v` 通过 `28/28`；新增测试直接用 SQLite 验证写入行的 `spider_modal=whatsapp`、`spider_code=whatsapp_users_lists` 和 `json_data`。 |

分类结论：

- 这一步证明 `getSpiderDataList` 所读的本地 `spider_data` 表可以被本地兼容桥写入，因此“空表 -> 有结果行”的最小本地闭环具备实现基础。
- 这一步没有证明真实采集恢复：没有创建远端任务、没有启动 `JCloudSpiderMaster`、没有运行 `whatsapp_users_lists.cnf`、没有访问 Google、没有验证码识别、没有代理/OSS/AI 辅助。
- 后续若要让页面可见 mock 结果，应增加显式 local-only 入口并刷新 dataCollect；真实任务队列和云任务页仍需单独设计，不应默认接入。

### 19.3 v53 页面显式 seed 入口

v53 在 v52 基础上把本地写表能力挂到页面可调用的显式 hook：

| 项 | 结果 |
| --- | --- |
| MiJava 新方法 | `m5WriteLocalMockResult(moduleCode, spiderCode, jsonData)`，带 `@JsAccessible`，内部调用 `M5LocalSpiderBridge.writeMockResult(StartApp.a, ...)`。 |
| 页面 hook | `window.__m5LocalSpider.seedWhatsAppMockResult()`，写入 `source=local-ui-mock`、`submitted=false` 的本地 mock 行后尝试 `window.reloadData()`。 |
| 产物 | `.artifacts/working/m5a-v53-local-datacollect-seed/App-m5a-v53-local-datacollect-seed.jar`，SHA-256 `B0D1192AABDBC6C67F3FC4AC9973122552D6B3A73C9E59205E5D3CBB0B88C74E`。 |
| 验证 | 完整单测 `28/28` 通过；产物级 `javap` 确认 MiJava 方法与注入脚本字符串。 |

分类结论：

- v53 比 v52 多的是“页面可显式触发本地 mock 结果写入并刷新”；它不是任务创建入口。
- 该 hook 不自动执行，避免进入真实采集、队列或外部网络流程。
- 下一步若需要宿主截图证据，可只读打开 `AI采集` 页后在控制台调用该 hook，观察表格从“暂无数据”变为本地 mock 行；仍不得输入真实关键词或点击云任务创建。

### 19.4 v56 宿主可见性验证

| 项 | 结果 |
| --- | --- |
| 产物 | `.artifacts/working/m5a-v56-local-datacollect-visible-fields/App-m5a-v56-local-datacollect-visible-fields.jar`，SHA-256 `4AE223B9CDD39993AAA5090EE682A0DE661B14189E6BAE203150868A7DB313F9`。 |
| 字段配置 | `MiJava.getCloudSpiderConfig` 对当前 `whatsapp_users_lists` 返回本地字段：`googSite/pltCode/keywords/phone/date/url`。 |
| 本地结果 | SQLite `spider_data` 一行：`googSite=google.com`、`pltCode=example.com`、`keywords=local-test`、`phone=+10000000000`、`date=2026-06-25`、`url=https://example.com/local-ui-mock`、`submitted=false`。 |
| UI 证据 | `.artifacts/runtime/m5a-v56-local-datacollect-visible-fields-host/screen-visible-fields-row-after-click.png` 显示 `AI采集` 页表头和一条 mock 行，底部 `共 1 条`。 |
| 副作用检查 | 未输入真实关键词，未点击云任务页，未运行 `.cnf`，未调用 `getNewTask/upstatus/cancelAllRun/submit/save/toClearDataAll/toPackageDowloadData`。 |

结论：WhatsApp `AI采集` 当前已证明“dataCollect 页面 -> 本地字段配置 -> 本地 `spider_data` 写入 -> 页面可见行”的 local-only 小闭环。它仍不是正式采集恢复；真实采集还缺任务来源、spider 宿主运行、Google/验证码/代理/OSS/AI 辅助和状态上报。
