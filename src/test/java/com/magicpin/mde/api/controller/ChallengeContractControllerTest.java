package com.magicpin.mde.api.controller;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ChallengeContractControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void contextEndpointReturnsAcceptedAndStaleSchemas() throws Exception {
    postMerchantContext("m_contract_context", 3);

    mockMvc.perform(
            post("/v1/context")
                .contentType(MediaType.APPLICATION_JSON)
                .content(merchantContextJson("m_contract_context", 2)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accepted").value(false))
        .andExpect(jsonPath("$.reason").value("stale_version"))
        .andExpect(jsonPath("$.current_version").value(3));
  }

  @Test
  void tickEndpointReturnsChallengeActionsSchema() throws Exception {
    postMerchantContext("m_contract_tick", 3);

    mockMvc.perform(
            post("/v1/tick")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "now": "2026-04-26T10:05:00Z",
                      "available_triggers": ["trg_001"]
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("\"customer_id\":null")))
        .andExpect(jsonPath("$.actions[*].merchant_id", hasItem("m_contract_tick")))
        .andExpect(jsonPath("$.actions[?(@.merchant_id == 'm_contract_tick')].conversation_id").exists())
        .andExpect(jsonPath("$.actions[?(@.merchant_id == 'm_contract_tick')].send_as", hasItem("vera")))
        .andExpect(jsonPath("$.actions[?(@.merchant_id == 'm_contract_tick')].trigger_id", hasItem("trg_001")))
        .andExpect(jsonPath("$.actions[?(@.merchant_id == 'm_contract_tick')].template_name").exists())
        .andExpect(jsonPath("$.actions[?(@.merchant_id == 'm_contract_tick')].template_params").exists())
        .andExpect(jsonPath("$.actions[?(@.merchant_id == 'm_contract_tick')].body").exists())
        .andExpect(jsonPath("$.actions[?(@.merchant_id == 'm_contract_tick')].cta", hasItem("open_ended")))
        .andExpect(jsonPath("$.actions[?(@.merchant_id == 'm_contract_tick')].suppression_key").exists())
        .andExpect(jsonPath("$.actions[?(@.merchant_id == 'm_contract_tick')].rationale").exists());
  }

  @Test
  void replyEndpointReturnsChallengeSendSchema() throws Exception {
    postMerchantContext("m_contract_reply", 3);

    mockMvc.perform(
            post("/v1/reply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "conversation_id": "conv_001",
                      "merchant_id": "m_contract_reply",
                      "customer_id": null,
                      "from_role": "merchant",
                      "message": "Yes, send me details",
                      "received_at": "2026-04-26T10:06:00Z",
                      "turn_number": 2
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.action").value("send"))
        .andExpect(jsonPath("$.body").exists())
        .andExpect(jsonPath("$.cta").value("open_ended"))
        .andExpect(jsonPath("$.rationale").exists());
  }

  @Test
  void systemEndpointsReturnChallengeSchemas() throws Exception {
    mockMvc.perform(get("/v1/healthz"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ok"))
        .andExpect(jsonPath("$.uptime_seconds").exists())
        .andExpect(jsonPath("$.contexts_loaded", hasKey("category")))
        .andExpect(jsonPath("$.contexts_loaded", hasKey("merchant")))
        .andExpect(jsonPath("$.contexts_loaded", hasKey("customer")))
        .andExpect(jsonPath("$.contexts_loaded", hasKey("trigger")));

    mockMvc.perform(get("/v1/metadata"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.team_name").exists())
        .andExpect(jsonPath("$.team_members").isArray())
        .andExpect(jsonPath("$.model").exists())
        .andExpect(jsonPath("$.approach").exists())
        .andExpect(jsonPath("$.contact_email").exists())
        .andExpect(jsonPath("$.version").exists())
        .andExpect(jsonPath("$.submitted_at").exists());
  }

  private void postMerchantContext(String merchantId, int version) throws Exception {
    mockMvc.perform(
            post("/v1/context")
                .contentType(MediaType.APPLICATION_JSON)
                .content(merchantContextJson(merchantId, version)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accepted").value(true))
        .andExpect(jsonPath("$.ack_id").exists())
        .andExpect(jsonPath("$.stored_at").exists());
  }

  private static String merchantContextJson(String merchantId, int version) {
    return """
        {
          "scope": "merchant",
          "context_id": "%s",
          "version": %d,
          "payload": {
            "merchant": {
              "merchantId": "%s",
              "merchantName": "Smile Dental",
              "city": "Delhi",
              "locality": "Dwarka"
            },
            "category": "DENTIST",
            "performance": {
              "conversionRatePct": 14,
              "conversionChangePct7d": -22,
              "bookingsLast7d": 4,
              "repeatRatePct": 18,
              "campaignResponseRatePct": 6,
              "lastCampaignDaysAgo": 21
            },
            "triggerContext": {
              "nearbySearchesCountToday": 190,
              "topSearchKeyword": "Dental Check Up",
              "searchChangePct3d": 28,
              "localFootfallIndex": 140,
              "weekendFlag": true,
              "customerResponseProbabilityPct": 76,
              "abandonedLeadsLast48h": 5
            },
            "offers": [{
              "offerId": "off_1",
              "title": "First-visit Dental Checkup",
              "type": "FIRST_VISIT",
              "priceInr": 299,
              "discountPercent": 40
            }]
          },
          "delivered_at": "2026-04-26T10:00:00Z"
        }
        """.formatted(merchantId, version, merchantId);
  }
}
