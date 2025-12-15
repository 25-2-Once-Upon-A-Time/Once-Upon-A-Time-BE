package pproject.once_upon_a_time.domain.admin.dto;

import lombok.Builder;
import lombok.Getter;
import pproject.once_upon_a_time.domain.audiobook.domain.AudioBook;

@Getter
@Builder
public class AdminAudioBookResponseDto {
    private Long audioBookId;
    private String storyTitle;
    private String memberName;    // 만든 사람 추가
    private String characterName; // 캐릭터 이름 추가

    public static AdminAudioBookResponseDto from(AudioBook audioBook) {
        return AdminAudioBookResponseDto.builder()
            .audioBookId(audioBook.getId())
            .storyTitle(audioBook.getStory().getTitle())
            .memberName(audioBook.getMember().getName())
            .characterName(audioBook.getCharacter().getCharacterName())
            .build();
    }
}
