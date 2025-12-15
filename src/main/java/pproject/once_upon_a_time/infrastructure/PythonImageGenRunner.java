package pproject.once_upon_a_time.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class PythonImageGenRunner {

    @Value("${python.image-path}")
    private String scriptPath;

    @Value("${python.timeout-seconds:120}")
    private long timeoutSeconds;

    /**
     * 파이썬 스크립트에 JSON 문자열을 stdin으로 전달하고 stdout 전체를 반환한다.
     */
    public String run(String inputJson) {
        validateScriptPath();

        ProcessBuilder processBuilder = new ProcessBuilder("python3", scriptPath);
        processBuilder.redirectErrorStream(false);

        Process process;
        try {
            process = processBuilder.start();
            writeToProcess(process, inputJson);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.PYTHON_PROCESS_FAILED, "파이썬 프로세스 시작 실패: " + e.getMessage());
        }

        String stdout = readStream(process.getInputStream());
        String stderr = readStream(process.getErrorStream());

        boolean exited;
        try {
            exited = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorCode.PYTHON_PROCESS_FAILED, "파이썬 프로세스가 인터럽트되었습니다.");
        }

        if (!exited) {
            process.destroyForcibly();
            throw new CustomException(ErrorCode.PYTHON_PROCESS_TIMEOUT,
                    "파이썬 프로세스 타임아웃(" + timeoutSeconds + "초)으로 종료되었습니다.");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.error("Python stderr: {}", stderr);
            throw new CustomException(ErrorCode.PYTHON_PROCESS_FAILED,
                    "파이썬 프로세스가 실패했습니다. exitCode=" + exitCode + ", stderr=" + stderr);
        }

        if (!stderr.isBlank()) {
            log.warn("Python stderr: {}", stderr);
        }

        log.debug("Python stdout: {}", stdout);
        return stdout;
    }

    private void writeToProcess(Process process, String inputJson) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(inputJson);
            writer.flush();
        }
    }

    private String readStream(InputStream inputStream) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!sb.isEmpty()) {
                    sb.append(System.lineSeparator());
                }
                sb.append(line);
            }
        } catch (IOException e) {
            throw new CustomException(ErrorCode.PYTHON_PROCESS_FAILED, "파이썬 프로세스 출력 읽기 실패: " + e.getMessage());
        }
        return sb.toString();
    }

    private void validateScriptPath() {
        if (scriptPath == null || scriptPath.isBlank()) {
            throw new CustomException(ErrorCode.PYTHON_PROCESS_FAILED, "python.script-path 설정이 비어 있습니다.");
        }
    }

    public Duration getTimeout() {
        return Duration.ofSeconds(timeoutSeconds);
    }
}
