package com.airesumeforge.payment.dto.request;

import lombok.Data;

/**
 * 支付宝异步通知回调请求参数
 *
 * 重要说明：
 * 1. 安全永远是第一位：接收数据后，业务代码必须进行签名验证，防止伪造回调。
 * 2. 要能应对重复通知：支付宝可能因为网络问题重复发送通知，你的业务逻辑需要支持幂等性。
 * 3. 与业务逻辑分离：这个类只是数据的载体，不要在这里写业务逻辑。
 */
@Data
public class AlipayCallbackRequest {

    // --- 公共参数 (必传) ---
    /**
     * 通知时间 (格式：yyyy-MM-dd HH:mm:ss)
     */
    private String notifyTime;

    /**
     * 通知类型 (如 trade_status_sync)
     */
    private String notifyType;

    /**
     * 通知校验ID (用于防重)
     */
    private String notifyId;

    /**
     * 签名
     * 注意：验签时，这个字段不参与签名计算。
     */
    private String sign;

    /**
     * 签名算法类型 (如 RSA2)
     */
    private String signType;

    // --- 业务参数 (必传) ---
    /**
     * 商户订单号（对应你的 orderNo）
     * 注意：这是对接你系统订单的关键字段。
     */
    private String outTradeNo;

    /**
     * 支付宝交易号
     * 注意：这是支付宝的唯一流水号，常用于排查问题和幂等性校验。
     */
    private String tradeNo;

    /**
     * 交易状态
     * 如：TRADE_SUCCESS (交易成功), WAIT_BUYER_PAY (等待付款) 等。
     * 注意：对支付成功的关键判断应基于此字段。
     */
    private String tradeStatus;

    /**
     * 订单金额 (单位：元)
     */
    private String totalAmount;

    /**
     * 卖家支付宝用户号 (对应你的商户PID)
     */
    private String sellerId;

    // --- 业务参数 (非必传) ---
    /**
     * 买家支付宝用户号
     */
    private String buyerId;

    /**
     * 买家支付宝账号
     */
    private String buyerLogonId;

    // 可扩展字段，用于接收更多非必传参数，或方便处理 fundBillList 等复杂JSON
    // private String gmtCreate;       // 交易创建时间
    // private String gmtPayment;      // 交易付款时间
    // private String fundBillList;    // 支付金额信息 (JSON数组格式)
}