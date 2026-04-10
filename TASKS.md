# dujiao-next-java 未完成任务清单

> **如何打勾**：完成某项后，把该行 `- [ ]` 改成 `- [x]`。需要我（Cursor 助手）在某次提交里代改此文件时，直接在对话里说明即可。

## 支付与订单

- [x] 在线支付网关：`UserPaymentService` / `GuestPaymentService` 中 `payment_not_implemented`
- [x] 创建订单并走在线渠道：`UserOrderService` / `GuestOrderService` 返回 `payment_gateway_not_implemented` 的路径

## 渠道 API（`ChannelApiController`，501）

### Telegram / 身份

- [x] `GET /channel/telegram/config`
- [x] `POST /channel/telegram/heartbeat`
- [x] `POST /channel/identities/telegram/resolve`
- [x] `POST /channel/identities/telegram/provision`
- [x] `POST /channel/identities/telegram/bind`

### 渠道推广（点击已接入，下列仍为 501）

- [x] `POST /channel/affiliate/open`
- [x] `GET /channel/affiliate/dashboard`
- [x] `GET /channel/affiliate/commissions`
- [x] `GET /channel/affiliate/withdraws`
- [x] `POST /channel/affiliate/withdraws`

### 渠道订单 / 支付 / 钱包

- [x] 渠道订单：preview / create / list / detail / cancel
- [x] 渠道支付：latest / detail / create
- [x] 渠道钱包与流水、充值：`channel_wallet_not_implemented` 等
- [x] 渠道礼品卡兑换：`channel_gift_card_redeem_not_implemented`

## 管理后台

- [x] SMTP 发信测试（`smtp_test_not_implemented`）
- [x] 通知发送测试（`notification_test_not_implemented`）
- [x] 推广管理接口实装：`AdminAffiliateMgmtController` 当前多为空响应（用户列表、佣金、提现审核等）

## 公开接口与其它

- [x] 图形验证码：`/public/captcha/image` 返回真实图片（当前占位）
- [x] 礼品卡兑换：如需与 Go 一致，接入 `captcha` 校验（请求体已预留字段）

## 业务对齐（可选 / 增强）

- [x] 推广提现：与 Go 一致的「任意金额 + 拆佣金行」算法（当前用户侧为「全额一次性提现」简化版）
- [x] 子订单 / 合并履约等 Go 有而 Java 未对齐的能力（若产品需要）
