package pproject.once_upon_a_time.domain.character.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pproject.once_upon_a_time.domain.character.domain.Character;
import pproject.once_upon_a_time.domain.character.dto.CharacterDetailResponseDto;
import pproject.once_upon_a_time.domain.character.dto.CharacterListResponseDto;
import pproject.once_upon_a_time.domain.character.repository.CharacterRepository;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CharacterService {

    private final CharacterRepository characterRepository;

    public List<CharacterListResponseDto> getCharacterList(String keyword) {
        List<Character> characters;
        if (keyword == null || keyword.isBlank()) {
            characters = characterRepository.findAll();
        } else {
            characters = characterRepository.findByCharacterNameContainingIgnoreCase(keyword);
        }
        return characters.stream()
                .map(CharacterListResponseDto::new)
                .collect(Collectors.toList());
    }

    public CharacterDetailResponseDto getCharacterDetail(Long characterId) {
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHARACTER_NOT_FOUND));
        return new CharacterDetailResponseDto(character);
    }
}
