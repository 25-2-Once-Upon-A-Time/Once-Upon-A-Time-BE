package pproject.once_upon_a_time.domain.audiobook.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pproject.once_upon_a_time.domain.audiobook.dto.AudioBookResponseDto;
import pproject.once_upon_a_time.domain.audiobook.repository.AudioBookRepository;
import pproject.once_upon_a_time.domain.member.domain.Member;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AudioBookService {

    private final AudioBookRepository audioBookRepository;

    // 회원이 보유한 오디오북 목록을 조회한다.
    @Transactional(readOnly = true)
    public AudioBookResponseDto getAudioBooks(Member member) {
        if (member == null) {
            throw new CustomException(ErrorCode.INVALID_LOGIN);
        }

        Long memberId = member.getId();
        if (memberId == null) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }

        List<AudioBookResponseDto.Item> items = audioBookRepository.findItemsByMemberId(memberId);
        return new AudioBookResponseDto(items);
    }
}
