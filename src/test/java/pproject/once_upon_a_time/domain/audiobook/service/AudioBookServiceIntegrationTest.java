package pproject.once_upon_a_time.domain.audiobook.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import pproject.once_upon_a_time.domain.audiobook.repository.AudioBookRepository;
import pproject.once_upon_a_time.domain.character.domain.Character;
import pproject.once_upon_a_time.domain.character.repository.CharacterRepository;
import pproject.once_upon_a_time.domain.job.domain.Job;
import pproject.once_upon_a_time.domain.job.domain.JobStatus;
import pproject.once_upon_a_time.domain.job.domain.JobTargetType;
import pproject.once_upon_a_time.domain.job.domain.JobType;
import pproject.once_upon_a_time.domain.job.service.JobService;
import pproject.once_upon_a_time.domain.member.domain.Member;
import pproject.once_upon_a_time.domain.member.repository.MemberRepository;
import pproject.once_upon_a_time.domain.script.domain.Script;
import pproject.once_upon_a_time.domain.script.repository.ScriptRepository;
import pproject.once_upon_a_time.domain.story.domain.GenerationStatus;
import pproject.once_upon_a_time.domain.story.domain.Story;
import pproject.once_upon_a_time.domain.story.repository.StoryRepository;
import pproject.once_upon_a_time.global.common.MemberRole;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;
import pproject.once_upon_a_time.global.storage.S3StorageService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "jwt.secret=test-secret-test-secret-test-secret-1234",
        "aws.region=ap-northeast-2",
        "aws.s3.bucket=test-bucket",
        "aws.s3.presign-ttl-seconds=900",
        "aws.sqs.queue-url=https://sqs.ap-northeast-2.amazonaws.com/000000000000/test",
        "kakao.client-id=test-client",
        "python.audiobook-path=content/audio_mock.py",
        "python.image-path=content/image_mock.py",
        "python.timeout-seconds=120"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AudioBookServiceIntegrationTest {

    @Autowired
    private AudioBookService audioBookService;

    @Autowired
    private AudioBookRepository audioBookRepository;

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private CharacterRepository characterRepository;

    @Autowired
    private ScriptRepository scriptRepository;

    @Autowired
    private MemberRepository memberRepository;

    @MockitoBean
    private JobService jobService;

    @MockitoBean
    private S3StorageService s3StorageService;

    @Test
    void createAudioBookJob_uploadsInputAndPublishesJob() throws Exception {
        Member member = memberRepository.save(createMember("kakao-1", "nick1"));
        Story story = storyRepository.save(createStory(member, "테스트 동화"));
        Character character = characterRepository.save(createCharacter("W0208_사서"));
        saveScripts(story, List.of("첫 문장", "두 번째 문장", "세 번째 문장"));

        Job job = Job.builder()
                .type(JobType.AUDIOBOOK)
                .status(JobStatus.PENDING)
                .targetType(JobTargetType.STORY)
                .targetId(story.getId())
                .build();
        UUID jobId = UUID.randomUUID();
        java.lang.reflect.Field idField = Job.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(job, jobId);

        when(jobService.createJob(eq(JobType.AUDIOBOOK), eq(JobTargetType.STORY), eq(story.getId()), eq(null)))
            .thenReturn(job);

        Job result = audioBookService.createAudioBookJob(story.getId(), character.getId(), member);

        assertThat(result.getId()).isEqualTo(jobId);
        assertThat(audioBookRepository.count()).isZero();

        String inputKey = "jobs/" + jobId + "/audiobook/input.json";
        verify(s3StorageService).uploadJson(eq(inputKey), any());
        verify(jobService).updateInputKey(job, inputKey);
        verify(jobService).publishJob(job);
    }

    @Test
    void createAudioBookJob_throwsWhenNoScripts() {
        Member member = memberRepository.save(createMember("kakao-2", "nick2"));
        Story story = storyRepository.save(createStory(member, "스크립트 없음 동화"));
        Character character = characterRepository.save(createCharacter("W0208_사서"));

        CustomException exception = assertThrows(CustomException.class,
                () -> audioBookService.createAudioBookJob(story.getId(), character.getId(), member));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
        assertThat(audioBookRepository.count()).isZero();
    }

    private Member createMember(String kakaoUserId, String nickname) {
        return Member.builder()
                .kakaoUserId(kakaoUserId)
                .name("name-" + nickname)
                .nickname(nickname)
                .role(MemberRole.USER)
                .build();
    }

    private Story createStory(Member member, String title) {
        return Story.builder()
                .member(member)
                .version("v1")
                .modelType("model")
                .totalSegments(0)
                .title(title)
                .theme("theme")
                .vibe("vibe")
                .originalPrompt("prompt")
                .targetAge("age")
                .summary("summary")
                .content("content")
                .generationStatus(GenerationStatus.COMPLETED)
                .thumbnailUrl("thumb")
                .completedAt(LocalDateTime.now())
                .keywords(List.of("k1", "k2"))
                .build();
    }

    private Character createCharacter(String name) {
        return Character.builder()
                .characterName(name)
                .voiceSampleUrl("https://sample/voice.wav")
                .build();
    }

    private void saveScripts(Story story, List<String> texts) {
        for (int i = 0; i < texts.size(); i++) {
            scriptRepository.save(
                    Script.builder()
                            .story(story)
                            .seq(i + 1)
                            .role("narrator")
                            .text(texts.get(i))
                            .emotion(null)
                            .audioFilePath(null)
                            .build()
            );
        }
    }
}
