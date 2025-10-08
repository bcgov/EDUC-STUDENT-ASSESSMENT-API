package ca.bc.gov.educ.assessment.api.reports;

import ca.bc.gov.educ.assessment.api.constants.v1.LanguageCode;
import ca.bc.gov.educ.assessment.api.constants.v1.ProvincialSpecialCaseCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.reports.AssessmentStudentReportTypeCode;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import ca.bc.gov.educ.assessment.api.exception.errors.ApiError;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentQuestionEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentAnswerEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentComponentEntity;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.service.v1.CodeTableService;
import ca.bc.gov.educ.assessment.api.struct.external.grad.v1.GradStudentRecord;
import ca.bc.gov.educ.assessment.api.struct.external.studentapi.v1.Student;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.DownloadableReportResponse;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.isr.*;
import ca.bc.gov.educ.assessment.api.util.PenUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.util.Pair;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRSaver;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static ca.bc.gov.educ.assessment.api.constants.v1.ComponentTypeCodes.MUL_CHOICE;
import static ca.bc.gov.educ.assessment.api.constants.v1.ComponentTypeCodes.OPEN_ENDED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Service class for generating School Students by Assessment Report
 */
@Service
@Slf4j
public class ISRReportService extends BaseReportGenerationService {

  private final AssessmentQuestionRepository assessmentQuestionRepository;
  private final AssessmentStudentAnswerRepository assessmentStudentAnswerRepository;
  private final AssessmentStudentRepository assessmentStudentRepository;
  private final AssessmentStudentComponentRepository assessmentStudentComponentRepository;
  private final RestUtils restUtils;
  private final CodeTableService codeTableService;
  private JasperReport isrReport;

  public ISRReportService(AssessmentQuestionRepository assessmentQuestionRepository, AssessmentStudentAnswerRepository assessmentStudentAnswerRepository, AssessmentStudentRepository assessmentStudentRepository, AssessmentStudentComponentRepository assessmentStudentComponentRepository, RestUtils restUtils, CodeTableService codeTableService) {
    super(restUtils);
    this.assessmentQuestionRepository = assessmentQuestionRepository;
    this.assessmentStudentAnswerRepository = assessmentStudentAnswerRepository;
    this.assessmentStudentRepository = assessmentStudentRepository;
    this.assessmentStudentComponentRepository = assessmentStudentComponentRepository;
    this.restUtils = restUtils;
    this.codeTableService = codeTableService;
  }

  @PostConstruct
  public void init() {
    ApplicationProperties.bgTask.execute(this::initialize);
  }

  private void initialize() {
    this.compileJasperReports();
  }

  private void compileJasperReports(){
    try {
      String compiledReportsPath = System.getProperty("java.io.tmpdir") + "/jasper-reports/";
      File compiledDir = new File(compiledReportsPath);
      if (!compiledDir.exists()) {
        compiledDir.mkdirs();
      }

      InputStream subReport1 = getClass().getResourceAsStream("/reports/lte10summary.jrxml");
      JasperReport compiled1 = JasperCompileManager.compileReport(subReport1);
      JRSaver.saveObject(compiled1, compiledReportsPath + "lte10summary.jasper");
      
      InputStream subReport2 = getClass().getResourceAsStream("/reports/lte12summary.jrxml");
      JasperReport compiled2 = JasperCompileManager.compileReport(subReport2);
      JRSaver.saveObject(compiled2, compiledReportsPath + "lte12summary.jasper");
      
      InputStream subReport3 = getClass().getResourceAsStream("/reports/ltp10summary.jrxml");
      JasperReport compiled3 = JasperCompileManager.compileReport(subReport3);
      JRSaver.saveObject(compiled3, compiledReportsPath + "ltp10summary.jasper");
      
      InputStream subReport4 = getClass().getResourceAsStream("/reports/ltp12summary.jrxml");
      JasperReport compiled4 = JasperCompileManager.compileReport(subReport4);
      JRSaver.saveObject(compiled4, compiledReportsPath + "ltp12summary.jasper");
      
      InputStream subReport5 = getClass().getResourceAsStream("/reports/ltf12summary.jrxml");
      JasperReport compiled5 = JasperCompileManager.compileReport(subReport5);
      JRSaver.saveObject(compiled5, compiledReportsPath + "ltf12summary.jasper");
      
      InputStream subReport6 = getClass().getResourceAsStream("/reports/nme10summary.jrxml");
      JasperReport compiled6 = JasperCompileManager.compileReport(subReport6);
      JRSaver.saveObject(compiled6, compiledReportsPath + "nme10summary.jasper");
      
      InputStream subReport7 = getClass().getResourceAsStream("/reports/nmf10summary.jrxml");
      JasperReport compiled7 = JasperCompileManager.compileReport(subReport7);
      JRSaver.saveObject(compiled7, compiledReportsPath + "nmf10summary.jasper");

      InputStream report = getClass().getResourceAsStream("/reports/isrMain.jrxml");
      isrReport = JasperCompileManager.compileReport(report);
    } catch (JRException e) {
      log.error("JRException: ", e);
      log.error("JRException: ", e.getMessage());
      e.printStackTrace();
      throw new StudentAssessmentAPIRuntimeException("Compiling Jasper reports has failed :: " + e.getMessage());
    }
  }

  public DownloadableReportResponse generateIndividualStudentReportByPEN(String pen){
    if(!PenUtil.validCheckDigit(pen)){
      ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Payload contains invalid PEN").status(BAD_REQUEST).build();
      throw new InvalidPayloadException(error);
    }
    var student = restUtils.getStudentByPEN(UUID.randomUUID(), pen);
    if(student.isEmpty()){
      ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Payload contains invalid PEN").status(BAD_REQUEST).build();
      throw new InvalidPayloadException(error);
    }
    return generateIndividualStudentReport(UUID.fromString(student.get().getStudentID()));
  }

  public DownloadableReportResponse generateIndividualStudentReport(UUID studentID){
    try {
      var studentAssessments = assessmentStudentRepository.findAllWrittenAssessmentsForStudent(studentID);
      var gradStudentRecord = restUtils.getGradStudentRecordByStudentID(UUID.randomUUID(), studentID).orElseThrow(() -> new EntityNotFoundException(GradStudentRecord.class, "studentID", studentID.toString()));
      var students = restUtils.getStudents(UUID.randomUUID(), Set.of(studentID.toString()));
      if(students.isEmpty()){
        log.error("Student could not be found while writing PDF report :: " + studentID);
        throw new EntityNotFoundException(Student.class, "Student could not be found while writing PDF report :: ", studentID.toString());
      }
      
      var student = students.get(0);
      
      ISRRootNode isrRootNode = new ISRRootNode();
      ISRReportNode reportNode = new ISRReportNode();
      setReportTombstoneValues(UUID.fromString(gradStudentRecord.getSchoolOfRecordId()), reportNode, student.getPen(), student.getLegalLastName() + ", " + student.getLegalFirstName());
      isrRootNode.setReport(reportNode);
      reportNode.setAssessments(new ArrayList<>());
      reportNode.setAssessmentDetails(new ArrayList<>());
      var assessmentTypes = codeTableService.getAllAssessmentTypeCodesAsMap();

      studentAssessments.forEach((assessmentStudent) -> {
        ISRAssessmentSummary assessmentSummary = new ISRAssessmentSummary();
        assessmentSummary.setSession(assessmentStudent.getAssessmentEntity().getAssessmentSessionEntity().getCourseYear() + "/" + assessmentStudent.getAssessmentEntity().getAssessmentSessionEntity().getCourseMonth());
        assessmentSummary.setScore(getProficiencyScore(assessmentStudent.getProficiencyScore(), getReportLanguage(assessmentStudent.getAssessmentEntity().getAssessmentTypeCode())));
        assessmentSummary.setSpecialCase(StringUtils.isNotBlank(assessmentStudent.getProvincialSpecialCaseCode()) ? ProvincialSpecialCaseCodes.findByValue(assessmentStudent.getProvincialSpecialCaseCode()).get().getDescription() : "");
        assessmentSummary.setAssessment(assessmentTypes.get(assessmentStudent.getAssessmentEntity().getAssessmentTypeCode()));
        assessmentSummary.setAssessmentCode(getAssessmentCodeValue(assessmentStudent.getAssessmentEntity().getAssessmentTypeCode()));
        reportNode.getAssessments().add(assessmentSummary);

        if(assessmentStudent.getProficiencyScore() != null) {
          var questions = assessmentQuestionRepository.findByAssessmentComponentEntity_AssessmentFormEntity_AssessmentFormID(assessmentStudent.getAssessmentFormID());
          var studentAnswers = assessmentStudentAnswerRepository.findAllByAssessmentStudentComponentEntity_AssessmentStudentEntity_AssessmentStudentID_AndAssessmentQuestionIDIsNotNull(assessmentStudent.getAssessmentStudentID());

          switch (assessmentStudent.getAssessmentEntity().getAssessmentTypeCode()) {
            case "LTE10":
              reportNode.getAssessmentDetails().add(populateLTE10Assessment(assessmentSummary, questions, studentAnswers, assessmentStudent.getAssessmentStudentID()));
              break;
            case "LTE12":
              reportNode.getAssessmentDetails().add(populateLTE12Assessment(assessmentSummary, questions, studentAnswers, assessmentStudent.getAssessmentStudentID()));
              break;
            case "LTF12":
              reportNode.getAssessmentDetails().add(populateLTF12Assessment(assessmentSummary, questions, studentAnswers, assessmentStudent.getAssessmentStudentID()));
              break;
            case "LTP10":
              reportNode.getAssessmentDetails().add(populateLTP10Assessment(assessmentSummary, questions, studentAnswers, assessmentStudent.getAssessmentStudentID()));
              break;
            case "LTP12":
              reportNode.getAssessmentDetails().add(populateLTP12Assessment(assessmentSummary, questions, studentAnswers, assessmentStudent.getAssessmentStudentID()));
              break;
            case "NME":
            case "NME10":
              reportNode.getAssessmentDetails().add(populateNME10Assessment(assessmentSummary, questions, studentAnswers, assessmentStudent.getAssessmentStudentID()));
              break;
            case "NMF":
            case "NMF10":
              reportNode.getAssessmentDetails().add(populateNMF10Assessment(assessmentSummary, questions, studentAnswers, assessmentStudent.getAssessmentStudentID()));
              break;
          }
        }
      });
      
      var payload = objectWriter.writeValueAsString(isrRootNode);
      log.info("Payload for ISR is: " + payload);
      
      return generateJasperReport(payload, isrReport, AssessmentStudentReportTypeCode.ISR.getCode());
    } catch (JsonProcessingException e) {
      log.error("Exception occurred while writing PDF report for ell programs :: " + e.getMessage());
      throw new StudentAssessmentAPIRuntimeException("Exception occurred while writing PDF report for ell programs :: " + e.getMessage());
    }
  }
  
  private LanguageCode getReportLanguage(String assessmentTypeCode){
      return switch (assessmentTypeCode) {
          case "LTE10", "LTE12", "NME", "NME10" -> LanguageCode.ENGLISH;
          case "LTF12", "LTP10", "LTP12", "NMF", "NMF10" -> LanguageCode.FRENCH;
          default -> LanguageCode.ENGLISH;
      };
  }
  
  private String getAssessmentCodeValue(String assessmentCode){
    if(assessmentCode.equalsIgnoreCase("NME")){
      return "NME10";
    }else if(assessmentCode.equalsIgnoreCase("NMF")){
      return "NMF10";
    }
    return assessmentCode;
  }

  private List<AssessmentQuestionEntity> getMultiChoiceQuestions(List<AssessmentQuestionEntity> questions){
    return questions.stream().filter(assessmentQuestionEntity -> assessmentQuestionEntity.getAssessmentComponentEntity().getComponentTypeCode().equalsIgnoreCase(MUL_CHOICE.getCode())).toList();
  }
  
  private Pair<String, String> getResultSummaryForQuestionsWithTaskCode(UUID assessmentStudentID, List<AssessmentQuestionEntity> questions, List<AssessmentStudentAnswerEntity> answers, String questionType, String taskCode){
    var filteredQuestions = questions.stream().filter(assessmentQuestionEntity -> StringUtils.isNotBlank(assessmentQuestionEntity.getTaskCode()) && assessmentQuestionEntity.getTaskCode().equals(taskCode) && assessmentQuestionEntity.getAssessmentComponentEntity().getComponentTypeCode().equalsIgnoreCase(questionType)).toList();
    String choicePath = getChoicePath(filteredQuestions, assessmentStudentID);
    return getTotals(filteredQuestions, answers, questionType, choicePath);
  }

  private Pair<String, String> getResultSummaryForQuestionsWithClaimCode(UUID assessmentStudentID, List<AssessmentQuestionEntity> questions, List<AssessmentStudentAnswerEntity> answers, String questionType, String claimCode){
    var filteredQuestions = questions.stream().filter(assessmentQuestionEntity -> StringUtils.isNotBlank(assessmentQuestionEntity.getClaimCode()) && assessmentQuestionEntity.getClaimCode().equals(claimCode) && assessmentQuestionEntity.getAssessmentComponentEntity().getComponentTypeCode().equalsIgnoreCase(questionType)).toList();
    String choicePath = getChoicePath(filteredQuestions, assessmentStudentID);
    return getTotals(filteredQuestions, answers, questionType, choicePath);
  }

  private Pair<String, String> getResultSummaryForQuestionsWithConceptsCode(UUID assessmentStudentID, List<AssessmentQuestionEntity> questions, List<AssessmentStudentAnswerEntity> answers, String questionType, String conceptsCode){
    var filteredQuestions = questions.stream().filter(assessmentQuestionEntity -> StringUtils.isNotBlank(assessmentQuestionEntity.getConceptCode()) && assessmentQuestionEntity.getConceptCode().equals(conceptsCode) && assessmentQuestionEntity.getAssessmentComponentEntity().getComponentTypeCode().equalsIgnoreCase(questionType)).toList();
    String choicePath = getChoicePath(filteredQuestions, assessmentStudentID);
    return getTotals(filteredQuestions, answers, questionType, choicePath);
  }

  private Pair<String, String> getResultSummaryForQuestionsWithAssessmentSectionStartsWith(UUID assessmentStudentID, List<AssessmentQuestionEntity> questions, List<AssessmentStudentAnswerEntity> answers, String questionType, String assessmentSection){
    var filteredQuestions = questions.stream().filter(assessmentQuestionEntity -> StringUtils.isNotBlank(assessmentQuestionEntity.getAssessmentSection()) && assessmentQuestionEntity.getAssessmentSection().startsWith(assessmentSection) && assessmentQuestionEntity.getAssessmentComponentEntity().getComponentTypeCode().equalsIgnoreCase(questionType)).toList();
    String choicePath = getChoicePath(filteredQuestions, assessmentStudentID);
    return getTotals(filteredQuestions, answers, questionType, choicePath);
  }
  
  private Pair<String, String> getTotals(List<AssessmentQuestionEntity> filteredQuestions, List<AssessmentStudentAnswerEntity> answers, String questionType, String choicePath){
    BigDecimal totalScore = new BigDecimal(0);
    
    log.debug("Incoming choice path for questions :: " + choicePath);
    log.debug("Filtered questions are: " + filteredQuestions);
    
    for(AssessmentStudentAnswerEntity answer : answers){
      log.debug("Answer question ID: {}", answer.getAssessmentQuestionID());
      var question = filteredQuestions.stream().filter(filteredQuestion -> filteredQuestion.getAssessmentQuestionID().equals(answer.getAssessmentQuestionID())).findFirst();
      log.debug("Found question: {}", question);
      if(answer.getScore() != null && question.isPresent()) {
        log.debug("Current total score: {}", totalScore);
        log.debug("Adding the following value to total score: {}", answer.getScore().multiply(new BigDecimal(question.get().getScaleFactor()/100)));
        totalScore = totalScore.add(answer.getScore().multiply(new BigDecimal(question.get().getScaleFactor()/100)));
        log.debug("Total score now: {}", totalScore);
      }
    }

    BigDecimal totalOutOf = new BigDecimal(0);

    for (AssessmentQuestionEntity question : filteredQuestions) {
      log.debug("Question check: {}", question.getQuestionNumber().equals(question.getMasterQuestionNumber()));
      if (questionType.equalsIgnoreCase(MUL_CHOICE.getCode())) {
        String choicePathToIgnore = null;
        if(StringUtils.isNotBlank(choicePath)) {
          choicePathToIgnore = choicePathToIgnore(choicePath);  
        }
        log.debug("Choice path to ignore currently: {}", choicePathToIgnore);
        if(StringUtils.isBlank(choicePathToIgnore) || !question.getTaskCode().equalsIgnoreCase(choicePathToIgnore)) {
          log.debug("Current total out of: {}", totalOutOf);
          log.debug("Adding the following value to total out of: {}", question.getQuestionValue().multiply(new BigDecimal(question.getScaleFactor() / 100)));
          totalOutOf = totalOutOf.add(question.getQuestionValue().multiply(new BigDecimal(question.getScaleFactor() / 100)));
          log.debug("Total out of now: {}", totalOutOf);
        }
      } else if (question.getQuestionNumber().equals(question.getMasterQuestionNumber())) {
        log.debug("Current total out of non-multi: {}", totalOutOf);
        log.debug("Adding the following value to total out of non-multi: {}", question.getQuestionValue().multiply(new BigDecimal(question.getScaleFactor() / 100)));
        totalOutOf = totalOutOf.add(question.getQuestionValue().multiply(new BigDecimal(question.getScaleFactor() / 100)));
        log.debug("Total out of now non-multi: {}", totalOutOf);
      }
    }

    if(answers.isEmpty()){
      return Pair.of("No Response", totalOutOf.toString());
    }
    
    return Pair.of(totalScore.toString(), totalOutOf.toString());
  }
  
  private String getChoicePath(List<AssessmentQuestionEntity> filteredQuestions, UUID assessmentStudentID){
    if(!filteredQuestions.isEmpty()){
      var componentID = filteredQuestions.getFirst().getAssessmentComponentEntity().getAssessmentComponentID();
      log.debug("Found component with ID: " + componentID);
      var studentComponent = assessmentStudentComponentRepository.findAllByAssessmentStudentEntity_AssessmentStudentIDAndAssessmentComponentID(assessmentStudentID, componentID);
      log.debug("Found student component through query: " + studentComponent);
      return studentComponent.map(AssessmentStudentComponentEntity::getChoicePath).orElse(null);
    }
    return null;
  }
  
  private String choicePathToIgnore(String choicePath){
    if(choicePath.equalsIgnoreCase("I")){
      return "E";
    }
    return "I";
  }

  protected void setReportTombstoneValues(UUID schoolID, ISRReportNode reportNode, String studentPEN, String studentName){
    var school = validateAndReturnSchool(schoolID);

    reportNode.setReportGeneratedDate(LocalDate.now().format(formatter));
    reportNode.setSchoolDetail(school.getDisplayName());
    reportNode.setStudentPEN(studentPEN);
    reportNode.setStudentName(studentName);
  }

  private NME10Assessment populateNME10Assessment(ISRAssessmentSummary assessmentSummary, List<AssessmentQuestionEntity> questions, List<AssessmentStudentAnswerEntity> studentAnswers, UUID assessmentStudentID){
    NME10Assessment assessmentNME10 = new NME10Assessment();
    assessmentNME10.setSession(assessmentSummary.getSession());
    assessmentNME10.setScore(assessmentSummary.getScore());
    assessmentNME10.setAssessmentCode(assessmentSummary.getAssessmentCode());

    var planDesign = getResultSummaryForQuestionsWithTaskCode(assessmentStudentID, questions, studentAnswers, MUL_CHOICE.getCode(), "P");
    assessmentNME10.setOnlinePlanAndDesignScore(planDesign.getLeft());
    assessmentNME10.setOnlinePlanAndDesignOutOf(planDesign.getRight());
    var reasonedEstimates = getResultSummaryForQuestionsWithTaskCode(assessmentStudentID,questions, studentAnswers, MUL_CHOICE.getCode(), "R");
    assessmentNME10.setOnlineReasonedEstimatesScore(reasonedEstimates.getLeft());
    assessmentNME10.setOnlineReasonedEstimatesOutOf(reasonedEstimates.getRight());
    var fairShare = getResultSummaryForQuestionsWithTaskCode(assessmentStudentID,questions, studentAnswers, MUL_CHOICE.getCode(), "F");
    assessmentNME10.setOnlineFairShareScore(fairShare.getLeft());
    assessmentNME10.setOnlineFairShareOutOf(fairShare.getRight());
    var model = getResultSummaryForQuestionsWithTaskCode(assessmentStudentID,questions, studentAnswers, MUL_CHOICE.getCode(), "M");
    assessmentNME10.setOnlineModelScore(model.getLeft());
    assessmentNME10.setOnlineModelOutOf(model.getRight());
    var writtenFairShare = getResultSummaryForQuestionsWithTaskCode(assessmentStudentID,questions, studentAnswers, OPEN_ENDED.getCode(), "F");
    assessmentNME10.setWrittenFairScore(writtenFairShare.getLeft());
    assessmentNME10.setWrittenFairOutOf(writtenFairShare.getRight());
    var writtenReasoned = getResultSummaryForQuestionsWithTaskCode(assessmentStudentID,questions, studentAnswers, OPEN_ENDED.getCode(), "R");
    assessmentNME10.setWrittenReasonedEstimatesScore(writtenReasoned.getLeft());
    assessmentNME10.setWrittenReasonedEstimatesOutOf(writtenReasoned.getRight());

    return assessmentNME10;
  }

  private NMF10Assessment populateNMF10Assessment(ISRAssessmentSummary assessmentSummary,List<AssessmentQuestionEntity> questions, List<AssessmentStudentAnswerEntity> studentAnswers, UUID assessmentStudentID){
    NMF10Assessment assessmentNMF10 = new NMF10Assessment();
    
    assessmentNMF10.setSession(assessmentSummary.getSession());
    assessmentNMF10.setScore(assessmentSummary.getScore());
    assessmentNMF10.setAssessmentCode(assessmentSummary.getAssessmentCode());
    
    var planDesign = getResultSummaryForQuestionsWithTaskCode(assessmentStudentID, questions, studentAnswers, MUL_CHOICE.getCode(), "P");
    assessmentNMF10.setMultiChoicePlanningScore(planDesign.getLeft().replace(".", ","));
    assessmentNMF10.setMultiChoicePlanningOutOf(planDesign.getRight());
    var reasonedEstimates = getResultSummaryForQuestionsWithTaskCode(assessmentStudentID, questions, studentAnswers, MUL_CHOICE.getCode(), "R");
    assessmentNMF10.setMultiChoiceEstimationsScore(reasonedEstimates.getLeft().replace(".", ","));
    assessmentNMF10.setMultiChoiceEstimationsOutOf(reasonedEstimates.getRight());
    var fairShare = getResultSummaryForQuestionsWithTaskCode(assessmentStudentID, questions, studentAnswers, MUL_CHOICE.getCode(), "F");
    assessmentNMF10.setMultiChoiceGroupingScore(fairShare.getLeft().replace(".", ","));
    assessmentNMF10.setMultiChoiceGroupingOutOf(fairShare.getRight());
    var model = getResultSummaryForQuestionsWithConceptsCode(assessmentStudentID, questions, studentAnswers, MUL_CHOICE.getCode(), "M");
    assessmentNMF10.setMultiChoiceModelScore(model.getLeft().replace(".", ","));
    assessmentNMF10.setMultiChoiceModelOutOf(model.getRight());
    var writtenFairShare = getResultSummaryForQuestionsWithTaskCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "F");
    assessmentNMF10.setWrittenGroupingScore(writtenFairShare.getLeft().replace(".", ","));
    assessmentNMF10.setWrittenGroupingOutOf(writtenFairShare.getRight());
    var writtenReasoned = getResultSummaryForQuestionsWithTaskCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "R");
    assessmentNMF10.setWrittenPlanningScore(writtenReasoned.getLeft().replace(".", ","));
    assessmentNMF10.setWrittenPlanningOutOf(writtenReasoned.getRight());

    return assessmentNMF10;
  }
  
  private LTE10Assessment populateLTE10Assessment(ISRAssessmentSummary assessmentSummary,List<AssessmentQuestionEntity> questions, List<AssessmentStudentAnswerEntity> studentAnswers, UUID assessmentStudentID){
    LTE10Assessment assessmentLTE10 = new LTE10Assessment();
    assessmentLTE10.setSession(assessmentSummary.getSession());
    assessmentLTE10.setScore(assessmentSummary.getScore());
    assessmentLTE10.setAssessmentCode(assessmentSummary.getAssessmentCode());

    var comprehend = getResultSummaryForQuestionsWithClaimCode(assessmentStudentID, questions, studentAnswers, MUL_CHOICE.getCode(), "C");
    assessmentLTE10.setComprehendScore(comprehend.getLeft());
    assessmentLTE10.setComprehendOutOf(comprehend.getRight());
    var communicate = getResultSummaryForQuestionsWithClaimCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "W");
    assessmentLTE10.setCommunicateScore(communicate.getLeft());
    assessmentLTE10.setCommunicateOutOf(communicate.getRight());
    var partASelectedResponse = getResultSummaryForQuestionsWithAssessmentSectionStartsWith(assessmentStudentID, questions, studentAnswers, MUL_CHOICE.getCode(), "A");
    assessmentLTE10.setPartASelectedResponseScore(partASelectedResponse.getLeft());
    assessmentLTE10.setPartASelectedResponseOutOf(partASelectedResponse.getRight());
    var partAWritten = getResultSummaryForQuestionsWithConceptsCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "GO");
    assessmentLTE10.setPartAWrittenResponseGraphicScore(partAWritten.getLeft());
    assessmentLTE10.setPartAWrittenResponseGraphicOutOf(partAWritten.getRight());
    var partAWrittenUnderstanding = getResultSummaryForQuestionsWithConceptsCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "WRA");
    assessmentLTE10.setPartAWrittenResponseUnderstandingScore(partAWrittenUnderstanding.getLeft());
    assessmentLTE10.setPartAWrittenResponseUnderstandingOutOf(partAWrittenUnderstanding.getRight());
    var partBSelectedResponse = getResultSummaryForQuestionsWithAssessmentSectionStartsWith(assessmentStudentID, questions, studentAnswers, MUL_CHOICE.getCode(), "B");
    assessmentLTE10.setPartBSelectedResponseScore(partBSelectedResponse.getLeft());
    assessmentLTE10.setPartBSelectedResponseOutOf(partBSelectedResponse.getRight());
    var partBWrittenResponse = getResultSummaryForQuestionsWithConceptsCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "WRB");
    assessmentLTE10.setPartBWrittenResponseUnderstandingScore(partBWrittenResponse.getLeft());
    assessmentLTE10.setPartBWrittenResponseUnderstandingOutOf(partBWrittenResponse.getRight());
    return assessmentLTE10;
  }

  private LTE12Assessment populateLTE12Assessment(ISRAssessmentSummary assessmentSummary,List<AssessmentQuestionEntity> questions, List<AssessmentStudentAnswerEntity> studentAnswers, UUID assessmentStudentID){
    LTE12Assessment assessmentLTE12 = new LTE12Assessment();
    assessmentLTE12.setSession(assessmentSummary.getSession());
    assessmentLTE12.setScore(assessmentSummary.getScore());
    assessmentLTE12.setAssessmentCode(assessmentSummary.getAssessmentCode());

    var comprehend = getResultSummaryForQuestionsWithClaimCode(assessmentStudentID, questions, studentAnswers, MUL_CHOICE.getCode(), "C");
    assessmentLTE12.setComprehendScore(comprehend.getLeft());
    assessmentLTE12.setComprehendOutOf(comprehend.getRight());
    var communicate = getResultSummaryForQuestionsWithClaimCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "W");
    assessmentLTE12.setCommunicateScore(communicate.getLeft());
    assessmentLTE12.setCommunicateOutOf(communicate.getRight());
    var partASelectedResponse = getResultSummaryForQuestionsWithAssessmentSectionStartsWith(assessmentStudentID, questions, studentAnswers, MUL_CHOICE.getCode(), "A");
    assessmentLTE12.setPartASelectedResponseScore(partASelectedResponse.getLeft());
    assessmentLTE12.setPartASelectedResponseOutOf(partASelectedResponse.getRight());
    var partAWritten = getResultSummaryForQuestionsWithConceptsCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "GO");
    assessmentLTE12.setPartAWrittenResponseGraphicScore(partAWritten.getLeft());
    assessmentLTE12.setPartAWrittenResponseGraphicOutOf(partAWritten.getRight());
    var partAWrittenUnderstanding = getResultSummaryForQuestionsWithConceptsCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "WRA");
    assessmentLTE12.setPartAWrittenResponseUnderstandingScore(partAWrittenUnderstanding.getLeft());
    assessmentLTE12.setPartAWrittenResponseUnderstandingOutOf(partAWrittenUnderstanding.getRight());
    var partBSelectedResponse = getResultSummaryForQuestionsWithAssessmentSectionStartsWith(assessmentStudentID, questions, studentAnswers, MUL_CHOICE.getCode(), "B");
    assessmentLTE12.setPartBSelectedResponseScore(partBSelectedResponse.getLeft());
    assessmentLTE12.setPartBSelectedResponseOutOf(partBSelectedResponse.getRight());
    var partBWrittenResponse = getResultSummaryForQuestionsWithConceptsCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "WRB");
    assessmentLTE12.setPartBWrittenResponseUnderstandingScore(partBWrittenResponse.getLeft());
    assessmentLTE12.setPartBWrittenResponseUnderstandingOutOf(partBWrittenResponse.getRight());
    return assessmentLTE12;
  }

  private LTP12Assessment populateLTP12Assessment(ISRAssessmentSummary assessmentSummary,List<AssessmentQuestionEntity> questions, List<AssessmentStudentAnswerEntity> studentAnswers, UUID assessmentStudentID){
    LTP12Assessment assessmentLTP12 = new LTP12Assessment();
    
    assessmentLTP12.setSession(assessmentSummary.getSession());
    assessmentLTP12.setScore(assessmentSummary.getScore());
    assessmentLTP12.setAssessmentCode(assessmentSummary.getAssessmentCode());

    var comprehend = getResultSummaryForQuestionsWithClaimCode(assessmentStudentID, questions, studentAnswers, MUL_CHOICE.getCode(), "C");
    assessmentLTP12.setComprehendScore(comprehend.getLeft().replace(".", ","));
    assessmentLTP12.setComprehendOutOf(comprehend.getRight());
    var communicate = getResultSummaryForQuestionsWithClaimCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "W");
    assessmentLTP12.setCommunicateScore(communicate.getLeft().replace(".", ","));
    assessmentLTP12.setCommunicateOutOf(communicate.getRight());
    var communicateOral = getResultSummaryForQuestionsWithClaimCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "O");
    assessmentLTP12.setCommunicateOralScore(communicateOral.getLeft().replace(".", ","));
    assessmentLTP12.setCommunicateOralOutOf(communicateOral.getRight());
    var partASelectedResponse = getResultSummaryForQuestionsWithAssessmentSectionStartsWith(assessmentStudentID, questions, studentAnswers, MUL_CHOICE.getCode(), "A");
    assessmentLTP12.setPartASelectedResponseScore(partASelectedResponse.getLeft().replace(".", ","));
    assessmentLTP12.setPartASelectedResponseOutOf(partASelectedResponse.getRight());
    var partAWritten = getResultSummaryForQuestionsWithConceptsCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "GO");
    assessmentLTP12.setPartAWrittenResponseGraphicScore(partAWritten.getLeft().replace(".", ","));
    assessmentLTP12.setPartAWrittenResponseGraphicOutOf(partAWritten.getRight());
    var partAWrittenUnderstanding = getResultSummaryForQuestionsWithConceptsCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "WRA");
    assessmentLTP12.setPartAWrittenResponseUnderstandingScore(partAWrittenUnderstanding.getLeft().replace(".", ","));
    assessmentLTP12.setPartAWrittenResponseUnderstandingOutOf(partAWrittenUnderstanding.getRight());
    var partBSelectedResponse = getResultSummaryForQuestionsWithAssessmentSectionStartsWith(assessmentStudentID, questions, studentAnswers, MUL_CHOICE.getCode(), "B");
    assessmentLTP12.setPartBSelectedResponseScore(partBSelectedResponse.getLeft().replace(".", ","));
    assessmentLTP12.setPartBSelectedResponseOutOf(partBSelectedResponse.getRight());
    var partBWrittenResponse = getResultSummaryForQuestionsWithConceptsCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "WRB");
    assessmentLTP12.setPartBWrittenResponseUnderstandingScore(partBWrittenResponse.getLeft().replace(".", ","));
    assessmentLTP12.setPartBWrittenResponseUnderstandingOutOf(partBWrittenResponse.getRight());
    var partCOralResponsePart1Response = getResultSummaryForQuestionsWithConceptsCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "O1");
    assessmentLTP12.setPartCOralResponsePart1Score(partCOralResponsePart1Response.getLeft().replace(".", ","));
    assessmentLTP12.setPartCOralResponsePart1OutOf(partCOralResponsePart1Response.getRight());
    var partCOralResponsePart2Response = getResultSummaryForQuestionsWithConceptsCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "O2");
    assessmentLTP12.setPartCOralResponsePart2Score(partCOralResponsePart2Response.getLeft().replace(".", ","));
    assessmentLTP12.setPartCOralResponsePart2OutOf(partCOralResponsePart2Response.getRight());
    var partCOralResponsePart3ScoreResponse = getResultSummaryForQuestionsWithConceptsCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "O3");
    assessmentLTP12.setPartCOralResponsePart3Score(partCOralResponsePart3ScoreResponse.getLeft().replace(".", ","));
    assessmentLTP12.setPartCOralResponsePart3OutOf(partCOralResponsePart3ScoreResponse.getRight());
    
    return assessmentLTP12;
  }

  private LTP10Assessment populateLTP10Assessment(ISRAssessmentSummary assessmentSummary,List<AssessmentQuestionEntity> questions, List<AssessmentStudentAnswerEntity> studentAnswers, UUID assessmentStudentID){
    LTP10Assessment assessmentLTP10 = new LTP10Assessment();

    assessmentLTP10.setSession(assessmentSummary.getSession());
    assessmentLTP10.setScore(assessmentSummary.getScore());
    assessmentLTP10.setAssessmentCode(assessmentSummary.getAssessmentCode());

    var comprehend = getResultSummaryForQuestionsWithClaimCode(assessmentStudentID, questions, studentAnswers, MUL_CHOICE.getCode(), "C");
    assessmentLTP10.setComprehendScore(comprehend.getLeft().replace(".", ","));
    assessmentLTP10.setComprehendOutOf(comprehend.getRight());
    var communicate = getResultSummaryForQuestionsWithClaimCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "W");
    assessmentLTP10.setCommunicateScore(communicate.getLeft().replace(".", ","));
    assessmentLTP10.setCommunicateOutOf(communicate.getRight());
    var communicateOral = getResultSummaryForQuestionsWithClaimCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "O");
    assessmentLTP10.setCommunicateOralScore(communicateOral.getLeft().replace(".", ","));
    assessmentLTP10.setCommunicateOralOutOf(communicateOral.getRight());
    var partASelectedResponse = getResultSummaryForQuestionsWithAssessmentSectionStartsWith(assessmentStudentID, questions, studentAnswers, MUL_CHOICE.getCode(), "A");
    assessmentLTP10.setPartASelectedResponseScore(partASelectedResponse.getLeft().replace(".", ","));
    assessmentLTP10.setPartASelectedResponseOutOf(partASelectedResponse.getRight());
    var partAWrittenShort = getResultSummaryForQuestionsWithConceptsCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "WRS");
    assessmentLTP10.setPartAWrittenShortScore(partAWrittenShort.getLeft().replace(".", ","));
    assessmentLTP10.setPartAWrittenShortOutOf(partAWrittenShort.getRight());
    var partAWrittenGraphic = getResultSummaryForQuestionsWithConceptsCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "GO");
    assessmentLTP10.setPartAWrittenGraphicScore(partAWrittenGraphic.getLeft().replace(".", ","));
    assessmentLTP10.setPartAWrittenGraphicOutOf(partAWrittenGraphic.getRight());
    var partAWrittenLong = getResultSummaryForQuestionsWithConceptsCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "WRA");
    assessmentLTP10.setPartAWrittenLongScore(partAWrittenLong.getLeft().replace(".", ","));
    assessmentLTP10.setPartAWrittenLongOutOf(partAWrittenLong.getRight());
    var partBSelectedResponse = getResultSummaryForQuestionsWithAssessmentSectionStartsWith(assessmentStudentID, questions, studentAnswers, MUL_CHOICE.getCode(), "B");
    assessmentLTP10.setPartBSelectedResponseScore(partBSelectedResponse.getLeft().replace(".", ","));
    assessmentLTP10.setPartBSelectedResponseOutOf(partBSelectedResponse.getRight());
    var partBWrittenShort = getResultSummaryForQuestionsWithConceptsCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "WRB");
    assessmentLTP10.setPartBWrittenShortScore(partBWrittenShort.getLeft().replace(".", ","));
    assessmentLTP10.setPartBWrittenShortOutOf(partBWrittenShort.getRight());
    var partOralPart1 = getResultSummaryForQuestionsWithConceptsCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "O1");
    assessmentLTP10.setPartOralPart1Score(partOralPart1.getLeft().replace(".", ","));
    assessmentLTP10.setPartOralPart1OutOf(partOralPart1.getRight());
    var partOralPart2 = getResultSummaryForQuestionsWithConceptsCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "O2");
    assessmentLTP10.setPartOralPart2Score(partOralPart2.getLeft().replace(".", ","));
    assessmentLTP10.setPartOralPart2OutOf(partOralPart2.getRight());
    var partOralPart3 = getResultSummaryForQuestionsWithConceptsCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "O3");
    assessmentLTP10.setPartOralPart3Score(partOralPart3.getLeft().replace(".", ","));
    assessmentLTP10.setPartOralPart3OutOf(partOralPart3.getRight());

    return assessmentLTP10;
  }

  private LTF12Assessment populateLTF12Assessment(ISRAssessmentSummary assessmentSummary,List<AssessmentQuestionEntity> questions, List<AssessmentStudentAnswerEntity> studentAnswers, UUID assessmentStudentID){
    LTF12Assessment assessmentLTF12 = new LTF12Assessment();
    
    assessmentLTF12.setSession(assessmentSummary.getSession());
    assessmentLTF12.setScore(assessmentSummary.getScore());
    assessmentLTF12.setAssessmentCode(assessmentSummary.getAssessmentCode());

    var comprehend = getResultSummaryForQuestionsWithClaimCode(assessmentStudentID, questions, studentAnswers, MUL_CHOICE.getCode(), "C");
    assessmentLTF12.setComprehendScore(comprehend.getLeft().replace(".", ","));
    assessmentLTF12.setComprehendOutOf(comprehend.getRight());
    var communicate = getResultSummaryForQuestionsWithClaimCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "W");
    assessmentLTF12.setCommunicateScore(communicate.getLeft().replace(".", ","));
    assessmentLTF12.setCommunicateOutOf(communicate.getRight());
    var communicateOral = getResultSummaryForQuestionsWithClaimCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "O");
    assessmentLTF12.setCommunicateOralScore(communicateOral.getLeft().replace(".", ","));
    assessmentLTF12.setCommunicateOralOutOf(communicateOral.getRight());
    var partASelectedResponse = getResultSummaryForQuestionsWithAssessmentSectionStartsWith(assessmentStudentID, questions, studentAnswers, MUL_CHOICE.getCode(), "A");
    assessmentLTF12.setPartASelectedResponseScore(partASelectedResponse.getLeft().replace(".", ","));
    assessmentLTF12.setPartASelectedResponseOutOf(partASelectedResponse.getRight());
    var partBSelectedResponse = getResultSummaryForQuestionsWithAssessmentSectionStartsWith(assessmentStudentID, questions, studentAnswers, MUL_CHOICE.getCode(), "B");
    assessmentLTF12.setPartBSelectedResponseScore(partBSelectedResponse.getLeft().replace(".", ","));
    assessmentLTF12.setPartBSelectedResponseOutOf(partBSelectedResponse.getRight());
    var partBWrittenAnalyzeResponse = getResultSummaryForQuestionsWithConceptsCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "WRS");
    assessmentLTF12.setPartBWrittenResponseAnalyzeScore(partBWrittenAnalyzeResponse.getLeft().replace(".", ","));
    assessmentLTF12.setPartBWrittenResponseAnalyzeOutOf(partBWrittenAnalyzeResponse.getRight());

    var partBWrittenResponseDissertationFoundationResponse = getResultSummaryForQuestionsWithConceptsCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "WRD");
    assessmentLTF12.setPartBWrittenResponseDissertationFoundationScore(partBWrittenResponseDissertationFoundationResponse.getLeft().replace(".", ","));
    assessmentLTF12.setPartBWrittenResponseDissertationFoundationOutOf(partBWrittenResponseDissertationFoundationResponse.getRight());

    var partBWrittenResponseDissertationFormResponse = getResultSummaryForQuestionsWithConceptsCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "WRF");
    assessmentLTF12.setPartBWrittenResponseDissertationFormScore(partBWrittenResponseDissertationFormResponse.getLeft().replace(".", ","));
    assessmentLTF12.setPartBWrittenResponseDissertationFormOutOf(partBWrittenResponseDissertationFormResponse.getRight());

    var multiChoiceQuestions = getMultiChoiceQuestions(questions);
    log.debug("Multiple choice questions found for student: {}", multiChoiceQuestions);
    var choicePath = getChoicePath(multiChoiceQuestions, assessmentStudentID);
    log.debug("Choice path found for student: {}", choicePath);
    
    var partBChoicePathResponse = getPartBChoicePath(choicePath);
    assessmentLTF12.setPartBChoicePath(partBChoicePathResponse);

    var partCOralResponsePart1FoundationResponse = getResultSummaryForQuestionsWithConceptsCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "O1D");
    assessmentLTF12.setPartCOralResponsePart1FoundationScore(partCOralResponsePart1FoundationResponse.getLeft().replace(".", ","));
    assessmentLTF12.setPartCOralResponsePart1FoundationOutOf(partCOralResponsePart1FoundationResponse.getRight());

    var partCOralResponsePart1FormResponse = getResultSummaryForQuestionsWithConceptsCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "O1F");
    assessmentLTF12.setPartCOralResponsePart1FormScore(partCOralResponsePart1FormResponse.getLeft().replace(".", ","));
    assessmentLTF12.setPartCOralResponsePart1FormOutOf(partCOralResponsePart1FormResponse.getRight());

    var partCOralResponsePart1OralResponse = getResultSummaryForQuestionsWithConceptsCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "O1E");
    assessmentLTF12.setPartCOralResponsePart1OralScore(partCOralResponsePart1OralResponse.getLeft().replace(".", ","));
    assessmentLTF12.setPartCOralResponsePart1OralOutOf(partCOralResponsePart1OralResponse.getRight());

    var partCOralResponsePart2DiscourseFoundationResponse = getResultSummaryForQuestionsWithConceptsCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "O2D");
    assessmentLTF12.setPartCOralResponsePart2DiscourseFoundationScore(partCOralResponsePart2DiscourseFoundationResponse.getLeft().replace(".", ","));
    assessmentLTF12.setPartCOralResponsePart2DiscourseFoundationOutOf(partCOralResponsePart2DiscourseFoundationResponse.getRight());

    var partCOralResponsePart2DiscourseFormResponse = getResultSummaryForQuestionsWithConceptsCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "O2F");
    assessmentLTF12.setPartCOralResponsePart2DiscourseFormScore(partCOralResponsePart2DiscourseFormResponse.getLeft().replace(".", ","));
    assessmentLTF12.setPartCOralResponsePart2DiscourseFormOutOf(partCOralResponsePart2DiscourseFormResponse.getRight());

    var partCOralResponsePart2DiscourseOralResponse = getResultSummaryForQuestionsWithConceptsCode(assessmentStudentID, questions, studentAnswers, OPEN_ENDED.getCode(), "O2E");
    assessmentLTF12.setPartCOralResponsePart2DiscourseOralScore(partCOralResponsePart2DiscourseOralResponse.getLeft().replace(".", ","));
    assessmentLTF12.setPartCOralResponsePart2DiscourseOralOutOf(partCOralResponsePart2DiscourseOralResponse.getRight());
    
    return assessmentLTF12;
  }

  private String getPartBChoicePath(String choicePath){
    if(!StringUtils.isEmpty(choicePath)){
      if(choicePath.equalsIgnoreCase("E")){
        return "le monde de l'expression";
      }
      return "le monde de l'information";
    }
    return "";
  }
  
  private String getProficiencyScore(Integer proficiencyScore, LanguageCode language){
    if(proficiencyScore == null){
      return "";
    }
    if(language.equals(LanguageCode.ENGLISH)){
      if(proficiencyScore == 1){
        return "1 - Emerging";
      }else if(proficiencyScore == 2){
        return "2 - Developing";
      }else if(proficiencyScore == 3){
        return "3 - Proficient";
      }else if(proficiencyScore == 4){
        return "4 - Extending";
      }
    }else{
      if(proficiencyScore == 1){
        return "1 - Émergente";
      }else if(proficiencyScore == 2){
        return "2 - En voie d’acquisition";
      }else if(proficiencyScore == 3){
        return "3 - Acquise";
      }else if(proficiencyScore == 4){
        return "4 - Approfondie";
      }    
    }
    return "";
  }
}
