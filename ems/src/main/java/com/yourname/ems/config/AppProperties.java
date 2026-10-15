package com.yourname.ems.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Binds the "app.*" properties from application.yml.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @DefaultValue("Employee Management API") String name,
        @DefaultValue("IT") String defaultDepartment,
        @DefaultValue("10000000") BigDecimal maxSalary) {
}
