package ca.bc.gov.educ.assessment.api.struct.v1.reports.student.byAssessment;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@NoArgsConstructor
@SuperBuilder
@SuppressWarnings("squid:S1700")
public class SchoolStudentNode implements Serializable {
  private static final long serialVersionUID = 6118916290604876032L;

  private String name;

  private String pen;

  private String localID;

  private String score;
}
