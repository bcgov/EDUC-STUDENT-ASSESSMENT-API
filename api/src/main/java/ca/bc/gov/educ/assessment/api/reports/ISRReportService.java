package ca.bc.gov.educ.assessment.api.reports;

import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.ProvincialSpecialCaseCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.reports.AssessmentReportTypeCode;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentQuestionEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentAnswerEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentQuestionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentAnswerRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.service.v1.CodeTableService;
import ca.bc.gov.educ.assessment.api.struct.external.grad.v1.GradStudentRecord;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.assessment.api.struct.external.studentapi.v1.Student;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentTypeCode;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.DownloadableReportResponse;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.isr.*;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.student.byAssessment.SchoolStudentNode;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.student.byAssessment.SchoolStudentReportNode;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.student.byAssessment.SchoolStudentRootNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.util.Pair;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;

import static ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes.*;
import static ca.bc.gov.educ.assessment.api.constants.v1.ComponentTypeCodes.MUL_CHOICE;
import static ca.bc.gov.educ.assessment.api.constants.v1.ComponentTypeCodes.OPEN_ENDED;

/**
 * Service class for generating School Students by Assessment Report
 */
@Service
@Slf4j
public class ISRReportService extends BaseReportGenerationService {
  
  private final AssessmentQuestionRepository assessmentQuestionRepository;
  private final AssessmentStudentAnswerRepository assessmentStudentAnswerRepository;
  private final AssessmentStudentRepository assessmentStudentRepository;
  private final RestUtils restUtils;
  private final CodeTableService codeTableService;
  private JasperReport schoolStudentByAssessmentReport;

  public ISRReportService(AssessmentQuestionRepository assessmentQuestionRepository, AssessmentStudentAnswerRepository assessmentStudentAnswerRepository, AssessmentStudentRepository assessmentStudentRepository, RestUtils restUtils, CodeTableService codeTableService) {
    super(restUtils);
    this.assessmentQuestionRepository = assessmentQuestionRepository;
    this.assessmentStudentAnswerRepository = assessmentStudentAnswerRepository;
    this.assessmentStudentRepository = assessmentStudentRepository;
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
      InputStream subReport1 = getClass().getResourceAsStream("/reports/lte10summary.jrxml");
      JasperCompileManager.compileReport(subReport1);
      InputStream subReport2 = getClass().getResourceAsStream("/reports/lte12summary.jrxml");
      JasperCompileManager.compileReport(subReport2);
      InputStream subReport3 = getClass().getResourceAsStream("/reports/ltp10summary.jrxml");
      JasperCompileManager.compileReport(subReport3);
      InputStream subReport4 = getClass().getResourceAsStream("/reports/ltp12summary.jrxml");
      JasperCompileManager.compileReport(subReport4);
      InputStream subReport5 = getClass().getResourceAsStream("/reports/ltf12summary.jrxml");
      JasperCompileManager.compileReport(subReport5);
      InputStream subReport6 = getClass().getResourceAsStream("/reports/nme10summary.jrxml");
      JasperCompileManager.compileReport(subReport6);
      InputStream subReport7 = getClass().getResourceAsStream("/reports/nmf10summary.jrxml");
      JasperCompileManager.compileReport(subReport7);
      InputStream report = getClass().getResourceAsStream("/reports/isrMain.jrxml");
      schoolStudentByAssessmentReport = JasperCompileManager.compileReport(report);
    } catch (JRException e) {
      throw new StudentAssessmentAPIRuntimeException("Compiling Jasper reports has failed :: " + e.getMessage());
    }
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
      setReportTombstoneValues(UUID.fromString(gradStudentRecord.getSchoolOfRecordId()), reportNode, student.getPen(), student.getLegalFirstName() + " " + student.getLegalLastName());
      isrRootNode.setReport(reportNode);
      reportNode.setAssessments(new ArrayList<>());
      reportNode.setAssessmentDetails(new ArrayList<>());
      var assessmentTypes = codeTableService.getAllAssessmentTypeCodesAsMap();

      studentAssessments.forEach((assessmentStudent) -> {
        ISRAssessmentSummary assessmentSummary = new ISRAssessmentSummary();
        assessmentSummary.setSession(assessmentStudent.getAssessmentEntity().getAssessmentSessionEntity().getCourseYear() + "/" + assessmentStudent.getAssessmentEntity().getAssessmentSessionEntity().getCourseMonth());
        assessmentSummary.setScore(assessmentStudent.getProficiencyScore() == null ? StringUtils.isNotBlank(assessmentStudent.getProvincialSpecialCaseCode()) ? ProvincialSpecialCaseCodes.findByValue(assessmentStudent.getProvincialSpecialCaseCode()).get().getDescription() : "" : assessmentStudent.getProficiencyScore().toString());
        assessmentSummary.setAssessment(assessmentTypes.get(assessmentStudent.getAssessmentEntity().getAssessmentTypeCode()));
        reportNode.getAssessments().add(assessmentSummary);
        
        var questions = assessmentQuestionRepository.findByAssessmentComponentEntity_AssessmentFormEntity_AssessmentFormID(assessmentStudent.getAssessmentFormID());
        var studentAnswers = assessmentStudentAnswerRepository.findAllByAssessmentStudentComponentEntity_AssessmentStudentEntity_AssessmentStudentID(assessmentStudent.getAssessmentStudentID());
        
        switch(assessmentStudent.getAssessmentEntity().getAssessmentTypeCode()){
          case "LTE10":
            reportNode.getAssessmentDetails().add(populateLTE10Assessment(assessmentSummary, questions, studentAnswers));
            break;
          case "LTE12":
            LTE12Assessment assessmentLTE12 = new LTE12Assessment();
            assessmentLTE12.setSession(assessmentSummary.getSession());
            assessmentLTE12.setScore(assessmentSummary.getScore());
            assessmentLTE12.setAssessmentCode(assessmentSummary.getAssessment());
            reportNode.getAssessmentDetails().add(assessmentLTE12);
            break;
          case "LTF12":
            LTF12Assessment assessmentLTF12 = new LTF12Assessment();
            assessmentLTF12.setSession(assessmentSummary.getSession());
            assessmentLTF12.setScore(assessmentSummary.getScore());
            assessmentLTF12.setAssessmentCode(assessmentSummary.getAssessment());
            reportNode.getAssessmentDetails().add(assessmentLTF12);
            break;
          case "LTP10":
            LTP10Assessment assessmentLTP10 = new LTP10Assessment();
            assessmentLTP10.setSession(assessmentSummary.getSession());
            assessmentLTP10.setScore(assessmentSummary.getScore());
            assessmentLTP10.setAssessmentCode(assessmentSummary.getAssessment());
            reportNode.getAssessmentDetails().add(assessmentLTP10);
            break;
          case "LTP12":
            LTP12Assessment assessmentLTP12 = new LTP12Assessment();
            assessmentLTP12.setSession(assessmentSummary.getSession());
            assessmentLTP12.setScore(assessmentSummary.getScore());
            assessmentLTP12.setAssessmentCode(assessmentSummary.getAssessment());
            reportNode.getAssessmentDetails().add(assessmentLTP12);
            break;
          case "NME":
          case "NME10":
            NME10Assessment assessmentNME10 = new NME10Assessment();
            assessmentNME10.setSession(assessmentSummary.getSession());
            assessmentNME10.setScore(assessmentSummary.getScore());
            assessmentNME10.setAssessmentCode(assessmentSummary.getAssessment());
            reportNode.getAssessmentDetails().add(assessmentNME10);
            break;
          case "NMF":
          case "NMF10":
            NMF10Assessment assessmentNMF10 = new NMF10Assessment();
            assessmentNMF10.setSession(assessmentSummary.getSession());
            assessmentNMF10.setScore(assessmentSummary.getScore());
            assessmentNMF10.setAssessmentCode(assessmentSummary.getAssessment());
            reportNode.getAssessmentDetails().add(assessmentNMF10);
            break;
        }
        
      });
      
      return generateJasperReport(objectWriter.writeValueAsString(isrRootNode), schoolStudentByAssessmentReport, AssessmentReportTypeCode.SCHOOL_STUDENTS_BY_ASSESSMENT.getCode());
    } catch (JsonProcessingException e) {
      log.error("Exception occurred while writing PDF report for ell programs :: " + e.getMessage());
      throw new StudentAssessmentAPIRuntimeException("Exception occurred while writing PDF report for ell programs :: " + e.getMessage());
    }
  }
  
  private Pair<String, String> getResultSummaryForQuestionsWithTaskCode(List<AssessmentQuestionEntity> questions, List<AssessmentStudentAnswerEntity> answers, String questionType, String taskCode){
    var filteredQuestions = questions.stream().filter(assessmentQuestionEntity -> assessmentQuestionEntity.getTaskCode().equals(taskCode) && assessmentQuestionEntity.getAssessmentComponentEntity().getComponentTypeCode().equalsIgnoreCase(questionType)).toList();
    return Pair.of(getTotalScore(filteredQuestions, answers), getTotalOutOf(filteredQuestions));
  }

  private Pair<String, String> getResultSummaryForQuestionsWithClaimCode(List<AssessmentQuestionEntity> questions, List<AssessmentStudentAnswerEntity> answers, String questionType, String claimCode){
    var filteredQuestions = questions.stream().filter(assessmentQuestionEntity -> assessmentQuestionEntity.getClaimCode().equals(claimCode) && assessmentQuestionEntity.getAssessmentComponentEntity().getComponentTypeCode().equalsIgnoreCase(questionType)).toList();
    return Pair.of(getTotalScore(filteredQuestions, answers), getTotalOutOf(filteredQuestions));
  }

  private Pair<String, String> getResultSummaryForQuestionsWithConceptsCode(List<AssessmentQuestionEntity> questions, List<AssessmentStudentAnswerEntity> answers, String questionType, String conceptsCode){
    var filteredQuestions = questions.stream().filter(assessmentQuestionEntity -> assessmentQuestionEntity.getConceptCode().equals(conceptsCode) && assessmentQuestionEntity.getAssessmentComponentEntity().getComponentTypeCode().equalsIgnoreCase(questionType)).toList();
    return Pair.of(getTotalScore(filteredQuestions, answers), getTotalOutOf(filteredQuestions));
  }

  private Pair<String, String> getResultSummaryForQuestionsWithAssessmentSectionStartsWith(List<AssessmentQuestionEntity> questions, List<AssessmentStudentAnswerEntity> answers, String questionType, String assessmentSection){
    var filteredQuestions = questions.stream().filter(assessmentQuestionEntity -> assessmentQuestionEntity.getAssessmentSection().startsWith(assessmentSection) && assessmentQuestionEntity.getAssessmentComponentEntity().getComponentTypeCode().equalsIgnoreCase(questionType)).toList();
    return Pair.of(getTotalScore(filteredQuestions, answers), getTotalOutOf(filteredQuestions));
  }
  
  private String getTotalScore(List<AssessmentQuestionEntity> filteredQuestions, List<AssessmentStudentAnswerEntity> answers){
    int totalScore = 0;

    for(AssessmentStudentAnswerEntity answer : answers){
      if(answer.getScore() != null && filteredQuestions.stream().anyMatch(filteredQuestion -> filteredQuestion.getAssessmentQuestionID().equals(answer.getAssessmentQuestionID()))) {
        totalScore = totalScore + answer.getScore().intValue();
      }
    }
    return totalScore + "";
  }

  private String getTotalOutOf(List<AssessmentQuestionEntity> filteredQuestions){
    int totalOutOf = 0;

    for(AssessmentQuestionEntity question : filteredQuestions){
      totalOutOf = totalOutOf + question.getQuestionValue().intValue();
    }
    return totalOutOf + "";
  }

  protected void setReportTombstoneValues(UUID schoolID, ISRReportNode reportNode, String studentPEN, String studentName){
    var school = validateAndReturnSchool(schoolID);

    reportNode.setReportGeneratedDate("Report Generated: " + LocalDate.now().format(formatter));
    reportNode.setSchoolDetail(school.getMincode() + " - " + school.getDisplayName());
    reportNode.setStudentPEN(studentPEN);
    reportNode.setStudentName(studentName);
  }

  private LTE10Assessment populateLTE10Assessment(ISRAssessmentSummary assessmentSummary,List<AssessmentQuestionEntity> questions, List<AssessmentStudentAnswerEntity> studentAnswers ){
    LTE10Assessment assessmentLTE10 = new LTE10Assessment();
    assessmentLTE10.setSession(assessmentSummary.getSession());
    assessmentLTE10.setScore(assessmentSummary.getScore());
    assessmentLTE10.setAssessmentCode(assessmentSummary.getAssessment());

    var comprehend = getResultSummaryForQuestionsWithClaimCode(questions, studentAnswers, MUL_CHOICE.getCode(), "C");
    assessmentLTE10.setComprehendScore(comprehend.getLeft());
    assessmentLTE10.setComprehendOutOf(comprehend.getRight());
    var communicate = getResultSummaryForQuestionsWithClaimCode(questions, studentAnswers, MUL_CHOICE.getCode(), "W");
    assessmentLTE10.setCommunicateScore(communicate.getLeft());
    assessmentLTE10.setCommunicateOutOf(communicate.getRight());
    var partASelectedResponse = getResultSummaryForQuestionsWithAssessmentSectionStartsWith(questions, studentAnswers, MUL_CHOICE.getCode(), "A");
    assessmentLTE10.setPartASelectedResponseScore(partASelectedResponse.getLeft());
    assessmentLTE10.setPartASelectedResponseOutOf(partASelectedResponse.getRight());
    var partAWritten = getResultSummaryForQuestionsWithConceptsCode(questions, studentAnswers, OPEN_ENDED.getCode(), "GO");
    assessmentLTE10.setPartAWrittenResponseGraphicScore(partAWritten.getLeft());
    assessmentLTE10.setPartAWrittenResponseGraphicOutOf(partAWritten.getRight());
    var partAWrittenUnderstanding = getResultSummaryForQuestionsWithConceptsCode(questions, studentAnswers, OPEN_ENDED.getCode(), "WRA");
    assessmentLTE10.setPartAWrittenResponseUnderstandingScore(partAWrittenUnderstanding.getLeft());
    assessmentLTE10.setPartAWrittenResponseUnderstandingOutOf(partAWrittenUnderstanding.getRight());
    var partBSelectedResponse = getResultSummaryForQuestionsWithAssessmentSectionStartsWith(questions, studentAnswers, MUL_CHOICE.getCode(), "B");
    assessmentLTE10.setPartBSelectedResponseScore(partBSelectedResponse.getLeft());
    assessmentLTE10.setPartBSelectedResponseOutOf(partBSelectedResponse.getRight());
    var partBWrittenResponse = getResultSummaryForQuestionsWithConceptsCode(questions, studentAnswers, OPEN_ENDED.getCode(), "WRB");
    assessmentLTE10.setPartBWrittenResponseUnderstandingScore(partBWrittenResponse.getLeft());
    assessmentLTE10.setPartBWrittenResponseUnderstandingOutOf(partBWrittenResponse.getRight());
    return assessmentLTE10;
  }
}
