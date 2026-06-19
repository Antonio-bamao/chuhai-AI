# 当前状态

- 当前阶段：M2 字符串解密 / j2026 静态字符串已扩展
- 已完成：已阅读外部工程设计文档与 SOP 六件套；已初始化 `.context`；已将目标、边界、契约、里程碑和风险迁入仓库上下文；已完成资产清点、原始备份、备份完整性校验、静态启动线索记录和源码树导出；已记录 ADR-0002 说明用 CFR 替代 JADX 的原因；已实现 `tools/decode_java_strings.py` 并生成扩展版 `string_map.json`，覆盖 `N(...)`、`k(...)`、`JLoginHTML$h.v(...)`。
- 进行中：继续 M2，处理 invokedynamic/bootstrap 形态；M1-03 原始启动验证仍等待隔离/断网环境确认。
- 下一步：分析 `ClawWorkspace.vv(...)`、`JLoginNew.vS(...)`、`StartApp.Sy(...)`，并基于 225 条授权候选反查调用链。
- 阻塞项：尚未确认隔离验证环境使用 VMware、VirtualBox 还是另一台机器；尚未确认授权以远端返回还是本地许可文件为主。
- 当前活跃日志分片：work-log.md
