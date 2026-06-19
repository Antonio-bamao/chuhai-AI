# 当前状态

- 当前阶段：M2 已达到验收门槛，准备转入 M3 授权接缝清单
- 已完成：已阅读外部工程设计文档与 SOP 六件套；已初始化 `.context`；已将目标、边界、契约、里程碑和风险迁入仓库上下文；已完成资产清点、原始备份、备份完整性校验、静态启动线索记录和源码树导出；已记录 ADR-0002 说明用 CFR 替代 JADX 的原因；已实现 `tools/decode_java_strings.py` 并生成扩展版 `string_map.json`，覆盖 9 类静态字符串解码器；已产出并更新 `seam-candidates.md` 草稿；已新增 `tools/decode_bootstrap_calls.py`，生成 `bootstrap_map.json` 与 `startapp_bootstrap_candidates.json`。
- 进行中：已区分 token 缓存 `expireTime` 与账号授权期限 `periodTime`；前者只控制 `header` 缓存并在到期前 60 秒失效，缺失/0 时默认约 1 小时，不直接影响 UI/启动；后者会触发过期/临期提示并在 `JReadme` 显示，但提示后仍继续进入产品选择器。已确认更直接的启动门槛是软件开通列表、token 和 `SBFApi.h(...).result.code == 200`；M1-03 原始启动验证仍等待隔离/断网环境确认。
- 下一步：转入 M3，生成正式 `seams.md`；重点追踪 `StartApp$1.a(JSONObject)`、`JProductSelectorHtml$a -> ClawWorkspace`，以及 `roles`、`overdue`、套餐/功能配置对普通版降级的影响。
- 阻塞项：尚未确认隔离验证环境使用 VMware、VirtualBox 还是另一台机器；尚未确认授权以远端返回还是本地许可文件为主。
- 当前活跃日志分片：work-log.md
