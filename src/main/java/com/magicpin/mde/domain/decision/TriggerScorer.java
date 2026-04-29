package com.magicpin.mde.domain.decision;

import com.magicpin.mde.api.dto.ContextUpsertRequest;
import com.magicpin.mde.api.dto.TriggerContextDto;
import com.magicpin.mde.domain.model.TriggerType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TriggerScorer {

  public TriggerPick pickBest(ContextUpsertRequest ctx) {
    TriggerPick demand = scoreDemandSpike(ctx);
    TriggerPick conversionDrop = scoreConversionDrop(ctx);
    TriggerPick lowBookings = scoreLowBookings(ctx);
    TriggerPick campaignInactivity = scoreCampaignInactivity(ctx);
    TriggerPick abandoned = scoreAbandonedInterest(ctx);

    List<TriggerPick> picks = List.of(demand, conversionDrop, lowBookings, abandoned, campaignInactivity);
    return picks.stream().max((a, b) -> Integer.compare(a.getScore(), b.getScore())).orElse(conversionDrop);
  }

  private TriggerPick scoreDemandSpike(ContextUpsertRequest ctx) {
    TriggerContextDto t = ctx.getTriggerContext();
    int searches = t == null || t.getNearbySearchesCountToday() == null ? 0 : t.getNearbySearchesCountToday();
    int change = t == null || t.getSearchChangePct3d() == null ? 0 : t.getSearchChangePct3d();
    int footfall = t == null || t.getLocalFootfallIndex() == null ? 0 : t.getLocalFootfallIndex();

    int score = 0;
    score += bucket(searches, 20, 60, 120, 200) * 18; // up to 72
    score += bucket(change, 5, 12, 20, 35) * 8; // up to 32
    score += bucket(footfall, 60, 90, 120, 150) * 4; // up to 16

    score = Math.min(100, score);

    Map<String, Object> ev = new LinkedHashMap<>();
    ev.put("nearby_searches_count_today", searches);
    ev.put("top_search_keyword", t == null ? null : t.getTopSearchKeyword());
    ev.put("search_change_pct_3d", change);
    ev.put("local_footfall_index", footfall);

    String reason;
    if (searches >= 120) {
      reason = searches + " nearby searches today signals live demand";
    } else if (searches >= 60) {
      reason = searches + " nearby searches today indicates rising intent";
    } else if (change >= 20) {
      reason = "Nearby searches up " + change + "% in last 3 days";
    } else {
      reason = "Moderate nearby demand signals";
    }

    return TriggerPick.builder()
        .type(TriggerType.NEARBY_SEARCH_DEMAND_SPIKE)
        .score(score)
        .reason(reason)
        .evidence(ev)
        .build();
  }

  private TriggerPick scoreConversionDrop(ContextUpsertRequest ctx) {
    Integer delta = ctx.getPerformance().getConversionChangePct7d();
    int drop = delta == null ? 0 : Math.max(0, -delta);

    int score = Math.min(100, drop * 3); // 10% drop -> 30
    if (drop >= 25) score = Math.max(score, 75);
    if (drop >= 15) score = Math.max(score, 55);

    Map<String, Object> ev = new LinkedHashMap<>();
    ev.put("conversion_change_pct_7d", delta);
    ev.put("conversion_rate_pct", ctx.getPerformance().getConversionRatePct());

    String reason =
        drop >= 15
            ? "Conversion dropped by " + drop + "% in 7 days"
            : "Conversion softness detected";

    return TriggerPick.builder()
        .type(TriggerType.CONVERSION_DROP)
        .score(score)
        .reason(reason)
        .evidence(ev)
        .build();
  }

  private TriggerPick scoreLowBookings(ContextUpsertRequest ctx) {
    Integer b = ctx.getPerformance().getBookingsLast7d();
    int bookings = b == null ? 0 : b;
    int score = 0;

    if (bookings <= 2) score = 78;
    else if (bookings <= 5) score = 62;
    else if (bookings <= 10) score = 42;
    else score = 18;

    Map<String, Object> ev = new LinkedHashMap<>();
    ev.put("bookings_last_7d", bookings);

    String reason =
        bookings <= 5
            ? "Only " + bookings + " bookings in last 7 days"
            : "Booking volume stable";

    return TriggerPick.builder()
        .type(TriggerType.LOW_BOOKING_VOLUME)
        .score(score)
        .reason(reason)
        .evidence(ev)
        .build();
  }

  private TriggerPick scoreCampaignInactivity(ContextUpsertRequest ctx) {
    Integer days = ctx.getPerformance().getLastCampaignDaysAgo();
    int d = days == null ? 999 : days;
    int score;
    if (d >= 30) score = 55;
    else if (d >= 14) score = 38;
    else score = 15;

    Map<String, Object> ev = new LinkedHashMap<>();
    ev.put("last_campaign_days_ago", d == 999 ? null : d);

    String reason = d >= 30 ? "No campaign in " + d + " days" : "Campaign activity recent";
    return TriggerPick.builder()
        .type(TriggerType.CAMPAIGN_INACTIVITY)
        .score(score)
        .reason(reason)
        .evidence(ev)
        .build();
  }

  private TriggerPick scoreAbandonedInterest(ContextUpsertRequest ctx) {
    TriggerContextDto t = ctx.getTriggerContext();
    int abandoned = t == null || t.getAbandonedLeadsLast48h() == null ? 0 : t.getAbandonedLeadsLast48h();
    int score = Math.min(100, abandoned * 12); // 5 -> 60
    if (abandoned >= 6) score = Math.max(score, 78);

    Map<String, Object> ev = new LinkedHashMap<>();
    ev.put("abandoned_leads_last_48h", abandoned);

    String reason =
        abandoned >= 4
            ? abandoned + " interested users dropped off in last 48h"
            : "Some abandoned interest detected";

    return TriggerPick.builder()
        .type(TriggerType.ABANDONED_INTEREST)
        .score(score)
        .reason(reason)
        .evidence(ev)
        .build();
  }

  private static int bucket(int value, int a, int b, int c, int d) {
    if (value >= d) return 4;
    if (value >= c) return 3;
    if (value >= b) return 2;
    if (value >= a) return 1;
    return 0;
  }
}

