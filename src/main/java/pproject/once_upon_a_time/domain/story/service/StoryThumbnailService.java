package pproject.once_upon_a_time.domain.story.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pproject.once_upon_a_time.domain.member.domain.Member;
import pproject.once_upon_a_time.domain.story.domain.GenerationStatus;
import pproject.once_upon_a_time.domain.story.domain.Story;
import pproject.once_upon_a_time.domain.story.repository.StoryRepository;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;
import pproject.once_upon_a_time.infrastructure.PythonImageGenRunner;
import pproject.once_upon_a_time.infrastructure.SupabaseStorageService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryThumbnailService {

    private final StoryRepository storyRepository;
    private final PythonImageGenRunner pythonImageGenRunner;
    private final SupabaseStorageService supabaseStorageService;
    private final ObjectMapper objectMapper;

    @Transactional
    public String generateThumbnail(Long storyId, Member loginMember) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORY_NOT_FOUND));

        validateOwner(story, loginMember);
        validateGenerationStatus(story);

        if (story.getThumbnailUrl() != null && !story.getThumbnailUrl().isBlank()) {
            return story.getThumbnailUrl();
        }

        String inputJson = buildPythonInput(story);
        String stdout = pythonImageGenRunner.run(inputJson);

        String imageBase64 = parseImageBase64(stdout);
        byte[] imageBytes = decodeBase64(imageBase64);

        String objectName = "stories/" + story.getId() + "/thumbnail.png";
        String uploadedUrl = uploadImage(imageBytes, objectName);

        story.updateThumbnailUrl(uploadedUrl);
        return uploadedUrl;
    }

    private void validateOwner(Story story, Member loginMember) {
        if (!Objects.equals(story.getMember().getId(), loginMember.getId())) {
            throw new CustomException(ErrorCode.STORY_OWNER_MISMATCH);
        }
    }

    private void validateGenerationStatus(Story story) {
        if (story.getGenerationStatus() != GenerationStatus.COMPLETED) {
            throw new CustomException(ErrorCode.STORY_THUMBNAIL_NOT_ALLOWED,
                    "썸네일은 COMPLETED 상태에서만 생성할 수 있습니다.");
        }
    }

    private String buildPythonInput(Story story) {
        try {
            var root = objectMapper.createObjectNode();
            root.put("title", story.getTitle());
            root.put("content", story.getContent());
            root.putPOJO("keywords", story.getKeywords());
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.PYTHON_PROCESS_FAILED, "파이썬 입력 JSON 생성 실패: " + e.getMessage());
        }
    }

    private String parseImageBase64(String stdout) {
        try {
            JsonNode root = objectMapper.readTree(stdout);
            boolean success = root.path("success").asBoolean(false);
            if (!success) {
                String error = root.path("error").asText("파이썬 처리에 실패했습니다.");
                throw new CustomException(ErrorCode.PYTHON_PROCESS_FAILED, error);
            }
            String imageBase64 = root.path("data").path("image_base64").asText(null);
            if (imageBase64 == null || imageBase64.isBlank()) {
                throw new CustomException(ErrorCode.PYTHON_PROCESS_FAILED, "파이썬 결과에 image_base64가 없습니다.");
            }
            return imageBase64;
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.PYTHON_PROCESS_FAILED, "파이썬 출력 파싱 실패: " + e.getMessage());
        }
    }

    private byte[] decodeBase64(String imageBase64) {
        try {
            return Base64.getDecoder().decode(imageBase64.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.PYTHON_PROCESS_FAILED, "image_base64 디코딩 실패: " + e.getMessage());
        }
    }

    private String uploadImage(byte[] imageBytes, String objectName) {
        try {
            return supabaseStorageService.uploadImageBytes(imageBytes, objectName);
        } catch (RuntimeException e) {
            throw new CustomException(ErrorCode.SUPABASE_UPLOAD_FAILED, e.getMessage());
        }
    }
}
