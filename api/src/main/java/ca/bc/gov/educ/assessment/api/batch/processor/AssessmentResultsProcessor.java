package ca.bc.gov.educ.assessment.api.batch.processor;

import ca.bc.gov.educ.assessment.api.batch.exception.KeyFileError;
import ca.bc.gov.educ.assessment.api.batch.exception.KeyFileUnProcessableException;
import ca.bc.gov.educ.assessment.api.batch.service.AssessmentResultService;
import ca.bc.gov.educ.assessment.api.batch.struct.AssessmentResultDetails;
import ca.bc.gov.educ.assessment.api.batch.struct.AssessmentResultFile;
import ca.bc.gov.educ.assessment.api.batch.validation.ResultsFileValidator;
import ca.bc.gov.educ.assessment.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.assessment.api.exception.errors.ApiError;
import ca.bc.gov.educ.assessment.api.mappers.StringMapper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentQuestionEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentResultFileUpload;
import ca.bc.gov.educ.assessment.api.util.ValidationUtil;
import com.google.common.base.Stopwatch;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.sf.flatpack.DataSet;
import net.sf.flatpack.DefaultParserFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static ca.bc.gov.educ.assessment.api.batch.constants.AssessmentResultsBatchFile.*;
import static ca.bc.gov.educ.assessment.api.batch.mapper.AssessmentResultsBatchFileMapper.mapper;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Component
@Slf4j
public class AssessmentResultsProcessor {

    public static final String INVALID_PAYLOAD_MSG = "Payload contains invalid data.";
    public static final String ASSESSMENT_RESULT_UPLOAD = "assessmentResultUpload";
    private final ResultsFileValidator resultsFileValidator;
    private final AssessmentResultService assessmentResultService;

    public AssessmentResultsProcessor(ResultsFileValidator resultsFileValidator, AssessmentResultService assessmentResultService) {
        this.resultsFileValidator = resultsFileValidator;
        this.assessmentResultService = assessmentResultService;
    }

    public void processAssessmentResults(AssessmentResultFileUpload fileUpload, String session) {
        val stopwatch = Stopwatch.createStarted();
        final var guid = UUID.randomUUID().toString();
        Optional<Reader> batchFileReaderOptional = Optional.empty();
        try {
            final Reader mapperReader = new FileReader(Objects.requireNonNull(this.getClass().getClassLoader().getResource("assessmentResultMapper.xml")).getFile());
            var byteArrayOutputStream = new ByteArrayInputStream(resultsFileValidator.getUploadedFileBytes(guid, fileUpload));
            batchFileReaderOptional = Optional.of(new InputStreamReader(byteArrayOutputStream));
            final DataSet ds = DefaultParserFactory.getInstance().newFixedLengthParser(mapperReader, batchFileReaderOptional.get()).setStoreRawDataToDataError(true).setStoreRawDataToDataSet(true).setNullEmptyStrings(true).parse();

            assessmentResultService.populateBatchFileAndLoadData(guid, ds, fileUpload);
        } catch (final KeyFileUnProcessableException keyFileUnProcessableException) {
            log.error("File could not be processed exception :: {}", keyFileUnProcessableException);
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message(INVALID_PAYLOAD_MSG).status(BAD_REQUEST).build();
            var validationError = ValidationUtil.createFieldError(ASSESSMENT_RESULT_UPLOAD, session, keyFileUnProcessableException.getReason());
            List<FieldError> fieldErrorList = new ArrayList<>();
            fieldErrorList.add(validationError);
            error.addValidationErrors(fieldErrorList);
            throw new InvalidPayloadException(error);
        } catch (final Exception e) {
            log.error("Exception while processing the file with guid :: {} :: Exception :: {}", guid, e);
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message(INVALID_PAYLOAD_MSG).status(BAD_REQUEST).build();
            var validationError = ValidationUtil.createFieldError(ASSESSMENT_RESULT_UPLOAD, session , KeyFileError.GENERIC_ERROR_MESSAGE.getMessage());
            List<FieldError> fieldErrorList = new ArrayList<>();
            fieldErrorList.add(validationError);
            error.addValidationErrors(fieldErrorList);
            throw new InvalidPayloadException(error);
        } finally {
            batchFileReaderOptional.ifPresent(this::closeBatchFileReader);
            stopwatch.stop();
            log.info("Time taken for batch processed is :: {} milli seconds", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    private void populateBatchFileAndLoadData(String guid, DataSet ds, AssessmentResultFileUpload fileUpload) throws KeyFileUnProcessableException {
        val batchFile = new AssessmentResultFile();
        populateAssessmentResultFile(guid, ds, batchFile);
        //load
    }

    public void populateAssessmentResultFile(final String guid, final DataSet ds, final AssessmentResultFile batchFile) throws KeyFileUnProcessableException {
        long index = 0;
        while (ds.next()) {
            batchFile.getAssessmentResultData().add(getAssessmentResultDetailRecordFromFile(ds));
            index++;
        }
    }

    public AssessmentQuestionEntity processLoadedRecordsInBatchFile(@NonNull final String guid, @NonNull final AssessmentResultFile batchFile, @NonNull final AssessmentResultFileUpload fileUpload, final String schoolID, final String districtID) throws KeyFileUnProcessableException {
        List<AssessmentQuestionEntity> questionEntityList = new ArrayList<>();

        // batch file can be processed further and persisted.
        for (final var result : batchFile.getAssessmentResultData()) {
            final var resultEntity = mapper.toResultEntity(result);
            questionEntityList.add(resultEntity);
        }

        return craftStudentSetAndMarkInitialLoadComplete(questionEntityList);
    }

    private AssessmentResultDetails getAssessmentResultDetailRecordFromFile(final DataSet ds) {
        return AssessmentResultDetails.builder()
        		.txID(StringMapper.trimAndUppercase(ds.getString(TX_ID.getName())))
        		.examType(StringMapper.trimAndUppercase(ds.getString(EXAM_TYPE.getName())))
        		.componentType(StringMapper.trimAndUppercase(ds.getString(COMPONENT_TYPE.getName())))
        		.sequenceNumber(StringMapper.trimAndUppercase(ds.getString(SEQUENCE_NUMBER.getName())))
        		.csid(StringMapper.trimAndUppercase(ds.getString(CSID.getName())))
        		.assessmentCode(StringMapper.trimAndUppercase(ds.getString(ASSESSMENT_CODE.getName())))
        		.courseLevel(StringMapper.trimAndUppercase(ds.getString(COURSE_LEVEL.getName())))
        		.assessmentSession(StringMapper.trimAndUppercase(ds.getString(ASSESSMENT_SESSION.getName())))
        		.mincode(StringMapper.trimAndUppercase(ds.getString(MINCODE.getName())))
        		.pen(StringMapper.trimAndUppercase(ds.getString(PEN.getName())))
        		.dateOfBirth(StringMapper.trimAndUppercase(ds.getString(DATE_OF_BIRTH.getName())))
        		.newStudentFlag(StringMapper.trimAndUppercase(ds.getString(NEW_STUDENT_FLAG.getName())))
        		.participation(StringMapper.trimAndUppercase(ds.getString(PARTICIPATION.getName())))
        		.formCode(StringMapper.trimAndUppercase(ds.getString(FORM_CODE.getName())))
        		.formCode2(StringMapper.trimAndUppercase(ds.getString(FORM_CODE2.getName())))
        		.pageLink(StringMapper.trimAndUppercase(ds.getString(PAGE_LINK.getName())))
        		.openEndedMarks(StringMapper.trimAndUppercase(ds.getString(OPEN_ENDED_MARKS.getName())))
        		.multiChoiceMarks(StringMapper.trimAndUppercase(ds.getString(MUL_CHOICE_MARKS.getName())))
        		.choicePath(StringMapper.trimAndUppercase(ds.getString(CHOICE_PATH.getName())))
        		.specialCaseCode(StringMapper.trimAndUppercase(ds.getString(SPECIAL_CASE_CODE.getName())))
        		.proficiencyScore(StringMapper.trimAndUppercase(ds.getString(PROFICIENCY_SCORE.getName())))
        		.irtScore(StringMapper.trimAndUppercase(ds.getString(IRT_SCORE.getName())))
        		.adaptedAssessmentIndicator(StringMapper.trimAndUppercase(ds.getString(ADAPTED_ASSESSMENT_INDICATOR.getName())))
        		.markingSession(StringMapper.trimAndUppercase(ds.getString(MARKING_SESSION.getName())))
        		.build();
    }

    private void closeBatchFileReader(final Reader reader) {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (final IOException e) {
            log.warn("Error closing the batch file :: ", e);
        }
    }
}