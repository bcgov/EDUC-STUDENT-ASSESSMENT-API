package ca.bc.gov.educ.assessment.api.config;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.io.IOException;

@TestConfiguration
public class TestDataSourceConfig {

    @Bean(destroyMethod = "close")
    @Primary
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        return EmbeddedPostgres.builder()
                .setPort(0)
                .start();
    }

    @Bean
    @Primary
    @DependsOn("embeddedPostgres")
    public DataSource dataSource(EmbeddedPostgres postgres) throws IOException {
        DataSource dataSource = postgres.getPostgresDatabase();
        
        // Run Flyway migrations immediately after datasource creation
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .baselineOnMigrate(true)
                .load();
        
        flyway.migrate();
        
        return dataSource;
    }
}
