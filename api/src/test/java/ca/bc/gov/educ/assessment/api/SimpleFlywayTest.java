package ca.bc.gov.educ.assessment.api;

import ca.bc.gov.educ.assessment.api.config.TestDataSourceConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {StudentAssessmentApiApplication.class})
@ActiveProfiles("test")
@Import(TestDataSourceConfig.class)
public class SimpleFlywayTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void testApplicationContextLoads() {
        assertNotNull(dataSource, "DataSource should be available");
    }

    @Test
    void testFlywayMigrationRan() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            // Check if some of your tables exist
            ResultSet rs = statement.executeQuery(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'assessment_session'"
            );
            assertTrue(rs.next(), "Should have result");
            assertEquals(1, rs.getInt(1), "assessment_session table should exist");
            
            rs = statement.executeQuery(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'adapted_assessment_code'"
            );
            assertTrue(rs.next(), "Should have result");
            assertEquals(1, rs.getInt(1), "adapted_assessment_code table should exist");
        }
    }
}
