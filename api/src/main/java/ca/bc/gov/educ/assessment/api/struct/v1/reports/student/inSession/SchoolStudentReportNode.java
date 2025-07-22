package ca.bc.gov.educ.assessment.api.struct.v1.reports.student.inSession;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@SuppressWarnings("squid:S1700")
public class SchoolStudentReportNode implements Serializable {
  private static final long serialVersionUID = 6118916290604876032L;

  private String sessionDetail;

  private String reportGeneratedDate;

  private String districtNumberAndName;

  private String schoolMincodeAndName;

  private List<SchoolStudentNode> students;

}
