package core.domain.chat.service;

import core.domain.chat.dto.ClovaXRequest;
import core.domain.chat.dto.ClovaXResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClovaXService {
    private final WebClient webClient;
    @Value("${ncp.clova.apiUrl}")
    private String apiUrl;
    @Value("${ncp.clova.apiKey}")
    private String apiKey;
    public Mono<ClovaXResponse> getAiResponse(List<ClovaXRequest.Message> messages) {
        ClovaXRequest requestPayload = ClovaXRequest.builder()
                .messages(messages)
                .maxTokens(200)
                .temperature(0.5)
                .topK(0)
                .topP(0.8)
                .repeatPenalty(5.0)
                .stopBeforeTermination(true)
                .build();

        return webClient.post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestPayload)
                .retrieve()
                .bodyToMono(ClovaXResponse.class);
    }
}