package com.magicpin.mde.domain.llm;

import com.magicpin.mde.config.AppProperties;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiHttpClient implements OpenAiClient {
  private final WebClient openAiWebClient;
  private final AppProperties appProperties;

  @Override
  @Retry(name = "openai")
  public String generate(String prompt) {
    if (!appProperties.getOpenai().isEnabled()) {
      throw new IllegalStateException("OpenAI disabled");
    }
    String apiKey = appProperties.getOpenai().getApiKey();
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException("OpenAI API key missing");
    }

    Map<String, Object> body =
        Map.of(
            "model", appProperties.getOpenai().getModel(),
            "temperature", 0,
            "top_p", 1,
            "max_tokens", appProperties.getOpenai().getMaxOutputTokens(),
            "messages",
                List.of(
                    Map.of("role", "system", "content", "You are a deterministic enterprise copywriter for merchant growth decisions."),
                    Map.of("role", "user", "content", prompt)));

    return openAiWebClient
        .post()
        .uri(OpenAiModels.CHAT_COMPLETIONS_PATH)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
        .bodyValue(body)
        .retrieve()
        .bodyToMono(Map.class)
        .onErrorResume(
            ex -> {
              log.warn("openai call failed: {}", ex.toString());
              return Mono.error(ex);
            })
        .map(OpenAiHttpClient::extract)
        .block();
  }

  @SuppressWarnings("unchecked")
  private static String extract(Map<?, ?> response) {
    // Expected: choices[0].message.content
    Object choicesObj = response.get("choices");
    if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) return "";
    Object first = choices.get(0);
    if (!(first instanceof Map<?, ?> firstMap)) return "";
    Object messageObj = firstMap.get("message");
    if (!(messageObj instanceof Map<?, ?> msg)) return "";
    Object content = msg.get("content");
    return content == null ? "" : content.toString().trim();
  }
}

