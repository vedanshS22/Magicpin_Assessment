package com.magicpin.mde.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {
  private Metadata metadata = new Metadata();
  private OpenAi openai = new OpenAi();

  @Data
  public static class Metadata {
    private String teamName = "Magicpin MDE";
    private List<String> teamMembers = new ArrayList<>();
    private String model = "hybrid";
    private String approach = "Rule engine chooses trigger, offer, urgency and rationale; deterministic renderer or LLM handles wording.";
    private String contactEmail = "team@example.com";
    private String version = "1.0.0";
    private String submittedAt = "2026-04-30T00:00:00Z";
  }

  @Data
  public static class OpenAi {
    private boolean enabled = true;
    private String apiKey;
    private String baseUrl;
    private String model;
    private long timeoutMs;
    private int maxOutputTokens;
  }
}

