package pproject.once_upon_a_time.domain.selection.domain;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_story_character_selections")
public class UserStoryCharacterSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "selection_id", nullable = false)
    private Long selectionId; // 선택 ID

    @Column(name = "character_id", nullable = false)
    private Long characterId; // 캐릭터 ID (FK)

    @Column(name = "member_id", length = 255, nullable = false)
    private String memberId; // 사용자 ID (FK)

    @Column(name = "story_id", nullable = false)
    private Long storyId; // 동화 ID (FK)
}