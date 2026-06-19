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
