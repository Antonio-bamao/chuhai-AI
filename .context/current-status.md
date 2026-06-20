# 当前状态

- 当前阶段：继续 M2，按外部 Phase 3 完整标准补齐全量字符串解密
- 已完成：已阅读外部工程设计文档与 SOP 六件套；已初始化 `.context`；已将目标、边界、契约、里程碑和风险迁入仓库上下文；已完成资产清点、原始备份、备份完整性校验、静态启动线索记录和源码树导出；已记录 ADR-0002 说明用 CFR 替代 JADX 的原因；已实现 `tools/decode_java_strings.py` 并生成扩展版 `string_map.json`，覆盖 9 类静态字符串解码器；已产出并更新 `seam-candidates.md` 草稿；已新增 `tools/decode_bootstrap_calls.py`，生成 `bootstrap_map.json` 与 `startapp_bootstrap_candidates.json`。
- 进行中：现有 9 类静态解密器已解码 4,599 条调用，但启发式全量扫描发现约 348 个候选调用族、35,642 个候选调用；尚缺全候选分类、CSV、带明文注释源码树、去花阅读副本和动态 dump 待办/runbook。已批准采用静态全量分类、注释副本、去花阅读副本、最后手动动态 dump 的分阶段方案。
- 下一步：复核并批准 `docs/superpowers/specs/2026-06-21-m2-full-string-decryption-design.md`，随后编写逐步实施计划；暂不进入 M3。
- 阻塞项：尚未确认隔离验证环境使用 VMware、VirtualBox 还是另一台机器；尚未确认授权以远端返回还是本地许可文件为主。
- 当前活跃日志分片：work-log.md
