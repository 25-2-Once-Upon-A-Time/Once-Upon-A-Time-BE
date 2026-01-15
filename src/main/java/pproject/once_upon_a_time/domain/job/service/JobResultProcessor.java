package pproject.once_upon_a_time.domain.job.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pproject.once_upon_a_time.domain.audiobook.domain.AudioBook;
import pproject.once_upon_a_time.domain.audiobook.repository.AudioBookRepository;
import pproject.once_upon_a_time.domain.character.domain.Character;
import pproject.once_upon_a_time.domain.character.repository.CharacterRepository;
import pproject.once_upon_a_time.domain.job.domain.Job;
import pproject.once_upon_a_time.domain.job.domain.JobTargetType;
import pproject.once_upon_a_time.domain.job.domain.JobType;
import pproject.once_upon_a_time.domain.story.domain.Story;
import pproject.once_upon_a_time.domain.story.dto.AiStoryResponseDto;
import pproject.once_upon_a_time.domain.story.repository.StoryRepository;
import pproject.once_upon_a_time.domain.story.service.StoryUpdateService;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;

import java.util.Iterator;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobResultProcessor {

    private final JobResultFetcher jobResultFetcher;
    private final ObjectMapper objectMapper;
    private final StoryUpdateService storyUpdateService;
    private final StoryRepository storyRepository;
    private final CharacterRepository characterRepository;
    private final AudioBookRepository audioBookRepository;

    public void applySuccess(Job job) {
        if (job.getOutputKey() == null || job.getOutputKey().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "outputKey가 비어 있습니다.");
        }

        if (job.getType() == JobType.STORY) {
            applyStoryResult(job);
            return;
        }
        if (job.getType() == JobType.ILLUSTRATION) {
            applyIllustrationResult(job);
            return;
        }
        if (job.getType() == JobType.AUDIOBOOK) {
            applyAudioBookResult(job);
            return;
        }

        log.warn("지원되지 않는 JobType: {}", job.getType());
    }

    public void applyFailure(Job job) {
        if (job.getType() == JobType.STORY && job.getTargetType() == JobTargetType.STORY) {
            Long storyId = job.getTargetId();
            if (storyId != null) {
                storyUpdateService.finalizeStoryOnError(storyId);
            }
        }
    }

    private void applyStoryResult(Job job) {
        validateTarget(job, JobTargetType.STORY);
        String payload = jobResultFetcher.fetchJson(job.getOutputKey());
        try {
            AiStoryResponseDto response = objectMapper.readValue(payload, AiStoryResponseDto.class);
            storyUpdateService.finalizeStory(job.getTargetId(), response);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "동화 결과 파싱 실패: " + e.getMessage());
        }
    }

    private void applyIllustrationResult(Job job) {
        validateTarget(job, JobTargetType.STORY);
        String payload = jobResultFetcher.fetchJson(job.getOutputKey());
        try {
            JsonNode root = objectMapper.readTree(payload);
            String imageKey = root.path("data").path("image_key").asText();
            if (imageKey == null || imageKey.isBlank()) {
                throw new CustomException(ErrorCode.INVALID_INPUT, "이미지 key가 비어 있습니다.");
            }

            Story story = storyRepository.findById(job.getTargetId())
                .orElseThrow(() -> new CustomException(ErrorCode.STORY_NOT_FOUND));
            story.updateThumbnailUrl(imageKey);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "썸네일 결과 파싱 실패: " + e.getMessage());
        }
    }

    private void applyAudioBookResult(Job job) {
        validateTarget(job, JobTargetType.STORY);
        if (job.getInputKey() == null || job.getInputKey().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "inputKey가 비어 있습니다.");
        }

        String inputPayload = jobResultFetcher.fetchJson(job.getInputKey());
        Long storyId = job.getTargetId();
        Long characterId = extractLong(inputPayload, "character_id");
        if (storyId == null || characterId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "오디오북 대상 식별 실패");
        }

        Story story = storyRepository.findById(storyId)
            .orElseThrow(() -> new CustomException(ErrorCode.STORY_NOT_FOUND));
        Character character = characterRepository.findById(characterId)
            .orElseThrow(() -> new CustomException(ErrorCode.CHARACTER_NOT_FOUND));

        String payload = jobResultFetcher.fetchJson(job.getOutputKey());
        String audioUrl = extractFirstAudioKey(payload).orElse(job.getOutputKey());

        AudioBook audioBook = AudioBook.builder()
            .story(story)
            .character(character)
            .member(story.getMember())
            .audioUrl(audioUrl)
            .duration(0.0d)
            .build();

        audioBookRepository.save(audioBook);
    }

    private void validateTarget(Job job, JobTargetType expected) {
        if (job.getTargetType() != expected || job.getTargetId() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "job target 정보가 올바르지 않습니다.");
        }
    }

    private Long extractLong(String payload, String field) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode node = root.path(field);
            if (node.isNumber()) {
                return node.longValue();
            }
            if (node.isTextual()) {
                return Long.valueOf(node.asText());
            }
        } catch (Exception ignored) {
            log.warn("필드 파싱 실패: {}", field);
        }
        return null;
    }

    private Optional<String> extractFirstAudioKey(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode keysNode = root.path("data").path("audio_keys");
            if (keysNode.isArray()) {
                Iterator<JsonNode> iterator = keysNode.elements();
                if (iterator.hasNext()) {
                    String key = iterator.next().asText();
                    if (key != null && !key.isBlank()) {
                        return Optional.of(key);
                    }
                }
            }
        } catch (Exception ignored) {
            log.warn("오디오 key 파싱 실패");
        }
        return Optional.empty();
    }
}
