package pproject.once_upon_a_time.domain.character.dto;

import lombok.Getter;
import pproject.once_upon_a_time.domain.character.domain.Character;

@Getter
public class CharacterListResponseDto {

    private final Long id;
    private final String characterName;
    private final String thumbnailUrl;

    public CharacterListResponseDto(Character character) {
        this.id = character.getId();
        this.characterName = character.getCharacterName();
        this.thumbnailUrl = character.getThumbnailUrl();
    }
}