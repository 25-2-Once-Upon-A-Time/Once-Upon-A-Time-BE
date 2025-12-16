package pproject.once_upon_a_time.domain.audiobook.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import pproject.once_upon_a_time.domain.audiobook.domain.AudioBook;
import pproject.once_upon_a_time.domain.audiobook.repository.AudioBookRepository;
import pproject.once_upon_a_time.domain.character.domain.Character;
import pproject.once_upon_a_time.domain.character.repository.CharacterRepository;
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
import pproject.once_upon_a_time.infrastructure.PythonAudioBookRunner;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "python.audiobook-path=content/audio_mock.py",
        "ai.python-path=python3",
        "python.timeout-seconds=30"
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

    @SpyBean
    private PythonAudioBookRunner pythonAudioBookRunner;

    @Test
    void makeAudioBook_savesAudioBookWithParsedResult() {
        Member member = memberRepository.save(createMember("kakao-1", "nick1"));
        Story story = storyRepository.save(createStory(member, "테스트 동화"));
        Character character = characterRepository.save(createCharacter("W0208_사서"));
        saveScripts(story, List.of("첫 문장", "두 번째 문장", "세 번째 문장"));

        AudioBook audioBook = audioBookService.makeAudioBook(story.getId(), character.getId(), member);

        assertThat(audioBookRepository.count()).isEqualTo(1);
        assertThat(audioBook.getAudioUrl()).startsWith("https://mock.storage.local/audiobooks/");
        assertThat(audioBook.getDuration()).isEqualTo(3 * 3.14);
        assertThat(audioBook.getMember().getId()).isEqualTo(member.getId());
        verify(pythonAudioBookRunner, times(1)).run(any());
    }

    @Test
    void makeAudioBook_throwsWhenNoScripts() {
        Member member = memberRepository.save(createMember("kakao-2", "nick2"));
        Story story = storyRepository.save(createStory(member, "스크립트 없음 동화"));
        Character character = characterRepository.save(createCharacter("W0208_사서"));

        CustomException exception = assertThrows(CustomException.class,
                () -> audioBookService.makeAudioBook(story.getId(), character.getId(), member));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
        assertThat(audioBookRepository.count()).isZero();
        verify(pythonAudioBookRunner, times(0)).run(any());
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
