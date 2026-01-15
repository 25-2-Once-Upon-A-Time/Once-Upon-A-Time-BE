package pproject.once_upon_a_time.domain.job.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pproject.once_upon_a_time.domain.audiobook.domain.AudioBook;
import pproject.once_upon_a_time.domain.audiobook.repository.AudioBookRepository;
import pproject.once_upon_a_time.domain.character.domain.Character;
import pproject.once_upon_a_time.domain.character.repository.CharacterRepository;
import pproject.once_upon_a_time.domain.job.domain.Job;
import pproject.once_upon_a_time.domain.job.domain.JobTargetType;
import pproject.once_upon_a_time.domain.job.domain.JobType;
import pproject.once_upon_a_time.domain.member.domain.Member;
import pproject.once_upon_a_time.domain.story.domain.Story;
import pproject.once_upon_a_time.domain.story.repository.StoryRepository;
import pproject.once_upon_a_time.domain.story.service.StoryUpdateService;
import pproject.once_upon_a_time.global.common.MemberRole;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobResultProcessorTest {

    @Mock
    private JobResultFetcher jobResultFetcher;

    @Mock
    private StoryUpdateService storyUpdateService;

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private CharacterRepository characterRepository;

    @Mock
    private AudioBookRepository audioBookRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private JobResultProcessor buildProcessor() {
        return new JobResultProcessor(
            jobResultFetcher,
            objectMapper,
            storyUpdateService,
            storyRepository,
            characterRepository,
            audioBookRepository
        );
    }

    @Test
    void applySuccess_story_updatesStory() {
        Job job = Job.builder()
            .type(JobType.STORY)
            .targetType(JobTargetType.STORY)
            .targetId(1L)
            .outputKey("jobs/1/story/result.json")
            .build();

        when(jobResultFetcher.fetchJson(eq("jobs/1/story/result.json"))).thenReturn("""
            {
              "success": true,
              "data": {
                "metadata": {"version": "v1", "model_type": "gpt", "total_segments": 1},
                "story_info": {"title": "t", "summary": "s"},
                "script": [{"seq": 1, "role": "n", "text": "hi", "emotion": "neutral", "audio_file_name": "001.wav"}],
                "content": "content",
                "tags": ["a"]
              }
            }
            """);

        buildProcessor().applySuccess(job);

        verify(storyUpdateService).finalizeStory(eq(1L), any());
    }

    @Test
    void applySuccess_illustration_updatesThumbnail() {
        Story story = Story.builder().title("title").build();
        Job job = Job.builder()
            .type(JobType.ILLUSTRATION)
            .targetType(JobTargetType.STORY)
            .targetId(2L)
            .outputKey("jobs/2/illustration/result.json")
            .build();

        when(jobResultFetcher.fetchJson(eq("jobs/2/illustration/result.json"))).thenReturn("""
            {"success": true, "data": {"image_key": "jobs/2/illustration/thumbnail.png"}}
            """);
        when(storyRepository.findById(2L)).thenReturn(Optional.of(story));

        buildProcessor().applySuccess(job);

        assertThat(story.getThumbnailUrl()).isEqualTo("jobs/2/illustration/thumbnail.png");
    }

    @Test
    void applySuccess_audiobook_createsAudioBook() {
        Member member = Member.builder()
            .id(1L)
            .kakaoUserId("kakao-1")
            .name("name")
            .nickname("nick")
            .role(MemberRole.USER)
            .build();
        Story story = Story.builder()
            .member(member)
            .title("title")
            .build();
        Character character = Character.builder()
            .id(5L)
            .characterName("캐릭터")
            .voiceSampleUrl("sample.wav")
            .build();

        Job job = Job.builder()
            .type(JobType.AUDIOBOOK)
            .targetType(JobTargetType.STORY)
            .targetId(3L)
            .inputKey("jobs/3/audiobook/input.json")
            .outputKey("jobs/3/audiobook/result.json")
            .build();

        when(jobResultFetcher.fetchJson(eq("jobs/3/audiobook/input.json"))).thenReturn("""
            {"story_id": 3, "character_id": 5}
            """);
        when(jobResultFetcher.fetchJson(eq("jobs/3/audiobook/result.json"))).thenReturn("""
            {"success": true, "data": {"audio_keys": ["jobs/3/audiobook/001.wav", "jobs/3/audiobook/002.wav"]}}
            """);
        when(storyRepository.findById(3L)).thenReturn(Optional.of(story));
        when(characterRepository.findById(5L)).thenReturn(Optional.of(character));

        buildProcessor().applySuccess(job);

        ArgumentCaptor<AudioBook> captor = ArgumentCaptor.forClass(AudioBook.class);
        verify(audioBookRepository).save(captor.capture());
        assertThat(captor.getValue().getAudioUrl()).isEqualTo("jobs/3/audiobook/001.wav");
        assertThat(captor.getValue().getDuration()).isEqualTo(0.0d);
    }

    @Test
    void applyFailure_story_marksFailed() {
        Job job = Job.builder()
            .type(JobType.STORY)
            .targetType(JobTargetType.STORY)
            .targetId(9L)
            .build();

        buildProcessor().applyFailure(job);

        verify(storyUpdateService).finalizeStoryOnError(9L);
    }
}
