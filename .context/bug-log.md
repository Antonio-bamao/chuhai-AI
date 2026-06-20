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
