package pproject.once_upon_a_time.domain.story.service;

import pproject.once_upon_a_time.domain.story.dto.UserRequestDto;

import java.util.List;

// AI 모델 응답을 표현할 DTOs
// record를 사용하여 불변 데이터 객체를 간결하게 정의합니다.
record AiResponse(
    String content, 
    String summary, 
    List<String> keywords, 
    List<ScriptDto> scripts, 
    MetadataDto metadata, 
    String thumbnailUrl
) {}

record ScriptDto(
    Integer seq, 
    String role, 
    String text, 
    String audioFilePath
) {}

record MetadataDto(
    String projectName, 
    String version, 
    String modelType, 
    Integer totalSegments
) {}


/**
 * 외부 AI 모델과의 통신을 담당하는 클라이언트 인터페이스입니다.
 * 실제 구현체(예: AiClientImpl)는 이 인터페이스를 구현해야 합니다.
 */
public interface AiClient {
    AiResponse generateStory(UserRequestDto request);
}