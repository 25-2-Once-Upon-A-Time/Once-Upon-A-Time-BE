package pproject.once_upon_a_time.domain.story.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pproject.once_upon_a_time.domain.story.dto.StoryCreateResponseDto;
import pproject.once_upon_a_time.domain.story.dto.UserRequestDto;
import pproject.once_upon_a_time.domain.story.service.StoryService;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;

    @PostMapping
    public ResponseEntity<StoryCreateResponseDto> createStory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UserRequestDto request
    ) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        StoryCreateResponseDto responseDto = storyService.createStory(memberId, request);
        return ResponseEntity.created(URI.create("/api/v1/stories/" + responseDto.getStoryId())).body(responseDto);
    }
}
