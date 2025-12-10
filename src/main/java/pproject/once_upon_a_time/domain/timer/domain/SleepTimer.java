package pproject.once_upon_a_time.domain.timer.domain;

import jakarta.persistence.*;
import lombok.*;
import pproject.once_upon_a_time.domain.playback.domain.AudioBookPlayback;
import pproject.once_upon_a_time.global.common.BaseTimeEntity;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "sleep_timers")
public class SleepTimer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "timer_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playback_id", nullable = false)
    private AudioBookPlayback playback;

    @Column(nullable = false)
    private String memberId;

    private Integer dutationSeconds;

    private LocalDateTime expiresAt;

    private Boolean isActive;
}
