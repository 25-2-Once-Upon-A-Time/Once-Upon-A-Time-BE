package pproject.once_upon_a_time.domain.story.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import pproject.once_upon_a_time.domain.story.dto.StoryDetailResponseDto;
import pproject.once_upon_a_time.domain.story.dto.StoryListResponseDto;
import pproject.once_upon_a_time.domain.story.dto.UserRequestDto;
import pproject.once_upon_a_time.domain.story.service.StoryService;
import pproject.once_upon_a_time.global.response.ApiResult;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;

    @PostMapping
    public ResponseEntity<StoryDetailResponseDto> createStory(
        @AuthenticationPrincipal UserDetails userDetails,
        @RequestBody UserRequestDto request
    ) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        StoryDetailResponseDto responseDto = storyService.createStory(memberId, request);
        return ResponseEntity.created(URI.create("/api/v1/stories/" + responseDto.getId())).body(responseDto);
    }
    @GetMapping
    public ResponseEntity<ApiResult<List<StoryListResponseDto>>> getStoryList(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResult.ok(storyService.getStoryList(keyword)));
    }

    @GetMapping("/{storyId}")
    public ResponseEntity<ApiResult<StoryDetailResponseDto>> getStoryDetail(
            @PathVariable Long storyId) {
        return ResponseEntity.ok(ApiResult.ok(storyService.getStoryDetail(storyId)));
    }
}
