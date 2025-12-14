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

            // [중요] stderr(에러)도 stdout(표준출력)으로 합칩니다.
            // 파이썬이 print() 하든 error() 하든 한 곳으로 받아서 처리하기 위함입니다.
            pb.redirectErrorStream(true);

            process = pb.start();

            // 2. 입력 데이터 전송 (Java -> Python Stdin)
            try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
                String inputJson = objectMapper.writeValueAsString(request);
                writer.write(inputJson);
                writer.flush();
                // 입력 끝났음을 명시 (파이썬의 read()가 종료되도록)
                writer.close();
            }

            // 3. 출력 읽기 (Python -> Java) + 실시간 로그 출력
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // [핵심 1] 파이썬이 뱉는 족족 자바 콘솔에 찍어줍니다.
                    // (JSON 데이터는 너무 길 수 있으므로, 로그에서는 간단히 처리할 수도 있습니다)
                    log.info("[Python Log] {}", line);

                    // 나중에 JSON 파싱을 위해 저장해둡니다.
                    fullOutput.append(line).append("\n");
                }
            }

            // 4. 프로세스 종료 대기 (타임아웃 2분)
            if (!process.waitFor(120, TimeUnit.SECONDS)) {
                process.destroy();
                throw new RuntimeException("AI processing timed out");
            }

            // 5. 결과 문자열에서 JSON 추출 (핵심 로직)
            String rawOutput = fullOutput.toString();
            String jsonResult = extractJsonFromOutput(rawOutput);

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
     * [핵심 2] 파이썬의 잡다한 로그들 사이에서 진짜 JSON 객체 문자열만 찾아내는 메서드
     */
    private String extractJsonFromOutput(String output) {
        if (output == null || output.isBlank()) {
            throw new RuntimeException("Python script returned empty output.");
        }

        // 전략: 우리 JSON에는 반드시 "metadata" 라는 키가 있습니다.
        // 이를 기준점(Anchor)으로 삼아 앞뒤 중괄호 {}를 찾습니다.

        int metadataIndex = output.lastIndexOf("\"metadata\"");
        if (metadataIndex == -1) {
            log.error("Full Output:\n{}", output);
            throw new RuntimeException("Could not find 'metadata' key in output. AI generation might have failed.");
        }

        // "metadata" 앞에 있는 가장 가까운 '{' (JSON 시작점)
        int jsonStartIndex = output.lastIndexOf("{", metadataIndex);

        // 전체 문자열의 맨 뒤에서부터 '}' (JSON 끝점)
        // (주의: JSON 출력 뒤에 로그가 더 있어도, 마지막 '}'가 JSON의 끝이라고 가정합니다)
        int jsonEndIndex = output.lastIndexOf("}");

        if (jsonStartIndex == -1 || jsonEndIndex == -1 || jsonStartIndex >= jsonEndIndex) {
            throw new RuntimeException("Failed to locate valid JSON brackets in output.");
        }

        // 깔끔하게 JSON만 잘라내서 반환
        return output.substring(jsonStartIndex, jsonEndIndex + 1);
    }
}
