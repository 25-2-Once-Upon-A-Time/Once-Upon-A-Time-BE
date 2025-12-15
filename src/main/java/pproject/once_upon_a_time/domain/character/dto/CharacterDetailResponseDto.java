package pproject.once_upon_a_time.domain.character.dto;

import lombok.Getter;
import pproject.once_upon_a_time.domain.character.domain.Character;
import pproject.once_upon_a_time.domain.character.domain.CharacterSampleAudio;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class CharacterDetailResponseDto {

    private final Long id;
    private final String characterName;
    private final String characterType;
    private final String description;
    private final String thumbnailUrl;
    private final String voiceSampleUrl;
    private final String voiceActor;
    private final List<String> tags;

    // likeCount 필드 제거됨
    private final List<AudioSampleDto> audios; // 샘플 오디오 리스트

    public CharacterDetailResponseDto(Character character) {
        this.id = character.getId();
        this.characterName = character.getCharacterName();
        this.characterType = character.getCharacterType();
        this.description = character.getDescription();
        this.thumbnailUrl = character.getThumbnailUrl();
        this.voiceSampleUrl = character.getVoiceSampleUrl();
        this.voiceActor = character.getVoiceActor();
        this.tags = character.getTags();

        // 샘플 오디오 리스트 매핑
        this.audios = character.getSampleAudios().stream()
            .map(AudioSampleDto::new)
            .collect(Collectors.toList());
    }

    @Getter
    public static class AudioSampleDto {
        private final String title;
        private final String duration;
        private final String audioUrl;

        public AudioSampleDto(CharacterSampleAudio sampleAudio) {
            this.title = sampleAudio.getTitle();
            this.duration = sampleAudio.getDuration();
            this.audioUrl = sampleAudio.getAudioUrl();
        }
    }
}
