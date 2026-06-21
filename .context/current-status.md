# 当前状态

- 当前阶段：M3 Phase 4 资源明文关键词定位进行中。首版 `.context/seams.md` 已完成 Java 授权接缝，Phase 4 Step 1–2 已完成资源入口盘点和只读批量解密。
- 已完成：已阅读外部工程设计文档与 SOP 六件套；已初始化 `.context`；已完成资产清点、原始备份、备份完整性校验、静态启动线索记录和源码树导出；已记录 ADR-0002 说明用 CFR 替代 JADX 的原因；已解码 bootstrap 调用；已完成 348 个字符串 family 的全候选分类、35,644 行统一 JSON/CSV、1,224 行 unresolved、带 `STRING_MAP` 注释源码树、20 条语义抽检、只读 Threadtear 去花阅读副本，离线动态 dump ISO/runbook，宿主侧 critical dump 导入与摘要，M3 首版授权接口清单，Phase 4 Step 1 资源入口盘点，以及 Step 2 只读资源解密工具与 7 个明文资源输出。
- 进行中：M3 已定位启动主门槛为 `StartApp$1.a(JSONObject)` -> `SBFApi.h(token)` `/getInfo` -> `StartApp$1$3` -> `JSBFMain`；`ResourceDecoder` 已复用 `ch.r` 解出 `master.html`、`msg.html`、`fm.js`、`country_ips.json` 和 3 个 j2026 HTML，下一步需区分授权、普通登录、业务 token 与支付关键词。
- 下一步：对解密资源和外部明文 CNF 跑授权关键词检索，逐条分类并把有效命中补到 `.context/seams.md`，精确到 `文件:行`。
- 阻塞项：尚未确认授权服务真实返回体全集，以及授权以远端返回还是本地许可文件为主。
- 当前活跃日志分片：work-log.md
