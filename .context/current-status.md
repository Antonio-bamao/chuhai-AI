# 当前状态

- 当前阶段：M2 Phase 3 全量字符串静态解密已完成；动态 dump 保留为手动离线 Windows VM 步骤，暂不进入 M3。
- 已完成：已阅读外部工程设计文档与 SOP 六件套；已初始化 `.context`；已完成资产清点、原始备份、备份完整性校验、静态启动线索记录和源码树导出；已记录 ADR-0002 说明用 CFR 替代 JADX 的原因；已解码 bootstrap 调用；已完成 348 个字符串 family 的全候选分类、35,644 行统一 JSON/CSV、1,224 行 unresolved、带 `STRING_MAP` 注释源码树、20 条语义抽检、只读 Threadtear 去花阅读副本，以及离线动态 dump ISO/runbook。
- 进行中：动态 dump 尚未执行。当前准备包只用于 Windows 离线 VM，第一轮限定 5 个 critical family，第二轮 high 目标需根据第一轮截图和 dump 决定；本机没有把 agent 挂载到 `App.jar`。
- 下一步：如要补齐动态证据，按 `.context/dynamic-dump-runbook.md` 手动创建断网 Windows VM 并逐步截图；若暂缓动态 dump，可在当前静态证据基础上进入 M3 接缝清单。
- 阻塞项：动态 dump 需要用户手动准备 Windows 离线 VM 并回传截图；尚未确认授权以远端返回还是本地许可文件为主。
- 当前活跃日志分片：work-log.md
