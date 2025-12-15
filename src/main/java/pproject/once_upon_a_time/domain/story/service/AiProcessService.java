package pproject.once_upon_a_time.domain.story.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pproject.once_upon_a_time.domain.story.dto.AiGenerationResponse;
import pproject.once_upon_a_time.domain.story.dto.UserRequestDto;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiProcessService {

    @Value("${ai.python-path}")
    private String pythonPath;

    @Value("${ai.script-path}")
    private String scriptPath;

    private final ObjectMapper objectMapper;

    public AiGenerationResponse generateStory(UserRequestDto request) {
        Process process = null;
        StringBuilder fullOutput = new StringBuilder();

        try {
            log.info("Running Python AI: {} {}", pythonPath, scriptPath);

            // 1. 프로세스 빌더 설정 (stderr -> stdout 병합)
            ProcessBuilder pb = new ProcessBuilder(pythonPath, scriptPath);
            pb.redirectErrorStream(true);
            process = pb.start();

            // 2. 입력 데이터 전송
            try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
                String inputJson = objectMapper.writeValueAsString(request);
                writer.write(inputJson);
                writer.flush();
            }

            // 3. 출력 읽기 (실시간 로그 출력 + 저장)
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // 파이썬 로그는 실시간으로 보면서 답답함 해소
                    log.info("[Python Log] {}", line);
                    fullOutput.append(line).append("\n");
                }
            }

            // 4. 종료 대기
            if (!process.waitFor(120, TimeUnit.SECONDS)) {
                process.destroy();
                throw new RuntimeException("AI processing timed out");
            }

            // =========================================================
            // [핵심 변경] Wrapper(포장지) 대응 로직
            // =========================================================

            // 5-1. 전체 출력에서 JSON 문자열 발굴 (기준: "success")
            String rawOutput = fullOutput.toString();
            String jsonWrapperString = extractJsonWrapper(rawOutput);

            // 5-2. JsonNode로 파싱 (구조를 모르니 일단 트리로 받음)
            JsonNode rootNode = objectMapper.readTree(jsonWrapperString);

            // 5-3. success 여부 확인
            boolean success = rootNode.path("success").asBoolean(false);
            if (!success) {
                String errorMsg = rootNode.path("error").asText("Unknown error");
                throw new RuntimeException("Python script returned failure: " + errorMsg);
            }

            // 5-4. "data" 필드 꺼내기 (여기가 진짜 알맹이)
            JsonNode dataNode = rootNode.get("data");
            if (dataNode == null || dataNode.isNull()) {
                throw new RuntimeException("Python script returned success but 'data' is null");
            }

            // 6. 알맹이(data)를 DTO로 변환
            return objectMapper.treeToValue(dataNode, AiGenerationResponse.class);

        } catch (Exception e) {
            log.error("Failed to run python script or parse result", e);
            throw new RuntimeException("AI processing failed", e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroy();
            }
        }
    }

    /**
     * 로그와 JSON이 뒤섞인 문자열에서 Wrapper JSON({ "success": ... })을 찾아냅니다.
     */
    private String extractJsonWrapper(String output) {
        if (output == null || output.isBlank()) {
            throw new RuntimeException("Python script returned empty output.");
        }

        // 전략: Wrapper JSON은 무조건 "success"라는 키를 가지고 있습니다.
        // "success"를 찾고, 그 앞의 가장 가까운 '{'를 찾습니다.
        int anchorIndex = output.lastIndexOf("\"success\"");

        if (anchorIndex == -1) {
            // 혹시 success가 없을 수도 있으니, 기존 방식대로 metadata로 시도하거나 에러 처리
            log.warn("'success' key not found, trying fallback extraction...");
            return extractJsonFallback(output);
        }

        // "success" 앞의 '{' 찾기 (JSON 시작)
        int jsonStartIndex = output.lastIndexOf("{", anchorIndex);

        // 전체의 마지막 '}' 찾기 (JSON 끝)
        int jsonEndIndex = output.lastIndexOf("}");

        if (jsonStartIndex == -1 || jsonEndIndex == -1 || jsonStartIndex >= jsonEndIndex) {
            log.error("Full Output:\n{}", output);
            throw new RuntimeException("Failed to locate valid JSON wrapper in output.");
        }

        return output.substring(jsonStartIndex, jsonEndIndex + 1);
    }

    // 만약 파이썬 팀원이 갑자기 맘 바꿔서 포장지를 없앨 경우를 대비한 보험
    private String extractJsonFallback(String output) {
        int metadataIndex = output.lastIndexOf("\"metadata\"");
        if (metadataIndex == -1) {
            log.error("Full Output:\n{}", output);
            throw new RuntimeException("Neither 'success' nor 'metadata' found in output.");
        }
        int jsonStartIndex = output.lastIndexOf("{", metadataIndex);
        int jsonEndIndex = output.lastIndexOf("}");
        return output.substring(jsonStartIndex, jsonEndIndex + 1);
    }
}
