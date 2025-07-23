package ca.bc.gov.educ.assessment.api.filter;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentHistorySearchEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class AssessmentStudentHistoryFilterSpecs extends BaseFilterSpecs<AssessmentStudentHistorySearchEntity> {

  public AssessmentStudentHistoryFilterSpecs(FilterSpecifications<AssessmentStudentHistorySearchEntity, ChronoLocalDate> dateFilterSpecifications, FilterSpecifications<AssessmentStudentHistorySearchEntity, ChronoLocalDateTime<?>> dateTimeFilterSpecifications, FilterSpecifications<AssessmentStudentHistorySearchEntity, Integer> integerFilterSpecifications, FilterSpecifications<AssessmentStudentHistorySearchEntity, String> stringFilterSpecifications, FilterSpecifications<AssessmentStudentHistorySearchEntity, Long> longFilterSpecifications, FilterSpecifications<AssessmentStudentHistorySearchEntity, UUID> uuidFilterSpecifications, FilterSpecifications<AssessmentStudentHistorySearchEntity, Boolean> booleanFilterSpecifications, Converters converters) {
    super(dateFilterSpecifications, dateTimeFilterSpecifications, integerFilterSpecifications, stringFilterSpecifications, longFilterSpecifications, uuidFilterSpecifications, booleanFilterSpecifications, converters);
  }
}
