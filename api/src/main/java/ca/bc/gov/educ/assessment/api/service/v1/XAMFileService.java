package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.DownloadableReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class XAMFileService {
    private final AssessmentStudentRepository assessmentStudentRepository;
    private AssessmentSessionRepository assessmentSessionRepository;
    private RestUtils restUtils;
    private final S3Client s3Client;
    private final ApplicationProperties applicationProperties;

    @Autowired
    public XAMFileService(AssessmentStudentRepository assessmentStudentRepository, AssessmentSessionRepository assessmentSessionRepository, RestUtils restUtils, S3Client s3Client, ApplicationProperties applicationProperties) {
        this.assessmentStudentRepository = assessmentStudentRepository;
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.restUtils = restUtils;
        this.s3Client = s3Client;
        this.applicationProperties = applicationProperties;
    }

    /**
     * Generic method to generate XAM content and return either a File or DownloadableReportResponse
     * @param sessionID the session ID
     * @param school the school tombstone
     * @param asFile whether to return a File (true) or DownloadableReportResponse (false)
     * @return T - either File or DownloadableReportResponse
     */
    @SuppressWarnings("unchecked")
    private <T> T generateXamContent(UUID sessionID, SchoolTombstone school, boolean asFile) {
        var assessmentSession = assessmentSessionRepository.findById(sessionID).orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class));
        List<AssessmentStudentEntity> students = assessmentStudentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolID(sessionID, UUID.fromString(school.getSchoolId()));

        StringBuilder sb = new StringBuilder();

        for (AssessmentStudentEntity student : students) {
            var examSchool = restUtils.getSchoolBySchoolID(String.valueOf(student.getAssessmentCenterSchoolID()));
            var examMincode = examSchool.map(SchoolTombstone::getMincode).orElse("");
            String record =
                padRight("E07", 3) + // TX_ID
                padRight(school.getVendorSourceSystemCode(), 1) + // VENDOR_ID
                padRight("", 1) + // VERI_FLAG (BLANK)
                padRight("", 5) + // FILLER1 (BLANK)
                padRight(school.getMincode(), 8) + // MINCODE
                padRight(student.getLocalID(), 12) + // STUD_LOCAL_ID
                padRight(student.getPen(), 10) + // STUD_NO (PEN)
                padRight(student.getAssessmentEntity() != null ? student.getAssessmentEntity().getAssessmentTypeCode() : "", 5) + // CRSE_CODE
                padRight("", 3) + // CRSE_LEVEL (BLANK)
                padRight(student.getAssessmentEntity() != null && student.getAssessmentEntity().getAssessmentSessionEntity() != null ? student.getAssessmentEntity().getAssessmentSessionEntity().getCourseYear() : "", 4) + // CRSE_YEAR
                padRight(student.getAssessmentEntity() != null && student.getAssessmentEntity().getAssessmentSessionEntity() != null ? student.getAssessmentEntity().getAssessmentSessionEntity().getCourseMonth() : "", 2) + // CRSE_MONTH
                padRight("", 2) + // INTERIM_LETTER_GRADE (BLANK)
                padRight("", 3) + // INTERIM_SCHOOL_PERCENT (BLANK)
                padRight("", 3) + // FINAL_SCHOOL_PERCENT (BLANK)
                padRight("", 3) + // EXAM_PERCENT (BLANK)
                (student.getProficiencyScore() == null 
                    ? padRight("", 3) 
                    : String.format("%03d", student.getProficiencyScore())) + // FINAL_PERCENT - formatted as 001-004
                padRight("", 2) + // FINAL_LETTER_GRADE (BLANK)
                padRight("Y", 1) + // E-EXAM FLAG - Always Y according to spec
                padRight(student.getProvincialSpecialCaseCode(), 1) + // PROV_SPEC_CASE
                padRight(student.getLocalAssessmentID(), 20) + // LOCAL_CRSE_ID
                padRight("A", 1) + // CRSE_STATUS - Always A according to spec
                padRight(student.getSurname(), 25) + // STUD_SURNAME
                padRight("", 2) + // NUM_CREDITS (BLANK)
                padRight("", 1) + // CRSE_TYPE (BLANK)
                padRight("", 1) + // TO_WRITE_FLAG (BLANK)
                padRight(examMincode, 8) + // EXAM_MINCODE
                "\n";
            sb.append(record);
        }
        String fileName = school.getMincode() + "-" + assessmentSession.getCourseYear() + assessmentSession.getCourseMonth() + "-Results" + ".xam";
        String content = sb.toString();

        if (asFile) {
            File file = new File("./" + fileName);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(content);
                return (T) file;
            } catch (Exception e) {
                log.error("Failed to write XAM file", e);
                throw new RuntimeException("Failed to write XAM file", e);
            }
        } else {
            DownloadableReportResponse response = new DownloadableReportResponse();
            response.setReportType(fileName);
            response.setDocumentData(Base64.getEncoder().encodeToString(content.getBytes()));
            return (T) response;
        }
    }

    /**
     * Returns a File for the XAM content (used by orchestrator)
     */
    public File generateXamFile(UUID sessionID, SchoolTombstone school) {
        return generateXamContent(sessionID, school, true);
    }

    /**
     * Returns a DownloadableReportResponse for the XAM content (used by controller)
     */
    public DownloadableReportResponse generateXamReport(UUID sessionID, UUID schoolID) {
        var schoolTombstone = this.restUtils.getSchoolBySchoolID(schoolID.toString()).orElseThrow(() -> new EntityNotFoundException(SchoolTombstone.class));
        return generateXamContent(sessionID, schoolTombstone, false);
    }

    public void uploadToS3(File file, String key) {
        try {
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(applicationProperties.getS3BucketName())
                    .key(key)
                    .build(), RequestBody.fromFile(file));
            log.debug("Successfully uploaded file to BCBox S3: {} (size: {} bytes)", key, file.length());
        } catch (Exception e) {
            log.error("Failed to upload file to BCBox S3: {}", key, e);
            throw new RuntimeException("Failed to upload file to BCBox S3", e);
        }
    }

    /**
     * for orchestration:
     * Generates a XAM file for the given school and session, returns the file path.
     */
    public String generateXamFileAndReturnPath(UUID sessionID, SchoolTombstone school) {
        File file = generateXamFile(sessionID, school);
        return file.getAbsolutePath();
    }

    /**
     * for orchestration:
     * Uploads the file at the given path to S3 for the given school and session.
     */
    public void uploadFilePathToS3(String filePath, UUID sessionID, SchoolTombstone school) {
        File file = new File(filePath);
        String key = "xam-files/" + school.getMincode() + "_" + sessionID + ".xam";
        uploadToS3(file, key);
    }

    /**
     * for orchestration:
     * Generates and uploads XAM files for all schools for the given session.
     * Only schools with vendor source system code MYED
     */
    public void generateAndUploadXamFiles(UUID sessionID) {
        List<SchoolTombstone> schools = restUtils.getAllSchoolTombstones();
        List<SchoolTombstone> myEdSchools = schools.stream()
                .filter(school -> "MYED".equalsIgnoreCase(school.getVendorSourceSystemCode()))
                .toList();
        log.debug("Starting generation and upload of XAM files for {} MYED schools, session {}", myEdSchools.size(), sessionID);
        for (SchoolTombstone school : myEdSchools) {
            String filePath = null;
            try {
                log.debug("Generating XAM file for school: {}", school.getMincode());
                filePath = generateXamFileAndReturnPath(sessionID, school);
                log.debug("Uploading XAM file for school: {}", school.getMincode());
                uploadFilePathToS3(filePath, sessionID, school);
                log.debug("Successfully uploaded XAM file for school: {}", school.getMincode());
            } catch (Exception e) {
                log.error("Failed to process XAM file for school: {}", school.getMincode(), e);
            } finally {
                if (filePath != null) {
                    deleteFile(filePath, school.getMincode());
                }
            }
        }
        log.debug("Completed processing XAM files for session {}", sessionID);
    }

    /**
     * Delete a file and log the result
     */
    private void deleteFile(String filePath, String schoolMincode) {
        try {
            File file = new File(filePath);
            if (file.exists() && file.delete()) {
                log.debug("Successfully deleted temporary XAM file for school {}: {}", schoolMincode, filePath);
            } else if (file.exists()) {
                log.warn("Failed to delete temporary XAM file for school {}: {}", schoolMincode, filePath);
            }
        } catch (Exception e) {
            log.error("Error deleting temporary XAM file for school {}: {}", schoolMincode, filePath, e);
        }
    }

    private String padRight(String value, int length) {
        String str = value == null ? "" : value;
        if (str.length() > length) {
            return str.substring(0, length);
        }
        return String.format("%-" + length + "s", str);
    }
}
