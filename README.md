# 花卉苗圃 · 温室育苗与出圃管理系统

城郊花圃的育苗管理：**温室与苗床台账**、**品种与育苗批次**、**苗床占用**、**出圃发货**。

## 技术栈

Spring Boot 3.3（Java 17）+ MySQL 8.0 + Redis 7 + Vue 3 + Element Plus + Vite + nginx，全栈 `docker compose` 一键启动。

## 启动

```bash
./start.sh              # 等价于 docker compose up -d --build
```

| 入口 | 地址 |
| --- | --- |
| 前端页面 | http://127.0.0.1:8203/ |
| 后端接口 | http://127.0.0.1:8303/api/ |
| MySQL | 127.0.0.1:3503（库 `nursery_farm`） |
| Redis | 127.0.0.1:6503 |

先起 MySQL / Redis，等 healthcheck 通过再起前后端（compose 里已经串好依赖）。

## 停止

```bash
docker compose down       # 保留数据卷
docker compose down -v    # 连数据卷一起删，下次启动重新灌种子数据
```

## 端口与库名

都在 `.env` 里改，`.env.example` 是同一份模板。容器名统一是
`claude-qd-003-{mysql,redis,backend,frontend}`。

## 业务模块

### 1. 温室与苗床台账（`greenhouse` / `seedbed`）

温室编号 `GH-xx` 唯一，类型分育苗棚 / 成苗棚 / 炼苗棚，状态 `在用 / 停用`。
苗床编号 `SB-xxx` 唯一，归属到一个温室（也可以先不归），带「可放株数」，状态 `在用 / 空置 / 维修`。
苗床上还有没出圃的批次时，不许改可放株数、也不许改成非在用状态。

- 页面：温室与苗床（`/greenhouses`）
- 接口：`GET/POST /api/greenhouses`、`PUT /api/greenhouses/{id}/status`、`GET/POST /api/seedbeds`、`PUT /api/seedbeds/{id}`

### 2. 品种与育苗批次（`variety` / `nursery_batch`）

品种编号 `V-xxxx` 唯一，类别草本 / 木本 / 多肉 / 蔬果，状态 `在售 / 停用`；停用的品种不能开新批次。
批次号 `NB-xxxx` 自动生成，状态机 `育苗中 → 待出圃 → 已出圃`；`育苗中 / 待出圃` 都能报废，
`已出圃` 不能报废。转待出圃时要写实际成苗株数。

- 页面：品种与育苗批次（`/batches`）
- 接口：`GET/POST /api/varieties`、`PUT /api/varieties/{id}/status`、`GET/POST /api/batches`、`POST /api/batches/{id}/advance?action=&actualQty=`

### 3. 苗床占用（床位账 `bed_occupancy`：批次 × 苗床 × 日期段）

一批苗占着一张苗床的一段时间，这段日期记在床位账（`bed_occupancy` 占用段，`to_date` 为空表示还占着），
批次账上的 `seedbed_id` 只表示这批现在落在哪张床。开批次时校验：品种在用、苗床在用、苗床所在温室在用、
计划株数不超过苗床可放株数、结束不早于开始；同一张苗床上两段占用区间不能重叠，撞了要说出撞的是哪一批。
出圃或报废之后批次状态变成已出圃/已报废，旧占用段不再占位。

- 页面：苗床占用（`/beds`）
- 接口：同上（开批次）、`GET /api/occupancies`

### 4. 转棚调拨（批次账 `bed_transfer`，从育苗棚换到炼苗棚走这张单）

场长的规矩：**只改批次上的棚名字、床位不跟着走不算数；占用转移必须有调拨单**。
调拨单要写清：从哪张床（`fromSeedbedId`）到哪张床（`toSeedbedId`）、谁经手（`operator`）、
计划哪天起迁、哪天迁完（`planStartDate` / `planEndDate`）、调多少株。

一张调拨单开出来就**直接落账**（一个事务做完）：

- 调入床在占用日期上不能跟别的批次撞（半开区间重叠校验，报出撞的是哪一批）；
- 调拨株数不能超过调入床可放株数；调入床及其所在温室必须在用；
- 落账即把旧床占用段在起迁日切掉（旧床让出，不能两边都占着），新床从起迁日开一段，批次床位跟着改到新床；
- 同一批次被并发调到两张不同的床：先落账那笔成功，后到的发现调出床已对不上 → 整笔失败；
- 中途任何一步失败都回滚，不会出现「新床已占、旧床没放」的半截状态。

- 页面：转棚调拨（`/transfers`），批次页「转棚」按钮带批次跳转
- 接口：`GET/POST /api/transfers`

### 5. 出圃发货（`shipment`）

发货单号 `SH-xxxx` 自动生成，挂在一个批次上。只有 `待出圃 / 已出圃` 的批次能发货；
一批苗累计发出去的株数（**退回来的不算**）不能超过它的实际成苗株数。
状态机 `待发货 → 已发货 → 已签收`，已发货还没签收的可以退回；发车必须写承运人。

- 页面：出圃发货（`/shipments`）
- 接口：`GET/POST /api/shipments`、`POST /api/shipments/{id}/advance?action=&carrier=&shipDate=`

## 目录

```
backend/src/main/java/com/nursery/farm/
├── config/       CORS 配置
├── controller/   REST 入口
├── dto/          BizException + 统一错误响应
├── entity/       5 张业务表
├── repository/   Spring Data JPA
└── service/      业务规则（编号唯一、日期区间占用、状态机、发货核减）
backend/src/main/resources/schema.sql   建表 + 种子数据（挂进 MySQL initdb）
frontend/src/views/                     4 个业务页面
```
