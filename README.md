# AI Interview Coach

面向技术求职者的 AI 面试训练平台。项目将简历诊断、实时模拟面试、能力评估、题库练习和成长数据整合为完整训练闭环。

## 核心能力

- PDF 简历解析与 AI 多维诊断，支持历史报告管理和 Markdown 导出。
- 基于 SSE 的流式模拟面试，支持上下文追问、停止生成、失败重试和会话恢复。
- 面试结束后生成技术基础、问题分析、项目实践、表达沟通、岗位匹配五维评估。
- 公共题库与用户私有题库并存，支持 AI 出题、来源筛选、分页和 Markdown 答案。
- 训练驾驶舱展示 7 天趋势、完成率、岗位分布、简历均分和动态训练建议。
- 桌面侧边栏与移动端底部导航，完整适配手机访问和深色模式。

## 工程亮点

- 使用 Spring AI 对接 DeepSeek OpenAI 兼容接口，并通过统一 `AiGateway` 实现空响应检测、失败重试、场景标识和调用耗时日志。
- 使用 Spring WebFlux `Flux` 与前端 Fetch `ReadableStream` 实现 SSE 增量输出和会话消息持久化。
- 使用 JWT 完成身份认证，所有简历、面试、题目和统计查询均按 `userId` 隔离。
- 使用 Flyway 管理数据库版本，新环境自动建表，已有环境通过 baseline 平滑接管。
- 集成 Spring Boot Actuator 与 Micrometer，提供标准健康探针并支持按需开放 Prometheus 指标。
- Redis 实现 AI 接口限流；Redis 不可用时自动降级为本地内存限流，避免核心功能完全失效。
- 密码使用 BCrypt 存储，并支持历史 MD5 密码在登录成功后无感升级。
- 头像资源通过鉴权接口访问，限制文件类型、大小和资源归属，并自动清理旧头像。
- 统一异常响应、CORS 白名单、分页参数校验和接口输入长度限制。

## 技术栈

- Backend: Java 17, Spring Boot 3.3, Spring AI, MyBatis-Plus, MySQL, Redis
- Frontend: Vue 3, Vite, Pinia, Element Plus, Axios, marked
- AI: DeepSeek Chat API
- Tooling: PDFBox, JWT, Knife4j, JUnit 5, Mockito

## 项目结构

```text
interview-coach/
|-- backend/                 Spring Boot API
|   |-- src/main/java/com/interviewcoach/
|   |   |-- config/         AI、CORS、MVC、MyBatis 配置
|   |   |-- controller/     REST 与 SSE 接口
|   |   |-- service/        业务、数据隔离、AI 网关
|   |   |-- interceptor/    JWT 登录拦截
|   |   `-- exception/      全局异常处理
|   `-- src/test/           服务与限流测试
|-- frontend/               Vue 3 单页应用
|   `-- src/
|       |-- views/          工作台与业务页面
|       |-- stores/         用户和主题状态
|       |-- api/            Axios 与 SSE 请求
|       `-- utils/          Markdown 渲染
`-- docs/                   初始化与迁移脚本
```

## 本地运行

环境要求：JDK 17+、Maven 3.9+、Node.js 18+、MySQL 8、Redis 6+。

配置环境变量：

```powershell
$env:DEEPSEEK_API_KEY="your-api-key"
$env:INTERVIEW_DB_PASSWORD="your-mysql-password"
$env:INTERVIEW_JWT_SECRET="a-long-random-secret"
```

数据库由 Flyway 在后端启动时自动初始化和迁移。已有旧版本数据库首次接入时会创建 baseline，不会重复执行初始建表脚本。

需要手动导入演示数据时仍可使用：

```powershell
mysql -u root -p < docs/init.sql
```

启动后端：

```powershell
cd backend
mvn spring-boot:run
```

启动前端：

```powershell
cd frontend
npm install
npm run dev -- --host 0.0.0.0
```

- Web: `http://localhost:5173`
- API: `http://localhost:8080`
- API Docs: `http://localhost:8080/doc.html`

## Docker 一键启动

复制环境变量模板并填写真实配置：

```powershell
Copy-Item .env.example .env
docker compose up --build -d
```

启动后访问 `http://localhost:5173`。Compose 会创建 MySQL、Redis、后端和 Nginx 前端四个服务，并使用命名卷保存数据库、头像和 Redis 数据。

查看状态与日志：

```powershell
docker compose ps
docker compose logs -f backend
```

停止服务：

```powershell
docker compose down
```

`docs/init.sql` 仅用于全新数据库初始化，其中包含建表重置语句。已有数据的环境请执行 `docs/20260914_user_data_isolation.sql` 等迁移脚本，不要重复执行初始化脚本。

标准运维端点：

- `GET /actuator/health`：应用健康与容器探针
- `GET /actuator/info`：应用版本信息

默认不公开指标详情。需要接入 Prometheus 时，将 `INTERVIEW_ACTUATOR_ENDPOINTS` 设置为 `health,info,prometheus,metrics`。

## 测试与构建

```powershell
cd backend
mvn clean test

cd ../frontend
npm run build
```

## 简历项目描述参考

AI Interview Coach 是一个基于 Spring Boot 3 与 Vue 3 的智能面试训练平台。项目通过 Spring AI 接入 DeepSeek，使用 SSE 实现上下文感知的流式模拟面试，并将会话持久化后生成五维能力评估；设计统一 AI 网关处理重试、空响应和调用观测，使用 Redis 限流及本地降级保障可用性；通过 JWT 与查询级用户条件实现简历、会话、题库等数据的多用户隔离，并完成响应式训练驾驶舱和移动端适配。

## 开源协作

- 提交代码前请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)。
- 安全问题请遵循 [SECURITY.md](SECURITY.md)，不要在公开 Issue 中披露漏洞或用户隐私。
- 项目使用 [MIT License](LICENSE)。
