package com.yourname.ems.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI emsOpenApi(AppProperties props) {
        return new OpenAPI().info(new Info()
                .title(props.name())
                .version("v1.0.0")
                .description("REST API for HR teams to create, view, update, search and deactivate "
                        + "employee records. Data is held in memory (replaced by JPA/PostgreSQL in Phase 9). "
                        + "Default department: " + props.defaultDepartment() + ".")
                .contact(new Contact().name("EMS Team").email("ems@example.com")));
    }
}
