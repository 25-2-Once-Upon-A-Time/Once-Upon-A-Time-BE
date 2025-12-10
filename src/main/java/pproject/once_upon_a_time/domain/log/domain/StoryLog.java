package pproject.once_upon_a_time.domain.log.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import pproject.once_upon_a_time.domain.playback.domain.AudioBookPlayback;
import pproject.once_upon_a_time.domain.story.domain.Story;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "story_log")
public class StoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "storylog_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playback_id", nullable = false)
    private AudioBookPlayback playback;

    private Integer totalPlays;
    
    private LocalDateTime date;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
