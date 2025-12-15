package pproject.once_upon_a_time.domain.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pproject.once_upon_a_time.domain.admin.dto.AdminAudioBookResponseDto;
import pproject.once_upon_a_time.domain.admin.dto.AdminMemberResponseDto;
import pproject.once_upon_a_time.domain.admin.dto.AdminStoryResponseDto;
import pproject.once_upon_a_time.domain.audiobook.repository.AudioBookRepository;
import pproject.once_upon_a_time.domain.member.repository.MemberRepository;
import pproject.once_upon_a_time.domain.story.repository.StoryRepository;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final MemberRepository memberRepository;
    private final StoryRepository storyRepository;
    private final AudioBookRepository audioBookRepository;

    public List<AdminMemberResponseDto> getAllMembers() {
        return memberRepository.findAll().stream()
            .map(AdminMemberResponseDto::from)
            .collect(Collectors.toList());
    }

    @Transactional
    public void deleteMember(Long memberId) {
        // 안전한 삭제를 위해 존재 확인
        if (!memberRepository.existsById(memberId)) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND); // 에러코드 없으면 IllegalArgumentException 사용
        }
        memberRepository.deleteById(memberId);
    }

    public List<AdminStoryResponseDto> getAllStories() {
        return storyRepository.findAll().stream()
            .map(AdminStoryResponseDto::from)
            .collect(Collectors.toList());
    }

    public List<AdminAudioBookResponseDto> getAllAudioBooks() {
        return audioBookRepository.findAll().stream()
            .map(AdminAudioBookResponseDto::from)
            .collect(Collectors.toList());
    }
}
