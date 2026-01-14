package pproject.once_upon_a_time.domain.story.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pproject.once_upon_a_time.domain.job.domain.Job;
import pproject.once_upon_a_time.domain.job.domain.JobType;
import pproject.once_upon_a_time.domain.job.service.JobService;
import pproject.once_upon_a_time.domain.member.domain.Member;
import pproject.once_upon_a_time.domain.story.domain.Story;
import pproject.once_upon_a_time.domain.story.dto.UserRequestDto;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;
import pproject.once_upon_a_time.global.storage.S3StorageService;

@Service
@RequiredArgsConstructor
public class StoryGenerationService {

    private final StoryUpdateService storyUpdateService;
    private final JobService jobService;
    private final S3StorageService s3StorageService;
    private final ObjectMapper objectMapper;

    @Transactional
    public Job createStoryJob(UserRequestDto request, Member member) {
        Member authenticatedMember = requireAuthenticatedMember(member);
        Story story = storyUpdateService.initiateStory(request, authenticatedMember);

        Job job = jobService.createJob(JobType.STORY, null);
        String inputKey = buildInputKey(job);
        String inputJson = buildStoryInput(request, story);
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

    private String buildStoryInput(UserRequestDto request, Story story) {
        try {
            var root = objectMapper.createObjectNode();
            root.put("title", request.getTitle());
            root.put("theme", request.getTheme());
            root.put("vibe", request.getVibe());
            root.put("prompt", request.getPrompt());
            root.put("story_id", story.getId());
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "동화 입력 JSON 생성 실패: " + e.getMessage());
        }
    }

    private String buildInputKey(Job job) {
        return "jobs/" + job.getId() + "/story/input.json";
    }
}
