<p align="center">
    <img alt="spzx-logo" src="https://oscimg.oschina.net/oscnet/up-b99b286755aef70355a7084753f89cdb7c9.png" width="160">
</p>
<h1 align="center" style="margin: 20px 0 10px; font-weight: bold;">好物甄选商城 spzx</h1>
<h4 align="center">基于 Spring Boot 3 / Spring Cloud Alibaba 的电商微服务系统 · 商品甄选实战项目</h4>
<p align="center">
    <a href="#"><img src="https://img.shields.io/badge/Spring%20Boot-3.0.5-brightgreen.svg"></a>
    <a href="#"><img src="https://img.shields.io/badge/Spring%20Cloud-2022.0.2-blue.svg"></a>
    <a href="#"><img src="https://img.shields.io/badge/Spring%20Cloud%20Alibaba-2022.0.0.0--RC2-orange.svg"></a>
    <a href="#"><img src="https://img.shields.io/badge/Java-17-yellow.svg"></a>
    <a href="#"><img src="https://img.shields.io/badge/Vue-3.3.9-4FC08D.svg"></a>
    <a href="#"><img src="https://img.shields.io/badge/Element%20Plus-2.4.3-409EFF.svg"></a>
    <a href="#"><img src="https://img.shields.io/badge/License-MIT-blue.svg"></a>
</p>


> 基于 [RuoYi-Cloud](https://gitee.com/y_project/RuoYi-Cloud) v3.6.3 深度二次开发的电商全链路微服务系统，覆盖 **商品管理 → 商城渠道 → 购物车 → 订单 → 支付宝支付 → 会员中心** 完整交易闭环，前端采用 Vue 3 技术栈，是学习电商分布式架构的完整实战项目。

<div align="center">
    <strong>⭐ 如果这个项目对你有帮助，欢迎 Star / Fork / 交流学习 ⭐</strong>
</div>

---

## 📑 目录

- [项目介绍](#-项目介绍)
- [技术栈](#-技术栈)
- [系统架构](#-系统架构)
- [模块说明](#-模块说明)
- [核心功能](#-核心功能)
- [技术亮点](#-技术亮点)
- [快速开始](#-快速开始)
- [Docker 部署](#-docker-部署)
- [目录结构](#-目录结构)
- [常见问题](#-常见问题)
- [License](#-license)

---

## 🚀 项目介绍

尚品甄选商城（spzx）是一套面向电商场景的前后端分离微服务系统，具备**商城前台 + 运营后台**双端能力：

- **商城前台（H5/渠道端）**：首页、商品列表、商品详情、品牌分类浏览、购物车、下单结算、支付宝支付、会员注册登录、收货地址管理等 C 端完整购物流程；
- **运营后台（管理端）**：基于若依 RBAC 权限体系，提供商品管理（分类 / 品牌 / 规格 / 单位 / SPU-SKU）、订单管理、会员管理、代码生成、定时任务、系统监控等 B 端运营能力。

项目以交易链路为骨架，将复杂业务拆分为独立微服务，通过 Nacos 注册发现、Gateway 统一网关、Feign 声明式调用、RabbitMQ 异步解耦，完整呈现了**真实电商系统的微服务落地方式**。

---

## 🛠 技术栈

| 分类 | 技术选型 |
| --- | --- |
| 后端框架 | Spring Boot 3.0.5 · Spring Cloud 2022.0.2 · Spring Cloud Alibaba 2022.0.0.0-RC2 |
| JDK | Java 17 |
| 注册 / 配置中心 | Nacos |
| 网关 | Spring Cloud Gateway（响应式） |
| 认证授权 | 自研 JWT + Redis Token 体系（后台 & H5 双端） |
| 流量控制 | Sentinel（网关流控 + Nacos 持久化） |
| 分布式事务 | Seata |
| ORM | MyBatis-Plus 3.5.3.1 · PageHelper 分页 · Druid 连接池 · 多数据源 |
| 消息队列 | RabbitMQ（Confirm 可靠性投递 + 延迟消息 + 消费去重） |
| 缓存 | Redis（Session/Token/购物车/商品缓存） |
| 文件存储 | MinIO · FastDFS · 本地磁盘 |
| 支付 | 支付宝 alipay-sdk-java |
| 接口文档 | Knife4j (OpenAPI 3) |
| 代码生成 | Velocity 模板 + Gen 模块 |
| 前端 | Vue 3.3.9 · Element Plus 2.4.3 · Vite 5 · Pinia · Vue Router 4 · ECharts · Axios |
| 部署 | Docker Compose · Nginx · Spring Boot Admin 监控 |

---

## 🏗 系统架构

```
                         ┌─────────────────────────────────────────────┐
                         │              Nginx (80)                     │
                         │   静态资源 spzx-ui · 反向代理 /prod-api      │
                         └───────────────────┬─────────────────────────┘
                                             │
                         ┌───────────────────▼─────────────────────────┐
                         │        spzx-gateway 网关 (8080)             │
                         │   认证拦截 · 验证码 · XSS · 黑名单 · 限流     │
                         └───────┬──────────┬──────────┬───────────────┘
                                 │          │          │
                 ┌───────────────▼──┐   ┌───▼───────────────┐
                 │   spzx-auth      │   │  业务微服务集群     │
                 │   认证中心 (9200) │   │  Feign 声明式调用   │
                 └───────────────┬──┘   └───┬───────────────┘
                                 │          │
                        ┌────────▼──────────▼───────────────┐
                        │        Nacos 注册 & 配置中心       │
                        │   Redis · MySQL · RabbitMQ · Seata  │
                        └────────────────────────────────────┘
```

**核心调用链路（下单支付）**：

```
商品详情 ──Redis 缓存──▶ 购物车(spzx-cart) ──▶ 订单(spzx-order)
     │                                              │
     │                                    锁定库存(spzx-product)
     │                                              │
     └────────────▶ 支付宝支付(spzx-payment) ◀──────┘
                           │
                     MQ 支付成功通知 ──▶ 扣减库存 · 更新订单状态
                     MQ 延迟消息(30min) ─▶ 超时未支付自动取消 · 解锁库存
```

---

## 📦 模块说明

```
com.spzx
├── spzx-ui                    // 前端工程（Vue3 + Element Plus）[80]
├── spzx-gateway               // 网关模块：统一鉴权/限流/XSS/验证码 [8080]
├── spzx-auth                  // 认证中心：后台 & H5 双端登录 [9200]
├── spzx-api                   // 接口模块（Feign 客户端 + 降级工厂）
│   ├── spzx-api-system        //   系统接口
│   ├── spzx-api-product       //   商品接口
│   ├── spzx-api-order         //   订单接口
│   ├── spzx-api-cart          //   购物车接口
│   └── spzx-api-user          //   会员接口
├── spzx-common                // 通用模块
│   ├── spzx-common-core       //   核心模块（统一返回/异常/工具类）
│   ├── spzx-common-security   //   安全认证（JWT / 权限）
│   ├── spzx-common-redis      //   缓存服务
│   ├── spzx-common-rabbit     //   MQ 消息服务（可靠性投递）
│   ├── spzx-common-log        //   日志记录
│   ├── spzx-common-datascope  //   数据权限
│   ├── spzx-common-datasource //   多数据源
│   └── spzx-common-seata      //   分布式事务
├── spzx-modules               // 业务模块
│   ├── spzx-system            //   系统管理（用户/角色/菜单/字典...）[9201]
│   ├── spzx-gen               //   代码生成 [9202]
│   ├── spzx-job               //   定时任务 [9203]
│   ├── spzx-file              //   文件服务（MinIO/FastDFS）[9300]
│   ├── spzx-product           //   商品管理（分类/品牌/规格/SPU-SKU/库存）[9205]
│   ├── spzx-user              //   会员中心（注册登录/地址/区域/短信）[9206]
│   ├── spzx-order             //   订单（下单/取消/库存还原）[9207]
│   ├── spzx-channel           //   商城渠道（首页/列表/详情/品牌）[9208]
│   ├── spzx-cart              //   购物车（Redis）[9209]
│   └── spzx-payment           //   支付宝支付 [9210]
└── spzx-visual                // 图形化管理
    └── spzx-visual-monitor    //   监控中心（Spring Boot Admin）[9100]
```

---

## ✨ 核心功能

### 🛒 商城端（C 端）

| 功能 | 说明 |
| --- | --- |
| 首页 | 远程聚合分类、品牌等多模块数据构建首页 |
| 商品列表 | 按分类 / 品牌 / 关键字检索，分页展示 |
| 商品详情 | SPU-SKU 规格联动、价格库存展示（Redis 缓存） |
| 品牌 / 分类 | 一级二级分类树展示与筛选 |
| 购物车 | Redis 存储，商品增删改查、数量调整、价格汇总 |
| 订单 | 确认订单（购物车快照）→ 提交订单 → 锁定库存 |
| 支付 | 支付宝 PC 网页支付，异步回调更新状态 |
| 会员 | 手机号 / 用户名注册登录、个人信息、收货地址管理 |
| 定时取消 | 提交订单 30 分钟未支付自动取消，解锁库存、还原销量 |

### 🧑‍💼 运营后台（B 端）

| 功能 | 说明 |
| --- | --- |
| 用户管理 | 系统用户配置、角色分配 |
| 部门 / 岗位 | 组织机构树、职务配置 |
| 菜单管理 | 菜单 / 目录 / 按钮权限标识配置 |
| 角色管理 | 角色菜单权限、数据范围权限分配 |
| 字典 / 参数 | 固定数据维护、动态参数配置 |
| 通知公告 | 系统公告发布维护 |
| 日志 | 操作日志、登录日志、在线用户监控 |
| 定时任务 | 在线任务调度 + 执行日志 |
| 商品管理 | 商品分类 / 品牌 / 分类品牌关联 / 商品规格 / 单位 / SPU-SKU 上架 |
| 订单管理 | 订单列表查询、订单流程跟踪 |
| 代码生成 | 前后端代码（java/xml/vue/sql）一键生成下载 |
| 系统监控 | CPU / 内存 / 磁盘 / 堆栈监控，连接池分析 |
| 服务监控 | Spring Boot Admin 健康检查 |

---

## 🎯 技术亮点

1. **消息可靠性投递**：RabbitMQ Confirm 模式 + 消息回退 + 消费端去重，解决重复消费问题（订单 / 支付 / 库存场景）；
2. **分布式库存管理**：下单锁定库存 → 支付成功 MQ 通知扣减库存 → 取消订单解锁库存并还原销量，全程异步解耦；
3. **延迟消息取消订单**：RabbitMQ 延迟插件实现订单超时自动取消（默认 30 分钟）；
4. **双端认证体系**：后台管理端 + H5 商城端两套登录链路，统一基于 Redis 的 Token 服务；
5. **商品缓存预热**：`CommandLineRunner` 在商品服务启动时将 SKU 加载至 Redis，加速详情读取；
6. **Feign 降级工厂**：所有远程调用均提供 `FallbackFactory`，服务不可用时优雅降级；
7. **网关统一防护**：认证过滤器、验证码、XSS 过滤、黑名单 URL、Sentinel 流控一体化；
8. **多源文件存储**：MinIO / FastDFS / 本地三种存储策略可切换。

---

## 🚀 快速开始

### 环境要求

| 依赖 | 版本 |
| --- | --- |
| JDK | 17 |
| Maven | 3.6+ |
| Node.js | 16+ |
| Nacos | 2.x（standalone） |
| MySQL | 5.7 / 8.0 |
| Redis | 5+ |
| RabbitMQ | 3.x（含延迟消息插件） |

### 第一步：启动基础中间件（Docker）

```bash
cd docker
# 首次构建并后台启动 nacos / mysql / redis / nginx / 各业务服务
docker-compose up -d
```

> 或在本地手动启动 Nacos（默认 `192.168.6.100:8848`，可在各服务 `bootstrap.yml` 中修改地址）、MySQL（初始化 `ry-cloud` 数据库及脚本）、Redis、RabbitMQ。

### 第二步：启动后端服务

按依赖顺序依次启动（IDEA 中运行 `main` 方法）：

```text
1. spzx-gateway     →  Nacos  → 网关 [8080]
2. spzx-auth        →  认证中心 [9200]
3. spzx-system      →  系统管理 [9201]
4. spzx-product     →  商品管理 [9205]
5. spzx-user        →  会员中心 [9206]
6. spzx-order       →  订单 [9207]
7. spzx-channel     →  商城渠道 [9208]
8. spzx-cart        →  购物车 [9209]
9. spzx-payment     →  支付 [9210]
10. spzx-file       →  文件服务 [9300]
11. spzx-job        →  定时任务（可选）[9203]
12. spzx-gen        →  代码生成（可选）[9202]
```

Windows 下也可使用 `bin/` 目录脚本：

```bat
bin\package.bat          :: 一键打包全部微服务
bin\run-gateway.bat      :: 启动网关
bin\run-modules-system.bat
bin\run-auth.bat
```

### 第三步：启动前端

```bash
cd spzx-ui
npm install          # 或 yarn
npm run dev          # 默认端口 80，/dev-api 代理至 http://localhost:8080
```

浏览器访问：http://localhost （默认账号请联系管理员 / 见演示环境）

### 第四步：生成代码（可选）

访问后台「系统工具 → 代码生成」，选择数据库表后一键生成前后端代码并下载。

---

## 🐳 Docker 部署

`docker/docker-compose.yml` 中已编排全部组件：

| 服务 | 端口 | 说明 |
| --- | --- | --- |
| spzx-nacos | 8848 / 9848 / 9849 | 注册配置中心（standalone） |
| spzx-mysql | 3306 | MySQL 5.7（utf8mb4，库 `ry-cloud`） |
| spzx-redis | 6379 | Redis |
| spzx-gateway | 8080 | 网关 |
| spzx-auth | 9200 | 认证中心 |
| spzx-modules-system | 9201 | 系统服务 |
| spzx-modules-gen | 9202 | 代码生成 |
| spzx-modules-job | 9203 | 定时任务 |
| spzx-modules-file | 9300 | 文件服务 |
| spzx-visual-monitor | 9100 | 监控中心 |
| spzx-nginx | 80 | 前端静态资源 + 反向代理 |

```bash
cd docker
./deploy.sh          # 打包 jar + 构建镜像 + 启动
```

---

## 📁 目录结构

```
spzx/
├── spzx-ui/                 # 前端（Vue3 + Vite）
├── spzx-gateway/            # 网关
├── spzx-auth/               # 认证中心
├── spzx-api/                # Feign 接口模块
├── spzx-common/             # 通用模块
├── spzx-modules/            # 业务模块
├── spzx-visual/             # 监控
├── docker/                  # 容器化部署（compose/nginx/nacos/mysql/redis）
├── bin/                     # Windows 一键启动脚本
├── pom.xml                  # 父工程（Spring Cloud Alibaba 版本管理）
└── LICENSE                  # MIT License
```

---

## ❓ 常见问题

**Q1：启动时无法连接 Nacos？**
A：Nacos 地址在各服务 `src/main/resources/bootstrap.yml` 的 `spring.cloud.nacos.server-addr` 配置（默认 `192.168.6.100:8848`），修改为本机地址即可。

**Q2：RabbitMQ 延迟消息不可用？**
A：确认已安装 `rabbitmq_delayed_message_exchange` 插件，并重启 RabbitMQ。

**Q3：前端代理 404？**
A：确认后端网关已启动且 `spzx-ui/vite.config.js` 中 `/dev-api` 代理目标为网关地址 `http://localhost:8080`。

---

## 📄 License

本项目基于 [MIT License](./LICENSE) 开源，可免费用于个人学习与商业项目。

> 二次开发基于 [RuoYi-Cloud](https://gitee.com/y_project/RuoYi-Cloud)（MIT License），感谢若依开源框架。