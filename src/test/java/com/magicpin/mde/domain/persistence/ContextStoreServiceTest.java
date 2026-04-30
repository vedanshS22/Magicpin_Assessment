package com.magicpin.mde.domain.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magicpin.mde.api.dto.ContextUpsertRequest;
import com.magicpin.mde.api.dto.MerchantIdentityDto;
import com.magicpin.mde.api.dto.MerchantPerformanceDto;
import com.magicpin.mde.api.exception.ApiException;
import com.magicpin.mde.domain.model.Category;
import org.junit.jupiter.api.Test;

class ContextStoreServiceTest {

  private final ContextStoreService service = new ContextStoreService(new ObjectMapper());

  @Test
  void upsertStoresOnlyHigherVersionsInMemory() {
    ContextUpsertRequest v1 = request(1);
    ContextUpsertRequest sameVersion = request(1);
    ContextUpsertRequest v2 = request(2);

    assertThat(service.upsert(v1).isStored()).isTrue();
    assertThat(service.upsert(sameVersion).isStored()).isFalse();

    var latest = service.upsert(v2);

    assertThat(latest.isStored()).isTrue();
    assertThat(service.getRequired("m_123").getVersion()).isEqualTo(2);
  }

  @Test
  void getRequiredThrowsApiExceptionWhenMissing() {
    assertThatThrownBy(() -> service.getRequired("missing"))
        .isInstanceOf(ApiException.class)
        .extracting("code")
        .isEqualTo("CONTEXT_NOT_FOUND");
  }

  private static ContextUpsertRequest request(long version) {
    ContextUpsertRequest ctx = new ContextUpsertRequest();
    ctx.setVersion(version);
    ctx.setCategory(Category.DENTIST);

    MerchantIdentityDto merchant = new MerchantIdentityDto();
    merchant.setMerchantId("m_123");
    merchant.setMerchantName("Smile Dental");
    ctx.setMerchant(merchant);

    MerchantPerformanceDto performance = new MerchantPerformanceDto();
    performance.setConversionRatePct(14);
    ctx.setPerformance(performance);

    return ctx;
  }
}
