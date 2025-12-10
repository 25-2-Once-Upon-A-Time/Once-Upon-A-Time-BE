package pproject.once_upon_a_time.domain.story.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pproject.once_upon_a_time.domain.story.dto.StoryCreateRequestDto;
import pproject.once_upon_a_time.domain.story.dto.StoryCreateResponseDto;
import pproject.once_upon_a_time.domain.story.dto.StoryDetailResponseDto;
import pproject.once_upon_a_time.domain.story.dto.StoryListResponseDto;
import pproject.once_upon_a_time.domain.story.service.StoryService;
import pproject.once_upon_a_time.global.response.ApiResult;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;

    @GetMapping
    public ResponseEntity<ApiResult<List<StoryListResponseDto>>> getStoryList(
            @RequestParam(required = false) String keyword) {

        return ResponseEntity.ok(ApiResult.ok(storyService.findAllStories(keyword)));
    }

    @GetMapping("/{storyId}")
    public ResponseEntity<ApiResult<StoryDetailResponseDto>> getStoryDetail(
            @PathVariable Long storyId) {

        return ResponseEntity.ok(ApiResult.ok(storyService.findStoryById(storyId)));
    }

    @PostMapping
    public ResponseEntity<ApiResult<StoryCreateResponseDto>> createStory(
            @RequestBody StoryCreateRequestDto requestDto) {

        StoryCreateResponseDto responseDto = storyService.createStory(requestDto);
        return new ResponseEntity<>(ApiResult.created(responseDto), HttpStatus.CREATED);
    }
}
