package pproject.once_upon_a_time.domain.audiobook.domain;

import jakarta.persistence.*;
import lombok.*;
import pproject.once_upon_a_time.domain.character.domain.Character;
import pproject.once_upon_a_time.domain.story.domain.Story;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "audio_books")
public class AudioBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audiobook_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private Character character;
    
    private String audioUrl;
}
