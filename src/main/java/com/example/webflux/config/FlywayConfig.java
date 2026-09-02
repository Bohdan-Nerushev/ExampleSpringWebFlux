package com.example.webflux.config;

import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    @Value("${spring.flyway.locations:classpath:db/migration}")
    private String locations;

    @Value("${spring.flyway.schemas:public}")
    private String schemas;

    @Value("${spring.flyway.baseline-on-migrate:true}")
    private boolean baselineOnMigrate;

    @Value("${spring.flyway.baseline-version:0}")
    private String baselineVersion;

    @Value("${spring.flyway.clean-disabled:true}")
    private boolean cleanDisabled;

    @Bean
    public Flyway flyway() {
        log.info("Initializing Flyway database migration...");

        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(url);
        dataSource.setUser(username);
        dataSource.setPassword(password);

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .schemas(schemas)
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion(baselineVersion)
                .cleanDisabled(cleanDisabled)
                .load();

        log.info("Executing Flyway migration...");
        try {
            var result = flyway.migrate();
            log.info("Flyway migration completed successfully! Applied {} migrations.", result.migrationsExecuted);
        } catch (Exception e) {
            log.error("Flyway migration failed!", e);
            throw e;
        }

        return flyway;
    }
}
