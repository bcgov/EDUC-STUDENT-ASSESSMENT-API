package ca.bc.gov.educ.api.studentassessment;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import ca.bc.gov.educ.api.studentassessment.model.dto.StudentAssessment;
import ca.bc.gov.educ.api.studentassessment.model.entity.StudentAssessmentEntity;

@SpringBootApplication
public class EducStudentAssessmentApiApplication {

	private static Logger logger = LoggerFactory.getLogger(EducStudentAssessmentApiApplication.class);

	public static void main(String[] args) {
		logger.debug("########Starting API");
		SpringApplication.run(EducStudentAssessmentApiApplication.class, args);
		logger.debug("########Started API");
	}

	@Bean
	public ModelMapper modelMapper() {

		ModelMapper modelMapper = new ModelMapper();

		modelMapper.typeMap(StudentAssessmentEntity.class, StudentAssessment.class);
		modelMapper.typeMap(StudentAssessment.class, StudentAssessmentEntity.class);
		return modelMapper;
	}
}