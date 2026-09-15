# Contributing

感谢你愿意改进 AI Interview Coach。

## 开始之前

1. 先搜索现有 Issue，确认问题尚未被讨论或处理。
2. 功能改动请先创建 Issue，说明使用场景、预期行为和大致方案。
3. 不要在 Issue、日志、截图或提交中包含 API Key、Token、简历原文等敏感数据。

## 本地开发

复制 `.env.example` 为 `.env` 并填写本地配置。也可以按 README 分别启动 MySQL、Redis、后端和前端。

提交前运行：

```powershell
cd backend
mvn clean test

cd ../frontend
npm ci
npm run build
```

## Pull Request

- 一个 PR 聚焦一个问题，避免夹带无关重构。
- 对新增接口补充输入校验、用户权限边界和异常行为。
- 对高风险服务逻辑补充单元测试。
- UI 改动需要验证桌面端、手机端和深色模式。
- PR 描述中写清动机、改动内容、验证方式和兼容性影响。

## Commit 建议

推荐使用 Conventional Commits：

- `feat:` 新功能
- `fix:` 缺陷修复
- `refactor:` 行为不变的重构
- `test:` 测试变更
- `docs:` 文档变更
- `chore:` 工程配置变更
