package pproject.once_upon_a_time.domain.story.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pproject.once_upon_a_time.domain.job.domain.Job;
import pproject.once_upon_a_time.domain.job.domain.JobStatus;
import pproject.once_upon_a_time.domain.job.domain.JobTargetType;
import pproject.once_upon_a_time.domain.job.domain.JobType;
import pproject.once_upon_a_time.domain.job.service.JobService;
import pproject.once_upon_a_time.domain.member.domain.Member;
import pproject.once_upon_a_time.domain.story.domain.GenerationStatus;
import pproject.once_upon_a_time.domain.story.domain.Story;
import pproject.once_upon_a_time.domain.story.dto.UserRequestDto;
import pproject.once_upon_a_time.global.common.MemberRole;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;
import pproject.once_upon_a_time.global.storage.S3StorageService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StoryGenerationServiceTest {

    @Mock
    private StoryUpdateService storyUpdateService;

    @Mock
    private JobService jobService;

    @Mock
    private S3StorageService s3StorageService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private StoryGenerationService buildService() {
        return new StoryGenerationService(
                storyUpdateService,
                jobService,
                s3StorageService,
                objectMapper
        );
    }

    @Test
    void createStoryJob_uploadsInputAndPublishesJob() throws Exception {
        Member member = createMember(1L);
        UserRequestDto request = createRequest();
        Story story = createStory(member);

        given(storyUpdateService.initiateStory(eq(request), eq(member))).willReturn(story);

        Job job = Job.builder()
                .type(JobType.STORY)
                .status(JobStatus.PENDING)
                .targetType(JobTargetType.STORY)
                .targetId(1L)
                .build();
        UUID jobId = UUID.randomUUID();
        ReflectionTestUtils.setField(job, "id", jobId);
        given(jobService.createJob(eq(JobType.STORY), eq(JobTargetType.STORY), eq(1L), eq(null))).willReturn(job);

        StoryGenerationService service = buildService();
        Job result = service.createStoryJob(request, member);

        assertThat(result.getId()).isEqualTo(jobId);
        String inputKey = "jobs/" + jobId + "/story/input.json";
        verify(s3StorageService).uploadJson(eq(inputKey), any());
        verify(jobService).updateInputKey(job, inputKey);
        verify(jobService).publishJob(job);
    }

    @Test
    void createStoryJob_throwsWhenMemberNull() {
        StoryGenerationService service = buildService();

        CustomException exception = assertThrows(CustomException.class,
                () -> service.createStoryJob(createRequest(), null));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_LOGIN);
    }

    @Test
    void createStoryJob_throwsWhenMemberIdNull() {
        Member member = Member.builder()
                .kakaoUserId("kakao")
                .name("name")
                .nickname("nick")
                .role(MemberRole.USER)
                .build();

        StoryGenerationService service = buildService();

        CustomException exception = assertThrows(CustomException.class,
                () -> service.createStoryJob(createRequest(), member));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    private UserRequestDto createRequest() {
        UserRequestDto request = new UserRequestDto();
        request.setTitle("title");
        request.setTheme("theme");
        request.setVibe("vibe");
        request.setPrompt("prompt");
        return request;
    }

    private Story createStory(Member member) {
        Story story = Story.builder()
                .member(member)
                .title("title")
                .generationStatus(GenerationStatus.PROCESSING)
                .build();
        ReflectionTestUtils.setField(story, "id", 1L);
        return story;
    }

    private Member createMember(Long id) {
        return Member.builder()
                .id(id)
                .kakaoUserId("kakao-" + id)
                .name("name" + id)
                .nickname("nick" + id)
                .role(MemberRole.USER)
                .build();
    }
}
