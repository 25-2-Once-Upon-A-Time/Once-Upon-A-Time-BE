package pproject.once_upon_a_time.domain.playback.domain;

import jakarta.persistence.*;
import lombok.*;
import pproject.once_upon_a_time.domain.audiobook.domain.AudioBook;
import pproject.once_upon_a_time.global.common.BaseTimeEntity;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "audio_books_playback")
public class AudioBookPlayback extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "playback_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audiobook_id", nullable = false)
    private AudioBook audioBook;

    @Column(nullable = false)
    private String memberId;

    private Integer lastPosition;

    private Integer listenedDuration;

    private Float progressRate;

    private LocalDateTime startedAt;
    
    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    private AudioBookStatus status;
}
