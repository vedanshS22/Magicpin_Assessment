package com.magicpin.mde.config;

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
    private String botName;
    private String version;
    private String strategy;
    private boolean deterministic;
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

