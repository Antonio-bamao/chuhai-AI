# M5A 菜单真实路由发现记录

> 目标：在不伪造远端菜单响应、不根据截图猜内部字段的前提下，确认原客户端菜单点击时真正消费哪些字段，以及当前 v40 为什么只能进入 AiCloud 外壳。

## 1. 当前结论

- 菜单 JSON 的关键入口字段是 `localCode` 与 `linkUrl`，不是单独的显示 `code`。
- `com.sbf.main.tree.i` 构造菜单项时读取 `id`、`parentId`、`localCode`、`code`、`name`、`icon`、`linkUrl`、`perms`、`treeEndFlg`、`webFlg`、`displayIndex`。
- `com.sbf.main.sub.b.a(i)` 是当前主侧边栏的菜单点击分发器；它根据 `localCode` 或部分 `code` 打开不同 Java/Swing/JxBrowser/native 页面。
- v40 的 `M4RecoveryCatalog.appendMenu()` 目前对 76 个菜单统一写入 `localCode=JSinglepage`、`linkUrl=/pc/aicloud/my`。这能保证九产品和侧边栏稳定显示，但会把所有菜单入口导向 AiCloud 页面，因此不能用于 WhatsApp/GEO 真实业务页面验收。
- 反编译代码和解码表已证明原客户端支持多个真实打开器，但尚未找到 WhatsApp/GEO 每个具体菜单 code 对应的原始 `localCode/linkUrl` 响应值。

## 2. 菜单字段契约证据

来源：`.artifacts/decompiled/cfr-app-20260620-0215/com/sbf/main/tree/i.java` 与 `.artifacts/analysis/string_map.json`。

| 字段 | 源码行 | 含义 |
| --- | ---: | --- |
| `id` | 105 | 菜单 ID。当前 M4A 使用稳定恢复值，不声明为原始真实值。 |
| `parentId` | 106 | 父级 ID。当前 M4A 以产品恢复 ID 为父级。 |
| `localCode` | 107 | 点击分发主字段，决定打开本地 native/Swing/JxBrowser/特殊页面。 |
| `code` | 108 | 菜单业务 code；部分分发器也会按 code 特判。 |
| `name` | 109 | 显示名，原程序会尝试 `menu_title_ + code` 国际化。 |
| `icon` | 120 | 图标资源名；v40 已按 `IconUtil` 消费契约去掉 `svg/` 与 `.svg`。 |
| `linkUrl` | 121 | Web 页面或特殊打开器的 URL/参数。 |
| `treeEndFlg` | 123 | 是否叶子菜单。 |
| `webFlg` | 124 | Web 标记。 |
| `displayIndex` | 127 | 显示排序。 |

## 3. 当前已解出的点击打开器

来源：`.artifacts/decompiled/cfr-app-20260620-0215/com/sbf/main/sub/b.java` 与 `.artifacts/analysis/string_map.json`。

| 触发值 | 字段 | 打开行为 | 当前判断 |
| --- | --- | --- | --- |
| `ZWBrowser` | `localCode` | 解析 `localCode` 中 `:` 分隔参数，创建 `JZWBrowserMaster`；默认参数含 `Quanquke`、`https://www.baidu.com`、`16`、`baidu` | 通用浏览器/云浏览器入口形态，可作为后续恢复 Web 类业务入口的候选形状 |
| `C102017` | `code` | 创建 `com.sbf.main.jxbrowser.ex.c(...)` | 特殊 code 分发，不可迁移到未知菜单 |
| `AiBotChat` | `localCode` | 打开 AI Bot Chat 组件 | 与 WhatsApp/GEO 不直接绑定 |
| `ai_mnq_manager` | `localCode` | 打开 `JOPENFrame("aimnq", JSBFMain.y)` | 云手机/模拟器管理入口证据 |
| `ai_arm_box` | `localCode` | 打开 `JAIARMBoxFrame("aiarmbox", JSBFMain.A)` | AI 安卓智能体入口证据 |
| `TkSpiderPanel` | `localCode` | 打开 TK 采集相关组件 | TikTok 采集入口证据 |
| `JBigDataMaster` | `localCode` | 打开大数据组件 | GEO/大数据类菜单的高价值候选，但尚未能绑定具体 GEO code |
| `JRealAndroidMaster` | `localCode` | 打开真实安卓/设备相关组件 | 本地/云设备类入口证据 |
| `PhoneFission` | `localCode` | 打开号码裂变相关组件 | 号码/裂变类入口证据 |
| `JSinglepage` | `localCode` | 创建 `com.sbf.main.sub.m(linkUrl, code)` | 当前 v40 保守统一入口；适合显示外壳，不适合宣称真实业务恢复 |
| 其他非空 `linkUrl` | 默认路径 | 创建 `com.sbf.main.jxbrowser.c(linkUrl)` | 普通 JxBrowser 页面入口 |

## 4. 产品级入口与菜单级入口的区别

`JSBFMain` 中还存在产品/命令级分发值：

| 值 | 证据 | 说明 |
| --- | --- | --- |
| `wskefu -> kefu` | `JSBFMain.java` 约 843-844 行 | WhatsApp 客服产品会被映射到客服入口，不等于普通 WhatsApp 采集菜单。 |
| `AigcAgent -> aigc` | `JSBFMain.java` 约 927-928 行 | AIGC 产品级入口。 |
| `OperMarket -> getcustomer` | `JSBFMain.java` 约 962-964 行 | 获客/采集类产品级入口，可能承载部分多平台采集能力。 |
| `rpa`、`aicloud`、`kefu`、`rpamarkting` | `JSBFMain.java` 约 1052-1139 行 | 原程序产品/模块级入口，不是八系统菜单 code 本身。 |

结论：`whatsapp/tiktok/facebook/instagram/twitter/telegram/geo/wskefu` 是产品选择器 code；真正进入业务页后，侧边栏菜单还需要独立的 `localCode/linkUrl/code` 契约。不能把产品 code 直接当菜单路由。

## 5. 本轮没有填入代码的原因

- 对 WhatsApp 重点 code（如 `C4749_006/007/009/005/011`）和 GEO 重点 code（如 `C4134_002/003/006`、`C4137_001..006`）做过反编译源码、解码表、`.context`、测试和 patcher 全局检索；当前只在 M4 恢复目录与证据文档中出现，没有发现原始 `linkUrl` 或 `localCode` 响应残留。
- 原真实菜单接口在本地 fake token 下返回空体；原产品接口返回 401。没有真实服务器 token、历史账号或旧环境，所以不能取得原始菜单 JSON。
- 因此本轮只能把“分发器支持的打开器”和“字段契约”沉淀为证据，不能把某个未知菜单强行绑定到 `JBigDataMaster`、`ZWBrowser` 或某条 URL。

## 6. 下一步建议

1. 继续从 Web 前端 chunk、资源包和缓存目录查找可证明的业务路由字符串，例如 `/pc/dataCollect/collectionTask`、`/pc/...`、`bigdata`、`collectionTask` 等。
2. 先做高优先只读入口：WhatsApp `AI采集/AI数据/AI筛选/API/AI客服`，GEO `全球号码/全球企业/海关数据`。
3. 若只能证明打开器类别但不能证明具体 URL，允许使用“恢复值”方式建立最小候选入口，但必须：
   - 在 `m4-product-menu-evidence.md` 或本文件标注为恢复值；
   - 不声明为原始真实值；
   - 先用测试保证可集中替换；
   - 只做页面打开和请求记录，不执行群发、批量采集、上传或云设备创建。

## 7. 2026-06-23 本地 spider/dataCollect 入口发现

本轮从包内资源和反编译代码继续向下找，确认原客户端保留了可读的本地采集脚本资产和数据采集页面入口。

### 7.1 本地 spider 配置资产

来源：`data/app/res/spider/*.cnf`。

| 平台 | 已确认配置 | 关键含义 |
| --- | --- | --- |
| WhatsApp | `whatsapp_users_lists.cnf`、`whatsapp_regional_collection.cnf`、`whatsapp_group_lists.cnf`、`wap_global_clue_users.cnf` | 包含 WhatsApp/全球线索采集配置，`moduleCode=whatsapp`，`homeUrl=https://www.google.com`，通过 Google 搜索和脚本步骤抽取号码/链接/地区等字段。 |
| TikTok | `tiktok_active_peers.cnf`、`tiktok_live_broadcast.cnf`、`tiktok_peers_details_dsp.cnf`、`tiktok_peers_video.cnf`、`tiktok_popular_account.cnf`、`tiktok_popular_videos.cnf`、`tiktok_user_fans.cnf`、`tiktok_user_following.cnf`、`tiktok_user_message_dsp.cnf` | 包含 TikTok 用户、视频、直播、评论、粉丝/关注等直连页面或 API 的采集配置。 |
| Facebook | `facebook_designated_group.cnf`、`facebook_group_comments.cnf`、`facebook_group_members.cnf`、`facebook_home_page.cnf`、`facebook_homepage_basics.cnf`、`facebook_peer_interception.cnf`、`facebook_posts_collection.cnf`、`facebook_specify_friend.cnf` | 包含 Facebook 群组、主页、帖子、好友/成员等页面与 GraphQL/API 入口配置。 |

WhatsApp 重点配置摘要：

| 配置 | `id` | `name` | `moduleCode` | 主要参数 | 输出字段 |
| --- | ---: | --- | --- | --- | --- |
| `whatsapp_users_lists` | 24 | `WS线索采集` | `whatsapp` | `googSite,areaCode,pltCode,keywords` | `googSite,pltCode,keywords,phone,date,url` |
| `whatsapp_regional_collection` | 41 | `WS地区采集` | `whatsapp` | `adds,keywords,isfilter,get_range` | `keywords,city,name,phone,links,oloc,date,adds` |
| `whatsapp_group_lists` | 25 | `WS小组采集` | `whatsapp` | `googSite,pltCode,keywords` | `googSite,pltCode,keywords,group_link,url,date` |
| `wap_global_clue_users` | 20 | `全球线索采集` | `whatsapp` | `googSite,areaCode,pltCode,keywords` | `googSite,pltCode,keywords,phone,date,url` |

判断：

- 这些 `.cnf` 是原包明文业务资产，能证明部分采集逻辑不是完全遗失在服务端。
- 其中 URL 大量指向 Google、TikTok、Facebook 等第三方页面/API，属于“客户端直连第三方 + 本地脚本自动化”的强证据。
- 同时脚本内仍出现 `spider.postData(...)`、`spider.base64TOss(...)`、验证码/代理/结果处理等调用，说明任务创建、结果保存、OSS 上传或辅助识别可能仍依赖原后端或云资源。

### 7.2 Java 数据采集入口

来源：`.artifacts/decompiled/cfr-app-20260620-0215/com/sbf/main/spide/cloud/JSpiderCloude.java`、`.artifacts/decompiled/cfr-app-20260620-0215/com/sbf/util/http/SBFApi.java` 与 `.artifacts/analysis/string_map.json`。

| 位置 | 解码值/行为 | 结论 |
| --- | --- | --- |
| `JSpiderCloude.java` 约 295-303 行 | 根据 tab 字段创建页面；其中一个分支构造 `/pc/cloud/task/myindex?spiderCode=...`，另一个分支构造 `/pc/dataCollect/collectionTask/data_index?spiderCode=...&moduleCode=...` | 原客户端确实存在云任务页和数据采集任务页入口。 |
| `JSpiderCloude.java` 约 500-525 行 | 使用 `/app/spider/<code>/ver.ini`、`ver`、`fileMd5`、`ossUrl`、`code` 做 spider 包版本检查/下载 | spider 脚本可本地缓存，但更新源和包分发依赖远端/OSS。 |
| `SBFApi.H(String)` 约 1551-1564 行 | 先请求 `/cloud/spider/code/<code>`，成功后写入本地 `/res/spider/<code>.cnf`；异常时读取本地 `/res/spider/<code>.cnf` | 这是“远端配置优先、本地配置兜底”的明确契约，当前本地 `.cnf` 可作为兼容恢复基础。 |
| `SBFApi.java` 约 1766、1782、1804、1815 行 | `/api/v1/client/pc/spider/v2/upstatus/`、`get/`、`cancelAllRun/`、`getNewTask/` | spider 运行状态、任务拉取、取消、任务获取仍有原后端任务队列/状态接口。 |
| `sub.g.java` 解码值 | `JBigDataMaster`、`PhoneFission`、`ThePlugIn`、`WapFilter`、`WSFilter`、`pc/dataCollect/collectionTask` | 侧边栏/子分发器中存在数据采集相关打开器线索，但还不能把它直接绑定到每个 WhatsApp 菜单 code。 |

判断：

- 可把 `pc/dataCollect/collectionTask` 作为高置信“数据采集入口族”证据。
- 还不能把 v40 的某个 WhatsApp 菜单直接改成该入口并声明“原始真实路由”，因为当前没有原始菜单 JSON 绑定关系。
- 后续若建立候选入口，应标注为“恢复值”，例如把 `spiderCode=whatsapp_users_lists`、`moduleCode=whatsapp` 作为可替换恢复参数，并只做页面打开/请求记录，不执行采集任务。

## 8. 2026-06-23 v41 WhatsApp AI采集只读入口候选

本轮按 TDD 建立了第一个可集中替换的恢复路由候选，只覆盖 WhatsApp 的 `AI采集` 菜单。

| 字段 | v41 恢复值 | 证据等级 |
| --- | --- | --- |
| 菜单 `code` | `C4749_006` | 原包 i18n/菜单恢复 code，已在 M4A 使用。 |
| 菜单名 | `AI采集` | 用户截图验收 + 原包菜单资源恢复。 |
| `localCode` | `pc/dataCollect/collectionTask` | 恢复值；来源于 `sub.g`/`f.d`/`JSBFMain$4` 解码字符串和数据采集入口族证据，不声明为原始菜单响应值。 |
| `linkUrl` | `/pc/dataCollect/collectionTask/data_index?spiderCode=whatsapp_users_lists&moduleCode=whatsapp` | 恢复值；`/pc/dataCollect/collectionTask/data_index` 来自 `JSpiderCloude`，`whatsapp_users_lists` 来自本地 spider 配置。 |
| `evidence` | `recovery-route:dataCollect:whatsapp_users_lists` | 显式标注为恢复路由，避免误当原始真实值。 |

验证：

- 新增测试先失败，失败点为当前 `AI采集` 仍是 `JSinglepage + /pc/aicloud/my`。
- 实现后完整 `python -m unittest discover -s tests -v` 通过 `27/27`。
- 生成候选产物 `.artifacts/working/m5a-whatsapp-collect-route-v41/App-m5a-v41-whatsapp-collect-route.jar`，大小 `31,880,904` 字节，SHA-256 `661AD0474127637FF3890DB61B95A6EAE66D09DA41C82C217BD334E3C5FA10FE`。
- 产物级检查确认 `SBFApi.class` 内含 `whatsapp_users_lists`、`/pc/dataCollect/collectionTask/data_index`、`recovery-route:dataCollect:whatsapp_users_lists`。

边界：

- v41 只恢复一个候选入口，不代表 WhatsApp 采集任务已可提交或结果已可保存。
- 暂不修改 `AI数据`、`AI筛选`，因为当前证据不足以把它们分别绑定到结果页或筛选器实现。
- 下一步只允许宿主机/VM 打开 `AI采集` 页面并记录请求、错误和渲染结果；不得提交关键词、创建采集任务、批量采集、上传或群发。
