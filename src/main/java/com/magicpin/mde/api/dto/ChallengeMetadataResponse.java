package com.magicpin.mde.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChallengeMetadataResponse {
  @JsonProperty("team_name")
  String teamName;

  @JsonProperty("team_members")
  List<String> teamMembers;

  String model;
  String approach;

  @JsonProperty("contact_email")
  String contactEmail;

  String version;

  @JsonProperty("submitted_at")
  String submittedAt;
}
