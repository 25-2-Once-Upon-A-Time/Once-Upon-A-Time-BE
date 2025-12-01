package pproject.once_upon_a_time.domain.timer.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "sleep_timers")
public class SleepTimer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "timer_id", nullable = false)
    private Long timerId; // 타이머 ID

    @Column(name = "dutation_seconds")
    private Integer dutationSeconds; // 설정 타이머 시간 (초)

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // 타이머 만료 예정 시각

    @Column(name = "is_active")
    private Boolean isActive; // 현재 타이머 활성 여부

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // 생성일

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 수정일

    @Column(name = "playback_id", nullable = false)
    private Long playbackId; // 재생 ID (FK)

    @Column(name = "member_id", length = 255, nullable = false)
    private String memberId; // 회원 ID (FK)
}