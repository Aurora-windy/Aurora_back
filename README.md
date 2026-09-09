# AURORA Backend

AURORA 综合一体化后台平台的**后端仓库**。Spring Boot 3 + MyBatis-Plus 多模块单体工程，一套工程里聚合了 RBAC 权限基座、人事、教务、在线判题、电商和 AI 智能体六大业务域。

对应前端仓库：[Aurora\_front](https://github.com/Aurora-windy/Aurora_front.git)

---

## 技术栈

| 分类      | 选型                                           | 版本             |
| ------- | -------------------------------------------- | -------------- |
| 语言 / 框架 | Java + Spring Boot                           | 17 / 3.3.6     |
| ORM     | MyBatis-Plus                                 | 3.5.10         |
| 数据源     | dynamic-datasource（MySQL 主 + PostgreSQL 从）   | 4.3.1          |
| 鉴权      | Sa-Token                                     | 1.39.0         |
| 数据库版本管理 | Liquibase                                    | Spring Boot 内置 |
| 主库      | MySQL                                        | 8.0            |
| 向量库     | PostgreSQL + PgVector                        | pg16           |
| 图数据库    | Neo4j                                        | 5.x（可选）        |
| 缓存      | Redis + Spring Data Redis                    | —              |
| 接口文档    | Knife4j（OpenAPI 3）                           | 4.5.0          |
| 工具      | Hutool、EasyExcel、Easy-Captcha、MapStruct、JJWT | —              |
| AI 协议   | MCP Java SDK                                 | 2.0.0          |
| 测试      | JUnit 5 + Testcontainers BOM                 | 1.20.4         |

> `spring-ai-bom` 已在 dependencyManagement 中锁定（1.0.9），但 AI 编排是**自研实现**（`AgentOrchestrator` + 自写 OpenAI 兼容客户端），未引入 Spring AI 编排框架；`mcp` 只作为协议库使用。

---

## 模块结构

| 模块              | 职责                                                                                  |
| --------------- | ----------------------------------------------------------------------------------- |
| `aurora-common` | 公共基座：统一响应 `Result`、全局异常、Sa-Token / MyBatis-Plus / Redis / Jackson 配置、逻辑删除与自动填充、权限常量 |
| `aurora-system` | RBAC 权限基座：认证登录（验证码 + JWT 风格 token）、用户、角色、菜单                                         |
| `aurora-hr`     | 人事管理：部门、岗位、员工、考勤（含定时任务自动结账）                                                         |
| `aurora-edu`    | 教务管理：学生、教师、课程、选课（含容量与选课时段控制）                                                        |
| `aurora-oj`     | 在线判题：题目、提交记录                                                                        |
| `aurora-mall`   | 电商：商品、购物车、订单、支付、秒杀、库存流水                                                             |
| `aurora-ai`     | AI 智能体：对话编排、模型 Provider、知识库 RAG、知识图谱、MCP 客户端/服务端、工具集市、调用审计、代码生成                     |
| `aurora-server` | **聚合启动模块**：`AuroraBackApplication`、`application.yml`、Liquibase changelog、打包产物       |

只有 `aurora-server` 是可执行模块，其余模块以依赖形式被它聚合。

---

## 快速开始

### 1. 环境准备

- **JDK 17 及以上**（开发机实测 JDK 21 可正常编译运行）
- **Maven 3.9+**（推荐直接用仓库自带的 `mvnw`）
- **MySQL 8.0**：创建一个空库，例如 `aurora`
- **Redis**：本地默认 `localhost:6379`，库 6
- **Docker（可选）**：知识库向量检索和图数据库需要，用仓库根目录的 `start-infra.ps1` 一键起容器

```powershell
# 在仓库外层目录（Aurora/）执行，起 Neo4j + PgVector 容器
.\start-infra.ps1
```

### 2. 配置数据库连接

`application.yml` 里所有连接信息都是 `${ENV:}` 占位，**不会硬编码任何密码**。两种方式任选其一：

**方式 A（推荐，本机开发）**：新建 `aurora-server/src/main/resources/application-dev.yml`（已被 `.gitignore` 忽略，不会进 Git）：

```yaml
spring:
  datasource:
    dynamic:
      datasource:
        master:
          url: jdbc:mysql://127.0.0.1:3306/aurora?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
          username: root
          password: 你的密码
        pgvector:
          url: jdbc:postgresql://127.0.0.1:5432/aurora_vector
          username: postgres
          password: postgres
```

`application.yml` 中 `spring.profiles.active` 默认是 `${SPRING_PROFILES_ACTIVE:dev}`，所以这个文件放好就自动生效。

**方式 B**：直接设置环境变量（见下面的配置表）。

### 3. 编译运行

```bash
# Windows / Git Bash
export JAVA_HOME="/c/Users/你的用户名/.jdks/ms-21.0.11"
./mvnw -q -pl aurora-server -am package -DskipTests
java -jar aurora-server/target/aurora-server.jar
```

或开发态热启动：

```bash
./mvnw -pl aurora-server spring-boot:run -Dspring-boot.run.profiles=dev
```

### 4. 验证

- 接口根地址：`http://localhost:1207/api`（`server.port=1207`，`context-path=/api`）
- Knife4j 文档：`http://localhost:1207/api/doc.html`
- 默认管理员账号：`admin` / `admin123`（由 Liquibase 初始化脚本写入）

**首次启动无需手工建表**：Liquibase 会按 `db/changelog/db.changelog-master.yaml` 的顺序自动执行全部建表与种子数据脚本。

---

## 环境变量

全部可省略，缺省值见 `application.yml`。

### 数据源与缓存

| 变量                                                            | 说明           | 默认                   |
| ------------------------------------------------------------- | ------------ | -------------------- |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | MySQL 主库     | 空（必填）                |
| `PG_HOST` / `PG_PORT` / `PG_DB` / `PG_USER` / `PG_PASSWORD`   | PgVector 向量库 | 空（懒加载，不配也能启动）        |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_DATABASE`                | Redis        | localhost / 6379 / 6 |

### AI 相关

| 变量                                                                                      | 说明                        | 默认                                        |
| --------------------------------------------------------------------------------------- | ------------------------- | ----------------------------------------- |
| `AURORA_AI_SECRET_KEY`                                                                  | Provider API Key 加密主密钥    | 代码内置 dev 兜底；**生产必须设置且保持稳定**，变更会导致已有密文无法解密 |
| `AURORA_UPLOAD_PATH`                                                                    | 本地上传根目录                   | `./uploads`                               |
| `AURORA_EMBEDDING_ENABLED`                                                              | 是否启用系统级 Embedding         | `false`                                   |
| `AURORA_EMBEDDING_PROVIDER_MODE`                                                        | `local` / `cloud`         | `local`                                   |
| `AURORA_EMBEDDING_CLOUD_CONSENT`                                                        | 是否同意把文本发到云端向量服务           | `false`                                   |
| `AURORA_EMBEDDING_BASE_URL` / `_API_KEY` / `_MODEL` / `_DIMENSION` / `_TIMEOUT_SECONDS` | 系统级 Embedding 参数          | 空 / 空 / 空 / 0 / 60                        |
| `AURORA_NEO4J_ENABLED` / `_URI` / `_USERNAME` / `_PASSWORD`                             | 知识图谱 Neo4j                | false / localhost:7474 / neo4j / neo4j    |
| `AURORA_MCP_SERVER_ENABLED`                                                             | 是否暴露内置 MCP Server         | `false`                                   |
| `AURORA_MCP_BEARER_TOKEN`                                                               | MCP Server 的 Bearer Token | 空                                         |
| `AURORA_MCP_STARTUP_SYNC_ENABLED`                                                       | 启动时同步 MCP Server 配置       | `false`                                   |
| `AURORA_GITHUB_TOKEN`                                                                   | GitHub 工具所需 token         | 空                                         |
| `AURORA_WORKSPACE_ROOT` / `_MAX_READ_CHARS` / `_MAX_ENTRIES` / `_MAX_SEARCH_RESULTS`    | 本地工作区 Agent 的沙箱根目录与限额     | 空 / 20000 / 200 / 100                     |

---

## 目录结构

```
Aurora_back/
├── aurora-common/      公共基座（响应体、异常、配置、工具）
├── aurora-system/      RBAC：auth / user / role / menu
├── aurora-hr/          人事：dept / position / employee / attendance
├── aurora-edu/         教务：student / teacher / course / selection
├── aurora-oj/          判题：problem / submission
├── aurora-mall/        电商：product / cart / order / payment / seckill
├── aurora-ai/          AI：chat / provider / knowledge / mcp / agent / tool / builder / audit / runtime / workspace
├── aurora-server/      启动模块
│   └── src/main/resources/
│       ├── application.yml
│       └── db/changelog/           Liquibase 变更集（按日期递进）
├── start-mcp.ps1          本地 MCP 演示启动脚本
└── start-mcp-peer.ps1     本地 MCP 对端演示启动脚本
```

每个业务模块内部统一按 `controller` / `service(+impl)` / `mapper` / `entity` / `model/req` / `model/resp` 分层。

---

## 接口一览

全部接口都在 `http://localhost:1207/api` 之下：

| 前缀                                                                  | 控制器              | 说明                     |
| ------------------------------------------------------------------- | ---------------- | ---------------------- |
| `/auth`                                                             | `AuthController` | 登录、验证码、注销、当前用户信息       |
| `/system/users` `/system/roles` `/system/menus` `/system/file`      | System           | RBAC 与文件管理             |
| `/hr/depts` `/hr/positions` `/hr/employees` `/hr/attendance`        | HR               | 人事域                    |
| `/edu/students` `/edu/teachers` `/edu/courses` `/edu/selections`    | Edu              | 教务域                    |
| `/oj/problems` `/oj/submissions`                                    | OJ               | 判题域                    |
| `/mall/products` `/mall/cart` `/mall/orders` `/mall/seckill`        | Mall             | 电商域                    |
| `/ai/sessions` `/ai/actions`                                        | AI               | 对话会话、Agent 动作          |
| `/ai`（`AiProviderController`）                                       | AI               | 模型 Provider 管理         |
| `/ai/admin/knowledge-docs` `/ai/graph`                              | AI               | 知识库文档、知识图谱             |
| `/ai/admin/mcp-demo` `/mcp`                                         | AI               | MCP 演示端点、MCP Server 端点 |
| `/ai/admin/audit` `/ai/admin/sessions` `/ai/admin/embedding-config` | AI               | 工具调用审计、会话管理、向量配置       |
| `/builder`                                                          | AI               | 需求解析 → 生成预览的代码生成能力     |

---

## AI 子系统

`aurora-ai` 是独立于业务模块的 AI 能力层，核心链路：

1. **Provider 管理**（`provider`）：管理员在后台配置模型服务，按用途分为 `CHAT` / `EMBEDDING` / `BOTH` / `GRAPH`。`code` 由后端自动生成，API Key 以 `enc:v1` 密文存储。
2. **对话编排**（`chat`）：`AgentOrchestrator` 负责「意图识别 → 工具调用 → 结果回填 → 生成回答」，支持 Function Calling 和本地工作区操作。
3. **工具集市**（`tool`）：`AiToolRegistry` 统一注册，已内置教务工具、GitHub 工具、天气工具、本地工作区工具；工具 schema 自动生成并下发给模型。
4. **知识库 RAG**（`knowledge`）：文档上传 → 文本抽取 → 分块 → 向量化（写 PgVector）→ 余弦相似度检索 → 带引用回答。
5. **知识图谱**（`knowledge/graph`）：对接 Neo4j，从文本抽取实体关系。
6. **MCP**（`mcp`）：同时具备 MCP 客户端（`McpClientManager`）和内置 MCP Server（SSE + Bearer Token）。
7. **审计与运行时**（`audit` / `runtime`）：工具调用日志、Agent 任务、Trace、事件流落库。

### 两个已知的选型坑

- **DeepSeek 官方没有 Embedding 接口**。只配 DeepSeek 会导致知识库发布失败，向量化必须另配一家（OpenAI / 智谱 / 通义 / 本地 Ollama）。
- **PgVector 向量列维度写死 1536**（`db/changelog/pgvector/init.sql`）。换用非 1536 维模型（bge-m3 是 1024、nomic-embed-text 是 768）必须改维度并重建表，否则报 `vector dimension mismatch`。优先选 1536 维模型：`text-embedding-3-small` / `text-embedding-ada-002` / 通义 `text-embedding-v2`。
- **图谱抽取的 Provider 必须包含 CHAT 能力**：`Neo4jGraphService` 只挑用途为 `GRAPH` / `CHAT` / `BOTH` 的 Provider，只配 `EMBEDDING` 会抽不到任何内容。

---

## 数据库变更

使用 Liquibase 管理，新增变更请**新建带日期前缀的 SQL 文件**并在 `db.changelog-master.yaml` 末尾追加 include，**不要修改已执行过的 changeset**。

```yaml
- include: { file: db/changelog/mysql/2026-09-09-your-change.sql }
```

每个 changeset 需要带 `-- changeset author:id dbms:mysql` 与 `-- rollback ...` 注释。

---

## 测试

```bash
./mvnw test
```

现有单测集中在 `aurora-ai`（编排器、工具执行、Provider 服务、密钥加解密、工作区服务、代码生成解析、天气/GitHub 服务）与 `aurora-common`（统一响应）。

---

## 常见问题

**Q：Git Bash 里 `./mvnw.cmd` 报 `JAVA_HOME environment variable is not defined correctly`，且不报错直接退出？**

用 Unix 版 `./mvnw`（不带 `.cmd`）。在 Git Bash 中 `.cmd` 会经 `cmd.exe` 执行，无法识别 `/c/Users/...` 这种 Unix 风格路径，导致静默失败——表现得很像「改了代码但不生效」。

**Q：启动报 `Access denied for user ''@'localhost'`？**

`application.yml` 中 `${DB_USER:}` 等占位没有值。检查 `application-dev.yml` 是否存在、dev profile 是否激活，或环境变量是否设置。注意数据源配置在 `spring.datasource.dynamic.datasource.master` 下，**不是**旧的扁平 `spring.datasource.username`。

**Q：没起 PgVector 容器能启动吗？**

能。`pgvector` 数据源配了 `lazy: true`，未就绪不阻塞启动，只有知识库检索/发布时才会真正连接。

**Q：改了 Java 代码不生效？**

后端没有热部署，必须重新 `package` 并重启。前端才是热更新。

**Q：为什么 Redisson 自动配置被排除了？**

`aurora-mall` 引入了 `redisson-spring-boot-starter`，但当前没有代码使用 Redisson，而它的自动配置会强制连 Redis 并发送 AUTH，与无密码 Redis 冲突。真正需要分布式锁时移除 `application.yml` 里的排除项即可。

---

## 当前限制

- **OJ 判题是简化实现**：按预期输出做比对，没有沙箱执行，不编译运行用户代码。
- **电商支付为模拟流程**，未对接真实支付渠道。
- **本地工作区 Agent**（`AURORA_WORKSPACE_ROOT`）默认关闭，需要在后台显式开启并配置根目录
