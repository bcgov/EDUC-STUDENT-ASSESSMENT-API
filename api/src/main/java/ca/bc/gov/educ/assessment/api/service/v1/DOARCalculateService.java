package ca.bc.gov.educ.assessment.api.service.v1;


import ca.bc.gov.educ.assessment.api.model.v1.*;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
@NoArgsConstructor
public class DOARCalculateService {

    public BigDecimal calculateTotal(AssessmentStudentEntity student, List<AssessmentQuestionEntity> selectedOeAssessmentQuestionsByTypeCode, List<AssessmentQuestionEntity> selectedMcAssessmentQuestionsByTypeCode) {
        //possible total
        BigDecimal possibleMcTotal = getPossibleMCTotal(selectedMcAssessmentQuestionsByTypeCode);
        log.debug("possibleMcTotal: {}", possibleMcTotal);

        BigDecimal possibleOeTotal = getPossibleOETotal(selectedOeAssessmentQuestionsByTypeCode, student); // check per question
        log.debug("possibleOeTotal: {}", possibleOeTotal);

        //student Total
        BigDecimal studentMcTotal = getStudentMCTotal(selectedMcAssessmentQuestionsByTypeCode, student);
        log.debug("studentMcTotal: {}", studentMcTotal);
        BigDecimal studentOeTotal = getStudentOETotal(selectedOeAssessmentQuestionsByTypeCode, student);
        log.debug("studentOeTotal: {}", studentOeTotal);

        var studentTotal = studentMcTotal.add(studentOeTotal);
        var possibleTotal = possibleMcTotal.add(possibleOeTotal);
        log.debug("studentTotal: {}", studentTotal);
        log.debug("possibleTotal: {}", possibleTotal);

        if(possibleTotal.compareTo(BigDecimal.ZERO) != 0) {
            var score = studentTotal.divide(possibleTotal, 4, RoundingMode.DOWN).multiply(BigDecimal.valueOf(100));
            log.debug("return-in-if: {}", score);
            return score;
        }

        return BigDecimal.ZERO;
    }

    private boolean checkIfStudentAnsweredOEQues(AssessmentStudentEntity student, List<AssessmentQuestionEntity> questions) {

        //question ids by typecode
        var listOfQuesIDs = questions.stream().map(AssessmentQuestionEntity::getAssessmentQuestionID).toList();

        //assessment choice Ids of questions by typecode
        var listOfQuesChoiceIDs =  questions.stream().map(AssessmentQuestionEntity::getAssessmentChoiceEntity)
                .filter(Objects::nonNull).map(AssessmentChoiceEntity::getAssessmentChoiceID).toList();

        //questionIds of student answers
        var listOfQuesAnswered = student.getAssessmentStudentComponentEntities().stream()
                .map(AssessmentStudentComponentEntity::getAssessmentStudentAnswerEntities).flatMap(Collection::stream).map(AssessmentStudentAnswerEntity::getAssessmentQuestionID).toList();

        //assessment choice Ids of student answers
        var listOfStudentChoices = student.getAssessmentStudentComponentEntities().stream()
                .map(AssessmentStudentComponentEntity::getAssessmentStudentChoiceEntities).flatMap(Collection::stream)
                .map(AssessmentStudentChoiceEntity::getAssessmentChoiceEntity).filter(Objects::nonNull).map(AssessmentChoiceEntity::getAssessmentChoiceID).toList();

        var hasAnsweredOeQues =  listOfQuesAnswered.stream().anyMatch(listOfQuesIDs::contains);
        var hasAnsweredADifferentChoice =  listOfStudentChoices.stream().anyMatch(listOfQuesChoiceIDs::contains);
        if(hasAnsweredOeQues) {
            return true;
        } else return !hasAnsweredADifferentChoice;
    }

    public BigDecimal calculateMCTotal(List<AssessmentQuestionEntity> selectedMcAssessmentQuestionsByTypeCode, AssessmentStudentEntity student) {
        BigDecimal possibleMcTotal = getPossibleMCTotal(selectedMcAssessmentQuestionsByTypeCode);
        BigDecimal studentMcTotal = getStudentMCTotal(selectedMcAssessmentQuestionsByTypeCode, student);

        if(possibleMcTotal.compareTo(BigDecimal.ZERO) != 0) {
            return studentMcTotal.divide(possibleMcTotal, 4, RoundingMode.DOWN).multiply(BigDecimal.valueOf(100));
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal getPossibleMCTotal(List<AssessmentQuestionEntity> selectedMcAssessmentQuestionsByTypeCode) {
        BigDecimal divisor = new BigDecimal(100);
        if(selectedMcAssessmentQuestionsByTypeCode.isEmpty()) {
            return BigDecimal.ZERO;
        }
        var totalQuestionValue = selectedMcAssessmentQuestionsByTypeCode.stream()
                .map(question -> question.getQuestionValue().multiply(BigDecimal.valueOf(question.getScaleFactor())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalQuestionValue.divide(divisor, 4, RoundingMode.DOWN);
    }

    private BigDecimal getPossibleOETotal(List<AssessmentQuestionEntity> selectedOeAssessmentQuestionsByTypeCode, AssessmentStudentEntity student) {
        if(selectedOeAssessmentQuestionsByTypeCode.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Map<Integer, List<AssessmentQuestionEntity>> groupedQuestionsByMasterQuestionNumber = selectedOeAssessmentQuestionsByTypeCode.stream().collect(Collectors.groupingBy(AssessmentQuestionEntity::getMasterQuestionNumber));

        BigDecimal possibleScore = BigDecimal.ZERO;
        BigDecimal divisor = new BigDecimal(100);
        for(List<AssessmentQuestionEntity> questionEntities : groupedQuestionsByMasterQuestionNumber.values()) {
            var totalQuestionValue = questionEntities.getFirst().getQuestionValue();
            var totalScale = questionEntities.getFirst().getScaleFactor();

            boolean includeOeTotalInCalc = checkIfStudentAnsweredOEQues(student, questionEntities);
            if(includeOeTotalInCalc) {
                possibleScore = possibleScore.add(totalQuestionValue.multiply(BigDecimal.valueOf(totalScale)).divide(divisor, 4, RoundingMode.DOWN));
            }
        }
        return possibleScore;
    }

    private BigDecimal getStudentMCTotal(List<AssessmentQuestionEntity> selectedMcAssessmentQuestionsByTypeCode, AssessmentStudentEntity student) {
        if(selectedMcAssessmentQuestionsByTypeCode.isEmpty()) {
            return BigDecimal.ZERO;
        }

        var assessmentComponentID = selectedMcAssessmentQuestionsByTypeCode.getFirst().getAssessmentComponentEntity().getAssessmentComponentID();
        // get student MC answers
        var studentMcAnswers = student.getAssessmentStudentComponentEntities().stream()
                .filter(assessmentStudentComponentEntity -> Objects.equals(assessmentStudentComponentEntity.getAssessmentComponentID(), assessmentComponentID))
                .map(AssessmentStudentComponentEntity::getAssessmentStudentAnswerEntities)
                .flatMap(Collection::stream)
                .toList();

        BigDecimal studentScore = BigDecimal.ZERO;
        for(AssessmentQuestionEntity  questionEntities : selectedMcAssessmentQuestionsByTypeCode) {
            BigDecimal scaledScore =  calculateSingleMarkerScore(questionEntities, studentMcAnswers);
            studentScore = studentScore.add(scaledScore);
        }
        return studentScore;
    }

    private BigDecimal getStudentOETotal(List<AssessmentQuestionEntity> selectedOeAssessmentQuestionsByTypeCode, AssessmentStudentEntity student) {
        if(selectedOeAssessmentQuestionsByTypeCode.isEmpty()) {
            return BigDecimal.ZERO;
        }

        var assessmentComponentID = selectedOeAssessmentQuestionsByTypeCode.getFirst().getAssessmentComponentEntity().getAssessmentComponentID();
        // get student OE answers
        var studentMcAnswers = student.getAssessmentStudentComponentEntities().stream()
                .filter(assessmentStudentComponentEntity -> Objects.equals(assessmentStudentComponentEntity.getAssessmentComponentID(), assessmentComponentID))
                .map(AssessmentStudentComponentEntity::getAssessmentStudentAnswerEntities)
                .flatMap(Collection::stream)
                .toList();

        Map<Integer, List<AssessmentQuestionEntity>> groupedQuestionsByQuestionNumber = selectedOeAssessmentQuestionsByTypeCode.stream().collect(Collectors.groupingBy(AssessmentQuestionEntity::getQuestionNumber));

        BigDecimal divisor = new BigDecimal(100);
        BigDecimal calcScore = BigDecimal.ZERO;

        for(List<AssessmentQuestionEntity> questionEntities : groupedQuestionsByQuestionNumber.values()) {
            if(questionEntities.size() > 1) {
                var scaleFactor = questionEntities.getFirst().getScaleFactor();
                var studentAnswers = studentMcAnswers.stream()
                        .filter(assessmentStudentAnswerEntity -> questionEntities.stream()
                                .anyMatch(assessmentQuestionEntity -> Objects.equals(assessmentStudentAnswerEntity.getAssessmentQuestionID(), assessmentQuestionEntity.getAssessmentQuestionID())))
                        .toList();

                if (!studentAnswers.isEmpty()) {
                    var studentScore = studentAnswers.stream().map(AssessmentStudentAnswerEntity::getScore)
                            .map(bigDecimal -> bigDecimal.compareTo(new BigDecimal(9999)) == 0 ? BigDecimal.ZERO : bigDecimal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(studentAnswers.size()), 4, RoundingMode.DOWN);
                    BigDecimal scaledScore = studentScore.multiply(BigDecimal.valueOf(scaleFactor));
                    calcScore = calcScore.add(scaledScore.divide(divisor, 4, RoundingMode.DOWN));
                }
            } else {
                BigDecimal scaledScore = calculateSingleMarkerScore(questionEntities.getFirst(), studentMcAnswers);
                calcScore = calcScore.add(scaledScore);
            }

        }
        return calcScore;
    }

    private BigDecimal calculateSingleMarkerScore(AssessmentQuestionEntity questionEntity, List<AssessmentStudentAnswerEntity> studentMcAnswers) {
        BigDecimal divisor = new BigDecimal(100);
        var scaleFactor = questionEntity.getScaleFactor();
        var studentAnswer = studentMcAnswers.stream()
                .filter(assessmentStudentAnswerEntity -> Objects.equals(assessmentStudentAnswerEntity.getAssessmentQuestionID(), questionEntity.getAssessmentQuestionID()))
                .findFirst();

        if (studentAnswer.isPresent()) {
            var studentScore = studentAnswer.get().getScore();
            BigDecimal scaledScore = studentScore.compareTo(new BigDecimal(9999)) == 0 ? BigDecimal.ZERO : studentScore.multiply(BigDecimal.valueOf(scaleFactor));
            return scaledScore.divide(divisor, 4, RoundingMode.DOWN);
        }
        return BigDecimal.ZERO;
    }
}
