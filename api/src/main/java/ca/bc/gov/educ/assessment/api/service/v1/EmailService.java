package ca.bc.gov.educ.assessment.api.service.v1;


import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.properties.EmailProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.v1.ApprovalSagaData;
import ca.bc.gov.educ.assessment.api.struct.v1.EmailSagaData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.*;

@Service
@Slf4j
public class EmailService {

  private final SpringTemplateEngine templateEngine;
  private final Map<String, String> templateConfig;
  private final RestUtils restUtils;
  private final EmailProperties emailProperties;
  private final AssessmentSessionRepository assessmentSessionRepository;

  public EmailService(final SpringTemplateEngine templateEngine, final Map<String, String> templateConfig, RestUtils restUtils, EmailProperties emailProperties, AssessmentSessionRepository assessmentSessionRepository) {
    this.templateEngine = templateEngine;
    this.templateConfig = templateConfig;
    this.restUtils = restUtils;
    this.emailProperties = emailProperties;
    this.assessmentSessionRepository = assessmentSessionRepository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void sendMyEDApprovalEmail(ApprovalSagaData approvalSagaData) {
    UUID sessionID = UUID.fromString(approvalSagaData.getSessionID());
    var session = assessmentSessionRepository.findById(sessionID).orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class, "assessmentSessionID", sessionID.toString()));

    var emailFields = new HashMap<String, String>();
    emailFields.put("currentSession", session.getCourseYear() + "/" + session.getCourseMonth());

    var subject = emailProperties.getEmailSubjectMyEdApproval();
    var fromEmail = emailProperties.getEmailMyEdApprovalFrom();
    var toEmail = Collections.singletonList(emailProperties.getEmailMyEdApprovalTo());

    var emailSagaData = createEmailSagaData(fromEmail, toEmail, subject, "myed.approval.notification", emailFields);
    sendEmail(emailSagaData);
  }

  public void sendEmail(final EmailSagaData emailSagaData) {
    log.debug("Sending email");

    final var ctx = new Context();
    emailSagaData.getEmailFields().forEach(ctx::setVariable);

    if (!this.templateConfig.containsKey(emailSagaData.getTemplateName())) {
      throw new StudentAssessmentAPIRuntimeException("Email template not found for template name :: " + emailSagaData.getTemplateName());
    }

    final var body = this.templateEngine.process(this.templateConfig.get(emailSagaData.getTemplateName()), ctx);

    this.restUtils.sendEmail(emailSagaData.getFromEmail(), emailSagaData.getToEmails(), body, emailSagaData.getSubject());
    log.debug("Email sent successfully");
  }

  public EmailSagaData createEmailSagaData(String fromEmail, List<String> emailList, String subject, String templateName, Map<String, String> emailFields){
    return EmailSagaData
            .builder()
            .fromEmail(fromEmail)
            .toEmails(emailList)
            .subject(subject)
            .templateName(templateName)
            .emailFields(emailFields)
            .build();
  }

}
