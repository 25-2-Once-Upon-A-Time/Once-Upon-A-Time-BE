package pproject.once_upon_a_time.domain.story.service;

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
        StringBuilder fullOutput = new StringBuilder(); // 파이썬의 모든 출력을 담을 그릇

        try {
            log.info("Running Python AI: {} {}", pythonPath, scriptPath);

            // 1. 프로세스 빌더 설정
            ProcessBuilder pb = new ProcessBuilder(pythonPath, scriptPath);
            pb.redirectErrorStream(true); // stderr도 stdout으로 합쳐서 받음 (파이썬 로그를 놓치지 않기 위해)
            process = pb.start();

            // 2. 입력 데이터 전송 (Java -> Python)
            try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
                String inputJson = objectMapper.writeValueAsString(request);
                writer.write(inputJson);
                writer.flush();
            }

            // 3. 출력 읽기 (Python -> Java) - 실시간 로그 출력 기능 추가
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // [핵심] 파이썬이 뱉는 줄을 실시간으로 자바 콘솔에 찍어줍니다.
                    // JSON 데이터인 경우 너무 기니까 로그에는 생략하거나 앞부분만 찍을 수도 있습니다.
                    if (!line.trim().startsWith("{") && !line.trim().startsWith("}")) {
                        log.info("[Python Log] {}", line);
                    }
                    fullOutput.append(line).append("\n"); // 나중에 파싱하기 위해 저장
                }
            }

            // 4. 프로세스 종료 대기 (타임아웃 설정: 2분)
            // 진짜 AI는 오래 걸리므로 넉넉하게 120초 줍니다.
            if (!process.waitFor(120, TimeUnit.SECONDS)) {
                process.destroy();
                throw new RuntimeException("AI processing timed out (120s)");
            }

            // 5. 결과 문자열에서 JSON 추출 (핵심 로직)
            String rawOutput = fullOutput.toString();
            String jsonResult = extractJsonFromOutput(rawOutput);

            log.info("Final JSON extracted successfully.");

            // 6. JSON -> DTO 변환
            return objectMapper.readValue(jsonResult, AiGenerationResponse.class);

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
     * 파이썬의 잡다한 로그들 사이에서 진짜 JSON 객체 문자열만 찾아내는 메서드
     */
    private String extractJsonFromOutput(String output) {
        if (output == null || output.isBlank()) {
            throw new RuntimeException("Python script returned empty output.");
        }

        // 전략: 최종 결과 JSON에는 반드시 "metadata" 라는 키가 포함되어 있습니다.
        // "metadata"가 포함된 가장 마지막 JSON 블록을 찾습니다.

        // 1. "metadata" 키워드가 있는지 확인
        int metadataIndex = output.lastIndexOf("\"metadata\"");
        if (metadataIndex == -1) {
            log.error("Full Output from Python:\n{}", output); // 디버깅을 위해 전체 출력
            throw new RuntimeException("Could not find 'metadata' key in Python output. AI generation might have failed.");
        }

        // 2. "metadata" 앞쪽에 있는 가장 가까운 '{' (JSON 시작) 찾기
        int jsonStartIndex = output.lastIndexOf("{", metadataIndex);

        // 3. 전체 문자열의 마지막 '}' (JSON 끝) 찾기
        int jsonEndIndex = output.lastIndexOf("}");

        if (jsonStartIndex == -1 || jsonEndIndex == -1 || jsonStartIndex >= jsonEndIndex) {
            throw new RuntimeException("Failed to locate valid JSON brackets in output.");
        }

        // 4. 잘라내기
        return output.substring(jsonStartIndex, jsonEndIndex + 1);
    }
}
