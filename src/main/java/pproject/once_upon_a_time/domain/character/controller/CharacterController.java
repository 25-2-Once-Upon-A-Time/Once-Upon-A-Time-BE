package pproject.once_upon_a_time.domain.character.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pproject.once_upon_a_time.domain.character.dto.CharacterDetailResponseDto;
import pproject.once_upon_a_time.domain.character.dto.CharacterListResponseDto;
import pproject.once_upon_a_time.domain.character.service.CharacterService;
import pproject.once_upon_a_time.global.response.ApiResult;

import java.util.List;

@RestController
@RequestMapping("/api/v1/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    @GetMapping
    public ResponseEntity<ApiResult<List<CharacterListResponseDto>>> getCharacterList(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResult.ok(characterService.getCharacterList(keyword)));
    }

    @GetMapping("/{characterId}")
    public ResponseEntity<ApiResult<CharacterDetailResponseDto>> getCharacterDetail(
            @PathVariable Long characterId) {
        return ResponseEntity.ok(ApiResult.ok(characterService.getCharacterDetail(characterId)));
    }
}
