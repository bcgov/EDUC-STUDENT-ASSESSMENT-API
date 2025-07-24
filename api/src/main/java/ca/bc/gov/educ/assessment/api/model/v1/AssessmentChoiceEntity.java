package ca.bc.gov.educ.assessment.api.model.v1;

import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Builder
@Table(name = "ASSESSMENT_CHOICE")
public class AssessmentChoiceEntity {

    @Id
    @UuidGenerator
    @Column(name = "ASSESSMENT_CHOICE_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
    private UUID assessmentChoiceID;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(optional = false, targetEntity = AssessmentComponentEntity.class)
    @JoinColumn(name = "ASSESSMENT_COMPONENT_ID", referencedColumnName = "ASSESSMENT_COMPONENT_ID", updatable = false)
    AssessmentComponentEntity assessmentComponentEntity;

    @Column(name = "ITEM_NUMBER")
    private Integer itemNumber;

    @Column(name = "CREATE_USER", updatable = false, length = 100)
    private String createUser;

    @PastOrPresent
    @Column(name = "CREATE_DATE", updatable = false)
    private LocalDateTime createDate;

    @Column(name = "UPDATE_USER", nullable = false, length = 100)
    private String updateUser;

    @PastOrPresent
    @Column(name = "UPDATE_DATE", nullable = false)
    private LocalDateTime updateDate;
}