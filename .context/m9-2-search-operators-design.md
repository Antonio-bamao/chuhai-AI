# M9-2 搜索算子配置化 Walking Skeleton 设计

## 目标

在不修改 `.cnf`、`cloud.spider.b`、`libmytrpc` 的前提下，把现有 Google 外部搜索任务的选项改为本地 JSON 数据源驱动；通过原 UI 选择第一组 Facebook 定向 Google dork，清除历史 mock，跨运行去重后新增至少 10 条真实唯一数据。

二元验收：

> UI 选中 Facebook 算子，经 `cloud.spider.a.a(Long) → cloud.spider.b` 执行，按跨运行去重键落库至少 10 条真实唯一数据：通 / 卡点。

## 明确非目标

- 不做 Facebook 登录态或平台原生采集。
- 不做多平台矩阵、批量执行、算子管理 UI。
- 不修改采集核心、`.cnf` 或 native 组件。
- 不改系统时钟。
- 不把历史 mock 或测试数据计入真实结果。

## 方案选择

采用外部 JSON：

`data/app/config/search-operators.json`

原因：

- 比 SQLite 配置表少迁移、DAO 和管理成本。
- 比 App.dll 内嵌资源更容易在阶段2增删改。
- UI 继续消费既有 `dataCollect/platform/list` 契约，不新增页面组件。

## 配置契约

```json
{
  "version": 1,
  "searchSites": [
    {
      "code": "google.com",
      "label": "Google"
    }
  ],
  "areas": [
    {
      "code": "+1",
      "label": "美国/加拿大 +1"
    }
  ],
  "operators": [
    {
      "code": "facebook.com",
      "label": "Facebook 公开电话线索",
      "platform": "facebook.com",
      "defaultGoogSite": "google.com",
      "queryMode": "legacy_site_dork",
      "queryTemplate": "site:{pltCode} {keywords} {areaCode}",
      "enabled": true
    },
    {
      "code": "google.com",
      "label": "Google",
      "platform": "google.com",
      "defaultGoogSite": "google.com",
      "queryMode": "legacy_web",
      "queryTemplate": "{keywords} {areaCode}",
      "enabled": true
    }
  ],
  "keywords": [
    {
      "code": "local-test",
      "label": "local-test"
    }
  ]
}
```

字段规则：

- `code` 是原 UI `pltCode` 的值。
- `defaultGoogSite=google.com` 表示搜索引擎仍是 Google。
- `queryTemplate=site:{pltCode} ...` 表示 Google dork 约束目标域名；不是打开或登录 Facebook。
- `queryMode` 由本地任务桥校验并写入任务溯源信息；采集核心仍消费既有 `googSite/areaCode/pltCode/keywords`。
- 阶段1只增加 Facebook 一组定向算子，不预留管理 API。

## 缺失和损坏回退

`M5LocalSpiderBridge` 持有当前已跑通选项的内置默认配置：

- Google 搜索站点；
- 美国/加拿大 `+1`；
- Facebook 和 Google 平台选项；
- 现有关键词默认项。

读取顺序：

1. 尝试读取外部 JSON。
2. 校验根对象、数组和至少一个可用 Google 搜索项。
3. 缺失、空文件、JSON 解析失败或校验失败时，记录回退日志。
4. 返回内置默认配置，UI 下拉不得为空。

必须有自动测试覆盖：

- 文件不存在；
- 文件内容损坏；
- 两种情况下 UI 仍获得默认选项；
- 生成的任务仍包含 `googSite=google.com` 和可执行的 Facebook/Google 参数。

## UI 与执行数据流

1. 原任务表单请求 `googSite/areaCode/pltCode/keywords` 选项。
2. Web bridge 调用本地 Java bridge，而不是返回 JavaScript 字符串常量。
3. Java bridge读取 `search-operators.json`，按原接口结构返回分组下拉数据。
4. 用户选择 `pltCode=facebook.com`。
5. 提交任务时，本地桥按 `pltCode` 解析 operator，补齐/校验：
   - `googSite=google.com`
   - `pltCode=facebook.com`
   - `queryMode=legacy_site_dork`
   - `queryTemplate=site:{pltCode} {keywords} {areaCode}`
6. 队列与 runner 继续走现有 `cloud.spider.a.a(Long) → cloud.spider.b`。
7. `.cnf` 仍只收到并消费原有参数；不改其内容。

## Mock 清理护栏

删除判定必须同时具备明确 mock 特征，不能只按 `time=0`：

- `json_data.source == "local-ui-mock"`；或
- `json_data.submitted == false` 且 URL 为 `https://example.com/local-ui-mock`。

操作顺序：

1. 只读 dry-run，输出待删行的 `id/time/source/url` 脱敏清单。
2. 备份整个 SQLite 文件到 `.artifacts/backups/m9-2-.../`。
3. 再次读取并确认待删数量为 9、总数为 47。
4. 在单事务内删除精确 id 集合。
5. 提交后确认总数 38、mock 数 0；否则回滚/从备份恢复。

生产入口处理：

- 移除 `MiJava.m5WriteLocalMockResult` 的生产注入方法。
- 保持 `InjectJsCallback` 中不存在 auto-seed、`local-ui-mock` 或 mock 写入入口。
- 测试辅助逻辑不得暴露给生产页面。

## 跨运行去重

`spider_code` 只作溯源，不进入唯一键。

去重键：

`normalized(platform) + normalized(url) + normalized(phone)`

规范化：

- `platform`：小写、去首尾空白；优先 `pltCode`，其次 URL host。
- `url`：host 小写；移除 fragment；移除尾部 `/`；保留能区分目标内容的 path/query。
- `phone`：仅保留数字和首个前导 `+`；多号码字符串按规范化后的完整值参与阶段1去重。

写入规则：

- 在同一 SQLite 事务中读取既有候选并比较规范化键。
- key 完整且已存在：跳过，记录 duplicate 日志。
- key 完整且不存在：插入。
- URL 或 phone 缺失：不计入本卡“真实唯一数据 ≥10”的验收数。
- 阶段1不改表 schema；避免给历史 JSON 数据做破坏性迁移。

## 测试策略

严格 TDD：

1. 配置文件正常读取与 UI 分组映射。
2. 配置缺失/损坏回退，默认下拉非空且 Google 参数可执行。
3. Facebook operator 解析为 Google `site:facebook.com` dork 元数据。
4. mock dry-run 只命中 9 行；事务删除后 47→38。
5. 生产注入不再暴露 mock 写入口。
6. 相同 URL+phone+platform 跨不同 task/spider trace 不重复写入。
7. 不同 URL 或 phone 可写入。
8. 受影响测试与完整测试集通过。

## 真实验收与证据

四段截图：

1. 原任务表单显示并选中 Facebook operator。
2. 创建/触发任务成功，日志或任务 UI 显示进入本地 runner。
3. SQLite 只读核验：基线 38，新增至少 10 条完整唯一 key 的真实行。
4. 原 UI 结果列表显示新数据。

数据样例必须脱敏；报告给出：

- 清理 dry-run 清单；
- 备份路径和 SHA256；
- 清理前后计数；
- operator 配置；
- 去重跳过/插入计数；
- 候选 App.dll SHA256；
- 最终“通/卡点”。

## Git 与现场隔离

- 根工作树入场已有 6 个修改、3 个未跟踪文件，均视为用户现场。
- M9-2 在独立 `codex/m9-2-search-operators` worktree/branch 实现。
- 不清理、不提交、不覆盖根工作树已有改动。
- 产物与运行证据放 `.artifacts/`。

