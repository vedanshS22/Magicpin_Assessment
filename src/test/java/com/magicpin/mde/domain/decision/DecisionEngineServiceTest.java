package com.magicpin.mde.domain.decision;

import com.magicpin.mde.api.dto.ContextUpsertRequest;
import com.magicpin.mde.api.dto.MerchantIdentityDto;
import com.magicpin.mde.api.dto.MerchantPerformanceDto;
import com.magicpin.mde.api.dto.OfferDto;
import com.magicpin.mde.api.dto.TriggerContextDto;
import com.magicpin.mde.domain.decision.category.CategoryStrategyFactory;
import com.magicpin.mde.domain.decision.category.DentistStrategy;
import com.magicpin.mde.domain.decision.category.GymStrategy;
import com.magicpin.mde.domain.decision.category.OtherStrategy;
import com.magicpin.mde.domain.decision.category.PharmacyStrategy;
import com.magicpin.mde.domain.decision.category.RestaurantStrategy;
import com.magicpin.mde.domain.decision.category.SalonStrategy;
import com.magicpin.mde.domain.model.Category;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionEngineServiceTest {

  @Test
  void proactiveDecision_isSpecificAndHighConfidenceWhenSignalsStrong() {
    var triggerScorer = new TriggerScorer();
    var merchantHealth = new MerchantHealthScorer();
    var offerScorer = new OfferStrengthScorer();
    var urgency = new UrgencyDecider();
    var cta = new CtaSelector();
    var factory =
        new CategoryStrategyFactory(
            new DentistStrategy(),
            new SalonStrategy(),
            new RestaurantStrategy(),
            new GymStrategy(),
            new PharmacyStrategy(),
            new OtherStrategy());

    var svc =
        new DecisionEngineService(triggerScorer, merchantHealth, offerScorer, urgency, cta, factory);

    ContextUpsertRequest ctx = new ContextUpsertRequest();
    ctx.setVersion(3L);
    ctx.setCategory(Category.DENTIST);

    MerchantIdentityDto m = new MerchantIdentityDto();
    m.setMerchantId("m_123");
    m.setMerchantName("Smile Dental");
    ctx.setMerchant(m);

    MerchantPerformanceDto p = new MerchantPerformanceDto();
    p.setConversionRatePct(14);
    p.setConversionChangePct7d(-22);
    p.setBookingsLast7d(4);
    p.setRepeatRatePct(18);
    p.setCampaignResponseRatePct(6);
    p.setLastCampaignDaysAgo(21);
    ctx.setPerformance(p);

    TriggerContextDto t = new TriggerContextDto();
    t.setNearbySearchesCountToday(190);
    t.setTopSearchKeyword("Dental Check Up");
    t.setSearchChangePct3d(28);
    t.setCustomerResponseProbabilityPct(76);
    t.setWeekendFlag(true);
    t.setLocalFootfallIndex(140);
    ctx.setTriggerContext(t);

    OfferDto o = new OfferDto();
    o.setOfferId("off_1");
    o.setTitle("First-visit Dental Checkup");
    o.setType(OfferDto.OfferType.FIRST_VISIT);
    o.setPriceInr(299);
    o.setDiscountPercent(40);
    ctx.getOffers().add(o);

    MessageStrategy s = svc.decideProactive(ctx);

    assertThat(s.getConfidenceScore()).isGreaterThanOrEqualTo(65);
    assertThat(s.getTriggerReason()).contains("nearby");
    assertThat(s.getOfferSelected()).contains("₹299");
    assertThat(s.getCtaText()).isNotBlank();
    assertThat(s.getEvidence()).containsKeys("nearby_searches_count_today", "offer_price_inr");
  }
}

