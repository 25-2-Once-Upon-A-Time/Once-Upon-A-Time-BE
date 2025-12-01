package pproject.once_upon_a_time.domain.playback.domain;

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
@Table(name = "audio_books_playback")
public class AudioBookPlayback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "playback_id", nullable = false)
    private Long playbackId; // 재생 ID

    @Column(name = "started_at")
    private LocalDateTime startedAt; // 재생 시작 시간

    @Column(name = "ended_at")
    private LocalDateTime endedAt; // 재생 종료 시간

    @Column(name = "last_position")
    private Integer lastPosition; // 마지막 재생 위치(초)

    @Column(name = "progress_rate")
    private Float progressRate; // 진척률

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PlaybackStatus status; // 현재 상태 (playing, paused, completed)

    @Column(name = "listened_duration")
    private Integer listenedDuration; // 누적 재생 시간(초)

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // 생성 시각

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 수정 시각

    @Column(name = "audiobook_id", nullable = false)
    private Long audiobookId; // 고유 식별자 (외래키)

    @Column(name = "member_id", length = 255, nullable = false)
    private String memberId; // 회원 아이디 (외래키)

    // 플레이백 상태 ENUM
    public enum PlaybackStatus {
        playing,
        paused,
        completed
    }
}