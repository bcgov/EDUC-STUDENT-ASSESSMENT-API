package ca.bc.gov.educ.assessment.api.struct.external.grad.v1;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportGradStudentData {
  private UUID graduationStudentRecordId;
  private UUID schoolOfRecordId;
  private String pen;
  private String localID;
  private String firstName;
  private String middleName;
  private String lastName;
  private String dob;
  private String studentGrade;
  private String programCode;
  @JsonAlias("schoolOfRecordName")
  private String schoolName;
  private String studentStatus;
}
