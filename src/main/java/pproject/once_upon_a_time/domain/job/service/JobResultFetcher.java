package pproject.once_upon_a_time.domain.job.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pproject.once_upon_a_time.global.exception.CustomException;
import pproject.once_upon_a_time.global.exception.ErrorCode;
import pproject.once_upon_a_time.global.storage.S3StorageService;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class JobResultFetcher {

    private final S3StorageService s3StorageService;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String fetchJson(String key) {
        String url = s3StorageService.createPresignedGetUrl(key);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .GET()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() != 200) {
                throw new CustomException(ErrorCode.INVALID_INPUT, "S3 조회 실패: status=" + response.statusCode());
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorCode.INVALID_INPUT, "S3 조회 실패: " + e.getMessage());
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "S3 조회 실패: " + e.getMessage());
        }
    }
}
