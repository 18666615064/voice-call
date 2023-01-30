package com.voicecall.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * @Autor: cxj
 * @Date: 2023-01-30
 * @Description:
 * @Version: V1.0
 */
@Configuration
public class WebSocketConfig extends WebMvcConfigurationSupport {
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        super.addResourceHandlers(registry);
        // 相对路径
        registry.addResourceHandler("image/**").addResourceLocations("classpath:/static/image/");
        // 绝对路径
        // registry.addResourceHandler("image/**").addResourceLocations("file:" + "image/");
    }
}