# AI 零代码应用生成平台

基于 Spring Boot 3 + LangChain4j + Vue 3 实现的 **企业级 AI 代码生成平台**，支持通过自然语言描述生成前端应用、可视化编辑与一键部署，涵盖 AI 智能体、工作流、多级缓存与监控等后端能力。

---

## 项目简介

用户输入需求描述后，系统由 AI 分析并选择生成策略，通过工具调用生成代码并流式输出；生成的应用可实时预览、可视化编辑（选中元素与 AI 对话修改），并支持一键部署到云端、自动截图与源码下载。后台提供用户与应用管理、系统与业务监控、精选应用配置等能力。

---

## 核心功能

| 模块 | 说明 |
|------|------|
| **智能代码生成** | 根据需求描述由 AI 选择策略，通过工具调用生成代码文件，流式输出展示执行过程 |
| **可视化编辑** | 生成应用实时预览，支持编辑模式下选中页面元素，与 AI 对话完成修改 |
| **一键部署分享** | 应用一键部署至云端、自动截取封面，生成可分享链接，支持完整源码下载 |
| **后台管理** | 用户管理、应用管理、系统监控（Prometheus）、业务指标与 AI 调用监控，精选应用配置 |

---

## 技术栈

| 层级 | 技术 |
|------|------|
| **后端** | Spring Boot 3、Java 21、LangChain4j、LangGraph4j、Spring Session + Redis、Redisson、MyBatis-Flex、MySQL、Knife4j |
| **前端** | Vue 3、TypeScript、Vite、Ant Design Vue、Pinia、Vue Router |
| **AI / 工具** | LangChain4j（OpenAI/阿里云 DashScope）、LangGraph4j 工作流、Selenium 截图 |
| **存储与缓存** | MySQL、Redis、Caffeine 本地缓存、腾讯云 COS |
| **运维与监控** | Spring Boot Actuator、Prometheus |

---

## 技术要点与实现亮点

- **AI 应用层**：基于 LangChain4j 接入大模型，LangGraph4j 编排工作流与工具调用，实现“需求 → 策略选择 → 代码生成”的智能体流程
- **流式输出与并发**：AI 结果流式返回，结合响应式编程提升并发与用户体验
- **对话与多租户**：基于 Session + Redis 的对话记忆与多用户隔离
- **性能与成本**：Redis + Caffeine 多级缓存、请求与结果缓存，降低重复调用与延迟
- **可观测性**：Actuator + Prometheus 指标暴露，便于监控与告警
- **工程化**：前后端分离、OpenAPI 规范、统一异常与参数校验，便于扩展与维护

---

## 项目结构

```
ai-web-generator/
├── src/                    # 后端 Spring Boot 源码
│   └── main/
│       ├── java/           # 业务、AI 编排、工具、缓存、监控等
│       └── resources/      # 配置、Prompt 模板等
├── frontend/               # Vue 3 前端工程
└── pom.xml
```

---

## 快速开始

### 环境要求

- JDK 21、Maven、Node.js 18+
- MySQL、Redis
- 大模型 API（如 OpenAI 或阿里云 DashScope），需在配置中填写密钥

### 后端

1. 在 `src/main/resources/application.yml` 中配置数据库、Redis、大模型 API 等。
2. 执行：`mvn spring-boot:run`。

### 前端

1. 进入 `frontend` 目录：`cd frontend`
2. 安装依赖：`npm install`
3. 启动开发服务：`npm run dev`

按实际配置访问前端页面与后端接口即可使用。

---

## 许可证

本项目仅供学习与个人作品展示使用。
