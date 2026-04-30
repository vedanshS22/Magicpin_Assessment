package com.magicpin.mde.domain.decision;

import static org.assertj.core.api.Assertions.assertThat;

import com.magicpin.mde.api.dto.ContextUpsertRequest;
import com.magicpin.mde.api.dto.OfferDto;
import com.magicpin.mde.domain.model.Category;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class OfferStrengthScorerTest {

  private final OfferStrengthScorer scorer = new OfferStrengthScorer();

  @Test
  void expiredOfferDoesNotReceiveExpiryUrgencyPoints() {
    ContextUpsertRequest ctx = new ContextUpsertRequest();
    ctx.setCategory(Category.DENTIST);

    OfferDto offer = new OfferDto();
    offer.setOfferId("expired");
    offer.setTitle("Expired Checkup");
    offer.setType(OfferDto.OfferType.FIRST_VISIT);
    offer.setPriceInr(299);
    offer.setValidUntil(LocalDate.now().minusDays(1));
    ctx.getOffers().add(offer);

    OfferPick pick = scorer.pickBest(ctx);

    assertThat(pick.getScore()).isEqualTo(58);
    assertThat(pick.getOfferSelected()).contains("Rs 299");
  }
}
