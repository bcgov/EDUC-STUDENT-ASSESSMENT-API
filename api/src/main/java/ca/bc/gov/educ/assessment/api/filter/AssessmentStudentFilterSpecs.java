package ca.bc.gov.educ.assessment.api.filter;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class AssessmentStudentFilterSpecs extends BaseFilterSpecs<AssessmentStudentEntity> {

  public AssessmentStudentFilterSpecs(FilterSpecifications<AssessmentStudentEntity, ChronoLocalDate> dateFilterSpecifications, FilterSpecifications<AssessmentStudentEntity, ChronoLocalDateTime<?>> dateTimeFilterSpecifications, FilterSpecifications<AssessmentStudentEntity, Integer> integerFilterSpecifications, FilterSpecifications<AssessmentStudentEntity, String> stringFilterSpecifications, FilterSpecifications<AssessmentStudentEntity, Long> longFilterSpecifications, FilterSpecifications<AssessmentStudentEntity, UUID> uuidFilterSpecifications, FilterSpecifications<AssessmentStudentEntity, Boolean> booleanFilterSpecifications, Converters converters) {
    super(dateFilterSpecifications, dateTimeFilterSpecifications, integerFilterSpecifications, stringFilterSpecifications, longFilterSpecifications, uuidFilterSpecifications, booleanFilterSpecifications, converters);
  }
}
