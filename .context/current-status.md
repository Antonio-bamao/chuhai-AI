# 当前状态

- 项目背景：这是用户本人早年设计并打包的火柴AI客户端。源码、原服务端、云服务器、数据库和管理后台控制权均已遗失；没有可用旧虚拟机、历史账号或历史凭据。当前工作是在本地恢复自己的客户端，不再把取得原服务端真实 token 作为前提。
- 当前阶段：M4B 免操作启动与运行验收。v33 是冻结的稳定技术基线，不是最终 M4 交付；v40 候选在其上增加九产品、76 菜单、原始 logo/icon 和本地自动登录，不回写或覆盖 v33 产物。v50 已制作项目内本地无更新双击包，绕过原 `HuoChaiAI.exe` 启动前覆盖 `App.dll` 的更新行为。
- 已完成：M1-M3 完成；v33 已实现本地登录 JSON、`getInfo`、产品初始化、Web token bridge、`getRouters`、Web 首屏必要响应形状，并验证 AiCloud 在线首屏可渲染。8 个可进入系统内部 code 已确认：`whatsapp/tiktok/facebook/instagram/twitter/telegram/geo/wskefu`；独立站采用高置信 `aishope` 候选并保持未开通。
- 已完成 M4A：v40 运行时已显示 8 个可进入系统、1 个未开通独立站、原包九产品 logo；WhatsApp 主界面已显示 11 项恢复侧边栏及原包菜单 icon。`41/aigc/C28500001/C28500002` 临时目录已撤除。
- 进行中 M4B：`StartApp$3.run()` 保留登录前初始化，只在创建 `JLoginHTML` 前调用本地登录成功链；无需账号、密码或点击登录即可进入产品选择器。`StartApp$1` 对不存在的登录窗增加 null guard，避免自动登录后的 dispose NPE。
- 当前 M4B 交付物：完整自包含目录包 `.artifacts/working/m4b-v50-local-launcher-package/`，大小 `1,710,123,210` 字节；`data/app/HuoChaiAI.exe` 为本地无更新启动器，SHA-256 `986BE98D4C7E7655BD6B8A738FD9D9B5637727F14EAC0851762D5CF226B8933E`；原官方更新器保留为 `HuoChaiAI-updater-original.exe`，SHA-256 `2A6DC95DE97761E4C92EF830ABCA56516B65A2FF3A3372E6ACCD156524A6D115`；`App.dll` 使用 v49 补丁，SHA-256 `26694D706D8141EF8131891285A4ADAB02A0D7E6F70BBF509D27395220F652D0`。
- 下一步：若要求最高强度证据，仍需在可控 VM 或具备管理员权限的环境补一次物理断网/网卡禁用验收；否则 M4B 当前可转入 M5A WhatsApp `AI采集` 的任务/队列/结果写入链路评估。AI大脑支线已按用户要求停止，不作为后续基线。
- 阻塞项：当前会话无管理员权限，无法创建临时 Windows 出站防火墙规则。v50 完整包已在进程级 HTTP/HTTPS/SOCKS 黑洞代理下通过九产品选择器与点击 WhatsApp 进入主界面壳层验证，但该证据不等同于阻断所有 UDP 的物理断网。
- 验收目标校正：用户桌面快捷方式 `C:\Users\m1591\Desktop\火柴AI.lnk` 指向用户本人安装的原始包 `H:\HuoChaiAI\app\HuoChaiAI.exe`，不作为本项目恢复产物验收目标。该原始安装包已恢复原 `App.dll` 哈希 `72689D3C96F28A9DFBDDCFFC3F14D082A174AC0FED153144CD2AA89D27C3D494`，后续不得再用它验证 M4B。
- M5A 已启动只读验收：`.context/m5a-business-dependency-inventory.md` 已记录第一轮结果，`.context/m5a-menu-route-discovery.md` 已记录菜单路由发现、本地 spider/dataCollect 入口证据和 WhatsApp `AI采集` 候选入口。AiCloud 首屏和空表证据可复用；ADB/FFmpeg 可执行；JxBrowser/Selenium/Playwright 依赖存在。关键新边界：菜单模型确实消费 `localCode/linkUrl`，原客户端支持 `ZWBrowser/JBigDataMaster/ai_mnq_manager/PhoneFission/JSinglepage` 等打开器；v40 的 76 个菜单仍统一 `JSinglepage + /pc/aicloud/my`，因此 WhatsApp/GEO 当前只能证明外壳和菜单存在；但 `data/app/res/spider/*.cnf` 与 `JSpiderCloude` 已证明 WhatsApp/TikTok/Facebook 等采集脚本和 `/pc/dataCollect/collectionTask/data_index?spiderCode=...&moduleCode=...` 入口族仍在包内。v41 已证明 `localCode=pc/dataCollect/collectionTask` 只会下发菜单字段、不触发页面；v42 改为 `localCode=JSinglepage`、`linkUrl=/pc/dataCollect/collectionTask/data_index?spiderCode=whatsapp_users_lists&moduleCode=whatsapp`，字段下发成功，但项目内宿主只读点击仍只高亮 `AI采集`，右侧空白，日志无 `M4_DIAG_MODERN_DISPATCH_ENTER`、`M4_V13_LOAD_URL`、`M4_V18_NORMALIZED_URL`、XHR 或 spider 任务接口。下一步应插桩 `com.sbf.main.ext.j2026.h$2`/左侧菜单回调条件，先确认为什么点击没有进入内容创建分发器。
- M5A 最新进展：v43 证明 `com.sbf.main.ext.j2026.h$2` 不是 WhatsApp 侧边栏点击处理器；v44 证明真实点击链在 `com.sbf.main.ext.j2026.d$2` 与 `d$1`，且点击可触发回调但无子菜单时不会进入内容分发；v45 增加 `REC_WHATSAPP_COLLECT_USERS_ROUTE` 子路由后进入 `M4_V12_DISPATCH`；v46 证明 JxBrowser 已创建但错误加载 `JSinglepage` 占位；v47 增加仅限当前 WhatsApp AI采集恢复子路由的 `JSinglepage` 归一化桥接后，宿主只读验证已加载 `https://app.xdxsoft.com/pc/dataCollect/collectionTask/data_index?spiderCode=whatsapp_users_lists&moduleCode=whatsapp`，主框架和静态资源 200，随后暴露 `mijava` bridge 缺口；v48 注入真实 `MiJava` 后消除 `mijava is not defined`，但因前端 Java bridge 分支期望顶层 `permissions`，`StartApp.m` 原嵌套形状导致 `hasPermiOr` 的 `.some` 报错；v49 将 `MiJava.getInfo(callback)` 改为扁平 `user/roles/permissions` bridge JSON 后，宿主只读验证进入 WhatsApp `AI采集` 空表页，截图显示“暂无数据”，无控制台错误。结论：v49 只证明 WhatsApp `AI采集` 到达 dataCollect 页面空表层；任务提交、结果保存、OSS、验证码/代理/AI 辅助和 spider 队列仍未恢复或验收；本轮未输入关键词、未创建任务、未采集、未上传、未群发。
- M5A 最新只读解析：dataCollect 页面 chunk `chunk-00b3289e.51ab7483.js` 的 `data_index` 组件首屏直接调用 `mijava.getCloudSpiderConfig(...)`，随后调用 `mijava.getSpiderDataList(...)` 读取本地结果；导出与清空分别是 `toPackageDowloadData`、`toClearDataAll`，均属于后续有副作用动作。原包 `MiJava` 确认暴露这些方法；原云采集宿主 `com.sbf.main.cloud.spider.b` 会注入 `MiJava` 和 `SpiderCallback`。v49 当前只补当前 dataCollect 恢复子路由所需的 `MiJava` 注入与 `getInfo` 扁平权限契约；`chunk-17c57094.8c2a9a84.js` 仅包含 `theme:"#059D81"`，不含任务接口。下一步应只读分类 `getCloudSpiderConfig/getSpiderDataList` 的配置源、本地数据库表和 spider v2 队列边界，而不是提交任务。
- M5A 最新边界确认：`getCloudSpiderConfig` 经 `MiJava$160 -> SBFApi.H(spiderCode)` 走远端 `/cloud/spider/code/<code>` 优先，成功时写回 `/res/spider/<code>.cnf`，异常时读取本地 `.cnf`，无文件返回空 JSON；`getSpiderDataList` 经 `MiJava$162` 只读本地 `JSpiderData` DAO，`countOf/queryLimit` 后返回 `total/rows`。v49 运行目录实测 `.artifacts/working/m5a-v49-datacollect-bridge-getinfo/data/whatsappdata/db_spider_data_whatsapp_users_lists.data` 表 `spider_data(spider_modal, spider_code, json_data, time, id)` 为空，解释了“暂无数据”。`data_index` 前端 chunk 本身只有列表、下载、清空、刷新，没有新增/提交任务按钮；`whatsapp_users_lists.cnf` 里的真实采集脚本会打开 Google、拼接关键词、调用 `spider.postData(...)` 写线索，并在验证码场景调用 `mijava.creatGoogleCRTask(...)`，这已经越过只读首屏边界。2026-06-25 用户明确停止 AI大脑支线；此前 v50/v51 记录仅保留为历史证据，代码改动不作为主线保留。当前若继续 M5A，应进入 WhatsApp `AI采集` 的任务创建/队列/本地写入小闭环设计或 M5B 兼容后端，而不是继续 AI大脑或首屏只读。
- M5A 最新实现进展：v52 新增 `M5LocalSpiderBridge` 并打入补丁产物，先实现 WhatsApp `AI采集` 的本地 mock 队列/结果表兼容点：`getNewTask("whatsapp")` 固定空队列，`previewTask(...)` 只返回 `dryRun=true/submitted=false`，`writeMockResult(...)` 只向本地 `data/whatsappdata/db_spider_data_whatsapp_users_lists.data` 的 `spider_data` 表写入 mock 行。产物 `.artifacts/working/m5a-v52-local-spider-bridge/App-m5a-v52-local-spider-bridge.jar`，SHA-256 `23C59F9ADE422317725402C9C0B1CA7B0AF3506A3FA25F739FD01FF5D1E06204`；完整单测 `28/28` 通过。该步仍未提交真实采集任务，未运行 `.cnf`，未访问 Google/验证码/代理/OSS/AI 辅助。
- M5A 最新实现进展续：v53 在 v52 基础上给 `MiJava` 增加 `m5WriteLocalMockResult(...)`，并在页面注入 `window.__m5LocalSpider.seedWhatsAppMockResult()`。该 hook 只有显式调用才写入 `local-ui-mock` 本地 mock 行并尝试 `window.reloadData()`；不自动运行、不创建任务、不接管 spider v2。产物 `.artifacts/working/m5a-v53-local-datacollect-seed/App-m5a-v53-local-datacollect-seed.jar`，SHA-256 `B0D1192AABDBC6C67F3FC4AC9973122552D6B3A73C9E59205E5D3CBB0B88C74E`；完整单测仍为 `28/28` 通过。
- M5A 宿主最新验证：v56 已进入 WhatsApp `AI采集` 页面并显示一条本地 mock 行。产物 `.artifacts/working/m5a-v56-local-datacollect-visible-fields/App-m5a-v56-local-datacollect-visible-fields.jar`，SHA-256 `4AE223B9CDD39993AAA5090EE682A0DE661B14189E6BAE203150868A7DB313F9`；截图 `.artifacts/runtime/m5a-v56-local-datacollect-visible-fields-host/screen-visible-fields-row-after-click.png`，SHA-256 `DE147C965011AB7230464D05AF08FE8A5D50F432F58C7EC62369E22632003AC6`。该步证明 local-only 的 dataCollect 字段配置、`spider_data` 写入和页面可见行闭环；仍未提交真实采集任务、未运行 `.cnf`、未访问 Google/验证码/代理/OSS/AI 辅助。
- M5C 最新进展：`AI筛选` 已恢复原版 `/ws/wsfilter/home`（模块 3317 / `chunk-2d0b957c.6a5f3dab.js`）。父菜单为 `JSinglepage + /ws/wsfilter/home`，j2026 子路由使用 `JSinglepage:/ws/wsfilter/home` 显式归一化，避免落入旧 dataCollect 兜底。`getWsFilterDataList` 保留原厂 `MiJava$171 -> JSBFMain.F() JWSFriends DAO -> WhereInfo.limit/currentPage -> countOf/queryLimit -> {total,rows}`；`checkWSfilterStatus` 与 `doGetAllOpenBrowserInWhatsapp` 保留原读链，无 WA 时 UI 显示 `已开通 0 / 未开通 0`、筛选通道“无数据”；`doZwFilterWhataspp` 已硬 gate 为 `-1 + 需登录 WhatsApp；执行新筛选待单独接入`，不再实例化 `MiJava$170`。候选 `.artifacts/working/m5c-ai-filter/App-m5c-ai-filter.jar`，SHA-256 `9BC58198EFD4B69A30198B25A384304861F077D6A004F1CFDEAFF088F5C674D5`；截图 `.artifacts/runtime/m5c-ai-filter/screen-06-ai-filter-fitted.png` 和 `screen-08-empty-wa-channel-options.png`。结论：UI 与本地列表/空态已跑通；真实 WhatsApp 筛选执行单独立项。
- 当前活跃日志分片：work-log.md
- v15 VM 证据：`M4_V14_RENDER_MODE=OFF_SCREEN`，包含 SwiftShader/D3D11 软件渲染开关；`M4_V13_LOAD_FINISHED` 成功；`C:\m2dump\m4-jxb-capture.png` 能完整渲染 `HuoChaiAI Offline Mode`，右侧空白问题被软件渲染解决。
- 重要校正：v8-v15 的 `offline-home.html` 只是诊断页，用来区分菜单/加载/渲染问题；它不是最终业务目标。最终目标不是“业务离线化”，而是“授权/登录/时效/付费门槛本地通过 + 采集/群发/云手机/投屏/视频继续联网”。
- v18 改动：保留 v15/v16 的 `RenderingMode.OFF_SCREEN`、`disableGpu()`、SwiftShader/D3D11 软件渲染修复；菜单仍保持原始业务路由 `/pc/aicloud/my`；在 `com.sbf.main.jxbrowser.c$3.run()` 调用 `Navigation.loadUrl` 前，如果 URL 以 `/` 开头，则拼为 `https://` + 原程序业务域名 `com.sbf.util.http.SBFApi.c()` + 原路径，并打印 `M4_V18_NORMALIZED_URL=`。该来源来自原始 `JSBFMain` 构造 AIGC URL 的字节码证据。
- v19 改动：新增 `com.sbf.main.StartApp.f(String)` 前置桥接。若入参包含 `getLoingIsToken` 或 `get_current_token`，打印 `M4_V19_WEB_TOKEN_BRIDGE url=...` 并返回本地登录 token `offline-local-token-1234567890`。这只补 Web 登录态入口，不改真实业务 URL，不恢复 `offline-home.html`，不把采集/群发/云手机/视频等业务功能离线化。
- v18 产物：`.artifacts/working/m4-auth-jump-v18-sbfapi-url-base/App-m4-auth-patched-v18-sbfapi-url-base.jar`，大小 `31,866,319` 字节，SHA-256 `1C0DC7C4A8D79FEAE71ADA673D960921B9E032CD1AC2E703BC3FB526A7EE33A2`。
- v18 ISO：`.artifacts/working/m4-auth-jump-v18-sbfapi-url-base.iso`，大小 `31,932,416` 字节，SHA-256 `D21AA803D8294A9BBFDB57DE9DFF4E087EB6C379A614BFC37C5D989F159CD8AE`；ISO 只包含 v18 JAR 和中文 README，不包含 `offline-home.html`。
- v19 产物：`.artifacts/working/m5-web-login-bridge-v19/App-m4-auth-patched-v19-web-login-bridge.jar`，大小 `31,866,544` 字节，SHA-256 `E7826E328D591445F8F8D3C5548DFBBDABE5B6D96B0D6FB180DAAB5FDA1E00E3`。
- v19 ISO：`.artifacts/working/m5-web-login-bridge-v19.iso`，大小 `31,932,416` 字节，SHA-256 `4EBA6D2D52D2384A3803FA7F9FFC2A37F3D574CCCDDF0CC2C6577E9D43DB7C2B`；ISO 只包含 v19 JAR 和中文 README，不包含 `offline-home.html`。
- 宿主侧验证：目标测试覆盖 `StartApp.f("...getLoingIsToken")` 和 `StartApp.f("...get_current_token")`，返回本地 token；`python -m unittest discover -s tests -v` 通过 23/23；产物级 ZIP 比较显示原始 App.jar 到 v19 仅 9 个预期 class 改变：v18 的 8 个 class 加 `com/sbf/main/StartApp.class`；`javap` 确认 `StartApp.f(String)` 中存在 `getLoingIsToken`、`get_current_token`、`M4_V19_WEB_TOKEN_BRIDGE url=`、`offline-local-token-1234567890` 和 `String.contains(CharSequence)`；`git diff --check` 仅有既有 CRLF 警告。
- 宿主机运行证据：覆盖 `C:\m2dump\app\App.jar` 为 v19 后启动，点击登录和产品页“进入系统”。日志对 `AIGC Video`、`Graphic Video` 均显示 `M4_V18_NORMALIZED_URL=https://app.xdxsoft.com/pc/aicloud/my?...`、`M4_V13_NAV_FINISHED ... error=OK`，且多次出现 `M4_V19_WEB_TOKEN_BRIDGE url=https://app.xdxsoft.com/prod-api/getLoingIsToken`；未再出现 v18 的 `login?redirect=...` 终态。截图 `C:\m2dump\host-screen-v19-business.png` 显示在线业务壳层和 `AIGC Video` / `Graphic Video` Tab，说明 Web 登录页重定向已越过。
- v27 产物：`.artifacts/working/m5-js-page-bootstrap-v27/App-m5-v27-js-page-bootstrap.jar`，大小 `31,871,018` 字节，SHA-256 `35F72D6A1C5CC4B9A1C47E38AF2028DA219ED98577261C2A515A17878FB2FDA8`。
- v27 ISO：`.artifacts/working/m5-js-page-bootstrap-v27.iso`，大小 `31,936,512` 字节，SHA-256 `3B32737E36DC6C00DA9CAA040D22EEFC3A3C779BAA60690625E91525E1F178C4`；ISO 只包含 v27 JAR 和中文 README，不包含 `offline-home.html`。
- 宿主机 v27 证据：`C:\m2dump\m5-v27-host.log` 显示 `M4_V18_NORMALIZED_URL=https://app.xdxsoft.com/pc/aicloud/my?...`、`M5_V26_WEB_BOOTSTRAP_XHR url=/prod-api/getInfo`、`/prod-api/getRouters`、`/prod-api/system/dict/data/type/yes_no_1_0`、`/prod-api/mnq/mnqAuthAccounts/mylist?pageNum=1&pageSize=10`，并加载真实业务 chunk `chunk-dea9eb98.0b47177e.js`。`C:\m2dump\m4-jxb-capture.png` 已显示 AiCloud 授权码表页面和“暂无数据”，右侧不再白屏。
- v28 产物：`.artifacts/working/m5-js-page-bootstrap-v28-init-shape/App-m5-v28-init-shape.jar`，大小 `31,871,132` 字节，SHA-256 `4AB22FF9AA1063E0CA3C74AFC057D83798DE76B9BB1554D59BD056D9DB7A25B1`。
- v28 ISO：`.artifacts/working/m5-js-page-bootstrap-v28-init-shape.iso`，大小 `31,936,512` 字节，SHA-256 `61B77F2512AC7A7042F9C537C90C2F556FCF741C5B29097ECE04BFBC67912BCC`；ISO 只包含 v28 JAR 和中文 README，不包含 `offline-home.html`。
- v28 校正：v28 补 `im.udp.port` 后宿主机仍出现非致命 `JSBFMain.<init>` NPE。v29-v32 的分段诊断证明构造函数能正常返回，异常来自内部 IM 配置 catch；原字节码密文顺序确认实际读取 `im.port.udp`，不是 `im.udp.port`。临时 `JSBFMain` 诊断/托盘兜底均未进入正式版本。
- v33 产物：`.artifacts/working/m5-im-shape-v33/App-m5-v33-im-shape.jar`，大小 `31,871,130` 字节，SHA-256 `24CCC59B18DC97EF05BBD57B46844B7B56F469E48BE1A85DA3A4649DC7957DF5`。
- v33 ISO：`.artifacts/working/m5-im-shape-v33.iso`，大小 `31,936,512` 字节，SHA-256 `AE54073C1745E08164946814ABC949EB54894F67867705DD5F7143D09416C154`；ISO 只包含 v33 JAR 和 README，不包含 `offline-home.html`。
- v34 取证产物：`.artifacts/working/m4-real-product-menu-logging-v34/App-m4-v34-real-product-menu-logging.jar`，大小 `31,870,641` 字节，SHA-256 `F0E39AFEF17D800B83F9C4066DE6D565C663AE3CB553FEBAAC8885A77B478150`。
- v34 取证 ISO：`.artifacts/working/m4-real-product-menu-logging-v34.iso`，大小 `31,934,464` 字节，SHA-256 `44FBB3CA12534BD9F5C697B7D09F290908F7A00B72063EC3EFBD1466DB4B492C`；ISO 只包含 v34 JAR 和中文 README，不包含 `offline-home.html`。v34 使用 `--real-product-menu-logging` 模式，保留真实 `SBFApi.C()` 与 `SBFApi.k()` 返回路径，并在返回边界打印 `M4_EVIDENCE_PRODUCT_MODULE_REAL_JSON=` 与 `M4_EVIDENCE_PC_MENUS_REAL_JSON=`；默认补丁模式仍保持 v33 临时产品/菜单行为。宿主机实测中真实产品接口返回 `code=401` 令牌验证失败；菜单接口用反射探针触发时在返回前抛出 `JSONObject text must begin with '{'`，说明当前本地 token 下菜单原始响应不是 JSON 对象，单纯 `ARETURN` 返回边界日志不足以取得菜单 raw body。
- v35 取证产物：`.artifacts/working/m4-real-menu-raw-logging-v35/App-m4-v35-real-menu-raw-logging.jar`，大小 `31,870,701` 字节，SHA-256 `02D912F0E2F3553374BFE5BDDFEBF34FA00E4A890D52F18B4F064F5542FDBF58`。
- v35 取证 ISO：`.artifacts/working/m4-real-menu-raw-logging-v35.iso`，大小 `31,934,464` 字节，SHA-256 `8B5C4EC9E0E9CDB82170CC2AF52DD4667470F019BDAAB70A972CD83537D448C8`；ISO 只包含 v35 JAR 和中文 README，不包含 `offline-home.html`。v35 在 v34 基础上把无参 `SBFApi.k()` 菜单取证点前移到 `new JSONObject(raw)` 之前，新增 `M4_EVIDENCE_PC_MENUS_RAW_BODY=`。独立探针实测当前本地 token 下 raw body 为空串，随后原方法按预期抛出 `JSONObject text must begin with '{'`。
- v36 取证产物：`.artifacts/working/m4-real-menu-request-logging-v36/App-m4-v36-real-menu-request-logging.jar`，大小 `31,870,823` 字节，SHA-256 `9DBC454856F9A09EEC35603404298EEB42D1BAAECBDF20E491D65C2F5269E66B`。
- v36 取证 ISO：`.artifacts/working/m4-real-menu-request-logging-v36.iso`，大小 `31,934,464` 字节，SHA-256 `2EE09AD403123125677A2FC7690761B2AD9CF848C9FBE6119E4A3922A58D0F56`；ISO 只包含 v36 JAR 和中文 README，不包含 `offline-home.html`。v36 在 v35 基础上，于无参 `SBFApi.k()` 加密请求体生成后、真实 HTTP 调用前打印 `M4_EVIDENCE_PC_MENUS_REQUEST_URL/REQUEST_JSON/REQUEST_BODY/STATIC_A/STATIC_K/STATIC_L/HEADER_E`，用于确认菜单接口空体是否来自请求入参或授权态。
- 宿主机 v36 证据：`C:\m2dump\m4-v36-real-menu-request-probe.log` 显示只手动设置 `SBFApi.a` 时，`k/l` 为空、`JSBFMain.E=null`、菜单 raw 为空；`C:\m2dump\m4-v36-initialized-menu-probe.log` 显示先调用 `SBFApi.j()` 后 `a/k/l` 已由硬件指纹链生成，且手动设置 `JSBFMain.E=offline-local-token-1234567890` 后，菜单请求仍返回空 raw body。结论：菜单空体不是单纯由 `k/l` 未初始化造成，更高置信是服务器不接受本地 fake token/header/signature；正式九产品仍需真实服务器登录态或继续定位真实登录态字段来源。宿主机 `C:\m2dump\app\App.jar` 已恢复 v33 哈希 `24CCC59B18DC97EF05BBD57B46844B7B56F469E48BE1A85DA3A4649DC7957DF5`。
- 真实登录态追踪已闭环：原登录成功响应 `data.token -> StartApp.l -> JSBFMain.E`，产品和菜单接口不存在第二枚隐藏 token。当前没有旧虚拟机、历史账号或历史凭据，新注册账号被“等待审核开通”阻断，原服务端权限也已遗失；该路线正式终止，不再继续猜测或伪造远端认可 token。
- 宿主机 v33 证据：`C:\m2dump\m5-v33-host.log` 显示真实 URL `https://app.xdxsoft.com/pc/aicloud/my?...`、四个定点 Web 初始化接口、`M4_V13_LOAD_FINISHED`；`C:\m2dump\m5-v33-host.err` 不再包含 `JSBFMain.<init>` NPE；`C:\m2dump\host-screen-v33-business.png` 显示 AiCloud 授权码表页面。stderr 仍有后台图标资源 `Stream closed`，但不影响主窗口和 Web 业务页。
- 当前边界：v33 已实现“本地授权/登录/有效期/产品门槛 + 真实在线业务首屏”。当前仅对 Java 授权/产品初始化、Web 路由守卫、页面首屏空表/字典做本地补形状；后续增删改、采集、群发、云手机、投屏、视频等真实业务动作仍应继续联网验证，不应扩大成通用离线业务代理。

## 2026-06-23 里程碑校准

### 总体进度

| 里程碑 | 状态 | 估算 | 当前结论 |
| --- | --- | ---: | --- |
| M1 资产与反编译环境 | 完成 | 100% | 原始资产、备份、源码树和启动入口已建立。 |
| M2 字符串与资源解密 | 完成 | 100% | 明文映射、bootstrap、资源解密和动态验证已完成。 |
| M3 授权接缝定位 | 完成 | 100% | 登录、有效期、产品门槛、Web token 等接缝已明确。 |
| M4 本地授权与完整主界面 | 进行中 | 约 98% | 九产品、菜单、免操作启动、完整双击包和黑洞代理隔离下进入 WhatsApp 主界面壳层已通过；仅缺物理断网/网卡禁用级别的最高强度验收。 |
| M5 真实业务联网回归 | WhatsApp 采集链路评估 | 约 65% | 已形成 M5A 第一版依赖分类、WhatsApp `AI采集` dataCollect 空表链路、spider 配置/DAO/队列边界；v56 已完成 local-only 字段配置、`spider_data` 写入和页面可见 mock 行闭环；AI大脑支线已停止且不作为后续基线。下一步若继续，应设计真实 spider v2 本地队列/M5B 兼容后端；仍禁止直接提交真实采集任务。 |
| M6 业务源码谱与模块文档 | 未正式开始 | 约 10% | 已有分析材料，尚未形成最终模块文档。 |

### v33 已完成

- 本地登录、授权、有效期和产品门槛能够通过。
- JxBrowser 在宿主机和无 GPU VM 中可用软件渲染。
- 真实业务域名、相对 URL 归一化和 Web 登录态入口已经打通。
- AiCloud 在线页面首屏能够显示。
- `JSBFMain` 初始化阶段的 IM 配置 NPE 已解决。

### M4 尚未完成

- 需要在可控 VM 或管理员环境补一次真正禁用网络后的启动、九产品选择器和主界面验收；当前已完成 v50 完整双击包的进程级 HTTP/HTTPS/SOCKS 黑洞代理验收。
- 原项目内 `data\app\HuoChaiAI.exe` 官方启动器会在启动 Java 前覆盖补丁版 `App.dll`，因此不能作为最终补丁入口。v50 交付包改用本地无更新启动器，普通启动和黑洞代理隔离下均验证 `App.dll` 启动前后哈希不变，并能进入九产品选择器；隔离状态下点击 WhatsApp 后窗口标题为 `火柴AI`，左侧 11 项菜单壳层可见。
- 76 个菜单当前统一使用恢复入口 `/pc/aicloud/my`；这是 M4 静态外壳入口，不代表各菜单业务已经恢复。M5A 第一轮已确认该统一入口会阻止 WhatsApp/GEO 真实业务页面验收；后续需基于 `data/app/res/spider/*.cnf`、`JSpiderCloude` 和分发器证据先恢复或定位真实菜单 `localCode/linkUrl`，无法证明的字段必须标为“恢复值”。
- v41/v42 的 WhatsApp `AI采集` 恢复值只证明菜单数据可进入运行时；项目内宿主点击没有触发现代菜单分发、JxBrowser 导航或 XHR。v43-v49 已进一步收敛：真实左侧点击链为 `j2026.d$2 -> d$1`，`JSBFMain$6` 需要菜单子项才会分发；因此当前实现为 `C4749_006` 保持父级菜单，新增恢复值子路由 `REC_WHATSAPP_COLLECT_USERS_ROUTE`，并用窄桥接把该子路由的 `JSinglepage` 占位归一化到 dataCollect URL，再补当前页面所需 `MiJava` 注入和扁平 `getInfo` bridge 权限契约。该实现仍是恢复值，不是原始菜单 JSON。

### 用户提供的产品结构证据

- 可进入系统：WhatsApp AI龙虾系统、TK AI龙虾系统、FB AI龙虾系统、Ins AI龙虾系统、X AI龙虾系统、TG AI龙虾系统、海外GEO AI龙虾系统、WhatsApp AI龙虾客服。
- 独立站 AI龙虾系统保持未开通和“进入官网了解”，当前阶段不实现其业务入口。
- WhatsApp AI龙虾系统截图显示的侧边栏包括：一句话、智能体模型、AI龙虾、超级号、AI采集、AI数据、AI筛选、AI群发、API、广告、AI客服。
- 这些截图是 M4 最终界面结构的验收证据，但不会打断当前里程碑顺序去提前开发各系统业务。

### 后续固定顺序

1. 冻结 v33 技术基线和哈希，不修改其现有产物。
2. M4A：用原包证据和明确恢复值实现 8 个可进入产品、1 个未开通独立站产品及各系统菜单拓扑。
3. M4B：正式化兼容鉴权、产品和菜单服务，实现双击免操作启动、断网进入、九产品卡与主界面验收。v50 已补完整双击包和等价隔离验收；仅物理断网/网卡禁用证据仍可在更高权限环境补做。
4. M5A：逐项分类业务依赖为客户端直连第三方、原后端代理、原后端数据库、云资源/算力和 native 依赖，并验证仍可直连的业务。AI大脑支线停止；当前主线回到 WhatsApp `AI采集`。v52 已做出本地 mock 队列/结果表写入桥，v53 已把它暴露为显式 `window.__m5LocalSpider.seedWhatsAppMockResult()` 页面 hook；下一步应做宿主 local-only 可见性验证，或进入 M5B 兼容后端设计，不提交真实采集任务。
5. M5B：对证明确实依赖遗失服务端的接口逐项重建兼容后端。
6. M6：整理系统、模块、Java 入口、Web 路由、JS bridge、业务 API、native 依赖和兼容后端文档。
