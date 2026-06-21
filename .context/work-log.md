# 工作日志

> 每完成一个明确步骤就追加一条记录，不写流水账。

## 2026-06-20 02:04｜初始化并更新项目 .context
- 目标：初始化并更新项目 .context
- 动作：从外部工程设计文档与 SOP 六件套迁移项目愿景、设计契约、里程碑、任务卡、ADR 和风险台账。
- 结果：已生成标准 .context 目录，并补齐 vision.md、design.md、master-plan.md、current-status.md、task-breakdown.md、decisions.md、risk-register.md。
- 验证：待运行 validate_context.py 校验上下文门禁。
- 下一步：运行上下文校验，修复任何模板残留或字段不合规问题。

## 2026-06-20 02:13｜执行 M1-01 资产备份与原始启动记录
- 目标：执行 M1-01 资产备份与原始启动记录
- 动作：清点根目录与 data 目录资产；创建 .artifacts/backups/original-20260620-021044 备份点；生成全量 SHA256 CSV；记录关键文件元数据、哈希和静态启动线索；因隔离环境未确认，暂不实际启动客户端。
- 结果：已产出 .context/assets-inventory.md、.context/backup-manifest.md、.context/verify.log；备份源与目标均为 772 个文件、1,706,314,191 bytes；App.dll 与 HuoChaiAI.exe 哈希抽检一致。
- 验证：使用 Get-ChildItem 统计源/备份文件数和字节数；使用 Get-FileHash 抽检关键文件；Format-Hex 确认 App.dll 为 PK/ZIP 形态、HuoChaiAI.exe 为 MZ/PE 形态。
- 下一步：由用户拍板先进入 M1-02 JADX 导出源码树，或先准备隔离环境执行 M1-03 原始启动验证。

## 2026-06-20 02:13｜防止本地备份误入 Git
- 目标：防止本地备份误入 Git
- 动作：新增根目录 .gitignore，忽略 .artifacts/；保留 .context 中的人读清单，机器全量清单和备份留在本地 artifacts。
- 结果：Git 后续不会默认显示 .artifacts/backups 和 .artifacts/manifests 中的大体积文件。
- 验证：待运行 git status --short 与 validate_context.py 确认状态。
- 下一步：完成最终校验并向用户汇报 M1-01 结果。

## 2026-06-20 02:20｜执行 M1-02 源码树导出
- 目标：执行 M1-02 源码树导出
- 动作：确认 App.dll 为 Java class JAR；记录 ADR-0002，采用 CFR 替代 JADX；下载 CFR 0.152 并校验 MD5；复制备份 App.dll 为 .artifacts/working/m1-02/App.jar；使用 CFR 导出源码树；统计输出并抽样验证 StartApp、JLoginNew、ClawWorkspace。
- 结果：已产出 .context/decompile-report.md；源码树位于 .artifacts/decompiled/cfr-app-20260620-0215，共 4,226 个 .java 文件；Manifest Main-Class 为 com.sbf.main.StartApp。
- 验证：CFR 命令退出码 0；输出统计为 4,226 个 Java 文件；StartApp.java 含 public static void main；JLoginNew.java 与 ext/j2026/ClawWorkspace.java 已存在；validate_context.py 待最终运行。
- 下一步：提交 M1-02 文档检查点，然后进入 M2 字符串解密分析。

## 2026-06-20 02:31｜执行 M2 第一阶段字符串解密
- 目标：执行 M2 第一阶段字符串解密
- 动作：复刻 JSetupDialog$JLoginNew.N 与 JTestFrame$JLoginNew$2.k 两类调用点相关解密算法；新增 tools/decode_java_strings.py；生成 .artifacts/analysis/string_map.json；筛选授权相关候选 auth_string_candidates.json；记录字符串解密报告。
- 结果：已产出 1,001 条字符串解码记录，其中 943 条可读性评分 >= 0.8；StartApp 中已解出 user、tenantCode、userId、token、result、header、data、expireTime 等字段；授权候选 132 条。
- 验证：python -m py_compile 已通过；脚本执行退出码 0；抽样 StartApp 第 75 行解出 D:/aimirror/，第 383 行解出 token，第 395 行解出 expireTime；validate_context.py 待最终运行。
- 下一步：提交 M2 第一阶段检查点，然后继续处理 JLoginHTML$h.v、ClawWorkspace.vv、JLoginNew.vS、StartApp.Sy 等动态调用/字符串形态。

## 2026-06-20 02:34｜扩展 M2 j2026 静态字符串解码
- 目标：扩展 M2 j2026 静态字符串解码
- 动作：分析 JLoginHTML$h.v 的 key/常量；将其加入 tools/decode_java_strings.py；重生成 string_map.json 和 auth_string_candidates.json；更新字符串解密报告、验证记录和当前状态。
- 结果：string_map 从 1,001 条增至 1,094 条；高可读记录从 943 条增至 1,034 条；授权候选从 132 条增至 225 条；j2026 路径解出 登录系统、/html/Login.html、ClawWorkspace、/html/ClawWorkspace.html。
- 验证：python tools/decode_java_strings.py 退出码 0；python -m py_compile 退出码 0；抽样检查 JLoginHTML 与 ClawWorkspace 明文；validate_context.py 待最终运行。
- 下一步：提交 j2026 静态字符串扩展检查点，然后继续分析 invokedynamic/bootstrap 形态。

## 2026-06-20 02:36｜整理 M3 预备接缝候选
- 目标：整理 M3 预备接缝候选
- 动作：基于 string_map 和 auth_string_candidates 反查 StartApp、JLoginHTML、ClawWorkspace；整理 StartApp.f(String)、StartApp.i()、JLoginHTML、ClawWorkspace 四个候选；明确哪些区域当前不作为 patch 点。
- 结果：已产出 .context/seam-candidates.md；StartApp.f(String) 被标记为高优先级候选，StartApp.i() 为中优先级候选；登录 UI 与工作区入口仅作为调用链入口。
- 验证：通过已解明文字段 token/result/header/data/expireTime/user/tenantCode/userId 与源码行段交叉确认；未修改客户端产物；validate_context.py 待最终运行。
- 下一步：提交接缝候选草稿；后续还原 StartApp.Sy/JLoginNew.vS/ClawWorkspace.vv 的动态调用目标。

## 2026-06-20 02:49｜执行 M2 bootstrap 动态调用解码
- 目标：还原 `StartApp.Sy(...)` 形态的 bootstrap 调用目标，支撑后续 M3 接缝定位。
- 动作：新增 `tools/decode_bootstrap_calls.py`；复刻 `StartApp.Sy(...)` 的 payload 解码与 class/method/descriptor 解析逻辑；生成 `bootstrap_map.json`；按方法签名和源码行段筛出 `StartApp.f(String)`、`StartApp.i()`、`StartApp.k(String)` 的高价值候选；修正脚本以记录 `caller_signature`，避免重载方法混淆。
- 结果：全量 bootstrap 记录 73,600 条，成功解码 73,597 条；`startapp_bootstrap_candidates.json` 包含 19 条高价值 `StartApp` 调用；`StartApp.f(String)` 的 `DTHelper.b(String,String): JSONObject`、`AESCBCHelper.a(String)`、`MD5Util2.c(String)` 与缓存链已明确。
- 验证：`python -m py_compile tools\decode_bootstrap_calls.py` 退出码 0；`python tools\decode_bootstrap_calls.py --source-root ... --out ...` 退出码 0；抽样打印 19 条候选并核对源码行段；未修改客户端产物。
- 下一步：反查 `DTHelper.b(...)`、`com.sbf.main.jxbrowser.n`、`StartApp$5`，并继续处理 `JLoginNew.vS(...)`、`ClawWorkspace.vv(...)`。

## 2026-06-20 03:03｜扩展 M2 HTTP/缓存字符串解码
- 目标：补齐 `DTHelper`、`jxbrowser.n`、`StartApp$5` 相关明文，确认网络/缓存/定时任务边界。
- 动作：为 `tools/decode_java_strings.py` 新增 5 个同构解码器；重生成 `string_map.json` 和 `auth_string_candidates.json`；结合 bootstrap map 反查 `DTHelper.b(...)`、`n.c()`、`StartApp$5.run()`。
- 结果：字符串记录从 1,094 条扩展到 4,473 条，授权候选从 225 条扩展到 362 条；确认 `DTHelper` 是通用 OkHttp wrapper，`jxbrowser.n` 持有 `result/header/data/expireTime` 状态缓存，`StartApp$5` 更像截图上传/状态上报任务。
- 验证：`python -m py_compile tools\decode_java_strings.py tools\decode_bootstrap_calls.py` 退出码 0；`python tools\decode_java_strings.py --source-root ... --out ...` 退出码 0；抽样 `DTHelper`、`n`、`StartApp$5` 明文字段并与源码/动态调用目标交叉核对。
- 下一步：反查 `StartApp.f(String)` 的调用者和入参 URL，继续还原 `JLoginNew.vS(...)`、`ClawWorkspace.vv(...)`。

## 2026-06-20 03:09｜确认 M2 JS bridge token action 到 StartApp.f(String) 的入口
- 目标：确认 M2 JS bridge token action 到 StartApp.f(String) 的入口
- 动作：新增 AdsCallback$SGAICloudPanel$2.I 静态字符串解码器；重生成 string_map.json 与 auth_string_candidates.json；对照 AdsCallback.getAction、MiJava.getAction 和 bootstrap_map 确认 action 与调用描述符。
- 结果：字符串映射增至 4,599 条并全部解码；两套 bridge 均在 get_current_token 或 getLoingIsToken 分支将原始 action 直接传给 StartApp.f(String)。
- 验证：python -m py_compile 退出码 0；全量解码输出 rows=4599、decoded=4599；AdsCallback 解码器 126 条、error=0；两处 bootstrap 均为 static StartApp.f(String): String。
- 下一步：继续还原 StartApp.f(String) 第 385 行最终 URL 拼接结果，并追踪 jxbrowser.n.c() 的刷新触发点。

## 2026-06-20 03:13｜还原 StartApp.f(String) 的 URL 语义与时间参数拼接
- 目标：还原 StartApp.f(String) 的 URL 语义与时间参数拼接
- 动作：核对 bridge 分支比较函数 bootstrap；解码 StartApp.f 第 385 行与 DTHelper.a 第 314 行常量；反查 Request.Builder.url(String) 使用点；搜索本地资源中的 token action 样本。
- 结果：确认 bridge 使用 contains 而非 equals；原始参数可为包含 token action 的完整 URL；StartApp.f 追加 RT，DTHelper 再追加 rdtime，随后直接交给 OkHttp。
- 验证：两套 bridge 的比较目标均为 String.contains(CharSequence)；明文常量为 ?、&RT=、?RT=、&rdtime=、?rdtime=；DTHelper.java:322 调用 Request.Builder.url(String)。
- 下一步：追踪 jxbrowser.n.c() 的刷新触发点，并在隔离环境中记录实际 token 请求域名和路径。

## 2026-06-20 03:18｜确认 jxbrowser.n.c() 的刷新触发点与缓存实例字段
- 目标：确认 jxbrowser.n.c() 的刷新触发点与缓存实例字段
- 动作：从 bootstrap_map 反查 n.c() 唯一调用者；核对 n.a(long) 的 Timer.schedule 参数；直接解析 App.jar 中 n.class 的 Code 属性，验证 n.a(String,String) 对象构造与 putfield 指令。
- 结果：n$1.run 是唯一刷新调用者；缓存写入时注册 1 小时首次、10 小时周期的刷新任务；URL 与请求体 c/d 写入同一返回实例。
- 验证：bootstrap 目标为 n$1.run -> n.c()；调度参数为 3600000/36000000；字节码仅一次 new n，并依次 putfield c、putfield d 后 areturn。
- 下一步：继续处理 ClawWorkspace.vv、JLoginNew.vS，并梳理 expireTime 对 UI/启动分支的实际影响。

## 2026-06-20 03:20｜确认 JLoginNew.vS 与 ClawWorkspace.vv 的 bootstrap 覆盖
- 目标：确认 JLoginNew.vS 与 ClawWorkspace.vv 的 bootstrap 覆盖
- 动作：按 bootstrap 名筛选 bootstrap_map；统计调用数量与错误；排除 Swing/AWT/String 等通用目标后复核剩余调用；读取 JLoginNew 与 ClawWorkspace 关键源码段。
- 结果：JLoginNew.vS 116 条、ClawWorkspace.vv 4 条全部解析；目标以 UI 装配、窗口调度、线程启动和异常打印为主，未出现新的授权网络接缝。
- 验证：两族记录 error 均为 null；全量仅 3 条空 key 错误，位于 JPLTStatusBrowser 与 Weta365Helper，不在当前关键路径。
- 下一步：梳理 expireTime 对 UI/启动分支的实际影响，并评估 M2 转入 M3 的门槛。

## 2026-06-20 03:27｜追踪 expireTime 对 UI 和启动分支的实际影响
- 目标：追踪 expireTime 对 UI 和启动分支的实际影响
- 动作：交叉核对 StartApp.f、jxbrowser.n、StartApp$1、JSBFMain、JReadme、bridge 调用与 i18 文案；区分 token 缓存 expireTime 和账号期限 periodTime；新增影响报告并更新接缝候选、解密报告、验证记录和当前状态。
- 结果：确认 expireTime 只控制 header 缓存，在到期前 60 秒失效，缺失/0 时默认约 1 小时；periodTime 只触发过期/临期提示并在 JReadme 展示，提示后仍继续创建产品选择器。M2 已达到转入 M3 的静态分析门槛。
- 验证：string_map 中 periodTime 仅见 StartApp$1 与 JSBFMain，expireTime 仅见 StartApp.f 与 n.c；bootstrap_map 确认 JOptionPane、JProductSelectorHtml、JLoginHTML.dispose 调用顺序；i18.cnf 核对过期与临期中文文案；未修改客户端产物。
- 下一步：进入 M3，生成正式 seams.md，追踪软件开通列表、token、SBFApi.h 结果码及 roles/overdue/套餐配置对普通版降级的影响。

## 2026-06-21 00:39｜确定 M2 完整 Phase 3 补齐方案
- 目标：确定 M2 完整 Phase 3 补齐方案
- 动作：对照外部 Phase 3 要求与现有产物，明确全候选分类、JSON/CSV、注释源码树、去花阅读副本和后续手动动态 dump 的分阶段设计；新增正式规格并将当前阶段从准备 M3 调整回继续 M2。
- 结果：已形成可审计的五阶段设计，规定所有候选族必须有状态，原始资产只读，动态 dump 不在当前主机执行，去花产物仅供阅读。
- 验证：规格已明确文件边界、状态模型、测试策略、停止条件、十项验收标准和非目标；待用户复核后进入实施计划。
- 下一步：用户复核规格；批准后使用 writing-plans 编写逐步实施计划。

## 2026-06-21 00:44｜编写 M2 完整 Phase 3 逐步实施计划
- 目标：编写 M2 完整 Phase 3 逐步实施计划
- 动作：依据已批准规格，将工作拆为基线冻结、共享扫描器、解密核心/注册表、全候选清单、静态解密批次、JSON/CSV、注释源码、20 条抽检、安全去花、动态 dump 手册和最终门禁共 11 个任务；加入 TDD、逐阶段提交和 Threadtear 主机安全限制。
- 结果：已生成 docs/superpowers/plans/2026-06-21-m2-full-string-decryption.md，包含精确文件、测试、命令、预期结果和停止条件。
- 验证：完成规格覆盖、占位符、类型一致性和安全边界自检；待上下文校验与提交。
- 下一步：选择 subagent-driven 或 inline execution 后，从 Task 1 冻结基线开始执行。

## 2026-06-21 00:51｜Freeze the M2 full-string-decryption baseline before expanding analysis.
- 目标：Freeze the M2 full-string-decryption baseline before expanding analysis.
- 动作：Added a deterministic source-tree hashing helper and fixture-based unit test; generated the ignored full tree baseline; documented immutable inputs and host-safety rules; recorded managed-sandbox ACL limitations.
- 结果：Baseline frozen at 4,227 files / 4,226 Java files / 59,649,060 bytes, JAR SHA-256 9084FABCE357AAD8B18D06D0FB708DE4E92E1B5D63686CEA1DED49E19F73A99B, 4,599 static-map rows and 73,600 bootstrap rows with 73,597 error-free decodes.
- 验证：Unit test passed; in-memory Python compilation passed; baseline counts and hashes were recomputed from artifacts; git diff --check passed.
- 下一步：Implement the shared Java source scanner and inventory extraction as Task 2.

## 2026-06-21 01:00｜Extract one tested Java source scanner without changing existing analysis outputs.
- 目标：Extract one tested Java source scanner without changing existing analysis outputs.
- 动作：Added Java literal unescaping, package/class inference, brace-depth method tracking, caller metadata, fixture tests, and a compatibility profile for the legacy bootstrap parser; refactored both decoder scripts to consume the shared scanner.
- 结果：The shared scanner handles static initializers, constructors, normal methods and multiple calls per line while preserving both prior maps exactly.
- 验证：Four scanner/hash tests passed; all three Python modules compiled in memory; regenerated 4,599 static-string rows and 73,600 bootstrap rows were byte-for-byte equal as parsed JSON to the frozen outputs; context and diff checks passed.
- 下一步：Separate the AES-like decoder algorithm from decoder-family registration in Task 3.

## 2026-06-21 01:06｜Separate the verified string decoder algorithm from decoder-family registration.
- 目标：Separate the verified string decoder algorithm from decoder-family registration.
- 动作：Moved 32-bit Java arithmetic, AES-style table construction and string decoding into string_decoder_core.py; moved all nine family patterns and constants into a typed registry; added cached decoder construction and verified-sample tests; redirected bootstrap arithmetic to the core module.
- 结果：The decoding implementation is now reusable by later inventory and expansion stages, while all nine existing families retain their exact behavior.
- 验证：Five unit tests passed including token, expireTime and ClawWorkspace samples; four modules compiled in memory; regenerated 4,599-row string map is exactly equal to the frozen map; bootstrap decoder imports successfully; diff checks passed.
- 下一步：Build the complete candidate decoder-family inventory in Task 4.

## 2026-06-21 01:10｜Build a complete conservative inventory of encrypted-string call families.
- 目标：Build a complete conservative inventory of encrypted-string call families.
- 动作：Added a one-literal Unicode-call scanner, method-definition index, evidence features, conservative status assignment, JSON/CSV/Markdown exporters, and fixture tests covering known, non-string and missing definitions.
- 结果：Inventory contains exactly 348 unique families and 35,642 call sites: 9 decoded_static, 337 dynamic_dump_required and 2 unsupported_shape. Every family has samples, definition evidence when available, and one allowed status.
- 验证：Inventory fixture tests and shared-scanner tests passed; real output counts match the expected baseline; uniqueness/status assertions, context validation and diff checks passed.
- 下一步：Inspect high-volume structurally identical families and expand the registry in deterministic verified batches.

## 2026-06-21 01:46｜Expand all statically verifiable AES-table decoder families without guessing ambiguous definitions.
- 目标：Expand all statically verifiable AES-table decoder families without guessing ambiguous definitions.
- 动作：Implemented deterministic source-based spec discovery, verified it reproduces all nine original configurations, registered 347 unique families, added ten high-volume semantic samples, corrected typed same-name method caller attribution, and excluded pre-existing plaintext arguments from ciphertext decoding.
- 结果：Static family coverage increased from 9 to 347 and encrypted call coverage from 4,599 baseline rows to 35,193 eligible calls. Only a.Z remains unresolved because two package-distinct definitions collapse to the same textual family.
- 验证：Nine tests passed; the inventory reports 347 decoded_static and one unsupported family; expanded map contains exactly 35,193 rows, matching decoded-static inventory calls; ten representative new samples decode to coherent plaintext.
- 下一步：Build the unified JSON/CSV exporter with explicit plaintext, unresolved, caller-confidence and decode-error statuses.

## 2026-06-21 01:55｜Produce stable complete JSON/CSV maps with explicit unresolved and caller-confidence evidence.
- 目标：Produce stable complete JSON/CSV maps with explicit unresolved and caller-confidence evidence.
- 动作：Implemented deterministic normalization/IDs, UTF-8 JSON, multiline-safe CSV, unresolved export, existing-plaintext status, original-classfile method extraction, and conservative caller candidate resolution for lambda/synthetic methods.
- 结果：Final static map has 35,644 records: 34,418 decoded_static, 2 decoded_existing_plaintext, 775 dynamic_dump_required and 449 unsupported_shape. The unresolved file contains 1,224 rows, and 2,640 callers were corrected using classfile evidence.
- 验证：JSON and CSV both contain 35,644 rows; IDs span sm-000001 through sm-035644; unresolved statuses are exclusive; no exported decoded value contains isolated surrogates; 12 tests pass; expireTime, token and ClawWorkspace are present as decoded_static.
- 下一步：Generate a separate annotated CFR source tree from the stable map without mutating the frozen source tree.

## 2026-06-21 01:57｜Generate a separate annotated CFR source tree without changing frozen inputs.
- 目标：Generate a separate annotated CFR source tree without changing frozen inputs.
- 动作：Implemented stable per-line STRING_MAP comments, safe JSON-style plaintext escaping, unresolved-status comments, multi-call ordering, complete tree copying and destination-safety checks; generated the real annotated tree.
- 结果：The annotated tree contains all 4,226 Java files and exactly 35,644 mapping comments. Original CFR sources remain byte-for-byte identical to the frozen baseline.
- 验证：Annotation tests pass including CR/LF, tabs, comment terminators, two calls on one line and idempotent regeneration; real comment/file counts and full source-tree hashes match expectations.
- 下一步：Perform a deterministic 20-sample semantic audit across UI, startup, network, Chinese text, synthetic callers and unresolved cases.

## 2026-06-21 02:00｜Semantically verify 20 deterministic records from the final string map.
- 目标：Semantically verify 20 deterministic records from the final string map.
- 动作：Selected five URL/path, five JSON/Cookie field, five UI-message and five startup/login/workspace records; recorded full source, caller, ciphertext, plaintext and context evidence; updated the historical report and verification log with final coverage.
- 结果：All 20 samples are coherent with their immediate Java usage; zero records were rejected or downgraded. StartApp token/expireTime and ClawWorkspace/JLogin/JProductSelector UI strings are explicitly included.
- 验证：The embedded JSON summary contains 20 unique stable IDs, all exist in the final map with decoded_static status, and asserts coherent=20/rejected=0; context and diff checks pass.
- 下一步：Run Threadtear preflight and only bytecode-only host cleanup on a copied JAR.

## 2026-06-21 02:08｜Run only source-verified bytecode-only Threadtear cleanup on an immutable JAR copy.
- 目标：Run only source-verified bytecode-only Threadtear cleanup on an immutable JAR copy.
- 动作：Implemented manifest preflight; verified official Threadtear 3.0.1 release and source; downloaded and fingerprinted Threadtear/JDK; source-audited RemoveUnusedVariables; compiled a minimal no-GUI driver; ran it on a hash-identical copy; re-decompiled output with CFR and compared structure/bootstrap semantics.
- 结果：Threadtear produced a valid 4,226-class output JAR without loading target classes. CFR retained 4,226 Java files and identical bootstrap semantic multiset; warnings remained 5,757, with only small line-count reductions.
- 验证：Preflight tests pass; official cleanup source contains no forbidden runtime-loading markers; input hashes match; output ZIP opens and contains StartApp.class; CFR completed successfully; bootstrap remains 73,600/73,597.
- 下一步：Generate prioritized dynamic-dump targets and a stop-and-screenshot offline Windows VM runbook.

## 2026-06-21 02:25｜Prepare offline Windows VM dynamic-dump package without running App.jar on host.
- 目标：Generate prioritized dynamic-dump targets and a stop-and-screenshot offline Windows VM runbook.
- 动作：Implemented unresolved-family grouping and priority scoring; exported javaagent target TSV; added a minimal ASM javaagent that records only configured static `String -> String` decoder returns; built a synthetic fixture smoke test; packaged `App.jar`, agent, Threadtear dependency, target lists, README and JRE into a read-only-transfer ISO.
- 结果：Dynamic targets contain 131 families: 5 critical, 9 high and 117 normal. The first offline VM pass is limited to critical targets, with high targets gated by screenshot/dump review and normal targets disabled by default. The final ISO is `.artifacts/dynamic-dump-package.iso` with SHA-256 `69C4B17704BFA2165C541FF16DDE4E288C73419D6CDE61DFDA0A7611A2A1D0C4`.
- 验证：Target builder test passed; generated verification targets matched the committed artifact outputs; agent compiled with the bundled JDK; synthetic-only javaagent smoke recorded `cipher -> cipher-plain`; ISO opened with Joliet long names and contains README, App.jar, agent, Threadtear jar, target JSON/TSV and `jre/bin/java.exe`; `git diff --check` passed.
- 下一步：Run final M2 verification gate, update status/report logs, and keep the actual dynamic dump as a manual offline-VM procedure.

## 2026-06-21 02:34｜Close the M2 full static string-decryption gate.
- 目标：Run final M2 verification gate, update status/report logs, and keep the actual dynamic dump as a manual offline-VM procedure.
- 动作：Updated current status, string decode report, verification log and bug log with the final static/dynamic boundary; re-ran the full unit suite, Python syntax compilation and artifact assertions after documentation changes.
- 结果：M2 Phase 3 static deliverables are internally consistent and ready for M3 use; dynamic dump remains explicitly unexecuted on the host and is documented as a later stop-and-screenshot Windows offline VM workflow.
- 验证：`python -m unittest discover -s tests -v` passed 17/17; in-memory compile covered 14 Python tool files; artifact gate asserted string-map, unresolved, inventory, dynamic targets and ISO hash; `git diff --check` exited 0.
- 下一步：Either run the manual offline VM critical-target dump with screenshots, or proceed to M3 using the completed static evidence set.

## 2026-06-21 20:03｜Repackage offline dynamic dump ISO with runtime dependencies.
- 目标：Fix the offline VM critical dump package after javaagent startup exposed missing App runtime dependencies.
- 动作：Created `.artifacts/dynamic-dump-package-full` with original `app/` resources, full `lib/` dependency directory, JRE, agent, targets and `RUN-CRITICAL.cmd` / `RUN-HIGH.cmd`; generated `.artifacts/dynamic-dump-package-full.iso`.
- 结果：The full ISO preserves the verified App and agent hashes while providing the manifest classpath layout needed by `app\App.jar`.
- 验证：Full ISO size is 1,123,694,592 bytes; SHA-256 is `59BB024ADD397B74964E2C46854A8B3216EF516813E8E98C2FF41B70EBF4D559`; ISO contains `RUN-CRITICAL.cmd`, `RUN-HIGH.cmd`, `app`, `lib` and `jre`; App hash remains `9084FABCE357AAD8B18D06D0FB708DE4E92E1B5D63686CEA1DED49E19F73A99B`, agent hash remains `DDB0DD9E4F4BAEBE238DADAB93784A698449A8BBFBBDD8A60C37E8E638D081A9`.
- 下一步：Mount `.artifacts/dynamic-dump-package-full.iso` in the offline Windows VM, copy it to `C:\m2dump`, run `RUN-CRITICAL.cmd`, stop and screenshot before any login or credential entry.

## 2026-06-21 20:37｜Harden dynamic dump agent after offline VM critical run.
- 目标：Prevent javaagent observation failures from crashing the offline App run.
- 动作：Added a regression test proving `DumpHooks.record(...)` must not propagate writer failures; wrapped dump record emission in a guarded block; rebuilt `dynamic-string-dump-agent.jar`; updated `RUN-CRITICAL.cmd` and `RUN-HIGH.cmd` to include `-noverify`; generated `.artifacts/dynamic-dump-package-full-v2.iso`.
- 结果：The critical run package now tolerates null/recording failures and bytecode verifier friction while keeping the VM offline workflow unchanged.
- 验证：`python -m unittest tests.test_dynamic_dump_agent_hooks -v` passes; rebuilt agent SHA-256 is `BE48399CFD98FF2A39821FC28C4D154F2E97FEC67F4716A8F359179E4233C665`; full v2 ISO SHA-256 is `568FD870A0BFF8BEE8491A44960817F42DF3F7D41A3FC70A582AFF127791BB13`; ISO contains `RUN-CRITICAL.cmd`, `app`, `lib` and `jre`, and javap confirms the `record failed` guard.
- 下一步：Mount `.artifacts/dynamic-dump-package-full-v2.iso`, refresh `C:\m2dump` from `D:\`, run `RUN-CRITICAL.cmd`, then stop and screenshot the console and any app window.

## 2026-06-21 21:02｜Rebuild dynamic dump agent with verifier-friendly return handling.
- 目标：Fix the v2 offline VM JVM crash caused by stack-shuffle instrumentation.
- 动作：Changed the ASM return hook from `DUP`/`SWAP` stack manipulation to `ASTORE 1` plus explicit `ALOAD` restoration; removed `-noverify` from the run scripts; rebuilt `dynamic-string-dump-agent.jar`; generated `.artifacts/dynamic-dump-package-full-v3.iso`.
- 结果：The agent no longer relies on verifier bypass and the inserted bytecode records a saved return value before restoring it to the original `ARETURN`.
- 验证：`python -m unittest tests.test_dynamic_dump_agent_hooks -v` passes; javap of `DynamicStringDumpAgent$Transformer$1$1` shows `visitVarInsn(ASTORE, 1)` / `visitVarInsn(ALOAD, 1)` and no `SWAP`; rebuilt agent SHA-256 is `E84287BD19EAEBB0444A4FF8EDD3E173D164793B6A2E17A515C1E8708E7D1E36`; full v3 ISO SHA-256 is `452EF7118A8FE2666804C6CFC853C1702A64F801F83F7EF12621BB955B34BB49`.
- 下一步：Mount `.artifacts/dynamic-dump-package-full-v3.iso`, refresh `C:\m2dump`, run `RUN-CRITICAL.cmd`, then stop and screenshot output.

## 2026-06-21 21:13｜Self-audit v3 verifier failure and generate output-only v4 agent.
- 目标：Stop the repeated offline VM failures by removing the invalid assumption that target local slot 0 still contains the input string at return time.
- 动作：Reviewed v3 bytecode and VM `VerifyError`; changed the return hook to duplicate only the return value and call `DumpHooks.recordOutput(output, family)`; stopped reading target method locals entirely; added a smoke test that compiles and runs the javaagent with verifier enabled from an ASCII path; generated `.artifacts/dynamic-dump-package-full-v4.iso`.
- 结果：v4 trades away dynamic input capture for verifier safety; static artifacts already retain encrypted inputs, while dynamic output/family/caller evidence is the needed missing piece.
- 验证：`python -m unittest tests.test_dynamic_dump_agent_hooks -v` passes and runs a real javaagent smoke; javap confirms the hook emits `DUP` + `recordOutput` and no target `ALOAD 0`, `ASTORE` or `SWAP`; rebuilt agent SHA-256 is `546F525CB28A84F7DCCAF8F941D1D2D87AEBBDB46A41B876420EB0FE37B14EDD`; full v4 ISO SHA-256 is `ADD3AAF110665D2855B507FCAF7D6E1C7B485CC27ED4FA79A70BA8AC423E1F90`.
- 下一步：Mount `.artifacts/dynamic-dump-package-full-v4.iso`, refresh `C:\m2dump`, run `RUN-CRITICAL.cmd`, then stop and screenshot output.

## 2026-06-21 21:28｜Complete offline critical dynamic dump and observer hash verification.
- 目标：Export the first critical dynamic dump without enabling network or shared folders.
- 动作：Ran v4 javaagent in the isolated Windows VM, stopped at the login page without credentials, wrote `C:\dump\strings.jsonl`, copied it to a one-time `DUMPXFER` virtual disk as `strings-critical.jsonl`, detached the disk from Windows, attached it to `lab-observer`, mounted `/dev/sdb1` read-only, and hashed the exported file.
- 结果：Critical dump produced 35 JSONL records, including login UI/path outputs from `JSetupDialog$JLoginNew.N`, `JLoginHTML$h.V`, and `JTestFrame$JLoginNew$2.k`.
- 验证：Windows VM hash and observer hash both equal `cecf8fab4cd92f6f8fed89edf743144d81eea1571127401880ec9b02dd83409`; observer mounted `DUMPXFER` as NTFS read-only at `/mnt/dumpxfer`.
- 下一步：Unmount `/mnt/dumpxfer`, detach `dump-transfer.vdi` from `lab-observer`, restore the Windows VM to `windows-offline-clean`, then import/analyze the critical dump before deciding whether any high-priority pass is justified.

## 2026-06-21 21:45｜Import observer-verified critical dump into host artifacts.
- 目标：Bring the observer-verified `strings-critical.jsonl` back into the host project without trusting screenshots or manual retyping.
- 动作：Decoded the pasted base64 payload from the observer terminal, stripped the trailing shell prompt, wrote `.artifacts/analysis/strings-critical.jsonl`, preserved `.artifacts/analysis/strings-critical.b64`, and generated `.artifacts/analysis/strings-critical-summary.json`.
- 结果：The host artifact contains 35 JSONL records across three families: `JSetupDialog$JLoginNew.N` (29), `JTestFrame$JLoginNew$2.k` (4), and `JLoginHTML$h.v` (2). Two critical target families did not execute during the login-page stop point: `JProductSelectorHtml$d.L` and `g$JMainMaster$4.r`.
- 验证：Host SHA-256 is `cec8fefab4cd92f6f8fed89edf743144d81eea1571127401880ec9b02dd83409`, matching both the Windows VM and observer hashes; JSON parsing counted 35 rows.
- 下一步：Decide whether the two missed critical families require a second strictly offline trigger path, or whether current evidence is sufficient to start M3 interface mapping.

## 2026-06-21 22:58｜Start M3 authorization seam/interface inventory from M2 static string decode and critical dynamic dump evidence.
- 目标：Start M3 authorization seam/interface inventory from M2 static string decode and critical dynamic dump evidence.
- 动作：Read current-status and work-log first; reviewed M2 string decode, bootstrap, semantic samples and critical dynamic dump summary; traced StartApp login callback, SBFApi /getInfo, product selector, JSBFMain permission/config consumption, token bridge cache and payment/order entrypoints; created .context/seams.md and updated current-status/INDEX.
- 结果：M3 first-pass seam inventory identifies `StartApp$1.a(JSONObject)` as the main startup gate, `SBFApi.h(String)` `/getInfo` as the primary interface seam, `StartApp$1$3` as the product-selector-to-main transition, `JSBFMain` roles/ucf as downstream config consumption, `StartApp.f`/`jxbrowser.n` as token/header cache, and payment/order paths as later M5 classification targets.
- 验证：Documentation-only step; evidence cross-checked against annotated CFR source, bootstrap map summary, strings-critical-summary.json and existing M2 reports; context validation passed; `git diff --check` exited cleanly with existing CRLF conversion warnings only.
- 下一步：Use `.context/seams.md` to choose the M4 minimal patch strategy; optionally run a second strictly offline dynamic trigger only if the two missed critical families are needed before patching.
