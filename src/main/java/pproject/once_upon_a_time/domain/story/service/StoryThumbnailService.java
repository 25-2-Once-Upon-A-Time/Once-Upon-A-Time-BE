package pproject.once_upon_a_time.domain.story.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pproject.once_upon_a_time.domain.job.domain.Job;
import pproject.once_upon_a_time.domain.job.domain.JobType;
import pproject.once_upon_a_time.domain.job.service.JobService;
import pproject.once_upon_a_time.domain.member.domain.Member;
import pproject.once_upon_a_time.domain.story.domain.GenerationStatus;
import pproject.once_upon_a_time.domain.story.domain.Story;
import pproject.once_upon_a_time.domain.story.repository.StoryRepository;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;
import pproject.once_upon_a_time.global.storage.S3StorageService;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StoryThumbnailService {

    private final StoryRepository storyRepository;
    private final JobService jobService;
    private final S3StorageService s3StorageService;
    private final ObjectMapper objectMapper;

    @Transactional
    public Job createThumbnailJob(Long storyId, Member loginMember) {
        Story story = storyRepository.findById(storyId)
            .orElseThrow(() -> new CustomException(ErrorCode.STORY_NOT_FOUND));

        validateOwner(story, loginMember);
        validateGenerationStatus(story);

        Job job = jobService.createJob(JobType.ILLUSTRATION, null);
        String inputKey = buildInputKey(job);
        String inputJson = buildIllustrationInput(story);
        s3StorageService.uploadJson(inputKey, inputJson);
        jobService.updateInputKey(job, inputKey);
        jobService.publishJob(job);

        return job;
    }

    private void validateOwner(Story story, Member loginMember) {
        if (!Objects.equals(story.getMember().getId(), loginMember.getId())) {
            throw new CustomException(ErrorCode.STORY_OWNER_MISMATCH);
        }
    }

    private void validateGenerationStatus(Story story) {
        if (story.getGenerationStatus() != GenerationStatus.COMPLETED) {
            throw new CustomException(ErrorCode.STORY_THUMBNAIL_NOT_ALLOWED,
                "썸네일은 COMPLETED 상태에서만 생성할 수 있습니다.");
        }
    }

    private String buildIllustrationInput(Story story) {
        try {
            var root = objectMapper.createObjectNode();
            var storyInfo = objectMapper.createObjectNode();
            storyInfo.put("title", story.getTitle());
            storyInfo.put("summary", story.getSummary());
            storyInfo.putPOJO("keywords", story.getKeywords());
            root.set("story_info", storyInfo);
            root.put("content", story.getContent());
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "일러스트 입력 JSON 생성 실패: " + e.getMessage());
        }
    }

    private String buildInputKey(Job job) {
        return "jobs/" + job.getId() + "/illustration/input.json";
    }
}
