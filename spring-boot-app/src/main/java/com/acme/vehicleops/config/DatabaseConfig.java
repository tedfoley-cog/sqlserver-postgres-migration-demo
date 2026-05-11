package com.acme.vehicleops.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Database configuration for stored procedure access.
 *
 * Production uses SQL Server via JDBC (com.microsoft.sqlserver.jdbc.SQLServerDriver).
 * Dev/demo uses H2 in SQL Server compatibility mode.
 *
 * All business logic is executed via stored procedures. The JdbcTemplate
 * is the primary interface — JPA entities are only used for simple lookups.
 */
@Configuration
public class DatabaseConfig {

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        template.setQueryTimeout(30);
        return template;
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }
}
