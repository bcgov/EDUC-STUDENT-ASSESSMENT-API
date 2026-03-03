package ca.bc.gov.educ.assessment.api.config;

import com.fasterxml.jackson.core.StreamReadConstraints;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder.postConfigurer(mapper ->
            mapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                    .maxStringLength(50000000)
                    .build()
            )
        );
    }
}