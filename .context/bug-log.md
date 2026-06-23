# Bug / 工程异常记录

> 所有会影响推进、质量、节奏或判断的异常都要记录，包括代码、环境、依赖、测试、打包和设计误判。

## PowerShell New-Item LiteralPath 参数不兼容
- 现象：创建备份目录时 New-Item 报错 A parameter cannot be found that matches parameter name LiteralPath，后续 Copy-Item 因目标目录不存在失败。
- 触发条件：执行 M1-01 备份脚本时使用 New-Item -ItemType Directory -Force -LiteralPath。
- 影响：备份目录未创建，复制未发生；未修改原始资产。
- 根因：当前 PowerShell 环境的 New-Item 参数集不支持 -LiteralPath，脚本假设了较新的参数兼容性。
- 解决方案：改用 New-Item -Path 创建目录，并在复制前显式检查目标目录存在。
- 预防措施：后续目录创建优先使用兼容性更好的 -Path；对备份脚本增加 Test-Path 检查。
- 状态：resolved

## Windows 控制台无法打印部分解码 Unicode
- 现象：筛选授权候选时，直接 print 部分 decoded 字符串触发 UnicodeEncodeError: gbk codec cannot encode character。
- 触发条件：在 PowerShell 控制台打印 string_map 中包含特殊 Unicode 或未完全可读字符的 decoded 字段。
- 影响：控制台预览提前中断，但 JSON 文件输出已成功，分析数据未丢失。
- 根因：Windows 当前控制台编码为 GBK，不能表示部分 UTF-16/Unicode 字符。
- 解决方案：将完整结果写入 UTF-8 JSON；控制台预览改用 ASCII/转义输出或只打印统计。
- 预防措施：后续不要把未筛洗的 decoded 字段直接打印到控制台；报告引用 JSON 路径和统计。
- 状态：resolved

## bootstrap 候选初筛混淆 Java 重载方法
- 现象：初版 `startapp_bootstrap_candidates.json` 把 `StartApp.i(String)` 第 478-479 行的 `HashMap` 缓存操作归到 `StartApp.i()` 候选里。
- 触发条件：`tools/decode_bootstrap_calls.py` 只记录方法名，不记录参数签名；`StartApp` 同时存在 `i()` 与 `i(String)` 重载。
- 影响：若直接进入 M3，会把不相关重载方法的缓存删除行为混入定时任务候选。
- 根因：方法上下文模型粒度过粗，只用 `caller` 字段不足以区分 Java 重载。
- 解决方案：为 bootstrap 解码结果新增 `caller_signature`、`caller_method`、`caller_method_line` 字段；重生成候选时按签名与行段过滤。
- 预防措施：后续所有接缝候选引用动态调用时同时看方法签名和源码行段，不只看方法名。
- 状态：resolved

## 误把 token action contains 分支描述为 equals
- 现象：阶段文档一度写成参数等于 get_current_token 或 getLoingIsToken，进而把 StartApp.f 入参误描述为纯 action 字符串。
- 触发条件：只核对了解密后的 action 明文和 StartApp.f 调用目标，没有同时核对同一源码行的比较函数 bootstrap 目标。
- 影响：会误导后续 URL 语义判断，使看似无协议的 action 字符串与 OkHttp URL 要求产生矛盾。
- 根因：混淆代码中的比较操作也由 invokedynamic 隐藏，CFR 外观不能代替 bootstrap 描述符证据。
- 解决方案：核对 AdsCallback.java:191 与 MiJava.java:1553，确认比较目标均为 String.contains(CharSequence)；同步修正文档，并还原 RT/rdtime 拼接。
- 预防措施：今后确认混淆分支语义时同时验证三项：明文字面量、比较函数 bootstrap 目标、被调用方法描述符。
- 状态：resolved

## CFR 将 dup 栈值误还原为第二次 new n()
- 现象：n.a(String,String) 被反编译为创建两个 n 实例，看起来 URL 字段 c 被写入一个随后丢弃的对象。
- 触发条件：追踪 n.c() 刷新时发现若按 CFR 输出理解，返回缓存实例没有 URL，和实际刷新设计矛盾。
- 影响：可能错误判断定时刷新不可用，或把字段归属和补丁候选定位到错误对象。
- 根因：混淆字节码使用 new/dup/dup/astore 的栈操作，CFR 0.152 在该方法上把同一对象引用错误展示为第二次构造。
- 解决方案：直接解析 App.jar 中 n.class 的 Code 属性，确认只有一次 new，参数 c/d 写入同一返回实例。
- 预防措施：遇到反编译结果与控制流不变量矛盾时，必须回到原始字节码核对 new/dup/astore/putfield 指令。
- 状态：resolved

## Windows sandbox tempfile ACL breaks Python tests
- 现象：TemporaryDirectory children reject writes, and Python cleanup of manually created test directories is denied under the managed workspace.
- 触发条件：Running unit tests that create and delete temporary directories with tempfile or shutil.
- 影响：Tests fail for sandbox-environment reasons unrelated to production behavior.
- 根因：Managed Windows sandbox ACL behavior makes Python-created temporary directories and Python deletion unusable in this workspace.
- 解决方案：Replaced runtime temporary directories with committed read-only fixtures and removed cleanup requirements.
- 预防措施：Use committed fixtures or persistent artifact outputs for tests; avoid tempfile and Python recursive deletion in this managed workspace.
- 状态：resolved

## Windows sandbox blocks py_compile cache replacement
- 现象：python -m py_compile writes a temporary .pyc but receives WinError 5 when atomically replacing the final __pycache__ file.
- 触发条件：Compiling tools/hash_tree.py with python -m py_compile under the managed Windows workspace.
- 影响：Bytecode-file verification fails despite valid Python syntax.
- 根因：Managed sandbox ACL permits temporary creation but denies the os.replace operation used by py_compile.
- 解决方案：Verify syntax with Python compile(source, filename, exec), which performs the same parser/compiler check in memory without filesystem replacement.
- 预防措施：Use in-memory compile checks for Python files in this managed workspace and reserve py_compile for an unrestricted environment.
- 状态：resolved

## Shared scanner changed bootstrap caller attribution
- 现象：The first shared-scanner refactor kept 73,600 bootstrap rows but changed caller metadata in 16,123 rows.
- 触发条件：Replacing decode_bootstrap_calls.py method tracking with the broader static-string method declaration parser.
- 影响：Historical bootstrap output was no longer byte-for-byte reproducible, especially around constructors and method-line metadata.
- 根因：The two legacy decoders used intentionally different method-declaration regexes; the bootstrap parser required a modifier and return type and therefore did not recognize constructors.
- 解决方案：Added a legacy bootstrap method-detection profile to the shared scanner and made the bootstrap decoder select it explicitly.
- 预防措施：Require exact JSON regression comparisons for every refactor of source-location or caller-attribution logic.
- 状态：resolved

## Typed method named like class misclassified as constructor
- 现象：Calls inside a static void a(...) method in class a were attributed to caller <init>, producing random Unicode instead of plaintext.
- 触发条件：The shared scanner converted every declaration whose method name equaled the source filename into <init> without checking for a return type.
- 影响：Caller-hash string decryption was wrong for legal typed methods named like their class, including high-value family f.w.
- 根因：Constructor detection relied only on name equality rather than a constructor-specific declaration grammar.
- 解决方案：Added a separate constructor parser and retained typed same-name methods as ordinary methods; added a regression fixture.
- 预防措施：Test constructor parsing separately from methods whose names equal their class, and require semantic plaintext samples for caller-hash decoders.
- 状态：resolved

## Plaintext arguments were passed through encrypted-literal decoder
- 现象：Expanded decoding produced two more rows than the encrypted-call inventory because literal values ')' and 'w' were treated as ciphertext.
- 触发条件：Per-family call regexes accepted any one-string argument while the inventory correctly required at least one Unicode escape.
- 影响：Existing plaintext parameters could be transformed into meaningless output and inflate coverage counts.
- 根因：decode_java_strings.py lacked the inventory's Unicode-escape eligibility guard.
- 解决方案：Require at least one \\uXXXX escape before static decryption; reserve plain literals for decoded_existing_plaintext export status.
- 预防措施：Use one shared encrypted-literal eligibility rule and assert map counts equal decoded_static inventory call counts.
- 状态：resolved

## CFR lexical caller produced invalid surrogate plaintext
- 现象：UTF-8 export failed because 660 initially decoded rows contained isolated UTF-16 surrogates; other rows contained random Unicode despite a verified decoder family.
- 触发条件：Using the enclosing CFR method name as the runtime stack-trace caller for calls folded into lambda expressions.
- 影响：The algorithm and constants were correct but the caller hash was wrong, so invalid output could be mislabeled decoded_static.
- 根因：CFR folds synthetic lambda methods into lexical source scopes, while the original classfile retains obfuscated synthetic method names used by StackTraceElement.
- 解决方案：Added a minimal classfile method reader and conservative caller resolver; high-confidence candidates are selected by text validity/score and ambiguous calls are downgraded to dynamic_dump_required.
- 预防措施：Never accept caller-hash plaintext containing surrogates or control characters; retain lexical and selected caller evidence and export unresolved calls separately.
- 状态：resolved

## Threadtear JarIO silently swallowed missing output directory
- 现象：The first bytecode-only run printed an output path even though JarIO.saveAsJar had caught FileNotFoundException and produced no JAR.
- 触发条件：Running the minimal Threadtear driver before creating .artifacts/deobfuscated.
- 影响：A wrapper could falsely report successful deobfuscation when no output exists.
- 根因：Threadtear 3.0.1 JarIO.saveAsJar returns void and catches IOException internally instead of propagating failure.
- 解决方案：The driver now creates the output parent first and verifies that the output is a non-empty file after saveAsJar.
- 预防措施：Treat third-party void save APIs as untrusted; verify output existence, size, ZIP readability and required entries before reporting success.
- 状态：resolved

## Dynamic dump agent JAR hash changed after rebuild
- 现象：复跑 javaagent 打包后，`dynamic-string-dump-agent.jar` 的 SHA-256 从早先记录的 `45CA60...` 变为 `DDB0DD...`。
- 触发条件：使用 JDK `jar` 重新打包相同 class 文件。
- 影响：若不复核，会导致 VM 手册中的 `certutil -hashfile` 期望值与实际 ISO 内容不一致。
- 根因：标准 JAR 打包会写入 ZIP entry 时间戳等元数据，未配置可复现构建时哈希不稳定。
- 解决方案：以复编译后合成 smoke 通过的 JAR 为准，更新 README/runbook 哈希，重新制作 ISO，并再次验证 ISO 内容与哈希。
- 预防措施：每次重新打包 agent 后必须重新计算 agent 与 ISO SHA-256；不要复用旧哈希。
- 状态：resolved

## Offline dynamic dump ISO omitted App runtime libraries
- 现象：离线 Windows VM 中 javaagent 已输出 `target classes=5` 和 `output=C:\dump\strings.jsonl`，随后 `App.jar` 启动失败：`NoClassDefFoundError: com/teamdev/jxbrowser/callback/Callback`。
- 触发条件：使用只包含 `App.jar`、agent、targets、Threadtear 和 JRE 的瘦 ISO，直接运行 `java -jar App.jar`。
- 影响：agent 已挂载但 App 主类依赖解析失败，critical 动态 dump 不能继续。
- 根因：`App.jar` manifest 的 `Class-Path` 依赖原始 `../lib/...` 布局，尤其需要 `jxbrowser-7.41.3.jar`、`jxbrowser-swing-7.41.3.jar`、`jxbrowser-win64-7.41.3.jar` 等运行库；瘦 ISO 没有提供原始 `app/` + `lib/` 布局。
- 解决方案：生成 `.artifacts/dynamic-dump-package-full.iso`，包含原始 `app/` 资源、完整 `lib/` 目录、JRE、agent、targets 和 `RUN-CRITICAL.cmd` / `RUN-HIGH.cmd`，并更新 runbook 使用 `app\App.jar`。
- 预防措施：动态运行包必须保留原始 Java 启动布局；不要用只含主 JAR 的瘦包验证桌面 App。
- 状态：resolved

## Dynamic dump record hook propagated observer failures
- 现象：离线 Windows VM critical run 在 `-noverify` 后成功 instrument `com/sbf/main/JSetupDialog$JLoginNew`，随后因 `DumpHooks.escape(...)` / record path 的 `NullPointerException` 终止主程序。
- 触发条件：agent 在目标 decoder 返回路径记录输入/输出时遇到 null 或记录链异常。
- 影响：观察逻辑反向影响被观察 App，导致 critical 动态 dump 不能继续到 UI 截图阶段。
- 根因：`DumpHooks.record(...)` 原实现直接执行 JSON 输出，记录异常会传播回被插桩的目标方法；脚本也未默认使用 `-noverify`，需要用户手动输入长命令。
- 解决方案：新增回归测试，保证 `record(...)` 不传播 writer failures；将 record emission 包在 guarded block 中；重建 agent；`RUN-CRITICAL.cmd` / `RUN-HIGH.cmd` 默认加入 `-noverify`；生成 `.artifacts/dynamic-dump-package-full-v2.iso`。
- 预防措施：javaagent 观察代码必须 fail-open；运行脚本固化已验证的 JVM 参数，避免在 VM 手动拼长命令。
- 状态：resolved

## Dynamic dump stack-shuffle hook crashed JVM under noverify
- 现象：离线 Windows VM v2 critical run 打印多条 `record failed: java.lang.NullPointerException` 后 JVM fatal crash：`EXCEPTION_ACCESS_VIOLATION`，problematic frame 指向 `java.lang.String.length()`。
- 触发条件：使用 `-noverify` 运行含 `DUP`/`SWAP` 栈操作的返回值插桩。
- 影响：VM 内 Java 进程直接崩溃，无法继续到 UI 截图或 dump 检查阶段。
- 根因：原插桩依赖栈顶重排并跳过 verifier，在混淆目标方法返回路径上不够稳健；`-noverify` 将本应被 verifier 拦下的风险推迟成运行时 JVM 崩溃。
- 解决方案：改为 verifier-friendly 的本地变量方案：`ASTORE 1` 保存返回值，加载原入参、保存的返回值和 family 调用记录函数，然后 `ALOAD 1` 恢复返回值并执行原 `ARETURN`；移除 run scripts 中的 `-noverify`；生成 `.artifacts/dynamic-dump-package-full-v3.iso`。
- 预防措施：字节码插桩优先使用局部变量和正常 verifier 路径，禁止用 `-noverify` 掩盖 frame/stack 问题。
- 状态：resolved

## Dynamic dump v3 still assumed target local slot 0 held input
- 现象：离线 Windows VM v3 critical run 仍报 `VerifyError: Incompatible argument to function`，位置在 `JSetupDialog$JLoginNew.N(String):String`。
- 触发条件：v3 插桩在返回点读取 `ALOAD 0` 作为原始输入。
- 影响：混淆方法若复用 local slot 0，返回点的 slot 0 不再保证是 `String`，verifier 继续拒绝目标类。
- 根因：v3 自审不足，只移除了 `SWAP` / `-noverify`，但仍保留“local 0 等于入参”的错误假设；混淆器可以合法复用参数槽。
- 解决方案：v4 改成 output-only hook：`DUP` 返回值，调用 `DumpHooks.recordOutput(output, family)`，不读取目标方法任何 local slot；新增 verifier-enabled javaagent smoke 测试。
- 预防措施：对混淆字节码插桩时不要读取目标 locals，除非先用 LocalVariablesSorter/AdviceAdapter 在 method entry 保存并重算 frames；动态 dump 优先记录不破坏 verifier 的最小证据。
- 状态：resolved

## M4 patcher rejected a new output path with Files.isSameFile
- 现象：The first implementation of M4AuthPatch failed before writing the patched JAR with java.nio.file.NoSuchFileException on App-m4-auth-patched.jar.
- 触发条件：The red-green M4 unittest invoked M4AuthPatch with a fresh temporary output path.
- 影响：The patcher could not create a new output JAR even though writing to a new artifact path is the required workflow.
- 根因：Files.isSameFile(input, output) requires both paths to exist on Windows; the intended safety check only needed normalized path equality.
- 解决方案：Changed the guard to compare normalized absolute Path values with input.equals(output), preserving same-path protection while allowing new output files.
- 预防措施：For future artifact writers, use normalized path equality for same-path checks unless both paths are known to exist; keep the fresh-output unittest in the M4 suite.
- 状态：resolved

## M4 v1 patch did not cover the login interface seam
- 现象：Offline VM verification with `App-m4-auth-patched.jar` reached the 火柴AI login UI, but entering a syntactically valid email still displayed `登录失败`; the console stack trace showed the path entering `com.sbf.util.http.SBFApi.k`.
- 触发条件：M4 v1 patched only `/getInfo` (`SBFApi.h`) and `/system/function_module/listmy/41` (`SBFApi.C`), assuming the login bridge token was already available.
- 影响：The app could not reach `StartApp$1.a(JSONObject)`, so the already-patched `/getInfo` and product-module gates were never exercised.
- 根因：M3 startup-chain notes identified the login bridge but the first patch scope omitted `SBFApi.k(String,String)`, whose result object is the direct input to `StartApp$1.a(JSONObject)`.
- 解决方案：Extended the M4 test to fail on the unpatched `SBFApi.k` method body, then patched `SBFApi.k` to return local `code=200`, `sf`, `data.token`, `data.ucf`, `data.imConfig`, `zone` and `time`; generated v2 JAR and v2 jump ISO.
- 预防措施：For startup-chain interface patching, include the first bridge callback return object in the automated probe, not only downstream authorization/resource endpoints.
- 状态：resolved

## Offline cloud spider dictionary fetch looped after entering system
- 现象：After v2 passed login and product selector, clicking `进入系统` hid the visible window and the console looped `java.lang.NullPointerException` at `com.sbf.util.http.SBFApi.M` and `com.sbf.main.cloud.spider.JCloudSpiderMaster$1.run`.
- 触发条件：`JCloudSpiderMaster` constructor starts a background thread that repeatedly waits for `SBFApi.M("spider_modules")`; in the offline VM, `/api/v1/client/pc/dict/data/type` fails and `SBFApi.M` returns `null`.
- 影响：The cloud-spider module discovery thread never completes cleanly, obscuring whether the main UI is stable after product entry. UDP `Network is unreachable` messages from mobile SDK logging were concurrent noise rather than the root cause.
- 根因：M4 v2 covered the authorization chain but did not cover a post-entry dictionary/resource interface that has no null-safe offline fallback in `JCloudSpiderMaster$1`.
- 解决方案：Extended the M4 test to fail on `SBFApi.M(String)` retaining the original method body, then patched `SBFApi.M` to return `new JSONArray("[]")`; generated v3 JAR and v3 jump ISO.
- 预防措施：For offline startup verification, include first-run background dictionary/module discovery calls in the local interface shim once they are observed, and separate harmless offline telemetry errors from null-propagating startup blockers.
- 状态：resolved

## M4 v3 product response omitted the JSBFMain theme contract
- 现象：The v3 offline VM passed local login and displayed the product selector, but clicking `进入系统` hid the UI. The captured log showed `NumberFormatException: Zero length string` at `Integer.decode` / `Color.decode`, followed by `NullPointerException` at `JSBFMain.<init>`.
- 触发条件：`StartApp$1$3.a(JSONObject)` converted the selected product JSON into the runtime theme object. Missing keys were read with `JSONObject.optString`, producing empty strings that reached the j2026 color parser.
- 影响：`JSBFMain` construction aborted before the main window could remain visible. Concurrent mobile SDK UDP `Network is unreachable` errors were offline telemetry noise, not this failure's cause.
- 根因：The local `SBFApi.C()` product object covered the frontend `status/remainingDays` gate but used several guessed camel-case theme keys. The Java transition actually consumes `primary_color`, `secondary_color`, and 14 exact menu/top-bar/button color keys.
- 解决方案：Added a regression probe that requires all consumed theme keys and parses each value with `Color.decode`; extended only `PRODUCT_MODULE_JSON`; generated `App-m4-auth-patched-v4.jar` and `m4-auth-jump-v4.iso`.
- 预防措施：When reconstructing an interface response, test both the frontend gate and the complete Java-side object construction contract; exact JSON key spelling is part of the interface.
- 状态：resolved-host-verification; pending offline VM confirmation

## M4 v4 product response reached JSBFMain but had no menu tree
- 现象：离线 VM 使用 v4 后能从产品选择器进入 `JSBFMain` 壳层，左侧显示 `HuoChaiAI` 顶层产品和版本号，但右侧主区域为空白；控制台重复 `java.net.SocketException: Network is unreachable: Datagram send failed`。
- 触发条件：`SBFApi.C()` 的本地 `/system/function_module/listmy/41` 响应只包含可进入的产品/主题字段，没有产品下的功能模块 `children`。
- 影响：登录、产品有效期、主题构造均通过，但主界面没有可构造的功能菜单/默认模块，因此没有右侧内容可显示。UDP 报错来自 mobile IM SDK 离线发送登录/数据上报，和空白主区域同时出现但不是根因。
- 根因：M4 v4 把 `/system/function_module/listmy/41` 当成产品卡片列表补齐，而该接口同时承担主界面模块树输入；`JSBFMain.a(com.sbf.main.f)` 后续按模块 `code`（如 `aigc`、`getcustomer`、`aicloud`）分流创建内容面板。
- 解决方案：新增回归探针要求 `SBFApi.C()` 产品对象包含非空 `children`、顶层 `aigc` 模块和至少一个 `webFlg=1/linkUrl` 子项；在 `PRODUCT_MODULE_JSON` 下加入最小 AIGC 菜单树；生成 `App-m4-auth-patched-v5.jar` 和 `m4-auth-jump-v5.iso`。
- 预防措施：重建接口响应时，要同时覆盖“选择页卡片字段”和“主界面菜单树字段”；遇到离线日志时先按调用栈和 UI 症状区分遥测噪声与阻断性异常。
- 状态：resolved-host-verification; pending offline VM confirmation

## M4 v5 product code pointed JSBFMain at a missing logo resource
- 现象：离线 VM 使用 v5 后仍为空白；非 UDP 日志显示 `Exception===/svg/main_logo_huochai-ai.svg message==Stream closed`，随后 `NullPointerException` at `com.sbf.main.JSBFMain.<init>` / `StartApp$1$3.a`。
- 触发条件：本地 `SBFApi.C()` 产品对象使用了合成 code `huochai-ai`。
- 影响：产品选择器仍可显示 `HuoChaiAI`，但进入主界面时 `JSBFMain` 按产品 code 拼接主 logo 路径 `/svg/main_logo_<code>.svg`；缺失资源导致构造阶段异常，后续菜单/右侧内容没有机会稳定加载。
- 根因：产品 `code` 不只是业务标识，也参与本地资源路径约定。原始 App.jar 中存在 `svg/main_logo_tiktok.svg` 等 logo，但不存在 `svg/main_logo_huochai-ai.svg`。
- 解决方案：新增回归探针要求产品 code 映射到已有 main-logo 资源族；将 `PRODUCT_MODULE_JSON` 的产品 `code` 从 `huochai-ai` 改为 `tiktok`，保留 `name/displayName=HuoChaiAI`；生成 `App-m4-auth-patched-v6.jar` 和 `m4-auth-jump-v6.iso`。
- 预防措施：接口替身里的标识字段如果参与本地资源拼接，必须从原包现有资源/代码枚举中选取，不要使用新造 code；显示文案可与内部 code 分离。
- 状态：resolved-host-verification; pending offline VM confirmation

## M4 v6 reached the shell but left the PC menus unpopulated
- 现象：离线 VM 使用 v6 后已显示 TikTok logo 和 `HuoChaiAI` 主界面壳层，但左侧没有功能子菜单、右侧内容区域仍为空白；日志截图只剩重复 `IMCORE-UDP` `Network is unreachable`。
- 触发条件：v6 补齐了产品列表、主题、logo code 和产品下 `children`，但没有补 `SBFApi.k()` 的 `/api/v1/client/pc/menus` 响应。
- 影响：`JSBFMain` 不再因缺 logo 崩溃，但功能菜单模型没有从 PC menus 接口产生，界面没有可选择的默认模块，因此右侧为空。
- 根因：主界面菜单加载和产品选择页不是同一个接口。`/system/function_module/listmy/41` 负责产品/授权/部分模块树，`/api/v1/client/pc/menus` 由无参 `SBFApi.k()` 返回 `result.data.result`，离线环境下原始方法仍会等待登录态并发起网络请求。
- 解决方案：新增回归探针要求 `SBFApi.k()` 被本地 JSON 替换，返回非空 `result.data.result`，包含顶层 `aigc` 和至少一个 `webFlg=1/linkUrl/parentId=3457` 子项；在 `M4AuthPatch.java` 中只 patch 无参 `k()`，不影响 `k(String,String)` 登录补丁；生成 `App-m4-auth-patched-v7.jar` 和 `m4-auth-jump-v7.iso`。
- 预防措施：离线 patch 需要按 UI 状态继续追踪“下一层接口”，不要把产品树和主界面菜单树视为同一数据源；日志里只有 UDP telemetry 时，应优先找无异常但数据为空的 UI 输入接口。
- 状态：resolved-host-verification; pending offline VM confirmation

## M4 v7 menu entries still opened an online AIGC web route
- 现象：离线 VM 使用 v7 后可进入主界面并显示 HuoChaiAI/菜单区域，但右侧内容仍为空白；非 UDP 日志出现 `java.net.UnknownHostException: gqkoss.oss-cn-hangzhou.aliyuncs.com` / `qqkoss.oss-cn-hangzhou.aliyuncs.com`，栈位于 `SBFApi.r(String)` 和 `SBFApi$5.run()`。
- 触发条件：v7 的 PC menu 子项虽然补齐了，但 `linkUrl` 仍是线上路由 `/pc/aicloud/my`，离线包内没有这个 Web 前端；同时原始 `SBFApi$5` 定时检查远程 OSS i18n/version 信息。
- 影响：授权、产品选择、主题、logo、菜单树都已通过，但右侧 JxBrowser 内容没有可加载的离线页面；OSS 异常持续刷日志，干扰判断。
- 根因：主界面右侧内容不是纯 Java 本地组件，而是由菜单 `localCode/linkUrl` 驱动的 JxBrowser 页面。v7 只解决了“菜单存在”，没有解决“菜单指向离线可加载内容”。OSS 栈是同一阶段暴露的后台远程依赖。
- 解决方案：新增回归探针禁止 `/pc/aicloud/my`，要求 `localCode=JSinglepage` 和 `file:///C:/m2dump/app/offline-home.html`；新增本地 `offline-home.html`；将 `SBFApi$5.run()` patch 为直接 `return`；生成 `App-m4-auth-patched-v8.jar` 和 `m4-auth-jump-v8.iso`。
- 预防措施：离线验证菜单时同时验证 `localCode`、`linkUrl` 和本地资源是否存在；遇到白屏但壳层正常时，优先查菜单 URL/浏览器载入路径，而不是回退到授权链。
- 状态：resolved-host-verification; pending offline VM confirmation

## M4 v11 renders menus and tabs but the j2026 content body is blank
- 现象：左侧 AIGC 菜单和顶部 `AIGC Video`、`Graphic Video` 标签均已显示，标签下方仍为空白。
- 已知噪声：`IMCORE-UDP Network is unreachable` 是离线遥测；`IconUtil Stream closed` 是图标资源读取错误，未阻止菜单/标签渲染。
- 调查结论：此前的分发日志插在旧版 `com.sbf.main.sub.b`，当前界面实际通过 `JSBFMain$4.a(JComponent,String)` 分发 `com.sbf.main.ext.j2026.h` 标签，因此 v11 日志没有覆盖真正的内容创建路径。
- 诊断方案：v12 在真实分发入口打印 `name/id/code/localCode/linkUrl`，并记录实际新建的内容组件分支。
- v13 证据：字段对调实验未解决空白；运行日志确认正确配置应保持 `localCode=JSinglepage`、`linkUrl=file:///C:/m2dump/app/offline-home.html`。字段反转判断属于诊断标签语义误读，现已撤销。
- 当前边界：Edge 可显示同一 file URL；App 已进入 `M4_V12_NEW_JXBROWSER`。剩余可能为异步 `loadUrl` 未执行、JxBrowser 返回加载错误、或加载完成但 BrowserView 尺寸/渲染异常。
- v14 诊断方案：在 `c$3.run()` 的真实 `Navigation.loadUrl` 前打印 URL；利用原有空回调打印 `NavigationFinished`/`LoadFinished`；在 `c.doLayout()` 后打印 BrowserView parent/size。
- 状态：diagnostic-ready; pending offline VM evidence

## M4 v14 loaded the local page but hardware rendering stayed blank in the VM
- 现象：JxBrowser 对本地 file URL 报 `LOAD_FINISHED`，BrowserView 尺寸非零且已挂入 Swing 容器，但内容区仍为空白；Edge 可显示同一页面。
- 触发条件：离线 Windows VM 缺少可用 GPU/硬件合成环境，而 `com.sbf.main.jxbrowser.g` 可根据 `StartApp.g` 选择 `HARDWARE_ACCELERATED`。
- 影响：导航、文件路径、HTML 和 Swing 布局均正常时，硬件窗口渲染仍可能不出画面。
- 当前假设：JxBrowser/Chromium 的硬件渲染或合成路径与 VM 图形环境不兼容。
- 控制变量：v15 强制 `OFF_SCREEN`、禁用 GPU/D3D11 并启用 SwiftShader，同时在加载完成时抓取 `Browser.bitmap()` PNG；不改 URL 和业务 JSON。
- 判读：PNG 有内容而 UI 空白为 Swing 呈现问题；PNG 也空白为 Chromium 离屏帧问题；UI 恢复则确认硬件渲染路径为根因。
- 状态：diagnostic-ready; pending offline VM evidence

## M4 v15 proved rendering but still pointed business tabs to a diagnostic page
- 现象：v15 在 VM 中已经让 `AIGC Video` / `Graphic Video` 右侧内容显示出来，`m4-jxb-capture.png` 也完整渲染，但显示的是 `HuoChaiAI Offline Mode` 诊断页，而不是在线业务页面。
- 触发条件：v8 为了隔离“菜单/加载/渲染”问题，将 AIGC 菜单入口从原始在线路由 `/pc/aicloud/my` 临时改成 `file:///C:/m2dump/app/offline-home.html`；v15 继承了这个诊断 URL。
- 影响：v15 可以证明 JxBrowser 在无 GPU VM 中需要软件渲染，但不能作为最终业务联网版本，因为它绕开了真实 AIGC 业务前端。
- 根因：阶段目标被临时诊断页混淆。项目目标不是“业务离线化”，而是“授权层本地化 + 业务功能继续联网”。
- 解决方案：新增 v16 回归探针，要求产品树和 PC 菜单中的 `JSinglepage` 入口全部恢复为 `/pc/aicloud/my`，且不得包含 `offline-home.html`；保留 v15 的 `OFF_SCREEN`、`disableGpu()`、SwiftShader/D3D11 开关和 bitmap 抓图。
- 预防措施：诊断页只能作为临时控制变量；进入 M5 前，自动测试必须断言业务菜单不再指向本地 fallback 页面。
- 状态：resolved-host-verification; pending VM online business verification

## M4 v16 restored business route but JxBrowser treated the relative path as host `pc`
- 现象：v16 在 VM 中能显示 AIGC 菜单和两个 Tab，JxBrowser 软件渲染也正常，但右侧显示 Chromium 离线页。日志显示 `M4_V12_DISPATCH ... localCode=/pc/aicloud/my linkUrl=JSinglepage`，随后 `M4_V13_LOAD_URL=/pc/aicloud/my?...` 和 `M4_V13_LOAD_FAILED url=http://pc/aicloud/my?... error=INTERNET_DISCONNECTED`。
- 触发条件：业务菜单恢复成原始相对路由 `/pc/aicloud/my` 后，`com.sbf.main.jxbrowser.c$3.run()` 直接把该相对路径传给 `Navigation.loadUrl`；在没有明确 base URL 的 JxBrowser/Chromium 里，它被解释成 `http://pc/aicloud/my`。
- 影响：授权链、菜单链、JxBrowser 创建和 SwiftShader 渲染均已正常，但真实 AIGC 业务页仍无法打开；如果继续看 UI，会容易误判成“没联网”或“业务页坏了”。
- 根因：原程序在 `JSBFMain` 构造菜单时有 `https://` + 运行时域名 + `/pc/aicloud/my` 的拼接语义；补丁 v16 保留了相对业务路径，却没有在 JxBrowser load 前补回运行时 base 域名。
- 解决方案：新增 v17 回归断言，要求 `c$3.run()` 中出现 `M4_V17_NORMALIZED_URL`、`String.startsWith("/")`、`https://` 和 `com/sbf/main/c.a`；在 `Navigation.loadUrl` 前将以 `/` 开头的 URL 归一化为 `https://` + `com.sbf.main.c.a` + 原路径，不硬编码具体业务域名。
- 预防措施：业务菜单可以保留相对路由，但任何直接进入 JxBrowser 的相对 URL 必须在加载边界补 base；后续若页面提示未登录/403，应转入 cookie/token/JS bridge 登录态传递排查，不再回退到 `offline-home.html`。
- 状态：resolved-host-verification; pending host/VM online business verification

## M4 v17 normalized relative URL with the wrong runtime field
- 现象：宿主机实测 v17 后，AIGC Tab 进入 JxBrowser，日志显示 `M4_V17_NORMALIZED_URL=https://41/pc/aicloud/my?...`，Chromium 最终尝试 `https://0.0.0.41/pc/aicloud/my?...` 并 `CONNECTION_CLOSED`。
- 触发条件：v17 根据不完整推断使用 `com.sbf.main.c.a` 作为业务 host；该字段当前运行时值为产品/模块 ID `41`，不是域名。
- 影响：v17 修复了“相对 URL 需要补 base”的边界，但 base 来源错误，真实业务站仍打不开。
- 根因：原始 `JSBFMain` 构造 AIGC URL 的字节码在 `/pc/aicloud/my` 附近调用的是 `com.sbf.util.http.SBFApi.c()`，而不是 `com.sbf.main.c.a`。此前把 `c.a` 当成 host 属于字段语义误判。
- 解决方案：新增 v18 回归断言，要求 `c$3.run()` 包含 `M4_V18_NORMALIZED_URL` 和 `com/sbf/util/http/SBFApi.c:()Ljava/lang/String;`；将 URL 归一化改为 `https://` + `SBFApi.c()` + 原路径。宿主机实测得到 `https://app.xdxsoft.com/pc/aicloud/my?...`，导航成功后被 Web 站重定向到登录页。
- 预防措施：对混淆字段不要只靠字段名/静态初值推断语义，必须用原始调用点的 bootstrap 解码和运行时日志交叉验证；URL base 这类边界必须打印最终值。
- 状态：resolved-host-verification; new boundary is Web login-state bridge

## M5 v18 reached Web login page because Admin-Token was not bridged
- 现象：v18 宿主机联网实测中，`/pc/aicloud/my` 已正确归一化为 `https://app.xdxsoft.com/pc/aicloud/my?...`，导航成功且 JxBrowser 渲染正常，但 Web 前端最终加载 `https://app.xdxsoft.com/login?redirect=...`。
- 触发条件：Java 客户端本地登录态已经通过，但 `app.xdxsoft.com` 的 Vue 前端没有拿到它自己的 `Admin-Token`。前端模块 `5f87` 在有 `window.mijava` 时调用 `window.mijava.getVVParentToken(origin + "/prod-api/getLoingIsToken")` 并写 cookie `Admin-Token`；否则或返回空时按未登录处理。
- 影响：M4 主链路会被误判成 URL/网络/渲染失败，但实际已到真实业务域名；阻塞在 Web 登录态桥接。
- 根因：原始桥链路 `MiJava.getVVParentToken(...) -> getAction(...) -> StartApp.f(String)` 仍按原程序逻辑获取远端 `header`，在当前本地授权恢复状态下没有为 Web 前端提供可用 token。
- 解决方案：v19 在 `StartApp.f(String)` 开头对 `getLoingIsToken` / `get_current_token` 做最小本地桥接，返回 `offline-local-token-1234567890`，并打印 `M4_V19_WEB_TOKEN_BRIDGE url=...`。宿主机实测中 Web 前端实际调用该桥，页面不再停在 `/login?redirect=...`，而是进入在线业务壳层。
- 预防措施：M5 不能再用 `offline-home.html` 或离线业务页绕开问题；所有判断都应以真实 `app.xdxsoft.com` 页面和 `M4_V19_WEB_TOKEN_BRIDGE` / Web 网络请求证据为准。
- 状态：resolved-host-verification; new boundary is post-login business content blank

## M5 v19 bridges token but business content remains blank
- 现象：v19 宿主机实测中，`AIGC Video` 和 `Graphic Video` 均加载真实 URL，`M4_V19_WEB_TOKEN_BRIDGE` 多次触发，截图 `C:\m2dump\host-screen-v19-business.png` 显示在线业务壳层和两个 Tab；但右侧内容区仍为空白，`C:\m2dump\m4-jxb-capture.png` 为纯白。
- 触发条件：v19 给 Web 前端一个本地占位 token，使路由守卫越过登录页；但后续业务组件仍可能用该 token 作为 `Authorization: Bearer ...` 请求真实服务端。
- 影响：登录页重定向已解决，但业务功能尚未可用。若只看“白屏”，容易错误回退到 v15 的渲染问题或 v8 的本地诊断页。
- 当前假设：后续业务接口需要原始 `StartApp.f(String)` 远端返回的真实 `header`，或需要额外 JS bridge / sessionStorage / async chunk 初始化数据；占位 token 只能过前端路由门槛，不能代表真实业务授权 header。
- 下一步方案：M5-v20 应优先做诊断而不是再猜 JSON：记录/对比 `StartApp.f` 原始返回、捕获前端网络/控制台错误，解析 `/pc/aicloud/my` 对应 `chunk-dea9eb98` 的实际 API/bridge 依赖，再决定是桥接真实 header、补 sessionStorage/localStorage，还是实现额外 Java bridge。
- 状态：resolved-by-v27

## M5 v24/v25 used an unsupported JxBrowser response-intercept callback
- 现象：v24 宿主机运行时 `Network.set(InterceptUrlRequestCallback.class, ...)` 先因回调对象不能强转为 `NetworkCallback` 失败；v25 让生成类同时实现 `NetworkCallback` 后，又报 `IllegalStateException: The callback type is not supported: interface com.teamdev.jxbrowser.net.callback.InterceptUrlRequestCallback`。
- 触发条件：只根据 `jxbrowser-7.41.3.jar` 中存在 `InterceptUrlRequestCallback` 类和 `UrlRequestJob` API 推断它可由 `Network.set` 注册。
- 影响：`m5InstallWebDiagnostics` 的 try/catch fail-open 避免了主流程崩溃，但该异常会提前跳出诊断安装块，导致后续 `RequestCompleted` 网络观察器也没有注册。
- 根因：JxBrowser 7.41.3 发布包包含该接口，但 `UniversalServiceConnection` 的 callback registry 不支持通过公开 `Network.set` 设置该类型；类存在不等于当前 surface 可注册。
- 解决方案：v26 移除 `Network.set(InterceptUrlRequestCallback...)` 路径，改在已验证可用的 `InjectJsCallback` 中定点覆写 `fetch/XMLHttpRequest`，只对 Web 登录态初始化接口补本地响应形状。
- 预防措施：以后使用 JxBrowser callback 不只看 `javap` 接口存在，还要通过宿主机注册实测或内部 `CallbackType` 支持表确认。
- 状态：resolved

## M5 v26 passed Web bootstrap but AiCloud first page expected rows/dict arrays
- 现象：v26 宿主机已接住 `/prod-api/getInfo` 和 `/prod-api/getRouters`，真实业务 chunk `chunk-dea9eb98` 开始加载，但控制台出现 `TypeError: Cannot read properties of undefined (reading 'length')`。
- 触发条件：`/pc/aicloud/my` 的首屏组件会请求 `/prod-api/mnq/mnqAuthAccounts/mylist?pageNum=1&pageSize=10` 和 `/prod-api/system/dict/data/type/yes_no_1_0`；fake token 下真实服务端返回 HTTP 200 但业务 `code=401,data={}`。
- 影响：路由守卫已过，页面 chunk 已加载，但表格 `rows/total` 和字典数组缺失，导致首屏渲染中断。
- 根因：M5 的 Web 侧登录门槛不止 `getInfo/getRouters`；当前页面首屏还要求授权码表和启用/禁用字典具备 RuoYi 风格成功响应形状。
- 解决方案：v27 仅对这两个首屏初始化接口补最小形状：`mylist` 返回 `rows:[], total:0`，`yes_no_1_0` 返回启用/禁用字典数组；真实增删改业务接口不接管。
- 预防措施：每新增一个本地 Web 响应都必须由具体页面 chunk 和宿主日志证明，不允许把 `/prod-api/*` 泛化成本地代理。
- 状态：resolved

## M5 v27-v28 local Java IM JSON used the wrong nested field order
- 现象：v27 宿主机 stderr 仍出现一次被捕获的 `java.lang.NullPointerException`，栈顶为 `com.sbf.main.JSBFMain.<init>`；页面最终仍能渲染 AiCloud 授权码表，所以这不是 v27 的首屏阻断，但属于主界面初始化形状缺口。
- 触发条件：本地 `SBFApi.h()` 返回的 `GET_INFO_JSON` 比原始 `JSBFMain` 构造阶段消费的字段更窄；静态解码显示构造函数读取 `im.ip`、`im.udp.port`、`humanFlag`、`userName`、`nickName`、`avatar`、`developerFlg` 等字段。产品选择回调还读取 `logoSvg`。
- 影响：容易把非阻断 stderr 误判为 Web 业务失败；也可能在不同启动顺序或窗口路径下导致主界面初始化不稳。
- 根因：早期 M4/M5 JSON 以“过授权门槛/显示菜单”为最小目标；v28 虽补了 IM 字段，但按常见语义写成 `im.udp.port`。原字节码同一行两个密文的实际顺序是先解出 `port`、再解出 `udp`，构造函数读取的是 `im.port.udp`。
- 解决方案：v29-v31 用临时分段/catch 标记排除托盘图标和布局分支，确认只命中 IM catch；v32 把本地 JSON 改为 `im.port.udp` 后宿主机不再命中该 catch；随后移除全部临时 `JSBFMain.class` 插桩，生成正式 v33。
- 预防措施：后续补授权/产品 JSON 时应从密文调用出现顺序和实际字节码栈顺序反推嵌套字段；同一源码行有多个解密字符串时，不能只按字段语义重排。不要通过 patch `JSBFMain` 吞异常来掩盖本地初始化数据缺口。
- 状态：resolved-host-verification

## M4A Java 目录中文源码被默认 GBK 编译拒绝
- 现象：新增 M4RecoveryCatalog.java 后，补丁目标测试在 javac 阶段出现多处 编码GBK的不可映射字符，未进入九产品行为断言。
- 触发条件：tests/test_m4_auth_patch.py 的 compile_patcher 未传 -encoding UTF-8，而新目录包含中文产品和菜单文案。
- 影响：M4A 补丁测试无法编译，红灯原因偏离预期功能缺口。
- 根因：测试夹具隐式依赖 Windows 默认代码页；此前 M4AuthPatch.java 主要是 ASCII 字符串，因此问题未暴露。
- 解决方案：在 compile_patcher 的 javac 参数中显式加入 -encoding UTF-8，并保留目录探针同样的编码参数。
- 预防措施：后续所有含中文 Java 源码的编译命令和测试夹具必须显式使用 -encoding UTF-8。
- 状态：resolved

## M4B 自动登录回调关闭空登录窗
- 现象：v38 已直接显示产品选择器，但 stderr 在 `StartApp$1.a -> StartApp$3.run` 报 `NullPointerException`。
- 根因：原成功回调最后会 dispose `StartApp.t`；自动登录没有创建 `JLoginHTML`，因此该字段为空。
- 解决方案：保留原成功链，只在读取 `StartApp.t` 后对 dispose 调用增加 null guard，并记录 `M4B_SKIP_LOGIN_DISPOSE`。
- 状态：resolved-by-v39

## M4B 产品 logo 使用资源 URL 而不是原始内联 SVG
- 现象：v38 九产品卡可见，但卡片 logo 全部显示破图。
- 根因：`product-selector.html` 直接把 `product.logoSvg` 插入 DOM，字段契约是 SVG markup；恢复值却写成了 `<img src="/svg/...">`。
- 解决方案：补丁生成时通过原包 `ch.r` 解密九个 `main_logo_<code>.svg`，去掉 XML 声明后以内联 SVG 写入产品 JSON。
- 状态：resolved-by-v39

## M4A 菜单 icon 字段被重复拼接路径和扩展名
- 现象：v39 WhatsApp 菜单文字已显示，但 icon 为空；日志路径为 `/svg/svg/whatsapp_menu_icon_1.svg.svg`。
- 根因：本地目录把 icon 写成 `svg/<name>.svg`，而原客户端 `IconUtil` 会自行添加 `/svg/` 和 `.svg`。
- 解决方案：兼容目录只返回资源 basename，例如 `whatsapp_menu_icon_1`；v40 宿主截图确认 11 项菜单 icon 正常显示。
- 状态：resolved-by-v40
