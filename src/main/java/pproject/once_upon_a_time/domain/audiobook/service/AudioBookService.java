package pproject.once_upon_a_time.domain.audiobook.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pproject.once_upon_a_time.domain.audiobook.domain.AudioBook;
import pproject.once_upon_a_time.domain.audiobook.dto.AudioBookResponseDto;
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
import pproject.once_upon_a_time.infrastructure.PythonAudioBookRunner;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AudioBookService {

    private final AudioBookRepository audioBookRepository;
    private final StoryRepository storyRepository;
    private final CharacterRepository characterRepository;
    private final ScriptRepository scriptRepository;
    private final PythonAudioBookRunner pythonAudioBookRunner;

    // 회원이 보유한 오디오북 목록을 조회한다.
    @Transactional(readOnly = true)
    public AudioBookResponseDto getAudioBooks(Member member) {
        Member authenticatedMember = requireAuthenticatedMember(member);

        List<AudioBookResponseDto.Item> items = audioBookRepository.findItemsByMemberId(authenticatedMember.getId());
        return new AudioBookResponseDto(items);
    }

    @Transactional
    public AudioBook makeAudioBook(Long storyId, Long characterId, Member member) {
        Member authenticatedMember = requireAuthenticatedMember(member);
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORY_NOT_FOUND));
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHARACTER_NOT_FOUND));

        List<Script> scripts = scriptRepository.findByStoryIdOrderBySeqAsc(storyId);
        if (scripts.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "해당 스토리에 스크립트가 없습니다.");
        }

        List<PythonAudioBookRunner.ScriptItem> scriptItems = scripts.stream()
                .map(script -> new PythonAudioBookRunner.ScriptItem(
                        script.getSeq(),
                        Objects.toString(script.getText(), "")
                ))
                .toList();

        String title = Objects.toString(story.getTitle(), "Untitled");
        String narratorVoice = Objects.toString(character.getCharacterName(), "UNKNOWN");

        PythonAudioBookRunner.AudioBookPayload payload = new PythonAudioBookRunner.AudioBookPayload(
                title,
                narratorVoice,
                scriptItems
        );

        PythonAudioBookRunner.AudioBookResult result = pythonAudioBookRunner.run(payload);

        AudioBook audioBook = AudioBook.builder()
                .story(story)
                .character(character)
                .member(authenticatedMember)
                .audioUrl(result.audioUrl())
                .duration(result.durationSecondsRounded())
                .build();

        return audioBookRepository.save(audioBook);
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
}
