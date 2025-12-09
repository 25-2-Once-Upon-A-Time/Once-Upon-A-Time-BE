package pproject.once_upon_a_time.domain.audiobook.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pproject.once_upon_a_time.domain.audiobook.dto.AudioBookResponseDto;
import pproject.once_upon_a_time.domain.audiobook.repository.AudioBookRepository;
import pproject.once_upon_a_time.domain.member.domain.Member;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AudioBookService {

    private final AudioBookRepository audioBookRepository;

    @Transactional(readOnly = true)
    public AudioBookResponseDto getAudioBooks(Member member) {
        List<AudioBookResponseDto.Item> items = audioBookRepository.findItemsByMemberId(member.getId());
        return new AudioBookResponseDto(items);
    }
}
