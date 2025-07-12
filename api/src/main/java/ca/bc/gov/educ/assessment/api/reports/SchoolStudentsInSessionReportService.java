package ca.bc.gov.educ.assessment.api.reports;

import ca.bc.gov.educ.assessment.api.constants.v1.reports.AssessmentReportTypeCode;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
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
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.UUID;


@Service
@Slf4j
public class SchoolStudentsInSessionReportService extends BaseReportGenerationService {

  private final AssessmentSessionRepository assessmentSessionRepository;
  private final AssessmentStudentRepository assessmentStudentRepository;
  private final RestUtils restUtils;
  private final CodeTableService codeTableService;
  private JasperReport schoolStudentInSessionReport;

  public SchoolStudentsInSessionReportService(AssessmentSessionRepository assessmentSessionRepository, 
                                               AssessmentStudentRepository assessmentStudentRepository, 
                                               RestUtils restUtils, 
                                               CodeTableService codeTableService) {
    super(restUtils);
    this.assessmentSessionRepository = assessmentSessionRepository;
    this.assessmentStudentRepository = assessmentStudentRepository;
    this.restUtils = restUtils;
    this.codeTableService = codeTableService;
  }

  @PostConstruct
  public void init() {
    this.loadPreCompiledReport();
  }

  private void loadPreCompiledReport() {
    try {
      System.setProperty("jasper.reports.compile.temp", System.getProperty("java.io.tmpdir"));
      // Load pre-compiled report from resources
      InputStream preCompiledStream = getClass().getResourceAsStream("/reports/schoolStudentsInSession.jasper");
      if (preCompiledStream == null) {
        throw new StudentAssessmentAPIRuntimeException("Could not find pre-compiled report: /reports/schoolStudentsInSession.jasper");
      }
      
      log.info("Loading pre-compiled Jasper report");
      schoolStudentInSessionReport = (JasperReport) JRLoader.loadObject(preCompiledStream);
      log.info("Pre-compiled Jasper report loaded successfully: " + schoolStudentInSessionReport);
    } catch (Exception e) {
      log.error("Failed to load pre-compiled report: " + e.getMessage(), e);
      throw new StudentAssessmentAPIRuntimeException("Failed to load pre-compiled report: " + e.getMessage());
    }
  }

  public DownloadableReportResponse generateSchoolStudentsInSessionReport(UUID assessmentSessionID, UUID schoolID) {
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
        } else {
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
      log.error("Exception occurred while writing PDF report for school students in session :: " + e.getMessage());
      throw new StudentAssessmentAPIRuntimeException("Exception occurred while writing PDF report for school students in session :: " + e.getMessage());
    }
  }
}
