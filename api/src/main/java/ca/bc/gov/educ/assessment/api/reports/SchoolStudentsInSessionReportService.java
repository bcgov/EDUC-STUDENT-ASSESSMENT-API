package ca.bc.gov.educ.assessment.api.reports;

import ca.bc.gov.educ.assessment.api.constants.v1.reports.AssessmentReportTypeCode;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.service.v1.CodeTableService;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.DownloadableReportResponse;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.schoolStudent.SchoolStudentGradAssessmentNode;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.schoolStudent.SchoolStudentNode;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.schoolStudent.SchoolStudentReportNode;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.schoolStudent.SchoolStudentRootNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.UUID;


@Service
@Slf4j
public class SchoolStudentsInSessionReportService extends BaseReportGenerationService{

  private final AssessmentSessionRepository assessmentSessionRepository;
  private final AssessmentStudentRepository assessmentStudentRepository;
  private final RestUtils restUtils;
  private final CodeTableService codeTableService;
  private JasperReport schoolStudentInSessionReport;

  public SchoolStudentsInSessionReportService(AssessmentSessionRepository assessmentSessionRepository, AssessmentStudentRepository assessmentStudentRepository, RestUtils restUtils, CodeTableService codeTableService) {
      super(restUtils);
      this.assessmentSessionRepository = assessmentSessionRepository;
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
      System.setProperty("net.sf.jasperreports.compiler.temp.dir", System.getProperty("user.home") + "/jasper-temp");

      File tempDir = new File(System.getProperty("user.home") + "/jasper-temp");
      if (!tempDir.exists()) {
        tempDir.mkdirs();
      }
      InputStream inputHeadcount = getClass().getResourceAsStream("/reports/schoolStudentsInSession.jrxml");
      log.info("Compiling Jasper reports");
      schoolStudentInSessionReport = JasperCompileManager.compileReport(inputHeadcount);
      log.info("Jasper report compiled " + schoolStudentInSessionReport);
    } catch (JRException e) {
      throw new StudentAssessmentAPIRuntimeException("Compiling Jasper reports has failed :: " + e.getMessage());
    }
  }

  public DownloadableReportResponse generateSchoolStudentsInSessionReport(UUID assessmentSessionID, UUID schoolID){
    try {
      var assessmentTypes = codeTableService.getAllAssessmentTypeCodesAsMap();
      var students = assessmentStudentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolID(assessmentSessionID, schoolID);
      var session = assessmentSessionRepository.findById(assessmentSessionID).orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class, "sessionID", assessmentSessionID.toString()));

      SchoolStudentRootNode schoolStudentRootNode = new SchoolStudentRootNode();
      SchoolStudentReportNode schoolStudentReportNode = new SchoolStudentReportNode();
      schoolStudentReportNode.setStudents(new ArrayList<>());
      schoolStudentRootNode.setReport(schoolStudentReportNode);
      setReportTombstoneValues(schoolID, session, schoolStudentReportNode);
      
      var studentList = new HashMap<UUID, SchoolStudentNode>();
      
      students.forEach(student -> {
        if (!studentList.containsKey(student.getStudentID())) {
          var studentNode = new SchoolStudentNode();
          studentNode.setPen(student.getPen());
          studentNode.setLocalID(student.getGivenName());
          studentNode.setName(student.getSurname() + ", " + student.getGivenName());
          SchoolStudentGradAssessmentNode studentGradAssessmentNode = new SchoolStudentGradAssessmentNode();
          studentGradAssessmentNode.setName(assessmentTypes.get(student.getAssessmentEntity().getAssessmentTypeCode()));
          studentGradAssessmentNode.setScore(student.getProficiencyScore() != null ? student.getProficiencyScore().toString() : null);
          studentList.put(student.getStudentID(), studentNode);
        }else{
          var loadedStudent = studentList.get(student.getStudentID());
          SchoolStudentGradAssessmentNode studentGradAssessmentNode = new SchoolStudentGradAssessmentNode();
          studentGradAssessmentNode.setName(assessmentTypes.get(student.getAssessmentEntity().getAssessmentTypeCode()));
          studentGradAssessmentNode.setScore(student.getProficiencyScore() != null ? student.getProficiencyScore().toString() : null);
          loadedStudent.getGradAssessments().add(studentGradAssessmentNode);
        }
      });
      
      schoolStudentReportNode.setStudents(schoolStudentReportNode.getStudents().stream().sorted(Comparator.comparing(SchoolStudentNode::getName)).toList());
      
      return generateJasperReport(objectWriter.writeValueAsString(schoolStudentRootNode), schoolStudentInSessionReport, AssessmentReportTypeCode.SCHOOL_STUDENTS_IN_SESSION.getCode());
    } catch (JsonProcessingException e) {
      log.error("Exception occurred while writing PDF report for ell programs :: " + e.getMessage());
      throw new StudentAssessmentAPIRuntimeException("Exception occurred while writing PDF report for ell programs :: " + e.getMessage());
    }
  }
  
  
}
