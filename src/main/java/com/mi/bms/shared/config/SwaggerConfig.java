package com.mi.bms.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI batteryManagementSystemOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Battery Management System API")
                        .description("API documentation for the Battery Management System")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("XiaoMi")
                                .url("https://www.mi.com")));
    }
}