package ca.bc.gov.educ.assessment.api.service.v1;


import ca.bc.gov.educ.assessment.api.model.v1.*;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


@Service
@Slf4j
@NoArgsConstructor
public class DOARCalculateService {

    public BigDecimal calculateTotal(AssessmentStudentEntity student, List<AssessmentQuestionEntity> selectedOeAssessmentQuestionsByTypeCode, List<AssessmentQuestionEntity> selectedMcAssessmentQuestionsByTypeCode) {
        //possible total
        BigDecimal possibleMcTotal = getPossibleMCTotal(selectedMcAssessmentQuestionsByTypeCode);
        BigDecimal possibleOeTotal = getPossibleOETotal(selectedOeAssessmentQuestionsByTypeCode);

        //student Total
        BigDecimal studentMcTotal = getStudentMCTotal(selectedMcAssessmentQuestionsByTypeCode, student);
        BigDecimal studentOeTotal = getStudentOETotal(selectedOeAssessmentQuestionsByTypeCode, student);

        var studentTotal = studentMcTotal.add(studentOeTotal);
        var possibleTotal = possibleMcTotal.add(possibleOeTotal);

        if(possibleTotal.compareTo(BigDecimal.ZERO) != 0) {
            return studentTotal.divide(possibleTotal, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        }

        return BigDecimal.ZERO;
    }

    public BigDecimal calculateMCTotal(List<AssessmentQuestionEntity> selectedMcAssessmentQuestionsByTypeCode, AssessmentStudentEntity student) {
        BigDecimal possibleMcTotal = getPossibleMCTotal(selectedMcAssessmentQuestionsByTypeCode);
        BigDecimal studentMcTotal = getStudentMCTotal(selectedMcAssessmentQuestionsByTypeCode, student);

        if(possibleMcTotal.compareTo(BigDecimal.ZERO) != 0) {
            return studentMcTotal.divide(possibleMcTotal, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal getPossibleMCTotal(List<AssessmentQuestionEntity> selectedMcAssessmentQuestionsByTypeCode) {
        BigDecimal divisor = new BigDecimal(100);
        if(selectedMcAssessmentQuestionsByTypeCode.isEmpty()) {
            return BigDecimal.ZERO;
        }
        var totalQuestionValue = selectedMcAssessmentQuestionsByTypeCode.stream().map(AssessmentQuestionEntity::getQuestionValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        var totalScale = selectedMcAssessmentQuestionsByTypeCode.stream().map(AssessmentQuestionEntity::getScaleFactor).reduce(0, Integer::sum);
        return totalQuestionValue.multiply(BigDecimal.valueOf(totalScale)).divide(divisor, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal getPossibleOETotal(List<AssessmentQuestionEntity> selectedOeAssessmentQuestionsByTypeCode) {
        if(selectedOeAssessmentQuestionsByTypeCode.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Map<Integer, List<AssessmentQuestionEntity>> groupedQuestionsByMasterQuestionNumber = selectedOeAssessmentQuestionsByTypeCode.stream().collect(Collectors.groupingBy(AssessmentQuestionEntity::getMasterQuestionNumber));

        AtomicReference<BigDecimal> possibleOEScore = new AtomicReference<>(new BigDecimal(0));
        BigDecimal divisor = new BigDecimal(100);
        groupedQuestionsByMasterQuestionNumber.values().forEach(questionEntities -> {
            var totalQuestionValue = questionEntities.getFirst().getQuestionValue();
            var totalScale = questionEntities.getFirst().getScaleFactor();
            possibleOEScore.set(possibleOEScore.get().add(totalQuestionValue.multiply(BigDecimal.valueOf(totalScale)).divide(divisor, 2, RoundingMode.HALF_UP)));
        });
        return possibleOEScore.get();
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

        AtomicReference<BigDecimal> mcStudentScoreCount = new AtomicReference<>(new BigDecimal(0));
        selectedMcAssessmentQuestionsByTypeCode.forEach(questionEntities ->
            calculateSingleMarkerScore(questionEntities, studentMcAnswers, mcStudentScoreCount)
        );
        return mcStudentScoreCount.get();
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
        AtomicReference<BigDecimal> mcStudentScoreCount = new AtomicReference<>(new BigDecimal(0));

        groupedQuestionsByQuestionNumber.values().forEach(questionEntities -> {
            if(questionEntities.size() > 1) {
                var scaleFactor = questionEntities.getFirst().getScaleFactor();
                var studentAnswers = studentMcAnswers.stream()
                        .filter(assessmentStudentAnswerEntity -> questionEntities.stream()
                                .anyMatch(assessmentQuestionEntity -> Objects.equals(assessmentStudentAnswerEntity.getAssessmentQuestionID(), assessmentQuestionEntity.getAssessmentQuestionID())))
                        .toList();

                if (!studentAnswers.isEmpty()) {
                    var studentScore = studentAnswers.stream().map(AssessmentStudentAnswerEntity::getScore)
                            .map(bigDecimal -> bigDecimal.compareTo(new BigDecimal(9999)) == 0 ? BigDecimal.ZERO : bigDecimal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(studentAnswers.size()), RoundingMode.HALF_UP);
                    BigDecimal scaledScore = studentScore.multiply(BigDecimal.valueOf(scaleFactor));
                    mcStudentScoreCount.set(mcStudentScoreCount.get().add(scaledScore.divide(divisor, 2, RoundingMode.HALF_UP)));
                }
            } else {
                calculateSingleMarkerScore(questionEntities.getFirst(), studentMcAnswers, mcStudentScoreCount);
            }

        });
        return mcStudentScoreCount.get();
    }

    private void calculateSingleMarkerScore(AssessmentQuestionEntity questionEntity, List<AssessmentStudentAnswerEntity> studentMcAnswers, AtomicReference<BigDecimal> mcStudentScoreCount) {
        BigDecimal divisor = new BigDecimal(100);
        var scaleFactor = questionEntity.getScaleFactor();
        var studentAnswer = studentMcAnswers.stream()
                .filter(assessmentStudentAnswerEntity -> Objects.equals(assessmentStudentAnswerEntity.getAssessmentQuestionID(), questionEntity.getAssessmentQuestionID()))
                .findFirst();

        if (studentAnswer.isPresent()) {
            var studentScore = studentAnswer.get().getScore();
            BigDecimal scaledScore = studentScore.compareTo(new BigDecimal(9999)) == 0 ? BigDecimal.ZERO : studentScore.multiply(BigDecimal.valueOf(scaleFactor));
            mcStudentScoreCount.set(mcStudentScoreCount.get().add(scaledScore.divide(divisor, 2, RoundingMode.HALF_UP)));
        }
    }
}
