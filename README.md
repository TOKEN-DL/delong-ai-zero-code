# Delong AI Zero Code

AI 驱动的零代码应用生成平台 — 通过自然语言对话，快速生成可部署的前端应用代码。

## 功能特性

- **智能对话生成代码** — 与 AI 实时对话，自动生成 HTML / Vue 等前端代码，支持流式输出
- **多模型支持** — 集成 DeepSeek、OpenAI、阿里 DashScope 等多种大模型，按任务复杂度智能路由
- **LangGraph 工作流** — 基于 LangGraph4j 实现多步骤 AI 工作流编排，支持状态管理与并发处理
- **应用管理** — 创建、编辑、部署 AI 应用，每个应用拥有独立的部署标识
- **用户体系** — 注册登录、角色权限（user / admin）、会话管理
- **截图生成** — 集成 Selenium 自动化截图，快速预览生成效果
- **接口限流** — 基于 Redisson 的分布式限流，保护服务稳定性
- **监控告警** — Prometheus + Micrometer 指标采集，Spring Boot Actuator 健康检查

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 | JDK |
| Spring Boot | 3.5.11 | Web 框架 |
| MyBatis-Flex | 1.11.0 | ORM |
| LangChain4j | 1.1.0-beta7 | AI 编排 |
| LangGraph4j | - | AI 工作流 |
| Redisson | 3.50.0 | 分布式锁与限流 |
| MySQL | 8.x | 关系数据库 |
| Redis | - | 会话存储 / 缓存 |
| Knife4j | 4.4.0 | API 文档 |
| Hutool | 5.8.38 | 工具库 |
| Lombok | - | 代码简化 |

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.5 | 前端框架 |
| TypeScript | 5.9 | 类型安全 |
| Vite | 7.3 | 构建工具 |
| Ant Design Vue | 4.2 | UI 组件库 |
| Pinia | 3.0 | 状态管理 |
| Vue Router | 5.0 | 路由 |
| Axios | 1.13 | HTTP 客户端 |
| Marked | 17.0 | Markdown 渲染 |
| Highlight.js | 11.11 | 代码高亮 |

## 项目结构

```
delong-ai-zero-code/
├── sql/                            # 数据库建表脚本
├── src/main/java/.../
│   ├── ai/                         # AI 服务、护轨、工具
│   ├── langgraph4j/                # LangGraph 工作流编排
│   ├── controller/                 # REST API 控制器
│   ├── service/                    # 业务逻辑层
│   ├── model/                      # 实体、DTO、VO
│   ├── core/                       # 代码生成核心（构建/解析/保存）
│   ├── config/                     # 配置类
│   ├── aop/                        # 切面
│   ├── ratelimter/                 # 限流模块
│   ├── exception/                  # 全局异常处理
│   └── common/                     # 通用工具
├── src/main/resources/
│   ├── application.yml             # 主配置
│   └── application-local.yml       # 本地开发配置
├── delong-ai-zero-code-frontend/   # Vue 3 前端
│   └── src/
│       ├── pages/                  # 页面组件
│       │   ├── HomePage.vue        # 首页
│       │   ├── admin/              # 管理后台
│       │   ├── app/                # 应用（聊天/编辑）
│       │   └── user/               # 登录/注册
│       ├── api/                    # 接口调用
│       ├── store/                  # 状态管理
│       ├── router/                 # 路由配置
│       └── layouts/                # 布局组件
├── pom.xml                         # Maven 依赖
└── prometheus.yml                  # Prometheus 配置
```

## 数据库设计

项目使用 3 张核心表：

- **user** — 用户表（账号、密码、角色等）
- **app** — 应用表（名称、Prompt、代码生成类型、部署标识等）
- **chat_history** — 对话历史表（消息内容、类型、关联应用等）

建表脚本位于 [sql/create_table.sql](sql/create_table.sql)。

## 快速开始

### 环境要求

- JDK 21+
- Node.js 20.19+ 或 22.12+
- MySQL 8.x
- Redis

### 1. 初始化数据库

```sql
-- 创建数据库
CREATE DATABASE delong_ai_zero_code DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 执行建表脚本
SOURCE sql/create_table.sql;
```

### 2. 配置后端

复制 `application-local.yml` 并填入实际配置：

```yaml
# MySQL
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/delong_ai_zero_code
    username: root
    password: your_password

# Redis
spring:
  data:
    redis:
      host: localhost
      port: 6379

# AI 模型 Key
langchain4j:
  open-ai:
    api-key: your_openai_key
```

### 3. 启动后端

```bash
./mvnw spring-boot:run
```

后端启动在 `http://localhost:8124/api`，API 文档地址 `http://localhost:8124/api/doc.html`。

### 4. 启动前端

```bash
cd delong-ai-zero-code-frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`。

## API 概览

| 路径 | 控制器 | 说明 |
|------|--------|------|
| `/api/app/*` | AppController | 应用管理、代码生成 |
| `/api/user/*` | UserController | 用户注册登录 |
| `/api/chat-history/*` | ChatHistoryController | 对话历史 |
| `/api/health` | HealthController | 健康检查 |
| `/api/static-resource/*` | StaticResourceController | 静态资源 |

## 页面路由

| 路径 | 页面 | 说明 |
|------|------|------|
| `/` | HomePage | 首页，展示精选应用 |
| `/user/login` | UserLoginPage | 登录 |
| `/user/register` | UserRegisterPage | 注册 |
| `/app/chat/:appId` | AppChatPage | AI 对话 |
| `/app/edit/:appId` | AppEditPage | 应用编辑 |
| `/admin/userManage` | UserManagePage | 用户管理（管理员） |
| `/admin/appManage` | AppManagePage | 应用管理（管理员） |
| `/admin/chatHistoryManage` | ChatHistoryManagePage | 历史管理（管理员） |

## License

Private - All Rights Reserved
