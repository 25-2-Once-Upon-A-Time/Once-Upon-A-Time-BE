package pproject.once_upon_a_time.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class PythonAudioBookRunner {

    private final ObjectMapper objectMapper;

    @Value("${ai.python-path:python3}")
    private String pythonBin;

    @Value("${python.audiobook-path}")
    private String scriptPath;

    @Value("${python.timeout-seconds:300}")
    private long timeoutSeconds;

    public AudioBookResult run(AudioBookPayload payload) {
        validateConfig();
        String inputJson = toJson(payload);

        Process process;
        try {
            process = new ProcessBuilder(pythonBin, resolveScriptPath()).start();
            writeInput(process, inputJson);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.PYTHON_PROCESS_FAILED, "파이썬 프로세스 시작 실패: " + e.getMessage());
        }

        boolean finished = waitFor(process);
        String stdout = readAll(process.getInputStream());
        String stderr = readAll(process.getErrorStream());

        if (!finished) {
            process.destroyForcibly();
            throw new CustomException(
                    ErrorCode.PYTHON_PROCESS_TIMEOUT,
                    "파이썬 프로세스가 제한 시간(" + timeoutSeconds + "초) 내 종료되지 않았습니다. stderr=" + stderr
            );
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.error("Python stdout: {}", stdout);
            log.error("Python stderr: {}", stderr);
            throw new CustomException(
                    ErrorCode.PYTHON_PROCESS_FAILED,
                    "파이썬 프로세스 실패(exitCode=" + exitCode + "). stdout=" + stdout + " stderr=" + stderr
            );
        }

        if (!stderr.isBlank()) {
            log.warn("Python stderr: {}", stderr);
        }

        return parse(stdout);
    }

    private String resolveScriptPath() {
        return Path.of(scriptPath).toAbsolutePath().toString();
    }

    private void validateConfig() {
        if (scriptPath == null || scriptPath.isBlank()) {
            throw new CustomException(ErrorCode.PYTHON_PROCESS_FAILED, "python.audiobook-path 설정이 비어 있습니다.");
        }
        if (pythonBin == null || pythonBin.isBlank()) {
            throw new CustomException(ErrorCode.PYTHON_PROCESS_FAILED, "ai.python-path 설정이 비어 있습니다.");
        }
    }

    private String toJson(AudioBookPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.PYTHON_PROCESS_FAILED, "파이썬 입력 JSON 직렬화 실패: " + e.getMessage());
        }
    }

    private void writeInput(Process process, String inputJson) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(inputJson);
            writer.flush();
        }
    }

    private boolean waitFor(Process process) {
        try {
            return process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorCode.PYTHON_PROCESS_FAILED, "파이썬 프로세스가 인터럽트되었습니다.");
        }
    }

    private String readAll(InputStream inputStream) {
        try {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.PYTHON_PROCESS_FAILED, "파이썬 출력 읽기 실패: " + e.getMessage());
        }
    }

    private AudioBookResult parse(String stdout) {
        if (stdout == null || stdout.isBlank()) {
            throw new CustomException(ErrorCode.PYTHON_PROCESS_FAILED, "파이썬 출력이 비어 있습니다.");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(stdout);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.PYTHON_PROCESS_FAILED, "파이썬 출력 파싱 실패: " + e.getMessage());
        }

        boolean success = root.path("success").asBoolean(false);
        if (!success) {
            String errorMessage = root.path("error").asText("알 수 없는 오류");
            throw new CustomException(ErrorCode.PYTHON_PROCESS_FAILED, "파이썬 프로세스 실패 응답: " + errorMessage);
        }

        JsonNode audioNode = root.path("data").path("audiobook");
        String audioUrl = audioNode.path("url").asText(null);
        double durationSeconds = audioNode.path("duration").asDouble(Double.NaN);

        if (audioUrl == null || audioUrl.isBlank()) {
            throw new CustomException(ErrorCode.PYTHON_PROCESS_FAILED, "파이썬 결과에 오디오 URL이 없습니다.");
        }
        if (Double.isNaN(durationSeconds)) {
            throw new CustomException(ErrorCode.PYTHON_PROCESS_FAILED, "파이썬 결과에 재생 시간이 없습니다.");
        }

        return new AudioBookResult(audioUrl, durationSeconds);
    }

    public record AudioBookPayload(
            @JsonProperty("title") String title,
            @JsonProperty("narrator_voice") String narratorVoice,
            @JsonProperty("script") List<ScriptItem> scriptItems
    ) {
    }

    public record ScriptItem(
            @JsonProperty("seq") Integer seq,
            @JsonProperty("text") String text
    ) {
    }

    public record AudioBookResult(String audioUrl, double durationSeconds) {
        public int durationSecondsRounded() {
            return (int) Math.round(durationSeconds);
        }
    }
}
