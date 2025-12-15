package pproject.once_upon_a_time.domain.story.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pproject.once_upon_a_time.domain.member.domain.Member;
import pproject.once_upon_a_time.domain.story.domain.GenerationStatus;
import pproject.once_upon_a_time.domain.story.domain.Story;
import pproject.once_upon_a_time.domain.story.repository.StoryRepository;
import pproject.once_upon_a_time.global.common.MemberRole;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.infrastructure.PythonImageGenRunner;
import pproject.once_upon_a_time.infrastructure.SupabaseStorageService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StoryThumbnailServiceTest {

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private PythonImageGenRunner pythonImageGenRunner;

    @Mock
    private SupabaseStorageService supabaseStorageService;

    private StoryThumbnailService storyThumbnailService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String BASE64_IMAGE = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMB/6X4Z9kAAAAASUVORK5CYII=";

    @BeforeEach
    void setUp() {
        storyThumbnailService = new StoryThumbnailService(
                storyRepository,
                pythonImageGenRunner,
                supabaseStorageService,
                objectMapper
        );
    }

    @Test
    void generateThumbnail_returnsUploadedUrl() {
        Member owner = createMember(1L);
        Story story = createStory(owner, GenerationStatus.COMPLETED, null);

        given(storyRepository.findById(1L)).willReturn(Optional.of(story));
        given(pythonImageGenRunner.run(any())).willReturn(
                "{\"success\": true, \"data\": {\"image_base64\": \"" + BASE64_IMAGE + "\"}}"
        );
        given(supabaseStorageService.uploadImageBytes(any(), eq("stories/1/thumbnail.png")))
                .willReturn("https://example.com/thumbnail.png");

        String result = storyThumbnailService.generateThumbnail(1L, owner);

        assertThat(result).isEqualTo("https://example.com/thumbnail.png");
        assertThat(story.getThumbnailUrl()).isEqualTo("https://example.com/thumbnail.png");
    }

    @Test
    void generateThumbnail_returnsExistingUrlWhenAlreadyPresent() {
        Member owner = createMember(1L);
        Story story = createStory(owner, GenerationStatus.COMPLETED, "https://existing.png");
        given(storyRepository.findById(1L)).willReturn(Optional.of(story));

        String result = storyThumbnailService.generateThumbnail(1L, owner);

        assertThat(result).isEqualTo("https://existing.png");
    }

    @Test
    void generateThumbnail_throwsWhenOwnerMismatch() {
        Member owner = createMember(1L);
        Member other = createMember(2L);
        Story story = createStory(owner, GenerationStatus.COMPLETED, null);
        given(storyRepository.findById(1L)).willReturn(Optional.of(story));

        CustomException exception = assertThrows(CustomException.class,
                () -> storyThumbnailService.generateThumbnail(1L, other));

        assertThat(exception.getErrorCode()).isNotNull();
    }

    @Test
    void generateThumbnail_throwsWhenStatusNotCompleted() {
        Member owner = createMember(1L);
        Story story = createStory(owner, GenerationStatus.PROCESSING, null);
        given(storyRepository.findById(1L)).willReturn(Optional.of(story));

        assertThrows(CustomException.class, () -> storyThumbnailService.generateThumbnail(1L, owner));
    }

    private Member createMember(Long id) {
        return Member.builder()
                .id(id)
                .kakaoUserId("kakao-" + id)
                .name("name" + id)
                .nickname("nick" + id)
                .role(MemberRole.USER)
                .build();
    }

    private Story createStory(Member member, GenerationStatus status, String thumbnailUrl) {
        return Story.builder()
                .id(1L)
                .member(member)
                .title("title")
                .content("content")
                .keywords(List.of("a", "b"))
                .generationStatus(status)
                .thumbnailUrl(thumbnailUrl)
                .build();
    }
}
