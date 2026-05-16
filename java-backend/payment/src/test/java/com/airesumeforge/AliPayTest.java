package com.airesumeforge;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AliPayTest {

    @Autowired
    private AlipayClient alipayClient;

    @Test
    void contextLoads() {
        System.out.println("AlipayClient 注入成功: " + (alipayClient != null));
    }

    @Test
    void testCreatePayUrl() throws AlipayApiException {
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl("http://localhost:8080/api/payment/callback/alipay");
        request.setReturnUrl("http://localhost:3000/order/result");

        request.setBizContent("{" +
                "\"out_trade_no\":\"TEST_ORDER_001\"," +
                "\"total_amount\":\"2.00\"," +
                "\"subject\":\"Start 套餐 - 月度订阅\"," +
                "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"" +
                "}");

        String form = alipayClient.pageExecute(request).getBody();
        System.out.println("支付宝支付页面表单:");
        System.out.println(form);
    }
}
