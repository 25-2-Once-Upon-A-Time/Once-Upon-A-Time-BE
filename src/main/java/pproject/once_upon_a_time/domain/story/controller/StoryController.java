package pproject.once_upon_a_time.domain.story.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Parameter;
import pproject.once_upon_a_time.auth.annotation.LoginUser;
import pproject.once_upon_a_time.domain.job.domain.Job;
import pproject.once_upon_a_time.domain.job.dto.JobResponseDto;
import pproject.once_upon_a_time.domain.story.dto.StoryDetailResponseDto;
import pproject.once_upon_a_time.domain.story.dto.StoryListResponseDto;
import pproject.once_upon_a_time.domain.story.dto.UserRequestDto;
import pproject.once_upon_a_time.domain.story.service.StoryGenerationService;
import pproject.once_upon_a_time.domain.story.service.StoryService;
import pproject.once_upon_a_time.domain.story.service.StoryThumbnailService;
import pproject.once_upon_a_time.domain.member.domain.Member;
import pproject.once_upon_a_time.global.response.ApiResult;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;
    private final StoryThumbnailService storyThumbnailService;
    private final StoryGenerationService storyGenerationService;

    @GetMapping
    public ResponseEntity<ApiResult<List<StoryListResponseDto>>> getStoryList(
            @RequestParam(required = false) String keyword,
            @Parameter(hidden = true) @LoginUser Member member) {
        return ResponseEntity.ok(ApiResult.ok(storyService.getStoryList(keyword, member.getId())));
    }

    @GetMapping("/{storyId}")
    public ResponseEntity<ApiResult<StoryDetailResponseDto>> getStoryDetail(
            @PathVariable Long storyId) {
        return ResponseEntity.ok(ApiResult.ok(storyService.getStoryDetail(storyId)));
    }

    @PostMapping
    public ResponseEntity<ApiResult<JobResponseDto>> createStory(
            @Valid @RequestBody UserRequestDto request,
            @Parameter(hidden = true) @LoginUser Member member) {
        Job job = storyGenerationService.createStoryJob(request, member);
        ApiResult<JobResponseDto> body = ApiResult.accepted(JobResponseDto.from(job));
        return ResponseEntity.status(body.httpStatus()).body(body);
    }

    @PostMapping("/{storyId}/thumbnail")
    public ResponseEntity<ApiResult<JobResponseDto>> generateThumbnail(
            @PathVariable Long storyId,
            @Parameter(hidden = true) @LoginUser Member member) {
        Job job = storyThumbnailService.createThumbnailJob(storyId, member);
        ApiResult<JobResponseDto> body = ApiResult.accepted(JobResponseDto.from(job));
        return ResponseEntity.status(body.httpStatus()).body(body);
    }
}
