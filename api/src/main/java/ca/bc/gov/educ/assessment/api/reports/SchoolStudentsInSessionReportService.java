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
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.UUID;

/**
 * Service class for generating School Students in Session Report
 */
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
      // Set temp directory for JasperReports compilation
      System.setProperty("jasper.reports.compile.temp", System.getProperty("java.io.tmpdir"));
      
      // Disable Spring Boot nested JAR provider during compilation
      String originalProvider = System.getProperty("java.nio.file.spi.DefaultFileSystemProvider");
      System.setProperty("java.nio.file.spi.DefaultFileSystemProvider", "");
      
      // Isolate from Spring Boot loader by setting context classloader
      ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
      try {
        // Use the system classloader instead of Spring Boot's nested loader
        Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
        
        // Work around Spring Boot nested JAR issues by copying JRXML to temp file
        InputStream inputStream = getClass().getResourceAsStream("/reports/schoolStudentsInSession.jrxml");
        if (inputStream == null) {
          throw new StudentAssessmentAPIRuntimeException("Could not find JRXML file: /reports/schoolStudentsInSession.jrxml");
        }
        
        // Copy to temporary file to avoid Spring Boot nested JAR issues
        Path tempJrxml = Files.createTempFile("schoolStudentsInSession", ".jrxml");
        Files.copy(inputStream, tempJrxml, StandardCopyOption.REPLACE_EXISTING);
        
        log.info("Compiling Jasper reports from temp file: " + tempJrxml);
        schoolStudentInSessionReport = JasperCompileManager.compileReport(tempJrxml.toString());
        
        // Clean up temp file
        Files.deleteIfExists(tempJrxml);
        
        log.info("Jasper report compiled successfully: " + schoolStudentInSessionReport);
      } finally {
        // Restore original classloader and system properties
        Thread.currentThread().setContextClassLoader(originalClassLoader);
        if (originalProvider != null) {
          System.setProperty("java.nio.file.spi.DefaultFileSystemProvider", originalProvider);
        } else {
          System.clearProperty("java.nio.file.spi.DefaultFileSystemProvider");
        }
      }
    } catch (Exception e) {
      log.error("JasperReports compilation failed: " + e.getMessage(), e);
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
