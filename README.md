# AI Resume Forge

围绕简历编辑、岗位要求分析和 AI 修改建议构建的全栈应用。用户维护简历内容，结合目标岗位生成修改方案，并在界面中审阅后应用。

## 仓库组成

| 目录 | 职责 |
| --- | --- |
| `desktop/` | Electron 桌面端：主进程 + `renderer/`（Vue 3 渲染层）。打包成 Windows 安装包 |
| `backend/java/` | Java 17 单体应用（Spring Boot），认证、简历、Agent、面试、通知等模块在同一个进程内 |
| `backend/python/` | FastAPI、LangGraph 工作流与模型调用 |
| `docs/` | 专题说明 |

**这是一个桌面端应用**，不是网页应用：

- 桌面端主进程负责拉起并守护 Java 后端和 Python 运行时，用户装完 exe 就能用，
  机器上不需要装 Java / Python，也不需要任何外部中间件；
- 后端运行时由 CI 打进安装包（Java 用 jpackage 自带 JRE，Python 用 PyInstaller 打成独立 exe）；
- 模型配置与后端地址在桌面端**设置界面**里改，保存到安装目录的 `config/application.yaml`。

Java 后端是单体应用：一次构建、一次启动、端口 8080，渲染层直接对接 `http://localhost:8080`。
Python 侧是独立的 AI 运行时，与 Java 之间通过内部 HTTP 接口和共享 SQLite 任务队列协作。

## 能力与调用关系

渲染层负责简历编辑和建议预览；Java 侧负责用户、简历和业务接口；Python 侧处理岗位分析、改写与审核等 AI 流程。Python 服务的具体入口、配置和运行方式见 [后端说明](backend/python/README.md)。

模块存在不代表所有外部服务已配置完成。运行相关流程还需要对应的模型服务（MiniMax API Key）；
数据与任务队列都落在本地 SQLite 文件上，**不需要再装 MySQL / Redis**。

## 本地开发

渲染层可独立启动，完整业务需要后端配合：

```sh
cd frontend
npm install
npm run dev
```

Java 单体应用在 `backend/java/` 中构建和启动：

```sh
cd backend/java
mvn package -DskipTests
java -jar target/ai-resume-forge-backend.jar
```

上述命令只打包，不执行测试。首次运行前需要导入建表脚本 `backend/java/db_schema/schema.sql`，
并按 `.env.example` 配置模型和邮件相关环境变量（数据与任务队列落在本地 SQLite，无需额外服务）。

Python 服务从 `backend/python/` 启动，先按其依赖文件安装环境：

```sh
uvicorn src.app.main:app --reload --port 8000
```

模型密钥和连接信息使用本地环境配置，不写入简历内容或前端代码。仓库中的 `.env.example` 可作为配置起点。

## 验证与进一步阅读

前端执行 `npm run build`。Java 和 Python 的测试分别在各自项目中运行；完整简历分析还需验证模型返回、建议预览和用户确认应用的整条流程。

- [架构说明](ARCHITECTURE.md)
- [界面设计](DESIGN.md)
- [Python 后端](backend/python/README.md)
