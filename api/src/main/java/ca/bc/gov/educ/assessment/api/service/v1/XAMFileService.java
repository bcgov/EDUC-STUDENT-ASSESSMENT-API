package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.StagedAssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.StagedAssessmentStudentRepository;
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

import java.nio.charset.StandardCharsets;
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
    private StagedAssessmentStudentRepository stagedAssessmentStudentRepository;

    @Autowired
    public XAMFileService(AssessmentStudentRepository assessmentStudentRepository, AssessmentSessionRepository assessmentSessionRepository, RestUtils restUtils, S3Client s3Client, ApplicationProperties applicationProperties, StagedAssessmentStudentRepository stagedAssessmentStudentRepository) {
        this.assessmentStudentRepository = assessmentStudentRepository;
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.restUtils = restUtils;
        this.s3Client = s3Client;
        this.applicationProperties = applicationProperties;
        this.stagedAssessmentStudentRepository = stagedAssessmentStudentRepository;
    }

    /**
     * Generic method to generate XAM content and return either a File or DownloadableReportResponse
     * @param assessmentSessionEntity the assessment session ID
     * @param school the school tombstone
     * @param forSaga whether for approval saga (staging uploaded to s3) or frontend request (full table students)
     * @return T - either byte[] or DownloadableReportResponse
     */
    @SuppressWarnings("unchecked")
    protected <T> T generateXamContent(AssessmentSessionEntity assessmentSessionEntity, SchoolTombstone school, boolean forSaga) {
        StringBuilder sb;
        if (forSaga) {
            List<StagedAssessmentStudentEntity> stagedStudents = stagedAssessmentStudentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolIDAndStagedAssessmentStudentStatusIn(assessmentSessionEntity.getSessionID(), UUID.fromString(school.getSchoolId()), List.of("ACTIVE"));
            sb = generateRowsStagedAssessmentStudent(stagedStudents, school);
        } else {
            List<AssessmentStudentEntity> students = assessmentStudentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolIDAndStudentStatusCodeIn(assessmentSessionEntity.getSessionID(), UUID.fromString(school.getSchoolId()), List.of("ACTIVE"));
            sb = generateRowsAssessmentStudent(students, school);
        }

        String fileName = generateXamFileName(school, assessmentSessionEntity);
        String content = sb.toString();

        if (forSaga) {
            return (T) content.getBytes(StandardCharsets.UTF_8);
        } else {
            DownloadableReportResponse response = new DownloadableReportResponse();
            response.setReportType(fileName);
            response.setDocumentData(Base64.getEncoder().encodeToString(content.getBytes()));
            return (T) response;
        }
    }

    private StringBuilder generateRowsAssessmentStudent(List<AssessmentStudentEntity> assessmentStudents, SchoolTombstone school) {
        StringBuilder sb = new StringBuilder();

        for (AssessmentStudentEntity student : assessmentStudents) {
            var examSchool = restUtils.getSchoolBySchoolID(String.valueOf(student.getAssessmentCenterSchoolID()));
            var examMincode = examSchool.map(SchoolTombstone::getMincode).orElse("");
            String row =
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
            sb.append(row);
        }
        return sb;
    }

    private StringBuilder generateRowsStagedAssessmentStudent(List<StagedAssessmentStudentEntity> assessmentStudents, SchoolTombstone school) {
        StringBuilder sb = new StringBuilder();

        for (StagedAssessmentStudentEntity student : assessmentStudents) {
            var examSchool = restUtils.getSchoolBySchoolID(String.valueOf(student.getAssessmentCenterSchoolID()));
            var examMincode = examSchool.map(SchoolTombstone::getMincode).orElse("");
            String row =
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
            sb.append(row);
        }
        return sb;
    }

    /**
     * Returns a DownloadableReportResponse for the XAM content (used by controller)
     */
    public DownloadableReportResponse generateXamReport(UUID sessionID, UUID schoolID) {
        var assessmentSession = assessmentSessionRepository.findById(sessionID).orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class));
        var schoolTombstone = this.restUtils.getSchoolBySchoolID(schoolID.toString()).orElseThrow(() -> new EntityNotFoundException(SchoolTombstone.class));
        return generateXamContent(assessmentSession, schoolTombstone, false);
    }

    public void uploadToS3(byte[] content, String key) {
        try {
            String bucketName = applicationProperties.getS3BucketName();
            String endpoint = applicationProperties.getS3EndpointUrl();

            log.info("S3 Upload Configuration - Bucket: {}, Key: {}, Endpoint: {}, Content Size: {} bytes",
                    bucketName, key, endpoint, content.length);

            // Log S3 client configuration details
            log.debug("S3 Access Key ID: {}", applicationProperties.getS3AccessKeyId() != null ? "***configured***" : "NULL");
            log.debug("S3 Secret Key: {}", applicationProperties.getS3AccessSecretKey() != null ? "***configured***" : "NULL");

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            var response = s3Client.putObject(request, RequestBody.fromBytes(content));

            log.info("S3 Upload Response - ETag: {}, VersionId: {}, RequestId: {}",
                    response.eTag(), response.versionId(), response.responseMetadata().requestId());

            // Verify the object actually exists in S3
            try {
                var headResponse = s3Client.headObject(software.amazon.awssdk.services.s3.model.HeadObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build());
                log.info("Verification SUCCESS - Object exists in S3. Size: {}, LastModified: {}",
                        headResponse.contentLength(), headResponse.lastModified());
                log.debug("Successfully uploaded file to BCBox S3: {} (size: {} bytes)", key, content.length);
            } catch (Exception verifyEx) {
                log.error("VERIFICATION FAILED - Object does NOT exist in S3 after upload: {}", key, verifyEx);
                throw new StudentAssessmentAPIRuntimeException("File upload appeared successful but verification failed: " + verifyEx.getMessage());
            }

        } catch (Exception e) {
            log.error("Failed to upload file to BCBox S3: {} to bucket: {} - Error: {}",
                    key, applicationProperties.getS3BucketName(), e.getMessage(), e);
            throw new StudentAssessmentAPIRuntimeException("Failed to upload file to BCBox S3: " + e.getMessage());
        }
    }

    /**
     * for orchestration:
     * Generates and uploads XAM files for all schools for the given session.
     * Only schools with vendor source system code MYED
     */
    public void generateAndUploadXamFiles(AssessmentSessionEntity assessmentSessionEntity) {
        List<SchoolTombstone> schools = restUtils.getAllSchoolTombstones();
        List<SchoolTombstone> myEdSchools = schools.stream()
                .filter(school -> "MYED".equalsIgnoreCase(school.getVendorSourceSystemCode()))
                .toList();
        String folderName = generateXamFolderName(assessmentSessionEntity);
        log.debug("Starting generation and upload of XAM files for {} MYED schools, session {} to folder {}", myEdSchools.size(), assessmentSessionEntity.getSessionID(), folderName);
        for (SchoolTombstone school : myEdSchools) {
            log.debug("Generating XAM file for school: {}", school.getMincode());
            byte[] xamFileContent = generateXamContent(assessmentSessionEntity, school, true);
            log.debug("Uploading XAM file for school: {}", school.getMincode());
            String fileName = generateXamFileName(school, assessmentSessionEntity);
            String key = folderName + "/" + fileName;
            uploadToS3(xamFileContent, key);
            log.debug("Successfully uploaded XAM file for school: {}", school.getMincode());
        }
        log.debug("Completed processing XAM files for session {} to folder {}", assessmentSessionEntity.getSessionID(), folderName);
    }

    /**
     * Generate the XAM folder name based on session information
     * Format: xam-files-yyyymm
     */
    private String generateXamFolderName(AssessmentSessionEntity assessmentSessionEntity) {
        return "xam-files-" + assessmentSessionEntity.getCourseYear() + assessmentSessionEntity.getCourseMonth();
    }

    /**
     * Generate the XAM filename based on school and session information
     */
    private String generateXamFileName(SchoolTombstone school, AssessmentSessionEntity assessmentSessionEntity) {
        return school.getMincode() + "-" + assessmentSessionEntity.getCourseYear() + assessmentSessionEntity.getCourseMonth() + "-Results.xam";
    }

    private String padRight(String value, int length) {
        String str = value == null ? "" : value;
        if (str.length() > length) {
            return str.substring(0, length);
        }
        return String.format("%-" + length + "s", str);
    }
}
