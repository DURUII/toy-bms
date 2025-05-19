package com.mi.bms;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.mi.bms")
@EntityScan(basePackages = "com.mi.bms")
@EnableJpaRepositories(basePackages = "com.mi.bms")
@ActiveProfiles("test")
public class TestConfig {
}