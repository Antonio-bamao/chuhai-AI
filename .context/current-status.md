# 当前状态

- 当前阶段：M2 字符串解密 / bootstrap 动态调用解码
- 已完成：已阅读外部工程设计文档与 SOP 六件套；已初始化 `.context`；已将目标、边界、契约、里程碑和风险迁入仓库上下文；已完成资产清点、原始备份、备份完整性校验、静态启动线索记录和源码树导出；已记录 ADR-0002 说明用 CFR 替代 JADX 的原因；已实现 `tools/decode_java_strings.py` 并生成扩展版 `string_map.json`，覆盖 8 类静态字符串解码器；已产出并更新 `seam-candidates.md` 草稿；已新增 `tools/decode_bootstrap_calls.py`，生成 `bootstrap_map.json` 与 `startapp_bootstrap_candidates.json`。
- 进行中：继续 M2，已还原 `StartApp.Sy(...)` 的目标类/方法/签名，重点覆盖 `StartApp.f(String)`、`StartApp.i()`、`StartApp.k(String)`；已确认 `DTHelper` 是通用 OkHttp 包装器、`jxbrowser.n` 是带 `expireTime` 的状态缓存、`StartApp$5` 更像截图/状态上报任务；M1-03 原始启动验证仍等待隔离/断网环境确认。
- 下一步：反查 `StartApp.f(String)` 的调用者和入参 URL，继续还原 `ClawWorkspace.vv(...)`、`JLoginNew.vS(...)` 的 bootstrap 目标，并追踪 `jxbrowser.n.c()` 的刷新触发点。
- 阻塞项：尚未确认隔离验证环境使用 VMware、VirtualBox 还是另一台机器；尚未确认授权以远端返回还是本地许可文件为主。
- 当前活跃日志分片：work-log.md
