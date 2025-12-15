package pproject.once_upon_a_time.domain.story.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Parameter;
import pproject.once_upon_a_time.auth.annotation.LoginUser;
import pproject.once_upon_a_time.domain.story.dto.StoryDetailResponseDto;
import pproject.once_upon_a_time.domain.story.dto.StoryListResponseDto;
import pproject.once_upon_a_time.domain.story.dto.UserRequestDto;
import pproject.once_upon_a_time.domain.story.dto.StoryThumbnailResponseDto;
import pproject.once_upon_a_time.domain.story.service.StoryService;
import pproject.once_upon_a_time.domain.story.service.StoryThumbnailService;
import pproject.once_upon_a_time.domain.member.domain.Member;
import pproject.once_upon_a_time.global.response.ApiResult;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;
    private final StoryThumbnailService storyThumbnailService;

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

    @PostMapping("/{storyId}/thumbnail")
    public ResponseEntity<ApiResult<StoryThumbnailResponseDto>> generateThumbnail(
            @PathVariable Long storyId,
            @Parameter(hidden = true) @LoginUser Member member) {
        String thumbnailUrl = storyThumbnailService.generateThumbnail(storyId, member);
        ApiResult<StoryThumbnailResponseDto> body = ApiResult.ok(new StoryThumbnailResponseDto(thumbnailUrl));
        return ResponseEntity.status(body.httpStatus()).body(body);
    }
}
