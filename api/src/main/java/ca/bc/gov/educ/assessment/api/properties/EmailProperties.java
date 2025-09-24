package ca.bc.gov.educ.assessment.api.properties;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class EmailProperties {

  @Value("${email.subject.myed.approval.notification}")
  private String emailSubjectMyEdApproval;

  @Value("${email.myed.approval.from}")
  private String emailMyEdApprovalFrom;

  @Value("${email.myed.approval.to}")
  private String emailMyEdApprovalTo;

}


