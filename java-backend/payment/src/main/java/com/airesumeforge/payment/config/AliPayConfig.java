package com.airesumeforge.payment.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class AliPayConfig {

    private final AliPayProperties aliPayProperties;

    @Value("${alipay.private-key:}")
    private String privateKeyPath;

    @Value("${alipay.alipay-public-key:}")
    private String alipayPublicKeyPath;

    @Bean
    public AlipayClient alipayClient() throws Exception {
        String privateKeyContent = aliPayProperties.getPrivateKey().trim();
        String alipayPublicKeyContent = aliPayProperties.getAlipayPublicKey().trim();

        // 如果是 file: 路径，读取文件内容
        if (privateKeyContent.startsWith("file:")) {
            String filePath = privateKeyContent.substring(5);
            privateKeyContent = new String(java.nio.file.Files.readAllBytes(
                    java.nio.file.Paths.get(filePath)
            )).trim();
        }
        if (alipayPublicKeyContent.startsWith("file:")) {
            String filePath = alipayPublicKeyContent.substring(5);
            alipayPublicKeyContent = new String(java.nio.file.Files.readAllBytes(
                    java.nio.file.Paths.get(filePath)
            )).trim();
        }

        return new DefaultAlipayClient(
                aliPayProperties.getGatewayUrl(),
                aliPayProperties.getAppId(),
                privateKeyContent,
                "json",
                "UTF-8",
                alipayPublicKeyContent,
                aliPayProperties.getSignType()
        );
    }
}