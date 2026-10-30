package com.bank.overdraft.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI overdraftOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Enterprise Overdraft (OD) Facility Management API")
                .version("1.0.0")
                .description("Core-banking style overdraft engine: drawdown, repayment, EOD interest accrual, "
                        + "monthly billing and OD vs personal loan analytics.")
                .contact(new Contact().name("Core Banking Platform Team")));
    }
}
