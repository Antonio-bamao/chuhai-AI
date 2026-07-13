# D-5 WhatsApp AI龙虾客服独立本地页设计

## 目标

将 WhatsApp AI龙虾客服（`wskefu`）的七个子菜单从旧客服会话页和归一化兜底迁移为各自独立的 `/pc/local/wa/<leaf>` 本地空态页。每页只提供对应标题、贴合语义的离线提示和禁用守门，不实现扫码、登录、账号接入、联系人读取、群发或消息收发。

## 显式叶路由

| 菜单代码 | 菜单名称 | 叶路由 |
| --- | --- | --- |
| `C4936_000` | 信息总览 | `/pc/local/wa/overview` |
| `C4936_001` | 账号分组 | `/pc/local/wa/account-groups` |
| `C4936_002` | 账号列表 | `/pc/local/wa/account-list` |
| `C4936_004` | 联系人数据池 | `/pc/local/wa/contact-pool` |
| `C4936_005` | 爆粉群发 | `/pc/local/wa/fan-broadcast` |
| `C4936_006` | 群聊群发 | `/pc/local/wa/group-broadcast` |
| `C4936_007` | 客服列表 | `/pc/local/wa/customer-service-list` |

## 页面契约

- 页面标记：`D5_WA_LOCAL_PAGE`。
- 每页有唯一 `data-d5-wa-route`、标题、离线提示和 `data-d1-action="guarded" disabled` 按钮。
- 所有按钮仅为视觉守门；不产生登录、会话、消息、联系人、群发或网络动作。

## 候选边界和验收

- 从当前 GEO live `App.dll` 构建；只允许 `SBFApi.class` 与 `M5LocalSpiderBridge.class` 两个 JAR 条目变化。
- 先跑红绿路由/页面/覆盖层测试，再跑全量回归、隔离冷启、180 次非回环 TCP 采样和 DB `848/858/858` 复核。
- 仅在用户单独授权后才允许 live swap；swap 后还需用户手工验收 7 页。
