package pproject.once_upon_a_time.domain.story.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import pproject.once_upon_a_time.domain.story.dto.UserRequestDto;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AiClientImpl implements AiClient {

    private final WebClient webClient;

    @Override
    public AiResponse generateStory(UserRequestDto request) {
        // WebClient를 사용하여 실제 AI 서버에 POST 요청
        return webClient.post()
                .uri("http://localhost:8000/generate") // 실제 AI 서버의 주소
                .body(Mono.just(request), UserRequestDto.class)
                .retrieve()
                .bodyToMono(AiResponse.class)
                .block(); // 비동기 결과를 동기적으로 기다림
    }
}
