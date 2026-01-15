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
import pproject.once_upon_a_time.domain.story.repository.StoryRepository;
import pproject.once_upon_a_time.global.common.MemberRole;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.storage.S3StorageService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StoryThumbnailServiceTest {

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private JobService jobService;

    @Mock
    private S3StorageService s3StorageService;

    private StoryThumbnailService storyThumbnailService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private StoryThumbnailService buildService() {
        return new StoryThumbnailService(
                storyRepository,
                jobService,
                s3StorageService,
                objectMapper
        );
    }

    @Test
    void createThumbnailJob_uploadsInputAndPublishesJob() throws Exception {
        Member owner = createMember(1L);
        Story story = createStory(owner, GenerationStatus.COMPLETED, null);

        given(storyRepository.findById(1L)).willReturn(Optional.of(story));
        Job job = Job.builder()
                .type(JobType.ILLUSTRATION)
                .status(JobStatus.PENDING)
                .targetType(JobTargetType.STORY)
                .targetId(1L)
                .build();
        UUID jobId = UUID.randomUUID();
        java.lang.reflect.Field idField = Job.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(job, jobId);
        given(jobService.createJob(eq(JobType.ILLUSTRATION), eq(JobTargetType.STORY), eq(1L), eq(null))).willReturn(job);

        storyThumbnailService = buildService();
        Job result = storyThumbnailService.createThumbnailJob(1L, owner);

        assertThat(result.getId()).isEqualTo(jobId);
        String inputKey = "jobs/" + jobId + "/illustration/input.json";
        verify(s3StorageService).uploadJson(eq(inputKey), any());
        verify(jobService).updateInputKey(job, inputKey);
        verify(jobService).publishJob(job);
    }

    @Test
    void createThumbnailJob_throwsWhenOwnerMismatch() {
        Member owner = createMember(1L);
        Member other = createMember(2L);
        Story story = createStory(owner, GenerationStatus.COMPLETED, null);
        given(storyRepository.findById(1L)).willReturn(Optional.of(story));

        CustomException exception = assertThrows(CustomException.class,
                () -> buildService().createThumbnailJob(1L, other));

        assertThat(exception.getErrorCode()).isNotNull();
    }

    @Test
    void createThumbnailJob_throwsWhenStatusNotCompleted() {
        Member owner = createMember(1L);
        Story story = createStory(owner, GenerationStatus.PROCESSING, null);
        given(storyRepository.findById(1L)).willReturn(Optional.of(story));

        assertThrows(CustomException.class, () -> buildService().createThumbnailJob(1L, owner));
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

    private Story createStory(Member member, GenerationStatus status, String thumbnailUrl) {
        Story story = Story.builder()
                .member(member)
                .title("title")
                .content("content")
                .keywords(List.of("a", "b"))
                .generationStatus(status)
                .thumbnailUrl(thumbnailUrl)
                .build();
        ReflectionTestUtils.setField(story, "id", 1L);
        return story;
    }
}
