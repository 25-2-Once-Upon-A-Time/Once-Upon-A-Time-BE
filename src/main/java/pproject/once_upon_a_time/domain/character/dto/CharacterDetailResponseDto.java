package pproject.once_upon_a_time.domain.character.dto;

import lombok.Getter;
import pproject.once_upon_a_time.domain.character.domain.Character;

@Getter
public class CharacterDetailResponseDto {

    private final Long id;
    private final String characterName;
    private final String characterType;
    private final String description;
    private final String thumbnailUrl;
    private final String voiceSampleUrl;

    public CharacterDetailResponseDto(Character character) {
        this.id = character.getId();
        this.characterName = character.getCharacterName();
        this.characterType = character.getCharacterType();
        this.description = character.getDescription();
        this.thumbnailUrl = character.getThumbnailUrl();
        this.voiceSampleUrl = character.getVoiceSampleUrl();
    }
}
