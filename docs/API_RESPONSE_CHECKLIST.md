# Java 接口响应格式检查清单（与 Go 对齐）

对照仓库内 Go 实现，路径均以**仓库根目录** `dujiao-next/` 为基准。

**自动打勾**：在 `dujiao-next-java` 目录执行：

```bash
python3 scripts/check_api_response_format.py --update-checklist
```

脚本仅会勾选带 `<!--check:...-->` 标记且**当前检查通过**的条目；手工核对项需自行将 `- [ ]` 改为 `- [x]`。  
已勾选项在后续失败时**不会自动取消**，需要先把对应行改回 `- [ ]` 再跑脚本以刷新勾选状态。

---

## 一、通用 JSON API（非渠道）

Go 参考：`internal/http/response/response.go` 的 `Response`、`PageResponse`、`Pagination`；状态码：`internal/http/response/codes.go`。

- [ ] <!--check:codes-core--> **业务状态码集合**：Java `ResponseCodes` 与 Go `codes.go` 中 **0 / 400 / 401 / 403 / 404 / 429 / 500** 数值一致（Java 额外保留 `501` 等时需在对比用例中说明用途）。
- [ ] <!--check:envelope-fields--> **成功/失败信封字段名**：`status_code`（snake_case）、`msg`、`data`；类型与 Go `Response` 一致。
- [ ] <!--check:success-values--> **成功响应**：`status_code == 0`，`msg == "success"`；无业务体时用 `data: null`（与 Go `Success` 一致；注意 Java `ApiResponse` 使用 `NON_NULL` 时可能**省略** `data` 键，若客户端严格依赖 `"data": null` 需单独评估）。
- [ ] <!--check:http-200-errors--> **HTTP 状态码**：业务错误仍返回 **HTTP 200**，错误信息放在 JSON 的 `status_code` + `msg`（与 Go `response.Error` 一致）。例外：过滤器/渠道鉴权等见下文。
- [ ] <!--check:global-exception--> **`GlobalExceptionHandler`**：`BusinessException`、校验失败等返回 `ResponseEntity.ok(ApiResponse.error(...))`，勿用 `4xx/5xx` HTTP 包装通用业务错误（与 Go 主站一致）。
- [ ] <!--check:no-responseentity-4xx--> **Controller 层**：无 `ResponseEntity.status(HttpStatus.XXX)`、`badRequest`、`notFound` 等用于普通业务错误（脚本会扫常见反模式）。
- [ ] <!--check:page-envelope--> **分页**：顶层为 `status_code`、`msg`、`data`、`pagination`；分页内字段为 `page`、`page_size`、`total`、`total_page`（与 Go `PageResponse` / `Pagination` 一致）。
- [ ] <!--check:pagination-math--> **total_page 计算**：与 Go `BuildPagination` 一致：`(total + pageSize - 1) / pageSize`，非法 `page`/`page_size` 归一化逻辑与 `NormalizePage` 对齐。

### 错误体中的 `request_id`（通用 API）

Go：`response.Error` 通过 `attachRequestID` 将 `request_id` 写入 `data`。  
Java：若已实现请求 ID 中间件，需核对**嵌套位置与键名**是否与 Go 一致（`data` 为 map 时合并键，否则结构可能不同）。

- [ ] **request_id 策略**：与 Go `attachRequestID` 行为一致（含 `data == null` 时为 `{"request_id":"..."}` 等分支）。

---

## 二、渠道 API（`/api/v1/channel/...`）

Go 参考：`internal/http/response/response.go` 的 `ChannelResponse`；错误映射：`internal/http/handlers/channel/response.go`。

- [ ] **成功体**：`status_code`、`msg`、`data`，以及 **`request_id`**（成功时 Go 带 `request_id`）。
- [ ] **错误体**：HTTP 状态码与 Go `ChannelError` 一致（可为 **400 / 401 / 403 / 404 / 429 / 500** 等），JSON 含 **`error_code`**，且通常带 **`request_id`**。
- [ ] **业务错误码与文案**：与 Go `respondChannelMappedError` / `respondChannelError` 的 **http 状态 + status_code + error_code + 消息** 对齐（含 i18n 键解析后的文案若需一致）。
- [ ] **限流**：若 Go 对某类错误设置了 `Retry-After`，Java 侧应对齐。

当前 Java 若仍用 `ApiResponse` + 全程 HTTP 200，在本节逐项标为未对齐，直到改为 `ChannelResponse` 等价结构。

---

## 三、明确例外（不要求与通用信封一致）

与 Go 保持一致即可，不要求改成 `ApiResponse`：

- [ ] **`GET /health`**：简单 JSON（Go/Java 各自实现一致即可）。参考 Java `HealthController`。
- [ ] **文件流 / 导出**：`ResponseEntity<byte[]>` 或 `Resource`（如订单履约下载、礼品卡导出）。
- [ ] **支付 Webhook / 上游回调**：可能为**渠道方要求的固定格式**，按支付文档与 Go 同路径对照，而非 `ApiResponse`。

---

## 四、按模块手工冒烟（路径与 Go 路由对齐后勾选）

每完成一组，用同一请求对比 Go/Java 的 JSON **键名、类型、嵌套**（可用 diff 或契约测试）。

- [ ] 认证与用户：`AuthApiController`、`UserApiController`
- [ ] 游客：`GuestApiController`
- [ ] 公开：`PublicApiController`
- [ ] 管理后台：各 `Admin*Controller`
- [ ] 渠道：`ChannelApiController`（务必按第二节标准）
- [ ] 上游：`UpstreamApiController`、`UpstreamCallbackController`
- [ ] 支付：`PaymentWebhookController`、`PaymentSimulateController`

---

## 五、回归建议

- [ ] 对关键路径增加 **契约测试**（JSON Schema 或 snapshot），或 CI 中对比 Golden JSON。
- [ ] 文档中记录 **Java 与 Go 已知差异**（如 `NON_NULL` 省略 `data`、渠道响应形态未对齐等），避免前端误判。
