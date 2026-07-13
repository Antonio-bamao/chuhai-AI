# D-5 GEO 本地页面骨架设计

## 目标

在已锁定的 TG live 基线之上，为海外 GEO 的九个现有一级菜单生成各自独立的本地空态页；本轮只交付候选 DLL，不覆盖 `data/app/App.dll`。

## 范围与路由

`C4134_002`、`C4134_003`、`C4134_006`、`C4137_001..006` 分别改为 `/pc/local/geo/google-seo`、`precise-number-mining`、`google-geo-media`、`global-number-collect`、`global-region-collect`、`customs-data-mining`、`global-company-data`、`global-big-data`、`number-ai-active-filter`。每页包含独立标题、贴合菜单语义的离线提示、`D5_GEO_LOCAL_PAGE` 与 `data-d1-action="guarded" disabled`。

## 实现边界

只修改 `M4RecoveryCatalog.java` 与 `M5LocalSpiderBridge.java`，候选 JAR 覆盖层只替换 `SBFApi.class` 和 `M5LocalSpiderBridge.class`。不改变 TG/X/Ins/FB/TikTok/WhatsApp 路由；不改 `.cnf`、`cloud.spider.b`、`libmytrpc`、系统时钟、采集链、数据库或桌面快捷方式。

## 验收

先让 GEO 路由、页面和两类 class 增量测试分别红绿；构建候选后执行完整回归（不得低于 84 项）、隔离冷启的 180 次非 loopback TCP 采样、DB `848/858/858` 复核。三门通过后才向用户申请单独的 live swap 授权；未获授权不得替换 TG live 基线。
