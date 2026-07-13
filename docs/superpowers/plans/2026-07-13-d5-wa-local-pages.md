# D-5 WhatsApp AI龙虾客服独立本地页执行计划

1. 为七个 `C4936_*` 菜单建立显式 `/pc/local/wa/<leaf>` 目录和路由/页面/两类覆盖层红测。
2. 实现目录、桥接页和 `--d5-wa-overlay`；仅修改 `M4RecoveryCatalog.java`、`M5LocalSpiderBridge.java`、`M4AuthPatch.java` 及测试。
3. 构建候选，确认 JAR 精确两类差异，并运行全量回归。
4. 在不影响 GEO live 的隔离 runner 内完成冷启、180 次非回环 TCP 采样及 DB 不变量复核；随后恢复 runner。
5. 报告候选结论，等待用户单独授权才做 live swap 和 7 页肉眼验收。
