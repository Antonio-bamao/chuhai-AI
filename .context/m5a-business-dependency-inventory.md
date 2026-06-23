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
