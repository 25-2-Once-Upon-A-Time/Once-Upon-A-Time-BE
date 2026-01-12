package pproject.once_upon_a_time.domain.audiobook.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pproject.once_upon_a_time.domain.audiobook.dto.AudioBookResponseDto;
import pproject.once_upon_a_time.domain.job.domain.Job;
import pproject.once_upon_a_time.domain.job.domain.JobType;
import pproject.once_upon_a_time.domain.job.service.JobService;
import pproject.once_upon_a_time.domain.character.domain.Character;
import pproject.once_upon_a_time.domain.character.repository.CharacterRepository;
import pproject.once_upon_a_time.domain.audiobook.repository.AudioBookRepository;
import pproject.once_upon_a_time.domain.member.domain.Member;
import pproject.once_upon_a_time.domain.script.domain.Script;
import pproject.once_upon_a_time.domain.script.repository.ScriptRepository;
import pproject.once_upon_a_time.domain.story.domain.Story;
import pproject.once_upon_a_time.domain.story.repository.StoryRepository;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;
import pproject.once_upon_a_time.global.storage.S3StorageService;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AudioBookService {

    private final AudioBookRepository audioBookRepository;
    private final StoryRepository storyRepository;
    private final CharacterRepository characterRepository;
    private final ScriptRepository scriptRepository;
    private final JobService jobService;
    private final S3StorageService s3StorageService;
    private final ObjectMapper objectMapper;

    // 회원이 보유한 오디오북 목록을 조회한다.
    @Transactional(readOnly = true)
    public AudioBookResponseDto getAudioBooks(Member member) {
        Member authenticatedMember = requireAuthenticatedMember(member);

        List<AudioBookResponseDto.Item> items = audioBookRepository.findItemsByMemberId(authenticatedMember.getId());
        return new AudioBookResponseDto(items);
    }

    @Transactional
    public Job createAudioBookJob(Long storyId, Long characterId, Member member) {
        Member authenticatedMember = requireAuthenticatedMember(member);
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORY_NOT_FOUND));
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHARACTER_NOT_FOUND));

        List<Script> scripts = scriptRepository.findByStoryIdOrderBySeqAsc(storyId);
        if (scripts.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "해당 스토리에 스크립트가 없습니다.");
        }

        Job job = jobService.createJob(JobType.AUDIOBOOK, null);
        String inputKey = buildInputKey(job);
        String inputJson = buildAudioBookInput(story, character, scripts);
        s3StorageService.uploadJson(inputKey, inputJson);
        jobService.updateInputKey(job, inputKey);
        jobService.publishJob(job);

        return job;
    }

    private Member requireAuthenticatedMember(Member member) {
        if (member == null) {
            throw new CustomException(ErrorCode.INVALID_LOGIN);
        }

        Long memberId = member.getId();
        if (memberId == null) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }

        return member;
    }

    private String buildAudioBookInput(Story story, Character character, List<Script> scripts) {
        try {
            var root = objectMapper.createObjectNode();
            root.put("title", Objects.toString(story.getTitle(), "Untitled"));
            root.put("character_name", Objects.toString(character.getCharacterName(), "UNKNOWN"));

            List<java.util.Map<String, Object>> scriptItems = new ArrayList<>();
            for (Script script : scripts) {
                java.util.Map<String, Object> item = new HashMap<>();
                item.put("seq", script.getSeq());
                item.put("role", script.getRole());
                item.put("text", Objects.toString(script.getText(), ""));
                item.put("emotion", script.getEmotion());
                item.put("audio_file_name", "segment_" + script.getSeq() + ".wav");
                scriptItems.add(item);
            }
            root.putPOJO("script", scriptItems);
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "오디오북 입력 JSON 생성 실패: " + e.getMessage());
        }
    }

    private String buildInputKey(Job job) {
        return "jobs/" + job.getId() + "/audiobook/input.json";
    }
}
