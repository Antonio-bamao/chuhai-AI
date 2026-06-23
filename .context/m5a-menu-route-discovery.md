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

