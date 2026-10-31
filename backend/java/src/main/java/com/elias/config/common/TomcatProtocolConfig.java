package com.elias.config.common;

import java.util.Locale;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

/** Windows 上使用 Tomcat NIO2，避免 NIO selector 初始化依赖本机回环通道。 */
@Configuration
public class TomcatProtocolConfig implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.contains("windows")) {
            factory.setProtocol("org.apache.coyote.http11.Http11Nio2Protocol");
        }
    }
}
