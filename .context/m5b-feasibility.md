# WhatsApp AI采集 真实联网可行性评估

日期：2026-06-25

## 边界

本评估只做本地静态调研和既有 `.context` 证据复核；未实现真实采集，未修改 spider 执行逻辑，未访问 Google 或任何目标站，未触发 `getNewTask/upstatus/cancelAllRun/submit/save/toClearDataAll/toPackageDowloadData`。

评估目标：判断“跑通一条真实 WhatsApp AI 采集”需要哪些原依赖、哪些依赖还在本地、哪些已经落在原后端/云资源里，以及纯客户端单机是否能替代。

## 一句话总判

“纯客户端单机能否跑通一条真实采集” = **能但需本地重建最小任务队列/任务详情、代理输入或直连策略、验证码识别替代**；在这些重建前，当前客户端只能证明“dataCollect 页面 + 本地字段配置 + 本地 `spider_data` 读写 + mock 行可见”，**还不能跑通真实采集**。

更保守地说：如果目标站触发 reCAPTCHA，而验证码替代没有先接上，则实际运行会卡住；如果直连 Google 不可用或风控过高，还需要可用代理来源。

## 逐项可行性表

| 环节 | 原实现依赖什么 | 依赖是否在本地/丢失服务器 | 本地能否替代 | 替代成本(低/中/高) | 结论(可做/需重建/拿不到) |
| --- | --- | --- | --- | --- | --- |
| 1. 任务创建与下发：`getNewTask` 的真实来源 | `SBFApi` 的 spider v2 队列接口：`/api/v1/client/pc/spider/v2/getNewTask/` 拉取任务数组，`upstatus/get/cancelAllRun` 做状态、详情、取消；`JSpiderCloude$9` 创建任务时组装 `spiderCode/spiderParams/cloudServer/moduleCode/taskConfig`，成功后继续组装 `taskId/data/spiderMode/cookie/proxy/spider_app_code/spider_exe_code`。 | **真实来源在原后端任务队列/数据库**。本地已有 v52/v56 的 local-only mock 桥，但它固定空队列或写 mock 行，不是真实任务来源。 | 能替代。需要做一个契约兼容的本地最小队列：生成 `taskId`，保存任务参数，提供 `getNewTask/get/upstatus/cancelAllRun` 语义，并向 spider 宿主提供同形运行 JSON。 | 中 | **需重建**。这是跑真采集的第一前置。 |
| 2. spider 宿主执行 `.cnf`：本地 `libmytrpc`/原生库是否自包含可跑 | 本地 Java 宿主链：`JCloudSpiderMaster` 持有队列并启动执行；`com.sbf.main.cloud.spider.b` 基于 JxBrowser 注入 `window.mijava` 和 `window.spider`；`.cnf` 通过 `spider.doHref/toCheckDom/postData/endTask` 和 `mijava.*` 执行业务脚本。运行还依赖 JxBrowser、Selenium/Playwright、多个 `libmytrpc*` 原生库、任务运行参数。 | **宿主类、`.cnf`、JxBrowser/自动化 jar、`libmytrpc*` 原生库在本地**；但真实任务参数、状态队列、代理/cookie 等运行上下文来自任务系统，当前没有原后端。 | 部分能替代。宿主和脚本资产可复用；不应改 spider 执行逻辑，而应用本地队列喂同形任务数据。是否所有原生库都能完整加载，仍需后续隔离运行验证；本轮未运行。 | 中 | **可做但依赖本地任务队列重建**。宿主不是最大丢失点，任务输入和外部依赖才是。 |
| 3. 目标站访问：是否必须代理池、代理原来从哪来 | WhatsApp 线索类 `.cnf` 均以 `https://www.google.com` 为起点，按 `googSite/areaCode/pltCode/keywords` 拼 Google 查询，例如 `site:<pltCode> <keywords> intext:whatsapp <areaCode>`；区域采集也走 Google/地图路线。代理值在任务运行 JSON 中出现为 `proxy`，客户端侧存在 `socks5://...`、`socksProxyHost/socksProxyPort` 解析设置。 | **目标站访问不是本地依赖**。代理池/代理来源未在本地发现为完整池；更像由原后端任务、账号或云任务配置下发。客户端能接收/使用 SOCKS5 代理，但没有证明本地自带可用代理池。 | 可以替代成“用户提供单个 SOCKS5 代理或直连策略”。跑一条低频采集可先做手动代理输入；要稳定跑，需要代理池、区域策略、失败重试和风控判断。 | 中到高 | **需重建/外部提供**。单机不自带可用代理来源；直连是否可行本轮未访问验证。 |
| 4. 验证码识别：是否远端 AI 服务、本地有无替代 | WhatsApp 相关 `.cnf` 都包含 reCAPTCHA 处理：检测 `#recaptcha/#recaptcha-demo`，抓图转 base64，调用 `mijava.creatGoogleCRTask(base64, qid, callback)`；回调结果按 `res.solution.objects` 或 `res.solution.hasObject` 点击图片块。`MiJava.creatGoogleCRTask` 启动异步线程并把结果回调给 JS。 | **识别能力本地不完整**。本地有 OpenCV/Tesseract/Javacv jar，但 reCAPTCHA 图片块目标识别不是普通 OCR；未发现可离线完成 `solution.objects/hasObject` 的本地模型、题库或服务。原逻辑高概率依赖远端 AI/打码识别服务。 | 可替代，但不是低成本：接第三方验证码服务、本地视觉模型，或为单条验证做人工辅助回调。要保持原脚本自动化，需要返回同形 JSON。 | 高；人工/第三方服务可降到中 | **需重建**。只要触发验证码，这就是真实采集的关键阻断点。 |
| 5. 结果回写：`JSpiderCloude.a` / `spider_data` | `.cnf` 采到数据后调用 `spider.postData(JSON.stringify(data))`；宿主侧 `SpiderCallback.postData` 委托到 spider；`JSpiderCloude.a(JSONObject)` 创建 `JSpiderData`，设置 `spiderCode/spiderModal/time/jsonData` 并 DAO `addOrUpdate`，随后 `window.reloadData`；`MiJava$162/getSpiderDataList` 读取 SQLite `spider_data` 分页返回 `total/rows`。 | **本地具备**。v52/v56 已证明本地 `spider_data` 可写入并被 dataCollect 页面刷新看见；SQLite 文件形状已知：`data/whatsappdata/db_spider_data_whatsapp_users_lists.data`，表 `spider_data(spider_modal, spider_code, json_data, time, id)`。 | 能替代，而且已经有 local-only 证据。真实采集只要沿原 `spider.postData` 形状进入，即可落本地 DAO。 | 低 | **可做**。这是已知可行项，不是主要阻断点。 |
| 6. OSS/文件上传等附带依赖 | spider 包更新使用 `/app/spider/<code>/ver.ini`、`ver/fileMd5/ossUrl/code`；`SpiderCallback.base64TOss` 可把 base64 图片上传 OSS；本地 lib 含 Aliyun OSS、AWS S3 SDK。部分其他采集脚本出现 `spider.base64TOss(...)`。 | **SDK 在本地，凭据/桶/远端分发不在本地可靠可用**。WhatsApp 线索主线 `.cnf` 当前未直接调用 `base64TOss`，但验证码识别会上传 base64 给识别逻辑；包更新和附件上传仍偏云资源。 | 对“跑一条 `whatsapp_users_lists` 真实线索采集”可先冻结本地 `.cnf`，不依赖远端包更新；若某脚本必须上传文件，可替换为本地文件存储或新 OSS 凭据。 | 低到中 | **主线可绕开；上传能力需重建**。不应作为第一阻断，但要在后续兼容层 fail-open。 |
| 7. spider 配置读取：`getCloudSpiderConfig` | `MiJava.getCloudSpiderConfig` 经 `SBFApi.H(spiderCode)` 先请求 `/cloud/spider/code/<code>`，成功后写本地 `/res/spider/<code>.cnf`；异常时读取本地同名 `.cnf`。 | **本地有兜底配置**。`data/app/res/spider` 下已存在 `whatsapp_users_lists.cnf`、`whatsapp_group_lists.cnf`、`whatsapp_regional_collection.cnf`、`wap_global_clue_users.cnf` 等。 | 能替代。M5B 最小实现可以直接使用本地缓存 `.cnf`，暂不恢复远端配置分发。 | 低 | **可做**。配置不必第一阶段重建远端服务。 |
| 8. dataCollect 页面与结果展示 | `data_index` 首屏只调用 `getCloudSpiderConfig` 和 `getSpiderDataList`，按钮是下载、清空、刷新；没有新增/提交任务按钮。 | **本地已可打开并展示 mock 结果**。但它只是结果页，不负责真实任务创建。 | 能替代为结果查看页；任务创建需要另建入口或兼容云任务页。 | 低 | **可做**。它不是采集启动入口。 |

## 最小重建清单和优先级

### P0：没有就无法开始真实采集

1. **本地最小任务队列/任务详情**
   - 实现契约兼容的 `getNewTask/get/upstatus/cancelAllRun` 本地语义。
   - 任务对象至少包含：`taskId`、`moduleCode=whatsapp`、`spiderCode=whatsapp_users_lists`、`data/spiderParams`、`spiderMode`、`cookie`、`proxy`、`spider_app_code`、`spider_exe_code`。
   - 先支持单任务、单并发、明确状态流转：pending -> running -> success/failed/canceled。

2. **本地任务创建/投喂入口**
   - 因 `data_index` 没有提交按钮，需要选择：兼容原云任务页，或做一个 local-only 显式入口。
   - 第一版只允许用户显式输入 `googSite/areaCode/pltCode/keywords`，不自动采集。

3. **验证码回调替代**
   - 最小目标不是通用 AI，而是返回 `.cnf` 期待的 JSON：`solution.objects`、`solution.hasObject`。
   - 可选路线：人工辅助回调、第三方打码服务、本地视觉模型。自动化本地模型成本最高。

### P1：决定能否稳定访问目标站

4. **代理/直连策略**
   - 第一版允许用户手工填 `socks5://host:port` 或选择直连。
   - 不先做代理池；等真实隔离验证后再决定是否需要池化、轮换和地域策略。

5. **运行前隔离和开关**
   - 必须保留“预览任务/不运行”的 dry-run。
   - 真采集前要有显式确认，避免误触发 Google、验证码、代理或上传链路。

### P2：不阻断第一条，但要降级

6. **OSS/文件上传降级**
   - 第一条 `whatsapp_users_lists` 可冻结本地 `.cnf`，跳过远端 spider 包更新。
   - 若执行路径触发 `base64TOss`，先改为本地文件存储或 fail-open，再决定是否接新 OSS。

7. **状态和日志可观测**
   - 记录任务参数、是否使用代理、是否触发验证码、验证码处理方式、`postData` 写入数量、结束状态。
   - 这是后续判断“直连可用/代理必需/验证码服务质量”的依据。

## 决策建议

如果下一步只想证明“客户端能不能真的跑一条”，最小路径应是：**先重建本地单任务队列 + 手动任务投喂 + 结果沿原 `postData` 写本地表**，同时把代理和验证码做成显式可插拔项。不要先重建完整云任务后台、OSS 分发或代理池。

如果不准备接验证码替代，也不准备提供可用代理/直连条件，则本阶段不建议启动真实采集；否则很可能只证明“脚本能打开宿主”，而不是证明“采集可用”。
