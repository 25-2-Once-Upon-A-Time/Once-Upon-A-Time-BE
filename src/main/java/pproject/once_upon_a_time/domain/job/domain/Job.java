package pproject.once_upon_a_time.domain.job.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pproject.once_upon_a_time.global.common.BaseTimeEntity;

import java.util.UUID;

@Entity
@Table(name = "jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Job extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private JobType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private JobStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private JobTargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "input_key", length = 500)
    private String inputKey;

    @Column(name = "output_key", length = 500)
    private String outputKey;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Builder
    public Job(
        JobType type,
        JobStatus status,
        JobTargetType targetType,
        Long targetId,
        String inputKey,
        String outputKey,
        String errorMessage
    ) {
        this.type = type;
        this.status = status;
        this.targetType = targetType;
        this.targetId = targetId;
        this.inputKey = inputKey;
        this.outputKey = outputKey;
        this.errorMessage = errorMessage;
    }

    public void updateInputKey(String inputKey) {
        this.inputKey = inputKey;
    }
}
